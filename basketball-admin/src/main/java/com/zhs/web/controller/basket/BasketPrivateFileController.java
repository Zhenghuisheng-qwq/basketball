package com.zhs.web.controller.basket;

import com.zhs.basket.service.IBasketPrivateFileService;
import com.zhs.common.core.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/basket/private-file")
public class BasketPrivateFileController extends BaseController {

    @Autowired
    private IBasketPrivateFileService privateFileService;

    @PreAuthorize("@ss.hasPermi('basket:playerCert:query')")
    @GetMapping("/{fileId}")
    public void stream(@PathVariable Long fileId, HttpServletResponse response) {
        privateFileService.streamCertMaterialFile(fileId, response);
    }
}
