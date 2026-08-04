package com.zhs.basket.support;

import com.zhs.basket.domain.BasketCaptainGrantLog;
import com.zhs.basket.domain.v4.BasketCaptainGrant;
import com.zhs.basket.enums.CaptainGrantLogActionTypeEnum;
import com.zhs.basket.enums.v4.CaptainGrantStatusEnum;
import com.zhs.basket.mapper.BasketCaptainGrantLogMapper;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class CaptainGrantLogSupport {

    private static final Integer NOT_DELETED = 0;

    @Autowired
    private BasketCaptainGrantLogMapper grantLogMapper;

    public void writeGrantLog(BasketCaptainGrant grant,
                              CaptainGrantLogActionTypeEnum actionType,
                              Integer beforeStatus,
                              Integer afterStatus,
                              String reason,
                              String remark,
                              String operatorType) {
        if (grant == null || actionType == null) {
            return;
        }
        Date now = new Date();
        Long operatorId = null;
        String operatorName = null;
        if ("ADMIN".equalsIgnoreCase(operatorType)) {
            try {
                operatorId = SecurityUtils.getUserId();
                operatorName = SecurityUtils.getUsername();
            } catch (Exception ignored) {
                operatorName = "管理员";
            }
        } else {
            operatorName = "系统";
        }

        BasketCaptainGrantLog log = new BasketCaptainGrantLog();
        log.setGrantId(grant.getId());
        log.setMemberId(grant.getMemberId());
        log.setPlayerId(grant.getPlayerId());
        log.setTeamId(grant.getTeamId());
        log.setSeasonId(grant.getSeasonId());
        log.setActionType(actionType.getCode());
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setPaymentConfirmed(1);
        log.setGrantTime(grant.getCreateTime());
        log.setExpireTime(grant.getValidTo());
        log.setOperatorId(operatorId);
        log.setOperatorName(StringUtils.isNotEmpty(operatorName) ? operatorName : "系统");
        log.setOperatorType(StringUtils.isNotEmpty(operatorType) ? operatorType : "ADMIN");
        log.setReason(StringUtils.trim(reason));
        log.setRemark(StringUtils.trim(remark));
        log.setCreateTime(now);
        log.setUpdateTime(now);
        log.setIsDelete(NOT_DELETED);
        grantLogMapper.insert(log);
    }

    public static String statusText(Integer status) {
        if (status == null) {
            return "-";
        }
        if (status == 1) {
            return CaptainGrantStatusEnum.ACTIVE.getDescription();
        }
        if (status == 2) {
            return CaptainGrantStatusEnum.REVOKED.getDescription();
        }
        if (status == 3) {
            return CaptainGrantStatusEnum.EXPIRED.getDescription();
        }
        return "未知(" + status + ")";
    }
}
