package com.seohamin.fshs.v2.domain.user.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.folder.repository.FolderRepository;
import com.seohamin.fshs.v2.domain.user.dto.UserRootFolderRequestDto;
import lombok.RequiredArgsConstructor;
import com.seohamin.fshs.v2.domain.user.dto.UserRequestDto;
import com.seohamin.fshs.v2.domain.user.dto.UserResponseDto;
import com.seohamin.fshs.v2.domain.user.entity.User;
import com.seohamin.fshs.v2.domain.user.repository.UserRepository;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final Cache<String, Boolean> fileAccessCache;

    /**
     * 유저 생성하는 메서드
     * 어드민 유저가 아닌 새로운 유저를 추가하는 메서드임
     * @param userRequestDto 유저 정보 담긴 DTO
     * @return 저장된 유저 정보 담긴 DTO
     */
    @Transactional
    public UserResponseDto createUser(final UserRequestDto userRequestDto) {

        // 1) null 및 유효성 검사
        final String username = userRequestDto.username();
        final String rawPassword = userRequestDto.password();

        if (username == null || username.isEmpty()) {
            throw new CustomException(ExceptionCode.INVALID_USERNAME);
        }
        if (rawPassword == null || rawPassword.length() < 4) {
            throw new CustomException(ExceptionCode.TOO_SHORT_PASSWORD);
        }

        // 2) 유저명 겹치는지 확인
        if (userRepository.existsByUsername(username)) {
            throw new CustomException(ExceptionCode.USERNAME_DUPLICATE);
        }

        // 3) 비밀번호 해싱
        final String password = passwordEncoder.encode(rawPassword);

        // 4) 엔티티 만들기
        final User user = User.builder()
                .username(username)
                .password(password)
                .build();

        // 5) DB에 저장
        final User savedUser = userRepository.save(user);

        return UserResponseDto.of(savedUser);
    }

    /**
     * 유저 조회하는 메서드
     * @return 조회한 유저 DTO
     */
    public UserResponseDto getUser(final Long userId) {

        // 1) 유저 조회
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));

        // 2) DTO에 담아 리턴
        return UserResponseDto.of(user);
    }

    /**
     * 유저 정보 수정하는 메서드
     * @param userRequestDto 수정할 정보 담긴 DTO
     * @return 수정된 유저 정보 DTO
     */
    @Transactional
    public UserResponseDto updateUser(
            final Long userId,
            final UserRequestDto userRequestDto
    ) {

        // 1) 유저 이름 변수에 저장
        final String username = userRequestDto.username();

        // 2) 비밀번호 비어있지 않으면 해싱
        final String password;
        if(userRequestDto.password() != null && !userRequestDto.password().isEmpty()) {

            // 비밀번호 길이 제한
            if(userRequestDto.password().length() < 4 ) {
                throw new CustomException(ExceptionCode.TOO_SHORT_PASSWORD);
            }

            password = passwordEncoder.encode(userRequestDto.password());
        }
        else {
            password = null;
        }

        // 3) 유저 엔티티 조회
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));

        // 4) 유저명 비어있지 않다면 변경
        if(username != null && !username.isEmpty()) {
            user.updateUsername(username);
        }

        // 5) 비밀번호 비어있지 않다면 변경
        if(password != null) {
            user.updatePassword(password);
        }

        // 6) DTO에 담아서 리턴
        return UserResponseDto.of(user);
    }

    /**
     * 유저 삭제용 메서드
     * @param userId 삭제할 유저 ID
     */
    @Transactional
    public void deleteUser(final Long userId) {

        // 1) 유저 존재 확인
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ExceptionCode.USER_NOT_EXIST);
        }

        // 2) 삭제
        userRepository.deleteById(userId);
    }

    /**
     * 특정 유저에게 특정 폴더를 루트 폴더로 지정시켜주는 메서드
     * 어드민만 사용가능하다.
     * @param userId 루트 폴더로 지정받을 유저
     * @param userRootFolderRequestDto 루트 폴더 정보 든 dto
     * @param authorities 요청 넣은 유저의 권한
     */
    @Transactional
    public void setRootFolder(
            final Long userId,
            final UserRootFolderRequestDto userRootFolderRequestDto,
            final Collection<? extends GrantedAuthority> authorities
    ) {
        // 1) null 검사
        if (userId == null || userRootFolderRequestDto == null || authorities == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 어드민 권한 확인
        boolean isAdmin = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new CustomException(ExceptionCode.ACCESS_DENIED);
        }

        // 3) 유저 조회
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));

        // 4) 폴더 조회
        final Folder folder = folderRepository.findById(userRootFolderRequestDto.folderId())
                .orElseThrow(() -> new CustomException(ExceptionCode.FOLDER_NOT_EXIST));

        // 5) 루트 폴더 설정
        user.updateRootFolder(folder);

        // 6) 해당 유저의 권한 캐시 무효화 — 루트 재지정 즉시 새 권한 적용
        final String username = user.getUsername();
        fileAccessCache.asMap().keySet().removeIf(key -> key.endsWith(":" + username));
    }

    /**
     * 세션 기반 인증시 스프링 시큐리티가 사용하는 메서드
     * @param username the username identifying the user whose data is required.
     * @return 스프링 시큐리티에서 사용하는 유저 객체
     */
    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {

        // 1) DB에서 유저 조회
        final User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // 2) 스프링 시큐리티가 사용하는 객체로 리턴
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getUserRole().name())
                .build();
    }
}
