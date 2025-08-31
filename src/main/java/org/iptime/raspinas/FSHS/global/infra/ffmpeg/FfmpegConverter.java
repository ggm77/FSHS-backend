package org.iptime.raspinas.FSHS.global.infra.ffmpeg;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.progress.Progress;
import org.iptime.raspinas.FSHS.global.exception.CustomException;
import org.iptime.raspinas.FSHS.global.exception.constants.ExceptionCode;
import org.iptime.raspinas.FSHS.global.common.file.FileConverter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class FfmpegConverter implements FileConverter {

    private final FFmpeg ffmpeg;
    private final FFprobe ffprobe;

    private FFmpegExecutor executor() {
        return new FFmpegExecutor(ffmpeg, ffprobe);
    }

    @Override
    @Async
    public void videoToHls(
            final String filePath,
            final String hlsPath,
            final String fileName
    ) {
        final FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(filePath) // 입력 소스
                .overrideOutputFiles(true)
                .addOutput(hlsPath + "/master.m3u8") // 출력 위치
                .setFormat("hls")
                .addExtraArgs("-hls_time", "10") // 10초
                .addExtraArgs("-hls_list_size", "0")
                .addExtraArgs("-hls_segment_filename", hlsPath + "/master_%08d.ts") // 청크 파일 이름
                .done();

        log.info("'{}' HLS conversion started", fileName);
        executor().createJob(builder, progress -> {
            log.info("file:'{}' progress ==> {}", fileName, progress);

            //Successfully completed the operation. | 작업이 정상 종료 되었을 때
            if (progress.status.equals(Progress.Status.END)) {
                log.info("================================= '{}' JOB FINISHED =================================", fileName);
            }
        }).run();
        log.info("'{}' HLS conversion completed", fileName);

        final File complete = new File(hlsPath+"/complete.txt");
        try {
            complete.createNewFile();
        } catch (IOException ex) {
            log.error("FFmpegConfig.convertToHlsVideo message:{}",ex.getMessage(), ex);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Async
    public void audioToHls(
            final String filePath,
            final String hlsPath,
            final String fileName
    ) {
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
            log.info("'{}' HLS conversion started", fileName);
            executor().createJob(builder, progress -> {
                log.info("file:'{}' progress ==> {}", fileName, progress);

                //Successfully completed the operation. | 작업이 정상 종료 되었을 때
                if (progress.status.equals(Progress.Status.END)) {
                    log.info("================================= '{}' JOB FINISHED =================================", fileName);
                }
            }).run();
            log.info("'{}' HLS conversion completed", fileName);
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

    @Override
    public void thumbnail(
            final String filePath,
            final String thumbnailPath
    ){

        final double durationInSeconds;
        try {
            durationInSeconds = ffprobe.probe(filePath).getFormat().duration;
        } catch (IOException ex) {
            throw new CustomException(ExceptionCode.FAILED_TO_GET_MEDIA_INFO);
        }

        final LocalTime timeOfDay = LocalTime.ofSecondOfDay((long) durationInSeconds/2);
        final String halfOfTime = timeOfDay.toString();

        final FFmpegBuilder builder = new FFmpegBuilder()
                .overrideOutputFiles(true)
                .setInput(filePath)
                .addOutput(thumbnailPath)
                .setFrames(1)
                .done();

        executor().createJob(builder).run();
    }

    @Override
    public void albumCoverImage(
            final String filePath,
            final String thumbnailPath,
            final String fileName
    ) {

        final FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(filePath)
                .overrideOutputFiles(true)
                .addOutput(thumbnailPath)
                .addExtraArgs("-an")
                .addExtraArgs("-vcodec", "copy")
                .done();

        log.info("'{}' album cover extraction started", fileName);
        executor().createJob(builder, progress -> {
            log.info("file:'{}' progress ==> {}", fileName, progress);

            //Successfully completed the operation. | 작업이 정상 종료 되었을 때
            if (progress.status.equals(Progress.Status.END)) {
                log.info("================================= '{}' JOB FINISHED =================================", fileName);
            }
        }).run();
        log.info("'{}' album cover extraction completed", fileName);
    }
}
