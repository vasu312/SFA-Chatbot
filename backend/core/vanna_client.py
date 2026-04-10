"""
NL-to-SQL engine using Anthropic Claude directly.

Replaces vanna.ai (which requires Python 3.10+) with a direct Anthropic API
call. The schema DDL, business docs, and sample Q&A pairs are embedded in the
system prompt so no separate vector store is needed.
"""
import json
import logging
import re
import sqlite3
from pathlib import Path
from typing import Dict, List, Optional

import anthropic

from config import settings

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# System prompt – built once at module load from the training files
# ---------------------------------------------------------------------------

def _build_system_prompt() -> str:
    base = Path(__file__).parent.parent / "data" / "training"

    ddl = (base / "ddl.sql").read_text() if (base / "ddl.sql").exists() else ""
    docs = (base / "documentation.md").read_text() if (base / "documentation.md").exists() else ""

    samples_text = ""
    samples_path = base / "sample_queries.json"
    if samples_path.exists():
        samples: List[Dict] = json.loads(samples_path.read_text())
        lines = []
        for s in samples:
            lines.append(f"Q: {s['question']}\nSQL: {s['sql']}")
        samples_text = "\n\n".join(lines)

    return f"""You are a SQL expert for a Sales Force Automation (SFA) database.
Given a natural language question, return ONLY a valid SQLite SELECT query — no explanations, no markdown, no code fences.

## Database Schema (DDL)
{ddl}

## Business Documentation
{docs}

## Example Q&A pairs
{samples_text}

Rules:
- Return only the raw SQL query, nothing else.
- Only SELECT statements are allowed.
- Use exact column and table names from the schema above.
- Boolean fields (is_active, has_order) store the text 'true' or 'false'.
- IDs are VARCHAR strings, not integers.
- Always use readable Title Case aliases for computed or ambiguous columns. Examples:
    COUNT(*) AS "Total Orders"
    SUM(total_amount) AS "Total Value"
    AVG(unit_price) AS "Avg Unit Price"
    SUM(oi.quantity) AS "Total Quantity"
    s.name AS "Salesman Name"
    o.order_date AS "Order Date"
    strftime('%Y-%m', order_date) AS "Month"
    ROUND(..., 2) AS "Percentage"
  Plain columns from a single unambiguous table (e.g. id, name, status) do not need aliases.
"""


_SYSTEM_PROMPT: str = _build_system_prompt()


# ---------------------------------------------------------------------------
# Conversational message detection
# ---------------------------------------------------------------------------

_CONVERSATIONAL_WORDS = {
    "hi", "hello", "hey", "howdy", "hiya",
    "thanks", "thank", "you", "cheers", "ok", "okay", "great", "cool", "awesome",
    "bye", "goodbye", "see", "later",
    "who", "what", "help", "can", "are", "do", "does",
    "good", "morning", "afternoon", "evening", "night",
}

_GREETING_PHRASES = {"hi", "hello", "hey", "thanks", "bye", "howdy", "hiya"}


def _is_conversational(question: str) -> bool:
    """Return True if the question is clearly conversational, not a data query.

    Matches pure greetings/pleasantries so we skip the LLM call entirely and
    avoid the ~60s token-generation delay for non-SQL responses.
    """
    q = question.lower().strip().rstrip("!.,?")
    if q in _GREETING_PHRASES:
        return True
    words = set(q.split())
    return bool(words) and words.issubset(_CONVERSATIONAL_WORDS)


# ---------------------------------------------------------------------------
# Core functions
# ---------------------------------------------------------------------------

def _generate_sql(question: str) -> str:
    """Ask Claude to convert a natural language question to SQL."""
    client = anthropic.Anthropic(api_key=settings.ANTHROPIC_API_KEY)
    response = client.messages.create(
        model=settings.LLM_MODEL,
        max_tokens=512,
        system=_SYSTEM_PROMPT,
        messages=[{"role": "user", "content": question}],
    )
    raw = response.content[0].text.strip()

    # Strip markdown code fences if the model wraps the SQL anyway
    raw = re.sub(r"^```(?:sql)?\s*", "", raw, flags=re.IGNORECASE)
    raw = re.sub(r"\s*```$", "", raw)
    return raw.strip()


def _run_sql(sql: str) -> List[Dict]:
    """Execute a SELECT query against the SQLite DB and return rows as dicts."""
    conn = sqlite3.connect(settings.SQLITE_DB_PATH)
    conn.row_factory = sqlite3.Row
    try:
        cursor = conn.execute(sql)
        rows = cursor.fetchall()
        return [dict(row) for row in rows]
    finally:
        conn.close()


def generate_and_execute(question: str) -> dict:
    """Convert a natural language question to SQL, execute it, and return results.

    Returns a dict with status, generated_sql, results, row_count, columns,
    and error fields. Always returns HTTP-200-friendly data; errors are in the
    'error' field.
    """
    # Short-circuit: skip the LLM entirely for greetings/conversational messages.
    # Without this, the LLM generates a long text reply (~60s) which hits the
    # Android read timeout before the backend can return an error response.
    if _is_conversational(question):
        logger.info(f"Conversational message intercepted (no LLM call): {question!r}")
        return {
            "status": "error",
            "generated_sql": None,
            "results": [],
            "row_count": 0,
            "columns": [],
            "error": "not_a_data_question",
        }

    # Generate SQL
    try:
        sql = _generate_sql(question)
    except Exception as e:
        logger.error(f"SQL generation failed: {e}")
        return {
            "status": "error",
            "generated_sql": None,
            "results": [],
            "row_count": 0,
            "columns": [],
            "error": f"Failed to generate SQL: {e}",
        }

    if not sql or sql.upper() in ("NO SQL", ""):
        return {
            "status": "error",
            "generated_sql": None,
            "results": [],
            "row_count": 0,
            "columns": [],
            "error": "Could not generate a valid SQL query. Please rephrase your question.",
        }

    # Safety: only allow SELECT
    if not sql.strip().upper().startswith("SELECT"):
        logger.warning(f"Blocked non-SELECT query: {sql[:100]}")
        return {
            "status": "error",
            "generated_sql": sql,
            "results": [],
            "row_count": 0,
            "columns": [],
            "error": "Only SELECT queries are allowed for safety.",
        }

    # Execute
    try:
        rows = _run_sql(sql)
    except Exception as e:
        logger.error(f"SQL execution failed: {e}")
        return {
            "status": "error",
            "generated_sql": sql,
            "results": [],
            "row_count": 0,
            "columns": [],
            "error": f"SQL execution failed: {e}",
        }

    columns = list(rows[0].keys()) if rows else []
    return {
        "status": "success",
        "generated_sql": sql,
        "results": rows,
        "row_count": len(rows),
        "columns": columns,
        "error": None,
    }


# ---------------------------------------------------------------------------
# Kept for compatibility with router.py which calls get_vanna() for /health
# ---------------------------------------------------------------------------

def get_vanna():
    """Stub — returns None; health check uses database.py directly."""
    return None
