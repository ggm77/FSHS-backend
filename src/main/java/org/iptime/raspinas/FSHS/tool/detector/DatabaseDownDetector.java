package org.iptime.raspinas.FSHS.tool.detector;

import org.springframework.stereotype.Component;

@Component
public class DatabaseDownDetector {
    public void databaseDown(){
        System.out.println("DATABASE DOWN");
        //shut down
    }
}
