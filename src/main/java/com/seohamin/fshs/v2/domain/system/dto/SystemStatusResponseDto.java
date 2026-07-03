package com.seohamin.fshs.v2.domain.system.dto;

import java.time.Instant;

public record SystemStatusResponseDto(
        Instant timestamp,
        String osName,
        Long uptimeSecond,
        Long cpuIdleTimeMillis,
        Long cpuTotalTimeMillis,
        Integer logicalCoreCount,
        Integer physicalCoreCount,
        Long ramTotalBytes,
        Long ramUsedBytes,
        Long ramAvailableBytes,
        Double ramUsagePercent,
        Long ramSwapTotalBytes,
        Long ramSwapUsedBytes,
        String mountPoint,
        Long diskTotalBytes,
        Long diskUsedBytes,
        Long diskAvailableBytes,
        Double diskUsagePercent,
        String netInterfaceName,
        Long netBytesSent,
        Long netBytesReceived
) { }
