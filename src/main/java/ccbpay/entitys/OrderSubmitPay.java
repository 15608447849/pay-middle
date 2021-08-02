package ccbpay.entitys;

/*
 * leezp 20210707
 * 提交并支付订单 MALL10001 / MALL10004
 * 订单信息提交至善融企业商城保存并支付，完成后善融将支付结果通知到第三方
 * 仅支持前端页面请求(RequestType =1)
 * 无接口数据返回
 * 必填字段:
 *   TransID 流水号
 *   GoPayType 1-不发起支付,仅保存订单 2-保存订单后,自动发送收款短信 3-保存订单后跳转商城账户支付 4-保存订单后跳转移动版支付页面
 *   OrderIniter 0-作为买方发起 1-作为卖方发起
 *   BuyerUserID_ThirdSys 买家在第三方系统中的用户ID信息
 *   BuyerUserName_ThirdSys 买家在第三方系统中的用户名
 *   SellerUserID_ThirdSys  订单卖方在第三方系统中的ID
 *   BuyerUserType_ThirdSys  0-个人会员 1-企业会员
 *   BuyerTrueName_ThirdSys  买方的真实姓名
 *   BuyerCompany_ThirdSys 买方的公司名, 个人，该字段与BuyerTrueName_ThirdSys保持一致
 *   BuyerPhoneNum_ThirdSys 买方的手机号码
 *   BuyerAddress_ThirdSys 买方在第三方系统中的地址
 *   OrderInfos 订单信息-列表
 *       Order_No 第三方系统的订单号
 *       Order_Money 订单支付金额
 *       Order_Time 订单创建时间 (yyyyMMddHHmmss)
 *       Order_Tile 订单的标题
 *       Order_BuyerPhone 订单买家的手机号码
 *       ReceiverTrueName_ThirdSys 收货方的真实姓名
 *       ReceiverCompany_ThirdSys 收货方的公司名
 *       ReceiverAddress_ThirdSys 收货方的收货地址
 *       HaveProducts 是否有订单详情信息 , 1-有 2-没有 此字段必填1
 *       Order_Products 商品详情-列表
 *           ProductID 商品ID
 *           ProductTitle 商品标题
 *           ProductCode 商品编码
 *           ProductModel 商品型号
 *           ProductPrice 商品价格
 *           ProductAmount 商品数量
 *           ProductUnit 商品单位
 * */

import ccbpay.common.CCBQueryFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static ccbpay.common.CCBQueryFactory.CCB_REQUEST;


public class OrderSubmitPay extends CCBQueryFactory.CCB_QUERY_STRUCT {


    private static final class OrderInfo{
        private static class Order_Product{
            private String ProductID;
            private String ProductTitle;
            private String ProductCode;
            private String ProductModel="/";
            private String ProductPrice;
            private int ProductAmount;
            private String ProductUnit = "/";

            private Order_Product(String productID, String productTitle, String productPrice, int productAmount) {
                ProductID = productID;
                ProductCode=productID;
                ProductTitle = productTitle;
                ProductPrice = productPrice;
                ProductAmount = productAmount;
            }
        }

        private String Order_No;
        private String Order_Money;
        private String Order_Time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        private String Order_Tile ;
        private String Order_BuyerPhone;
        private String ReceiverTrueName_ThirdSys;
        private String ReceiverCompany_ThirdSys ;
        private String ReceiverAddress_ThirdSys;
        private int HaveProducts = 1;
        private List<Order_Product> Order_Products;

        private OrderInfo(String order_No, String order_Money,String order_Tile, String order_BuyerPhone, String receiverTrueName_ThirdSys,
                          String receiverAddress_ThirdSys, List<Order_Product> order_Products) {
            Order_No = order_No;
            Order_Money = order_Money;

            Order_Tile = order_Tile;
            Order_BuyerPhone = order_BuyerPhone;
            ReceiverTrueName_ThirdSys = receiverTrueName_ThirdSys;
            ReceiverCompany_ThirdSys = receiverTrueName_ThirdSys;
            ReceiverAddress_ThirdSys = receiverAddress_ThirdSys;
            Order_Products = order_Products;
        }
    }


    private int GoPayType;
    private int OrderIniter = 1;
    private String BuyerUserID_ThirdSys;
    private String BuyerUserName_ThirdSys;
    private String SellerUserID_ThirdSys = "YKKY0001";
    private int BuyerUserType_ThirdSys = 0;
    private String BuyerTrueName_ThirdSys;
    private String BuyerCompany_ThirdSys;
    private String BuyerPhoneNum_ThirdSys;
    private String BuyerAddress_ThirdSys;
    private List<OrderInfo> OrderInfos;
    private String Expand1;

    private OrderSubmitPay(String TxCode, int goPayType,
                           String buyerUserID_ThirdSys,
                           String buyerUserName_ThirdSys,
                           String buyerTrueName_ThirdSys,
                           String buyerPhoneNum_ThirdSys,
                           String buyerAddress_ThirdSys,
                           List<OrderInfo> orderInfos,
                           String expand1) {
        super(TxCode);
        GoPayType = goPayType;
        BuyerUserID_ThirdSys = buyerUserID_ThirdSys;
        BuyerUserName_ThirdSys = buyerUserName_ThirdSys;
        BuyerTrueName_ThirdSys = buyerTrueName_ThirdSys;
        BuyerCompany_ThirdSys = buyerTrueName_ThirdSys;
        BuyerPhoneNum_ThirdSys = buyerPhoneNum_ThirdSys;
        BuyerAddress_ThirdSys = buyerAddress_ThirdSys;
        OrderInfos = orderInfos;
        Expand1 = expand1;
    }

    @Override
    protected int RequestType() {
        return 1;
    }

    public static String submitOrder(int goPayType, String companyID, String userName, String phone, String address,String orderID, String money, String expand1){
        // 创建商品
        List<OrderSubmitPay.OrderInfo.Order_Product> productList = new ArrayList<>();
        productList.add(new OrderSubmitPay.OrderInfo.Order_Product(orderID,"订单编号"+orderID,money,1));

        // 创建订单
        List<OrderSubmitPay.OrderInfo> orderList = new ArrayList<>();
        orderList.add(new OrderSubmitPay.OrderInfo(orderID,money,"一块医药订单",phone,userName,address, productList));

        // 创建请求
        OrderSubmitPay req = new OrderSubmitPay("MALL10004",goPayType,companyID,companyID,userName,phone,address,orderList, expand1);

        return CCB_REQUEST(req);
    }
}
