package com.zhs.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;
import org.springframework.format.annotation.DateTimeFormat;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.alibaba.fastjson2.JSONObject;


/**
 * 荣誉记录表
 *
 * @author 
 * @since 2026-07-24
 */

@Data
@ApiModel
public class BasketHonorRecord implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛事系列主键")
    private Long seriesId;


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


    @ApiModelProperty(value = "荣誉标题")
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


    @ApiModelProperty(value = "展示状态")
    private Integer displayStatus;

    @ApiModelProperty(value = "展示顺序")
    private Integer displayOrder;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;


    @ApiModelProperty(value = "更新时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;


    @ApiModelProperty(value = "删除标记")
    private Integer isDelete;


//===========================自定义字段=================================

}
