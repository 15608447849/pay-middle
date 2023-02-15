package server.yeepay;

import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;

import bottle.tuples.Tuple2;
import bottle.util.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.google.gson.Gson;
import com.yeepay.yop.sdk.auth.credentials.provider.YopCredentialsProviderRegistry;


import com.yeepay.yop.sdk.base.auth.credentials.provider.YopFixedCredentialsProvider;
import com.yeepay.yop.sdk.base.config.YopAppConfig;
import com.yeepay.yop.sdk.base.config.provider.YopFixedSdkConfigProvider;
import com.yeepay.yop.sdk.base.config.provider.YopSdkConfigProviderRegistry;

import com.yeepay.yop.sdk.config.YopSdkConfig;
import com.yeepay.yop.sdk.config.enums.CertStoreType;

import com.yeepay.yop.sdk.config.provider.file.YopCertConfig;
import com.yeepay.yop.sdk.config.provider.file.YopCertStore;
import com.yeepay.yop.sdk.config.provider.file.YopHttpClientConfig;

import com.yeepay.yop.sdk.http.YopContentType;
import com.yeepay.yop.sdk.security.CertTypeEnum;

import com.yeepay.yop.sdk.service.common.YopCallbackEngine;
import com.yeepay.yop.sdk.service.common.YopClient;
import com.yeepay.yop.sdk.service.common.YopClientBuilder;
import com.yeepay.yop.sdk.service.common.callback.YopCallback;
import com.yeepay.yop.sdk.service.common.callback.YopCallbackRequest;

import com.yeepay.yop.sdk.service.common.request.YopRequest;
import com.yeepay.yop.sdk.service.common.response.YopResponse;

import server.Launch;
import server.beans.IceTrade;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.PrivateKey;
import java.text.SimpleDateFormat;
import java.util.*;

import static bottle.util.WebServerUtil.localHostPublicNetIpAddr;
import static com.yeepay.yop.sdk.utils.Encodes.decodeBase64;
import static server.common.CommFunc.getMapStr;


@PropertiesFilePath("/yeepay_onek.properties")
public class YeepayApiFunction {

//    private static final String PUBLIC_IP_HOST = localHostPublicNetIpAddr();
    private static final String PUBLIC_IP_HOST = "0.0.0.0";


    public static final String PAY_CALLBACK_URL_BODY = "/yeepay/result/";

    @PropertiesName("yeepay.appKey")
    private static String yeeapy_appKey;
    @PropertiesName("yeepay.parentMerchantNo")
    private static String parentMerchantNo;
    @PropertiesName("yeepay.isv_private_key")
    private static String isv_private_key;

    @PropertiesName("yeepay.wrchat.minProcPay.url")
    public static String minProcPayUrl;
    @PropertiesName("yeepay.wechat.midproc.appId")
    private static String wx_midproc_appid;
    @PropertiesName("yeepay.wechat.midproc.appSecret")
    private static String wx_midproc_appSecret;
    @PropertiesName("yeepay.wechat.midproc.authUrl")
    private static String wx_midproc_authUrl;
    @PropertiesName("yeepay.wechat.midproc.orgId")
    public static String wx_midproc_orgId;

    @PropertiesName("yeepay.wechat.app.appId")
    public static String wx_app_appid;

    @PropertiesName("yeepay.alipay.appId")
    private static String alipay_appid;


    static {
        ApplicationPropertiesBase.initStaticFields(YeepayApiFunction.class);
//        System.setProperty("yop.sdk.config.file","file:///D:\\A_Java\\JavaProjects\\IDEAWORK\\pay-middle\\src\\main\\resources\\config/yop_sdk_config_default.json");
        initConfig();
    }

    private static void initConfig() {

        YopSdkConfigProviderRegistry.registerProvider(new YopFixedSdkConfigProvider() {

            @Override
            public void removeConfig(String key) {

            }

            @Override
            protected YopSdkConfig loadSdkConfig() {
                YopSdkConfig yopSdkConfig = new YopSdkConfig();
                try {
                    yopSdkConfig.setServerRoot("https://openapi.yeepay.com/yop-center");
                    yopSdkConfig.setYosServerRoot("https://yos.yeepay.com/yop-center");

                    YopHttpClientConfig httpClient = new YopHttpClientConfig();
                    httpClient.setConnectTimeout(10000);
                    httpClient.setConnectRequestTimeout(10000);
                    httpClient.setReadTimeout(30000);
                    httpClient.setMaxConnTotal(200);
                    httpClient.setMaxConnPerRoute(100);

                    yopSdkConfig.setYopHttpClientConfig(httpClient);

                    YopCertStore yopCertStore = new YopCertStore();
                    yopCertStore.setEnable(true);
                    yopCertStore.setLazy(false);
                    yopSdkConfig.setYopCertStore(yopCertStore);
                    yopSdkConfig.setTrustAllCerts(true);
                } catch (Exception e) {
                    Log4j.error("Yeepay config 初始化失败",e);
                }

                return yopSdkConfig;
            }
        });

        YopCredentialsProviderRegistry.registerProvider(new YopFixedCredentialsProvider() {
            @Override
            protected YopAppConfig loadAppConfig(String appKey) {
                YopAppConfig yopAppConfig = new YopAppConfig();
                try {
                    yopAppConfig.setAppKey(yeeapy_appKey);
                    // SM2
                    YopCertConfig rsaCertConfig = new YopCertConfig();
                    rsaCertConfig.setCertType(CertTypeEnum.SM2);
                    rsaCertConfig.setStoreType(CertStoreType.STRING);
                    rsaCertConfig.setValue(isv_private_key);

                    List<YopCertConfig> isvPrivateKeys = new ArrayList<>();
                    isvPrivateKeys.add(rsaCertConfig);

                    yopAppConfig.setIsvPrivateKey(isvPrivateKeys);
                } catch (Exception e) {
                    Log4j.error("Yeepay Credentials 初始化失败",e);
                }
                return yopAppConfig;
            }
        });
    }


    private static String saveYeepayParam(String attr,String orderID,BigDecimal price){
//        attr = "1663812784250@DRUG@order2Server0@PayModule@payCallBack@536894493";

        // 存储信息及对应订单流水号信息到本地
        Map<String,String> map = new HashMap<>();
        map.put("attr",attr);
        map.put("orderID",orderID);
        map.put("price",price.toString());
        String json = GoogleGsonUtil.javaBeanToJson(map);

        String md5_fileName = EncryptUtil.encryption(json);

        File fileDict = new File("./yeepay");
        if(!fileDict.exists()) fileDict.mkdirs();
        File f = new File(fileDict,md5_fileName);

        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(f.toPath()), StandardCharsets.UTF_8))){
            bw.write(json);
            bw.flush();
            return md5_fileName;
        }catch (Exception e){
           throw new RuntimeException(e);
        }
    }

    public static String getLocalYeepayAttr(String md5){
        File fn = new File("./yeepay",md5);
        if (!fn.exists()) return null;

        try(BufferedReader bf = new BufferedReader(new InputStreamReader(Files.newInputStream(fn.toPath()), StandardCharsets.UTF_8))){
            String json = bf.readLine();
           Map<String,String> map = GoogleGsonUtil.string2Map(json);
           return map.get("attr");
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static void delLocalYeepayAttr(String md5){
        File fn = new File("./yeepay",md5);
        String json = null;
        if (fn.exists()) {
            try(BufferedReader bf = new BufferedReader(new InputStreamReader(Files.newInputStream(fn.toPath()), StandardCharsets.UTF_8))){
                json = bf.readLine();
            }catch (Exception e){
                e.printStackTrace();
            }
            boolean isDelete = fn.delete();
            Log4j.info("删除yeepay文件: "+ md5 +" 删除结果: "+ isDelete +"\n文件内容: "+ json);
        }
    }

    public static List<Tuple2<String,String>> getLocalYeepayQuerying(long interval){
        List<Tuple2<String,String>> list = new ArrayList<>();

        File fileDict = new File("./yeepay");
        if(!fileDict.exists()) fileDict.mkdirs();
        File[] files = fileDict.listFiles();
        if(files != null && files.length > 0) {
            for (File f : files){
                if ( (System.currentTimeMillis() - f.lastModified()) <= interval ) continue;
                try(BufferedReader bf = new BufferedReader(new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8))){
                    String json = bf.readLine();
                    Map<String,String> map = GoogleGsonUtil.string2Map(json);
                    String orderID = map.get("orderID");
                    list.add(new Tuple2<>(orderID,f.getName()));
                    Log4j.info("获取到待查询的本地文件: "+ f+" 对应订单ID: "+ orderID);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return list;
    }

    /** 微信获取用户openid */
    private static String wechat_openid(String code){
        Log4j.info("微信授权code: "+ code);
        String url = wx_midproc_authUrl + "?appid=" + wx_midproc_appid + "&secret=" + wx_midproc_appSecret+ "&js_code=" + code + "&grant_type=authorization_code";
        Log4j.info("微信请求: "+ url);
        String response = HttpUtil.formText(url,"GET",null);
        Log4j.info("微信获取登录凭证: "+ response);

        JSONObject json = JSON.parseObject(response);
        String errcode = json.getString("errcode");
        if (!StringUtil.isEmpty(errcode) && !"0".equals(errcode)) {
            throw new RuntimeException("微信鉴权处理失败 code: "+ code +"\nresponse: "+ response);
        }
        return json.getString("openid");
    }

    /**
     APP 预支付下单

     channel>>
     WECHAT:微信
     ALIPAY:支付宝
     UNIONPAY:银联云闪付
     DCEP:数字人民币

     payWay>>
     USER_SCAN:用户扫码
     MINI_PROGRAM:小程序支付
     WECHAT_OFFIACCOUNT:微信公众号
     ALIPAY_LIFE:支付宝生活号
     JS_PAY:JS支付
     SDK_PAY:SDK支付
     H5_PAY:H5支付

    */
    public static Tuple2<String,String> createPayOrderMobileApp(String payChannel,String payWay,String payPlatformCode, String attr,String orderNo,BigDecimal price,String subject, Date expiredTime){
        try {

            String appid = null;
            String openID = null;

            if (payChannel.equals("WECHAT")){
                appid = wx_midproc_appid;
                openID = wechat_openid(payPlatformCode);
            }

            YopClient yopClient = YopClientBuilder.builder().build();
            YopRequest yopRequest = new YopRequest("/rest/v1.0/aggpay/pre-pay", "POST");
            yopRequest.addParameter("parentMerchantNo",parentMerchantNo);
            yopRequest.addParameter("merchantNo", parentMerchantNo);
            yopRequest.addParameter("orderId", orderNo);
            yopRequest.addParameter("orderAmount", price);
            yopRequest.addParameter("goodsName", subject);
            yopRequest.addParameter("notifyUrl", Launch.domain + PAY_CALLBACK_URL_BODY+ saveYeepayParam(attr,orderNo,price));
            yopRequest.addParameter("expiredTime",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(expiredTime));
            yopRequest.addParameter("payWay", payWay);
            yopRequest.addParameter("channel", payChannel);
            yopRequest.addParameter("userIp", PUBLIC_IP_HOST);
            yopRequest.addParameter("scene", "OFFLINE");

            if (appid!=null) yopRequest.addParameter("appId", appid);
            if (openID!=null) yopRequest.addParameter("userId", openID);

            Log4j.info("下单请求参数:\t"+yopRequest.getParameters());

            YopResponse yopResponse = yopClient.request(yopRequest);
            String response = yopResponse.getStringResult();
            Log4j.info("下单请求结果:\t"+response);

            // 获取预支付信息
            JSONObject json = JSON.parseObject(response);
            String errcode = json.getString("code");
            if (!StringUtil.isEmpty(errcode) && !"00000".equals(errcode)) {
                throw new RuntimeException(json.getString("message"));
            }
            return new Tuple2<>(json.getString("prePayTn"),null);
        } catch (Exception e) {
//            Log4j.error(e);
            return new Tuple2<>(null,e.getMessage());
        }

    }

    public static String createPayOrderORCode(String attr, String orderNo, BigDecimal price, String subject, Date expiredTime){
        try {

            YopClient yopClient = YopClientBuilder.builder().build();
            YopRequest yopRequest = new YopRequest("/rest/v1.0/aggpay/pay-link", "POST");
            yopRequest.addParameter("parentMerchantNo",parentMerchantNo);
            yopRequest.addParameter("merchantNo", parentMerchantNo);
            yopRequest.addParameter("orderId", orderNo);
            yopRequest.addParameter("orderAmount", price);
            yopRequest.addParameter("goodsName", subject);
            yopRequest.addParameter("notifyUrl", Launch.domain + PAY_CALLBACK_URL_BODY+saveYeepayParam(attr,orderNo,price));
            yopRequest.addParameter("expiredTime",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(expiredTime));
            yopRequest.addParameter("userIp", PUBLIC_IP_HOST);
            yopRequest.addParameter("scene", "OFFLINE");

            Log4j.info("聚合码 下单请求参数:\t"+yopRequest.getParameters());

            YopResponse yopResponse = yopClient.request(yopRequest);
            String response = yopResponse.getStringResult();
            Log4j.info("聚合码 下单请求结果:\t"+response);

            // 获取预支付信息
            JSONObject json = JSON.parseObject(response);
            String errcode = json.getString("code");
            if (!StringUtil.isEmpty(errcode) && !"00000".equals(errcode)) {
                throw new RuntimeException(json.getString("message"));
            }
            return json.getString("qrCodeUrl");// 订单二维码地址
        } catch (Exception e) {
            Log4j.error(e);
        }
        return null;
    }




    /**
     * 查询结果
     status>>
     PROCESSING：订单待支付
     SUCCESS：订单支付成功
     TIME_OUT：订单已过期
     FAIL:订单支付失败
     CLOSE:订单关闭
     * */
    public static String queryPayOrder(String orderid){
        try {
            YopClient yopClient = YopClientBuilder.builder().build();
            YopRequest yopRequest = new YopRequest("/rest/v1.0/trade/order/query", "GET");
            yopRequest.addParameter("parentMerchantNo",parentMerchantNo);
            yopRequest.addParameter("merchantNo", parentMerchantNo);
            yopRequest.addParameter("orderId", orderid);

            Log4j.info("查询 请求参数:\t"+yopRequest.getParameters());

            YopResponse yopResponse = yopClient.request(yopRequest);
            String response = yopResponse.getStringResult();
            Log4j.info("查询 请求结果:\t"+response);
            return response;

        } catch (Exception e) {
            Log4j.error(e);
        }
        return null;
    }


    /**
     退款
     status>>
     退款订单状态
     PROCESSING：退款处理中
     SUCCESS：退款成功
     FAILED：退款失败
     CANCEL:退款关闭,商户通知易宝结束该笔退款后返回该状态
     SUSPEND:退款中断,如需继续退款,请调用上送卡信息退款进行打款退款;如想结束退款,请调用结束退款来关闭退款订单
     * */
    /*[ INFO] 2022-09-22 16:37:49:809 main	查询请求结果:	{
        "code" : "OPR00000",
                "message" : "成功",
                "parentMerchantNo" : "10088770384",
                "merchantNo" : "10088770384",
                "orderId" : "2209220013785835007",
                "refundRequestId" : "2209220013785835007001",
                "uniqueRefundNo" : "1002202209220000004430922722",
                "status" : "PROCESSING",
                "refundAmount" : "0.01",
                "residualAmount" : "0.00",
                "refundRequestDate" : "2022-09-22 16:37:48",
                "deductionFundDate" : "2022-09-22 16:37:48",
                "refundMerchantFee" : "0.00",
                "refundAccountDetail" : "[{\"accountType\":\"SETTLE_ACCOUNT\",\"debitAmount\":0.01}]"
    }*/
    public static String refundPayOrder(String orderid,String refundRequestId,String uniqueOrderNo,String refundAmount){
        try {
            YopClient yopClient = YopClientBuilder.builder().build();
            YopRequest yopRequest = new YopRequest("/rest/v1.0/trade/refund", "POST");
            yopRequest.addParameter("parentMerchantNo",parentMerchantNo);
            yopRequest.addParameter("merchantNo", parentMerchantNo);
            yopRequest.addParameter("orderId", orderid);//  收款交易对应的商户收款请求号
            yopRequest.addParameter("refundRequestId", refundRequestId);// 商户退款请求号(流水号)
            yopRequest.addParameter("uniqueOrderNo", uniqueOrderNo);// 收款交易对应的易宝收款订单号
            yopRequest.addParameter("refundAmount", refundAmount);// 退款申请金额

            Log4j.info("退款 请求参数:\t"+yopRequest.getParameters());

            YopResponse yopResponse = yopClient.request(yopRequest);
            String response = yopResponse.getStringResult();
            Log4j.info("退款 请求结果:\t"+response);

           return response;
        } catch (Exception e) {
            Log4j.error(e);
        }
        return null;
    }

    /**
     * 提交退款申请后，通过调用该接口查询退款状态
     *
     * */
/*
    {
        "code" : "OPR00000",
            "message" : "成功",
            "parentMerchantNo" : "10088770384",
            "merchantNo" : "10088770384",
            "orderId" : "2209220013785835007",
            "refundRequestId" : "2209220013785835007001",
            "uniqueOrderNo" : "1013202209220000004428424545",
            "uniqueRefundNo" : "1002202209220000004430922722",
            "refundAmount" : 0.01,
            "returnMerchantFee" : 0.00,
            "status" : "SUCCESS",
            "refundRequestDate" : "2022-09-22 16:37:48",
            "refundSuccessDate" : "2022-09-22 16:37:50",
            "realDeductAmount" : 0.01,
            "realRefundAmount" : 0.01,
            "cashRefundFee" : 0.01,
            "disAccountAmount" : 0.01,
            "refundAccountType" : "SETTLE_ACCOUNT",
            "refundAccountDetail" : "[{\"accountType\":\"SETTLE_ACCOUNT\",\"debitAmount\":0.01}]",
            "channelReceiverInfo" : "[{\"receiverType\":\"ALIPAYACCOUNT\"}]"
    }
*/
    public static String refundPayOrderQuery(String orderid,String refundRequestId,String uniqueOrderNo){
        try {
            YopClient yopClient = YopClientBuilder.builder().build();
            YopRequest yopRequest = new YopRequest("/rest/v1.0/trade/refund/query", "GET");
            yopRequest.addParameter("parentMerchantNo",parentMerchantNo);
            yopRequest.addParameter("merchantNo", parentMerchantNo);
            yopRequest.addParameter("orderId", orderid);//  收款交易对应的商户收款请求号
            yopRequest.addParameter("refundRequestId", refundRequestId);// 商户退款请求号(流水号)
            yopRequest.addParameter("uniqueOrderNo", uniqueOrderNo);// 收款交易对应的易宝收款订单号

            Log4j.info("查询退款 请求参数:\t"+yopRequest.getParameters());

            YopResponse yopResponse = yopClient.request(yopRequest);
            String response = yopResponse.getStringResult();
            Log4j.info("查询退款 请求结果:\t"+response);

            return response;
        } catch (Exception e) {
            Log4j.error(e);
        }
        return null;
    }

    /** 支付结果处理 */
    // {"channelOrderId":"072022092222001433571416870692","orderId":"2209220013785835007","bankOrderId":"ST5538495790220922","paySuccessDate":"2022-09-22 10:13:55","channel":"ALIPAY","payWay":"USER_SCAN","uniqueOrderNo":"1013202209220000004428424545","orderAmount":"0.01","payAmount":"0.01","payerInfo":"{\"bankCardNo\":\"\",\"bankId\":\"ALIPAY\",\"buyerLogonId\":\"156****7849\",\"cardType\":\"CREDIT\",\"mobilePhoneNo\":\"\",\"userID\":\"2088902342933577\"}","realPayAmount":"0.01","parentMerchantNo":"10088770384","merchantNo":"10088770384","status":"SUCCESS"}
    public static String payResultCallback(HttpServletRequest request){
        //获取回调数据
        try {
            Map<String,String> headers = new HashMap<>();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()){
                String headerKey = headerNames.nextElement();
                headers.put(headerKey,request.getHeader(headerKey));
            }

            try(BufferedReader reader =  new BufferedReader(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))){
                String content = reader.readLine();
                Log4j.info("接收 易宝支付结果通知: "+  getMapStr(headers)+"\n密文: "+ content );

                YopCallbackRequest yopRequest =
                        new YopCallbackRequest("/rest/v1.0/test/app-alias/create", "POST")
                                .setContentType(YopContentType.JSON)
                                .setHeaders(headers)
                                .setContent(content);
                YopCallback yopCallback = YopCallbackEngine.parse(yopRequest);
                Log4j.info("易宝支付结果通知 明文: " +yopCallback.getBizData());
                return yopCallback.getBizData();
            }

        } catch (Exception e) {
            Log4j.error(e);
        }
        return null;
    }




    public static String wechat_config_add(){
        try {
            YopClient yopClient = YopClientBuilder.builder().build();
            YopRequest yopRequest = new YopRequest("/rest/v2.0/aggpay/wechat-config/add", "POST");
            yopRequest.addParameter("parentMerchantNo",parentMerchantNo);
            yopRequest.addParameter("merchantNo", parentMerchantNo);
            List<Map<String,String>> list = new ArrayList<Map<String,String>>(){};
            list.add(new HashMap<String,String>(){
                {
                put("appId",wx_midproc_appid);
                put("appSecret",wx_midproc_appSecret);
                put("appIdType","MINI_PROGRAM");
                }
            });

            list.add(new HashMap<String,String>(){
                {
                    put("appId","wx6e72dac3e21c3b97");
                    put("appSecret","c47ef8343c9b93ad299c54f5242810d8");
                    put("appIdType","OFFICIAL_ACCOUNT");
                }
            });

            list.add(new HashMap<String,String>(){
                {
                    put("appId","wxed67c3663806cb77");
                    put("appSecret","a66bd4ba03322b4788a58000cf4370cc");
                    put("appIdType","OFFICIAL_ACCOUNT");
                }
            });

            yopRequest.addParameter("appIdList", JSON.toJSONString(list));


            Log4j.info("添加 请求参数:\t"+yopRequest.getParameters());

            YopResponse yopResponse = yopClient.request(yopRequest);
            String response = yopResponse.getStringResult();
            Log4j.info("添加 请求结果:\t"+response);
            return response;
        } catch (Exception e) {
            Log4j.error(e);
        }
        return null;
    }

    public static String wechat_config_query(){
        try {
            YopClient yopClient = YopClientBuilder.builder().build();
            YopRequest yopRequest = new YopRequest("/rest/v2.0/aggpay/wechat-config/query", "GET");
            yopRequest.addParameter("parentMerchantNo",parentMerchantNo);
            yopRequest.addParameter("merchantNo", parentMerchantNo);
            Log4j.info("查询 请求参数:\t"+yopRequest.getParameters());
            YopResponse yopResponse = yopClient.request(yopRequest);
            String response = yopResponse.getStringResult();
            Log4j.info("查询 请求结果:\t"+response);
            return response;
        } catch (Exception e) {
            Log4j.error(e);
        }
        return null;
    }
    public static void main(String[] args)  {
//       String resp = createPayOrderMobileApp("ALIPAY","USER_SCAN",null,
//                "536674842@prevPay@callback", "202209090001", new BigDecimal("0.01"), "一块医药",
//                new Date(System.currentTimeMillis() + 10 * 1000 * 60L));
//        System.out.println(resp);

//        queryPayOrder("202209090001");

//        final PrivateKey privateKey = RSAKeyUtils.string2PrivateKey(isv_private_key);
//          String cipherText ="QB5wsb5VEU513BqdLph694x9UhXLnS-3E7iPVYE1lJSGL2zaPMj6C2ga34MIFpKtii32yUdEigZTL4M9J4MDl1OIUU4wbIz99Ut7jhPButLoE4fMwxk_TBHQhYCu6CqVNjSKWYORUwqQ_iZXJpH6z3-mW0mvv-fmRxUkkTKtFG_XST9Yd9d1trk0nQlytEYVfC8gstYhCxLR_-qyTDlhkyGdSgtRGhr2gjC1yEjlDus-ibUXMfrBu_rR96IH6iilW6okVxF3H8A2HgwidIjFSbv5isw3N4jXLpzbrPmfinYo3N42slF_isNkfwAzXYjKuqJUhlkaiLFsh5SlWb9-gDG6zepUCHTD7APD8DMezbRM6E-0PcUyOB4t2sx1hnRIcH2C8lT_UprAi3E177QUZgXfef2wRgPA0YzNxXnK4NQJG6ohscgkVRgot4OD_POqfT_G5DS-kMU5d9zS8Cm1GZXItVSMExxvLqkwe4lFtxTbvmh9I6x-i68KucXnj0XQrewBK-b0XAi3-_uH6BsRUHJr9SVxokFjr7304Hf4juPDp6c6nc381q1bXjgJqJKV2wQXsmdkYfOOI88dkWXZHYXW9mcjZMAzUpoc4hUsddo2Sd_53uP8wO-3H4NO9dOITdgnYmpMRIiHIcQ-TJB0gGQXtWS30ruZbN2Z9CJbG-M9hNgwQpThTiv1AgbE_gMw7q3mIEkww5P5Q9_tPR9vYu86neCQFYocPqcwxJFh1qc";
//        System.out.println(DigitalEnvelopeUtils.decrypt(cipherText, privateKey));
//        System.out.println(1);

//        String cipherText = "yop-encrypt-v1//SM4_CBC_PKCS5Padding/BI3ykYxD5QHF9hjLnB_9FfDHkMA03m2Ic6-T9EIHevoiPQ1tnk5oeskonIF5N-UjYHS3_SwHtqTfhmxRlG8e6dWgUjFDJpz4TJYbBvsUHuOAuFeaIKYlfRfZ3OYNtGhcgPPRSmT5FcYYeJK15-D9xpw/X0jlQWUM9Unw1lHfIKttdw;eW9w/stream//JA";
//
//
//        String decryptMsg = com.yeepay.yop.sdk.security.DigitalEnvelopeUtils.decrypt(cipherText, yeeapy_appKey, "SM2");
//        System.out.println(decryptMsg);


//        refundPayOrderQuery("2209220013785835007","2209220013785835007001","1013202209220000004428424545");
        wechat_config_add();
//        wechat_config_query();
//        Log4j.info("李世平");
//        System.out.println("name : 李世平");

    }

}
