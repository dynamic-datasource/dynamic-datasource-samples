DROP TABLE IF EXISTS `t_user`;
DROP TABLE IF EXISTS `t_order0`;
DROP TABLE IF EXISTS `t_order1`;

CREATE TABLE `t_user`
(
    `id`   BIGINT PRIMARY KEY,
    `name` VARCHAR(255),
    `age`  BIGINT
);

CREATE TABLE `t_order0`
(
    `order_id` BIGINT PRIMARY KEY,
    `name`     VARCHAR(255),
    `user_id`  BIGINT
);

CREATE TABLE `t_order1`
(
    `order_id` BIGINT PRIMARY KEY,
    `name`     VARCHAR(255),
    `user_id`  BIGINT
);
