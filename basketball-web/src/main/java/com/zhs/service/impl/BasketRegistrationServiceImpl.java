package com.zhs.service.impl;


import com.zhs.model.BasketRegistration;
import com.zhs.dao.BasketRegistrationDao;
import com.zhs.service.IBasketRegistrationService;
import com.zhs.dto.BasketRegistrationDto;


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
public class BasketRegistrationServiceImpl extends ServiceImpl< BasketRegistrationDao, BasketRegistration> implements IBasketRegistrationService {

    @Resource
    private BasketRegistrationDao basketRegistrationDao;

    @Override
    public R addOrUpdate(BasketRegistrationDto basketRegistrationDto) {
        if (BeanUtil.isEmpty(basketRegistrationDto)) throw new ServiceException("数据不能为空");
        BasketRegistration basketRegistration = new BasketRegistration();
        BeanUtil.copyProperties(basketRegistrationDto,basketRegistration);
        if (basketRegistrationDto.getId() == null){
            basketRegistrationDao.insert(basketRegistration);
            return R.ok("数据插入成功");
        }else {
            basketRegistrationDao.updateById(basketRegistration);
            return R.ok("数据更新成功");
        }
    }
}