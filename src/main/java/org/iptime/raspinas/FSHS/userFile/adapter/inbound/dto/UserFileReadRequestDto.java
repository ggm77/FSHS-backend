package org.iptime.raspinas.FSHS.userFile.adapter.inbound.dto;

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
