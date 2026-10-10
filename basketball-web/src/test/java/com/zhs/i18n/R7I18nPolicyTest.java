package com.zhs.i18n;

import com.zhs.exception.BusinessReason;
import com.zhs.exception.ServiceException;
import com.zhs.service.mail.EmailVerificationMailContent;
import com.zhs.support.AgreementCatalog;
import com.zhs.support.LoginFailurePolicy;
import com.zhs.support.MemberLocaleSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class R7I18nPolicyTest {

    @Test
    void loginFailureDoesNotRevealWhetherAccountExists() {
        ServiceException missingOrWrongPassword = LoginFailurePolicy.invalidCredentials();
        ServiceException wrongPassword = LoginFailurePolicy.invalidCredentials();
        assertEquals(missingOrWrongPassword.getReasonCode(), wrongPassword.getReasonCode());
        assertEquals("INVALID_CREDENTIALS", missingOrWrongPassword.getReasonCode());
        assertFalse(LoginFailurePolicy.revealsAccountExistence(missingOrWrongPassword));
        assertTrue(LoginFailurePolicy.revealsAccountExistence(BusinessReason.EMAIL_ALREADY_REGISTERED.exception()));
    }

    @Test
    void verifyCodeFailuresUseDistinctCodes() {
        assertEquals("VERIFY_CODE_EXPIRED", BusinessReason.VERIFY_CODE_EXPIRED.code());
        assertEquals("VERIFY_CODE_INVALID", BusinessReason.VERIFY_CODE_INVALID.code());
        assertEquals("VERIFY_CODE_MISMATCH", BusinessReason.VERIFY_CODE_MISMATCH.code());
        assertEquals("VERIFY_CODE_TOO_MANY_FAILURES", BusinessReason.VERIFY_CODE_TOO_MANY_FAILURES.code());
        assertNotEquals(BusinessReason.VERIFY_CODE_EXPIRED.message(), BusinessReason.VERIFY_CODE_MISMATCH.message());
    }

    @Test
    void registrationAndTeamCodesKeepChineseMessages() {
        assertEquals("TEAM_FULL", BusinessReason.TEAM_FULL.code());
        assertEquals("球队成员已满", BusinessReason.TEAM_FULL.message());
        assertEquals("APPLICATION_PENDING", BusinessReason.APPLICATION_PENDING.code());
        assertEquals("REGISTRATION_CLOSED", BusinessReason.REGISTRATION_CLOSED.code());
        assertEquals("报名已结束", BusinessReason.REGISTRATION_CLOSED.message());
        assertEquals("PLAYER_NOT_ELIGIBLE", BusinessReason.PLAYER_NOT_ELIGIBLE.code());
        assertEquals("NO_CAPTAIN_GRANT", BusinessReason.NO_CAPTAIN_GRANT.code());
    }

    @Test
    void unknownLocaleAndMissingHeaderStayChinese() {
        assertEquals(MemberLocaleSupport.ZH_CN, MemberLocaleSupport.resolve(null, null, null));
        assertEquals(MemberLocaleSupport.ZH_CN, MemberLocaleSupport.resolve("  ", "", "zh-CN,en;q=0.8"));
        assertEquals(MemberLocaleSupport.EN_US, MemberLocaleSupport.resolve(null, "en-US", "zh-CN"));
        assertEquals(MemberLocaleSupport.EN_US, MemberLocaleSupport.resolve(null, null, "en-GB,en;q=0.9"));
    }

    @Test
    void mailSubjectAndBodyFollowLocaleWithoutChangingTheCode() {
        String code = "123456";
        String chinese = EmailVerificationMailContent.html(code, 5, "zh-CN");
        String english = EmailVerificationMailContent.html(code, 5, "en-US");
        String omitted = EmailVerificationMailContent.html(code, 5, null);
        assertTrue(chinese.contains(code));
        assertTrue(english.contains(code));
        assertTrue(omitted.contains("验证码"));
        assertTrue(english.contains("expires in 5 minutes"));
        assertEquals("【CNPA】邮箱验证码", EmailVerificationMailContent.subject(null));
        assertEquals("[CNPA] Email verification code", EmailVerificationMailContent.subject("en"));
        String hostile = EmailVerificationMailContent.html("<script>alert(1)</script>", 5, "en-US");
        assertFalse(hostile.contains("<script>"));
        assertTrue(hostile.contains("&lt;script&gt;"));
    }

    @Test
    void historicalNoticeWithoutTemplateKeepsStoredText() {
        assertEquals("球队邀请原文", NoticeDisplay.text(null, "球队邀请原文", "Team invitation"));
        assertEquals("Team invitation", NoticeDisplay.text("TEAM_INVITATION_RECEIVED", "球队邀请原文", "Team invitation"));
        assertEquals("球队邀请原文", NoticeDisplay.text("UNKNOWN", "球队邀请原文", null));
    }

    @Test
    void noticeParamsEscapeHtml() {
        String json = NoticeParams.json("remark", "<b>ok</b> & \"x\"");
        assertFalse(json.contains("<b>"));
        assertTrue(json.contains("\\u003c"));
        assertTrue(json.contains("\\u0026"));
    }

    @Test
    void agreementVersionDoesNotChangeWithLanguage() {
        assertEquals("v1.0", AgreementCatalog.VERSION);
        assertEquals(AgreementCatalog.versionForLocale("zh-CN"), AgreementCatalog.versionForLocale("en-US"));
        assertEquals("PENDING_REVIEW", AgreementCatalog.ENGLISH_REVIEW_STATUS);
    }
}
