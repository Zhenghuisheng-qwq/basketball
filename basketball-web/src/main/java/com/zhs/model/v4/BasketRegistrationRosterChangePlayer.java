package com.zhs.model.v4;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("basket_registration_roster_change_player")
public class BasketRegistrationRosterChangePlayer {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long changeId;
    private Long playerId;
    private Integer isCaptain;
    private Integer isStarter;
    private Integer sortOrder;
    private Date createTime;
    private Integer isDelete;
}
