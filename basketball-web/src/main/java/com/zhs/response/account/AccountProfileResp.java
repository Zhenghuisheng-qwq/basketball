package com.zhs.response.account;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("账户资料响应")
public class AccountProfileResp implements Serializable {

    @ApiModelProperty("账号主键")
    private String accountId;

    @ApiModelProperty("账号编号")
    private String accountNo;

    @ApiModelProperty("昵称")
    private String nickname;

    @ApiModelProperty("头像 URL")
    private String avatarUrl;

    @ApiModelProperty("脱敏邮箱")
    private String emailMasked;

    @ApiModelProperty("邮箱是否已验证")
    private Boolean emailVerified;

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

    @ApiModelProperty("个人简介")
    private String bio;

    @ApiModelProperty("注册时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date registerTime;

    @ApiModelProperty("最后登录时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastLoginTime;
}
