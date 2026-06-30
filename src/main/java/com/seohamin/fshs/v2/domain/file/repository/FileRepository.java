package com.seohamin.fshs.v2.domain.file.repository;

import com.seohamin.fshs.v2.domain.file.entity.Category;
import com.seohamin.fshs.v2.domain.file.entity.File;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<File, Long> {
    boolean existsByUuid(final String uuid);
    Optional<File> findByUuid(final String uuid);
    List<File> findAllByRelativePathStartingWith(final String relativePathPrefix);

    @Query("""
            SELECT f
            FROM File f
            WHERE f.relativePath LIKE :rootPathPattern ESCAPE '\\'
              AND f.lowerName LIKE :queryPattern ESCAPE '\\'
              AND f.category = :category
            """)
    Page<File> searchFiles(@Param("rootPathPattern") String rootPathPattern,
                           @Param("queryPattern") String queryPattern,
                           @Param("category") Category category,
                           Pageable pageable);

    @Query("""
            SELECT f
            FROM File f
            WHERE f.relativePath LIKE :rootPathPattern ESCAPE '\\'
              AND f.lowerName LIKE :queryPattern ESCAPE '\\'
            """)
    Page<File> searchFiles(@Param("rootPathPattern") String rootPathPattern,
                           @Param("queryPattern") String queryPattern,
                           Pageable pageable);

    @Query("""
            SELECT f
            FROM File f
            WHERE f.relativePath LIKE :rootPathPattern ESCAPE '\\'
              AND f.category IN ('IMAGE', 'VIDEO')
            """)
    Page<File> findImageAndVideo(@Param("rootPathPattern") String rootPathPattern,
                           Pageable pageable);

    // 폴더 이동/이름 변경 시 하위 파일들의 상대 경로/부모 경로 접두사를 한 번에 치환한다
    // pattern 은 '이전경로/%' 형태(ESCAPE '\'), cutFrom 은 '이전경로 길이 + 1'
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE File f
            SET f.relativePath = CONCAT(:newPrefix, SUBSTRING(f.relativePath, :cutFrom)),
                f.parentPath = CONCAT(:newPrefix, SUBSTRING(f.parentPath, :cutFrom))
            WHERE f.relativePath LIKE :pattern ESCAPE '\\'
            """)
    int rewriteDescendantPaths(@Param("newPrefix") String newPrefix,
                               @Param("cutFrom") int cutFrom,
                               @Param("pattern") String pattern);
}
