package com.zhs.basket.vo;

import lombok.Data;

@Data
public class DashboardTrendSummaryVo {
    private Integer todayNew;
    private Integer todayHandled;
    private Integer pendingTotal;
    private Integer monthTotal;
}
