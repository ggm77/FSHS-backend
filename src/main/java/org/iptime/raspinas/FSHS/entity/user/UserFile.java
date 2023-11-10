package org.iptime.raspinas.FSHS.entity.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_file")
    private UserInfo userInfo;

    @Column(length = 260, nullable = false)
    private String fileName;

    @Column(nullable = false)
    private int fileSize;

    @Column(length = 2048, nullable = false)
    private String thumbnailUrl;

    @CreationTimestamp
    private Timestamp uploadDate;

    @CreationTimestamp
    private Timestamp updateDate;

    @Column(length = 2048, nullable = false)
    private String url;

    private boolean isFavorite;

    private boolean isStreamingMusic;

    private boolean isStreamingVideo;

    private boolean isShared;

    private boolean isSecrete;

    private boolean isDeleted;

    @Column(nullable = true)
    private Timestamp deletedDate;
}
