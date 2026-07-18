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
@ApiModel(value = "BasketRenameApplyDetailResp")
public class BasketRenameApplyDetailResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "球队编号")
        private Long teamId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "队长球员编号")
        private Long captainPlayerId;

        @ApiModelProperty(value = "原球队名称", required = true)
        private String oldTeamName;

        @ApiModelProperty(value = "新球队名称", required = true)
        private String newTeamName;

        @ApiModelProperty(value = "更名原因")
        private String changeReason;

        @ApiModelProperty(value = "申请时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date applyTime;

        @ApiModelProperty(value = "审核状态", required = true)
        private String auditStatus;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "审核用户编号")
        private Long auditorUserId;

        @ApiModelProperty(value = "审核时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date auditTime;

        @ApiModelProperty(value = "审核备注")
        private String auditRemark;

        @ApiModelProperty(value = "创建者")
        private String createBy;

        @ApiModelProperty(value = "创建时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;

        @ApiModelProperty(value = "更新者")
        private String updateBy;

        @ApiModelProperty(value = "更新时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date updateTime;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
