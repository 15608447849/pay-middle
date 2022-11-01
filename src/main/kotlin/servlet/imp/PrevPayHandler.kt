package servlet.imp

import bottle.util.Log4j
import com.egzosn.pay.common.bean.PayOrder
import server.Launch
import server.common.CommFunc.getURLDecoderParameter
import server.yeepay.payimps.AlipayImp
import server.yeepay.payimps.WxpayImp
import server.beans.IceResult
import server.beans.QrImage
import server.yeepay.payimps.YeepayImp
import java.lang.IllegalArgumentException
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
        var questStr = "支付请求: "

        try {
            //获取表单数据
            val map = mutableMapOf<String, String>()

            if (req.getHeader("content-type") == "application/x-www-form-urlencoded") {
                map["type"] = getURLDecoderParameter(req.getParameter("type"), "") // 三方支付平台类型
                map["orderNo"] = getURLDecoderParameter(req.getParameter("orderNo"), "0") // 订单号或订单流水号
                map["subject"] = getURLDecoderParameter(req.getParameter("subject"), "一块医药") // 标题
                map["body"] = getURLDecoderParameter(req.getParameter("body"), "") // 附加信息: 时间戳,服务标识,服务名,回调类,回调方法,公司码
                map["price"] = getURLDecoderParameter(req.getParameter("price"), "0") // 金额
                map["openid"] = getURLDecoderParameter(req.getParameter("openid"), "") // 微信公众号-支付者openID
                map["channel"] = getURLDecoderParameter(req.getParameter("channel"), "")// 易宝支付-支付渠道 (WECHAT/ALIPAY)
                val isApp = getURLDecoderParameter(req.getParameter("app"), "false")!!.toBoolean() // 是否app支付

                val expirationTime = Date(System.currentTimeMillis() + 10 * 1000 * 60L)
                questStr += "type=${map["type"]} ,orderNo=${map["orderNo"]} ,subject=${map["subject"]} ," +
                        "body=${map["body"]} ,price=${map["price"]} ,openid=${map["openid"]} ,app=$isApp," +
                        "expirationTime=${SimpleDateFormat("yyyy年-MM月dd日 HH时mm分ss秒").format(expirationTime)}"

                // 支付金额
                val price = BigDecimal(map["price"])

                // 二维码文件对象
                val qrImageBean = QrImage(Launch.domain,Launch.dirPath,map["type"],map["orderNo"])
                // 三方应用预支付字段
                val prevPayFieldMap =  mutableMapOf<String, Any>()

                when (map["type"]){

                    "alipay" -> {
                        val payOrder = PayOrder(map["subject"], map["body"], price, map["orderNo"])
                        payOrder.addition = map["body"] //附加信息
                        payOrder.expirationTime = expirationTime;//到期时间

                        AlipayImp.create(isApp,payOrder,qrImageBean,prevPayFieldMap)
                    }

                    "wxpay" -> {
                        val payOrder = PayOrder(map["subject"], map["body"], price, map["orderNo"])
                        payOrder.addition = map["body"] // 附加信息
                        payOrder.openid = map["openid"] // 微信公众号-支付者openID
                        payOrder.expirationTime = expirationTime;//到期时间

                        WxpayImp.create(isApp,payOrder,qrImageBean,prevPayFieldMap)
                    }

                "yeepay"-> {
                     YeepayImp.create(isApp,map["channel"].toString(),map["orderNo"].toString(),map["subject"].toString(),
                         price,expirationTime, map["body"].toString(), qrImageBean,prevPayFieldMap);
                }

                    else -> throw IllegalArgumentException("pay type refuse")
                }

                if (isApp){
                    if(prevPayFieldMap.isEmpty()) throw IllegalArgumentException("预支付信息不存在")
                    // APP 预支付信息
                    result.set(1,prevPayFieldMap)
                }else{
                    // PC 二维码链接
                    result.set(1,qrImageBean.link)
                }
            }
        } catch (e: Exception) {
//            Log4j.error(e)
            result.set(-1,e)
        }
        Log4j.info("$questStr\n\t响应结果: ${result.toJson()}")

        resp.writer.println(result.toJson())
    }


















}