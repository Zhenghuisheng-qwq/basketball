package com.zhs.basket.support;

import com.zhs.basket.domain.BasketAccountSecurityLog;
import com.zhs.basket.mapper.BasketAccountSecurityLogMapper;
import com.zhs.common.utils.ServletUtils;
import com.zhs.common.utils.ip.IpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class BasketAccountSecurityLogWriter {

    private static final Integer NOT_DELETED = 0;
    private static final String OPERATOR_ADMIN = "ADMIN";

    @Autowired
    private BasketAccountSecurityLogMapper securityLogMapper;

    public void write(Long memberId, String eventType, String title,
                      String oldMasked, String newMasked, Long operatorId) {
        BasketAccountSecurityLog log = new BasketAccountSecurityLog();
        log.setMemberId(memberId);
        log.setEventType(eventType);
        log.setEventTitle(title);
        log.setOldValueMasked(oldMasked);
        log.setNewValueMasked(newMasked);
        log.setOperatorType(OPERATOR_ADMIN);
        log.setOperatorId(operatorId);
        Date now = new Date();
        log.setClientIp(IpUtils.getIpAddr());
        log.setEventTime(now);
        log.setCreateTime(now);
        log.setUpdateTime(now);
        log.setIsDelete(NOT_DELETED);
        securityLogMapper.insert(log);
    }
}
