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
 * @since 2026-07-18
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@ApiModel(value = "BasketTournamentTeamDto")
public class BasketTournamentTeamDto implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事编号")
        private Long tournamentId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "报名编号")
        private Long registrationId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "来源球队编号")
        private Long sourceTeamId;

        @ApiModelProperty(value = "球队编号", required = true)
        private String teamNo;

        @ApiModelProperty(value = "球队名称", required = true)
        private String teamName;

        @ApiModelProperty(value = "球队标志")
        private String logoUrl;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "队长球员编号")
        private Long captainPlayerId;

        @ApiModelProperty(value = "比赛终端", required = true)
        private String terminalType;

        @ApiModelProperty(value = "成员数量", required = true)
        private Integer memberCount;

        @ApiModelProperty(value = "球队状态", required = true)
        private String teamStatus;

        @ApiModelProperty(value = "创建者")
        private String createBy;

        @ApiModelProperty(value = "创建时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;

        @ApiModelProperty(value = "备注")
        private String remark;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
