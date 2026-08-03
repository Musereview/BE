ALTER TABLE mentor_chat_sessions
    ADD COLUMN IF NOT EXISTS generation_token VARCHAR(36),
    ADD COLUMN IF NOT EXISTS generation_started_at TIMESTAMP WITHOUT TIME ZONE;
