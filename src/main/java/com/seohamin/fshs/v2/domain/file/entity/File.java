package com.seohamin.fshs.v2.domain.file.entity;

import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.share.entity.SharedFile;
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
        name = "files",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_file_path",
                        columnNames = {"parent_folder_id", "name", "is_nfd"}
                )
        }
)
public class File {

    // 파일 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 파일이 속한 폴더
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_folder_id")
    private Folder parentFolder;

    // 파일 공유
    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SharedFile> sharedFiles = new ArrayList<>();

    // 파일 업로드한 유저 (비정규화)
    @Column(nullable = true)
    private Long ownerId;

    // 확장자 포함 파일명
    @Column(length = 255, nullable = false)
    @Size(max = 255)
    @NotNull
    private String name;

    // 확장자 제외 파일명
    @Column(length = 255, nullable = false)
    @Size(max = 255)
    @NotNull
    private String baseName;

    // 확장자
    @Column(length = 50, nullable = false)
    @Size(max = 50)
    @NotNull
    private String extension;

    // 파일 상대 경로
    @Column(length = 4096, nullable = false)
    @Size(max = 4096)
    @NotNull
    private String relativePath;

    // 부모 폴더 상대 경로 (비정규화)
    @Column(length = 4096, nullable = false)
    @Size(max = 4096)
    @NotNull
    private String parentPath;

    // MIME 타입
    @Column(length = 255, nullable = true)
    @Size(max = 255)
    private String mimeType;

    // 파일 크기
    @Column(nullable = false)
    @NotNull
    private Long size;

    // 비디오 코덱
    @Column(length = 255, nullable = true)
    @Size(max = 255)
    private String videoCodec;

    // 오디오 코덱
    @Column(length = 255, nullable = true)
    @Size(max = 255)
    private String audioCodec;

    // 이미지 너비
    @Column(nullable = true)
    private Integer width;

    // 이미지 높이
    @Column(nullable = true)
    private Integer height;

    // 파일 재생 시간
    @Column(nullable = true)
    private Long duration;

    // 이미지 회전 정보
    @Column(nullable = true)
    private Integer orientation;

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

    // 파일의 카테고리
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @NotNull
    private Category category;

    // 파일명이 NFD로 되어있는지 여부
    @Column(nullable = false)
    @NotNull
    private Boolean isNfd;

    @Builder
    public File(
            final Folder parentFolder,
            final Long ownerId,
            final String name,
            final String baseName,
            final String extension,
            final String relativePath,
            final String parentPath,
            final String mimeType,
            final Long size,
            final String videoCodec,
            final String audioCodec,
            final Integer width,
            final Integer height,
            final Long duration,
            final Integer orientation,
            final Instant originCreatedAt,
            final Instant originUpdatedAt,
            final Category category,
            final Boolean isNfd
    ){
        this.parentFolder = parentFolder;
        this.ownerId = ownerId;
        this.name = name;
        this.baseName = baseName;
        this.extension = extension;
        this.relativePath = relativePath;
        this.parentPath = parentPath;
        this.mimeType = mimeType;
        this.size = size;
        this.videoCodec = videoCodec;
        this.audioCodec = audioCodec;
        this.width = width;
        this.height = height;
        this.duration = duration;
        this.orientation = orientation;
        this.originCreatedAt = originCreatedAt;
        this.originUpdatedAt = originUpdatedAt;
        this.category = category;
        this.isNfd = isNfd;
    }

    // 상위 폴더 변경 메서드
    public void updateParentFolder(final Folder parentFolder) {
        this.parentFolder = parentFolder;
    }

    // 파일 주인 변경 메서드
    public void updateOwnerId(final Long ownerId) {
        this.ownerId = ownerId;
    }

    // 확장자 포함한 파일명 변경 메서드
    public void updateName(final String name) {
        this.name = name;
    }

    // 확장자 없는 파일명 변경 메서드
    public void updateBaseName(final String baseName) {
        this.baseName = baseName;
    }

    // 확장자 변경 메서드
    public void updateExtension(final String extension) {
        this.extension = extension;
    }

    // 파일 상대 경로 변경 메서드
    public void updateRelativePath(final String relativePath) {
        this.relativePath = relativePath;
    }

    // 상위 폴더 상대 경로 변경 메서드
    public void updateParentPath(final String parentPath) {
        this.parentPath = parentPath;
    }

    // MIME 타입 변경 메서드
    public void updateMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    // 파일 크기 변경 메서드
    public void updateSize(final Long size) {
        this.size = size;
    }

    // 비디오 코덱 변경 메서드
    public void updateVideoCodec(final String videoCodec) {
        this.videoCodec = videoCodec;
    }

    // 오디오 코덱 변경 메서드
    public void updateAudioCodec(final String audioCodec) {
        this.audioCodec = audioCodec;
    }

    // 재생시간 변경 메서드
    public void updateDuration(final Long duration) {
        this.duration = duration;
    }

    // 이미지 회전 정보 변경 메서드
    public void updateOrientation(final Integer orientation) {
        this.orientation = orientation;
    }

    // 파일 생성일 변경 메서드
    public void updateOriginCreatedAt(final Instant originCreatedAt) {
        this.originCreatedAt = originCreatedAt;
    }

    // 파일 수정일 변경 메서드
    public void updateOriginUpdatedAt(final Instant originUpdatedAt) {
        this.originUpdatedAt = originUpdatedAt;
    }

    // 파일 카테고리 변경 메서드
    public void updateCategory(final Category category) {
        this.category = category;
    }

    // 파일명 NFD인지 여부 변경 메서드
    public void updateIsNfd(final Boolean isNfd) {
        this.isNfd = isNfd;
    }
}
