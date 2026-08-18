package com.jobbot.module.platform;

import com.jobbot.common.ApiResponse;
import com.jobbot.module.platform.dto.PlatformConfigUpdateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform-config")
@RequiredArgsConstructor
public class PlatformConfigController {

    private final PlatformConfigService service;

    @GetMapping
    public ApiResponse<List<PlatformConfig>> list() {
        return ApiResponse.ok(service.getAll());
    }

    @GetMapping("/{platform}")
    public ApiResponse<PlatformConfig> get(@PathVariable String platform) {
        return ApiResponse.ok(service.get(platform));
    }

    @PutMapping("/{platform}")
    public ApiResponse<PlatformConfig> update(@PathVariable String platform,
                                              @RequestBody PlatformConfigUpdateDTO dto) {
        return ApiResponse.ok(service.update(platform, dto), "Updated " + platform);
    }

    @PostMapping("/{platform}/pause")
    public ApiResponse<PlatformConfig> pause(@PathVariable String platform) {
        return ApiResponse.ok(service.pause(platform), "Paused " + platform);
    }

    @PostMapping("/{platform}/resume")
    public ApiResponse<PlatformConfig> resume(@PathVariable String platform) {
        return ApiResponse.ok(service.resume(platform), "Resumed " + platform);
    }

    @PostMapping("/{platform}/reset-count")
    public ApiResponse<PlatformConfig> resetCount(@PathVariable String platform) {
        return ApiResponse.ok(service.resetCount(platform), "Reset count " + platform);
    }
}

