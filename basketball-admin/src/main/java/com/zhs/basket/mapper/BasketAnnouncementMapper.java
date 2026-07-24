package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketAnnouncement;
import com.zhs.basket.req.AnnouncementQueryReq;
import com.zhs.basket.resp.AnnouncementDetailResp;
import com.zhs.basket.resp.AnnouncementListResp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketAnnouncementMapper {
    BasketAnnouncement selectById(@Param("id") Long id);

    AnnouncementDetailResp selectDetailById(@Param("id") Long id);

    List<AnnouncementListResp> selectList(AnnouncementQueryReq query);

    int countByNoPrefix(@Param("prefix") String prefix);

    int insert(BasketAnnouncement announcement);

    int updateById(BasketAnnouncement announcement);

    int updateStatusIfMatch(@Param("id") Long id,
                            @Param("fromStatus") Integer fromStatus,
                            @Param("toStatus") Integer toStatus);

    int publishIfMatch(@Param("id") Long id, @Param("fromStatus") Integer fromStatus);

    int offlineIfMatch(@Param("id") Long id, @Param("fromStatus") Integer fromStatus);

    int updateTop(@Param("id") Long id, @Param("isTop") Integer isTop);

    int softDelete(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses);
}
