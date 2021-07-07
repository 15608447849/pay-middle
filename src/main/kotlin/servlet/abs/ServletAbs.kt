package servlet.abs

import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @Author: leeping
 * @Date: 2019/4/16 11:58
 */
open class ServletAbs : javax.servlet.http.HttpServlet() {

     //跨域
    protected fun filter(req: HttpServletRequest, resp: HttpServletResponse) {
 //        http://www.ruanyifeng.com/blog/2016/04/cors.html
        try
        {
            resp.characterEncoding = "UTF-8"
            req.characterEncoding = "UTF-8"
        }
        catch (e: UnsupportedEncodingException)
        {
            e.printStackTrace()
        }

        resp.addHeader("Access-Control-Allow-Origin", "*")
        resp.addHeader("Access-Control-Allow-Methods", "GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS")
        resp.addHeader("Access-Control-Allow-Headers", "x-requested-with") // 允许x-requested-with请求头
}

    @Throws(ServletException::class, IOException::class)
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        filter(req, resp)
    }

    @Throws(ServletException::class, IOException::class)
    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        filter(req, resp)

    }

    @Throws(ServletException::class, IOException::class)
    override fun doOptions(req: HttpServletRequest, resp: HttpServletResponse) {
        filter(req, resp)
        super.doOptions(req, resp)
    }

    //获取文本
    protected fun getText(parameter: String?, def: String = ""): String {
        if (parameter == null) return def
        return URLDecoder.decode(parameter,"UTF-8")
    }

}


