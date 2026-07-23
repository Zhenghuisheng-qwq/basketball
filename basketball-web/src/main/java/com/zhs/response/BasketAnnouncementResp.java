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
 * @since 2026-07-24
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@ApiModel(value = "BasketAnnouncementResp")
public class BasketAnnouncementResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @ApiModelProperty(value = "公告编号", required = true)
        private String announcementNo;

        @ApiModelProperty(value = "公告分类", required = true)
        private String categoryCode;

        @ApiModelProperty(value = "公告标题", required = true)
        private String title;

        @ApiModelProperty(value = "公告摘要")
        private String summary;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "封面文件主键")
        private Long coverFileId;

        @ApiModelProperty(value = "公告正文", required = true)
        private Object contentText;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "发布人主键")
        private Long authorId;

        @ApiModelProperty(value = "发布人名称")
        private String authorName;

        @ApiModelProperty(value = "公告状态", required = true)
        private Integer announcementStatus;

        @ApiModelProperty(value = "公开范围", required = true)
        private String publicScope;

        @ApiModelProperty(value = "置顶标记", required = true)
        private Integer isTop;

        @ApiModelProperty(value = "重要标记", required = true)
        private Integer isImportant;

        @ApiModelProperty(value = "发布时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date publishTime;

        @ApiModelProperty(value = "下线时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date offlineTime;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "浏览次数")
        private Long viewCount;

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
