package com.zhs.response.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("账户文件上传响应")
public class FileUploadResp implements Serializable {

    @ApiModelProperty("文件主键")
    private String fileId;

    @ApiModelProperty("访问地址")
    private String accessUrl;

    @ApiModelProperty("业务用途")
    private String usageType;

    @ApiModelProperty("原始文件名")
    private String originalName;

    @ApiModelProperty("文件大小")
    private Long fileSize;
}
