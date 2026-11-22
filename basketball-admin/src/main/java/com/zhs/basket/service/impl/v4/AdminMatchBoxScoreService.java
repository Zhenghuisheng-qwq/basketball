package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.BasketPlayerPlatformAccount;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketEventPlayer;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.domain.v4.BasketMatchSubmission;
import com.zhs.basket.domain.v4.BasketMatchSubmissionPeriodScore;
import com.zhs.basket.domain.v4.BasketMatchSubmissionPlayerStat;
import com.zhs.basket.domain.v4.BasketMatchSubmissionTeamStat;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.dto.v4.BasketAdminBoxScoreReq;
import com.zhs.basket.enums.v4.SubmissionStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketEventPlayerMapper;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.mapper.v4.BasketMatchSubmissionMapper;
import com.zhs.basket.mapper.v4.BasketMatchSubmissionPeriodScoreMapper;
import com.zhs.basket.mapper.v4.BasketMatchSubmissionPlayerStatMapper;
import com.zhs.basket.mapper.v4.BasketMatchSubmissionTeamStatMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.req.AuditFieldInput;
import com.zhs.basket.support.BasketBusinessAuditWriter;
import com.zhs.basket.support.v4.AdminOfficialResultCapability;
import com.zhs.basket.support.v4.CurrentEventGameId;
import com.zhs.basket.support.v4.EventGameIdSnapshotSync;
import com.zhs.basket.support.v4.StatsScreenshotRecognizer;
import com.zhs.basket.support.v4.StatsScreenshotRecognizer.PlayerRef;
import com.zhs.basket.support.v4.StatsScreenshotRecognizer.SideRoster;
import com.zhs.basket.support.v4.StatsScreenshotResult;
import com.zhs.basket.support.v4.schedule.PairingResolutionGuard;
import com.zhs.basket.support.v4.settlement.OfficialMatchResultSettlementService;
import com.zhs.basket.vo.v4.AdminMatchBoxScoreRosterVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 后台按一场比赛上传双方详细技术统计。识别只填表，确认后写成已通过的完整提交并结算正式结果。
 */
@Service
public class AdminMatchBoxScoreService {

    private static final int NOT_DELETED = 0;

    private final BasketV4MatchMapper matchMapper;
    private final BasketDivisionMapper divisionMapper;
    private final BasketEventTeamMapper eventTeamMapper;
    private final BasketEventPlayerMapper eventPlayerMapper;
    private final BasketMatchSubmissionMapper submissionMapper;
    private final BasketMatchSubmissionTeamStatMapper teamStatMapper;
    private final BasketMatchSubmissionPlayerStatMapper playerStatMapper;
    private final BasketMatchSubmissionPeriodScoreMapper periodScoreMapper;
    private final OfficialMatchResultSettlementService settlementService;
    private final StatsScreenshotRecognizer recognizer;
    private final BasketBusinessAuditWriter auditWriter;
    private final EventGameIdSnapshotSync eventGameIdSnapshotSync;

    public AdminMatchBoxScoreService(BasketV4MatchMapper matchMapper,
                                     BasketDivisionMapper divisionMapper,
                                     BasketEventTeamMapper eventTeamMapper,
                                     BasketEventPlayerMapper eventPlayerMapper,
                                     BasketMatchSubmissionMapper submissionMapper,
                                     BasketMatchSubmissionTeamStatMapper teamStatMapper,
                                     BasketMatchSubmissionPlayerStatMapper playerStatMapper,
                                     BasketMatchSubmissionPeriodScoreMapper periodScoreMapper,
                                     OfficialMatchResultSettlementService settlementService,
                                     StatsScreenshotRecognizer recognizer,
                                     BasketBusinessAuditWriter auditWriter,
                                     EventGameIdSnapshotSync eventGameIdSnapshotSync) {
        this.matchMapper = matchMapper;
        this.divisionMapper = divisionMapper;
        this.eventTeamMapper = eventTeamMapper;
        this.eventPlayerMapper = eventPlayerMapper;
        this.submissionMapper = submissionMapper;
        this.teamStatMapper = teamStatMapper;
        this.playerStatMapper = playerStatMapper;
        this.periodScoreMapper = periodScoreMapper;
        this.settlementService = settlementService;
        this.recognizer = recognizer;
        this.auditWriter = auditWriter;
        this.eventGameIdSnapshotSync = eventGameIdSnapshotSync;
    }

    public AdminMatchBoxScoreRosterVo roster(Long matchId) {
        BasketV4Match match = requireEnterableMatch(matchId, false);
        BasketDivision division = divisionMapper.selectById(match.getDivisionId());
        AdminMatchBoxScoreRosterVo vo = new AdminMatchBoxScoreRosterVo();
        vo.setMatchId(match.getId());
        vo.setHomeEventTeamId(match.getHomeEventTeamId());
        vo.setAwayEventTeamId(match.getAwayEventTeamId());
        vo.setHomeTeamName(teamName(match.getHomeEventTeamId()));
        vo.setAwayTeamName(teamName(match.getAwayEventTeamId()));
        vo.setGameMode(division == null ? null : division.getGameMode());
        vo.setPeriodRequired(requiresPeriods(vo.getGameMode()));
        List<BasketEventPlayer> home = activePlayers(match.getHomeEventTeamId());
        List<BasketEventPlayer> away = activePlayers(match.getAwayEventTeamId());
        Set<Long> playerIds = new HashSet<>();
        collectSourcePlayerIds(home, playerIds);
        collectSourcePlayerIds(away, playerIds);
        Map<Long, List<BasketPlayerPlatformAccount>> accounts = eventGameIdSnapshotSync.loadAccounts(playerIds);
        vo.setHomePlayers(toRoster(home, accounts));
        vo.setAwayPlayers(toRoster(away, accounts));
        return vo;
    }

    public StatsScreenshotResult recognize(Long matchId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请上传比赛结算截图");
        }
        AdminMatchBoxScoreRosterVo roster = roster(matchId);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception ex) {
            throw new ServiceException("读取截图失败");
        }
        return recognizer.recognize(
                bytes,
                file.getContentType(),
                new SideRoster(roster.getHomeTeamName(), toRefs(roster.getHomePlayers())),
                new SideRoster(roster.getAwayTeamName(), toRefs(roster.getAwayPlayers())));
    }

    @Transactional(rollbackFor = Exception.class)
    public void enter(BasketAdminBoxScoreReq req) {
        if (req == null || req.getMatchId() == null) {
            throw new ServiceException("请选择比赛");
        }
        String reason = req.getReason() == null ? "" : req.getReason().trim();
        if (reason.isEmpty()) {
            throw new ServiceException("请填写录入原因");
        }
        if (reason.length() > 500) {
            throw new ServiceException("录入原因不能超过500字");
        }
        BasketV4Match match = requireEnterableMatch(req.getMatchId(), true);
        BasketDivision division = divisionMapper.selectById(match.getDivisionId());
        boolean periodsRequired = requiresPeriods(division == null ? null : division.getGameMode());
        List<AdminMatchBoxScoreRosterVo.Player> homeRoster = rosterOf(match.getHomeEventTeamId());
        List<AdminMatchBoxScoreRosterVo.Player> awayRoster = rosterOf(match.getAwayEventTeamId());
        String homeName = teamName(match.getHomeEventTeamId());
        String awayName = teamName(match.getAwayEventTeamId());
        List<BasketAdminBoxScoreReq.PlayerRow> homeRows = usedRows(req.getHomePlayers());
        List<BasketAdminBoxScoreReq.PlayerRow> awayRows = usedRows(req.getAwayPlayers());
        validateSide(homeName, homeRows, idsOf(homeRoster));
        validateSide(awayName, awayRows, idsOf(awayRoster));
        int homeScore = req.getHomeScore() != null ? req.getHomeScore() : sumPoints(homeRows);
        int awayScore = req.getAwayScore() != null ? req.getAwayScore() : sumPoints(awayRows);
        if (homeScore < 0 || awayScore < 0) {
            throw new ServiceException("比分不能为负数");
        }
        if (homeScore == awayScore) {
            throw new ServiceException("双方得分相同，不能结算胜负。请先改比分");
        }
        if (periodsRequired) {
            validatePeriods(req.getPeriods(), homeScore, awayScore);
        }

        assertNoPending(match.getId(), match.getHomeEventTeamId());
        assertNoPending(match.getId(), match.getAwayEventTeamId());

        Date now = new Date();
        Integer maxVersion = submissionMapper.selectMaxVersionNo(match.getId(), match.getHomeEventTeamId());
        BasketMatchSubmission submission = new BasketMatchSubmission();
        submission.setMatchId(match.getId());
        submission.setSeasonId(match.getSeasonId());
        submission.setDivisionId(match.getDivisionId());
        submission.setSubmitEventTeamId(match.getHomeEventTeamId());
        submission.setOpponentEventTeamId(match.getAwayEventTeamId());
        submission.setSubmitterRole("ADMIN");
        submission.setSubmissionStatus(SubmissionStatusEnum.APPROVED.getCode());
        submission.setVersionNo(maxVersion == null ? 1 : maxVersion + 1);
        submission.setHomeScore(homeScore);
        submission.setAwayScore(awayScore);
        submission.setSubmitTime(now);
        submission.setAuditUserId(SecurityUtils.getUserId());
        submission.setAuditUserName(SecurityUtils.getUsername());
        submission.setAuditTime(now);
        submission.setAuditRemark("管理员上传详细数据");
        submission.setSubmitRemark(reason);
        submission.setSubmissionMode("WINNER_FULL_MATCH");
        submission.setDraftComplete(1);
        submission.setCreateTime(now);
        submission.setUpdateTime(now);
        submission.setIsDelete(NOT_DELETED);
        if (submissionMapper.insert(submission) != 1 || submission.getId() == null) {
            throw new ServiceException("保存比赛数据失败");
        }
        insertTeam(submission, match.getHomeEventTeamId(), homeRows, homeScore, now);
        insertTeam(submission, match.getAwayEventTeamId(), awayRows, awayScore, now);
        insertPlayers(submission, match.getHomeEventTeamId(), homeRows, now);
        insertPlayers(submission, match.getAwayEventTeamId(), awayRows, now);
        if (periodsRequired) {
            insertPeriods(submission, req.getPeriods(), now);
        }
        settlementService.settleFromSubmission(submission.getId());
        auditWriter.writeAudit("V4_STAGE", "MATCH", match.getId(), "ADMIN_BOX_SCORE",
                "管理员上传详细数据", reason, List.of(
                        AuditFieldInput.of("homeScore", "主队比分", null, homeScore),
                        AuditFieldInput.of("awayScore", "客队比分", null, awayScore),
                        AuditFieldInput.of("submissionId", "提交", null, submission.getId())
                ));
    }

    private BasketV4Match requireEnterableMatch(Long matchId, boolean lock) {
        if (matchId == null) {
            throw new ServiceException("请选择比赛");
        }
        BasketV4Match match = matchMapper.selectById(matchId);
        if (match == null) {
            throw new ServiceException("比赛不存在");
        }
        if (lock) {
            divisionMapper.selectByIdForUpdate(match.getDivisionId());
            match = matchMapper.selectByIdForUpdate(matchId);
            if (match == null) {
                throw new ServiceException("比赛不存在");
            }
        }
        PairingResolutionGuard.assertResolvedForExecution(match.getHomeEventTeamId(), match.getAwayEventTeamId());
        if (!AdminOfficialResultCapability.canEnterOfficialResult(match)) {
            throw new ServiceException("这场已经有正式结果，或还不能录入。已结束的比赛请先退回重传");
        }
        return match;
    }

    private void assertNoPending(Long matchId, Long eventTeamId) {
        BasketMatchSubmission latest = submissionMapper.selectLatestByMatchAndTeam(matchId, eventTeamId);
        if (latest != null && SubmissionStatusEnum.PENDING_REVIEW.getCode().equals(latest.getSubmissionStatus())) {
            throw new ServiceException("这场已有队长提交正在审核，请先审核或驳回后再由后台录入");
        }
    }

    private void insertTeam(BasketMatchSubmission submission, Long eventTeamId,
                            List<BasketAdminBoxScoreReq.PlayerRow> rows, int score, Date now) {
        BasketMatchSubmissionTeamStat stat = new BasketMatchSubmissionTeamStat();
        stat.setSubmissionId(submission.getId());
        stat.setMatchId(submission.getMatchId());
        stat.setDivisionId(submission.getDivisionId());
        stat.setEventTeamId(eventTeamId);
        stat.setScore(score);
        stat.setFieldGoalMade(sum(rows, BasketAdminBoxScoreReq.PlayerRow::getFieldGoalMade));
        stat.setFieldGoalAttempt(sum(rows, BasketAdminBoxScoreReq.PlayerRow::getFieldGoalAttempt));
        stat.setThreePointMade(sum(rows, BasketAdminBoxScoreReq.PlayerRow::getThreePointMade));
        stat.setThreePointAttempt(sum(rows, BasketAdminBoxScoreReq.PlayerRow::getThreePointAttempt));
        stat.setFreeThrowMade(sum(rows, BasketAdminBoxScoreReq.PlayerRow::getFreeThrowMade));
        stat.setFreeThrowAttempt(sum(rows, BasketAdminBoxScoreReq.PlayerRow::getFreeThrowAttempt));
        stat.setRebound(sum(rows, BasketAdminBoxScoreReq.PlayerRow::getRebound));
        stat.setAssist(sum(rows, BasketAdminBoxScoreReq.PlayerRow::getAssist));
        stat.setSteal(sum(rows, BasketAdminBoxScoreReq.PlayerRow::getSteal));
        stat.setBlock(sum(rows, BasketAdminBoxScoreReq.PlayerRow::getBlock));
        stat.setTurnover(sum(rows, BasketAdminBoxScoreReq.PlayerRow::getTurnover));
        stat.setFoul(sum(rows, BasketAdminBoxScoreReq.PlayerRow::getFoul));
        stat.setCreateTime(now);
        stat.setUpdateTime(now);
        stat.setIsDelete(NOT_DELETED);
        teamStatMapper.insert(stat);
    }

    private void insertPlayers(BasketMatchSubmission submission, Long eventTeamId,
                               List<BasketAdminBoxScoreReq.PlayerRow> rows, Date now) {
        for (BasketAdminBoxScoreReq.PlayerRow row : rows) {
            BasketMatchSubmissionPlayerStat stat = new BasketMatchSubmissionPlayerStat();
            stat.setSubmissionId(submission.getId());
            stat.setMatchId(submission.getMatchId());
            stat.setDivisionId(submission.getDivisionId());
            stat.setEventTeamId(eventTeamId);
            stat.setEventPlayerId(row.getEventPlayerId());
            stat.setPoints(nvl(row.getPoints()));
            stat.setRebound(nvl(row.getRebound()));
            stat.setAssist(nvl(row.getAssist()));
            stat.setSteal(nvl(row.getSteal()));
            stat.setBlock(nvl(row.getBlock()));
            stat.setTurnover(nvl(row.getTurnover()));
            stat.setFoul(nvl(row.getFoul()));
            stat.setFieldGoalMade(nvl(row.getFieldGoalMade()));
            stat.setFieldGoalAttempt(nvl(row.getFieldGoalAttempt()));
            stat.setThreePointMade(nvl(row.getThreePointMade()));
            stat.setThreePointAttempt(nvl(row.getThreePointAttempt()));
            stat.setFreeThrowMade(nvl(row.getFreeThrowMade()));
            stat.setFreeThrowAttempt(nvl(row.getFreeThrowAttempt()));
            stat.setCreateTime(now);
            stat.setUpdateTime(now);
            stat.setIsDelete(NOT_DELETED);
            playerStatMapper.insert(stat);
        }
    }

    private void insertPeriods(BasketMatchSubmission submission, List<BasketAdminBoxScoreReq.PeriodRow> periods, Date now) {
        for (BasketAdminBoxScoreReq.PeriodRow period : periods) {
            if (period == null || period.getPeriodNo() == null) {
                continue;
            }
            BasketMatchSubmissionPeriodScore row = new BasketMatchSubmissionPeriodScore();
            row.setSubmissionId(submission.getId());
            row.setMatchId(submission.getMatchId());
            row.setDivisionId(submission.getDivisionId());
            row.setPeriodNo(period.getPeriodNo());
            row.setHomeScore(nvl(period.getHomeScore()));
            row.setAwayScore(nvl(period.getAwayScore()));
            row.setCreateTime(now);
            row.setUpdateTime(now);
            row.setIsDelete(NOT_DELETED);
            periodScoreMapper.insert(row);
        }
    }

    private List<AdminMatchBoxScoreRosterVo.Player> rosterOf(Long eventTeamId) {
        List<BasketEventPlayer> players = activePlayers(eventTeamId);
        Set<Long> playerIds = new HashSet<>();
        collectSourcePlayerIds(players, playerIds);
        return toRoster(players, eventGameIdSnapshotSync.loadAccounts(playerIds));
    }

    private List<BasketEventPlayer> activePlayers(Long eventTeamId) {
        List<BasketEventPlayer> rows = new ArrayList<>();
        if (eventTeamId == null) {
            return rows;
        }
        for (BasketEventPlayer player : eventPlayerMapper.selectByEventTeamId(eventTeamId)) {
            if (player == null || player.getId() == null) {
                continue;
            }
            String status = player.getEventPlayerStatus();
            if (status != null && !status.isBlank() && !"ACTIVE".equalsIgnoreCase(status)) {
                continue;
            }
            rows.add(player);
        }
        return rows;
    }

    private void collectSourcePlayerIds(List<BasketEventPlayer> players, Set<Long> playerIds) {
        for (BasketEventPlayer player : players) {
            if (player.getSourcePlayerId() != null) {
                playerIds.add(player.getSourcePlayerId());
            }
        }
    }

    private List<AdminMatchBoxScoreRosterVo.Player> toRoster(List<BasketEventPlayer> players,
                                                             Map<Long, List<BasketPlayerPlatformAccount>> accounts) {
        List<AdminMatchBoxScoreRosterVo.Player> rows = new ArrayList<>();
        for (BasketEventPlayer player : players) {
            AdminMatchBoxScoreRosterVo.Player item = new AdminMatchBoxScoreRosterVo.Player();
            item.setEventPlayerId(player.getId());
            item.setPlayerName(player.getPlayerNameSnapshot());
            String current = CurrentEventGameId.resolve(
                    player.getPlatformIdentityId(),
                    player.getPlatformCode(),
                    player.getSourcePlayerId() == null ? null : accounts.get(player.getSourcePlayerId()));
            item.setGameId(StringUtils.hasText(current) ? current : player.getGameIdSnapshot());
            item.setJerseyNo(player.getJerseyNo());
            item.setIsCaptain(player.getIsCaptain());
            rows.add(item);
        }
        return rows;
    }

    private static List<PlayerRef> toRefs(List<AdminMatchBoxScoreRosterVo.Player> players) {
        List<PlayerRef> refs = new ArrayList<>();
        for (AdminMatchBoxScoreRosterVo.Player player : players) {
            refs.add(new PlayerRef(player.getEventPlayerId(), player.getPlayerName(), player.getGameId(), player.getJerseyNo()));
        }
        return refs;
    }

    private String teamName(Long eventTeamId) {
        if (eventTeamId == null) {
            return "";
        }
        BasketEventTeam team = eventTeamMapper.selectById(eventTeamId);
        if (team == null || team.getTeamNameSnapshot() == null) {
            return "";
        }
        return team.getTeamNameSnapshot();
    }

    private static void validateSide(String teamName, List<BasketAdminBoxScoreReq.PlayerRow> rows, Set<Long> rosterIds) {
        if (rows.isEmpty()) {
            throw new ServiceException(teamName + "至少需要一名球员数据");
        }
        Set<Long> seen = new HashSet<>();
        int index = 0;
        for (BasketAdminBoxScoreReq.PlayerRow row : rows) {
            index++;
            String prefix = teamName + " 第" + index + "行";
            if (row.getEventPlayerId() == null) {
                throw new ServiceException(prefix + "还没有选择球员");
            }
            if (!rosterIds.contains(row.getEventPlayerId())) {
                throw new ServiceException(prefix + "不在这支球队的赛事名单里");
            }
            if (!seen.add(row.getEventPlayerId())) {
                throw new ServiceException(prefix + "重复选择了同一名球员");
            }
            int pts = nvl(row.getPoints());
            int fgm = nvl(row.getFieldGoalMade());
            int fga = nvl(row.getFieldGoalAttempt());
            int tpm = nvl(row.getThreePointMade());
            int tpa = nvl(row.getThreePointAttempt());
            int ftm = nvl(row.getFreeThrowMade());
            int fta = nvl(row.getFreeThrowAttempt());
            assertNonNegative(prefix, pts, nvl(row.getRebound()), nvl(row.getAssist()), nvl(row.getSteal()),
                    nvl(row.getBlock()), nvl(row.getTurnover()), nvl(row.getFoul()), fgm, fga, tpm, tpa, ftm, fta);
            if (fgm > fga) {
                throw new ServiceException(prefix + "：FGM 大于 FGA");
            }
            if (tpm > tpa) {
                throw new ServiceException(prefix + "：3PM 大于 3PA");
            }
            if (ftm > fta) {
                throw new ServiceException(prefix + "：FTM 大于 FTA");
            }
            if (tpm > fgm) {
                throw new ServiceException(prefix + "：3PM 大于 FGM");
            }
            if (tpa > fga) {
                throw new ServiceException(prefix + "：3PA 大于 FGA");
            }
        }
    }

    private static void validatePeriods(List<BasketAdminBoxScoreReq.PeriodRow> periods, int homeScore, int awayScore) {
        if (periods == null || periods.isEmpty()) {
            throw new ServiceException("请填写节比分，四节合计要等于双方总分");
        }
        int home = 0;
        int away = 0;
        Set<Integer> seen = new HashSet<>();
        for (BasketAdminBoxScoreReq.PeriodRow period : periods) {
            if (period == null || period.getPeriodNo() == null) {
                continue;
            }
            if (period.getPeriodNo() < 1 || period.getPeriodNo() > 10 || !seen.add(period.getPeriodNo())) {
                throw new ServiceException("节次不正确或重复");
            }
            if (nvl(period.getHomeScore()) < 0 || nvl(period.getAwayScore()) < 0) {
                throw new ServiceException("节比分不能为负数");
            }
            home += nvl(period.getHomeScore());
            away += nvl(period.getAwayScore());
        }
        if (seen.isEmpty()) {
            throw new ServiceException("请填写节比分，四节合计要等于双方总分");
        }
        if (home != homeScore || away != awayScore) {
            throw new ServiceException("节比分合计和比赛比分不一致，请改节比分或上面的比分");
        }
    }

    private static List<BasketAdminBoxScoreReq.PlayerRow> usedRows(List<BasketAdminBoxScoreReq.PlayerRow> rows) {
        List<BasketAdminBoxScoreReq.PlayerRow> used = new ArrayList<>();
        if (rows == null) {
            return used;
        }
        for (BasketAdminBoxScoreReq.PlayerRow row : rows) {
            if (row == null) {
                continue;
            }
            if (row.getEventPlayerId() != null || hasStat(row)) {
                used.add(row);
            }
        }
        return used;
    }

    private static boolean hasStat(BasketAdminBoxScoreReq.PlayerRow row) {
        return nvl(row.getPoints()) + nvl(row.getRebound()) + nvl(row.getAssist()) + nvl(row.getSteal())
                + nvl(row.getBlock()) + nvl(row.getTurnover()) + nvl(row.getFoul())
                + nvl(row.getFieldGoalMade()) + nvl(row.getFieldGoalAttempt())
                + nvl(row.getThreePointMade()) + nvl(row.getThreePointAttempt())
                + nvl(row.getFreeThrowMade()) + nvl(row.getFreeThrowAttempt()) > 0;
    }

    private static Set<Long> idsOf(List<AdminMatchBoxScoreRosterVo.Player> players) {
        Set<Long> ids = new HashSet<>();
        for (AdminMatchBoxScoreRosterVo.Player player : players) {
            ids.add(player.getEventPlayerId());
        }
        return ids;
    }

    private static int sumPoints(List<BasketAdminBoxScoreReq.PlayerRow> rows) {
        return sum(rows, BasketAdminBoxScoreReq.PlayerRow::getPoints);
    }

    private static int sum(List<BasketAdminBoxScoreReq.PlayerRow> rows,
                           java.util.function.Function<BasketAdminBoxScoreReq.PlayerRow, Integer> getter) {
        int total = 0;
        for (BasketAdminBoxScoreReq.PlayerRow row : rows) {
            total += nvl(getter.apply(row));
        }
        return total;
    }

    private static void assertNonNegative(String prefix, int... values) {
        for (int value : values) {
            if (value < 0) {
                throw new ServiceException(prefix + "的数据不能为负数");
            }
        }
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private static boolean requiresPeriods(String gameMode) {
        if (gameMode == null || gameMode.isBlank()) {
            return true;
        }
        String code = gameMode.trim().toUpperCase();
        return !("THREE_V_THREE".equals(code) || "3V3".equals(code));
    }
}
