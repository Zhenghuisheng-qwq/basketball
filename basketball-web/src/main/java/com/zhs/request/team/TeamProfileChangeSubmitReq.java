package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("提交球队资料变更申请")
public class TeamProfileChangeSubmitReq implements Serializable {

    @ApiModelProperty("球队名称")
    private String teamName;

    @ApiModelProperty("球队简称")
    private String teamShortName;

    @ApiModelProperty("Logo 文件 ID")
    private String logoFileId;

    @ApiModelProperty("球队介绍")
    private String description;

    @ApiModelProperty("变更说明")
    private String changeReason;
}
