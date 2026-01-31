package com.seohamin.fshs.v2.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User {

    // 유저 ID
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    // 유저 이름
    @Column(length = 255, nullable = false, unique = true)
    private String username;

    // 유저 비밀번호
    @Column(length = 100, nullable = false)
    private String password;

    // 유저 Role
    @Enumerated(EnumType.STRING)
    private Role role;   // enum Role { USER, ADMIN }

    // 폴더 엔티티 추가 되면 루트 폴더만 N:1

    @Builder
    public User(
            final String username,
            final String password
    ) {
        this.username = username;
        this.password = password;
        this.role = Role.USER;
    }

    // 이름 변경용 메서드
    public void updateUsername(final String username) {
        this.username = username;
    }

    // 비밀번호 변경용 메서드
    public void updatePassword(final String password) {
        this.password = password;
    }

    // Role 변경용 메서드
    public void updateRole(final Role role) {
        this.role = role;
    }
}
