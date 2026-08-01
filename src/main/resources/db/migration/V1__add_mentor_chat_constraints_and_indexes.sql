DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_mentor_chat_sessions_analysis_id'
          AND conrelid = 'mentor_chat_sessions'::regclass
    ) THEN
        ALTER TABLE mentor_chat_sessions
            ADD CONSTRAINT uk_mentor_chat_sessions_analysis_id UNIQUE (analysis_id);
    END IF;
END
$$;

DROP INDEX IF EXISTS idx_mentor_chat_sessions_analysis_id;

CREATE INDEX IF NOT EXISTS idx_mentor_message_session_created_id
    ON mentor_message (mentor_chat_session_id, created_at, mentor_message_id);
