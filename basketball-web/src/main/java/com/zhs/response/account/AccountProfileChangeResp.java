package com.zhs.response.account;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("账号公开资料变更申请")
public class AccountProfileChangeResp implements Serializable {

    @ApiModelProperty("变更主键")
    private String changeId;

    @ApiModelProperty("变更编号")
    private String changeNo;

    @ApiModelProperty("原昵称")
    private String oldNickname;

    @ApiModelProperty("新昵称")
    private String newNickname;

    @ApiModelProperty("原头像")
    private String oldAvatarUrl;

    @ApiModelProperty("新头像")
    private String newAvatarUrl;

    @ApiModelProperty("变更摘要")
    private String changeSummary;

    @ApiModelProperty("审核状态码")
    private Integer auditStatus;

    @ApiModelProperty("审核状态文案")
    private String auditStatusName;

    @ApiModelProperty("审核备注")
    private String auditRemark;

    @ApiModelProperty("提交时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date submitTime;

    @ApiModelProperty("审核时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date auditTime;
}
