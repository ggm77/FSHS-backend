package com.seohamin.fshs.v2.domain.system.service;

import com.seohamin.fshs.v2.domain.system.dto.SystemStatusResponseDto;
import com.seohamin.fshs.v2.global.exception.CustomException;
import com.seohamin.fshs.v2.global.exception.constants.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.hardware.VirtualMemory;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemService {

    @Value("${fshs.storage.data-path}")
    private String dataPath;

    private final SystemInfo systemInfo = new SystemInfo();
    private final HardwareAbstractionLayer hardware = systemInfo.getHardware();
    private final OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
    private final CentralProcessor processor = hardware.getProcessor();

    /**
     * 서버 컴퓨터의 상태 조회하는 메서드
     * CPU 사용률은 누적 틱 값만 내려주고, 프론트에서 이전 응답과의 차이로 계산한다.
     * @return 조회된 정보
     */
    public SystemStatusResponseDto getSystemStatus() {

        // 1) CPU 누적 틱 조회 (부팅 이후 누적된 밀리초 단위 값)
        final long[] cpuTicks = processor.getSystemCpuLoadTicks();
        final long cpuIdleTimeMillis = cpuTicks[CentralProcessor.TickType.IDLE.getIndex()];
        final long cpuTotalTimeMillis = Arrays.stream(cpuTicks).sum();

        // 2) 메모리 정보 조회
        final GlobalMemory memory = hardware.getMemory();
        final VirtualMemory virtualMemory = memory.getVirtualMemory();
        final long ramTotal = memory.getTotal();
        final long ramAvailable = memory.getAvailable();
        final long ramUsed = ramTotal - ramAvailable;
        final double ramUsagePercent = ramTotal == 0 ? 0 : (double) ramUsed / ramTotal * 100;

        // 3) 디스크 정보 조회
        final OSFileStore fileStore = findFileStore(dataPath);
        final long diskTotal = fileStore.getTotalSpace();
        final long diskAvailable = fileStore.getUsableSpace();
        final long diskUsed = diskTotal - diskAvailable;
        final double diskUsagePercent = diskTotal == 0 ? 0 : (double) diskUsed / diskTotal * 100;

        // 4) 네트워크 정보 조회
        final NetworkIF networkIF = findPrimaryNetworkIF();

        // 5) DTO에 담아서 리턴
        return new SystemStatusResponseDto(
                Instant.now(),
                operatingSystem.toString(),
                operatingSystem.getSystemUptime(),
                cpuIdleTimeMillis,
                cpuTotalTimeMillis,
                processor.getLogicalProcessorCount(),
                processor.getPhysicalProcessorCount(),
                ramTotal,
                ramUsed,
                ramAvailable,
                ramUsagePercent,
                virtualMemory.getSwapTotal(),
                virtualMemory.getSwapUsed(),
                fileStore.getMount(),
                diskTotal,
                diskUsed,
                diskAvailable,
                diskUsagePercent,
                networkIF.getName(),
                networkIF.getBytesSent(),
                networkIF.getBytesRecv()
        );
    }

    /**
     * 주어진 경로가 속한 파일 스토어를 찾는 메서드
     * @param path 조회할 경로
     * @return 해당 경로가 속한 파일 스토어
     */
    private OSFileStore findFileStore(final String path) {
        return operatingSystem.getFileSystem().getFileStores().stream()
                .filter(store -> path.startsWith(store.getMount()))
                .max(Comparator.comparingInt(store -> store.getMount().length()))
                .orElseThrow(() -> new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR));
    }

    /**
     * 트래픽이 가장 많은(=주로 사용중인) 네트워크 인터페이스를 찾는 메서드
     * @return 조회된 네트워크 인터페이스
     */
    private NetworkIF findPrimaryNetworkIF() {
        final List<NetworkIF> networkIFs = hardware.getNetworkIFs();
        networkIFs.forEach(NetworkIF::updateAttributes);

        return networkIFs.stream()
                .max(Comparator.comparingLong(nif -> nif.getBytesSent() + nif.getBytesRecv()))
                .orElseThrow(() -> new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR));
    }
}