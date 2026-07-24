package com.zhs.controller.publicapi;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.common.NoLoginRequest;
import com.zhs.response.publicapi.PublicAnnouncementAdjacentResp;
import com.zhs.response.publicapi.PublicAnnouncementCategoryResp;
import com.zhs.response.publicapi.PublicAnnouncementDetailResp;
import com.zhs.response.publicapi.PublicAnnouncementItemResp;
import com.zhs.service.IBasketPublicAnnouncementService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@NoLoginRequest
@RestController
@RequestMapping("/basket/public/announcement")
@Api(value = "官网公开公告")
public class BasketPublicAnnouncementController {

    @Resource
    private IBasketPublicAnnouncementService announcementService;

    @ApiOperation("公告分类枚举")
    @GetMapping("/category")
    public R<List<PublicAnnouncementCategoryResp>> listCategories() {
        return R.ok(announcementService.listCategories());
    }

    @ApiOperation("公告分页（不含正文）")
    @GetMapping("/page")
    public R<IPage<PublicAnnouncementItemResp>> pageAnnouncements(
            @RequestParam(value = "currentPage", defaultValue = "1") Integer currentPage,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "categoryCode", required = false) String categoryCode,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortOrder", required = false) String sortOrder) {
        return R.ok(announcementService.pageAnnouncements(
                currentPage, pageSize, categoryCode, keyword, sortField, sortOrder));
    }

    @ApiOperation("公告详情（含纯文本正文，浏览数 +1）")
    @GetMapping("/{id}")
    public R<PublicAnnouncementDetailResp> getDetail(@PathVariable("id") Long id) {
        return R.ok(announcementService.getDetail(id));
    }

    @ApiOperation("上一篇/下一篇")
    @GetMapping("/{id}/previous-next")
    public R<PublicAnnouncementAdjacentResp> getPreviousNext(@PathVariable("id") Long id) {
        return R.ok(announcementService.getPreviousNext(id));
    }
}
