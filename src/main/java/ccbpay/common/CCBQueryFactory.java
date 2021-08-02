package ccbpay.common;
import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;
import server.Launch;
import utils.HTTPUtil;


import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@PropertiesFilePath("/application.properties")
public class CCBQueryFactory {

    @PropertiesName("ccb.url")
    public static String ccbUrl;
    @PropertiesName("ccb.third.sys.id")
    public static String thirdSysID; //对接系统的ID，请联系建设银行获取
    @PropertiesName("ccb.rsa.pub")
    public static String rsaPub;
    @PropertiesName("ccb.md5.key")
    public static String md5Key;

    /**
     * 加密算法RSA
     */
    private static final String KEY_ALGORITHM = "RSA";

    /**
     * RSA最大加密明文大小
     */
    private static final int MAX_ENCRYPT_BLOCK = 117;

    /**
     * RSA最大解密密文大小
     */
    private static final int MAX_DECRYPT_BLOCK = 128;

    /**
     * Method: decryptBASE64 <br/>
     * description: 解码返回byte <br/>
     *
     * @param key
     * @return
     * @throws Exception
     */
    private static byte[] decryptBASE64(String key) throws Exception {
        return Base64.decodeBase64(key);
    }

    /**
     * Method: encryptBASE64 <br/>
     * description: 编码返回字符串 <br/>
     *
     * @param key
     * @return
     * @throws Exception
     */
    private static String encryptBASE64(byte[] key) throws Exception {
        return Base64.encodeBase64String(key);
    }

    /**
     * 获取base64加密后的字符串的原始公钥
     *
     * @param keyStr
     * @return
     * @throws Exception
     */
    private static Key getPublicKeyFromBase64KeyEncodeStr(String keyStr) throws Exception {
        byte[] keyBytes = decryptBASE64(keyStr);
        // 取得公钥
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(x509KeySpec);
    }

    /**
     * Method: encrypt <br/>
     * description: 客户端响应使用公钥分段加密 <br/>
     *
     * @param dataStr
     *            加密内容，明文
     * @param publicKeyStr
     *            公钥内容
     * @return 密文
     * @throws Exception
     */
    static String clientEncrypt(String dataStr, String publicKeyStr) throws Exception {
        String encodedDataStr;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] data = dataStr.getBytes(StandardCharsets.UTF_8);
            // 获取原始公钥
            Key decodePublicKey = getPublicKeyFromBase64KeyEncodeStr(publicKeyStr);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, decodePublicKey);
            int inputLen = data.length;
            int offSet = 0;
            byte[] cache;
            int i = 0;
            // 对数据分段加密
            while (inputLen - offSet > 0) {
                if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                    cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
                } else {
                    cache = cipher.doFinal(data, offSet, inputLen - offSet);
                }
                out.write(cache, 0, cache.length);
                i++;
                offSet = i * MAX_ENCRYPT_BLOCK;
            }
            byte[] encryptedData = out.toByteArray();
            encodedDataStr = encryptBASE64(encryptedData);
        }
        return encodedDataStr;
    }


    /**
     * Method: clientDecrypt <br/>
     * description: 公钥分段解密 <br/>
     *
     * @param dataStr
     *            解密内容，密文
     * @param publicKey
     *            公钥
     * @return 明文
     * @throws Exception
     */
    private static String clientDecrypt(String dataStr, String publicKey) throws Exception {
        String decodedDataStr;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte[] encryptedData = decryptBASE64(dataStr);
            // 获取原始私钥
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            Key decodePrivateKey = getPublicKeyFromBase64KeyEncodeStr(publicKey);
            Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, decodePrivateKey);
            int inputLen = encryptedData.length;

            int offSet = 0;
            byte[] cache;
            int i = 0;
            // 对数据分段解密
            while (inputLen - offSet > 0) {
                if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                    cache = cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);
                } else {
                    cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet);
                }
                out.write(cache, 0, cache.length);
                i++;
                offSet = i * MAX_DECRYPT_BLOCK;
            }
            byte[] decryptedData = out.toByteArray();
            decodedDataStr = new String(decryptedData, StandardCharsets.UTF_8);
        }
        return decodedDataStr;
    }


    /** MD5签名校验 */
    private static String getMD5(String text, String charset) throws Exception {
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9','a', 'b', 'c', 'd', 'e', 'f'};
        byte[] strTemp = text.getBytes(charset); // 设字符编码
        MessageDigest mdTemp = MessageDigest.getInstance("MD5");

        mdTemp.update(strTemp);

        byte[] md = mdTemp.digest();
        int j = md.length;
        char[] str = new char[j * 2];
        int k = 0;
        for (byte byte0 : md) {
            str[k++] = hexDigits[byte0 >>> 4 & 0xf];
            str[k++] = hexDigits[byte0 & 0xf];
        }
        return new String(str);
    }



    public static void ccb_print(String log){
        Launch.log.info(" 【CCB】 " + log);
    }

    private static final class MessageStruct{
        private String ThirdSysID;
        private String TxCode;
        private String Data;
        private String Auth;
        private String errorCode; // 错误码，发送请求时无需上送；接口响应报错时返回该字段
        private String errorMsg; // 错误信息，发送请求时无需上送；接口响应报错时返回该字段
    }

    /** 加密数据 */
    public static String encrypt_ccb_data(String data){
        try {
            String dataJson_RSAEncrypt = clientEncrypt(data,rsaPub);
            String dataJson_RSAEncrypt_URLEncode = URLEncoder.encode(dataJson_RSAEncrypt,"UTF-8");
            //ccb_print("\n"+data +"\n============加密数据>>>\n" + dataJson_RSAEncrypt_URLEncode);
            return dataJson_RSAEncrypt_URLEncode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    /** 解密数据 */
    public static String decrypt_ccb_data(String data){
        try {
            String data_URLDecoder = URLDecoder.decode( data , "UTF-8");
            String data_URLDecoder_RSADecrypt = clientDecrypt(data_URLDecoder,rsaPub);
            //ccb_print("\n"+data_URLDecoder_RSADecrypt +"\n============解密数据>>>\n" + data);
            return data_URLDecoder_RSADecrypt;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }



    /*
     * 最外层报文格式
     *  ThirdSysID  对接系统的ID，请联系建设银行获取
     *  TxCode 交易码 ps:接口文档对应字段备注内的的接口标识
     *  RequestType 1-前端页面请求 2-后端请求:
     *      1-> 请求本接口之后，善融商务企业商城通过调用第三方平台提供的接口将处理结果返回给第三方
     *      2-> 请求本接口之后，页面上直接返回处理结果, 返回结果的类型为'最外层报文格式'相同格式json
     *   Data 相应接口所需数据
     *   Auth ThirdSysID + TxCode + Data(RSA加密) + MD5Key->MD5
     */
    private static String sendQuery(String txCode , int requestType, String data) throws Exception{
        ccb_print("[ccb] [" + txCode + "] sendQuery data : " + data);

        data = encrypt_ccb_data(data);
        String authMD5 =  getMD5(thirdSysID + txCode + data + md5Key, "UTF-8");

        Map<String,String> map = new LinkedHashMap<>();
        map.put("ThirdSysID",thirdSysID);
        map.put("TxCode",txCode);
        map.put("Data",data);
        map.put("Auth",authMD5);

        String reqLog = Thread.currentThread().getName()+","+Thread.currentThread().getId()+ ">"
                +"\tURL " +ccbUrl
                +"\n\tThirdSysID "+ thirdSysID
                +"\n\tTxCode " + txCode
                +"\n\trequestType " + requestType
                +"\n\tAuth " + authMD5
                +"\n\tData " + data;

        String result = null;

        if (requestType == 1){
            // 前端请求
            // 产生ccb请求的表单数据
            StringBuilder form = new StringBuilder();
            form.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n");
            form.append("<HTML>\n");
            form.append("   <HEAD><TITLE>sender</TITLE></HEAD>\n");
            form.append("   <BODY>\n");
            form.append("       <form name=\"submitForm\" action=\"" + ccbUrl + "\" method=\"post\">\n");
            for (Map.Entry<String, String> e : map.entrySet()) {
                form.append("       <input type=\"hidden\" name=\"" + e.getKey() + "\" value=\"" + e.getValue() + "\"/>\n");
            }
            form.append("       </from>\n");
            form.append("   <script>window.document.submitForm.submit();</script>\n");
            form.append("   </BODY>\n");
            form.append("</HTML>\n");
            result = form.toString();
        }
        if (requestType == 2){
            // 后端请求
            long stime = System.currentTimeMillis();
            String responseStr = HTTPUtil.formText(ccbUrl,"POST",map);
            long etime = System.currentTimeMillis();

            reqLog += "\n\t响应耗时: " + (etime - stime) +" 毫秒";
            reqLog +="\n\t响应结果: " +responseStr ;

            MessageStruct ms = new Gson().fromJson(responseStr,MessageStruct.class);
            if (ms!=null
                    && ms.ThirdSysID.equals(thirdSysID)
                    && ms.errorCode == null && ms.errorMsg == null
                    && ms.Data!=null ){
                result = decrypt_ccb_data(ms.Data);
            }else{
                result = responseStr;
            }
        }

//        ccb_print(reqLog);
        return result;
    }

    private static AtomicInteger transIndex = new AtomicInteger(1);
    /*
    * 由第三方系统生成，作为此次交易的唯一标识
    * ThirdSysID+ yyyyMMddHHmmss +5位顺序号
    * 一个流水号只能使用一次（含发送/返回两次跳转），重复请求无效
    * */
    private static String GenTransID(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timeStr = sdf.format(new Date());

        int index = transIndex.getAndIncrement();
        if (index > 99999) transIndex.set(1);

        return thirdSysID + timeStr + index;
    }

    static {
        ApplicationPropertiesBase.initStaticFields(CCBQueryFactory.class);
    }

    public static abstract class CCB_QUERY_STRUCT{
        private final String TxCode;
        private String TransID = GenTransID();

        public CCB_QUERY_STRUCT(String txCode) {
            TxCode = txCode;
        }

        protected abstract int RequestType();

        private String execute() throws Exception {
            return sendQuery(TxCode,RequestType(),new Gson().toJson(this));
        }
    }

    public static String CCB_REQUEST(CCB_QUERY_STRUCT query){
        try {
            return query.execute();
        } catch (Exception e) {
           e.printStackTrace();
        }
        return null;
    }

}
