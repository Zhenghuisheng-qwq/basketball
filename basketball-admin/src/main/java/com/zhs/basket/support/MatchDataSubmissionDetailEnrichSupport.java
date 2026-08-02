package com.zhs.basket.support;

import com.zhs.basket.domain.BasketFileAsset;
import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketMatchDataSubmission;
import com.zhs.basket.domain.BasketMatchDataSubmissionPeriod;
import com.zhs.basket.domain.BasketMatchDataSubmissionPlayerStat;
import com.zhs.basket.domain.BasketMatchDataSubmissionTeamStat;
import com.zhs.basket.domain.BasketTeam;
import com.zhs.basket.domain.BasketTeamSeasonProfile;
import com.zhs.basket.enums.MatchDataSubmissionStatusEnum;
import com.zhs.basket.enums.MatchStatusEnum;
import com.zhs.basket.mapper.BasketFileAssetMapper;
import com.zhs.basket.mapper.BasketMatchDataSubmissionMapper;
import com.zhs.basket.mapper.BasketMemberAccountMapper;
import com.zhs.basket.mapper.BasketTeamMapper;
import com.zhs.basket.mapper.BasketTeamSeasonProfileMapper;
import com.zhs.basket.vo.AccountPlayerDetailVo;
import com.zhs.basket.vo.MatchDataSubmissionComparisonDiffVo;
import com.zhs.basket.vo.MatchDataSubmissionComparisonSummaryVo;
import com.zhs.basket.vo.MatchDataSubmissionDetailVo;
import com.zhs.basket.vo.MatchDataSubmissionMaterialVo;
import com.zhs.basket.vo.MatchDataSubmissionPeriodVo;
import com.zhs.basket.vo.MatchDataSubmissionPermissionsVo;
import com.zhs.basket.vo.MatchDataSubmissionPlayerStatVo;
import com.zhs.basket.vo.MatchDataSubmissionScoreVo;
import com.zhs.basket.vo.MatchDataSubmissionSubmitterVo;
import com.zhs.basket.vo.MatchDataSubmissionTeamSideVo;
import com.zhs.basket.vo.MatchDataSubmissionTeamStatVo;
import com.zhs.basket.vo.MatchDataSubmissionVersionVo;
import com.zhs.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class MatchDataSubmissionDetailEnrichSupport {

    private static final Logger log = LoggerFactory.getLogger(MatchDataSubmissionDetailEnrichSupport.class);
    private static final String SIDE_HOME = "HOME";
    private static final String SIDE_AWAY = "AWAY";
    private static final String NOT_SUBMITTED = "NOT_SUBMITTED";

    @Autowired private BasketTeamSeasonProfileMapper eventTeamMapper;
    @Autowired private BasketTeamMapper teamMapper;
    @Autowired private BasketFileAssetMapper fileAssetMapper;
    @Autowired private BasketMemberAccountMapper memberAccountMapper;
    @Autowired private BasketMatchDataSubmissionMapper submissionMapper;

    public void enrich(MatchDataSubmissionDetailVo vo, BasketMatchDataSubmission submission, BasketMatch match) {
        if (vo == null || submission == null || match == null) {
            return;
        }
        vo.setRoundNo(match.getRoundNo());
        vo.setRoundName(match.getRoundNo() == null ? null : "第" + match.getRoundNo() + "轮");
        vo.setMatchTime(match.getScheduledStartTime());
        vo.setMatchStatus(match.getMatchStatus());
        vo.setMatchStatusText(safeMatchStatusText(match.getMatchStatus()));

        String submitSide = resolveSubmitSide(submission);
        vo.setSubmitSide(submitSide);
        vo.setSubmitSideText(SIDE_HOME.equals(submitSide) ? "主队" : SIDE_AWAY.equals(submitSide) ? "客队" : "未知");
        vo.setSubmitSideAbnormal(!SIDE_HOME.equals(submitSide) && !SIDE_AWAY.equals(submitSide));
        vo.setSubmitEventTeamId(SIDE_HOME.equals(submitSide) ? match.getHomeEventTeamId() : match.getAwayEventTeamId());
        vo.setSubmitterRoleText(resolveSubmitterRoleText(submission.getSubmitterRole()));
        vo.setAuditStatusText(resolveAuditStatusText(submission.getAuditStatus()));

        Map<Long, String> logoUrlMap = loadLogoUrlMap(match);
        MatchDataSubmissionTeamSideVo homeTeam = buildTeamSide(match.getHomeEventTeamId(), submission.getHomeTeamId(),
                resolveTeamName(match.getHomeTeamNameSnapshot(), vo.getHomeTeamName()), SIDE_HOME, "主队", logoUrlMap.get(match.getHomeEventTeamId()));
        MatchDataSubmissionTeamSideVo awayTeam = buildTeamSide(match.getAwayEventTeamId(), submission.getAwayTeamId(),
                resolveTeamName(match.getAwayTeamNameSnapshot(), vo.getAwayTeamName()), SIDE_AWAY, "客队", logoUrlMap.get(match.getAwayEventTeamId()));
        fillSubmissionSideStatus(homeTeam, awayTeam, submission, match);
        homeTeam.setCurrentSubmissionSide(SIDE_HOME.equals(submitSide));
        awayTeam.setCurrentSubmissionSide(SIDE_AWAY.equals(submitSide));
        vo.setHomeTeam(homeTeam);
        vo.setAwayTeam(awayTeam);

        vo.setSubmitter(buildSubmitter(submission, vo.getSubmitterTeamName(), vo.getSubmitSideText()));
        vo.setSubmittedScore(buildSubmittedScore(submission));
        vo.setOfficialScore(buildOfficialScore(match));
        vo.setComparisonResultText(buildComparisonResultText(homeTeam, awayTeam));

        enrichPeriodRows(vo.getPeriods());
        enrichTeamStatRows(vo.getTeamStats(), homeTeam, awayTeam);
        enrichPlayerStatRows(vo.getPlayerStats());
        enrichMaterialRows(vo.getMaterials(), vo.getSubmitter());
        enrichVersionRows(vo.getVersions(), submission.getId());

        vo.setPermissions(buildPermissions(vo.getCanAudit()));
    }

    public MatchDataSubmissionComparisonSummaryVo buildComparison(BasketMatchDataSubmission submission, BasketMatch match) {
        MatchDataSubmissionComparisonSummaryVo summary = new MatchDataSubmissionComparisonSummaryVo();
        summary.setDiffs(new ArrayList<>());
        Long opponentTeamId = Objects.equals(submission.getSubmitterTeamId(), submission.getHomeTeamId())
                ? submission.getAwayTeamId() : submission.getHomeTeamId();
        if (opponentTeamId == null) {
            summary.setOpponentSubmitted(false);
            summary.setSummaryText("无法识别对手球队");
            summary.setDiffCount(0);
            return summary;
        }
        BasketMatchDataSubmission opponent = submissionMapper.selectActiveByMatchIdAndSubmitterTeamId(
                submission.getMatchId(), opponentTeamId);
        if (opponent == null || Objects.equals(opponent.getId(), submission.getId())) {
            summary.setOpponentSubmitted(false);
            summary.setSummaryText("对方尚未提交比赛数据，暂无法进行双方核对");
            summary.setDiffCount(0);
            return summary;
        }
        summary.setOpponentSubmitted(true);
        List<MatchDataSubmissionComparisonDiffVo> diffs = new ArrayList<>();
        if (!Objects.equals(submission.getHomeScore(), opponent.getHomeScore())
                || !Objects.equals(submission.getAwayScore(), opponent.getAwayScore())) {
            MatchDataSubmissionComparisonDiffVo diff = new MatchDataSubmissionComparisonDiffVo();
            diff.setField("totalScore");
            diff.setFieldLabel("总比分");
            diff.setSelfValue(submission.getHomeScore() + " : " + submission.getAwayScore());
            diff.setOpponentValue(opponent.getHomeScore() + " : " + opponent.getAwayScore());
            diffs.add(diff);
        }
        summary.setDiffs(diffs);
        summary.setDiffCount(diffs.size());
        summary.setSummaryText(diffs.isEmpty() ? "双方提交比分一致" : "比分不一致");
        return summary;
    }

    private MatchDataSubmissionPermissionsVo buildPermissions(Boolean canAudit) {
        MatchDataSubmissionPermissionsVo permissions = new MatchDataSubmissionPermissionsVo();
        boolean allowed = Boolean.TRUE.equals(canAudit);
        permissions.setCanApprove(allowed);
        permissions.setCanReject(allowed);
        permissions.setCanViewMaterial(true);
        return permissions;
    }

    private Map<Long, String> loadLogoUrlMap(BasketMatch match) {
        Set<Long> fileIds = new HashSet<>();
        Map<Long, Long> eventTeamLogoFileId = new HashMap<>();
        if (match.getHomeEventTeamId() != null) {
            BasketTeamSeasonProfile profile = eventTeamMapper.selectById(match.getHomeEventTeamId());
            if (profile != null && profile.getLogoFileId() != null) {
                fileIds.add(profile.getLogoFileId());
                eventTeamLogoFileId.put(match.getHomeEventTeamId(), profile.getLogoFileId());
            }
        }
        if (match.getAwayEventTeamId() != null) {
            BasketTeamSeasonProfile profile = eventTeamMapper.selectById(match.getAwayEventTeamId());
            if (profile != null && profile.getLogoFileId() != null) {
                fileIds.add(profile.getLogoFileId());
                eventTeamLogoFileId.put(match.getAwayEventTeamId(), profile.getLogoFileId());
            }
        }
        if (match.getHomeTeamId() != null) {
            BasketTeam team = teamMapper.selectById(match.getHomeTeamId());
            if (team != null && team.getLogoFileId() != null) {
                fileIds.add(team.getLogoFileId());
            }
        }
        if (match.getAwayTeamId() != null) {
            BasketTeam team = teamMapper.selectById(match.getAwayTeamId());
            if (team != null && team.getLogoFileId() != null) {
                fileIds.add(team.getLogoFileId());
            }
        }
        Map<Long, String> fileUrlMap = new HashMap<>();
        for (Long fileId : fileIds) {
            BasketFileAsset asset = fileAssetMapper.selectById(fileId);
            if (asset != null && StringUtils.isNotEmpty(asset.getAccessUrl())) {
                fileUrlMap.put(fileId, asset.getAccessUrl());
            }
        }
        Map<Long, String> result = new HashMap<>();
        if (StringUtils.isNotEmpty(match.getHomeTeamLogoSnapshot())) {
            result.put(match.getHomeEventTeamId(), match.getHomeTeamLogoSnapshot());
        } else {
            Long fileId = eventTeamLogoFileId.get(match.getHomeEventTeamId());
            if (fileId != null && fileUrlMap.containsKey(fileId)) {
                result.put(match.getHomeEventTeamId(), fileUrlMap.get(fileId));
            } else if (match.getHomeTeamId() != null) {
                BasketTeam team = teamMapper.selectById(match.getHomeTeamId());
                if (team != null && team.getLogoFileId() != null) {
                    result.put(match.getHomeEventTeamId(), fileUrlMap.get(team.getLogoFileId()));
                }
            }
        }
        if (StringUtils.isNotEmpty(match.getAwayTeamLogoSnapshot())) {
            result.put(match.getAwayEventTeamId(), match.getAwayTeamLogoSnapshot());
        } else {
            Long fileId = eventTeamLogoFileId.get(match.getAwayEventTeamId());
            if (fileId != null && fileUrlMap.containsKey(fileId)) {
                result.put(match.getAwayEventTeamId(), fileUrlMap.get(fileId));
            } else if (match.getAwayTeamId() != null) {
                BasketTeam team = teamMapper.selectById(match.getAwayTeamId());
                if (team != null && team.getLogoFileId() != null) {
                    result.put(match.getAwayEventTeamId(), fileUrlMap.get(team.getLogoFileId()));
                }
            }
        }
        return result;
    }

    private MatchDataSubmissionTeamSideVo buildTeamSide(Long eventTeamId, Long teamId, String teamName,
            String sideType, String sideText, String logoUrl) {
        MatchDataSubmissionTeamSideVo side = new MatchDataSubmissionTeamSideVo();
        side.setEventTeamId(eventTeamId);
        side.setTeamId(teamId);
        side.setTeamName(teamName);
        side.setLogoUrl(logoUrl);
        side.setSideType(sideType);
        side.setSideText(sideText);
        if (eventTeamId != null) {
            BasketTeamSeasonProfile profile = eventTeamMapper.selectById(eventTeamId);
            if (profile != null) {
                side.setTeamCode(profile.getEventTeamNo());
            }
        }
        return side;
    }

    private void fillSubmissionSideStatus(MatchDataSubmissionTeamSideVo homeTeam, MatchDataSubmissionTeamSideVo awayTeam,
            BasketMatchDataSubmission current, BasketMatch match) {
        fillSideSubmissionStatus(homeTeam, match.getHomeTeamId(), current);
        fillSideSubmissionStatus(awayTeam, match.getAwayTeamId(), current);
    }

    private void fillSideSubmissionStatus(MatchDataSubmissionTeamSideVo side, Long teamId,
            BasketMatchDataSubmission current) {
        if (side == null || teamId == null) {
            return;
        }
        BasketMatchDataSubmission latest = submissionMapper.selectActiveByMatchIdAndSubmitterTeamId(current.getMatchId(), teamId);
        if (latest == null) {
            side.setSubmissionStatus(NOT_SUBMITTED);
            side.setSubmissionStatusText("未提交");
            return;
        }
        side.setSubmissionId(latest.getId());
        side.setSubmissionVersion(latest.getVersionNo());
        side.setHomeScore(latest.getHomeScore());
        side.setAwayScore(latest.getAwayScore());
        side.setSubmissionStatus(String.valueOf(latest.getSubmissionStatus()));
        side.setSubmissionStatusText(MatchDataSubmissionStatusEnum.fromCode(latest.getSubmissionStatus()).getDescription());
    }

    private MatchDataSubmissionSubmitterVo buildSubmitter(BasketMatchDataSubmission submission, String teamName, String sideText) {
        MatchDataSubmissionSubmitterVo submitter = new MatchDataSubmissionSubmitterVo();
        submitter.setTeamName(teamName);
        submitter.setRole(submission.getSubmitterRole());
        submitter.setRoleText(resolveSubmitterRoleText(submission.getSubmitterRole()));
        if (submission.getSubmitterMemberId() == null) {
            submitter.setMissing(true);
            submitter.setDisplayName("提交人信息缺失");
            log.warn("submission {} missing submitterMemberId", submission.getId());
            return submitter;
        }
        AccountPlayerDetailVo account = memberAccountMapper.selectDetail(submission.getSubmitterMemberId());
        if (account == null) {
            submitter.setMissing(true);
            submitter.setDisplayName("提交人账号已不存在");
            submitter.setUserId(submission.getSubmitterMemberId());
            log.warn("submission {} submitter account missing, memberId={}", submission.getId(), submission.getSubmitterMemberId());
            return submitter;
        }
        submitter.setMissing(false);
        submitter.setUserId(account.getMemberId());
        submitter.setUserName(account.getUsername());
        submitter.setNickName(account.getNickname());
        submitter.setAvatarUrl(account.getAvatarUrl());
        submitter.setDisplayEmail(BasketDataMaskUtil.maskEmail(account.getEmail()));
        submitter.setPlayerId(account.getPlayerId());
        submitter.setPlayerCode(account.getPlayerNo());
        submitter.setPlayerName(StringUtils.isNotEmpty(account.getGameNickname()) ? account.getGameNickname() : account.getNickname());
        submitter.setGameId(account.getGameAccount());
        submitter.setDisplayName(resolveDisplayName(account));
        if (submission.getSubmitterPlayerId() != null && !Objects.equals(submission.getSubmitterPlayerId(), account.getPlayerId())) {
            submitter.setPlayerId(submission.getSubmitterPlayerId());
        }
        if (StringUtils.isNotEmpty(sideText) && StringUtils.isNotEmpty(teamName)) {
            submitter.setRoleText(submitter.getRoleText() + " · " + teamName + "（" + sideText + "）");
        }
        return submitter;
    }

    private String resolveDisplayName(AccountPlayerDetailVo account) {
        if (StringUtils.isNotEmpty(account.getGameNickname())) {
            return account.getGameNickname();
        }
        if (StringUtils.isNotEmpty(account.getNickname())) {
            return account.getNickname();
        }
        if (StringUtils.isNotEmpty(account.getEmail())) {
            return BasketDataMaskUtil.maskEmail(account.getEmail());
        }
        if (StringUtils.isNotEmpty(account.getAccountNo())) {
            return account.getAccountNo();
        }
        return "用户" + account.getMemberId();
    }

    private MatchDataSubmissionScoreVo buildSubmittedScore(BasketMatchDataSubmission submission) {
        MatchDataSubmissionScoreVo score = new MatchDataSubmissionScoreVo();
        score.setHomeScore(submission.getHomeScore());
        score.setAwayScore(submission.getAwayScore());
        score.setConfirmed(false);
        return score;
    }

    private MatchDataSubmissionScoreVo buildOfficialScore(BasketMatch match) {
        MatchDataSubmissionScoreVo score = new MatchDataSubmissionScoreVo();
        boolean confirmed = match.getScoreConfirmed() != null && match.getScoreConfirmed() == 1;
        score.setConfirmed(confirmed);
        score.setHomeScore(confirmed ? match.getHomeScore() : null);
        score.setAwayScore(confirmed ? match.getAwayScore() : null);
        return score;
    }

    private String buildComparisonResultText(MatchDataSubmissionTeamSideVo homeTeam, MatchDataSubmissionTeamSideVo awayTeam) {
        boolean homeSubmitted = homeTeam != null && !NOT_SUBMITTED.equals(homeTeam.getSubmissionStatus());
        boolean awaySubmitted = awayTeam != null && !NOT_SUBMITTED.equals(awayTeam.getSubmissionStatus());
        if (!homeSubmitted || !awaySubmitted) {
            return "等待对方提交";
        }
        if (Objects.equals(homeTeam.getHomeScore(), awayTeam.getHomeScore())
                && Objects.equals(homeTeam.getAwayScore(), awayTeam.getAwayScore())) {
            return "双方比分一致";
        }
        return "比分不一致";
    }

    private void enrichPeriodRows(List<MatchDataSubmissionPeriodVo> periods) {
        if (CollectionUtils.isEmpty(periods)) {
            return;
        }
        for (MatchDataSubmissionPeriodVo period : periods) {
            int home = period.getHomeScore() == null ? 0 : period.getHomeScore();
            int away = period.getAwayScore() == null ? 0 : period.getAwayScore();
            period.setScoreDiff(home - away);
            period.setValidationStatus(period.getHomeScore() == null || period.getAwayScore() == null ? "异常" : "正常");
        }
    }

    private void enrichTeamStatRows(List<MatchDataSubmissionTeamStatVo> teamStats,
            MatchDataSubmissionTeamSideVo homeTeam, MatchDataSubmissionTeamSideVo awayTeam) {
        if (CollectionUtils.isEmpty(teamStats)) {
            return;
        }
        for (MatchDataSubmissionTeamStatVo stat : teamStats) {
            if (homeTeam != null && Objects.equals(stat.getTeamId(), homeTeam.getTeamId())) {
                stat.setLogoUrl(homeTeam.getLogoUrl());
                stat.setSideText(homeTeam.getSideText());
                stat.setEventTeamId(homeTeam.getEventTeamId());
                if (StringUtils.isEmpty(stat.getTeamName())) {
                    stat.setTeamName(homeTeam.getTeamName());
                }
            } else if (awayTeam != null && Objects.equals(stat.getTeamId(), awayTeam.getTeamId())) {
                stat.setLogoUrl(awayTeam.getLogoUrl());
                stat.setSideText(awayTeam.getSideText());
                stat.setEventTeamId(awayTeam.getEventTeamId());
                if (StringUtils.isEmpty(stat.getTeamName())) {
                    stat.setTeamName(awayTeam.getTeamName());
                }
            }
        }
    }

    private void enrichPlayerStatRows(List<MatchDataSubmissionPlayerStatVo> playerStats) {
        if (CollectionUtils.isEmpty(playerStats)) {
            return;
        }
        for (MatchDataSubmissionPlayerStatVo stat : playerStats) {
            if (StringUtils.isEmpty(stat.getPlayerName())) {
                stat.setPlayerName("未知球员");
            }
            if (StringUtils.isEmpty(stat.getValidationStatus())) {
                stat.setValidationStatus("待核对");
            }
        }
    }

    private void enrichMaterialRows(List<MatchDataSubmissionMaterialVo> materials, MatchDataSubmissionSubmitterVo submitter) {
        if (CollectionUtils.isEmpty(materials)) {
            return;
        }
        for (MatchDataSubmissionMaterialVo material : materials) {
            material.setMaterialTypeName(resolveMaterialTypeName(material.getMaterialType()));
            if (submitter != null) {
                material.setUploaderName(submitter.getDisplayName());
            }
            if (material.getUploadTime() == null) {
                material.setUploadTime(null);
            }
        }
    }

    private void enrichVersionRows(List<MatchDataSubmissionVersionVo> versions, Long currentSubmissionId) {
        if (CollectionUtils.isEmpty(versions)) {
            return;
        }
        for (MatchDataSubmissionVersionVo version : versions) {
            version.setSubmissionStatusName(MatchDataSubmissionStatusEnum.fromCode(version.getSubmissionStatus()).getDescription());
            version.setIsCurrent(Objects.equals(version.getSubmissionId(), currentSubmissionId));
        }
    }

    private String resolveSubmitSide(BasketMatchDataSubmission submission) {
        if (Objects.equals(submission.getSubmitterTeamId(), submission.getHomeTeamId())) {
            return SIDE_HOME;
        }
        if (Objects.equals(submission.getSubmitterTeamId(), submission.getAwayTeamId())) {
            return SIDE_AWAY;
        }
        return "UNKNOWN";
    }

    private String resolveSubmitterRoleText(String role) {
        if (!StringUtils.hasText(role)) {
            return "提交人";
        }
        return switch (role.trim().toUpperCase()) {
            case "CAPTAIN" -> "赛事队长";
            case "ADMIN" -> "管理员代提交";
            default -> role;
        };
    }

    private String resolveAuditStatusText(String auditStatus) {
        if (!StringUtils.hasText(auditStatus)) {
            return "-";
        }
        return switch (auditStatus.trim().toUpperCase()) {
            case "PENDING" -> "审核中";
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已驳回";
            default -> auditStatus;
        };
    }

    private String safeMatchStatusText(Integer matchStatus) {
        if (matchStatus == null) {
            return "-";
        }
        try {
            return MatchStatusEnum.fromCode(matchStatus).getDescription();
        } catch (Exception ex) {
            return String.valueOf(matchStatus);
        }
    }

    private String resolveTeamName(String snapshot, String fallback) {
        return StringUtils.isNotEmpty(snapshot) ? snapshot : fallback;
    }

    private String resolveMaterialTypeName(String materialType) {
        if (!StringUtils.hasText(materialType)) {
            return "其他材料";
        }
        return switch (materialType.trim().toUpperCase()) {
            case "RESULT_SCREENSHOT" -> "比赛结果截图";
            case "DISPUTE_EVIDENCE" -> "争议证据";
            case "EXCEPTION_EVIDENCE" -> "异常说明材料";
            default -> materialType;
        };
    }
}
