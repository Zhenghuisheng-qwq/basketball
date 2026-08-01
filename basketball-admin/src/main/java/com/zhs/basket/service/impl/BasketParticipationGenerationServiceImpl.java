package com.zhs.basket.service.impl;

import com.zhs.basket.domain.*;
import com.zhs.basket.enums.*;
import com.zhs.basket.mapper.*;
import com.zhs.basket.service.IBasketParticipationGenerationService;
import com.zhs.basket.support.EventTeamNoGenerator;
import com.zhs.basket.support.GameAccountNormalizer;
import com.zhs.basket.support.GameTerminalSupport;
import com.zhs.basket.support.RosterActiveSlotSupport;
import com.zhs.basket.support.RosterNoGenerator;
import com.zhs.basket.vo.RegistrationPlayerVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BasketParticipationGenerationServiceImpl implements IBasketParticipationGenerationService {

    private static final Integer NOT_DELETED = 0;
    private static final String REGISTRATION_SOURCE = "TEAM_REGISTRATION";
    private static final String BACKFILL_REMARK = "历史补偿生成";

    @Autowired private BasketTournamentRegistrationMapper registrationMapper;
    @Autowired private BasketTournamentRegistrationPlayerMapper registrationPlayerMapper;
    @Autowired private BasketTournamentRegistrationAuditMapper registrationAuditMapper;
    @Autowired private BasketTeamMapper teamMapper;
    @Autowired private BasketTournamentMapper tournamentMapper;
    @Autowired private BasketTournamentSeasonMapper seasonMapper;
    @Autowired private BasketTeamSeasonProfileMapper teamSeasonProfileMapper;
    @Autowired private BasketTournamentSeasonRosterMapper seasonRosterMapper;
    @Autowired private BasketPlayerProfileMapper profileMapper;
    @Autowired private BasketPlayerPlatformAccountMapper platformAccountMapper;
    @Autowired private BasketCaptainQualificationMapper captainQualificationMapper;
    @Autowired private EventTeamNoGenerator eventTeamNoGenerator;
    @Autowired private RosterNoGenerator rosterNoGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateFromTeamRegistration(Long registrationId, Long auditorId, String remark) {
        return doGenerate(registrationId, auditorId, remark, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long backfillFromApprovedRegistration(Long registrationId, Long operatorId, String remark) {
        return doGenerate(registrationId, operatorId, remark, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ensureRosterLockedForApprovedRegistration(Long registrationId, Long operatorId) {
        BasketTournamentRegistration reg = requireApprovedRegistration(registrationId);
        BasketTeamSeasonProfile profile = teamSeasonProfileMapper.selectByRegistrationId(registrationId);
        if (profile == null) {
            backfillFromApprovedRegistration(registrationId, operatorId, "修复阵容锁定");
            return;
        }
        boolean needsRepair = !Integer.valueOf(RosterLockStatusEnum.LOCKED.getCode()).equals(profile.getRosterLockStatus())
                || seasonRosterMapper.countUnlockedActiveByEventTeamId(profile.getId()) > 0;
        lockEventTeamRoster(profile, new Date());
        if (needsRepair) {
            writeParticipationAudit(reg, "REPAIR_ROSTER_LOCK", profile.getId(),
                    "修复正式赛事球队阵容锁定状态；eventTeamId=" + profile.getId());
        }
    }

    private Long doGenerate(Long registrationId, Long operatorId, String remark, boolean backfill) {
        BasketTournamentRegistration reg = requireApprovedRegistration(registrationId);
        GenerationContext context = buildContext(reg, backfill);
        BasketTeamSeasonProfile existing = teamSeasonProfileMapper.selectByRegistrationId(registrationId);
        Date now = context.approvedTime();
        Long eventTeamId;
        int createdRosterCount = 0;
        if (existing != null) {
            createdRosterCount = createMissingRosterRows(reg, existing, context, now);
            eventTeamId = existing.getId();
        } else {
            BasketTeamSeasonProfile profile = buildEventTeamProfile(reg, context, now, backfill);
            teamSeasonProfileMapper.insert(profile);
            createdRosterCount = createRosterRows(reg, profile.getId(), context, now);
            eventTeamId = profile.getId();
            existing = profile;
            writeParticipationAudit(reg, "GENERATE_EVENT_TEAM", eventTeamId, buildEventTeamAuditRemark(profile, backfill));
        }
        if (createdRosterCount > 0) {
            writeParticipationAudit(reg, "GENERATE_EVENT_PLAYER", eventTeamId,
                    "生成赛事球员快照 " + createdRosterCount + " 人"
                            + (backfill ? "（" + BACKFILL_REMARK + "）" : "")
                            + (StringUtils.hasText(remark) ? "；" + remark : ""));
        }
        lockEventTeamRoster(existing, now);
        return eventTeamId;
    }

    private void lockEventTeamRoster(BasketTeamSeasonProfile profile, Date now) {
        if (profile == null || profile.getId() == null) {
            return;
        }
        teamSeasonProfileMapper.updateRosterLockStatus(profile.getId(), RosterLockStatusEnum.LOCKED.getCode(), now);
        profile.setRosterLockStatus(RosterLockStatusEnum.LOCKED.getCode());
        profile.setRosterLockedTime(now);
        seasonRosterMapper.updateLockStatusByEventTeamId(profile.getId(), RosterLockStatusEnum.LOCKED.getCode(), now);
    }

    private GenerationContext buildContext(BasketTournamentRegistration reg, boolean backfill) {
        BasketTeam team = requireTeam(reg.getTeamId());
        BasketTournamentSeason season = seasonMapper.selectById(reg.getSeasonId());
        BasketTournament tournament = season != null && season.getTournamentId() != null
                ? tournamentMapper.selectById(season.getTournamentId()) : null;
        String platformCode = resolvePlatformCode(team, tournament);
        BasketCaptainQualification grant = captainQualificationMapper.selectActiveGrantForEvent(
                reg.getCaptainMemberId(), reg.getTeamId(), reg.getSeasonId());
        Map<Long, RegistrationPlayerVo> playerDetails = registrationPlayerMapper.selectDetailByRegistrationId(reg.getId())
                .stream().collect(Collectors.toMap(RegistrationPlayerVo::getPlayerId, v -> v, (a, b) -> a));
        Date approvedTime = reg.getAuditTime() != null ? reg.getAuditTime() : new Date();
        return new GenerationContext(team, tournament, platformCode, grant, playerDetails, approvedTime, backfill);
    }

    private BasketTeamSeasonProfile buildEventTeamProfile(BasketTournamentRegistration reg, GenerationContext context,
            Date now, boolean backfill) {
        BasketTeam team = context.team();
        BasketTeamSeasonProfile profile = new BasketTeamSeasonProfile();
        profile.setTeamId(reg.getTeamId());
        profile.setSeasonId(reg.getSeasonId());
        profile.setRegistrationId(reg.getId());
        if (context.grant() != null) {
            profile.setCaptainGrantId(context.grant().getId());
        }
        profile.setEventTeamNo(eventTeamNoGenerator.generateEventTeamNo(reg.getSeasonId()));
        profile.setTeamNameSnapshot(team.getTeamName());
        profile.setShortNameSnapshot(team.getShortName());
        profile.setTeamRegionSnapshot(resolveTeamRegion(team));
        profile.setDescriptionSnapshot(team.getDescription());
        profile.setGameTerminal(context.platformCode());
        profile.setRegistrationSource(REGISTRATION_SOURCE);
        profile.setParticipationStatus(ParticipationStatusEnum.ACTIVE.getCode());
        profile.setRosterLockStatus(RosterLockStatusEnum.LOCKED.getCode());
        profile.setRosterLockedTime(now);
        profile.setApprovedTime(context.approvedTime());
        profile.setDisplayName(team.getTeamName());
        profile.setLogoFileId(team.getLogoFileId());
        profile.setCaptainPlayerId(reg.getCaptainPlayerId());
        profile.setCaptainNameSnapshot(resolveCaptainName(reg, context));
        profile.setRegistrationStatus(RegistrationStatusEnum.APPROVED.getCode());
        profile.setSeasonStatus(1);
        if (backfill) {
            profile.setRemark(BACKFILL_REMARK);
        }
        profile.setCreateTime(now);
        profile.setUpdateTime(now);
        profile.setIsDelete(NOT_DELETED);
        return profile;
    }

    private int createRosterRows(BasketTournamentRegistration reg, Long eventTeamId, GenerationContext context, Date now) {
        List<BasketTournamentRegistrationPlayer> players = registrationPlayerMapper.selectByRegistrationId(reg.getId());
        int created = 0;
        for (BasketTournamentRegistrationPlayer rp : players) {
            if (seasonRosterMapper.countActiveBySeasonAndPlayer(reg.getSeasonId(), rp.getPlayerId()) > 0) {
                continue;
            }
            insertRosterRow(reg, eventTeamId, context, rp, now);
            created++;
        }
        return created;
    }

    private int createMissingRosterRows(BasketTournamentRegistration reg, BasketTeamSeasonProfile profile,
            GenerationContext context, Date now) {
        Set<Long> existingPlayerIds = seasonRosterMapper.selectByRegistrationId(reg.getId()).stream()
                .map(BasketTournamentSeasonRoster::getPlayerId).collect(Collectors.toSet());
        List<BasketTournamentRegistrationPlayer> players = registrationPlayerMapper.selectByRegistrationId(reg.getId());
        int created = 0;
        for (BasketTournamentRegistrationPlayer rp : players) {
            if (existingPlayerIds.contains(rp.getPlayerId())) {
                continue;
            }
            if (seasonRosterMapper.countActiveBySeasonAndPlayer(reg.getSeasonId(), rp.getPlayerId()) > 0) {
                continue;
            }
            insertRosterRow(reg, profile.getId(), context, rp, now);
            created++;
        }
        return created;
    }

    private void insertRosterRow(BasketTournamentRegistration reg, Long eventTeamId, GenerationContext context,
            BasketTournamentRegistrationPlayer rp, Date now) {
        BasketPlayerProfile profile = profileMapper.selectById(rp.getPlayerId());
        RegistrationPlayerVo detail = context.playerDetails().get(rp.getPlayerId());
        PlatformSnapshot platform = resolvePlatformSnapshot(profile, context.platformCode(), rp.getPlayerId());
        BasketTournamentSeasonRoster roster = new BasketTournamentSeasonRoster();
        roster.setRosterNo(rosterNoGenerator.generateRosterNo());
        roster.setSeasonId(reg.getSeasonId());
        roster.setEventTeamId(eventTeamId);
        roster.setTeamId(reg.getTeamId());
        roster.setPlayerId(rp.getPlayerId());
        roster.setMemberId(rp.getMemberId());
        roster.setRegistrationId(reg.getId());
        roster.setRegistrationPlayerId(rp.getId());
        roster.setPlayerNicknameSnapshot(resolveNickname(profile, detail));
        roster.setAvatarFileIdSnapshot(resolveAvatarFileId(rp.getMemberId()));
        roster.setPlatformSnapshot(platform.platform());
        roster.setGameIdDisplaySnapshot(platform.displayGameId());
        roster.setGameIdNormalizedSnapshot(platform.normalizedGameId());
        roster.setPositionSnapshot(resolvePositionSnapshot(profile, detail));
        roster.setRosterRole(StringUtils.hasText(rp.getRosterRole()) ? rp.getRosterRole() : "PLAYER");
        roster.setIsCaptain(rp.getIsCaptain() != null ? rp.getIsCaptain() : 0);
        roster.setIsStarting(rp.getIsStarting() != null ? rp.getIsStarting() : 0);
        roster.setIsSubstitute(rp.getIsStarting() != null && rp.getIsStarting() == 1 ? 0 : 1);
        roster.setSourceType(RosterSourceTypeEnum.INITIAL_REGISTRATION.getCode());
        roster.setRosterStatus(SeasonRosterStatusEnum.ACTIVE.getCode());
        roster.setLockStatus(RosterLockStatusEnum.LOCKED.getCode());
        roster.setActiveSlot(RosterActiveSlotSupport.buildActiveSlot(reg.getSeasonId(), rp.getPlayerId()));
        roster.setEffectiveTime(now);
        roster.setCreateTime(now);
        roster.setUpdateTime(now);
        roster.setIsDelete(NOT_DELETED);
        seasonRosterMapper.insert(roster);
    }

    private PlatformSnapshot resolvePlatformSnapshot(BasketPlayerProfile profile, String gameTerminal, Long playerId) {
        List<BasketPlayerPlatformAccount> accounts = platformAccountMapper.selectByPlayerId(playerId);
        String normalizedTerminal = GameTerminalSupport.normalize(gameTerminal);
        BasketPlayerPlatformAccount matched = accounts.stream()
                .filter(a -> Objects.equals(a.getAccountStatus(), PlatformAccountStatusEnum.ACTIVE.getCode()))
                .filter(a -> matchesPlatform(a.getPlatform(), normalizedTerminal))
                .findFirst()
                .orElseGet(() -> accounts.stream()
                        .filter(a -> Objects.equals(a.getAccountStatus(), PlatformAccountStatusEnum.ACTIVE.getCode()))
                        .filter(a -> Objects.equals(a.getIsPrimary(), 1))
                        .findFirst()
                        .orElse(accounts.stream()
                                .filter(a -> Objects.equals(a.getAccountStatus(), PlatformAccountStatusEnum.ACTIVE.getCode()))
                                .findFirst().orElse(null)));
        if (matched != null) {
            return new PlatformSnapshot(matched.getPlatform(), matched.getDisplayGameId(), matched.getNormalizedGameId());
        }
        if (profile != null && StringUtils.hasText(profile.getGameAccount())) {
            return new PlatformSnapshot(profile.getGameTerminal(), profile.getGameAccount(),
                    GameAccountNormalizer.normalize(profile.getGameAccount()));
        }
        return new PlatformSnapshot(gameTerminal, null, null);
    }

    private boolean matchesPlatform(String accountPlatform, String normalizedTerminal) {
        if (!StringUtils.hasText(accountPlatform) || !StringUtils.hasText(normalizedTerminal)) {
            return false;
        }
        return normalizedTerminal.equals(GameTerminalSupport.normalize(accountPlatform));
    }

    private String resolveNickname(BasketPlayerProfile profile, RegistrationPlayerVo detail) {
        if (detail != null && StringUtils.hasText(detail.getPlayerNickname())) {
            return detail.getPlayerNickname();
        }
        return profile != null && StringUtils.hasText(profile.getGameNickname()) ? profile.getGameNickname() : null;
    }

    private String resolvePositionSnapshot(BasketPlayerProfile profile, RegistrationPlayerVo detail) {
        if (detail != null && StringUtils.hasText(detail.getPositionCode())) {
            return detail.getPositionCode();
        }
        return profile != null ? profile.getPositionCode() : null;
    }

    private Long resolveAvatarFileId(Long memberId) {
        if (memberId == null) {
            return null;
        }
        return profileMapper.selectAvatarFileIdByMemberId(memberId);
    }

    private String resolveCaptainName(BasketTournamentRegistration reg, GenerationContext context) {
        RegistrationPlayerVo captainDetail = context.playerDetails().get(reg.getCaptainPlayerId());
        if (captainDetail != null && StringUtils.hasText(captainDetail.getPlayerNickname())) {
            return captainDetail.getPlayerNickname();
        }
        if (reg.getCaptainPlayerId() != null) {
            BasketPlayerProfile captainProfile = profileMapper.selectById(reg.getCaptainPlayerId());
            if (captainProfile != null && StringUtils.hasText(captainProfile.getGameNickname())) {
                return captainProfile.getGameNickname();
            }
        }
        return StringUtils.hasText(reg.getContactName()) ? reg.getContactName() : null;
    }

    private String resolveTeamRegion(BasketTeam team) {
        if (StringUtils.hasText(team.getRegionName())) {
            return team.getRegionName();
        }
        return team.getRegionCode();
    }

    private String resolvePlatformCode(BasketTeam team, BasketTournament tournament) {
        if (tournament != null && StringUtils.hasText(tournament.getGameTerminal())) {
            return GameTerminalSupport.normalize(tournament.getGameTerminal());
        }
        if (team != null && StringUtils.hasText(team.getGameTerminal())) {
            return GameTerminalSupport.normalize(team.getGameTerminal());
        }
        return null;
    }

    private BasketTournamentRegistration requireApprovedRegistration(Long registrationId) {
        BasketTournamentRegistration reg = registrationMapper.selectById(registrationId);
        if (reg == null) {
            throw new ServiceException("报名记录不存在");
        }
        if (!Objects.equals(reg.getRegistrationStatus(), RegistrationStatusEnum.APPROVED.getCode())) {
            throw new ServiceException("仅已通过的报名可生成正式参赛");
        }
        return reg;
    }

    private BasketTeam requireTeam(Long teamId) {
        BasketTeam team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new ServiceException("球队不存在");
        }
        return team;
    }

    private void writeParticipationAudit(BasketTournamentRegistration reg, String action, Long eventTeamId, String remark) {
        Date now = new Date();
        BasketTournamentRegistrationAudit audit = new BasketTournamentRegistrationAudit();
        audit.setRegistrationId(reg.getId());
        audit.setAuditAction(action);
        audit.setBeforeStatus(reg.getRegistrationStatus());
        audit.setAfterStatus(reg.getRegistrationStatus());
        audit.setAuditorId(SecurityUtils.getUserId());
        audit.setAuditorName(SecurityUtils.getUsername());
        audit.setAuditRemark(appendEventTeamRemark(remark, eventTeamId));
        audit.setAuditTime(now);
        audit.setCreateTime(now);
        audit.setUpdateTime(now);
        audit.setIsDelete(NOT_DELETED);
        registrationAuditMapper.insert(audit);
    }

    private String buildEventTeamAuditRemark(BasketTeamSeasonProfile profile, boolean backfill) {
        StringBuilder sb = new StringBuilder("生成正式赛事球队 ");
        sb.append(profile.getEventTeamNo());
        if (backfill) {
            sb.append("（").append(BACKFILL_REMARK).append("）");
        }
        return sb.toString();
    }

    private String appendEventTeamRemark(String remark, Long eventTeamId) {
        if (eventTeamId == null) {
            return remark;
        }
        if (!StringUtils.hasText(remark)) {
            return "eventTeamId=" + eventTeamId;
        }
        return remark + "；eventTeamId=" + eventTeamId;
    }

    private record GenerationContext(BasketTeam team, BasketTournament tournament, String platformCode,
            BasketCaptainQualification grant, Map<Long, RegistrationPlayerVo> playerDetails, Date approvedTime,
            boolean backfill) {
    }

    private record PlatformSnapshot(String platform, String displayGameId, String normalizedGameId) {
    }
}
