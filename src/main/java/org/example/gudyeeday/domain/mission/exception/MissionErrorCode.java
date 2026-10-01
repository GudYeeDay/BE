package org.example.gudyeeday.domain.mission.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.gudyeeday.common.response.BaseCode;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MissionErrorCode implements BaseCode {

    MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "MISSION4041", "존재하지 않는 미션입니다."),
    IN_PROGRESS_MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "MISSION4042", "진행중인 미션이 없습니다."),
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "MISSION4043", "보관함에 저장되지 않은 미션입니다."),

    MISSION_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "MISSION4091", "이미 진행중인 미션이 있습니다."),
    ALREADY_BOOKMARKED(HttpStatus.CONFLICT, "MISSION4092", "이미 보관함에 저장된 미션입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
