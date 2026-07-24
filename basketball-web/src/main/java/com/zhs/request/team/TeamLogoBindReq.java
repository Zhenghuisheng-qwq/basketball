package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("球队 Logo 绑定请求")
public class TeamLogoBindReq implements Serializable {

    @ApiModelProperty("Logo 文件主键")
    private String fileId;
}
