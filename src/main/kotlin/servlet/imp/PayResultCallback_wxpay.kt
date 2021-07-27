package servlet.imp

import server.apyimp.AlipayImp
import server.apyimp.WxpayImp
import servlet.abs.ServletAbs
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @Author: leeping
 * @Date: 2019/4/18 11:43
 * 微信结果处理
 */
class PayResultCallback_wxpay  : ServletAbs()  {
    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        val result = WxpayImp.response(req)
        resp.writer.println(result)
    }

}