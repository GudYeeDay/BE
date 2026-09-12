package org.example.gudyeeday.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode implements BaseCode {

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400_1", "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404_1", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON405_1", "허용되지 않은 요청 방식입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500_1", "예기치 않은 서버 에러가 발생했습니다."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "COMMON409_1","중복된 데이터가 이미 존재합니다."),
    INVALID_DATA_RELATION(HttpStatus.BAD_REQUEST, "COMMON400_2","연관된 데이터가 존재하지 않거나 참조할 수 없습니다."),
    NOT_NULL_VIOLATION(HttpStatus.BAD_REQUEST, "COMMON400_3","필수 데이터 누락으로 처리할 수 없습니다.");
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}
