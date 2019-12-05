package server.apyimp;

import com.egzosn.pay.common.api.PayMessageHandler;
import com.egzosn.pay.common.api.PayService;
import com.egzosn.pay.common.bean.*;
import com.egzosn.pay.common.exception.PayErrorException;
import com.egzosn.pay.common.http.HttpConfigStorage;
import com.egzosn.pay.wx.api.WxPayConfigStorage;
import com.egzosn.pay.wx.api.WxPayService;
import com.egzosn.pay.wx.bean.WxTransactionType;
import org.jetbrains.annotations.NotNull;
import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;
import server.Launch;
import server.beans.IceTrade;
import server.threads.IceNotifyThread;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Map;

import static properties.abs.ApplicationPropertiesBase.readPathProperties;

/**
 * @Author: leeping
 * @Date: 2019/5/21 13:54
 */
@PropertiesFilePath("wxpay_swzy.properties")
public class WxpayImp  implements PayMessageHandler {

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

    private static PayService service_native;
    private static PayService service_app;

    static{
       initInstance();
    }

    private static void initInstance() {
        ApplicationPropertiesBase.initStaticFields(WxpayImp.class);
        //ssl 设置
        HttpConfigStorage httpConfigStorage =new HttpConfigStorage();
        //设置ssl证书对应的存储方式
        httpConfigStorage.setCertStoreType(CertStoreType.INPUT_STREAM);
        try {
            String ssl_cert = "/wx_"+mchid+"_cert.p12";
            //ssl 退款证书相关
            InputStream in = readPathProperties(WxpayImp.class, ssl_cert);
            httpConfigStorage.setKeystore(in);
            httpConfigStorage.setStorePassword(mchid);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //二维码支付服务
        WxPayConfigStorage wxPayConfigStorage = new WxPayConfigStorage();
        wxPayConfigStorage.setAppid(appid_native);
        wxPayConfigStorage.setSecretKey(secretKey_native);
        wxPayConfigStorage.setMchId(mchid);
        wxPayConfigStorage.setNotifyUrl("http://"+ Launch.domain+"/result/wxpay");
        wxPayConfigStorage.setSignType("MD5");
        wxPayConfigStorage.setInputCharset("utf-8");
        service_native =  new WxPayService(wxPayConfigStorage,httpConfigStorage);
        service_native.setPayMessageHandler(new WxpayImp());

        //app支付服务
        WxPayConfigStorage wxPayConfigStorage_app = new WxPayConfigStorage();
        wxPayConfigStorage_app.setAppid(appid_app);
        wxPayConfigStorage_app.setSecretKey(secretKey_app);
        wxPayConfigStorage_app.setMchId(mchid);
        wxPayConfigStorage_app.setNotifyUrl("http://"+ Launch.domain+"/result/wxpay");
        wxPayConfigStorage_app.setSignType("MD5");
        wxPayConfigStorage_app.setInputCharset("utf-8");
        service_app =  new WxPayService(wxPayConfigStorage_app,httpConfigStorage);
        service_app.setPayMessageHandler(new WxpayImp());
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
            if (isApp)  return service_app.refund(rorder);
            return service_native.refund(rorder);
        } catch (Exception e) {
            if (e instanceof  PayErrorException){
                PayErrorException error = (PayErrorException) e;
                if (error.getPayError().getErrorMsg().equals("api.mch.weixin.qq.com:443 failed to respond")){
                    initInstance();
                }
            }
            throw e;
        }
    }



    //回调处理
    @Override
    public PayOutMessage handle(PayMessage payMessage, Map<String, Object> context, PayService payService) throws PayErrorException {
        try {
            Map<String, Object> message = payMessage.getPayMessage();
            message.forEach((k,v) -> {
                Launch.log.info( k+" = "+v);
            });
            Launch.log.info("\n");
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

                if (trade.notifyIceServer()){
                    return  payService.getPayOutMessage("SUCCESS", "OK");
                }else{
                    IceNotifyThread.addTrade(trade);
                };
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  payService.getPayOutMessage("FAIL", "失败");
    }


    public static void main(String[] args) {
//        String buyer_pay_amount = "18432";
//        BigDecimal temp  = new BigDecimal(buyer_pay_amount).divide(new BigDecimal(100));
//        BigDecimal    temp  = new BigDecimal(vbuyer_pay_amount).divide(new BigDecimal(100),new MathContext(2, RoundingMode.HALF_DOWN));
//       buyer_pay_amount = String.valueOf(temp.doubleValue());
//        System.out.println(buyer_pay_amount);
    }

}
