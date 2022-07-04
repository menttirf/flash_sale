package cn.wolfcode.web.feign.fallback;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.Product;
import cn.wolfcode.web.feign.ProductFeignApi;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Component
public class ProductFeignFallback implements ProductFeignApi {
    @Override
    public Result<List<Product>> queryByIds(List<Long> ids) {
        return null;
    }
}
