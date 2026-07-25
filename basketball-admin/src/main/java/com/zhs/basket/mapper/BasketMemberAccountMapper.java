package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMemberAccount;
import com.zhs.basket.query.AccountPlayerQuery;
import com.zhs.basket.vo.AccountPlayerDetailVo;
import com.zhs.basket.vo.AccountPlayerListVo;
import java.util.List;

public interface BasketMemberAccountMapper {
    BasketMemberAccount selectById(Long id);
    List<AccountPlayerListVo> selectList(AccountPlayerQuery query);
    AccountPlayerDetailVo selectDetail(Long memberId);
}
