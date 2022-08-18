package server;



import bottle.util.Log4j;
import io.undertow.servlet.spec.HttpServletRequestImpl;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Enumeration;


/**
 * @Author: leeping
 * @Date: 2020/3/10 14:03
 * 跨域说明:  http://www.ruanyifeng.com/blog/2016/04/cors.html
 */
public class AccessControlAllowOriginFilter implements javax.servlet.Filter{

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if (request instanceof HttpServletRequestImpl){
            StringBuilder sb = new StringBuilder();
            HttpServletRequestImpl imp = (HttpServletRequestImpl) request;
            sb.append(imp.getMethod() ).append(" , ").append(imp.getRequestURI());

            /*Enumeration<String> header = imp.getHeaderNames();
            while (header.hasMoreElements()){
                String headerStr = header.nextElement();
                sb.append("\n\t").append(headerStr).append(" = ").append(imp.getHeader(headerStr));
            }*/

            Log4j.info(Thread.currentThread()+ " 接入访问: " + sb);
        }

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        try {
            req.setCharacterEncoding("UTF-8");
            resp.setCharacterEncoding("UTF-8");

            resp.setContentType("text/html;charset=utf-8");

            resp.addHeader("Access-Control-Allow-Origin", "*");
            resp.addHeader("Access-Control-Allow-Methods", "GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS");
            resp.addHeader("Access-Control-Allow-Headers", "x-requested-with");

            resp.addHeader("Access-Control-Allow-Headers",
                    "specify-path,specify-filename,save-md5,is-sync,tailor-list," +
                            "path-list,excel-path,ergodic-sub,"+
                            "delete-list,image-compress,image-logo,image-size-limit,image-spec-suffix-limit,image-compress-size,image-min-exist,"+
                            "delete-time,"+
                            "image-base64," +
                            "image-pix-color");

            chain.doFilter(req, resp);
        } catch (UnsupportedEncodingException ignored) { }
    }

    @Override
    public void destroy() {

    }
}
