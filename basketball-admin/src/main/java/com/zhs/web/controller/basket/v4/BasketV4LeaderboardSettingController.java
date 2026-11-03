package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.support.v4.leaderboard.LeaderboardApplicationService;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/v4/leaderboard")
public class BasketV4LeaderboardSettingController extends BaseController {

    @Autowired
    private LeaderboardApplicationService leaderboardApplicationService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.LEADERBOARD_PUBLISH + "')")
    @PutMapping("/featured")
    public AjaxResult featured(@RequestBody FeaturedBody body) {
        leaderboardApplicationService.updateFeatured(body == null ? null : body.divisionId, body == null ? null : body.reason);
        return success();
    }

    public static class FeaturedBody {
        public Long divisionId;
        public String reason;
    }
}
