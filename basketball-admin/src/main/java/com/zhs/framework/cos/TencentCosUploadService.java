package com.zhs.framework.cos;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;
import com.zhs.common.utils.uuid.IdUtils;
import com.zhs.framework.config.properties.TencentCosProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

/**
 * 管理端腾讯云 COS 上传服务。
 */
@Service
public class TencentCosUploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "pdf", "zip", "rar",
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "mp4");

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Autowired
    private TencentCosProperties cosProperties;

    @Autowired
    private TencentCosClientHolder cosClientHolder;

    public CosUploadResult upload(MultipartFile file, String scope) {
        validateFile(file);
        String originalFilename = file.getOriginalFilename();
        String ext = resolveExtension(originalFilename);
        String datePath = LocalDate.now().format(DATE_PATH);
        String storedName = IdUtils.fastSimpleUUID() + "." + ext;
        String normalizedScope = StringUtils.isNotEmpty(scope) ? scope.trim() : "upload";
        String prefix = cosProperties.normalizeKeyPrefix();
        String objectKey = (StringUtils.isNotEmpty(prefix) ? prefix + "/" : "")
                + normalizedScope + "/" + datePath + "/" + storedName;

        try {
            byte[] bytes = file.getBytes();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            if (StringUtils.isNotEmpty(file.getContentType())) {
                metadata.setContentType(file.getContentType());
            }

            COSClient client = cosClientHolder.getRequiredClient();
            try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
                PutObjectRequest request = new PutObjectRequest(
                        cosProperties.getBucket(), objectKey, inputStream, metadata);
                client.putObject(request);
            }

            CosUploadResult result = new CosUploadResult();
            result.setBucketName(cosProperties.getBucket());
            result.setObjectKey(objectKey);
            result.setFileName(storedName);
            result.setOriginalFilename(resolveOriginalName(originalFilename));
            result.setUrl(cosProperties.buildAccessUrl(objectKey));
            result.setFileSize(file.getSize());
            result.setFileExt(ext);
            return result;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException("COS 文件上传失败: " + ex.getMessage());
        }
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
        if (StringUtils.isEmpty(originalFilename) || !originalFilename.contains(".")) {
            throw new ServiceException("文件扩展名无效");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (ext.contains("/") || ext.contains("\\") || ext.contains("..")) {
            throw new ServiceException("文件扩展名无效");
        }
        return ext;
    }

    private String resolveOriginalName(String originalFilename) {
        if (StringUtils.isEmpty(originalFilename)) {
            return "unknown";
        }
        String normalized = originalFilename.replace("\\", "/");
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    public static class CosUploadResult {
        private String bucketName;
        private String objectKey;
        private String fileName;
        private String originalFilename;
        private String url;
        private long fileSize;
        private String fileExt;

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public String getObjectKey() {
            return objectKey;
        }

        public void setObjectKey(String objectKey) {
            this.objectKey = objectKey;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getOriginalFilename() {
            return originalFilename;
        }

        public void setOriginalFilename(String originalFilename) {
            this.originalFilename = originalFilename;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public long getFileSize() {
            return fileSize;
        }

        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }

        public String getFileExt() {
            return fileExt;
        }

        public void setFileExt(String fileExt) {
            this.fileExt = fileExt;
        }
    }
}
