package com.zhs.controller;



import com.zhs.service.IBasketWebUserRoleService;
import com.zhs.model.BasketWebUserRole;
import com.zhs.dto.BasketWebUserRoleDto;

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
@RequestMapping("/basket/web/user/role")
@Api(value = "")
@Slf4j
public class BasketWebUserRoleController {

    @Resource
    private IBasketWebUserRoleService  iBasketWebUserRoleService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketWebUserRole>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        Page<BasketWebUserRole> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketWebUserRole> lambda = new QueryWrapper<BasketWebUserRole>().lambda();
        lambda.eq(BasketWebUserRole::getIsDelete,0);
        IPage<BasketWebUserRole> pages =  iBasketWebUserRoleService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketWebUserRole> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketWebUserRole> wrapper = new QueryWrapper<BasketWebUserRole>()
                            .lambda().eq(BasketWebUserRole::getId,id).eq(BasketWebUserRole::getIsDelete,0);
        return R.ok(iBasketWebUserRoleService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketWebUserRole> query = new QueryWrapper<BasketWebUserRole>().lambda().eq(BasketWebUserRole::getId, id).eq(BasketWebUserRole::getIsDelete, 0);
        BasketWebUserRole basketWebUserRole = iBasketWebUserRoleService.getOne(query);
        if(ObjectUtils.isEmpty(basketWebUserRole)) throw new ServiceException("该数据不存在或者已经被删除");
        basketWebUserRole.setIsDelete(1);
        iBasketWebUserRoleService.updateById(basketWebUserRole);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketWebUserRoleDto basketWebUserRoleDto){
        if (CollectionUtils.isEmpty(basketWebUserRoleDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        List<BasketWebUserRole> list = new ArrayList<>();
        basketWebUserRoleDto.getIdList().stream().forEach(id ->{
        BasketWebUserRole basketWebUserRole = iBasketWebUserRoleService.getById(id);
            if (ObjectUtils.isEmpty(basketWebUserRole)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketWebUserRole.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketWebUserRole.setIsDelete(1);
            list.add(basketWebUserRole);
        });
        iBasketWebUserRoleService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketWebUserRoleDto basketWebUserRoleDto){
        return iBasketWebUserRoleService.addOrUpdate(basketWebUserRoleDto);
    }

}
