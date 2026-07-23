package com.zhs.service.impl;


import com.zhs.model.BasketHomeBanner;
import com.zhs.dao.BasketHomeBannerDao;
import com.zhs.service.IBasketHomeBannerService;
import com.zhs.dto.BasketHomeBannerDto;


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
public class BasketHomeBannerServiceImpl extends ServiceImpl< BasketHomeBannerDao, BasketHomeBanner> implements IBasketHomeBannerService {

    @Resource
    private BasketHomeBannerDao basketHomeBannerDao;

    @Override
    public R addOrUpdate(BasketHomeBannerDto basketHomeBannerDto) {
        if (BeanUtil.isEmpty(basketHomeBannerDto)) throw new ServiceException("数据不能为空");
        BasketHomeBanner basketHomeBanner = new BasketHomeBanner();
        BeanUtil.copyProperties(basketHomeBannerDto,basketHomeBanner);
        if (basketHomeBannerDto.getId() == null){
            basketHomeBannerDao.insert(basketHomeBanner);
            return R.ok("数据插入成功");
        }else {
            basketHomeBannerDao.updateById(basketHomeBanner);
            return R.ok("数据更新成功");
        }
    }
}