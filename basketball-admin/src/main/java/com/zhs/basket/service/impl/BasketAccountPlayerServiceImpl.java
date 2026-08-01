package com.zhs.basket.service.impl;

import com.zhs.basket.domain.*;
import com.zhs.basket.enums.CaptainQualificationStatusEnum;
import com.zhs.basket.enums.PlatformAccountStatusEnum;
import com.zhs.basket.enums.PlayerCertStatusEnum;
import com.zhs.basket.enums.RegistrationStatusEnum;
import com.zhs.basket.enums.TeamAuditStatusEnum;
import com.zhs.basket.enums.TeamMembershipStatusEnum;
import com.zhs.basket.mapper.*;
import com.zhs.basket.query.AccountPlayerQuery;
import com.zhs.basket.service.IBasketAccountPlayerService;
import com.zhs.basket.support.CaptainQualificationValiditySupport;
import com.zhs.basket.support.MemberAvatarSupport;
import com.zhs.basket.support.PlatformGameIdDisplaySupport;
import com.zhs.basket.vo.*;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BasketAccountPlayerServiceImpl implements IBasketAccountPlayerService {

    private static final int ACCOUNT_DISABLED = 0;
    private static final int ACCOUNT_NORMAL = 1;

    @Autowired private BasketMemberAccountMapper memberAccountMapper;
    @Autowired private BasketPlayerPlatformAccountMapper platformAccountMapper;
    @Autowired private BasketPlayerCertApplicationMapper certApplicationMapper;
    @Autowired private BasketCaptainQualificationMapper captainQualificationMapper;
    @Autowired private BasketPlayerProfileMapper playerProfileMapper;
    @Autowired private BasketTeamMemberMapper teamMemberMapper;
    @Autowired private BasketTournamentRegistrationMapper registrationMapper;
    @Autowired private BasketMessageReceiverMapper messageReceiverMapper;
    @Autowired private BasketAccountSecurityLogMapper securityLogMapper;

    @Override
    public List<AccountPlayerListVo> selectList(AccountPlayerQuery query) {
        List<AccountPlayerListVo> list = memberAccountMapper.selectList(query);
        attachPlatformGameIds(list);
        list.forEach(this::enrichListVo);
        return list;
    }

    @Override
    public AccountPlayerFullDetailVo selectDetail(Long memberId) {
        AccountPlayerDetailVo base = memberAccountMapper.selectDetail(memberId);
        if (base == null) {
            throw new ServiceException("账号不存在");
        }
        AccountPlayerFullDetailVo detail = new AccountPlayerFullDetailVo();
        detail.setAccount(buildAccountSection(base));
        detail.setProfile(buildProfileSection(base));
        detail.setPlatformAccounts(loadPlatformAccounts(base.getPlayerId()));
        detail.setCertHistory(loadCertHistory(memberId));
        detail.setCaptainQual(loadCaptainQual(base.getPlayerId()));
        detail.setCaptainGrants(loadCaptainGrants(memberId));
        detail.setTeamSummary(loadTeamSummary(memberId));
        detail.setRegistrations(loadRegistrations(memberId));
        detail.setNotifications(loadNotifications(memberId));
        detail.setOperationLogs(loadOperationLogs(memberId));
        return detail;
    }

    private void enrichListVo(AccountPlayerListVo vo) {
        vo.setUserId(vo.getMemberId());
        vo.setAvatarUrl(MemberAvatarSupport.resolve(vo.getAvatarUrl()));
        vo.setAccountStatusName(resolveAccountStatusName(vo.getAccountStatus()));
        if (vo.getGameIdSummary() == null) {
            vo.setGameIdSummary(PlatformGameIdDisplaySupport.UNBOUND);
            vo.setGameIdCompactSummary(PlatformGameIdDisplaySupport.UNBOUND);
            vo.setPlatformGameIds(Collections.emptyList());
        }
        applyCertDisplay(vo);
        if (vo.getCertificationStatus() != null) {
            vo.setCertificationStatusName(
                    PlayerCertStatusEnum.fromCode(vo.getCertificationStatus()).getDescription());
        }
    }

    private void attachPlatformGameIds(List<AccountPlayerListVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> playerIds = list.stream()
                .map(AccountPlayerListVo::getPlayerId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, List<BasketPlayerPlatformAccount>> accountMap = loadActivePlatformAccounts(playerIds);
        for (AccountPlayerListVo vo : list) {
            List<BasketPlayerPlatformAccount> accounts = vo.getPlayerId() == null
                    ? Collections.emptyList()
                    : accountMap.getOrDefault(vo.getPlayerId(), Collections.emptyList());
            vo.setPlatformGameIds(PlatformGameIdDisplaySupport.toItems(accounts));
            vo.setGameIdSummary(PlatformGameIdDisplaySupport.summary(accounts));
            vo.setGameIdCompactSummary(PlatformGameIdDisplaySupport.compactSummary(accounts));
        }
    }

    private Map<Long, List<BasketPlayerPlatformAccount>> loadActivePlatformAccounts(List<Long> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketPlayerPlatformAccount> rows = platformAccountMapper.selectActiveByPlayerIds(playerIds);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<BasketPlayerPlatformAccount>> map = new java.util.HashMap<>();
        for (BasketPlayerPlatformAccount row : rows) {
            if (row.getPlayerId() == null) {
                continue;
            }
            map.computeIfAbsent(row.getPlayerId(), key -> new java.util.ArrayList<>()).add(row);
        }
        return map;
    }

    private void applyCertDisplay(AccountPlayerListVo vo) {
        if (vo.getPlayerId() == null) {
            vo.setCertDisplayStatus("NO_PROFILE");
            vo.setCertDisplayStatusName("未建档");
            return;
        }
        if (vo.getCertificationStatus() != null
                && vo.getCertificationStatus() == PlayerCertStatusEnum.APPROVED.getCode()) {
            vo.setCertDisplayStatus("APPROVED");
            vo.setCertDisplayStatusName("已认证");
            return;
        }
        Integer latest = vo.getLatestApplicationStatus();
        if (latest != null && (latest == PlayerCertStatusEnum.SUBMITTED.getCode()
                || latest == PlayerCertStatusEnum.REVIEWING.getCode())) {
            vo.setCertDisplayStatus("PENDING");
            vo.setCertDisplayStatusName("待审核");
            return;
        }
        if (latest != null && latest == PlayerCertStatusEnum.REJECTED.getCode()) {
            vo.setCertDisplayStatus("REJECTED");
            vo.setCertDisplayStatusName("已驳回");
            return;
        }
        vo.setCertDisplayStatus("NOT_CERTIFIED");
        vo.setCertDisplayStatusName("未认证");
    }

    private String resolveAccountStatusName(Integer accountStatus) {
        if (accountStatus == null) {
            return "-";
        }
        if (accountStatus == ACCOUNT_DISABLED) {
            return "禁用";
        }
        if (accountStatus == ACCOUNT_NORMAL) {
            return "正常";
        }
        return String.valueOf(accountStatus);
    }

    private AccountPlayerAccountVo buildAccountSection(AccountPlayerDetailVo base) {
        AccountPlayerAccountVo account = new AccountPlayerAccountVo();
        account.setMemberId(base.getMemberId());
        account.setUserId(base.getMemberId());
        account.setAccountNo(base.getAccountNo());
        account.setUsername(base.getUsername());
        account.setEmail(base.getEmail());
        account.setEmailVerified(base.getEmailVerified());
        account.setAccountStatus(base.getAccountStatus());
        account.setAccountStatusName(resolveAccountStatusName(base.getAccountStatus()));
        account.setLastLoginTime(base.getLastLoginTime());
        account.setLastLoginIp(base.getLastLoginIp());
        account.setCreateTime(base.getAccountCreateTime());
        account.setUpdateTime(base.getAccountUpdateTime());
        account.setAvatarUrl(MemberAvatarSupport.resolve(base.getAvatarUrl()));
        account.setNickname(base.getNickname());
        return account;
    }

    private AccountPlayerProfileVo buildProfileSection(AccountPlayerDetailVo base) {
        AccountPlayerProfileVo profile = new AccountPlayerProfileVo();
        profile.setPlayerId(base.getPlayerId());
        profile.setMemberId(base.getMemberId());
        profile.setNickname(base.getNickname());
        profile.setGender(base.getGender());
        profile.setProfileStatus(base.getProfileStatus());
        profile.setPlayerNo(base.getPlayerNo());
        profile.setAvatarUrl(MemberAvatarSupport.resolve(base.getAvatarUrl()));
        profile.setGameTerminal(base.getGameTerminal());
        profile.setGameAccount(base.getGameAccount());
        profile.setGameNickname(base.getGameNickname());
        profile.setPositionCode(base.getPositionCode());
        profile.setCertificationStatus(base.getCertificationStatus());
        if (base.getCertificationStatus() != null) {
            profile.setCertificationStatusName(
                    PlayerCertStatusEnum.fromCode(base.getCertificationStatus()).getDescription());
        }
        profile.setPlayerStatus(base.getPlayerStatus());
        profile.setCertifiedTime(base.getCertifiedTime());
        profile.setCurrentTeamId(base.getCurrentTeamId());
        profile.setCurrentTeamName(base.getCurrentTeamName());
        return profile;
    }

    private List<AccountPlayerPlatformAccountVo> loadPlatformAccounts(Long playerId) {
        if (playerId == null) {
            return Collections.emptyList();
        }
        return platformAccountMapper.selectByPlayerId(playerId).stream().map(record -> {
            AccountPlayerPlatformAccountVo vo = new AccountPlayerPlatformAccountVo();
            BeanUtils.copyBeanProp(vo, record);
            vo.setAccountStatusName(PlatformAccountStatusEnum.fromCode(record.getAccountStatus()).getDescription());
            return vo;
        }).collect(Collectors.toList());
    }

    private List<AccountPlayerCertHistoryVo> loadCertHistory(Long memberId) {
        return certApplicationMapper.selectByMemberId(memberId).stream().map(app -> {
            AccountPlayerCertHistoryVo vo = new AccountPlayerCertHistoryVo();
            vo.setApplicationId(app.getId());
            vo.setApplicationNo(app.getApplicationNo());
            vo.setGameTerminal(app.getGameTerminal());
            vo.setGameAccount(app.getGameAccount());
            vo.setApplicationStatus(app.getApplicationStatus());
            vo.setApplicationStatusName(PlayerCertStatusEnum.fromCode(app.getApplicationStatus()).getDescription());
            vo.setSubmittedTime(app.getSubmittedTime());
            vo.setReviewedTime(app.getReviewedTime());
            vo.setReviewRemark(app.getReviewRemark());
            return vo;
        }).collect(Collectors.toList());
    }

    private AccountPlayerCaptainQualVo loadCaptainQual(Long playerId) {
        if (playerId == null) {
            return null;
        }
        BasketCaptainQualification qual = captainQualificationMapper.selectCurrentByPlayerId(playerId);
        if (qual == null) {
            return null;
        }
        BasketPlayerProfile profile = playerProfileMapper.selectById(playerId);
        Integer certStatus = profile != null ? profile.getCertificationStatus() : null;
        String invalidReason = CaptainQualificationValiditySupport.resolveInvalidReason(qual, certStatus, new java.util.Date());
        AccountPlayerCaptainQualVo vo = new AccountPlayerCaptainQualVo();
        vo.setQualificationId(qual.getId());
        vo.setQualificationNo(qual.getQualificationNo());
        vo.setQualificationStatus(qual.getQualificationStatus());
        vo.setQualificationStatusName(
                CaptainQualificationStatusEnum.fromCode(qual.getQualificationStatus()).getDescription());
        vo.setIsCurrent(qual.getIsCurrent());
        vo.setGrantedTime(qual.getGrantedTime());
        vo.setValidFrom(qual.getValidFrom());
        vo.setValidTo(qual.getValidTo());
        vo.setGrantReason(qual.getGrantReason());
        vo.setRuntimeValid(invalidReason == null ? 1 : 0);
        vo.setRuntimeInvalidReason(invalidReason);
        return vo;
    }

    private List<AccountPlayerCaptainGrantVo> loadCaptainGrants(Long memberId) {
        return captainQualificationMapper.selectGrantHistoryByMemberId(memberId).stream().peek(vo -> {
            if (vo.getQualificationStatus() != null) {
                vo.setQualificationStatusName(
                        CaptainQualificationStatusEnum.fromCode(vo.getQualificationStatus()).getDescription());
            }
        }).collect(Collectors.toList());
    }

    private List<AccountPlayerTeamSummaryVo> loadTeamSummary(Long memberId) {
        return teamMemberMapper.selectTeamSummaryByMemberId(memberId).stream().peek(vo -> {
            if (vo.getAuditStatus() != null) {
                vo.setAuditStatusName(TeamAuditStatusEnum.fromCode(vo.getAuditStatus()).getDescription());
            }
            vo.setMembershipStatusName(resolveMembershipStatusName(vo.getMembershipStatus()));
        }).collect(Collectors.toList());
    }

    private List<AccountPlayerRegistrationVo> loadRegistrations(Long memberId) {
        return registrationMapper.selectSummaryByMemberId(memberId).stream().peek(vo -> {
            if (vo.getRegistrationStatus() != null) {
                RegistrationStatusEnum status = RegistrationStatusEnum.fromCode(vo.getRegistrationStatus());
                vo.setRegistrationStatusName(status.getDescription());
                vo.setAuditStatusName(status.getAuditDisplayDescription());
            }
        }).collect(Collectors.toList());
    }

    private List<AccountPlayerNotificationVo> loadNotifications(Long memberId) {
        return messageReceiverMapper.selectAccountNotificationsByMemberId(memberId).stream().peek(vo -> {
            vo.setReadStatusName(Objects.equals(vo.getReadStatus(), 1) ? "已读" : "未读");
        }).collect(Collectors.toList());
    }

    private List<AccountPlayerOperationLogVo> loadOperationLogs(Long memberId) {
        return securityLogMapper.selectByMemberId(memberId);
    }

    private String resolveMembershipStatusName(Integer membershipStatus) {
        if (membershipStatus == null) {
            return "-";
        }
        if (membershipStatus == TeamMembershipStatusEnum.ACTIVE.getCode()) {
            return "有效";
        }
        if (membershipStatus == TeamMembershipStatusEnum.INACTIVE.getCode()) {
            return "无效";
        }
        return String.valueOf(membershipStatus);
    }
}
