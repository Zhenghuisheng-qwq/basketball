package com.zhs.controller;



import com.zhs.service.IBasketFavoriteService;
import com.zhs.model.BasketFavorite;
import com.zhs.dto.BasketFavoriteDto;

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
@RequestMapping("/basket/favorite")
@Api(value = "")
@Slf4j
public class BasketFavoriteController {

    @Resource
    private IBasketFavoriteService  iBasketFavoriteService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketFavorite>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketFavorite> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketFavorite> lambda = new QueryWrapper<BasketFavorite>().lambda();
        //此处可以拼条件
        lambda.eq(BasketFavorite::getIsDelete,0);
        IPage<BasketFavorite> pages =  iBasketFavoriteService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketFavorite> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketFavorite> wrapper = new QueryWrapper<BasketFavorite>()
                            .lambda().eq(BasketFavorite::getId,id).eq(BasketFavorite::getIsDelete,0);
        return R.ok(iBasketFavoriteService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketFavorite> query = new QueryWrapper<BasketFavorite>().lambda().eq(BasketFavorite::getId, id).eq(BasketFavorite::getIsDelete, 0);
        BasketFavorite basketFavorite = iBasketFavoriteService.getOne(query);
        if(ObjectUtils.isEmpty(basketFavorite)) throw new ServiceException("该数据不存在或者已经被删除");
        basketFavorite.setIsDelete(1);
        iBasketFavoriteService.updateById(basketFavorite);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketFavoriteDto basketFavoriteDto){
        if (CollectionUtils.isEmpty(basketFavoriteDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketFavorite> list = new ArrayList<>();
        basketFavoriteDto.getIdList().stream().forEach(id ->{
        BasketFavorite basketFavorite = iBasketFavoriteService.getById(id);
            if (ObjectUtils.isEmpty(basketFavorite)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketFavorite.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketFavorite.setIsDelete(1);
            list.add(basketFavorite);
        });
        iBasketFavoriteService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketFavoriteDto basketFavoriteDto){
        return iBasketFavoriteService.addOrUpdate(basketFavoriteDto);
    }

}
