UPDATE users
SET external_id = CONCAT('legacy-user-', id)
WHERE external_id IS NULL OR TRIM(external_id) = '';

UPDATE users u
    JOIN users u_prev
      ON u.external_id = u_prev.external_id
     AND u.id > u_prev.id
SET u.external_id = CONCAT(u.external_id, '-dup-', u.id);

UPDATE users
SET user_attributes = JSON_OBJECT('email', CONCAT('unknown+', id, '@legacy.local'))
WHERE user_attributes IS NULL;

ALTER TABLE users
    MODIFY COLUMN external_id VARCHAR(255) NOT NULL,
    MODIFY COLUMN user_attributes JSON NOT NULL;

CREATE UNIQUE INDEX uk_users_external_id ON users (external_id);

UPDATE campaigns
SET name = CONCAT('legacy-campaign-', id)
WHERE name IS NULL OR TRIM(name) = '';

UPDATE campaigns c
    JOIN campaigns c_prev
      ON c.name = c_prev.name
     AND c.id > c_prev.id
SET c.name = CONCAT(c.name, '-dup-', c.id);

ALTER TABLE campaigns
    MODIFY COLUMN name VARCHAR(255) NOT NULL;

CREATE UNIQUE INDEX uk_campaigns_name ON campaigns (name);

UPDATE email_templates
SET template_name = CONCAT('legacy-template-', id)
WHERE template_name IS NULL OR TRIM(template_name) = '';

UPDATE email_templates t
    JOIN email_templates t_prev
      ON t.template_name = t_prev.template_name
     AND t.id > t_prev.id
SET t.template_name = CONCAT(t.template_name, '-dup-', t.id);

UPDATE email_templates
SET subject = ''
WHERE subject IS NULL;

UPDATE email_templates
SET body = ''
WHERE body IS NULL;

UPDATE email_templates
SET variables = ''
WHERE variables IS NULL;

UPDATE email_templates
SET version = 1.0
WHERE version IS NULL;

ALTER TABLE email_templates
    MODIFY COLUMN template_name VARCHAR(255) NOT NULL,
    MODIFY COLUMN subject VARCHAR(255) NOT NULL,
    MODIFY COLUMN body LONGTEXT NOT NULL,
    MODIFY COLUMN variables VARCHAR(255) NOT NULL,
    MODIFY COLUMN version FLOAT NOT NULL;

CREATE UNIQUE INDEX uk_email_templates_template_name ON email_templates (template_name);

DELETE FROM email_template_histories
WHERE template_id IS NULL;

UPDATE email_template_histories
SET subject = ''
WHERE subject IS NULL;

UPDATE email_template_histories
SET body = ''
WHERE body IS NULL;

UPDATE email_template_histories
SET variables = ''
WHERE variables IS NULL;

UPDATE email_template_histories
SET version = 1.0
WHERE version IS NULL;

DELETE h2
FROM email_template_histories h1
         JOIN email_template_histories h2
              ON h1.template_id = h2.template_id
                  AND h1.version = h2.version
                  AND h1.id < h2.id;

ALTER TABLE email_template_histories
    MODIFY COLUMN template_id BIGINT NOT NULL,
    MODIFY COLUMN subject VARCHAR(255) NOT NULL,
    MODIFY COLUMN body LONGTEXT NOT NULL,
    MODIFY COLUMN variables VARCHAR(255) NOT NULL,
    MODIFY COLUMN version FLOAT NOT NULL;

CREATE UNIQUE INDEX uk_email_template_histories_template_version
    ON email_template_histories (template_id, version);

DELETE FROM events
WHERE user_id IS NULL;

ALTER TABLE events
    MODIFY COLUMN user_id BIGINT NOT NULL;

CREATE INDEX idx_events_name_created_at ON events (name, created_at);
CREATE INDEX idx_events_user_id_created_at ON events (user_id, created_at);

DELETE ce2
FROM campaign_events ce1
         JOIN campaign_events ce2
              ON ce1.campaign_id = ce2.campaign_id
                  AND ce1.event_id = ce2.event_id
                  AND ce1.id < ce2.id;

CREATE UNIQUE INDEX uk_campaign_events_campaign_event
    ON campaign_events (campaign_id, event_id);
CREATE INDEX idx_campaign_events_event_id ON campaign_events (event_id);

DELETE FROM email_send_histories
WHERE user_id IS NULL;

UPDATE email_send_histories
SET user_email = CONCAT('unknown+', id, '@legacy.local')
WHERE user_email IS NULL OR TRIM(user_email) = '';

UPDATE email_send_histories
SET email_message_id = CONCAT('legacy-message-', id)
WHERE email_message_id IS NULL OR TRIM(email_message_id) = '';

UPDATE email_send_histories
SET email_body = ''
WHERE email_body IS NULL;

DELETE FROM email_send_histories
WHERE send_status IS NULL OR TRIM(send_status) = '';

UPDATE email_send_histories
SET created_at = CURRENT_TIMESTAMP(6)
WHERE created_at IS NULL;

ALTER TABLE email_send_histories
    MODIFY COLUMN user_id BIGINT NOT NULL,
    MODIFY COLUMN user_email VARCHAR(255) NOT NULL,
    MODIFY COLUMN email_message_id VARCHAR(255) NOT NULL,
    MODIFY COLUMN email_body VARCHAR(255) NOT NULL,
    MODIFY COLUMN send_status VARCHAR(255) NOT NULL,
    MODIFY COLUMN created_at DATETIME(6) NOT NULL;

CREATE INDEX idx_email_send_histories_created_at ON email_send_histories (created_at);
CREATE INDEX idx_email_send_histories_message_id ON email_send_histories (email_message_id);

UPDATE scheduled_events
SET completed = b'0'
WHERE completed IS NULL;

UPDATE scheduled_events
SET is_not_consumed = b'0'
WHERE is_not_consumed IS NULL;

UPDATE scheduled_events
SET canceled = b'0'
WHERE canceled IS NULL;

UPDATE scheduled_events
SET event_class = 'UNKNOWN_EVENT'
WHERE event_class IS NULL OR TRIM(event_class) = '';

UPDATE scheduled_events
SET event_id = CONCAT('legacy-event-id-', id)
WHERE event_id IS NULL OR TRIM(event_id) = '';

UPDATE scheduled_events
SET event_payload = '{}'
WHERE event_payload IS NULL;

UPDATE scheduled_events
SET scheduled_at = 'APP'
WHERE scheduled_at IS NULL OR TRIM(scheduled_at) = '';

UPDATE scheduled_events
SET created_at = CURRENT_TIMESTAMP(6)
WHERE created_at IS NULL;

ALTER TABLE scheduled_events
    MODIFY COLUMN completed BIT NOT NULL,
    MODIFY COLUMN is_not_consumed BIT NOT NULL,
    MODIFY COLUMN canceled BIT NOT NULL,
    MODIFY COLUMN event_class VARCHAR(255) NOT NULL,
    MODIFY COLUMN event_id VARCHAR(255) NOT NULL,
    MODIFY COLUMN event_payload VARCHAR(255) NOT NULL,
    MODIFY COLUMN scheduled_at VARCHAR(255) NOT NULL,
    MODIFY COLUMN created_at DATETIME(6) NOT NULL;

CREATE INDEX idx_scheduled_events_event_id_completed ON scheduled_events (event_id, completed);
CREATE INDEX idx_scheduled_events_event_class_completed ON scheduled_events (event_class, completed);
