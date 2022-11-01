package servlet.imp


import bottle.util.Log4j
import server.yeepay.payimps.AlipayImp
import server.beans.IceResult
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import server.common.CommFunc.getURLDecoderParameter
import server.yeepay.payimps.WxpayImp
import server.yeepay.payimps.YeepayImp
import java.lang.IllegalArgumentException


/**
 * @Author: leeping
 * @Date: 2019/4/22 10:57
 * 查询支付结果
 * 供ICE节点使用
 * 0-待支付 1已支付 -2异常
 */
class PayStatusQuery : javax.servlet.http.HttpServlet(){

    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        val result = IceResult()
        var questStr = "查询请求: "

        try {

            if (req.getHeader("content-type") == "application/x-www-form-urlencoded") {

                val type = getURLDecoderParameter(req.getParameter("type"), "") // 支付平台类型
                val orderNo = getURLDecoderParameter(req.getParameter("orderNo"), "0") // 支付订单号或者流水号
                val isApp = getURLDecoderParameter(req.getParameter("app"), "false")!!.toBoolean() // 是否移动支付
                questStr += "type=$type ,orderNo=$orderNo ,app=$isApp"

               val tuple = when(type){
                    "alipay" -> AlipayImp.query(orderNo)
                    "wxpay" ->  WxpayImp.query(orderNo,isApp);
                    "yeepay" -> YeepayImp.query(orderNo);
                    else -> throw IllegalArgumentException("pay type refuse")
                }

                result.set(200, tuple.value1, tuple.value0)
            }

        } catch (e: Exception) {
//            Log4j.error(e)
            result.set(-200,e.message,-2)
        }

        Log4j.info("$questStr\n\t响应结果: ${result.toJson()}")
        resp.writer.println(result.toJson())
    }
}


