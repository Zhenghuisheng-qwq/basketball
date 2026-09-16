package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.zhs.common.cachekey.MiniCacheKey;
import com.zhs.common.cachekey.member.EmailCodeValueCacheKey;
import com.zhs.config.MemberAuthProperties;
import com.zhs.dao.BasketAccountSecurityLogDao;
import com.zhs.dao.BasketMemberAccountDao;
import com.zhs.dao.BasketMemberAgreementRecordDao;
import com.zhs.dao.BasketMemberProfileDao;
import com.zhs.dao.BasketNotificationSettingDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketPrivacySettingDao;
import com.zhs.dao.BasketVerifyCodeRecordDao;
import com.zhs.enums.VerifyPurposeEnum;
import com.zhs.enums.VerifySendStatusEnum;
import com.zhs.enums.VerifyStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketAccountSecurityLog;
import com.zhs.model.BasketMemberAccount;
import com.zhs.model.BasketMemberAgreementRecord;
import com.zhs.model.BasketMemberProfile;
import com.zhs.model.BasketNotificationSetting;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketPrivacySetting;
import com.zhs.model.BasketVerifyCodeRecord;
import com.zhs.request.member.EmailCodeSendReq;
import com.zhs.request.member.MemberPasswordResetReq;
import com.zhs.request.member.MemberRegisterReq;
import com.zhs.service.mail.IMemberMailService;
import com.zhs.support.MemberSessionSupport;
import com.zhs.util.MiniRedisUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketMemberAuthEmailCodeTest {

    private static final String EMAIL = "captain99@cnpa-test.com";
    private static final String PASSWORD = "Test1234aa";

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, BasketVerifyCodeRecord.class);
        TableInfoHelper.initTableInfo(assistant, BasketMemberAccount.class);
    }

    @Mock
    private BasketMemberAccountDao memberAccountDao;
    @Mock
    private BasketMemberProfileDao memberProfileDao;
    @Mock
    private BasketPlayerProfileDao playerProfileDao;
    @Mock
    private BasketPrivacySettingDao privacySettingDao;
    @Mock
    private BasketNotificationSettingDao notificationSettingDao;
    @Mock
    private BasketMemberAgreementRecordDao agreementRecordDao;
    @Mock
    private BasketVerifyCodeRecordDao verifyCodeRecordDao;
    @Mock
    private BasketAccountSecurityLogDao securityLogDao;
    @Mock
    private IMemberMailService memberMailService;
    @Mock
    private MiniRedisUtil miniRedisUtil;
    @Mock
    private MemberSessionSupport memberSessionSupport;

    private final MemberAuthProperties authProperties = new MemberAuthProperties();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, Object> redis = new ConcurrentHashMap<>();

    @InjectMocks
    private BasketMemberAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        setField(service, "authProperties", authProperties);
        setField(service, "passwordEncoder", passwordEncoder);
        authProperties.setDevMailBypass(false);
        authProperties.setCodeExpireSeconds(300);
        authProperties.setCodeResendSeconds(60);
        authProperties.setCodeDailyLimit(20);
        authProperties.setCodeIpHourlyLimit(30);
        authProperties.setCodeMaxFailTimes(5);

        org.mockito.Mockito.lenient().when(miniRedisUtil.hasKey(any(MiniCacheKey.class))).thenAnswer(inv ->
                redis.containsKey(((MiniCacheKey) inv.getArgument(0)).buildKey()));
        org.mockito.Mockito.lenient().when(miniRedisUtil.get(any(MiniCacheKey.class))).thenAnswer(inv ->
                redis.get(((MiniCacheKey) inv.getArgument(0)).buildKey()));
        org.mockito.Mockito.lenient().when(miniRedisUtil.delete(any(MiniCacheKey.class))).thenAnswer(inv ->
                redis.remove(((MiniCacheKey) inv.getArgument(0)).buildKey()) != null);
        org.mockito.Mockito.lenient().when(miniRedisUtil.increment(any(MiniCacheKey.class))).thenAnswer(inv -> {
            MiniCacheKey key = inv.getArgument(0);
            long next = toLong(redis.get(key.buildKey())) + 1;
            redis.put(key.buildKey(), next);
            return next;
        });
        org.mockito.Mockito.lenient().doAnswer(inv -> {
            MiniCacheKey key = inv.getArgument(0);
            redis.put(key.buildKey(), inv.getArgument(1));
            return null;
        }).when(miniRedisUtil).set(any(MiniCacheKey.class), any());
    }

    @Test
    void case2_sendSuccess_writesRedisCodeWithTtl() {
        stubSendOk();
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

        service.sendEmailCode(sendReq(EMAIL, "REGISTER"), "127.0.0.1");

        verify(memberMailService).sendVerificationCode(eq(EMAIL), codeCaptor.capture(),
                eq(VerifyPurposeEnum.REGISTER), eq(5L));
        String code = codeCaptor.getValue();
        assertTrue(code.matches("^\\d{6}$"));
        EmailCodeValueCacheKey codeKey = new EmailCodeValueCacheKey("REGISTER", EMAIL, 300);
        assertEquals(code, redis.get(codeKey.buildKey()));
        assertEquals(300, codeKey.getExpireTime());
        assertTrue(redis.containsKey(new com.zhs.common.cachekey.member.EmailCodeLimitCacheKey(
                "REGISTER", EMAIL, 60).buildKey()));
    }

    @Test
    void case3_resendWithin60Seconds_rejectedWithoutSecondMail() {
        stubSendOk();
        service.sendEmailCode(sendReq(EMAIL, "REGISTER"), "127.0.0.1");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.sendEmailCode(sendReq(EMAIL, "REGISTER"), "127.0.0.1"));
        assertEquals("\u9a8c\u8bc1\u7801\u53d1\u9001\u8fc7\u4e8e\u9891\u7e41\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5", ex.getMessage());
        verify(memberMailService, times(1)).sendVerificationCode(anyString(), anyString(),
                any(VerifyPurposeEnum.class), anyLong());
    }

    @Test
    void case4_correctCode_passesAndDeletesRedis() {
        IssuedCode issued = sendRegisterAndCapture();
        stubRegisterPersist(issued.record);

        service.register(buildRegisterReq(issued.bizNo, issued.code), "127.0.0.1", "junit");

        assertFalse(redis.containsKey(issued.codeKey.buildKey()));
        verify(memberAccountDao).insert(any(BasketMemberAccount.class));
    }

    @Test
    void case5_wrongCode_failsAndKeepsRedis() {
        IssuedCode issued = sendRegisterAndCapture();
        when(verifyCodeRecordDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(issued.record);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.register(buildRegisterReq(issued.bizNo, "000000"), "127.0.0.1", "junit"));
        assertEquals("\u9a8c\u8bc1\u7801\u9519\u8bef", ex.getMessage());
        assertEquals(issued.code, redis.get(issued.codeKey.buildKey()));
        verify(memberAccountDao, never()).insert(any(BasketMemberAccount.class));
    }

    @Test
    void case6_expiredCode_returnsExpiredMessage() {
        IssuedCode issued = sendRegisterAndCapture();
        redis.remove(issued.codeKey.buildKey());
        when(verifyCodeRecordDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(issued.record);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.register(buildRegisterReq(issued.bizNo, issued.code), "127.0.0.1", "junit"));
        assertEquals("\u9a8c\u8bc1\u7801\u5df2\u8fc7\u671f\uff0c\u8bf7\u91cd\u65b0\u83b7\u53d6", ex.getMessage());
    }

    @Test
    void case7_smtpFailure_doesNotLeaveValidCode() {
        when(memberAccountDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(verifyCodeRecordDao.insert(any(BasketVerifyCodeRecord.class))).thenReturn(1);
        doThrow(new ServiceException("smtp down")).when(memberMailService)
                .sendVerificationCode(anyString(), anyString(), any(VerifyPurposeEnum.class), anyLong());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.sendEmailCode(sendReq(EMAIL, "REGISTER"), "127.0.0.1"));
        assertEquals("\u9a8c\u8bc1\u7801\u53d1\u9001\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5", ex.getMessage());
        assertFalse(redis.keySet().stream().anyMatch(k -> k.startsWith("member:email:code:REGISTER:")));
        verify(memberMailService).sendVerificationCode(anyString(), anyString(),
                eq(VerifyPurposeEnum.REGISTER), anyLong());
    }

    @Test
    void case8_registerCodeCannotResetPassword() {
        IssuedCode issued = sendRegisterAndCapture();
        org.mockito.Mockito.lenient().when(memberAccountDao.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(existingAccount());

        MemberPasswordResetReq resetReq = new MemberPasswordResetReq();
        resetReq.setEmail(EMAIL);
        resetReq.setVerifyBizNo(issued.bizNo);
        resetReq.setVerifyCode(issued.code);
        resetReq.setPassword(PASSWORD);
        resetReq.setConfirmPassword(PASSWORD);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.resetPassword(resetReq, "127.0.0.1"));
        assertTrue(ex.getMessage().contains("\u9a8c\u8bc1\u7801"));
        assertEquals(issued.code, redis.get(issued.codeKey.buildKey()));
    }

    @Test
    void fixedCode123456_isRejectedUnlessItIsTheRealRedisValue() {
        IssuedCode issued = sendRegisterAndCapture();
        when(verifyCodeRecordDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(issued.record);
        if ("123456".equals(issued.code)) {
            return;
        }
        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.register(buildRegisterReq(issued.bizNo, "123456"), "127.0.0.1", "junit"));
        assertEquals("\u9a8c\u8bc1\u7801\u9519\u8bef", ex.getMessage());
        assertNotEquals("123456", issued.code);
    }

    @Test
    void bypassSkipsSmtpButStillRequiresRedisCode() {
        authProperties.setDevMailBypass(true);
        stubSendOk();
        service.sendEmailCode(sendReq(EMAIL, "REGISTER"), "127.0.0.1");
        verify(memberMailService, never()).sendVerificationCode(anyString(), anyString(),
                any(VerifyPurposeEnum.class), anyLong());

        EmailCodeValueCacheKey codeKey = new EmailCodeValueCacheKey("REGISTER", EMAIL, 300);
        String code = String.valueOf(redis.get(codeKey.buildKey()));
        assertTrue(code.matches("^\\d{6}$"));
    }

    private IssuedCode sendRegisterAndCapture() {
        stubSendOk();
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<BasketVerifyCodeRecord> recordCaptor = ArgumentCaptor.forClass(BasketVerifyCodeRecord.class);
        when(verifyCodeRecordDao.insert(recordCaptor.capture())).thenReturn(1);

        var resp = service.sendEmailCode(sendReq(EMAIL, "REGISTER"), "127.0.0.1");
        verify(memberMailService).sendVerificationCode(eq(EMAIL), codeCaptor.capture(),
                eq(VerifyPurposeEnum.REGISTER), eq(5L));

        BasketVerifyCodeRecord stored = recordCaptor.getValue();
        stored.setId(100L);
        stored.setSendStatus(VerifySendStatusEnum.SUCCESS.getCode());
        stored.setVerifyStatus(VerifyStatusEnum.PENDING.getCode());
        return new IssuedCode(resp.getBizNo(), codeCaptor.getValue(), stored,
                new EmailCodeValueCacheKey("REGISTER", EMAIL, 300));
    }

    private void stubSendOk() {
        org.mockito.Mockito.lenient().when(memberAccountDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        org.mockito.Mockito.lenient().when(verifyCodeRecordDao.insert(any(BasketVerifyCodeRecord.class))).thenReturn(1);
    }

    private void stubRegisterPersist(BasketVerifyCodeRecord record) {
        when(verifyCodeRecordDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);
        when(verifyCodeRecordDao.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(memberAccountDao.insert(any(BasketMemberAccount.class))).thenReturn(1);
        when(memberProfileDao.insert(any(BasketMemberProfile.class))).thenReturn(1);
        when(playerProfileDao.insert(any(BasketPlayerProfile.class))).thenReturn(1);
        when(privacySettingDao.insert(any(BasketPrivacySetting.class))).thenReturn(1);
        when(notificationSettingDao.insert(any(BasketNotificationSetting.class))).thenReturn(1);
        when(agreementRecordDao.insert(any(BasketMemberAgreementRecord.class))).thenReturn(1);
        when(securityLogDao.insert(any(BasketAccountSecurityLog.class))).thenReturn(1);
    }

    private BasketMemberAccount existingAccount() {
        BasketMemberAccount account = new BasketMemberAccount();
        account.setId(9L);
        account.setEmail(EMAIL);
        account.setAccountStatus(1);
        account.setIsDelete(0);
        return account;
    }

    private EmailCodeSendReq sendReq(String email, String purpose) {
        EmailCodeSendReq req = new EmailCodeSendReq();
        req.setEmail(email);
        req.setPurpose(purpose);
        return req;
    }

    private MemberRegisterReq buildRegisterReq(String bizNo, String code) {
        MemberRegisterReq req = new MemberRegisterReq();
        req.setEmail(EMAIL);
        req.setPassword(PASSWORD);
        req.setConfirmPassword(PASSWORD);
        req.setTerminalType("WEB");
        req.setVerifyBizNo(bizNo);
        req.setVerifyCode(code);
        req.setAgreeUserAgreement(true);
        req.setAgreePrivacyPolicy(true);
        return req;
    }

    private record IssuedCode(String bizNo, String code, BasketVerifyCodeRecord record, EmailCodeValueCacheKey codeKey) {
    }

    private static long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static void setField(Object target, String name, Object value) {
        try {
            var field = BasketMemberAuthServiceImpl.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
