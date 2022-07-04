package cn.wolfcode.web.feign;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.OperateIntegralVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(value = "integral-service")
public interface IntegralFeignApi {

    @RequestMapping("/integral/decrValue")
    Result<Boolean> decrValue(@RequestBody OperateIntegralVo vo);

    @RequestMapping("/integral/incrValue")
    Result<Boolean> incrValue(@RequestBody OperateIntegralVo integralVo);
}
