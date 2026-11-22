package com.zhs.basket.service.impl.v4;

import com.zhs.basket.mapper.v4.BasketEventPlayerMapper;
import com.zhs.basket.query.v4.BasketEventPlayerQuery;
import com.zhs.basket.service.v4.IBasketV4EventPlayerService;
import com.zhs.basket.support.v4.EventGameIdSnapshotSync;
import com.zhs.basket.vo.v4.BasketEventPlayerListVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BasketV4EventPlayerServiceImpl implements IBasketV4EventPlayerService {

    @Autowired
    private BasketEventPlayerMapper eventPlayerMapper;
    @Autowired
    private EventGameIdSnapshotSync eventGameIdSnapshotSync;

    @Override
    public List<BasketEventPlayerListVo> selectList(BasketEventPlayerQuery query) {
        List<BasketEventPlayerListVo> rows = eventPlayerMapper.selectVoList(query);
        eventGameIdSnapshotSync.applyEventPlayerList(rows);
        return rows;
    }
}
