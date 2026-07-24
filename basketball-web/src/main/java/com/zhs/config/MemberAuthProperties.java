package com.zhs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "member.auth")
public class MemberAuthProperties {

    /** Token 过期天数 */
    private long tokenExpireDays = 30;

    /** 验证码有效秒数 */
    private long codeExpireSeconds = 300;

    /** 重发间隔秒数 */
    private long codeResendSeconds = 60;

    /** 同一邮箱每日发送上限 */
    private int codeDailyLimit = 20;

    /** 同一 IP 每小时发送上限 */
    private int codeIpHourlyLimit = 30;

    /** 同一验证码最大错误次数 */
    private int codeMaxFailTimes = 5;

    /** 密码登录连续失败次数上限 */
    private int loginFailMaxTimes = 5;

    /** 密码登录锁定分钟数 */
    private long loginLockMinutes = 15;

    /**
     * 本地开发：跳过真实 SMTP。
     * true 时不发邮件，注册/登录验证码任意 6 位数字可通过。
     * 配好邮箱后请设环境变量 CNPA_MAIL_DEV_BYPASS=false。
     */
    private boolean devMailBypass = true;
}
