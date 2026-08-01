package com.zhs.web.controller.basket;

import com.zhs.basket.service.IBasketDashboardService;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

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

    @PreAuthorize("@ss.hasPermi('basket:dashboard:summary')")
    @GetMapping("/trend")
    public AjaxResult trend(@RequestParam(defaultValue = "30d") String rangeType,
                            @RequestParam(required = false) String startDate,
                            @RequestParam(required = false) String endDate,
                            @RequestParam(defaultValue = "ALL") String metricGroup) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        return success(dashboardService.getTrend(rangeType, start, end, metricGroup));
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }
}
