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
@ApiModel(value = "BasketStatChangeRecordResp")
public class BasketStatChangeRecordResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @ApiModelProperty(value = "业务类型", required = true)
        private String bizType;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "业务主键")
        private Long bizId;

        @ApiModelProperty(value = "变更字段", required = true)
        private String fieldName;

        @ApiModelProperty(value = "变更前值")
        private Object beforeValue;

        @ApiModelProperty(value = "变更后值")
        private Object afterValue;

        @ApiModelProperty(value = "变更原因", required = true)
        private String changeReason;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "操作人主键")
        private Long operatorId;

        @ApiModelProperty(value = "操作人名称")
        private String operatorName;

        @ApiModelProperty(value = "审批状态", required = true)
        private Integer approvalStatus;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "审批人主键")
        private Long approvedBy;

        @ApiModelProperty(value = "审批时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date approvedTime;

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
