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
@ApiModel(value = "BasketPlayerProfileDto")
public class BasketPlayerProfileDto implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "会员主键")
        private Long memberId;

        @ApiModelProperty(value = "永久球员编号")
        private String playerNo;

        @ApiModelProperty(value = "比赛终端")
        private String gameTerminal;

        @ApiModelProperty(value = "游戏账号")
        private String gameAccount;

        @ApiModelProperty(value = "游戏昵称")
        private String gameNickname;

        @ApiModelProperty(value = "场上位置")
        private String positionCode;

        @ApiModelProperty(value = "惯用手")
        private String dominantHand;

        @ApiModelProperty(value = "身高")
        private java.math.BigDecimal heightCm;

        @ApiModelProperty(value = "体重")
        private java.math.BigDecimal weightKg;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "当前球队主键")
        private Long currentTeamId;

        @ApiModelProperty(value = "认证状态", required = true)
        private Integer certificationStatus;

        @ApiModelProperty(value = "球员状态", required = true)
        private Integer playerStatus;

        @ApiModelProperty(value = "认证通过时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date certifiedTime;

        @ApiModelProperty(value = "档案公开状态", required = true)
        private Integer profileVisible;

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
