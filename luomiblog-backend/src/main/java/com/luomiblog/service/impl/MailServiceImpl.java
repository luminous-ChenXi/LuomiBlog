package com.luomiblog.service.impl;

import com.luomiblog.service.MailService;
import com.luomiblog.service.SiteSettingsService;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * 邮件服务实现：jakarta.mail + angus-mail，全部读取站点设置中的 SMTP 配置
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final SiteSettingsService siteSettingsService;

    @Override
    public boolean isConfigured() {
        return siteSettingsService.isSmtpConfigured();
    }

    @Override
    public void sendMail(String to, String subject, String html) throws Exception {
        SmtpConfig config = new SmtpConfig(
                siteSettingsService.getString(SiteSettingsService.KEY_SMTP_HOST, ""),
                siteSettingsService.getInt(SiteSettingsService.KEY_SMTP_PORT, 587),
                siteSettingsService.getString(SiteSettingsService.KEY_SMTP_USERNAME, ""),
                siteSettingsService.getString(SiteSettingsService.KEY_SMTP_PASSWORD, ""),
                siteSettingsService.getBool(SiteSettingsService.KEY_SMTP_SSL, true),
                siteSettingsService.getString(SiteSettingsService.KEY_SMTP_FROM, "")
        );
        sendMailWithConfig(to, subject, html, config);
    }

    @Override
    public void sendMailWithConfig(String to, String subject, String html, SmtpConfig config) throws Exception {
        if (config == null || config.host() == null || config.host().isBlank()) {
            throw new IllegalStateException("SMTP 服务器未配置");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.writetimeout", "15000");
        if (config.ssl()) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }
        props.put("mail.smtp.host", config.host());
        props.put("mail.smtp.port", String.valueOf(config.port()));

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.username(), config.password());
            }
        });

        MimeMessage message = new MimeMessage(session);
        String from = (config.from() == null || config.from().isBlank())
                ? config.username() : config.from();
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject, "UTF-8");
        message.setContent(html, "text/html;charset=UTF-8");
        message.setSentDate(new java.util.Date());

        try (Transport transport = session.getTransport("smtp")) {
            transport.connect(config.host(), config.port(), config.username(), config.password());
            transport.sendMessage(message, message.getAllRecipients());
        }

        log.info("邮件发送成功: to={}, subject={}, smtp={}:{}", to, subject, config.host(), config.port());
    }
}
