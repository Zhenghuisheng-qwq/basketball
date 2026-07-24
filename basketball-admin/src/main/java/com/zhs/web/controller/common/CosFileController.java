package com.zhs.web.controller.common;

import com.zhs.common.core.domain.AjaxResult;
import com.zhs.framework.cos.TencentCosUploadService;
import com.zhs.framework.cos.TencentCosUploadService.CosUploadResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理端腾讯云 COS 上传接口。
 * <p>
 * 与本地 {@code /common/upload} 并列，密钥配置后即可使用。
 */
@RestController
@RequestMapping("/common/cos")
public class CosFileController {

    @Autowired
    private TencentCosUploadService tencentCosUploadService;

    /**
     * COS 单文件上传
     *
     * @param file  文件
     * @param scope 对象目录作用域，默认 upload
     */
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                             @RequestParam(value = "scope", required = false, defaultValue = "upload") String scope) {
        CosUploadResult result = tencentCosUploadService.upload(file, scope);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("url", result.getUrl());
        ajax.put("fileName", result.getObjectKey());
        ajax.put("newFileName", result.getFileName());
        ajax.put("originalFilename", result.getOriginalFilename());
        ajax.put("bucketName", result.getBucketName());
        ajax.put("objectKey", result.getObjectKey());
        ajax.put("fileSize", result.getFileSize());
        return ajax;
    }

    /**
     * COS 多文件上传
     */
    @PostMapping("/uploads")
    public AjaxResult uploads(@RequestParam("files") List<MultipartFile> files,
                              @RequestParam(value = "scope", required = false, defaultValue = "upload") String scope) {
        List<String> urls = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();
        List<String> newFileNames = new ArrayList<>();
        List<String> originalFilenames = new ArrayList<>();
        List<String> objectKeys = new ArrayList<>();
        for (MultipartFile file : files) {
            CosUploadResult result = tencentCosUploadService.upload(file, scope);
            urls.add(result.getUrl());
            fileNames.add(result.getObjectKey());
            newFileNames.add(result.getFileName());
            originalFilenames.add(result.getOriginalFilename());
            objectKeys.add(result.getObjectKey());
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put("urls", String.join(",", urls));
        ajax.put("fileNames", String.join(",", fileNames));
        ajax.put("newFileNames", String.join(",", newFileNames));
        ajax.put("originalFilenames", String.join(",", originalFilenames));
        ajax.put("objectKeys", String.join(",", objectKeys));
        return ajax;
    }
}
