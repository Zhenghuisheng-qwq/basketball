package com.zhs.response.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("我的赛程平台筛选项")
public class PlayerMySchedulePlatformOptionResp implements Serializable {

    private String platformCode;
    private String platformName;
}
