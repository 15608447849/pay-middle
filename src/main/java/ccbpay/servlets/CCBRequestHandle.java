package ccbpay.servlets;



import ccbpay.entitys.OrderRefund;
import server.beans.IceResult;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;

import static ccbpay.common.CCBQueryFactory.CCB_REQUEST;
import static ccbpay.common.CCBQueryFactory.ccb_print;
import static ccbpay.entitys.OrderStatusQuery.queryOrderStatus;
import static ccbpay.entitys.OrderSubmitPay.submitOrder;
import static ccbpay.entitys.StructConvertHandle.order_status_query_result_convert;


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

    /** 支付请求 */
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

        String expand1 = "@" + companyID;

        return submitOrder(Integer.parseInt(payType),companyID,userName,phone,address,orderID,money, expand1);
    }

    /** 状态查询 */
    private String order_status(HttpServletRequest req) throws Exception{
        // 订单状态查询
        String orderID = getParam(req.getParameter("orderID"),null);
        if (orderID == null){
            return "查询订单请求参数错误";
        }
        String json_ccb = queryOrderStatus(orderID);
        System.out.println("CCB 查询订单: " + orderID +" , 结果JSON: "+ json_ccb);
        IceResult r = order_status_query_result_convert(json_ccb);

        return String.valueOf(r);
    }

    /** 退款请求 */
    private String refund(HttpServletRequest req) throws Exception{
        //退款
        String Order_No_CCB = getParam(req.getParameter("Order_No_CCB"),null);
        if (Order_No_CCB == null){
            return "查询订单请求参数错误";
        }
        String sellerUserID_thirdSys = getParam(req.getParameter("companyID"),null);
        String payBackID = getParam(req.getParameter("refundNo"),null);
        String payBackMoney = getParam(req.getParameter("money"),null);

        OrderRefund orderRefund = new OrderRefund(Order_No_CCB, sellerUserID_thirdSys, payBackID, payBackMoney);

        String json_ccb = CCB_REQUEST(orderRefund);
        System.out.println("CCB 退款: " + Order_No_CCB +" , 结果JSON: "+ json_ccb);

        return String.valueOf(json_ccb);
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

}
