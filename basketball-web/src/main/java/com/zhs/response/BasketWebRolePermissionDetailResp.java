package com.zhs.response;

import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.alibaba.fastjson2.JSONObject;

/**
 *
 * @author 
 * @since 2026-07-18
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@ApiModel(value = "BasketWebRolePermissionDetailResp")
public class BasketWebRolePermissionDetailResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "角色编号")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "权限编号")
        private Long permissionId;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
