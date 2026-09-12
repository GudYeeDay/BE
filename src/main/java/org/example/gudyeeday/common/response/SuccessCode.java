package org.example.gudyeeday.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessCode implements BaseCode{

    OK(HttpStatus.OK, "SUCCESS200", "요청에 성공했습니다."),
    CREATED(HttpStatus.CREATED, "SUCCESS201", "리소스가 생성되었습니다."),
    ACCEPTED(HttpStatus.ACCEPTED, "SUCCESS202", "요청을 접수했습니다."),
    NO_CONTENT(HttpStatus.NO_CONTENT, "SUCCESS204", "콘텐츠가 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
