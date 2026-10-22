create table account (
    id bigint auto_increment primary key,
    user_id bigint not null unique,
    balance decimal(12,2) not null
);

insert into account (user_id, balance) values (1, 100.00);
