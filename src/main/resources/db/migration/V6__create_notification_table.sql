CREATE TABLE notification (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    recipient_user_id   UUID         NOT NULL,
    type                VARCHAR(50)  NOT NULL,
    title               VARCHAR(200) NOT NULL,
    body                VARCHAR(1000),
    target_type         VARCHAR(30),
    target_id           VARCHAR(100),
    read                BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_notification PRIMARY KEY (id),
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_user_id)
        REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE INDEX idx_notification_recipient_read_created
    ON notification (recipient_user_id, read, created_at DESC);

CREATE INDEX idx_notification_recipient_unread_count
    ON notification (recipient_user_id) WHERE read = FALSE;
