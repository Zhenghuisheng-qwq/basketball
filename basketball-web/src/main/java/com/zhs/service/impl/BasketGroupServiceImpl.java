package com.zhs.service.impl;


import com.zhs.model.BasketGroup;
import com.zhs.dao.BasketGroupDao;
import com.zhs.service.IBasketGroupService;
import com.zhs.dto.BasketGroupDto;


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
public class BasketGroupServiceImpl extends ServiceImpl< BasketGroupDao, BasketGroup> implements IBasketGroupService {

    @Resource
    private BasketGroupDao basketGroupDao;

    @Override
    public R addOrUpdate(BasketGroupDto basketGroupDto) {
        if (BeanUtil.isEmpty(basketGroupDto)) throw new ServiceException("数据不能为空");
        BasketGroup basketGroup = new BasketGroup();
        BeanUtil.copyProperties(basketGroupDto,basketGroup);
        if (basketGroupDto.getId() == null){
            basketGroupDao.insert(basketGroup);
            return R.ok("数据插入成功");
        }else {
            basketGroupDao.updateById(basketGroup);
            return R.ok("数据更新成功");
        }
    }
}