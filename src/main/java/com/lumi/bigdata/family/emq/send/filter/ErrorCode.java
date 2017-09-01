package com.lumi.bigdata.family.emq.send.filter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lumichenbingzhi
 *
 */
public class ErrorCode {
	// code':0,'message':'ok'
	public static int OK = 0;
	// 'code':-1,'message':'permission denied'
	public static int PERMISSION_DENIED = -1;
	// 'code':-2,'message':'device offline'
	public static int DEVICE_OFFLINE = -2;
	// 'code':-3,'message':'request time out'
	public static int TIME_OUT = -3;
	// 'code':-4,'message':'internal exception occurred from server'
	public static int OCCURRED_FROM_SERVER = -4;
	// 'code':-5,'message':'internal exception occurred from device'
	public static int OCCURRED_FROM_DEVICE = -5;
	// 'code':-6,'message':'invalid request'
	public static int INVALID = -6;
	// 'code':-9,'message':'unknown error'
	public static int UNKNOWN = -9;
	
	private static Map<Integer, String> msgs = new HashMap<Integer, String>();
	
	static{
		msgs.put(OK, "ok");
		msgs.put(PERMISSION_DENIED, "permission denied");
		msgs.put(DEVICE_OFFLINE, "device offline");
		msgs.put(DEVICE_OFFLINE, "request time out");
		msgs.put(OCCURRED_FROM_SERVER, "internal exception occurred from server");
		msgs.put(OCCURRED_FROM_DEVICE, "internal exception occurred from device");
		msgs.put(INVALID, "invalid request");
		msgs.put(UNKNOWN, "unknown error");
	}
	
	public static String getMsg(int code){
		return msgs.get(code);
	}
}