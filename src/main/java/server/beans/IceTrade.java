package server.beans;

import com.egzosn.pay.common.bean.RefundOrder;
import com.egzosn.pay.common.util.str.StringUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;
import server.Launch;
import server.apyimp.AlipayImp;
import server.apyimp.WxpayImp;
import server.threads.QrImageDeleteThread;
import utils.IceClient;

import java.math.BigDecimal;
import java.util.*;

/**
 * @Author: leeping
 * @Date: 2019/4/18 16:07
 */
@PropertiesFilePath("/application.properties")
public class IceTrade {

    @PropertiesName("reg.tag")
    public static String tag;
    @PropertiesName("reg.address")
    public static String address;
    @PropertiesName("reg.serverName")
    public static String serverName;
    @PropertiesName("reg.className")
    public static String className;
    @PropertiesName("reg.methodName")
    public static String methodName;

    private static Timer autoRefundTimer = new Timer();
    private static final IceClient client;

    static {
        ApplicationPropertiesBase.initStaticFields(IceTrade.class);
        client = new IceClient(tag,address).startCommunication();
    }


    //第三方交易流水号
    private String trade_no = "";
    //后台订单号
    private String out_trade_no = "";
    //付款平台 alipay, wxpay, ccbpay
    private String pay_type  = "";
    //交易完成时间
    private String gmt_payment ="";
    //交易状态 1成功
    private String trade_status= "";
    //交易金额
    private String buyer_pay_amount = "";
    //支付客户端类型 0-PC 1-APP
    private String pay_client_type = "0";
    //支付请求附带信息
    private String pay_request_attach;

    public IceTrade(String body,String trade_no, String out_trade_no, String pay_type, String gmt_payment, String trade_status,String buyer_pay_amount,String pay_client_type) {
        this.trade_no = trade_no;
        this.out_trade_no = out_trade_no;
        this.pay_type = pay_type;
        this.gmt_payment = gmt_payment;
        this.trade_status = trade_status;
        this.buyer_pay_amount = buyer_pay_amount;
        this.pay_client_type = pay_client_type;
        this.pay_request_attach = body.substring(body.lastIndexOf("@")+1);
    }

    private static class IceResult{
        private int code;
        private Object data;
    }

    public boolean notifyIceServer() {

        boolean flag = false;

        //0 后台订单号,1 平台类型,2 第三方流水号,3 交易状态,4 交易时间,5 交易金额,6 附带信息,7 客户端类型
        String[] params = new String[]{out_trade_no,pay_type,trade_no,trade_status,gmt_payment,buyer_pay_amount,pay_request_attach,pay_client_type};
        Launch.log.info("[支付结果通知] ICE-请求参数:" + Arrays.toString(params));
        String json =
                client.settingProxy(serverName)
                        .settingReq("支付中间件",className,methodName)
                        .settingParam(params)
                        .executeSync();

        if (StringUtils.isNotEmpty(json)){
            Launch.log.info("[支付结果通知] ICE-返回结果:" + json);
            IceResult result = new Gson().fromJson(json,IceResult.class);



            flag =  result!=null && result.code == 200;
            if (flag) {
                if (result.data!=null && Integer.parseInt(String.valueOf(result.data)) == 1){

                    // 已取消订单, 自动退款
                    autoRefundTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            String type = pay_type;// 平台类型
                            String tradeNo =trade_no; //平台相关订单号
                            String refundNo = out_trade_no; //退款单号
                            String price = buyer_pay_amount; //退款金额
                            String priceTotal = buyer_pay_amount; //退款总金额
                            boolean isApp = pay_client_type.equals("1"); //是不是移动支付
                            Launch.log.info("后台订单已取消,发起退款: "+ type+" , "+tradeNo+" , "+ refundNo+" , "+priceTotal+" , "+ isApp);
                            RefundOrder rorder = new  RefundOrder(refundNo, tradeNo,new BigDecimal(price));
                            rorder.setTotalAmount(new BigDecimal(priceTotal));
                            Map<String, Object> map = type.equals("alipay")? AlipayImp.refund(rorder): WxpayImp.refund(rorder,isApp);
                            Launch.printMap(map);
                        }
                    },3000);

                }
            }
        }


        return flag;
    }
}
