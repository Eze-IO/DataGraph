CREATE TABLE orders (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_date  DATE           NOT NULL,
    region      VARCHAR(20)    NOT NULL,
    category    VARCHAR(30)    NOT NULL,
    product     VARCHAR(50)    NOT NULL,
    units       INT            NOT NULL,
    unit_price  DECIMAL(10, 2) NOT NULL,
    revenue     DECIMAL(12, 2) NOT NULL
);

CREATE INDEX idx_orders_date ON orders (order_date);
CREATE INDEX idx_orders_region ON orders (region);
