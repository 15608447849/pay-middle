package ccbpay.servlets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import static ccbpay.CCBQueryFactory.ccb_print;
import static ccbpay.CCBQueryFactory.decrypt_ccb_data;


public class PayResultReceive extends javax.servlet.http.HttpServlet {

    // {"TxCode":"MALL20001","TransID":"YKQYY001202107081632253","OrderInfos":[{"Order_No":"2000000000006","Order_No_CCB":"5021070816172007","Order_Money":".01","PayStatus":"1","PayTime":"20210708163806","Remark":"","Expand2":null,"Epay_Type":"06","Pay_Name":"李**","Pay_Card":"6217************4769"}],"Expand1":null}
    private static class CCB_PAY_STATE{

    }

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

        resp.getWriter().println("支付成功");
    }
}
