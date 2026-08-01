package com.zhs.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.config.MemberInitProperties;
import com.zhs.dao.BasketMemberAccountDao;
import com.zhs.dao.BasketMemberAgreementRecordDao;
import com.zhs.dao.BasketMemberProfileDao;
import com.zhs.dao.BasketNotificationSettingDao;
import com.zhs.dao.BasketPlayerCertApplicationDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketPrivacySettingDao;
import com.zhs.enums.MemberAccountStatusEnum;
import com.zhs.enums.PlayerCertStatusEnum;
import com.zhs.enums.VerifiedFlagEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketMemberAccount;
import com.zhs.model.BasketMemberAgreementRecord;
import com.zhs.model.BasketMemberProfile;
import com.zhs.model.BasketNotificationSetting;
import com.zhs.model.BasketPlayerCertApplication;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketPrivacySetting;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class MemberInitAccountCreator {

    private static final String REGISTER_SOURCE_WEB = "WEB";
    private static final String REGISTER_TERMINAL = "WEB";

    @Resource
    private MemberInitProperties memberInitProperties;
    @Resource
    private BasketMemberAccountDao memberAccountDao;
    @Resource
    private BasketMemberProfileDao memberProfileDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketPrivacySettingDao privacySettingDao;
    @Resource
    private BasketNotificationSettingDao notificationSettingDao;
    @Resource
    private BasketMemberAgreementRecordDao agreementRecordDao;
    @Resource
    private BasketPlayerCertApplicationDao certApplicationDao;
    @Resource
    private PasswordEncoder passwordEncoder;

    public Long createMemberAccount(String email, Long avatarFileId) {
        Date now = new Date();
        Long memberId = IdUtil.getSnowflakeNextId();

        BasketMemberAccount account = new BasketMemberAccount();
        account.setId(memberId);
        account.setAccountNo("M" + IdUtil.getSnowflakeNextIdStr());
        account.setUsername("cnpa" + memberId);
        account.setPasswordHash(passwordEncoder.encode(memberInitProperties.getPassword()));
        account.setEmail(email);
        account.setEmailVerified(VerifiedFlagEnum.YES.getCode());
        account.setAccountStatus(MemberAccountStatusEnum.NORMAL.getCode());
        account.setRegisterSource(REGISTER_SOURCE_WEB);
        account.setRegisterTerminal(REGISTER_TERMINAL);
        account.setPasswordUpdateTime(now);
        account.setIsDelete(0);

        try {
            memberAccountDao.insert(account);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException("邮箱已注册: " + email);
        }

        String localPart = email.substring(0, email.indexOf('@'));
        BasketMemberProfile profile = new BasketMemberProfile();
        profile.setMemberId(memberId);
        profile.setNickname("CNPA用户" + localPart);
        profile.setGender(0);
        profile.setProfileStatus(1);
        profile.setIsDelete(0);
        if (avatarFileId != null) {
            profile.setAvatarFileId(avatarFileId);
        }
        memberProfileDao.insert(profile);

        BasketPlayerProfile playerProfile = new BasketPlayerProfile();
        playerProfile.setMemberId(memberId);
        playerProfile.setCertificationStatus(0);
        playerProfile.setPlayerStatus(1);
        playerProfile.setProfileVisible(1);
        playerProfile.setIsDelete(0);
        playerProfileDao.insert(playerProfile);

        BasketPrivacySetting privacy = new BasketPrivacySetting();
        privacy.setMemberId(memberId);
        privacy.setProfileVisible(1);
        privacy.setAllowTeamInvite(1);
        privacy.setShowTeamHistory(1);
        privacy.setShowMatchData(1);
        privacy.setShowOnlineStatus(0);
        privacy.setIsDelete(0);
        privacySettingDao.insert(privacy);

        BasketNotificationSetting notice = new BasketNotificationSetting();
        notice.setMemberId(memberId);
        notice.setSystemNotice(1);
        notice.setCertificationNotice(1);
        notice.setTournamentNotice(1);
        notice.setTeamNotice(1);
        notice.setAnnouncementNotice(1);
        notice.setEmailNotice(1);
        notice.setSmsNotice(0);
        notice.setIsDelete(0);
        notificationSettingDao.insert(notice);

        saveAgreement(memberId, "USER_AGREEMENT", "用户协议", now);
        saveAgreement(memberId, "PRIVACY_POLICY", "隐私政策", now);
        return memberId;
    }

    public void assertCanApplyCert(Long memberId) {
        Long count = certApplicationDao.selectCount(new LambdaQueryWrapper<BasketPlayerCertApplication>()
                .eq(BasketPlayerCertApplication::getMemberId, memberId)
                .eq(BasketPlayerCertApplication::getIsDelete, 0)
                .ne(BasketPlayerCertApplication::getApplicationStatus, PlayerCertStatusEnum.CANCELLED.getCode()));
        if (count != null && count > 0) {
            throw new ServiceException("账号已存在球员认证申请: " + memberId);
        }
    }

    public BasketMemberAccount findByEmail(String email) {
        return memberAccountDao.selectOne(new LambdaQueryWrapper<BasketMemberAccount>()
                .eq(BasketMemberAccount::getEmail, email)
                .eq(BasketMemberAccount::getIsDelete, 0)
                .last("limit 1"));
    }

    private void saveAgreement(Long memberId, String type, String title, Date now) {
        BasketMemberAgreementRecord record = new BasketMemberAgreementRecord();
        record.setMemberId(memberId);
        record.setAgreementType(type);
        record.setAgreementVersion("v1.0");
        record.setAgreementTitle(title);
        record.setAgreeStatus(1);
        record.setAgreeTime(now);
        record.setTerminalType(REGISTER_TERMINAL);
        record.setIsDelete(0);
        agreementRecordDao.insert(record);
    }
}
