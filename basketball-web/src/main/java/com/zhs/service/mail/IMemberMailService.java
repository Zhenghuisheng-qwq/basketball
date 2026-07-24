package com.zhs.service.mail;

/**
 * 官网会员邮件发送
 */
public interface IMemberMailService {

    /**
     * 发送验证码邮件。配置缺失时抛出业务异常，禁止伪造成功。
     */
    void sendVerifyCodeMail(String toEmail, String code, String purposeLabel, long expireMinutes);
}
