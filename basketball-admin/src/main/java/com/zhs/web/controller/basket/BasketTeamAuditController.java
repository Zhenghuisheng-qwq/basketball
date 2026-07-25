package com.zhs.web.controller.basket;

import com.zhs.basket.query.TeamAuditQuery;
import com.zhs.basket.req.TeamAuditApproveReq;
import com.zhs.basket.req.TeamAuditRejectReq;
import com.zhs.basket.service.IBasketTeamAuditService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/teamAudit")
public class BasketTeamAuditController extends BaseController {

    @Autowired
    private IBasketTeamAuditService teamAuditService;

    @PreAuthorize("@ss.hasPermi('basket:teamAudit:list')")
    @GetMapping("/list")
    public TableDataInfo list(TeamAuditQuery query) {
        startPage();
        return getDataTable(teamAuditService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:teamAudit:query')")
    @GetMapping("/{teamId}")
    public AjaxResult getInfo(@PathVariable Long teamId) {
        return success(teamAuditService.selectDetail(teamId));
    }

    @PreAuthorize("@ss.hasPermi('basket:teamAudit:query')")
    @GetMapping("/{teamId}/audit-record/list")
    public AjaxResult auditRecords(@PathVariable Long teamId) {
        return success(teamAuditService.selectAuditRecords(teamId));
    }

    @PreAuthorize("@ss.hasPermi('basket:teamAudit:audit')")
    @Log(title = "球队审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{teamId}/start-review")
    public AjaxResult startReview(@PathVariable Long teamId) {
        teamAuditService.startReview(teamId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:teamAudit:audit')")
    @Log(title = "球队审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{teamId}/approve")
    public AjaxResult approve(@PathVariable Long teamId, @RequestBody TeamAuditApproveReq req) {
        teamAuditService.approve(teamId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:teamAudit:audit')")
    @Log(title = "球队审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{teamId}/reject")
    public AjaxResult reject(@PathVariable Long teamId, @RequestBody TeamAuditRejectReq req) {
        teamAuditService.reject(teamId, req);
        return success();
    }
}
