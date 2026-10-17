package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketEventPlayer;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.domain.v4.BasketRegistration;
import com.zhs.basket.domain.v4.BasketRegistrationPlayer;
import com.zhs.basket.mapper.BasketPlayerProfileMapper;
import com.zhs.basket.mapper.v4.BasketEventPlayerMapper;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.mapper.v4.BasketMatchSubmissionMapper;
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
 * 开赛后的审核名单变更：当前阵容切换为 ACTIVE，已被统计引用的旧球员改为 RETIRED。
 * 不删除历史 EventPlayer，不改写比赛统计，也不覆盖已引用行的昵称快照。
 */
@Component
public class InSeasonEventRosterApply {

    private static final Integer NOT_DELETED = 0;

    @Autowired
    private BasketEventTeamMapper eventTeamMapper;
    @Autowired
    private BasketEventPlayerMapper eventPlayerMapper;
    @Autowired
    private BasketRegistrationPlayerMapper registrationPlayerMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private BasketMatchSubmissionMapper submissionMapper;
    @Autowired
    private BasketV4MatchPlayerStatMapper matchPlayerStatMapper;
    @Autowired
    private BasketMatchSubmissionPlayerStatMapper submissionPlayerStatMapper;
    @Autowired
    private BasketPlayerProfileMapper playerProfileMapper;

    public void apply(BasketRegistration registration) {
        if (registration == null || registration.getDivisionId() == null || registration.getTeamId() == null) {
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
        if (submissionMapper.countOpenByEventTeamId(eventTeam.getId()) > 0) {
            throw new ServiceException("已有比赛数据正在审核，不能同时调整参赛名单");
        }
        List<BasketRegistrationPlayer> desired = registrationPlayerMapper.selectByRegistrationId(registration.getId());
        Set<Long> desiredIds = new HashSet<>();
        Date now = new Date();
        for (BasketRegistrationPlayer player : desired) {
            if (player.getPlayerId() == null) {
                continue;
            }
            desiredIds.add(player.getPlayerId());
            BasketEventPlayer current = eventPlayerMapper.selectAnyByEventTeamAndSource(eventTeam.getId(), player.getPlayerId());
            boolean exists = current != null;
            boolean deleted = exists && !NOT_DELETED.equals(current.getIsDelete());
            boolean referenced = exists && referenced(current.getId());
            switch (EventPlayerRosterTransition.decide(true, exists, deleted, referenced)) {
                case INSERT -> eventPlayerMapper.insert(newPlayer(eventTeam, player, now));
                case ACTIVATE_KEEP_SNAPSHOT -> keepSnapshot(current, player);
                case ACTIVATE_REFRESH -> refresh(current, player);
                default -> {
                }
            }
        }
        for (BasketEventPlayer existing : eventPlayerMapper.selectByEventTeamId(eventTeam.getId())) {
            if (existing.getSourcePlayerId() != null && desiredIds.contains(existing.getSourcePlayerId())) {
                continue;
            }
            boolean referenced = referenced(existing.getId());
            switch (EventPlayerRosterTransition.decide(false, true, false, referenced)) {
                case RETIRE -> retire(existing);
                case DELETE -> eventPlayerMapper.softDelete(existing.getId());
                default -> {
                }
            }
        }
    }

    private void keepSnapshot(BasketEventPlayer current, BasketRegistrationPlayer player) {
        if (!NOT_DELETED.equals(current.getIsDelete())) {
            BasketEventPlayer patch = new BasketEventPlayer();
            patch.setId(current.getId());
            patch.setEventPlayerStatus(EventPlayerRosterTransition.ACTIVE);
            patch.setIsCaptain(player.getIsCaptain());
            patch.setIsStarter(player.getIsStarter() == null ? 1 : player.getIsStarter());
            eventPlayerMapper.restoreWithoutSnapshot(patch);
            return;
        }
        BasketEventPlayer patch = new BasketEventPlayer();
        patch.setId(current.getId());
        patch.setEventPlayerStatus(EventPlayerRosterTransition.ACTIVE);
        patch.setIsCaptain(player.getIsCaptain());
        patch.setIsStarter(player.getIsStarter() == null ? 1 : player.getIsStarter());
        eventPlayerMapper.updateById(patch);
    }

    private void refresh(BasketEventPlayer current, BasketRegistrationPlayer player) {
        copyOnto(current, player);
        current.setEventPlayerStatus(EventPlayerRosterTransition.ACTIVE);
        if (!NOT_DELETED.equals(current.getIsDelete())) {
            eventPlayerMapper.revive(current);
            return;
        }
        eventPlayerMapper.updateById(current);
    }

    private void retire(BasketEventPlayer existing) {
        BasketEventPlayer patch = new BasketEventPlayer();
        patch.setId(existing.getId());
        patch.setEventPlayerStatus(EventPlayerRosterTransition.RETIRED);
        eventPlayerMapper.updateById(patch);
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
        if (player.getMemberId() != null) {
            eventPlayer.setAvatarFileId(playerProfileMapper.selectAvatarFileIdByMemberId(player.getMemberId()));
        }
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
        eventPlayer.setPlayerNameSnapshot(player.getPlayerNameSnapshot());
        eventPlayer.setPosition(player.getPosition());
        eventPlayer.setJerseyNo(player.getJerseyNo());
        eventPlayer.setIsCaptain(player.getIsCaptain());
        eventPlayer.setIsStarter(player.getIsStarter() == null ? 1 : player.getIsStarter());
        eventPlayer.setEventPlayerStatus(EventPlayerRosterTransition.ACTIVE);
    }
}
