package servlet.imp

import server.payimps.AlipayImp
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @Author: leeping
 * @Date: 2019/4/18 11:43
 * 支付结果处理
 */
class PayResultCallcackAlipay  : javax.servlet.http.HttpServlet()  {
    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.writer.println(AlipayImp.response(req))
    }

}