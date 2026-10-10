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
 * 首页横幅表
 *
 * @author 
 * @since 2026-07-24
 */

@Data
@ApiModel
public class BasketHomeBanner implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @ApiModelProperty(value = "横幅名称")
    private String bannerName;

    @ApiModelProperty(value = "横幅标题")
    private String title;

    @ApiModelProperty(value = "英文横幅标题")
    private String titleEn;

    @ApiModelProperty(value = "横幅副标题")
    private String subtitle;

    @ApiModelProperty(value = "英文横幅副标题")
    private String subtitleEn;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "图片文件主键")
    private Long imageFileId;


    @ApiModelProperty(value = "展示位置")
    private String positionCode;

    @ApiModelProperty(value = "跳转类型")
    private String targetType;

    @ApiModelProperty(value = "跳转值")
    private String targetValue;

    @ApiModelProperty(value = "展示开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;


    @ApiModelProperty(value = "展示结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;


    @ApiModelProperty(value = "横幅状态")
    private Integer bannerStatus;

    @ApiModelProperty(value = "排序号")
    private Integer sortNo;

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
