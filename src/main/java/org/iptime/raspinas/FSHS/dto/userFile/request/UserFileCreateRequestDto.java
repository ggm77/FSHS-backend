package org.iptime.raspinas.FSHS.dto.userFile.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFileCreateRequestDto {
    private String path; // '/user/~~~'
    private boolean isSecrete;
}
