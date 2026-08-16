package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.dto.v4.BasketMatchSubmissionRejectReq;
import com.zhs.basket.query.v4.BasketMatchSubmissionAuditQuery;
import com.zhs.basket.query.v4.BasketV4StatsQuery;
import com.zhs.basket.service.v4.IBasketV4MatchSubmissionAuditService;
import com.zhs.basket.service.v4.IBasketV4StatsQueryService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

@RestController
@RequestMapping("/basket/v4/match-submission-audit")
public class BasketV4MatchSubmissionAuditController extends BaseController {

    @Autowired
    private IBasketV4MatchSubmissionAuditService auditService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.MATCH_SUBMISSION_AUDIT_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(BasketMatchSubmissionAuditQuery query) {
        startPage();
        return getDataTable(auditService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.MATCH_SUBMISSION_AUDIT_QUERY + "')")
    @GetMapping("/material/{fileId}")
    public void streamMaterial(@PathVariable Long fileId, HttpServletResponse response) {
        auditService.streamMaterial(fileId, response);
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.MATCH_SUBMISSION_AUDIT_QUERY + "')")
    @GetMapping("/{submissionId}")
    public AjaxResult getInfo(@PathVariable Long submissionId) {
        return success(auditService.selectDetail(submissionId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.MATCH_SUBMISSION_AUDIT_APPROVE + "')")
    @Log(title = "V4 MatchSubmissionAudit", businessType = BusinessType.UPDATE)
    @PostMapping("/{submissionId}/approve")
    public AjaxResult approve(@PathVariable Long submissionId, @RequestBody(required = false) Map<String, String> body) {
        String auditRemark = body != null ? body.get("auditRemark") : null;
        auditService.approve(submissionId, auditRemark);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.MATCH_SUBMISSION_AUDIT_REJECT + "')")
    @Log(title = "V4 MatchSubmissionAudit", businessType = BusinessType.UPDATE)
    @PostMapping("/{submissionId}/reject")
    public AjaxResult reject(@PathVariable Long submissionId, @RequestBody BasketMatchSubmissionRejectReq req) {
        auditService.reject(submissionId, req);
        return success();
    }
}
