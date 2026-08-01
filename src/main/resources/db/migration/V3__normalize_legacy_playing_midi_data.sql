DO
$$
DECLARE
    playing_row RECORD;
    midi_event JSONB;
    event_ordinality BIGINT;
    timestamp_value JSONB;
    numeric_value NUMERIC;
BEGIN
    FOR playing_row IN
        SELECT playing_id, midi_data
        FROM playing
        WHERE midi_data IS NOT NULL
    LOOP
        IF jsonb_typeof(playing_row.midi_data) IS DISTINCT FROM 'array' THEN
            RAISE EXCEPTION 'playing %.midi_data must be a JSON array', playing_row.playing_id;
        END IF;

        FOR midi_event, event_ordinality IN
            SELECT value, ordinality
            FROM jsonb_array_elements(playing_row.midi_data) WITH ORDINALITY
        LOOP
            IF jsonb_typeof(midi_event) IS DISTINCT FROM 'object' THEN
                RAISE EXCEPTION 'playing %.midi_data[%] must be a JSON object',
                    playing_row.playing_id, event_ordinality - 1;
            END IF;

            IF jsonb_typeof(midi_event -> 'type') IS DISTINCT FROM 'string'
                OR midi_event ->> 'type' NOT IN ('NOTE_ON', 'NOTE_OFF') THEN
                RAISE EXCEPTION 'playing %.midi_data[%] has an invalid type',
                    playing_row.playing_id, event_ordinality - 1;
            END IF;

            IF jsonb_typeof(midi_event -> 'pitch') IS DISTINCT FROM 'number' THEN
                RAISE EXCEPTION 'playing %.midi_data[%] has an invalid pitch',
                    playing_row.playing_id, event_ordinality - 1;
            END IF;
            numeric_value := (midi_event ->> 'pitch')::NUMERIC;
            IF numeric_value <> trunc(numeric_value) OR numeric_value < 0 OR numeric_value > 127 THEN
                RAISE EXCEPTION 'playing %.midi_data[%] has an invalid pitch',
                    playing_row.playing_id, event_ordinality - 1;
            END IF;

            IF jsonb_typeof(midi_event -> 'velocity') IS DISTINCT FROM 'number' THEN
                RAISE EXCEPTION 'playing %.midi_data[%] has an invalid velocity',
                    playing_row.playing_id, event_ordinality - 1;
            END IF;
            numeric_value := (midi_event ->> 'velocity')::NUMERIC;
            IF numeric_value <> trunc(numeric_value) OR numeric_value < 0 OR numeric_value > 127 THEN
                RAISE EXCEPTION 'playing %.midi_data[%] has an invalid velocity',
                    playing_row.playing_id, event_ordinality - 1;
            END IF;

            timestamp_value := COALESCE(
                NULLIF(midi_event -> 'timestamp_ms', 'null'::JSONB),
                NULLIF(midi_event -> 'timestampMs', 'null'::JSONB)
            );
            IF jsonb_typeof(timestamp_value) IS DISTINCT FROM 'number' THEN
                RAISE EXCEPTION 'playing %.midi_data[%] has an invalid timestamp',
                    playing_row.playing_id, event_ordinality - 1;
            END IF;
            numeric_value := (timestamp_value #>> '{}')::NUMERIC;
            IF numeric_value <> trunc(numeric_value) OR numeric_value < 0 OR numeric_value > 9223372036854775807 THEN
                RAISE EXCEPTION 'playing %.midi_data[%] has an invalid timestamp',
                    playing_row.playing_id, event_ordinality - 1;
            END IF;

            IF NULLIF(midi_event -> 'sequence', 'null'::JSONB) IS NOT NULL THEN
                IF jsonb_typeof(midi_event -> 'sequence') IS DISTINCT FROM 'number' THEN
                    RAISE EXCEPTION 'playing %.midi_data[%] has an invalid sequence',
                        playing_row.playing_id, event_ordinality - 1;
                END IF;
                numeric_value := (midi_event ->> 'sequence')::NUMERIC;
                IF numeric_value <> trunc(numeric_value) OR numeric_value < 0 OR numeric_value > 2147483647 THEN
                    RAISE EXCEPTION 'playing %.midi_data[%] has an invalid sequence',
                        playing_row.playing_id, event_ordinality - 1;
                END IF;
            END IF;
        END LOOP;
    END LOOP;
END
$$;

UPDATE playing AS p
SET midi_data = CASE
    WHEN p.midi_data IS NULL THEN '[]'::JSONB
    ELSE COALESCE(
        (
            SELECT jsonb_agg(
                (midi_event - 'timestampMs' - 'sequence')
                    || jsonb_build_object(
                        'sequence', COALESCE(
                            NULLIF(midi_event -> 'sequence', 'null'::JSONB),
                            to_jsonb((event_ordinality - 1)::INTEGER)
                        ),
                        'timestamp_ms', COALESCE(
                            NULLIF(midi_event -> 'timestamp_ms', 'null'::JSONB),
                            NULLIF(midi_event -> 'timestampMs', 'null'::JSONB)
                        )
                    )
                ORDER BY event_ordinality
            )
            FROM jsonb_array_elements(p.midi_data) WITH ORDINALITY
                AS events(midi_event, event_ordinality)
        ),
        '[]'::JSONB
    )
END
WHERE p.midi_data IS NULL
   OR EXISTS (
        SELECT 1
        FROM jsonb_array_elements(p.midi_data) AS events(midi_event)
        WHERE NULLIF(midi_event -> 'sequence', 'null'::JSONB) IS NULL
           OR NULLIF(midi_event -> 'timestamp_ms', 'null'::JSONB) IS NULL
           OR midi_event ? 'timestampMs'
    );
