package com.zhs.support;

/**
 * 注册同意记录使用的协议版本。语言切换不改变版本号，也不触发重新同意。
 * 英文正文尚未经过法律审核，不能当作已发布的英文协议。
 */
public final class AgreementCatalog {

    public static final String VERSION = "v1.0";
    public static final String USER_AGREEMENT = "USER_AGREEMENT";
    public static final String PRIVACY_POLICY = "PRIVACY_POLICY";
    public static final String USER_AGREEMENT_TITLE = "用户协议";
    public static final String PRIVACY_POLICY_TITLE = "隐私政策";
    public static final String ENGLISH_REVIEW_STATUS = "PENDING_REVIEW";

    private AgreementCatalog() {
    }

    public static String versionForLocale(String locale) {
        return VERSION;
    }
}
