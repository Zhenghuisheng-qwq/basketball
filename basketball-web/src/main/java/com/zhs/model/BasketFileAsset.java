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
 * 文件资源表
 *
 * @author 
 * @since 2026-07-24
 */

@Data
@ApiModel
public class BasketFileAsset implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @ApiModelProperty(value = "存储服务商")
    private String storageProvider;

    @ApiModelProperty(value = "存储桶名称")
    private String bucketName;

    @ApiModelProperty(value = "对象存储键")
    private String objectKey;

    @ApiModelProperty(value = "文件名称")
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

    @ApiModelProperty(value = "访问地址")
    private String accessUrl;

    @ApiModelProperty(value = "业务用途")
    private String usageType;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "所属会员主键")
    private Long ownerMemberId;


    @ApiModelProperty(value = "文件状态")
    private Integer fileStatus;

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
