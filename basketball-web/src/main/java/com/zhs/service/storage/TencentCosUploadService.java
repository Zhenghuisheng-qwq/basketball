package com.zhs.service.storage;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.zhs.config.TencentCosClientHolder;
import com.zhs.config.TencentCosProperties;
import com.zhs.exception.ServiceException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

/**
 * 腾讯云 COS 通用上传/读写（不依赖 account.file.storage-type）。
 */
@Service
public class TencentCosUploadService {

    private static final String STORAGE_PROVIDER = "cos";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif", "pdf", "zip", "rar", "doc", "docx", "xls", "xlsx");

    @Resource
    private TencentCosProperties cosProperties;
    @Resource
    private TencentCosClientHolder cosClientHolder;

    public StoredFileInfo upload(MultipartFile file, String scope) throws IOException {
        validateFile(file);
        String ext = resolveExtension(file.getOriginalFilename());
        String datePath = DateUtil.format(new Date(), "yyyy/MM/dd");
        String storedName = IdUtil.fastSimpleUUID() + "." + ext;
        String normalizedScope = StringUtils.hasText(scope) ? scope.trim() : "public";
        String prefix = cosProperties.normalizeKeyPrefix();
        String objectKey = (StringUtils.hasText(prefix) ? prefix + "/" : "")
                + normalizedScope + "/" + datePath + "/" + storedName;

        byte[] bytes = file.getBytes();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(bytes.length);
        if (StringUtils.hasText(file.getContentType())) {
            metadata.setContentType(file.getContentType());
        }

        COSClient client = cosClientHolder.getRequiredClient();
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            PutObjectRequest request = new PutObjectRequest(
                    cosProperties.getBucket(), objectKey, inputStream, metadata);
            client.putObject(request);
        }

        StoredFileInfo info = new StoredFileInfo();
        info.setStorageProvider(STORAGE_PROVIDER);
        info.setBucketName(cosProperties.getBucket());
        info.setObjectKey(objectKey);
        info.setFileName(storedName);
        info.setFileExt(ext);
        info.setMimeType(file.getContentType());
        info.setFileSize(file.getSize());
        info.setFileHash(DigestUtil.sha256Hex(bytes));
        info.setPublicRelativePath(datePath + "/" + storedName);
        info.setAccessUrl(cosProperties.buildAccessUrl(objectKey));
        return info;
    }

    public byte[] read(String bucketName, String objectKey) throws IOException {
        if (!StringUtils.hasText(objectKey)) {
            throw new ServiceException("文件路径无效");
        }
        COSClient client = cosClientHolder.getRequiredClient();
        String bucket = StringUtils.hasText(bucketName) ? bucketName : cosProperties.getBucket();
        COSObject cosObject = client.getObject(bucket, objectKey);
        try (InputStream inputStream = cosObject.getObjectContent()) {
            return inputStream.readAllBytes();
        }
    }

    public void delete(String bucketName, String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return;
        }
        COSClient client = cosClientHolder.getRequiredClient();
        String bucket = StringUtils.hasText(bucketName) ? bucketName : cosProperties.getBucket();
        client.deleteObject(bucket, objectKey);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        String ext = resolveExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new ServiceException("不支持的文件类型: " + ext);
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
}
