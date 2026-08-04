package com.zhs.model.v4;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("basket_personal_draw_assignment")
public class BasketPersonalDrawAssignment implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long drawBatchId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long drawTeamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long personalRegistrationId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long playerId;

    private String assignmentRole;
    private String joinStatus;
    private Integer sortOrder;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
