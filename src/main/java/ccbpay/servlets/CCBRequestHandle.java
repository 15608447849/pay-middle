package ccbpay.servlets;



import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import static ccbpay.CCBQueryFactory.ccb_print;
import static ccbpay.req.OrderStatusQuery.queryOrderStatus;
import static ccbpay.req.SubmitOrderStartPay.submitOrder;


public class CCBRequestHandle extends javax.servlet.http.HttpServlet {

    private static String getParam(String param,String def){
        if (param!=null){
            try {
                return URLDecoder.decode(param,"UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return def;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String requestFunc = getParam(req.getParameter("requestFunc"),null);
        try {
            if (requestFunc == null) throw new IllegalAccessException("请求方法错误");
            callFunc(requestFunc,req,resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().println("请求执行错误: "+ e);
        }
    }

    private void callFunc(String requestFunc, HttpServletRequest req,HttpServletResponse resp) throws Exception{
        Method method = this.getClass().getDeclaredMethod(requestFunc,HttpServletRequest.class);
        method.setAccessible(true);
        String result = String.valueOf(method.invoke(this,req));
        ccb_print("处理前端请求: " + requestFunc+" 响应结果: \n"+result);
        resp.getWriter().println(result);
    }

    private String pay(HttpServletRequest req) throws Exception{
        // 支付
        String orderID = getParam(req.getParameter("orderID"),null);
        String money = getParam(req.getParameter("money"),null);
        String companyID = getParam(req.getParameter("companyID"),null);
        String userName = getParam(req.getParameter("userName"),"/");
        String phone = getParam(req.getParameter("phone"),"0");
        String address = getParam(req.getParameter("address"),"/");
        String payType = getParam(req.getParameter("payType"),"3");

        if (orderID == null || money == null || companyID == null){
            return "支付请求参数错误";
        }
        return submitOrder(Integer.parseInt(payType),companyID,userName,phone,address,orderID,money);
    }

    private String order_status(HttpServletRequest req) throws Exception{
        // 订单状态查询
        String orderID = getParam(req.getParameter("orderID"),null);
        if (orderID == null){
            return "查询订单请求参数错误";
        }
        return queryOrderStatus(orderID);
    }

    private String refund(HttpServletRequest req) throws Exception{
        //退款
        String orderID = getParam(req.getParameter("orderID"),null);
//        String orderID = getParam(req.getParameter("orderID"),null);
        return null;


    }


}
