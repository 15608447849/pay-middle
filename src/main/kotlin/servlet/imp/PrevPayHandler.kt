package servlet.imp

import com.egzosn.pay.common.bean.PayOrder
import server.Launch
import server.Launch.getURLDecoderParameter
import server.payimps.AlipayImp
import server.payimps.WxpayImp
import server.beans.IceResult
import server.beans.QrImage
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse




/**
 * @Author: leeping
 * @Date: 2019/4/16 13:39
 * 预支付请求处理
 */
open class PrevPayHandler : javax.servlet.http.HttpServlet() {
    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        val result = IceResult()
        try {
            //获取表单数据
            val map = mutableMapOf<String, String>()

            val contentType = req.getHeader("content-type")

            if (contentType == "application/x-www-form-urlencoded") {
                map["type"] = getURLDecoderParameter(req.getParameter("type"),"") // 三方支付平台类型
                map["orderNo"] = getURLDecoderParameter(req.getParameter("orderNo"),"0") // 订单号或订单流水号
                map["subject"] = getURLDecoderParameter(req.getParameter("subject"),"") // 标题
                map["body"] = getURLDecoderParameter(req.getParameter("body"),"") // 附加信息: 时间戳,服务标识,服务名,回调类,回调方法,公司码
                map["price"] = getURLDecoderParameter(req.getParameter("price"),"0") // 金额
                map["app"] = getURLDecoderParameter(req.getParameter("app"),"false") // 是否app支付
                map["openid"] = getURLDecoderParameter(req.getParameter("openid"),"") // 微信公众号对应的支付者openID
            }

            Launch.printMap(map)

            val qrImage = QrImage(Launch.dirPath,map["type"],map["orderNo"])

            val price = BigDecimal(map["price"])
            //支付订单基础信息
            val payOrder = PayOrder(map["subject"], map["body"], price, map["orderNo"])
            payOrder.addition = map["body"] //附加信息
            payOrder.openid = map["openid"]
            payOrder.expirationTime = Date(System.currentTimeMillis() + 10 * 1000 * 60L);

            Launch.log.info("到期时间: ${SimpleDateFormat("yyyy年-MM月dd日 HH时mm分ss秒").format(payOrder.expirationTime)}")

            val isApp = map["app"]!!.toBoolean()
            val appPayMap = when (map["type"]){
                "alipay" ->  AlipayImp.execute(payOrder,qrImage.qrImage,isApp)
                "wxpay" -> WxpayImp.execute(payOrder,qrImage.qrImage,isApp)
                 else -> throw Exception("type refuse")
            }

            if (appPayMap!=null){
                result.set(1,appPayMap)
            }else{

                val qrCodeLinke = "${Launch.domain}${qrImage.link}" //返回前端二维码信息
                result.set(1,qrCodeLinke)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            result.set(-1,e)
        }

        resp.writer.println(result.toJson())
    }


















}