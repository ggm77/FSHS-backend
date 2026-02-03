package com.seohamin.fshs.v2.domain.share.entity;

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

@Entity
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "shared_file")
public class SharedFile {

    // 공유 링크 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 공유 키
    @Column(length = 50, nullable = false, unique = true, updatable = false)
    @Size(max = 50)
    @NotNull
    private String shareKey;

    // 생성 시점
    @CreatedDate
    @Column(nullable = false)
    @NotNull
    private Instant createdAt;

    // 수정 시점
    @LastModifiedDate
    @Column(nullable = false)
    @NotNull
    private Instant updatedAt;

    // 공유한 파일
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", updatable = false, nullable = false)
    @NotNull
    private File file;

    // 공유한 유저
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", updatable = false, nullable = false)
    @NotNull
    private User owner;

    @Builder
    public SharedFile(
            final String shareKey,
            final File file,
            final User owner
    ) {
        this.shareKey = shareKey;
        this.file = file;
        this.owner = owner;
    }
}
