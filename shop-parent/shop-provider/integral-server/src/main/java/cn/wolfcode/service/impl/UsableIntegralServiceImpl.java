package cn.wolfcode.service.impl;

import cn.wolfcode.domain.AccountTransaction;
import cn.wolfcode.domain.OperateIntegralVo;
import cn.wolfcode.mapper.AccountTransactionMapper;
import cn.wolfcode.mapper.UsableIntegralMapper;
import cn.wolfcode.service.IUsableIntegralService;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Created by lanxw
 */
@Service
public class UsableIntegralServiceImpl implements IUsableIntegralService {
    @Autowired
    private UsableIntegralMapper usableIntegralMapper;
    @Autowired
    private AccountTransactionMapper accountTransactionMapper;

    //积分扣减
    @Override
    public void decrValue(OperateIntegralVo vo) {
        usableIntegralMapper.decrIntegral(vo.getUserId(), vo.getValue());
    }

    //积分退还
    @Override
    public void incrValue(OperateIntegralVo vo) {
        usableIntegralMapper.incrIntegral(vo.getUserId(), vo.getValue());
    }

    //积分支付
    //try方法
    @Override
    @Transactional
    public Boolean decrValueTry(OperateIntegralVo vo, BusinessActionContext context) {
        //插入事务日志
        AccountTransaction at = new AccountTransaction();
        at.setTxId(context.getXid());//全局事务id
        at.setActionId(context.getBranchId());//分布式事务id
        at.setGmtCreated(new Date());//日志创建时间
        at.setGmtModified(at.getGmtCreated());//日志修改时间
        at.setUserId(vo.getUserId());//用户id
        at.setAmount(vo.getValue());//积分数量
        try {
            accountTransactionMapper.insert(at);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
            //如果插入失败，属于悬挂情况，不做任何处理
        }
        int effectCount = usableIntegralMapper.freezeIntegral(vo.getUserId(), vo.getValue());
        return effectCount > 0;
    }

    //confirm方法
    @Override
    @Transactional
    public void decrValueConfirm(BusinessActionContext context) {
        //查询事务日志表
        AccountTransaction at = accountTransactionMapper.get(context.getXid(), context.getBranchId());
        if (at != null) {
            //如果有记录,判断其状态是否为初始化，执行confirm
            if (AccountTransaction.STATE_TRY == at.getState()) {
                accountTransactionMapper.updateAccountTransactionState(context.getXid(), context.getBranchId(), AccountTransaction.STATE_COMMIT, AccountTransaction.STATE_TRY);
                usableIntegralMapper.commitChange(at.getUserId(), at.getAmount());
            } else {
                //幂等情况，不做处理
            }
        } else {
            //异常情况====> 通知开发
        }
    }

    //cancel
    @Override
    @Transactional
    public void decrValueCancel(BusinessActionContext context) {
        //查询事务日志
        AccountTransaction at = accountTransactionMapper.get(context.getXid(), context.getBranchId());
        if (at == null) {
            //如果没有事务日志（空回滚）
            //cancel不做事情，为了解决悬挂问题，插入事务日志（状态cancel）
            at = new AccountTransaction();
            at.setTxId(context.getXid());//全局事务id
            at.setActionId(context.getBranchId());//分布式事务id
            at.setGmtCreated(new Date());//日志创建时间
            at.setGmtModified(at.getGmtCreated());//日志修改时间
            at.setState(AccountTransaction.STATE_CANCEL);//设置日志状态为cancel
            accountTransactionMapper.insert(at);
        } else {
            //如果有事务日志（正常情况）
            //判断是否为初始化
            if (AccountTransaction.STATE_TRY==at.getState()){
                //如果初始化，执行cancel逻辑
                accountTransactionMapper.updateAccountTransactionState(context.getXid(),context.getBranchId(),AccountTransaction.STATE_CANCEL,AccountTransaction.STATE_TRY);
                usableIntegralMapper.unFreezeIntegral(at.getUserId(),at.getAmount());
            }else {
                //如果不是初始化，幂等处理
            }
        }
    }


    //积分退还
    @Override
    @Transactional
    public Boolean incrValueTry(OperateIntegralVo vo, BusinessActionContext context) {
        //插入事务日志
        AccountTransaction at = new AccountTransaction();
        at.setTxId(context.getXid());//全局事务id
        at.setActionId(context.getBranchId());//分布式事务id
        at.setGmtCreated(new Date());//日志创建时间
        at.setGmtModified(at.getGmtCreated());//日志修改时间
        at.setUserId(vo.getUserId());//用户id
        at.setAmount(vo.getValue());//积分数量
        try {
            accountTransactionMapper.insert(at);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
            //如果插入失败，属于悬挂情况，不做任何处理
        }
        int effectCount = usableIntegralMapper.incrFreezeAmount(vo.getUserId(),vo.getValue());
        return effectCount > 0;
    }

    @Override
    public void incrValueConfirm(BusinessActionContext context) {
        //查询事务日志表
        AccountTransaction at = accountTransactionMapper.get(context.getXid(), context.getBranchId());
        if (at != null) {
            //如果有记录,判断其状态是否为初始化，执行confirm
            if (AccountTransaction.STATE_TRY == at.getState()) {
                accountTransactionMapper.updateAccountTransactionState(context.getXid(), context.getBranchId(), AccountTransaction.STATE_COMMIT, AccountTransaction.STATE_TRY);
                usableIntegralMapper.commitIncrChange(at.getUserId(), at.getAmount());
            } else {
                //幂等情况，不做处理
            }
        } else {
            //异常情况====> 通知开发
        }
    }

    @Override
    public void incrValueCancel(BusinessActionContext context) {
        //查询事务日志
        AccountTransaction at = accountTransactionMapper.get(context.getXid(), context.getBranchId());
        if (at == null) {
            //如果没有事务日志（空回滚）
            //cancel不做事情，为了解决悬挂问题，插入事务日志（状态cancel）
            at = new AccountTransaction();
            at.setTxId(context.getXid());//全局事务id
            at.setActionId(context.getBranchId());//分布式事务id
            at.setGmtCreated(new Date());//日志创建时间
            at.setGmtModified(at.getGmtCreated());//日志修改时间
            at.setState(AccountTransaction.STATE_CANCEL);//设置日志状态为cancel
            accountTransactionMapper.insert(at);
        } else {
            //如果有事务日志（正常情况）
            //判断是否为初始化
            if (AccountTransaction.STATE_TRY==at.getState()){
                //如果初始化，执行cancel逻辑
                accountTransactionMapper.updateAccountTransactionState(context.getXid(),context.getBranchId(),AccountTransaction.STATE_CANCEL,AccountTransaction.STATE_TRY);
                usableIntegralMapper.unFreezeIntegral(at.getUserId(),at.getAmount());
            }else {
                //如果不是初始化，幂等处理
            }
        }
    }
}
