create table email_template_histories
(
    id          bigint auto_increment
        primary key,
    body        longtext     null,
    created_at  datetime(6)  null,
    subject     varchar(255) null,
    template_id bigint       null,
    version     float        null,
    variables   varchar(255) null
);
