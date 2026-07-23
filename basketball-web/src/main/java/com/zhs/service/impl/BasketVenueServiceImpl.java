package com.zhs.service.impl;


import com.zhs.model.BasketVenue;
import com.zhs.dao.BasketVenueDao;
import com.zhs.service.IBasketVenueService;
import com.zhs.dto.BasketVenueDto;


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
public class BasketVenueServiceImpl extends ServiceImpl< BasketVenueDao, BasketVenue> implements IBasketVenueService {

    @Resource
    private BasketVenueDao basketVenueDao;

    @Override
    public R addOrUpdate(BasketVenueDto basketVenueDto) {
        if (BeanUtil.isEmpty(basketVenueDto)) throw new ServiceException("数据不能为空");
        BasketVenue basketVenue = new BasketVenue();
        BeanUtil.copyProperties(basketVenueDto,basketVenue);
        if (basketVenueDto.getId() == null){
            basketVenueDao.insert(basketVenue);
            return R.ok("数据插入成功");
        }else {
            basketVenueDao.updateById(basketVenue);
            return R.ok("数据更新成功");
        }
    }
}