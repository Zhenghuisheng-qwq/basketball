package com.zhs.service.impl;


import com.zhs.model.BasketTeamSeasonStat;
import com.zhs.dao.BasketTeamSeasonStatDao;
import com.zhs.service.IBasketTeamSeasonStatService;
import com.zhs.dto.BasketTeamSeasonStatDto;


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
public class BasketTeamSeasonStatServiceImpl extends ServiceImpl< BasketTeamSeasonStatDao, BasketTeamSeasonStat> implements IBasketTeamSeasonStatService {

    @Resource
    private BasketTeamSeasonStatDao basketTeamSeasonStatDao;

    @Override
    public R addOrUpdate(BasketTeamSeasonStatDto basketTeamSeasonStatDto) {
        if (BeanUtil.isEmpty(basketTeamSeasonStatDto)) throw new ServiceException("数据不能为空");
        BasketTeamSeasonStat basketTeamSeasonStat = new BasketTeamSeasonStat();
        BeanUtil.copyProperties(basketTeamSeasonStatDto,basketTeamSeasonStat);
        if (basketTeamSeasonStatDto.getId() == null){
            basketTeamSeasonStatDao.insert(basketTeamSeasonStat);
            return R.ok("数据插入成功");
        }else {
            basketTeamSeasonStatDao.updateById(basketTeamSeasonStat);
            return R.ok("数据更新成功");
        }
    }
}