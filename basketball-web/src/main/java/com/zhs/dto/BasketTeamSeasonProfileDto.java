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
@ApiModel(value = "BasketTeamSeasonProfileDto")
public class BasketTeamSeasonProfileDto implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "球队主键")
        private Long teamId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事赛季主键")
        private Long seasonId;

        @ApiModelProperty(value = "赛季展示名称", required = true)
        private String displayName;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛季球队标志文件主键")
        private Long logoFileId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛季队长球员主键")
        private Long captainPlayerId;

        @ApiModelProperty(value = "赛季报名状态", required = true)
        private Integer registrationStatus;

        @ApiModelProperty(value = "种子编号")
        private Integer seedNo;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "所属分组主键")
        private Long groupId;

        @ApiModelProperty(value = "最终排名")
        private Integer finalRank;

        @ApiModelProperty(value = "赛季状态", required = true)
        private Integer seasonStatus;

        @ApiModelProperty(value = "备注")
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
