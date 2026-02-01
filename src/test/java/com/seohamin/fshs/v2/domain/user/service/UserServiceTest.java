package com.seohamin.fshs.v2.domain.user.service;

import com.seohamin.fshs.v2.domain.user.dto.UserRequestDto;
import com.seohamin.fshs.v2.domain.user.dto.UserResponseDto;
import com.seohamin.fshs.v2.domain.user.entity.User;
import com.seohamin.fshs.v2.domain.user.repository.UserRepository;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.BDDMockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("유저 등록 : 성공")
    void createUser_Success() {
        // Given
        final UserRequestDto dto = UserRequestDto.builder()
                .username("newbie")
                .password("password123")
                .build();

        given(passwordEncoder.encode(dto.getPassword())).willReturn("encoded_password");
        given(userRepository.existsByUsername(dto.getUsername())).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        final UserResponseDto response = userService.createUser(dto);

        // Then
        assertThat(response).isNotNull();
        then(userRepository).should().save(any(User.class));
    }

    @Test
    @DisplayName("유저 등록 : 빈 유저명 등록 불가")
    void preventEmptyUsername() {
        // Given
        final UserRequestDto userRequestDto = UserRequestDto.builder()
                .username("")
                .password("password123")
                .build();

        // When & Then
        assertThatThrownBy(() -> userService.createUser(userRequestDto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("exceptionCode", ExceptionCode.INVALID_USERNAME);
    }

    @Test
    @DisplayName("유저 등록 : 너무 짧은 비밀번호 등록 불가")
    void preventShortPassword() {
        // Given
        final UserRequestDto userRequestDto = UserRequestDto.builder()
                .username("test")
                .password("1")
                .build();

        // When & Then
        assertThatThrownBy(() -> userService.createUser(userRequestDto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("exceptionCode", ExceptionCode.TOO_SHORT_PASSWORD);
    }

    @Test
    @DisplayName("유저 등록 : 중복 username 등록 불가")
    void preventDuplicateUsername() {
        // Given
        given(userRepository.existsByUsername("test")).willReturn(true);
        final UserRequestDto userRequestDto = UserRequestDto.builder()
                .username("test")
                .password("test")
                .build();

        // When & Then
        assertThatThrownBy(() -> userService.createUser(userRequestDto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("exceptionCode", ExceptionCode.USERNAME_DUPLICATE);
    }

    @Test
    @DisplayName("유저 조회 : 유저 조회 성공")
    void findUserById_Success() {
        // Given
        final Long userId = 1L;
        final User existingUser = User.builder().username("oldName").password("oldPass").build();
        given(userRepository.findById(userId)).willReturn(Optional.of(existingUser));

        // When
        final UserResponseDto userResponseDto = userService.getUser(userId);

        // Then
        assertThat(userResponseDto).isNotNull();
        assertThat(userResponseDto.getUsername()).isEqualTo("oldName");
        then(userRepository).should().findById(userId);
    }

    @Test
    @DisplayName("유저 조회 : 잘못된 유저 ID")
    void findUserById_NotFound() {
        // Given
        final Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUser(userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("exceptionCode", ExceptionCode.USER_NOT_EXIST);
    }

    @Test
    @DisplayName("유저 수정 : 잘못된 유저 ID")
    void updateUser_NotFound() {
        // Given
        final Long userId = 1L;
        final UserRequestDto updateDto = UserRequestDto.builder()
                .username("newName")
                .password("newPassword")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(userId, updateDto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("exceptionCode", ExceptionCode.USER_NOT_EXIST);
    }

    @Test
    @DisplayName("유저 수정 : 비밀번호만 수정")
    void updateUser_PasswordOnly() {
        // Given
        final Long userId = 1L;
        final User existingUser = User.builder().username("oldName").password("oldPass").build();
        final UserRequestDto updateDto = UserRequestDto.builder()
                .password("newPassword")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(existingUser));
        given(passwordEncoder.encode("newPassword")).willReturn("hashedNewPassword");

        // When
        userService.updateUser(userId, updateDto);

        // Then
        assertThat(existingUser.getPassword()).isEqualTo("hashedNewPassword");
        assertThat(existingUser.getUsername()).isEqualTo("oldName");
    }

    @Test
    @DisplayName("유저 수정 : 유저명만 수정")
    void updateUser_UsernameOnly() {
        // Given
        final Long userId = 1L;
        final User existingUser = User.builder().username("oldName").password("oldPass").build();
        final UserRequestDto updateDto = UserRequestDto.builder()
                .username("newName")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(existingUser));

        // When
        userService.updateUser(userId, updateDto);

        // Then
        assertThat(existingUser.getPassword()).isEqualTo("oldPass");
        assertThat(existingUser.getUsername()).isEqualTo("newName");
    }

    @Test
    @DisplayName("유저 수정 : 아무것도 수정 안함")
    void updateUser_Nothing() {
        // Given
        final Long userId = 1L;
        final User existingUser = User.builder().username("oldName").password("oldPass").build();
        final UserRequestDto updateDto = UserRequestDto.builder()
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(existingUser));

        // When
        userService.updateUser(userId, updateDto);

        // Then
        assertThat(existingUser.getPassword()).isEqualTo("oldPass");
        assertThat(existingUser.getUsername()).isEqualTo("oldName");
    }

    @Test
    @DisplayName("유저 삭제 : 삭제 성공")
    void deleteUser_Success() {
        // Given
        final Long userId = 1L;
        given(userRepository.existsById(userId)).willReturn(Boolean.TRUE);

        // When
        userService.deleteUser(userId);

        // Then
        then(userRepository).should().deleteById(userId);
    }

    @Test
    @DisplayName("유저 삭제 : 잘못된 유저 ID")
    void deleteUser_NotFound() {
        // Given
        final Long userId = 1L;
        given(userRepository.existsById(userId)).willReturn(Boolean.FALSE);

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("exceptionCode", ExceptionCode.USER_NOT_EXIST);
    }

    @Test
    @DisplayName("시큐리티 유저 로드 : 성공")
    void loadUserByUsername_Success() {
        // Given
        final String username = "test";
        final User user = User.builder()
                .username(username)
                .password("hashed_password")
                .build();

        given(userRepository.findByUsername(username)).willReturn(Optional.of(user));

        // When
        final UserDetails userDetails = userService.loadUserByUsername(username);

        // Then
        assertThat(userDetails.getUsername()).isEqualTo(username);
        assertThat(userDetails.getPassword()).isEqualTo("hashed_password");
        assertThat(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"))).isTrue();
    }

    @Test
    @DisplayName("시큐리티 유저 로드 : 유저가 없으면 예외 발생")
    void loadUserByUsername_NotFound() {
        // Given
        String username = "none";
        given(userRepository.findByUsername(username)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.loadUserByUsername(username))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }
}
