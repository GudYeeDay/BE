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
import org.example.gudyeeday.domain.user.dto.response.UserResponse;
import org.example.gudyeeday.domain.user.service.EmailVerificationService;
import org.example.gudyeeday.domain.user.service.UserService;
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

}
