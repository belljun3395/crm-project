create table webhooks
(
    id          bigserial primary key,
    name        varchar(255)  not null unique,
    url         varchar(1024) not null,
    events      jsonb         not null,
    active      boolean       not null default true,
    created_at  timestamp(6)   not null default current_timestamp(6)
);
