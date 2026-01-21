package org.iptime.raspinas.FSHS.v1.domain.test.controller;

import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.reader.HWPReader;
import org.iptime.raspinas.FSHS.v1.global.exception.CustomException;
import org.iptime.raspinas.FSHS.v1.global.exception.constants.ExceptionCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.UnsupportedEncodingException;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @Value("${user-file.directory.path}")
    private String UserFileDirPath;

    @GetMapping("/connection")
    public String connectionTest(){
        return "Good";
    }

    @GetMapping("/path/{path}")
    public String pathTest(@PathVariable String path){
        System.out.println(path);
        return "hello";
    }

    @GetMapping("/param")
    public String paramTest(@RequestParam String path){
        System.out.println(path);
        return path;
    }

    @GetMapping("/hwp")
    public String hwpTest(){
        HWPFile hwpFile;
        try {
             hwpFile = HWPReader.fromFile(new File(UserFileDirPath+"/1/1e889384bf814c2ebccc46dffe439137.hwp"));
        } catch (Exception e) {
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }


        for(int i = 0; i < hwpFile.getBodyText().getSectionList().size(); i++){
            for(int j = 0; j < hwpFile.getBodyText().getSectionList().get(i).getParagraphCount(); j++){
                try {
                    System.out.println(hwpFile.getBodyText().getSectionList().get(i).getParagraph(j).getNormalString());
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return hwpFile.getSummaryInformation().getTitle();

    }
}
