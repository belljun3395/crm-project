create table webhooks
(
    id          bigint auto_increment primary key,
    name        varchar(255)  not null unique,
    url         varchar(1024) not null,
    events      json          not null,
    active      boolean       not null default true,
    created_at  datetime(6)   not null default current_timestamp(6)
);
