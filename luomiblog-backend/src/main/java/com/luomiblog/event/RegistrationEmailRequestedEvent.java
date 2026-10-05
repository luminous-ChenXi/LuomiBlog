package com.luomiblog.event;

/**
 * 注册验证邮件发送请求（BUG-022 修复）：
 * 注册事务提交后由监听器异步发送 SMTP，事务内不再同步阻塞。
 *
 * @param recipientEmail 收件人
 * @param username       用户名（邮件正文展示）
 * @param verifyToken    验证令牌（激活链接参数）
 * @param verifyCode     6 位验证码
 */
public record RegistrationEmailRequestedEvent(
        String recipientEmail,
        String username,
        String verifyToken,
        String verifyCode) {
}
