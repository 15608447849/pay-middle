package server.yeepay;

import bottle.util.Log4j;
import server.yeepay.payimps.YeepayImp;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 易宝
 * 支付结果通知处理
 * */
public class YeepayCallbackServlet extends javax.servlet.http.HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String md5 = req.getRequestURI().replace(req.getServletPath(),"/").replace("/","");

            String attr = YeepayApiFunction.getLocalYeepayAttr(md5);;
            if (attr == null) {
                resp.setStatus(200);
                resp.getWriter().print("SUCCESS");
                return;
            }

            Log4j.info("易宝支付结果通知: "+  req.getRequestURI());
            String json = YeepayApiFunction.payResultCallback(req);
            boolean flag = YeepayImp.payResultNotify(md5,json);
            Log4j.info("返回易宝通知响应 : "+  ( flag ? "SUCCESS" : "FAIL" ));

            resp.setStatus(flag ? 200 :  500);
            resp.getWriter().print(flag ? "SUCCESS" : "FAIL");

        } catch (Exception e) {
            resp.getWriter().println("ERROR: "+ e);
        }
    }

}
