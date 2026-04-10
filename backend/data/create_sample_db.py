"""Script to create and populate the SFA SQLite database with sample data."""

import sqlite3
import os
from datetime import date

DB_PATH = os.path.join(os.path.dirname(__file__), "sfa.db")


def create_db():
    if os.path.exists(DB_PATH):
        os.remove(DB_PATH)

    conn = sqlite3.connect(DB_PATH)
    conn.execute("PRAGMA foreign_keys = ON")
    cursor = conn.cursor()

    # Create tables (SQLite-compatible, foreign keys inline)
    cursor.executescript("""
        CREATE TABLE routes (
            id VARCHAR(100) NOT NULL PRIMARY KEY,
            name VARCHAR(100) NOT NULL
        );

        CREATE TABLE products (
            id VARCHAR(100) NOT NULL PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            category VARCHAR(26) NOT NULL,
            unit_price NUMERIC(12,2) NOT NULL
        );

        CREATE TABLE salesmen (
            id VARCHAR(100) NOT NULL PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            is_active VARCHAR(20) NOT NULL
        );

        CREATE TABLE outlets (
            id VARCHAR(100) NOT NULL PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            type VARCHAR(26) NOT NULL,
            route_id VARCHAR(100) NOT NULL REFERENCES routes(id),
            is_active VARCHAR(20) NOT NULL
        );

        CREATE TABLE salesman_routes (
            id VARCHAR(100) NOT NULL PRIMARY KEY,
            salesman_id VARCHAR(100) NOT NULL REFERENCES salesmen(id),
            route_id VARCHAR(100) NOT NULL REFERENCES routes(id),
            is_active VARCHAR(20) NOT NULL
        );

        CREATE TABLE orders (
            id VARCHAR(100) NOT NULL PRIMARY KEY,
            outlet_id VARCHAR(100) NOT NULL REFERENCES outlets(id),
            salesman_id VARCHAR(100) NOT NULL REFERENCES salesmen(id),
            order_date TIMESTAMP NOT NULL,
            total_amount NUMERIC(12,2) NOT NULL,
            status VARCHAR(20) NOT NULL
        );

        CREATE TABLE visits (
            id VARCHAR(100) NOT NULL PRIMARY KEY,
            salesman_id VARCHAR(100) NOT NULL REFERENCES salesmen(id),
            outlet_id VARCHAR(100) NOT NULL REFERENCES outlets(id),
            visit_date TIMESTAMP NOT NULL,
            has_order VARCHAR(20) NOT NULL
        );

        CREATE TABLE order_items (
            id VARCHAR(100) NOT NULL PRIMARY KEY,
            order_id VARCHAR(100) NOT NULL REFERENCES orders(id),
            product_id VARCHAR(100) NOT NULL REFERENCES products(id),
            quantity INTEGER NOT NULL,
            unit_price NUMERIC(12,2) NOT NULL
        );

        -- Indexes
        CREATE INDEX idx_products_category ON products (category);
        CREATE INDEX idx_outlets_route_id ON outlets (route_id);
        CREATE INDEX idx_outlets_type ON outlets (type);
        CREATE INDEX idx_salesman_routes_route_id ON salesman_routes (route_id);
        CREATE INDEX idx_salesman_routes_salesman_id ON salesman_routes (salesman_id);
        CREATE INDEX idx_orders_outlet_id ON orders (outlet_id);
        CREATE INDEX idx_orders_salesman_id ON orders (salesman_id);
        CREATE INDEX idx_orders_status ON orders (status);
        CREATE INDEX idx_visits_outlet_id ON visits (outlet_id);
        CREATE INDEX idx_visits_salesman_id ON visits (salesman_id);
        CREATE INDEX idx_order_items_order_id ON order_items (order_id);
        CREATE INDEX idx_order_items_product_id ON order_items (product_id);
    """)

    # --- Sample Data ---

    # Routes
    routes = [
        ("R001", "North City Route"),
        ("R002", "South City Route"),
        ("R003", "East Industrial Route"),
        ("R004", "West Suburb Route"),
        ("R005", "Central Market Route"),
    ]
    cursor.executemany("INSERT INTO routes VALUES (?, ?)", routes)

    # Products
    products = [
        ("P001", "Rice Premium 5kg", "Grocery", 450.00),
        ("P002", "Wheat Flour 1kg", "Grocery", 65.00),
        ("P003", "Sunflower Oil 1L", "Grocery", 180.00),
        ("P004", "Paracetamol 500mg", "Pharmacy", 25.00),
        ("P005", "Vitamin C Tablets", "Pharmacy", 120.00),
        ("P006", "Hand Wash 250ml", "Personal Care", 85.00),
        ("P007", "Shampoo 200ml", "Personal Care", 150.00),
        ("P008", "Dish Soap 500ml", "Household", 60.00),
        ("P009", "Detergent Powder 1kg", "Household", 110.00),
        ("P010", "Instant Noodles Pack", "Grocery", 30.00),
    ]
    cursor.executemany("INSERT INTO products VALUES (?, ?, ?, ?)", products)

    # Salesmen
    salesmen = [
        ("S001", "Arun Kumar", "true"),
        ("S002", "Priya Sharma", "true"),
        ("S003", "Rajesh Menon", "true"),
        ("S004", "Deepa Nair", "true"),
        ("S005", "Vikram Singh", "false"),
    ]
    cursor.executemany("INSERT INTO salesmen VALUES (?, ?, ?)", salesmen)

    # Outlets
    outlets = [
        ("O001", "City Mart", "Supermarket", "R001", "true"),
        ("O002", "Green Grocery", "Grocery", "R001", "true"),
        ("O003", "Health Plus Pharmacy", "Pharmacy", "R002", "true"),
        ("O004", "Quick Stop", "Grocery", "R002", "true"),
        ("O005", "MegaStore", "Supermarket", "R003", "true"),
        ("O006", "Corner Shop", "Grocery", "R003", "true"),
        ("O007", "Wellness Pharmacy", "Pharmacy", "R004", "true"),
        ("O008", "Fresh Mart", "Grocery", "R004", "true"),
        ("O009", "Central Bazaar", "Supermarket", "R005", "true"),
        ("O010", "Daily Needs", "Grocery", "R005", "true"),
        ("O011", "Old Store", "Grocery", "R001", "false"),
    ]
    cursor.executemany("INSERT INTO outlets VALUES (?, ?, ?, ?, ?)", outlets)

    # Salesman-Route assignments
    salesman_routes = [
        ("SR001", "S001", "R001", "true"),
        ("SR002", "S001", "R005", "true"),
        ("SR003", "S002", "R002", "true"),
        ("SR004", "S002", "R003", "true"),
        ("SR005", "S003", "R003", "true"),
        ("SR006", "S003", "R004", "true"),
        ("SR007", "S004", "R004", "true"),
        ("SR008", "S004", "R005", "true"),
        ("SR009", "S005", "R001", "false"),
    ]
    cursor.executemany("INSERT INTO salesman_routes VALUES (?, ?, ?, ?)", salesman_routes)

    # Orders
    orders = [
        ("ORD001", "O001", "S001", "2026-04-01 09:30:00", 1250.00, "completed"),
        ("ORD002", "O002", "S001", "2026-04-01 11:00:00", 680.00, "completed"),
        ("ORD003", "O003", "S002", "2026-04-01 10:00:00", 340.00, "completed"),
        ("ORD004", "O005", "S002", "2026-04-02 09:00:00", 2100.00, "completed"),
        ("ORD005", "O006", "S003", "2026-04-02 10:30:00", 450.00, "completed"),
        ("ORD006", "O007", "S003", "2026-04-02 14:00:00", 290.00, "completed"),
        ("ORD007", "O008", "S004", "2026-04-03 09:15:00", 870.00, "completed"),
        ("ORD008", "O009", "S004", "2026-04-03 11:30:00", 1560.00, "completed"),
        ("ORD009", "O010", "S001", "2026-04-03 15:00:00", 520.00, "pending"),
        ("ORD010", "O001", "S001", "2026-04-04 09:00:00", 980.00, "completed"),
        ("ORD011", "O004", "S002", "2026-04-04 10:00:00", 175.00, "cancelled"),
        ("ORD012", "O005", "S002", "2026-04-05 09:30:00", 1890.00, "completed"),
        ("ORD013", "O009", "S004", "2026-04-05 11:00:00", 2340.00, "completed"),
        ("ORD014", "O002", "S001", "2026-04-06 10:00:00", 410.00, "completed"),
        ("ORD015", "O006", "S003", "2026-04-07 09:00:00", 720.00, "completed"),
        ("ORD016", "O001", "S001", "2026-04-07 14:00:00", 1100.00, "completed"),
        ("ORD017", "O003", "S002", "2026-04-08 09:30:00", 485.00, "completed"),
        ("ORD018", "O008", "S004", "2026-04-08 11:00:00", 650.00, "pending"),
        ("ORD019", "O010", "S004", "2026-04-08 14:30:00", 390.00, "completed"),
        ("ORD020", "O009", "S004", "2026-04-09 09:00:00", 1750.00, "completed"),
    ]
    cursor.executemany("INSERT INTO orders VALUES (?, ?, ?, ?, ?, ?)", orders)

    # Visits
    visits = [
        ("V001", "S001", "O001", "2026-04-01 09:15:00", "true"),
        ("V002", "S001", "O002", "2026-04-01 10:45:00", "true"),
        ("V003", "S001", "O011", "2026-04-01 12:00:00", "false"),
        ("V004", "S002", "O003", "2026-04-01 09:45:00", "true"),
        ("V005", "S002", "O004", "2026-04-01 11:30:00", "false"),
        ("V006", "S002", "O005", "2026-04-02 08:45:00", "true"),
        ("V007", "S003", "O006", "2026-04-02 10:15:00", "true"),
        ("V008", "S003", "O007", "2026-04-02 13:45:00", "true"),
        ("V009", "S003", "O008", "2026-04-02 15:30:00", "false"),
        ("V010", "S004", "O008", "2026-04-03 09:00:00", "true"),
        ("V011", "S004", "O009", "2026-04-03 11:15:00", "true"),
        ("V012", "S004", "O010", "2026-04-03 14:00:00", "false"),
        ("V013", "S001", "O010", "2026-04-03 14:45:00", "true"),
        ("V014", "S001", "O001", "2026-04-04 08:45:00", "true"),
        ("V015", "S002", "O004", "2026-04-04 09:45:00", "true"),
        ("V016", "S002", "O005", "2026-04-05 09:15:00", "true"),
        ("V017", "S004", "O009", "2026-04-05 10:45:00", "true"),
        ("V018", "S001", "O002", "2026-04-06 09:45:00", "true"),
        ("V019", "S003", "O006", "2026-04-07 08:45:00", "true"),
        ("V020", "S001", "O001", "2026-04-07 13:45:00", "true"),
        ("V021", "S002", "O003", "2026-04-08 09:15:00", "true"),
        ("V022", "S004", "O008", "2026-04-08 10:45:00", "true"),
        ("V023", "S004", "O010", "2026-04-08 14:15:00", "true"),
        ("V024", "S004", "O009", "2026-04-09 08:45:00", "true"),
        ("V025", "S003", "O007", "2026-04-09 10:00:00", "false"),
    ]
    cursor.executemany("INSERT INTO visits VALUES (?, ?, ?, ?, ?)", visits)

    # Order Items
    order_items = [
        ("OI001", "ORD001", "P001", 2, 450.00),
        ("OI002", "ORD001", "P003", 1, 180.00),
        ("OI003", "ORD001", "P010", 4, 30.00),
        ("OI004", "ORD002", "P002", 5, 65.00),
        ("OI005", "ORD002", "P008", 3, 60.00),
        ("OI006", "ORD002", "P010", 5, 30.00),
        ("OI007", "ORD003", "P004", 10, 25.00),
        ("OI008", "ORD003", "P005", 3, 120.00),
        ("OI009", "ORD004", "P001", 3, 450.00),
        ("OI010", "ORD004", "P003", 2, 180.00),
        ("OI011", "ORD004", "P009", 1, 110.00),
        ("OI012", "ORD005", "P002", 3, 65.00),
        ("OI013", "ORD005", "P010", 8, 30.00),
        ("OI014", "ORD006", "P004", 5, 25.00),
        ("OI015", "ORD006", "P005", 1, 120.00),
        ("OI016", "ORD007", "P006", 4, 85.00),
        ("OI017", "ORD007", "P007", 3, 150.00),
        ("OI018", "ORD008", "P001", 2, 450.00),
        ("OI019", "ORD008", "P009", 3, 110.00),
        ("OI020", "ORD008", "P008", 5, 60.00),
        ("OI021", "ORD009", "P002", 4, 65.00),
        ("OI022", "ORD009", "P010", 6, 30.00),
        ("OI023", "ORD010", "P001", 1, 450.00),
        ("OI024", "ORD010", "P006", 2, 85.00),
        ("OI025", "ORD010", "P003", 1, 180.00),
        ("OI026", "ORD012", "P001", 3, 450.00),
        ("OI027", "ORD012", "P007", 2, 150.00),
        ("OI028", "ORD012", "P009", 2, 110.00),
        ("OI029", "ORD013", "P001", 4, 450.00),
        ("OI030", "ORD013", "P003", 2, 180.00),
        ("OI031", "ORD013", "P010", 6, 30.00),
        ("OI032", "ORD014", "P002", 3, 65.00),
        ("OI033", "ORD014", "P010", 7, 30.00),
        ("OI034", "ORD015", "P001", 1, 450.00),
        ("OI035", "ORD015", "P008", 3, 60.00),
        ("OI036", "ORD015", "P010", 3, 30.00),
        ("OI037", "ORD016", "P001", 2, 450.00),
        ("OI038", "ORD016", "P006", 1, 85.00),
        ("OI039", "ORD016", "P007", 1, 150.00),
        ("OI040", "ORD017", "P004", 8, 25.00),
        ("OI041", "ORD017", "P005", 2, 120.00),
        ("OI042", "ORD018", "P006", 3, 85.00),
        ("OI043", "ORD018", "P009", 2, 110.00),
        ("OI044", "ORD019", "P002", 2, 65.00),
        ("OI045", "ORD019", "P010", 5, 30.00),
        ("OI046", "ORD019", "P008", 2, 60.00),
        ("OI047", "ORD020", "P001", 3, 450.00),
        ("OI048", "ORD020", "P003", 1, 180.00),
        ("OI049", "ORD020", "P009", 2, 110.00),
    ]
    cursor.executemany("INSERT INTO order_items VALUES (?, ?, ?, ?, ?)", order_items)

    # --- Today's data (always uses actual today so the dashboard always shows live numbers) ---
    today = str(date.today())

    today_orders = [
        ("ORD021", "O001", "S001", f"{today} 08:55:00", 1680.00, "completed"),
        ("ORD022", "O003", "S002", f"{today} 09:30:00",  560.00, "completed"),
        ("ORD023", "O005", "S002", f"{today} 10:45:00", 2320.00, "completed"),
        ("ORD024", "O007", "S003", f"{today} 11:00:00",  380.00, "completed"),
        ("ORD025", "O009", "S004", f"{today} 14:00:00", 1710.00, "completed"),
        ("ORD026", "O010", "S004", f"{today} 15:30:00",  420.00, "pending"),   # pending — not counted
    ]
    cursor.executemany("INSERT INTO orders VALUES (?, ?, ?, ?, ?, ?)", today_orders)

    today_visits = [
        ("V026", "S001", "O001", f"{today} 08:40:00", "true"),
        ("V027", "S001", "O002", f"{today} 10:15:00", "false"),
        ("V028", "S002", "O003", f"{today} 09:10:00", "true"),
        ("V029", "S002", "O005", f"{today} 10:30:00", "true"),
        ("V030", "S003", "O007", f"{today} 10:45:00", "true"),
        ("V031", "S004", "O009", f"{today} 13:45:00", "true"),
        ("V032", "S004", "O010", f"{today} 15:20:00", "true"),
    ]
    cursor.executemany("INSERT INTO visits VALUES (?, ?, ?, ?, ?)", today_visits)

    today_order_items = [
        # ORD021 (O001, S001) = 1680
        ("OI050", "ORD021", "P001", 3, 450.00),   # 1350
        ("OI051", "ORD021", "P003", 1, 180.00),   #  180
        ("OI052", "ORD021", "P010", 5,  30.00),   #  150  → 1680 ✓
        # ORD022 (O003, S002) = 560
        ("OI053", "ORD022", "P004", 8,  25.00),   #  200
        ("OI054", "ORD022", "P005", 3, 120.00),   #  360  →  560 ✓
        # ORD023 (O005, S002) = 2320
        ("OI055", "ORD023", "P001", 4, 450.00),   # 1800
        ("OI056", "ORD023", "P007", 2, 150.00),   #  300
        ("OI057", "ORD023", "P009", 2, 110.00),   #  220  → 2320 ✓
        # ORD024 (O007, S003) = 380
        ("OI058", "ORD024", "P006", 3,  85.00),   #  255
        ("OI059", "ORD024", "P004", 5,  25.00),   #  125  →  380 ✓
        # ORD025 (O009, S004) = 1710
        ("OI060", "ORD025", "P001", 3, 450.00),   # 1350
        ("OI061", "ORD025", "P003", 1, 180.00),   #  180
        ("OI062", "ORD025", "P008", 3,  60.00),   #  180  → 1710 ✓
    ]
    cursor.executemany("INSERT INTO order_items VALUES (?, ?, ?, ?, ?)", today_order_items)

    conn.commit()
    conn.close()
    print(f"Database created at: {DB_PATH}")

    # Verify
    conn = sqlite3.connect(DB_PATH)
    for table in ["routes", "products", "salesmen", "outlets", "salesman_routes", "orders", "visits", "order_items"]:
        count = conn.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
        print(f"  {table}: {count} rows")

    today_str = str(date.today())
    today_orders_count = conn.execute(
        "SELECT COUNT(*) FROM orders WHERE date(order_date) = ? AND status = 'completed'", (today_str,)
    ).fetchone()[0]
    today_value = conn.execute(
        "SELECT COALESCE(SUM(total_amount),0) FROM orders WHERE date(order_date) = ? AND status = 'completed'", (today_str,)
    ).fetchone()[0]
    month_str = today_str[:7]
    month_orders_count = conn.execute(
        "SELECT COUNT(*) FROM orders WHERE strftime('%Y-%m', order_date) = ? AND status = 'completed'", (month_str,)
    ).fetchone()[0]
    print(f"\nToday ({today_str}): {today_orders_count} completed orders, value = {today_value}")
    print(f"This month ({month_str}): {month_orders_count} completed orders")
    conn.close()


if __name__ == "__main__":
    create_db()
