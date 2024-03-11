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
        } catch (IOException ex) {
            throw new CustomException(ExceptionCode.FAILED_TO_GET_MEDIA_INFO);
        }
    }

    public FFprobe ffprobe(){
        try{
            return new FFprobe(probe);
        } catch (IOException ex){
            throw new CustomException(ExceptionCode.FAILED_TO_GET_MEDIA_INFO);
        }
    }

    public FFmpegProbeResult getProbeResult(final String filePath){

        final FFmpegProbeResult ffmpegProbeResult;
        try{
            ffmpegProbeResult = ffprobe().probe(filePath);
        } catch (IOException ex){
            throw new CustomException(ExceptionCode.FAILED_TO_GET_MEDIA_INFO);
        }

        return ffmpegProbeResult;
    }

    public void generateThumbnail(
            final String filePath,
            final String thumbnailPath
    ){

        final double duration = getProbeResult(filePath).getStreams().get(0).duration;
        final LocalTime timeOfDay = LocalTime.ofSecondOfDay((long) duration/2);
        final String halfOfTime = timeOfDay.toString();
        final FFmpegBuilder builder = new FFmpegBuilder()
                .overrideOutputFiles(true)
                .setInput(filePath)
                .addExtraArgs("-ss",halfOfTime)
                .addOutput(thumbnailPath)
                .setFrames(1)
                .done();

        final FFmpegExecutor executor = new FFmpegExecutor(ffmpeg(), ffprobe());
        executor.createJob(builder).run();
    }

    public void getAlbumCoverImage(
            final String filePath,
            final String thumbnailPath
    ){

        final FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(filePath)
                .overrideOutputFiles(true)
                .addOutput(thumbnailPath)
                .addExtraArgs("-an")
                .addExtraArgs("-vcodec", "copy")
                .done();


        final FFmpegExecutor executor = new FFmpegExecutor(ffmpeg(), ffprobe());
        executor.createJob(builder, progress -> {
            log.info("progress ==> {}", progress);

            //Successfully completed the operation. | 작업이 정상 종료 되었을 때
            if (progress.status.equals(Progress.Status.END)) {
                log.info("================================= JOB FINISHED =================================");
            }
        }).run();
    }

    @Async
    public void convertToHlsVideo(
            final String filePath,
            final String hlsPath
    ){

        final FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(filePath) // 입력 소스
                .overrideOutputFiles(true)
                .addOutput(hlsPath + "/master.m3u8") // 출력 위치
                .setFormat("hls")
                .addExtraArgs("-hls_time", "10") // 10초
                .addExtraArgs("-hls_list_size", "0")
                .addExtraArgs("-hls_segment_filename", hlsPath + "/master_%08d.ts") // 청크 파일 이름
                .done();

        final FFmpegExecutor executor = new FFmpegExecutor(ffmpeg(), ffprobe());
        executor.createJob(builder, progress -> {
            log.info("progress ==> {}", progress);

            //Successfully completed the operation. | 작업이 정상 종료 되었을 때
            if (progress.status.equals(Progress.Status.END)) {
                log.info("================================= JOB FINISHED =================================");
            }
        }).run();

        final File complete = new File(hlsPath+"/complete.txt");
        try {
            complete.createNewFile();
        } catch (IOException ex) {
            log.error("FFmpegConfig.convertToHlsVideo message:{}",ex.getMessage(), ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Async
    public void convertToHlsAudio(
            final String filePath,
            final String hlsPath
    ){
        final FFmpegBuilder builder = new FFmpegBuilder()
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

        try {
            final FFmpegExecutor executor = new FFmpegExecutor(ffmpeg(), ffprobe());
            executor.createJob(builder, progress -> {
                log.info("progress ==> {}", progress);

                //Successfully completed the operation. | 작업이 정상 종료 되었을 때
                if (progress.status.equals(Progress.Status.END)) {
                    log.info("================================= JOB FINISHED =================================");
                }
            }).run();
        } catch (IllegalArgumentException ex) {
            log.error("FFmpegConfig.convertToHlsAudio message:{}", ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("FFmpegConfig.convertToHlsAudio message:{}", ex.getMessage(), ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        final File complete = new File(hlsPath+"/complete.txt");
        try {
            complete.createNewFile();
        } catch (IOException ex) {
            log.error("FFmpegConfig.convertToHlsAudio message:{}",ex.getMessage(), ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }
    }

}
