package com.zhs.basket.support;

import com.zhs.basket.domain.BasketCaptainGrantLog;
import com.zhs.basket.domain.BasketCaptainQualification;
import com.zhs.basket.enums.CaptainGrantLogActionTypeEnum;
import com.zhs.basket.enums.CaptainQualificationStatusEnum;
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

    public void writeGrantLog(BasketCaptainQualification grant,
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
        log.setPaymentConfirmed(grant.getPaymentConfirmed());
        log.setPaymentReference(grant.getPaymentReference());
        log.setGrantTime(grant.getGrantedTime());
        log.setExpireTime(grant.getValidTo());
        log.setRevokeTime(grant.getRevokedTime());
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

    /**
     * 授权日志状态文案。首次 GRANT 的 beforeStatus 为 null，展示为 "-"，不能抛异常。
     */
    public static String statusText(Integer status) {
        if (status == null) {
            return "-";
        }
        for (CaptainQualificationStatusEnum value : CaptainQualificationStatusEnum.values()) {
            if (value.getCode() == status) {
                return value.getDescription();
            }
        }
        return "未知(" + status + ")";
    }
}
