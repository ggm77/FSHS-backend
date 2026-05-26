package com.seohamin.fshs.v2.domain.file.repository;

import com.seohamin.fshs.v2.domain.file.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileRepository extends JpaRepository<File, Long> {
    boolean existsByUuid(final String uuid);
    Optional<File> findByUuid(final String uuid);
}
