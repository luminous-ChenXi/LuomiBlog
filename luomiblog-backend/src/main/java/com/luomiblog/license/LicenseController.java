package com.luomiblog.license;

import com.luomiblog.common.ApiResponse;
import com.luomiblog.license.dto.LicenseStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 授权状态接口（N2 占位）
 *
 * 仅供前端横幅读取展示状态；无论状态如何，本站功能与数据都不受限制。
 */
@RestController
@RequestMapping("/api/license")
@RequiredArgsConstructor
public class LicenseController {

    private final LicenseClient licenseClient;

    @GetMapping("/status")
    public ApiResponse<LicenseStatusResponse> status() {
        return ApiResponse.success(licenseClient.getStatus());
    }
}
