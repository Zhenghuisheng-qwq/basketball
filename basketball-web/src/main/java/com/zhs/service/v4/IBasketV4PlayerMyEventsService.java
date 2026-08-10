package com.zhs.service.v4;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.response.v4.V4MyAvailableEventItemResp;
import com.zhs.response.v4.V4MyEventResp;

import java.util.List;

public interface IBasketV4PlayerMyEventsService {
    List<V4MyEventResp> listMyEvents();

    IPage<V4MyEventResp> pageMyEvents(Integer currentPage, Integer pageSize);

    IPage<V4MyAvailableEventItemResp> pageAvailableEvents(Integer currentPage, Integer pageSize);
}
