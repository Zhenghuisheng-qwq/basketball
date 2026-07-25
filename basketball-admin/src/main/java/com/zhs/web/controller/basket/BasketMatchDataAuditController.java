package com.zhs.web.controller.basket;

import com.zhs.basket.query.MatchDataAuditQuery;
import com.zhs.basket.service.IBasketMatchDataAuditService;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
}
