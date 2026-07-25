package com.zhs.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class BasketAnnouncementRuleLink implements Serializable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private Long tournamentId;
    private Long seasonId;
    private Long oldAnnouncementId;
    private Long newAnnouncementId;
    private String ruleVersion;
    private Date effectiveTime;
    private Date invalidTime;
    private Integer currentFlag;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
