package com.zhs.basket.service.impl;

import com.zhs.basket.domain.*;
import com.zhs.basket.enums.*;
import com.zhs.basket.mapper.*;
import com.zhs.basket.service.IBasketParticipationGenerationService;
import com.zhs.basket.support.EventTeamNoGenerator;
import com.zhs.basket.support.GameAccountNormalizer;
import com.zhs.basket.support.RosterActiveSlotSupport;
import com.zhs.basket.support.RosterNoGenerator;
import com.zhs.common.exception.ServiceException;
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

    @Autowired private BasketTournamentRegistrationMapper registrationMapper;
    @Autowired private BasketTournamentRegistrationPlayerMapper registrationPlayerMapper;
    @Autowired private BasketTeamMapper teamMapper;
    @Autowired private BasketTeamSeasonProfileMapper teamSeasonProfileMapper;
    @Autowired private BasketTournamentSeasonRosterMapper seasonRosterMapper;
    @Autowired private BasketPlayerProfileMapper profileMapper;
    @Autowired private BasketPlayerPlatformAccountMapper platformAccountMapper;
    @Autowired private EventTeamNoGenerator eventTeamNoGenerator;
    @Autowired private RosterNoGenerator rosterNoGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateFromTeamRegistration(Long registrationId, Long auditorId, String remark) {
        BasketTournamentRegistration reg = requireApprovedRegistration(registrationId);
        BasketTeamSeasonProfile existing = teamSeasonProfileMapper.selectByRegistrationId(registrationId);
        Date now = new Date();
        if (existing != null) {
            createMissingRosterRows(reg, existing, now);
            return existing.getId();
        }
        BasketTeam team = requireTeam(reg.getTeamId());
        BasketTeamSeasonProfile profile = buildEventTeamProfile(reg, team, now);
        teamSeasonProfileMapper.insert(profile);
        createRosterRows(reg, profile.getId(), team, now);
        return profile.getId();
    }

    private BasketTeamSeasonProfile buildEventTeamProfile(BasketTournamentRegistration reg, BasketTeam team, Date now) {
        BasketTeamSeasonProfile profile = new BasketTeamSeasonProfile();
        profile.setTeamId(reg.getTeamId());
        profile.setSeasonId(reg.getSeasonId());
        profile.setRegistrationId(reg.getId());
        profile.setEventTeamNo(eventTeamNoGenerator.generateEventTeamNo(reg.getSeasonId()));
        profile.setTeamNameSnapshot(team.getTeamName());
        profile.setShortNameSnapshot(team.getShortName());
        profile.setDescriptionSnapshot(team.getDescription());
        profile.setGameTerminal(team.getGameTerminal());
        profile.setRegistrationSource(REGISTRATION_SOURCE);
        profile.setParticipationStatus(ParticipationStatusEnum.ACTIVE.getCode());
        profile.setRosterLockStatus(RosterLockStatusEnum.UNLOCKED.getCode());
        profile.setApprovedTime(now);
        profile.setDisplayName(team.getTeamName());
        profile.setLogoFileId(team.getLogoFileId());
        profile.setCaptainPlayerId(reg.getCaptainPlayerId());
        profile.setRegistrationStatus(RegistrationStatusEnum.APPROVED.getCode());
        profile.setSeasonStatus(1);
        profile.setCreateTime(now);
        profile.setUpdateTime(now);
        profile.setIsDelete(NOT_DELETED);
        return profile;
    }

    private void createRosterRows(BasketTournamentRegistration reg, Long eventTeamId, BasketTeam team, Date now) {
        List<BasketTournamentRegistrationPlayer> players = registrationPlayerMapper.selectByRegistrationId(reg.getId());
        for (BasketTournamentRegistrationPlayer rp : players) {
            if (seasonRosterMapper.countActiveBySeasonAndPlayer(reg.getSeasonId(), rp.getPlayerId()) > 0) {
                continue;
            }
            insertRosterRow(reg, eventTeamId, team, rp, now);
        }
    }

    private void createMissingRosterRows(BasketTournamentRegistration reg, BasketTeamSeasonProfile profile, Date now) {
        BasketTeam team = requireTeam(reg.getTeamId());
        Set<Long> existingPlayerIds = seasonRosterMapper.selectByRegistrationId(reg.getId()).stream()
                .map(BasketTournamentSeasonRoster::getPlayerId).collect(Collectors.toSet());
        List<BasketTournamentRegistrationPlayer> players = registrationPlayerMapper.selectByRegistrationId(reg.getId());
        for (BasketTournamentRegistrationPlayer rp : players) {
            if (existingPlayerIds.contains(rp.getPlayerId())) {
                continue;
            }
            if (seasonRosterMapper.countActiveBySeasonAndPlayer(reg.getSeasonId(), rp.getPlayerId()) > 0) {
                continue;
            }
            insertRosterRow(reg, profile.getId(), team, rp, now);
        }
    }

    private void insertRosterRow(BasketTournamentRegistration reg, Long eventTeamId, BasketTeam team,
            BasketTournamentRegistrationPlayer rp, Date now) {
        BasketPlayerProfile profile = profileMapper.selectById(rp.getPlayerId());
        PlatformSnapshot platform = resolvePlatformSnapshot(profile, team.getGameTerminal(), rp.getPlayerId());
        BasketTournamentSeasonRoster roster = new BasketTournamentSeasonRoster();
        roster.setRosterNo(rosterNoGenerator.generateRosterNo());
        roster.setSeasonId(reg.getSeasonId());
        roster.setEventTeamId(eventTeamId);
        roster.setTeamId(reg.getTeamId());
        roster.setPlayerId(rp.getPlayerId());
        roster.setMemberId(rp.getMemberId());
        roster.setRegistrationId(reg.getId());
        roster.setRegistrationPlayerId(rp.getId());
        roster.setPlayerNicknameSnapshot(resolveNickname(profile));
        roster.setPlatformSnapshot(platform.platform());
        roster.setGameIdDisplaySnapshot(platform.displayGameId());
        roster.setGameIdNormalizedSnapshot(platform.normalizedGameId());
        roster.setPositionSnapshot(profile != null ? profile.getPositionCode() : null);
        roster.setRosterRole(StringUtils.hasText(rp.getRosterRole()) ? rp.getRosterRole() : "PLAYER");
        roster.setIsCaptain(rp.getIsCaptain() != null ? rp.getIsCaptain() : 0);
        roster.setIsStarting(rp.getIsStarting() != null ? rp.getIsStarting() : 0);
        roster.setIsSubstitute(rp.getIsStarting() != null && rp.getIsStarting() == 1 ? 0 : 1);
        roster.setSourceType(RosterSourceTypeEnum.INITIAL_REGISTRATION.getCode());
        roster.setRosterStatus(SeasonRosterStatusEnum.ACTIVE.getCode());
        roster.setLockStatus(RosterLockStatusEnum.UNLOCKED.getCode());
        roster.setActiveSlot(RosterActiveSlotSupport.buildActiveSlot(reg.getSeasonId(), rp.getPlayerId()));
        roster.setEffectiveTime(now);
        roster.setCreateTime(now);
        roster.setUpdateTime(now);
        roster.setIsDelete(NOT_DELETED);
        seasonRosterMapper.insert(roster);
    }

    private PlatformSnapshot resolvePlatformSnapshot(BasketPlayerProfile profile, String gameTerminal, Long playerId) {
        List<BasketPlayerPlatformAccount> accounts = platformAccountMapper.selectByPlayerId(playerId);
        BasketPlayerPlatformAccount matched = accounts.stream()
                .filter(a -> Objects.equals(a.getAccountStatus(), PlatformAccountStatusEnum.ACTIVE.getCode()))
                .filter(a -> StringUtils.hasText(gameTerminal) && gameTerminal.equals(a.getPlatform()))
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

    private String resolveNickname(BasketPlayerProfile profile) {
        return profile != null && StringUtils.hasText(profile.getGameNickname()) ? profile.getGameNickname() : null;
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

    private record PlatformSnapshot(String platform, String displayGameId, String normalizedGameId) {
    }
}
