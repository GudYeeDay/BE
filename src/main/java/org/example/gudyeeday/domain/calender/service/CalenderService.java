package org.example.gudyeeday.domain.calender.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gudyeeday.common.exception.CustomException;
import org.example.gudyeeday.domain.calender.dto.response.CalenderRecordResponse;
import org.example.gudyeeday.domain.calender.dto.response.CalenderResponse;
import org.example.gudyeeday.domain.complete.repository.CompleteMissionRepository;
import org.example.gudyeeday.domain.user.entity.User;
import org.example.gudyeeday.domain.user.exception.AuthErrorCode;
import org.example.gudyeeday.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalenderService {

    private final UserRepository userRepository;
    private final CompleteMissionRepository completeMissionRepository;

    public CalenderResponse getCalenderByYearMonth(String email, YearMonth yearMonth) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        List<CalenderRecordResponse> records = completeMissionRepository
                .findAllByUserIdAndCreatedAtBetween(user.getId(), start, end)
                .stream()
                .map(CalenderRecordResponse::from)
                .toList();

        return CalenderResponse.of((long) records.size(), records);
    }

}
