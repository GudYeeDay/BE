package org.example.gudyeeday.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "result","timestamp"})
public class ApiResponse<T> {

    @JsonProperty("isSuccess")
    private final Boolean isSuccess;

    private final String code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T result;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime timestamp;

    // 요청 성공 - 기본 (200 OK)
    public static <T> ApiResponse<T> onSuccess(T result) {
        return ApiResponse.<T>builder()
                .isSuccess(true)
                .code(SuccessCode.OK.getCode())
                .message(SuccessCode.OK.getMessage())
                .result(result)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 리소스 생성 (201 CREATED)
    public static <T> ApiResponse<T> created(T result) {
        return ApiResponse.<T>builder()
                .isSuccess(true)
                .code(SuccessCode.CREATED.getCode())
                .message(SuccessCode.CREATED.getMessage())
                .result(result)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 요청 실패 - 기본 메시지
    public static <T> ApiResponse<T> onFailure(BaseCode errorCode, T result) {
        return ApiResponse.<T>builder()
                .isSuccess(false)
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .result(result)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 요청 실패 - 커스텀 메시지
    public static <T> ApiResponse<T> onFailure(BaseCode errorCode, String customMessage, T result) {
        return ApiResponse.<T>builder()
                .isSuccess(false)
                .code(errorCode.getCode())
                .message(customMessage)
                .result(result)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
