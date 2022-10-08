package server.common;

import bottle.util.Log4j;

import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Map;

public class CommFunc {
    public static String getMapStr(Map<?,?> map){
        StringBuilder sb = new StringBuilder();
        if( map == null || map.isEmpty() ){
            sb.append("MAP is null or is empty");
        }else{
            sb.append("MAP hashcode = ").append(map.hashCode()).append("\n");
            Iterator<? extends Map.Entry<?, ?>> it = map.entrySet().iterator();
            Map.Entry<?,?> entry;
            while (it.hasNext()){
                entry = it.next();
                sb.append("\t").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
            }
            sb.deleteCharAt(sb.length()-1);
        }
        return sb.toString();
    }

    public static String getURLDecoderParameter(String parameter,String def){
        try {
            if (parameter != null){
                return URLDecoder.decode(parameter,"UTF-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (def == null) throw new IllegalArgumentException("请求参数("+parameter+")不存在");
        return def;
    }

    public static String getURLDecoderParameterThrowEx(HttpServletRequest req, String key, String def){
        try {
            String parameter = req.getParameter(key);
            if (parameter != null){
                return URLDecoder.decode(parameter,"UTF-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (def == null) throw new IllegalArgumentException("请求参数("+key+")不存在");
        return def;
    }
    public static String getURLDecoderParameterThrowEx(HttpServletRequest req, String key){
        return getURLDecoderParameterThrowEx(req,key,null);
    }
}
