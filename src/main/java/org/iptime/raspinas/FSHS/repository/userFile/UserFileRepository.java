package org.iptime.raspinas.FSHS.repository.userFile;

import org.iptime.raspinas.FSHS.entity.userFile.UserFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFileRepository extends JpaRepository<UserFile, Long> {
    UserFile findByUrlAndIsDirectory(String url, boolean isDirectory);
}
