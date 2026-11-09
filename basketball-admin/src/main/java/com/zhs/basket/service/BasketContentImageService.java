package com.zhs.basket.service;

import com.zhs.basket.domain.BasketFileAsset;
import com.zhs.basket.mapper.BasketFileAssetMapper;
import com.zhs.basket.resp.ContentImageResp;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;
import com.zhs.framework.cos.TencentCosUploadService;
import com.zhs.framework.cos.TencentCosUploadService.CosUploadResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

/**
 * 内容图上传。写入 basket_file_asset，调用方只保存返回的文件编号。
 */
@Service
public class BasketContentImageService {

    private static final String USAGE_DEFAULT = "CONTENT_IMAGE";
    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif", "bmp");

    @Autowired private TencentCosUploadService cosUploadService;
    @Autowired private BasketFileAssetMapper fileAssetMapper;

    public ContentImageResp upload(MultipartFile file, String usageType) {
        String ext = extension(file);
        if (!IMAGE_EXT.contains(ext)) {
            throw new ServiceException("只支持 jpg、png、webp、gif、bmp");
        }
        CosUploadResult stored = cosUploadService.upload(file, "content");
        BasketFileAsset asset = new BasketFileAsset();
        asset.setStorageProvider("cos");
        asset.setBucketName(stored.getBucketName());
        asset.setObjectKey(stored.getObjectKey());
        asset.setFileName(stored.getFileName());
        asset.setOriginalName(stored.getOriginalFilename());
        asset.setFileExt(stored.getFileExt());
        asset.setMimeType(StringUtils.isEmpty(file.getContentType()) ? "image/" + ext : file.getContentType());
        asset.setFileSize(stored.getFileSize());
        asset.setAccessUrl(stored.getUrl());
        asset.setUsageType(normalizeUsage(usageType));
        asset.setFileStatus(1);
        asset.setIsDelete(0);
        if (fileAssetMapper.insert(asset) != 1 || asset.getId() == null) {
            throw new ServiceException("图片保存失败");
        }
        ContentImageResp resp = new ContentImageResp();
        resp.setFileId(asset.getId());
        resp.setAccessUrl(asset.getAccessUrl());
        resp.setOriginalName(asset.getOriginalName());
        return resp;
    }

    private String normalizeUsage(String usageType) {
        String usage = StringUtils.isEmpty(usageType) ? USAGE_DEFAULT : usageType.trim().toUpperCase(Locale.ROOT);
        if (!usage.matches("[A-Z0-9_]{1,64}")) {
            throw new ServiceException("文件用途不正确");
        }
        return usage;
    }

    private String extension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择图片");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.contains(".")) {
            throw new ServiceException("无法识别图片格式");
        }
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
