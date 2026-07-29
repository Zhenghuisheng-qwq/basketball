package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketAccountSecurityLog;
import com.zhs.basket.vo.AccountPlayerOperationLogVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketAccountSecurityLogMapper {
    int insert(BasketAccountSecurityLog record);

    List<AccountPlayerOperationLogVo> selectByMemberId(@Param("memberId") Long memberId);
}
