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
@ApiModel(value = "BasketTournamentRegistrationDetailResp")
public class BasketTournamentRegistrationDetailResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @ApiModelProperty(value = "报名编号", required = true)
        private String registrationNo;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事赛季主键")
        private Long seasonId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "报名球队主键")
        private Long teamId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "报名队长会员主键")
        private Long captainMemberId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "报名队长球员主键")
        private Long captainPlayerId;

        @ApiModelProperty(value = "联系人", required = true)
        private String contactName;

        @ApiModelProperty(value = "联系电话", required = true)
        private String contactMobile;

        @ApiModelProperty(value = "联系邮箱")
        private String contactEmail;

        @ApiModelProperty(value = "报名状态", required = true)
        private Integer registrationStatus;

        @ApiModelProperty(value = "报名阵容人数", required = true)
        private Integer lineupCount;

        @ApiModelProperty(value = "提交时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date submittedTime;

        @ApiModelProperty(value = "审核完成时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date auditTime;

        @ApiModelProperty(value = "撤回时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date withdrawalTime;

        @ApiModelProperty(value = "撤回原因")
        private String withdrawalReason;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "当前审核人主键")
        private Long currentAuditorId;

        @ApiModelProperty(value = "报名备注")
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
