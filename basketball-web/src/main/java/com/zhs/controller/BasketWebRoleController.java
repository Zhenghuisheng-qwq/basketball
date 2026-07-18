package com.zhs.controller;



import com.zhs.service.IBasketWebRoleService;
import com.zhs.model.BasketWebRole;
import com.zhs.dto.BasketWebRoleDto;

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
@RequestMapping("/basket/web/role")
@Api(value = "")
@Slf4j
public class BasketWebRoleController {

    @Resource
    private IBasketWebRoleService  iBasketWebRoleService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketWebRole>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketWebRole> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketWebRole> lambda = new QueryWrapper<BasketWebRole>().lambda();
        //此处可以拼条件
        lambda.eq(BasketWebRole::getIsDelete,0);
        IPage<BasketWebRole> pages =  iBasketWebRoleService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketWebRole> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketWebRole> wrapper = new QueryWrapper<BasketWebRole>()
                            .lambda().eq(BasketWebRole::getId,id).eq(BasketWebRole::getIsDelete,0);
        return R.ok(iBasketWebRoleService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketWebRole> query = new QueryWrapper<BasketWebRole>().lambda().eq(BasketWebRole::getId, id).eq(BasketWebRole::getIsDelete, 0);
        BasketWebRole basketWebRole = iBasketWebRoleService.getOne(query);
        if(ObjectUtils.isEmpty(basketWebRole)) throw new ServiceException("该数据不存在或者已经被删除");
        basketWebRole.setIsDelete(1);
        iBasketWebRoleService.updateById(basketWebRole);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketWebRoleDto basketWebRoleDto){
        if (CollectionUtils.isEmpty(basketWebRoleDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketWebRole> list = new ArrayList<>();
        basketWebRoleDto.getIdList().stream().forEach(id ->{
        BasketWebRole basketWebRole = iBasketWebRoleService.getById(id);
            if (ObjectUtils.isEmpty(basketWebRole)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketWebRole.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketWebRole.setIsDelete(1);
            list.add(basketWebRole);
        });
        iBasketWebRoleService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketWebRoleDto basketWebRoleDto){
        return iBasketWebRoleService.addOrUpdate(basketWebRoleDto);
    }

}
