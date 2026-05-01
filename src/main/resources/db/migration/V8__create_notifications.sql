CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR NOT NULL,
    message TEXT,
    channel VARCHAR NOT NULL CHECK (channel IN ('EMAIL', 'PUSH', 'IN_APP')),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    related_entity_type VARCHAR,
    related_entity_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE TABLE notification_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR NOT NULL,
    aggregate_id VARCHAR NOT NULL,
    event_type VARCHAR NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR NOT NULL CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')) DEFAULT 'PENDING',
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read) WHERE is_read = FALSE;
CREATE INDEX idx_notification_outbox_status ON notification_outbox(status);