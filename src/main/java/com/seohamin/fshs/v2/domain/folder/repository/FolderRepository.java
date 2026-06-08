package com.seohamin.fshs.v2.domain.folder.repository;

import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    // @NotNull 어노테이션 우회해서 시스템 루트 폴더를 DB에 저장하기 위한 메서드
    // 자기 자신을 참조하기 위해 강제로 id를 1번으로 지정함
    @Modifying
    @Transactional
    @Query(value = """

            INSERT INTO folder (
            id, parent_folder_id, name, lower_name, relative_path,
            is_root, is_system_root, owner_id,
            origin_updated_at, created_at, updated_at
        ) VALUES (
            1, 1, :name, :lowerName, '',
            true, true, 0,
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        );

        ALTER TABLE folder ALTER COLUMN id RESTART WITH 2;
        """, nativeQuery = true)
    void insertSystemRoot(@Param("name") String name, @Param("lowerName") String lowerName);

    // 폴더 이동/이름 변경 시 하위 폴더들의 상대 경로 접두사를 한 번에 치환한다
    // pattern 은 '이전경로/%' 형태(ESCAPE '\'), cutFrom 은 '이전경로 길이 + 1'
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Folder f
            SET f.relativePath = CONCAT(:newPrefix, SUBSTRING(f.relativePath, :cutFrom))
            WHERE f.relativePath LIKE :pattern ESCAPE '\\'
            """)
    int rewriteDescendantPaths(@Param("newPrefix") String newPrefix,
                               @Param("cutFrom") int cutFrom,
                               @Param("pattern") String pattern);
}
