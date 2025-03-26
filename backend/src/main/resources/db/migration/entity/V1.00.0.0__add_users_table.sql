create table users
(
    id              bigint auto_increment
        primary key,
    created_at      datetime(6)  null,
    external_id     varchar(255) null,
    updated_at      datetime(6)  null,
    user_attributes json         null
);


