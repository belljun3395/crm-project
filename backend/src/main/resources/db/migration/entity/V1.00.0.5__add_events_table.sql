create table events
(
    id              bigint auto_increment
        primary key,
    name            varchar(255) not null,
    external_id     varchar(255) not null,
    properties      json         not null,
    created_at      datetime(6)  null
);