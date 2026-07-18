package com.zhs.service.impl;


import com.zhs.model.BasketInviteApply;
import com.zhs.dao.BasketInviteApplyDao;
import com.zhs.service.IBasketInviteApplyService;
import com.zhs.dto.BasketInviteApplyDto;


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
public class BasketInviteApplyServiceImpl extends ServiceImpl< BasketInviteApplyDao, BasketInviteApply> implements IBasketInviteApplyService {

    @Resource
    private BasketInviteApplyDao basketInviteApplyDao;

    @Override
    public R addOrUpdate(BasketInviteApplyDto basketInviteApplyDto) {
        if (BeanUtil.isEmpty(basketInviteApplyDto)) throw new ServiceException("数据不能为空");
        BasketInviteApply basketInviteApply = new BasketInviteApply();
        BeanUtil.copyProperties(basketInviteApplyDto,basketInviteApply);
        if (basketInviteApplyDto.getId() == null){
            basketInviteApplyDao.insert(basketInviteApply);
            return R.ok("数据插入成功");
        }else {
            basketInviteApplyDao.updateById(basketInviteApply);
            return R.ok("数据更新成功");
        }
    }
}