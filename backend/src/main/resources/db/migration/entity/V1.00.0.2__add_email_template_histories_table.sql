create table email_template_histories
(
    id          bigserial primary key,
    body        text         null,
    created_at  timestamp(6) null,
    subject     varchar(255) null,
    template_id bigint       null,
    version     real         null,
    variables   varchar(255) null
);
