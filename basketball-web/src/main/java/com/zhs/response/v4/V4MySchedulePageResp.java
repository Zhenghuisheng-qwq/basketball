package com.zhs.response.v4;

import lombok.Data;

import java.util.List;

@Data
public class V4MySchedulePageResp {
    private V4MyScheduleSummaryResp summary;
    private List<V4MyScheduleResp> items;
}
