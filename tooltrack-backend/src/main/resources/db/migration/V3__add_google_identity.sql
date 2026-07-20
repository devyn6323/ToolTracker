ALTER TABLE app_users ADD COLUMN google_subject VARCHAR(255);
ALTER TABLE app_users ADD COLUMN password_login_enabled BOOLEAN NOT NULL DEFAULT TRUE;
CREATE UNIQUE INDEX uk_user_google_subject ON app_users(google_subject);
