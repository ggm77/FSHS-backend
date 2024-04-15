package org.iptime.raspinas.FSHS.controller.userFolder;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.iptime.raspinas.FSHS.dto.userFolder.request.UserFolderRequestDto;
import org.iptime.raspinas.FSHS.security.TokenProvider;
import org.iptime.raspinas.FSHS.service.userFolder.UserFolderCreateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserFolderController {

    private final UserFolderCreateService userFolderCreateService;
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
}
