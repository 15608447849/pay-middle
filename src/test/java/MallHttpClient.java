
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipInputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;



import java.net.URLEncoder;

public class MallHttpClient {
	private String ThirdSysID;
	private String TxCode;
	private String Data;
	private String Auth;
	private String RequestType;

	private void init() throws Exception {
		// SFT10008
		String payOrder = "{\"TxCode\":\"MALL10023\",\"TransID\":\"23zq6210100148579886\",\"SellerUserID_ThirdSys\":\"mengtian888\",\"Order_No_CCB\":\"5016102411920017\",\"PayBackReason\":\"1\",\"GetProduct\":\"1\",\"Introduce\":\"fjdhfhjkdhf\",\"Expand1\":\"确认退款1\"}";

		String publicKeyStr = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCJxVpdRbOcH7xnetiskp7fakeQs4FmuBu9Nwop wcf1m+DQYaqGZZ8YUtVGY2O5aMHwo5gsa7kprDWlre8grA66EN2cyaKgllH0E3UDIPyCkpDSBXNs VFKfiyjngJMFZi5ubV1xIYO8l8YKYvajtQUnkzpaF3ed6XGgDXUnEjvzfwIDAQAB";
		String enStr = RSATool.clientEncrypt(payOrder, publicKeyStr);
		//String Data = null;
		Data = URLEncoder.encode(enStr, "utf-8");
		String MD5Key = "TestMD5Key";

		ThirdSysID = "1015";
		TxCode = "MALL10023";
		RequestType = "1";
		Auth = MD5.getMD5(ThirdSysID + TxCode + RequestType + Data + MD5Key, "utf-8");
		//MD5.getMD5("1015" + "MALL10023" +"1"+ Data + "TestMD5Key", "UTF-8")
	}

	public static void main(String[] args) {
		String uri = "http://emall.ccb.com:8880/ecp/thirdPartAPI";
		MallHttpClient instance = new MallHttpClient();
		HttpClient client = new HttpClient();
		PostMethod method = new PostMethod(uri);
		try {
			instance.init();

			method.setRequestBody(new NameValuePair[] { new NameValuePair("ThirdSysID", instance.ThirdSysID),
					new NameValuePair("TxCode", instance.TxCode), new NameValuePair("Data", instance.Data),
					new NameValuePair("Auth", instance.Auth) });
			method.releaseConnection();

			int statuscode = client.executeMethod(method);

			if (statuscode == 200) {
				byte[] bodybyt = method.getResponseBody();
				System.out.println(new String(bodybyt, "utf-8"));
				byte[] rs = instance.unZip(bodybyt);

				if (rs != null) {
					System.out.println(new String(rs, "GBK"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private byte[] unZip(byte[] data) {
		ByteArrayInputStream bis = null;
		ByteArrayOutputStream bos = null;
		ZipInputStream zip = null;
		byte[] b = null;
		try {
			bis = new ByteArrayInputStream(data);
			bos = new ByteArrayOutputStream();
			zip = new ZipInputStream(bis);
			while (zip.getNextEntry() != null) {
				int r = -1;
				byte[] buf = new byte[1024];

				while ((r = zip.read(buf, 0, buf.length)) != -1) {
					bos.write(buf, 0, r);
				}
				b = bos.toByteArray();
				bos.flush();
				bos.close();
			}
			zip.close();
			bis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return b;
	}

}
