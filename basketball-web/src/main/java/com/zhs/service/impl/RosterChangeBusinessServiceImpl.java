package com.zhs.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketCaptainQualificationDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamSeasonProfileDao;
import com.zhs.dao.BasketTournamentRosterChangeRequestDao;
import com.zhs.dao.BasketTournamentSeasonDao;
import com.zhs.dao.BasketTournamentSeasonRosterDao;
import com.zhs.enums.CaptainQualificationStatusEnum;
import com.zhs.enums.ParticipationStatusEnum;
import com.zhs.enums.PlayerStatusEnum;
import com.zhs.enums.RosterChangeStatusEnum;
import com.zhs.enums.RosterChangeTypeEnum;
import com.zhs.enums.RosterLockStatusEnum;
import com.zhs.enums.RosterRoleEnum;
import com.zhs.enums.RosterStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketCaptainQualification;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeamSeasonProfile;
import com.zhs.model.BasketTournamentRosterChangeRequest;
import com.zhs.model.BasketTournamentSeason;
import com.zhs.model.BasketTournamentSeasonRoster;
import com.zhs.request.participation.RosterChangeCaptainTransferReq;
import com.zhs.request.participation.RosterChangeFreeAgentAddReq;
import com.zhs.request.participation.RosterChangeTransferReq;
import com.zhs.response.participation.RosterChangeRequestItemResp;
import com.zhs.response.participation.RosterChangeSubmitResp;
import com.zhs.service.IRosterChangeBusinessService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class RosterChangeBusinessServiceImpl implements IRosterChangeBusinessService {

    private static final Integer NOT_DELETED = 0;
    private static final int ENABLED = 1;
    private static final int IS_CAPTAIN = 1;
    private static final int IS_CURRENT = 1;

    @Resource
    private BasketTournamentRosterChangeRequestDao rosterChangeRequestDao;
    @Resource
    private BasketTournamentSeasonRosterDao seasonRosterDao;
    @Resource
    private BasketTeamSeasonProfileDao eventTeamDao;
    @Resource
    private BasketTournamentSeasonDao seasonDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketCaptainQualificationDao captainQualificationDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RosterChangeSubmitResp submitTransfer(RosterChangeTransferReq req) {
        if (req == null) {
            throw new ServiceException("请求参数不能为空");
        }
        Long memberId = UserContext.getRequiredUserId();
        BasketPlayerProfile profile = requirePlayerByMember(memberId);
        assertPlayerNotRestricted(profile);

        Long seasonPk = parseLongId(req.getSeasonId(), "赛季主键");
        Long sourceEventTeamPk = parseLongId(req.getSourceEventTeamId(), "原赛事球队主键");
        Long targetEventTeamPk = parseLongId(req.getTargetEventTeamId(), "目标赛事球队主键");
        if (sourceEventTeamPk.equals(targetEventTeamPk)) {
            throw new ServiceException("原球队与目标球队不能相同");
        }

        BasketTournamentSeason season = requireSeason(seasonPk);
        Date now = new Date();
        validateTransferWindow(season, now);

        BasketTeamSeasonProfile sourceTeam = requireActiveEventTeam(sourceEventTeamPk, seasonPk);
        BasketTeamSeasonProfile targetTeam = requireActiveEventTeam(targetEventTeamPk, seasonPk);
        validateEventTeamNotLocked(sourceTeam);
        validateEventTeamNotLocked(targetTeam);
        validateRosterCapacity(season, targetEventTeamPk);

        BasketTournamentSeasonRoster sourceRoster = requireActiveRosterOnTeam(profile.getId(), sourceEventTeamPk, seasonPk);
        assertNoPendingChange(seasonPk, profile.getId(), RosterChangeTypeEnum.TRANSFER);

        return insertPendingRequest(seasonPk, RosterChangeTypeEnum.TRANSFER, memberId, profile.getId(),
                sourceEventTeamPk, targetEventTeamPk, sourceRoster.getId(), null,
                null, null, null, trimToNull(req.getApplyReason()), now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RosterChangeSubmitResp submitFreeAgentAdd(RosterChangeFreeAgentAddReq req) {
        if (req == null) {
            throw new ServiceException("请求参数不能为空");
        }
        Long memberId = UserContext.getRequiredUserId();
        Long seasonPk = parseLongId(req.getSeasonId(), "赛季主键");
        Long targetEventTeamPk = parseLongId(req.getTargetEventTeamId(), "目标赛事球队主键");
        Long playerPk = parseLongId(req.getPlayerId(), "球员主键");

        BasketTournamentSeason season = requireSeason(seasonPk);
        Date now = new Date();
        validateFreeAgentWindow(season, now);

        BasketTeamSeasonProfile targetTeam = requireActiveEventTeam(targetEventTeamPk, seasonPk);
        validateEventTeamNotLocked(targetTeam);
        requireEventTeamCaptain(memberId, targetEventTeamPk, seasonPk);
        validateRosterCapacity(season, targetEventTeamPk);
        validateFreeAgentQuota(season, targetEventTeamPk);

        BasketPlayerProfile player = requirePlayer(playerPk);
        assertPlayerNotRestricted(player);
        assertPlayerFreeInSeason(playerPk, seasonPk);
        assertNoPendingChange(seasonPk, playerPk, RosterChangeTypeEnum.FREE_AGENT_ADD);

        String targetRole = StringUtils.hasText(req.getTargetRosterRole())
                ? req.getTargetRosterRole().trim().toUpperCase() : RosterRoleEnum.PLAYER.getCode();
        RosterRoleEnum.fromCode(targetRole);

        return insertPendingRequest(seasonPk, RosterChangeTypeEnum.FREE_AGENT_ADD, memberId, playerPk,
                null, targetEventTeamPk, null, null, targetRole,
                req.getTargetIsStarting(), req.getTargetIsSubstitute(), trimToNull(req.getApplyReason()), now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RosterChangeSubmitResp submitCaptainTransfer(RosterChangeCaptainTransferReq req) {
        if (req == null) {
            throw new ServiceException("请求参数不能为空");
        }
        Long memberId = UserContext.getRequiredUserId();
        requireValidCaptainQualification(memberId);

        Long eventTeamPk = parseLongId(req.getEventTeamId(), "赛事球队主键");
        Long newCaptainPlayerPk = parseLongId(req.getNewCaptainPlayerId(), "新队长球员主键");

        BasketTeamSeasonProfile eventTeam = requireEventTeam(eventTeamPk);
        BasketTournamentSeason season = requireSeason(eventTeam.getSeasonId());
        validateEventTeamNotLocked(eventTeam);
        requireEventTeamCaptain(memberId, eventTeamPk, eventTeam.getSeasonId());

        BasketTournamentSeasonRoster currentCaptainRoster = requireCaptainRoster(eventTeamPk);
        if (newCaptainPlayerPk.equals(currentCaptainRoster.getPlayerId())) {
            throw new ServiceException("新队长不能与当前队长相同");
        }

        BasketTournamentSeasonRoster newCaptainRoster = requireActiveRosterOnTeam(newCaptainPlayerPk, eventTeamPk, eventTeam.getSeasonId());
        assertNoPendingChange(eventTeam.getSeasonId(), currentCaptainRoster.getPlayerId(), RosterChangeTypeEnum.CAPTAIN_TRANSFER);

        Date now = new Date();
        return insertPendingRequest(eventTeam.getSeasonId(), RosterChangeTypeEnum.CAPTAIN_TRANSFER, memberId,
                currentCaptainRoster.getPlayerId(), eventTeamPk, eventTeamPk, currentCaptainRoster.getId(),
                newCaptainPlayerPk, newCaptainRoster.getRosterRole(), newCaptainRoster.getIsStarting(),
                newCaptainRoster.getIsSubstitute(), trimToNull(req.getApplyReason()), now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawRequest(String requestId) {
        Long requestPk = parseLongId(requestId, "申请主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTournamentRosterChangeRequest request = requireOwnedRequest(memberId, requestPk);
        if (!RosterChangeStatusEnum.fromCode(request.getRequestStatus()).canWithdraw()) {
            throw new ServiceException("当前状态不可撤回");
        }

        int updated = rosterChangeRequestDao.update(null, new LambdaUpdateWrapper<BasketTournamentRosterChangeRequest>()
                .eq(BasketTournamentRosterChangeRequest::getId, requestPk)
                .eq(BasketTournamentRosterChangeRequest::getApplicantMemberId, memberId)
                .eq(BasketTournamentRosterChangeRequest::getRequestStatus, RosterChangeStatusEnum.PENDING.getCode())
                .eq(BasketTournamentRosterChangeRequest::getIsDelete, NOT_DELETED)
                .set(BasketTournamentRosterChangeRequest::getRequestStatus, RosterChangeStatusEnum.WITHDRAWN.getCode())
                .set(BasketTournamentRosterChangeRequest::getReviewedTime, new Date()));
        if (updated == 0) {
            throw new ServiceException("撤回失败，申请状态已变更");
        }
    }

    @Override
    public IPage<RosterChangeRequestItemResp> pageMyRequests(Integer currentPage, Integer pageSize, Integer status) {
        Long memberId = UserContext.getRequiredUserId();
        LambdaQueryWrapper<BasketTournamentRosterChangeRequest> wrapper = new LambdaQueryWrapper<BasketTournamentRosterChangeRequest>()
                .eq(BasketTournamentRosterChangeRequest::getApplicantMemberId, memberId)
                .eq(BasketTournamentRosterChangeRequest::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketTournamentRosterChangeRequest::getSubmitTime);
        if (status != null) {
            wrapper.eq(BasketTournamentRosterChangeRequest::getRequestStatus, status);
        }

        Page<BasketTournamentRosterChangeRequest> page = new Page<>(normalizePage(currentPage), normalizeSize(pageSize));
        IPage<BasketTournamentRosterChangeRequest> requestPage = rosterChangeRequestDao.selectPage(page, wrapper);

        Page<RosterChangeRequestItemResp> respPage = new Page<>(requestPage.getCurrent(), requestPage.getSize(), requestPage.getTotal());
        List<RosterChangeRequestItemResp> items = new ArrayList<>();
        for (BasketTournamentRosterChangeRequest request : requestPage.getRecords()) {
            items.add(toItemResp(request));
        }
        respPage.setRecords(items);
        return respPage;
    }

    void validateEventTeamNotLocked(BasketTeamSeasonProfile eventTeam) {
        if (eventTeam == null) {
            throw new ServiceException("赛事球队不存在");
        }
        if (Integer.valueOf(RosterLockStatusEnum.LOCKED.getCode()).equals(eventTeam.getRosterLockStatus())) {
            throw new ServiceException("赛事球队阵容已锁定，无法提交变更");
        }
    }

    void validateTransferWindow(BasketTournamentSeason season, Date now) {
        if (season == null) {
            throw new ServiceException("赛季不存在");
        }
        if (!Integer.valueOf(ENABLED).equals(season.getTransferEnabled())) {
            throw new ServiceException("该赛季未开放转会");
        }
        if (season.getTransferStartTime() != null && season.getTransferStartTime().after(now)) {
            throw new ServiceException("转会窗口尚未开始");
        }
        if (season.getTransferEndTime() != null && season.getTransferEndTime().before(now)) {
            throw new ServiceException("转会窗口已结束");
        }
    }

    void validateFreeAgentWindow(BasketTournamentSeason season, Date now) {
        if (season == null) {
            throw new ServiceException("赛季不存在");
        }
        if (!Integer.valueOf(ENABLED).equals(season.getFreeAgentEnabled())) {
            throw new ServiceException("该赛季未开放自由球员补员");
        }
        if (season.getFreeAgentStartTime() != null && season.getFreeAgentStartTime().after(now)) {
            throw new ServiceException("补员窗口尚未开始");
        }
        if (season.getFreeAgentEndTime() != null && season.getFreeAgentEndTime().before(now)) {
            throw new ServiceException("补员窗口已结束");
        }
    }

    void validateFreeAgentQuota(BasketTournamentSeason season, Long eventTeamId) {
        int maxQuota = season.getMaxFreeAgentAddPerTeam() == null ? 3 : season.getMaxFreeAgentAddPerTeam();
        Long used = rosterChangeRequestDao.selectCount(new LambdaQueryWrapper<BasketTournamentRosterChangeRequest>()
                .eq(BasketTournamentRosterChangeRequest::getSeasonId, season.getId())
                .eq(BasketTournamentRosterChangeRequest::getTargetEventTeamId, eventTeamId)
                .eq(BasketTournamentRosterChangeRequest::getChangeType, RosterChangeTypeEnum.FREE_AGENT_ADD.getCode())
                .in(BasketTournamentRosterChangeRequest::getRequestStatus,
                        RosterChangeStatusEnum.PENDING.getCode(),
                        RosterChangeStatusEnum.APPROVED.getCode())
                .eq(BasketTournamentRosterChangeRequest::getIsDelete, NOT_DELETED));
        if (used != null && used >= maxQuota) {
            throw new ServiceException("该队补员次数已达上限");
        }
    }

    void validateRosterCapacity(BasketTournamentSeason season, Long eventTeamId) {
        if (season.getMaxRosterCount() == null) {
            return;
        }
        Long count = seasonRosterDao.selectCount(new LambdaQueryWrapper<BasketTournamentSeasonRoster>()
                .eq(BasketTournamentSeasonRoster::getEventTeamId, eventTeamId)
                .eq(BasketTournamentSeasonRoster::getRosterStatus, RosterStatusEnum.ACTIVE.getCode())
                .eq(BasketTournamentSeasonRoster::getIsDelete, NOT_DELETED));
        if (count != null && count >= season.getMaxRosterCount()) {
            throw new ServiceException("目标球队阵容已满");
        }
    }

    private RosterChangeSubmitResp insertPendingRequest(Long seasonId, RosterChangeTypeEnum changeType,
                                                        Long applicantMemberId, Long playerId,
                                                        Long sourceEventTeamId, Long targetEventTeamId,
                                                        Long sourceRosterId, Long newCaptainPlayerId,
                                                        String targetRosterRole, Integer targetIsStarting,
                                                        Integer targetIsSubstitute, String applyReason, Date now) {
        BasketTournamentRosterChangeRequest request = new BasketTournamentRosterChangeRequest();
        request.setRequestNo("RC" + IdUtil.getSnowflakeNextIdStr());
        request.setSeasonId(seasonId);
        request.setChangeType(changeType.getCode());
        request.setRequestStatus(RosterChangeStatusEnum.PENDING.getCode());
        request.setApplicantMemberId(applicantMemberId);
        request.setPlayerId(playerId);
        request.setSourceEventTeamId(sourceEventTeamId);
        request.setTargetEventTeamId(targetEventTeamId);
        request.setSourceRosterId(sourceRosterId);
        request.setNewCaptainPlayerId(newCaptainPlayerId);
        request.setTargetRosterRole(targetRosterRole);
        request.setTargetIsStarting(targetIsStarting);
        request.setTargetIsSubstitute(targetIsSubstitute);
        request.setApplyReason(applyReason);
        request.setSubmitTime(now);
        request.setIsDelete(NOT_DELETED);
        rosterChangeRequestDao.insert(request);

        RosterChangeSubmitResp resp = new RosterChangeSubmitResp();
        resp.setRequestId(String.valueOf(request.getId()));
        resp.setRequestNo(request.getRequestNo());
        return resp;
    }

    private void assertNoPendingChange(Long seasonId, Long playerId, RosterChangeTypeEnum changeType) {
        Long count = rosterChangeRequestDao.selectCount(new LambdaQueryWrapper<BasketTournamentRosterChangeRequest>()
                .eq(BasketTournamentRosterChangeRequest::getSeasonId, seasonId)
                .eq(BasketTournamentRosterChangeRequest::getPlayerId, playerId)
                .eq(BasketTournamentRosterChangeRequest::getChangeType, changeType.getCode())
                .eq(BasketTournamentRosterChangeRequest::getRequestStatus, RosterChangeStatusEnum.PENDING.getCode())
                .eq(BasketTournamentRosterChangeRequest::getIsDelete, NOT_DELETED));
        if (count != null && count > 0) {
            throw new ServiceException("已有待审核的同类型申请");
        }
    }

    private void assertPlayerFreeInSeason(Long playerId, Long seasonId) {
        Long count = seasonRosterDao.selectCount(new LambdaQueryWrapper<BasketTournamentSeasonRoster>()
                .eq(BasketTournamentSeasonRoster::getSeasonId, seasonId)
                .eq(BasketTournamentSeasonRoster::getPlayerId, playerId)
                .eq(BasketTournamentSeasonRoster::getRosterStatus, RosterStatusEnum.ACTIVE.getCode())
                .eq(BasketTournamentSeasonRoster::getIsDelete, NOT_DELETED));
        if (count != null && count > 0) {
            throw new ServiceException("该球员已在赛季阵容中");
        }
    }

    private void requireEventTeamCaptain(Long memberId, Long eventTeamId, Long seasonId) {
        BasketTournamentSeasonRoster roster = seasonRosterDao.selectOne(new LambdaQueryWrapper<BasketTournamentSeasonRoster>()
                .eq(BasketTournamentSeasonRoster::getEventTeamId, eventTeamId)
                .eq(BasketTournamentSeasonRoster::getSeasonId, seasonId)
                .eq(BasketTournamentSeasonRoster::getMemberId, memberId)
                .eq(BasketTournamentSeasonRoster::getIsCaptain, IS_CAPTAIN)
                .eq(BasketTournamentSeasonRoster::getRosterStatus, RosterStatusEnum.ACTIVE.getCode())
                .eq(BasketTournamentSeasonRoster::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (roster == null) {
            throw new ServiceException("仅赛事队长可操作", 403);
        }
    }

    private BasketTournamentSeasonRoster requireCaptainRoster(Long eventTeamId) {
        BasketTournamentSeasonRoster roster = seasonRosterDao.selectOne(new LambdaQueryWrapper<BasketTournamentSeasonRoster>()
                .eq(BasketTournamentSeasonRoster::getEventTeamId, eventTeamId)
                .eq(BasketTournamentSeasonRoster::getIsCaptain, IS_CAPTAIN)
                .eq(BasketTournamentSeasonRoster::getRosterStatus, RosterStatusEnum.ACTIVE.getCode())
                .eq(BasketTournamentSeasonRoster::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (roster == null) {
            throw new ServiceException("赛事球队暂无队长");
        }
        return roster;
    }

    private BasketTournamentSeasonRoster requireActiveRosterOnTeam(Long playerId, Long eventTeamId, Long seasonId) {
        BasketTournamentSeasonRoster roster = seasonRosterDao.selectOne(new LambdaQueryWrapper<BasketTournamentSeasonRoster>()
                .eq(BasketTournamentSeasonRoster::getPlayerId, playerId)
                .eq(BasketTournamentSeasonRoster::getEventTeamId, eventTeamId)
                .eq(BasketTournamentSeasonRoster::getSeasonId, seasonId)
                .eq(BasketTournamentSeasonRoster::getRosterStatus, RosterStatusEnum.ACTIVE.getCode())
                .eq(BasketTournamentSeasonRoster::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (roster == null) {
            throw new ServiceException("球员不在该赛事球队阵容中");
        }
        return roster;
    }

    private BasketTeamSeasonProfile requireActiveEventTeam(Long eventTeamId, Long seasonId) {
        BasketTeamSeasonProfile eventTeam = requireEventTeam(eventTeamId);
        if (!seasonId.equals(eventTeam.getSeasonId())) {
            throw new ServiceException("赛事球队与赛季不匹配");
        }
        if (!Integer.valueOf(ParticipationStatusEnum.ACTIVE.getCode()).equals(eventTeam.getParticipationStatus())) {
            throw new ServiceException("赛事球队未处于参赛状态");
        }
        return eventTeam;
    }

    private BasketTeamSeasonProfile requireEventTeam(Long eventTeamId) {
        BasketTeamSeasonProfile eventTeam = eventTeamDao.selectById(eventTeamId);
        if (eventTeam == null || !NOT_DELETED.equals(eventTeam.getIsDelete())) {
            throw new ServiceException("赛事球队不存在");
        }
        return eventTeam;
    }

    private BasketTournamentSeason requireSeason(Long seasonId) {
        BasketTournamentSeason season = seasonDao.selectById(seasonId);
        if (season == null || !NOT_DELETED.equals(season.getIsDelete())) {
            throw new ServiceException("赛季不存在");
        }
        return season;
    }

    private BasketPlayerProfile requirePlayerByMember(Long memberId) {
        BasketPlayerProfile profile = playerProfileDao.selectOne(new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getMemberId, memberId)
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (profile == null) {
            throw new ServiceException("尚未完成球员认证");
        }
        return profile;
    }

    private BasketPlayerProfile requirePlayer(Long playerId) {
        BasketPlayerProfile profile = playerProfileDao.selectById(playerId);
        if (profile == null || !NOT_DELETED.equals(profile.getIsDelete())) {
            throw new ServiceException("球员不存在");
        }
        return profile;
    }

    private void assertPlayerNotRestricted(BasketPlayerProfile profile) {
        if (PlayerStatusEnum.fromCode(profile.getPlayerStatus()).isRestricted()) {
            throw new ServiceException("球员参赛资格受限");
        }
    }

    private void requireValidCaptainQualification(Long memberId) {
        BasketCaptainQualification qualification = captainQualificationDao.selectOne(
                new LambdaQueryWrapper<BasketCaptainQualification>()
                        .eq(BasketCaptainQualification::getMemberId, memberId)
                        .eq(BasketCaptainQualification::getIsCurrent, IS_CURRENT)
                        .eq(BasketCaptainQualification::getIsDelete, NOT_DELETED)
                        .orderByDesc(BasketCaptainQualification::getCreateTime)
                        .last("limit 1"));
        if (qualification == null
                || !Objects.equals(CaptainQualificationStatusEnum.ACTIVE.getCode(), qualification.getQualificationStatus())) {
            throw new ServiceException("队长资质未生效，无法移交队长");
        }
    }

    private BasketTournamentRosterChangeRequest requireOwnedRequest(Long memberId, Long requestPk) {
        BasketTournamentRosterChangeRequest request = rosterChangeRequestDao.selectById(requestPk);
        if (request == null || !NOT_DELETED.equals(request.getIsDelete())) {
            throw new ServiceException("申请不存在");
        }
        if (!memberId.equals(request.getApplicantMemberId())) {
            throw new ServiceException("无权操作该申请", 403);
        }
        return request;
    }

    private RosterChangeRequestItemResp toItemResp(BasketTournamentRosterChangeRequest request) {
        RosterChangeStatusEnum status = RosterChangeStatusEnum.fromCode(request.getRequestStatus());
        RosterChangeTypeEnum changeType = RosterChangeTypeEnum.fromCode(request.getChangeType());
        RosterChangeRequestItemResp resp = new RosterChangeRequestItemResp();
        resp.setRequestId(toStringId(request.getId()));
        resp.setRequestNo(request.getRequestNo());
        resp.setSeasonId(toStringId(request.getSeasonId()));
        resp.setChangeType(changeType.getCode());
        resp.setChangeTypeText(changeType.getDescription());
        resp.setRequestStatus(status.getCode());
        resp.setRequestStatusText(status.getLabel());
        resp.setPlayerId(toStringId(request.getPlayerId()));
        resp.setSourceEventTeamId(toStringId(request.getSourceEventTeamId()));
        resp.setTargetEventTeamId(toStringId(request.getTargetEventTeamId()));
        resp.setApplyReason(request.getApplyReason());
        resp.setRejectReason(request.getRejectReason());
        resp.setCanWithdraw(status.canWithdraw());
        resp.setSubmitTime(request.getSubmitTime());
        resp.setReviewedTime(request.getReviewedTime());
        return resp;
    }

    private long normalizePage(Integer currentPage) {
        return currentPage == null || currentPage < 1 ? 1L : currentPage;
    }

    private long normalizeSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, 100L);
    }

    private Long parseLongId(String id, String fieldName) {
        if (!StringUtils.hasText(id)) {
            throw new ServiceException(fieldName + "不能为空");
        }
        try {
            return Long.parseLong(id.trim());
        } catch (NumberFormatException ex) {
            throw new ServiceException(fieldName + "格式不正确");
        }
    }

    private String toStringId(Long id) {
        return id == null ? null : String.valueOf(id);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
