package com.luomiblog.mail;

import com.luomiblog.event.RegistrationEmailRequestedEvent;
import com.luomiblog.service.MailService;
import com.luomiblog.service.MemoryCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 注册验证邮件异步监听器（BUG-022 修复）：
 * 事务 AFTER_COMMIT 后在 mailExecutor 线程池发送——
 * 注册链路不再被 SMTP 阻塞最长 ~25s 拖住连接池。
 *
 * <p>行为变化说明：邮件发送失败不再回滚注册（异步阶段无法回滚已提交事务），
 * 失败时清理验证缓存并告警，用户可通过限流保护的重发通道（/api/auth/email/resend）重试。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationEmailListener {

    private static final String EMAIL_VERIFY_TOKEN_PREFIX = "emailverify:token:";
    private static final String EMAIL_VERIFY_CODE_PREFIX = "emailverify:code:";

    private final MailService mailService;
    private final MemoryCacheService memoryCacheService;

    /** 前端基础地址（与 AuthServiceImpl 同键，用于拼接激活链接） */
    @Value("${app.base-url:${APP_BASE_URL:http://localhost:4321}}")
    private String baseUrl;

    /** fallbackExecution=true：resend 等非事务上下文也能触发 */
    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRegistrationEmailRequested(RegistrationEmailRequestedEvent event) {
        String siteName = "LuomiBlog";
        String verifyUrl = baseUrl + "/register?verify-token=" + event.verifyToken()
                + "&verify-code=" + event.verifyCode();
        String html = """
                <div style="max-width:520px;margin:0 auto;font-family:Arial,'PingFang SC','Microsoft YaHei',sans-serif;">
                  <h2 style="color:#ff6b9d;">欢迎注册 %s</h2>
                  <p>您好，<b>%s</b>！请使用以下验证码完成邮箱验证（30 分钟内有效）：</p>
                  <p style="font-size:28px;font-weight:bold;letter-spacing:8px;background:#fdf2f7;padding:12px 16px;border-radius:8px;text-align:center;">%s</p>
                  <p>也可以点击下方链接直接激活账号：</p>
                  <p><a href="%s">%s</a></p>
                  <p style="color:#999;font-size:12px;">如果这不是您的操作，请忽略本邮件。</p>
                </div>
                """.formatted(siteName, event.username(), event.verifyCode(), verifyUrl, verifyUrl);

        try {
            mailService.sendMail(event.recipientEmail(),
                    "注册邮箱验证 - " + siteName, html);
            log.info("注册验证邮件已异步发送: {}", event.recipientEmail());
        } catch (Exception e) {
            memoryCacheService.delete(EMAIL_VERIFY_TOKEN_PREFIX + event.verifyToken());
            memoryCacheService.delete(EMAIL_VERIFY_CODE_PREFIX + event.recipientEmail());
            log.error("注册验证邮件异步发送失败（已清理验证缓存，可走重发通道）: {} - {}",
                    event.recipientEmail(), e.getMessage());
        }
    }
}
