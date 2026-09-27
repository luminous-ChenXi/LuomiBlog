package com.luomiblog.dto.install;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EnvironmentCheckResponse {
    private boolean allPassed;
    private List<CheckItem> checks;
    private List<String> logs; // 详细日志信息

    @Data
    @Builder
    public static class CheckItem {
        private String name;
        private boolean passed;
        private String message;
        private String suggestion;
        private List<String> details; // 检查详情
        /** 是否为阻塞项（false 时即使未通过也不阻塞安装，如 SMTP 未配置） */
        @Builder.Default
        private boolean blocking = true;
    }
}
