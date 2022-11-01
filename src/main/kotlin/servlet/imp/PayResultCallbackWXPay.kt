package servlet.imp

import server.yeepay.payimps.WxpayImp
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @Author: leeping
 * @Date: 2019/4/18 11:43
 * 微信
 * 支付结果处理
 */
class PayResultCallbackWXPay  : javax.servlet.http.HttpServlet()  {
    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.writer.println(WxpayImp.response(req))
    }

}