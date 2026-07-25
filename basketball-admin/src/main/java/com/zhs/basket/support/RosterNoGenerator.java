package com.zhs.basket.support;

import com.zhs.basket.mapper.BasketTournamentSeasonRosterMapper;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class RosterNoGenerator {

    private static final int MAX_RETRY = 8;
    private static final String PREFIX = "CNPA-ER-";

    @Autowired
    private BasketTournamentSeasonRosterMapper seasonRosterMapper;

    public String generateRosterNo() {
        Integer maxSeq = seasonRosterMapper.selectMaxRosterNoSequence();
        int start = maxSeq != null ? maxSeq + 1 : 1;
        for (int i = 0; i < MAX_RETRY; i++) {
            int candidate = start + i;
            if (candidate > 999_999) break;
            String rosterNo = formatNo(candidate);
            if (seasonRosterMapper.countByRosterNo(rosterNo) == 0) {
                return rosterNo;
            }
        }
        for (int i = 0; i < MAX_RETRY; i++) {
            int candidate = ThreadLocalRandom.current().nextInt(1, 1_000_000);
            String rosterNo = formatNo(candidate);
            if (seasonRosterMapper.countByRosterNo(rosterNo) == 0) {
                return rosterNo;
            }
        }
        throw new ServiceException("生成赛事阵容编号失败，请重试");
    }

    private String formatNo(int sequence) {
        return PREFIX + String.format("%06d", sequence);
    }
}
