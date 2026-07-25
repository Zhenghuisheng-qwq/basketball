package com.zhs.web.controller.basket;

import com.zhs.basket.query.RegistrationAuditQuery;
import com.zhs.basket.req.RegistrationAuditApproveReq;
import com.zhs.basket.req.RegistrationAuditRejectReq;
import com.zhs.basket.service.IBasketRegistrationAuditService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/registrationAudit")
public class BasketRegistrationAuditController extends BaseController {

    @Autowired
    private IBasketRegistrationAuditService registrationAuditService;

    @PreAuthorize("@ss.hasPermi('basket:registrationAudit:list')")
    @GetMapping("/list")
    public TableDataInfo list(RegistrationAuditQuery query) {
        startPage();
        return getDataTable(registrationAuditService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:registrationAudit:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(registrationAuditService.selectDetail(id));
    }

    @PreAuthorize("@ss.hasPermi('basket:registrationAudit:audit')")
    @Log(title = "报名审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/approve")
    public AjaxResult approve(@PathVariable Long id, @RequestBody RegistrationAuditApproveReq req) {
        registrationAuditService.approve(id, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:registrationAudit:audit')")
    @Log(title = "报名审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/reject")
    public AjaxResult reject(@PathVariable Long id, @RequestBody RegistrationAuditRejectReq req) {
        registrationAuditService.reject(id, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:registrationAudit:query')")
    @GetMapping("/{id}/audit-record/list")
    public AjaxResult auditRecords(@PathVariable Long id) {
        return success(registrationAuditService.selectAuditRecords(id));
    }
}
