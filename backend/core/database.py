import sqlite3
from contextlib import contextmanager
from config import settings


@contextmanager
def get_readonly_connection():
    """Open a read-only SQLite connection using URI mode."""
    conn = sqlite3.connect(
        f"file:{settings.SQLITE_DB_PATH}?mode=ro",
        uri=True,
        check_same_thread=False,
    )
    conn.row_factory = sqlite3.Row
    try:
        yield conn
    finally:
        conn.close()


def get_table_schema() -> list[dict]:
    """Return all tables and their columns from the database."""
    with get_readonly_connection() as conn:
        cursor = conn.execute(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"
        )
        tables = []
        for row in cursor.fetchall():
            table_name = row["name"]
            col_cursor = conn.execute(f"PRAGMA table_info('{table_name}')")
            columns = [
                {"name": col["name"], "type": col["type"]}
                for col in col_cursor.fetchall()
            ]
            tables.append({"name": table_name, "columns": columns})
        return tables


def check_connection() -> bool:
    """Verify the database file is accessible."""
    try:
        with get_readonly_connection() as conn:
            conn.execute("SELECT 1")
        return True
    except Exception:
        return False
