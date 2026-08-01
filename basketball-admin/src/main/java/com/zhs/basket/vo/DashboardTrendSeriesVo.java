package com.zhs.basket.vo;

import lombok.Data;

import java.util.List;

@Data
public class DashboardTrendSeriesVo {
    private String name;
    private String code;
    private List<Integer> data;
}
