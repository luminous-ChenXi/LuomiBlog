package com.luomiblog.service;

/**
 * 邮件服务：基于站点设置中的 SMTP 配置发送邮件
 */
public interface MailService {

    /**
     * SMTP 是否已配置（host + username 非空）
     */
    boolean isConfigured();

    /**
     * 使用已保存的站点 SMTP 配置发送 HTML 邮件。
     *
     * @param to      收件人
     * @param subject 主题
     * @param html    HTML 正文
     * @throws Exception 发送失败（连接/认证等），由调用方转为业务提示
     */
    void sendMail(String to, String subject, String html) throws Exception;

    /**
     * 使用显式 SMTP 配置发送（安装向导/测试发送场景，配置可尚未保存）。
     *
     * @param config smtp.host/port/username/password/ssl/from
     */
    void sendMailWithConfig(String to, String subject, String html, SmtpConfig config) throws Exception;

    /** SMTP 配置载体 */
    record SmtpConfig(String host, int port, String username, String password, boolean ssl, String from) {
    }
}
