package com.zhs.service.mail;

import com.zhs.enums.VerifyPurposeEnum;

/**
 * Official site member email sender.
 */
public interface IMemberMailService {

    /**
     * Send verification email. Missing SMTP config throws a business exception; never fake success.
     */
    void sendVerifyCodeMail(String toEmail, String code, String purposeLabel, long expireMinutes);

    /**
     * Send verification email for an existing scene.
     */
    void sendVerificationCode(String toEmail, String code, VerifyPurposeEnum scene, long expireMinutes);

    /**
     * 按请求语言发送。locale 为空时使用中文。不会因为语言不同而额外发送一封。
     */
    void sendVerificationCode(String toEmail, String code, VerifyPurposeEnum scene, long expireMinutes, String locale);

    /** Whether SMTP host / username / SMTP auth code are configured. */
    boolean isConfigured();
}
