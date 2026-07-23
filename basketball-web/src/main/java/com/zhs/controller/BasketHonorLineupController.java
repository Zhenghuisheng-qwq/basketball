package com.zhs.controller;



import com.zhs.service.IBasketHonorLineupService;
import com.zhs.model.BasketHonorLineup;
import com.zhs.dto.BasketHonorLineupDto;

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
 * @since 2026-07-24
 */
@RestController
@RequestMapping("/basket/honor/lineup")
@Api(value = "")
@Slf4j
public class BasketHonorLineupController {

    @Resource
    private IBasketHonorLineupService  iBasketHonorLineupService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketHonorLineup>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketHonorLineup> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketHonorLineup> lambda = new QueryWrapper<BasketHonorLineup>().lambda();
        //此处可以拼条件
        lambda.eq(BasketHonorLineup::getIsDelete,0);
        IPage<BasketHonorLineup> pages =  iBasketHonorLineupService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketHonorLineup> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketHonorLineup> wrapper = new QueryWrapper<BasketHonorLineup>()
                            .lambda().eq(BasketHonorLineup::getId,id).eq(BasketHonorLineup::getIsDelete,0);
        return R.ok(iBasketHonorLineupService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketHonorLineup> query = new QueryWrapper<BasketHonorLineup>().lambda().eq(BasketHonorLineup::getId, id).eq(BasketHonorLineup::getIsDelete, 0);
        BasketHonorLineup basketHonorLineup = iBasketHonorLineupService.getOne(query);
        if(ObjectUtils.isEmpty(basketHonorLineup)) throw new ServiceException("该数据不存在或者已经被删除");
        basketHonorLineup.setIsDelete(1);
        iBasketHonorLineupService.updateById(basketHonorLineup);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketHonorLineupDto basketHonorLineupDto){
        if (CollectionUtils.isEmpty(basketHonorLineupDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketHonorLineup> list = new ArrayList<>();
        basketHonorLineupDto.getIdList().stream().forEach(id ->{
        BasketHonorLineup basketHonorLineup = iBasketHonorLineupService.getById(id);
            if (ObjectUtils.isEmpty(basketHonorLineup)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketHonorLineup.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketHonorLineup.setIsDelete(1);
            list.add(basketHonorLineup);
        });
        iBasketHonorLineupService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketHonorLineupDto basketHonorLineupDto){
        return iBasketHonorLineupService.addOrUpdate(basketHonorLineupDto);
    }

}
