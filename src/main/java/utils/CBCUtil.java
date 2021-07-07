package utils;
import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;


public class CBCUtil {

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
        Key publicKey = KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(x509KeySpec);
        return publicKey;
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
    static String clientDecrypt(String dataStr, String publicKey) throws Exception {
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
    static String getMD5(String text, String charset) throws Exception {
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


}
