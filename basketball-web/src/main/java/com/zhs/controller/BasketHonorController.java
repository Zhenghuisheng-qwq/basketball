package com.zhs.controller;



import com.zhs.service.IBasketHonorService;
import com.zhs.model.BasketHonor;
import com.zhs.dto.BasketHonorDto;

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
@RequestMapping("/basket/honor")
@Api(value = "")
@Slf4j
public class BasketHonorController {

    @Resource
    private IBasketHonorService  iBasketHonorService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketHonor>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketHonor> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketHonor> lambda = new QueryWrapper<BasketHonor>().lambda();
        //此处可以拼条件
        lambda.eq(BasketHonor::getIsDelete,0);
        IPage<BasketHonor> pages =  iBasketHonorService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketHonor> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketHonor> wrapper = new QueryWrapper<BasketHonor>()
                            .lambda().eq(BasketHonor::getId,id).eq(BasketHonor::getIsDelete,0);
        return R.ok(iBasketHonorService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketHonor> query = new QueryWrapper<BasketHonor>().lambda().eq(BasketHonor::getId, id).eq(BasketHonor::getIsDelete, 0);
        BasketHonor basketHonor = iBasketHonorService.getOne(query);
        if(ObjectUtils.isEmpty(basketHonor)) throw new ServiceException("该数据不存在或者已经被删除");
        basketHonor.setIsDelete(1);
        iBasketHonorService.updateById(basketHonor);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketHonorDto basketHonorDto){
        if (CollectionUtils.isEmpty(basketHonorDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketHonor> list = new ArrayList<>();
        basketHonorDto.getIdList().stream().forEach(id ->{
        BasketHonor basketHonor = iBasketHonorService.getById(id);
            if (ObjectUtils.isEmpty(basketHonor)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketHonor.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketHonor.setIsDelete(1);
            list.add(basketHonor);
        });
        iBasketHonorService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketHonorDto basketHonorDto){
        return iBasketHonorService.addOrUpdate(basketHonorDto);
    }

}
