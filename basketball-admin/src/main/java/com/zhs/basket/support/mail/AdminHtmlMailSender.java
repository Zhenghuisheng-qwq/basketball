package com.zhs.basket.support.mail;

import com.zhs.basket.support.BasketDataMaskUtil;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class AdminHtmlMailSender {

    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${cnpa.mail.from:}")
    private String mailFrom;

    @Value("${cnpa.mail.from-name:CNPA}")
    private String mailFromName;

    public AdminHtmlMailSender(ObjectProvider<JavaMailSender> javaMailSenderProvider) {
        this.javaMailSenderProvider = javaMailSenderProvider;
    }

    public boolean isConfigured() {
        if (!StringUtils.hasText(mailHost) || !StringUtils.hasText(mailUsername) || !StringUtils.hasText(mailPassword)) {
            return false;
        }
        if (javaMailSenderProvider.getIfAvailable() == null) {
            return false;
        }
        return StringUtils.hasText(resolveFrom());
    }

    public void sendHtml(String toEmail, String subject, String html) throws Exception {
        JavaMailSender mailSender = javaMailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("JavaMailSender missing");
        }
        String from = resolveFrom();
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
        if (StringUtils.hasText(mailFromName)) {
            helper.setFrom(from, mailFromName);
        } else {
            helper.setFrom(from);
        }
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(mimeMessage);
        log.info("[mail-notice] sent to={}", BasketDataMaskUtil.maskEmail(toEmail));
    }

    private String resolveFrom() {
        return StringUtils.hasText(mailFrom) ? mailFrom : mailUsername;
    }
}
