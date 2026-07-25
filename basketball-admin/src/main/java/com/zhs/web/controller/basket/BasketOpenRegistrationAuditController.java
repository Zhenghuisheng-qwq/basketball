package com.zhs.web.controller.basket;

import com.zhs.basket.query.OpenRegistrationAuditQuery;
import com.zhs.basket.req.OpenRegistrationAuditApproveReq;
import com.zhs.basket.req.OpenRegistrationAuditRejectReq;
import com.zhs.basket.service.IBasketOpenRegistrationAuditService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/openRegistrationAudit")
public class BasketOpenRegistrationAuditController extends BaseController {

    @Autowired
    private IBasketOpenRegistrationAuditService openRegistrationAuditService;

    @PreAuthorize("@ss.hasPermi('basket:openRegistrationAudit:list')")
    @GetMapping("/list")
    public TableDataInfo list(OpenRegistrationAuditQuery query) { startPage(); return getDataTable(openRegistrationAuditService.selectList(query)); }
    @PreAuthorize("@ss.hasPermi('basket:openRegistrationAudit:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) { return success(openRegistrationAuditService.selectDetail(id)); }
    @PreAuthorize("@ss.hasPermi('basket:openRegistrationAudit:audit')")
    @Log(title = "Open报名审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/start-review")
    public AjaxResult startReview(@PathVariable Long id) { openRegistrationAuditService.startReview(id); return success(); }
    @PreAuthorize("@ss.hasPermi('basket:openRegistrationAudit:audit')")
    @Log(title = "Open报名审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/approve")
    public AjaxResult approve(@PathVariable Long id, @RequestBody OpenRegistrationAuditApproveReq req) { openRegistrationAuditService.approve(id, req); return success(); }
    @PreAuthorize("@ss.hasPermi('basket:openRegistrationAudit:audit')")
    @Log(title = "Open报名审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/reject")
    public AjaxResult reject(@PathVariable Long id, @RequestBody OpenRegistrationAuditRejectReq req) { openRegistrationAuditService.reject(id, req); return success(); }
    @PreAuthorize("@ss.hasPermi('basket:openRegistrationAudit:query')")
    @GetMapping("/{id}/audit-record/list")
    public AjaxResult auditRecords(@PathVariable Long id) { return success(openRegistrationAuditService.selectAuditRecords(id)); }
}
