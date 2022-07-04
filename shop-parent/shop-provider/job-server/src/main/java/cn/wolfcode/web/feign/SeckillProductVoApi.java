package cn.wolfcode.web.feign;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.job.SeckillProductCacheJob;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Component
@FeignClient(name = "seckill-service",fallback = SeckillProductCacheJob.class)
public interface SeckillProductVoApi {

    @RequestMapping("/seckillProduct/queryByTimeForApi")
    Result<List<SeckillProductVo>> seckillProductVoList(@RequestParam("time") Integer time);
}
