create table email_templates
(
    id            bigserial primary key,
    body          text         null,
    created_at    timestamp(6) null,
    subject       varchar(255) null,
    template_name varchar(255) null,
    version       real         null,
    variables     varchar(255) null
);
