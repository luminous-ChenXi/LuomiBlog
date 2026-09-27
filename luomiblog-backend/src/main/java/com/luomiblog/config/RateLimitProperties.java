package com.luomiblog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 接口限流阈值（全部配置化，见 application.yml 的 app.rate-limit 段）。
 *
 * <p>默认阈值：</p>
 * <ul>
 *   <li>注册：5 次/小时/IP</li>
 *   <li>注册验证邮件重发：60 秒冷却/邮箱 + 10 次/小时/IP</li>
 *   <li>评论发表：10 次/分钟/用户身份</li>
 *   <li>点赞/浏览量：60 次/分钟/用户身份</li>
 * </ul>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    /** 总开关（关闭后所有限流规则不生效，仅用于本地调试） */
    private boolean enabled = true;

    /** 注册：max-requests 次 / window-seconds 秒 / IP */
    private Rule register = new Rule(5, 3600);

    /** 注册验证邮件重发：冷却 + 频次 */
    private EmailResend emailResend = new EmailResend();

    /** 评论发表：max-requests 次 / window-seconds 秒 / 身份 */
    private Rule comment = new Rule(10, 60);

    /** 点赞/浏览量：max-requests 次 / window-seconds 秒 / 身份 */
    private Rule interaction = new Rule(60, 60);

    @Data
    public static class Rule {
        private int maxRequests;
        private int windowSeconds;

        public Rule() {
        }

        public Rule(int maxRequests, int windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
        }
    }

    @Data
    public static class EmailResend {
        /** 同一邮箱两次发送的最小间隔（秒） */
        private int cooldownSeconds = 60;
        /** 窗口内每 IP 最大发送次数 */
        private int maxRequests = 10;
        /** 频次窗口（秒） */
        private int windowSeconds = 3600;
    }
}
