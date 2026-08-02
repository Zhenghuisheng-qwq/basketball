package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketFileAsset;
import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketMatchDataSubmission;
import com.zhs.basket.domain.BasketMatchDataSubmissionMaterial;
import com.zhs.basket.domain.BasketMatchDataSubmissionPeriod;
import com.zhs.basket.domain.BasketMatchDataSubmissionPlayerStat;
import com.zhs.basket.domain.BasketMatchDataSubmissionTeamStat;
import com.zhs.basket.domain.BasketTournamentSeasonRoster;
import com.zhs.basket.enums.MatchPeriodTypeEnum;
import com.zhs.basket.mapper.BasketFileAssetMapper;
import com.zhs.basket.mapper.BasketMatchMapper;
import com.zhs.basket.mapper.BasketMatchRosterMapper;
import com.zhs.basket.mapper.BasketTournamentSeasonRosterMapper;
import com.zhs.basket.service.IBasketMatchDataSubmissionValidationService;
import com.zhs.basket.service.IBasketMatchValidationService;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BasketMatchDataSubmissionValidationServiceImpl implements IBasketMatchDataSubmissionValidationService {

    private static final String MATERIAL_RESULT_SCREENSHOT = "RESULT_SCREENSHOT";

    @Autowired private IBasketMatchValidationService validationService;
    @Autowired private BasketMatchRosterMapper rosterMapper;
    @Autowired private BasketMatchMapper matchMapper;
    @Autowired private BasketTournamentSeasonRosterMapper seasonRosterMapper;
    @Autowired private BasketFileAssetMapper fileAssetMapper;

    @Override
    public void validateSubmissionData(BasketMatchDataSubmission submission,
            List<BasketMatchDataSubmissionPeriod> periods,
            List<BasketMatchDataSubmissionTeamStat> teamStats,
            List<BasketMatchDataSubmissionPlayerStat> playerStats,
            List<BasketMatchDataSubmissionMaterial> materials) {
        if (submission == null) {
            throw new ServiceException("提交记录不存在");
        }
        int homeScore = nvl(submission.getHomeScore());
        int awayScore = nvl(submission.getAwayScore());
        if (homeScore < 0 || awayScore < 0) {
            throw new ServiceException("比分不能为负数");
        }
        validatePeriods(submission, periods, homeScore, awayScore);
        validateTeamStats(submission, teamStats, homeScore, awayScore);
        validatePlayerStats(submission, playerStats, teamStats, homeScore, awayScore);
        validateMaterials(materials);
    }

    @Override
    public void validateCorrectionData(BasketMatchDataSubmission submission,
            List<BasketMatchDataSubmissionPeriod> periods,
            List<BasketMatchDataSubmissionTeamStat> teamStats,
            List<BasketMatchDataSubmissionPlayerStat> playerStats) {
        if (submission == null) {
            throw new ServiceException("提交记录不存在");
        }
        int homeScore = nvl(submission.getHomeScore());
        int awayScore = nvl(submission.getAwayScore());
        if (homeScore < 0 || awayScore < 0) {
            throw new ServiceException("比分不能为负数");
        }
        validatePeriods(submission, periods, homeScore, awayScore);
        validateTeamStats(submission, teamStats, homeScore, awayScore);
        validatePlayerStats(submission, playerStats, teamStats, homeScore, awayScore);
    }

    private void validatePeriods(BasketMatchDataSubmission submission, List<BasketMatchDataSubmissionPeriod> periods,
            int homeScore, int awayScore) {
        if (CollectionUtils.isEmpty(periods) || periods.size() < 4) {
            throw new ServiceException("分节比分至少包含4节");
        }
        int homeTotal = 0;
        int awayTotal = 0;
        int otCount = 0;
        for (BasketMatchDataSubmissionPeriod period : periods) {
            if (period.getPeriodNo() == null || period.getPeriodType() == null) {
                throw new ServiceException("节次信息不完整");
            }
            MatchPeriodTypeEnum.fromCode(period.getPeriodType());
            int hs = nvl(period.getHomeScore());
            int as = nvl(period.getAwayScore());
            if (hs < 0 || as < 0) {
                throw new ServiceException("分节比分不能为负数");
            }
            homeTotal += hs;
            awayTotal += as;
            if (MatchPeriodTypeEnum.OVERTIME.getCode().equalsIgnoreCase(period.getPeriodType())) {
                otCount++;
            }
        }
        if (homeTotal != homeScore || awayTotal != awayScore) {
            throw new ServiceException("分节比分之和须等于最终比分");
        }
        if (homeScore == awayScore && otCount == 0) {
            throw new ServiceException("常规时间平局须通过加时赛分出胜负");
        }
    }

    private void validateTeamStats(BasketMatchDataSubmission submission,
            List<BasketMatchDataSubmissionTeamStat> teamStats, int homeScore, int awayScore) {
        if (CollectionUtils.isEmpty(teamStats) || teamStats.size() < 2) {
            throw new ServiceException("球队技术统计须包含主客队各一条");
        }
        Map<Long, BasketMatchDataSubmissionTeamStat> map = new HashMap<>();
        for (BasketMatchDataSubmissionTeamStat stat : teamStats) {
            if (stat.getTeamId() == null) {
                throw new ServiceException("球队统计须包含球队ID");
            }
            if (!stat.getTeamId().equals(submission.getHomeTeamId()) && !stat.getTeamId().equals(submission.getAwayTeamId())) {
                throw new ServiceException("球队须为参赛主客队");
            }
            validateNonNegativeStat(stat);
            validationService.validateShooting(stat.getFieldGoalsMade(), stat.getFieldGoalsAttempted(), "投篮");
            validationService.validateShooting(stat.getThreePointsMade(), stat.getThreePointsAttempted(), "三分");
            validationService.validateShooting(stat.getFreeThrowsMade(), stat.getFreeThrowsAttempted(), "罚球");
            validationService.validateThreeWithinFg(stat.getFieldGoalsMade(), stat.getThreePointsMade(), "三分");
            validationService.validateTheoreticalPoints(stat.getPoints(), stat.getFieldGoalsMade(),
                    stat.getThreePointsMade(), stat.getFreeThrowsMade(), "球队");
            map.put(stat.getTeamId(), stat);
        }
        BasketMatchDataSubmissionTeamStat home = map.get(submission.getHomeTeamId());
        BasketMatchDataSubmissionTeamStat away = map.get(submission.getAwayTeamId());
        if (home == null || away == null) {
            throw new ServiceException("缺少主客队技术统计");
        }
        if (!nvl(home.getPoints()).equals(homeScore) || !nvl(away.getPoints()).equals(awayScore)) {
            throw new ServiceException("球队得分须与比赛比分一致");
        }
    }

    private void validatePlayerStats(BasketMatchDataSubmission submission,
            List<BasketMatchDataSubmissionPlayerStat> playerStats,
            List<BasketMatchDataSubmissionTeamStat> teamStats,
            int homeScore, int awayScore) {
        if (CollectionUtils.isEmpty(playerStats)) {
            throw new ServiceException("球员技术统计不能为空");
        }
        Set<Long> effectiveRosterPlayerIds = loadEffectiveRosterPlayerIds(submission.getMatchId());
        Set<Long> playerIds = new HashSet<>();
        Map<Long, int[]> teamAgg = new HashMap<>();
        teamAgg.put(submission.getHomeTeamId(), new int[10]);
        teamAgg.put(submission.getAwayTeamId(), new int[10]);
        int totalPlayerPoints = 0;
        for (BasketMatchDataSubmissionPlayerStat ps : playerStats) {
            if (ps.getPlayerId() == null || ps.getTeamId() == null) {
                throw new ServiceException("球员统计须包含球队与球员");
            }
            if (!playerIds.add(ps.getPlayerId())) {
                throw new ServiceException("同一球员不能重复录入");
            }
            if (!ps.getTeamId().equals(submission.getHomeTeamId()) && !ps.getTeamId().equals(submission.getAwayTeamId())) {
                throw new ServiceException("球员所属球队无效");
            }
            if (!effectiveRosterPlayerIds.contains(ps.getPlayerId())) {
                throw new ServiceException("球员须来自本场出场名单：playerId=" + ps.getPlayerId());
            }
            validateNonNegativePlayerStat(ps);
            validationService.validateShooting(ps.getFieldGoalsMade(), ps.getFieldGoalsAttempted(), "投篮");
            validationService.validateShooting(ps.getThreePointsMade(), ps.getThreePointsAttempted(), "三分");
            validationService.validateShooting(ps.getFreeThrowsMade(), ps.getFreeThrowsAttempted(), "罚球");
            validationService.validateThreeWithinFg(ps.getFieldGoalsMade(), ps.getThreePointsMade(), "三分");
            validationService.validateTheoreticalPoints(ps.getPoints(), ps.getFieldGoalsMade(),
                    ps.getThreePointsMade(), ps.getFreeThrowsMade(), "球员");
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
        Long submitterTeamId = submission.getSubmitterTeamId();
        Long opponentTeamId = resolveOpponentTeamId(submission, submitterTeamId);
        boolean captainOnlySubmit = isCaptainOnlyTeamPlayerSubmit(submitterTeamId, opponentTeamId, teamAgg);
        if (captainOnlySubmit) {
            int expectedSubmitterScore = submitterTeamId.equals(submission.getHomeTeamId()) ? homeScore : awayScore;
            int submitterPlayerPoints = teamAgg.get(submitterTeamId)[0];
            if (submitterPlayerPoints != expectedSubmitterScore) {
                throw new ServiceException("提交方球员总得分须与该队比分一致");
            }
        } else if (totalPlayerPoints != homeScore + awayScore) {
            throw new ServiceException("球员总得分须等于比赛总得分");
        }
        for (BasketMatchDataSubmissionTeamStat ts : teamStats) {
            int[] agg = teamAgg.get(ts.getTeamId());
            if (agg == null) {
                continue;
            }
            if (captainOnlySubmit && !ts.getTeamId().equals(submitterTeamId)) {
                int expectedTeamScore = ts.getTeamId().equals(submission.getHomeTeamId()) ? homeScore : awayScore;
                if (nvl(ts.getPoints()) != expectedTeamScore) {
                    throw new ServiceException("球队得分须与比赛比分一致");
                }
                continue;
            }
            if (nvl(ts.getPoints()) != agg[0] || nvl(ts.getRebounds()) != agg[1]
                    || nvl(ts.getAssists()) != agg[2] || nvl(ts.getSteals()) != agg[3]
                    || nvl(ts.getBlocks()) != agg[4] || nvl(ts.getTurnovers()) != agg[5]
                    || nvl(ts.getFouls()) != agg[6] || nvl(ts.getFieldGoalsMade()) != agg[7]
                    || nvl(ts.getThreePointsMade()) != agg[8] || nvl(ts.getFreeThrowsMade()) != agg[9]) {
                throw new ServiceException("球队统计与球员汇总不一致");
            }
        }
    }

    /**
     * Web 端「本队球员数据」简化提交：仅提交方录入真实数据，对手球员为零分占位。
     */
    private boolean isCaptainOnlyTeamPlayerSubmit(Long submitterTeamId, Long opponentTeamId, Map<Long, int[]> teamAgg) {
        if (submitterTeamId == null || opponentTeamId == null) {
            return false;
        }
        int[] submitterAgg = teamAgg.get(submitterTeamId);
        int[] opponentAgg = teamAgg.get(opponentTeamId);
        if (submitterAgg == null || opponentAgg == null) {
            return false;
        }
        return submitterAgg[0] > 0 && opponentAgg[0] == 0;
    }

    private Long resolveOpponentTeamId(BasketMatchDataSubmission submission, Long submitterTeamId) {
        if (submitterTeamId == null) {
            return null;
        }
        if (submitterTeamId.equals(submission.getHomeTeamId())) {
            return submission.getAwayTeamId();
        }
        if (submitterTeamId.equals(submission.getAwayTeamId())) {
            return submission.getHomeTeamId();
        }
        return null;
    }

    private void validateMaterials(List<BasketMatchDataSubmissionMaterial> materials) {
        if (CollectionUtils.isEmpty(materials)) {
            return;
        }
        boolean hasScreenshot = false;
        for (BasketMatchDataSubmissionMaterial material : materials) {
            if (material.getFileId() == null) {
                throw new ServiceException("材料文件不能为空");
            }
            BasketFileAsset file = fileAssetMapper.selectById(material.getFileId());
            if (file == null) {
                throw new ServiceException("材料文件不存在或已删除");
            }
            if (MATERIAL_RESULT_SCREENSHOT.equalsIgnoreCase(material.getMaterialType())) {
                hasScreenshot = true;
            }
        }
        if (!hasScreenshot) {
            throw new ServiceException("至少上传一张完整比赛结算截图");
        }
    }

    private void validateNonNegativeStat(BasketMatchDataSubmissionTeamStat stat) {
        if (nvl(stat.getPoints()) < 0 || nvl(stat.getRebounds()) < 0 || nvl(stat.getAssists()) < 0
                || nvl(stat.getSteals()) < 0 || nvl(stat.getBlocks()) < 0 || nvl(stat.getTurnovers()) < 0
                || nvl(stat.getFouls()) < 0) {
            throw new ServiceException("球队统计不能为负数");
        }
    }

    private void validateNonNegativePlayerStat(BasketMatchDataSubmissionPlayerStat stat) {
        if (nvl(stat.getPoints()) < 0 || nvl(stat.getRebounds()) < 0 || nvl(stat.getAssists()) < 0
                || nvl(stat.getSteals()) < 0 || nvl(stat.getBlocks()) < 0 || nvl(stat.getTurnovers()) < 0
                || nvl(stat.getFouls()) < 0 || nvl(stat.getMinutesSeconds()) < 0) {
            throw new ServiceException("球员统计不能为负数");
        }
    }

    private Integer nvl(Integer value) {
        return value != null ? value : 0;
    }

    /**
     * 与 Web 端提交校验一致：优先比赛出场名单，缺失时回退赛事阵容快照。
     */
    private Set<Long> loadEffectiveRosterPlayerIds(Long matchId) {
        BasketMatch match = matchMapper.selectById(matchId);
        if (match == null) {
            throw new ServiceException("比赛不存在");
        }
        Set<Long> playerIds = new HashSet<>();
        addTeamEffectiveRosterPlayers(playerIds, match.getId(), match.getHomeTeamId(), match.getHomeEventTeamId());
        addTeamEffectiveRosterPlayers(playerIds, match.getId(), match.getAwayTeamId(), match.getAwayEventTeamId());
        return playerIds;
    }

    private void addTeamEffectiveRosterPlayers(Set<Long> playerIds, Long matchId, Long teamId, Long eventTeamId) {
        if (teamId == null) {
            return;
        }
        List<Long> matchRosterPlayerIds = rosterMapper.selectPlayerIdsByMatchAndTeam(matchId, teamId);
        if (!CollectionUtils.isEmpty(matchRosterPlayerIds)) {
            playerIds.addAll(matchRosterPlayerIds);
            return;
        }
        if (eventTeamId == null) {
            return;
        }
        List<BasketTournamentSeasonRoster> seasonRosters = seasonRosterMapper.selectActiveByEventTeamId(eventTeamId);
        if (CollectionUtils.isEmpty(seasonRosters)) {
            return;
        }
        for (BasketTournamentSeasonRoster roster : seasonRosters) {
            if (roster.getPlayerId() != null) {
                playerIds.add(roster.getPlayerId());
            }
        }
    }
}
