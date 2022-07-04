package cn.wolfcode.web.controller;


import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.web.feign.PayFeignApi;
import cn.wolfcode.web.feign.ProductFeignApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * Created by lanxw
 */
@RestController
@RequestMapping("/orderPay")
@RefreshScope
public class OrderPayController {
    @Autowired
    private IOrderInfoService orderInfoService;

    @Autowired
    private PayFeignApi payFeignApi;

    @RequestMapping("/pay")
    public Result<String> pay(String orderNo, Integer type) {
        //判断支付类型
        if (OrderInfo.PAYTYPE_ONLINE.equals(type)) {
            //现金支付
            return Result.success(orderInfoService.payOnLine(orderNo));
        }else {
            //积分支付
            orderInfoService.integralPay(orderNo);
            return Result.success();
        }
    }

    //异步回调
    //接收支付宝传来的参数以key-value的形式封装
    @RequestMapping("/notify_url")
    public String notifyUrl(@RequestParam Map<String, String> params) {
        System.out.println("异步回调" + new Date());
        //远程调用，支付服务
        Result<Boolean> result = payFeignApi.rsaCheckV1(params);
        if (result == null || result.hasError()) {
            return "fail";
        }
        Boolean data = result.getData();
        if (data) {
            //修改订单状态
            orderInfoService.paySuccess(params.get("out_trade_no"));
            return "success";
        } else {
            return "fail";
        }
    }

    //同步回调
    @Value("${pay.errorUrl}")
    private String errorUrl;
    @Value("${pay.frontEndPayUrl}")
    private String frontEndPayUrl;
    @RequestMapping("/return_url")
    public void returnrl(@RequestParam Map<String, String> params,HttpServletResponse response) throws IOException {
        System.out.println("同步回调" + new Date());
        //远程调用，支付服务
        Result<Boolean> result = payFeignApi.rsaCheckV1(params);
        if (result == null || result.hasError()) {
            response.sendRedirect(errorUrl);
        }
        Boolean data = result.getData();
        if (data) {
            String orderNo = params.get("out_trade_no");
            response.sendRedirect(frontEndPayUrl + orderNo);
        } else {
            response.sendRedirect(errorUrl);
        }
    }

    //退款业务
    @RequestMapping("/refund")
    public Result<String> refund(String orderNo){
        orderInfoService.refundOnLine(orderNo);
        return Result.success();
    }


}
