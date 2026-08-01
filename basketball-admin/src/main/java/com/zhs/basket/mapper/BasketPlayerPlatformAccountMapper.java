package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketPlayerPlatformAccount;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BasketPlayerPlatformAccountMapper {
    int insert(BasketPlayerPlatformAccount record);
    int updateById(BasketPlayerPlatformAccount record);
    BasketPlayerPlatformAccount selectById(Long id);
    List<BasketPlayerPlatformAccount> selectByPlayerId(@Param("playerId") Long playerId);

    List<BasketPlayerPlatformAccount> selectActiveByPlayerIds(@Param("playerIds") List<Long> playerIds);
    int countActiveByPlatformAndNormalized(@Param("platform") String platform,
            @Param("normalizedGameId") String normalizedGameId);
    int countActiveByPlatformAndNormalizedExcludePlayer(@Param("platform") String platform,
            @Param("normalizedGameId") String normalizedGameId, @Param("excludePlayerId") Long excludePlayerId);
    BasketPlayerPlatformAccount selectPrimaryByPlayerId(@Param("playerId") Long playerId);
}
