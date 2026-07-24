package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketTournament;
import com.zhs.basket.enums.TournamentStatusEnum;
import com.zhs.basket.mapper.BasketTournamentMapper;
import com.zhs.basket.query.TournamentQuery;
import com.zhs.basket.req.TournamentCreateReq;
import com.zhs.basket.req.TournamentUpdateReq;
import com.zhs.basket.service.IBasketTournamentService;
import com.zhs.basket.support.PlayerNoGenerator;
import com.zhs.basket.vo.TournamentDetailVo;
import com.zhs.basket.vo.TournamentListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BasketTournamentServiceImpl implements IBasketTournamentService {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketTournamentMapper tournamentMapper;
    @Autowired private PlayerNoGenerator playerNoGenerator;

    @Override
    public List<TournamentListVo> selectList(TournamentQuery query) {
        return tournamentMapper.selectList(query).stream().map(this::toListVo).collect(Collectors.toList());
    }

    @Override
    public TournamentDetailVo selectById(Long id) {
        BasketTournament t = requireTournament(id);
        TournamentDetailVo vo = new TournamentDetailVo();
        BeanUtils.copyBeanProp(vo, t);
        vo.setTournamentStatusName(TournamentStatusEnum.fromCode(t.getTournamentStatus()).getDescription());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TournamentCreateReq req) {
        BasketTournament t = new BasketTournament();
        BeanUtils.copyBeanProp(t, req);
        t.setTournamentNo(playerNoGenerator.generateBizNo("TN"));
        t.setTournamentStatus(TournamentStatusEnum.DRAFT.getCode());
        t.setIsDelete(NOT_DELETED);
        tournamentMapper.insert(t);
        return t.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TournamentUpdateReq req) {
        BasketTournament t = requireTournament(id);
        BeanUtils.copyBeanProp(t, req);
        if (tournamentMapper.updateById(t) != 1) {
            throw new ServiceException("更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        int rows = tournamentMapper.updateStatusIfMatch(id,
                Arrays.asList(TournamentStatusEnum.DRAFT.getCode(), TournamentStatusEnum.DISABLED.getCode()),
                TournamentStatusEnum.ACTIVE.getCode());
        assertSingleRow(rows, "发布失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpublish(Long id) {
        int rows = tournamentMapper.updateStatusIfMatch(id,
                Arrays.asList(TournamentStatusEnum.ACTIVE.getCode()),
                TournamentStatusEnum.DISABLED.getCode());
        assertSingleRow(rows, "下架失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id) {
        int rows = tournamentMapper.updateStatusIfMatch(id,
                Arrays.asList(TournamentStatusEnum.ACTIVE.getCode(), TournamentStatusEnum.DRAFT.getCode()),
                TournamentStatusEnum.ENDED.getCode());
        assertSingleRow(rows, "关闭失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (tournamentMapper.softDelete(id) != 1) {
            throw new ServiceException("删除失败");
        }
    }

    private BasketTournament requireTournament(Long id) {
        BasketTournament t = tournamentMapper.selectById(id);
        if (t == null) {
            throw new ServiceException("赛事不存在");
        }
        return t;
    }

    private TournamentListVo toListVo(BasketTournament t) {
        TournamentListVo vo = new TournamentListVo();
        BeanUtils.copyBeanProp(vo, t);
        vo.setTournamentStatusName(TournamentStatusEnum.fromCode(t.getTournamentStatus()).getDescription());
        return vo;
    }

    private void assertSingleRow(int rows, String message) {
        if (rows != 1) {
            throw new ServiceException(message);
        }
    }
}
