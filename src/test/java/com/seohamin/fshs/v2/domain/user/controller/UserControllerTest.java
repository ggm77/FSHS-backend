package com.seohamin.fshs.v2.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seohamin.fshs.v2.domain.user.dto.UserRequestDto;
import com.seohamin.fshs.v2.domain.user.dto.UserResponseDto;
import com.seohamin.fshs.v2.domain.user.entity.User;
import com.seohamin.fshs.v2.domain.user.service.UserService;
import com.seohamin.fshs.v2.global.util.SessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private SessionUtil sessionUtil;

    @Test
    @WithMockUser
    @DisplayName("유저 API : 유저 등록 API 성공")
    void createUser_Success() throws Exception {
        // Given
        final UserRequestDto requestDto = UserRequestDto.builder()
                .username("tester")
                .password("password123")
                .build();
        final String requestBody = new ObjectMapper().writeValueAsString(requestDto);
        final UserResponseDto responseDto = UserResponseDto.builder()
                .user(new User("tester", "hashedPassword123"))
                .rootFolderId(null)
                .build();

        given(userService.createUser(any(UserRequestDto.class))).willReturn(responseDto);

        // When & Then
        mockMvc.perform(post("/api/v2/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("tester"));
    }

    @Test
    @WithMockUser
    @DisplayName("유저 API : 유저 조회 API 성공")
    void getUser_Success() throws Exception {
        // Given
        final Long userId = 1L;
        final UserResponseDto responseDto = UserResponseDto.builder()
                .user(new User("tester", "password123"))
                .rootFolderId(null)
                .build();

        given(userService.getUser(userId)).willReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/api/v2/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("tester"));
    }

    @Test
    @WithMockUser
    @DisplayName("유저 API : 유저 수정 API 성공")
    void updateUser_Success() throws Exception {
        // Given
        final Long userId = 1L;
        final UserRequestDto requestDto = UserRequestDto.builder()
                .username("tester")
                .password("password123")
                .build();
        final String requestBody = new ObjectMapper().writeValueAsString(requestDto);
        final UserResponseDto responseDto = UserResponseDto.builder()
                .user(new User("tester", "hashedPassword123"))
                .rootFolderId(null)
                .build();

        given(userService.updateUser(eq(userId), any(UserRequestDto.class))).willReturn(responseDto);

        // When & Then
        mockMvc.perform(patch("/api/v2/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("tester"));
        then(sessionUtil).should(times(1)).forceLogout(any(HttpServletRequest.class));
    }

    @Test
    @WithMockUser
    @DisplayName("유저 API : 유저 삭제 API 성공")
    void deleteUser_Success() throws Exception {
        // Given
        final Long userId = 1L;

        // When & Then
        mockMvc.perform(delete("/api/v2/users/" + userId))
                .andExpect(status().isNoContent());
        then(sessionUtil).should(times(1)).forceLogout(any(HttpServletRequest.class));
    }
}
