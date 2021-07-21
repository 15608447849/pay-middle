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

    //获取文本
    protected fun getText(parameter: String?, def: String = ""): String {
        if (parameter == null) return def
        return URLDecoder.decode(parameter,"UTF-8")
    }

}


