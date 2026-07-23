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
@ApiModel(value = "BasketFileAssetResp")
public class BasketFileAssetResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @ApiModelProperty(value = "存储服务商", required = true)
        private String storageProvider;

        @ApiModelProperty(value = "存储桶名称")
        private String bucketName;

        @ApiModelProperty(value = "对象存储键", required = true)
        private String objectKey;

        @ApiModelProperty(value = "文件名称", required = true)
        private String fileName;

        @ApiModelProperty(value = "原始文件名称")
        private String originalName;

        @ApiModelProperty(value = "文件扩展名")
        private String fileExt;

        @ApiModelProperty(value = "文件媒体类型")
        private String mimeType;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "文件大小")
        private Long fileSize;

        @ApiModelProperty(value = "文件哈希")
        private String fileHash;

        @ApiModelProperty(value = "访问地址", required = true)
        private String accessUrl;

        @ApiModelProperty(value = "业务用途", required = true)
        private String usageType;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "所属会员主键")
        private Long ownerMemberId;

        @ApiModelProperty(value = "文件状态", required = true)
        private Integer fileStatus;

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
