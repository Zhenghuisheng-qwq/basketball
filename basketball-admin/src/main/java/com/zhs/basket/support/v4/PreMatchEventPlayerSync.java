package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketEventPlayer;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.domain.v4.BasketRegistration;
import com.zhs.basket.domain.v4.BasketRegistrationPlayer;
import com.zhs.basket.mapper.v4.BasketEventPlayerMapper;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.mapper.v4.BasketMatchSubmissionPlayerStatMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationPlayerMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchPlayerStatMapper;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 未冻结时，把已生成的 EventPlayer 增量同步到当前合法报名名单。
 * 已有比赛或统计引用的球员不覆盖，调用方事务回滚。
 */
@Component
public class PreMatchEventPlayerSync {

    private static final Integer NOT_DELETED = 0;
    private static final String ACTIVE = "ACTIVE";

    @Autowired
    private RosterEligibilityQueries eligibilityQueries;
    @Autowired
    private BasketEventTeamMapper eventTeamMapper;
    @Autowired
    private BasketEventPlayerMapper eventPlayerMapper;
    @Autowired
    private BasketRegistrationPlayerMapper registrationPlayerMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private BasketV4MatchPlayerStatMapper matchPlayerStatMapper;
    @Autowired
    private CurrentPlayerNameSupport currentPlayerNameSupport;
    @Autowired
    private BasketMatchSubmissionPlayerStatMapper submissionPlayerStatMapper;

    public void syncWritable(BasketRegistration registration) {
        if (registration == null || registration.getDivisionId() == null || registration.getTeamId() == null) {
            return;
        }
        if (eligibilityQueries.frozen(registration.getDivisionId())) {
            return;
        }
        BasketEventTeam eventTeam = eventTeamMapper.selectByDivisionSourceTeam(
                registration.getDivisionId(), registration.getTeamId());
        if (eventTeam == null) {
            return;
        }
        matchMapper.lockOpenMatchesByEventTeamId(eventTeam.getId());
        if (matchMapper.countPlayingByEventTeamId(eventTeam.getId()) > 0) {
            throw new ServiceException("比赛进行中，不能调整该场参赛人员");
        }
        List<BasketRegistrationPlayer> desired = legalPlayers(registration);
        Set<Long> desiredIds = new HashSet<>();
        Date now = new Date();
        for (BasketRegistrationPlayer player : desired) {
            desiredIds.add(player.getPlayerId());
            BasketEventPlayer current = eventPlayerMapper.selectAnyByEventTeamAndSource(eventTeam.getId(), player.getPlayerId());
            if (current == null) {
                eventPlayerMapper.insert(newPlayer(eventTeam, player, now));
            } else if (!NOT_DELETED.equals(current.getIsDelete())) {
                copyOnto(current, player);
                eventPlayerMapper.revive(current);
            } else {
                copyOnto(current, player);
                eventPlayerMapper.updateById(current);
            }
        }
        for (BasketEventPlayer existing : eventPlayerMapper.selectByEventTeamId(eventTeam.getId())) {
            if (desiredIds.contains(existing.getSourcePlayerId())) {
                continue;
            }
            if (referenced(existing.getId())) {
                throw new ServiceException("赛事球员已被比赛数据引用，不能改写正式名单");
            }
            eventPlayerMapper.softDelete(existing.getId());
        }
    }

    public List<BasketRegistrationPlayer> legalPlayers(BasketRegistration registration) {
        List<BasketRegistrationPlayer> rows = registrationPlayerMapper.selectByRegistrationId(registration.getId());
        if (eligibilityQueries.frozen(registration.getDivisionId())) {
            return rows;
        }
        return rows.stream()
                .filter(row -> registrationPlayerMapper.countActiveMember(registration.getTeamId(), row.getPlayerId()) > 0)
                .toList();
    }

    private boolean referenced(Long eventPlayerId) {
        return matchPlayerStatMapper.countByEventPlayerId(eventPlayerId) > 0
                || submissionPlayerStatMapper.countByEventPlayerId(eventPlayerId) > 0;
    }

    private BasketEventPlayer newPlayer(BasketEventTeam eventTeam, BasketRegistrationPlayer player, Date now) {
        BasketEventPlayer eventPlayer = new BasketEventPlayer();
        eventPlayer.setSeasonId(eventTeam.getSeasonId());
        eventPlayer.setDivisionId(eventTeam.getDivisionId());
        eventPlayer.setRosterVersionId(eventTeam.getRosterVersionId());
        eventPlayer.setEventTeamId(eventTeam.getId());
        copyOnto(eventPlayer, player);
        eventPlayer.setCreateTime(now);
        eventPlayer.setUpdateTime(now);
        eventPlayer.setIsDelete(NOT_DELETED);
        return eventPlayer;
    }

    private void copyOnto(BasketEventPlayer eventPlayer, BasketRegistrationPlayer player) {
        eventPlayer.setSourcePlayerId(player.getPlayerId());
        eventPlayer.setMemberId(player.getMemberId());
        eventPlayer.setPlatformIdentityId(player.getPlatformIdentityId());
        eventPlayer.setPlatformCode(player.getPlatformCode());
        eventPlayer.setGameIdSnapshot(player.getGameIdSnapshot());
        eventPlayer.setPlayerNameSnapshot(currentName(player));
        eventPlayer.setPosition(player.getPosition());
        eventPlayer.setJerseyNo(player.getJerseyNo());
        eventPlayer.setIsCaptain(player.getIsCaptain());
        eventPlayer.setIsStarter(player.getIsStarter());
        eventPlayer.setEventPlayerStatus(ACTIVE);
    }

    private String currentName(BasketRegistrationPlayer player) {
        String name = currentPlayerNameSupport.displayName(player.getPlayerId());
        return name != null && !name.isBlank() ? name : player.getPlayerNameSnapshot();
    }
}
