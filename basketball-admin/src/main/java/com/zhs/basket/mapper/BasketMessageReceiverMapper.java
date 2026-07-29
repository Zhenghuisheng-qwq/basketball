package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMessageReceiver;
import com.zhs.basket.vo.AccountPlayerNotificationVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketMessageReceiverMapper {
    int insert(BasketMessageReceiver record);
    int batchInsert(@Param("list") List<BasketMessageReceiver> list);
    List<AccountPlayerNotificationVo> selectAccountNotificationsByMemberId(@Param("memberId") Long memberId);
}
