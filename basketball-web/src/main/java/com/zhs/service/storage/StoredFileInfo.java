package com.zhs.service.storage;

import lombok.Data;

@Data
public class StoredFileInfo {

    private String storageProvider;

    private String bucketName;

    /** 相对 objectKey，如 public/2026/07/24/uuid.jpg */
    private String objectKey;

    private String fileName;

    private String fileExt;

    private String mimeType;

    private long fileSize;

    private String fileHash;

    /** 公开访问相对路径，如 2026/07/24/uuid.jpg */
    private String publicRelativePath;

    /** 直接可访问地址（COS 公开文件时由存储层填充） */
    private String accessUrl;
}
