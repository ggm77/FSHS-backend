package com.seohamin.fshs.v2.domain.share.service;

import com.seohamin.fshs.v2.domain.share.entity.SharedFile;
import com.seohamin.fshs.v2.domain.share.repository.SharedFileRepository;
import com.seohamin.fshs.v2.domain.user.entity.User;
import com.seohamin.fshs.v2.domain.user.repository.UserRepository;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final UserRepository userRepository;
    private final SharedFileRepository sharedFileRepository;

    /**
     * 생성된 공유키 삭제하는 메서드
     * @param shareId 공유키 아이디
     * @param username 요청한 유저명
     */
    public void deleteShareKey(
            final Long shareId,
            final String username
    ) {

        // 1) null 검사
        if (shareId == null || username == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        // 2) 공유키 조회
        final SharedFile sharedFile = sharedFileRepository.findById(shareId)
                .orElseThrow(() -> new CustomException(ExceptionCode.INVALID_REQUEST));

        // 3) 유저 조회
        final User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));

        // 4) 공유키 소유권자 확인
        if (!user.getId().equals(sharedFile.getOwner().getId())) {
            throw new CustomException(ExceptionCode.ACCESS_DENIED);
        }

        // 5) 공유키 삭제
        sharedFileRepository.delete(sharedFile);
    }
}
