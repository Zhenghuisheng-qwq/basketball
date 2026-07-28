package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("创建赛事报名草稿请求")
public class TeamRegistrationDraftReq implements Serializable {

    @ApiModelProperty("赛季主键")
    private String seasonId;

    @ApiModelProperty("报名平台 PC/PS/XBOX")
    private String platformCode;

    @ApiModelProperty("联系人")
    private String contactName;

    @ApiModelProperty("联系电话")
    private String contactMobile;

    @ApiModelProperty("联系邮箱")
    private String contactEmail;

    @ApiModelProperty("备注")
    private String remark;
}
