package capstone2.team3.realplan.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "요청 값이 올바르지 않습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다."),
    AI_SERVICE_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "AI_SERVICE_UNAVAILABLE", "AI 서비스 호출에 실패했습니다."),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_TOKEN", "만료된 토큰입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "INVALID_PASSWORD", "비밀번호가 올바르지 않습니다."),

    // Folder
    FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLDER_NOT_FOUND", "폴더를 찾을 수 없습니다."),
    DEFAULT_FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "DEFAULT_FOLDER_NOT_FOUND", "기본 폴더를 찾을 수 없습니다."),
    DUPLICATE_FOLDER_NAME(HttpStatus.CONFLICT, "DUPLICATE_FOLDER_NAME", "이미 존재하는 폴더 이름입니다."),
    DEFAULT_FOLDER_CANNOT_BE_DELETED(HttpStatus.BAD_REQUEST, "DEFAULT_FOLDER_CANNOT_BE_DELETED", "기본 폴더는 삭제할 수 없습니다."),

    // Task
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "태스크를 찾을 수 없습니다."),
    TASK_TYPE_NOT_FOUND(HttpStatus.NOT_FOUND, "TASK_TYPE_NOT_FOUND", "태스크 유형을 찾을 수 없습니다."),

    // DailyPlan
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "플랜을 찾을 수 없습니다."),
    DUPLICATE_DAILY_PLAN(HttpStatus.CONFLICT, "DUPLICATE_DAILY_PLAN", "해당 날짜의 플랜이 이미 존재합니다."),

    // FocusSession
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "세션을 찾을 수 없습니다."),
    SESSION_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "SESSION_ALREADY_ENDED", "이미 종료된 세션입니다."),
    SESSION_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "SESSION_NOT_ACTIVE", "활성 상태가 아닌 세션입니다."),
    SESSION_ALREADY_ACTIVE(HttpStatus.BAD_REQUEST, "SESSION_ALREADY_ACTIVE", "이미 진행 중인 세션이 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
