package com.zhs.service.impl;


import com.zhs.model.BasketTournamentSeason;
import com.zhs.dao.BasketTournamentSeasonDao;
import com.zhs.service.IBasketTournamentSeasonService;
import com.zhs.dto.BasketTournamentSeasonDto;


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
public class BasketTournamentSeasonServiceImpl extends ServiceImpl< BasketTournamentSeasonDao, BasketTournamentSeason> implements IBasketTournamentSeasonService {

    @Resource
    private BasketTournamentSeasonDao basketTournamentSeasonDao;

    @Override
    public R addOrUpdate(BasketTournamentSeasonDto basketTournamentSeasonDto) {
        if (BeanUtil.isEmpty(basketTournamentSeasonDto)) throw new ServiceException("数据不能为空");
        BasketTournamentSeason basketTournamentSeason = new BasketTournamentSeason();
        BeanUtil.copyProperties(basketTournamentSeasonDto,basketTournamentSeason);
        if (basketTournamentSeasonDto.getId() == null){
            basketTournamentSeasonDao.insert(basketTournamentSeason);
            return R.ok("数据插入成功");
        }else {
            basketTournamentSeasonDao.updateById(basketTournamentSeason);
            return R.ok("数据更新成功");
        }
    }
}