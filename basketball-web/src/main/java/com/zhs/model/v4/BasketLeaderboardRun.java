package com.zhs.model.v4;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("basket_leaderboard_run")
public class BasketLeaderboardRun implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long divisionId;
    private String scopeKey;
    private String state;
    private Date finishedTime;
}
