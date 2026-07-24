package com.zhs.response.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("待办任务")
public class PendingTaskResp implements Serializable {

    @ApiModelProperty("任务类型")
    private String taskType;

    @ApiModelProperty("任务标题")
    private String title;

    @ApiModelProperty("任务描述")
    private String description;

    @ApiModelProperty("跳转路径")
    private String actionPath;
}
