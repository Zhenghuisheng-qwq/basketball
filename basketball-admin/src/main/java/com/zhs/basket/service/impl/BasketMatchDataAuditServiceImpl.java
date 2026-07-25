package com.zhs.basket.service.impl;

import com.zhs.basket.enums.MatchDataStatusEnum;
import com.zhs.basket.mapper.BasketMatchDataAuditMapper;
import com.zhs.basket.query.MatchDataAuditQuery;
import com.zhs.basket.service.IBasketMatchDataAuditService;
import com.zhs.basket.vo.MatchDataAuditListVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BasketMatchDataAuditServiceImpl implements IBasketMatchDataAuditService {

    @Autowired
    private BasketMatchDataAuditMapper auditMapper;

    @Override
    public List<MatchDataAuditListVo> listPending(MatchDataAuditQuery query) {
        if (query == null) {
            query = new MatchDataAuditQuery();
        }
        if (query.getDataStatus() == null) {
            query.setDataStatus(MatchDataStatusEnum.SUBMITTED.getCode());
        }
        List<MatchDataAuditListVo> list = auditMapper.selectList(query);
        for (MatchDataAuditListVo vo : list) {
            vo.setDataStatusName(MatchDataStatusEnum.fromCode(vo.getDataStatus()).getDescription());
        }
        return list;
    }
}
