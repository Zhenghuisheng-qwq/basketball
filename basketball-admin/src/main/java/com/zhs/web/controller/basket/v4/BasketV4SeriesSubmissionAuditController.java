package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.dto.v4.BasketMatchSubmissionRejectReq;
import com.zhs.basket.query.v4.BasketSeriesSubmissionAuditQuery;
import com.zhs.basket.service.v4.IBasketV4SeriesSubmissionAuditService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/basket/v4/series-submission-audit")
public class BasketV4SeriesSubmissionAuditController extends BaseController {

    @Autowired
    private IBasketV4SeriesSubmissionAuditService auditService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.MATCH_SUBMISSION_AUDIT_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(BasketSeriesSubmissionAuditQuery query) {
        startPage();
        return getDataTable(auditService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.MATCH_SUBMISSION_AUDIT_QUERY + "')")
    @GetMapping("/{seriesSubmissionId}")
    public AjaxResult getInfo(@PathVariable Long seriesSubmissionId) {
        return success(auditService.selectDetail(seriesSubmissionId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.MATCH_SUBMISSION_AUDIT_APPROVE + "')")
    @Log(title = "V4 SeriesSubmissionAudit", businessType = BusinessType.UPDATE)
    @PostMapping("/{seriesSubmissionId}/approve")
    public AjaxResult approve(@PathVariable Long seriesSubmissionId, @RequestBody(required = false) Map<String, String> body) {
        String auditRemark = body != null ? body.get("auditRemark") : null;
        auditService.approve(seriesSubmissionId, auditRemark);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.MATCH_SUBMISSION_AUDIT_REJECT + "')")
    @Log(title = "V4 SeriesSubmissionAudit", businessType = BusinessType.UPDATE)
    @PostMapping("/{seriesSubmissionId}/reject")
    public AjaxResult reject(@PathVariable Long seriesSubmissionId, @RequestBody BasketMatchSubmissionRejectReq req) {
        auditService.reject(seriesSubmissionId, req);
        return success();
    }
}
