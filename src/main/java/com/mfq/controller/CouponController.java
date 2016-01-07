package com.mfq.controller;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mfq.bean.app.CouponInfo2App;
import com.mfq.constants.ErrorCodes;
import com.mfq.helper.SignHelper;
import com.mfq.service.CouponService;
import com.mfq.utils.JsonUtil;

@Controller
@RequestMapping("/coupon")
public class CouponController {

    private static final Logger logger = LoggerFactory
            .getLogger(CouponController.class);

    @Resource
    CouponService couponService;

    /**
     * 优惠券列表
     * 
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = { "/list", "/list/" }, method = RequestMethod.POST)
    @ResponseBody
    public String booking(HttpServletRequest request,
            HttpServletResponse response) {
        String ret = "";
        try {
            Map<String, Object> params = JsonUtil.readMapFromReq(request);
            Long uid = Long.parseLong(params.get("uid").toString());
            if (!SignHelper.validateSign(params)) { // 签名验证失败
                return JsonUtil.toJson(ErrorCodes.SIGN_VALIDATE_ERROR, "签名验证失败",
                        null);
            }
            if (uid == null || uid == 0 ) { // 参数异常
                return JsonUtil.toJson(ErrorCodes.CORE_PARAM_UNLAWFUL, "参数异常",
                        null);
            }
            int status = -1;
            if(params.get("status") != null){
            	status = Integer.parseInt(params.get("status").toString());
            }
            List<CouponInfo2App> data = couponService.queryCoupons(uid, status);
            ret = JsonUtil.successResultJson(data);
        } catch (Exception e) {
            logger.error("Exception CouponList Process!", e);
            ret = JsonUtil.toJson(ErrorCodes.CORE_ERROR, "系统异常", null);
        }
        logger.info("CouponList_Ret is:{}", ret);
        return ret;
    }

    //TODO 优惠券删除功能

}
