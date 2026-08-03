package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.dto.v4.BasketRegistrationAuditRejectReq;
import com.zhs.basket.query.v4.BasketRegistrationAuditQuery;
import com.zhs.basket.service.v4.IBasketV4RegistrationAuditService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/v4/registration-audit")
public class BasketV4RegistrationAuditController extends BaseController {

    @Autowired
    private IBasketV4RegistrationAuditService registrationAuditService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.REGISTRATION_AUDIT_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(BasketRegistrationAuditQuery query) {
        startPage();
        return getDataTable(registrationAuditService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.REGISTRATION_AUDIT_QUERY + "')")
    @GetMapping("/{registrationId}")
    public AjaxResult getInfo(@PathVariable Long registrationId) {
        return success(registrationAuditService.selectDetail(registrationId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.REGISTRATION_AUDIT_APPROVE + "')")
    @Log(title = "V4 RegistrationAudit", businessType = BusinessType.UPDATE)
    @PostMapping("/{registrationId}/approve")
    public AjaxResult approve(@PathVariable Long registrationId) {
        registrationAuditService.approve(registrationId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.REGISTRATION_AUDIT_REJECT + "')")
    @Log(title = "V4 RegistrationAudit", businessType = BusinessType.UPDATE)
    @PostMapping("/{registrationId}/reject")
    public AjaxResult reject(@PathVariable Long registrationId, @RequestBody BasketRegistrationAuditRejectReq req) {
        registrationAuditService.reject(registrationId, req);
        return success();
    }
}
