package com.zhs.service.mail;

/**
 * CNPA verification-code email content. No password / token / internal id.
 */
public final class EmailVerificationMailContent {

    public static final String SUBJECT = "\u3010CNPA\u3011\u90ae\u7bb1\u9a8c\u8bc1\u7801";

    private EmailVerificationMailContent() {
    }

    public static String html(String code, long expireMinutes) {
        String safeCode = escape(code);
        long minutes = expireMinutes > 0 ? expireMinutes : 5;
        return "<!DOCTYPE html>"
                + "<html><body style=\"margin:0;padding:0;background:#f5f5f5;font-family:Arial,Helvetica,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f5f5f5;padding:24px 0;\">"
                + "<tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"480\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background:#ffffff;border:1px solid #e5e5e5;max-width:480px;width:100%;\">"
                + "<tr><td style=\"background:#111111;color:#ffffff;font-size:16px;font-weight:700;"
                + "letter-spacing:2px;padding:16px 24px;\">CNPA \u90ae\u7bb1\u9a8c\u8bc1</td></tr>"
                + "<tr><td style=\"padding:28px 24px 32px 24px;color:#333333;\">"
                + "<p style=\"margin:0 0 16px 0;font-size:14px;line-height:1.6;\">\u60a8\u7684\u9a8c\u8bc1\u7801\uff1a</p>"
                + "<p style=\"margin:0 0 20px 0;color:#c8102e;font-size:32px;font-weight:700;"
                + "letter-spacing:8px;line-height:1.2;\">" + safeCode + "</p>"
                + "<p style=\"margin:0 0 8px 0;font-size:13px;color:#555555;line-height:1.6;\">"
                + "\u9a8c\u8bc1\u7801 " + minutes + " \u5206\u949f\u5185\u6709\u6548\uff0c\u8bf7\u52ff\u6cc4\u9732\u7ed9\u4ed6\u4eba\u3002</p>"
                + "<p style=\"margin:0;font-size:13px;color:#888888;line-height:1.6;\">"
                + "\u5982\u679c\u4e0d\u662f\u60a8\u672c\u4eba\u64cd\u4f5c\uff0c\u8bf7\u5ffd\u7565\u672c\u90ae\u4ef6\u3002</p>"
                + "</td></tr></table></td></tr></table></body></html>";
    }

    private static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
