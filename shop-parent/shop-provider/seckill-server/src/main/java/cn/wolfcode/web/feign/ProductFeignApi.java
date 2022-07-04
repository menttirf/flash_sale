package cn.wolfcode.web.feign;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.Product;
import cn.wolfcode.web.feign.fallback.ProductFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(value = "product-service")
public interface ProductFeignApi {
    /**
     * 远程调用商品信息
     * @param ids 秒杀商品ids
     * @return 返回商品信息
     */
    @RequestMapping("/product/queryByIds")
    Result<List<Product>> queryByIds(@RequestParam("ids") List<Long> ids);
}
