package org.iptime.raspinas.FSHS.userFile.adapter.inbound;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.userFile.application.service.*;
import org.iptime.raspinas.FSHS.userFile.adapter.inbound.dto.UserFileCreateRequestDto;
import org.iptime.raspinas.FSHS.userFile.adapter.inbound.dto.UserFileUpdateRequestDto;
import org.iptime.raspinas.FSHS.userFile.adapter.inbound.dto.UserFileResponseDto;
import org.iptime.raspinas.FSHS.userFile.adapter.inbound.dto.UserFileSimpleResponseDto;
import org.iptime.raspinas.FSHS.userFolder.adapter.inbound.dto.UserFolderResponseDto;
import org.iptime.raspinas.FSHS.userFile.domain.UserFile;
import org.iptime.raspinas.FSHS.auth.adapter.outbound.jwt.TokenProvider;
import org.iptime.raspinas.FSHS.userFolder.application.service.UserFolderReadService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserFileController {

    private final UserFileCreateService userFileCreateService;
    private final UserFileDownloadService userFileDownloadService;
    private final UserFileReadService userFileReadService;
    private final UserFolderReadService userFolderReadService;
    private final UserFileUpdateService userFileUpdateService;
    private final UserFileDeleteService userFileDeleteService;
    private final TokenProvider tokenProvider;

    @PostMapping(value = "/files/{fileId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)//upload file
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "유저 파일 업로드 성공"
            )
    })
    public ResponseEntity<?> createUserFile(
            @PathVariable final Long fileId,
            @RequestPart(value = "files") final List<MultipartFile> multipartFiles,
            @RequestPart(value = "info") final UserFileCreateRequestDto requestDto,
            @RequestHeader(value = "Authorization") final String token
    ){
        final String userId = tokenProvider.validate(token.substring(7));
        final Long id = Long.parseLong(userId);

        final List<UserFile> result = userFileCreateService.createUserFile(fileId, multipartFiles, requestDto, id);


        final URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/files/{id}")
                .buildAndExpand(result.get(0).getId())
                .toUri();
        return ResponseEntity.created(location).build();
    }


    @GetMapping("/files")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "유저 파일 조회 성공",
                    content = @Content(schema = @Schema(implementation = UserFolderResponseDto.class))
            )
    })
    public ResponseEntity<?> getUserAllFile(
            @RequestHeader(value = "Authorization") final String token
    ){
        final String userId = tokenProvider.validate(token.substring(7));
        final Long longUserId = Long.parseLong(userId);

        final Optional<UserFile> root = userFolderReadService.getUserAllFile(longUserId);

        return root.map(node -> ResponseEntity.ok(UserFolderResponseDto.fromEntity(node)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/files/{id}/download")//download file
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "유저 파일 다운로드 성공"
            )
    })
    public ResponseEntity<?> getUserFile(
            @PathVariable final Long id,
            @RequestHeader(value = "Authorization") final String token
    ){
        final String userId = tokenProvider.validate(token.substring(7));
        final Long longUserId = Long.parseLong(userId);

        return userFileDownloadService.downloadUserFile(id, longUserId);
    }


    @GetMapping("/files/{id}")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "유저 파일 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = UserFileResponseDto.class))
            )
    })
    public ResponseEntity<?> getUserFileInfo(
            @PathVariable final Long id,
            @RequestHeader(value = "Authorization") final String token
    ){
        final String userId = tokenProvider.validate(token.substring(7));
        final Long longUserId = Long.parseLong(userId);

        return userFileReadService.readUserFileInfo(longUserId, id);
    }

    @PatchMapping("/files/{id}")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "유저 파일 수정 성공",
                    content = @Content(schema = @Schema(implementation = UserFileSimpleResponseDto.class))
            )
    })
    public ResponseEntity<?> updateUserFile(
            @PathVariable final Long id,
            @RequestHeader(value = "Authorization") final String token,
            @RequestBody final UserFileUpdateRequestDto userFileUpdateRequestDto
    ){

        final String userId = tokenProvider.validate(token.substring(7));
        final Long longUserId = Long.parseLong(userId);

        final UserFileSimpleResponseDto result = userFileUpdateService.updateUserFile(longUserId, id, userFileUpdateRequestDto);

        return ResponseEntity.ok(result);
    }


    @DeleteMapping("/files/{id}")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "유저 파일 삭제 성공"
            )
    })
    public ResponseEntity<?> deleteUserFile(
            @PathVariable final Long id,
            @RequestHeader(value = "Authorization") final String token
    ){

        final Long userId = Long.parseLong(
                tokenProvider.validate(token.substring(7))
        );

        userFileDeleteService.deleteUserFile(id, userId);

        return ResponseEntity.noContent().build();
    }
}
