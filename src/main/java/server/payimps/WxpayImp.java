package server.payimps;

import bottle.util.Log4j;
import com.egzosn.pay.common.api.DefaultPayMessageHandler;
import com.egzosn.pay.common.api.PayService;
import com.egzosn.pay.common.bean.*;
import com.egzosn.pay.common.exception.PayErrorException;
import com.egzosn.pay.common.http.HttpConfigStorage;
import com.egzosn.pay.wx.api.WxPayConfigStorage;
import com.egzosn.pay.wx.api.WxPayService;
import com.egzosn.pay.wx.bean.WxTransactionType;
import org.jetbrains.annotations.NotNull;
import com.bottle.properties.abs.ApplicationPropertiesBase;
import com.bottle.properties.annotations.PropertiesFilePath;
import com.bottle.properties.annotations.PropertiesName;
import server.Launch;
import server.beans.IceTrade;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Map;

import static com.bottle.properties.abs.ApplicationPropertiesBase.readPathProperties;
import static server.beans.IceTrade.sendTrade;

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
    private static PayService service_native;
    private static PayService service_app;


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
            e.printStackTrace();
        }
    }

    private static HttpConfigStorage genSSLCert() {
        //ssl 设置
        HttpConfigStorage httpConfigStorage =new HttpConfigStorage();
        //设置ssl证书对应的存储方式
        httpConfigStorage.setCertStoreType(CertStoreType.INPUT_STREAM);

        String ssl_cert = "/wx_"+mchid+"_cert.p12";
        //ssl 退款证书相关
        try{
            InputStream in = readPathProperties(WxpayImp.class, ssl_cert);
            httpConfigStorage.setKeystore(in);
            httpConfigStorage.setStorePassword(mchid);
            httpConfigStorage.setCharset("UTF-8");
            //Log4j.info("加载微信SSL证书: "+ ssl_cert);
            return httpConfigStorage;
        }catch (Exception e){
            e.printStackTrace();
        }
        throw new RuntimeException("加载SSL证书失败");
    }

    //获取扫码付的二维码
    public static Map execute(final PayOrder payOrder, final File qrImage,boolean isApp) throws Exception{
        if (isApp){
            payOrder.setTransactionType(WxTransactionType.APP);
            return service_app.orderInfo(payOrder);
        }
        if (payOrder.getOpenid()!=null && payOrder.getOpenid().length()>0){ //公众号付款
            payOrder.setTransactionType(WxTransactionType.JSAPI);
            return service_native.orderInfo(payOrder);
        }
        payOrder.setTransactionType(WxTransactionType.NATIVE);
        BufferedImage image = service_native.genQrPay(payOrder);
        ImageIO.write(image, "png", qrImage);
       return null;
    }

    //支付回调
    public static Object response(@NotNull HttpServletRequest req) {
        try {
            return service_native.payBack(req.getParameterMap(), req.getInputStream()).toMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "fail";
    }

    //查询信息
    public static Map<String, Object>  queryInfo(@NotNull String orderNo,boolean isApp) {
        if (isApp) return service_native.query("",orderNo);
        return service_native.query("", orderNo);
    }

    //退款
    public static Map<String, Object> refund(@NotNull RefundOrder rorder,boolean isApp) {
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

            PayService service = isApp? service_app : service_native;
            Map<String,Object> map = service.refund(rorder);

            //Log4j.info("微信退款结果: " + new Gson().toJson(map) );

            try {
                httpConfigStorage.getKeystoreInputStream().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return map;
        } catch (Exception e) {
            Log4j.error("微信退款失败",e);
            throw e;
        }
    }



    //回调处理
    @Override
    public PayOutMessage handle(PayMessage payMessage, Map<String, Object> context, PayService payService) throws PayErrorException {
        try {
            Map<String, Object> message = payMessage.getPayMessage();
            Log4j.info("微信支付结果通知:");
            Launch.printMap(message);
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
                BigDecimal temp  = new BigDecimal(buyer_pay_amount).divide(new BigDecimal(100));
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
            e.printStackTrace();
        }
        return  payService.getPayOutMessage("FAIL", "失败");
    }




}
