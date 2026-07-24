package com.zhs.service.storage;

import com.zhs.config.AccountFileProperties;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketFileAsset;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * 腾讯云 COS 存储实现。account.file.storage-type=cos 时启用，接管原账户文件上传链路。
 */
@Service
@ConditionalOnProperty(prefix = "account.file", name = "storage-type", havingValue = "cos")
public class CosAccountFileStorageServiceImpl implements IAccountFileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    @Resource
    private AccountFileProperties accountFileProperties;
    @Resource
    private TencentCosUploadService tencentCosUploadService;

    @Override
    public StoredFileInfo store(MultipartFile file, boolean publicAccess) throws IOException {
        validateImageFile(file);
        StoredFileInfo info = tencentCosUploadService.upload(file, publicAccess ? "public" : "private");
        if (!publicAccess) {
            // 私有文件不直接暴露 COS URL，由业务层改写为受控访问地址
            info.setAccessUrl(null);
        }
        return info;
    }

    @Override
    public byte[] read(BasketFileAsset asset) throws IOException {
        if (asset == null) {
            throw new ServiceException("文件路径无效");
        }
        return tencentCosUploadService.read(asset.getBucketName(), asset.getObjectKey());
    }

    @Override
    public void delete(BasketFileAsset asset) {
        if (asset == null) {
            return;
        }
        tencentCosUploadService.delete(asset.getBucketName(), asset.getObjectKey());
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        if (file.getSize() > accountFileProperties.getMaxImageSize()) {
            throw new ServiceException("图片大小超过限制");
        }
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            throw new ServiceException("文件扩展名无效");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new ServiceException("仅支持 jpg/jpeg/png/webp 图片");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new ServiceException("文件类型必须为图片");
        }
    }
}
