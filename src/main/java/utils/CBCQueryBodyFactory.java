package utils;

import com.google.gson.Gson;
import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;
import server.Launch;
;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

import static utils.CBCUtil.*;

@PropertiesFilePath("/application.properties")
public class CBCQueryBodyFactory {

    @PropertiesName("ccb.url")
    public static String ccbUrl;
    @PropertiesName("ccb.third.sys.id")
    public static String thirdSysID;
    @PropertiesName("ccb.rsa.pub")
    public static String rsaPub;
    @PropertiesName("ccb.md5.key")
    public static String md5Key;

    private static final Gson gson =  new Gson();

    static {
        ApplicationPropertiesBase.initStaticFields(CBCQueryBodyFactory.class);
    }

    private static void print(String log){
        Launch.log.info("建行支付对接日志>\t" + log);
    }

    private static final class MessageStruct{
        private String ThirdSysID; // 对接系统的ID，请联系建设银行获取
        private String TxCode;// 交易码-> 接口文档对应字段备注内的的接口标识
        private int RequestType;// 请求类型
        private String Data; // 接口所需数据->RSA加密
        private String Auth; // ThirdSysID+ TxCode+ RequestType+ Data+MD5Key字段的MD5码
        private int errorCode; // 错误码，发送请求时无需上送；接口响应报错时返回该字段
        private String errorMsg; // 错误信息，发送请求时无需上送；接口响应报错时返回该字段
    }
    /*
     * 最外层报文格式
     *  ThirdSysID  对接系统的ID，请联系建设银行获取
     *  TxCode 交易码 ps:接口文档对应字段备注内的的接口标识
     *  RequestType 1-前端页面请求 2-后端请求:
     *      1-> 请求本接口之后，善融商务企业商城通过调用第三方平台提供的接口将处理结果返回给第三方
     *      2-> 请求本接口之后，页面上直接返回处理结果, 返回结果的类型为'最外层报文格式'相同格式json
     *   Data 相应接口所需数据
     *   Auth ThirdSysID + TxCode + RequestType + Data(RSA加密) + MD5Key->MD5
     */
    public static String gen_query_body(String txCode,int requestType, Object data) throws Exception{

        String dataJson;
        if (data instanceof String){
            dataJson = String.valueOf(data);
        }else{
            dataJson = gson.toJson(data);
        }

        if (dataJson == null || dataJson.length() == 0) throw new IllegalArgumentException("接口数据异常");

        String dataJson_RSAEncrypt = clientEncrypt(dataJson,rsaPub);
        String dataJson_RSAEncrypt_URLEncode = URLEncoder.encode(dataJson_RSAEncrypt,"UTF-8");

        String authMD5 =  getMD5(thirdSysID + txCode + requestType + dataJson_RSAEncrypt_URLEncode + md5Key, "UTF-8");

        MessageStruct messageStruct = new MessageStruct();
        messageStruct.ThirdSysID = thirdSysID;
        messageStruct.TxCode = txCode;
        messageStruct.Data = dataJson_RSAEncrypt_URLEncode;
        messageStruct.Auth = authMD5;
        messageStruct.RequestType = requestType;

        String requestBody = gson.toJson(messageStruct);

        print( "接口 "+ txCode+","+requestType
                +"\n\t元数据: "+ dataJson
                +"\n\tRSA加密数据: "+ dataJson_RSAEncrypt
                +"\n\tURLEncode编码数据: "+ dataJson_RSAEncrypt_URLEncode
                +"\n\tMD5签名: "+ authMD5
                +"\n\t请求内容: "+ requestBody
        );
        return requestBody;
    }




    public static void main(String[] args) throws Exception{

        String order = "{\"TxCode\":\"MALL10002\",\"TransID\":\"zq2015112010203300010\",\"OrderInfos\":#OrderInfos,\"Expand1\":\"扩展信息1\"}";

        String requestJson = gen_query_body("MALL10002",2,order);

        String responseStr = HTTPUtil.contentToHttpBody(ccbUrl,"POST",requestJson);

        System.out.println(responseStr);

    }










}
