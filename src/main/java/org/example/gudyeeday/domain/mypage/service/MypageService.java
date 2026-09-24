package org.example.gudyeeday.domain.mypage.service;

import lombok.RequiredArgsConstructor;
import org.example.gudyeeday.common.exception.CustomException;
import org.example.gudyeeday.domain.mypage.dto.response.ProfileResponse;
import org.example.gudyeeday.domain.user.dto.request.SignupRequest;
import org.example.gudyeeday.domain.user.dto.response.UserResponse;
import org.example.gudyeeday.domain.user.entity.User;
import org.example.gudyeeday.domain.user.exception.AuthErrorCode;
import org.example.gudyeeday.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MypageService {

    private final UserRepository userRepository;

    @Transactional
    public ProfileResponse profile(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        //기록된 미션수 가져오는 로직

        //완성된 미션수 가져오는 로직

        //미완성 미션수 가져오는 로직

        //아래에 위 3개 채우기
        return  new ProfileResponse(user.getName(), user.getDaysSinceSignup(), 30l, 20l, 10l);

    }

}
