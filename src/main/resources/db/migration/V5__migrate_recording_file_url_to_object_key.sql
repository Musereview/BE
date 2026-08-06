BEGIN;

-- 1. 컬럼명 변경
ALTER TABLE playing RENAME COLUMN recording_file_url TO recording_object_key;

-- 2. URL에서 S3 Object Key만 추출 (도메인/프로토콜 제거 + 쿼리 스트링 제거)
UPDATE playing
SET recording_object_key = regexp_replace(
        recording_object_key,
        '^(https?://[^/]+/)?([^?#]*).*$',
        '\2'
                           )
WHERE recording_object_key IS NOT NULL
  AND recording_object_key <> '';

-- 3. 검증: 쿼리 결과는 반드시 0건이어야 함
SELECT playing_id, recording_object_key
FROM playing
WHERE recording_object_key LIKE '%?%'
   OR recording_object_key LIKE 'http%'
   OR recording_object_key LIKE '%X-Amz%';

-- 결과가 0건이면 COMMIT, 에러나 잔여 데이터가 있으면 ROLLBACK 실행
COMMIT;