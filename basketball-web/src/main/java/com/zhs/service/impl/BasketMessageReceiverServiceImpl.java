package com.zhs.service.impl;


import com.zhs.model.BasketMessageReceiver;
import com.zhs.dao.BasketMessageReceiverDao;
import com.zhs.service.IBasketMessageReceiverService;
import com.zhs.dto.BasketMessageReceiverDto;


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
public class BasketMessageReceiverServiceImpl extends ServiceImpl< BasketMessageReceiverDao, BasketMessageReceiver> implements IBasketMessageReceiverService {

    @Resource
    private BasketMessageReceiverDao basketMessageReceiverDao;

    @Override
    public R addOrUpdate(BasketMessageReceiverDto basketMessageReceiverDto) {
        if (BeanUtil.isEmpty(basketMessageReceiverDto)) throw new ServiceException("数据不能为空");
        BasketMessageReceiver basketMessageReceiver = new BasketMessageReceiver();
        BeanUtil.copyProperties(basketMessageReceiverDto,basketMessageReceiver);
        if (basketMessageReceiverDto.getId() == null){
            basketMessageReceiverDao.insert(basketMessageReceiver);
            return R.ok("数据插入成功");
        }else {
            basketMessageReceiverDao.updateById(basketMessageReceiver);
            return R.ok("数据更新成功");
        }
    }
}