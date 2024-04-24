create table if not exists products
(
    id          serial not null primary key,
    name        text   not null,
    price       float  not null default 0,
    sku         text   not null,
    description text   not null

);