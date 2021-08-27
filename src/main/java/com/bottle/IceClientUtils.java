package com.bottle;

import com.onek.server.inf.IRequest;
import com.onek.server.inf.InterfacesPrx;
import com.onek.server.inf.InterfacesPrxHelper;
import com.bottle.properties.abs.ApplicationPropertiesBase;
import com.bottle.properties.annotations.PropertiesFilePath;
import com.bottle.properties.annotations.PropertiesName;

import java.util.HashMap;

/**
 * @Author: leeping
 * @Date: 2019/4/9 14:15
 * ice客户端远程调用
 */

@PropertiesFilePath("/application.properties")
public class IceClientUtils {

    @PropertiesName("reg.tcp.tag")
    public static String tag;

    private static final HashMap<String,Ice.Communicator> map = new HashMap<>();

    static {
        ApplicationPropertiesBase.initStaticFields(IceClientUtils.class);
        String[]  arr = tag.split(";");
        for (String s : arr){
            try {
                String[] sarr = s.split(",");
                String tag = sarr[0];
                String serverAdds = sarr[1];
                Ice.Communicator ic = createICE(tag,serverAdds);
                map.put(tag,ic);
            } catch (Exception e) {
                throw new RuntimeException("无法连接: " + s);
            }

        }
    }

    private static Ice.Communicator createICE(String tag,String serverAdds) {
        StringBuilder sb = new StringBuilder("--Ice.Default.Locator="+tag+"/Locator");

        String[] infos = serverAdds.split(";");

        for (String info : infos){
            String[] host_port = info.split(":");
            sb.append(":tcp -h ").append(host_port[0]).append(" -p ").append(host_port[1]);
        }

        return Ice.Util.initialize( new String[]{sb.toString()});
    }

    public static String executeICE(String tag,String token,String serverName,String cls,String med,String[] array){
        try {
            if (tag.startsWith("预留参数")) tag = "DRUG"; // 兼容处理

            Ice.Communicator ic = map.get(tag);
            assert ic!= null;
            Ice.ObjectPrx base = ic.stringToProxy(serverName);
            InterfacesPrx curPrx =  InterfacesPrxHelper.checkedCast(base);
            curPrx.ice_invocationTimeout(30000);
            IRequest request = new IRequest();
            request.cls = cls;
            request.method = med;
            request.param.token = token;
            request.param.arrays = array;

            return curPrx.accessService(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
