package com.seohamin.fshs.v2.global.config;

import com.seohamin.fshs.v2.domain.file.repository.FileRepository;
import com.seohamin.fshs.v2.domain.folder.repository.FolderRepository;
import com.seohamin.fshs.v2.domain.share.repository.SharedFileRepository;
import com.seohamin.fshs.v2.domain.transcoding.repository.TranscodingSettingRepository;
import com.seohamin.fshs.v2.domain.user.repository.UserRepository;
import com.seohamin.fshs.v2.global.init.AdminUserInitializer;
import com.seohamin.fshs.v2.global.init.StoragePathInitializer;
import com.seohamin.fshs.v2.global.init.SystemRootInitializer;
import com.seohamin.fshs.v2.global.init.TranscodingSettingInitializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private FileRepository fileRepository;

    @MockitoBean
    private FolderRepository folderRepository;

    @MockitoBean
    private SharedFileRepository sharedFileRepository;

    @MockitoBean
    private TranscodingSettingRepository transcodingSettingRepository;

    // 보안 필터 체인 검증 테스트라 초기화 러너는 Mock으로 무력화
    @MockitoBean
    private AdminUserInitializer adminUserInitializer;

    @MockitoBean
    private SystemRootInitializer systemRootInitializer;

    @MockitoBean
    private StoragePathInitializer storagePathInitializer;

    @MockitoBean
    private TranscodingSettingInitializer transcodingSettingInitializer;

    @Test
    @DisplayName("보안 설정 : 허용된 경로 인증 없이 접근 가능")
    void permittedEndpointsTest() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("보안 설정 : 인증이 필요한 API 접근시 401 반환")
    void apiUnauthorizedTest() throws Exception {
        mockMvc.perform(get("/api/v2/users/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("보안 설정 : 로그인 엔드포인트가 제대로 설정 되어있다.")
    void loginEndpointsTest() throws Exception {
        mockMvc.perform(formLogin("/api/v2/auth/login")
                .user("wrongUser")
                .password("wrongPassword")
        ).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("보안 설정 : 인증된 사용자는 로그아웃시 204를 받음")
    @WithMockUser
    void logoutEndpointsTest() throws Exception {
        mockMvc.perform(post("/api/v2/auth/logout").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
