create table campaign_events
(
    id           bigint auto_increment primary key,
    campaign_id  bigint      not null,
    event_id     bigint      not null,
    created_at   datetime(6) not null default CURRENT_TIMESTAMP(6)
);