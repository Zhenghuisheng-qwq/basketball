package com.zhs.model.v4;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 赛区最终名单版本。锁定后的当前版本才是正式参赛名单。
 */
@Data
@TableName("basket_division_roster_version")
public class BasketDivisionRosterVersion implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long divisionId;

    private String status;

    private Integer isCurrent;

    private Integer isDelete;
}
