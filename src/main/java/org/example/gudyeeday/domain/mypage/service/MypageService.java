package org.example.gudyeeday.domain.mypage.service;

import lombok.RequiredArgsConstructor;
import org.example.gudyeeday.common.exception.CustomException;
import org.example.gudyeeday.domain.complete.repository.CompleteMissionRepository;
import org.example.gudyeeday.domain.mission.enums.UserMissionStatus;
import org.example.gudyeeday.domain.mission.repository.UserMissionRepository;
import org.example.gudyeeday.domain.mypage.dto.request.NameChangeRequest;
import org.example.gudyeeday.domain.mypage.dto.request.PasswordChangeRequest;
import org.example.gudyeeday.domain.mypage.dto.response.ProfileResponse;
import org.example.gudyeeday.domain.user.dto.request.SignupRequest;
import org.example.gudyeeday.domain.user.dto.response.EmailVerificationResponse;
import org.example.gudyeeday.domain.user.dto.response.UserResponse;
import org.example.gudyeeday.domain.user.entity.User;
import org.example.gudyeeday.domain.user.enums.Provider;
import org.example.gudyeeday.domain.user.exception.AuthErrorCode;
import org.example.gudyeeday.domain.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MypageService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMissionRepository userMissionRepository;
    private final CompleteMissionRepository completeMissionRepository;

    @Transactional
    public ProfileResponse profile(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        Long userMissionCount = userMissionRepository.countByUserId(user.getId());

        Long completeMissionCount = userMissionRepository.countByUserIdAndStatus(user.getId(), UserMissionStatus.COMPLETED);

        Long unCompleteMissionCount = userMissionCount - completeMissionCount;

        return  new ProfileResponse(user.getName(), user.getDaysSinceSignup(), userMissionCount, completeMissionCount, unCompleteMissionCount);

    }

    @Transactional
    public String changeName(String email, NameChangeRequest nameChangeRequest) {

        User user = userRepository.findByEmail(email).orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        user.updateName(nameChangeRequest.name());

        return "닉네임이 성공적으로 변경되었습니다.";
    }

    @Transactional
    public String changePassword(String email, PasswordChangeRequest passwordChangeRequest) {

        if (!passwordChangeRequest.newPassword().equals(passwordChangeRequest.newPasswordCheck())) {
            throw new CustomException(AuthErrorCode.PASSWORD_MISMATCH);
        }

        User user = userRepository.findByEmail(email).orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        if (!user.hasPassword()) {
            throw new CustomException(AuthErrorCode.SOCIAL_LOGIN_REQUIRED);
        }

        if (!passwordEncoder.matches(passwordChangeRequest.currentPassword(), user.getPassword())) {
            throw new CustomException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        if(passwordChangeRequest.newPassword().equals(passwordChangeRequest.currentPassword())){
            throw new CustomException(AuthErrorCode.SAME_AS_CURRENT_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(passwordChangeRequest.newPassword()));

        user.getRefreshTokens().clear();

        return "비밀번호가 성공적으로 변경되었습니다.";
    }

}
