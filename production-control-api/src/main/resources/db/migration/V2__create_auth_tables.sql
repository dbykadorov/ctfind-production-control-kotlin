CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    login VARCHAR(120) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_app_user_login ON app_user (login);

CREATE TABLE app_role (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_app_role_code ON app_role (code);

CREATE TABLE app_user_role (
    user_id UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES app_role (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE auth_audit_event (
    id UUID PRIMARY KEY,
    event_type VARCHAR(60) NOT NULL,
    outcome VARCHAR(80) NOT NULL,
    login VARCHAR(120),
    user_id UUID REFERENCES app_user (id) ON DELETE SET NULL,
    request_ip VARCHAR(80),
    user_agent VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL,
    details TEXT
);

CREATE INDEX ix_auth_audit_event_occurred_at ON auth_audit_event (occurred_at);
CREATE INDEX ix_auth_audit_event_login ON auth_audit_event (login);
