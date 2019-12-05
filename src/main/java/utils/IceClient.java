package utils;

import com.onek.server.inf.IRequest;
import com.onek.server.inf.InterfacesPrx;
import com.onek.server.inf.InterfacesPrxHelper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

/**
 * @Author: leeping
 * @Date: 2019/4/9 14:15
 * ice客户端远程调用
 */
public class IceClient {

    private  Ice.Communicator ic = null;

    private final String[] args ;

    private int timeout = 30000;

    public IceClient(String tag,String serverAdds) {
        StringBuffer sb = new StringBuffer("--Ice.Default.Locator="+tag+"/Locator");
        String str = ":tcp -h %s -p %s";
        String[] infos = serverAdds.split(";");
        for (String info : infos){
            String[] host_port = info.split(":");
            sb.append(String.format(Locale.CHINA,str, host_port[0],host_port[1]));
        }
        args = new String[]{sb.toString()};
    }

    public IceClient setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    synchronized
    public IceClient startCommunication() {
        if (ic == null) {
            ic = Ice.Util.initialize(args);
        }
        return this;
    }

    synchronized
    public IceClient stopCommunication() {
        if (ic != null) {
            ic.destroy();
        }
        return this;
    }

    public InterfacesPrx curPrx;

    public IceClient settingProxy(String serverName){
        Ice.ObjectPrx base = ic.stringToProxy(serverName);
        curPrx =  InterfacesPrxHelper.checkedCast(base);
        curPrx.ice_invocationTimeout(timeout);
        return this;
    }

    private IRequest request;

    public IceClient settingReq(String token,String cls,String med){
        request = new IRequest();
        request.cls = cls;
        request.method = med;
        request.param.token = token;
        return this;
    }

    public IceClient settingParam(String[] array){
        request.param.arrays = array;
        return this;
    }

    public String executeSync(){
        if (curPrx!=null && request!=null){
            String res =  curPrx.accessService(request);
            curPrx = null;
            request = null;
            return res;
        }
        return null;
    }

}
