package com.zhs.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("basket_team_membership_change")
public class BasketTeamMembershipChange implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String changeNo;
    private Long teamId;
    private Long divisionId;
    private Long registrationId;
    private String changeType;
    private Long subjectPlayerId;
    private Long subjectMemberId;
    private Long initiatorMemberId;
    private String initiatorRole;
    private String sourceBizType;
    private Long sourceBizId;
    private String beforeSummary;
    private String afterSummary;
    private String changeStatus;
    private String effectStage;
    private String impactSummary;
    private Long auditUserId;
    private String auditUserName;
    private String auditRemark;
    private Date auditTime;
    private Date effectiveTime;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
