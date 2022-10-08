package server.payimps;



import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.tuples.Tuple2;
import bottle.util.Log4j;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.egzosn.pay.ali.api.AliPayConfigStorage;
import com.egzosn.pay.ali.api.AliPayService;
import com.egzosn.pay.ali.bean.AliTransactionType;
import com.egzosn.pay.common.api.DefaultPayMessageHandler;
import com.egzosn.pay.common.api.PayService;
import com.egzosn.pay.common.bean.PayMessage;
import com.egzosn.pay.common.bean.PayOrder;
import com.egzosn.pay.common.bean.PayOutMessage;
import com.egzosn.pay.common.bean.RefundOrder;
import com.egzosn.pay.common.exception.PayErrorException;
import org.jetbrains.annotations.NotNull;



import server.Launch;
import server.beans.IceTrade;
import server.beans.QrImage;
import server.common.CommFunc;


import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

import static server.beans.IceTrade.sendTrade;
import static server.common.CommFunc.getMapStr;


/**
 * @Author: leeping
 * @Date: 2019/4/17 14:26
 */
//@PropertiesFilePath("alipay_swzy.com.bottle.properties")
@PropertiesFilePath("alipay_onek.properties")
public class AlipayImp extends DefaultPayMessageHandler {

    @PropertiesName("alipay.appid")
    private static String appid;
    @PropertiesName("alipay.seller")
    private static String seller;
    @PropertiesName("alipay.pubkey")
    private static String alipayPubKey;
    @PropertiesName("alipay.self.privkey")
    private static String privKey;

    private static AliPayService service;

    static {
        ApplicationPropertiesBase.initStaticFields(AlipayImp.class);
        AliPayConfigStorage aliPayConfigStorage = new AliPayConfigStorage();
        aliPayConfigStorage.setAppid(appid);
        aliPayConfigStorage.setPid(seller);
        aliPayConfigStorage.setSeller(seller);
        aliPayConfigStorage.setKeyPublic(alipayPubKey);
        aliPayConfigStorage.setKeyPrivate(privKey);
        aliPayConfigStorage.setSignType("RSA2");
        aliPayConfigStorage.setInputCharset("utf-8");
        aliPayConfigStorage.setNotifyUrl(Launch.domain+"/result/alipay");
//        //是否为测试账号，沙箱环境
//        aliPayConfigStorage.setTest(true);
        service = new AliPayService(aliPayConfigStorage);
        service.setPayMessageHandler(new AlipayImp());
        //Log4j.info("alipay 信息\n "+ appid +" \n " + seller +" \n "+ alipayPubKey +" \n " + privKey);
    }

    //获取扫码付的二维码或预支付信息
    public static void create(boolean isApp,@NotNull PayOrder payOrder,@NotNull QrImage qrImage,@NotNull Map<String,Object> prevPayFieldMap) throws Exception{
        if (isApp){
            // 移动原生APP
            payOrder.setTransactionType(AliTransactionType.APP);
            Map<String, Object> map = service.orderInfo(payOrder);
            prevPayFieldMap.putAll(map);
        }else {
            //二维码
            payOrder.setTransactionType(AliTransactionType.SWEEPPAY);//扫码付款
            BufferedImage image = service.genQrPay(payOrder);
            ImageIO.write(image, "png", qrImage.file);
        }

    }

    //查询信息
    public static Tuple2<Integer,String> query(@NotNull String orderNo) {
        try {
            Map<String, Object> map = service.query("", orderNo);
            Log4j.info("支付宝查询结果: " + getMapStr(map) );

            if (map!=null){
                String json = String.valueOf(map.getOrDefault("alipay_trade_query_response",""));
                JSONObject jsonObject = JSON.parseObject(json);
                String trade_status = jsonObject.getString("trade_status");
                if (trade_status == null) trade_status = jsonObject.getString("sub_msg");
                if (trade_status!=null){
                    if (trade_status.equals("TRADE_SUCCESS")){
                        return new Tuple2<>(1,"已支付");
                    }
                    if (trade_status.equals("WAIT_BUYER_PAY")){
                        return new Tuple2<>(0,"待支付");
                    }
                }
            }
        } catch (Exception e) {
            Log4j.error(e);
            return new Tuple2<>(-2,"异常 "+ e);
        }
        return new Tuple2<>(-2,"支付宝响应失败");
    }

    //退款
    public static Tuple2<Boolean,String> refund(@NotNull RefundOrder rorder) {
        try {
            Map<String, Object> map = service.refund(rorder);
            Log4j.info("支付宝退款响应: "+ getMapStr(map));
            if (map!=null){
                String json = String.valueOf(map.getOrDefault("alipay_trade_refund_response",""));
                JSONObject jsonObject = JSON.parseObject(json);
                int code = jsonObject.getInteger("code");
                if (code == 10000){
                    String fund_change = jsonObject.getString("fund_change");
                    if (fund_change.equals("Y")) {
                        return new Tuple2<>(true,"退款成功");
                    }
                    if (fund_change.equals("N")) {
                        return new Tuple2<>(false,"已申请退款,请勿重复提交");
                    }
                }else {
                    String sub_code = jsonObject.getString("sub_code");

                    switch (sub_code){
                        case  "ACQ.SYSTEM_ERROR":
                            sub_code = "系统错误,请使用相同的参数再次调用";
                            break;
                        case  "ACQ.INVALID_PARAMETER":
                            sub_code = "参数无效,请求参数有错，重新检查请求后，再调用退款";
                            break;
                        case  "ACQ.SELLER_BALANCE_NOT_ENOUGH":
                            sub_code = "卖家余额不足,商户支付宝账户充值后重新发起退款即可";
                            break;
                        case  "ACQ.REFUND_AMT_NOT_EQUAL_TOTAL":
                            sub_code = "退款金额超限,检查退款金额是否正确，重新修改请求后，重新发起退款";
                            break;
                        case  "ACQ.TRADE_NOT_EXIST":
                            sub_code = "交易不存在,检查请求中的交易号和商户订单号是否正确，确认后重新发起";
                            break;
                        case "ACQ.TRADE_HAS_FINISHED":
                            sub_code = "交易已完结,该交易已完结，不允许进行退款，确认请求的退款的交易信息是否正确";
                            break;
                        case  "ACQ.TRADE_STATUS_ERROR":
                            sub_code = "交易状态非法,查询交易，确认交易是否已经付款";
                            break;
                        case  "ACQ.DISCORDANT_REPEAT_REQUEST":
                            sub_code = "不一致的请求,检查该退款号是否已退过款或更换退款号重新发起请求";
                            break;
                        case   "ACQ.REASON_TRADE_REFUND_FEE_ERR":
                            sub_code = "退款金额无效,检查退款请求的金额是否正确请使用相同的参数再次调用";
                            break;
                        case  "ACQ.TRADE_NOT_ALLOW_REFUND":
                            sub_code = "当前交易不允许退款,检查当前交易的状态是否为交易成功状态以及签约的退款属性是否允许退款，确认后，重新发起请求";
                            break;
                    }
                    return new Tuple2<>(false,sub_code);
                }

            }
        } catch (Exception e) {
            Log4j.error(e);
            return new Tuple2<>(false,"异常 "+ e);
        }
        return new Tuple2<>(false,"支付宝响应失败");
    }

    //支付回调
    public static String response(@NotNull HttpServletRequest req) {
        try {
            return service.payBack(req.getParameterMap(), req.getInputStream()).toMessage();
        } catch (IOException e) {
            Log4j.error(e);
        }
        return "fail";
    }

    @Override
    public PayOutMessage handle(PayMessage payMessage, Map<String, Object> context, PayService payService) throws PayErrorException {

        try {
            Map<String, Object> message = payMessage.getPayMessage();
            Log4j.info("支付宝支付结果通知: " + getMapStr(message) );

            if(message.get("refund_fee") != null){
                // 退款通知
                return payService.getPayOutMessage("success", "成功");
            }

            //交易状态
            String trade_status =  message.get("trade_status").toString();

            //交易完成
            if ( "TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)) {

                //携带ice接口信息
                String body = String.valueOf(message.get("body"));
                //第三方批次号
                String trade_no = String.valueOf(message.get("trade_no"));
                //后台订单号
                String out_trade_no = String.valueOf(message.get("out_trade_no"));
                //付款平台
                String pay_type  = "alipay";
                //交易完成时间
                String gmt_payment = String.valueOf(message.get("gmt_payment"));
                //交易金额
                String buyer_pay_amount = String.valueOf(message.get("buyer_pay_amount"));
                //支付状态
                int state = "TRADE_SUCCESS".equals(trade_status) ?  1 :  -2;

                IceTrade trade = new IceTrade(body,trade_no,out_trade_no,pay_type,gmt_payment,state+"",buyer_pay_amount,"0");

                if (sendTrade(trade)){
                    return payService.getPayOutMessage("success", "成功");
                }
            }
        } catch (Exception e) {
            Log4j.error(e);
        }
        return payService.getPayOutMessage("fail", "失败");
    }

}
