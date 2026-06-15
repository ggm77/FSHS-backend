package com.seohamin.fshs.v2.domain.transcoding.repository;

import com.seohamin.fshs.v2.domain.transcoding.entity.TranscodingSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranscodingSettingRepository extends JpaRepository<TranscodingSetting, Long> {
}