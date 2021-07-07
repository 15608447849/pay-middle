package servlet.imp

import com.alibaba.fastjson.JSON
import com.egzosn.pay.common.bean.RefundOrder
import server.Launch
import server.apyimp.AlipayImp
import server.apyimp.WxpayImp
import server.beans.BackResult
import servlet.abs.ServletAbs
import java.lang.IllegalStateException
import java.math.BigDecimal
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


/**
 * @Author: leeping
 * @Date: 2019/4/16 13:39
 */
open class RefundHandler : ServletAbs() {

    companion object{
        @JvmField
        val ingSet = HashSet<String>()

        @JvmStatic
        fun checkRefundIng(orderNo: String? ) : Boolean{
            if (orderNo != null && orderNo.isNotEmpty()) {
                return !ingSet.add(orderNo)
            }
            return false
        }

        @JvmStatic
        fun removeRefundIngOrderNo(orderNo: String? ){
            if(orderNo != null){
                ingSet.remove(orderNo)
            }
        }

    }


    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        super.doPost(req, resp)
        val result = BackResult();
        try {
            val contentType = req.getHeader("content-type")

            if (contentType == "application/x-www-form-urlencoded") {
                val type = getText(req.getParameter("type"))// 平台类型
                val tradeNo = getText(req.getParameter("tradeNo")) //支付平台相关订单号
                val orderNo = getText(req.getParameter("orderNo")) //平台订单号
                val refundNo = getText(req.getParameter("refundNo")) //退款单号
                val price = getText(req.getParameter("price")) //退款金额
                val priceTotal = getText(req.getParameter("priceTotal")) //退款总金额
                val isApp = getText(req.getParameter("app"),"false").toBoolean() //是不是移动支付

                println("退款: type=$type ,orderNo=$orderNo,tradeNo=$tradeNo,refundNo=$refundNo,price=$price,priceTotal=$priceTotal,app=$isApp")

                if(tradeNo.isEmpty() || tradeNo.equals("0")) throw IllegalStateException("订单号( $orderNo ) 交易流水号不正确")

                // 检查退款是否正在进行中

                if(checkRefundIng(orderNo)){
                    throw IllegalStateException("订单号( $orderNo ) 正在退款中")
                }

                val rorder = RefundOrder(refundNo, tradeNo,BigDecimal(price))
                    rorder.totalAmount = if (priceTotal.isNotEmpty()) BigDecimal(priceTotal) else BigDecimal(price)

                if(type == "alipay"){
                    val map =  AlipayImp.refund(rorder);
                    Launch.printMap(map)
                    if(map!=null) {
                        val json = map["alipay_trade_refund_response"]!!.toString()
                        val maps = JSON.parse(json) as Map<*, *>
                        Launch.printMap(maps)
                        val code = maps["code"]!!.toString().toInt()
                        if (code == 10000){
                            val fund_change = maps["fund_change"]
                            if (fund_change == "Y") {
                                removeRefundIngOrderNo(orderNo)
                                result.set(2,"退款成功",true)
                            }
                            if (fund_change == "N") result.set(2,"已申请退款,请勿重复提交",true)
                        }else{
                            val sub_code = maps["sub_code"]

                            val msg = when (sub_code){
                                "ACQ.SYSTEM_ERROR" -> "系统错误,请使用相同的参数再次调用"
                                "ACQ.INVALID_PARAMETER" -> "参数无效,请求参数有错，重新检查请求后，再调用退款"
                                "ACQ.SELLER_BALANCE_NOT_ENOUGH" -> "卖家余额不足,商户支付宝账户充值后重新发起退款即可"
                                "ACQ.REFUND_AMT_NOT_EQUAL_TOTAL" -> "退款金额超限,检查退款金额是否正确，重新修改请求后，重新发起退款"
                                "ACQ.TRADE_NOT_EXIST" -> "交易不存在,检查请求中的交易号和商户订单号是否正确，确认后重新发起"
                                "ACQ.TRADE_HAS_FINISHED" -> "交易已完结,该交易已完结，不允许进行退款，确认请求的退款的交易信息是否正确"
                                "ACQ.TRADE_STATUS_ERROR" -> "交易状态非法,查询交易，确认交易是否已经付款"
                                "ACQ.DISCORDANT_REPEAT_REQUEST" -> "不一致的请求,检查该退款号是否已退过款或更换退款号重新发起请求"
                                "ACQ.REASON_TRADE_REFUND_FEE_ERR" -> "退款金额无效,检查退款请求的金额是否正确请使用相同的参数再次调用"
                                "ACQ.TRADE_NOT_ALLOW_REFUND" -> "当前交易不允许退款,检查当前交易的状态是否为交易成功状态以及签约的退款属性是否允许退款，确认后，重新发起请求"
                                else -> maps["sub_msg"].toString()
                            }
                            result.set(-2,msg,false)
                        }
                    }

                }
                else if(type == "wxpay"){
                    val map =  WxpayImp.refund(rorder,isApp);
                    Launch.printMap(map)
                    if(map!=null) {
                        //通讯结果
                        val return_code = map["return_code"]!!.toString()
                        if(return_code == "SUCCESS"){
                            //业务结果
                            val result_code  = map["result_code"].toString()
                            if (result_code == "SUCCESS"){
                                removeRefundIngOrderNo(orderNo)
                                result.set(2,"退款成功",true)
                            }else if(result_code == "FAIL"){
                                val err_code = map["err_code"].toString()

                                val msg = when (err_code){
                                    "SYSTEMERROR" -> "系统超时等原因,接口返回错误,请不要更换商户退款单号，请使用相同参数再次调用API"
                                    "BIZERR_NEED_RETRY" -> "并发情况下，业务被拒绝，商户重试即可解决,请不要更换商户退款单号，请使用相同参数再次调用API"
                                    "TRADE_OVERDUE" -> "订单已经超过可退款的最大期限(支付后一年内可退款),请选择其他方式自行退款"
                                    "ERROR" -> "申请退款业务发生错误,${map["err_code_des"].toString()}"
                                    "USER_ACCOUNT_ABNORMAL" -> "用户帐号注销,退款申请失败，商户可自行处理退款。"
                                    "INVALID_REQ_TOO_MUCH" -> "连续错误请求数过多被系统短暂屏蔽,请检查业务是否正常，确认业务正常后请在1分钟后再来重试"
                                    "NOTENOUGH" -> "商户可用退款余额不足"
                                    "INVALID_TRANSACTIONID" -> "无效transaction_id,请求参数错误，检查原交易号是否存在或发起支付交易接口返回失败"
                                    "PARAM_ERROR" -> "请求参数错误，请重新检查再调用退款申请"
                                    "APPID_NOT_EXIST" -> "请检查APPID是否正确"
                                    "MCHID_NOT_EXIST" -> "请检查MCHID是否正确"
                                    "ORDERNOTEXIST" -> "请检查你的订单号是否正确且是否已支付，未支付的订单不能发起退款"
                                    "REQUIRE_POST_METHOD" -> "请检查请求参数是否通过post方法提交"
                                    "SIGNERROR" -> "请检查签名参数和方法是否都符合签名算法要求"
                                    "XML_FORMAT_ERROR" -> "请检查XML参数格式是否正确"
                                    "FREQUENCY_LIMITED" -> "该笔退款未受理，请降低频率后重试"
                                    else -> map["err_code"].toString()+"-"+map["err_code_des"].toString()
                                }
                                result.set(-2,msg,false)
                            }else throw Exception("业务错误:" + Launch.printMap(map))

                        }else if (return_code == "FAIL"){
                            //通讯失败 原因
                            result.set(-2,map["return_msg"].toString(),map);
                        }else throw Exception("通讯错误:" + Launch.printMap(map))

                    }
                }
                else throw Exception("type refuse")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            result.set(-2,e.message,false)
        }

        resp.writer.println(result)
    }


















}