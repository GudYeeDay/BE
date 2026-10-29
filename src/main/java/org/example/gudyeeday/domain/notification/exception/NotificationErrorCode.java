package org.example.gudyeeday.domain.notification.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.gudyeeday.common.response.BaseCode;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements BaseCode {

    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION4041", "존재하지 않는 알림입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
