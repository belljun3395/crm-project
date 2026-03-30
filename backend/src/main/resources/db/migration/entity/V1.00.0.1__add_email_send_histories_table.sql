create table email_send_histories
(
    id               bigserial primary key,
    created_at       timestamp(6)  null,
    email_body       varchar(255) null,
    email_message_id varchar(255) null,
    send_status      varchar(255) null,
    updated_at       timestamp(6)  null,
    user_email       varchar(255) null,
    user_id          bigint       null
);
