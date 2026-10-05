package org.example.gudyeeday.domain.complete.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gudyeeday.common.response.ApiResponse;
import org.example.gudyeeday.domain.complete.dto.request.CompleteMissionRequest;
import org.example.gudyeeday.domain.complete.dto.response.CompleteMissionResponse;
import org.example.gudyeeday.domain.complete.service.CompleteService;
import org.example.gudyeeday.domain.mypage.dto.request.NameChangeRequest;
import org.example.gudyeeday.domain.mypage.dto.request.PasswordChangeRequest;
import org.example.gudyeeday.domain.mypage.dto.response.ProfileResponse;
import org.example.gudyeeday.domain.mypage.service.MypageService;
import org.example.gudyeeday.domain.user.service.EmailVerificationService;
import org.example.gudyeeday.domain.user.service.UserService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Tag(name = "Complete API", description = "미션 완료")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/complete")
public class CompleteController {

    private final CompleteService completeService;

    @Operation(summary = "미션 완료하기", description = "")
    @PostMapping(value = "/complete/{userMissionId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<CompleteMissionResponse>>> completeMission(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userMissionId,
            @RequestPart("image") MultipartFile image,
            @Valid @ParameterObject @ModelAttribute CompleteMissionRequest request) {

        List<CompleteMissionResponse> response = completeService.completeMission(userDetails.getUsername(), userMissionId, image, request);

        log.info("2차로 request = {}", request);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

}
