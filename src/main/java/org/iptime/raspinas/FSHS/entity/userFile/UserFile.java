package org.iptime.raspinas.FSHS.entity.userFile;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.iptime.raspinas.FSHS.entity.userInfo.UserInfo;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_file")
    private UserInfo userInfo;

    @Column(length = 260, nullable = false)
    private String originalFileName;

    @Column(length = 47, nullable = false) //extension max len = 10
    private String fileName;

    @Column(nullable = true)
    private Long fileSize;

    @Column(length = 10, nullable = true)
    private String fileExtension;

    @CreationTimestamp
    private Timestamp uploadDate;

    @CreationTimestamp
    private Timestamp updateDate;

    @Column(length = 2048, nullable = false)
    private String url;

    @Column(columnDefinition = "BOOLEAN", nullable = false)
    private boolean isDirectory;

    @Column(columnDefinition = "BOOLEAN", nullable = false)
    private boolean isFavorite;

    @Column(columnDefinition = "BOOLEAN", nullable = false)
    private boolean isStreaming;

    @Column(columnDefinition = "BOOLEAN", nullable = false)
    private boolean isStreamingMusic;

    @Column(columnDefinition = "BOOLEAN", nullable = false)
    private boolean isStreamingVideo;

    @Column(columnDefinition = "BOOLEAN", nullable = false)
    private boolean isShared;

    @Column(columnDefinition = "BOOLEAN", nullable = false)
    private boolean isSecrete;

    @Column(columnDefinition = "BOOLEAN", nullable = false)
    private boolean isDeleted;

    @Column(nullable = true)
    private Timestamp deletedDate;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private UserFile parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserFile> children = new ArrayList<>();

}
