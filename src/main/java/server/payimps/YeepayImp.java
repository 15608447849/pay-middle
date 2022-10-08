package server.payimps;

import bottle.tuples.Tuple2;
import bottle.util.Log4j;
import bottle.util.StringUtil;
import bottle.util.ThreadTool;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.egzosn.pay.common.bean.RefundOrder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import server.beans.IceTrade;
import server.beans.QrImage;
import server.yeepay.YeepayApiFunction;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static server.beans.IceTrade.sendTrade;
import static server.common.CommFunc.getMapStr;
import static server.yeepay.YeepayApiFunction.delLocalYeepayAttr;
import static server.yeepay.YeepayApiFunction.getLocalYeepayQuerying;

public class YeepayImp {

    private static int invTime =  60 * 1000;

    static{
        Thread loopResultThread = new Thread(
                () -> {

                    while (true){


                        List<Tuple2<String, String>> list = getLocalYeepayQuerying(invTime);

                        for (Tuple2<String, String> it : list){

                            Tuple2<Integer, String> query = query(it.getValue0());
                            if (query.getValue0() == -1){
                                // 支付取消
                                delLocalYeepayAttr(it.getValue1());
                            }
                            if (query.getValue0() == -2){
                                // 支付失败
                                Log4j.info("[轮询] 易宝订单支付失败: "+ it.getValue0() +" 文件: "+it.getValue0());
                                // 发送失败通知?
                                delLocalYeepayAttr(it.getValue1());
                            }
                        }

                        ThreadTool.threadSleep(invTime);

                    }

                }
        );
        loopResultThread.setDaemon(true);
        loopResultThread.start();
    }

    //获取扫码付的二维码或预支付信息
    public static void create(boolean isApp, @NotNull String channel,@NotNull String orderNo, @NotNull  String subject, @NotNull BigDecimal price, @NotNull Date expirationTime,
                              @NotNull String attr,
                              @NotNull QrImage qrImage, @NotNull Map<String,Object> prevPayFieldMap){
        if (isApp && !StringUtil.isEmpty(channel)){
            if (channel.equals("WECHAT")){
                // 组装小程序需要的字段
                prevPayFieldMap.put("channel","WECHAT");
                prevPayFieldMap.put("minProcPay",YeepayApiFunction.minProcPayUrl);
                prevPayFieldMap.put("wx_appid",YeepayApiFunction.wx_app_appid);
                prevPayFieldMap.put("wx_orgid",YeepayApiFunction.wx_midproc_orgId);
                prevPayFieldMap.put("orderNo",orderNo);
                prevPayFieldMap.put("attr",attr);
                prevPayFieldMap.put("price",price.toString());
                prevPayFieldMap.put("subject",subject);
            }
            if (channel.equals("ALIPAY")){
                // 获取h5支付的URL
                Tuple2<String,String> tuple  = YeepayApiFunction.createPayOrderMobileApp("ALIPAY","USER_SCAN",null,
                        attr, orderNo, price, subject,expirationTime);
                String prePayTn = tuple.getValue0();
                String errmsg = tuple.getValue1();
                if (prePayTn == null) throw new IllegalStateException("易宝支平台支付宝预支付错误"+ ( errmsg!=null ? ","+ errmsg : ".") );
                prevPayFieldMap.put("channel","ALIPAY");
                prevPayFieldMap.put("alipay_qr_url",prePayTn);
            }
        }
        if ( !isApp || StringUtil.isEmpty(channel) ){
            // 扫码聚合支付
            String url =YeepayApiFunction.createPayOrderORCode(attr,orderNo,price,subject,expirationTime);
            if (url == null) throw new IllegalStateException("易宝支平台聚合码预支付错误.");
            qrImage.link = url;
        }

    }

    //查询信息
    public static Tuple2<Integer,String> query(@Nullable String orderNo) {

        String response = YeepayApiFunction.queryPayOrder(orderNo);
        Log4j.info("易宝查询结果: " + response);

        if (response != null){
            JSONObject json = JSON.parseObject(response);
            String errcode = json.getString("code");
            if (!StringUtil.isEmpty(errcode) && !"00000".equals(errcode)) {
                return new Tuple2<>(-2,json.getString("message"));
            }
            String status = json.getString("status");

//        PROCESSING：订单待支付
//        SUCCESS：订单支付成功
//        TIME_OUT：订单已过期
//        FAIL:订单支付失败
//        CLOSE:订单关闭
            switch (status){
                case "PROCESSING":
                    return new Tuple2<>(0,"订单待支付");
                case "SUCCESS":
                    return new Tuple2<>(1,"订单支付成功");
                case "TIME_OUT":
                    return new Tuple2<>(-1,"订单已过期");
                case "FAIL":
                    return new Tuple2<>(-2,"订单支付失败");
                case "CLOSE":
                    return new Tuple2<>(-1,"订单已关闭");
            }
        }
        return new Tuple2<>(-2,"易宝响应失败");
    }

    //退款
    public static Tuple2<Boolean,String> refund(@NotNull String orderNo, @NotNull String  orderNoSerial,@NotNull String yeepayOrderID,@NotNull String price) {
        String response = YeepayApiFunction.refundPayOrder(orderNo,orderNoSerial,yeepayOrderID,price);
        if (response != null){
            // 获取预支付信息
            JSONObject json = JSON.parseObject(response);
            String errcode = json.getString("code");
            if (!StringUtil.isEmpty(errcode) && !"OPR00000".equals(errcode)) {
                return new Tuple2<>(false,"退款受理失败: "+  errcode+ "-" + json.getString("message"));
            }
            String status = json.getString("status");
//            PROCESSING：退款处理中
//            SUCCESS：退款成功
//            FAILED：退款失败
//            CANCEL:退款关闭,商户通知易宝结束该笔退款后返回该状态
//            SUSPEND:退款中断

            switch (status){
                case "PROCESSING":
//                    return new Tuple2<>(true,"退款处理中");
                    return refundQuery(orderNo,orderNoSerial,yeepayOrderID);
//                    return new Tuple2<>(true,"退款处理中");
                case "SUCCESS":
                    return new Tuple2<>(true,"退款成功");
                case "FAILED":
                    return new Tuple2<>(false,"退款失败");
                case "CANCEL":
                    return new Tuple2<>(false,"退款关闭");
                case "SUSPEND":
                    return new Tuple2<>(false,"退款中断");
            }
        }
        return new Tuple2<>(false,"易宝响应失败");
    }

    private static Tuple2<Boolean,String> refundQuery(@NotNull String orderNo, @NotNull String  orderNoSerial,@NotNull String yeepayOrderID){
        ThreadTool.threadSleep(1000);

        String response = YeepayApiFunction.refundPayOrderQuery(orderNo,orderNoSerial,yeepayOrderID);
        if (response != null){
            // 获取预支付信息
            JSONObject json = JSON.parseObject(response);
            String errcode = json.getString("code");
            if (!StringUtil.isEmpty(errcode) && !"OPR00000".equals(errcode)) {
                return new Tuple2<>(false,"退款受理失败: " + errcode+ "-" + json.getString("message"));
            }
            String status = json.getString("status");
//            PROCESSING：退款处理中
//            SUCCESS：退款成功
//            FAILED：退款失败
//            CANCEL:退款关闭,商户通知易宝结束该笔退款后返回该状态
//            SUSPEND:退款中断

            switch (status){
                case "PROCESSING":
                    return refundQuery(orderNo,orderNoSerial,yeepayOrderID);
                case "SUCCESS":
                    return new Tuple2<>(true,"退款成功");
                case "FAILED":
                    return new Tuple2<>(false,"退款失败");
                case "CANCEL":
                    return new Tuple2<>(false,"退款关闭");
                case "SUSPEND":
                    return new Tuple2<>(false,"退款中断");
            }
        }
        return new Tuple2<>(true,"易宝响应失败,退款已受理");
    }

    public static boolean payResultNotify(String md5, String json) {
        if (json == null) return false;

        Log4j.info("易宝支付结果通知JSON : " + json);
        JSONObject message = JSON.parseObject(json);

        //携带ice接口信息
        String body = YeepayApiFunction.getLocalYeepayAttr(md5);;
        if (body == null) return true;
        //第三方订单号
        String trade_no = String.valueOf(message.get("uniqueOrderNo"));
        //后台订单号
        String out_trade_no = String.valueOf(message.get("orderId"));
        //付款平台
        String pay_type  = "yeepay";
        //交易完成时间
        String gmt_payment = String.valueOf(message.get("paySuccessDate"));
        //交易金额
        String buyer_pay_amount = String.valueOf(message.get("orderAmount"));
        //支付状态
        int state = "SUCCESS".equals(String.valueOf(message.get("status"))) ?  1 :  -2;

        //支付客户端类型 0-PC 1-APP
        int pay_client_type = 0;
        String payWay = String.valueOf(message.get("payWay"));
        String channel = String.valueOf(message.get("channel"));
        if (channel.equals("ALIPAY") && payWay.equals("USER_SCAN")){
            pay_client_type = 1;
        }
        if (channel.equals("WECHAT") && payWay.equals("MINI_PROGRAM")){
            pay_client_type = 1;
        }

        IceTrade trade = new IceTrade(body,trade_no,out_trade_no,pay_type,gmt_payment,String.valueOf(state),buyer_pay_amount,String.valueOf(pay_client_type));
        boolean isAdd = sendTrade(trade);
        Log4j.info("易宝支付 业务处理 响应结果: "+ isAdd);
        if (isAdd) {
            delLocalYeepayAttr(md5);
        }
        return isAdd;
    }
}
