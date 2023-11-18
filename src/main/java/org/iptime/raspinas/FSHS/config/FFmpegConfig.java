package org.iptime.raspinas.FSHS.config;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.iptime.raspinas.FSHS.exception.CustomException;
import org.iptime.raspinas.FSHS.exception.constants.ExceptionCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.*;
import java.time.LocalTime;

@Configuration
public class FFmpegConfig {

    @Value("${ffmpeg.path.mpeg}")
    private String mpeg;

    @Value("${ffmpeg.path.probe}")
    private String probe;

    public FFmpeg ffmpeg(){
        try {
            return new FFmpeg(mpeg);
        } catch (IOException e) {
            throw new CustomException(ExceptionCode.FAILED_TO_GET_MEDIA_INFO);
        }
    }

    public FFprobe ffprobe(){
        try{
            return new FFprobe(probe);
        } catch (IOException e){
            throw new CustomException(ExceptionCode.FAILED_TO_GET_MEDIA_INFO);
        }
    }

    public FFmpegProbeResult getProbeResult(String filePath){
        FFmpegProbeResult ffmpegProbeResult;
        try{
            ffmpegProbeResult = ffprobe().probe(filePath);
        } catch (IOException e){
            throw new CustomException(ExceptionCode.FAILED_TO_GET_MEDIA_INFO);
        }
        return ffmpegProbeResult;
    }

    public void generateThumbnail(String filePath, String thumbnailPath){
        double duration = getProbeResult(filePath).getStreams().get(0).duration;
        LocalTime timeOfDay = LocalTime.ofSecondOfDay((long) duration/2);
        String halfOfTime = timeOfDay.toString();
        FFmpegBuilder builder = new FFmpegBuilder()
                .overrideOutputFiles(true)
                .setInput(filePath)
                .addExtraArgs("-ss",halfOfTime)
                .addOutput(thumbnailPath)
                .setFrames(1)
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg(), ffprobe());
        executor.createJob(builder).run();
    }

    public void getAlbumCoverImage(String filePath, String thumbnailPath){

        String command = "ffmpeg -i " + filePath + " -an -vcodec copy "+thumbnailPath;
        try {
            Process process = Runtime.getRuntime().exec(command);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new CustomException(ExceptionCode.FAILED_TO_GENERATE_THUMBNAIL);
            }
        } catch (IOException | InterruptedException e) {
            throw new CustomException(ExceptionCode.FAILED_TO_GENERATE_THUMBNAIL);
        }
    }

}
