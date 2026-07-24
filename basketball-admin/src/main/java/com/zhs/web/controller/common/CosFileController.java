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
 * 腾讯云 COS 上传接口（与 {@link CommonController} 图片上传逻辑一致，保留兼容路径）。
 */
@RestController
@RequestMapping("/common/cos")
public class CosFileController {

    @Autowired
    private TencentCosUploadService tencentCosUploadService;

    @Autowired
    private CommonController commonController;

    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                             @RequestParam(value = "scope", required = false, defaultValue = "upload") String scope) {
        CosUploadResult result = tencentCosUploadService.upload(file, scope);
        return commonController.buildCosUploadResult(result);
    }

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
            fileNames.add(result.getUrl());
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
