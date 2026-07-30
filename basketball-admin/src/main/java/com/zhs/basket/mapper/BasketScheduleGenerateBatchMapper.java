package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketScheduleGenerateBatch;
import org.apache.ibatis.annotations.Param;

public interface BasketScheduleGenerateBatchMapper {
    int insert(BasketScheduleGenerateBatch batch);
    BasketScheduleGenerateBatch selectById(@Param("id") Long id);
    BasketScheduleGenerateBatch selectLatestByEventId(@Param("eventId") Long eventId);
    int countByEventId(@Param("eventId") Long eventId);
    int countPublishedByEventId(@Param("eventId") Long eventId);
    int updatePublishStatus(@Param("id") Long id, @Param("publishStatus") Integer publishStatus,
            @Param("publishTime") java.util.Date publishTime);
    int softDeleteDraftByEventId(@Param("eventId") Long eventId);
}
