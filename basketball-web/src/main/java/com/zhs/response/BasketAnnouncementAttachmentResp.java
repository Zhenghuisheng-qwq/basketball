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
@ApiModel(value = "BasketAnnouncementAttachmentResp")
public class BasketAnnouncementAttachmentResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "公告主键")
        private Long announcementId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "文件主键")
        private Long fileId;

        @ApiModelProperty(value = "附件名称", required = true)
        private String attachmentName;

        @ApiModelProperty(value = "排序号", required = true)
        private Integer sortNo;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "下载次数")
        private Long downloadCount;

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
