create table campaigns
(
    id              bigint auto_increment
        primary key,
    created_at      datetime(6)  null,
    name            varchar(255) null,
    properties      json         not null
);


