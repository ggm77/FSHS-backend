package com.seohamin.fshs.v2.domain.user.entity;

import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.share.entity.SharedFile;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "users")
public class User {

    // 유저 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 루트 폴더 연관 관계
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "root_folder_id")
    private Folder rootFolder;

    // 공유 키 연관 관계
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SharedFile> sharedFiles = new ArrayList<>();

    // 유저 이름
    @Column(length = 255, nullable = false, unique = true)
    private String username;

    // 유저 비밀번호
    @Column(length = 100, nullable = false)
    private String password;

    // 유저 Role
    @Enumerated(EnumType.STRING)
    private Role role;   // enum Role { USER, ADMIN }

    @Builder
    public User(
            final String username,
            final String password
    ) {
        this.username = username;
        this.password = password;
        this.role = Role.USER;
        this.rootFolder = null;
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

    // 루트 폴더 변경용 메서드
    public void updateRootFolder(final Folder rootFolder) {
        this.rootFolder = rootFolder;
    }
}
