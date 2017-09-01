package com.lumi.bigdata.family.emq.send.util;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.CodingErrorAction;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * 采用连接池来管理HttpClient
 * 
 * @author xiatiansong
 *
 */
public class HttpClientUtils {

	private static final Log LOG = LogFactory.getLog(HttpClientUtils.class);

	private static final JSONObject JO404 = new JSONObject();
	static {
		JO404.put("code", 404);
		JO404.put("message", "404 error");
	}

	/**
	 * The default timeout for a connected socket.
	 */
	public static final int DEFAULT_SOCKET_TIMEOUT_MS = 50 * 1000;

	/**
	 * The default timeout for establishing a connection.
	 */
	public static final int DEFAULT_CONNECTION_TIMEOUT_MS = 50 * 1000;

	/**
	 * max connections a client can have at same time
	 */
	private static final int DEFAULT_MAX_CONNECTIONS = 100;

	private static final int DEFAULT_MAX_CONNECTIONS_ROUTE = 100;

	private static final String ENCODE = "utf-8";

	private static PoolingHttpClientConnectionManager connManager = null;
	private static CloseableHttpClient httpclient = null;

	static {
		try {
			SSLContext sslContext = SSLContexts.custom().build();
			sslContext.init(null, new TrustManager[] { new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			} }, null);

			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create().register("http", PlainConnectionSocketFactory.INSTANCE)
					.register("https", new SSLConnectionSocketFactory(sslContext, new HostnameVerifier() {
						public boolean verify(String hostname, SSLSession session) {
							return true;
						}
					})).build();

			connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
			// 设置超时
			RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT_MS).setSocketTimeout(DEFAULT_SOCKET_TIMEOUT_MS)
					.setConnectionRequestTimeout(DEFAULT_CONNECTION_TIMEOUT_MS).build();

			// 设置http cleint参数
			httpclient = HttpClients.custom().setConnectionManager(connManager).setDefaultRequestConfig(requestConfig).setRetryHandler(new DefaultHttpRequestRetryHandler(3, false)).build();
			// Create socket configuration
			SocketConfig socketConfig = SocketConfig.custom().setTcpNoDelay(true).build();
			connManager.setDefaultSocketConfig(socketConfig);
			// Create message constraints
			MessageConstraints messageConstraints = MessageConstraints.custom().setMaxHeaderCount(10000).setMaxLineLength(100000).build();
			// Create connection configuration
			ConnectionConfig connectionConfig = ConnectionConfig.custom().setMalformedInputAction(CodingErrorAction.IGNORE).setUnmappableInputAction(CodingErrorAction.IGNORE).setCharset(Consts.UTF_8)
					.setMessageConstraints(messageConstraints).build();
			connManager.setDefaultConnectionConfig(connectionConfig);
			connManager.setMaxTotal(DEFAULT_MAX_CONNECTIONS);
			connManager.setDefaultMaxPerRoute(DEFAULT_MAX_CONNECTIONS_ROUTE);
		} catch (KeyManagementException e) {
			LOG.error("KeyManagementException", e);
		} catch (NoSuchAlgorithmException e) {
			LOG.error("NoSuchAlgorithmException", e);
		}
	}

	public static CloseableHttpClient getTimeoutHttpClient() {
		return httpclient;
	}

	public static CloseableHttpClient getHttpClient() {
		return httpclient;
	}

	/**
	 * <pre>
	 * 发送请求获取byte数组结果
	 * </pre>
	 * 
	 * @param uri
	 * @param queryParameters
	 * @param method
	 * @return
	 * @throws Exception
	 */
	public static byte[] requestByteArray(String uri, Map<String, Object> queryParameters, String method) throws Exception {
		return requestByteArray(uri, null, queryParameters, method);
	}

	/**
	 * 发送请求获取response返回
	 * @param uri
	 * @param headers
	 * @param queryParameters
	 * @param method
	 * @return
	 * @throws Exception
	 */
	public static Map<String, Object> requestResponse(String uri, Map<String, String> headers, Map<String, Object> queryParameters, String method) throws Exception {
		String requestMethod = method == null ? "get" : method.toLowerCase();
		Map<String, Object> map = new HashMap<>();
		CloseableHttpResponse httpResponse = null;
		switch (requestMethod) {
		case "get":
			HttpGet httpGet = new HttpGet();
			try {
				httpResponse = get(httpGet, uri, headers, queryParameters);
				map.put("header", httpResponse.getAllHeaders());
				map.put("entity", extractByte(uri, method, httpResponse));
			} finally {
				httpGet.releaseConnection();
			}
			break;
		case "post":
			HttpPost httpPost = new HttpPost(uri);
			try {
				httpResponse = post(httpPost, uri, headers, queryParameters);
				map.put("header", httpResponse.getAllHeaders());
				map.put("entity", extractByte(uri, method, httpResponse));
			} finally {
				httpPost.releaseConnection();
			}
			break;
		default:
			break;
		}
		return map;
	}

	/**
	 * <pre>
	 * 发送请求获取byte数组结果
	 * </pre>
	 * 
	 * @param uri
	 * @param queryParameters
	 * @param method
	 * @return
	 * @throws Exception
	 */
	public static byte[] requestByteArray(String uri, Map<String, String> headers, Map<String, Object> queryParameters, String method) throws Exception {
		String requestMethod = method == null ? "get" : method.toLowerCase();
		CloseableHttpResponse httpResponse = null;
		byte[] responseByte = null;
		switch (requestMethod) {
		case "get":
			HttpGet httpGet = new HttpGet();
			try {
				httpResponse = get(httpGet, uri, headers, queryParameters);
				responseByte = extractByte(uri, method, httpResponse);
			} finally {
				httpGet.releaseConnection();
			}
			break;
		case "post":
			HttpPost httpPost = new HttpPost(uri);
			try {
				httpResponse = post(httpPost, uri, headers, queryParameters);
				responseByte = extractByte(uri, method, httpResponse);
			} finally {
				httpPost.releaseConnection();
			}
			break;
		default:
			break;
		}
		return responseByte;
	}

	/**
	 * <pre>
	 * 发送请求获取string结果
	 * </pre>
	 * 
	 * @param uri
	 * @param queryParameters
	 * @param method
	 * @return
	 * @throws Exception
	 */
	public static String requestString(String uri, Map<String, String> headers, Map<String, Object> queryParameters, String method, boolean isJson) throws Exception {
		String requestMethod = method == null ? "get" : method.toLowerCase();
		CloseableHttpResponse httpResponse = null;
		String responseString = null;
		switch (requestMethod) {
		case "get":
			HttpGet httpGet = new HttpGet();
			try {
				httpResponse = get(httpGet, uri, headers, queryParameters);
				responseString = extractStr(uri, httpResponse);
			} finally {
				httpGet.releaseConnection();
			}
			break;
		case "post":
			HttpPost httpPost = new HttpPost(uri);
			try {
				httpResponse = post(httpPost, uri, headers, queryParameters, isJson);
				responseString = extractStr(uri, httpResponse);
			} finally {
				httpPost.releaseConnection();
			}
			break;
		default:
			break;
		}
		LOG.debug(String.format("[%s]%s:%s\n[response]%s", requestMethod, uri, queryParameters, responseString));
		return responseString;
	}

	private static byte[] extractByte(String uri, String method, CloseableHttpResponse httpResponse) throws Exception {
		byte[] responseByte = null;
		try {
			if (httpResponse != null) {
				HttpEntity entity = httpResponse.getEntity();
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				try {
					if (statusCode == HttpStatus.SC_OK) {
						responseByte = EntityUtils.toByteArray(entity);
					} else {
						LOG.error(String.format("request url: %s return error code: %s", uri, statusCode));
					}
				} finally {
					if (entity != null) {
						entity.getContent().close();
					}
				}
			}
		} finally {
			closeResponseEntity(httpResponse);
		}
		return responseByte;
	}

	/**
	 * <pre>
	 * 发送请求获取string结果
	 * </pre>
	 * 
	 * @param uri
	 * @param queryParameters
	 * @param method
	 * @return
	 * @throws Exception
	 */
	public static String requestString(String uri, Map<String, Object> queryParameters, String method) throws Exception {
		return requestString(uri, null, queryParameters, method, false);
	}

	/**
	 * <pre>
	 * 发送请求获取string结果
	 * </pre>
	 * 
	 * @param uri
	 * @param queryParameters
	 * @param method
	 * @return
	 * @throws Exception
	 */
	public static String requestString(String uri, Map<String, String> headers, Map<String, Object> queryParameters, String method) throws Exception {
		return requestString(uri, headers, queryParameters, method, false);
	}

	private static String extractStr(String uri, CloseableHttpResponse httpResponse) {
		String responseString = null;
		try {
			if (httpResponse != null) {
				HttpEntity entity = httpResponse.getEntity();
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				try {
					if (statusCode == HttpStatus.SC_OK) {
						responseString = EntityUtils.toString(entity, ENCODE);
					} else {
						LOG.error(String.format("request url: %s return error code: %s", uri, statusCode));
					}
				} finally {
					if (entity != null) {
						entity.getContent().close();
					}
				}
			}
		} catch (Exception e) {
			LOG.error(String.format("[HttpClientsUtils Get] get response error, url:%s", uri), e);
			return responseString;
		} finally {
			closeResponseEntity(httpResponse);
		}
		return responseString;
	}

	/**
	 * <pre>
	 * get请求获取数据
	 * uri:请求地址
	 * queryParameters:参数
	 * </pre>
	 * 
	 * @param uri
	 * @param queryParameters
	 * @return
	 * @throws Exception
	 */
	public static CloseableHttpResponse get(String uri, Map<String, Object> queryParameters) throws Exception {
		HttpGet httpGet = new HttpGet();
		try {
			CloseableHttpResponse response = get(httpGet, uri, null, queryParameters);
			return response;
		} finally {
			httpGet.releaseConnection();
		}
	}

	/**
	 * <pre>
	 * get请求获取数据
	 * uri:请求地址
	 * queryParameters:参数
	 * </pre>
	 * 
	 * @param uri
	 * @param queryParameters
	 * @return
	 * @throws Exception
	 */
	public static CloseableHttpResponse get(HttpGet httpGet, String uri, Map<String, String> headers, Map<String, Object> queryParameters) throws Exception {
		try {
			StringBuffer buf = new StringBuffer();
			buf.append(uri);
			// 设置头部
			boolean hasHeader = null != headers && !headers.isEmpty();
			if (hasHeader) {
				for (Entry<String, String> entry : headers.entrySet()) {
					httpGet.addHeader(entry.getKey(), entry.getValue());
				}
			}
			boolean hasParameter = null != queryParameters && !queryParameters.isEmpty();
			if (hasParameter) {
				int i = 0;
				for (Entry<String, Object> entry : queryParameters.entrySet()) {
					if (i == 0 && !uri.contains("?")) {
						buf.append("?");
					} else {
						buf.append("&");
					}
					i++;
					String value = "";
					Object obj = entry.getValue();
					if (!(obj instanceof String)) {
						value = JSON.toJSONString(obj, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteNullStringAsEmpty);
					} else {
						value = obj.toString();
					}
					buf.append(entry.getKey()).append('=').append(URLEncoder.encode(value, "UTF-8"));
				}
			}
			httpGet.setURI(new URI(buf.toString()));
			CloseableHttpResponse response = getHttpClient().execute(httpGet);
			return response;
		} catch (ClientProtocolException e) {
			LOG.error(String.format("[HttpClientsUtils Get] get response error, url:%s", uri), e);
		} catch (IOException e) {
			LOG.error(String.format("[HttpClientsUtils Get] get response error, url:%s", uri), e);
		}
		return null;
	}

	/**
	 * <pre>
	 * Post请求获取数据
	 * uri:请求地址
	 * queryParameters:参数static {
		JO404.put("code", 404);
		JO404.put("message", "404 error");
	}
	 * </pre>
	 * 
	 * @param uri
	 * @param queryParameters
	 * @param keepClient
	 * @return
	 * @throws Exception
	 */
	public static CloseableHttpResponse post(HttpPost httpPost, String uri, Map<String, Object> queryParameters) throws Exception {
		return post(httpPost, uri, null, queryParameters);
	}

	public static CloseableHttpResponse post(String uri, Map<String, Object> queryParameters) throws Exception {
		HttpPost httpPost = new HttpPost(uri);
		try {
			CloseableHttpResponse response = post(httpPost, uri, null, queryParameters);
			return response;
		} finally {
			httpPost.releaseConnection();
		}
	}

	/**
	 * <pre>
	 * Post请求获取数据
	 * uri:请求地址
	 * queryParameters:参数static {
		JO404.put("code", 404);
		JO404.put("message", "404 error");
	}
	 * </pre>
	 * 
	 * @param uri
	 * @param queryParameters
	 * @param keepClient
	 * @return
	 * @throws Exception
	 */
	public static CloseableHttpResponse post(HttpPost httpPost, String uri, Map<String, String> headers, Map<String, Object> queryParameters) throws Exception {
		return post(httpPost, uri, headers, queryParameters, false);
	}

	/**
	 * <pre>
	 * Post请求获取数据
	 * uri:请求地址
	 * queryParameters:参数static {
		JO404.put("code", 404);
		JO404.put("message", "404 error");
	}
	 * </pre>
	 * 
	 * @param uri
	 * @param queryParameters
	 * @param keepClient
	 * @return
	 * @throws Exception
	 */
	public static CloseableHttpResponse post(HttpPost httpPost, String uri, Map<String, String> headers, Map<String, Object> queryParameters, boolean isJson) throws Exception {
		try {
			// 设置头部
			boolean hasHeader = null != headers && !headers.isEmpty();
			if (hasHeader) {
				for (Entry<String, String> entry : headers.entrySet()) {
					httpPost.addHeader(entry.getKey(), entry.getValue());
				}
			}
			// 设置参数
			boolean hasParameter = null != queryParameters && !queryParameters.isEmpty();
			if (hasParameter) {
				if (isJson) {
					String json = JSON.toJSONString(queryParameters, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteNullStringAsEmpty);
					StringEntity entity = new StringEntity(json, Consts.UTF_8);
					entity.setContentType("application/json");
					entity.setContentEncoding("UTF-8");
					httpPost.setEntity(entity);
				} else {
					List<NameValuePair> params = new ArrayList<NameValuePair>();
					for (Entry<String, Object> entry : queryParameters.entrySet()) {
						String value = "";
						Object obj = entry.getValue();
						if (!(obj instanceof String)) {
							value =  JSON.toJSONString(obj, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteNullStringAsEmpty);
						} else {
							value = obj.toString();
						}
						params.add(new BasicNameValuePair(entry.getKey(), value));
					}
					httpPost.setEntity(new UrlEncodedFormEntity(params, Consts.UTF_8));
				}
			}
			CloseableHttpResponse response = getHttpClient().execute(httpPost);
			return response;
		} catch (Exception e) {
			LOG.error(String.format("[HttpClientsUtils Get] get response error, url:%s,params:%s", uri, JSON.toJSONString(queryParameters, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteNullStringAsEmpty)), e);
		}
		return null;
	}

	/**
	 * 以json形式发送数据
	 */
	public static String postWithJson(String uri, Map<String, String> headers, String json) throws Exception {
		return postWithContent(uri, headers, json, "application/json");
	}

	/**
	 * <pre>
	 * Post请求获取数据
	 * uri:请求地址
	 * queryParameters:参数static {
		JO404.put("code", 404);
		JO404.put("message", "404 error");
	}
	 * </pre>
	 * 
	 * @param uri
	 * @param queryParameters
	 * @param keepClient
	 * @return
	 * @throws Exception
	 */
	public static String postWithContent(String uri, Map<String, String> headers, String content, String contType) throws Exception {
		HttpPost httpPost = new HttpPost(uri);
		String responseString = null;
		try {
			// 设置头部
			boolean hasHeader = null != headers && !headers.isEmpty();
			if (hasHeader) {
				for (Entry<String, String> entry : headers.entrySet()) {
					httpPost.addHeader(entry.getKey(), entry.getValue());
				}
			}
			StringEntity entity = new StringEntity(content, Consts.UTF_8);
			entity.setContentType(contType);
			entity.setContentEncoding("UTF-8");
			httpPost.setEntity(entity);
			CloseableHttpResponse response = getHttpClient().execute(httpPost);
			responseString = extractStr(uri, response);
			return responseString;
		} catch (Exception e) {
			LOG.error(String.format("[HttpClientsUtils Post] get response error, url:%s,params:%s", uri, content), e);
		} finally {
			httpPost.releaseConnection();
		}
		return null;
	}

	public static void release() {
		if (connManager != null) {
			connManager.shutdown();
		}
	}

	public static JSONObject toJsonObject(byte[] response, byte[] nonce, String appSecret) throws Exception {
		JSONObject jsonObject = null;
		try {
			byte[] sessionSecurity = SecurityUtils.getSecret(nonce, appSecret);
			byte[] decodeBase = Base64.decodeBase64(response);
			String decrypt = SecurityUtils.decrypt(decodeBase, sessionSecurity);
			jsonObject = JSON.parseObject(decrypt);
			return jsonObject;
		} catch (Exception e) {
			LOG.error(response, e);
		}
		return JO404;
	}

	public static String toStringResult(byte[] response, byte[] nonce, String appSecret) throws Exception {
		try {
			byte[] sessionSecurity = SecurityUtils.getSecret(nonce, appSecret);
			byte[] decodeBase = Base64.decodeBase64(response);
			String decrypt = SecurityUtils.decrypt(decodeBase, sessionSecurity);
			return decrypt;
		} catch (Exception e) {
			LOG.error(response, e);
		}
		return null;
	}

	public static void closeResponseEntity(HttpResponse response) {
		if (response == null)
			return;
		EntityUtils.consumeQuietly(response.getEntity());
	}

	public static String postFile(String url, Map<String, String> headers, String filekey, String filename, byte[] data) throws Exception {
		String responseString = null;
		HttpPost post = new HttpPost(url);
		try {
			// 设置头部
			boolean hasHeader = null != headers && !headers.isEmpty();
			if (hasHeader) {
				for (Entry<String, String> entry : headers.entrySet()) {
					post.addHeader(entry.getKey(), entry.getValue());
				}
			}
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			builder.addBinaryBody("file", data, ContentType.APPLICATION_OCTET_STREAM, filename);
			builder.addTextBody("name", filekey);
			builder.addTextBody("filename", filename);
			HttpEntity entity = builder.build();
			post.setEntity(entity);
			CloseableHttpResponse httpResponse = getHttpClient().execute(post);
			if (httpResponse != null) {
				HttpEntity responseEntity = httpResponse.getEntity();
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				try {
					if (statusCode == HttpStatus.SC_OK) {
						responseString = EntityUtils.toString(responseEntity, ENCODE);
					} else {
						LOG.error(String.format("request url: %s return error code: %s", url, statusCode));
					}
				} finally {
					if (responseEntity != null) {
						responseEntity.getContent().close();
					}
					closeResponseEntity(httpResponse);
				}
			}
		} catch (Exception e) {
			LOG.error(String.format("[HttpClientsUtils upload file] get response error, url:%s", url), e);
		} finally {
			post.releaseConnection();
		}
		return responseString;
	}

	public static String postFile(String url, Map<String, String> headers, String filename, byte[] data) throws Exception {
		String responseString = null;
		HttpPost post = new HttpPost(url);
		try {
			// 设置头部
			boolean hasHeader = null != headers && !headers.isEmpty();
			if (hasHeader) {
				for (Entry<String, String> entry : headers.entrySet()) {
					post.addHeader(entry.getKey(), entry.getValue());
				}
			}
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.addBinaryBody("media", data, ContentType.MULTIPART_FORM_DATA, filename);
			HttpEntity entity = builder.build();
			post.setEntity(entity);
			CloseableHttpResponse httpResponse = getHttpClient().execute(post);
			if (httpResponse != null) {
				HttpEntity responseEntity = httpResponse.getEntity();
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				try {
					if (statusCode >= 200 && statusCode <= 300) {
						responseString = EntityUtils.toString(responseEntity, ENCODE);
					} else {
						LOG.error(String.format("request url: %s return error code: %s", url, statusCode));
					}
				} finally {
					if (responseEntity != null) {
						responseEntity.getContent().close();
					}
					closeResponseEntity(httpResponse);
				}
			}
		} finally {
			post.releaseConnection();
		}
		return responseString;
	}

}