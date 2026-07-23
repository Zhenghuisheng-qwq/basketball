package com.zhs.service.impl;


import com.zhs.model.BasketStageStanding;
import com.zhs.dao.BasketStageStandingDao;
import com.zhs.service.IBasketStageStandingService;
import com.zhs.dto.BasketStageStandingDto;


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
public class BasketStageStandingServiceImpl extends ServiceImpl< BasketStageStandingDao, BasketStageStanding> implements IBasketStageStandingService {

    @Resource
    private BasketStageStandingDao basketStageStandingDao;

    @Override
    public R addOrUpdate(BasketStageStandingDto basketStageStandingDto) {
        if (BeanUtil.isEmpty(basketStageStandingDto)) throw new ServiceException("数据不能为空");
        BasketStageStanding basketStageStanding = new BasketStageStanding();
        BeanUtil.copyProperties(basketStageStandingDto,basketStageStanding);
        if (basketStageStandingDto.getId() == null){
            basketStageStandingDao.insert(basketStageStanding);
            return R.ok("数据插入成功");
        }else {
            basketStageStandingDao.updateById(basketStageStanding);
            return R.ok("数据更新成功");
        }
    }
}