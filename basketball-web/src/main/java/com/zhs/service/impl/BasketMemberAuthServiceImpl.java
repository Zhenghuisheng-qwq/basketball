package com.zhs.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhs.common.cachekey.MiniCacheKey;
import com.zhs.common.cachekey.member.EmailCodeDailyLimitCacheKey;
import com.zhs.common.cachekey.member.EmailCodeFailCacheKey;
import com.zhs.common.cachekey.member.EmailCodeIpLimitCacheKey;
import com.zhs.common.cachekey.member.EmailCodeLimitCacheKey;
import com.zhs.common.cachekey.member.LoginFailCacheKey;
import com.zhs.config.MemberAuthProperties;
import com.zhs.dao.BasketAccountSecurityLogDao;
import com.zhs.dao.v4.BasketCaptainGrantDao;
import com.zhs.dao.BasketLoginLogDao;
import com.zhs.dao.BasketFileAssetDao;
import com.zhs.dao.BasketMemberAccountDao;
import com.zhs.dao.BasketMemberAgreementRecordDao;
import com.zhs.dao.BasketMemberProfileDao;
import com.zhs.dao.BasketNotificationSettingDao;
import com.zhs.dao.BasketPrivacySettingDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketVerifyCodeRecordDao;
import com.zhs.enums.LoginResultEnum;
import com.zhs.enums.MemberAccountStatusEnum;
import com.zhs.enums.VerifiedFlagEnum;
import com.zhs.enums.VerifyPurposeEnum;
import com.zhs.enums.VerifySendStatusEnum;
import com.zhs.enums.VerifyStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketAccountSecurityLog;
import com.zhs.enums.v4.CaptainGrantStatusEnum;
import com.zhs.model.v4.BasketCaptainGrant;
import com.zhs.model.BasketFileAsset;
import com.zhs.model.BasketLoginLog;
import com.zhs.model.BasketMemberAccount;
import com.zhs.model.BasketMemberAgreementRecord;
import com.zhs.model.BasketMemberProfile;
import com.zhs.model.BasketNotificationSetting;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketPrivacySetting;
import com.zhs.model.BasketVerifyCodeRecord;
import com.zhs.request.member.EmailCodeSendReq;
import com.zhs.request.member.MemberPasswordLoginReq;
import com.zhs.request.member.MemberPasswordResetReq;
import com.zhs.request.member.MemberRegisterReq;
import com.zhs.response.member.EmailCodeSendResp;
import com.zhs.request.account.AccountChangeEmailReq;
import com.zhs.response.account.AccountChangeEmailResp;
import com.zhs.response.member.MemberLoginResp;
import com.zhs.response.member.MemberRegisterResp;
import com.zhs.response.member.MemberSessionResp;
import com.zhs.response.member.MemberSummaryResp;
import com.zhs.service.IBasketMemberAuthService;
import com.zhs.service.mail.IMemberMailService;
import com.zhs.support.MemberAvatarSupport;
import com.zhs.support.MemberSessionSupport;
import com.zhs.util.MiniRedisUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class BasketMemberAuthServiceImpl implements IBasketMemberAuthService {

    private static final String CHANNEL_EMAIL = "EMAIL";
    private static final String REGISTER_SOURCE_WEB = "WEB";
    private static final String LOGIN_TYPE_PASSWORD = "PASSWORD";
    private static final String TOKEN_TYPE_BEARER = "Bearer";
    private static final String SEND_PROVIDER_DEV_BYPASS = "DEV_BYPASS";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String EVENT_PASSWORD_RESET = "PASSWORD_RESET";
    private static final String EVENT_EMAIL_CHANGE = "EMAIL_CHANGE";
    private static final String FAKE_BIZ_NO_PREFIX = "NOP";
    private static final int CURRENT_QUALIFICATION = 1;
    private static final int ACTIVE_QUALIFICATION_STATUS = 1;

    @Resource
    private BasketMemberAccountDao memberAccountDao;
    @Resource
    private BasketMemberProfileDao memberProfileDao;
    @Resource
    private BasketFileAssetDao fileAssetDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketCaptainGrantDao captainGrantDao;
    @Resource
    private BasketPrivacySettingDao privacySettingDao;
    @Resource
    private BasketNotificationSettingDao notificationSettingDao;
    @Resource
    private BasketMemberAgreementRecordDao agreementRecordDao;
    @Resource
    private BasketVerifyCodeRecordDao verifyCodeRecordDao;
    @Resource
    private BasketLoginLogDao loginLogDao;
    @Resource
    private BasketAccountSecurityLogDao securityLogDao;
    @Resource
    private IMemberMailService memberMailService;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private MiniRedisUtil miniRedisUtil;
    @Resource
    private MemberAuthProperties authProperties;
    @Resource
    private MemberSessionSupport memberSessionSupport;

    @PostConstruct
    public void logDevMailBypassState() {
        if (isDevMailBypass()) {
            log.warn("[member-auth] 开发模式已开启：未对接真实邮箱，发送验证码后输入任意 6 位数字即可通过。"
                    + " 上线前请配置 spring.mail 并设置 CNPA_MAIL_DEV_BYPASS=false");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmailCodeSendResp sendEmailCode(EmailCodeSendReq req, String clientIp) {
        String email = normalizeEmail(req.getEmail());
        VerifyPurposeEnum purpose = VerifyPurposeEnum.fromCode(req.getPurpose());
        BasketMemberAccount existingAccount = findByEmail(email);
        if (purpose == VerifyPurposeEnum.RESET_PASSWORD && existingAccount == null) {
            return buildFakeEmailCodeResp();
        }
        assertPurposeAccountState(email, purpose, existingAccount);

        EmailCodeLimitCacheKey limitKey = new EmailCodeLimitCacheKey(
                purpose.getCode(), email, authProperties.getCodeResendSeconds());
        if (Boolean.TRUE.equals(miniRedisUtil.hasKey(limitKey))) {
            throw new ServiceException("发送过于频繁，请稍后再试");
        }

        String day = DateUtil.format(new Date(), "yyyyMMdd");
        EmailCodeDailyLimitCacheKey dailyKey = new EmailCodeDailyLimitCacheKey(day, email);
        long dailyCount = miniRedisUtil.increment(dailyKey);
        if (dailyCount > authProperties.getCodeDailyLimit()) {
            throw new ServiceException("今日验证码发送次数已达上限");
        }

        String hourBucket = DateUtil.format(new Date(), "yyyyMMddHH");
        EmailCodeIpLimitCacheKey ipKey = new EmailCodeIpLimitCacheKey(hourBucket, clientIp);
        long ipCount = miniRedisUtil.increment(ipKey);
        if (ipCount > authProperties.getCodeIpHourlyLimit()) {
            throw new ServiceException("当前网络发送过于频繁，请稍后再试");
        }

        String code = generateNumericCode(6);
        String bizNo = "VC" + IdUtil.getSnowflakeNextIdStr();
        Date now = new Date();
        Date expireTime = new Date(now.getTime() + authProperties.getCodeExpireSeconds() * 1000L);

        BasketVerifyCodeRecord record = new BasketVerifyCodeRecord();
        record.setBizNo(bizNo);
        record.setChannelType(CHANNEL_EMAIL);
        record.setReceiver(email);
        record.setPurposeType(purpose.getCode());
        record.setCodeHash(passwordEncoder.encode(code));
        record.setSendStatus(VerifySendStatusEnum.INIT.getCode());
        record.setVerifyStatus(VerifyStatusEnum.PENDING.getCode());
        record.setExpireTime(expireTime);
        record.setClientIp(clientIp);
        record.setIsDelete(0);

        if (isDevMailBypass()) {
            // 本地开发：不发真实邮件，仅落库成功流水，校验时任意 6 位数字可通过
            record.setSendProvider(SEND_PROVIDER_DEV_BYPASS);
            record.setSendStatus(VerifySendStatusEnum.SUCCESS.getCode());
            record.setFailureReason("DEV_MAIL_BYPASS");
            verifyCodeRecordDao.insert(record);
            log.warn("[member-auth] 开发模式跳过邮件发送 email={} purpose={} bizNo={}",
                    maskEmail(email), purpose.getCode(), bizNo);
        } else {
            record.setSendProvider("SMTP");
            try {
                memberMailService.sendVerifyCodeMail(
                        email,
                        code,
                        purpose.getLabel(),
                        Math.max(1, authProperties.getCodeExpireSeconds() / 60));
                record.setSendStatus(VerifySendStatusEnum.SUCCESS.getCode());
                verifyCodeRecordDao.insert(record);
            } catch (ServiceException e) {
                record.setSendStatus(VerifySendStatusEnum.FAIL.getCode());
                record.setFailureReason(trimReason(e.getMessage()));
                verifyCodeRecordDao.insert(record);
                throw e;
            } catch (Exception e) {
                record.setSendStatus(VerifySendStatusEnum.FAIL.getCode());
                record.setFailureReason("发送异常");
                verifyCodeRecordDao.insert(record);
                throw new ServiceException("验证码发送失败，请稍后重试");
            }
        }

        miniRedisUtil.set(limitKey, "1");
        return buildEmailCodeSendResp(bizNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberRegisterResp register(MemberRegisterReq req, String clientIp, String userAgent) {
        String email = normalizeEmail(req.getEmail());
        validatePasswordPair(req.getPassword(), req.getConfirmPassword());
        if (!Boolean.TRUE.equals(req.getAgreeUserAgreement()) || !Boolean.TRUE.equals(req.getAgreePrivacyPolicy())) {
            throw new ServiceException("请先阅读并同意用户协议与隐私政策");
        }
        String terminalType = normalizeTerminal(req.getTerminalType());

        if (findByEmail(email) != null) {
            throw new ServiceException("邮箱已注册");
        }

        consumeVerifyCode(email, VerifyPurposeEnum.REGISTER, req.getVerifyBizNo(), req.getVerifyCode());

        Date now = new Date();
        Long memberId = IdUtil.getSnowflakeNextId();
        BasketMemberAccount account = new BasketMemberAccount();
        account.setId(memberId);
        account.setAccountNo(generateAccountNo());
        account.setUsername(generateInternalUsername(memberId));
        account.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        account.setEmail(email);
        account.setEmailVerified(VerifiedFlagEnum.YES.getCode());
        account.setAccountStatus(MemberAccountStatusEnum.NORMAL.getCode());
        account.setRegisterSource(REGISTER_SOURCE_WEB);
        account.setRegisterTerminal(terminalType);
        account.setPasswordUpdateTime(now);
        account.setIsDelete(0);

        try {
            memberAccountDao.insert(account);
        } catch (DuplicateKeyException e) {
            throw translateDuplicate(e);
        }

        BasketMemberProfile profile = new BasketMemberProfile();
        profile.setMemberId(account.getId());
        profile.setNickname(generateDefaultNickname());
        profile.setGender(0);
        profile.setProfileStatus(1);
        profile.setIsDelete(0);
        memberProfileDao.insert(profile);

        BasketPlayerProfile playerProfile = new BasketPlayerProfile();
        playerProfile.setMemberId(account.getId());
        playerProfile.setCertificationStatus(0);
        playerProfile.setPlayerStatus(1);
        playerProfile.setProfileVisible(1);
        playerProfile.setIsDelete(0);
        playerProfileDao.insert(playerProfile);

        BasketPrivacySetting privacy = new BasketPrivacySetting();
        privacy.setMemberId(account.getId());
        privacy.setProfileVisible(1);
        privacy.setAllowTeamInvite(1);
        privacy.setShowTeamHistory(1);
        privacy.setShowMatchData(1);
        privacy.setShowOnlineStatus(0);
        privacy.setIsDelete(0);
        privacySettingDao.insert(privacy);

        BasketNotificationSetting notice = new BasketNotificationSetting();
        notice.setMemberId(account.getId());
        notice.setSystemNotice(1);
        notice.setCertificationNotice(1);
        notice.setTournamentNotice(1);
        notice.setTeamNotice(1);
        notice.setAnnouncementNotice(1);
        notice.setEmailNotice(1);
        notice.setSmsNotice(0);
        notice.setIsDelete(0);
        notificationSettingDao.insert(notice);

        saveAgreement(account.getId(), "USER_AGREEMENT", "用户协议", terminalType, clientIp, userAgent, now);
        saveAgreement(account.getId(), "PRIVACY_POLICY", "隐私政策", terminalType, clientIp, userAgent, now);

        writeSecurityLog(account.getId(), "REGISTER", "用户注册",
                null, maskEmail(email), "MEMBER", account.getId(), clientIp, terminalType, now, "官网邮箱注册");

        MemberRegisterResp resp = new MemberRegisterResp();
        resp.setEmail(email);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmailCodeSendResp sendChangeEmailCode(String newEmail, Long memberId, String clientIp) {
        if (memberId == null) {
            throw new ServiceException("未登录", 401);
        }
        String normalizedEmail = normalizeEmail(newEmail);
        BasketMemberAccount account = memberAccountDao.selectById(memberId);
        if (account == null || Integer.valueOf(1).equals(account.getIsDelete())) {
            throw new ServiceException("账号不存在");
        }
        if (!MemberAccountStatusEnum.isNormal(account.getAccountStatus())) {
            throw new ServiceException("账号已被禁用");
        }
        if (normalizedEmail.equals(account.getEmail())) {
            throw new ServiceException("新邮箱不能与当前邮箱相同");
        }
        if (findByEmail(normalizedEmail) != null) {
            throw new ServiceException("新邮箱已被占用");
        }

        EmailCodeSendReq req = new EmailCodeSendReq();
        req.setEmail(normalizedEmail);
        req.setPurpose(VerifyPurposeEnum.CHANGE_EMAIL.getCode());
        return sendEmailCode(req, clientIp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountChangeEmailResp changeEmail(AccountChangeEmailReq req, Long memberId, String clientIp, String currentToken) {
        if (req == null) {
            throw new ServiceException("请求参数不能为空");
        }
        if (memberId == null) {
            throw new ServiceException("未登录", 401);
        }
        String newEmail = normalizeEmail(req.getEmail());
        BasketMemberAccount account = memberAccountDao.selectById(memberId);
        if (account == null || Integer.valueOf(1).equals(account.getIsDelete())) {
            throw new ServiceException("账号不存在");
        }
        if (!MemberAccountStatusEnum.isNormal(account.getAccountStatus())) {
            throw new ServiceException("账号已被禁用");
        }
        if (newEmail.equals(account.getEmail())) {
            throw new ServiceException("新邮箱不能与当前邮箱相同");
        }
        if (findByEmail(newEmail) != null) {
            throw new ServiceException("新邮箱已被占用");
        }

        consumeVerifyCode(newEmail, VerifyPurposeEnum.CHANGE_EMAIL, req.getVerifyBizNo(), req.getVerifyCode());

        String oldMasked = maskEmail(account.getEmail());
        Date now = new Date();
        account.setEmail(newEmail);
        account.setEmailVerified(VerifiedFlagEnum.YES.getCode());
        try {
            memberAccountDao.updateById(account);
        } catch (DuplicateKeyException e) {
            throw new ServiceException("新邮箱已被占用");
        }

        long authVersion = memberSessionSupport.resolveAuthVersion(account);
        memberSessionSupport.invalidateAllSessions(memberId, currentToken, authVersion);

        writeSecurityLog(memberId, EVENT_EMAIL_CHANGE, "修改登录邮箱",
                oldMasked, maskEmail(newEmail), "MEMBER", memberId, clientIp, null, now, null);

        AccountChangeEmailResp resp = new AccountChangeEmailResp();
        resp.setNeedRelogin(true);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberLoginResp loginByPassword(MemberPasswordLoginReq req, String clientIp, String userAgent) {
        String email = normalizeEmail(req.getEmail());
        String terminalType = normalizeTerminal(req.getTerminalType());
        if (!StringUtils.hasText(req.getPassword())) {
            throw new ServiceException("密码不能为空");
        }

        LoginFailCacheKey failKey = new LoginFailCacheKey(email, authProperties.getLoginLockMinutes());
        long failCount = readCounter(failKey);
        if (failCount >= authProperties.getLoginFailMaxTimes()) {
            writeLoginLog(null, LOGIN_TYPE_PASSWORD, LoginResultEnum.FAIL, "账号临时锁定",
                    clientIp, terminalType, userAgent, new Date());
            throw new ServiceException("登录失败次数过多，请稍后再试");
        }

        BasketMemberAccount account = findByEmail(email);
        if (account == null) {
            increaseLoginFail(failKey);
            writeLoginLog(null, LOGIN_TYPE_PASSWORD, LoginResultEnum.FAIL, "邮箱或密码错误",
                    clientIp, terminalType, userAgent, new Date());
            throw new ServiceException("邮箱或密码错误");
        }
        if (account.getLockExpireTime() != null && account.getLockExpireTime().after(new Date())) {
            writeLoginLog(account, LOGIN_TYPE_PASSWORD, LoginResultEnum.FAIL, "账号已锁定",
                    clientIp, terminalType, userAgent, new Date());
            throw new ServiceException("账号已锁定，请稍后再试");
        }
        if (!MemberAccountStatusEnum.isNormal(account.getAccountStatus())) {
            writeLoginLog(account, LOGIN_TYPE_PASSWORD, LoginResultEnum.FAIL, "账号已禁用",
                    clientIp, terminalType, userAgent, new Date());
            throw new ServiceException("账号已被禁用");
        }
        if (!VerifiedFlagEnum.isYes(account.getEmailVerified())) {
            writeLoginLog(account, LOGIN_TYPE_PASSWORD, LoginResultEnum.FAIL, "邮箱尚未验证",
                    clientIp, terminalType, userAgent, new Date());
            throw new ServiceException("邮箱尚未验证");
        }
        if (!StringUtils.hasText(account.getPasswordHash())
                || !passwordEncoder.matches(req.getPassword(), account.getPasswordHash())) {
            increaseLoginFail(failKey);
            writeLoginLog(account, LOGIN_TYPE_PASSWORD, LoginResultEnum.FAIL, "邮箱或密码错误",
                    clientIp, terminalType, userAgent, new Date());
            writeSecurityLog(account.getId(), "LOGIN_FAIL", "密码登录失败",
                    null, null, "MEMBER", account.getId(), clientIp, terminalType, new Date(), "邮箱或密码错误");
            throw new ServiceException("邮箱或密码错误");
        }

        miniRedisUtil.delete(failKey);
        Date now = new Date();
        touchLogin(account, clientIp, now);
        String token = issueToken(account);
        writeLoginLog(account, LOGIN_TYPE_PASSWORD, LoginResultEnum.SUCCESS, null, clientIp, terminalType, userAgent, now);
        writeSecurityLog(account.getId(), "LOGIN_SUCCESS", "密码登录成功",
                null, null, "MEMBER", account.getId(), clientIp, terminalType, now, null);
        return buildLoginResp(account, token);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(MemberPasswordResetReq req, String clientIp) {
        if (req == null) {
            throw new ServiceException("请求参数不能为空");
        }
        String email = normalizeEmail(req.getEmail());
        validatePasswordPair(req.getPassword(), req.getConfirmPassword());
        consumeVerifyCode(email, VerifyPurposeEnum.RESET_PASSWORD, req.getVerifyBizNo(), req.getVerifyCode());

        BasketMemberAccount account = findByEmail(email);
        if (account == null) {
            throw new ServiceException("验证码错误或已过期");
        }
        if (!MemberAccountStatusEnum.isNormal(account.getAccountStatus())) {
            throw new ServiceException("账号已被禁用");
        }

        Date now = new Date();
        account.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        account.setPasswordUpdateTime(now);
        memberAccountDao.updateById(account);

        long authVersion = memberSessionSupport.resolveAuthVersion(account);
        memberSessionSupport.invalidateAllSessions(account.getId(), null, authVersion);

        writeSecurityLog(account.getId(), EVENT_PASSWORD_RESET, "重置登录密码",
                null, null, "MEMBER", account.getId(), clientIp, null, now, null);
    }

    @Override
    public MemberSessionResp getSession(Long memberId, String token) {
        if (memberId == null) {
            throw new ServiceException("未登录", 401);
        }
        BasketMemberAccount account = memberAccountDao.selectById(memberId);
        if (account == null || Integer.valueOf(1).equals(account.getIsDelete())) {
            memberSessionSupport.revokeToken(token);
            throw new ServiceException("登录已过期", 401);
        }
        if (!MemberAccountStatusEnum.isNormal(account.getAccountStatus())) {
            memberSessionSupport.revokeToken(token);
            throw new ServiceException("账号已被禁用", 401);
        }
        if (StringUtils.hasText(token)) {
            MemberSessionSupport.ParsedTokenSession session = memberSessionSupport.resolveTokenSession(token);
            if (session == null || !memberId.equals(session.memberId())
                    || !memberSessionSupport.isTokenAuthVersionValid(memberId, session.authVersion())) {
                memberSessionSupport.revokeToken(token);
                throw new ServiceException("登录已过期", 401);
            }
        }

        MemberSessionResp resp = new MemberSessionResp();
        fillSummary(resp, account, loadOfficialProfileSummary(account.getId(), account.getEmail()));
        resp.setRegisterSource(account.getRegisterSource());
        resp.setRegisterTerminal(account.getRegisterTerminal());
        fillPlayerSessionFields(resp, memberId);
        return resp;
    }

    @Override
    public void logout(String token) {
        memberSessionSupport.revokeToken(token);
    }

    private void consumeVerifyCode(String email, VerifyPurposeEnum purpose, String bizNo, String code) {
        if (!StringUtils.hasText(bizNo)) {
            throw new ServiceException("验证码流水号不能为空");
        }
        if (bizNo.trim().startsWith(FAKE_BIZ_NO_PREFIX)) {
            throw new ServiceException("验证码错误或已过期");
        }
        if (!StringUtils.hasText(code) || !code.trim().matches("^\\d{6}$")) {
            throw new ServiceException("验证码格式不正确");
        }
        String normalizedBizNo = bizNo.trim();
        String normalizedCode = code.trim();

        BasketVerifyCodeRecord record = verifyCodeRecordDao.selectOne(
                new LambdaQueryWrapper<BasketVerifyCodeRecord>()
                        .eq(BasketVerifyCodeRecord::getBizNo, normalizedBizNo)
                        .eq(BasketVerifyCodeRecord::getReceiver, email)
                        .eq(BasketVerifyCodeRecord::getPurposeType, purpose.getCode())
                        .eq(BasketVerifyCodeRecord::getChannelType, CHANNEL_EMAIL)
                        .eq(BasketVerifyCodeRecord::getIsDelete, 0)
                        .last("limit 1")
        );
        if (record == null) {
            throw new ServiceException("验证码错误或已过期");
        }
        if (!Integer.valueOf(VerifySendStatusEnum.SUCCESS.getCode()).equals(record.getSendStatus())) {
            throw new ServiceException("验证码错误或已过期");
        }
        if (!Integer.valueOf(VerifyStatusEnum.PENDING.getCode()).equals(record.getVerifyStatus())) {
            throw new ServiceException("验证码已失效");
        }
        if (record.getExpireTime() != null && record.getExpireTime().before(new Date())) {
            invalidateVerifyRecord(record.getId(), "已过期");
            throw new ServiceException("验证码已过期");
        }

        EmailCodeFailCacheKey failKey = new EmailCodeFailCacheKey(normalizedBizNo, authProperties.getCodeExpireSeconds());
        long failTimes = readCounter(failKey);
        if (failTimes >= authProperties.getCodeMaxFailTimes()) {
            invalidateVerifyRecord(record.getId(), "错误次数超限");
            throw new ServiceException("验证码错误次数过多，请重新获取");
        }

        boolean bypassCodeCheck = isDevMailBypass();
        if (!bypassCodeCheck && !passwordEncoder.matches(normalizedCode, record.getCodeHash())) {
            miniRedisUtil.increment(failKey);
            throw new ServiceException("验证码错误或已过期");
        }

        int updated = verifyCodeRecordDao.update(null,
                new LambdaUpdateWrapper<BasketVerifyCodeRecord>()
                        .eq(BasketVerifyCodeRecord::getId, record.getId())
                        .eq(BasketVerifyCodeRecord::getVerifyStatus, VerifyStatusEnum.PENDING.getCode())
                        .set(BasketVerifyCodeRecord::getVerifyStatus, VerifyStatusEnum.USED.getCode())
                        .set(BasketVerifyCodeRecord::getVerifyTime, new Date())
        );
        if (updated == 0) {
            throw new ServiceException("验证码已失效");
        }
        miniRedisUtil.delete(failKey);
    }

    private void invalidateVerifyRecord(Long id, String reason) {
        verifyCodeRecordDao.update(null,
                new LambdaUpdateWrapper<BasketVerifyCodeRecord>()
                        .eq(BasketVerifyCodeRecord::getId, id)
                        .eq(BasketVerifyCodeRecord::getVerifyStatus, VerifyStatusEnum.PENDING.getCode())
                        .set(BasketVerifyCodeRecord::getVerifyStatus, VerifyStatusEnum.INVALID.getCode())
                        .set(BasketVerifyCodeRecord::getFailureReason, trimReason(reason))
        );
    }

    private void assertPurposeAccountState(String email, VerifyPurposeEnum purpose, BasketMemberAccount account) {
        if (purpose == VerifyPurposeEnum.REGISTER && account != null) {
            throw new ServiceException("邮箱已注册");
        }
        if (purpose == VerifyPurposeEnum.CHANGE_EMAIL && account != null) {
            throw new ServiceException("新邮箱已被占用");
        }
    }

    private EmailCodeSendResp buildFakeEmailCodeResp() {
        EmailCodeSendResp resp = buildEmailCodeSendResp(FAKE_BIZ_NO_PREFIX + IdUtil.getSnowflakeNextIdStr());
        resp.setDevBypass(false);
        return resp;
    }

    private EmailCodeSendResp buildEmailCodeSendResp(String bizNo) {
        EmailCodeSendResp resp = new EmailCodeSendResp();
        resp.setBizNo(bizNo);
        resp.setExpireSeconds(authProperties.getCodeExpireSeconds());
        resp.setResendSeconds(authProperties.getCodeResendSeconds());
        resp.setDevBypass(isDevMailBypass());
        return resp;
    }

    private String generateInternalUsername(Long memberId) {
        return "cnpa" + memberId;
    }

    private void fillPlayerSessionFields(MemberSessionResp resp, Long memberId) {
        BasketPlayerProfile playerProfile = playerProfileDao.selectOne(new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getMemberId, memberId)
                .eq(BasketPlayerProfile::getIsDelete, 0)
                .last("limit 1"));
        if (playerProfile != null) {
            resp.setPlayerStatus(playerProfile.getPlayerStatus());
        }

        BasketCaptainGrant captainGrant = captainGrantDao.selectOne(new LambdaQueryWrapper<BasketCaptainGrant>()
                .eq(BasketCaptainGrant::getMemberId, memberId)
                .eq(BasketCaptainGrant::getGrantStatus, CaptainGrantStatusEnum.ACTIVE.getCode())
                .eq(BasketCaptainGrant::getIsDelete, 0)
                .orderByDesc(BasketCaptainGrant::getCreateTime)
                .orderByDesc(BasketCaptainGrant::getId)
                .last("limit 1"));
        if (captainGrant != null) {
            resp.setCaptainQualificationStatus(1);
        }
    }

    private BasketMemberAccount findByEmail(String email) {
        return memberAccountDao.selectOne(new LambdaQueryWrapper<BasketMemberAccount>()
                .eq(BasketMemberAccount::getEmail, email)
                .eq(BasketMemberAccount::getIsDelete, 0)
                .last("limit 1"));
    }

    private void touchLogin(BasketMemberAccount account, String clientIp, Date now) {
        account.setLastLoginTime(now);
        account.setLastLoginIp(clientIp);
        memberAccountDao.updateById(account);
    }

    private String issueToken(BasketMemberAccount account) {
        String token = UUID.randomUUID().toString().replace("-", "");
        long authVersion = memberSessionSupport.resolveAuthVersion(account);
        memberSessionSupport.syncAuthVersion(account.getId(), authVersion);
        memberSessionSupport.storeToken(token, account.getId(), authVersion);
        return token;
    }

    private MemberLoginResp buildLoginResp(BasketMemberAccount account, String token) {
        MemberLoginResp resp = new MemberLoginResp();
        resp.setToken(token);
        resp.setTokenType(TOKEN_TYPE_BEARER);
        resp.setExpireSeconds(authProperties.getTokenExpireDays() * 24 * 3600);
        MemberSummaryResp member = new MemberSummaryResp();
        fillSummary(member, account, loadOfficialProfileSummary(account.getId(), account.getEmail()));
        resp.setMember(member);
        return resp;
    }

    private void fillSummary(MemberSummaryResp target, BasketMemberAccount account, OfficialProfileSummary profile) {
        target.setMemberId(String.valueOf(account.getId()));
        target.setUserId(String.valueOf(account.getId()));
        target.setAccountNo(account.getAccountNo());
        target.setNickname(profile.nickname());
        // 正式头像；无 avatar_file_id 时保持 null，前端 Placeholder。绝不读 pending change。
        target.setAvatarUrl(profile.avatarUrl());
        target.setEmailMasked(maskEmail(account.getEmail()));
        target.setEmailVerified(VerifiedFlagEnum.isYes(account.getEmailVerified()));
        target.setAccountStatus(account.getAccountStatus());
    }

    /**
     * 一次查询正式资料（basket_member_profile），供 login / session 共用。
     * 不读 basket_member_profile_change。
     */
    private OfficialProfileSummary loadOfficialProfileSummary(Long memberId, String email) {
        BasketMemberProfile profile = memberProfileDao.selectOne(new LambdaQueryWrapper<BasketMemberProfile>()
                .eq(BasketMemberProfile::getMemberId, memberId)
                .eq(BasketMemberProfile::getIsDelete, 0)
                .last("limit 1"));
        String nickname = profile != null && StringUtils.hasText(profile.getNickname())
                ? profile.getNickname()
                : deriveNicknameFromEmail(email);
        String avatarUrl = null;
        if (profile != null && profile.getAvatarFileId() != null) {
            BasketFileAsset asset = fileAssetDao.selectById(profile.getAvatarFileId());
            if (asset != null && Integer.valueOf(0).equals(asset.getIsDelete())) {
                avatarUrl = MemberAvatarSupport.accessUrlOrNull(asset.getAccessUrl());
            }
        }
        return new OfficialProfileSummary(nickname, avatarUrl);
    }

    private record OfficialProfileSummary(String nickname, String avatarUrl) {
    }

    private void saveAgreement(Long memberId, String type, String title, String terminalType,
                               String clientIp, String userAgent, Date now) {
        BasketMemberAgreementRecord record = new BasketMemberAgreementRecord();
        record.setMemberId(memberId);
        record.setAgreementType(type);
        record.setAgreementVersion("v1.0");
        record.setAgreementTitle(title);
        record.setAgreeStatus(1);
        record.setAgreeTime(now);
        record.setClientIp(clientIp);
        record.setTerminalType(terminalType);
        record.setUserAgent(trimUa(userAgent));
        record.setIsDelete(0);
        agreementRecordDao.insert(record);
    }

    private void writeLoginLog(BasketMemberAccount account, String loginType, LoginResultEnum result,
                               String failureReason, String clientIp, String terminalType,
                               String userAgent, Date loginTime) {
        BasketLoginLog logEntity = new BasketLoginLog();
        if (account != null) {
            logEntity.setMemberId(account.getId());
            logEntity.setAccountNo(account.getAccountNo());
        }
        logEntity.setLoginType(loginType);
        logEntity.setLoginResult(result.getCode());
        logEntity.setFailureReason(trimReason(failureReason));
        logEntity.setClientIp(clientIp);
        logEntity.setTerminalType(terminalType);
        logEntity.setUserAgent(trimUa(userAgent));
        logEntity.setLoginTime(loginTime);
        logEntity.setIsDelete(0);
        loginLogDao.insert(logEntity);
    }

    private void writeSecurityLog(Long memberId, String eventType, String title,
                                  String oldMasked, String newMasked, String operatorType,
                                  Long operatorId, String clientIp, String terminalType,
                                  Date eventTime, String remark) {
        BasketAccountSecurityLog logEntity = new BasketAccountSecurityLog();
        logEntity.setMemberId(memberId);
        logEntity.setEventType(eventType);
        logEntity.setEventTitle(title);
        logEntity.setOldValueMasked(oldMasked);
        logEntity.setNewValueMasked(newMasked);
        logEntity.setOperatorType(operatorType);
        logEntity.setOperatorId(operatorId);
        logEntity.setClientIp(clientIp);
        logEntity.setTerminalType(terminalType);
        logEntity.setEventTime(eventTime);
        logEntity.setRemark(trimReason(remark));
        logEntity.setIsDelete(0);
        securityLogDao.insert(logEntity);
    }

    private void increaseLoginFail(LoginFailCacheKey failKey) {
        miniRedisUtil.increment(failKey);
    }

    private long readCounter(MiniCacheKey cacheKey) {
        Object value = miniRedisUtil.get(cacheKey);
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private ServiceException translateDuplicate(DuplicateKeyException e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        if (message.contains("uk_member_email") || message.contains("email")) {
            return new ServiceException("邮箱已注册");
        }
        if (message.contains("uk_member_username") || message.contains("username")) {
            return new ServiceException("注册失败，请稍后重试");
        }
        if (message.contains("uk_member_account_no") || message.contains("account_no")) {
            return new ServiceException("账号编号冲突，请重试");
        }
        return new ServiceException("注册失败，请稍后重试");
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ServiceException("邮箱不能为空");
        }
        String value = email.trim().toLowerCase();
        if (!value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ServiceException("邮箱格式不正确");
        }
        return value;
    }

    private String generateDefaultNickname() {
        int suffix = ThreadLocalRandom.current().nextInt(1000, 1_000_000);
        return "CNPA用户" + suffix;
    }

    private String normalizeTerminal(String terminalType) {
        return StringUtils.hasText(terminalType) ? terminalType.trim() : "WEB";
    }

    private void validatePasswordPair(String password, String confirmPassword) {
        if (!StringUtils.hasText(password)) {
            throw new ServiceException("密码不能为空");
        }
        if (password.length() < 8 || password.length() > 20) {
            throw new ServiceException("密码长度应为 8 至 20 位");
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new ServiceException("密码需同时包含字母和数字");
        }
        if (!password.equals(confirmPassword)) {
            throw new ServiceException("两次输入的密码不一致");
        }
    }

    private String generateAccountNo() {
        return "M" + IdUtil.getSnowflakeNextIdStr();
    }

    private String deriveNicknameFromEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return "CNPA用户";
        }
        String local = email.split("@")[0];
        return local.length() > 20 ? local.substring(0, 20) : local;
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String visible = local.length() <= 2 ? local.substring(0, 1) + "*" : local.substring(0, 2) + "***";
        return visible + "@" + parts[1];
    }

    private String trimReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return null;
        }
        return reason.length() > 500 ? reason.substring(0, 500) : reason;
    }

    private String trimUa(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return null;
        }
        return userAgent.length() > 1000 ? userAgent.substring(0, 1000) : userAgent;
    }

    /** 使用 SecureRandom 生成数字验证码，禁止 Math.random / 非安全 RNG。 */
    private String generateNumericCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(SECURE_RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /** 本地开发跳过 SMTP：配置开关为 true，或显式开关开启。 */
    private boolean isDevMailBypass() {
        return authProperties.isDevMailBypass();
    }
}
