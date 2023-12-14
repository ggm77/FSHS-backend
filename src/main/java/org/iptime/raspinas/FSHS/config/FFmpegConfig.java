package org.iptime.raspinas.FSHS.config;

import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.progress.Progress;
import org.iptime.raspinas.FSHS.exception.CustomException;
import org.iptime.raspinas.FSHS.exception.constants.ExceptionCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;

import java.io.*;
import java.time.LocalTime;

@Slf4j
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

    @Async
    public void convertToHlsVideo(String filePath, String hlsPath){
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(filePath) // 입력 소스
                .overrideOutputFiles(true)
                .addOutput(hlsPath + "/master.m3u8") // 출력 위치
                .setFormat("hls")
                .addExtraArgs("-hls_time", "10") // 10초
                .addExtraArgs("-hls_list_size", "0")
                .addExtraArgs("-hls_segment_filename", hlsPath + "/master_%08d.ts") // 청크 파일 이름
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg(), ffprobe());
        executor.createJob(builder, progress -> {
            log.info("progress ==> {}", progress);
            if (progress.status.equals(Progress.Status.END)) {
                log.info("================================= JOB FINISHED =================================");
            }
        }).run();
        File complete = new File(hlsPath+"/complete.txt");
        try {
            complete.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Async
    public void convertToHlsAudio(String filePath, String hlsPath){
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(filePath) // 입력 소스
                .overrideOutputFiles(true)
                .addOutput(hlsPath + "/master.m3u8") // 출력 위치
                .setFormat("hls")
                .setAudioCodec("aac")
                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL) // 실험적인 옵션을 허용
                .addExtraArgs("-map","0:a")
                .addExtraArgs("-c:a","aac")
//                .addExtraArgs("-b:a","320k")
                .addExtraArgs("-hls_time", "10") // 10초
                .addExtraArgs("-hls_list_size", "0")
                .addExtraArgs("-hls_segment_filename", hlsPath + "/master_%08d.ts") // 청크 파일 이름
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg(), ffprobe());
        executor.createJob(builder, progress -> {
            log.info("progress ==> {}", progress);
            if (progress.status.equals(Progress.Status.END)) {
                log.info("================================= JOB FINISHED =================================");
            }
        }).run();
        File complete = new File(hlsPath+"/complete.txt");
        try {
            complete.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }
    }

}
