ALTER TABLE analysis
ALTER COLUMN processing_started_at TYPE TIMESTAMP WITH TIME ZONE
USING processing_started_at AT TIME ZONE 'Asia/Seoul';

ALTER TABLE analysis
ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE
USING created_at AT TIME ZONE 'Asia/Seoul';