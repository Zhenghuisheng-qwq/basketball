package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zhs.common.UserContext;
import com.zhs.config.AccountFileProperties;
import com.zhs.dao.BasketFileAssetDao;
import com.zhs.dao.BasketPlayerCertApplicationDao;
import com.zhs.dao.BasketPlayerCertMaterialDao;
import com.zhs.enums.FileUsageTypeEnum;
import com.zhs.enums.PlayerCertStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketFileAsset;
import com.zhs.model.BasketPlayerCertApplication;
import com.zhs.model.BasketPlayerCertMaterial;
import com.zhs.response.account.FileUploadResp;
import com.zhs.service.IAccountFileService;
import com.zhs.service.account.AccessibleFile;
import com.zhs.service.storage.IAccountFileStorageService;
import com.zhs.service.storage.StoredFileInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
public class AccountFileServiceImpl implements IAccountFileService {

    private static final int FILE_STATUS_ACTIVE = 1;
    private static final Integer NOT_DELETED = 0;
    private static final int DELETED = 1;

    @Resource
    private BasketFileAssetDao fileAssetDao;
    @Resource
    private BasketPlayerCertMaterialDao certMaterialDao;
    @Resource
    private BasketPlayerCertApplicationDao certApplicationDao;
    @Resource
    private IAccountFileStorageService accountFileStorageService;
    @Resource
    private AccountFileProperties accountFileProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResp upload(MultipartFile file, String usageType) {
        FileUsageTypeEnum usage = FileUsageTypeEnum.fromCode(usageType);
        Long ownerMemberId = UserContext.getRequiredUserId();

        StoredFileInfo stored;
        try {
            stored = accountFileStorageService.store(file, usage.isPublicAccess());
        } catch (IOException ex) {
            log.error("文件写入失败", ex);
            throw new ServiceException("文件上传失败");
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
            asset.setAccessUrl(buildInitialAccessUrl(usage, stored));
            if (!StringUtils.hasText(asset.getAccessUrl())) {
                throw new ServiceException("公开文件地址生成失败");
            }
        } else {
            asset.setId(IdWorker.getId());
            asset.setAccessUrl("/basket/account/file/" + asset.getId());
        }

        try {
            fileAssetDao.insert(asset);
        } catch (Exception ex) {
            deleteStoredFileQuietly(stored);
            throw ex;
        }

        FileUploadResp resp = new FileUploadResp();
        resp.setFileId(String.valueOf(asset.getId()));
        resp.setAccessUrl(asset.getAccessUrl());
        resp.setUsageType(usage.getCode());
        resp.setOriginalName(asset.getOriginalName());
        resp.setFileSize(asset.getFileSize());
        return resp;
    }

    @Override
    public AccessibleFile getAccessibleFile(Long fileId) {
        BasketFileAsset asset = requireActiveFile(fileId);
        FileUsageTypeEnum usage = FileUsageTypeEnum.fromCode(asset.getUsageType());

        if (usage.isPublicAccess()) {
            AccessibleFile result = new AccessibleFile();
            result.setAsset(asset);
            result.setRedirectUrl(resolvePublicAccessUrl(asset));
            return result;
        }

        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null || !currentUserId.equals(asset.getOwnerMemberId())) {
            throw new ServiceException("无权访问该文件", 403);
        }

        try {
            AccessibleFile result = new AccessibleFile();
            result.setAsset(asset);
            result.setContent(accountFileStorageService.read(asset));
            return result;
        } catch (IOException ex) {
            log.error("读取文件失败, fileId={}", fileId, ex);
            throw new ServiceException("文件读取失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOwnFile(Long fileId) {
        Long ownerMemberId = UserContext.getRequiredUserId();
        BasketFileAsset asset = requireOwnedFile(fileId, ownerMemberId);
        assertDeletable(fileId);

        LambdaUpdateWrapper<BasketFileAsset> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BasketFileAsset::getId, fileId)
                .set(BasketFileAsset::getIsDelete, DELETED);
        fileAssetDao.update(null, updateWrapper);

        try {
            accountFileStorageService.delete(asset);
        } catch (IOException ex) {
            log.warn("删除物理文件失败, fileId={}", fileId, ex);
        }
    }

    @Override
    public BasketFileAsset requireOwnedFile(Long fileId, Long accountId) {
        BasketFileAsset asset = requireActiveFile(fileId);
        if (accountId == null || !accountId.equals(asset.getOwnerMemberId())) {
            throw new ServiceException("无权操作该文件", 403);
        }
        return asset;
    }

    @Override
    public BasketFileAsset requireOwnedImageFile(Long fileId, Long accountId) {
        BasketFileAsset asset = requireOwnedFile(fileId, accountId);
        if (!FileUsageTypeEnum.ACCOUNT_AVATAR.getCode().equalsIgnoreCase(asset.getUsageType())) {
            throw new ServiceException("文件用途不匹配");
        }
        if (!StringUtils.hasText(asset.getMimeType())
                || !asset.getMimeType().toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new ServiceException("文件类型必须为图片");
        }
        return asset;
    }

    private BasketFileAsset requireActiveFile(Long fileId) {
        if (fileId == null) {
            throw new ServiceException("文件主键不能为空");
        }
        BasketFileAsset asset = fileAssetDao.selectById(fileId);
        if (asset == null || !Integer.valueOf(NOT_DELETED).equals(asset.getIsDelete())
                || !Integer.valueOf(FILE_STATUS_ACTIVE).equals(asset.getFileStatus())) {
            throw new ServiceException("文件不存在");
        }
        return asset;
    }

    private void assertDeletable(Long fileId) {
        LambdaQueryWrapper<BasketPlayerCertMaterial> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasketPlayerCertMaterial::getFileId, fileId)
                .eq(BasketPlayerCertMaterial::getIsDelete, NOT_DELETED);
        List<BasketPlayerCertMaterial> materials = certMaterialDao.selectList(wrapper);
        for (BasketPlayerCertMaterial material : materials) {
            BasketPlayerCertApplication application = certApplicationDao.selectById(material.getApplicationId());
            if (application == null || Integer.valueOf(DELETED).equals(application.getIsDelete())) {
                continue;
            }
            PlayerCertStatusEnum status = PlayerCertStatusEnum.fromCode(application.getApplicationStatus());
            if (status == PlayerCertStatusEnum.SUBMITTED
                    || status == PlayerCertStatusEnum.REVIEWING
                    || status == PlayerCertStatusEnum.APPROVED) {
                throw new ServiceException("文件已绑定认证材料，无法删除");
            }
        }
    }

    private String buildInitialAccessUrl(FileUsageTypeEnum usage, StoredFileInfo stored) {
        if (StringUtils.hasText(stored.getAccessUrl())) {
            return stored.getAccessUrl();
        }
        if (usage.isPublicAccess()) {
            return resolvePublicUrl(stored.getPublicRelativePath());
        }
        return null;
    }

    private String resolvePublicAccessUrl(BasketFileAsset asset) {
        if (StringUtils.hasText(asset.getAccessUrl())) {
            return asset.getAccessUrl();
        }
        if (StringUtils.hasText(asset.getObjectKey()) && asset.getObjectKey().startsWith("public/")) {
            String relative = asset.getObjectKey().substring("public/".length());
            return resolvePublicUrl(relative);
        }
        throw new ServiceException("公开文件地址无效");
    }

    private String resolvePublicUrl(String relativePath) {
        String path = "/files/public/" + relativePath;
        String baseUrl = accountFileProperties.getPublicBaseUrl();
        if (StringUtils.hasText(baseUrl)) {
            String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            return normalizedBase + path;
        }
        return path;
    }

    private String resolveOriginalName(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalName)) {
            return file.getName();
        }
        return originalName.replace("\\", "/").substring(originalName.replace("\\", "/").lastIndexOf('/') + 1);
    }

    private void deleteStoredFileQuietly(StoredFileInfo stored) {
        BasketFileAsset temp = new BasketFileAsset();
        temp.setObjectKey(stored.getObjectKey());
        try {
            accountFileStorageService.delete(temp);
        } catch (IOException ex) {
            log.warn("回滚物理文件失败, objectKey={}", stored.getObjectKey(), ex);
        }
    }
}
