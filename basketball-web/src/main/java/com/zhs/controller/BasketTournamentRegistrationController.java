package com.zhs.controller;



import com.zhs.service.IBasketTournamentRegistrationService;
import com.zhs.model.BasketTournamentRegistration;
import com.zhs.dto.BasketTournamentRegistrationDto;

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
@RequestMapping("/basket/tournament/registration")
@Api(value = "")
@Slf4j
public class BasketTournamentRegistrationController {

    @Resource
    private IBasketTournamentRegistrationService  iBasketTournamentRegistrationService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketTournamentRegistration>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketTournamentRegistration> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketTournamentRegistration> lambda = new QueryWrapper<BasketTournamentRegistration>().lambda();
        //此处可以拼条件
        lambda.eq(BasketTournamentRegistration::getIsDelete,0);
        IPage<BasketTournamentRegistration> pages =  iBasketTournamentRegistrationService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketTournamentRegistration> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketTournamentRegistration> wrapper = new QueryWrapper<BasketTournamentRegistration>()
                            .lambda().eq(BasketTournamentRegistration::getId,id).eq(BasketTournamentRegistration::getIsDelete,0);
        return R.ok(iBasketTournamentRegistrationService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketTournamentRegistration> query = new QueryWrapper<BasketTournamentRegistration>().lambda().eq(BasketTournamentRegistration::getId, id).eq(BasketTournamentRegistration::getIsDelete, 0);
        BasketTournamentRegistration basketTournamentRegistration = iBasketTournamentRegistrationService.getOne(query);
        if(ObjectUtils.isEmpty(basketTournamentRegistration)) throw new ServiceException("该数据不存在或者已经被删除");
        basketTournamentRegistration.setIsDelete(1);
        iBasketTournamentRegistrationService.updateById(basketTournamentRegistration);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketTournamentRegistrationDto basketTournamentRegistrationDto){
        if (CollectionUtils.isEmpty(basketTournamentRegistrationDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketTournamentRegistration> list = new ArrayList<>();
        basketTournamentRegistrationDto.getIdList().stream().forEach(id ->{
        BasketTournamentRegistration basketTournamentRegistration = iBasketTournamentRegistrationService.getById(id);
            if (ObjectUtils.isEmpty(basketTournamentRegistration)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketTournamentRegistration.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketTournamentRegistration.setIsDelete(1);
            list.add(basketTournamentRegistration);
        });
        iBasketTournamentRegistrationService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketTournamentRegistrationDto basketTournamentRegistrationDto){
        return iBasketTournamentRegistrationService.addOrUpdate(basketTournamentRegistrationDto);
    }

}
