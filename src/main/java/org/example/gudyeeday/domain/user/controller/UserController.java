package org.example.gudyeeday.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gudyeeday.common.response.ApiResponse;
import org.example.gudyeeday.domain.user.dto.request.*;
import org.example.gudyeeday.domain.user.dto.response.*;
import org.example.gudyeeday.domain.user.service.EmailVerificationService;
import org.example.gudyeeday.domain.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "User API", description = "유저 - 회원가입/로그인")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;

    @Operation(
            summary = "로컬 회원가입",
            description = "")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(ApiResponse.onSuccess(userService.signup(request)));
    }

    @Operation(
            summary = "로컬 로그인",
            description = "")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.onSuccess(userService.login(request)));
    }

    @Operation(
            summary = "accessToken 재발급",
            description = "")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> reissue(@Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.onSuccess(userService.reissue(request)));
    }

    @Operation(
            summary = "로그아웃",
            description = "")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserDetails userDetails) {
        userService.logout(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }

    @Operation(
            summary = "이메일 인증코드 전송",
            description = "")
    @PostMapping("/email/verification-code")
    public ResponseEntity<ApiResponse<EmailCodeSendResponse>> sendVerificationCode(@Valid @RequestBody EmailCodeSendRequest request) {
        return ResponseEntity.ok(ApiResponse.onSuccess(emailVerificationService.sendVerificationCode(request)));
    }

    @Operation(
            summary = "이메일 인증코드 확인",
            description = "")
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<EmailVerificationResponse>> verifyCode(@Valid @RequestBody EmailVerificationRequest request) {
        return ResponseEntity.ok(ApiResponse.onSuccess(emailVerificationService.verifyCode(request)));
    }

    @Operation(
            summary = "구글 로그인",
            description = "")
    @PostMapping("/login/google")
    public ResponseEntity<ApiResponse<GoogleLoginResponse>> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.onSuccess(userService.googleLogin(request)));
    }

    @Operation(
            summary = "비밀번호 재설정",
            description = "")
    @PostMapping("/passwordReset")
    public ResponseEntity<ApiResponse<String>> passwordReset(@Valid @RequestBody PasswordResetRequest request){
        userService.passwordReset(request);
     return ResponseEntity.ok(ApiResponse.onSuccess("비밀번호가 성공적으로 재설정되었습니다."));
    }

}
