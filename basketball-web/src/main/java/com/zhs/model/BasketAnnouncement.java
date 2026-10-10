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
 * 公告表
 *
 * @author 
 * @since 2026-07-24
 */

@Data
@ApiModel
public class BasketAnnouncement implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @ApiModelProperty(value = "公告编号")
    private String announcementNo;

    @ApiModelProperty(value = "公告分类")
    private String categoryCode;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "关联赛事主键")
    private Long tournamentId;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "关联赛季主键")
    private Long seasonId;

    @ApiModelProperty(value = "公告标题")
    private String title;

    @ApiModelProperty(value = "英文公告标题")
    private String titleEn;

    @ApiModelProperty(value = "公告摘要")
    private String summary;

    @ApiModelProperty(value = "英文公告摘要")
    private String summaryEn;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "封面文件主键")
    private Long coverFileId;


    @ApiModelProperty(value = "公告正文")
    private Object contentText;

    @ApiModelProperty(value = "英文公告正文")
    private String contentTextEn;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "发布人主键")
    private Long authorId;


    @ApiModelProperty(value = "发布人名称")
    private String authorName;

    @ApiModelProperty(value = "公告状态")
    private Integer announcementStatus;

    @ApiModelProperty(value = "公开范围")
    private String publicScope;

    @ApiModelProperty(value = "置顶标记")
    private Integer isTop;

    @ApiModelProperty(value = "重要标记")
    private Integer isImportant;

    @ApiModelProperty(value = "发布时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date publishTime;

    @ApiModelProperty(value = "定时发布时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date scheduledPublishTime;

    @ApiModelProperty(value = "下线时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date offlineTime;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "被替代的新公告主键")
    private Long supersededBy;

    @ApiModelProperty(value = "规则版本号")
    private String ruleVersion;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "浏览次数")
    private Long viewCount;


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
