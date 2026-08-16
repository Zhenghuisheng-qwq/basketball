package com.zhs.basket.service.v4;

import com.zhs.basket.dto.v4.BasketMatchSubmissionRejectReq;
import com.zhs.basket.query.v4.BasketSeriesSubmissionAuditQuery;
import com.zhs.basket.vo.v4.BasketSeriesSubmissionAuditDetailVo;
import com.zhs.basket.vo.v4.BasketSeriesSubmissionAuditListVo;

import java.util.List;

public interface IBasketV4SeriesSubmissionAuditService {
    List<BasketSeriesSubmissionAuditListVo> selectList(BasketSeriesSubmissionAuditQuery query);

    BasketSeriesSubmissionAuditDetailVo selectDetail(Long seriesSubmissionId);

    void approve(Long seriesSubmissionId, String auditRemark);

    void reject(Long seriesSubmissionId, BasketMatchSubmissionRejectReq req);
}
