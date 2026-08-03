package com.zhs.basket.service.impl.v4;

import com.zhs.basket.query.v4.BasketEventTeamQuery;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.service.v4.IBasketV4EventTeamService;
import com.zhs.basket.vo.v4.BasketEventTeamListVo;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BasketV4EventTeamServiceImpl implements IBasketV4EventTeamService {

    @Autowired
    private BasketEventTeamMapper eventTeamMapper;

    @Override
    public List<BasketEventTeamListVo> selectList(BasketEventTeamQuery query) {
        return eventTeamMapper.selectVoList(query);
    }

    @Override
    public BasketEventTeamListVo selectById(Long id) {
        if (id == null) {
            throw new ServiceException("ID 不能为空");
        }
        BasketEventTeamQuery query = new BasketEventTeamQuery();
        query.setId(id);
        List<BasketEventTeamListVo> rows = eventTeamMapper.selectVoList(query);
        if (rows.isEmpty()) {
            throw new ServiceException("赛事球队快照不存在");
        }
        return rows.get(0);
    }
}
