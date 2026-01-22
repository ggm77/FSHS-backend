package org.iptime.raspinas.FSHS.v2.global.util;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EnumUtilV2 {

    /**
     * String으로 들어온 Enum 값을 특정 Enum 타입으로 변환하는 메서드
     * @param enumClass YOUR_ENUM.class
     * @param value 변환할 문자열
     * @return
     * @param <T> Enum타입으로 변환된 값
     */
    public <T extends Enum<T>> Optional<T> toEnum(final Class<T> enumClass, final String value) {
        if(enumClass == null || value == null){
            return Optional.empty();
        }

        try{
            return Optional.of(Enum.valueOf(enumClass, value));
        } catch(IllegalArgumentException ex){
            return Optional.empty();
        }
    }
}
