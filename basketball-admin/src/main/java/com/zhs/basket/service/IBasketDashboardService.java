package com.zhs.basket.service;

import com.zhs.basket.vo.DashboardSummaryVo;
import com.zhs.basket.vo.DashboardTrendVo;

import java.time.LocalDate;

public interface IBasketDashboardService {
    DashboardSummaryVo getSummary();

    DashboardTrendVo getTrend(String rangeType, LocalDate startDate, LocalDate endDate, String metricGroup);
}
