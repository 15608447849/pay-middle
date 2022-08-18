import com.egzosn.pay.common.bean.RefundOrder;
import com.google.gson.Gson;
import server.payimps.AlipayImp;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;

public class RefundTest {
    public static void main(String[] args) {

        String tradeNo = "2021100222001444281409166813"; //三方平台相关订单号
        String refundNo = "2110020009411604002"; //一块医药退款单号/流水号
        String price = "417.32"; //退款金额
        String priceTotal = "417.32"; //退款总金额

        RefundOrder rorder = new  RefundOrder(refundNo, tradeNo,new BigDecimal(price));
        rorder.setTotalAmount(new BigDecimal(priceTotal));
        Map<String, Object> map =  AlipayImp.refund(rorder);

        Iterator it = map.entrySet().iterator();
        Map.Entry entry;
        while (it.hasNext()){
            entry = (Map.Entry) it.next();
            System.out.println( entry.getKey() + " = " +entry.getValue() );
        }
    }
}
