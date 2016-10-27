package com.lumi.largedata.family.emq.send.util;

import java.net.URLEncoder;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * 用于向miot发送rpc调用，从而调用绿米网关，并返回调用结果
 * @author xiatiansong
 *
 */
public class PushMsgUtil {

	/**
	 * 在失败的情况下重试一次
	 * @param path
	 * @param uri
	 * @param appKey
	 * @param appSecret
	 * @param parameters
	 * @param retry
	 * @return
	 * @throws Exception
	 */
	public static JSONObject processRequest(String path, String uri, String appKey, String appSecret, Map<String, Object> parameters, boolean retry) throws Exception {
		String data = JSON.toJSONString(parameters);
		return processRequest(path, uri, appKey, appSecret, data, retry);
	}

	/**
	 * 封装的处理请求过程
	 * @param path
	 * @param uri
	 * @param appKey
	 * @param appSecret
	 * @param parameters
	 * @param retry
	 * @return
	 * @throws Exception
	 */
	public static JSONObject processRequest(String path, String uri, String appKey, String appSecret, String parameters, boolean retry) throws Exception {
		byte[] _nonce = SecurityUtils.getNonce();
		byte[] base64nonce = Base64.encodeBase64(_nonce);
		byte[] sessionSecurity = SecurityUtils.getSecret(_nonce, appSecret);
		byte[] dataByte = SecurityUtils.encrypt(parameters, sessionSecurity);
		String encryptedData = Base64.encodeBase64String(dataByte);
		String signature = SecurityUtils.signAppKey("POST", uri, appKey, sessionSecurity, encryptedData);
		String url = String.format("%s?data=%s&app_key=%s&signature=%s&_nonce=%s", path, URLEncoder.encode(encryptedData, "utf-8"), appKey, URLEncoder.encode(signature, "utf-8"),
				URLEncoder.encode(new String(base64nonce), "utf-8"));
		byte[] retBYte = HttpClientUtils.requestByteArray(url, null, "POST");
		if (retBYte == null && retry) {
			retBYte = HttpClientUtils.requestByteArray(url, null, "POST");
		}
		return HttpClientUtils.toJsonObject(retBYte, _nonce, appSecret);
	}
}