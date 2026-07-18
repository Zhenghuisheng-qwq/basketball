package com.zhs.service.impl;


import com.zhs.model.BasketRemoveApply;
import com.zhs.dao.BasketRemoveApplyDao;
import com.zhs.service.IBasketRemoveApplyService;
import com.zhs.dto.BasketRemoveApplyDto;


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
public class BasketRemoveApplyServiceImpl extends ServiceImpl< BasketRemoveApplyDao, BasketRemoveApply> implements IBasketRemoveApplyService {

    @Resource
    private BasketRemoveApplyDao basketRemoveApplyDao;

    @Override
    public R addOrUpdate(BasketRemoveApplyDto basketRemoveApplyDto) {
        if (BeanUtil.isEmpty(basketRemoveApplyDto)) throw new ServiceException("数据不能为空");
        BasketRemoveApply basketRemoveApply = new BasketRemoveApply();
        BeanUtil.copyProperties(basketRemoveApplyDto,basketRemoveApply);
        if (basketRemoveApplyDto.getId() == null){
            basketRemoveApplyDao.insert(basketRemoveApply);
            return R.ok("数据插入成功");
        }else {
            basketRemoveApplyDao.updateById(basketRemoveApply);
            return R.ok("数据更新成功");
        }
    }
}