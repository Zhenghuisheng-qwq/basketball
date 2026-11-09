package com.zhs.web.controller.basket;

import com.zhs.basket.service.BasketContentImageService;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/basket/content-image")
public class BasketContentImageController extends BaseController {

    @Autowired
    private BasketContentImageService contentImageService;

    @PreAuthorize("@ss.hasAnyPermi('basket:homeContent:add,basket:homeContent:edit,basket:pageHero:edit,basket:v4Season:edit,basket:siteTheme:edit')")
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                             @RequestParam(value = "usageType", required = false) String usageType) {
        return success(contentImageService.upload(file, usageType));
    }
}
