package org.iptime.raspinas.FSHS.userFolder.adapter.inbound.rest;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.userFolder.adapter.inbound.dto.UserFolderRequestDto;
import org.iptime.raspinas.FSHS.userFolder.adapter.inbound.dto.UserFolderResponseDto;
import org.iptime.raspinas.FSHS.userFile.domain.UserFile;
import org.iptime.raspinas.FSHS.auth.adapter.outbound.jwt.TokenProvider;
import org.iptime.raspinas.FSHS.userFolder.application.service.UserFolderCreateService;
import org.iptime.raspinas.FSHS.userFolder.application.service.UserFolderDeleteService;
import org.iptime.raspinas.FSHS.userFolder.application.service.UserFolderUpdateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserFolderController {

    private final UserFolderCreateService userFolderCreateService;
    private final UserFolderUpdateService userFolderUpdateService;
    private final UserFolderDeleteService userFolderDeleteService;
    private final TokenProvider tokenProvider;

    @PostMapping(value = "/folder/{fileId}")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "폴더 생성 성공"
            )
    })
    public ResponseEntity<?> createUserFolder(
            @RequestHeader(value = "Authorization") final String token,
            @PathVariable final Long fileId,//부모 폴더 아이디
            @RequestBody final UserFolderRequestDto userFolderRequestDto
    ){

        final String userId = tokenProvider.validate(token.substring(7));
        final Long id = Long.parseLong(userId);

        userFolderCreateService.createUserFolder(fileId, id, userFolderRequestDto);

        final URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/files")
                .build()
                .toUri();

        return ResponseEntity.created(location).build();


    }

    @PatchMapping(value = "/folder/{folderId}")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "폴더 업데이트 성공",
                    content = @Content(schema = @Schema(implementation = UserFolderRequestDto.class))
            )
    })
    public ResponseEntity<?> updateUserFolder(
            @RequestHeader(value = "Authorization") final String token,
            @PathVariable final Long folderId,//고칠 폴더의 아이디
            @RequestBody final UserFolderRequestDto userFolderRequestDto
    ){

        final String userId = tokenProvider.validate(token.substring(7));
        final Long id = Long.parseLong(userId);

        final Optional<UserFile> result = userFolderUpdateService.updateUserFolder(folderId, id, userFolderRequestDto);

        return result.map(node -> ResponseEntity.ok(UserFolderResponseDto.fromEntity(node)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping(value = "/folder/{folderId}")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "폴더 삭제 성공"
            )
    })
    public ResponseEntity<?> deleteUserFolder(
            @RequestHeader(value = "Authorization") final String token,
            @PathVariable final Long folderId
    ){

        final String userId = tokenProvider.validate(token.substring(7));
        final Long id = Long.parseLong(userId);

        userFolderDeleteService.deleteUserFolder(folderId, id);

        return ResponseEntity.noContent().build();
    }
}
