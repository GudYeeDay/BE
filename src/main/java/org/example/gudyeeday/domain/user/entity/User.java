package org.example.gudyeeday.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.gudyeeday.common.entity.BaseEntity;
import org.example.gudyeeday.domain.user.enums.Provider;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user",uniqueConstraints = {@UniqueConstraint(name = "uk_user_provider_provider_id", columnNames = {"provider", "provider_id"})})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    // 소셜 로그인 계정은 null (암호화 저장)
    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private Provider provider;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RefreshToken> refreshTokens = new ArrayList<>();

//    @Builder.Default
//    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<Application> applications = new ArrayList<>();

    // 일반(로컬) 회원가입 생성 메서드
    public static User createLocalUser(
            String email,
            String password,
            String name
    ) {
        return User.builder()
                .email(email)
                .password(password)
                .name(name)
                .provider(Provider.LOCAL)

                .build();
    }

    // 소셜(구글) 회원가입 생성 메서드
    public static User createSocialUser(
            String email,
            String name,
            String providerId
    ) {
        return User.builder()
                .email(email)
                .name(name)
                .provider(Provider.GOOGLE)
                .providerId(providerId)
                .build();
    }

    // 닉네임 수정
    public void updateName(String name) {
        this.name = name;
    }

    public void linkGoogleAccount(String providerId) {
        this.providerId = providerId;
    }

    public boolean hasGoogleLinked() {
        return this.providerId != null;
    }

    public boolean hasPassword() {
        return this.password != null && !this.password.isBlank();
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}