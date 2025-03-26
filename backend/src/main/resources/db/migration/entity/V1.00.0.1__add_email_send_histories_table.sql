create table email_send_histories
(
    id               bigint auto_increment
        primary key,
    created_at       datetime(6)  null,
    email_body       varchar(255) null,
    email_message_id varchar(255) null,
    send_status      varchar(255) null,
    updated_at       datetime(6)  null,
    user_email       varchar(255) null,
    user_id varchar(255) null
);
