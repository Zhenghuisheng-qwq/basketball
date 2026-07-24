package com.zhs.util;

import com.zhs.dao.BasketAccountSecurityLogDao;
import com.zhs.model.BasketAccountSecurityLog;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class AccountSecurityLogHelper {

    private static final Integer NOT_DELETED = 0;
    private static final String OPERATOR_MEMBER = "MEMBER";

    @Resource
    private BasketAccountSecurityLogDao securityLogDao;

    public void write(Long memberId, String eventType, String title,
                      String oldMasked, String newMasked, Long operatorId, String clientIp) {
        BasketAccountSecurityLog logEntity = new BasketAccountSecurityLog();
        logEntity.setMemberId(memberId);
        logEntity.setEventType(eventType);
        logEntity.setEventTitle(title);
        logEntity.setOldValueMasked(oldMasked);
        logEntity.setNewValueMasked(newMasked);
        logEntity.setOperatorType(OPERATOR_MEMBER);
        logEntity.setOperatorId(operatorId);
        logEntity.setClientIp(clientIp);
        logEntity.setEventTime(new Date());
        logEntity.setIsDelete(NOT_DELETED);
        securityLogDao.insert(logEntity);
    }
}
