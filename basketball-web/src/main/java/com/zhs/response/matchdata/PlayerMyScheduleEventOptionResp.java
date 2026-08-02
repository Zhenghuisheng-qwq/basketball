package com.zhs.response.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("我的赛程赛事筛选项")
public class PlayerMyScheduleEventOptionResp implements Serializable {

    private String seasonId;
    private String eventCode;
    private String eventName;
    private String seasonName;
}
