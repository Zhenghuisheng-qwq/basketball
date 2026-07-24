package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketMatchPlayerStat;
import com.zhs.basket.domain.BasketMatchTeamStat;
import com.zhs.basket.domain.BasketStatChangeRecord;
import com.zhs.basket.enums.MatchChangeTypeEnum;
import com.zhs.basket.enums.MatchDataStatusEnum;
import com.zhs.basket.enums.MatchStatusEnum;
import com.zhs.basket.mapper.BasketMatchMapper;
import com.zhs.basket.mapper.BasketMatchPlayerStatMapper;
import com.zhs.basket.mapper.BasketMatchStatusLogMapper;
import com.zhs.basket.mapper.BasketMatchTeamStatMapper;
import com.zhs.basket.mapper.BasketStatChangeRecordMapper;
import com.zhs.basket.req.MatchPlayerStatSaveReq;
import com.zhs.basket.req.MatchReopenReq;
import com.zhs.basket.req.MatchTeamStatSaveReq;
import com.zhs.basket.service.IBasketDataSettlementService;
import com.zhs.basket.service.IBasketMatchStatService;
import com.zhs.basket.service.IBasketMatchValidationService;
import com.zhs.basket.support.BasketMatchMessageHelper;
import com.zhs.basket.vo.MatchAuditLogVo;
import com.zhs.basket.vo.MatchPlayerStatVo;
import com.zhs.basket.vo.MatchStatusLogVo;
import com.zhs.basket.vo.MatchTeamStatVo;
import com.zhs.basket.vo.StatChangeRecordVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BasketMatchStatServiceImpl implements IBasketMatchStatService {

    private static final Integer NOT_DELETED = 0;
    private static final String BIZ_TYPE_MATCH = "MATCH";
    private static final int APPROVAL_NONE = 0;

    @Autowired private IBasketMatchValidationService validationService;
    @Lazy
    @Autowired private IBasketDataSettlementService dataSettlementService;
    @Autowired private BasketMatchMapper matchMapper;
    @Autowired private BasketMatchTeamStatMapper teamStatMapper;
    @Autowired private BasketMatchPlayerStatMapper playerStatMapper;
    @Autowired private BasketMatchStatusLogMapper statusLogMapper;
    @Autowired private BasketStatChangeRecordMapper statChangeRecordMapper;
    @Autowired private BasketMatchMessageHelper messageHelper;

    @Override
    public List<MatchTeamStatVo> selectTeamStats(Long matchId) {
        validationService.requireMatch(matchId);
        List<MatchTeamStatVo> list = teamStatMapper.selectVoByMatchId(matchId);
        for (MatchTeamStatVo vo : list) {
            vo.setDataStatusName(MatchDataStatusEnum.fromCode(vo.getDataStatus()).getDescription());
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTeamStats(Long matchId, MatchTeamStatSaveReq req) {
        BasketMatch match = assertStatEditable(matchId);
        if (req == null || CollectionUtils.isEmpty(req.getStats())) {
            throw new ServiceException("球队技术统计不能为空");
        }
        List<BasketMatchTeamStat> list = new ArrayList<>();
        for (MatchTeamStatSaveReq.TeamStatItem item : req.getStats()) {
            if (!item.getTeamId().equals(match.getHomeTeamId()) && !item.getTeamId().equals(match.getAwayTeamId())) {
                throw new ServiceException("球队须为参赛主客队");
            }
            validateTeamStatItem(item);
            Long opponentId = item.getTeamId().equals(match.getHomeTeamId()) ? match.getAwayTeamId() : match.getHomeTeamId();
            BasketMatchTeamStat stat = buildTeamStat(match, item, opponentId);
            list.add(stat);
        }
        teamStatMapper.softDeleteByMatchId(matchId);
        teamStatMapper.batchInsert(list);
        validationService.writeStatusLog(match, match.getMatchStatus(), MatchChangeTypeEnum.STAT_SAVE, "保存球队技术统计");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitTeamStats(Long matchId) {
        BasketMatch match = validationService.requireMatch(matchId);
        List<MatchTeamStatVo> stats = teamStatMapper.selectVoByMatchId(matchId);
        if (stats.size() < 2) {
            throw new ServiceException("请先录入双方球队技术统计");
        }
        validateTeamStatsAgainstMatch(match, stats);
        validationService.writeStatusLog(match, match.getMatchStatus(), MatchChangeTypeEnum.STAT_SUBMIT, "提交球队技术统计");
    }

    @Override
    public List<MatchPlayerStatVo> selectPlayerStats(Long matchId) {
        validationService.requireMatch(matchId);
        List<MatchPlayerStatVo> list = playerStatMapper.selectVoByMatchId(matchId);
        for (MatchPlayerStatVo vo : list) {
            vo.setDataStatusName(MatchDataStatusEnum.fromCode(vo.getDataStatus()).getDescription());
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePlayerStats(Long matchId, MatchPlayerStatSaveReq req) {
        BasketMatch match = assertStatEditable(matchId);
        if (req == null || CollectionUtils.isEmpty(req.getStats())) {
            throw new ServiceException("球员技术统计不能为空");
        }
        List<BasketMatchPlayerStat> list = new ArrayList<>();
        for (MatchPlayerStatSaveReq.PlayerStatItem item : req.getStats()) {
            if (item.getTeamId() == null || item.getPlayerId() == null) {
                throw new ServiceException("球员统计须包含球队与球员");
            }
            if (!item.getTeamId().equals(match.getHomeTeamId()) && !item.getTeamId().equals(match.getAwayTeamId())) {
                throw new ServiceException("球员所属球队无效");
            }
            validatePlayerStatItem(item);
            BasketMatchPlayerStat stat = buildPlayerStat(match, item);
            list.add(stat);
        }
        playerStatMapper.softDeleteByMatchId(matchId);
        playerStatMapper.batchInsert(list);
        validationService.writeStatusLog(match, match.getMatchStatus(), MatchChangeTypeEnum.STAT_SAVE, "保存球员技术统计");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitPlayerStats(Long matchId) {
        BasketMatch match = validationService.requireMatch(matchId);
        List<MatchPlayerStatVo> playerStats = playerStatMapper.selectVoByMatchId(matchId);
        List<MatchTeamStatVo> teamStats = teamStatMapper.selectVoByMatchId(matchId);
        if (playerStats.isEmpty()) {
            throw new ServiceException("请先录入球员技术统计");
        }
        validatePlayerStatsConsistency(match, playerStats, teamStats);
        validationService.writeStatusLog(match, match.getMatchStatus(), MatchChangeTypeEnum.STAT_SUBMIT, "提交球员技术统计");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmResult(Long matchId) {
        BasketMatch match = validationService.requireMatch(matchId);
        if (match.getMatchStatus() == null || match.getMatchStatus() != MatchStatusEnum.FINISHED.getCode()) {
            throw new ServiceException("比赛未结束，无法确认结果");
        }
        if (match.getDataStatus() == null || !MatchDataStatusEnum.fromCode(match.getDataStatus()).canConfirm()) {
            throw new ServiceException("当前数据状态不可确认");
        }
        List<MatchTeamStatVo> teamStats = teamStatMapper.selectVoByMatchId(matchId);
        List<MatchPlayerStatVo> playerStats = playerStatMapper.selectVoByMatchId(matchId);
        validateTeamStatsAgainstMatch(match, teamStats);
        validatePlayerStatsConsistency(match, playerStats, teamStats);

        Date now = new Date();
        Long operatorId = SecurityUtils.getUserId();
        if (matchMapper.updateDataStatus(matchId, Collections.singletonList(MatchDataStatusEnum.SUBMITTED.getCode()),
                MatchDataStatusEnum.CONFIRMED.getCode()) != 1) {
            throw new ServiceException("确认比赛结果失败");
        }
        teamStatMapper.updateDataStatusByMatchId(matchId,
                Arrays.asList(MatchDataStatusEnum.DRAFT.getCode(), MatchDataStatusEnum.SUBMITTED.getCode(),
                        MatchDataStatusEnum.REOPENED.getCode()),
                MatchDataStatusEnum.CONFIRMED.getCode(), operatorId, now);
        playerStatMapper.updateDataStatusByMatchId(matchId,
                Arrays.asList(MatchDataStatusEnum.DRAFT.getCode(), MatchDataStatusEnum.SUBMITTED.getCode(),
                        MatchDataStatusEnum.REOPENED.getCode()),
                MatchDataStatusEnum.CONFIRMED.getCode(), operatorId, now);

        match.setDataStatus(MatchDataStatusEnum.CONFIRMED.getCode());
        validationService.writeStatusLog(match, match.getMatchStatus(), MatchChangeTypeEnum.RESULT_CONFIRM, "确认比赛结果");
        writeStatSummary(match, MatchDataStatusEnum.SUBMITTED.getCode(), MatchDataStatusEnum.CONFIRMED.getCode(), "确认比赛结果");
        // 同一事务内同步重算；失败则确认回滚
        dataSettlementService.rebuildMatch(matchId);
        messageHelper.notifyCaptains(match, operatorId, "比赛结果已确认",
                "比赛 " + match.getMatchNo() + " 结果已确认。");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reopenResult(Long matchId, MatchReopenReq req) {
        if (StringUtils.isEmpty(req.getReason())) {
            throw new ServiceException("重开原因不能为空");
        }
        BasketMatch match = validationService.requireMatch(matchId);
        if (match.getDataStatus() == null || !MatchDataStatusEnum.fromCode(match.getDataStatus()).canReopen()) {
            throw new ServiceException("当前数据状态不可重开");
        }
        if (matchMapper.updateDataStatus(matchId, Collections.singletonList(MatchDataStatusEnum.CONFIRMED.getCode()),
                MatchDataStatusEnum.REOPENED.getCode()) != 1) {
            throw new ServiceException("重开失败");
        }
        match.setDataStatus(MatchDataStatusEnum.REOPENED.getCode());
        validationService.writeStatusLog(match, match.getMatchStatus(), MatchChangeTypeEnum.RESULT_REOPEN, req.getReason());
        writeStatSummary(match, MatchDataStatusEnum.CONFIRMED.getCode(), MatchDataStatusEnum.REOPENED.getCode(), req.getReason());
        // 重开后该场不再纳入确认集，同一事务内同步重算；失败则重开回滚
        dataSettlementService.rebuildMatch(matchId);
    }

    @Override
    public MatchAuditLogVo selectAuditLog(Long matchId) {
        validationService.requireMatch(matchId);
        MatchAuditLogVo vo = new MatchAuditLogVo();
        List<MatchStatusLogVo> logs = statusLogMapper.selectVoByMatchId(matchId);
        for (MatchStatusLogVo log : logs) {
            if (log.getBeforeStatus() != null) {
                log.setBeforeStatusName(MatchStatusEnum.fromCode(log.getBeforeStatus()).getDescription());
            }
            log.setAfterStatusName(MatchStatusEnum.fromCode(log.getAfterStatus()).getDescription());
            log.setChangeTypeName(MatchChangeTypeEnum.fromCode(log.getChangeType()).getDescription());
        }
        vo.setStatusLogs(logs);
        vo.setStatChanges(statChangeRecordMapper.selectByBiz(BIZ_TYPE_MATCH, matchId));
        return vo;
    }

    private BasketMatch assertStatEditable(Long matchId) {
        BasketMatch match = validationService.requireMatch(matchId);
        MatchStatusEnum status = MatchStatusEnum.fromCode(match.getMatchStatus());
        MatchDataStatusEnum dataStatus = MatchDataStatusEnum.fromCode(match.getDataStatus());
        if (!status.canEnterStat() || !dataStatus.canEdit()) {
            throw new ServiceException("当前状态不可编辑技术统计");
        }
        return match;
    }

    private void validateTeamStatItem(MatchTeamStatSaveReq.TeamStatItem item) {
        validationService.validateShooting(item.getFieldGoalsMade(), item.getFieldGoalsAttempted(), "投篮");
        validationService.validateShooting(item.getThreePointsMade(), item.getThreePointsAttempted(), "三分");
        validationService.validateShooting(item.getFreeThrowsMade(), item.getFreeThrowsAttempted(), "罚球");
        validationService.validateThreeWithinFg(item.getFieldGoalsMade(), item.getThreePointsMade(), "三分");
        validationService.validateTheoreticalPoints(item.getPoints(), item.getFieldGoalsMade(),
                item.getThreePointsMade(), item.getFreeThrowsMade(), "球队");
    }

    private void validatePlayerStatItem(MatchPlayerStatSaveReq.PlayerStatItem item) {
        validationService.validateShooting(item.getFieldGoalsMade(), item.getFieldGoalsAttempted(), "投篮");
        validationService.validateShooting(item.getThreePointsMade(), item.getThreePointsAttempted(), "三分");
        validationService.validateShooting(item.getFreeThrowsMade(), item.getFreeThrowsAttempted(), "罚球");
        validationService.validateThreeWithinFg(item.getFieldGoalsMade(), item.getThreePointsMade(), "三分");
        validationService.validateTheoreticalPoints(item.getPoints(), item.getFieldGoalsMade(),
                item.getThreePointsMade(), item.getFreeThrowsMade(), "球员");
    }

    private BasketMatchTeamStat buildTeamStat(BasketMatch match, MatchTeamStatSaveReq.TeamStatItem item, Long opponentId) {
        BasketMatchTeamStat stat = new BasketMatchTeamStat();
        stat.setMatchId(match.getId());
        stat.setSeasonId(match.getSeasonId());
        stat.setStageId(match.getStageId());
        stat.setTeamId(item.getTeamId());
        stat.setOpponentTeamId(opponentId);
        stat.setPoints(nvl(item.getPoints()));
        stat.setRebounds(nvl(item.getRebounds()));
        stat.setOffensiveRebounds(nvl(item.getOffensiveRebounds()));
        stat.setDefensiveRebounds(nvl(item.getDefensiveRebounds()));
        stat.setAssists(nvl(item.getAssists()));
        stat.setSteals(nvl(item.getSteals()));
        stat.setBlocks(nvl(item.getBlocks()));
        stat.setTurnovers(nvl(item.getTurnovers()));
        stat.setFouls(nvl(item.getFouls()));
        stat.setFieldGoalsMade(nvl(item.getFieldGoalsMade()));
        stat.setFieldGoalsAttempted(nvl(item.getFieldGoalsAttempted()));
        stat.setFieldGoalPct(validationService.computePct(item.getFieldGoalsMade(), item.getFieldGoalsAttempted()));
        stat.setThreePointsMade(nvl(item.getThreePointsMade()));
        stat.setThreePointsAttempted(nvl(item.getThreePointsAttempted()));
        stat.setThreePointPct(validationService.computePct(item.getThreePointsMade(), item.getThreePointsAttempted()));
        stat.setFreeThrowsMade(nvl(item.getFreeThrowsMade()));
        stat.setFreeThrowsAttempted(nvl(item.getFreeThrowsAttempted()));
        stat.setFreeThrowPct(validationService.computePct(item.getFreeThrowsMade(), item.getFreeThrowsAttempted()));
        stat.setPossessions(item.getPossessions() != null ? item.getPossessions() : BigDecimal.ZERO);
        stat.setEfficiency(item.getEfficiency() != null ? item.getEfficiency() : BigDecimal.ZERO);
        stat.setDataStatus(MatchDataStatusEnum.DRAFT.getCode());
        Date now = new Date();
        stat.setCreateTime(now);
        stat.setUpdateTime(now);
        stat.setIsDelete(NOT_DELETED);
        return stat;
    }

    private BasketMatchPlayerStat buildPlayerStat(BasketMatch match, MatchPlayerStatSaveReq.PlayerStatItem item) {
        BasketMatchPlayerStat stat = new BasketMatchPlayerStat();
        stat.setMatchId(match.getId());
        stat.setSeasonId(match.getSeasonId());
        stat.setStageId(match.getStageId());
        stat.setTeamId(item.getTeamId());
        stat.setPlayerId(item.getPlayerId());
        stat.setRosterId(item.getRosterId());
        stat.setStartedFlag(item.getStartedFlag() != null ? item.getStartedFlag() : 0);
        stat.setMinutesSeconds(nvl(item.getMinutesSeconds()));
        stat.setPoints(nvl(item.getPoints()));
        stat.setRebounds(nvl(item.getRebounds()));
        stat.setOffensiveRebounds(nvl(item.getOffensiveRebounds()));
        stat.setDefensiveRebounds(nvl(item.getDefensiveRebounds()));
        stat.setAssists(nvl(item.getAssists()));
        stat.setSteals(nvl(item.getSteals()));
        stat.setBlocks(nvl(item.getBlocks()));
        stat.setTurnovers(nvl(item.getTurnovers()));
        stat.setFouls(nvl(item.getFouls()));
        stat.setFieldGoalsMade(nvl(item.getFieldGoalsMade()));
        stat.setFieldGoalsAttempted(nvl(item.getFieldGoalsAttempted()));
        stat.setFieldGoalPct(validationService.computePct(item.getFieldGoalsMade(), item.getFieldGoalsAttempted()));
        stat.setThreePointsMade(nvl(item.getThreePointsMade()));
        stat.setThreePointsAttempted(nvl(item.getThreePointsAttempted()));
        stat.setThreePointPct(validationService.computePct(item.getThreePointsMade(), item.getThreePointsAttempted()));
        stat.setFreeThrowsMade(nvl(item.getFreeThrowsMade()));
        stat.setFreeThrowsAttempted(nvl(item.getFreeThrowsAttempted()));
        stat.setFreeThrowPct(validationService.computePct(item.getFreeThrowsMade(), item.getFreeThrowsAttempted()));
        stat.setPlusMinus(nvl(item.getPlusMinus()));
        stat.setEfficiency(item.getEfficiency() != null ? item.getEfficiency() : BigDecimal.ZERO);
        stat.setDoubleDouble(0);
        stat.setTripleDouble(0);
        stat.setDataStatus(MatchDataStatusEnum.DRAFT.getCode());
        Date now = new Date();
        stat.setCreateTime(now);
        stat.setUpdateTime(now);
        stat.setIsDelete(NOT_DELETED);
        return stat;
    }


    private void validateTeamStatsAgainstMatch(BasketMatch match, List<MatchTeamStatVo> stats) {
        Map<Long, MatchTeamStatVo> map = new HashMap<>();
        for (MatchTeamStatVo stat : stats) {
            map.put(stat.getTeamId(), stat);
        }
        MatchTeamStatVo home = map.get(match.getHomeTeamId());
        MatchTeamStatVo away = map.get(match.getAwayTeamId());
        if (home == null || away == null) {
            throw new ServiceException("缺少主客队技术统计");
        }
        if (!home.getPoints().equals(match.getHomeScore()) || !away.getPoints().equals(match.getAwayScore())) {
            throw new ServiceException("球队得分须与比赛比分一致");
        }
    }

    private void validatePlayerStatsConsistency(BasketMatch match, List<MatchPlayerStatVo> playerStats,
                                                List<MatchTeamStatVo> teamStats) {
        Map<Long, int[]> teamAgg = new HashMap<>();
        teamAgg.put(match.getHomeTeamId(), new int[10]);
        teamAgg.put(match.getAwayTeamId(), new int[10]);
        int totalPlayerPoints = 0;
        for (MatchPlayerStatVo ps : playerStats) {
            totalPlayerPoints += nvl(ps.getPoints());
            int[] agg = teamAgg.get(ps.getTeamId());
            if (agg == null) {
                throw new ServiceException("球员所属球队无效");
            }
            agg[0] += nvl(ps.getPoints());
            agg[1] += nvl(ps.getRebounds());
            agg[2] += nvl(ps.getAssists());
            agg[3] += nvl(ps.getSteals());
            agg[4] += nvl(ps.getBlocks());
            agg[5] += nvl(ps.getTurnovers());
            agg[6] += nvl(ps.getFouls());
            agg[7] += nvl(ps.getFieldGoalsMade());
            agg[8] += nvl(ps.getThreePointsMade());
            agg[9] += nvl(ps.getFreeThrowsMade());
        }
        int matchTotal = nvl(match.getHomeScore()) + nvl(match.getAwayScore());
        if (totalPlayerPoints != matchTotal) {
            throw new ServiceException("球员总得分须等于比赛总得分");
        }
        for (MatchTeamStatVo ts : teamStats) {
            int[] agg = teamAgg.get(ts.getTeamId());
            if (agg == null) {
                continue;
            }
            if (!ts.getPoints().equals(agg[0]) || !ts.getRebounds().equals(agg[1])
                    || !ts.getAssists().equals(agg[2]) || !ts.getSteals().equals(agg[3])
                    || !ts.getBlocks().equals(agg[4]) || !ts.getTurnovers().equals(agg[5])
                    || !ts.getFouls().equals(agg[6]) || !ts.getFieldGoalsMade().equals(agg[7])
                    || !ts.getThreePointsMade().equals(agg[8]) || !ts.getFreeThrowsMade().equals(agg[9])) {
                throw new ServiceException("球队统计与球员汇总不一致：" + ts.getTeamName());
            }
        }
    }

    private void writeStatSummary(BasketMatch match, int beforeStatus, int afterStatus, String reason) {
        Date now = new Date();
        BasketStatChangeRecord record = new BasketStatChangeRecord();
        record.setBizType(BIZ_TYPE_MATCH);
        record.setBizId(match.getId());
        record.setFieldName("data_status");
        record.setBeforeValue(String.valueOf(beforeStatus));
        record.setAfterValue(String.valueOf(afterStatus));
        record.setChangeReason(reason);
        record.setOperatorId(SecurityUtils.getUserId());
        record.setOperatorName(SecurityUtils.getUsername());
        record.setApprovalStatus(APPROVAL_NONE);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setIsDelete(NOT_DELETED);
        statChangeRecordMapper.insert(record);
    }

    private int nvl(Integer v) {
        return v != null ? v : 0;
    }
}
