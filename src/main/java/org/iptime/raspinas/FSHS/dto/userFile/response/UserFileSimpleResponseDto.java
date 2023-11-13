package org.iptime.raspinas.FSHS.dto.userFile.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.iptime.raspinas.FSHS.entity.userFile.UserFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFileSimpleResponseDto {
    private Long id;


    public UserFileSimpleResponseDto(UserFile userFile){
        this.id = userFile.getId();
    }
}
