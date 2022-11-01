package server.yeepay;

import bottle.tuples.Tuple2;
import bottle.util.Log4j;
import bottle.util.StringUtil;
import server.beans.IceResult;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import static server.common.CommFunc.getURLDecoderParameterThrowEx;
import static server.yeepay.YeepayApiFunction.createPayOrderMobileApp;

/**
 * 易宝预支付接口
 * 微信小程序预支付 使用
 * */
public class YeepayMinProcPrevPayServlet extends javax.servlet.http.HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
       doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        IceResult result = new IceResult();

        try {
            String contentType = req.getHeader("content-type");
            if (StringUtil.isEmpty(contentType) || !contentType.equals("application/x-www-form-urlencoded")) throw new IllegalArgumentException("仅接收请求类型:application/x-www-form-urlencoded");

            String orderNo = getURLDecoderParameterThrowEx(req,"orderNo"); // 订号单或订单流水号
            String attr = getURLDecoderParameterThrowEx(req,"attr"); // 附加信息: 时间戳,服务标识,服务名,回调类,回调方法,公司码
            String price = getURLDecoderParameterThrowEx(req,"price"); // 支付金额
            String subject = getURLDecoderParameterThrowEx(req,"subject"); // 标题
            String payPlatformCode = getURLDecoderParameterThrowEx(req,"payPlatformCode",""); // 支付者的三方平台标识编码

            Tuple2<String,String> tuple = createPayOrderMobileApp("WECHAT","MINI_PROGRAM",payPlatformCode,attr,orderNo,new BigDecimal(price),subject,new Date(System.currentTimeMillis() + 10 * 1000 * 60L));
            String prePayTn = tuple.getValue0();
            String errmsg = tuple.getValue1();
            if (prePayTn == null) throw new IllegalStateException("易宝支平台微信预支付错误"+ ( errmsg!=null ? ","+ errmsg : ".") );
            result.set(200,prePayTn);

        } catch (Exception e) {
            Log4j.error(e);
            result.set(-1,e.getMessage());
        }
        Log4j.info("********************小程序返回结果: "+ result.toJson());
        resp.getWriter().println(result.toJson());
    }
}
