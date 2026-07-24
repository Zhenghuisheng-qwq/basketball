package com.zhs.service;

import com.zhs.model.BasketFileAsset;
import com.zhs.response.account.FileUploadResp;
import com.zhs.service.account.AccessibleFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * 账户文件业务服务
 */
public interface IAccountFileService {

    FileUploadResp upload(MultipartFile file, String usageType);

    AccessibleFile getAccessibleFile(Long fileId);

    void deleteOwnFile(Long fileId);

    BasketFileAsset requireOwnedFile(Long fileId, Long accountId);

    BasketFileAsset requireOwnedImageFile(Long fileId, Long accountId);
}
