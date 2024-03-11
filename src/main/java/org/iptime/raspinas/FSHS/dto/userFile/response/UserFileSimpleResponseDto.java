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
    private String originalFileName;
    private String fileName;
    private String fileExtension;
    private Long fileSize;


    public UserFileSimpleResponseDto(final UserFile userFile){
        this.id = userFile.getId();
        this.originalFileName = userFile.getOriginalFileName();
        this.fileName = userFile.getFileName();
        this.fileExtension = userFile.getFileExtension();
        this.fileSize = userFile.getFileSize();
    }
}
