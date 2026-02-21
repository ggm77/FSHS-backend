package com.seohamin.fshs.v2.global.infra.ffmpeg.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public record FfmpegAnalysisResultDto(
        List<FfprobeStream> streams,
        FfprobeFormat format
) {
    public String getVideoCodecs() {

        if (streams == null) {
            return "none";
        }

        final String result = streams.stream()
                .filter(FfprobeStream::isVideo)
                .map(FfprobeStream::codec_name)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining("|"));

        if (result.isBlank()) {
            return "none";
        } else  {
            return result;
        }
    }

    public String getAudioCodecs() {

        if (streams == null) {
            return "none";
        }

        final String result = streams.stream()
                .filter(FfprobeStream::isAudio)
                .map(FfprobeStream::codec_name)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining("|"));

        if (result.isBlank()) {
            return "none";
        } else  {
            return result;
        }
    }

    public String getLocation() {

        if (format == null || format.tags() == null) {
            return null;
        }

        if (format.tags.location() != null) {
            return format.tags.location();
        }

        final Map<String, String> dynamic = format.tags.dynamicTags();
        if (dynamic == null ) {
            return null;
        }

        return dynamic.getOrDefault("com.apple.quicktime.location.ISO6709",
                dynamic.get("location-eng"));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FfprobeStream(
            int index,
            String codec_type,
            String codec_name,
            Integer width,
            Integer height,
            String display_aspect_ratio,
            String r_frame_rate,
            String avg_frame_rate,
            Integer channels,
            String sample_rate,
            FfprobeDisposition disposition,
            StreamTags tags
    ) {

        public boolean isVideo() {
            return "video".equalsIgnoreCase(codec_type) && !isThumbnail();
        }

        public boolean isAudio() {
            return "audio".equalsIgnoreCase(codec_type);
        }

        public boolean isThumbnail() {
            return disposition != null && disposition.isAttachedPic();
        }

        public boolean isDefault() {
            return disposition != null && disposition.isDefault();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FfprobeDisposition(
            //default 예약어 때문
            @JsonProperty("default")
            int default_flag,

            int attached_pic
    ) {
        public boolean isDefault() {
            return default_flag == 1;
        }

        public boolean isAttachedPic() {
            return attached_pic == 1;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FfprobeFormat(
            String format_name,
            String duration,
            String bit_rate,
            FormatTags tags
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StreamTags(
            String rotate,
            String language
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FormatTags(
            String creation_time,
            String location,
            @JsonAnySetter
            Map<String, String> dynamicTags
    ) {}
}
