package server;



import bottle.util.Log4j;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import com.bottle.properties.abs.ApplicationPropertiesBase;
import com.bottle.properties.annotations.PropertiesFilePath;
import com.bottle.properties.annotations.PropertiesName;
import servlet.imp.*;

import javax.servlet.DispatcherType;
import java.io.File;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.util.*;

import static io.undertow.servlet.Servlets.servlet;
import static server.beans.IceTrade.resumeLocalNotifications;


/**
 * @Author: leeping
 * @Date: 2019/4/16 11:54
 */
@PropertiesFilePath("/application.properties")
public class Launch {

    @PropertiesName("local.port")
    public static int port;

    @PropertiesName("local.domain")
    public static String domain;

    static {

        ApplicationPropertiesBase.initStaticFields(Launch.class);
    }

    public static String dirPath;

    public static void main(String[] args) throws Exception{
        resumeLocalNotifications();

        dirPath = new File(Launch.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent() + "/store";
        File dir = new File(dirPath);
        if (!dir.exists()){
            if (!dir.mkdirs()) throw new RuntimeException("无法创建文件夹:"+ dirPath);
        }

        DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(Launch.class.getClassLoader())
                .setContextPath("/")
                .setDeploymentName("pay-middle.war")
                .addFilter(new FilterInfo("跨域过滤", AccessControlAllowOriginFilter.class))
                .addFilterUrlMapping("跨域过滤","/*", DispatcherType.REQUEST)
                .setResourceManager(
                        new PathResourceManager(Paths.get(dirPath), 16*4069L)
                );

        servletBuilder.addServlet(servlet("支付处理", PrevPayHandler.class).addMapping("/pay"));
        servletBuilder.addServlet(servlet("支付查询", PayStatusQuery.class).addMapping("/query"));
        servletBuilder.addServlet(servlet("退款", RefundHandler.class).addMapping("/refund"));
        servletBuilder.addServlet(servlet("结果处理-异步-支付宝", PayResultCallcackAlipay.class).addMapping("/result/alipay"));
        servletBuilder.addServlet(servlet("结果处理-异步-微信", PayResultCallbackWXPay.class).addMapping("/result/wxpay"));

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();
        HttpHandler httpHandler = manager.start();
        //路径默认处理程序
        PathHandler pathHandler = Handlers.path(httpHandler);

        Undertow.Builder builder = Undertow.builder();

        builder.addHttpListener(port,"0.0.0.0",pathHandler);

       builder.build().start();

       Log4j.info("空间折叠,支付中间件,端口 = " + port + " , 域名 = "+ domain +" , 文件存储 = "+ dirPath);
    }

    public static String printMap(Map map){
        StringBuilder sb = new StringBuilder();
        if( map!= null ){
            sb.append("打印MAP hashcode = ").append(map.hashCode()).append("\n");
            Iterator it = map.entrySet().iterator();
            Map.Entry entry;
            while (it.hasNext()){
                entry = (Map.Entry) it.next();
                sb.append("\t").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
            }
            sb.deleteCharAt(sb.length()-1);
            Log4j.info(sb.deleteCharAt(sb.length()-1));
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
        return def;
    }


}
