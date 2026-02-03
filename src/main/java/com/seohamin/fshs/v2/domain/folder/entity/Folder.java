package com.seohamin.fshs.v2.domain.folder.entity;

import com.seohamin.fshs.v2.domain.file.entity.File;
import com.seohamin.fshs.v2.domain.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(
        name = "folder",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_folder_path",
                        columnNames = {"parent_folder_id", "name", "is_nfd"}
                )
        }
)
public class Folder {

    // 폴더 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 루트 폴더 주인
    @OneToOne(mappedBy = "rootFolder")
    private User owner;

    // 하위 파일
    @OneToMany(mappedBy = "parentFolder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<File> files = new ArrayList<>();

    // 하위 폴더
    @OneToMany(mappedBy = "parentFolder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Folder> folders = new ArrayList<>();

    // 상위 폴더
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_folder_id")
    private Folder parentFolder;

    // 폴더 만든 유저 (비정규화)
    @Column(nullable = true)
    private Long ownerId;

    // 폴더 상대 위치
    @Column(length = 4096, nullable = false)
    @Size(max = 4096)
    @NotNull
    private String relativePath;

    // 폴더 이름
    @Column(length = 255, nullable = false)
    @Size(max = 255)
    @NotNull
    private String name;

    // 파일 생성 시점
    @Column(nullable = false)
    @NotNull
    private Instant originCreatedAt;

    // 파일 수정 시점
    @Column(nullable = false)
    @NotNull
    private Instant originUpdatedAt;

    // DB 저장 시점
    @CreatedDate
    @Column(nullable = false)
    @NotNull
    private Instant createdAt;

    // DB 수정 시점
    @LastModifiedDate
    @Column(nullable = false)
    @NotNull
    private Instant updatedAt;

    @Column(nullable = false)
    @NotNull
    private Boolean isNfd;

    @Builder
    public Folder(
            final Folder parentFolder,
            final Long ownerId,
            final String relativePath,
            final String name,
            final Instant originCreatedAt,
            final Instant originUpdatedAt,
            final Boolean isNfd
    ) {
        this.parentFolder = parentFolder;
        this.ownerId = ownerId;
        this.relativePath = relativePath;
        this.name = name;
        this.originCreatedAt = originCreatedAt;
        this.originUpdatedAt = originUpdatedAt;
        this.isNfd = isNfd;
    }

    // 상위 폴더 상대 경로 변경 메서드
    public void updateParentFolder(final Folder parentFolder) {
        this.parentFolder = parentFolder;
    }

    // 폴더 주인 변경 메서드
    public void updateOwnerId(final Long ownerId) {
        this.ownerId = ownerId;
    }

    // 폴더 상대 경로 변경 메서드
    public void updateRelativePath(final String relativePath) {
        this.relativePath = relativePath;
    }

    // 폴더명 변경 메서드
    public void updateName(final String name) {
        this.name = name;
    }

    // 폴더 생성 시점 변경 메서드
    public void updateOriginCreatedAt(final Instant originCreatedAt) {
        this.originCreatedAt = originCreatedAt;
    }

    // 폴더 수정 시점 변경 메서드
    public void updateOriginUpdatedAt(final Instant originUpdatedAt) {
        this.originUpdatedAt = originUpdatedAt;
    }

    // NFD 여부 변경 메서드
    public void updateIsNfd(final Boolean isNfd) {
        this.isNfd = isNfd;
    }
}
