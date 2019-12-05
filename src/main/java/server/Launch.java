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
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;

import static io.undertow.servlet.Servlets.servlet;


/**
 * @Author: leeping
 * @Date: 2019/4/16 11:54
 */
@PropertiesFilePath("/application.properties")
public class Launch {
    public static final Log log = LogFactory.getLog(Launch.class);

    @PropertiesName("local.host")
    private static String host;

    @PropertiesName(("local.port"))
    private static int port;

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

        Undertow.builder()
                .addHttpListener(port, host, pathHandler)
                .build()
                .start();

        log.info("启动空间折叠,支付中间件, address = "+ host+":"+port+" ,文件存储:"+ dirPath);
    }

    public static String printMap(Map map){
        Iterator<Map.Entry> it = map.entrySet().iterator();
        StringBuilder sb = new StringBuilder("\n");
        Map.Entry entry;
        while (it.hasNext()){
            entry = it.next();
            sb.append("\t").append(entry.getKey()+" = "+ entry.getValue()+",");
        }
        log.info(sb.deleteCharAt(sb.length()-1));
        return sb.deleteCharAt(sb.length()-1).toString();
    }
}
