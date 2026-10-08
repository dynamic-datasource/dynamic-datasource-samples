DROP TABLE IF EXISTS p_order;
CREATE TABLE p_order
(
    id               SERIAL PRIMARY KEY,
    user_id          BIGINT           DEFAULT NULL,
    product_id       BIGINT           DEFAULT NULL,
    amount           BIGINT           DEFAULT NULL,
    total_price      DOUBLE PRECISION DEFAULT NULL,
    status           VARCHAR(100)     DEFAULT NULL,
    add_time         TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    last_update_time TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);