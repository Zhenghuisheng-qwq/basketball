package com.zhs.service.impl;


import com.zhs.model.BasketPartner;
import com.zhs.dao.BasketPartnerDao;
import com.zhs.service.IBasketPartnerService;
import com.zhs.dto.BasketPartnerDto;


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
public class BasketPartnerServiceImpl extends ServiceImpl< BasketPartnerDao, BasketPartner> implements IBasketPartnerService {

    @Resource
    private BasketPartnerDao basketPartnerDao;

    @Override
    public R addOrUpdate(BasketPartnerDto basketPartnerDto) {
        if (BeanUtil.isEmpty(basketPartnerDto)) throw new ServiceException("数据不能为空");
        BasketPartner basketPartner = new BasketPartner();
        BeanUtil.copyProperties(basketPartnerDto,basketPartner);
        if (basketPartnerDto.getId() == null){
            basketPartnerDao.insert(basketPartner);
            return R.ok("数据插入成功");
        }else {
            basketPartnerDao.updateById(basketPartner);
            return R.ok("数据更新成功");
        }
    }
}