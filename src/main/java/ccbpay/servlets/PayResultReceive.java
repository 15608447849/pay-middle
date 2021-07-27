package ccbpay.servlets;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static ccbpay.CCBQueryFactory.ccb_print;
import static ccbpay.CCBQueryFactory.decrypt_ccb_data;


public class PayResultReceive extends javax.servlet.http.HttpServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

//        StringBuilder sb = new StringBuilder();
//        try(BufferedReader reader =  new BufferedReader(new InputStreamReader(req.getInputStream()))){
//            sb.append(reader.readLine());
//        }

        String data = req.getParameter("Data");
        data = decrypt_ccb_data(data);
        ccb_print("建行支付结果返回:\n\t"+data);

        // 发送消息到 ice客户端
        resp.getWriter().println("SUCCESS");
    }
}
