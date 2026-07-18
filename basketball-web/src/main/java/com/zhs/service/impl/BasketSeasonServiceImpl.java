package com.zhs.service.impl;


import com.zhs.model.BasketSeason;
import com.zhs.dao.BasketSeasonDao;
import com.zhs.service.IBasketSeasonService;
import com.zhs.dto.BasketSeasonDto;


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
public class BasketSeasonServiceImpl extends ServiceImpl< BasketSeasonDao, BasketSeason> implements IBasketSeasonService {

    @Resource
    private BasketSeasonDao basketSeasonDao;

    @Override
    public R addOrUpdate(BasketSeasonDto basketSeasonDto) {
        if (BeanUtil.isEmpty(basketSeasonDto)) throw new ServiceException("数据不能为空");
        BasketSeason basketSeason = new BasketSeason();
        BeanUtil.copyProperties(basketSeasonDto,basketSeason);
        if (basketSeasonDto.getId() == null){
            basketSeasonDao.insert(basketSeason);
            return R.ok("数据插入成功");
        }else {
            basketSeasonDao.updateById(basketSeason);
            return R.ok("数据更新成功");
        }
    }
}