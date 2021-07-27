package ccbpay.servlets;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static ccbpay.common.CCBQueryFactory.ccb_print;

/**
 * ccb收银台支付异常时,点击 '返回商场' 访问地址
 *
 * */
public class PayAbnormalReceive extends javax.servlet.http.HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ccb_print("支付异常");
        resp.getWriter().println("支付异常");
    }
}
