package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketStatChangeRecord;
import com.zhs.basket.domain.BasketTournament;
import com.zhs.basket.domain.BasketTournamentGroup;
import com.zhs.basket.domain.BasketTournamentSeason;
import com.zhs.basket.domain.BasketTournamentStage;
import com.zhs.basket.enums.MatchDataStatusEnum;
import com.zhs.basket.enums.MatchStatusEnum;
import com.zhs.basket.enums.SettlementTriggerTypeEnum;
import com.zhs.basket.mapper.BasketConfirmedMatchAdminMapper;
import com.zhs.basket.mapper.BasketMatchMapper;
import com.zhs.basket.mapper.BasketPlayerRankAdminMapper;
import com.zhs.basket.mapper.BasketPlayerSeasonStatAdminMapper;
import com.zhs.basket.mapper.BasketStageStandingAdminMapper;
import com.zhs.basket.mapper.BasketStatChangeRecordMapper;
import com.zhs.basket.mapper.BasketTeamRankAdminMapper;
import com.zhs.basket.mapper.BasketTeamSeasonStatAdminMapper;
import com.zhs.basket.mapper.BasketTournamentGroupMapper;
import com.zhs.basket.mapper.BasketTournamentMapper;
import com.zhs.basket.mapper.BasketTournamentRuleMapper;
import com.zhs.basket.mapper.BasketTournamentSeasonMapper;
import com.zhs.basket.mapper.BasketTournamentStageMapper;
import com.zhs.basket.service.IBasketDataSettlementService;
import com.zhs.basket.service.IBasketDataValidationService;
import com.zhs.basket.service.IBasketPlayerSeasonStatService;
import com.zhs.basket.service.IBasketRankingService;
import com.zhs.basket.service.IBasketStandingService;
import com.zhs.basket.service.IBasketTeamSeasonStatService;
import com.zhs.basket.support.SettlementLockHelper;
import com.zhs.basket.support.StandingPointsRule;
import com.zhs.basket.support.StandingPointsRuleParser;
import com.zhs.basket.vo.SeasonSettlementSummaryVo;
import com.zhs.basket.vo.SettlementRebuildResultVo;
import com.zhs.basket.vo.SettlementValidationResultVo;
import com.zhs.basket.vo.StatChangeRecordVo;
import com.zhs.common.core.redis.RedisCache;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Service
public class BasketDataSettlementServiceImpl implements IBasketDataSettlementService {

    private static final Logger log = LoggerFactory.getLogger(BasketDataSettlementServiceImpl.class);

    private static final String BIZ_TYPE_DATA_SETTLEMENT = "DATA_SETTLEMENT";
    private static final String FIELD_REBUILD = "REBUILD";
    private static final String PUBLIC_CACHE_PREFIX = "basket:public:*";
    private static final int APPROVAL_NONE = 0;
    private static final Integer NOT_DELETED = 0;

    @Autowired
    private IBasketDataValidationService validationService;
    @Autowired
    private IBasketStandingService standingService;
    @Autowired
    private IBasketTeamSeasonStatService teamSeasonStatService;
    @Autowired
    private IBasketPlayerSeasonStatService playerSeasonStatService;
    @Autowired
    private IBasketRankingService rankingService;
    @Autowired
    private SettlementLockHelper settlementLockHelper;
    @Autowired
    private BasketTournamentSeasonMapper seasonMapper;
    @Autowired
    private BasketTournamentMapper tournamentMapper;
    @Autowired
    private BasketTournamentStageMapper stageMapper;
    @Autowired
    private BasketTournamentGroupMapper groupMapper;
    @Autowired
    private BasketTournamentRuleMapper ruleMapper;
    @Autowired
    private BasketMatchMapper matchMapper;
    @Autowired
    private BasketConfirmedMatchAdminMapper confirmedMatchAdminMapper;
    @Autowired
    private BasketStageStandingAdminMapper standingAdminMapper;
    @Autowired
    private BasketTeamSeasonStatAdminMapper teamSeasonStatAdminMapper;
    @Autowired
    private BasketPlayerSeasonStatAdminMapper playerSeasonStatAdminMapper;
    @Autowired
    private BasketTeamRankAdminMapper teamRankAdminMapper;
    @Autowired
    private BasketPlayerRankAdminMapper playerRankAdminMapper;
    @Autowired
    private BasketStatChangeRecordMapper statChangeRecordMapper;
    @Autowired
    private RedisCache redisCache;

    @Override
    public List<SeasonSettlementSummaryVo> listSeasonSummaries(Long tournamentId) {
        if (tournamentId == null) {
            throw new ServiceException("赛事ID不能为空");
        }
        List<BasketTournamentSeason> seasons = seasonMapper.selectByTournamentId(tournamentId);
        List<SeasonSettlementSummaryVo> list = new ArrayList<>();
        if (CollectionUtils.isEmpty(seasons)) {
            return list;
        }
        for (BasketTournamentSeason season : seasons) {
            list.add(buildSummary(season));
        }
        return list;
    }

    @Override
    public SeasonSettlementSummaryVo getSeasonSummary(Long seasonId) {
        return buildSummary(requireSeason(seasonId));
    }

    @Override
    public SettlementValidationResultVo validate(Long seasonId) {
        return validationService.validateSeason(seasonId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementRebuildResultVo rebuildSeason(Long seasonId) {
        BasketTournamentSeason season = requireSeason(seasonId);
        return doRebuild(season.getId(), null, null, null, SettlementTriggerTypeEnum.MANUAL_SEASON);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementRebuildResultVo rebuildStage(Long stageId) {
        BasketTournamentStage stage = requireStage(stageId);
        return doRebuild(stage.getSeasonId(), stage.getId(), null, null, SettlementTriggerTypeEnum.MANUAL_STAGE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementRebuildResultVo rebuildGroup(Long groupId) {
        BasketTournamentGroup group = requireGroup(groupId);
        return doRebuild(group.getSeasonId(), group.getStageId(), group.getId(), null,
                SettlementTriggerTypeEnum.MANUAL_GROUP);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementRebuildResultVo rebuildMatch(Long matchId) {
        if (matchId == null) {
            throw new ServiceException("比赛ID不能为空");
        }
        BasketMatch match = matchMapper.selectById(matchId);
        if (match == null || (match.getIsDelete() != null && match.getIsDelete() != 0)) {
            throw new ServiceException("比赛不存在");
        }
        SettlementTriggerTypeEnum trigger = SettlementTriggerTypeEnum.MANUAL_MATCH;
        if (match.getDataStatus() != null
                && match.getDataStatus() == MatchDataStatusEnum.CONFIRMED.getCode()) {
            trigger = SettlementTriggerTypeEnum.MATCH_CONFIRM;
        } else if (match.getDataStatus() != null
                && match.getDataStatus() == MatchDataStatusEnum.REOPENED.getCode()) {
            trigger = SettlementTriggerTypeEnum.MATCH_REOPEN;
        }
        return doRebuild(match.getSeasonId(), null, null, matchId, trigger);
    }

    private SettlementRebuildResultVo doRebuild(Long seasonId, Long stageId, Long groupId, Long matchId,
                                                SettlementTriggerTypeEnum trigger) {
        boolean autoTrigger = trigger == SettlementTriggerTypeEnum.MATCH_CONFIRM
                || trigger == SettlementTriggerTypeEnum.MATCH_REOPEN;
        SettlementValidationResultVo validation = matchId != null && autoTrigger
                ? validationService.validateMatch(matchId)
                : validationService.validateSeason(seasonId);
        if (!validation.isPassed()) {
            // 自动触发时：仅积分规则缺失不阻断确认/重开，仍重算赛季统计与排行；手工重算必须规则完整
            boolean onlyRuleMissing = validation.getErrorCount() == 1
                    && !validation.isStandingRuleConfigured()
                    && validation.getErrors() != null
                    && validation.getErrors().stream().allMatch(e ->
                    e.getReason() != null && e.getReason().contains("积分规则"));
            if (!(autoTrigger && onlyRuleMissing)) {
                throw new ServiceException("数据校验未通过，无法重算，错误数=" + validation.getErrorCount());
            }
        }

        String token = settlementLockHelper.tryLock(seasonId);
        SettlementRebuildResultVo result = new SettlementRebuildResultVo();
        result.setSeasonId(seasonId);
        result.setStageId(stageId);
        result.setGroupId(groupId);
        result.setMatchId(matchId);
        result.setTriggerType(trigger.getCode());
        try {
            StandingPointsRule rule = null;
            try {
                rule = StandingPointsRuleParser.requireFromRules(ruleMapper.selectBySeasonId(seasonId));
            } catch (ServiceException ex) {
                if (!autoTrigger) {
                    throw ex;
                }
            }
            Date now = new Date();
            Date snapshotDate = normalizeSnapshotDate(now);

            int matchCount = confirmedMatchAdminMapper.countBySeasonAndStatuses(
                    seasonId, MatchStatusEnum.FINISHED.getCode(), MatchDataStatusEnum.CONFIRMED.getCode());
            result.setMatchCount(matchCount);

            int standingCount = 0;
            if (rule != null) {
                if (groupId != null) {
                    standingCount = standingService.rebuildGroupStandings(groupId, rule, now);
                } else if (stageId != null) {
                    standingCount = standingService.rebuildStageStandings(stageId, rule, now);
                } else {
                    standingCount = standingService.rebuildSeasonStandings(seasonId, rule, now);
                }
            }
            int teamStatCount = teamSeasonStatService.rebuildSeason(seasonId, now);
            int playerStatCount = playerSeasonStatService.rebuildSeason(seasonId, now);
            rankingService.rebuildSeasonRankings(seasonId, snapshotDate);

            result.setStandingCount(standingCount);
            result.setTeamStatCount(teamStatCount);
            result.setPlayerStatCount(playerStatCount);
            result.setTeamRankCount(teamRankAdminMapper.countBySeasonAndDate(seasonId, snapshotDate));
            result.setPlayerRankCount(playerRankAdminMapper.countBySeasonAndDate(seasonId, snapshotDate));

            writeAudit(seasonId, trigger, result);
            clearPublicCache();

            result.setSuccess(true);
            result.setMessage(rule == null ? "重算成功（积分规则未配置，已跳过积分榜）" : "重算成功");
            return result;
        } finally {
            settlementLockHelper.unlock(seasonId, token);
        }
    }

    private void writeAudit(Long seasonId, SettlementTriggerTypeEnum trigger, SettlementRebuildResultVo result) {
        BasketStatChangeRecord record = new BasketStatChangeRecord();
        record.setBizType(BIZ_TYPE_DATA_SETTLEMENT);
        record.setBizId(seasonId);
        record.setFieldName(FIELD_REBUILD);
        record.setBeforeValue(null);
        record.setAfterValue(trigger.getCode()
                + "|standing=" + result.getStandingCount()
                + "|teamStat=" + result.getTeamStatCount()
                + "|playerStat=" + result.getPlayerStatCount()
                + "|teamRank=" + result.getTeamRankCount()
                + "|playerRank=" + result.getPlayerRankCount());
        record.setChangeReason(trigger.getDescription());
        try {
            record.setOperatorId(SecurityUtils.getUserId());
            record.setOperatorName(SecurityUtils.getUsername());
        } catch (Exception ex) {
            record.setOperatorId(0L);
            record.setOperatorName("system");
        }
        record.setApprovalStatus(APPROVAL_NONE);
        Date auditTime = new Date();
        record.setCreateTime(auditTime);
        record.setUpdateTime(auditTime);
        record.setIsDelete(NOT_DELETED);
        statChangeRecordMapper.insert(record);
    }

    private void clearPublicCache() {
        try {
            Collection<String> keys = redisCache.keys(PUBLIC_CACHE_PREFIX);
            if (keys != null && !keys.isEmpty()) {
                redisCache.deleteObject(keys);
            }
        } catch (Exception ex) {
            log.warn("清理公开缓存失败，已忽略: {}", ex.getMessage());
        }
    }

    private SeasonSettlementSummaryVo buildSummary(BasketTournamentSeason season) {
        SeasonSettlementSummaryVo vo = new SeasonSettlementSummaryVo();
        vo.setSeasonId(season.getId());
        vo.setSeasonName(season.getSeasonName());
        vo.setTournamentId(season.getTournamentId());
        vo.setSeasonStatus(season.getSeasonStatus());
        if (season.getTournamentId() != null) {
            BasketTournament tournament = tournamentMapper.selectById(season.getTournamentId());
            if (tournament != null) {
                vo.setTournamentName(tournament.getTournamentName());
            }
        }
        Long seasonId = season.getId();
        vo.setConfirmedMatchCount(confirmedMatchAdminMapper.countBySeasonAndStatuses(
                seasonId, MatchStatusEnum.FINISHED.getCode(), MatchDataStatusEnum.CONFIRMED.getCode()));
        vo.setUnfinishedMatchCount(confirmedMatchAdminMapper.countUnfinished(seasonId));
        vo.setReopenedMatchCount(confirmedMatchAdminMapper.countReopened(seasonId));
        vo.setCancelledMatchCount(confirmedMatchAdminMapper.countCancelled(seasonId));
        vo.setStandingCount(standingAdminMapper.countBySeason(seasonId));
        vo.setTeamSeasonStatCount(teamSeasonStatAdminMapper.countBySeason(seasonId));
        vo.setPlayerSeasonStatCount(playerSeasonStatAdminMapper.countBySeason(seasonId));
        Date today = normalizeSnapshotDate(new Date());
        vo.setTeamRankCount(teamRankAdminMapper.countBySeasonAndDate(seasonId, today));
        vo.setPlayerRankCount(playerRankAdminMapper.countBySeasonAndDate(seasonId, today));
        boolean ruleOk = false;
        try {
            StandingPointsRuleParser.requireFromRules(ruleMapper.selectBySeasonId(seasonId));
            ruleOk = true;
        } catch (ServiceException ignored) {
            // not configured
        }
        vo.setStandingRuleConfigured(ruleOk);
        vo.setLastStandingTime(standingAdminMapper.selectMaxSnapshotTime(seasonId));
        vo.setLastTeamStatTime(teamSeasonStatAdminMapper.selectMaxCalculatedTime(seasonId));
        vo.setLastPlayerStatTime(playerSeasonStatAdminMapper.selectMaxCalculatedTime(seasonId));

        Date last = maxDate(vo.getLastStandingTime(), vo.getLastTeamStatTime(), vo.getLastPlayerStatTime());
        List<StatChangeRecordVo> audits = statChangeRecordMapper.selectByBiz(BIZ_TYPE_DATA_SETTLEMENT, seasonId);
        if (!CollectionUtils.isEmpty(audits)) {
            StatChangeRecordVo latest = audits.get(0);
            vo.setLastOperatorName(latest.getOperatorName());
            if (latest.getCreateTime() != null) {
                last = maxDate(last, latest.getCreateTime());
            }
        }
        vo.setLastSettlementTime(last);
        return vo;
    }

    private Date maxDate(Date... dates) {
        Date max = null;
        for (Date d : dates) {
            if (d == null) {
                continue;
            }
            if (max == null || d.after(max)) {
                max = d;
            }
        }
        return max;
    }

    private Date normalizeSnapshotDate(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private BasketTournamentSeason requireSeason(Long seasonId) {
        if (seasonId == null) {
            throw new ServiceException("赛季ID不能为空");
        }
        BasketTournamentSeason season = seasonMapper.selectById(seasonId);
        if (season == null || (season.getIsDelete() != null && season.getIsDelete() != 0)) {
            throw new ServiceException("赛季不存在");
        }
        return season;
    }

    private BasketTournamentStage requireStage(Long stageId) {
        if (stageId == null) {
            throw new ServiceException("阶段ID不能为空");
        }
        BasketTournamentStage stage = stageMapper.selectById(stageId);
        if (stage == null || (stage.getIsDelete() != null && stage.getIsDelete() != 0)) {
            throw new ServiceException("阶段不存在");
        }
        return stage;
    }

    private BasketTournamentGroup requireGroup(Long groupId) {
        if (groupId == null) {
            throw new ServiceException("小组ID不能为空");
        }
        BasketTournamentGroup group = groupMapper.selectById(groupId);
        if (group == null || (group.getIsDelete() != null && group.getIsDelete() != 0)) {
            throw new ServiceException("小组不存在");
        }
        return group;
    }
}
