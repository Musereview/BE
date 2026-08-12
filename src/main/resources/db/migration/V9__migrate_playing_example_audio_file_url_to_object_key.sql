-- 1. 컬럼명 변경
ALTER TABLE playing_example
    RENAME COLUMN audio_file_url TO audio_object_key;

-- 2. 기존 URL 값에서 S3 Object Key만 추출
UPDATE playing_example
SET audio_object_key = regexp_replace(
        audio_object_key,
        '^(https?://[^/]+/)?([^?#]*).*$',
        '\2'
                       )
WHERE audio_object_key IS NOT NULL
  AND btrim(audio_object_key) <> '';

-- 3. URL이나 잘못된 prefix가 남아 있으면 마이그레이션을 중단
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM playing_example
        WHERE audio_object_key IS NULL
           OR btrim(audio_object_key) = ''
           OR audio_object_key LIKE 'http%'
           OR audio_object_key LIKE '%?%'
           OR audio_object_key LIKE '%X-Amz%'
           OR audio_object_key NOT LIKE 'playing_example/%'
    ) THEN
        RAISE EXCEPTION 'playing_example audio_object_key migration validation failed';
    END IF;
END
$$;
