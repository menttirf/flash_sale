package cn.wolfcode.web.controller;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.OperateIntegralVo;
import cn.wolfcode.service.IUsableIntegralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by lanxw
 */
@RestController
@RequestMapping("/integral")
public class IntegralController {
    @Autowired
    private IUsableIntegralService usableIntegralService;

    //积分支付
    @RequestMapping("/decrValue")
    public Result<Boolean> decrValue(@RequestBody OperateIntegralVo vo){
        //usableIntegralService.decrValue(vo);
        return Result.success(usableIntegralService.decrValueTry(vo,null));
    }

    //积分退还（增加）
    @RequestMapping("/incrValue")
    public Result<Boolean> incrValue(@RequestBody OperateIntegralVo vo){
        //usableIntegralService.incrValue(vo);
        return Result.success(usableIntegralService.incrValueTry(vo,null));
    }
}
