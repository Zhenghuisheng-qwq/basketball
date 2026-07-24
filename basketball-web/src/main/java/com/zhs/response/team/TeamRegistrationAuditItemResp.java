package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("赛事报名审核记录项")
public class TeamRegistrationAuditItemResp implements Serializable {

    @ApiModelProperty("审核记录主键")
    private String auditId;

    @ApiModelProperty("审核动作")
    private String auditAction;

    @ApiModelProperty("审核前状态")
    private Integer beforeStatus;

    @ApiModelProperty("审核后状态")
    private Integer afterStatus;

    @ApiModelProperty("审核人名称")
    private String auditorName;

    @ApiModelProperty("审核说明")
    private String auditRemark;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("审核时间")
    private Date auditTime;
}
