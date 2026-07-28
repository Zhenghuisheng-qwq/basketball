package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketPlayerPlatformIdentityHistory;
import org.apache.ibatis.annotations.Param;

public interface BasketPlayerPlatformIdentityHistoryMapper {
    int insert(BasketPlayerPlatformIdentityHistory record);

    int countInitialBind(@Param("playerId") Long playerId, @Param("platformCode") String platformCode);
}
