package com.zhs.model.v4;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("basket_leaderboard_snapshot")
public class BasketLeaderboardSnapshot implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long runId;
    private Long divisionId;
    private String scopeKey;
    private Long policyId;
    private String sourceRevision;
    private Date publishedAt;
    private Integer isPublic;
    private String publicScopeKey;
    private Date createTime;
}
