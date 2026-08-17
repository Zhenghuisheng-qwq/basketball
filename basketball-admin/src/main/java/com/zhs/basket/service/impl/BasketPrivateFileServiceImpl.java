package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketFileAsset;
import com.zhs.basket.mapper.BasketFileAssetMapper;
import com.zhs.basket.mapper.BasketPlayerCertMaterialMapper;
import com.zhs.basket.service.IBasketPrivateFileService;
import com.zhs.common.config.RuoYiConfig;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;
import com.zhs.common.utils.file.FileUtils;
import com.zhs.framework.cos.TencentCosUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class BasketPrivateFileServiceImpl implements IBasketPrivateFileService {

    @Autowired private BasketFileAssetMapper fileAssetMapper;
    @Autowired private BasketPlayerCertMaterialMapper materialMapper;
    @Autowired private TencentCosUploadService tencentCosUploadService;

    @Value("${CNPA_FILE_ROOT:./upload}")
    private String cnpaFileRoot;

    @Override
    public void streamCertMaterialFile(Long fileId, HttpServletResponse response) {
        if (materialMapper.countByFileIdForCert(fileId) <= 0) {
            throw new ServiceException("无权访问该文件");
        }
        streamFileAsset(fileId, response);
    }

    @Override
    public void streamFileAsset(Long fileId, HttpServletResponse response) {
        BasketFileAsset asset = fileAssetMapper.selectById(fileId);
        if (asset == null) {
            throw new ServiceException("文件不存在");
        }
        try {
            byte[] bytes = readBytes(asset);
            String fileName = StringUtils.hasText(asset.getOriginalName()) ? asset.getOriginalName() : asset.getFileName();
            String mimeType = StringUtils.hasText(asset.getMimeType()) ? asset.getMimeType() : "application/octet-stream";
            response.setContentType(mimeType);
            if (mimeType.toLowerCase().startsWith("image/")) {
                String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
                response.setHeader("Content-Disposition", "inline; filename*=UTF-8''" + encodedName);
            } else {
                FileUtils.setAttachmentResponseHeader(response, fileName);
            }
            response.getOutputStream().write(bytes);
        } catch (IOException e) {
            throw new ServiceException("读取文件失败");
        }
    }

    private byte[] readBytes(BasketFileAsset asset) throws IOException {
        if (StringUtils.hasText(asset.getAccessUrl()) && asset.getAccessUrl().startsWith("http")) {
            try (InputStream in = new URL(asset.getAccessUrl()).openStream()) {
                return in.readAllBytes();
            }
        }
        if ("cos".equalsIgnoreCase(asset.getStorageProvider()) && StringUtils.hasText(asset.getObjectKey())) {
            return tencentCosUploadService.read(asset.getBucketName(), asset.getObjectKey());
        }
        Path path = resolveLocalPath(asset);
        if (Files.exists(path)) {
            return Files.readAllBytes(path);
        }
        throw new ServiceException("文件不存在");
    }

    private Path resolveLocalPath(BasketFileAsset asset) {
        String objectKey = asset.getObjectKey();
        if (!StringUtils.hasText(objectKey)) {
            throw new ServiceException("文件路径无效");
        }
        Path[] roots = new Path[] {
                Paths.get(cnpaFileRoot).toAbsolutePath().normalize(),
                Paths.get(RuoYiConfig.getProfile()).toAbsolutePath().normalize(),
                Paths.get(RuoYiConfig.getUploadPath()).toAbsolutePath().normalize()
        };
        for (Path root : roots) {
            Path target = root.resolve(objectKey).normalize();
            if (target.startsWith(root) && Files.exists(target)) {
                return target;
            }
        }
        return roots[0].resolve(objectKey).normalize();
    }
}
