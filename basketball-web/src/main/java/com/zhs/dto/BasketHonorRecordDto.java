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
@ApiModel(value = "BasketHonorRecordDto")
public class BasketHonorRecordDto implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事主键")
        private Long tournamentId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事赛季主键")
        private Long seasonId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "荣誉分类主键")
        private Long honorCategoryId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "获奖球队主键")
        private Long teamId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "获奖球员主键")
        private Long playerId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "关联比赛主键")
        private Long matchId;

        @ApiModelProperty(value = "荣誉标题", required = true)
        private String honorTitle;

        @ApiModelProperty(value = "荣誉副标题")
        private String honorSubtitle;

        @ApiModelProperty(value = "荣誉数值")
        private String honorValue;

        @ApiModelProperty(value = "授予日期")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date awardDate;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "荣誉封面文件主键")
        private Long coverFileId;

        @ApiModelProperty(value = "数据来源类型")
        private String sourceType;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "数据来源主键")
        private Long sourceId;

        @ApiModelProperty(value = "展示状态", required = true)
        private Integer displayStatus;

        @ApiModelProperty(value = "展示顺序", required = true)
        private Integer displayOrder;

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
