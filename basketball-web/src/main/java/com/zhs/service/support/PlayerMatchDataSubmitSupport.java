package com.zhs.service.support;

import com.zhs.enums.MatchDataSubmissionStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketMatch;
import com.zhs.model.BasketMatchDataSubmission;
import com.zhs.model.BasketMatchExceptionApplication;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Objects;

/**
 * 球员端比赛数据提交开放条件（基于计划开赛时间，不要求后台手动结束比赛）。
 */
@Component
public class PlayerMatchDataSubmitSupport {

    private static final int MATCH_CANCELLED = 5;
    private static final int MATCH_POSTPONED = 4;

    @Resource
    private PlayerMatchDisputeExceptionSupport disputeExceptionSupport;

    public boolean isMatchTimePassed(BasketMatch match) {
        if (match == null || match.getScheduledStartTime() == null) {
            return false;
        }
        return !match.getScheduledStartTime().after(new Date());
    }

    public boolean isMatchCancelled(BasketMatch match) {
        return match != null && match.getMatchStatus() != null && match.getMatchStatus() == MATCH_CANCELLED;
    }

    public boolean isMatchPostponed(BasketMatch match) {
        return match != null && match.getMatchStatus() != null && match.getMatchStatus() == MATCH_POSTPONED;
    }

    public boolean shouldDisplayAsFinished(BasketMatch match) {
        if (match == null || isMatchCancelled(match) || isMatchPostponed(match)) {
            return false;
        }
        Integer status = match.getMatchStatus();
        if (status != null && status == BasketPublicConstants.MATCH_FINISHED) {
            return true;
        }
        return status != null
                && status == BasketPublicConstants.MATCH_SCHEDULED
                && isMatchTimePassed(match);
    }

    public boolean isExceptionLocked(BasketMatch match) {
        if (match == null || match.getId() == null) {
            return false;
        }
        BasketMatchExceptionApplication active = disputeExceptionSupport.findActiveException(match.getId());
        return active != null;
    }

    public MatchDataSubmitAvailability evaluate(BasketMatch match, boolean isCaptain, Long myTeamId,
                                                  BasketMatchDataSubmission submission) {
        MatchDataSubmitAvailability availability = new MatchDataSubmitAvailability();
        availability.setCaptain(isCaptain);
        availability.setMatchTimePassed(isMatchTimePassed(match));

        boolean cancelled = isMatchCancelled(match);
        boolean postponed = isMatchPostponed(match);
        boolean exceptionLocked = isExceptionLocked(match);
        boolean activeSubmission = submission != null
                && MatchDataSubmissionStatusEnum.fromCode(submission.getSubmissionStatus()).isActive();
        boolean rejected = submission != null
                && Objects.equals(submission.getSubmissionStatus(), MatchDataSubmissionStatusEnum.REJECTED.getCode());
        boolean confirmed = match.getDataStatus() != null
                && match.getDataStatus() == BasketPublicConstants.DATA_CONFIRMED;

        boolean canSubmit = isCaptain
                && availability.isMatchTimePassed()
                && !cancelled
                && !postponed
                && !exceptionLocked
                && !activeSubmission
                && !confirmed
                && !rejected;
        boolean canResubmit = isCaptain
                && availability.isMatchTimePassed()
                && !cancelled
                && !postponed
                && !exceptionLocked
                && rejected
                && submission != null
                && myTeamId != null
                && Objects.equals(submission.getSubmitterTeamId(), myTeamId);

        availability.setCanSubmit(canSubmit);
        availability.setCanResubmit(canResubmit);
        availability.setCanSubmitMatchData(canSubmit || canResubmit);
        availability.setSubmitDisabledReason(resolveSubmitDisabledReason(
                isCaptain, availability.isMatchTimePassed(), cancelled, postponed, exceptionLocked,
                activeSubmission, confirmed, rejected, canSubmit, canResubmit));
        return availability;
    }

    public void assertSubmissionAllowed(BasketMatch match) {
        if (isMatchCancelled(match)) {
            throw new ServiceException("比赛已取消，不能提交比赛数据");
        }
        if (isMatchPostponed(match)) {
            throw new ServiceException("比赛已延期，按新的比赛时间开放提交");
        }
        if (!isMatchTimePassed(match)) {
            throw new ServiceException("比赛开始时间未到，暂不能提交比赛数据");
        }
        if (isExceptionLocked(match)) {
            throw new ServiceException("当前比赛存在异常处理流程，暂不能提交比赛数据");
        }
    }

    private String resolveSubmitDisabledReason(boolean isCaptain, boolean matchTimePassed, boolean cancelled,
                                               boolean postponed, boolean exceptionLocked, boolean activeSubmission,
                                               boolean confirmed, boolean rejected,
                                               boolean canSubmit, boolean canResubmit) {
        if (canSubmit || canResubmit) {
            return null;
        }
        if (!isCaptain) {
            return "普通队员仅可查看比赛数据，比赛数据由赛事队长提交。";
        }
        if (!matchTimePassed) {
            return "比赛开始后开放数据提交。";
        }
        if (cancelled) {
            return "比赛已取消，不能提交比赛数据。";
        }
        if (postponed) {
            return "比赛已延期，按新的比赛时间开放提交。";
        }
        if (exceptionLocked) {
            return "当前比赛存在异常处理流程，暂不能提交比赛数据。";
        }
        if (activeSubmission) {
            return "比赛数据已提交，正在等待管理员审核。";
        }
        if (confirmed) {
            return "比赛数据已审核通过，不能再次提交。";
        }
        if (rejected) {
            return "仅驳回后可重新提交。";
        }
        return "当前暂不能提交比赛数据。";
    }

    @lombok.Data
    public static class MatchDataSubmitAvailability {
        private boolean captain;
        private boolean matchTimePassed;
        private boolean canSubmit;
        private boolean canResubmit;
        private boolean canSubmitMatchData;
        private String submitDisabledReason;
    }
}
