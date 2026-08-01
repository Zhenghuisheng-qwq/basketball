package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("提醒队员补充平台游戏 ID 结果")
public class TeamRegistrationRemindPlatformResp implements Serializable {

    @ApiModelProperty("是否成功")
    private Boolean success;

    @ApiModelProperty("提示信息")
    private String message;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("上次提醒时间")
    private Date lastRemindTime;
}
