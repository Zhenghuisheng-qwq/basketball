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
 * @since 2026-07-18
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@ApiModel(value = "BasketFileResourceResp")
public class BasketFileResourceResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @ApiModelProperty(value = "文件名称", required = true)
        private String fileName;

        @ApiModelProperty(value = "文件路径", required = true)
        private String filePath;

        @ApiModelProperty(value = "文件地址", required = true)
        private String fileUrl;

        @ApiModelProperty(value = "文件类型")
        private String fileType;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "文件大小")
        private Long fileSize;

        @ApiModelProperty(value = "业务类型")
        private String businessType;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "业务编号")
        private Long businessId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "上传用户编号")
        private Long uploadUserId;

        @ApiModelProperty(value = "上传时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date uploadTime;

        @ApiModelProperty(value = "文件状态", required = true)
        private String fileStatus;

        @ApiModelProperty(value = "创建者")
        private String createBy;

        @ApiModelProperty(value = "创建时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;

        @ApiModelProperty(value = "备注")
        private String remark;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
