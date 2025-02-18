create table scheduled_events
(
    id              bigint auto_increment
        primary key,
    completed       bit          null,
    event_class     varchar(255) null,
    event_id        varchar(255) null,
    event_payload   varchar(255) null,
    is_not_consumed bit          null,
    canceled        bit          null,
    scheduled_at    varchar(255) null
);