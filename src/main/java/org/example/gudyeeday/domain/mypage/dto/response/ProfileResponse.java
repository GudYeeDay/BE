package org.example.gudyeeday.domain.mypage.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProfileResponse(
        String name,
        Long date,
        Long record,
        Long complete,
        Long left,

        @Schema(description = "애플 계정 연동 여부. true면 회원 탈퇴 전에 애플 로그인을 다시 해서 받은 authorizationCode를 탈퇴 API에 보내야 함")
        boolean appleLinked
) {
}
