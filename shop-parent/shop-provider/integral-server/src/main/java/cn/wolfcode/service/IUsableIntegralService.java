package cn.wolfcode.service;

import cn.wolfcode.domain.OperateIntegralVo;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * Created by lanxw
 */
@LocalTCC
public interface IUsableIntegralService {
    /**
     * 积分支付
     * @param vo
     */
    void decrValue(OperateIntegralVo vo);

    /**
     * 积分退还
     * @param vo
     */
    void incrValue(OperateIntegralVo vo);

    //积分支付
    //try方法
    @TwoPhaseBusinessAction(name = "decrValueTry",commitMethod = "decrValueConfirm",rollbackMethod = "decrValueCancel")
    Boolean decrValueTry(@BusinessActionContextParameter(paramName = "vo") OperateIntegralVo vo,BusinessActionContext context);
    //confirm方法
    void decrValueConfirm(BusinessActionContext context);
    //cancel方法
    void decrValueCancel(BusinessActionContext context);


    //积分退款
    @TwoPhaseBusinessAction(name = "incrValueTry",commitMethod = "incrValueConfirm",rollbackMethod = "incrValueCancel")
    Boolean incrValueTry(@BusinessActionContextParameter(paramName = "vo") OperateIntegralVo vo, BusinessActionContext context);
    //confirm方法
    void incrValueConfirm(BusinessActionContext context);
    //cancel方法
    void incrValueCancel(BusinessActionContext context);
}
