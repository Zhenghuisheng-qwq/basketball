package com.zhs.response.account;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("账户摘要")
public class AccountSummaryResp implements Serializable {

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

    @ApiModelProperty("账号状态")
    private Integer accountStatus;

    @ApiModelProperty("注册时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date registerTime;

    @ApiModelProperty("最后登录时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastLoginTime;
}
