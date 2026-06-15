package com.seohamin.fshs.v2.domain.file;

import com.seohamin.fshs.v2.domain.file.controller.FileController;
import com.seohamin.fshs.v2.domain.file.dto.FileDownloadResponseDto;
import com.seohamin.fshs.v2.domain.file.service.FileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileController.class)
@Import(FileControllerTest.AuthenticationPrincipalResolverConfig.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileService fileService;

    private static final String USERNAME = "tester";
    private static final long FILE_SIZE = 10L; // "0123456789"

    @TestConfiguration
    static class AuthenticationPrincipalResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(final List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    private void givenFile(final Long fileId) {
        final byte[] content = "0123456789".getBytes(StandardCharsets.UTF_8);
        final FileDownloadResponseDto dto = new FileDownloadResponseDto(
                "video.mp4", "text/plain", FILE_SIZE, new ByteArrayResource(content));
        given(fileService.getFile(eq(fileId), eq(USERNAME))).willReturn(dto);
    }

    @Test
    @WithMockUser(username = USERNAME)
    @DisplayName("파일 다운로드 : Range 헤더가 없으면 200 전체 응답")
    void getFile_noRange_returns200() throws Exception {
        givenFile(1L);

        mockMvc.perform(get("/api/v2/files/1/content").param("download", "false"))
                .andExpect(status().isOk())
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, FILE_SIZE));
    }

    @Test
    @WithMockUser(username = USERNAME)
    @DisplayName("파일 다운로드 : 정상 Range 는 206 부분 응답 + Content-Range")
    void getFile_validRange_returns206() throws Exception {
        givenFile(1L);

        mockMvc.perform(get("/api/v2/files/1/content")
                        .param("download", "false")
                        .header(HttpHeaders.RANGE, "bytes=0-3"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 0-3/10"));
    }

    @Test
    @WithMockUser(username = USERNAME)
    @DisplayName("파일 다운로드 : 파일 크기를 벗어난 Range 는 416 (500 아님)")
    void getFile_unsatisfiableRange_returns416() throws Exception {
        givenFile(1L);

        mockMvc.perform(get("/api/v2/files/1/content")
                        .param("download", "false")
                        .header(HttpHeaders.RANGE, "bytes=100-"))
                .andExpect(status().isRequestedRangeNotSatisfiable())
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes */10"));
    }

    @Test
    @WithMockUser(username = USERNAME)
    @DisplayName("파일 다운로드 : 형식이 잘못된 Range(역전 구간)는 무시하고 200 (500 아님)")
    void getFile_malformedRange_returns200() throws Exception {
        givenFile(1L);

        mockMvc.perform(get("/api/v2/files/1/content")
                        .param("download", "false")
                        .header(HttpHeaders.RANGE, "bytes=5-2"))
                .andExpect(status().isOk());
    }
}