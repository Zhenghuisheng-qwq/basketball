package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketCaptainQualificationDao;
import com.zhs.dao.BasketFileAssetDao;
import com.zhs.dao.BasketMemberAccountDao;
import com.zhs.dao.BasketMemberProfileDao;
import com.zhs.dao.BasketPlayerCertApplicationDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.enums.PlayerCertStatusEnum;
import com.zhs.enums.VerifiedFlagEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketCaptainQualification;
import com.zhs.model.BasketFileAsset;
import com.zhs.model.BasketMemberAccount;
import com.zhs.model.BasketMemberProfile;
import com.zhs.model.BasketPlayerCertApplication;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.response.account.AccountCenterOverviewResp;
import com.zhs.response.account.AccountSummaryResp;
import com.zhs.response.account.CaptainQualificationResp;
import com.zhs.response.account.PendingTaskResp;
import com.zhs.response.account.PlayerIdentityResp;
import com.zhs.service.IAccountCenterService;
import com.zhs.service.IAccountMessageService;
import com.zhs.service.IAccountTournamentService;
import com.zhs.util.DataMaskUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AccountCenterServiceImpl implements IAccountCenterService {

    private static final Integer NOT_DELETED = 0;
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
    private BasketPlayerCertApplicationDao certApplicationDao;
    @Resource
    private BasketCaptainQualificationDao captainQualificationDao;
    @Resource
    private IAccountMessageService accountMessageService;
    @Resource
    private IAccountTournamentService accountTournamentService;

    @Override
    public AccountCenterOverviewResp getOverview() {
        Long memberId = UserContext.getRequiredUserId();
        BasketMemberAccount account = requireAccount(memberId);
        BasketMemberProfile profile = loadMemberProfile(memberId);
        BasketPlayerProfile playerProfile = loadPlayerProfile(memberId);
        BasketPlayerCertApplication latestCertApp = loadLatestCertApplication(memberId);
        BasketCaptainQualification captainQualification = loadCaptainQualification(memberId);

        AccountCenterOverviewResp resp = new AccountCenterOverviewResp();
        resp.setAccountSummary(buildAccountSummary(account, profile));
        resp.setPlayerIdentity(buildPlayerIdentity(playerProfile, latestCertApp));
        resp.setCaptainQualification(buildCaptainQualification(captainQualification));
        resp.setCurrentTournaments(accountTournamentService.listRecentTournaments(5));
        resp.setUnreadMessageCount(accountMessageService.countUnread());
        resp.setRecentMessages(accountMessageService.listRecentMessages(5));
        resp.setPendingTasks(buildPendingTasks(account, playerProfile, latestCertApp, resp.getUnreadMessageCount()));
        return resp;
    }

    private BasketMemberAccount requireAccount(Long memberId) {
        BasketMemberAccount account = memberAccountDao.selectById(memberId);
        if (account == null || !NOT_DELETED.equals(account.getIsDelete())) {
            throw new ServiceException("账号不存在");
        }
        return account;
    }

    private BasketMemberProfile loadMemberProfile(Long memberId) {
        return memberProfileDao.selectOne(new LambdaQueryWrapper<BasketMemberProfile>()
                .eq(BasketMemberProfile::getMemberId, memberId)
                .eq(BasketMemberProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
    }

    private BasketPlayerProfile loadPlayerProfile(Long memberId) {
        return playerProfileDao.selectOne(new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getMemberId, memberId)
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
    }

    private BasketPlayerCertApplication loadLatestCertApplication(Long memberId) {
        return certApplicationDao.selectOne(new LambdaQueryWrapper<BasketPlayerCertApplication>()
                .eq(BasketPlayerCertApplication::getMemberId, memberId)
                .eq(BasketPlayerCertApplication::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketPlayerCertApplication::getCreateTime)
                .orderByDesc(BasketPlayerCertApplication::getId)
                .last("limit 1"));
    }

    private BasketCaptainQualification loadCaptainQualification(Long memberId) {
        return captainQualificationDao.selectOne(new LambdaQueryWrapper<BasketCaptainQualification>()
                .eq(BasketCaptainQualification::getMemberId, memberId)
                .eq(BasketCaptainQualification::getIsCurrent, CURRENT_QUALIFICATION)
                .eq(BasketCaptainQualification::getQualificationStatus, ACTIVE_QUALIFICATION_STATUS)
                .eq(BasketCaptainQualification::getIsDelete, NOT_DELETED)
                .last("limit 1"));
    }

    private AccountSummaryResp buildAccountSummary(BasketMemberAccount account, BasketMemberProfile profile) {
        AccountSummaryResp summary = new AccountSummaryResp();
        summary.setAccountId(String.valueOf(account.getId()));
        summary.setAccountNo(account.getAccountNo());
        summary.setEmailMasked(DataMaskUtil.maskEmail(account.getEmail()));
        summary.setMobileMasked(DataMaskUtil.maskMobile(account.getMobile()));
        summary.setEmailVerified(VerifiedFlagEnum.isYes(account.getEmailVerified()));
        summary.setMobileVerified(VerifiedFlagEnum.isYes(account.getMobileVerified()));
        summary.setAccountStatus(account.getAccountStatus());
        summary.setRegisterTime(account.getCreateTime());
        summary.setLastLoginTime(account.getLastLoginTime());
        if (profile != null) {
            summary.setNickname(profile.getNickname());
            summary.setAvatarUrl(resolveAvatarUrl(profile.getAvatarFileId()));
        }
        return summary;
    }

    private PlayerIdentityResp buildPlayerIdentity(BasketPlayerProfile playerProfile,
                                                   BasketPlayerCertApplication latestCertApp) {
        if (playerProfile == null && latestCertApp == null) {
            return null;
        }

        PlayerIdentityResp identity = new PlayerIdentityResp();
        if (playerProfile != null) {
            identity.setCertificationStatus(playerProfile.getCertificationStatus());
            identity.setPlayerNo(playerProfile.getPlayerNo());
            identity.setRealName(playerProfile.getRealName());
            identity.setGameTerminal(playerProfile.getGameTerminal());
            identity.setGameAccount(playerProfile.getGameAccount());
            identity.setPositionCode(playerProfile.getPositionCode());
            identity.setCertifiedTime(playerProfile.getCertifiedTime());
        }
        if (latestCertApp != null) {
            identity.setApplicationNo(latestCertApp.getApplicationNo());
            identity.setApplicationStatus(latestCertApp.getApplicationStatus());
            identity.setSubmittedTime(latestCertApp.getSubmittedTime());
            identity.setReviewedTime(latestCertApp.getReviewedTime());
            identity.setReviewRemark(latestCertApp.getReviewRemark());
        }
        return identity;
    }

    private CaptainQualificationResp buildCaptainQualification(BasketCaptainQualification qualification) {
        if (qualification == null) {
            return null;
        }
        CaptainQualificationResp resp = new CaptainQualificationResp();
        resp.setQualificationNo(qualification.getQualificationNo());
        resp.setQualificationStatus(qualification.getQualificationStatus());
        resp.setGrantedTime(qualification.getGrantedTime());
        resp.setValidFrom(qualification.getValidFrom());
        resp.setValidTo(qualification.getValidTo());
        return resp;
    }

    private List<PendingTaskResp> buildPendingTasks(BasketMemberAccount account,
                                                    BasketPlayerProfile playerProfile,
                                                    BasketPlayerCertApplication latestCertApp,
                                                    long unreadMessageCount) {
        List<PendingTaskResp> tasks = new ArrayList<>();

        if (!VerifiedFlagEnum.isYes(account.getEmailVerified())) {
            tasks.add(buildTask("EMAIL_UNVERIFIED", "验证邮箱", "您的邮箱尚未完成验证", "/account/setting"));
        }
        if (!VerifiedFlagEnum.isYes(account.getMobileVerified())) {
            tasks.add(buildTask("MOBILE_UNVERIFIED", "验证手机", "您的手机号尚未完成验证", "/account/setting"));
        }

        if (playerProfile == null) {
            tasks.add(buildTask("NO_CERT", "完成球员认证", "您尚未提交球员认证申请", "/account/player/certification"));
        } else {
            Integer profileStatus = playerProfile.getCertificationStatus();
            if (profileStatus != null && profileStatus == PlayerCertStatusEnum.DRAFT.getCode()) {
                tasks.add(buildTask("CERT_DRAFT", "继续完成认证", "您的球员认证申请仍为草稿", "/account/player/certification"));
            }
            if (profileStatus != null && PlayerCertStatusEnum.fromCode(profileStatus).isReviewing()) {
                tasks.add(buildTask("CERT_REVIEWING", "认证审核中", "您的球员认证申请正在审核", "/account/player/certification"));
            }
            if (profileStatus != null && profileStatus == PlayerCertStatusEnum.REJECTED.getCode()) {
                tasks.add(buildTask("CERT_REJECTED", "认证未通过", "您的球员认证申请未通过，请修改后重新提交", "/account/player/certification"));
            }
        }

        if (latestCertApp != null) {
            PlayerCertStatusEnum appStatus = PlayerCertStatusEnum.fromCode(latestCertApp.getApplicationStatus());
            if (appStatus == PlayerCertStatusEnum.DRAFT
                    && tasks.stream().noneMatch(task -> "CERT_DRAFT".equals(task.getTaskType()))) {
                tasks.add(buildTask("CERT_DRAFT", "继续完成认证", "您的球员认证申请仍为草稿", "/account/player/certification"));
            }
            if (appStatus.isReviewing()
                    && tasks.stream().noneMatch(task -> "CERT_REVIEWING".equals(task.getTaskType()))) {
                tasks.add(buildTask("CERT_REVIEWING", "认证审核中", "您的球员认证申请正在审核", "/account/player/certification"));
            }
            if (appStatus == PlayerCertStatusEnum.REJECTED
                    && tasks.stream().noneMatch(task -> "CERT_REJECTED".equals(task.getTaskType()))) {
                tasks.add(buildTask("CERT_REJECTED", "认证未通过", "您的球员认证申请未通过，请修改后重新提交", "/account/player/certification"));
            }
        }

        if (unreadMessageCount > 0) {
            tasks.add(buildTask("UNREAD_MESSAGES", "查看未读消息",
                    "您有 " + unreadMessageCount + " 条未读消息", "/account/notifications"));
        }
        return tasks;
    }

    private PendingTaskResp buildTask(String taskType, String title, String description, String actionPath) {
        PendingTaskResp task = new PendingTaskResp();
        task.setTaskType(taskType);
        task.setTitle(title);
        task.setDescription(description);
        task.setActionPath(actionPath);
        return task;
    }

    private String resolveAvatarUrl(Long avatarFileId) {
        if (avatarFileId == null) {
            return null;
        }
        BasketFileAsset asset = fileAssetDao.selectById(avatarFileId);
        if (asset == null || !NOT_DELETED.equals(asset.getIsDelete())) {
            return null;
        }
        return asset.getAccessUrl();
    }
}
