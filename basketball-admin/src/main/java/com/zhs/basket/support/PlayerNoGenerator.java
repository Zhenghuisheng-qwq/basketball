package com.zhs.basket.support;

import com.zhs.basket.mapper.BasketPlayerProfileMapper;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class PlayerNoGenerator {

    private static final int MAX_RETRY = 8;

    @Autowired
    private BasketPlayerProfileMapper playerProfileMapper;

    public String generatePlayerNo() {
        String year = String.valueOf(LocalDate.now().getYear());
        for (int i = 0; i < MAX_RETRY; i++) {
            String suffix = String.format("%08d", Math.abs(ThreadLocalRandom.current().nextLong()) % 100_000_000L);
            String playerNo = "CNPA" + year + suffix;
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
}
