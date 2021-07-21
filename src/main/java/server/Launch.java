package server;


import ccbpay.servlets.CCBRequestHandle;
import ccbpay.servlets.PayAbnormalReceive;
import ccbpay.servlets.PayResultReceive;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;
import servlet.imp.*;

import javax.servlet.DispatcherType;
import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Paths;
import java.util.*;

import static io.undertow.servlet.Servlets.servlet;


/**
 * @Author: leeping
 * @Date: 2019/4/16 11:54
 */
@PropertiesFilePath("/application.properties")
public class Launch {

    public static final Log log = LogFactory.getLog(Launch.class);

    @PropertiesName("local.port")
    public static int port;

    @PropertiesName("local.domain")
    public static String domain;

    static {
        ApplicationPropertiesBase.initStaticFields(Launch.class);
    }

    public static String dirPath;

    public static void main(String[] args) throws Exception{

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

        servletBuilder.addServlet(servlet("支付处理", PayHandler.class).addMapping("/pay"));
        servletBuilder.addServlet(servlet("支付查询", QueryPay.class).addMapping("/query"));
        servletBuilder.addServlet(servlet("退款", RefundHandler.class).addMapping("/refund"));
        servletBuilder.addServlet(servlet("支付宝-结果处理-异步", AlipayResult.class).addMapping("/result/alipay"));
        servletBuilder.addServlet(servlet("微信-结果处理-异步", WxpayResult.class).addMapping("/result/wxpay"));

        servletBuilder.addServlet(servlet("CCB-处理请求", CCBRequestHandle.class).addMapping("/ccb/request"));
        servletBuilder.addServlet(servlet("CCB-支付结果接收", PayResultReceive.class).addMapping("/result/ccbpay"));
        servletBuilder.addServlet(servlet("CCB-支付异常接收", PayAbnormalReceive.class).addMapping("/result/ccbpay/abnormal"));

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();
        HttpHandler httpHandler = manager.start();
        //路径默认处理程序
        PathHandler pathHandler = Handlers.path(httpHandler);

        Undertow.Builder builder = Undertow.builder();

        builder.addHttpListener(port,"0.0.0.0",pathHandler);

       builder.build().start();

        log.info("空间折叠,支付中间件,端口 = " + port + " , 域名 = "+ domain +" , 文件存储 = "+ dirPath);
    }

    public static String printMap(Map map){
        StringBuilder sb = new StringBuilder("\n");
        if( map!= null ){
            Iterator it = map.entrySet().iterator();
            Map.Entry entry;
            while (it.hasNext()){
                entry = (Map.Entry) it.next();
                sb.append("\t").append(entry.getKey()).append(" = ").append(entry.getValue()).append(",");
            }
            sb.deleteCharAt(sb.length()-1);
            log.info(sb.deleteCharAt(sb.length()-1));
        }
        return sb.toString();
    }
}
