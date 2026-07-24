package com.zhs.controller;

import com.zhs.response.account.FileUploadResp;
import com.zhs.service.IAccountFileService;
import com.zhs.service.account.AccessibleFile;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/basket/account/file")
@Api(value = "账户文件")
@Slf4j
public class AccountFileController {

    @Resource
    private IAccountFileService accountFileService;

    @ApiOperation("上传账户文件")
    @PostMapping("/upload")
    public R<FileUploadResp> upload(@RequestParam("file") MultipartFile file,
                                    @ApiParam("文件用途") @RequestParam("usageType") String usageType) {
        return R.ok(accountFileService.upload(file, usageType), "上传成功");
    }

    @ApiOperation("访问文件")
    @GetMapping("/{fileId}")
    public void getFile(@PathVariable Long fileId, HttpServletResponse response) throws IOException {
        AccessibleFile accessibleFile = accountFileService.getAccessibleFile(fileId);
        if (accessibleFile.isRedirect()) {
            response.sendRedirect(accessibleFile.getRedirectUrl());
            return;
        }

        String fileName = accessibleFile.getAsset().getOriginalName();
        if (!StringUtils.hasText(fileName)) {
            fileName = accessibleFile.getAsset().getFileName();
        }
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType(accessibleFile.getAsset().getMimeType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName);
        response.getOutputStream().write(accessibleFile.getContent());
        response.flushBuffer();
    }

    @ApiOperation("删除自己的文件")
    @DeleteMapping("/{fileId}")
    public R<Void> delete(@PathVariable Long fileId) {
        accountFileService.deleteOwnFile(fileId);
        return R.ok(null, "删除成功");
    }
}
