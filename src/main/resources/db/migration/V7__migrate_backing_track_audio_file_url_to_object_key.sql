-- 1. 컬럼명 변경
ALTER TABLE backing_track
    RENAME COLUMN audio_file_url TO audio_object_key;

-- 2. 기존 URL 값이 있다면 S3 Object Key만 추출
UPDATE backing_track
SET audio_object_key = regexp_replace(
        audio_object_key,
        '^(https?://[^/]+/)?([^?#]*).*$',
        '\2'
                       )
WHERE audio_object_key IS NOT NULL
  AND btrim(audio_object_key) <> '';

-- 3. 검증: URL/쿼리스트링이 남아 있으면 예외 발생
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM backing_track
        WHERE audio_object_key IS NOT NULL
          AND (
              audio_object_key LIKE '%?%'
              OR audio_object_key LIKE 'http%'
              OR audio_object_key LIKE '%X-Amz%'
              OR btrim(audio_object_key) = ''
          )
    ) THEN
        RAISE EXCEPTION 'audio_object_key migration validation failed';
END IF;
END
$$;