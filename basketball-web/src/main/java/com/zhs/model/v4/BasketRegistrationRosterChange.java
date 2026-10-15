package com.zhs.model.v4;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("basket_registration_roster_change")
public class BasketRegistrationRosterChange {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long registrationId;
    private Long divisionId;
    private Long seasonId;
    private Long teamId;
    private String changeStatus;
    private Integer baseLineupVersion;
    private Long submitMemberId;
    private Long auditUserId;
    private String auditRemark;
    private Date auditTime;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
