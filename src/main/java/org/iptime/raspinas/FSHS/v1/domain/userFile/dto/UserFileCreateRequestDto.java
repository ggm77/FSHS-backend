package org.iptime.raspinas.FSHS.v1.domain.userFile.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFileCreateRequestDto {
    private boolean isSecrete;
}
