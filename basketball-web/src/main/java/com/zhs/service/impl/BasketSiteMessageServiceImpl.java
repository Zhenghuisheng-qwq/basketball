package com.zhs.service.impl;


import com.zhs.model.BasketSiteMessage;
import com.zhs.dao.BasketSiteMessageDao;
import com.zhs.service.IBasketSiteMessageService;
import com.zhs.dto.BasketSiteMessageDto;


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
public class BasketSiteMessageServiceImpl extends ServiceImpl< BasketSiteMessageDao, BasketSiteMessage> implements IBasketSiteMessageService {

    @Resource
    private BasketSiteMessageDao basketSiteMessageDao;

    @Override
    public R addOrUpdate(BasketSiteMessageDto basketSiteMessageDto) {
        if (BeanUtil.isEmpty(basketSiteMessageDto)) throw new ServiceException("数据不能为空");
        BasketSiteMessage basketSiteMessage = new BasketSiteMessage();
        BeanUtil.copyProperties(basketSiteMessageDto,basketSiteMessage);
        if (basketSiteMessageDto.getId() == null){
            basketSiteMessageDao.insert(basketSiteMessage);
            return R.ok("数据插入成功");
        }else {
            basketSiteMessageDao.updateById(basketSiteMessage);
            return R.ok("数据更新成功");
        }
    }
}