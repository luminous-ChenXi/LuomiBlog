package com.luomiblog.license.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 授权校验请求（N2 占位，协议只定义到接口层）
 *
 * instanceId 为本站实例指纹（主机名 + 端口的 SHA-256 摘要前 32 位），
 * 服务端据此区分多实例部署。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseVerifyRequest {

    /** 实例指纹（主机名 + 端口派生，同一部署稳定不变） */
    private String instanceId;

    /** 站点名称（安装向导中填写） */
    private String siteName;

    /** 后端版本号 */
    private String version;
}
