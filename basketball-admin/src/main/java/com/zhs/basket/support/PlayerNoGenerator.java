package com.zhs.basket.support;

import com.zhs.basket.mapper.BasketPlayerProfileMapper;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class PlayerNoGenerator {

    private static final int MAX_RETRY = 8;
    private static final String PLAYER_NO_PREFIX = "CNPA-P-";

    @Autowired
    private BasketPlayerProfileMapper playerProfileMapper;

    public String generatePlayerNo() {
        Integer maxSeq = playerProfileMapper.selectMaxPlayerNoSequence();
        int start = maxSeq != null ? maxSeq + 1 : 1;
        for (int i = 0; i < MAX_RETRY; i++) {
            int candidate = start + i;
            if (candidate > 999_999) {
                break;
            }
            String playerNo = formatPlayerNo(candidate);
            if (playerProfileMapper.countByPlayerNo(playerNo) == 0) {
                return playerNo;
            }
        }
        for (int i = 0; i < MAX_RETRY; i++) {
            int candidate = ThreadLocalRandom.current().nextInt(1, 1_000_000);
            String playerNo = formatPlayerNo(candidate);
            if (playerProfileMapper.countByPlayerNo(playerNo) == 0) {
                return playerNo;
            }
        }
        throw new ServiceException("生成球员编号失败，请重试");
    }

    public String generateBizNo(String prefix) {
        long id = Math.abs(ThreadLocalRandom.current().nextLong());
        return prefix + String.format("%016d", id % 1_0000_0000_0000_0000L);
    }

    private String formatPlayerNo(int sequence) {
        return PLAYER_NO_PREFIX + String.format("%06d", sequence);
    }
}
