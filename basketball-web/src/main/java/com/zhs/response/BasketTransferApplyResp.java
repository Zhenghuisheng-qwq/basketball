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
@ApiModel(value = "BasketTransferApplyResp")
public class BasketTransferApplyResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "球员编号")
        private Long playerId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "原球队编号")
        private Long fromTeamId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "新球队编号")
        private Long toTeamId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "原队长球员编号")
        private Long fromCaptainPlayerId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "新队长球员编号")
        private Long toCaptainPlayerId;

        @ApiModelProperty(value = "转会原因")
        private String transferReason;

        @ApiModelProperty(value = "申请时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date applyTime;

        @ApiModelProperty(value = "原队确认状态", required = true)
        private String fromConfirmStatus;

        @ApiModelProperty(value = "原队确认时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date fromConfirmTime;

        @ApiModelProperty(value = "新队确认状态", required = true)
        private String toConfirmStatus;

        @ApiModelProperty(value = "新队确认时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date toConfirmTime;

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
