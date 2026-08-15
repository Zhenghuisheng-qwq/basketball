package com.zhs.basket.service.v4;

import com.zhs.basket.dto.v4.BasketMatchSubmissionRejectReq;
import com.zhs.basket.query.v4.BasketMatchSubmissionAuditQuery;
import com.zhs.basket.vo.v4.BasketMatchSubmissionAuditDetailVo;
import com.zhs.basket.vo.v4.BasketMatchSubmissionAuditListVo;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface IBasketV4MatchSubmissionAuditService {
    List<BasketMatchSubmissionAuditListVo> selectList(BasketMatchSubmissionAuditQuery query);

    BasketMatchSubmissionAuditDetailVo selectDetail(Long submissionId);

    void approve(Long submissionId, String auditRemark);

    void reject(Long submissionId, BasketMatchSubmissionRejectReq req);

    void streamMaterial(Long fileId, HttpServletResponse response);
}
