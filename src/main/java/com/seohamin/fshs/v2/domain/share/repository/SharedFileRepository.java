package com.seohamin.fshs.v2.domain.share.repository;

import com.seohamin.fshs.v2.domain.share.entity.SharedFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedFileRepository extends JpaRepository<SharedFile, Long> {
}
