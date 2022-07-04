package cn.wolfcode.service;

import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.RefundLog;

import java.util.Date;

public interface IRefundLogService {
    /**
     * 支付宝退款记录
     */
    void saveByRefundLog(RefundLog log);
}
