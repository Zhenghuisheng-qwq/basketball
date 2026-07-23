package com.zhs.dto;

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
@ApiModel(value = "BasketTournamentRegistrationPlayerDto")
public class BasketTournamentRegistrationPlayerDto implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事报名主键")
        private Long registrationId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事赛季主键")
        private Long seasonId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "球队主键")
        private Long teamId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "球员主键")
        private Long playerId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "会员主键")
        private Long memberId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "球队成员主键")
        private Long teamMemberId;

        @ApiModelProperty(value = "报名阵容角色", required = true)
        private String rosterRole;

        @ApiModelProperty(value = "球衣号码")
        private String jerseyNo;

        @ApiModelProperty(value = "首发标记", required = true)
        private Integer isStarting;

        @ApiModelProperty(value = "场上队长标记", required = true)
        private Integer isCaptain;

        @ApiModelProperty(value = "资格校验状态", required = true)
        private Integer validationStatus;

        @ApiModelProperty(value = "不通过原因")
        private String rejectReason;

        @ApiModelProperty(value = "排序号", required = true)
        private Integer sortNo;

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
