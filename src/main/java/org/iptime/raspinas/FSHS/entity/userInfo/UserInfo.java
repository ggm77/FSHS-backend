package org.iptime.raspinas.FSHS.entity.userInfo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.iptime.raspinas.FSHS.dto.auth.signUp.request.SignUpRequestDto;
import org.iptime.raspinas.FSHS.entity.userFile.UserFile;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50,nullable = false)
    private String userName;

    @Column(length = 320, unique = true, nullable = false)
    private String userEmail;

    @Column(length = 60, nullable = false)
    private String userPassword;

    @Column(columnDefinition = "TEXT", nullable = true)
    private String userProfilePictureUrl;

    @CreationTimestamp
    private Timestamp signUpDate;

    @OneToMany(mappedBy = "userInfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserFile> userFile = new ArrayList<>();

    @Column(columnDefinition = "BOOLEAN", nullable = false)
    private boolean isAdmin;

    @Column(columnDefinition ="BOOLEAN", nullable = false)
    private boolean isDisabled;

    public UserInfo(SignUpRequestDto requestDto){
        this.userName = requestDto.getUserName();
        this.userEmail = requestDto.getUserEmail();
        this.userPassword = requestDto.getUserPassword();
        this.isAdmin = false;
        this.isDisabled = false;
    }
}
