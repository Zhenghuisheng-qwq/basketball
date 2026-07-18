package com.zhs.service.impl;


import com.zhs.model.BasketWebPermission;
import com.zhs.dao.BasketWebPermissionDao;
import com.zhs.service.IBasketWebPermissionService;
import com.zhs.dto.BasketWebPermissionDto;


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
public class BasketWebPermissionServiceImpl extends ServiceImpl< BasketWebPermissionDao, BasketWebPermission> implements IBasketWebPermissionService {

    @Resource
    private BasketWebPermissionDao basketWebPermissionDao;

    @Override
    public R addOrUpdate(BasketWebPermissionDto basketWebPermissionDto) {
        if (BeanUtil.isEmpty(basketWebPermissionDto)) throw new ServiceException("数据不能为空");
        BasketWebPermission basketWebPermission = new BasketWebPermission();
        BeanUtil.copyProperties(basketWebPermissionDto,basketWebPermission);
        if (basketWebPermissionDto.getId() == null){
            basketWebPermissionDao.insert(basketWebPermission);
            return R.ok("数据插入成功");
        }else {
            basketWebPermissionDao.updateById(basketWebPermission);
            return R.ok("数据更新成功");
        }
    }
}