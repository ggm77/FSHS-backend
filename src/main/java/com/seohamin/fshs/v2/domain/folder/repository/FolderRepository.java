package com.seohamin.fshs.v2.domain.folder.repository;

import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FolderRepository extends JpaRepository<Folder, Long> {
}
