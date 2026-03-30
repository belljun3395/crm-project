create table campaign_events
(
    id           bigserial primary key,
    campaign_id  bigint      not null,
    event_id     bigint      not null,
    created_at   timestamp(6) not null default CURRENT_TIMESTAMP(6)
);
