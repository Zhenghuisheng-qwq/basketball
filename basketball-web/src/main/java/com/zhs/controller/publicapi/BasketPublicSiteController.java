package com.zhs.controller.publicapi;

import com.zhs.common.NoLoginRequest;
import com.zhs.response.publicapi.PublicSocialLinkResp;
import com.zhs.service.IBasketPublicSocialLinkService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@NoLoginRequest
@RestController
@RequestMapping("/basket/public/site")
@Api(value = "官网公开站点配置")
public class BasketPublicSiteController {

    @Resource
    private IBasketPublicSocialLinkService socialLinkService;

    @ApiOperation("社群与媒体入口")
    @GetMapping("/social-links")
    public R<List<PublicSocialLinkResp>> listSocialLinks() {
        return R.ok(socialLinkService.listEnabled());
    }
}
