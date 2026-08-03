package com.zhs.service.v4;

import com.zhs.response.v4.V4MyScheduleResp;

import java.util.List;

public interface IBasketV4PlayerMyScheduleService {
    List<V4MyScheduleResp> listMySchedule(Long seasonId, Long divisionId, String status,
                                          Boolean includeFinished, Boolean onlyPendingSubmit);
}
