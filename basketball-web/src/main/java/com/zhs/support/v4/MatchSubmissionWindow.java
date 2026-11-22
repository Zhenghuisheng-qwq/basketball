package com.zhs.support.v4;

import com.zhs.enums.v4.MatchStatusEnum;

import java.util.Date;

/**
 * 队长提交比赛数据的时间窗。
 * 胜者和「已结束」是审核通过后才落库的，不能当成提交前提。
 */
public final class MatchSubmissionWindow {

    private MatchSubmissionWindow() {
    }

    public static boolean openForDataSubmit(String matchStatus, Date matchTime, Date now) {
        if (matchStatus == null || MatchStatusEnum.CANCELLED.getCode().equals(matchStatus)) {
            return false;
        }
        if (MatchStatusEnum.FINISHED.getCode().equals(matchStatus)
                || MatchStatusEnum.FORFEITED.getCode().equals(matchStatus)
                || MatchStatusEnum.PLAYING.getCode().equals(matchStatus)) {
            return true;
        }
        return matchTime != null && now != null && !now.before(matchTime);
    }
}
