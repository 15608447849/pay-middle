package server.beans;

import com.egzosn.pay.common.bean.RefundOrder;
import com.egzosn.pay.common.util.str.StringUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import server.Launch;
import server.apyimp.AlipayImp;
import server.apyimp.WxpayImp;
import utils.IceClient;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: leeping
 * @Date: 2019/4/18 16:07
 */
public class IceTrade {

    //携带ice接口信息
    public String body = "";
    //第三方批次号
    public String trade_no = "";
    //后台订单号
    public String out_trade_no = "";
    //付款平台
    public String pay_type  = "";
    //交易完成时间
    public String gmt_payment ="";
    //交易状态 1成功
    public String trade_status= "";
    //交易金额
    public String buyer_pay_amount = "";
    //支付客户端类型
    public String pay_client_type = "0";

    private IceClient client = null;
    private String serverName;
    private String className;
    private String methodName;

    public IceTrade(String body, String trade_no, String out_trade_no, String pay_type, String gmt_payment, String trade_status,String buyer_pay_amount,String pay_client_type) {
        this.body = body;
        this.trade_no = trade_no;
        this.out_trade_no = out_trade_no;
        this.pay_type = pay_type;
        this.gmt_payment = gmt_payment;
        this.trade_status = trade_status;
        this.buyer_pay_amount = buyer_pay_amount;
        this.pay_client_type = pay_client_type;
        pauseBody();
    }

    private void pauseBody() {
        //分解body 获取ice地址信息 body=DemoIceGrid@192.168.1.152:4061@orderServer0@PayModule@payCallBack
        String[] arr ;
        try {
            Launch.log.info("body = "+ body);
            arr  = body.split("@");
            if (arr.length < 6) throw new Exception("body = "+ body);
            body = body.substring(body.lastIndexOf("@")+1);
            Launch.log.info("body = "+ body);
            client = new IceClient(arr[0],arr[1]);
            client.startCommunication(); //连接
            serverName = arr[2];
            className = arr[3];
            methodName = arr[4];
        } catch (Exception e) {
            Launch.log.error(e.toString());
        }
    }


    public boolean notifyIceServer() {

        boolean flag = false;
        boolean isRefund = false;
        if (client != null){
            //后台订单号,平台类型,第三方流水号,交易状态,交易时间,交易金额,附带信息,客户端类型
            String[] params = new String[]{out_trade_no,pay_type,trade_no,trade_status,gmt_payment,buyer_pay_amount,body,pay_client_type};
            Launch.log.info("ICE-请求参数:" + Arrays.toString(params));
            String json =
                    client.settingProxy(serverName)
                            .settingReq("",className,methodName)
                            .settingParam(params)
                            .executeSync();
            if (StringUtils.isNotEmpty(json)){
                Launch.log.info("ICE-返回结果:" + json);
                HashMap<String,Object> hashMap =
                        new Gson().fromJson(json,new TypeToken<HashMap<String,Object>>(){}.getType());
                Object code = hashMap.get("code");
                flag =  code!=null && code.toString().contains("200");
                if (flag) {
                    Object data = hashMap.get("data");
                    if (data!=null && data.toString().contains("1")){
                        isRefund = true;
                    }
                }
            }
            client.stopCommunication();
        }
        if (flag && isRefund){
            //延时发起退款
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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

            }).start();
        }
        return flag;
    }
}
