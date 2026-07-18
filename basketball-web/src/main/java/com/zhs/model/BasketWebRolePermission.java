package com.zhs.model;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;


/**
 * 网页角色权限关联表
 *
 * @author 
 * @since 2026-07-18
 */

@Data
@ApiModel
public class BasketWebRolePermission implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "role_id", type = IdType.INPUT)
    @ApiModelProperty(value = "角色编号")
    private Long id;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "权限编号")
    private Long permissionId;


    @ApiModelProperty(value = "逻辑删除：0未删 1已删")
    private Integer isDelete;


//===========================自定义字段=================================

}
