package org.example.gudyeeday.domain.user.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.gudyeeday.common.response.BaseCode;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseCode {


    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH4001","비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "AUTH4002", "이메일 인증이 완료되지 않았습니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "AUTH4003", "인증번호가 올바르지 않습니다."),
    EXPIRED_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "AUTH4004", "인증번호가 만료되었습니다. 다시 요청해주세요."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH4010", "이메일 또는 비밀번호가 일치하지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH4011", "유효하지 않은 Refresh Token입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH4012", "인증이 필요합니다."),
    INVALID_SOCIAL_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH4013", "유효하지 않은 소셜 로그인 토큰입니다."),
    ALREADY_LINKED_GOOGLE_ACCOUNT(HttpStatus.CONFLICT,"AUTH4014", "이미 다른 구글 계정과 연동된 이메일입니다."),
    SOCIAL_LOGIN_REQUIRED(HttpStatus.BAD_REQUEST, "AUTH4015", "구글 로그인으로 가입된 계정입니다. 구글 로그인을 이용해주세요."),

    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH4030", "접근 권한이 없습니다."),

    TOO_MANY_VERIFICATION_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "AUTH4291", "인증번호 발송 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),

    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "MEMBER4091", "이미 가입된 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER4041", "존재하지 않는 사용자입니다.");;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}
