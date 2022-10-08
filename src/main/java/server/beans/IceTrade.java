package server.beans;

import bottle.util.Log4j;
import com.egzosn.pay.common.bean.RefundOrder;
import com.egzosn.pay.common.util.str.StringUtils;
import com.google.gson.Gson;
import framework.client.IceClientUtils;
import server.payimps.AlipayImp;
import server.payimps.WxpayImp;
import server.payimps.YeepayImp;


import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @Author: leeping
 * @Date: 2019/4/18 16:07
 */

public class IceTrade {

    private static Timer autoRefundTimer = new Timer();

    private static LinkedBlockingQueue<IceTrade> queue = new LinkedBlockingQueue<>();

    private static Thread notify_thread = new Thread(() -> {
        while (true){
            try{
                IceTrade trade = queue.take();
                while (!trade.notifyIceServer()) {
                    Thread.sleep(500);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    });

    static {
        notify_thread.setDaemon(true);
        notify_thread.setName("notify_pay_result_thread"+notify_thread.getId());
        notify_thread.start();
    }

    //第三方平台交易流水号
    private String trade_no = "";
    //后台订单号或流水号
    private String out_trade_no = "";
    //付款平台 alipay, wxpay
    private String pay_type  = "";
    //交易完成时间
    private String gmt_payment ="";
    //交易状态 1成功
    private String trade_status= "";
    //交易金额
    private String buyer_pay_amount = "";
    //支付客户端类型 0-PC 1-APP
    private String pay_client_type = "0";
    //支付请求附带信息: 时间戳,服务标识,服务名,回调类名,回调方法,公司码
    private String pay_request_attach;

    public IceTrade(String body,String trade_no, String out_trade_no, String pay_type, String gmt_payment, String trade_status,String buyer_pay_amount,String pay_client_type) {
        this.trade_no = trade_no;
        this.out_trade_no = out_trade_no;
        this.pay_type = pay_type;
        this.gmt_payment = gmt_payment;
        this.trade_status = trade_status;
        this.buyer_pay_amount = buyer_pay_amount;
        this.pay_client_type = pay_client_type;
        this.pay_request_attach = body;
    }

    private boolean notifyIceServer() {
        boolean flag = false;
        // 时间戳@服务标识@服务名@回调类名@回调方法@公司码
        String[] attrArr = this.pay_request_attach.split("@");

        String iceTag = attrArr[1];
        String sn = attrArr[2];
        String cls = attrArr[3];
        String med = attrArr[4];
        String compid = attrArr[5];

        // 0 后台订单号或流水号,1 支付平台类型,2 第三方流水号,3 交易状态,4 交易时间,5 交易金额,6 公司码,7 客户端类型
        String[] params = new String[]{out_trade_no,pay_type,trade_no,trade_status,gmt_payment,buyer_pay_amount,compid,pay_client_type};
        Log4j.info("[支付结果通知] ICE-请求参数:" + Arrays.toString(params));

        if (iceTag.startsWith("预留参数")) iceTag = "DRUG"; // 兼容处理
        String json = IceClientUtils.executeICE(iceTag,"空间折叠支付中间件",sn,cls,med,params);

        if (StringUtils.isNotEmpty(json)){

            IceResult result = new Gson().fromJson(json,IceResult.class);
            flag =  result!=null && result.code == 200;
            Log4j.info("[支付结果通知] ICE-返回结果:" + json+" 响应flag = "+ flag);

            if (flag) {
                deleteLocalNotifyFile(this);

                if (result.data!=null && Double.parseDouble(String.valueOf(result.data)) == 1.0){
                    Log4j.info("[支付结果通知] 5秒后自动取消订单");

                    // 已取消订单, 自动退款
                    autoRefundTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            String type = pay_type;// 平台类型
                            String tradeNo =trade_no; //三方平台相关订单号
                            String refundNo = out_trade_no; //一块医药退款单号/流水号
                            String price = buyer_pay_amount; //退款金额
                            String priceTotal = buyer_pay_amount; //退款总金额
                            boolean isApp = pay_client_type.equals("1"); //是不是移动支付

                            RefundOrder rorder = new  RefundOrder(refundNo, tradeNo,new BigDecimal(price));
                            rorder.setTotalAmount(new BigDecimal(priceTotal));

                            Log4j.info("后台订单已取消,自动尝试发起退款: type="+ type+" , tradeNo="+tradeNo+" , refundNo="+ refundNo+" , priceTotal="+priceTotal+" , isApp="+ isApp);
                            if (type.equals("alipay")){
                                AlipayImp.refund(rorder);
                            }
                            if (type.equals("wxpay")){
                                WxpayImp.refund(rorder,isApp);
                            }
                            if (type.equals("yeepay")){
                                YeepayImp.refund(refundNo,refundNo,tradeNo,price);
                            }


                        }
                    },5000);

                }
            }
        }

        return flag;
    }

    // 发送支付结果到一块医药业务后台
    public static boolean sendTrade(IceTrade trade){
        //持久化存储 JSON文件
        if (writeNotifyToLocal(trade)){
//            if (trade.notifyIceServer()){
//                return true;
//            }else{
//                return queue.offer(trade);
//            }
            return queue.offer(trade);
        }
        return false;

    }

    public static void resumeLocalNotifications() {
        File fileDict = new File("./trade");
        if(!fileDict.exists()) fileDict.mkdirs();
        File[] files = fileDict.listFiles();
        if(files == null || files.length == 0) return;

        for (File f : files){
            try(BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))){
                String json = bf.readLine();
                IceTrade trade = new Gson().fromJson(json,IceTrade.class);
                Log4j.debug("本地恢复支付结果通知: " + f.getName()+"\n\t"+json);
                queue.put(trade);
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    private static boolean writeNotifyToLocal(IceTrade trade){
        String json = new Gson().toJson(trade);
        String fileName = trade.out_trade_no;

        File fileDict = new File("./trade");
        if(!fileDict.exists()) fileDict.mkdirs();
        File f = new File(fileDict,fileName);

        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(f.toPath()), StandardCharsets.UTF_8))){
            bw.write(json);
            bw.flush();
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }
       return false;
    }

    private void deleteLocalNotifyFile(IceTrade trade) {
        String fileName = trade.out_trade_no;
        File fileDict = new File("./trade");
        if(!fileDict.exists()) fileDict.mkdirs();
        File f = new File(fileDict,fileName);
        if (f.exists()) f.delete();
    }

}
