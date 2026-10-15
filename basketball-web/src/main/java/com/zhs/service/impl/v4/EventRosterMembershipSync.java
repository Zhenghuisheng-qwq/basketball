package com.zhs.service.impl.v4;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.dao.v4.BasketDivisionDao;
import com.zhs.dao.v4.BasketEventPlayerDao;
import com.zhs.dao.v4.BasketEventTeamDao;
import com.zhs.dao.v4.BasketMatchSubmissionPlayerStatDao;
import com.zhs.dao.v4.BasketRegistrationAuditDao;
import com.zhs.dao.v4.BasketRegistrationDao;
import com.zhs.dao.v4.BasketRegistrationPlayerDao;
import com.zhs.dao.v4.BasketRegistrationRosterChangeDao;
import com.zhs.dao.v4.BasketRegistrationRosterChangePlayerDao;
import com.zhs.dao.v4.BasketV4MatchDao;
import com.zhs.dao.v4.BasketV4MatchPlayerStatDao;
import com.zhs.model.BasketTeamMember;
import com.zhs.model.v4.BasketDivision;
import com.zhs.model.v4.BasketEventPlayer;
import com.zhs.model.v4.BasketEventTeam;
import com.zhs.model.v4.BasketMatchSubmissionPlayerStat;
import com.zhs.model.v4.BasketRegistration;
import com.zhs.model.v4.BasketRegistrationAudit;
import com.zhs.model.v4.BasketRegistrationPlayer;
import com.zhs.model.v4.BasketRegistrationRosterChange;
import com.zhs.model.v4.BasketRegistrationRosterChangePlayer;
import com.zhs.model.v4.BasketV4Match;
import com.zhs.model.v4.BasketV4MatchPlayerStat;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 开赛前，永久球队成员离开后，立即从本队尚未锁定的报名名单释放占用并重算人数。
 * 不改 EventTeam / EventPlayer / 已开始的比赛和统计。
 */
@Service
public class EventRosterMembershipSync {

    private static final Integer NOT_DELETED = 0;
    private static final List<String> LIVE_REGISTRATION = Arrays.asList("PENDING_REVIEW", "APPROVED");
    private static final List<String> STARTED = Arrays.asList("PLAYING", "FINISHED", "FORFEITED");

    @Resource
    private BasketRegistrationDao registrationDao;
    @Resource
    private BasketTeamMemberDao teamMemberDao;
    @Resource
    private BasketRegistrationPlayerDao registrationPlayerDao;
    @Resource
    private BasketRegistrationAuditDao registrationAuditDao;
    @Resource
    private BasketRegistrationRosterChangeDao rosterChangeDao;
    @Resource
    private BasketRegistrationRosterChangePlayerDao rosterChangePlayerDao;
    @Resource
    private BasketV4MatchDao matchDao;
    @Resource
    private BasketV4MatchPlayerStatDao playerStatDao;
    @Resource
    private BasketMatchSubmissionPlayerStatDao submissionPlayerStatDao;
    @Resource
    private BasketDivisionDao divisionDao;
    @Resource
    private BasketEventTeamDao eventTeamDao;
    @Resource
    private BasketEventPlayerDao eventPlayerDao;

    @Transactional(rollbackFor = Exception.class)
    public int releaseFromWritableRegistrations(Long teamId, Long playerId, String reason) {
        if (teamId == null || playerId == null) {
            return 0;
        }
        List<BasketRegistration> registrations = registrationDao.selectList(new LambdaQueryWrapper<BasketRegistration>()
                .eq(BasketRegistration::getTeamId, teamId)
                .in(BasketRegistration::getRegistrationStatus, LIVE_REGISTRATION)
                .eq(BasketRegistration::getIsDelete, NOT_DELETED));
        int released = 0;
        Date now = new Date();
        for (BasketRegistration registration : registrations) {
            if (registration.getDivisionId() != null) {
                divisionDao.selectOne(new LambdaQueryWrapper<BasketDivision>()
                        .eq(BasketDivision::getId, registration.getDivisionId())
                        .last("FOR UPDATE"));
                if (locked(registration.getDivisionId())) {
                    continue;
                }
            }
            if (eventPlayerReferenced(registration.getDivisionId(), registration.getTeamId(), playerId)) {
                continue;
            }
            int rows = registrationPlayerDao.update(null, new LambdaUpdateWrapper<BasketRegistrationPlayer>()
                    .eq(BasketRegistrationPlayer::getRegistrationId, registration.getId())
                    .eq(BasketRegistrationPlayer::getPlayerId, playerId)
                    .eq(BasketRegistrationPlayer::getIsDelete, NOT_DELETED)
                    .set(BasketRegistrationPlayer::getIsDelete, 1)
                    .set(BasketRegistrationPlayer::getUpdateTime, now));
            if (rows <= 0) {
                continue;
            }
            Long remain = registrationPlayerDao.selectCount(new LambdaQueryWrapper<BasketRegistrationPlayer>()
                    .eq(BasketRegistrationPlayer::getRegistrationId, registration.getId())
                    .eq(BasketRegistrationPlayer::getIsDelete, NOT_DELETED));
            registration.setLineupCount(remain == null ? 0 : remain.intValue());
            registration.setLineupVersion((registration.getLineupVersion() == null ? 0 : registration.getLineupVersion()) + 1);
            registration.setUpdateTime(now);
            registrationDao.updateById(registration);
            dropFromPendingChanges(registration.getId(), playerId);
            syncWritableEventPlayers(registration);
            writeAudit(registration, reason + "，当前报名人数 " + registration.getLineupCount(), now);
            released++;
        }
        return released;
    }

    private boolean locked(Long divisionId) {
        BasketDivision division = divisionDao.selectById(divisionId);
        if (division != null) {
            String status = division.getDivisionStatus();
            if ("FINISHED".equals(status) || "ARCHIVED".equals(status)) {
                return true;
            }
        }
        Long started = matchDao.selectCount(new LambdaQueryWrapper<BasketV4Match>()
                .eq(BasketV4Match::getDivisionId, divisionId)
                .eq(BasketV4Match::getIsDelete, NOT_DELETED)
                .in(BasketV4Match::getMatchStatus, STARTED));
        if (started != null && started > 0) {
            return true;
        }
        Long stats = playerStatDao.selectCount(new LambdaQueryWrapper<BasketV4MatchPlayerStat>()
                .eq(BasketV4MatchPlayerStat::getDivisionId, divisionId)
                .eq(BasketV4MatchPlayerStat::getIsDelete, NOT_DELETED));
        if (stats != null && stats > 0) {
            return true;
        }
        Long submissions = submissionPlayerStatDao.selectCount(new LambdaQueryWrapper<BasketMatchSubmissionPlayerStat>()
                .eq(BasketMatchSubmissionPlayerStat::getDivisionId, divisionId)
                .eq(BasketMatchSubmissionPlayerStat::getIsDelete, NOT_DELETED));
        return submissions != null && submissions > 0;
    }

    private void dropFromPendingChanges(Long registrationId, Long playerId) {
        List<BasketRegistrationRosterChange> pending = rosterChangeDao.selectList(new LambdaQueryWrapper<BasketRegistrationRosterChange>()
                .eq(BasketRegistrationRosterChange::getRegistrationId, registrationId)
                .eq(BasketRegistrationRosterChange::getChangeStatus, "PENDING")
                .eq(BasketRegistrationRosterChange::getIsDelete, NOT_DELETED));
        Date now = new Date();
        for (BasketRegistrationRosterChange change : pending) {
            rosterChangePlayerDao.update(null, new LambdaUpdateWrapper<BasketRegistrationRosterChangePlayer>()
                    .eq(BasketRegistrationRosterChangePlayer::getChangeId, change.getId())
                    .eq(BasketRegistrationRosterChangePlayer::getPlayerId, playerId)
                    .eq(BasketRegistrationRosterChangePlayer::getIsDelete, NOT_DELETED)
                    .set(BasketRegistrationRosterChangePlayer::getIsDelete, 1));
            change.setChangeStatus("REJECTED");
            change.setAuditRemark("队员已离队，原名单变更申请失效，请按当前成员重新提交");
            change.setAuditTime(now);
            change.setUpdateTime(now);
            rosterChangeDao.updateById(change);
        }
    }

    public boolean divisionWritable(Long divisionId) {
        return divisionId != null && !locked(divisionId);
    }

    public int countEffectiveRoster(Long registrationId, Long teamId) {
        if (registrationId == null || teamId == null) {
            return 0;
        }
        BasketRegistration registration = registrationDao.selectById(registrationId);
        List<BasketRegistrationPlayer> players = registrationPlayerDao.selectList(new LambdaQueryWrapper<BasketRegistrationPlayer>()
                .eq(BasketRegistrationPlayer::getRegistrationId, registrationId)
                .eq(BasketRegistrationPlayer::getIsDelete, NOT_DELETED));
        if (registration != null && locked(registration.getDivisionId())) {
            return players.size();
        }
        int count = 0;
        for (BasketRegistrationPlayer player : players) {
            Long active = teamMemberDao.selectCount(new LambdaQueryWrapper<BasketTeamMember>()
                    .eq(BasketTeamMember::getTeamId, teamId)
                    .eq(BasketTeamMember::getPlayerId, player.getPlayerId())
                    .eq(BasketTeamMember::getIsCurrent, 1)
                    .eq(BasketTeamMember::getMembershipStatus, 1)
                    .eq(BasketTeamMember::getIsDelete, NOT_DELETED));
            if (active != null && active > 0) {
                count++;
            }
        }
        return count;
    }

    /**
     * 重新入队前失效未冻结的旧报名行，避免未经审核恢复参赛资格。
     */
    public int retireStaleBeforeRejoin(Long teamId, Long playerId) {
        return releaseFromWritableRegistrations(teamId, playerId, "重新加入球队前，旧报名资格已失效");
    }

    /**
     * 修正开赛前已经离队、但报名行仍未删除的历史残留。已冻结赛事不改。
     */
    public int retireHistoricalStaleRows() {
        List<BasketRegistration> registrations = registrationDao.selectList(new LambdaQueryWrapper<BasketRegistration>()
                .in(BasketRegistration::getRegistrationStatus, LIVE_REGISTRATION)
                .eq(BasketRegistration::getIsDelete, NOT_DELETED));
        Set<String> seen = new HashSet<>();
        int released = 0;
        for (BasketRegistration registration : registrations) {
            if (registration.getDivisionId() != null && locked(registration.getDivisionId())) {
                continue;
            }
            List<BasketRegistrationPlayer> players = registrationPlayerDao.selectList(new LambdaQueryWrapper<BasketRegistrationPlayer>()
                    .eq(BasketRegistrationPlayer::getRegistrationId, registration.getId())
                    .eq(BasketRegistrationPlayer::getIsDelete, NOT_DELETED));
            for (BasketRegistrationPlayer player : players) {
                Long active = teamMemberDao.selectCount(new LambdaQueryWrapper<BasketTeamMember>()
                        .eq(BasketTeamMember::getTeamId, registration.getTeamId())
                        .eq(BasketTeamMember::getPlayerId, player.getPlayerId())
                        .eq(BasketTeamMember::getIsCurrent, 1)
                        .eq(BasketTeamMember::getMembershipStatus, 1)
                        .eq(BasketTeamMember::getIsDelete, NOT_DELETED));
                if (active != null && active > 0) {
                    continue;
                }
                String key = registration.getTeamId() + ":" + player.getPlayerId();
                if (!seen.add(key)) {
                    continue;
                }
                released += releaseFromWritableRegistrations(registration.getTeamId(), player.getPlayerId(),
                        "历史离队残留已从开赛前报名名单失效");
            }
        }
        return released;
    }

    private boolean eventPlayerReferenced(Long divisionId, Long teamId, Long playerId) {
        BasketEventTeam eventTeam = findEventTeam(divisionId, teamId);
        if (eventTeam == null) {
            return false;
        }
        BasketEventPlayer eventPlayer = findEventPlayer(eventTeam.getId(), playerId);
        return eventPlayer != null && NOT_DELETED.equals(eventPlayer.getIsDelete()) && referenced(eventPlayer.getId());
    }

    private void syncWritableEventPlayers(BasketRegistration registration) {
        if (registration.getDivisionId() != null && locked(registration.getDivisionId())) {
            return;
        }
        BasketEventTeam eventTeam = findEventTeam(registration.getDivisionId(), registration.getTeamId());
        if (eventTeam == null) {
            return;
        }
        List<BasketRegistrationPlayer> desired = registrationPlayerDao.selectList(new LambdaQueryWrapper<BasketRegistrationPlayer>()
                .eq(BasketRegistrationPlayer::getRegistrationId, registration.getId())
                .eq(BasketRegistrationPlayer::getIsDelete, NOT_DELETED));
        Set<Long> desiredIds = new HashSet<>();
        Date now = new Date();
        for (BasketRegistrationPlayer player : desired) {
            Long active = teamMemberDao.selectCount(new LambdaQueryWrapper<BasketTeamMember>()
                    .eq(BasketTeamMember::getTeamId, registration.getTeamId())
                    .eq(BasketTeamMember::getPlayerId, player.getPlayerId())
                    .eq(BasketTeamMember::getIsCurrent, 1)
                    .eq(BasketTeamMember::getMembershipStatus, 1)
                    .eq(BasketTeamMember::getIsDelete, NOT_DELETED));
            if (active == null || active == 0) {
                continue;
            }
            desiredIds.add(player.getPlayerId());
            BasketEventPlayer current = findEventPlayer(eventTeam.getId(), player.getPlayerId());
            if (current == null) {
                eventPlayerDao.insert(newEventPlayer(eventTeam, player, now));
            } else if (!NOT_DELETED.equals(current.getIsDelete())) {
                copyEventPlayer(current, player);
                current.setIsDelete(NOT_DELETED);
                current.setUpdateTime(now);
                eventPlayerDao.updateById(current);
            } else {
                copyEventPlayer(current, player);
                current.setUpdateTime(now);
                eventPlayerDao.updateById(current);
            }
        }
        List<BasketEventPlayer> existing = eventPlayerDao.selectList(new LambdaQueryWrapper<BasketEventPlayer>()
                .eq(BasketEventPlayer::getEventTeamId, eventTeam.getId())
                .eq(BasketEventPlayer::getIsDelete, NOT_DELETED));
        for (BasketEventPlayer eventPlayer : existing) {
            if (desiredIds.contains(eventPlayer.getSourcePlayerId())) {
                continue;
            }
            if (referenced(eventPlayer.getId())) {
                throw new com.zhs.exception.ServiceException("赛事球员已被比赛数据引用，不能改写正式名单");
            }
            eventPlayerDao.update(null, new LambdaUpdateWrapper<BasketEventPlayer>()
                    .eq(BasketEventPlayer::getId, eventPlayer.getId())
                    .set(BasketEventPlayer::getIsDelete, 1)
                    .set(BasketEventPlayer::getUpdateTime, now));
        }
    }

    private BasketEventTeam findEventTeam(Long divisionId, Long teamId) {
        if (divisionId == null || teamId == null) {
            return null;
        }
        return eventTeamDao.selectOne(new LambdaQueryWrapper<BasketEventTeam>()
                .eq(BasketEventTeam::getDivisionId, divisionId)
                .eq(BasketEventTeam::getSourceTeamId, teamId)
                .eq(BasketEventTeam::getIsDelete, NOT_DELETED)
                .last("limit 1"));
    }

    private BasketEventPlayer findEventPlayer(Long eventTeamId, Long playerId) {
        List<BasketEventPlayer> rows = eventPlayerDao.selectList(new LambdaQueryWrapper<BasketEventPlayer>()
                .eq(BasketEventPlayer::getEventTeamId, eventTeamId)
                .eq(BasketEventPlayer::getSourcePlayerId, playerId)
                .orderByAsc(BasketEventPlayer::getIsDelete)
                .orderByDesc(BasketEventPlayer::getId));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private boolean referenced(Long eventPlayerId) {
        Long stats = playerStatDao.selectCount(new LambdaQueryWrapper<BasketV4MatchPlayerStat>()
                .eq(BasketV4MatchPlayerStat::getEventPlayerId, eventPlayerId)
                .eq(BasketV4MatchPlayerStat::getIsDelete, NOT_DELETED));
        if (stats != null && stats > 0) {
            return true;
        }
        Long submissions = submissionPlayerStatDao.selectCount(new LambdaQueryWrapper<BasketMatchSubmissionPlayerStat>()
                .eq(BasketMatchSubmissionPlayerStat::getEventPlayerId, eventPlayerId)
                .eq(BasketMatchSubmissionPlayerStat::getIsDelete, NOT_DELETED));
        return submissions != null && submissions > 0;
    }

    private BasketEventPlayer newEventPlayer(BasketEventTeam eventTeam, BasketRegistrationPlayer player, Date now) {
        BasketEventPlayer eventPlayer = new BasketEventPlayer();
        eventPlayer.setSeasonId(eventTeam.getSeasonId());
        eventPlayer.setDivisionId(eventTeam.getDivisionId());
        eventPlayer.setEventTeamId(eventTeam.getId());
        copyEventPlayer(eventPlayer, player);
        eventPlayer.setCreateTime(now);
        eventPlayer.setUpdateTime(now);
        eventPlayer.setIsDelete(NOT_DELETED);
        return eventPlayer;
    }

    private void copyEventPlayer(BasketEventPlayer eventPlayer, BasketRegistrationPlayer player) {
        eventPlayer.setSourcePlayerId(player.getPlayerId());
        eventPlayer.setMemberId(player.getMemberId());
        eventPlayer.setPlatformIdentityId(player.getPlatformIdentityId());
        eventPlayer.setPlatformCode(player.getPlatformCode());
        eventPlayer.setGameIdSnapshot(player.getGameIdSnapshot());
        eventPlayer.setPlayerNameSnapshot(player.getPlayerNameSnapshot());
        eventPlayer.setPosition(player.getPosition());
        eventPlayer.setJerseyNo(player.getJerseyNo());
        eventPlayer.setIsCaptain(player.getIsCaptain());
        eventPlayer.setIsStarter(player.getIsStarter());
        eventPlayer.setEventPlayerStatus("ACTIVE");
    }

    private void writeAudit(BasketRegistration registration, String reason, Date now) {
        BasketRegistrationAudit audit = new BasketRegistrationAudit();
        audit.setRegistrationId(registration.getId());
        audit.setDivisionId(registration.getDivisionId());
        audit.setSeasonId(registration.getSeasonId());
        audit.setAuditAction("MEMBER_LEFT");
        audit.setBeforeStatus(registration.getRegistrationStatus());
        audit.setAfterStatus(registration.getRegistrationStatus());
        audit.setAuditRemark(reason);
        audit.setAuditTime(now);
        audit.setCreateTime(now);
        audit.setUpdateTime(now);
        audit.setIsDelete(NOT_DELETED);
        registrationAuditDao.insert(audit);
    }
}
