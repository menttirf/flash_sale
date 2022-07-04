package cn.wolfcode.web.feign;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.PayVo;
import cn.wolfcode.domain.RefundVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(value = "pay-service")
public interface PayFeignApi {

    @RequestMapping("/alipay/pay")
    Result<String> alipay(@RequestBody PayVo vo);

    @RequestMapping("/alipay/rsaCheckV1")
    Result<Boolean> rsaCheckV1(@RequestParam Map<String,String> params);

    @RequestMapping("/alipay/refund")
    Result<Boolean> refund(@RequestBody RefundVo vo);
}
