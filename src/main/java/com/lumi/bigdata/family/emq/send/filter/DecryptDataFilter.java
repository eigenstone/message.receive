package com.lumi.bigdata.family.emq.send.filter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.lumi.bigdata.family.emq.send.util.SecurityUtils;

/**
 * 拦截器：
 * 用于解密数据，验证数据完整性，验证没问题后请求会转到对应的servlet或者controller
 * 
 */
public class DecryptDataFilter implements Filter {
	private static final Logger LOG = Logger.getLogger(DecryptDataFilter.class);
	public static final String APP_KEY = "app_key";
	//填入你的appsecret
	public static final String APP_SECRET = "h3fnX+uDWSPeYv2uyuAJUg==";
	public static final String API_ARG_DATA = "data";
	public static final String API_ARG_SIGNATURE = "signature";
	public static final String API_ARG_NONCE = "_nonce";
	public static final String API_KEY_CODE = "code";
	public static final String API_KEY_MESSAGE = "message";
	public static final String API_KEY_RESULT = "result";
	public static final String API_ARG_DID = "did";

	public void init(FilterConfig filterConfig) throws ServletException {
		Locale.setDefault(Locale.CHINA);
		TimeZone.setDefault(TimeZone.getTimeZone("GMT+8:00"));
	}

	private void responseFailureResult(HttpServletResponse httpResponse, int errorCode) throws IOException {
		Map<String, Object> json = new HashMap<String, Object>();
		try {
			json.put(API_KEY_CODE, errorCode);
			json.put(API_KEY_MESSAGE, ErrorCode.getMsg(errorCode));
			json.put(API_KEY_RESULT, new JSONObject());
		} catch (JSONException e) {
		}
		httpResponse.getWriter().write(json.toString());
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
		try {
			final HttpServletRequest httpRequest = (HttpServletRequest) request;
			ResponseWrapper responseWrapper = new ResponseWrapper((HttpServletResponse) response);
			// 获取该请求的对应厂商的app key
			String appKey = httpRequest.getParameter(APP_KEY);
			System.out.println(appKey);

			if (appKey == null) {
				responseFailureResult(responseWrapper, ErrorCode.OCCURRED_FROM_DEVICE);
				return;
			}

			String encryptedData = httpRequest.getParameter(API_ARG_DATA);
			String base64nonce = httpRequest.getParameter(API_ARG_NONCE);
			byte[] databin = Base64.decodeBase64(encryptedData);
			byte[] nonceBytes = Base64.decodeBase64(base64nonce);
			//根据appKey，从数据库或者缓存获取对应厂商的app_secret，这里示例直接给出秘钥
			String appSecret = APP_SECRET;
			
			/**
			// 检查时间间隔
			ByteBuffer buf = (ByteBuffer) ByteBuffer.allocate(4).put(nonceBytes, (nonceBytes.length - 4), 4).flip();
			int reqTs = buf.getInt();
			int current = (int) (System.currentTimeMillis() / 1000 / 60);
			if (Math.abs(reqTs - current) > 2) {
				responseFailureResult(responseWrapper, ErrorCode.INVALID);
				return;
			}
			**/
			
			byte[] secret = SecurityUtils.getSecret(nonceBytes, appSecret);

			/**
			//验证签名
			String method = httpRequest.getMethod();
			String uri = httpRequest.getRequestURI();
			String signature = httpRequest.getParameter(API_ARG_SIGNATURE);
			String realSign = sign(method, uri, appKey, secret, encryptedData);
			if (!StringUtils.equals(signature, realSign)) {
				responseFailureResult(responseWrapper, ErrorCode.INVALID);
				return;
			}
			 **/
			
			String dataDecrypted = SecurityUtils.decrypt(databin, secret);

			chain.doFilter(new RequestWrapper(httpRequest, API_ARG_DATA, dataDecrypted), responseWrapper);

			encryptedData = null;
			base64nonce = null;
			databin = null;
			nonceBytes = null;
			dataDecrypted = null;
			// 加密返回結果
			String responseText = new String(responseWrapper.getDataStream()).intern();
			if (StringUtils.isNotEmpty(responseText)) {
				try {
					String encryptedText = Base64.encodeBase64String(SecurityUtils.encrypt(responseText, secret));
					response.getWriter().write(encryptedText);
					encryptedText = null;
				} catch (SecurityException e) {
					LOG.error("exit parameters decryptor filter cause by encrypt response", e);
					return;
				} catch (InvalidKeyException e) {
					LOG.error(e.getMessage(), e);
					return;
				} catch (NoSuchAlgorithmException e) {
					LOG.error(e.getMessage(), e);
					return;
				}
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private static String sign(String method, String uri, String appKey, byte[] secret, String data) {
		String url = String.format("%s&%s&app_key=%s&data=%s&%s", method, uri, appKey, data, Base64.encodeBase64String(secret).trim());
		byte[] signature = DigestUtils.sha(url);
		return Base64.encodeBase64String(signature).trim();
	}

	public void destroy() {
	}
}