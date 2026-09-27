package com.luomiblog.dto.install;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InstallStatusResponse {
    private boolean installed;
    private boolean locked;
    private boolean hasData;
    /** 是否存在未完成的半安装过程标记（装库中断等），前端据此提供"重置安装状态"入口 */
    @Builder.Default
    private boolean inProgress = false;
    private String message;
}
