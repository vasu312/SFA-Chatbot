## Sales Force Automation (SFA) Database

This database tracks a field sales operation where salesmen visit retail outlets along predefined routes to take orders.

### Tables

- **routes**: Geographic sales routes that salesmen follow. Each route covers a set of outlets.
- **products**: Product catalog. Each product has a category and a unit_price in the local currency.
- **salesmen**: Sales representatives. `is_active` is 'true' or 'false'.
- **outlets**: Retail stores/shops. Each outlet belongs to a route (via route_id) and has a type (e.g., 'Grocery', 'Pharmacy', 'Supermarket'). `is_active` is 'true' or 'false'.
- **salesman_routes**: Maps salesmen to the routes they cover. A salesman can cover multiple routes. `is_active` is 'true' or 'false'.
- **orders**: Sales orders placed at outlets by salesmen. `status` can be 'completed', 'pending', or 'cancelled'. `total_amount` is the order total in local currency.
- **visits**: Records of salesman visits to outlets. `has_order` is 'true' if the visit resulted in an order, 'false' otherwise. A visit may or may not produce an order.
- **order_items**: Line items within an order. Each links to a product and has quantity and unit_price at the time of sale.

### Key Relationships

- A salesman is assigned to routes via salesman_routes.
- Each outlet belongs to exactly one route.
- Orders are placed by a salesman at an outlet.
- Each order contains one or more order_items, each referencing a product.
- Visits track whether a salesman visited an outlet, regardless of whether an order was placed.

### Common Query Patterns

- Total sales = SUM of total_amount from orders WHERE status = 'completed'.
- Visit effectiveness = visits with has_order = 'true' / total visits.
- Product performance = SUM of (quantity * unit_price) from order_items joined with products.
- Active salesmen = WHERE is_active = 'true'.
- Active outlets = WHERE is_active = 'true'.

### Data Conventions

- Boolean fields (is_active, has_order) store 'true' or 'false' as text strings.
- Dates (order_date, visit_date) are stored as ISO 8601 timestamps (YYYY-MM-DD HH:MM:SS).
- All monetary values (unit_price, total_amount) are in local currency with 2 decimal places.
- IDs are VARCHAR strings, not integers.
