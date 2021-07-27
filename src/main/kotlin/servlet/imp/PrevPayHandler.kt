package servlet.imp

import com.egzosn.pay.common.bean.PayOrder
import server.Launch
import server.apyimp.AlipayImp
import server.apyimp.WxpayImp
import server.beans.IceResult
import server.beans.QrImage
import servlet.abs.ServletAbs
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
open class PrevPayHandler : ServletAbs() {
    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        val result = IceResult()
        try {
            //获取表单数据
            val map = mutableMapOf<String, String>()

            val contentType = req.getHeader("content-type")

            if (contentType == "application/x-www-form-urlencoded") {
                map["type"] = getText(req.getParameter("type"))
                map["orderNo"] = getText(req.getParameter("orderNo"))
                map["subject"] = getText(req.getParameter("subject"))
                map["body"] = getText(req.getParameter("body"))
                map["price"] = getText(req.getParameter("price"))
                map["app"] = getText(req.getParameter("app"),"false")
                map["openid"] = getText(req.getParameter("openid"))
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

        resp.writer.println(result)
    }


















}