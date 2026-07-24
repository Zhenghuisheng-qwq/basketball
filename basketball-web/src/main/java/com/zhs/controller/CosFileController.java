package com.zhs.controller;

import com.zhs.common.UserContext;
import com.zhs.dao.BasketFileAssetDao;
import com.zhs.enums.FileUsageTypeEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketFileAsset;
import com.zhs.response.account.FileUploadResp;
import com.zhs.service.storage.StoredFileInfo;
import com.zhs.service.storage.TencentCosUploadService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 官网侧腾讯云 COS 上传接口（独立于 account.file.storage-type）。
 */
@RestController
@RequestMapping("/basket/cos/file")
@Api(value = "腾讯云COS文件")
@Slf4j
public class CosFileController {

    private static final int FILE_STATUS_ACTIVE = 1;
    private static final Integer NOT_DELETED = 0;

    @Resource
    private TencentCosUploadService tencentCosUploadService;
    @Resource
    private BasketFileAssetDao fileAssetDao;

    @ApiOperation("COS 上传文件")
    @PostMapping("/upload")
    @Transactional(rollbackFor = Exception.class)
    public R<FileUploadResp> upload(@RequestParam("file") MultipartFile file,
                                    @ApiParam("文件用途") @RequestParam("usageType") String usageType) {
        FileUsageTypeEnum usage = FileUsageTypeEnum.fromCode(usageType);
        Long ownerMemberId = UserContext.getRequiredUserId();

        StoredFileInfo stored;
        try {
            stored = tencentCosUploadService.upload(file, usage.isPublicAccess() ? "public" : "private");
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("COS 上传失败", ex);
            throw new ServiceException("COS 文件上传失败: " + ex.getMessage());
        }

        BasketFileAsset asset = new BasketFileAsset();
        asset.setStorageProvider(stored.getStorageProvider());
        asset.setBucketName(stored.getBucketName());
        asset.setObjectKey(stored.getObjectKey());
        asset.setFileName(stored.getFileName());
        asset.setOriginalName(resolveOriginalName(file));
        asset.setFileExt(stored.getFileExt());
        asset.setMimeType(stored.getMimeType());
        asset.setFileSize(stored.getFileSize());
        asset.setFileHash(stored.getFileHash());
        asset.setUsageType(usage.getCode());
        asset.setOwnerMemberId(ownerMemberId);
        asset.setFileStatus(FILE_STATUS_ACTIVE);
        asset.setIsDelete(NOT_DELETED);
        if (usage.isPublicAccess()) {
            asset.setAccessUrl(stored.getAccessUrl());
        }

        try {
            fileAssetDao.insert(asset);
        } catch (Exception ex) {
            try {
                tencentCosUploadService.delete(stored.getBucketName(), stored.getObjectKey());
            } catch (Exception deleteEx) {
                log.warn("回滚 COS 文件失败, objectKey={}", stored.getObjectKey(), deleteEx);
            }
            throw ex;
        }

        if (!usage.isPublicAccess()) {
            asset.setAccessUrl("/basket/account/file/" + asset.getId());
            fileAssetDao.updateById(asset);
        }

        FileUploadResp resp = new FileUploadResp();
        resp.setFileId(String.valueOf(asset.getId()));
        resp.setAccessUrl(asset.getAccessUrl());
        resp.setUsageType(usage.getCode());
        resp.setOriginalName(asset.getOriginalName());
        resp.setFileSize(asset.getFileSize());
        return R.ok(resp, "上传成功");
    }

    private String resolveOriginalName(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalName)) {
            return file.getName();
        }
        String normalized = originalName.replace("\\", "/");
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }
}
