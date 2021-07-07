package servlet.imp


import server.Launch
import server.apyimp.AlipayImp
import server.beans.BackResult


import servlet.abs.ServletAbs

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import com.alibaba.fastjson.JSON
import server.apyimp.WxpayImp


/**
 * @Author: leeping
 * @Date: 2019/4/22 10:57
 */
class QueryPay : ServletAbs(){

    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        super.doPost(req, resp)
        val result = BackResult();
        try {

            val contentType = req.getHeader("content-type")

            if (contentType == "application/x-www-form-urlencoded") {
                val type = getText(req.getParameter("type"))
                val orderNo = getText(req.getParameter("orderNo"))
                val isApp = getText(req.getParameter("app"),"false").toBoolean()

                if (type == "alipay") {
                    val map =  AlipayImp.queryInfo(orderNo);
                    if(map!=null) {
                        val json = map["alipay_trade_query_response"]!!.toString()
                        val maps = JSON.parse(json) as Map<*, *>
                        Launch.printMap(maps)

                        val trade_status = maps["trade_status"]
                        val state = if ("TRADE_SUCCESS" == trade_status) 1 else if ("WAIT_BUYER_PAY" == trade_status) 0 else -2
                        result.set(200,"查询成功",state)
                    }
                } else if (type == "wxpay"){
                    val map =  WxpayImp.queryInfo(orderNo,isApp);
                    if(map!=null) {
                        Launch.printMap(map)
                        /*
                        SUCCESS—支付成功
                        REFUND—转入退款
                        NOTPAY—未支付
                        CLOSED—已关闭
                        REVOKED—已撤销（付款码支付）
                        USERPAYING--用户支付中（付款码支付）
                        PAYERROR--支付失败(其他原因，如银行返回失败)
                        */
                        val return_code = map["return_code"]!!.toString()
                        if (return_code == "SUCCESS"){
                            val result_code = map["result_code"].toString()
                            if (result_code == "SUCCESS"){
                                val trade_state = map["trade_state"].toString()
                                val trade_state_desc = map["trade_state_desc"].toString()
                                val state = when(trade_state){
                                    "SUCCESS" -> 1
                                    "REFUND" -> 2
                                    "NOTPAY" -> 0
                                    "CLOSED" -> -1
                                    "REVOKED" -> -1
                                    "USERPAYING" -> 0
                                    "PAYERROR" -> -1
                                    else -> -2
                                }
                                result.set(200,trade_state_desc,state)
                            }else if(result_code == "FAIL"){
                               val msg =  map["err_code"].toString()+"-"+map["err_code_des"].toString()
                                result.set(-200,msg,-2)
                            }else throw Exception("业务错误:" + Launch.printMap(map))
                        }else if (return_code == "FAIL"){
                            result.set(200,map["return_msg"].toString(),-2)
                        }else throw Exception("通讯错误:" + Launch.printMap(map))
                    }
                }else throw Exception("type refuse")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            result.set(-1,e.message,-2)
        }

        resp.writer.println(result)
    }
}

