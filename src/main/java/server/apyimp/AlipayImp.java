package server.apyimp;



import com.egzosn.pay.ali.api.AliPayConfigStorage;
import com.egzosn.pay.ali.api.AliPayService;
import com.egzosn.pay.ali.bean.AliTransactionType;
import com.egzosn.pay.common.api.DefaultPayMessageHandler;
import com.egzosn.pay.common.api.PayMessageHandler;
import com.egzosn.pay.common.api.PayService;
import com.egzosn.pay.common.bean.PayMessage;
import com.egzosn.pay.common.bean.PayOrder;
import com.egzosn.pay.common.bean.PayOutMessage;
import com.egzosn.pay.common.bean.RefundOrder;
import com.egzosn.pay.common.exception.PayErrorException;
import org.jetbrains.annotations.NotNull;
import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;
import server.threads.IceNotifyThread;
import server.Launch;
import server.beans.IceTrade;


import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;


/**
 * @Author: leeping
 * @Date: 2019/4/17 14:26
 */
//@PropertiesFilePath("alipay_swzy.properties")
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

    private static PayService service;

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
        Launch.log.info("alipay 信息\n "+ appid +" \n " + seller +" \n "+ alipayPubKey +" \n " + privKey);
    }

    //获取扫码付的二维码
    public static Map execute(final PayOrder payOrder,final File qrImage,boolean isApp) throws Exception{
        if (isApp){
            payOrder.setTransactionType(AliTransactionType.APP);
            return service.orderInfo(payOrder);
        }
        payOrder.setTransactionType(AliTransactionType.SWEEPPAY);//扫码付款
        BufferedImage image = service.genQrPay(payOrder);
        ImageIO.write(image, "png", qrImage);
        return null;
    }

    //支付回调
    public static String response(@NotNull HttpServletRequest req) {
        try {
            return service.payBack(req.getParameterMap(), req.getInputStream()).toMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "fail";
    }

    //查询信息
    public static Map<String, Object>  queryInfo(@NotNull String orderNo) {
        return service.query("", orderNo);
    }

    //退款
    public static Map<String, Object> refund(@NotNull RefundOrder rorder) {
        return service.refund(rorder);
    }

    @Override
    public PayOutMessage handle(PayMessage payMessage, Map<String, Object> context, PayService payService) throws PayErrorException {

        try {
            Map<String, Object> message = payMessage.getPayMessage();
            message.forEach((k,v) -> {
                Launch.log.info( k+" = "+v);
            });
            Launch.log.info("\n");
            //交易状态
            String trade_status =  message.get("trade_status").toString();

            //交易完成
            if ( "TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)) {

                //携带ice接口信息
                String body = message.get("body").toString();
                //第三方批次号
                String trade_no = message.get("trade_no").toString();
                //后台订单号
                String out_trade_no = message.get("out_trade_no").toString();
                //付款平台
                String pay_type  = "alipay";
                //交易完成时间
                String gmt_payment = message.get("gmt_payment").toString();
                //交易金额
                String buyer_pay_amount = message.get("buyer_pay_amount").toString();
                //
                int state = "TRADE_SUCCESS".equals(trade_status) ?  1 :  -2;

                IceTrade trade = new IceTrade(body,trade_no,out_trade_no,pay_type,gmt_payment,state+"",buyer_pay_amount,"0");

                if (trade.notifyIceServer()){
                    return payService.getPayOutMessage("success", "成功");
                }else{
                    IceNotifyThread.addTrade(trade);
                };
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return payService.getPayOutMessage("fail", "失败");
    }

}
