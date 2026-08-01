package com.zhs.web.controller.basket;

import com.zhs.basket.query.RosterChangeAuditQuery;
import com.zhs.basket.req.BatchAuditOperationReq;
import com.zhs.basket.req.RosterChangeAuditApproveReq;
import com.zhs.basket.req.RosterChangeAuditRejectReq;
import com.zhs.basket.service.IBasketRosterChangeAuditService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/rosterChangeAudit")
public class BasketRosterChangeAuditController extends BaseController {

    @Autowired
    private IBasketRosterChangeAuditService rosterChangeAuditService;

    @PreAuthorize("@ss.hasPermi('basket:rosterChangeAudit:list')")
    @GetMapping("/list")
    public TableDataInfo list(RosterChangeAuditQuery query) { startPage(); return getDataTable(rosterChangeAuditService.selectList(query)); }
    @PreAuthorize("@ss.hasPermi('basket:rosterChangeAudit:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) { return success(rosterChangeAuditService.selectDetail(id)); }
    @PreAuthorize("@ss.hasPermi('basket:rosterChangeAudit:audit')")
    @Log(title = "阵容变更审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/approve")
    public AjaxResult approve(@PathVariable Long id, @RequestBody RosterChangeAuditApproveReq req) { rosterChangeAuditService.approve(id, req); return success(); }
    @PreAuthorize("@ss.hasPermi('basket:rosterChangeAudit:audit')")
    @Log(title = "阵容变更审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/reject")
    public AjaxResult reject(@PathVariable Long id, @RequestBody RosterChangeAuditRejectReq req) { rosterChangeAuditService.reject(id, req); return success(); }

    @PreAuthorize("@ss.hasPermi('basket:rosterChangeAudit:audit')")
    @Log(title = "阵容变更审核", businessType = BusinessType.UPDATE)
    @PostMapping("/batch-approve")
    public AjaxResult batchApprove(@RequestBody BatchAuditOperationReq req) { return success(rosterChangeAuditService.batchApprove(req)); }

    @PreAuthorize("@ss.hasPermi('basket:rosterChangeAudit:audit')")
    @Log(title = "阵容变更审核", businessType = BusinessType.UPDATE)
    @PostMapping("/batch-reject")
    public AjaxResult batchReject(@RequestBody BatchAuditOperationReq req) { return success(rosterChangeAuditService.batchReject(req)); }
}
