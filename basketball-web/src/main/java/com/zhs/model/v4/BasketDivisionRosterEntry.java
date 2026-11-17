package com.zhs.model.v4;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 最终名单里的一支球队。EXCLUDED 表示本届不参赛。
 */
@Data
@TableName("basket_division_roster_entry")
public class BasketDivisionRosterEntry implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long rosterVersionId;

    private Long divisionId;

    private Long teamId;

    private String participationRole;

    private Integer isDelete;
}
