package cn.wolfcode.service.impl;

import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.RefundLog;
import cn.wolfcode.mapper.RefundLogMapper;
import cn.wolfcode.service.IRefundLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class RefundLogServiceImpl implements IRefundLogService {

    @Autowired
    private RefundLogMapper refundLogMapper;

    //支付宝退款记录

    @Override
    public void saveByRefundLog(RefundLog log) {
        refundLogMapper.saveByRefundLog(log.getOrderNo(), log.getRefundTime(), log.getRefundAmount(), log.getRefundReason(), log.getRefundType());
    }
}
