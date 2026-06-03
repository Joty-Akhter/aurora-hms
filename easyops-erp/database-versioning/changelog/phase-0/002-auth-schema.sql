--liquibase formatted sql

--changeset easyops:017-create-user-sessions-table context:auth
--comment: Create user sessions table for authentication service
CREATE TABLE auth.user_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users.users(id) ON DELETE CASCADE,
    token VARCHAR(1000) UNIQUE NOT NULL,
    refresh_token VARCHAR(1000) UNIQUE,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_activity_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_user_sessions_user_id ON auth.user_sessions(user_id);
CREATE INDEX idx_user_sessions_token ON auth.user_sessions(token);
CREATE INDEX idx_user_sessions_refresh_token ON auth.user_sessions(refresh_token);
CREATE INDEX idx_user_sessions_is_active ON auth.user_sessions(is_active);

--changeset easyops:018-create-password-reset-tokens-table context:auth
--comment: Create password reset tokens table
CREATE TABLE auth.password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users.users(id) ON DELETE CASCADE,
    token VARCHAR(255) UNIQUE NOT NULL,
    is_used BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_password_reset_tokens_user_id ON auth.password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_token ON auth.password_reset_tokens(token);

--changeset easyops:019-create-login-attempts-table context:auth
--comment: Create login attempts table
CREATE TABLE auth.login_attempts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    successful BOOLEAN DEFAULT false,
    failure_reason VARCHAR(255),
    attempted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_login_attempts_username ON auth.login_attempts(username);
CREATE INDEX idx_login_attempts_attempted_at ON auth.login_attempts(attempted_at);

--changeset easyops:020-grant-auth-permissions context:auth
--comment: Grant permissions on auth tables to easyops user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA auth TO easyops;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA auth TO easyops;
