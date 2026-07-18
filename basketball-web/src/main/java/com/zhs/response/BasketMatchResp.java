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
@ApiModel(value = "BasketMatchResp")
public class BasketMatchResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事编号")
        private Long tournamentId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "分组编号")
        private Long groupId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "轮次编号")
        private Long roundId;

        @ApiModelProperty(value = "比赛编号", required = true)
        private String matchNo;

        @ApiModelProperty(value = "比赛名称")
        private String matchName;

        @ApiModelProperty(value = "比赛时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date matchTime;

        @ApiModelProperty(value = "房间名称")
        private String roomName;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主队编号")
        private Long homeTeamId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "客队编号")
        private Long awayTeamId;

        @ApiModelProperty(value = "主队得分", required = true)
        private Integer homeScore;

        @ApiModelProperty(value = "客队得分", required = true)
        private Integer awayScore;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "胜方球队编号")
        private Long winnerTeamId;

        @ApiModelProperty(value = "比赛状态", required = true)
        private String matchStatus;

        @ApiModelProperty(value = "数据状态", required = true)
        private String dataStatus;

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

        @ApiModelProperty(value = "备注")
        private String remark;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
