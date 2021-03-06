package com.mfq.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import com.google.common.collect.Lists;
import com.mfq.bean.Product;
import com.mfq.bean.Refund;
import com.mfq.bean.app.CouponInfo2App;
import com.mfq.bean.app.Refund2App;
import com.mfq.bean.coupon.Coupon;
import com.mfq.constants.PolicyStatus;
import com.mfq.service.ProductService;
import com.mfq.service.RefundService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mfq.annotation.LoginRequired;
import com.mfq.bean.app.OrderInfo2App;
import com.mfq.bean.user.UserQuota;
import com.mfq.constants.ErrorCodes;
import com.mfq.constants.PayType;
import com.mfq.dataservice.context.UserIdHolder;
import com.mfq.helper.SignHelper;
import com.mfq.service.CouponService;
import com.mfq.service.OrderService;
import com.mfq.service.user.UserQuotaService;
import com.mfq.utils.JsonUtil;

@Controller
@RequestMapping("/order")
public class OrderController {

    private static final Logger logger = LoggerFactory
            .getLogger(OrderController.class);

    @Resource
    OrderService orderService;//订单
    @Resource
    CouponService couponService;//优惠券
    @Resource
    UserQuotaService userQuotaService;//用户额度
    @Resource
    RefundService refundService;//资金还款
    @Resource
    ProductService productService;//产品


    /**
     * 进入预定页面
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = { "/book", "/book/" }, method = RequestMethod.POST)
    @ResponseBody
    @LoginRequired
    public String booking(HttpServletRequest request,
            HttpServletResponse response) {
        String ret = "";
        try {

            Map<String, Object> params = JsonUtil.readMapFromReq(request);
            Long uid = Long.parseLong(params.get("uid").toString());
            Long pid = Long.parseLong(params.get("pid").toString());
            if (!SignHelper.validateSign(params)) { // 签名验证失败
                return JsonUtil.toJson(ErrorCodes.SIGN_VALIDATE_ERROR, "签名验证失败",
                        null);
            }
            if (uid == null || uid == 0 || pid == null) { // 参数异常
                return JsonUtil.toJson(ErrorCodes.CORE_PARAM_UNLAWFUL, "参数异常",
                        null);
            }

            ret = orderService.bookingOrder(uid, pid);
        } catch (Exception e) {
            logger.error("Exception PreBuy Process!", e);
            ret = JsonUtil.toJson(ErrorCodes.CORE_ERROR, "系统异常", null);
        }
        logger.info("Order_PreBuy_Ret is:{}", ret);
        return ret;
    }

    /**
     * 根据用户、订单ID获取可用余额（包含优惠券）
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = { "/available_balance",
            "/available_balance/" }, method = RequestMethod.POST)
    @ResponseBody
    @LoginRequired
    public String availableBalance(HttpServletRequest request,
            HttpServletResponse response) {
        String ret = "";
        try {
            Map<String, Object> params = JsonUtil.readMapFromReq(request);
            Long uid = Long.parseLong(params.get("uid").toString());
            Long pid = Long.parseLong(params.get("pid").toString());
            if (!SignHelper.validateSign(params)) { // 签名验证失败
                return JsonUtil.toJson(ErrorCodes.SIGN_VALIDATE_ERROR, "签名验证失败",
                        null);
            }
            if (uid == null || uid == 0 || pid == null) { // 参数异常
                return JsonUtil.toJson(ErrorCodes.CORE_PARAM_UNLAWFUL, "参数异常",
                        null);
            }
            ret = orderService.orderValidBalance(uid, pid);
        } catch (Exception e) {
            logger.error("Exception PreBuy Process!", e);
            ret = JsonUtil.toJson(ErrorCodes.CORE_ERROR, "系统异常", null);
        }
        logger.info("Order_PreBuy_Ret is:{}", ret);
        return ret;
    }

    /**
     *
     * @param request
     * @param response
     * @return
     * 全款付
     */
    @RequestMapping(value = { "/create","/create/" }, method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    @ResponseBody
    @LoginRequired
    public String create(HttpServletRequest request,
            HttpServletResponse response) {
        String ret = "";
        try {
            Map<String, Object> params = JsonUtil.readMapFromReq(request);
            if (!SignHelper.validateSign(params)) { // 签名验证失败
                return JsonUtil.toJson(ErrorCodes.SIGN_VALIDATE_ERROR, "签名验证失败",
                        null);
            }
            Integer t = (Integer) params.get("type");

            PayType payType = PayType.fromId(t);

            logger.info("--------------t:{},payType:{}",t,payType.getName());
            if(t == null){
                payType = PayType.FULL;
            }
            Long uid = Long.parseLong(params.get("uid").toString());
            Long pid = Long.parseLong(params.get("pid").toString());
            Product product = productService.findById(pid);
            BigDecimal amount = product.getPrice();

            if (UserIdHolder.isLogin() && UserIdHolder.getLongUid() != uid
                    || pid == null) {
                logger.error("系统非法请求！ATTENTION_UNLAWFULL_ACCESS");
                return JsonUtil.toJson(ErrorCodes.CORE_PARAM_UNLAWFUL, "参数非法",
                        null);
            }


            BigDecimal onlinePay = new BigDecimal(0),
                    balancePay = new BigDecimal(0),
                    periodPay = new BigDecimal(0),
                    hospitalPay = new BigDecimal(0);

            int period = 0;

            logger.info("下单类别:{}", payType);

            String couponNum = null;
            //-------------------如果有优惠券的话,基本上什么都不用做,应该在支付的时候,考虑到优惠券的优惠,而不是在这里修改amount的值.
            if(params.get("coupon_num")!=null){
                couponNum = params.get("coupon_num").toString();
                if(couponNum.equals("didi")){

                }else{
                    Coupon coupon = couponService.findByCouponNum(params.get("coupon_num").toString());
                    List<Coupon> couponList = new ArrayList<>();
                    couponList.add(coupon);
                    CouponInfo2App CouponInfo2App = couponService.convert2AppList(couponList).get(0);//这里有可能出错,出错了的话很可能就是没有该优惠券

                    if(CouponInfo2App.getMoney().compareTo(amount) > 0){
                        ret = JsonUtil.toJson(ErrorCodes.CORE_ERROR, "减免金额低于总价", null);
                        return ret;
                    }
                    if(CouponInfo2App.getCondition().compareTo(amount) > 0){
                        ret = JsonUtil.toJson(ErrorCodes.CORE_ERROR, "不满足优惠条件", null);
                        return ret;
                    }
                }

            }
            //--------------------优惠券结束
            String operation_time = params.get("operation_time").toString();  // yyyy-MM-dd 预约就医时间
            if(operation_time.length()!=13){
                ret = JsonUtil.toJson(ErrorCodes.CORE_ERROR, "不是时间戳", null);
                return ret;
            }
            Date operationT = new Date(Long.parseLong(operation_time));

            int policy = PolicyStatus.WITHOUT.getId();
            if(params.get("policy")!=null)policy = Integer.parseInt(params.get("policy").toString());

            //创建订单
            OrderInfo2App order = orderService.createOrder(payType, uid, pid,
                    amount, onlinePay, hospitalPay, balancePay, period, periodPay,
                    couponNum, policy, operationT);

            if (order != null) {
                ret = JsonUtil.successResultJson(order);
                logger.info("Order_Create_Ret is:{}", ret);
            }
        } catch (Exception e) {
            logger.error("Exception OrderCreate Process!", e);
            ret = JsonUtil.toJson(ErrorCodes.CORE_ERROR, e.getMessage(), null);
        }
        return ret;
    }




    /**
     * 用于创建分期订单
     * 1、uid
     * 2、pid
     * 4、period期数
     * 5、是否悟空保
     * 6、预约就医时间
     *
     * 现在不需要判定用户输入的金额,用商品的现价或者分期价代替,优先使用分期价
     * @param request
     * @param response
     * @return
     * @author liuzhiguo1
     * 分期付
     */
    @RequestMapping(value = { "/create/finance","/create/finance/" }, method = {RequestMethod.POST,RequestMethod.GET}, produces = "application/json;charset=utf-8")
    @ResponseBody
    @LoginRequired
    public String createFinance(HttpServletRequest request,
            HttpServletResponse response){
        String ret = "";
        logger.info("进来了create/finance");
        try {
            Map<String, Object> params = JsonUtil.readMapFromReq(request);
            if (!SignHelper.validateSign(params)) { // 签名验证失败
                return JsonUtil.toJson(ErrorCodes.SIGN_VALIDATE_ERROR, "签名验证失败", null);
            }
            Long uid = Long.parseLong(params.get("uid").toString());
            Long pid = Long.parseLong(params.get("pid").toString());
            BigDecimal amount = productService.selectPriceOrFqPrice(pid);

            if (UserIdHolder.isLogin() && UserIdHolder.getLongUid() != uid || pid == null) {
                logger.error("系统非法请求！ATTENTION_UNLAWFULL_ACCESS");
                return JsonUtil.toJson(ErrorCodes.CORE_PARAM_UNLAWFUL, "参数非法",
                        null);
            }

            // MARK 如果有优惠券的话,应该先把amount减掉相应的金额,然后该干嘛干嘛,最后把金额给amout加上.
            String couponNum = null;
            if(params.get("coupon_num")!=null){
                couponNum = params.get("coupon_num").toString();
                Coupon coupon = couponService.findByCouponNum(params.get("coupon_num").toString());
                List<Coupon> couponList = new ArrayList<>();
                couponList.add(coupon);
                CouponInfo2App couponInfo2App = couponService.convert2AppList(couponList).get(0);//这里有可能出错,出错了的话很可能就是没有该优惠券

                //如果减免金额比总价多,减完成负数了,就不能给丫减. 如果优惠条件比总价低,就不能优惠
                if(amount.compareTo(couponInfo2App.getMoney()) < 0 ||
                        amount.compareTo(couponInfo2App.getCondition()) < 0){
                    ret = JsonUtil.toJson(ErrorCodes.CORE_ERROR,"金额不符合优惠券条件",null);
                    return ret;
                }
                //!!!!!!这里非常重要.需要先把 amount-money ,然后最后在生成的 order_info 中把order的amount加上.
                amount = amount.subtract(couponInfo2App.getMoney());
            }
            // MARK  优惠券结束

            int period = 0;
            logger.info("下单类别:{}", "分期付款");
            period = (Integer) params.get("period");//总共多少期
            UserQuota userQuota = userQuotaService.queryUserQuota(uid);
            BigDecimal quotaLeft = userQuota.getQuotaLeft();

            BigDecimal periodPay;

            if(params.get("coupon_num")!=null){

            }

            if(quotaLeft.compareTo(amount)<0){
            	periodPay = quotaLeft;
            }else{
            	periodPay = amount;
            }


            String operation_time = params.get("operation_time").toString();  // yyyy-MM-dd 预约就医时间
            if(operation_time.length()!=13){
                ret = JsonUtil.toJson(ErrorCodes.CORE_ERROR, "时间戳不对", null);
            }



            Date operationT = new Date(Long.parseLong(operation_time));
            int policy = PolicyStatus.WITHOUT.getId();
            if(params.get("policy")!=null){
            	policy = Integer.parseInt(params.get("policy").toString());
            }

            BigDecimal onlinePay = BigDecimal.valueOf(0);
            BigDecimal hospitalPay = BigDecimal.valueOf(0);
            BigDecimal balancePay = BigDecimal.valueOf(0);

            OrderInfo2App order = orderService.createOrder(PayType.FINANCING, uid, pid,amount, onlinePay, hospitalPay,
            												balancePay, period, periodPay,couponNum, policy, operationT);


            if (order != null) {
            	//更新用户额度 现在的钱数 = 现在的额度 < 总价 ? 0 : 现在的额度 - 总价
            	BigDecimal quotaNow = (quotaLeft.subtract(amount).compareTo(BigDecimal.valueOf(0))<0) ?BigDecimal.valueOf(0):quotaLeft.subtract(amount);
            	userQuotaService.updateUserQuota(userQuota.getUid(), quotaNow);
                ret = JsonUtil.successResultJson(order);
                logger.info("Order_Create_Ret is:{}", ret);
            }



        } catch (Exception e) {
            logger.error("Exception OrderCreate Process!", e);
            ret = JsonUtil.toJson(ErrorCodes.CORE_ERROR, e.getMessage(), null);
        }
    	return ret;
    }




    /**
     * 我的订单
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = { "/list",
            "/list/" }, method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    @ResponseBody
    @LoginRequired
    public String orderList(HttpServletRequest request,
            HttpServletResponse response) {
        String ret = "";
        try {
            Map<String, Object> params = JsonUtil.readMapFromReq(request);
            if (!SignHelper.validateSign(params)) { // 签名验证失败
                return JsonUtil.toJson(ErrorCodes.SIGN_VALIDATE_ERROR, "签名验证失败",
                        null);
            }
            if (params.get("uid") == null) {
                ret = JsonUtil.toJson(ErrorCodes.CORE_PARAM_UNLAWFUL, "参数不合法",
                        null);
                logger.error("参数不合法！ret={}", ret);
                return ret;
            }
            Long uid = Long.parseLong(params.get("uid").toString());
            Integer status = null;
            if(params.get("status") != null){
                status = Integer.parseInt(params.get("status").toString());
            }

            ret = orderService.queryOrdersByUid(uid, status);
        }catch (Exception e) {
            logger.error("Exception OrderList Process!", e);
            ret = JsonUtil.toJson(ErrorCodes.CORE_ERROR, "系统异常", null);
        }
        logger.debug("Order_OrderList_Ret is:{}", ret);
        return ret;
    }


    /**
     * 订单详情
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = { "/detail",
            "/detail/" }, method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    @ResponseBody
    @LoginRequired
    public String orderDetail(HttpServletRequest request,
            HttpServletResponse response) {
        String ret = "";
        try {
            Map<String, Object> params = JsonUtil.readMapFromReq(request);
            if (!SignHelper.validateSign(params)) { // 签名验证失败
                return JsonUtil.toJson(ErrorCodes.SIGN_VALIDATE_ERROR, "签名验证失败",
                        null);
            }
            if (params.get("uid") == null || params.get("order_no") == null) {
                ret = JsonUtil.toJson(ErrorCodes.CORE_PARAM_UNLAWFUL, "参数不合法",
                        null);
                logger.error("参数不合法！ret={}", ret);
                return ret;
            }
            Long uid = Long.parseLong(params.get("uid").toString());
            String orderNo = params.get("order_no").toString();
            String orderinfo = orderService.queryOrderDetailByOid(uid, orderNo);
            logger.info("orderinfo = "+orderinfo);
            ret = orderinfo ;
        } catch (Exception e) {
            logger.error("Exception OrderDetail Process!", e);
            ret = JsonUtil.toJson(ErrorCodes.CORE_ERROR, "系统异常", null);
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * 申请退款
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = { "/refund",
            "/refund/" }, method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    @ResponseBody
    @LoginRequired
    public String refund(HttpServletRequest request,
            HttpServletResponse response) {
        String ret = "";
        try {
            Map<String, Object> params = JsonUtil.readMapFromReq(request);
            if (!SignHelper.validateSign(params)) { // 签名验证失败
                return JsonUtil.toJson(ErrorCodes.SIGN_VALIDATE_ERROR, "签名验证失败",
                        null);
            }
            if (params.get("uid") == null || params.get("order_no") == null) {
                ret = JsonUtil.toJson(ErrorCodes.CORE_PARAM_UNLAWFUL, "参数不合法",
                        null);
                logger.error("参数不合法！ret={}", ret);
                return ret;
            }
            Long uid = Long.parseLong(params.get("uid").toString());
            String orderNo = params.get("order_no").toString();

            int type = 0;
            if(params.get("type") != null){
            	type = Integer.parseInt(params.get("type").toString());
            }
            ret = orderService.refundOrder(uid, orderNo, type);
        } catch (Exception e) {
            logger.error("Exception OrderRefund Process!", e);
            ret = JsonUtil.toJson(ErrorCodes.CORE_ERROR, "系统异常", null);
        }
        logger.info("Order_OrderRefund_Ret is:{}", ret);
        return ret;
    }

    /**
     * 申请退款
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = { "/refund/list/",
            "/refund/list" }, method = {RequestMethod.POST,RequestMethod.GET}, produces = "application/json;charset=utf-8")
    @ResponseBody
    @LoginRequired
    public String refundList(HttpServletRequest request,
                         HttpServletResponse response) {
        String ret = "";
        Map<String, Object> params = JsonUtil.readMapFromReq(request);
        if (!SignHelper.validateSign(params)) { // 签名验证失败
            return JsonUtil.toJson(ErrorCodes.SIGN_VALIDATE_ERROR, "签名验证失败",
                    null);
        }
        if (params.get("uid") == null) {
            ret = JsonUtil.toJson(ErrorCodes.CORE_PARAM_UNLAWFUL, "参数不合法",
                    null);
            logger.error("参数不合法！ret={}", ret);
            return ret;
        }

        Refund refund = new Refund();
        refund.setId(1);
        refund.setOrderNo("mn20160112172938382800ce");
        refund.setRefundPay(BigDecimal.valueOf(1777));
        refund.setCheckFlag(1);
        refund.setCheckTime(new Date());
        refund.getCheckUser();
        refund.setStatus(1);
        refund.setRefundTime(new Date());
        refund.setContent("退款...");
        refund.setCreated(new Date());
        refund.setUpdated(new Date());

        Refund refund2 = new Refund();
        refund2.setId(1);
        refund2.setOrderNo("mn20160115174310517400ce");
        refund2.setRefundPay(BigDecimal.valueOf(2342));
        refund2.setCheckFlag(1);
        refund2.setCheckTime(new Date());
        refund2.getCheckUser();
        refund2.setStatus(1);
        refund2.setRefundTime(new Date());
        refund2.setContent("退款2...");
        refund2.setCreated(new Date());
        refund2.setUpdated(new Date());

        List<Refund> list = Lists.newArrayList();
        list.add(refund);
        list.add(refund2);
        List<Refund2App> data = refundService.create2RefundApps(list);
        ret= JsonUtil.successResultJson(data);
        logger.info("refund list is ret ={}",ret);
        return ret;

    }


    /**
     * 订单取消
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = { "/cancel",
            "/cancel/" }, method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    @ResponseBody
    @LoginRequired
    public String cancel(HttpServletRequest request,
            HttpServletResponse response) {
        String ret = "";
        try {
            Map<String, Object> params = JsonUtil.readMapFromReq(request);
            if (!SignHelper.validateSign(params)) { // 签名验证失败
                return JsonUtil.toJson(ErrorCodes.SIGN_VALIDATE_ERROR, "签名验证失败",
                        null);
            }
            if (params.get("uid") == null || params.get("order_no") == null) {
                ret = JsonUtil.toJson(ErrorCodes.CORE_PARAM_UNLAWFUL, "参数不合法",
                        null);
                logger.error("参数不合法！ret={}", ret);
                return ret;
            }
            Long uid = Long.parseLong(params.get("uid").toString());
            String orderNo = params.get("order_no").toString();
            ret = orderService.cancelOrder(uid, orderNo);
        } catch (Exception e) {
            logger.error("Exception OrderCancel Process!", e);
            ret = JsonUtil.toJson(ErrorCodes.CORE_ERROR, "系统异常", null);
        }
        logger.info("Order_OrderCancel_Ret is:{}", ret);
        return ret;
    }

    /**
     * 获取分期金额
     * 传入uid 和 amount 返回所有的
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = { "/finance_info",
            "/finance_info/" }, method = RequestMethod.POST)
    @ResponseBody
    @LoginRequired
    public String financeInfo(HttpServletRequest request,
            HttpServletResponse response) {
    	String ret = "";
        try {
            Map<String, Object> params = JsonUtil.readMapFromReq(request);
            if (!SignHelper.validateSign(params)) { // 签名验证失败
                return JsonUtil.toJson(ErrorCodes.SIGN_VALIDATE_ERROR, "签名验证失败",
                        null);
            }

            Integer pid = Integer.parseInt(params.get("pid").toString());

            ret = JsonUtil.successResultJson(orderService.calculateFinancing(pid));

        } catch (Exception e) {
            logger.error("Exception Financing Process!", e);
            ret = JsonUtil.toJson(ErrorCodes.CORE_ERROR, "系统异常", null);
        }
        logger.info("Order_Financing_Ret is:{}", ret);
    	return ret;
    }





}







