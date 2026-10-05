package org.example.gudyeeday.domain.home.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.gudyeeday.common.response.BaseCode;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum HomeErrorCode implements BaseCode {

    WEEKLY_PHOTO_EMPTY(HttpStatus.BAD_REQUEST, "HOME4001", "업로드할 사진이 없습니다."),
    WEEKLY_PHOTO_NOT_IMAGE(HttpStatus.BAD_REQUEST, "HOME4002", "이미지 파일만 업로드할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
