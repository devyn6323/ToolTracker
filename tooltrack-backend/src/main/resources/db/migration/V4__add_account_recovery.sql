ALTER TABLE app_users ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE app_users ADD COLUMN session_version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE app_users ADD COLUMN password_reset_code_hash VARCHAR(64);
ALTER TABLE app_users ADD COLUMN password_reset_expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE app_users ADD COLUMN password_reset_attempts INTEGER NOT NULL DEFAULT 0;
