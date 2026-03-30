create table campaigns
(
    id              bigserial primary key,
    created_at      timestamp(6) null,
    name            varchar(255) null,
    properties      jsonb        not null
);

