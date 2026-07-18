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
@ApiModel(value = "BasketMatchTeamResp")
public class BasketMatchTeamResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "比赛编号")
        private Long matchId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事编号")
        private Long tournamentId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事球队编号")
        private Long tournamentTeamId;

        @ApiModelProperty(value = "球队方向", required = true)
        private String teamSide;

        @ApiModelProperty(value = "球队名称", required = true)
        private String teamName;

        @ApiModelProperty(value = "得分", required = true)
        private Integer score;

        @ApiModelProperty(value = "胜利标志", required = true)
        private String winFlag;

        @ApiModelProperty(value = "弃权标志", required = true)
        private String forfeitFlag;

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
