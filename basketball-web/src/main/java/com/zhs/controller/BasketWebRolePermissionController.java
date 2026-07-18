package com.zhs.controller;



import com.zhs.service.IBasketWebRolePermissionService;
import com.zhs.model.BasketWebRolePermission;
import com.zhs.dto.BasketWebRolePermissionDto;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.zhs.exception.ServiceException;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author 
 * @since 2026-07-18
 */
@RestController
@RequestMapping("/basket/web/role/permission")
@Api(value = "")
@Slf4j
public class BasketWebRolePermissionController {

    @Resource
    private IBasketWebRolePermissionService  iBasketWebRolePermissionService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketWebRolePermission>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        Page<BasketWebRolePermission> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketWebRolePermission> lambda = new QueryWrapper<BasketWebRolePermission>().lambda();
        lambda.eq(BasketWebRolePermission::getIsDelete,0);
        IPage<BasketWebRolePermission> pages =  iBasketWebRolePermissionService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketWebRolePermission> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketWebRolePermission> wrapper = new QueryWrapper<BasketWebRolePermission>()
                            .lambda().eq(BasketWebRolePermission::getId,id).eq(BasketWebRolePermission::getIsDelete,0);
        return R.ok(iBasketWebRolePermissionService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketWebRolePermission> query = new QueryWrapper<BasketWebRolePermission>().lambda().eq(BasketWebRolePermission::getId, id).eq(BasketWebRolePermission::getIsDelete, 0);
        BasketWebRolePermission basketWebRolePermission = iBasketWebRolePermissionService.getOne(query);
        if(ObjectUtils.isEmpty(basketWebRolePermission)) throw new ServiceException("该数据不存在或者已经被删除");
        basketWebRolePermission.setIsDelete(1);
        iBasketWebRolePermissionService.updateById(basketWebRolePermission);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketWebRolePermissionDto basketWebRolePermissionDto){
        if (CollectionUtils.isEmpty(basketWebRolePermissionDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        List<BasketWebRolePermission> list = new ArrayList<>();
        basketWebRolePermissionDto.getIdList().stream().forEach(id ->{
        BasketWebRolePermission basketWebRolePermission = iBasketWebRolePermissionService.getById(id);
            if (ObjectUtils.isEmpty(basketWebRolePermission)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketWebRolePermission.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketWebRolePermission.setIsDelete(1);
            list.add(basketWebRolePermission);
        });
        iBasketWebRolePermissionService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketWebRolePermissionDto basketWebRolePermissionDto){
        return iBasketWebRolePermissionService.addOrUpdate(basketWebRolePermissionDto);
    }

}
