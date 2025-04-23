package org.iptime.raspinas.FSHS.common.util.detector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DatabaseDownDetector {
    public void databaseDown(){
        log.error("[!=====DATABASE DOWN=====!]");
        //shut down
    }
}
