package com.zhs.service.impl;


import com.zhs.model.BasketWebUser;
import com.zhs.dao.BasketWebUserDao;
import com.zhs.service.IBasketWebUserService;
import com.zhs.dto.BasketWebUserDto;


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
public class BasketWebUserServiceImpl extends ServiceImpl< BasketWebUserDao, BasketWebUser> implements IBasketWebUserService {

    @Resource
    private BasketWebUserDao basketWebUserDao;

    @Override
    public R addOrUpdate(BasketWebUserDto basketWebUserDto) {
        if (BeanUtil.isEmpty(basketWebUserDto)) throw new ServiceException("数据不能为空");
        BasketWebUser basketWebUser = new BasketWebUser();
        BeanUtil.copyProperties(basketWebUserDto,basketWebUser);
        if (basketWebUserDto.getId() == null){
            basketWebUserDao.insert(basketWebUser);
            return R.ok("数据插入成功");
        }else {
            basketWebUserDao.updateById(basketWebUser);
            return R.ok("数据更新成功");
        }
    }
}