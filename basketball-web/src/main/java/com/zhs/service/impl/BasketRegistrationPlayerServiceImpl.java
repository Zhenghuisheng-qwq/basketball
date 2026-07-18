package com.zhs.service.impl;


import com.zhs.model.BasketRegistrationPlayer;
import com.zhs.dao.BasketRegistrationPlayerDao;
import com.zhs.service.IBasketRegistrationPlayerService;
import com.zhs.dto.BasketRegistrationPlayerDto;


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
public class BasketRegistrationPlayerServiceImpl extends ServiceImpl< BasketRegistrationPlayerDao, BasketRegistrationPlayer> implements IBasketRegistrationPlayerService {

    @Resource
    private BasketRegistrationPlayerDao basketRegistrationPlayerDao;

    @Override
    public R addOrUpdate(BasketRegistrationPlayerDto basketRegistrationPlayerDto) {
        if (BeanUtil.isEmpty(basketRegistrationPlayerDto)) throw new ServiceException("数据不能为空");
        BasketRegistrationPlayer basketRegistrationPlayer = new BasketRegistrationPlayer();
        BeanUtil.copyProperties(basketRegistrationPlayerDto,basketRegistrationPlayer);
        if (basketRegistrationPlayerDto.getId() == null){
            basketRegistrationPlayerDao.insert(basketRegistrationPlayer);
            return R.ok("数据插入成功");
        }else {
            basketRegistrationPlayerDao.updateById(basketRegistrationPlayer);
            return R.ok("数据更新成功");
        }
    }
}