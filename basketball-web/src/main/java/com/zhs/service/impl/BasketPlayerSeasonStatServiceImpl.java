package com.zhs.service.impl;


import com.zhs.model.BasketPlayerSeasonStat;
import com.zhs.dao.BasketPlayerSeasonStatDao;
import com.zhs.service.IBasketPlayerSeasonStatService;
import com.zhs.dto.BasketPlayerSeasonStatDto;


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
public class BasketPlayerSeasonStatServiceImpl extends ServiceImpl< BasketPlayerSeasonStatDao, BasketPlayerSeasonStat> implements IBasketPlayerSeasonStatService {

    @Resource
    private BasketPlayerSeasonStatDao basketPlayerSeasonStatDao;

    @Override
    public R addOrUpdate(BasketPlayerSeasonStatDto basketPlayerSeasonStatDto) {
        if (BeanUtil.isEmpty(basketPlayerSeasonStatDto)) throw new ServiceException("数据不能为空");
        BasketPlayerSeasonStat basketPlayerSeasonStat = new BasketPlayerSeasonStat();
        BeanUtil.copyProperties(basketPlayerSeasonStatDto,basketPlayerSeasonStat);
        if (basketPlayerSeasonStatDto.getId() == null){
            basketPlayerSeasonStatDao.insert(basketPlayerSeasonStat);
            return R.ok("数据插入成功");
        }else {
            basketPlayerSeasonStatDao.updateById(basketPlayerSeasonStat);
            return R.ok("数据更新成功");
        }
    }
}