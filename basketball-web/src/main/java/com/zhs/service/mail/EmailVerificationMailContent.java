package com.zhs.service.mail;

import com.zhs.support.MemberLocaleSupport;

/**
 * 验证码邮件。验证码原文不变，只切换标题和说明文字。
 */
public final class EmailVerificationMailContent {

    private EmailVerificationMailContent() {
    }

    public static String subject(String locale) {
        if (MemberLocaleSupport.isEnglish(locale)) {
            return "[CNPA] Email verification code";
        }
        return "【CNPA】邮箱验证码";
    }

    public static String html(String code, long expireMinutes, String locale) {
        String safeCode = escape(code);
        long minutes = expireMinutes > 0 ? expireMinutes : 5;
        if (MemberLocaleSupport.isEnglish(locale)) {
            return shell(
                    "CNPA email verification",
                    "Your verification code:",
                    safeCode,
                    "This code expires in " + minutes + " minutes. Do not share it.",
                    "If you did not request this email, you can ignore it.");
        }
        return shell(
                "CNPA 邮箱验证",
                "您的验证码：",
                safeCode,
                "验证码 " + minutes + " 分钟内有效，请勿泄露给他人。",
                "如果不是您本人操作，请忽略本邮件。");
    }

    private static String shell(String banner, String lead, String safeCode, String expiry, String ignore) {
        return "<!DOCTYPE html>"
                + "<html><body style=\"margin:0;padding:0;background:#f5f5f5;font-family:Arial,Helvetica,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f5f5f5;padding:24px 0;\">"
                + "<tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"480\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background:#ffffff;border:1px solid #e5e5e5;max-width:480px;width:100%;\">"
                + "<tr><td style=\"background:#111111;color:#ffffff;font-size:16px;font-weight:700;"
                + "letter-spacing:2px;padding:16px 24px;\">" + banner + "</td></tr>"
                + "<tr><td style=\"padding:28px 24px 32px 24px;color:#333333;\">"
                + "<p style=\"margin:0 0 16px 0;font-size:14px;line-height:1.6;\">" + lead + "</p>"
                + "<p style=\"margin:0 0 20px 0;color:#c8102e;font-size:32px;font-weight:700;"
                + "letter-spacing:8px;line-height:1.2;\">" + safeCode + "</p>"
                + "<p style=\"margin:0 0 8px 0;font-size:13px;color:#555555;line-height:1.6;\">"
                + expiry + "</p>"
                + "<p style=\"margin:0;font-size:13px;color:#888888;line-height:1.6;\">"
                + ignore + "</p>"
                + "</td></tr></table></td></tr></table></body></html>";
    }

    static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
