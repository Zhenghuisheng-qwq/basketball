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
import com.zhs.request.member.MemberRegisterReq;
import com.zhs.response.member.EmailCodeSendResp;
import com.zhs.response.member.MemberRegisterResp;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Bypass only skips SMTP. Redis still stores a real random code. 123456 is never a fallback.
 */
@ExtendWith(MockitoExtension.class)
class BasketMemberAuthMailBypassTest {

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                BasketVerifyCodeRecord.class);
    }

    private static final String TEST_EMAIL = "captain99@cnpa-test.com";
    private static final String PASSWORD = "Test1234aa";

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
            long next = 1L;
            Object current = redis.get(key.buildKey());
            if (current != null) {
                try {
                    next = Long.parseLong(String.valueOf(current)) + 1;
                } catch (NumberFormatException ignored) {
                    next = 1L;
                }
            }
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
    void caseA_devBypassTrue_skipsSmtpButStoresRealRedisCode() {
        authProperties.setDevMailBypass(true);
        when(memberAccountDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        ArgumentCaptor<BasketVerifyCodeRecord> insertCaptor = ArgumentCaptor.forClass(BasketVerifyCodeRecord.class);
        when(verifyCodeRecordDao.insert(insertCaptor.capture())).thenReturn(1);

        EmailCodeSendReq sendReq = new EmailCodeSendReq();
        sendReq.setEmail(TEST_EMAIL);
        sendReq.setPurpose("REGISTER");
        EmailCodeSendResp sendResp = service.sendEmailCode(sendReq, "127.0.0.1");

        assertNotNull(sendResp.getBizNo());
        assertNull(sendResp.getDevBypass(), "must not expose bypass flag to client");
        verify(memberMailService, never()).sendVerificationCode(anyString(), anyString(),
                any(VerifyPurposeEnum.class), anyLong());
        assertEquals("DEV_BYPASS", insertCaptor.getValue().getSendProvider());

        EmailCodeValueCacheKey codeKey = new EmailCodeValueCacheKey("REGISTER", TEST_EMAIL, 300);
        String storedCode = String.valueOf(redis.get(codeKey.buildKey()));
        assertTrue(storedCode.matches("^\\d{6}$"));

        BasketVerifyCodeRecord stored = insertCaptor.getValue();
        stored.setId(100L);
        when(verifyCodeRecordDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(stored);
        when(verifyCodeRecordDao.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(memberAccountDao.insert(any(BasketMemberAccount.class))).thenReturn(1);
        when(memberProfileDao.insert(any(BasketMemberProfile.class))).thenReturn(1);
        when(playerProfileDao.insert(any(BasketPlayerProfile.class))).thenReturn(1);
        when(privacySettingDao.insert(any(BasketPrivacySetting.class))).thenReturn(1);
        when(notificationSettingDao.insert(any(BasketNotificationSetting.class))).thenReturn(1);
        when(agreementRecordDao.insert(any(BasketMemberAgreementRecord.class))).thenReturn(1);
        when(securityLogDao.insert(any(BasketAccountSecurityLog.class))).thenReturn(1);

        MemberRegisterReq registerReq = buildRegisterReq(sendResp.getBizNo(), storedCode);
        MemberRegisterResp registerResp = service.register(registerReq, "127.0.0.1", "junit");
        assertEquals(TEST_EMAIL, registerResp.getEmail());
    }

    @Test
    void caseB_devBypassFalse_wrongCode_registerFails() {
        authProperties.setDevMailBypass(false);

        BasketVerifyCodeRecord record = new BasketVerifyCodeRecord();
        record.setId(200L);
        record.setBizNo("VC_TEST_BIZ");
        record.setReceiver(TEST_EMAIL);
        record.setPurposeType("REGISTER");
        record.setChannelType("EMAIL");
        record.setSendStatus(VerifySendStatusEnum.SUCCESS.getCode());
        record.setVerifyStatus(VerifyStatusEnum.PENDING.getCode());
        record.setExpireTime(new Date(System.currentTimeMillis() + 300_000L));
        record.setCodeHash(passwordEncoder.encode("111111"));
        record.setIsDelete(0);

        when(memberAccountDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(verifyCodeRecordDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);
        redis.put(new EmailCodeValueCacheKey("REGISTER", TEST_EMAIL, 300).buildKey(), "111111");

        MemberRegisterReq registerReq = buildRegisterReq("VC_TEST_BIZ", "999999");
        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.register(registerReq, "127.0.0.1", "junit"));
        assertTrue(ex.getMessage() != null && ex.getMessage().contains("\u9a8c\u8bc1\u7801"));
        verify(memberAccountDao, never()).insert(any(BasketMemberAccount.class));
        verify(memberMailService, never()).sendVerificationCode(anyString(), anyString(),
                any(VerifyPurposeEnum.class), anyLong());
        assertEquals("111111", redis.get(new EmailCodeValueCacheKey("REGISTER", TEST_EMAIL, 300).buildKey()));
    }

    private MemberRegisterReq buildRegisterReq(String bizNo, String code) {
        MemberRegisterReq req = new MemberRegisterReq();
        req.setEmail(TEST_EMAIL);
        req.setPassword(PASSWORD);
        req.setConfirmPassword(PASSWORD);
        req.setTerminalType("WEB");
        req.setVerifyBizNo(bizNo);
        req.setVerifyCode(code);
        req.setAgreeUserAgreement(true);
        req.setAgreePrivacyPolicy(true);
        return req;
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
