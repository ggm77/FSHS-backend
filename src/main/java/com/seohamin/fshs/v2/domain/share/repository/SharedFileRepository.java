package com.seohamin.fshs.v2.domain.share.repository;

import com.seohamin.fshs.v2.domain.share.entity.SharedFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SharedFileRepository extends JpaRepository<SharedFile, Long> {
    Optional<SharedFile> findByShareKey(final String shareKey);
}
