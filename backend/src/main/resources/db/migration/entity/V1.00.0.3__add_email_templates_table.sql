create table email_templates
(
    id            bigint auto_increment
        primary key,
    body          longtext     null,
    created_at    datetime(6)  null,
    subject       varchar(255) null,
    template_name varchar(255) null,
    version       float        null,
    variables     varchar(255) null
);
