package org.iptime.raspinas.FSHS.domain.userFile.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFileReadRequestDto {
    private List<Long> idList;
}
