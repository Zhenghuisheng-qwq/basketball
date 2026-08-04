package com.zhs.service.impl.v4;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.enums.v4.MatchPublishStatusEnum;
import com.zhs.enums.v4.MatchStatusEnum;
import com.zhs.enums.v4.SubmissionStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.mapper.v4.BasketV4PlayerMatchSubmissionMapper;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.response.v4.V4MySchedulePageResp;
import com.zhs.response.v4.V4MyScheduleResp;
import com.zhs.response.v4.V4MyScheduleSummaryResp;
import com.zhs.service.v4.IBasketV4PlayerMyScheduleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Service
public class BasketV4PlayerMyScheduleServiceImpl implements IBasketV4PlayerMyScheduleService {

    private static final Integer NOT_DELETED = 0;

    @Resource
    private BasketV4PlayerMatchSubmissionMapper matchSubmissionMapper;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;

    @Override
    public V4MySchedulePageResp listMySchedule(Long seasonId, Long divisionId, String status,
                                             Boolean includeFinished, Boolean onlyPendingSubmit) {
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile player = requirePlayerByMemberId(memberId);

        List<V4MyScheduleResp> allRows = matchSubmissionMapper.selectMySchedule(
                memberId, player.getId(), seasonId, divisionId, status, true, onlyPendingSubmit);
        for (V4MyScheduleResp row : allRows) {
            enrichDisplayFields(row);
            enrichActionFields(row);
        }

        V4MyScheduleSummaryResp summary = buildSummary(allRows);
        List<V4MyScheduleResp> items = filterAndSortItems(allRows, includeFinished);
        V4MySchedulePageResp page = new V4MySchedulePageResp();
        page.setSummary(summary);
        page.setItems(items);
        return page;
    }

    private List<V4MyScheduleResp> filterAndSortItems(List<V4MyScheduleResp> allRows, Boolean includeFinished) {
        boolean showFinished = Boolean.TRUE.equals(includeFinished);
        List<V4MyScheduleResp> filtered = new ArrayList<>();
        for (V4MyScheduleResp row : allRows) {
            if (!showFinished && MatchStatusEnum.FINISHED.getCode().equals(row.getMatchStatus())) {
                continue;
            }
            filtered.add(row);
        }
        if (showFinished) {
            filtered.sort((a, b) -> {
                int groupA = isUpcoming(a) ? 0 : 1;
                int groupB = isUpcoming(b) ? 0 : 1;
                if (groupA != groupB) {
                    return Integer.compare(groupA, groupB);
                }
                Date timeA = a.getMatchTime();
                Date timeB = b.getMatchTime();
                if (timeA == null && timeB == null) {
                    return Long.compare(b.getMatchId(), a.getMatchId());
                }
                if (timeA == null) {
                    return 1;
                }
                if (timeB == null) {
                    return -1;
                }
                int timeCompare = groupA == 0 ? timeA.compareTo(timeB) : timeB.compareTo(timeA);
                if (timeCompare != 0) {
                    return timeCompare;
                }
                return Long.compare(b.getMatchId(), a.getMatchId());
            });
        } else {
            filtered.sort(Comparator
                    .comparing(V4MyScheduleResp::getMatchTime, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(V4MyScheduleResp::getMatchId));
        }
        return filtered;
    }

    private V4MyScheduleSummaryResp buildSummary(List<V4MyScheduleResp> rows) {
        V4MyScheduleSummaryResp summary = new V4MyScheduleSummaryResp();
        int upcoming = 0;
        int pendingSubmit = 0;
        int reviewing = 0;
        int finished = 0;
        for (V4MyScheduleResp row : rows) {
            if (MatchStatusEnum.FINISHED.getCode().equals(row.getMatchStatus())) {
                finished++;
            }
            if (isUpcoming(row)) {
                upcoming++;
            }
            String myStatus = row.getMySubmissionStatus();
            if (MatchStatusEnum.FINISHED.getCode().equals(row.getMatchStatus())
                    && (myStatus == null || SubmissionStatusEnum.DRAFT.getCode().equals(myStatus))) {
                pendingSubmit++;
            }
            if (SubmissionStatusEnum.PENDING_REVIEW.getCode().equals(myStatus)) {
                reviewing++;
            }
        }
        summary.setUpcomingCount(upcoming);
        summary.setPendingSubmitCount(pendingSubmit);
        summary.setReviewingCount(reviewing);
        summary.setFinishedCount(finished);
        summary.setTotalCount(rows.size());
        return summary;
    }

    private void enrichDisplayFields(V4MyScheduleResp row) {
        row.setStageName(resolveStageName(row.getStageCode()));
        row.setRoundName(resolveRoundName(row));
        row.setDivisionCode(row.getPlatformCode());
    }

    private String resolveRoundName(V4MyScheduleResp row) {
        if (row.getRoundNo() == null || row.getRoundNo() <= 0) {
            return null;
        }
        String stage = row.getStageCode() != null ? row.getStageCode().toUpperCase() : "";
        if ("KNOCKOUT".equals(stage) || "PLAYOFF".equals(stage)) {
            int teamsInRound = 1 << row.getRoundNo();
            if (teamsInRound >= 8) {
                return switch (teamsInRound) {
                    case 8 -> "八强赛";
                    case 4 -> "半决赛";
                    case 2 -> "决赛";
                    default -> "淘汰赛第 " + row.getRoundNo() + " 轮";
                };
            }
            return "淘汰赛第 " + row.getRoundNo() + " 轮";
        }
        if ("GROUP".equals(stage)) {
            return "小组赛第 " + row.getRoundNo() + " 轮";
        }
        return "循环赛第 " + row.getRoundNo() + " 轮";
    }

    private void enrichActionFields(V4MyScheduleResp row) {
        row.setCanSubmit(false);
        row.setCanSubmitData(false);
        row.setCanEditSubmission(false);
        row.setCanViewSubmission(false);
        row.setUserActionType("VIEW_DETAIL");
        row.setUserActionText("查看比赛详情");
        row.setCannotSubmitReason(null);

        if (row.getMyEventTeamId() == null) {
            row.setCannotSubmitReason("不属于本场比赛球队");
            return;
        }
        if (!MatchPublishStatusEnum.PUBLISHED.getCode().equals(row.getPublishStatus())) {
            row.setCannotSubmitReason("比赛未发布");
            return;
        }
        if (MatchStatusEnum.CANCELLED.getCode().equals(row.getMatchStatus())) {
            row.setUserActionType("NONE");
            row.setUserActionText("比赛已取消");
            row.setCannotSubmitReason("比赛已取消");
            return;
        }

        String myStatus = row.getMySubmissionStatus();
        boolean hasSubmission = myStatus != null;
        boolean matchEnded = MatchStatusEnum.FINISHED.getCode().equals(row.getMatchStatus())
                || MatchStatusEnum.FORFEITED.getCode().equals(row.getMatchStatus());
        boolean matchPlaying = MatchStatusEnum.PLAYING.getCode().equals(row.getMatchStatus());

        if (SubmissionStatusEnum.PENDING_REVIEW.getCode().equals(myStatus)) {
            row.setCanViewSubmission(true);
            row.setUserActionType("VIEW_SUBMISSION");
            row.setUserActionText("查看提交详情");
            row.setCannotSubmitReason("本方审核中");
            return;
        }
        if (SubmissionStatusEnum.APPROVED.getCode().equals(myStatus)) {
            row.setCanViewSubmission(true);
            row.setUserActionType("VIEW_SUBMISSION");
            row.setUserActionText("查看审核结果");
            row.setCannotSubmitReason("本方已通过");
            return;
        }
        if (SubmissionStatusEnum.REJECTED.getCode().equals(myStatus)) {
            row.setCanSubmit(true);
            row.setCanSubmitData(true);
            row.setCanEditSubmission(true);
            row.setCanViewSubmission(true);
            row.setUserActionType("RESUBMIT");
            row.setUserActionText("修改并重新提交");
            return;
        }

        if (matchPlaying) {
            row.setUserActionType("VIEW_DETAIL");
            row.setUserActionText("查看比赛详情");
            row.setCannotSubmitReason("比赛进行中，结束后提交数据");
            return;
        }

        if (!matchEnded) {
            row.setUserActionType("VIEW_DETAIL");
            row.setUserActionText("查看比赛详情");
            row.setCannotSubmitReason("比赛尚未结束");
            return;
        }

        if (!hasSubmission || SubmissionStatusEnum.DRAFT.getCode().equals(myStatus)) {
            row.setCanSubmit(true);
            row.setCanSubmitData(true);
            row.setUserActionType("SUBMIT");
            row.setUserActionText("提交比赛数据");
            return;
        }

        row.setCanViewSubmission(hasSubmission);
        row.setUserActionType("VIEW_SUBMISSION");
        row.setUserActionText("查看提交详情");
    }

    private boolean isUpcoming(V4MyScheduleResp row) {
        if (row == null) {
            return false;
        }
        String status = row.getMatchStatus();
        return !MatchStatusEnum.FINISHED.getCode().equals(status)
                && !MatchStatusEnum.CANCELLED.getCode().equals(status);
    }

    private String resolveStageName(String stageCode) {
        if (stageCode == null || stageCode.isEmpty()) {
            return "循环赛";
        }
        return switch (stageCode.toUpperCase()) {
            case "REGULAR" -> "循环赛";
            case "GROUP" -> "小组赛";
            case "KNOCKOUT", "PLAYOFF" -> "淘汰赛";
            case "SEMI_FINAL" -> "半决赛";
            case "FINAL" -> "决赛";
            default -> stageCode;
        };
    }

    private BasketPlayerProfile requirePlayerByMemberId(Long memberId) {
        BasketPlayerProfile profile = playerProfileDao.selectOne(new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getMemberId, memberId)
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (profile == null) {
            throw new ServiceException("当前账号未关联认证球员");
        }
        return profile;
    }
}
