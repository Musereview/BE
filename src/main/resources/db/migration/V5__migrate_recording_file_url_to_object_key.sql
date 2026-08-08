-- 1. 컬럼명 변경
ALTER TABLE playing
    RENAME COLUMN recording_file_url TO recording_object_key;

-- 2. URL에서 S3 Object Key만 추출
UPDATE playing
SET recording_object_key = regexp_replace(
        recording_object_key,
        '^(https?://[^/]+/)?([^?#]*).*$',
        '\2'
   )
WHERE recording_object_key IS NOT NULL
  AND btrim(recording_object_key) <> '';

-- 3. 검증: 실패 시 예외 발생 → Flyway가 트랜잭션 롤백 처리
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM playing
        WHERE recording_object_key IS NOT NULL
          AND (
              recording_object_key LIKE '%?%'
              OR recording_object_key LIKE 'http%'
              OR recording_object_key LIKE '%X-Amz%'
              OR btrim(recording_object_key) = ''
          )
    ) THEN
        RAISE EXCEPTION 'recording_object_key migration validation failed';
    END IF;
END
$$;