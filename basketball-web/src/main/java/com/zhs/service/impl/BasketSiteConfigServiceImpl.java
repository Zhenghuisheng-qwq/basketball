package com.zhs.service.impl;


import com.zhs.model.BasketSiteConfig;
import com.zhs.dao.BasketSiteConfigDao;
import com.zhs.service.IBasketSiteConfigService;
import com.zhs.dto.BasketSiteConfigDto;


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
public class BasketSiteConfigServiceImpl extends ServiceImpl< BasketSiteConfigDao, BasketSiteConfig> implements IBasketSiteConfigService {

    @Resource
    private BasketSiteConfigDao basketSiteConfigDao;

    @Override
    public R addOrUpdate(BasketSiteConfigDto basketSiteConfigDto) {
        if (BeanUtil.isEmpty(basketSiteConfigDto)) throw new ServiceException("数据不能为空");
        BasketSiteConfig basketSiteConfig = new BasketSiteConfig();
        BeanUtil.copyProperties(basketSiteConfigDto,basketSiteConfig);
        if (basketSiteConfigDto.getId() == null){
            basketSiteConfigDao.insert(basketSiteConfig);
            return R.ok("数据插入成功");
        }else {
            basketSiteConfigDao.updateById(basketSiteConfig);
            return R.ok("数据更新成功");
        }
    }
}