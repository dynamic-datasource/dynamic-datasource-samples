create table orders (
    id bigint auto_increment primary key,
    user_id bigint not null,
    amount decimal(12,2) not null
);
