package utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HTTPUtil {

    public static String formText(String url, String type, Map<String,String> params){
        String text = null;
        HttpURLConnection con = null;
        try{

            StringBuilder sb = new StringBuilder();
            String content = null;

            if (params!=null){
                for (Map.Entry<String, String> e : params.entrySet()) {
                    sb.append(e.getKey());
                    sb.append("=");
                    sb.append(URLEncoder.encode(e.getValue(),"UTF-8"));
                    sb.append("&");
                }
                sb.substring(0, sb.length() - 1);

                content = sb.toString();
                if (type .equals("GET") && params.size()>0){
                    url += "?" +content ;
                }
            }

            con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod(type);
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setUseCaches(false);
            con.setRequestProperty("Charset", "UTF-8");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            if (content!=null){
                if (type.equals("POST")){
                    OutputStreamWriter osw = new OutputStreamWriter(con.getOutputStream(), StandardCharsets.UTF_8);
                    osw.write(content);
                    osw.flush();
                    osw.close();
                }
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
            sb.delete(0,sb.length());
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            text = sb.toString();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (con!=null)  con.disconnect();
        }
        return text;
    }

    public static String contentToHttpBody(String url, String type, String json){
        String text = null;
        HttpURLConnection con = null;
        System.out.println("URL: "+ url+" , type="+type+"\n\tJSON>> "+json);
        try{
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod(type);
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setUseCaches(false);
            con.setRequestProperty("Charset", "UTF-8");
            con.setRequestProperty("Content-Type", "application/json");

            if (json!=null){
                OutputStreamWriter osw = new OutputStreamWriter(con.getOutputStream(), StandardCharsets.UTF_8);
                osw.write(json);
                osw.flush();
                osw.close();
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            text = sb.toString();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (con!=null)  con.disconnect();
        }
        return text;
    }

}
