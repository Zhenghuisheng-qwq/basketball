package com.zhs.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.BasketMatchDataDisputeDao;
import com.zhs.dao.BasketMatchExceptionApplicationDao;
import com.zhs.dao.BasketMatchOfficialResultDao;
import com.zhs.enums.MatchDataDisputeStatusEnum;
import com.zhs.enums.MatchExceptionApplicationStatusEnum;
import com.zhs.model.BasketMatch;
import com.zhs.model.BasketMatchDataDispute;
import com.zhs.model.BasketMatchExceptionApplication;
import com.zhs.model.BasketMatchOfficialResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Component
public class PlayerMatchDisputeExceptionSupport {

    public static final int DISPUTE_WINDOW_HOURS = 12;
    private static final Integer NOT_DELETED = 0;
    private static final int PUBLIC_FLAG = 1;

    @Resource
    private BasketMatchOfficialResultDao officialResultDao;
    @Resource
    private BasketMatchDataDisputeDao disputeDao;
    @Resource
    private BasketMatchExceptionApplicationDao exceptionApplicationDao;

    public BasketMatchOfficialResult getCurrentOfficialResult(Long matchId) {
        if (matchId == null) {
            return null;
        }
        return officialResultDao.selectCurrentByMatchId(matchId);
    }

    public Date resolveDisputeBaseTime(BasketMatch match, BasketMatchOfficialResult official) {
        if (official != null) {
            if (official.getEffectiveTime() != null) {
                return official.getEffectiveTime();
            }
            if (official.getConfirmedTime() != null) {
                return official.getConfirmedTime();
            }
        }
        if (match != null && match.getUpdateTime() != null) {
            return match.getUpdateTime();
        }
        return null;
    }

    public Date resolveDisputeDeadline(BasketMatch match, BasketMatchOfficialResult official) {
        Date base = resolveDisputeBaseTime(match, official);
        if (base == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(base);
        cal.add(Calendar.HOUR_OF_DAY, DISPUTE_WINDOW_HOURS);
        return cal.getTime();
    }

    public Date resolvePublicTime(BasketMatchOfficialResult official) {
        if (official == null || official.getIsPublic() == null || official.getIsPublic() != PUBLIC_FLAG) {
            return null;
        }
        return official.getEffectiveTime() != null ? official.getEffectiveTime() : official.getConfirmedTime();
    }

    public Date resolveConfirmedTime(BasketMatch match, BasketMatchOfficialResult official) {
        if (official != null && official.getConfirmedTime() != null) {
            return official.getConfirmedTime();
        }
        if (match != null && match.getDataStatus() != null
                && match.getDataStatus() == BasketPublicConstants.DATA_CONFIRMED) {
            return match.getUpdateTime();
        }
        return null;
    }

    public long computeRemainingSeconds(Date deadline) {
        if (deadline == null) {
            return 0L;
        }
        long diff = deadline.getTime() - System.currentTimeMillis();
        return Math.max(diff / 1000, 0L);
    }

    public boolean isOfficialPublicOrConfirmed(BasketMatch match, BasketMatchOfficialResult official) {
        if (match != null && match.getDataStatus() != null
                && match.getDataStatus() == BasketPublicConstants.DATA_CONFIRMED) {
            return true;
        }
        return official != null && official.getIsPublic() != null && official.getIsPublic() == PUBLIC_FLAG;
    }

    public BasketMatchDataDispute findActiveDispute(Long matchId) {
        LambdaQueryWrapper<BasketMatchDataDispute> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasketMatchDataDispute::getMatchId, matchId)
                .eq(BasketMatchDataDispute::getIsDelete, NOT_DELETED)
                .in(BasketMatchDataDispute::getDisputeStatus,
                        MatchDataDisputeStatusEnum.PENDING.getCode(),
                        MatchDataDisputeStatusEnum.PROCESSING.getCode())
                .orderByDesc(BasketMatchDataDispute::getCreateTime)
                .last("LIMIT 1");
        return disputeDao.selectOne(wrapper);
    }

    public BasketMatchDataDispute findLatestDispute(Long matchId) {
        LambdaQueryWrapper<BasketMatchDataDispute> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasketMatchDataDispute::getMatchId, matchId)
                .eq(BasketMatchDataDispute::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketMatchDataDispute::getCreateTime)
                .last("LIMIT 1");
        return disputeDao.selectOne(wrapper);
    }

    public List<BasketMatchDataDispute> listDisputeHistory(Long matchId) {
        LambdaQueryWrapper<BasketMatchDataDispute> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasketMatchDataDispute::getMatchId, matchId)
                .eq(BasketMatchDataDispute::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketMatchDataDispute::getCreateTime);
        return disputeDao.selectList(wrapper);
    }

    public BasketMatchExceptionApplication findActiveException(Long matchId) {
        LambdaQueryWrapper<BasketMatchExceptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasketMatchExceptionApplication::getMatchId, matchId)
                .eq(BasketMatchExceptionApplication::getIsDelete, NOT_DELETED)
                .in(BasketMatchExceptionApplication::getApplicationStatus,
                        MatchExceptionApplicationStatusEnum.PENDING.getCode(),
                        MatchExceptionApplicationStatusEnum.PROCESSING.getCode())
                .orderByDesc(BasketMatchExceptionApplication::getCreateTime)
                .last("LIMIT 1");
        return exceptionApplicationDao.selectOne(wrapper);
    }

    public BasketMatchExceptionApplication findLatestException(Long matchId) {
        LambdaQueryWrapper<BasketMatchExceptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasketMatchExceptionApplication::getMatchId, matchId)
                .eq(BasketMatchExceptionApplication::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketMatchExceptionApplication::getCreateTime)
                .last("LIMIT 1");
        return exceptionApplicationDao.selectOne(wrapper);
    }

    public boolean hasApprovedException(Long matchId) {
        LambdaQueryWrapper<BasketMatchExceptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasketMatchExceptionApplication::getMatchId, matchId)
                .eq(BasketMatchExceptionApplication::getIsDelete, NOT_DELETED)
                .eq(BasketMatchExceptionApplication::getApplicationStatus,
                        MatchExceptionApplicationStatusEnum.APPROVED.getCode());
        return exceptionApplicationDao.selectCount(wrapper) > 0;
    }

    public List<BasketMatchExceptionApplication> listExceptionHistory(Long matchId) {
        LambdaQueryWrapper<BasketMatchExceptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasketMatchExceptionApplication::getMatchId, matchId)
                .eq(BasketMatchExceptionApplication::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketMatchExceptionApplication::getCreateTime);
        return exceptionApplicationDao.selectList(wrapper);
    }

    public DisputeAvailability evaluateDisputeAvailability(BasketMatch match, boolean captain) {
        DisputeAvailability availability = new DisputeAvailability();
        BasketMatchOfficialResult official = getCurrentOfficialResult(match.getId());
        Date deadline = resolveDisputeDeadline(match, official);
        availability.setDeadlineTime(deadline);
        availability.setRemainingSeconds(computeRemainingSeconds(deadline));
        availability.setConfirmedTime(resolveConfirmedTime(match, official));
        availability.setPublicTime(resolvePublicTime(official));

        BasketMatchDataDispute active = findActiveDispute(match.getId());
        if (active != null) {
            availability.setCurrentDisputeId(active.getId());
            availability.setCurrentDisputeStatus(active.getDisputeStatus());
        }

        if (!captain) {
            availability.setCanDispute(false);
            availability.setCannotReason("仅参赛球队队长可发起争议");
            return availability;
        }
        if (match.getDataStatus() == null || match.getDataStatus() != BasketPublicConstants.DATA_CONFIRMED) {
            availability.setCanDispute(false);
            availability.setCannotReason("比赛数据尚未确认，无法发起争议");
            return availability;
        }
        if (!isOfficialPublicOrConfirmed(match, official)) {
            availability.setCanDispute(false);
            availability.setCannotReason("正式结果尚未公开或确认");
            return availability;
        }
        if (active != null) {
            availability.setCanDispute(false);
            availability.setCannotReason("当前比赛已有待处理争议");
            return availability;
        }
        if (deadline != null && new Date().after(deadline)) {
            availability.setCanDispute(false);
            availability.setCannotReason("争议窗口已结束（确认/公开后12小时内可发起）");
            return availability;
        }
        availability.setCanDispute(true);
        availability.setCannotReason(null);
        return availability;
    }

    public ExceptionAvailability evaluateExceptionAvailability(BasketMatch match, boolean captain) {
        ExceptionAvailability availability = new ExceptionAvailability();
        BasketMatchExceptionApplication active = findActiveException(match.getId());
        if (active != null) {
            availability.setCurrentExceptionId(active.getId());
            availability.setCurrentExceptionStatus(active.getApplicationStatus());
        }

        if (!captain) {
            availability.setCanSubmitException(false);
            availability.setCannotReason("仅参赛球队队长可提交异常申请");
            return availability;
        }
        if (match.getDataStatus() != null && match.getDataStatus() == BasketPublicConstants.DATA_CONFIRMED) {
            availability.setCanSubmitException(false);
            availability.setCannotReason("比赛数据已确认，请通过争议流程处理");
            return availability;
        }
        if (active != null) {
            availability.setCanSubmitException(false);
            availability.setCannotReason("当前比赛已有待处理异常申请");
            return availability;
        }
        if (hasApprovedException(match.getId())) {
            availability.setCanSubmitException(false);
            availability.setCannotReason("该比赛已有已通过的异常申请");
            return availability;
        }
        availability.setCanSubmitException(true);
        availability.setCannotReason(null);
        return availability;
    }

    @lombok.Data
    public static class DisputeAvailability {
        private Boolean canDispute;
        private String cannotReason;
        private Date deadlineTime;
        private Long remainingSeconds;
        private Long currentDisputeId;
        private String currentDisputeStatus;
        private Date confirmedTime;
        private Date publicTime;
    }

    @lombok.Data
    public static class ExceptionAvailability {
        private Boolean canSubmitException;
        private String cannotReason;
        private Long currentExceptionId;
        private String currentExceptionStatus;
    }
}
