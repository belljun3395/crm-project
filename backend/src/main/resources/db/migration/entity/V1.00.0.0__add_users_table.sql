create table users
(
    id              bigserial primary key,
    created_at      timestamp(6) null,
    external_id     varchar(255) null,
    updated_at      timestamp(6) null,
    user_attributes jsonb        null
);

