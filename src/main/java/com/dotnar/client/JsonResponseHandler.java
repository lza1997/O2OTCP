package com.dotnar.client;

import com.dotnar.exception.WXPayException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;
import com.dotnar.util.JsonUtil;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JsonResponseHandler{

	private static Map<String, ResponseHandler<?>> map = new HashMap<String, ResponseHandler<?>>();

	@SuppressWarnings("unchecked")
	public static <T> ResponseHandler<T> createResponseHandler(final Class<T> clazz) throws Exception{

		if(map.containsKey(clazz.getName())){
			return (ResponseHandler<T>)map.get(clazz.getName());
		}else{
			ResponseHandler<T> responseHandler = new ResponseHandler<T>() {
				@Override
				public T handleResponse(HttpResponse response)
						throws ClientProtocolException, IOException {
					int status = response.getStatusLine().getStatusCode();
	                if (status >= 200 && status < 300) {
	                    HttpEntity entity = response.getEntity();
	                    String str = EntityUtils.toString(entity);
						System.out.println("==== 从服务器获取json"+ new Date() +"：" + str +" ====");
						if(JsonUtil.checkIsError(str)){
							try {
								throw new WXPayException(str);
							} catch (WXPayException e) {
								System.out.println("==== 校验失败：" + e.getMessage() + " ====");
							}
						}
						return JsonUtil.parseObject(new String(str.getBytes("iso-8859-1"),"utf-8"), clazz);
	                } else {
	                    throw new ClientProtocolException("Unexpected response status: " + status);
	                }
				}
			};
			map.put(clazz.getName(), responseHandler);
			return responseHandler;
		}
	}

}
