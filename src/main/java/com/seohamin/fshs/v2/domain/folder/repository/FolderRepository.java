package com.seohamin.fshs.v2.domain.folder.repository;

import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    // @NotNull 어노테이션 우회해서 시스템 루트 폴더를 DB에 저장하기 위한 메서드
    // 자기 자신을 참조하기 위해 강제로 id를 1번으로 지정함
    @Modifying
    @Transactional
    @Query(value = """

            INSERT INTO folder (
            id, parent_folder_id, name, relative_path, is_nfd, 
            is_root, is_system_root, owner_id, 
            origin_created_at, origin_updated_at, created_at, updated_at
        ) VALUES (
            1, 1, '</SYSTEM/ROOT/>', '</SYSTEM/ROOT/>', false, 
            false, true, 0, 
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        );
        
        ALTER TABLE folder ALTER COLUMN id RESTART WITH 2;
        """, nativeQuery = true)
    void insertSystemRoot();
}
