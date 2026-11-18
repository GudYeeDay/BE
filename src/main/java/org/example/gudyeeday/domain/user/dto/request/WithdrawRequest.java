package org.example.gudyeeday.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record WithdrawRequest(
        @Schema(description = "애플 연동 계정만 필요. 탈퇴 직전 앱에서 애플 로그인을 다시 해서 받은 authorizationCode (5분 안에 1회만 사용 가능)")
        String appleAuthorizationCode
) {
}