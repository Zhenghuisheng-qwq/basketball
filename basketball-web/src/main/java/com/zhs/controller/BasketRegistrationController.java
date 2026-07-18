package com.zhs.controller;



import com.zhs.service.IBasketRegistrationService;
import com.zhs.model.BasketRegistration;
import com.zhs.dto.BasketRegistrationDto;

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
@RequestMapping("/basket/registration")
@Api(value = "")
@Slf4j
public class BasketRegistrationController {

    @Resource
    private IBasketRegistrationService  iBasketRegistrationService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketRegistration>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketRegistration> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketRegistration> lambda = new QueryWrapper<BasketRegistration>().lambda();
        //此处可以拼条件
        lambda.eq(BasketRegistration::getIsDelete,0);
        IPage<BasketRegistration> pages =  iBasketRegistrationService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketRegistration> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketRegistration> wrapper = new QueryWrapper<BasketRegistration>()
                            .lambda().eq(BasketRegistration::getId,id).eq(BasketRegistration::getIsDelete,0);
        return R.ok(iBasketRegistrationService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketRegistration> query = new QueryWrapper<BasketRegistration>().lambda().eq(BasketRegistration::getId, id).eq(BasketRegistration::getIsDelete, 0);
        BasketRegistration basketRegistration = iBasketRegistrationService.getOne(query);
        if(ObjectUtils.isEmpty(basketRegistration)) throw new ServiceException("该数据不存在或者已经被删除");
        basketRegistration.setIsDelete(1);
        iBasketRegistrationService.updateById(basketRegistration);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketRegistrationDto basketRegistrationDto){
        if (CollectionUtils.isEmpty(basketRegistrationDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketRegistration> list = new ArrayList<>();
        basketRegistrationDto.getIdList().stream().forEach(id ->{
        BasketRegistration basketRegistration = iBasketRegistrationService.getById(id);
            if (ObjectUtils.isEmpty(basketRegistration)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketRegistration.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketRegistration.setIsDelete(1);
            list.add(basketRegistration);
        });
        iBasketRegistrationService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketRegistrationDto basketRegistrationDto){
        return iBasketRegistrationService.addOrUpdate(basketRegistrationDto);
    }

}
