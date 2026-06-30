package com.seohamin.fshs.v2.domain.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seohamin.fshs.v2.domain.user.controller.UserController;
import com.seohamin.fshs.v2.domain.user.dto.UserRequestDto;
import com.seohamin.fshs.v2.domain.user.dto.UserResponseDto;
import com.seohamin.fshs.v2.domain.user.dto.UserUpdateRequestDto;
import com.seohamin.fshs.v2.domain.user.entity.User;
import com.seohamin.fshs.v2.domain.user.service.UserService;
import com.seohamin.fshs.v2.global.util.SessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(UserControllerTest.AuthenticationPrincipalResolverConfig.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @TestConfiguration
    static class AuthenticationPrincipalResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(final List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("유저 API : 유저 등록 API 성공")
    void createUser_Success() throws Exception {
        // Given
        final UserRequestDto requestDto = new UserRequestDto("tester", "password123");
        final String requestBody = new ObjectMapper().writeValueAsString(requestDto);
        final UserResponseDto responseDto = UserResponseDto.of(new User("tester", "hashedPassword123"));

        given(userService.createUser(any(UserRequestDto.class), anyCollection())).willReturn(responseDto);

        // When & Then
        mockMvc.perform(post("/api/v2/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("tester"));
    }

    @Test
    @WithMockUser(username = "tester")
    @DisplayName("유저 API : 유저 조회 API 성공")
    void getUser_Success() throws Exception {
        // Given
        final Long userId = 1L;
        final UserResponseDto responseDto = UserResponseDto.of(new User("tester", "password123"));

        given(userService.getUser(userId, "tester")).willReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/api/v2/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("tester"));
    }

    @Test
    @WithMockUser(username = "tester")
    @DisplayName("유저 API : 유저 수정 API 성공")
    void updateUser_Success() throws Exception {
        // Given
        final Long userId = 1L;
        final UserUpdateRequestDto requestDto = new UserUpdateRequestDto("tester", "oldPassword", "password123");
        final String requestBody = new ObjectMapper().writeValueAsString(requestDto);
        final UserResponseDto responseDto = UserResponseDto.of(new User("tester", "hashedPassword123"));

        given(userService.updateUser(eq(userId), any(UserUpdateRequestDto.class), eq("tester"))).willReturn(responseDto);

        // When & Then
        try (MockedStatic<SessionUtil> mockedStatic = Mockito.mockStatic(SessionUtil.class)) {
            mockMvc.perform(patch("/api/v2/users/" + userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("tester"));
            mockedStatic.verify(() -> SessionUtil.forceLogout(any(HttpServletRequest.class)), times(1));
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("유저 API : 유저 삭제 API 성공")
    void deleteUser_Success() throws Exception {
        // Given
        final Long userId = 1L;

        // When & Then
        try (MockedStatic<SessionUtil> mockedStatic = Mockito.mockStatic(SessionUtil.class)) {
            mockMvc.perform(delete("/api/v2/users/" + userId))
                    .andExpect(status().isNoContent());
            then(userService).should().deleteUser(eq(userId), anyCollection());
            mockedStatic.verify(() -> SessionUtil.forceLogout(any(HttpServletRequest.class)), times(1));
        }
    }
}
