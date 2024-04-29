package org.iptime.raspinas.FSHS.controller.userFolder;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.dto.userFolder.request.UserFolderRequestDto;
import org.iptime.raspinas.FSHS.dto.userFolder.response.UserFolderResponseDto;
import org.iptime.raspinas.FSHS.dto.userInfo.response.UserInfoResponseDto;
import org.iptime.raspinas.FSHS.entity.userFile.UserFile;
import org.iptime.raspinas.FSHS.security.TokenProvider;
import org.iptime.raspinas.FSHS.service.userFolder.UserFolderCreateService;
import org.iptime.raspinas.FSHS.service.userFolder.UserFolderUpdateService;
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
    private final TokenProvider tokenProvider;

    @PostMapping(value = "/folder")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "폴더 생성 성공"
            )
    })
    public ResponseEntity<?> createUserFolder(
            @RequestHeader(value = "Authorization") final String token,
            @RequestBody final UserFolderRequestDto userFolderRequestDto
    ){

        final String userId = tokenProvider.validate(token.substring(7));
        final Long id = Long.parseLong(userId);

        userFolderCreateService.createUserFolder(userFolderRequestDto, id);

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
            @PathVariable final Long folderId,
            @RequestBody final UserFolderRequestDto userFolderRequestDto
    ){

        final String userId = tokenProvider.validate(token.substring(7));
        final Long id = Long.parseLong(userId);

        final Optional<UserFile> result = userFolderUpdateService.updateUserFolder(userFolderRequestDto, folderId, id);

        return result.map(node -> ResponseEntity.ok(UserFolderResponseDto.fromEntity(node)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
