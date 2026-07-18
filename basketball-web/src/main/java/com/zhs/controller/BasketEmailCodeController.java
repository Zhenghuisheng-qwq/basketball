package com.zhs.controller;



import com.zhs.service.IBasketEmailCodeService;
import com.zhs.model.BasketEmailCode;
import com.zhs.dto.BasketEmailCodeDto;

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
@RequestMapping("/basket/email/code")
@Api(value = "")
@Slf4j
public class BasketEmailCodeController {

    @Resource
    private IBasketEmailCodeService  iBasketEmailCodeService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketEmailCode>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketEmailCode> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketEmailCode> lambda = new QueryWrapper<BasketEmailCode>().lambda();
        //此处可以拼条件
        lambda.eq(BasketEmailCode::getIsDelete,0);
        IPage<BasketEmailCode> pages =  iBasketEmailCodeService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketEmailCode> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketEmailCode> wrapper = new QueryWrapper<BasketEmailCode>()
                            .lambda().eq(BasketEmailCode::getId,id).eq(BasketEmailCode::getIsDelete,0);
        return R.ok(iBasketEmailCodeService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketEmailCode> query = new QueryWrapper<BasketEmailCode>().lambda().eq(BasketEmailCode::getId, id).eq(BasketEmailCode::getIsDelete, 0);
        BasketEmailCode basketEmailCode = iBasketEmailCodeService.getOne(query);
        if(ObjectUtils.isEmpty(basketEmailCode)) throw new ServiceException("该数据不存在或者已经被删除");
        basketEmailCode.setIsDelete(1);
        iBasketEmailCodeService.updateById(basketEmailCode);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketEmailCodeDto basketEmailCodeDto){
        if (CollectionUtils.isEmpty(basketEmailCodeDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketEmailCode> list = new ArrayList<>();
        basketEmailCodeDto.getIdList().stream().forEach(id ->{
        BasketEmailCode basketEmailCode = iBasketEmailCodeService.getById(id);
            if (ObjectUtils.isEmpty(basketEmailCode)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketEmailCode.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketEmailCode.setIsDelete(1);
            list.add(basketEmailCode);
        });
        iBasketEmailCodeService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketEmailCodeDto basketEmailCodeDto){
        return iBasketEmailCodeService.addOrUpdate(basketEmailCodeDto);
    }

}
