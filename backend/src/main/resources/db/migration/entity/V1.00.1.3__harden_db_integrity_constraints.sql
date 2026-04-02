UPDATE users
SET external_id = 'legacy-user-' || id
WHERE external_id IS NULL OR btrim(external_id) = '';

WITH duplicated_users AS (
    SELECT id,
           row_number() OVER (PARTITION BY external_id ORDER BY id) AS rn
    FROM users
)
UPDATE users u
SET external_id = u.external_id || '-dup-' || u.id
FROM duplicated_users d
WHERE u.id = d.id
  AND d.rn > 1;

UPDATE users
SET user_attributes = jsonb_build_object('email', 'unknown+' || id || '@legacy.local')
WHERE user_attributes IS NULL;

ALTER TABLE users
    ALTER COLUMN external_id SET NOT NULL,
    ALTER COLUMN user_attributes SET NOT NULL;

CREATE UNIQUE INDEX uk_users_external_id ON users (external_id);

UPDATE campaigns
SET name = 'legacy-campaign-' || id
WHERE name IS NULL OR btrim(name) = '';

WITH duplicated_campaigns AS (
    SELECT id,
           row_number() OVER (PARTITION BY name ORDER BY id) AS rn
    FROM campaigns
)
UPDATE campaigns c
SET name = c.name || '-dup-' || c.id
FROM duplicated_campaigns d
WHERE c.id = d.id
  AND d.rn > 1;

ALTER TABLE campaigns
    ALTER COLUMN name SET NOT NULL;

CREATE UNIQUE INDEX uk_campaigns_name ON campaigns (name);

UPDATE email_templates
SET template_name = 'legacy-template-' || id
WHERE template_name IS NULL OR btrim(template_name) = '';

WITH duplicated_templates AS (
    SELECT id,
           row_number() OVER (PARTITION BY template_name ORDER BY id) AS rn
    FROM email_templates
)
UPDATE email_templates t
SET template_name = t.template_name || '-dup-' || t.id
FROM duplicated_templates d
WHERE t.id = d.id
  AND d.rn > 1;

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
    ALTER COLUMN template_name SET NOT NULL,
    ALTER COLUMN subject SET NOT NULL,
    ALTER COLUMN body SET NOT NULL,
    ALTER COLUMN variables SET NOT NULL,
    ALTER COLUMN version SET NOT NULL;

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

WITH duplicated_template_histories AS (
    SELECT id,
           row_number() OVER (PARTITION BY template_id, version ORDER BY id) AS rn
    FROM email_template_histories
)
DELETE FROM email_template_histories h
USING duplicated_template_histories d
WHERE h.id = d.id
  AND d.rn > 1;

ALTER TABLE email_template_histories
    ALTER COLUMN template_id SET NOT NULL,
    ALTER COLUMN subject SET NOT NULL,
    ALTER COLUMN body SET NOT NULL,
    ALTER COLUMN variables SET NOT NULL,
    ALTER COLUMN version SET NOT NULL;

CREATE UNIQUE INDEX uk_email_template_histories_template_version
    ON email_template_histories (template_id, version);

DELETE FROM events
WHERE user_id IS NULL;

ALTER TABLE events
    ALTER COLUMN user_id SET NOT NULL;

CREATE INDEX idx_events_name_created_at ON events (name, created_at);
CREATE INDEX idx_events_user_id_created_at ON events (user_id, created_at);

WITH duplicated_campaign_events AS (
    SELECT id,
           row_number() OVER (PARTITION BY campaign_id, event_id ORDER BY id) AS rn
    FROM campaign_events
)
DELETE FROM campaign_events ce
USING duplicated_campaign_events d
WHERE ce.id = d.id
  AND d.rn > 1;

CREATE UNIQUE INDEX uk_campaign_events_campaign_event
    ON campaign_events (campaign_id, event_id);
CREATE INDEX idx_campaign_events_event_id ON campaign_events (event_id);

DELETE FROM email_send_histories
WHERE user_id IS NULL;

UPDATE email_send_histories
SET user_email = 'unknown+' || id || '@legacy.local'
WHERE user_email IS NULL OR btrim(user_email) = '';

UPDATE email_send_histories
SET email_message_id = 'legacy-message-' || id
WHERE email_message_id IS NULL OR btrim(email_message_id) = '';

UPDATE email_send_histories
SET email_body = ''
WHERE email_body IS NULL;

DELETE FROM email_send_histories
WHERE send_status IS NULL OR btrim(send_status) = '';

UPDATE email_send_histories
SET created_at = CURRENT_TIMESTAMP(6)
WHERE created_at IS NULL;

ALTER TABLE email_send_histories
    ALTER COLUMN user_id SET NOT NULL,
    ALTER COLUMN user_email SET NOT NULL,
    ALTER COLUMN email_message_id SET NOT NULL,
    ALTER COLUMN email_body SET NOT NULL,
    ALTER COLUMN send_status SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL;

CREATE INDEX idx_email_send_histories_created_at ON email_send_histories (created_at);
CREATE INDEX idx_email_send_histories_message_id ON email_send_histories (email_message_id);

UPDATE scheduled_events
SET completed = FALSE
WHERE completed IS NULL;

UPDATE scheduled_events
SET is_not_consumed = FALSE
WHERE is_not_consumed IS NULL;

UPDATE scheduled_events
SET canceled = FALSE
WHERE canceled IS NULL;

UPDATE scheduled_events
SET event_class = 'UNKNOWN_EVENT'
WHERE event_class IS NULL OR btrim(event_class) = '';

UPDATE scheduled_events
SET event_id = 'legacy-event-id-' || id
WHERE event_id IS NULL OR btrim(event_id) = '';

UPDATE scheduled_events
SET event_payload = '{}'
WHERE event_payload IS NULL;

UPDATE scheduled_events
SET scheduled_at = 'APP'
WHERE scheduled_at IS NULL OR btrim(scheduled_at) = '';

UPDATE scheduled_events
SET created_at = CURRENT_TIMESTAMP(6)
WHERE created_at IS NULL;

ALTER TABLE scheduled_events
    ALTER COLUMN completed SET NOT NULL,
    ALTER COLUMN is_not_consumed SET NOT NULL,
    ALTER COLUMN canceled SET NOT NULL,
    ALTER COLUMN event_class SET NOT NULL,
    ALTER COLUMN event_id SET NOT NULL,
    ALTER COLUMN event_payload SET NOT NULL,
    ALTER COLUMN scheduled_at SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL;

CREATE INDEX idx_scheduled_events_event_id_completed ON scheduled_events (event_id, completed);
CREATE INDEX idx_scheduled_events_event_class_completed ON scheduled_events (event_class, completed);
