package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("球队资料变更详情")
public class TeamProfileChangeDetailResp implements Serializable {

    private String changeId;
    private String changeNo;
    private String teamId;

    private String oldTeamName;
    private String newTeamName;
    private String oldTeamShortName;
    private String newTeamShortName;
    private String oldLogoUrl;
    private String newLogoUrl;
    private String oldDescription;
    private String newDescription;
    private Integer oldPublicStatus;
    private Integer newPublicStatus;

    private String changeReason;
    private Integer auditStatus;
    private String auditStatusText;
    private String auditRemark;

    private String submitUserId;
    private String submitUserName;
    private String submitPlayerNo;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date submitTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date auditTime;

    private String changeSummary;
}
