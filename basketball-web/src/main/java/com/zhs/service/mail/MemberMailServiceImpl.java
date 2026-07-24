package com.zhs.service.mail;

import com.zhs.exception.ServiceException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class MemberMailServiceImpl implements IMemberMailService {

    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${member.mail.from:}")
    private String mailFrom;

    @Value("${member.mail.from-name:CNPA赛事平台}")
    private String mailFromName;

    public MemberMailServiceImpl(ObjectProvider<JavaMailSender> javaMailSenderProvider) {
        this.javaMailSenderProvider = javaMailSenderProvider;
    }

    @Override
    public boolean isConfigured() {
        if (!StringUtils.hasText(mailHost) || !StringUtils.hasText(mailUsername)
                || !StringUtils.hasText(mailPassword)) {
            return false;
        }
        if (javaMailSenderProvider.getIfAvailable() == null) {
            return false;
        }
        String from = StringUtils.hasText(mailFrom) ? mailFrom : mailUsername;
        return StringUtils.hasText(from);
    }

    @Override
    public void sendVerifyCodeMail(String toEmail, String code, String purposeLabel, long expireMinutes) {
        if (!isConfigured()) {
            throw new ServiceException("邮箱服务未配置");
        }
        JavaMailSender mailSender = javaMailSenderProvider.getIfAvailable();
        String from = StringUtils.hasText(mailFrom) ? mailFrom : mailUsername;

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            if (StringUtils.hasText(mailFromName)) {
                helper.setFrom(from, mailFromName);
            } else {
                helper.setFrom(from);
            }
            helper.setTo(toEmail);
            helper.setSubject("CNPA 邮箱验证码");
            helper.setText(buildText(code, purposeLabel, expireMinutes), false);
            mailSender.send(mimeMessage);
            log.info("[member-mail] 验证码邮件已发送 to={} purpose={}", maskEmail(toEmail), purposeLabel);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[member-mail] 验证码邮件发送失败 to={} purpose={}", maskEmail(toEmail), purposeLabel, e);
            throw new ServiceException("验证码发送失败，请稍后重试");
        }
    }

    private String buildText(String code, String purposeLabel, long expireMinutes) {
        return "【CNPA】您正在进行" + purposeLabel + "操作，验证码为：" + code
                + "，" + expireMinutes + " 分钟内有效。"
                + "如非本人操作，请忽略本邮件。";
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String visible = local.length() <= 2 ? local.charAt(0) + "*" : local.substring(0, 2) + "***";
        return visible + "@" + parts[1];
    }
}
