DO $$
DECLARE
    invalid_values text;
BEGIN
    SELECT string_agg(DISTINCT status, ', ' ORDER BY status)
    INTO invalid_values
    FROM mentor_chat_sessions
    WHERE status NOT IN ('ACTIVE', 'GENERATING', 'CLOSED', 'DISABLED');

    IF invalid_values IS NOT NULL THEN
        RAISE EXCEPTION 'Unsupported mentor_chat_sessions.status values: %', invalid_values;
    END IF;
END
$$;

ALTER TABLE mentor_chat_sessions
    DROP CONSTRAINT IF EXISTS mentor_chat_sessions_status_check;

ALTER TABLE mentor_chat_sessions
    ADD CONSTRAINT mentor_chat_sessions_status_check
        CHECK (status IN ('ACTIVE', 'GENERATING', 'CLOSED', 'DISABLED'));
