package org.example.gudyeeday.domain.calender.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gudyeeday.common.response.ApiResponse;
import org.example.gudyeeday.domain.calender.dto.response.CalenderResponse;
import org.example.gudyeeday.domain.calender.service.CalenderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.ZoneId;


@Slf4j
@Tag(name = "Calender API", description = "캘린더")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calender")
public class CalenderController {

    private final CalenderService calenderService;

    @Operation(summary = "캘린더", description = "yyyy-MM 형식의 연월로 캘린더를 조회합니다. 생략 시 이번 달 기준.")
    @GetMapping
    public ResponseEntity<ApiResponse<CalenderResponse>> getCalenderByYearMonth(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "조회할 연월 (yyyy-MM), 생략 시 이번 달", example = "2026-10")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth
    ) {
        YearMonth target = (yearMonth != null) ? yearMonth : YearMonth.now(ZoneId.of("Asia/Seoul"));

        CalenderResponse response = calenderService.getCalenderByYearMonth(userDetails.getUsername(), target);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

}
