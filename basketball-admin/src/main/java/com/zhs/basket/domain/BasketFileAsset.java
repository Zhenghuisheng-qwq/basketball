package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketFileAsset {
    private Long id;
    private String storageProvider;
    private String bucketName;
    private String objectKey;
    private String fileName;
    private String originalName;
    private String fileExt;
    private String mimeType;
    private Long fileSize;
    private String fileHash;
    private String accessUrl;
    private String usageType;
    private Long ownerMemberId;
    private Integer fileStatus;
    private String remark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
