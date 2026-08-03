package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.BasketTeam;
import com.zhs.basket.domain.v4.*;
import com.zhs.basket.enums.v4.AuditResultEnum;
import com.zhs.basket.enums.v4.RegistrationStatusEnum;
import com.zhs.basket.mapper.BasketPlayerProfileMapper;
import com.zhs.basket.mapper.BasketTeamMapper;
import com.zhs.basket.mapper.v4.*;
import com.zhs.basket.service.v4.IBasketV4EventSnapshotService;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateFromApprovedRegistration(BasketRegistration registration) {
        BasketDivision division = divisionMapper.selectById(registration.getDivisionId());
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        BasketEventTeam existingTeam = eventTeamMapper.selectByDivisionSourceTeam(
                registration.getDivisionId(), registration.getTeamId());
        if (existingTeam != null) {
            throw new ServiceException("该 Division 下已存在该球队的赛事快照");
        }

        List<BasketRegistrationPlayer> players = registrationPlayerMapper.selectByRegistrationId(registration.getId());
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
}
