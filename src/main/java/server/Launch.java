package server;



import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
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
import server.common.AccessControlAllowOriginFilter;
import server.yeepay.YeepayCallbackServlet;
import server.yeepay.YeepayMinProcPrevPayServlet;
import servlet.imp.*;

import javax.servlet.DispatcherType;
import java.io.File;
import java.nio.file.Paths;
import java.util.Map;

import static io.undertow.servlet.Servlets.servlet;
import static server.beans.IceTrade.resumeLocalNotifications;
import static server.yeepay.YeepayApiFunction.PAY_CALLBACK_URL_BODY;


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

        servletBuilder.addServlet(servlet("预支付", PrevPayHandler.class).addMapping("/pay"));
        servletBuilder.addServlet(servlet("支付查询", PayStatusQuery.class).addMapping("/query"));
        servletBuilder.addServlet(servlet("退款处理", RefundHandler.class).addMapping("/refund"));
        servletBuilder.addServlet(servlet("支付结果-支付宝", PayResultCallbackAliPay.class).addMapping("/result/alipay"));
        servletBuilder.addServlet(servlet("支付结果-微信", PayResultCallbackWXPay.class).addMapping("/result/wxpay"));

        servletBuilder.addServlet(servlet("易宝小程序预支付", YeepayMinProcPrevPayServlet.class).addMapping("/yeepay/minProcPay"));
        servletBuilder.addServlet(servlet("易宝支付结果通知", YeepayCallbackServlet.class).addMapping(PAY_CALLBACK_URL_BODY+"*"));

       DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
       manager.deploy();

       HttpHandler httpHandler = manager.start();
        //路径默认处理程序
       PathHandler pathHandler = Handlers.path(httpHandler);

       Undertow.Builder builder = Undertow.builder();

       builder.addHttpListener(port,"0.0.0.0",pathHandler);

       builder.build().start();

       Log4j.info("空间折叠,支付中间件,端口 = " + port + " , 域名 = "+ domain +" , 文件存储 = "+ dirPath);

        String json = "{\"code\":1,\"data\":{\"wx_appid\":\"wx8d45b8ae300bb465\",\"wx_orgid\":\"gh_89d0b4f95a06\",\"orderNo\":\"2209150013770034009\",\"attr\":\"1663225395693@DRUG@order2Server0@PayModule@payCallBack@536894204\",\"price\":\"118.0\",\"subject\":\"一块医药\"}}";

    }


}
