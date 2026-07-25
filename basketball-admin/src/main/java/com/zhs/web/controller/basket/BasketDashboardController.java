package com.zhs.web.controller.basket;

import com.zhs.basket.service.IBasketDashboardService;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/dashboard")
public class BasketDashboardController extends BaseController {

    @Autowired
    private IBasketDashboardService dashboardService;

    @PreAuthorize("@ss.hasPermi('basket:dashboard:summary')")
    @GetMapping("/summary")
    public AjaxResult summary() {
        return success(dashboardService.getSummary());
    }
}
