package org.example.gudyeeday.domain.mypage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gudyeeday.common.response.ApiResponse;
import org.example.gudyeeday.domain.mypage.dto.request.NameChangeRequest;
import org.example.gudyeeday.domain.mypage.dto.request.PasswordChangeRequest;
import org.example.gudyeeday.domain.mypage.dto.response.ProfileResponse;
import org.example.gudyeeday.domain.mypage.service.MypageService;
import org.example.gudyeeday.domain.user.dto.request.SignupRequest;
import org.example.gudyeeday.domain.user.dto.request.WithdrawRequest;
import org.example.gudyeeday.domain.user.dto.response.UserResponse;
import org.example.gudyeeday.domain.user.service.EmailVerificationService;
import org.example.gudyeeday.domain.user.service.UserService;
import org.example.gudyeeday.domain.user.service.WithdrawService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Mypage API", description = "마이페이지")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage")
public class MypageController {

    private final UserService userService;
    private final MypageService mypageService;
    private final EmailVerificationService emailVerificationService;
    private final WithdrawService withdrawService;

    @Operation(
            summary = "프로필",
            description = "")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> profile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.onSuccess(mypageService.profile(userDetails.getUsername())));
    }

    @Operation(
            summary = "닉네임 수정",
            description = "")
    @PutMapping("/changeName")
    public ResponseEntity<ApiResponse<String>> changeName(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NameChangeRequest nameChangeRequest
                                        ) {
        return ResponseEntity.ok(ApiResponse.onSuccess(mypageService.changeName(userDetails.getUsername(), nameChangeRequest)));
    }

    @Operation(
            summary = "비밀번호 변경",
            description = "")
    @PutMapping("/changePassword")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PasswordChangeRequest passwordChangeRequest
                                        ) {
        return ResponseEntity.ok(ApiResponse.onSuccess(mypageService.changePassword(userDetails.getUsername(), passwordChangeRequest)));
    }

    @Operation(
            summary = "회원 탈퇴",
            description = """
                    계정과 연결된 데이터를 즉시 모두 삭제합니다. (되돌릴 수 없음)
                    - 삭제: 미션 진행/완료 기록과 사진, 보관함, 나만의 굳이, 주간 사진, 알림, 푸시 기기 토큰, 로그인 토큰
                    - 구글 연동: 계정과 함께 연동 정보가 삭제됩니다.
                    - 애플 연동(appleLinked): 탈퇴 직전 앱에서 애플 로그인을 한 번 더 해서 받은 authorizationCode를
                      appleAuthorizationCode로 보내야 합니다. 서버가 애플에 연동 해제(revoke)를 요청한 뒤 삭제합니다.
                    - 애플 연동이 없는 계정은 요청 본문 없이 호출해도 됩니다.
                    오류: AUTH4018(애플 코드 누락), AUTH4019(코드 만료/잘못됨), AUTH4020(다른 애플 계정), AUTH5021(애플 서버 오류)""")
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<String>> withdraw(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) WithdrawRequest request
    ) {
        withdrawService.withdraw(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.onSuccess("회원 탈퇴가 완료되었습니다."));
    }

}
