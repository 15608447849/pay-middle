package ccbpay.entitys;

import java.util.List;

/**
 *  转换结果为 一块医药后台识别的数据结构
 */
public class StructConvertHandle {
    private static final class PayResult {
        private static final class OrderInfo {
            private String Order_No;
            private String ExistInCCB;
            private String Order_No_CCB;
            private String SendStatus;
            private String PayBackStatus;
            private String PayStatus;
            private String Order_Money;
            private String PayTime;
            private String UpdateTime;
            private String Remark;
            private String CLR_STATUS;
            private String CLR_TIME;
            private String Expand2;
            private String Epay_Type;
            private String Pay_Name;
            private String Pay_Card;
        }
        private String TxCode;
        private String TransID;
        private List<OrderInfo> OrderInfos;
        private String Status;
        private String Remark;
        private String Expand1;
    }

    /**
     *
     */
    public static String order_status_query_result_convert(String json){

        return null;
    }

    public static String order_pay_result_convert(String json){
        return null;
    }



}
