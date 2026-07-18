package com.zhs.service.impl;


import com.zhs.model.BasketTournamentStanding;
import com.zhs.dao.BasketTournamentStandingDao;
import com.zhs.service.IBasketTournamentStandingService;
import com.zhs.dto.BasketTournamentStandingDto;


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
 * @since 2026-07-18
 */
@Service
@Slf4j
public class BasketTournamentStandingServiceImpl extends ServiceImpl< BasketTournamentStandingDao, BasketTournamentStanding> implements IBasketTournamentStandingService {

    @Resource
    private BasketTournamentStandingDao basketTournamentStandingDao;

    @Override
    public R addOrUpdate(BasketTournamentStandingDto basketTournamentStandingDto) {
        if (BeanUtil.isEmpty(basketTournamentStandingDto)) throw new ServiceException("数据不能为空");
        BasketTournamentStanding basketTournamentStanding = new BasketTournamentStanding();
        BeanUtil.copyProperties(basketTournamentStandingDto,basketTournamentStanding);
        if (basketTournamentStandingDto.getId() == null){
            basketTournamentStandingDao.insert(basketTournamentStanding);
            return R.ok("数据插入成功");
        }else {
            basketTournamentStandingDao.updateById(basketTournamentStanding);
            return R.ok("数据更新成功");
        }
    }
}