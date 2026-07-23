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
 * @since 2026-07-24
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@ApiModel(value = "BasketSiteConfigResp")
public class BasketSiteConfigResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @ApiModelProperty(value = "配置键", required = true)
        private String configKey;

        @ApiModelProperty(value = "配置名称", required = true)
        private String configName;

        @ApiModelProperty(value = "配置值")
        private Object configValue;

        @ApiModelProperty(value = "值类型", required = true)
        private String valueType;

        @ApiModelProperty(value = "配置分组", required = true)
        private String groupCode;

        @ApiModelProperty(value = "公开标记", required = true)
        private Integer publicFlag;

        @ApiModelProperty(value = "配置状态", required = true)
        private Integer configStatus;

        @ApiModelProperty(value = "备注")
        private String remark;

        @ApiModelProperty(value = "创建时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;

        @ApiModelProperty(value = "更新时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date updateTime;

        @ApiModelProperty(value = "删除标记", required = true)
        private Integer isDelete;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
