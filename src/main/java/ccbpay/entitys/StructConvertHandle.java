package ccbpay.entitys;

import IceUtilInternal.StringUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.google.zxing.common.StringUtils;
import server.Launch;
import server.beans.IceResult;
import server.beans.IceTrade;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 *  转换结果为 一块医药后台识别的数据结构
 */
public class StructConvertHandle {
    private static final class PayResult {
        private static final class OrderInfo {
            private String Order_No;
            private String ExistInCCB;
            private String Order_No_CCB;
            private String SendStatus;
            private String PayBackStatus;
            private String PayStatus;
            private String Order_Money;
            private String PayTime;
            private String UpdateTime;
            private String Remark;
            private String CLR_STATUS;
            private String CLR_TIME;
            private String Expand2;
            private String Epay_Type;
            private String Pay_Name;
            private String Pay_Card;
        }
        private String TxCode;
        private String TransID;
        private List<OrderInfo> OrderInfos;
        private String Status;
        private String Remark;
        private String Expand1;
    }

    /**
     * 查询结果转换
     *IceResult  code = 200 , data = 0-待支付 1已支付 -2异常
     */
    public static IceResult order_status_query_result_convert(String json){
        IceResult iceResult = new IceResult();

        try {
            JSONObject rootObj = JSONObject.parseObject(json);
            if (!Integer.valueOf(1).equals(rootObj.getInteger("Status"))) {
                throw new IllegalStateException("order_status_query_result_convert get a failed status from ccb... ");
            }

            JSONArray orderInfos = rootObj.getJSONArray("OrderInfos");
            JSONObject orderInfo;
            if (orderInfos == null || orderInfos.size() != 1 || (orderInfo = orderInfos.getJSONObject(0)) == null) {
                throw new NullPointerException("order_status_query_result_convert get an empty/illegal orderInfos from ccb... ");
            }

            iceResult.set(200, Integer.valueOf(1).equals(orderInfo.getInteger("PayStatus")) ? "1" : "0");
        } catch (Exception e) {
            Launch.log.error("order_status_query_result_convert ", e);
            iceResult.set(-2, "异常状态", "-2");
        }

        return iceResult;
    }

    /** 支付结果转换 */
    public static IceTrade order_pay_result_convert(String json){
        try {
            JSONObject rootObject  = JSONObject.parseObject(json);
            JSONArray orderArray = rootObject.getJSONArray("OrderInfos");
            if (orderArray == null || orderArray.size() != 1) {
                Launch.log.info("order_pay_result_convert OrderInfos is illegal... " + json);
                return null;
            }
            JSONObject orderObject = orderArray.getJSONObject(0);
            String body = rootObject.getString("Expand1");
            String trade_no = rootObject.getString("TransID");
            String out_trade_no = orderObject.getString("Order_No");
            String pay_type = "ccbpay";
            String gmt_payment = null;

            String payTime = orderObject.getString("PayTime");
            if (payTime != null && !payTime.trim().isEmpty()) {
                gmt_payment = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new SimpleDateFormat("yyyyMMddHHmmss").parse(payTime));
            }
            String trade_status = Integer.valueOf(1).equals(orderObject.getInteger("PayStatus")) ? "1" : "-2";
            String buyer_pay_amount = orderObject.getBigDecimal("Order_Money").toPlainString();
            String pay_client_type = "0";

            return new IceTrade(
                    body, trade_no, out_trade_no,
                    pay_type, gmt_payment, trade_status,
                    buyer_pay_amount, pay_client_type);
        } catch (JSONException e) {
            Launch.log.error("Could not convert json from orderPayResult...");
        } catch (Exception e) {
            Launch.log.error("order_pay_result_convert error param " + json, e);
        }

        return null;
    }
}
