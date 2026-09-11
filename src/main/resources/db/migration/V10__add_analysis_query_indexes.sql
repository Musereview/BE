-- 배포 중 analysis 쓰기 차단 최소화
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_analysis_user_status_completed_at
    ON analysis (user_id, status, completed_at);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_analysis_pending_created_at_id
    ON analysis (created_at, analysis_id)
    WHERE status = 'PENDING';

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_analysis_processing_started_at_id
    ON analysis (processing_started_at, analysis_id)
    WHERE status = 'PROCESSING'
      AND processing_started_at IS NOT NULL;
