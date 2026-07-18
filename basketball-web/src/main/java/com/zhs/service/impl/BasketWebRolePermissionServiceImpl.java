package com.zhs.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhs.dao.BasketWebRolePermissionDao;
import com.zhs.dto.BasketWebRolePermissionDto;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketWebRolePermission;
import com.zhs.service.IBasketWebRolePermissionService;
import com.zhs.util.R;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 *
 * @author 
 * @since 2026-07-18
 */
@Service
@Slf4j
public class BasketWebRolePermissionServiceImpl extends ServiceImpl<BasketWebRolePermissionDao, BasketWebRolePermission> implements IBasketWebRolePermissionService {

    @Resource
    private BasketWebRolePermissionDao basketWebRolePermissionDao;

    @Override
    public R addOrUpdate(BasketWebRolePermissionDto basketWebRolePermissionDto) {
        if (BeanUtil.isEmpty(basketWebRolePermissionDto)) throw new ServiceException("数据不能为空");
        BasketWebRolePermission basketWebRolePermission = new BasketWebRolePermission();
        BeanUtil.copyProperties(basketWebRolePermissionDto, basketWebRolePermission);
        if (basketWebRolePermissionDto.getId() == null) {
            basketWebRolePermissionDao.insert(basketWebRolePermission);
            return R.ok("数据插入成功");
        } else {
            basketWebRolePermissionDao.updateById(basketWebRolePermission);
            return R.ok("数据更新成功");
        }
    }
}
