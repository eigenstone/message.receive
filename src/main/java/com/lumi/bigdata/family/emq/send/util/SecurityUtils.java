package com.lumi.bigdata.family.emq.send.util;

import java.nio.ByteBuffer;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * 安全加密工具类
 * @author xiatiansong
 *
 */
public class SecurityUtils {
	private static final SecureRandom random = new SecureRandom();

	public static byte[] encrypt(String msg, byte[] secret) throws Exception {
		SecretKeySpec skeySpec = new SecretKeySpec(secret, "AES");
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
		byte[] content = msg.getBytes("utf-8");
		byte[] ret = cipher.doFinal(content);
		return ret;
	}

	public static byte[] getSecret(byte[] nonce, String appKey) throws Exception {
		byte[] secretBytes = appKey.getBytes("utf-8");
		byte[] content = new byte[secretBytes.length + nonce.length];
		System.arraycopy(secretBytes, 0, content, 0, secretBytes.length);
		System.arraycopy(nonce, 0, content, secretBytes.length, nonce.length);

		return DigestUtils.sha256(content);
	}

	public static String decrypt(byte[] encryptedMsg, byte[] secret) throws Exception {
		SecretKeySpec skeySpec = new SecretKeySpec(secret, "AES");
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, skeySpec);
		byte[] ret = cipher.doFinal(encryptedMsg);
		return new String(ret, "utf-8");
	}

	@SuppressWarnings("deprecation")
	public static String signAppKey(String method, String uri, String appKey, byte[] secret, String data) {
		String url = String.format("%s&%s&app_key=%s&data=%s&%s", method, uri, appKey, data, Base64.encodeBase64String(secret).trim());
		byte[] signature = DigestUtils.sha(url);
		return Base64.encodeBase64String(signature).trim();
	}

	public static byte[] packN3(int r1, byte[] r2, int r3) {
		byte[] b1 = ByteBuffer.allocate(4).putInt(r1).array();
		byte[] b2 = ByteBuffer.allocate(4).put(r2).array();
		byte[] b3 = ByteBuffer.allocate(4).putInt(r3).array();

		byte[] total = new byte[b1.length + b2.length + b3.length];
		for (int i = 0; i < total.length; ++i) {
			if (i < b1.length) {
				total[i] = b1[i];
			} else if (i < b1.length + b2.length) {
				total[i] = b2[i - b1.length];
			} else {
				total[i] = b3[i - b1.length - b2.length];
			}
		}
		return total;
	}

	public static String getEncodeData(String data, byte[] _nonce, String key) throws Exception {
		byte[] sessionSecurity = getSecret(_nonce, key);
		byte[] dataByte = encrypt(data, sessionSecurity);
		String encryptedData = Base64.encodeBase64String(dataByte);
		return encryptedData;
	}

	public static String getEncodeNonce(byte[] _nonce) throws Exception {
		byte[] base64nonce = Base64.encodeBase64(_nonce);
		return new String(base64nonce, "utf-8");
	}

	/**
	 * 获取nonce
	 * @return
	 */
	public static byte[] getNonce() {
		Long now = System.currentTimeMillis();
		int r1 = now.intValue();
		byte[] bytes = new byte[4];
		random.nextBytes(bytes);
		Double time = new Double(now / 1000 / 60);
		int r3 = time.intValue();
		byte[] _nonce = packN3(r1, bytes, r3);
		return _nonce;
	}
}