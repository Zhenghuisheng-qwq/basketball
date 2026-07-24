package com.zhs.response.publicapi;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("公开公告列表项（不含正文）")
public class PublicAnnouncementItemResp implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("公告主键")
    private Long announcementId;

    @ApiModelProperty("公告编号")
    private String announcementNo;

    @ApiModelProperty("分类编码")
    private String categoryCode;

    @ApiModelProperty("分类名称")
    private String categoryName;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("摘要")
    private String summary;

    @ApiModelProperty("封面 URL")
    private String coverUrl;

    @ApiModelProperty("作者名称")
    private String authorName;

    @ApiModelProperty("置顶标记")
    private Integer isTop;

    @ApiModelProperty("重要标记")
    private Integer isImportant;

    @ApiModelProperty("发布时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date publishTime;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("浏览次数")
    private Long viewCount;
}
