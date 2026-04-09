
CREATE TABLE routes (
    id VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE products (
    id VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(26) NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE salesmen (
    id VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_active VARCHAR(20) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE outlets (
    id VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(26) NOT NULL,
    route_id VARCHAR(100) NOT NULL,
    is_active VARCHAR(20) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE salesman_routes (
    id VARCHAR(100) NOT NULL,
    salesman_id VARCHAR(100) NOT NULL,
    route_id VARCHAR(100) NOT NULL,
    is_active VARCHAR(20) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE orders (
    id VARCHAR(100) NOT NULL,
    outlet_id VARCHAR(100) NOT NULL,
    salesman_id VARCHAR(100) NOT NULL,
    order_date TIMESTAMP NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE visits (
    id VARCHAR(100) NOT NULL,
    salesman_id VARCHAR(100) NOT NULL,
    outlet_id VARCHAR(100) NOT NULL,
    visit_date TIMESTAMP NOT NULL,
    has_order VARCHAR(20) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE order_items (
    id VARCHAR(100) NOT NULL,
    order_id VARCHAR(100) NOT NULL,
    product_id VARCHAR(100) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL,
    PRIMARY KEY (id)
);

-- Foreign key constraints
ALTER TABLE salesman_routes ADD CONSTRAINT fk_salesman_routes_salesman_id
    FOREIGN KEY (salesman_id) REFERENCES salesmen(id);
ALTER TABLE salesman_routes ADD CONSTRAINT fk_salesman_routes_route_id
    FOREIGN KEY (route_id) REFERENCES routes(id);
ALTER TABLE outlets ADD CONSTRAINT fk_outlets_route_id
    FOREIGN KEY (route_id) REFERENCES routes(id);
ALTER TABLE orders ADD CONSTRAINT fk_orders_outlet_id
    FOREIGN KEY (outlet_id) REFERENCES outlets(id);
ALTER TABLE orders ADD CONSTRAINT fk_orders_salesman_id
    FOREIGN KEY (salesman_id) REFERENCES salesmen(id);
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_order_id
    FOREIGN KEY (order_id) REFERENCES orders(id);
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_product_id
    FOREIGN KEY (product_id) REFERENCES products(id);
ALTER TABLE visits ADD CONSTRAINT fk_visits_salesman_id
    FOREIGN KEY (salesman_id) REFERENCES salesmen(id);
ALTER TABLE visits ADD CONSTRAINT fk_visits_outlet_id
    FOREIGN KEY (outlet_id) REFERENCES outlets(id);

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
