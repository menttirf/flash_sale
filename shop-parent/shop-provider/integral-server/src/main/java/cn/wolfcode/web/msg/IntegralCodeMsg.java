package cn.wolfcode.web.msg;
import cn.wolfcode.common.web.CodeMsg;

/**
 * Created by wolfcode-lanxw
 */
public class IntegralCodeMsg extends CodeMsg {
    private IntegralCodeMsg(Integer code, String msg){
        super(code,msg);
    }
    public static final IntegralCodeMsg OP_INTEGRAL_ERROR = new IntegralCodeMsg(500601,"操作积分失败");
    public static final IntegralCodeMsg INTEGRAL_NOT_ENOUGH = new IntegralCodeMsg(500602,"积分余额不足");
}
