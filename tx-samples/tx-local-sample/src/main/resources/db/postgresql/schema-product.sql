DROP TABLE IF EXISTS product;
CREATE TABLE product
(
    id               SERIAL PRIMARY KEY,
    price            DOUBLE PRECISION DEFAULT NULL,
    stock            BIGINT           DEFAULT NULL,
    last_update_time TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

insert into product (id, price, stock)
VALUES (1, 10, 20);