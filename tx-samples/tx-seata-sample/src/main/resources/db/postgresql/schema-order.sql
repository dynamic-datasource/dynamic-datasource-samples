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

DROP TABLE IF EXISTS undo_log;
CREATE TABLE undo_log
(
    id            SERIAL PRIMARY KEY,
    branch_id     BIGINT       NOT NULL,
    xid           VARCHAR(100) NOT NULL,
    context       VARCHAR(128) NOT NULL,
    rollback_info BYTEA        NOT NULL,
    log_status    INT          NOT NULL,
    log_created   TIMESTAMP    NOT NULL,
    log_modified  TIMESTAMP    NOT NULL,
    CONSTRAINT ux_undo_log UNIQUE (xid, branch_id)
);