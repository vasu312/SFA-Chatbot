import json
import logging
from pathlib import Path

import pandas as pd
from vanna.chromadb import ChromaDB_VectorStore
from vanna.openai import OpenAI_Chat

from config import settings

logger = logging.getLogger(__name__)


class SFAVanna(ChromaDB_VectorStore, OpenAI_Chat):
    """Custom Vanna class combining ChromaDB for vector storage and OpenAI for LLM."""

    def __init__(self):
        ChromaDB_VectorStore.__init__(self, config={
            "path": settings.CHROMADB_PATH,
            "n_results_sql": 5,
            "n_results_ddl": 3,
            "n_results_documentation": 3,
        })
        OpenAI_Chat.__init__(self, config={
            "api_key": settings.OPENAI_API_KEY,
            "model": settings.LLM_MODEL,
        })


# Module-level singleton
_vanna_instance: SFAVanna | None = None


def get_vanna() -> SFAVanna:
    """Get or create the singleton Vanna instance."""
    global _vanna_instance
    if _vanna_instance is None:
        logger.info("Initializing Vanna instance...")
        _vanna_instance = SFAVanna()
        _vanna_instance.connect_to_sqlite(settings.SQLITE_DB_PATH)
        _train_vanna(_vanna_instance)
        logger.info("Vanna instance ready.")
    return _vanna_instance


def _train_vanna(vn: SFAVanna):
    """Train Vanna on schema, documentation, and sample queries.
    Skips if training data already exists (persisted in ChromaDB)."""
    existing = vn.get_training_data()
    if existing is not None and len(existing) > 0:
        logger.info(f"Vanna already has {len(existing)} training entries, skipping training.")
        return

    logger.info("Training Vanna on SFA database schema...")
    base_path = Path(__file__).parent.parent / "data" / "training"

    # 1. Train with DDL statements
    ddl_path = base_path / "ddl.sql"
    if ddl_path.exists():
        ddl_text = ddl_path.read_text()
        for statement in ddl_text.split(";"):
            statement = statement.strip()
            if statement and statement.upper().startswith("CREATE"):
                vn.train(ddl=statement + ";")
                logger.info(f"Trained DDL: {statement[:60]}...")

    # 2. Train with business documentation
    doc_path = base_path / "documentation.md"
    if doc_path.exists():
        doc_text = doc_path.read_text()
        vn.train(documentation=doc_text)
        logger.info("Trained on business documentation.")

    # 3. Train with sample question-SQL pairs
    samples_path = base_path / "sample_queries.json"
    if samples_path.exists():
        samples = json.loads(samples_path.read_text())
        for sample in samples:
            vn.train(question=sample["question"], sql=sample["sql"])
        logger.info(f"Trained on {len(samples)} sample queries.")

    logger.info("Vanna training complete.")


def generate_and_execute(question: str) -> dict:
    """Convert a natural language question to SQL, execute it, and return results.

    Returns a dict with status, generated_sql, results, row_count, columns, and error fields.
    Always returns HTTP-200-friendly data; errors are in the 'error' field.
    """
    vn = get_vanna()

    # Generate SQL
    try:
        sql = vn.generate_sql(question=question)
    except Exception as e:
        logger.error(f"SQL generation failed: {e}")
        return {
            "status": "error",
            "generated_sql": None,
            "results": [],
            "row_count": 0,
            "columns": [],
            "error": f"Failed to generate SQL: {str(e)}",
        }

    if not sql or sql.strip().upper() in ("NO SQL", ""):
        return {
            "status": "error",
            "generated_sql": None,
            "results": [],
            "row_count": 0,
            "columns": [],
            "error": "Could not generate a valid SQL query. Please rephrase your question.",
        }

    # Safety: only allow SELECT statements
    cleaned = sql.strip()
    if cleaned.upper().startswith("SELECT") is False:
        logger.warning(f"Blocked non-SELECT query: {cleaned[:100]}")
        return {
            "status": "error",
            "generated_sql": cleaned,
            "results": [],
            "row_count": 0,
            "columns": [],
            "error": "Only SELECT queries are allowed for safety.",
        }

    # Execute SQL
    try:
        df: pd.DataFrame = vn.run_sql(sql)
    except Exception as e:
        logger.error(f"SQL execution failed: {e}")
        return {
            "status": "error",
            "generated_sql": cleaned,
            "results": [],
            "row_count": 0,
            "columns": [],
            "error": f"SQL execution failed: {str(e)}",
        }

    if df is None or df.empty:
        return {
            "status": "success",
            "generated_sql": cleaned,
            "results": [],
            "row_count": 0,
            "columns": list(df.columns) if df is not None else [],
            "error": None,
        }

    return {
        "status": "success",
        "generated_sql": cleaned,
        "results": df.to_dict(orient="records"),
        "row_count": len(df),
        "columns": list(df.columns),
        "error": None,
    }
