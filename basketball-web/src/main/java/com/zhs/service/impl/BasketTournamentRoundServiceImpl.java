package com.zhs.service.impl;


import com.zhs.model.BasketTournamentRound;
import com.zhs.dao.BasketTournamentRoundDao;
import com.zhs.service.IBasketTournamentRoundService;
import com.zhs.dto.BasketTournamentRoundDto;


import com.zhs.exception.ServiceException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhs.util.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import java.util.Date;


/**
 *
 * @author 
 * @since 2026-07-24
 */
@Service
@Slf4j
public class BasketTournamentRoundServiceImpl extends ServiceImpl< BasketTournamentRoundDao, BasketTournamentRound> implements IBasketTournamentRoundService {

    @Resource
    private BasketTournamentRoundDao basketTournamentRoundDao;

    @Override
    public R addOrUpdate(BasketTournamentRoundDto basketTournamentRoundDto) {
        if (BeanUtil.isEmpty(basketTournamentRoundDto)) throw new ServiceException("数据不能为空");
        BasketTournamentRound basketTournamentRound = new BasketTournamentRound();
        BeanUtil.copyProperties(basketTournamentRoundDto,basketTournamentRound);
        if (basketTournamentRoundDto.getId() == null){
            basketTournamentRoundDao.insert(basketTournamentRound);
            return R.ok("数据插入成功");
        }else {
            basketTournamentRoundDao.updateById(basketTournamentRound);
            return R.ok("数据更新成功");
        }
    }
}