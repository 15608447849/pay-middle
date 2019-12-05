package servlet.imp

import server.apyimp.AlipayImp
import servlet.abs.ServletAbs
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @Author: leeping
 * @Date: 2019/4/18 11:43
 */
class AlipayResult  : ServletAbs()  {
    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        super.doGet(req, resp)
        val result = AlipayImp.response(req)
        resp.writer.println(result)
    }

}