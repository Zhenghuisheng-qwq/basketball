package com.zhs.service;

import com.zhs.request.matchdata.PlayerMyScheduleQuery;
import com.zhs.response.matchdata.PlayerMyScheduleEventOptionResp;
import com.zhs.response.matchdata.PlayerMySchedulePageResp;

import java.util.List;

public interface IPlayerMyScheduleService {

    PlayerMySchedulePageResp pageMySchedules(PlayerMyScheduleQuery query);

    List<PlayerMyScheduleEventOptionResp> listEventOptions();
}
