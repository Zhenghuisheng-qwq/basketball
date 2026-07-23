package com.zhs.controller;



import com.zhs.service.IBasketFileAssetService;
import com.zhs.model.BasketFileAsset;
import com.zhs.dto.BasketFileAssetDto;

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
@RequestMapping("/basket/file/asset")
@Api(value = "")
@Slf4j
public class BasketFileAssetController {

    @Resource
    private IBasketFileAssetService  iBasketFileAssetService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketFileAsset>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketFileAsset> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketFileAsset> lambda = new QueryWrapper<BasketFileAsset>().lambda();
        //此处可以拼条件
        lambda.eq(BasketFileAsset::getIsDelete,0);
        IPage<BasketFileAsset> pages =  iBasketFileAssetService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketFileAsset> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketFileAsset> wrapper = new QueryWrapper<BasketFileAsset>()
                            .lambda().eq(BasketFileAsset::getId,id).eq(BasketFileAsset::getIsDelete,0);
        return R.ok(iBasketFileAssetService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketFileAsset> query = new QueryWrapper<BasketFileAsset>().lambda().eq(BasketFileAsset::getId, id).eq(BasketFileAsset::getIsDelete, 0);
        BasketFileAsset basketFileAsset = iBasketFileAssetService.getOne(query);
        if(ObjectUtils.isEmpty(basketFileAsset)) throw new ServiceException("该数据不存在或者已经被删除");
        basketFileAsset.setIsDelete(1);
        iBasketFileAssetService.updateById(basketFileAsset);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketFileAssetDto basketFileAssetDto){
        if (CollectionUtils.isEmpty(basketFileAssetDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketFileAsset> list = new ArrayList<>();
        basketFileAssetDto.getIdList().stream().forEach(id ->{
        BasketFileAsset basketFileAsset = iBasketFileAssetService.getById(id);
            if (ObjectUtils.isEmpty(basketFileAsset)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketFileAsset.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketFileAsset.setIsDelete(1);
            list.add(basketFileAsset);
        });
        iBasketFileAssetService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketFileAssetDto basketFileAssetDto){
        return iBasketFileAssetService.addOrUpdate(basketFileAssetDto);
    }

}
