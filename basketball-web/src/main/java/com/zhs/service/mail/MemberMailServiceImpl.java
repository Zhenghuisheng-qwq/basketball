package com.zhs.service.mail;

import com.zhs.enums.VerifyPurposeEnum;
import com.zhs.exception.BusinessReason;
import com.zhs.exception.ServiceException;
import com.zhs.support.MemberLocaleSupport;
import com.zhs.util.DataMaskUtil;
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

    @Value("${member.mail.from-name:CNPA}")
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
        String from = resolveFrom();
        return StringUtils.hasText(from);
    }

    @Override
    public void sendVerifyCodeMail(String toEmail, String code, String purposeLabel, long expireMinutes) {
        sendVerificationCode(toEmail, code, purposeLabel, expireMinutes, MemberLocaleSupport.ZH_CN);
    }

    @Override
    public void sendVerificationCode(String toEmail, String code, VerifyPurposeEnum scene, long expireMinutes) {
        sendVerificationCode(toEmail, code, scene, expireMinutes, MemberLocaleSupport.ZH_CN);
    }

    @Override
    public void sendVerificationCode(String toEmail, String code, VerifyPurposeEnum scene, long expireMinutes, String locale) {
        sendVerificationCode(toEmail, code, scene == null ? "" : scene.getCode(), expireMinutes, locale);
    }

    private void sendVerificationCode(String toEmail, String code, String scene, long expireMinutes, String locale) {
        String resolvedLocale = MemberLocaleSupport.resolve(locale, null, null);
        if (!isConfigured()) {
            log.error("[member-mail] SMTP not configured, cannot send email to={} scene={}",
                    DataMaskUtil.maskEmail(toEmail), scene);
            throw BusinessReason.VERIFY_CODE_SEND_FAILED.exception();
        }
        JavaMailSender mailSender = javaMailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.error("[member-mail] JavaMailSender missing, cannot send email to={} scene={}",
                    DataMaskUtil.maskEmail(toEmail), scene);
            throw BusinessReason.VERIFY_CODE_SEND_FAILED.exception();
        }
        String from = resolveFrom();
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            if (StringUtils.hasText(mailFromName)) {
                helper.setFrom(from, mailFromName);
            } else {
                helper.setFrom(from);
            }
            helper.setTo(toEmail);
            helper.setSubject(EmailVerificationMailContent.subject(resolvedLocale));
            helper.setText(EmailVerificationMailContent.html(code, expireMinutes, resolvedLocale), true);
            mailSender.send(mimeMessage);
            log.info("[member-mail] verification email sent to={} scene={} locale={}",
                    DataMaskUtil.maskEmail(toEmail), scene, resolvedLocale);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[member-mail] verification email send failed to={} scene={} ex={}",
                    DataMaskUtil.maskEmail(toEmail), scene, e.getClass().getName());
            throw BusinessReason.VERIFY_CODE_SEND_FAILED.exception();
        }
    }

    private String resolveFrom() {
        return StringUtils.hasText(mailFrom) ? mailFrom : mailUsername;
    }
}
