package org.example.gudyeeday.common.admin;

import org.example.gudyeeday.common.exception.CustomException;
import org.example.gudyeeday.common.response.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

// 관리자 API 검증 (사용자 권한(role)이 없어서 서버에만 있는 키로 확인)
@Component
public class AdminKeyValidator {

    private final String adminApiKey;

    public AdminKeyValidator(@Value("${admin.api-key:}") String adminApiKey) {
        this.adminApiKey = adminApiKey;
    }

    // 키가 설정되지 않았거나 다르면 403
    public void validate(String requestKey) {
        if (!StringUtils.hasText(adminApiKey) || requestKey == null
                || !MessageDigest.isEqual(
                adminApiKey.getBytes(StandardCharsets.UTF_8),
                requestKey.getBytes(StandardCharsets.UTF_8))) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}