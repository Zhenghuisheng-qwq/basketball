package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketFileAssetDao;
import com.zhs.dao.BasketMemberAccountDao;
import com.zhs.dao.BasketMemberProfileDao;
import com.zhs.dao.BasketPlayerCertApplicationDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.enums.PlayerCertStatusEnum;
import com.zhs.enums.VerifiedFlagEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketFileAsset;
import com.zhs.model.BasketMemberAccount;
import com.zhs.model.BasketMemberProfile;
import com.zhs.model.BasketPlayerCertApplication;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.response.account.AccountCenterOverviewResp;
import com.zhs.response.account.AccountSummaryResp;
import com.zhs.response.account.AccountTournamentItemResp;
import com.zhs.response.account.CaptainQualificationResp;
import com.zhs.response.account.PendingTaskResp;
import com.zhs.response.account.PlayerIdentityResp;
import com.zhs.response.team.CaptainQualificationDetailResp;
import com.zhs.response.v4.V4MyEventResp;
import com.zhs.service.IAccountCenterService;
import com.zhs.service.IAccountMessageService;
import com.zhs.service.ICaptainPermissionService;
import com.zhs.service.v4.IBasketV4PlayerMyEventsService;
import com.zhs.util.DataMaskUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AccountCenterServiceImpl implements IAccountCenterService {

    private static final Integer NOT_DELETED = 0;
    private static final int APPROVED_REGISTRATION_STATUS = 3;
    private static final int OVERVIEW_TOURNAMENT_LIMIT = 5;

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
    private IAccountMessageService accountMessageService;
    @Resource
    private ICaptainPermissionService captainPermissionService;
    @Resource
    private IBasketV4PlayerMyEventsService playerMyEventsService;

    @Override
    public AccountCenterOverviewResp getOverview() {
        Long memberId = UserContext.getRequiredUserId();
        BasketMemberAccount account = requireAccount(memberId);
        BasketMemberProfile profile = loadMemberProfile(memberId);
        BasketPlayerProfile playerProfile = loadPlayerProfile(memberId);
        BasketPlayerCertApplication latestCertApp = loadLatestCertApplication(memberId);
        CaptainQualificationDetailResp captainQualDetail = captainPermissionService.getCurrentQualification(memberId);

        AccountCenterOverviewResp resp = new AccountCenterOverviewResp();
        resp.setAccountSummary(buildAccountSummary(account, profile));
        resp.setPlayerIdentity(buildPlayerIdentity(playerProfile, latestCertApp));
        resp.setCaptainQualification(buildCaptainQualification(captainQualDetail));
        resp.setCurrentTournaments(loadCurrentTournaments(memberId, playerProfile));
        resp.setUnreadMessageCount(accountMessageService.countUnread());
        resp.setRecentMessages(accountMessageService.listRecentMessages(5));
        resp.setPendingTasks(buildPendingTasks(account, playerProfile, latestCertApp, resp.getUnreadMessageCount()));
        return resp;
    }

    private List<AccountTournamentItemResp> loadCurrentTournaments(Long memberId, BasketPlayerProfile playerProfile) {
        if (playerProfile == null) {
            return List.of();
        }
        try {
            return playerMyEventsService.listMyEvents().stream()
                    .limit(OVERVIEW_TOURNAMENT_LIMIT)
                    .map(this::mapMyEventToAccountTournamentItem)
                    .collect(Collectors.toList());
        } catch (ServiceException ex) {
            return List.of();
        }
    }

    private AccountTournamentItemResp mapMyEventToAccountTournamentItem(V4MyEventResp event) {
        AccountTournamentItemResp item = new AccountTournamentItemResp();
        item.setRegistrationId(event.getEventTeamId() == null ? null : String.valueOf(event.getEventTeamId()));
        item.setRegistrationNo(event.getEventTeamId() == null ? null : String.valueOf(event.getEventTeamId()));
        item.setTournamentId(event.getSeasonId() == null ? null : String.valueOf(event.getSeasonId()));
        item.setTournamentName(event.getSeriesName() != null ? event.getSeriesName() : event.getSeasonTitle());
        item.setSeasonId(event.getSeasonId() == null ? null : String.valueOf(event.getSeasonId()));
        item.setSeasonName(event.getSeasonTitle());
        item.setPlatformCode(event.getPlatformCode());
        item.setPlatformName(event.getPlatformName() != null ? event.getPlatformName() : event.getPlatformCode());
        item.setTeamId(event.getEventTeamId() == null ? null : String.valueOf(event.getEventTeamId()));
        item.setTeamName(event.getEventTeamName() != null ? event.getEventTeamName() : event.getTeamName());
        item.setRosterRole(event.getRoleInTeam());
        item.setRegistrationStatus(APPROVED_REGISTRATION_STATUS);
        item.setNextMatch(buildNextMatchSummary(event));
        return item;
    }

    private Map<String, Object> buildNextMatchSummary(V4MyEventResp event) {
        if (event.getNextMatchId() == null && event.getNextMatchTime() == null) {
            return null;
        }
        Map<String, Object> nextMatch = new HashMap<>();
        nextMatch.put("matchId", event.getNextMatchId());
        nextMatch.put("matchTime", event.getNextMatchTime());
        nextMatch.put("stageName", event.getCurrentStageName());
        nextMatch.put("groupName", event.getCurrentGroupName());
        nextMatch.put("standingRank", event.getStandingRank());
        nextMatch.put("standingPoints", event.getStandingPoints());
        nextMatch.put("qualificationStatus", event.getQualificationStatus());
        nextMatch.put("divisionId", event.getDivisionId());
        nextMatch.put("divisionName", event.getDivisionName());
        return nextMatch;
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

    private AccountSummaryResp buildAccountSummary(BasketMemberAccount account, BasketMemberProfile profile) {
        AccountSummaryResp summary = new AccountSummaryResp();
        summary.setAccountId(String.valueOf(account.getId()));
        summary.setAccountNo(account.getAccountNo());
        summary.setEmailMasked(DataMaskUtil.maskEmail(account.getEmail()));
        summary.setEmailVerified(VerifiedFlagEnum.isYes(account.getEmailVerified()));
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
            identity.setPlayerStatus(playerProfile.getPlayerStatus());
            identity.setPlayerNo(playerProfile.getPlayerNo());
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

    private CaptainQualificationResp buildCaptainQualification(CaptainQualificationDetailResp detail) {
        if (detail == null || !Boolean.TRUE.equals(detail.getHasQualification())) {
            return null;
        }
        CaptainQualificationResp resp = new CaptainQualificationResp();
        resp.setQualificationNo(detail.getQualificationNo());
        resp.setQualificationStatus(detail.getStatus());
        resp.setValidFrom(detail.getValidFrom());
        resp.setValidTo(detail.getValidTo());
        resp.setCanCreateTeam(detail.getCanCreateTeam());
        resp.setRuntimeValid(detail.getRuntimeValid());
        resp.setInvalidReason(detail.getInvalidReason());
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
            return com.zhs.support.MemberAvatarSupport.resolve(null);
        }
        BasketFileAsset asset = fileAssetDao.selectById(avatarFileId);
        if (asset == null || !NOT_DELETED.equals(asset.getIsDelete())) {
            return com.zhs.support.MemberAvatarSupport.resolve(null);
        }
        return com.zhs.support.MemberAvatarSupport.resolve(asset.getAccessUrl());
    }
}
