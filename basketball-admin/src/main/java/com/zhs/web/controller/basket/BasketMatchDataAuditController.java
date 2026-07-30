package com.zhs.web.controller.basket;

import com.zhs.basket.query.MatchDataAuditQuery;
import com.zhs.basket.req.AdminMatchDataSubmissionApproveReq;
import com.zhs.basket.req.AdminMatchDataSubmissionPageReq;
import com.zhs.basket.req.AdminMatchDataSubmissionRejectReq;
import com.zhs.basket.service.IBasketMatchDataAuditService;
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

@RestController
@RequestMapping("/basket/matchAudit")
public class BasketMatchDataAuditController extends BaseController {

    @Autowired
    private IBasketMatchDataAuditService auditService;

    @PreAuthorize("@ss.hasPermi('basket:matchAudit:list')")
    @GetMapping("/pending")
    public AjaxResult pending(MatchDataAuditQuery query) {
        return success(auditService.listPending(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:matchAudit:list')")
    @GetMapping("/submission/page")
    public TableDataInfo submissionPage(AdminMatchDataSubmissionPageReq query) {
        startPage();
        return getDataTable(auditService.pageSubmissions(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:matchAudit:query')")
    @GetMapping("/submission/{submissionId}")
    public AjaxResult submissionDetail(@PathVariable Long submissionId) {
        return success(auditService.getSubmissionDetail(submissionId));
    }

    @PreAuthorize("@ss.hasPermi('basket:matchAudit:approve')")
    @Log(title = "比赛数据审核", businessType = BusinessType.UPDATE)
    @PostMapping("/submission/{submissionId}/approve")
    public AjaxResult approveSubmission(@PathVariable Long submissionId,
            @RequestBody(required = false) AdminMatchDataSubmissionApproveReq req) {
        auditService.approveSubmission(submissionId, req != null ? req : new AdminMatchDataSubmissionApproveReq());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:matchAudit:reject')")
    @Log(title = "比赛数据审核", businessType = BusinessType.UPDATE)
    @PostMapping("/submission/{submissionId}/reject")
    public AjaxResult rejectSubmission(@PathVariable Long submissionId,
            @RequestBody AdminMatchDataSubmissionRejectReq req) {
        auditService.rejectSubmission(submissionId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:matchAudit:query')")
    @GetMapping("/submission/{matchId}/versions")
    public AjaxResult submissionVersions(@PathVariable Long matchId) {
        return success(auditService.listSubmissionVersions(matchId));
    }
}
