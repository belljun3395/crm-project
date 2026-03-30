ALTER TABLE journeys
    ADD COLUMN lifecycle_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN version INT NOT NULL DEFAULT 1;

UPDATE journeys
SET lifecycle_status = 'PAUSED'
WHERE active = FALSE;

UPDATE journeys
SET version = 1
WHERE version < 1;

CREATE INDEX idx_journeys_lifecycle_status ON journeys (lifecycle_status);
