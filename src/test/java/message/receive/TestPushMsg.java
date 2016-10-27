package message.receive;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.lumi.largedata.family.emq.send.util.PushMsgUtil;

/**
 * 测试:
 * 使用open接口添加米聊号
 * appKey: 应用app key
 * appSecret:应用app secret
 * @author xiatiansong
 *
 */
public class TestPushMsg {
	//需填入由绿米生成的密钥对,用于加密数据
	static String appKey = "填入appkey";
	static String appSecret = "填入appsecret";

	public static void main(String[] args) throws Exception {
		//启动本项目,尝试往本项目发数据
		JSONObject jsonObject = sendMessage();
		System.out.println(jsonObject);
		System.out.println(jsonObject.getString("result"));

		//测试接口获取数据
		JSONObject jsonObject2 = addMinum();
		System.out.println(jsonObject2);
		System.out.println(jsonObject2.getString("result"));

		JSONObject jsonObject3 = getDeviceInfo();
		System.out.println(jsonObject3);
		System.out.println(jsonObject3.getString("result"));
	}

	/**
	 * 添加要推送数据的米聊号
	 * @return
	 * @throws Exception
	 */
	private static JSONObject addMinum() throws Exception {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("minum", "123456789");
		return PushMsgUtil.processRequest("http://open.aqara.cn/open/v1/user/minum", "/open/v1/user/minum", appKey,
				appSecret, parameters, true);
	}

	/**
	 * 查询设备信息,如设备名称和父设备名称
	 * @return
	 * @throws Exception
	 */
	private static JSONObject getDeviceInfo() throws Exception {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("did", "lumi.123456789");
		return PushMsgUtil.processRequest("http://open.aqara.cn/open/v1/device/getinfo", "/open/v1/device/getinfo",
				appKey, appSecret, parameters, true);
	}

	/**
	 * 测试从绿米服务端推送数据给当前示例：
	 * 1、启动本项目，以root根目录形式部署，即访问时是以 http://ip:port/  访问本项目
	 * 2、填入推送地址到下面的示例中，注意 miSensorData 这个是处理数据的servlet映射访问地址，在正式项目中访问地址会根据你配置的地址有所变化
	 * 3、看本类main函数，运行发送消息函数，查看返回和控制台打印
	 * 4、控制台有打印接收到的数据，返回json正常即认为示例运行正常。
	 * 5、sendMessage() 注释部分是我们测试的使用此函数向php的客户端推送数据，不是php的不需要关注。
	 * @return
	 * @throws Exception
	 */
	private static JSONObject sendMessage() throws Exception {

		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("did", "lumi.1234567891111");
		return PushMsgUtil.processRequest("http://ip:port/miSensorData", "/miSensorData", appKey, appSecret, parameters,
				true);
		/**
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("did", "lumi.1234567891111");
		return PushMsgUtil.processRequest("http://php.aqara.cn/pushData.php", "/pushData.php", appKey, appSecret, parameters, true);
		**/
	}
}