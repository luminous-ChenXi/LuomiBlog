package com.luomiblog.service;

/**
 * 通用接口限流（仿 LoginSecurityService 模式，基于 MemoryCacheService 计数）。
 * 超限统一抛 BusinessException(TOO_MANY_REQUESTS) → HTTP 429。
 */
public interface RateLimitService {

    /**
     * 注册限流：按 IP 限次（默认 5 次/小时）。
     */
    void checkRegister(String clientIp);

    /**
     * 注册验证邮件重发限流：
     * 同一邮箱冷却期内（默认 60 秒）拒绝 + 同一 IP 窗口限次（默认 10 次/小时）。
     */
    void checkEmailResend(String email, String clientIp);

    /**
     * 评论发表限流：按身份（登录用户按 userId，访客按 visitorId）限次（默认 10 次/分钟）。
     */
    void checkComment(String identity);

    /**
     * 点赞/浏览量限流：按身份限次（默认 60 次/分钟）。
     */
    void checkInteraction(String identity);
}
