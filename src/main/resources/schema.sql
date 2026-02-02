/*

 @UniqueConstraint가 SQLite에서 적용이 안되서 여기에서 유니크 설정

 */

-- 1. 하위 폴더용 유니크 인덱스 (부모가 있을 때)
CREATE UNIQUE INDEX IF NOT EXISTS uk_folder_sub_path
    ON folders (parent_folder_id, name, is_nfd)
    WHERE parent_folder_id IS NOT NULL;

-- 2. 루트 폴더용 유니크 인덱스 (부모가 NULL일 때)
CREATE UNIQUE INDEX IF NOT EXISTS uk_folder_root_path
    ON folders (owner_id, name, is_nfd)
    WHERE parent_folder_id IS NULL;

-- 폴더 내 파일 중복 방지
CREATE UNIQUE INDEX IF NOT EXISTS uk_file_path
    ON files (parent_folder_id, name, is_nfd);