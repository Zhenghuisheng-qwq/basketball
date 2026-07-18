package com.zhs.service.impl;


import com.zhs.model.BasketSiteBanner;
import com.zhs.dao.BasketSiteBannerDao;
import com.zhs.service.IBasketSiteBannerService;
import com.zhs.dto.BasketSiteBannerDto;


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
public class BasketSiteBannerServiceImpl extends ServiceImpl< BasketSiteBannerDao, BasketSiteBanner> implements IBasketSiteBannerService {

    @Resource
    private BasketSiteBannerDao basketSiteBannerDao;

    @Override
    public R addOrUpdate(BasketSiteBannerDto basketSiteBannerDto) {
        if (BeanUtil.isEmpty(basketSiteBannerDto)) throw new ServiceException("数据不能为空");
        BasketSiteBanner basketSiteBanner = new BasketSiteBanner();
        BeanUtil.copyProperties(basketSiteBannerDto,basketSiteBanner);
        if (basketSiteBannerDto.getId() == null){
            basketSiteBannerDao.insert(basketSiteBanner);
            return R.ok("数据插入成功");
        }else {
            basketSiteBannerDao.updateById(basketSiteBanner);
            return R.ok("数据更新成功");
        }
    }
}