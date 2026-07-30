package com.zhs.basket.support;

import com.zhs.basket.domain.BasketMatchDataAudit;
import com.zhs.basket.enums.MatchDataAuditActionEnum;
import com.zhs.basket.mapper.BasketMatchDataAuditMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class BasketMatchDataAuditLogSupport {

    private static final Integer NOT_DELETED = 0;
    private static final String AUDIT_TYPE_SUBMISSION = "SUBMISSION";

    @Autowired
    private BasketMatchDataAuditMapper auditMapper;

    public void writeAuditLog(Long matchId, Long submissionId, Integer dataVersion,
            MatchDataAuditActionEnum action, String beforeStatus, String afterStatus,
            Long submitterId, Date submitTime, String auditStatus, Long auditorId, String auditorName,
            Date auditTime, String auditRemark, String rejectReason, Long prevAuditId) {
        Date now = new Date();
        BasketMatchDataAudit row = new BasketMatchDataAudit();
        row.setMatchId(matchId);
        row.setSubmissionId(submissionId);
        row.setDataVersion(dataVersion != null ? dataVersion : 1);
        row.setAuditType(AUDIT_TYPE_SUBMISSION);
        row.setAuditAction(action.getCode());
        row.setBeforeStatus(beforeStatus);
        row.setAfterStatus(afterStatus);
        row.setSubmitterId(submitterId);
        row.setSubmitTime(submitTime);
        row.setAuditStatus(auditStatus);
        row.setAuditorId(auditorId);
        row.setAuditorName(auditorName);
        row.setAuditTime(auditTime != null ? auditTime : now);
        row.setAuditRemark(auditRemark);
        row.setRejectReason(rejectReason);
        row.setPrevAuditId(prevAuditId);
        row.setCreateTime(now);
        row.setUpdateTime(now);
        row.setIsDelete(NOT_DELETED);
        auditMapper.insert(row);
    }
}
