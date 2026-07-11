package  com.mr.domain.user.entity.enums;

import com.mr.global.apipayload.code.BaseCode;
import org.springframework.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserUsageErrorStatus implements BaseCode {

    // 잔여 횟수 소진 에러
    USAGE_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "USAGE_403_01", "일일 잔여 분석 횟수를 모두 소진하였습니다."),

    // 유효하지 않은 카운트 수정 요청
    INVALID_USAGE_COUNT(HttpStatus.BAD_REQUEST, "USAGE_400_01", "잔여 횟수는 최대 제한 횟수를 초과할 수 없습니다."),

    // 해당 날짜의 이용 제한 데이터가 존재하지 않을 때
    USAGE_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "USAGE_404_01", "해당 날짜의 이용 제한 기록을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}