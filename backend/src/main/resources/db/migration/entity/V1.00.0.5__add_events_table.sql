create table events
(
    id              bigserial primary key,
    name            varchar(255) not null,
    external_id     varchar(255) not null,
    properties      jsonb        not null,
    created_at      timestamp(6) null
);
