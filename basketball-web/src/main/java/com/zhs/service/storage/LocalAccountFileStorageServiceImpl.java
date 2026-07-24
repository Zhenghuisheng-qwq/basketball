package com.zhs.service.storage;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.zhs.config.AccountFileProperties;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketFileAsset;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

@Service
public class LocalAccountFileStorageServiceImpl implements IAccountFileStorageService {

    private static final String STORAGE_PROVIDER = "local";
    private static final String BUCKET_NAME = "local";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    @Resource
    private AccountFileProperties accountFileProperties;

    @Override
    public StoredFileInfo store(MultipartFile file, boolean publicAccess) throws IOException {
        validateImageFile(file);

        String ext = resolveExtension(file.getOriginalFilename());
        String datePath = DateUtil.format(new Date(), "yyyy/MM/dd");
        String storedName = IdUtil.fastSimpleUUID() + "." + ext;
        String scope = publicAccess ? "public" : "private";
        String objectKey = scope + "/" + datePath + "/" + storedName;

        Path root = resolveRootPath();
        Path target = root.resolve(objectKey).normalize();
        assertNoPathTraversal(root, target);

        Files.createDirectories(target.getParent());
        file.transferTo(target.toFile());

        byte[] bytes = Files.readAllBytes(target);
        StoredFileInfo info = new StoredFileInfo();
        info.setStorageProvider(STORAGE_PROVIDER);
        info.setBucketName(BUCKET_NAME);
        info.setObjectKey(objectKey);
        info.setFileName(storedName);
        info.setFileExt(ext);
        info.setMimeType(file.getContentType());
        info.setFileSize(file.getSize());
        info.setFileHash(DigestUtil.sha256Hex(bytes));
        if (publicAccess) {
            info.setPublicRelativePath(datePath + "/" + storedName);
        }
        return info;
    }

    @Override
    public byte[] read(BasketFileAsset asset) throws IOException {
        Path filePath = resolveAssetPath(asset);
        if (!Files.exists(filePath)) {
            throw new ServiceException("文件不存在");
        }
        return Files.readAllBytes(filePath);
    }

    @Override
    public void delete(BasketFileAsset asset) throws IOException {
        Path filePath = resolveAssetPath(asset);
        Files.deleteIfExists(filePath);
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        if (file.getSize() > accountFileProperties.getMaxImageSize()) {
            throw new ServiceException("图片大小超过限制");
        }
        String ext = resolveExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new ServiceException("仅支持 jpg/jpeg/png/webp 图片");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new ServiceException("文件类型必须为图片");
        }
    }

    private String resolveExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            throw new ServiceException("文件扩展名无效");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (ext.contains("/") || ext.contains("\\") || ext.contains("..")) {
            throw new ServiceException("文件扩展名无效");
        }
        return ext;
    }

    private Path resolveRootPath() {
        Path root = Paths.get(accountFileProperties.getLocalRoot()).toAbsolutePath().normalize();
        FileUtil.mkdir(root.toFile());
        return root;
    }

    private Path resolveAssetPath(BasketFileAsset asset) {
        if (asset == null || !StringUtils.hasText(asset.getObjectKey())) {
            throw new ServiceException("文件路径无效");
        }
        Path root = resolveRootPath();
        Path filePath = root.resolve(asset.getObjectKey()).normalize();
        assertNoPathTraversal(root, filePath);
        return filePath;
    }

    private void assertNoPathTraversal(Path root, Path target) {
        if (!target.startsWith(root)) {
            throw new ServiceException("非法文件路径");
        }
    }
}
