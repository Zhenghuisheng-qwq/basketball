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
     * 邮箱验证码 Dev Bypass（由环境变量 CNPA_MAIL_DEV_BYPASS / application 配置控制，禁止业务代码写死）。
     * true：跳过真实 SMTP；任意合法 6 位数字可通过验证码校验；注册/登录仍走正式 Service。
     * false：生成验证码 → SMTP 发送 → 校验真实 codeHash。
     * 公网测试/老板验收可设 true；正式真实用户开放前必须设 false。
     */
    private boolean devMailBypass = true;
}
