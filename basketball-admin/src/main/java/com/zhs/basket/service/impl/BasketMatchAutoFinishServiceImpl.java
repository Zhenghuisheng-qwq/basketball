package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.enums.MatchChangeTypeEnum;
import com.zhs.basket.enums.MatchDataStatusEnum;
import com.zhs.basket.enums.MatchStatusEnum;
import com.zhs.basket.mapper.BasketMatchMapper;
import com.zhs.basket.service.IBasketMatchAutoFinishService;
import com.zhs.basket.service.IBasketMatchValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class BasketMatchAutoFinishServiceImpl implements IBasketMatchAutoFinishService {

    private static final Logger log = LoggerFactory.getLogger(BasketMatchAutoFinishServiceImpl.class);
    private static final int BATCH_SIZE = 100;
    private static final String AUTO_FINISH_REASON =
            "比赛计划时间已过，系统自动结束比赛并开放数据提交";

    @Autowired
    private BasketMatchMapper matchMapper;
    @Autowired
    private IBasketMatchValidationService validationService;

    @Override
    public int autoFinishEligibleMatches() {
        Date now = new Date();
        int total = 0;
        while (true) {
            List<Long> matchIds = matchMapper.selectIdsEligibleForAutoFinish(now, BATCH_SIZE);
            if (matchIds.isEmpty()) {
                break;
            }
            for (Long matchId : matchIds) {
                total += finishOne(matchId, now);
            }
            if (matchIds.size() < BATCH_SIZE) {
                break;
            }
        }
        if (total > 0) {
            log.info("计划时间自动结束比赛完成，成功 {} 场", total);
        }
        return total;
    }

    private int finishOne(Long matchId, Date now) {
        BasketMatch before = matchMapper.selectById(matchId);
        if (before == null) {
            return 0;
        }
        Integer beforeStatus = before.getMatchStatus();
        int updated = matchMapper.autoFinishBySchedule(
                matchId,
                MatchStatusEnum.SCHEDULED.getCode(),
                MatchStatusEnum.FINISHED.getCode(),
                MatchDataStatusEnum.DRAFT.getCode(),
                now);
        if (updated != 1) {
            return 0;
        }
        BasketMatch after = matchMapper.selectById(matchId);
        if (after != null) {
            validationService.writeSystemStatusLog(after, beforeStatus,
                    MatchChangeTypeEnum.AUTO_FINISH_BY_SCHEDULE, AUTO_FINISH_REASON);
        }
        return 1;
    }
}
