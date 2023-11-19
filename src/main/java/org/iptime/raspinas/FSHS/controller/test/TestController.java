package org.iptime.raspinas.FSHS.controller.test;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {

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
}
