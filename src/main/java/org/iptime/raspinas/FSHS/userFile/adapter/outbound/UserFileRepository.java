package org.iptime.raspinas.FSHS.userFile.adapter.outbound;

import org.iptime.raspinas.FSHS.userFile.domain.UserFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFileRepository extends JpaRepository<UserFile, Long> {
    UserFile findByUrlAndIsDirectory(String url, boolean isDirectory);
    boolean existsByUrlAndIsDirectory(String url, boolean isDirectory);
    boolean existsByParentIdAndOriginalFileName(Long parentId, String originalFileName);
}
