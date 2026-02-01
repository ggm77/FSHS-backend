package com.seohamin.fshs.v2.domain.user;

import com.seohamin.fshs.v2.domain.user.entity.Role;
import com.seohamin.fshs.v2.domain.user.entity.User;
import com.seohamin.fshs.v2.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    @DisplayName("유저 저장 : 유저 ID가 생성됨")
    void savedUserHasId() {
        // Given
        final User user = new User("test", "test");

        // When
        final User savedUser = userRepository.save(user);

        // Then
        assertThat(savedUser.getId()).isNotNull();
    }

    @Test
    @DisplayName("유저 저장 : 저장된 username이 요청한 값과 일치함")
    void savedUserUsernameSame() {
        // Given
        final User user = new User("test", "test");

        // When
        final User savedUser = userRepository.save(user);

        // Then
        assertThat(savedUser.getUsername()).isEqualTo("test");
    }

    @Test
    @DisplayName("유저 조회 : 유저 ID로 유저 조회가 가능함")
    void findById_Success() {
        // Given
        final User user = new User("test", "test");
        final User savedUser = userRepository.save(user);
        final Long savedUserId = savedUser.getId();

        // When
        final Optional<User> foundUser = userRepository.findById(savedUserId);

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("test");
    }

    @Test
    @DisplayName("유저 조회 : 존재하지 않는 ID로 조회시 빈 Optional 반환")
    void findById_NotFound() {
        // Given
        final Long userId = 999999L;

        // When
        final Optional<User> foundUser = userRepository.findById(userId);

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("유저 조회 : username으로 유저 조회가 가능함")
    void findByUsername_Success() {
        // Given
        final User user = new User("test", "test");
        userRepository.save(user);

        // When
        final Optional<User> foundUser = userRepository.findByUsername("test");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("test");
    }

    @Test
    @DisplayName("유저 조회 : 존재하지 않는 username으로 조회시 빈 Optional 반환")
    void findByUsername_NotFound() {
        // Given
        final String username = "non-existent";

        // When
        final Optional<User> foundUser = userRepository.findByUsername(username);

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("유저 업데이트 : 더티 체킹을 통한 데이터 업데이트 확인")
    void updateWithDirtyChecking() {
        // Given
        final User user = new User("oldUsername", "oldPassword");
        final User savedUser = userRepository.save(user);
        final Long savedUserId = savedUser.getId();

        // When
        savedUser.updateUsername("newUsername");
        savedUser.updatePassword("newPassword");
        savedUser.updateRole(Role.ADMIN);
        userRepository.flush();
        testEntityManager.clear();

        // Then
        final Optional<User> foundUser = userRepository.findById(savedUserId);
        assertThat(foundUser.get().getUsername()).isEqualTo("newUsername");
        assertThat(foundUser.get().getPassword()).isEqualTo("newPassword");
        assertThat(foundUser.get().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("유저 삭제 : 유저 ID로 유저 삭제가 가능함")
    void deleteById_Success() {
        // Given
        final User user = new User("test", "test");
        final User savedUser = userRepository.save(user);
        final Long savedUserId = savedUser.getId();

        // When
        userRepository.deleteById(savedUserId);

        // Then
        final Optional<User> foundUser = userRepository.findById(savedUserId);
        assertThat(foundUser).isEmpty();
    }
}
