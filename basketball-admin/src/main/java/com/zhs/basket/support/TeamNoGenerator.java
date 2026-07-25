package com.zhs.basket.support;

import com.zhs.basket.mapper.BasketTeamMapper;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class TeamNoGenerator {

    private static final int MAX_RETRY = 8;
    private static final String TEAM_NO_PREFIX = "CNPA-T-";

    @Autowired
    private BasketTeamMapper teamMapper;

    public String generateTeamNo() {
        Integer maxSeq = teamMapper.selectMaxTeamNoSequence();
        int start = maxSeq != null ? maxSeq + 1 : 1;
        for (int i = 0; i < MAX_RETRY; i++) {
            int candidate = start + i;
            if (candidate > 999_999) {
                break;
            }
            String teamNo = formatTeamNo(candidate);
            if (teamMapper.countByTeamNo(teamNo) == 0) {
                return teamNo;
            }
        }
        for (int i = 0; i < MAX_RETRY; i++) {
            int candidate = ThreadLocalRandom.current().nextInt(1, 1_000_000);
            String teamNo = formatTeamNo(candidate);
            if (teamMapper.countByTeamNo(teamNo) == 0) {
                return teamNo;
            }
        }
        throw new ServiceException("生成球队编号失败，请重试");
    }

    private String formatTeamNo(int sequence) {
        return TEAM_NO_PREFIX + String.format("%06d", sequence);
    }
}
