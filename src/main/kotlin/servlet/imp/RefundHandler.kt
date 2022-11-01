package servlet.imp

import bottle.util.Log4j
import com.egzosn.pay.common.bean.RefundOrder
import server.common.CommFunc.getURLDecoderParameter
import server.yeepay.payimps.AlipayImp
import server.yeepay.payimps.WxpayImp
import server.beans.IceResult
import server.yeepay.payimps.YeepayImp
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.math.BigDecimal
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


/**
 * @Author: leeping
 * @Date: 2019/4/16 13:39
 * 退款处理
 */
open class RefundHandler : javax.servlet.http.HttpServlet() {

    companion object{
        @JvmField
        val ingSet = HashSet<String>()

        @JvmStatic
        fun checkRefundIng(orderNo: String? ) : Boolean{
            if (!orderNo.isNullOrEmpty()) {
                return !ingSet.add(orderNo)
            }
            return false
        }

        @JvmStatic
        fun removeRefundIngOrderNo(orderNo: String? ){
            if(orderNo != null){
                ingSet.remove(orderNo)
            }
        }

    }


    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {

        val result = IceResult()
        var questStr = "退款请求: "
        try {

            if (req.getHeader("content-type") == "application/x-www-form-urlencoded") {
                val type = getURLDecoderParameter(req.getParameter("type"), "")// 平台类型
                val orderNo = getURLDecoderParameter(req.getParameter("orderNo"), "0") //平台订单号
                val orderNoSec = getURLDecoderParameter(req.getParameter("orderNoSec"), "0") //平台订单号下单流水号
                val refundNo = getURLDecoderParameter(req.getParameter("refundNo"), "0") //退款单流水号
                val tradeNo = getURLDecoderParameter(req.getParameter("tradeNo"), "0") //支付平台相关订单号
                val price = getURLDecoderParameter(req.getParameter("price"), "0") //退款金额
                val priceTotal = getURLDecoderParameter(req.getParameter("priceTotal"), "0") //退款总金额
                val isApp = getURLDecoderParameter(req.getParameter("app"), "false")!!.toBoolean() //是不是移动支付

                questStr += "type=$type ,orderNo=$orderNo ,tradeNo=$tradeNo ,refundNo=$refundNo ,price=$price ,priceTotal=$priceTotal ,app=$isApp"

                if (tradeNo.isEmpty() || tradeNo.equals("0")) throw IllegalStateException("订单号( $orderNo ) 交易流水号不正确")

                // 检查退款是否正在进行中

                if (checkRefundIng(orderNo)) {
                    throw IllegalStateException("订单号( $orderNo ) 正在退款中")
                }

                val rorder = RefundOrder(refundNo, tradeNo, BigDecimal(price))
                rorder.totalAmount = if (priceTotal.isNotEmpty()) BigDecimal(priceTotal) else BigDecimal(price)

                val tuple = when (type) {
                    "alipay" -> AlipayImp.refund(rorder);
                    "wxpay" ->  WxpayImp.refund(rorder, isApp);
                    "yeepay" ->  YeepayImp.refund(orderNoSec,refundNo,tradeNo,price);
                    else -> throw IllegalArgumentException("pay type refuse")
                }

                removeRefundIngOrderNo(orderNo)
                result.set(if (tuple.value0) 2 else -2, tuple.value1, tuple.value0)

            }
        } catch (e: Exception) {
//            Log4j.error(e)
            result.set(-2,e.message,false)
        }

        Log4j.info("$questStr\n\t响应结果: ${result.toJson()}")

        resp.writer.println(result.toJson())
    }


















}