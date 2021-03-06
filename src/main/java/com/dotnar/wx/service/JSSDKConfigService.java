package com.dotnar.wx.service;

import com.dotnar.api.ShorturlAPI;
import com.dotnar.bean.Shorturl;
import com.dotnar.bean.js.JSInitialize;
import com.dotnar.bean.js.JSInitializeResult;
import com.dotnar.exception.WXPayException;
import com.dotnar.support.TicketManager;
import com.dotnar.support.TokenManager;
import com.dotnar.util.JsUtil;
import com.dotnar.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.UUID;

/**
 * 初始化jssdk
 * Created by chovans on 15/7/18.
 */
@Service
public class JSSDKConfigService {

    private static Logger logger = LoggerFactory.getLogger(JSSDKConfigService.class);

    /**
     * jssdk中wx的初始化参数
     * 参数有：  appid       第三方用户唯一凭证
     * secret      第三方用户唯一凭证密钥，即appsecret
     * url         访问网页的URL，不包含#及其后面部分
     *
     * @param jsonObj
     * @return
     */
    public static String jsConfig(String jsonObj) {

        JSInitialize jsInitialize = JsonUtil.parseObject(jsonObj, JSInitialize.class);
        String ticket = TicketManager.getTicket(jsInitialize.getAppid());

        JSInitializeResult result = new JSInitializeResult();

        logger.info("jsConfig:" + jsonObj);
        try {
            if (StringUtils.isEmpty(ticket)) {

                logger.info("=== 获取新的accesstoken ====");

                TokenManager.init(jsInitialize.getAppid(), jsInitialize.getSecret());
                while (TokenManager.getToken(jsInitialize.getAppid()) == null) {
                    Thread.sleep(500);
                }
                TicketManager.init(jsInitialize.getAppid());

                while (TicketManager.getTicket(jsInitialize.getAppid()) == null) {
                    Thread.sleep(500);
                }

                logger.info("==== 获取的token和ticket:" + TokenManager.getToken(jsInitialize.getAppid()) + " ::: " +
                        TicketManager.getTicket(jsInitialize.getAppid()) + " ====");

            }

            result.setAppId(jsInitialize.getAppid());
            result.setNonceStr(UUID.randomUUID().toString().replaceAll("-", "").substring(0, 20));
            result.setTimestamp(String.valueOf(new Date().getTime() / 1000));

            String sign = JsUtil.generateConfigSignature(result.getNonceStr(), TicketManager.getTicket(jsInitialize.getAppid()),
                    result.getTimestamp(), jsInitialize.getUrl());

            result.setSignature(sign);

        } catch (Exception e) {
            return JsonUtil.toJSONString(new WXPayException(e.getMessage()));
        }

        return JsonUtil.toJSONString(result);

    }


    /**
     * 长地址转短地址
     *
     * @param appid
     * @param long_url
     * @return
     */
    public static String shortUrl(String appid, String long_url) {
        Shorturl shorturl = null;
        String access_token = TokenManager.getToken(appid);

        if (StringUtils.isEmpty(access_token)) {
            return JsonUtil.toJSONString(new WXPayException("appid is error!"));
        }

        logger.info("transform url" + long_url);

        try {
            shorturl = ShorturlAPI.shorturl(access_token, long_url);
        } catch (Exception e) {
            return JsonUtil.toJSONString(new WXPayException(e.getMessage()));
        }

        if (StringUtils.isEmpty(shorturl.getShort_url())) {
            return JsonUtil.toJSONString(new WXPayException("Unknow exception!"));
        }

        return JsonUtil.toJSONString(shorturl);
    }

}
