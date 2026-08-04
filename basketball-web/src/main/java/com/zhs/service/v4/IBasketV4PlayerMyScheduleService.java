package com.zhs.service.v4;

import com.zhs.response.v4.V4MySchedulePageResp;

public interface IBasketV4PlayerMyScheduleService {
    V4MySchedulePageResp listMySchedule(Long seasonId, Long divisionId, String status,
                                        Boolean includeFinished, Boolean onlyPendingSubmit);
}
