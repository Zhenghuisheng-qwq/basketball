package com.zhs.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("平台游戏 ID 有效变更历史")
@TableName("basket_player_platform_identity_history")
public class BasketPlayerPlatformIdentityHistory implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("登录用户主键")
    private Long userId;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("球员档案主键")
    private Long playerId;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("平台游戏身份主键")
    private Long platformIdentityId;

    @ApiModelProperty("平台代码")
    private String platformCode;

    @ApiModelProperty("变更类型")
    private String changeType;

    @ApiModelProperty("变更前游戏 ID")
    private String oldGameId;

    @ApiModelProperty("变更后游戏 ID")
    private String newGameId;

    @ApiModelProperty("生效时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date effectiveTime;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("来源审核记录主键")
    private Long auditRecordId;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("操作人主键")
    private Long operatorId;

    @ApiModelProperty("首次绑定幂等槽位")
    private String initialBindSlot;

    @ApiModelProperty("创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @ApiModelProperty("删除标记")
    private Integer isDelete;
}
