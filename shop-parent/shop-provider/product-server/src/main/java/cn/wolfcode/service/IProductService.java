package cn.wolfcode.service;

import cn.wolfcode.domain.Product;

import java.util.List;

/**
 * Created by lanxw
 */
public interface IProductService {
    /**
     * 根据秒杀商品id查询商品信息
     * @param ids
     * @return
     */
    List<Product> queryByIds(List<Long> ids);
}
