package com.zhs.request.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("账户资料更新请求")
public class AccountProfileUpdateReq implements Serializable {

    @ApiModelProperty("昵称（2-20 字符）")
    private String nickname;

    @ApiModelProperty("性别：0 未知，1 男，2 女")
    private Integer gender;

    @ApiModelProperty("出生日期，格式 yyyy-MM-dd")
    private String birthday;

    @ApiModelProperty("省份编码")
    private String provinceCode;

    @ApiModelProperty("省份名称")
    private String provinceName;

    @ApiModelProperty("城市编码")
    private String cityCode;

    @ApiModelProperty("城市名称")
    private String cityName;

    @ApiModelProperty("个人简介（最多 500 字）")
    private String bio;
}
