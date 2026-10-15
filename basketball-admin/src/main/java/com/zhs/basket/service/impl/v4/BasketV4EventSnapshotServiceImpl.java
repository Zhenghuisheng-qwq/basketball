package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.BasketPlayerProfile;
import com.zhs.basket.domain.BasketTeam;
import com.zhs.basket.domain.v4.*;
import com.zhs.basket.enums.v4.AuditResultEnum;
import com.zhs.basket.enums.v4.RegistrationStatusEnum;
import com.zhs.basket.mapper.BasketPlayerProfileMapper;
import com.zhs.basket.mapper.BasketTeamMapper;
import com.zhs.basket.mapper.v4.*;
import com.zhs.basket.service.v4.IBasketV4EventSnapshotService;
import com.zhs.basket.support.v4.PreMatchEventPlayerSync;
import com.zhs.basket.support.v4.RosterEligibilityQueries;
import com.zhs.basket.support.v4.V4PublicReadCachePublisher;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class BasketV4EventSnapshotServiceImpl implements IBasketV4EventSnapshotService {

    private static final Integer NOT_DELETED = 0;
    private static final String EVENT_TEAM_STATUS_ACTIVE = "ACTIVE";
    private static final String EVENT_PLAYER_STATUS_ACTIVE = "ACTIVE";

    @Autowired
    private BasketEventTeamMapper eventTeamMapper;
    @Autowired
    private BasketEventPlayerMapper eventPlayerMapper;
    @Autowired
    private BasketRegistrationPlayerMapper registrationPlayerMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketTeamMapper teamMapper;
    @Autowired
    private BasketPlayerProfileMapper playerProfileMapper;
    @Autowired
    private V4PublicReadCachePublisher publicReadCachePublisher;
    @Autowired
    private PreMatchEventPlayerSync eventPlayerSync;
    @Autowired
    private RosterEligibilityQueries eligibilityQueries;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateFromApprovedRegistration(BasketRegistration registration) {
        generateInternal(registration, null, false);
        publicReadCachePublisher.divisionMutation(registration.getDivisionId(), registration.getSeasonId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateFromFormalRegistration(BasketRegistration registration, Long rosterVersionId) {
        if (rosterVersionId == null) {
            throw new ServiceException("rosterVersionId 不能为空");
        }
        generateInternal(registration, rosterVersionId, true);
        publicReadCachePublisher.divisionMutation(registration.getDivisionId(), registration.getSeasonId());
    }

    private void generateInternal(BasketRegistration registration, Long rosterVersionId, boolean updateIfExists) {
        BasketDivision division = divisionMapper.selectById(registration.getDivisionId());
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        BasketEventTeam existingTeam = eventTeamMapper.selectByDivisionSourceTeam(
                registration.getDivisionId(), registration.getTeamId());
        if (existingTeam != null) {
            if (updateIfExists) {
                existingTeam.setRosterVersionId(rosterVersionId);
                existingTeam.setRegistrationId(registration.getId());
                eventTeamMapper.updateById(existingTeam);
                if (!eligibilityQueries.frozen(registration.getDivisionId())) {
                    eventPlayerSync.syncWritable(registration);
                }
                return;
            }
            throw new ServiceException("该 Division 下已存在该球队的赛事快照");
        }

        List<BasketRegistrationPlayer> players = eventPlayerSync.legalPlayers(registration);
        if (players.isEmpty()) {
            throw new ServiceException("报名阵容为空，无法生成快照");
        }
        for (BasketRegistrationPlayer player : players) {
            BasketEventPlayer existingPlayer = eventPlayerMapper.selectByDivisionSourcePlayer(
                    registration.getDivisionId(), player.getPlayerId());
            if (existingPlayer != null) {
                throw new ServiceException("该 Division 下球员已存在于其他赛事球队：" + player.getPlayerId());
            }
        }

        BasketTeam team = teamMapper.selectById(registration.getTeamId());
        if (team == null) {
            throw new ServiceException("来源球队不存在");
        }

        Date now = new Date();
        BasketEventTeam eventTeam = new BasketEventTeam();
        eventTeam.setSeasonId(registration.getSeasonId());
        eventTeam.setDivisionId(registration.getDivisionId());
        eventTeam.setRosterVersionId(rosterVersionId);
        eventTeam.setRegistrationId(registration.getId());
        eventTeam.setSourceTeamId(registration.getTeamId());
        eventTeam.setTeamNameSnapshot(team.getTeamName());
        eventTeam.setTeamLogoFileId(team.getLogoFileId());
        eventTeam.setTeamShortName(team.getShortName());
        eventTeam.setCaptainPlayerId(registration.getCaptainPlayerId());
        eventTeam.setCaptainMemberId(registration.getCaptainMemberId());
        eventTeam.setEventTeamNo(team.getTeamNo());
        eventTeam.setEventTeamStatus(EVENT_TEAM_STATUS_ACTIVE);
        eventTeam.setPlatformCode(division.getPlatformCode());
        eventTeam.setCreateTime(now);
        eventTeam.setUpdateTime(now);
        eventTeam.setIsDelete(NOT_DELETED);
        eventTeamMapper.insert(eventTeam);

        List<BasketEventPlayer> eventPlayers = new ArrayList<>();
        for (BasketRegistrationPlayer regPlayer : players) {
            Long avatarFileId = playerProfileMapper.selectAvatarFileIdByMemberId(regPlayer.getMemberId());
            BasketEventPlayer eventPlayer = new BasketEventPlayer();
            eventPlayer.setSeasonId(registration.getSeasonId());
            eventPlayer.setDivisionId(registration.getDivisionId());
            eventPlayer.setEventTeamId(eventTeam.getId());
            eventPlayer.setSourcePlayerId(regPlayer.getPlayerId());
            eventPlayer.setMemberId(regPlayer.getMemberId());
            eventPlayer.setPlatformIdentityId(regPlayer.getPlatformIdentityId());
            eventPlayer.setPlatformCode(regPlayer.getPlatformCode());
            eventPlayer.setGameIdSnapshot(regPlayer.getGameIdSnapshot());
            eventPlayer.setPlayerNameSnapshot(regPlayer.getPlayerNameSnapshot());
            eventPlayer.setAvatarFileId(avatarFileId);
            eventPlayer.setPosition(regPlayer.getPosition());
            eventPlayer.setJerseyNo(regPlayer.getJerseyNo());
            eventPlayer.setIsCaptain(regPlayer.getIsCaptain());
            eventPlayer.setIsStarter(regPlayer.getIsStarter());
            eventPlayer.setEventPlayerStatus(EVENT_PLAYER_STATUS_ACTIVE);
            eventPlayer.setCreateTime(now);
            eventPlayer.setUpdateTime(now);
            eventPlayer.setIsDelete(NOT_DELETED);
            eventPlayers.add(eventPlayer);
        }
        if (!eventPlayers.isEmpty()) {
            eventPlayerMapper.batchInsert(eventPlayers);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BasketEventTeam createTemporaryEventTeam(BasketDivision division,
                                                    BasketDivisionRosterVersion rosterVersion,
                                                    BasketPersonalDrawTeam drawTeam,
                                                    List<BasketPersonalDrawAssignment> assignments,
                                                    Map<Long, BasketPersonalRegistration> registrationById,
                                                    Long captainPlayerId,
                                                    Long captainMemberId) {
        if (division == null || rosterVersion == null || drawTeam == null) {
            throw new ServiceException("临时参赛球队参数不完整");
        }
        if (assignments == null || assignments.isEmpty()) {
            throw new ServiceException("临时球队没有成员");
        }
        for (BasketPersonalDrawAssignment assignment : assignments) {
            BasketEventPlayer existing = eventPlayerMapper.selectByDivisionRosterSourcePlayer(
                    division.getId(), rosterVersion.getId(), assignment.getPlayerId());
            if (existing != null) {
                throw new ServiceException("该名单版本下球员已存在于其他临时球队：" + assignment.getPlayerId());
            }
        }
        Date now = new Date();
        BasketEventTeam eventTeam = new BasketEventTeam();
        eventTeam.setSeasonId(division.getSeasonId());
        eventTeam.setDivisionId(division.getId());
        eventTeam.setRosterVersionId(rosterVersion.getId());
        eventTeam.setRegistrationId(null);
        eventTeam.setSourceTeamId(null);
        eventTeam.setSourceDrawTeamId(drawTeam.getId());
        eventTeam.setTeamNameSnapshot(drawTeam.getTemporaryTeamName());
        eventTeam.setTeamShortName(drawTeam.getTemporaryTeamCode());
        eventTeam.setCaptainPlayerId(captainPlayerId);
        eventTeam.setCaptainMemberId(captainMemberId);
        eventTeam.setEventTeamNo(drawTeam.getTemporaryTeamCode());
        eventTeam.setEventTeamStatus(EVENT_TEAM_STATUS_ACTIVE);
        eventTeam.setPlatformCode(division.getPlatformCode());
        eventTeam.setCreateTime(now);
        eventTeam.setUpdateTime(now);
        eventTeam.setIsDelete(NOT_DELETED);
        eventTeamMapper.insert(eventTeam);

        List<BasketEventPlayer> eventPlayers = new ArrayList<>();
        for (BasketPersonalDrawAssignment assignment : assignments) {
            BasketPersonalRegistration personal = registrationById.get(assignment.getPersonalRegistrationId());
            BasketPlayerProfile profile = playerProfileMapper.selectById(assignment.getPlayerId());
            Long memberId = personal != null ? personal.getUserId() : (profile != null ? profile.getMemberId() : null);
            Long avatarFileId = memberId == null ? null : playerProfileMapper.selectAvatarFileIdByMemberId(memberId);
            BasketEventPlayer eventPlayer = new BasketEventPlayer();
            eventPlayer.setSeasonId(division.getSeasonId());
            eventPlayer.setDivisionId(division.getId());
            eventPlayer.setRosterVersionId(rosterVersion.getId());
            eventPlayer.setEventTeamId(eventTeam.getId());
            eventPlayer.setSourcePlayerId(assignment.getPlayerId());
            eventPlayer.setMemberId(memberId);
            eventPlayer.setPlatformIdentityId(personal != null ? personal.getPlayerPlatformId() : null);
            eventPlayer.setPlatformCode(personal != null && personal.getActualPlatformCode() != null
                    ? personal.getActualPlatformCode() : division.getPlatformCode());
            eventPlayer.setGameIdSnapshot(personal != null ? personal.getGameAccountId() : null);
            eventPlayer.setPlayerNameSnapshot(profile != null ? profile.getGameNickname() : null);
            eventPlayer.setAvatarFileId(avatarFileId);
            eventPlayer.setPosition(personal != null ? personal.getPreferredPosition() : null);
            eventPlayer.setIsCaptain(assignment.getPlayerId() != null && assignment.getPlayerId().equals(captainPlayerId) ? 1 : 0);
            eventPlayer.setIsStarter(0);
            eventPlayer.setEventPlayerStatus(EVENT_PLAYER_STATUS_ACTIVE);
            eventPlayer.setCreateTime(now);
            eventPlayer.setUpdateTime(now);
            eventPlayer.setIsDelete(NOT_DELETED);
            eventPlayers.add(eventPlayer);
        }
        if (!eventPlayers.isEmpty()) {
            eventPlayerMapper.batchInsert(eventPlayers);
        }
        return eventTeam;
    }
}
