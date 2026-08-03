package com.zhs.basket.service.impl.v4;

import com.zhs.basket.mapper.v4.BasketEventPlayerMapper;
import com.zhs.basket.query.v4.BasketEventPlayerQuery;
import com.zhs.basket.service.v4.IBasketV4EventPlayerService;
import com.zhs.basket.vo.v4.BasketEventPlayerListVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BasketV4EventPlayerServiceImpl implements IBasketV4EventPlayerService {

    @Autowired
    private BasketEventPlayerMapper eventPlayerMapper;

    @Override
    public List<BasketEventPlayerListVo> selectList(BasketEventPlayerQuery query) {
        return eventPlayerMapper.selectVoList(query);
    }
}
