package ccbpay.entitys;

import ccbpay.common.CCBQueryFactory;

public class OrderRefund extends CCBQueryFactory.CCB_QUERY_STRUCT {
    private final String Order_No_CCB;
    private final String SellerUserID_ThirdSys;
    private final int PayBackReason = 3;
    private final int GetProduct = 0;
    private final String PayBackID;
    private final int PaybackType = 1;
    private final String PayBackMoney;
    private final String Introduce = "";

    public OrderRefund(String order_No_CCB, String sellerUserID_thirdSys, String payBackID, String payBackMoney) {
        super("MALL10026");

        Order_No_CCB = order_No_CCB;
        SellerUserID_ThirdSys = sellerUserID_thirdSys;
        PayBackID = payBackID;
        PayBackMoney = payBackMoney;
    }

    @Override
    protected int RequestType() {
        return 2;
    }
}
