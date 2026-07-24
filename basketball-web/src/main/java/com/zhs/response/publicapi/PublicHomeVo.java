package com.zhs.response.publicapi;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel("官网首页聚合")
public class PublicHomeVo implements Serializable {

    @ApiModelProperty("近期公开赛果")
    private List<PublicMatchItemVo> recentMatches;

    @ApiModelProperty("球队排行（来源：当前公开赛季 WIN_RATE 榜，非热度字段）")
    private List<PublicTeamRankItemVo> teamRankings;

    @ApiModelProperty("球员排行（当前公开赛季 AVG_POINTS）")
    private List<PublicPlayerRankItemVo> playerRankings;

    @ApiModelProperty("赛季统计摘要")
    private PublicSeasonSummaryVo seasonSummary;

    @ApiModelProperty("热门球队说明：无热度字段时取当前公开赛季球队榜 WIN_RATE 前 N")
    private String hotTeamSourceNote;

    @ApiModelProperty("运营位横幅（不替换静态 home_top.png）")
    private List<PublicHomeBannerResp> banners;

    @ApiModelProperty("最新公告（最多 5 条，不含正文）")
    private List<PublicAnnouncementItemResp> latestAnnouncements;

    @ApiModelProperty("合作伙伴")
    private List<PublicPartnerResp> partners;
}
