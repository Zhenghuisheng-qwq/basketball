package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamSeasonProfileDao;
import com.zhs.dao.BasketTournamentDao;
import com.zhs.dao.BasketTournamentRosterChangeRecordDao;
import com.zhs.dao.BasketTournamentRosterChangeRequestDao;
import com.zhs.dao.BasketTournamentSeasonDao;
import com.zhs.dao.BasketTournamentSeasonRosterDao;
import com.zhs.enums.ParticipationStatusEnum;
import com.zhs.enums.RosterChangeTypeEnum;
import com.zhs.enums.RosterLockStatusEnum;
import com.zhs.enums.RosterRoleEnum;
import com.zhs.enums.RosterStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeamSeasonProfile;
import com.zhs.model.BasketTournament;
import com.zhs.model.BasketTournamentRosterChangeRecord;
import com.zhs.model.BasketTournamentRosterChangeRequest;
import com.zhs.model.BasketTournamentSeason;
import com.zhs.model.BasketTournamentSeasonRoster;
import com.zhs.response.participation.EventRosterMemberResp;
import com.zhs.response.participation.EventTeamDetailResp;
import com.zhs.response.participation.EventTeamSummaryResp;
import com.zhs.response.participation.RosterChangeHistoryItemResp;
import com.zhs.service.IEventParticipationQueryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EventParticipationQueryServiceImpl implements IEventParticipationQueryService {

    private static final Integer NOT_DELETED = 0;
    private static final int IS_CAPTAIN = 1;

    @Resource
    private BasketTournamentSeasonRosterDao seasonRosterDao;
    @Resource
    private BasketTeamSeasonProfileDao eventTeamDao;
    @Resource
    private BasketTournamentSeasonDao seasonDao;
    @Resource
    private BasketTournamentDao tournamentDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketTournamentRosterChangeRecordDao rosterChangeRecordDao;
    @Resource
    private BasketTournamentRosterChangeRequestDao rosterChangeRequestDao;

    @Override
    public List<EventTeamSummaryResp> getMyEventTeams(String seasonId) {
        Long memberId = UserContext.getRequiredUserId();
        LambdaQueryWrapper<BasketTournamentSeasonRoster> wrapper = new LambdaQueryWrapper<BasketTournamentSeasonRoster>()
                .eq(BasketTournamentSeasonRoster::getMemberId, memberId)
                .eq(BasketTournamentSeasonRoster::getRosterStatus, RosterStatusEnum.ACTIVE.getCode())
                .eq(BasketTournamentSeasonRoster::getIsDelete, NOT_DELETED)
                .isNotNull(BasketTournamentSeasonRoster::getEventTeamId);
        if (StringUtils.hasText(seasonId)) {
            wrapper.eq(BasketTournamentSeasonRoster::getSeasonId, parseLongId(seasonId, "赛季主键"));
        }

        List<BasketTournamentSeasonRoster> myRosters = seasonRosterDao.selectList(wrapper);
        if (myRosters.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> eventTeamIds = myRosters.stream()
                .map(BasketTournamentSeasonRoster::getEventTeamId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, BasketTeamSeasonProfile> eventTeamMap = loadEventTeamMap(eventTeamIds);
        Map<Long, BasketTournamentSeason> seasonMap = loadSeasonMap(eventTeamMap.values());
        Map<Long, BasketTournament> tournamentMap = loadTournamentMap(seasonMap.values());
        Map<Long, Long> rosterCountMap = countActiveRosterByEventTeam(eventTeamIds);

        List<EventTeamSummaryResp> result = new ArrayList<>();
        for (BasketTournamentSeasonRoster roster : myRosters) {
            BasketTeamSeasonProfile eventTeam = eventTeamMap.get(roster.getEventTeamId());
            if (eventTeam == null) {
                continue;
            }
            BasketTournamentSeason season = seasonMap.get(eventTeam.getSeasonId());
            BasketTournament tournament = season == null ? null : tournamentMap.get(season.getTournamentId());
            ParticipationStatusEnum participationStatus = ParticipationStatusEnum.fromCode(eventTeam.getParticipationStatus());
            RosterLockStatusEnum lockStatus = resolveEffectiveLockStatus(eventTeam);

            EventTeamSummaryResp resp = new EventTeamSummaryResp();
            resp.setEventTeamId(toStringId(eventTeam.getId()));
            resp.setEventTeamNo(eventTeam.getEventTeamNo());
            resp.setTeamId(toStringId(eventTeam.getTeamId()));
            resp.setTeamName(firstNonBlank(eventTeam.getTeamNameSnapshot(), eventTeam.getDisplayName()));
            resp.setShortName(eventTeam.getShortNameSnapshot());
            resp.setSeasonId(toStringId(eventTeam.getSeasonId()));
            resp.setSeasonName(season == null ? null : season.getSeasonName());
            resp.setTournamentName(tournament == null ? null : tournament.getTournamentName());
            resp.setGameTerminal(eventTeam.getGameTerminal());
            resp.setParticipationStatus(participationStatus.getCode());
            resp.setParticipationStatusText(participationStatus.getDescription());
            resp.setRosterLockStatus(lockStatus.getCode());
            resp.setRosterLockStatusText(lockStatus.getDescription());
            resp.setRosterCount(rosterCountMap.getOrDefault(eventTeam.getId(), 0L).intValue());
            resp.setIsCaptain(Integer.valueOf(IS_CAPTAIN).equals(roster.getIsCaptain()));
            resp.setRosterRole(roster.getRosterRole());
            result.add(resp);
        }
        result.sort(Comparator.comparing(EventTeamSummaryResp::getSeasonId, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    @Override
    public EventTeamDetailResp getEventTeamDetail(String eventTeamId) {
        Long eventTeamPk = parseLongId(eventTeamId, "赛事球队主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeamSeasonProfile eventTeam = requireEventTeam(eventTeamPk);
        requireEventTeamMember(memberId, eventTeamPk, eventTeam.getSeasonId());

        BasketTournamentSeason season = seasonDao.selectById(eventTeam.getSeasonId());
        BasketTournament tournament = season == null ? null : tournamentDao.selectById(season.getTournamentId());
        BasketTournamentSeasonRoster captainRoster = findCaptainRoster(eventTeamPk);
        BasketTournamentSeasonRoster myRoster = findMemberRoster(memberId, eventTeamPk);
        long rosterCount = countActiveRoster(eventTeamPk);
        int freeAgentUsed = countFreeAgentUsed(eventTeam.getSeasonId(), eventTeamPk);

        ParticipationStatusEnum participationStatus = ParticipationStatusEnum.fromCode(eventTeam.getParticipationStatus());
        RosterLockStatusEnum lockStatus = resolveEffectiveLockStatus(eventTeam);

        EventTeamDetailResp resp = new EventTeamDetailResp();
        resp.setEventTeamId(toStringId(eventTeam.getId()));
        resp.setEventTeamNo(eventTeam.getEventTeamNo());
        resp.setTeamId(toStringId(eventTeam.getTeamId()));
        resp.setTeamName(firstNonBlank(eventTeam.getTeamNameSnapshot(), eventTeam.getDisplayName()));
        resp.setShortName(eventTeam.getShortNameSnapshot());
        resp.setDescription(eventTeam.getDescriptionSnapshot());
        resp.setSeasonId(toStringId(eventTeam.getSeasonId()));
        resp.setSeasonName(season == null ? null : season.getSeasonName());
        resp.setTournamentId(season == null ? null : toStringId(season.getTournamentId()));
        resp.setTournamentName(tournament == null ? null : tournament.getTournamentName());
        resp.setGameTerminal(eventTeam.getGameTerminal());
        resp.setRegistrationSource(eventTeam.getRegistrationSource());
        resp.setParticipationStatus(participationStatus.getCode());
        resp.setParticipationStatusText(participationStatus.getDescription());
        resp.setRosterLockStatus(lockStatus.getCode());
        resp.setRosterLockStatusText(lockStatus.getDescription());
        resp.setRosterCount((int) rosterCount);
        resp.setMaxRosterCount(season == null ? null : season.getMaxRosterCount());
        resp.setMinRosterCount(season == null ? null : season.getMinRosterCount());
        resp.setTransferEnabled(season != null && Integer.valueOf(1).equals(season.getTransferEnabled()));
        resp.setFreeAgentEnabled(season != null && Integer.valueOf(1).equals(season.getFreeAgentEnabled()));
        resp.setTransferEndTime(season == null ? null : season.getTransferEndTime());
        resp.setFreeAgentEndTime(season == null ? null : season.getFreeAgentEndTime());
        resp.setFreeAgentQuota(season == null ? null : season.getMaxFreeAgentAddPerTeam());
        resp.setFreeAgentUsed(freeAgentUsed);
        if (captainRoster != null) {
            resp.setCaptainPlayerId(toStringId(captainRoster.getPlayerId()));
            BasketPlayerProfile captainProfile = playerProfileDao.selectById(captainRoster.getPlayerId());
            resp.setCaptainNickname(captainProfile == null ? captainRoster.getPlayerNicknameSnapshot() : captainProfile.getGameNickname());
        }
        resp.setCurrentUserIsCaptain(myRoster != null && Integer.valueOf(IS_CAPTAIN).equals(myRoster.getIsCaptain()));
        resp.setApprovedTime(eventTeam.getApprovedTime());
        return resp;
    }

    @Override
    public List<EventRosterMemberResp> getEventRoster(String eventTeamId) {
        Long eventTeamPk = parseLongId(eventTeamId, "赛事球队主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeamSeasonProfile eventTeam = requireEventTeam(eventTeamPk);
        requireEventTeamMember(memberId, eventTeamPk, eventTeam.getSeasonId());

        List<BasketTournamentSeasonRoster> rosterList = seasonRosterDao.selectList(
                new LambdaQueryWrapper<BasketTournamentSeasonRoster>()
                        .eq(BasketTournamentSeasonRoster::getEventTeamId, eventTeamPk)
                        .eq(BasketTournamentSeasonRoster::getRosterStatus, RosterStatusEnum.ACTIVE.getCode())
                        .eq(BasketTournamentSeasonRoster::getIsDelete, NOT_DELETED)
                        .orderByDesc(BasketTournamentSeasonRoster::getIsCaptain)
                        .orderByAsc(BasketTournamentSeasonRoster::getCreateTime));

        Map<Long, BasketPlayerProfile> playerMap = loadPlayerMap(rosterList);
        List<EventRosterMemberResp> result = new ArrayList<>();
        for (BasketTournamentSeasonRoster roster : rosterList) {
            result.add(toRosterMemberResp(roster, playerMap.get(roster.getPlayerId())));
        }
        return result;
    }

    @Override
    public List<RosterChangeHistoryItemResp> getRosterHistory(String seasonId, String playerId, String eventTeamId) {
        Long memberId = UserContext.getRequiredUserId();
        if (!StringUtils.hasText(seasonId)) {
            throw new ServiceException("赛季主键不能为空");
        }
        Long seasonPk = parseLongId(seasonId, "赛季主键");

        LambdaQueryWrapper<BasketTournamentRosterChangeRecord> wrapper = new LambdaQueryWrapper<BasketTournamentRosterChangeRecord>()
                .eq(BasketTournamentRosterChangeRecord::getSeasonId, seasonPk)
                .eq(BasketTournamentRosterChangeRecord::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketTournamentRosterChangeRecord::getCreateTime);

        if (StringUtils.hasText(playerId)) {
            Long playerPk = parseLongId(playerId, "球员主键");
            assertCanViewPlayerHistory(memberId, seasonPk, playerPk);
            wrapper.eq(BasketTournamentRosterChangeRecord::getPlayerId, playerPk);
        } else if (StringUtils.hasText(eventTeamId)) {
            Long eventTeamPk = parseLongId(eventTeamId, "赛事球队主键");
            requireEventTeamMember(memberId, eventTeamPk, seasonPk);
            wrapper.and(w -> w.eq(BasketTournamentRosterChangeRecord::getSourceEventTeamId, eventTeamPk)
                    .or().eq(BasketTournamentRosterChangeRecord::getTargetEventTeamId, eventTeamPk));
        } else {
            throw new ServiceException("请指定球员或赛事球队");
        }

        List<RosterChangeHistoryItemResp> result = new ArrayList<>();
        for (BasketTournamentRosterChangeRecord record : rosterChangeRecordDao.selectList(wrapper)) {
            RosterChangeHistoryItemResp resp = new RosterChangeHistoryItemResp();
            resp.setRecordId(toStringId(record.getId()));
            resp.setChangeType(record.getChangeType());
            try {
                resp.setChangeTypeText(RosterChangeTypeEnum.fromCode(record.getChangeType()).getDescription());
            } catch (ServiceException ex) {
                resp.setChangeTypeText(record.getChangeType());
            }
            resp.setPlayerId(toStringId(record.getPlayerId()));
            resp.setSourceEventTeamId(toStringId(record.getSourceEventTeamId()));
            resp.setTargetEventTeamId(toStringId(record.getTargetEventTeamId()));
            resp.setBeforeSnapshot(record.getBeforeSnapshot());
            resp.setAfterSnapshot(record.getAfterSnapshot());
            resp.setOperateReason(record.getOperateReason());
            resp.setOperatorName(record.getOperatorName());
            resp.setEffectiveTime(record.getEffectiveTime());
            resp.setCreateTime(record.getCreateTime());
            result.add(resp);
        }
        return result;
    }

    private void assertCanViewPlayerHistory(Long memberId, Long seasonId, Long playerId) {
        BasketPlayerProfile profile = playerProfileDao.selectById(playerId);
        if (profile != null && memberId.equals(profile.getMemberId())) {
            return;
        }
        Long count = seasonRosterDao.selectCount(new LambdaQueryWrapper<BasketTournamentSeasonRoster>()
                .eq(BasketTournamentSeasonRoster::getSeasonId, seasonId)
                .eq(BasketTournamentSeasonRoster::getMemberId, memberId)
                .eq(BasketTournamentSeasonRoster::getRosterStatus, RosterStatusEnum.ACTIVE.getCode())
                .eq(BasketTournamentSeasonRoster::getIsDelete, NOT_DELETED));
        if (count == null || count == 0) {
            throw new ServiceException("无权查看该阵容历史", 403);
        }
    }

    private void requireEventTeamMember(Long memberId, Long eventTeamId, Long seasonId) {
        Long count = seasonRosterDao.selectCount(new LambdaQueryWrapper<BasketTournamentSeasonRoster>()
                .eq(BasketTournamentSeasonRoster::getEventTeamId, eventTeamId)
                .eq(BasketTournamentSeasonRoster::getSeasonId, seasonId)
                .eq(BasketTournamentSeasonRoster::getMemberId, memberId)
                .eq(BasketTournamentSeasonRoster::getRosterStatus, RosterStatusEnum.ACTIVE.getCode())
                .eq(BasketTournamentSeasonRoster::getIsDelete, NOT_DELETED));
        if (count == null || count == 0) {
            throw new ServiceException("无权查看该赛事球队", 403);
        }
    }

    private BasketTeamSeasonProfile requireEventTeam(Long eventTeamId) {
        BasketTeamSeasonProfile eventTeam = eventTeamDao.selectById(eventTeamId);
        if (eventTeam == null || !NOT_DELETED.equals(eventTeam.getIsDelete())) {
            throw new ServiceException("赛事球队不存在");
        }
        return eventTeam;
    }

    private BasketTournamentSeasonRoster findCaptainRoster(Long eventTeamId) {
        return seasonRosterDao.selectOne(new LambdaQueryWrapper<BasketTournamentSeasonRoster>()
                .eq(BasketTournamentSeasonRoster::getEventTeamId, eventTeamId)
                .eq(BasketTournamentSeasonRoster::getIsCaptain, IS_CAPTAIN)
                .eq(BasketTournamentSeasonRoster::getRosterStatus, RosterStatusEnum.ACTIVE.getCode())
                .eq(BasketTournamentSeasonRoster::getIsDelete, NOT_DELETED)
                .last("limit 1"));
    }

    private BasketTournamentSeasonRoster findMemberRoster(Long memberId, Long eventTeamId) {
        return seasonRosterDao.selectOne(new LambdaQueryWrapper<BasketTournamentSeasonRoster>()
                .eq(BasketTournamentSeasonRoster::getEventTeamId, eventTeamId)
                .eq(BasketTournamentSeasonRoster::getMemberId, memberId)
                .eq(BasketTournamentSeasonRoster::getRosterStatus, RosterStatusEnum.ACTIVE.getCode())
                .eq(BasketTournamentSeasonRoster::getIsDelete, NOT_DELETED)
                .last("limit 1"));
    }

    private long countActiveRoster(Long eventTeamId) {
        Long count = seasonRosterDao.selectCount(new LambdaQueryWrapper<BasketTournamentSeasonRoster>()
                .eq(BasketTournamentSeasonRoster::getEventTeamId, eventTeamId)
                .eq(BasketTournamentSeasonRoster::getRosterStatus, RosterStatusEnum.ACTIVE.getCode())
                .eq(BasketTournamentSeasonRoster::getIsDelete, NOT_DELETED));
        return count == null ? 0 : count;
    }

    private int countFreeAgentUsed(Long seasonId, Long eventTeamId) {
        Long count = rosterChangeRequestDao.selectCount(new LambdaQueryWrapper<BasketTournamentRosterChangeRequest>()
                .eq(BasketTournamentRosterChangeRequest::getSeasonId, seasonId)
                .eq(BasketTournamentRosterChangeRequest::getTargetEventTeamId, eventTeamId)
                .eq(BasketTournamentRosterChangeRequest::getChangeType, RosterChangeTypeEnum.FREE_AGENT_ADD.getCode())
                .in(BasketTournamentRosterChangeRequest::getRequestStatus, 0, 1)
                .eq(BasketTournamentRosterChangeRequest::getIsDelete, NOT_DELETED));
        return count == null ? 0 : count.intValue();
    }

    private Map<Long, Long> countActiveRosterByEventTeam(Set<Long> eventTeamIds) {
        Map<Long, Long> map = new HashMap<>();
        for (Long eventTeamId : eventTeamIds) {
            map.put(eventTeamId, countActiveRoster(eventTeamId));
        }
        return map;
    }

    private Map<Long, BasketTeamSeasonProfile> loadEventTeamMap(Set<Long> eventTeamIds) {
        if (eventTeamIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, BasketTeamSeasonProfile> map = new HashMap<>();
        for (BasketTeamSeasonProfile profile : eventTeamDao.selectBatchIds(eventTeamIds)) {
            if (profile != null && NOT_DELETED.equals(profile.getIsDelete())) {
                map.put(profile.getId(), profile);
            }
        }
        return map;
    }

    private Map<Long, BasketTournamentSeason> loadSeasonMap(java.util.Collection<BasketTeamSeasonProfile> eventTeams) {
        Set<Long> seasonIds = eventTeams.stream().map(BasketTeamSeasonProfile::getSeasonId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (seasonIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, BasketTournamentSeason> map = new HashMap<>();
        for (BasketTournamentSeason season : seasonDao.selectBatchIds(seasonIds)) {
            if (season != null && NOT_DELETED.equals(season.getIsDelete())) {
                map.put(season.getId(), season);
            }
        }
        return map;
    }

    private Map<Long, BasketTournament> loadTournamentMap(java.util.Collection<BasketTournamentSeason> seasons) {
        Set<Long> tournamentIds = seasons.stream().map(BasketTournamentSeason::getTournamentId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (tournamentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, BasketTournament> map = new HashMap<>();
        for (BasketTournament tournament : tournamentDao.selectBatchIds(tournamentIds)) {
            if (tournament != null && NOT_DELETED.equals(tournament.getIsDelete())) {
                map.put(tournament.getId(), tournament);
            }
        }
        return map;
    }

    private Map<Long, BasketPlayerProfile> loadPlayerMap(List<BasketTournamentSeasonRoster> rosterList) {
        Set<Long> playerIds = rosterList.stream().map(BasketTournamentSeasonRoster::getPlayerId).collect(Collectors.toSet());
        if (playerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, BasketPlayerProfile> map = new HashMap<>();
        for (BasketPlayerProfile profile : playerProfileDao.selectBatchIds(playerIds)) {
            if (profile != null && NOT_DELETED.equals(profile.getIsDelete())) {
                map.put(profile.getId(), profile);
            }
        }
        return map;
    }

    private EventRosterMemberResp toRosterMemberResp(BasketTournamentSeasonRoster roster, BasketPlayerProfile profile) {
        RosterStatusEnum rosterStatus = RosterStatusEnum.fromCode(roster.getRosterStatus());
        RosterRoleEnum role = RosterRoleEnum.fromCode(roster.getRosterRole());
        EventRosterMemberResp resp = new EventRosterMemberResp();
        resp.setRosterId(toStringId(roster.getId()));
        resp.setPlayerId(toStringId(roster.getPlayerId()));
        resp.setPlayerNo(profile == null ? null : profile.getPlayerNo());
        resp.setNickname(firstNonBlank(roster.getPlayerNicknameSnapshot(), profile == null ? null : profile.getGameNickname()));
        // 优先使用 EventPlayer/阵容快照；无快照时展示“未绑定”，避免用当前最新游戏ID覆盖历史
        resp.setPlatform(StringUtils.hasText(roster.getPlatformSnapshot()) ? roster.getPlatformSnapshot() : "-");
        resp.setGameId(StringUtils.hasText(roster.getGameIdDisplaySnapshot()) ? roster.getGameIdDisplaySnapshot() : "未绑定");
        resp.setPosition(roster.getPositionSnapshot());
        resp.setRosterRole(role.getCode());
        resp.setRosterRoleText(role.getDescription());
        resp.setIsCaptain(roster.getIsCaptain());
        resp.setIsStarting(roster.getIsStarting());
        resp.setRosterStatus(rosterStatus.getCode());
        resp.setRosterStatusText(rosterStatus.getDescription());
        resp.setSourceType(roster.getSourceType());
        resp.setEffectiveTime(roster.getEffectiveTime());
        return resp;
    }

    private RosterLockStatusEnum resolveEffectiveLockStatus(BasketTeamSeasonProfile eventTeam) {
        if (eventTeam == null) {
            return RosterLockStatusEnum.UNLOCKED;
        }
        if (Integer.valueOf(RosterLockStatusEnum.LOCKED.getCode()).equals(eventTeam.getRosterLockStatus())) {
            return RosterLockStatusEnum.LOCKED;
        }
        if (eventTeam.getApprovedTime() != null) {
            return RosterLockStatusEnum.LOCKED;
        }
        return RosterLockStatusEnum.UNLOCKED;
    }

    private RosterLockStatusEnum resolveLockStatus(Integer code) {
        if (code == null) {
            return RosterLockStatusEnum.UNLOCKED;
        }
        for (RosterLockStatusEnum value : RosterLockStatusEnum.values()) {
            if (value.getCode() == code) {
                return value;
            }
        }
        return RosterLockStatusEnum.UNLOCKED;
    }

    private String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        return second;
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
}
