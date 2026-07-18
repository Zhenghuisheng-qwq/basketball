package com.zhs.controller;



import com.zhs.service.IBasketRegistrationPlayerService;
import com.zhs.model.BasketRegistrationPlayer;
import com.zhs.dto.BasketRegistrationPlayerDto;

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
@RequestMapping("/basket/registration/player")
@Api(value = "")
@Slf4j
public class BasketRegistrationPlayerController {

    @Resource
    private IBasketRegistrationPlayerService  iBasketRegistrationPlayerService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketRegistrationPlayer>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketRegistrationPlayer> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketRegistrationPlayer> lambda = new QueryWrapper<BasketRegistrationPlayer>().lambda();
        //此处可以拼条件
        lambda.eq(BasketRegistrationPlayer::getIsDelete,0);
        IPage<BasketRegistrationPlayer> pages =  iBasketRegistrationPlayerService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketRegistrationPlayer> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketRegistrationPlayer> wrapper = new QueryWrapper<BasketRegistrationPlayer>()
                            .lambda().eq(BasketRegistrationPlayer::getId,id).eq(BasketRegistrationPlayer::getIsDelete,0);
        return R.ok(iBasketRegistrationPlayerService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketRegistrationPlayer> query = new QueryWrapper<BasketRegistrationPlayer>().lambda().eq(BasketRegistrationPlayer::getId, id).eq(BasketRegistrationPlayer::getIsDelete, 0);
        BasketRegistrationPlayer basketRegistrationPlayer = iBasketRegistrationPlayerService.getOne(query);
        if(ObjectUtils.isEmpty(basketRegistrationPlayer)) throw new ServiceException("该数据不存在或者已经被删除");
        basketRegistrationPlayer.setIsDelete(1);
        iBasketRegistrationPlayerService.updateById(basketRegistrationPlayer);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketRegistrationPlayerDto basketRegistrationPlayerDto){
        if (CollectionUtils.isEmpty(basketRegistrationPlayerDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketRegistrationPlayer> list = new ArrayList<>();
        basketRegistrationPlayerDto.getIdList().stream().forEach(id ->{
        BasketRegistrationPlayer basketRegistrationPlayer = iBasketRegistrationPlayerService.getById(id);
            if (ObjectUtils.isEmpty(basketRegistrationPlayer)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketRegistrationPlayer.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketRegistrationPlayer.setIsDelete(1);
            list.add(basketRegistrationPlayer);
        });
        iBasketRegistrationPlayerService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketRegistrationPlayerDto basketRegistrationPlayerDto){
        return iBasketRegistrationPlayerService.addOrUpdate(basketRegistrationPlayerDto);
    }

}
