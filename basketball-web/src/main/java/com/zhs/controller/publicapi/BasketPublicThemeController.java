package com.zhs.controller.publicapi;

import com.zhs.common.NoLoginRequest;
import com.zhs.response.publicapi.PublicSiteThemeResp;
import com.zhs.service.IBasketPublicThemeService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@NoLoginRequest
@RestController
@RequestMapping("/basket/public/theme")
@Api(value = "官网公开主题")
public class BasketPublicThemeController {

    @Resource
    private IBasketPublicThemeService themeService;

    @ApiOperation("按路由范围返回已发布主题。没有绑定时 configured=false")
    @GetMapping
    public R<PublicSiteThemeResp> getTheme(
            @RequestParam String scope,
            @RequestParam(required = false) Long seasonId) {
        return R.ok(themeService.getPublished(scope, seasonId));
    }
}
