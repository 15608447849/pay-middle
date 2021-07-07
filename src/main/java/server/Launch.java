package server;


import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;
import servlet.imp.*;

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

    @PropertiesName(("local.domain"))
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
                .setResourceManager(
                        new PathResourceManager(Paths.get(dirPath), 16*4069L)
                );

        servletBuilder.addServlet(servlet("支付处理", PayHandler.class).addMapping("/pay"));
        servletBuilder.addServlet(servlet("支付查询", QueryPay.class).addMapping("/query"));
        servletBuilder.addServlet(servlet("退款", RefundHandler.class).addMapping("/refund"));
        servletBuilder.addServlet(servlet("支付宝-结果处理-异步", AlipayResult.class).addMapping("/result/alipay"));
        servletBuilder.addServlet(servlet("微信-结果处理-异步", WxpayResult.class).addMapping("/result/wxpay"));

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();
        HttpHandler httpHandler = manager.start();
        //路径默认处理程序
        PathHandler pathHandler = Handlers.path(httpHandler);
        List<String> ipList = getLocalIPList();
        if (ipList.isEmpty()) throw new RuntimeException("没有可用的IP地址");

        Undertow.Builder builder = Undertow.builder();

        for (String ip : ipList){
            builder.addHttpListener(port,ip,pathHandler);
        }

       builder.build().start();

        log.info("启动空间折叠,支付中间件, 域名 = "+ domain +" ,文件存储:"+ dirPath);
    }

    //获取本机所有IP地址
    private static List<String> getLocalIPList() {
        List<String> ipList = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            NetworkInterface networkInterface;
            Enumeration<InetAddress> inetAddresses;
            InetAddress inetAddress;
            String ip;
            while (networkInterfaces.hasMoreElements()) {
                networkInterface = networkInterfaces.nextElement();
                inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    inetAddress = inetAddresses.nextElement();
                    if (inetAddress instanceof Inet4Address) { // IPV4
                        ip = inetAddress.getHostAddress();
                        ipList.add(ip);
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return ipList;
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
