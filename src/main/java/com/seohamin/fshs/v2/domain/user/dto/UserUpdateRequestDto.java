package com.seohamin.fshs.v2.domain.user.dto;

public record UserUpdateRequestDto(
    String username,
    String currentPassword,
    String newPassword
) {}
