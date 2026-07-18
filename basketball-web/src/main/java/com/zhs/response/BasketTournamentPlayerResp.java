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
@ApiModel(value = "BasketTournamentPlayerResp")
public class BasketTournamentPlayerResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事编号")
        private Long tournamentId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事球队编号")
        private Long tournamentTeamId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "来源球员编号")
        private Long sourcePlayerId;

        @ApiModelProperty(value = "球员编号", required = true)
        private String playerNo;

        @ApiModelProperty(value = "球员名称", required = true)
        private String playerName;

        @ApiModelProperty(value = "头像地址")
        private String avatarUrl;

        @ApiModelProperty(value = "游戏账号")
        private String gameAccount;

        @ApiModelProperty(value = "游戏昵称")
        private String gameRoleName;

        @ApiModelProperty(value = "球衣号码")
        private Integer jerseyNo;

        @ApiModelProperty(value = "场上位置")
        private String positionName;

        @ApiModelProperty(value = "首发标志", required = true)
        private String starterFlag;

        @ApiModelProperty(value = "队长标志", required = true)
        private String captainFlag;

        @ApiModelProperty(value = "球员状态", required = true)
        private String playerStatus;

        @ApiModelProperty(value = "创建者")
        private String createBy;

        @ApiModelProperty(value = "创建时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
