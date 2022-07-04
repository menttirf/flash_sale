package cn.wolfcode.mapper;

import cn.wolfcode.domain.OrderInfo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/**
 * Created by wolfcode-lanxw
 */
public interface RefundLogMapper {

    //支付宝退款记录
    void saveByRefundLog(@Param("orderNo") String orderNo, @Param("date") Date date, @Param("seckillPrice") Long seckillPrice, @Param("reason") String reason, @Param("statusRefund") Integer statusRefund);
}
