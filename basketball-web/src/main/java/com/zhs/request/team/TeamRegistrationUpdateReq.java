package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("更新赛事报名请求")
public class TeamRegistrationUpdateReq implements Serializable {

    @ApiModelProperty("联系人")
    private String contactName;

    @ApiModelProperty("联系电话")
    private String contactMobile;

    @ApiModelProperty("联系邮箱")
    private String contactEmail;

    @ApiModelProperty("备注")
    private String remark;
}
