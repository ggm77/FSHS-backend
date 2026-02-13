package com.seohamin.fshs.v2.global.infra.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JsonMapper {

    private final ObjectMapper objectMapper;

    // 문자열로 된 Json을 특정 클래스로 변경
    public <T> T fromJson(
            final String json,
            final Class<T> clazz
    ) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (final JsonProcessingException ex) {
            log.error("[Json 파싱 에러] 클래스: {}, 내용: {}", clazz.getSimpleName(), json, ex);
            throw new CustomException(ExceptionCode.JSON_PARSING_ERROR);
        }
    }

    // 특정 클래스를 Json 문자열로 변환
    public String toJson(final Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (final JsonProcessingException ex) {
            log.error("[Json 직렬화 에러] 대상: {}", object.getClass().getSimpleName(), ex);
            throw new CustomException(ExceptionCode.JSON_PARSING_ERROR);
        }
    }
}
