DROP TABLE IF EXISTS account;
CREATE TABLE account
(
    id               SERIAL PRIMARY KEY,
    balance          DOUBLE PRECISION DEFAULT NULL,
    last_update_time TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

insert into account (id, balance)
VALUES (1, 50);