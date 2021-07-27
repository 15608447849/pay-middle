package ccbpay.servlets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static ccbpay.common.CCBQueryFactory.ccb_print;
import static ccbpay.common.CCBQueryFactory.decrypt_ccb_data;

/**
 * ccb 正常支付结果回执
 * */
public class PayResultReceive extends javax.servlet.http.HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String data = req.getParameter("Data");
        data = decrypt_ccb_data(data);
        ccb_print("建行支付结果返回:\n\t"+data);

        // 发送消息到 ice客户端
        resp.getWriter().println("SUCCESS");
    }
}
