package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketCaptainQualification;
import com.zhs.basket.query.CaptainQualificationQuery;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BasketCaptainQualificationMapper {
    List<BasketCaptainQualification> selectList(CaptainQualificationQuery query);
    BasketCaptainQualification selectById(Long id);
    int countCurrentActiveByPlayerId(@Param("playerId") Long playerId);
    int insert(BasketCaptainQualification record);
    int updateById(BasketCaptainQualification record);
    int clearCurrentByPlayerId(@Param("playerId") Long playerId);
    BasketCaptainQualification selectCurrentByPlayerId(@Param("playerId") Long playerId);
}
