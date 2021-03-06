package com.dotnar.support;

import com.dotnar.api.SnsAPI;
import com.dotnar.bean.BaseResult;
import com.dotnar.bean.SnsToken;
import com.dotnar.bean.sns.Oauth2;
import com.dotnar.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 网页授权access_token
 * Created by chovans on 15/7/21.
 */
public class Oauth2Manager {

    private static Logger logger = LoggerFactory.getLogger(Oauth2Manager.class);

    private static Map<String,String> refreshTokenMap = new LinkedHashMap<String,String>();
    private static Map<String,String> accessTokenMap = new LinkedHashMap<String,String>();
    private static Map<String,String> openIdUnionIdMap = new HashMap<String, String>();

    public static void setRefreshToken(String openid,String refreshToken){
        refreshTokenMap.put(openid,refreshToken);
    }
    public static void setAccessToken(String openid,String accessToken){
        accessTokenMap.put(openid,accessToken);
    }
    public static void setUnionId(String openid,String unionId){
        openIdUnionIdMap.put(openid,unionId);
    }
    public static String getUnionId(String openid){
        return openIdUnionIdMap.get(openid);
    }

    /**
     * 程序启动时从数据库存入内存
     * @param oauth2s
     */
    public static void init(List<Oauth2> oauth2s){
        logger.info("==== 初始化Oauth2长度：" + oauth2s.size() +" ====");
        for(Oauth2 oauth2:oauth2s){
            refreshTokenMap.put(oauth2.getOpenid(),oauth2.getRefresh_token());
            accessTokenMap.put(oauth2.getOpenid(),oauth2.getAccess_token());
            openIdUnionIdMap.put(oauth2.getOpenid(),oauth2.getUnionid());
        }
    }

    /**
     * @param appid
     * @param openid
     * @return
     */
    public static String getAccessToken(String appid,String openid) throws Exception {

        if(accessTokenMap.get(openid) == null)
            return null;

        //检验是否可用
        BaseResult result = null;
        try{

            result = SnsAPI.checkOauth2AccessToken(accessTokenMap.get(openid),openid);
        }catch (Exception e){
            result = new BaseResult();
            result.setErrmsg(e.getMessage());
        }

        //accessToken可用时
        if(result.getErrmsg().equals("ok")){
            logger.info("==== " +
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + ":token = "+accessTokenMap.get(openid)
                    +" 验证可用 ====");
            return accessTokenMap.get(openid);
        }
        //accessToken不可用时
        else
        {
            logger.info("==== " +
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + ":token = "+accessTokenMap.get(openid)
                    +" 验证不可用，重新获取 ====");
            logger.info("==== " +
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + ":token = "+accessTokenMap.get(openid)
                    +" 验证不可用，重新获取 ====");
            //重新获取accessToken
            String refreshToken = refreshTokenMap.get(openid);

            logger.info("==== result:"+ JsonUtil.toJSONString(result)+" ====");
            logger.info("==== openid:"+openid+" ====");
            logger.info("==== refreshToken:" + refreshToken + " ====");

            SnsToken snsToken = SnsAPI.refreshOauth2AccessToken(appid, refreshToken);

            logger.info("==== 获取到的token：" + snsToken +" ====");
            //如果获取到accessToken的情况下，则表明refreshToken未过期
            if(!StringUtils.isEmpty(snsToken.getAccess_token())){
                accessTokenMap.put(openid,snsToken.getAccess_token());
                return snsToken.getAccess_token();
            }
        }

        //refreshToken过期，返回null
        return null;
    }
}
