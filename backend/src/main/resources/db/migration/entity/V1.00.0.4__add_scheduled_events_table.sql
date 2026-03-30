create table scheduled_events
(
    id              bigserial primary key,
    completed       boolean      null,
    event_class     varchar(255) null,
    event_id        varchar(255) null,
    event_payload   text         null,
    is_not_consumed boolean      null,
    canceled        boolean      null,
    scheduled_at    varchar(255) null
);
