package com.seohamin.fshs.v2.domain.folder.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.seohamin.fshs.v2.domain.file.entity.File;
import com.seohamin.fshs.v2.domain.file.repository.FileRepository;
import com.seohamin.fshs.v2.domain.file.service.FileThumbnailProcessor;
import com.seohamin.fshs.v2.domain.folder.dto.FolderSyncResponseDto;
import com.seohamin.fshs.v2.domain.folder.entity.Folder;
import com.seohamin.fshs.v2.domain.folder.repository.FolderRepository;
import com.seohamin.fshs.v2.domain.user.entity.User;
import com.seohamin.fshs.v2.domain.user.repository.UserRepository;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import com.seohamin.fshs.v2.global.infra.storage.FileAnalyzer;
import com.seohamin.fshs.v2.global.infra.storage.StorageManager;
import com.seohamin.fshs.v2.global.infra.storage.dto.FileAnalysisResultDto;
import com.seohamin.fshs.v2.global.util.storage.PathNameUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class FolderSyncService {

    private static final String MACOS_DIRECTORY_METADATA_FILE = ".DS_Store";

    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final StorageManager storageManager;
    private final FileAnalyzer fileAnalyzer;
    private final Cache<Long, String> filePathCache;
    private final Cache<String, Boolean> fileAccessCache;
    private final FileThumbnailProcessor fileThumbnailProcessor;

    // 부모/자식 폴더 동시 동기화가 겹치면 삭제/생성 판단이 충돌할 수 있어 전역 단일 실행으로 제한한다.
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);

    /**
     * 지정한 폴더와 그 하위 트리의 실제 디스크 상태를 DB 메타데이터와 맞춘다.
     * 디스크의 이름 변경/이동은 안정 식별자가 없으므로 삭제 + 신규 생성으로 반영된다.
     */
    @Transactional
    public FolderSyncResponseDto syncFolder(
            final Long folderId,
            final String username
    ) {
        if (folderId == null || username == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }
        if (!syncInProgress.compareAndSet(false, true)) {
            throw new CustomException(ExceptionCode.SYNC_ALREADY_RUNNING);
        }

        // 트랜잭션 커밋/롤백이 끝나기 전까지 다음 동기화가 시작되지 않도록 락 해제를 지연한다.
        boolean releaseLockOnFinally = true;
        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                releaseLockOnFinally = false;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(final int status) {
                        syncInProgress.set(false);
                    }
                });
            }
            return syncFolderInternal(folderId, username);
        } finally {
            if (releaseLockOnFinally) {
                syncInProgress.set(false);
            }
        }
    }

    private FolderSyncResponseDto syncFolderInternal(
            final Long folderId,
            final String username
    ) {
        // 요청 유저가 접근 가능한 폴더만 동기화한다. 권한 기준은 기존 폴더 API와 동일하다.
        final User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));
        final Folder targetFolder = folderRepository.findById(folderId)
                .orElseThrow(() -> new CustomException(ExceptionCode.FOLDER_NOT_EXIST));
        if (!hasPermission(user, targetFolder)) {
            throw new CustomException(ExceptionCode.INVALID_PATH);
        }

        final String targetRelativePath = targetFolder.getRelativePath();
        final Path targetAbsolutePath = storageManager.resolvePath(targetRelativePath, false);
        if (Files.notExists(targetAbsolutePath) || !Files.isDirectory(targetAbsolutePath)) {
            throw new CustomException(ExceptionCode.PATH_NOT_FOUND);
        }

        // 디스크 스냅샷과 DB 스냅샷을 각각 만든 뒤 차이만 DB에 반영한다.
        final SyncResult result = new SyncResult();
        final DiskSnapshot diskSnapshot = scanDisk(targetRelativePath, targetAbsolutePath);
        final Map<String, Folder> foldersByPath = loadFoldersByPath(targetRelativePath);
        final Map<String, File> filesByPath = loadFilesByPath(targetRelativePath);

        deleteMissingFiles(diskSnapshot, filesByPath, result);
        deleteMissingFolders(targetRelativePath, diskSnapshot, foldersByPath, result);
        createMissingFolders(diskSnapshot, foldersByPath, user, result);
        syncFiles(diskSnapshot, foldersByPath, filesByPath, user, result);

        if (result.hasChanges()) {
            filePathCache.invalidateAll();
            fileAccessCache.invalidateAll();
        }

        return result.toResponse();
    }

    /**
     * 동기화 대상 폴더 아래의 실제 디스크 상태를 storage root 기준 상대 경로로 수집한다.
     */
    private DiskSnapshot scanDisk(
            final String targetRelativePath,
            final Path targetAbsolutePath
    ) {
        final Map<String, Instant> folders = new HashMap<>();
        final Map<String, DiskFile> files = new HashMap<>();

        try (Stream<Path> paths = Files.walk(targetAbsolutePath)) {
            for (final Path path : (Iterable<Path>) paths::iterator) {
                final String relativePath = toStorageRelativePath(targetRelativePath, targetAbsolutePath, path);
                if (Files.isDirectory(path)) {
                    folders.put(relativePath, Files.getLastModifiedTime(path).toInstant());
                } else if (Files.isRegularFile(path) && !isIgnoredFile(path)) {
                    files.put(relativePath, new DiskFile(
                            path,
                            relativePath,
                            Files.size(path),
                            Files.getLastModifiedTime(path).toInstant()
                    ));
                }
            }
        } catch (final UncheckedIOException ex) {
            throw new CustomException(ExceptionCode.FILE_READ_ERROR, ex);
        } catch (final IOException ex) {
            throw new CustomException(ExceptionCode.FILE_READ_ERROR, ex);
        }

        return new DiskSnapshot(folders, files);
    }

    /**
     * OS가 자동 생성하는 폴더 메타데이터 파일은 FSHS 파일 목록에 포함하지 않는다.
     */
    private boolean isIgnoredFile(final Path path) {
        return MACOS_DIRECTORY_METADATA_FILE.equals(path.getFileName().toString());
    }

    /**
     * 대상 폴더 자신과 하위 폴더만 조회한다.
     * 시스템 루트처럼 상대 경로가 빈 문자열이면 전체 storage root 범위를 대상으로 한다.
     */
    private Map<String, Folder> loadFoldersByPath(final String targetRelativePath) {
        final List<Folder> folders;
        if (targetRelativePath.isBlank()) {
            folders = folderRepository.findAll();
        } else {
            folders = new ArrayList<>();
            folderRepository.findByRelativePath(targetRelativePath).ifPresent(folders::add);
            folders.addAll(folderRepository.findAllByRelativePathStartingWith(targetRelativePath + java.io.File.separator));
        }

        final Map<String, Folder> foldersByPath = new HashMap<>();
        folders.forEach(folder -> foldersByPath.put(folder.getRelativePath(), folder));
        return foldersByPath;
    }

    /**
     * 대상 폴더 하위 파일만 조회한다.
     * 파일은 폴더 자신과 같은 경로가 될 수 없으므로 prefix 조회만으로 충분하다.
     */
    private Map<String, File> loadFilesByPath(final String targetRelativePath) {
        final List<File> files = targetRelativePath.isBlank()
                ? fileRepository.findAll()
                : fileRepository.findAllByRelativePathStartingWith(targetRelativePath + java.io.File.separator);

        final Map<String, File> filesByPath = new HashMap<>();
        files.forEach(file -> filesByPath.put(file.getRelativePath(), file));
        return filesByPath;
    }

    /**
     * DB에는 있지만 디스크에는 없는 파일을 DB에서 제거한다.
     * 동기화는 디스크를 기준으로 DB를 맞추는 작업이므로 실제 파일 삭제는 하지 않는다.
     */
    private void deleteMissingFiles(
            final DiskSnapshot diskSnapshot,
            final Map<String, File> filesByPath,
            final SyncResult result
    ) {
        final List<File> filesToDelete = filesByPath.values().stream()
                .filter(file -> !diskSnapshot.files().containsKey(file.getRelativePath()))
                .toList();

        for (final File file : filesToDelete) {
            fileRepository.delete(file);
            filesByPath.remove(file.getRelativePath());
            result.deletedFiles().add(file.getRelativePath());
        }
    }

    /**
     * 디스크에서 사라진 폴더를 DB에서 제거한다.
     * 자식 폴더가 먼저 삭제되도록 깊은 경로부터 처리한다.
     */
    private void deleteMissingFolders(
            final String targetRelativePath,
            final DiskSnapshot diskSnapshot,
            final Map<String, Folder> foldersByPath,
            final SyncResult result
    ) {
        final List<Folder> foldersToDelete = foldersByPath.values().stream()
                .filter(folder -> !folder.getRelativePath().equals(targetRelativePath))
                .filter(folder -> !diskSnapshot.folders().containsKey(folder.getRelativePath()))
                .sorted(Comparator.comparingInt((Folder folder) -> depth(folder.getRelativePath())).reversed())
                .toList();

        for (final Folder folder : foldersToDelete) {
            if (Boolean.TRUE.equals(folder.getIsRoot()) || Boolean.TRUE.equals(folder.getIsSystemRoot())) {
                result.skipped().add(folder.getRelativePath());
                continue;
            }
            folderRepository.delete(folder);
            foldersByPath.remove(folder.getRelativePath());
            result.deletedFolders().add(folder.getRelativePath());
        }
    }

    /**
     * 디스크에는 있지만 DB에는 없는 폴더를 생성한다.
     * 부모 폴더가 먼저 필요하므로 얕은 경로부터 처리한다.
     */
    private void createMissingFolders(
            final DiskSnapshot diskSnapshot,
            final Map<String, Folder> foldersByPath,
            final User user,
            final SyncResult result
    ) {
        final List<String> foldersToCreate = diskSnapshot.folders().keySet().stream()
                .filter(path -> !path.isBlank())
                .filter(path -> !foldersByPath.containsKey(path))
                .sorted(Comparator.comparingInt(this::depth))
                .toList();

        for (final String relativePath : foldersToCreate) {
            final String parentPath = parentPath(relativePath);
            final Folder parentFolder = foldersByPath.get(parentPath);
            if (parentFolder == null) {
                result.errors().add(relativePath + ": parent folder not found");
                continue;
            }

            final Folder folder = Folder.builder()
                    .parentFolder(parentFolder)
                    .ownerId(user.getId())
                    .relativePath(relativePath)
                    .name(fileName(relativePath))
                    .originUpdatedAt(diskSnapshot.folders().get(relativePath))
                    .isRoot(false)
                    .build();
            final Folder savedFolder = folderRepository.save(folder);
            foldersByPath.put(relativePath, savedFolder);
            result.createdFolders().add(relativePath);
        }
    }

    /**
     * 디스크 파일별로 신규 생성 또는 메타데이터 갱신을 수행한다.
     */
    private void syncFiles(
            final DiskSnapshot diskSnapshot,
            final Map<String, Folder> foldersByPath,
            final Map<String, File> filesByPath,
            final User user,
            final SyncResult result
    ) {
        for (final DiskFile diskFile : diskSnapshot.files().values()) {
            final File file = filesByPath.get(diskFile.relativePath());
            if (file == null) {
                createFile(diskFile, foldersByPath, user, filesByPath, result);
            } else if (isChanged(file, diskFile)) {
                updateFile(file, diskFile, result);
            }
        }
    }

    /**
     * 디스크에서 발견한 신규 파일을 분석해 File 엔티티로 저장한다.
     * 분석 실패 파일은 전체 동기화를 중단하지 않고 결과의 errors에 남긴다.
     */
    private void createFile(
            final DiskFile diskFile,
            final Map<String, Folder> foldersByPath,
            final User user,
            final Map<String, File> filesByPath,
            final SyncResult result
    ) {
        final String parentPath = parentPath(diskFile.relativePath());
        final Folder parentFolder = foldersByPath.get(parentPath);
        if (parentFolder == null) {
            result.errors().add(diskFile.relativePath() + ": parent folder not found");
            return;
        }

        try {
            final FileAnalysisResultDto analysisResult = fileAnalyzer.analyzeFile(diskFile.absolutePath());
            log.info("[파일 검증 및 정보 추출 완료]: {}", diskFile.absolutePath());

            final File file = File.builder()
                    .parentFolder(parentFolder)
                    .ownerId(user.getId())
                    .uuid(UUID.randomUUID().toString())
                    .name(analysisResult.name())
                    .baseName(analysisResult.baseName())
                    .extension(analysisResult.extension())
                    .relativePath(diskFile.relativePath())
                    .parentPath(parentPath)
                    .mimeType(analysisResult.mimeType())
                    .size(analysisResult.size())
                    .videoCodec(analysisResult.videoCodec())
                    .audioCodec(analysisResult.audioCodec())
                    .width(analysisResult.width())
                    .height(analysisResult.height())
                    .duration(analysisResult.duration())
                    .bitrate(analysisResult.bitrate())
                    .orientation(analysisResult.orientation())
                    .lat(analysisResult.lat())
                    .lon(analysisResult.lon())
                    .fps(analysisResult.fps())
                    .format(analysisResult.format())
                    .capturedAt(analysisResult.capturedAt())
                    .originUpdatedAt(diskFile.lastModified())
                    .category(analysisResult.category())
                    .build();
            final File savedFile = fileRepository.save(file);
            log.info("[파일 정보 DB에 저장 완료]: {}", file.getUuid());

            filesByPath.put(diskFile.relativePath(), savedFile);
            result.createdFiles().add(diskFile.relativePath());

            // 섬네일 생성
            try {
                fileThumbnailProcessor.process(file.getUuid(), file.getRelativePath(), analysisResult.category());
            } catch (final TaskRejectedException ex) {
                log.warn("[썸네일 생성 큐 초과]: {}", file.getUuid(), ex);
            }

        } catch (final CustomException ex) {
            result.errors().add(diskFile.relativePath() + ": " + ex.getExceptionCode().name());
        }
    }

    /**
     * 이미 DB에 있는 파일의 크기/수정 시점이 달라졌을 때 메타데이터를 다시 분석해 갱신한다.
     */
    private void updateFile(
            final File file,
            final DiskFile diskFile,
            final SyncResult result
    ) {
        try {
            final FileAnalysisResultDto analysisResult = fileAnalyzer.analyzeFile(diskFile.absolutePath());
            applyAnalysis(file, analysisResult, diskFile.lastModified());
            result.updatedFiles().add(diskFile.relativePath());
        } catch (final CustomException ex) {
            result.errors().add(diskFile.relativePath() + ": " + ex.getExceptionCode().name());
        }
    }

    /**
     * FileAnalyzer 결과를 기존 엔티티에 반영한다.
     * relativePath와 parentPath는 같은 경로의 파일 갱신이므로 변경하지 않는다.
     */
    private void applyAnalysis(
            final File file,
            final FileAnalysisResultDto analysisResult,
            final Instant lastModified
    ) {
        file.updateName(analysisResult.name());
        file.updateBaseName(analysisResult.baseName());
        file.updateExtension(analysisResult.extension());
        file.updateMimeType(analysisResult.mimeType());
        file.updateSize(analysisResult.size());
        file.updateVideoCodec(analysisResult.videoCodec());
        file.updateAudioCodec(analysisResult.audioCodec());
        file.updateWidth(analysisResult.width());
        file.updateHeight(analysisResult.height());
        file.updateDuration(analysisResult.duration());
        file.updateBitrate(analysisResult.bitrate());
        file.updateOrientation(analysisResult.orientation());
        file.updateLat(analysisResult.lat());
        file.updateLon(analysisResult.lon());
        file.updateFps(analysisResult.fps());
        file.updateFormat(analysisResult.format());
        file.updateCapturedAt(analysisResult.capturedAt());
        file.updateOriginUpdatedAt(lastModified);
        file.updateCategory(analysisResult.category());
    }

    /**
     * 파일 내용 변경 여부는 파일 크기와 원본 수정 시점을 기준으로 판단한다.
     */
    private boolean isChanged(
            final File file,
            final DiskFile diskFile
    ) {
        return !file.getSize().equals(diskFile.size())
                || file.getOriginUpdatedAt().toEpochMilli() != diskFile.lastModified().toEpochMilli();
    }

    /**
     * Files.walk로 얻은 절대 경로를 DB에서 쓰는 storage root 기준 상대 경로로 변환한다.
     */
    private String toStorageRelativePath(
            final String targetRelativePath,
            final Path targetAbsolutePath,
            final Path path
    ) {
        final Path localPath = targetAbsolutePath.relativize(path);
        if (localPath.toString().isBlank()) {
            return PathNameUtil.normalize(targetRelativePath);
        }
        final Path relativePath = targetRelativePath.isBlank()
                ? localPath
                : Path.of(targetRelativePath).resolve(localPath);
        return PathNameUtil.normalizePath(relativePath).toString();
    }

    /**
     * storage root 직하 항목은 부모 경로를 빈 문자열로 표현한다.
     */
    private String parentPath(final String relativePath) {
        final Path parentPath = Path.of(relativePath).getParent();
        return parentPath == null ? "" : parentPath.toString();
    }

    /**
     * 경로의 마지막 이름 성분을 NFC 정규화된 DB 이름으로 변환한다.
     */
    private String fileName(final String relativePath) {
        return PathNameUtil.normalize(PathNameUtil.extractFileNameFromPath(Path.of(relativePath)));
    }

    /**
     * 삭제/생성 순서 정렬에 사용할 경로 깊이를 계산한다.
     */
    private int depth(final String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return 0;
        }
        return Path.of(relativePath).getNameCount();
    }

    /**
     * 유저 루트 폴더 하위 경로인지 확인한다.
     * 루트가 시스템 루트이면 모든 storage 경로 접근을 허용한다.
     */
    private boolean hasPermission(final User user, final Folder folder) {
        final Folder userRootFolder = user.getRootFolder();
        if (userRootFolder == null) {
            throw new CustomException(ExceptionCode.ROOT_NOT_EXIST);
        }
        final String userRootPath = userRootFolder.getRelativePath();
        if (userRootPath.isEmpty()) return true;
        final Path userRootFolderPath = Path.of(userRootPath).normalize();
        final Path folderPath = Path.of(folder.getRelativePath()).normalize();
        return folderPath.startsWith(userRootFolderPath);
    }

    /**
     * 디스크 스캔 결과. key는 모두 storage root 기준 상대 경로다.
     */
    private record DiskSnapshot(
            Map<String, Instant> folders,
            Map<String, DiskFile> files
    ) {
    }

    /**
     * 디스크 파일 하나의 실제 경로와 DB 비교에 필요한 속성.
     */
    private record DiskFile(
            Path absolutePath,
            String relativePath,
            Long size,
            Instant lastModified
    ) {
    }

    /**
     * 동기화 처리 결과를 누적해 API 응답 DTO로 변환한다.
     */
    private record SyncResult(
            List<String> createdFolders,
            List<String> createdFiles,
            List<String> updatedFiles,
            List<String> deletedFolders,
            List<String> deletedFiles,
            List<String> skipped,
            List<String> errors
    ) {
        SyncResult() {
            this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                    new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        boolean hasChanges() {
            return !createdFolders.isEmpty()
                    || !createdFiles.isEmpty()
                    || !updatedFiles.isEmpty()
                    || !deletedFolders.isEmpty()
                    || !deletedFiles.isEmpty();
        }

        FolderSyncResponseDto toResponse() {
            return new FolderSyncResponseDto(
                    List.copyOf(createdFolders),
                    List.copyOf(createdFiles),
                    List.copyOf(updatedFiles),
                    List.copyOf(deletedFolders),
                    List.copyOf(deletedFiles),
                    List.copyOf(skipped),
                    List.copyOf(errors)
            );
        }
    }
}
