package org.iptime.raspinas.FSHS.dto.userFile.request;

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
