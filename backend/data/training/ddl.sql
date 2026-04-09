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
