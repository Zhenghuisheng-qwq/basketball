package com.zhs.basket.service;

import com.zhs.basket.query.AccountPlayerQuery;
import com.zhs.basket.vo.AccountPlayerFullDetailVo;
import com.zhs.basket.vo.AccountPlayerListVo;
import java.util.List;

public interface IBasketAccountPlayerService {
    List<AccountPlayerListVo> selectList(AccountPlayerQuery query);
    AccountPlayerFullDetailVo selectDetail(Long memberId);
}
