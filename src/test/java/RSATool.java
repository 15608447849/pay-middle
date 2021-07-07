import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

import org.apache.commons.codec.binary.Base64;

public class RSATool {
	
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
	public static String clientEncrypt(String dataStr, String publicKeyStr) throws Exception {
		System.out.println("客户端公钥分段加密开始");
		ByteArrayOutputStream out = null;
		String encodedDataStr = null;
		try {
			out = new ByteArrayOutputStream();
			byte[] data = dataStr.getBytes("utf-8");
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
			encodedDataStr = new String(encryptBASE64(encryptedData));
			System.out.println("客户端公钥分段加密完毕");
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				out.close();
			} catch (Exception e2) {
				// TODO: handle exception
			}
		}
		return encodedDataStr;
	}

	
	/**
	 * Method: clientDecrypt <br/>
	 * description: 公钥分段解密 <br/>
	 * 
	 * @param content
	 *            解密内容，密文
	 * @param publicKey
	 *            公钥
	 * @return 明文
	 * @throws Exception
	 */
	public static String clientDecrypt(String dataStr, String publicKey) throws Exception {
		System.out.println("客户端公钥分段解密处理开始");
		
		ByteArrayOutputStream out = null;
		String decodedDataStr = null;
		try {
			out = new ByteArrayOutputStream();
			
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
			decodedDataStr = new String(decryptedData, "utf-8");
			System.out.println("客户端公钥分段加密完毕");
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				out.close();
			} catch (Exception e2) {
				// TODO: handle exception
			}
		}
		return decodedDataStr;
	}
	
	public static void main(String[] args) throws Exception {
		//公钥 联系建行获取
		String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCJxVpdRbOcH7xnetiskp7fakeQs4FmuBu9Nwop wcf1m+DQYaqGZZ8YUtVGY2O5aMHwo5gsa7kprDWlre8grA66EN2cyaKgllH0E3UDIPyCkpDSBXNs VFKfiyjngJMFZi5ubV1xIYO8l8YKYvajtQUnkzpaF3ed6XGgDXUnEjvzfwIDAQAB";
		//加密
		//1、生成请求报文
		String decodeStr = "{\"TxCode\":\"SFT10001\",\"TransID\":\"S201610221030056328\",\"GoPayType\":3,\"OrderIniter\":0,\"BuyerUserID_ThirdSys\":\"048000015\",\"BuyerUserName_ThirdSys\":\"12345\",\"SellerUserID_ThirdSys\":\"YXNM.999\",\"BuyerUserType_ThirdSys\":1,\"BuyerTrueName_ThirdSys\":\"000011184\",\"BuyerCompany_ThirdSys\":\"000011184\",\"BuyerAddress_ThirdSys\":\"完美公司\",\"BuyerPhoneNum_ThirdSys\":\"13827897383\",\"BuyerCertType_ThirdSys\":\"17\",\"BuyerCertValue_ThirdSys\":\"123456789012345678\",\"OrderInfos\":[{\"Order_No\":\"QY20151123023\",\"HaveProducts\":1,\"Order_Products\":[{\"ProductID\":1231,\"ProductTitle\":\"衣服\",\"ProductCode\":\"C001\",\"ProductModel\":\"M001\",\"ProductPrice\":32.3,\"ProductAmount\":30,\"ProductUnit\":\"件\",\"ProductDesc\":\"红色\"},{\"ProductID\":1232,\"ProductTitle\":\"裤子\",\"ProductCode\":\"C002\",\"ProductModel\":\"M002\",\"ProductPrice\":32.3,\"ProductAmount\":30,\"ProductUnit\":\"件\",\"ProductDesc\":\"黑色快点送货\"}],\"Order_Money\":222.33,\"Order_Time\":20151120125001,\"Order_Title\":\"形象使用费\",\"Order_BuyerPhone\":\"13800138000\", \"Expand2\":\"订单1扩展信息\"},{\"Order_No\":\"QY20161120034\",\"HaveProducts\":1,\"Order_Products\":[{\"ProductID\":1233,\"ProductTitle\":\"鞋子\",\"ProductCode\":\"C003\",\"ProductModel\":\"M003\",\"ProductPrice\":32.3,\"ProductAmount\":30,\"ProductUnit\":\"双\",\"ProductDesc\":\"板鞋\"},{\"ProductID\":1234,\"ProductTitle\":\"帽子\",\"ProductCode\":\"C004\",\"ProductModel\":\"M004\",\"ProductPrice\":32.3,\"ProductAmount\":30,\"ProductUnit\":\"顶\",\"ProductDesc\":\"鸭舌帽\"}],\"Order_Money\":0.01,\"Order_Time\":\"20151120125102\",\"Order_Title\":\"商品保证金\",\"Order_BuyerPhone\":\"13800138000\", \"Expand2\":\"订单2备注信息\"}],\"Expand1\":\"扩展信息2\"}";
		//2、加密
		String encodeStr = RSATool.clientEncrypt(decodeStr, publicKey);
		//3、字符转义
		encodeStr = URLEncoder.encode(encodeStr, "UTF-8");
		System.out.println("加密信息 encodeStr：" + encodeStr);
		
		//解密
		//1、接收建行返回的报文密文
		encodeStr = "YYgeM4OKuTmSbP84wCDWszN9jPhPZ3Ob5zoAU0kd4fh3CzvDznhMFcFy9Zln3Gyd8C%2FwPRMhKNV%2BGnyNd9AoFZN3YPkThXROT9paKzVK%2BjyKa%2BwX%2B3F20Sj9b3aLRHhVUdCdutFbYqr1r%2F98XJUID9MQbYpMo5RxHc2R6sGYIFZvK8HRMEJ53s7BzR6%2BBd%2BvFUEVvFS8YGQqvyjPr9HdKOHIvC7ELQkQu2lAU0plbzxnoYWMBQoYPiBrNkZP87n4h6PgqfzvPxClv7fVyBYirRUrrpXhEYl4FOm0vLdS5MlQGCj5FSofJN1xnWCMgB0hcSbkB4q6tfHNjdKeWtxxhykcTP%2B3sK47T8LAPQphr0zZfIpDospYO%2Bb17B9UHLYINu8C2mdtksZsWef4WgcAhcscLvBauXnw%2BiDfEycJynUX57jzFC2ESWoYSxAvwBPVLCyGxytvfKC0Z3mi458PbJrIHtINZ23%2F3PoQBrhGHMkdhFG763ERO7awmF8qaQduIraH1VlfBVTM2ETRcvgtALwjGdSi8gUrU%2BpcP%2ByIwzeihG5mJ%2F3kEgGjsCDZJpA9aDQTxhcQ9k4n4g%2FkG%2BLhGIP9BAKZtWmxL67ynKVF0mDnG%2BubmRxvGf8wS3dwXQ4Wy8ReDMhnXXvIlSFvGON5TQiFAmDFEwW6jf8pk%2F1yWZlBGUFqj8RLuBIXI5IgdDurt3dp1PTlDqIHCP%2BeT9MzxB3cfC4w6XeAwxTQo%2BdoE3xQrhBHVjHOyz11QwkN3nczgAgh3oh4atJf%2BxufBPbNB6QOwlzyuMV4svfUwGEJMfn69dyYkfHADIO3UpLOxK4uvAF1v6%2FxIahYAy0dBbj8lgkgBLmac266g9CsuEEk%2BZSiFwMSkL9twLcmrtOH2mr%2FAOGqRGA0GhaiASewaO8ysNetd1C9XuG28Wbl3yLHLvhFwTih9SkoZ7BOCyasE0rZT5OZiZkYfQVr1nhu5SBWDk8KLMfGV1Xpowcgs75EBlsEVvukJtciBNaTHBbYsEHkXaHv9RO5PfMVMvAhgyNGyggG7LzvG0ZI0Ajylw9HbwQiJfImPYw1EKWt5iBKbgBRu9SZ6fNsMqMCCmsc1ji4f2h8gBfbFd%2B%2F%2BqWghLYiOXxX6%2BbOTL48fpBG%2FJycaFZoci41E%2FJXRXg6MnNVaa6sO4HzKRcLpW1KjTjoTX0vSakBXANtlV2pWkkDoYLmvZjHkZ2rIW8h7BHo%2B3FDrcxRMvHQZLC%2BW18ys%2BXaV%2FugfO2GzNtWr31%2BOrPejp5fHNyNxW22jb668ygrC9StOnTxaJO57JFxEO%2Fu0NYHRdWPnhlVdevhKDbpTwyOK2TusuTLvT45AqjnSacP0w%2FYUsK2Yg38ybS7CLXps1%2BDkZfdQ%2Fqa4X8LElKaZrASX760XYtyg4QIP9MdRUB3gWJmmPT4cTclK4Bvfgxq0DfsjddWFapZ7dnjiMgIZbfvS7ONcZwl0RvwA4oCygfoVUcfGNEYKGoae8erFtdcDvt8NHABCGwQBx%2Bh%2FuU0bGR77pCGq0GFbMQmjxAfto4rtCPXTFjTll%2FNEVPq5l%2BNyYeefkI6FFNkgbZdfw1mn0JVBipc5QPbeQ65dyISdldnGoWUv7brtna73aTRJ5Z8wzH7zF097sUmOFYicM9eZcBEQ1MmQabGBEfOXTIFNTz03LEBo5anO1f9J%2BDClmQxE9rGfHdrliJrC%2FedjiE2zOm9G255QvRpXsIzvESWfh3cu43Hx7mjNGJLv07LVryldppYNOAsM01agJpGLsnp4MvqheGa%2FC%2FymMVigSefdg%2BhMLzcGZvtYAI0Rv9ahvDGWSGxfPv3NHa8gE7VC%2BZVvZgRIQ0Ypyqxda3B7vXtFp0x2IgtKT%2ByFlVEWaDnJuyR6in5NknVcv0svLO4%2FrhtvclGOsyZBdXhu6FTMmvjdxZWPdlkLowJH5QYP5g3zHaX5Ec%2FWZe3yMwEDPOcYpAL3gWU67wUn9a2MueYtSCtxM9xXEKo6WIyAP%2BmXPLbq99YgHR6gqm2gOBgmSg93wrYdCbHHbU%2BI7heMSjWsVC1SsWrChRYGTnAI8j6xJDHjzAnDuxtRFwvF2auaWpFVQ1FmmzpGvQD8jj2MJbE8Ow1HVMVktiAywA5IAhkMSrLSy%2BKgnmJYJjmC4V8SXtqRzGTPjntII5bp7f2Ue1g07p8PKBaKTJ2ANgpBVZVoCGFobmyuzgWuLBuxrF%2BxCt9YI55Q%2FIPGP1R1YWIzsp%2BKv6m6VxgOQV5fDIbdavkTt16WRUlTFSfCotKLHLbgWLkDcOgKmIfyWAl1P7t0rKf11aosjJAc716J4X3g8BfcbcAjH4uTcbqfJvTe%2BX3xqW1RbSJIBWp2%2B2BzJZyYz2JQTjBaOLptBjUHtuBbw%3D%3D";
		//2、字符转义
		encodeStr = URLDecoder.decode(encodeStr, "UTF-8");
		//3、解密
		decodeStr = RSATool.clientDecrypt(encodeStr, publicKey);
		System.out.println("解密信息 decodeStr ：" + decodeStr);

		
	}

}
