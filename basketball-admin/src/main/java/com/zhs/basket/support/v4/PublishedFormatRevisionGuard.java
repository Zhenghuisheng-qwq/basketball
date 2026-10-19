package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.dto.v4.StandardTemplateApplyReq;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.enums.v4.SnapshotStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionStageQualificationMapper;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketMatchSubmissionMapper;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchPeriodScoreMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchPlayerStatMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchTeamStatMapper;
import com.zhs.basket.vo.v4.FormatRevisionPreviewVo;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 已发布规则是否允许生成新版本。
 * 草稿编辑锁、名单锁、报名审核状态都不在这里放宽。
 */
@Component
public class PublishedFormatRevisionGuard {

    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private BasketV4MatchPeriodScoreMapper periodScoreMapper;
    @Autowired
    private BasketDivisionStageQualificationMapper qualificationMapper;
    @Autowired
    private BasketMatchSeriesMapper matchSeriesMapper;
    @Autowired
    private BasketScheduleBatchMapper scheduleBatchMapper;
    @Autowired
    private BasketV4MatchPlayerStatMapper playerStatMapper;
    @Autowired
    private BasketV4MatchTeamStatMapper teamStatMapper;
    @Autowired
    private BasketMatchSubmissionMapper submissionMapper;

    /**
     * 还没有正式开赛结果时，超级管理员可以退回赛程并改规则。
     * 草稿状态本身不在这里拦截。
     */
    public java.util.List<String> unstartedRewriteBlockers(BasketDivision division) {
        java.util.List<String> blockers = new java.util.ArrayList<>();
        if (division == null || division.getId() == null) {
            blockers.add("赛区不存在");
            return blockers;
        }
        String divisionStatus = division.getDivisionStatus();
        if (DivisionStatusEnum.PLAYING.getCode().equals(divisionStatus)
                || DivisionStatusEnum.FINISHED.getCode().equals(divisionStatus)
                || DivisionStatusEnum.ARCHIVED.getCode().equals(divisionStatus)) {
            blockers.add("赛区已进入比赛中、已结束或已归档，不能改写比赛历史");
        }
        Long divisionId = division.getId();
        int started = matchMapper.countStartedByDivisionId(divisionId);
        int irreversibleSeries = matchSeriesMapper.countIrreversibleByDivisionId(divisionId);
        int playerStats = playerStatMapper.countByDivisionId(divisionId);
        int teamStats = teamStatMapper.countByDivisionId(divisionId);
        int submissions = submissionMapper.countByDivisionId(divisionId);
        if (started > 0) {
            blockers.add("已有 " + started + " 场正式开赛、完赛或弃权比赛，不能改写比赛历史");
        }
        if (irreversibleSeries > 0) {
            blockers.add("已有系列赛进行中、完赛、弃权或人工结算，不能改写比赛历史");
        }
        if (playerStats > 0 || teamStats > 0) {
            blockers.add("已产生比赛技术统计，不能改写比赛历史");
        }
        if (submissions > 0) {
            blockers.add("已有比赛数据提交，不能改写比赛历史");
        }
        int scored = matchMapper.countScoredByDivisionId(divisionId);
        if (scored > 0) {
            blockers.add("已有 " + scored + " 场比赛写入比分，不能改写比赛历史");
        }
        int periodScores = periodScoreMapper.countByDivisionId(divisionId);
        if (periodScores > 0) {
            blockers.add("已有节比分，不能改写比赛历史");
        }
        int confirmedQualifications = qualificationMapper.countConfirmedByDivisionId(divisionId);
        if (confirmedQualifications > 0) {
            blockers.add("已有确认晋级记录，不能改写比赛历史");
        }
        return blockers;
    }

    public FormatRevisionPreviewVo assess(BasketDivision division, BasketDivisionFormatSnapshot snapshot) {
        return assess(division, snapshot, null, null);
    }

    public FormatRevisionPreviewVo assess(BasketDivision division,
                                          BasketDivisionFormatSnapshot snapshot,
                                          StandardTemplateApplyReq req,
                                          String nextFormatSummary) {
        FormatRevisionPreviewVo preview = new FormatRevisionPreviewVo();
        preview.setPreservedScope("报名记录、名单锁定和 EventPlayer 不会被修改或删除。");
        if (division == null || snapshot == null) {
            preview.getBlockers().add("赛区或当前规则快照不存在");
            preview.setAllowed(false);
            return preview;
        }
        preview.setCurrentTemplateCode(snapshot.getSourceTemplateCode());
        preview.setCurrentTemplateType(snapshot.getSourceTemplateType());
        if (req != null) {
            preview.setNextTemplateCode(req.getTemplateCode());
            preview.setNextFormatSummary(nextFormatSummary);
        }

        SnapshotStatusEnum snapshotStatus = SnapshotStatusEnum.fromCode(snapshot.getSnapshotStatus());
        if (snapshotStatus == null || !snapshotStatus.isLockedOrBeyond()) {
            preview.getBlockers().add("当前规则仍是草稿，请直接应用模板，不必重新发布");
        }
        if (snapshot.getIsCurrent() != null && snapshot.getIsCurrent() != 1) {
            preview.getBlockers().add("只能基于当前规则版本重新发布");
        }
        preview.getBlockers().addAll(unstartedRewriteBlockers(division));
        if (req != null && StringUtils.isNotEmpty(req.getRegistrationMode())
                && StringUtils.isNotEmpty(division.getRegistrationMode())
                && !req.getRegistrationMode().equals(division.getRegistrationMode())) {
            preview.getBlockers().add("重新发布不能修改报名方式");
        }
        if (req != null && StringUtils.isNotEmpty(req.getGameMode())
                && StringUtils.isNotEmpty(division.getGameMode())
                && !req.getGameMode().equals(division.getGameMode())) {
            preview.getBlockers().add("重新发布不能修改比赛模式");
        }

        Long divisionId = division.getId();
        int started = matchMapper.countStartedByDivisionId(divisionId);
        int matches = matchMapper.countByDivisionId(divisionId);
        int series = matchSeriesMapper.countByDivisionId(divisionId);
        int batches = scheduleBatchMapper.countByDivisionId(divisionId);
        preview.setUnstartedMatchCount(Math.max(matches - started, 0));
        preview.setMatchSeriesCount(series);
        preview.setScheduleBatchCount(batches);
        boolean invalidate = preview.getBlockers().isEmpty()
                && (preview.getUnstartedMatchCount() > 0 || series > 0 || batches > 0);
        preview.setScheduleWillBeInvalidated(invalidate);
        if (invalidate) {
            preview.setScheduleImpact("未开赛赛程将失效：软删除未开赛比赛 "
                    + preview.getUnstartedMatchCount() + " 场、系列赛 " + series
                    + " 条，并把 " + batches
                    + " 个赛程批次标为结构失效。需要按新规则重新生成赛程。已锁定的历史规则快照会保留。");
        } else if (preview.getBlockers().isEmpty()) {
            preview.setScheduleImpact("当前没有未开赛赛程，重新发布只生成新的规则版本。");
        }
        preview.setAllowed(preview.getBlockers().isEmpty());
        if (snapshotStatus != null && snapshotStatus.isLockedOrBeyond()) {
            preview.setCurrentRuleVersionLabel("已发布规则");
        }
        return preview;
    }
}
