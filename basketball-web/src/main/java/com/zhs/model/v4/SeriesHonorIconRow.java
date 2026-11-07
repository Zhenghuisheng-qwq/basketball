package com.zhs.model.v4;

import lombok.Data;

/**
 * 赛事系列荣誉徽章，单独查询，避免默认 Series 查询依赖尚未上线的列。
 */
@Data
public class SeriesHonorIconRow {
    private Long id;
    private String honorIconUrl;
}
