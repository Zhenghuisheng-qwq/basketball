package com.zhs.dto;

import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 *
 * @author 
 * @since 2026-07-18
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@ApiModel(value = "BasketWebRolePermissionDto")
public class BasketWebRolePermissionDto implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "角色编号")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "权限编号")
        private Long permissionId;

        //=====================自定义字段=====================

        private List<String> idList;
}
