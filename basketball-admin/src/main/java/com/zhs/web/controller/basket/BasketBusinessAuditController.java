package com.zhs.web.controller.basket;

import com.zhs.basket.query.BusinessAuditQuery;
import com.zhs.basket.service.IBasketBusinessAuditService;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/businessAudit")
public class BasketBusinessAuditController extends BaseController {

    @Autowired
    private IBasketBusinessAuditService businessAuditService;

    @PreAuthorize("@ss.hasPermi('basket:businessAudit:list')")
    @GetMapping("/list")
    public AjaxResult list(BusinessAuditQuery query) {
        return success(businessAuditService.list(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:businessAudit:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        return success(businessAuditService.detail(id));
    }
}
