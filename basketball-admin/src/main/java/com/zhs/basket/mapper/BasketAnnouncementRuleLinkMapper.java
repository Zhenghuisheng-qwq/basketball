package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketAnnouncementRuleLink;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface BasketAnnouncementRuleLinkMapper {

    BasketAnnouncementRuleLink selectById(@Param("id") Long id);

    BasketAnnouncementRuleLink selectCurrentBySeason(@Param("seasonId") Long seasonId);

    BasketAnnouncementRuleLink selectPendingByNewAnnouncement(@Param("newAnnouncementId") Long newAnnouncementId);

    List<BasketAnnouncementRuleLink> selectByOldAnnouncement(@Param("oldAnnouncementId") Long oldAnnouncementId);

    int clearCurrentBySeason(@Param("seasonId") Long seasonId, @Param("invalidTime") Date invalidTime);

    int insert(BasketAnnouncementRuleLink link);

    int activateLink(@Param("id") Long id, @Param("effectiveTime") Date effectiveTime);

    int softDeleteByNewAnnouncement(@Param("newAnnouncementId") Long newAnnouncementId);
}
