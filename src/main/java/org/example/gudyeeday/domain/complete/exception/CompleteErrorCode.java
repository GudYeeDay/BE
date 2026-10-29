package org.example.gudyeeday.domain.complete.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.gudyeeday.common.response.BaseCode;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CompleteErrorCode implements BaseCode {


    ALREADY_EXIST_MISSION_RECORD(HttpStatus.CONFLICT, "COMPLETE6001","이미 해당 미션에 기록이 존재합니다."),
    MISSION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "COMPLETE6002","내 미션이 아닙니다."),
    IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "COMPLETE6003","이미지는 필수입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}
