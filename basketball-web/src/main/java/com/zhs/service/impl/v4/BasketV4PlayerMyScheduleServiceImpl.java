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
import com.zhs.response.v4.V4MyScheduleResp;
import com.zhs.service.v4.IBasketV4PlayerMyScheduleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BasketV4PlayerMyScheduleServiceImpl implements IBasketV4PlayerMyScheduleService {

    private static final Integer NOT_DELETED = 0;

    @Resource
    private BasketV4PlayerMatchSubmissionMapper matchSubmissionMapper;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;

    @Override
    public List<V4MyScheduleResp> listMySchedule(Long seasonId, Long divisionId, String status,
                                                 Boolean includeFinished, Boolean onlyPendingSubmit) {
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile player = requirePlayerByMemberId(memberId);
        boolean showFinished = Boolean.TRUE.equals(includeFinished);
        List<V4MyScheduleResp> rows = matchSubmissionMapper.selectMySchedule(
                memberId, player.getId(), seasonId, divisionId, status, showFinished, onlyPendingSubmit);
        for (V4MyScheduleResp row : rows) {
            enrichCanSubmit(row);
        }
        return rows;
    }

    private void enrichCanSubmit(V4MyScheduleResp row) {
        row.setCanSubmit(false);
        row.setCanSubmitData(false);
        if (row.getMyEventTeamId() == null) {
            row.setCannotSubmitReason("不属于本场比赛球队");
            return;
        }
        if (!MatchPublishStatusEnum.PUBLISHED.getCode().equals(row.getPublishStatus())) {
            row.setCannotSubmitReason("比赛未发布");
            return;
        }
        if (MatchStatusEnum.CANCELLED.getCode().equals(row.getMatchStatus())) {
            row.setCannotSubmitReason("比赛已取消");
            return;
        }
        if (SubmissionStatusEnum.PENDING_REVIEW.getCode().equals(row.getMySubmissionStatus())) {
            row.setCannotSubmitReason("本方审核中");
            return;
        }
        if (SubmissionStatusEnum.APPROVED.getCode().equals(row.getMySubmissionStatus())) {
            row.setCannotSubmitReason("本方已通过");
            return;
        }
        row.setCanSubmit(true);
        row.setCanSubmitData(true);
        row.setCannotSubmitReason(null);
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
