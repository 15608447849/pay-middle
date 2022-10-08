package server.payimps;

import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.tuples.Tuple2;
import bottle.util.Log4j;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.egzosn.pay.common.api.DefaultPayMessageHandler;
import com.egzosn.pay.common.api.PayService;
import com.egzosn.pay.common.bean.*;
import com.egzosn.pay.common.exception.PayErrorException;
import com.egzosn.pay.common.http.HttpConfigStorage;
import com.egzosn.pay.wx.api.WxPayConfigStorage;
import com.egzosn.pay.wx.api.WxPayService;
import com.egzosn.pay.wx.bean.WxTransactionType;

import org.jetbrains.annotations.NotNull;
import server.Launch;
import server.beans.IceTrade;
import server.beans.QrImage;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Map;


import static bottle.properties.abs.ApplicationPropertiesBase.readPathProperties;
import static server.beans.IceTrade.sendTrade;
import static server.common.CommFunc.getMapStr;

/**
 * @Author: leeping
 * @Date: 2019/5/21 13:54
 */
//@PropertiesFilePath("wxpay_swzy.com.bottle.properties")
@PropertiesFilePath("wxpay_onek.properties")
public class WxpayImp  extends DefaultPayMessageHandler {

    @PropertiesName("wxpay.mchid")
    private static String mchid;//合作者id（商户号）

    @PropertiesName("wxpay.native.appid")
    private static String appid_native; //二维码付款
    @PropertiesName("wxpay.native.secret.key")
    private static String secretKey_native;//密钥

    @PropertiesName("wxpay.app.appid")
    private static String appid_app; //app付款
    @PropertiesName("wxpay.app.secret.key")
    private static String secretKey_app;//密钥

    private static  WxPayConfigStorage wxPayConfigStorage_native;
    private static WxPayConfigStorage wxPayConfigStorage_app;
    private static WxPayService service_native;
    private static WxPayService service_app;


    static{
       initInstance();
    }

    private static void initInstance() {
        ApplicationPropertiesBase.initStaticFields(WxpayImp.class);
        try {
            WxpayImp handler = new WxpayImp();
            HttpConfigStorage httpConfigStorage = genSSLCert();

            //二维码支付服务
            wxPayConfigStorage_native = new WxPayConfigStorage();
            wxPayConfigStorage_native.setAppid(appid_native);
            wxPayConfigStorage_native.setSecretKey(secretKey_native);
            wxPayConfigStorage_native.setMchId(mchid);
            wxPayConfigStorage_native.setNotifyUrl(Launch.domain+"/result/wxpay");
            wxPayConfigStorage_native.setSignType("MD5");
            wxPayConfigStorage_native.setInputCharset("utf-8");
            service_native =  new WxPayService(wxPayConfigStorage_native,httpConfigStorage);
            service_native.setPayMessageHandler(handler);

            //app支付服务
            wxPayConfigStorage_app = new WxPayConfigStorage();
            wxPayConfigStorage_app.setAppid(appid_app);
            wxPayConfigStorage_app.setSecretKey(secretKey_app);
            wxPayConfigStorage_app.setMchId(mchid);
            wxPayConfigStorage_app.setNotifyUrl(Launch.domain+"/result/wxpay");
            wxPayConfigStorage_app.setSignType("MD5");
            wxPayConfigStorage_app.setInputCharset("utf-8");
            service_app =  new WxPayService(wxPayConfigStorage_app,httpConfigStorage);
            service_app.setPayMessageHandler(handler);
        } catch (Exception e) {
            Log4j.error(e);
        }
    }

    private static HttpConfigStorage genSSLCert() {
        //ssl 设置
        HttpConfigStorage httpConfigStorage = new HttpConfigStorage();
        //设置ssl证书对应的存储方式
        httpConfigStorage.setCertStoreType(CertStoreType.INPUT_STREAM);

        String ssl_cert = "/wx_"+mchid+"_cert.p12";
        //ssl 退款证书相关
        try{
            InputStream in = readPathProperties(WxpayImp.class, ssl_cert);
            httpConfigStorage.setKeystore(in);
            httpConfigStorage.setStorePassword(mchid);
            httpConfigStorage.setCharset("UTF-8");
            return httpConfigStorage;
        }catch (Exception e){
           Log4j.error(e);
        }
        throw new RuntimeException("加载SSL证书失败");
    }

    //获取扫码付的二维码或预支付信息
    public static void create(boolean isApp, @NotNull PayOrder payOrder,@NotNull QrImage qrImage,@NotNull Map<String,Object> prevPayFieldMap) throws Exception{
        if (isApp){
            Map<String,Object> map;
            if (payOrder.getOpenid()!=null && payOrder.getOpenid().length()>0){
                // 微信公众号付款
                payOrder.setTransactionType(WxTransactionType.JSAPI);
                map = service_native.orderInfo(payOrder);
            }else {
                // 移动原生APP
                payOrder.setTransactionType(WxTransactionType.APP);
                map = service_app.orderInfo(payOrder);
            }
            prevPayFieldMap.putAll(map);
        }else {
            // 二维码
            payOrder.setTransactionType(WxTransactionType.NATIVE);
            BufferedImage image = service_native.genQrPay(payOrder);
            ImageIO.write(image, "png", qrImage.file);
        }

    }

    //查询信息
    public static Tuple2<Integer,String> query(@NotNull String orderNo,boolean isApp) {


        try {
            Map<String, Object> map ;
            if (isApp) map = service_app.query("",orderNo);
            else map = service_native.query("", orderNo);
            Log4j.info("微信查询结果: " + getMapStr(map) );
            if (map!=null){


                String return_code = String.valueOf(map.getOrDefault("return_code",""));
                if (return_code.equals("SUCCESS")){
                    String result_code = String.valueOf(map.getOrDefault("result_code",""));

                    if (result_code.equals("SUCCESS")){
                        String trade_state = String.valueOf(map.getOrDefault("trade_state",""));
                        String trade_state_desc = String.valueOf(map.getOrDefault("trade_state_desc",""));
//                SUCCESS—支付成功
//                REFUND—转入退款
//                NOTPAY—未支付
//                CLOSED—已关闭
//                REVOKED—已撤销（付款码支付）
//                USERPAYING--用户支付中（付款码支付）
//                PAYERROR--支付失败(其他原因，如银行返回失败)
                        switch (trade_state){
                            case "SUCCESS":
                                return new Tuple2<>(1,trade_state_desc);
                            case "REFUND":
                                return new Tuple2<>(2,trade_state_desc);
                            case "NOTPAY":
                            case "PAYERROR":
                            case "USERPAYING":
                                return new Tuple2<>(0,trade_state_desc);
                            case "CLOSED":
                            case "REVOKED":
                                return new Tuple2<>(-1,trade_state_desc);
                        }

                    }else if (result_code.equals("FAIL")){
                        String err_code = String.valueOf(map.getOrDefault("err_code",""));
                        String err_code_des = String.valueOf(map.getOrDefault("err_code_des",""));
                        return new Tuple2<>(-2,err_code+"-"+err_code_des);
                    }

                }else if (return_code.equals("FAIL")){
                    String return_msg = String.valueOf(map.getOrDefault("return_msg",""));
                    return new Tuple2<>(-2,return_msg);
                }

            }
        } catch (Exception e) {
            Log4j.error(e);
            return new Tuple2<>(-2,"异常 "+ e);
        }
        return new Tuple2<>(-2,"微信响应失败");
    }

    //退款
    public static Tuple2<Boolean,String> refund(@NotNull RefundOrder rorder, boolean isApp) {
        try {
            // 退款特殊处理
            WxpayImp handler = new WxpayImp();
            HttpConfigStorage httpConfigStorage = genSSLCert();

            //二维码支付服务
            WxPayConfigStorage wxPayConfigStorage_native = new WxPayConfigStorage();
            wxPayConfigStorage_native.setAppid(appid_native);
            wxPayConfigStorage_native.setSecretKey(secretKey_native);
            wxPayConfigStorage_native.setMchId(mchid);
            wxPayConfigStorage_native.setNotifyUrl(Launch.domain+"/result/wxpay");
            wxPayConfigStorage_native.setSignType("MD5");
            wxPayConfigStorage_native.setInputCharset("utf-8");

            WxPayService service_native =  new WxPayService(wxPayConfigStorage_native,httpConfigStorage);
            service_native.setPayMessageHandler(handler);

            //app支付服务
            WxPayConfigStorage wxPayConfigStorage_app = new WxPayConfigStorage();
            wxPayConfigStorage_app.setAppid(appid_app);
            wxPayConfigStorage_app.setSecretKey(secretKey_app);
            wxPayConfigStorage_app.setMchId(mchid);
            wxPayConfigStorage_app.setNotifyUrl(Launch.domain+"/result/wxpay");
            wxPayConfigStorage_app.setSignType("MD5");
            wxPayConfigStorage_app.setInputCharset("utf-8");

            WxPayService service_app =  new WxPayService(wxPayConfigStorage_app,httpConfigStorage);
            service_app.setPayMessageHandler(handler);

            WxPayService service = isApp? service_app : service_native;
            Map<String,Object> map = service.refund(rorder);
            Log4j.info("微信退款响应: "+ getMapStr(map));

            try {
                httpConfigStorage.getKeystoreInputStream().close();
            } catch (IOException e) {
                Log4j.error(e);
            }

            if (map!=null){
                String return_code = String.valueOf(map.getOrDefault("return_code",""));
                if(return_code.equals("SUCCESS")){
                    String result_code = String.valueOf(map.getOrDefault("result_code",""));

                    if (result_code.equals("SUCCESS")){
                        return new Tuple2<>(true,"退款成功");
                    }else if (result_code.equals("FAIL")){
                        String err_code = String.valueOf(map.getOrDefault("err_code",""));
                        String err_code_des = String.valueOf(map.getOrDefault("err_code_des",""));




                        switch (err_code){
                            case "SYSTEMERROR":
                                err_code = "系统超时等原因,接口返回错误,请不要更换商户退款单号，请使用相同参数再次调用API";
                                break;
                            case  "BIZERR_NEED_RETRY":
                                err_code = "并发情况下，业务被拒绝，商户重试即可解决,请不要更换商户退款单号，请使用相同参数再次调用API";
                                break;
                            case  "TRADE_OVERDUE":
                                err_code = "订单已经超过可退款的最大期限(支付后一年内可退款),请选择其他方式自行退款";
                                break;
                            case  "ERROR":
                                err_code =  "申请退款业务发生错误,"+err_code_des;
                                break;
                            case  "USER_ACCOUNT_ABNORMAL":
                                err_code = "用户帐号注销,退款申请失败，商户可自行处理退款";
                                break;
                            case  "INVALID_REQ_TOO_MUCH":
                                err_code = "连续错误请求数过多被系统短暂屏蔽,请检查业务是否正常，确认业务正常后请在1分钟后再来重试";
                                break;
                            case  "NOTENOUGH":
                                err_code = "商户可用退款余额不足";
                                break;
                            case  "INVALID_TRANSACTIONID":
                                err_code = "无效transaction_id,请求参数错误，检查原交易号是否存在或发起支付交易接口返回失败";
                                break;
                            case    "PARAM_ERROR":
                                err_code = "请求参数错误，请重新检查再调用退款申请";
                                break;
                            case   "MCHID_NOT_EXIST":
                                err_code = "请检查MCHID是否正确";
                                break;
                            case   "ORDERNOTEXIST":
                                err_code = "请检查你的订单号是否正确且是否已支付，未支付的订单不能发起退款";
                                break;
                            case   "REQUIRE_POST_METHOD":
                                err_code = "请检查请求参数是否通过post方法提交";
                                break;
                            case   "SIGNERROR":
                                err_code = "请检查签名参数和方法是否都符合签名算法要求";
                                break;
                            case   "XML_FORMAT_ERROR":
                                err_code = "请检查XML参数格式是否正确";
                                break;
                            case   "FREQUENCY_LIMITED":
                                err_code = "该笔退款未受理，请降低频率后重试";
                                break;
                            case   "APPID_NOT_EXIST":
                                err_code = "请检查APPID是否正确";
                                break;
                        }

                        return new Tuple2<>(false,err_code);
                    }


                }else if (return_code.equals("FAIL")){
                    String return_msg = String.valueOf(map.getOrDefault("return_msg",""));
                    return new Tuple2<>(false,return_msg);
                }
            }

        } catch (Exception e) {
            Log4j.error(e);
            return new Tuple2<>(false,"异常 "+ e);
        }
        return new Tuple2<>(false,"微信响应失败");
    }


    //支付回调
    public static Object response(@NotNull HttpServletRequest req) {
        try {
            return service_native.payBack(req.getParameterMap(), req.getInputStream()).toMessage();
        } catch (IOException e) {
            Log4j.error(e);
        }
        return "fail";
    }

    //回调处理
    @Override
    public PayOutMessage handle(PayMessage payMessage, Map<String, Object> context, PayService payService) throws PayErrorException {
        try {
            Map<String, Object> message = payMessage.getPayMessage();
            Log4j.info("微信支付结果通知: " +    getMapStr(message));

            //交易状态
            String result_code =  message.get("result_code").toString();
            //交易状态
            if ("SUCCESS".equals(result_code) || "FAIL".equals(result_code)){
                //这里进行成功的处理
                //携带ice接口信息
                String body = message.get("attach").toString();
                //第三方批次号
                String trade_no = message.get("transaction_id").toString();
                //后台订单号
                String out_trade_no = message.get("out_trade_no").toString();
                //付款平台
                String pay_type  = "wxpay";
                //交易完成时间
                String gmt_payment = message.get("time_end").toString();
                gmt_payment = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new SimpleDateFormat("yyyyMMddHHmmss").parse(gmt_payment));
                //交易金额
                String buyer_pay_amount = message.get("total_fee").toString();
                BigDecimal temp  = new BigDecimal(buyer_pay_amount).divide(new BigDecimal("100"),2, RoundingMode.HALF_UP);

                buyer_pay_amount = String.valueOf(temp.setScale(2, RoundingMode.HALF_UP).doubleValue());
                //
                int state = "SUCCESS".equals(result_code) ?  1 :  -2;

                int pay_client_type = message.get("trade_type").equals("APP") ? 1 : 0;

                IceTrade trade = new IceTrade(body,trade_no,out_trade_no,pay_type,gmt_payment,state+"",buyer_pay_amount,pay_client_type+"");

                if (sendTrade(trade)){
                    return  payService.getPayOutMessage("SUCCESS", "OK");
                }
            }
        } catch (Exception e) {
            Log4j.error(e);
        }
        return  payService.getPayOutMessage("FAIL", "失败");
    }




}
