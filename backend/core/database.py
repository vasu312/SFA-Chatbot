import sqlite3
from contextlib import contextmanager
from datetime import date
from typing import Any, Dict, List, Optional

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


def get_table_schema() -> List[Dict]:
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


def get_summary_stats() -> Dict[str, Any]:
    """Return day and month summary stats using actual today's date as reference.
    Shows 0 for today/this month if no data exists — never falls back to last billed date.
    """
    ref_date = str(date.today())          # always actual today
    ref_month = ref_date[:7]              # "YYYY-MM"

    def _zero_stats() -> Dict[str, Any]:
        return {"order_count": 0, "order_value": 0.0, "total_visits": 0, "lines_sold": 0}

    with get_readonly_connection() as conn:

        def _order_stats(date_filter: str, date_value: str) -> Dict[str, Any]:
            order_row = conn.execute(
                f"SELECT COUNT(*), COALESCE(SUM(total_amount), 0) "
                f"FROM orders WHERE {date_filter} = ? AND status = 'completed'",
                (date_value,),
            ).fetchone()
            lines_row = conn.execute(
                f"SELECT COALESCE(SUM(oi.quantity), 0) "
                f"FROM order_items oi JOIN orders o ON oi.order_id = o.id "
                f"WHERE {date_filter} = ? AND o.status = 'completed'",
                (date_value,),
            ).fetchone()
            return {
                "order_count": order_row[0] or 0,
                "order_value": float(order_row[1] or 0),
                "lines_sold": lines_row[0] or 0,
            }

        day_stats = _order_stats("date(order_date)", ref_date)
        day_stats["total_visits"] = conn.execute(
            "SELECT COUNT(*) FROM visits WHERE date(visit_date) = ?", (ref_date,)
        ).fetchone()[0] or 0

        month_stats = _order_stats("strftime('%Y-%m', order_date)", ref_month)
        month_stats["total_visits"] = conn.execute(
            "SELECT COUNT(*) FROM visits WHERE strftime('%Y-%m', visit_date) = ?", (ref_month,)
        ).fetchone()[0] or 0

        top_salesman = conn.execute(
            "SELECT s.name, COALESCE(SUM(o.total_amount), 0) AS val "
            "FROM orders o JOIN salesmen s ON o.salesman_id = s.id "
            "WHERE strftime('%Y-%m', o.order_date) = ? AND o.status = 'completed' "
            "GROUP BY s.id ORDER BY val DESC LIMIT 1",
            (ref_month,),
        ).fetchone()

        top_outlet = conn.execute(
            "SELECT ot.name, COALESCE(SUM(o.total_amount), 0) AS val "
            "FROM orders o JOIN outlets ot ON o.outlet_id = ot.id "
            "WHERE strftime('%Y-%m', o.order_date) = ? AND o.status = 'completed' "
            "GROUP BY ot.id ORDER BY val DESC LIMIT 1",
            (ref_month,),
        ).fetchone()

        top_product = conn.execute(
            "SELECT p.name, COALESCE(SUM(oi.quantity), 0) AS val "
            "FROM order_items oi "
            "JOIN products p ON oi.product_id = p.id "
            "JOIN orders o ON oi.order_id = o.id "
            "WHERE strftime('%Y-%m', o.order_date) = ? AND o.status = 'completed' "
            "GROUP BY p.id ORDER BY val DESC LIMIT 1",
            (ref_month,),
        ).fetchone()

        top_route = conn.execute(
            "SELECT r.name, COALESCE(SUM(o.total_amount), 0) AS val "
            "FROM orders o "
            "JOIN outlets ot ON o.outlet_id = ot.id "
            "JOIN routes r ON ot.route_id = r.id "
            "WHERE strftime('%Y-%m', o.order_date) = ? AND o.status = 'completed' "
            "GROUP BY r.id ORDER BY val DESC LIMIT 1",
            (ref_month,),
        ).fetchone()

        return {
            "day": day_stats,
            "month": month_stats,
            "reference_date": ref_date,
            "top_salesman": {"name": top_salesman[0], "value": float(top_salesman[1])} if top_salesman else None,
            "top_outlet": {"name": top_outlet[0], "value": float(top_outlet[1])} if top_outlet else None,
            "top_product": {"name": top_product[0], "value": float(top_product[1])} if top_product else None,
            "top_route": {"name": top_route[0], "value": float(top_route[1])} if top_route else None,
        }
