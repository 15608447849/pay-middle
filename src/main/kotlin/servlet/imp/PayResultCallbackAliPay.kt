package servlet.imp

import server.yeepay.payimps.AlipayImp
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @Author: leeping
 * @Date: 2019/4/18 11:43
 * 支付宝
 * 支付结果处理
 */
class PayResultCallbackAliPay  : javax.servlet.http.HttpServlet()  {
    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.writer.println(AlipayImp.response(req))
    }

}