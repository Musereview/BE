# `CREATE INDEX CONCURRENTLY` 실패 복구

## 목적

`CREATE INDEX CONCURRENTLY`가 중단되면 같은 이름의 인덱스가 남아 있지만 사용할 수 없는 상태가 될 수 있습니다. 이 상태에서 `CREATE INDEX CONCURRENTLY IF NOT EXISTS`를 다시 실행하면 이름이 이미 존재한다는 이유로 생성을 건너뛸 수 있으므로, 이 문서는 `V10__add_analysis_query_indexes.sql`에서 생성하는 인덱스의 상태 확인 및 복구 절차를 설명합니다.

## 대상 인덱스

- `idx_analysis_user_status_completed_at`
- `idx_analysis_pending_created_at_id`
- `idx_analysis_processing_started_at_id`

## 사전 주의사항

- 운영 데이터베이스에 접속했는지와 대상 스키마가 맞는지 먼저 확인합니다.
- 아래 `DROP INDEX CONCURRENTLY`와 `CREATE INDEX CONCURRENTLY`는 명시적 트랜잭션 블록(`BEGIN`/`COMMIT`) 안에서 실행하지 않습니다.
- 유효한 인덱스는 삭제하지 않습니다. 반드시 조회 결과에서 `indisvalid = false`인 대상만 복구합니다.
- 여러 인덱스를 복구할 때는 각 삭제 및 생성 명령을 하나씩 실행하고 결과를 확인합니다.
- 인덱스 생성은 테이블 크기와 부하에 따라 오래 걸리고 추가 I/O를 유발할 수 있으므로, 데이터베이스 지표를 관찰하며 실행합니다.

## 1. 인덱스 상태 확인

다음 쿼리로 대상 인덱스의 존재 여부, 유효성 및 실제 정의를 확인합니다.

```sql
SELECT
    ns.nspname AS schema_name,
    idx.relname AS index_name,
    pi.indisvalid,
    pi.indisready,
    pi.indislive,
    pg_get_indexdef(pi.indexrelid) AS index_definition
FROM pg_index AS pi
JOIN pg_class AS idx ON idx.oid = pi.indexrelid
JOIN pg_namespace AS ns ON ns.oid = idx.relnamespace
WHERE idx.relname IN (
    'idx_analysis_user_status_completed_at',
    'idx_analysis_pending_created_at_id',
    'idx_analysis_processing_started_at_id'
)
ORDER BY idx.relname;
```

판단 기준은 다음과 같습니다.

- 조회되지 않음: 인덱스가 존재하지 않으므로 해당 `CREATE INDEX CONCURRENTLY` 문을 실행합니다.
- `indisvalid = true`: PostgreSQL이 쿼리 계획에 사용할 수 있는 유효한 인덱스입니다. 실제 정의가 V10과 일치하는지도 함께 확인합니다.
- `indisvalid = false`: 실패한 동시 생성의 잔여물일 수 있습니다. 다음 절차로 삭제 후 재생성합니다.

## 2. 유효하지 않은 인덱스 삭제

1단계에서 `indisvalid = false`로 확인된 인덱스에 대해서만 아래 명령 중 해당 명령을 실행합니다. 스키마가 `public`이 아니라면 실제 스키마 이름으로 변경합니다.

아래 형식에서 `<invalid_index_name>`을 1단계에서 유효하지 않다고 확인한 대상 인덱스 이름 하나로 바꿔 실행합니다. 꺾쇠괄호를 포함한 예시를 그대로 실행할 수는 없습니다.

```sql
DROP INDEX CONCURRENTLY IF EXISTS public.<invalid_index_name>;
```

각 명령을 실행한 뒤 1단계 조회를 다시 수행해 해당 인덱스가 제거되었는지 확인합니다.

## 3. 인덱스 재생성

삭제했거나 존재하지 않았던 인덱스만 원본 마이그레이션과 동일한 정의로 하나씩 생성합니다.

```sql
CREATE INDEX CONCURRENTLY idx_analysis_user_status_completed_at
    ON public.analysis (user_id, status, completed_at);

CREATE INDEX CONCURRENTLY idx_analysis_pending_created_at_id
    ON public.analysis (created_at, analysis_id)
    WHERE status = 'PENDING';

CREATE INDEX CONCURRENTLY idx_analysis_processing_started_at_id
    ON public.analysis (processing_started_at, analysis_id)
    WHERE status = 'PROCESSING'
      AND processing_started_at IS NOT NULL;
```

인덱스 이름이 다시 충돌하면 `IF NOT EXISTS`를 추가해 우회하지 말고 1단계 조회로 현재 상태와 정의를 다시 확인합니다.

## 4. 복구 검증

1단계 조회를 다시 실행해 세 인덱스가 모두 조회되고 다음 조건을 만족하는지 확인합니다.

- `indisvalid = true`
- `indisready = true`
- `indislive = true`
- `index_definition`이 `V10__add_analysis_query_indexes.sql`의 정의와 일치

Flyway 실행 자체가 실패한 상태라면 Flyway 스키마 이력 테이블의 V10 상태도 확인합니다. 실패 이력이 남아 후속 마이그레이션을 막는 경우에는 인덱스 복구와 검증을 완료한 뒤, 프로젝트의 배포 절차에 따라 Flyway 이력을 정리하고 마이그레이션을 다시 실행합니다. 인덱스 상태를 확인하지 않은 채 Flyway 이력만 수정하지 않습니다.

## 재발 시 확인 사항

- 배포 또는 데이터베이스 작업 로그에서 세션 종료, statement timeout, 교착 상태, 디스크 부족 등 최초 실패 원인을 확인합니다.
- 동일한 인덱스 생성 작업이 다른 세션에서 실행 중인지 확인합니다.
- 재생성 도중 다시 실패하면 남은 `INVALID` 인덱스를 삭제하기 전에 원인을 먼저 해소합니다.
