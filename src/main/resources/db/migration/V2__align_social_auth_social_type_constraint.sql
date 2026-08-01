DO $$
DECLARE
    invalid_values text;
BEGIN
    SELECT string_agg(DISTINCT social_type, ', ' ORDER BY social_type)
    INTO invalid_values
    FROM social_auth
    WHERE UPPER(BTRIM(social_type)) NOT IN ('KAKAO', 'GOOGLE');

    IF invalid_values IS NOT NULL THEN
        RAISE EXCEPTION 'Unsupported social_auth.social_type values: %', invalid_values;
    END IF;
END
$$;

ALTER TABLE social_auth
    DROP CONSTRAINT IF EXISTS social_auth_social_type_check;

UPDATE social_auth
SET social_type = UPPER(BTRIM(social_type))
WHERE social_type <> UPPER(BTRIM(social_type));

ALTER TABLE social_auth
    ADD CONSTRAINT social_auth_social_type_check
        CHECK (social_type IN ('KAKAO', 'GOOGLE'));
