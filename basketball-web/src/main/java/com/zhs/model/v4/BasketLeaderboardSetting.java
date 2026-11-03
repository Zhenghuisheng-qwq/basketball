package com.zhs.model.v4;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("basket_leaderboard_setting")
public class BasketLeaderboardSetting implements Serializable {
    @TableId
    private Long id;
    private Long featuredDivisionId;
}
