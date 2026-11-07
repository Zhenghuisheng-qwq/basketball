package com.zhs.web.controller.basket;

import com.zhs.basket.constant.BasketContentPerms;
import com.zhs.basket.service.v4.IBasketV4SeriesService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 球员荣誉头像：一个赛事系列对应一张徽章图。
 */
@RestController
@RequestMapping("/basket/hall-of-fame/series-icon")
public class BasketSeriesHonorIconController extends BaseController {

    @Autowired
    private IBasketV4SeriesService seriesService;

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.HALL_OF_FAME_LIST + "')")
    @GetMapping("/list")
    public AjaxResult list() {
        return success(seriesService.selectHonorIconList());
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.HALL_OF_FAME_EDIT + "')")
    @Log(title = "荣誉头像", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String url = body == null ? null : body.get("honorIconUrl");
        seriesService.updateHonorIconUrl(id, url);
        return success();
    }
}
