package com.seohamin.fshs.v2.domain.user.dto;

import com.seohamin.fshs.v2.domain.share.dto.ShareKeyDto;

import java.util.List;

public record UserShareResponseDto(
    List<ShareKeyDto> shares
) { }
