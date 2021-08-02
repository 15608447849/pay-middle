package ccbpay.entitys;

import ccbpay.common.CCBQueryFactory;

import java.util.ArrayList;
import java.util.List;

import static ccbpay.common.CCBQueryFactory.CCB_REQUEST;

public class OrderStatusQuery extends CCBQueryFactory.CCB_QUERY_STRUCT {
    private static final class OrderInfo {
        String Order_No;
        private OrderInfo(String order_No) {
            Order_No = order_No;
        }
    }
    @Override
    protected int RequestType() {
        return 2;
    }

    private List<OrderInfo> OrderInfos;

    private OrderStatusQuery(List<OrderInfo> orderInfos) {
        super("MALL10002");
        OrderInfos = orderInfos;
    }

    public static String queryOrderStatus(String orderID){
        // 创建订单号
        List<OrderStatusQuery.OrderInfo> list = new ArrayList<>();
        list.add(new OrderStatusQuery.OrderInfo(orderID));
        OrderStatusQuery req = new OrderStatusQuery(list);
        return CCB_REQUEST(req);
    }
}
