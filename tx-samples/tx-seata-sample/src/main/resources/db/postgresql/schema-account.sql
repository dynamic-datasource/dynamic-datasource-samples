DROP TABLE IF EXISTS account;
CREATE TABLE account
(
    id               SERIAL PRIMARY KEY,
    balance          DOUBLE PRECISION DEFAULT NULL,
    last_update_time TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

insert into account (id, balance)
VALUES (1, 50);

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