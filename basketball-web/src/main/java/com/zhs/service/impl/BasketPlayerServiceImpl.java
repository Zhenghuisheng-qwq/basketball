package com.zhs.service.impl;


import com.zhs.model.BasketPlayer;
import com.zhs.dao.BasketPlayerDao;
import com.zhs.service.IBasketPlayerService;
import com.zhs.dto.BasketPlayerDto;


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
public class BasketPlayerServiceImpl extends ServiceImpl< BasketPlayerDao, BasketPlayer> implements IBasketPlayerService {

    @Resource
    private BasketPlayerDao basketPlayerDao;

    @Override
    public R addOrUpdate(BasketPlayerDto basketPlayerDto) {
        if (BeanUtil.isEmpty(basketPlayerDto)) throw new ServiceException("数据不能为空");
        BasketPlayer basketPlayer = new BasketPlayer();
        BeanUtil.copyProperties(basketPlayerDto,basketPlayer);
        if (basketPlayerDto.getId() == null){
            basketPlayerDao.insert(basketPlayer);
            return R.ok("数据插入成功");
        }else {
            basketPlayerDao.updateById(basketPlayer);
            return R.ok("数据更新成功");
        }
    }
}