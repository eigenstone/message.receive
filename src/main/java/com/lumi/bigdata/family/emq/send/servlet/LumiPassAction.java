package com.lumi.bigdata.family.emq.send.servlet;
/**
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lumi.bigdata.family.emq.send.filter.DecryptDataFilter;
import com.lumi.bigdata.family.emq.send.filter.ErrorCode;

//spring mvc的crontoller示例
@Controller
@RequestMapping("lumipass")
public class LumiPassAction {

	@RequestMapping(value = "action", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> action(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> map = null;
		try {
			String data = request.getParameter("data");
			JSONObject jsonObject = JSON.parseObject(data);
			//处理结果
			Map<String, Object> retMap = process(jsonObject);
			if (retMap != null) {
				//正常返回的数据部分放在result
				map = responseResult(ErrorCode.OK, "Process OK".intern(), retMap);
			} else {
				map = responseResult(ErrorCode.INVALID, "data type is invalid.", null);
			}
		} catch (Exception e) {
			map = responseResult(ErrorCode.OCCURRED_FROM_SERVER, "internal exception occurred from server", null);
		}
		return map;
	}

	private Map<String, Object> responseResult(int errorCode, String message, Map<String, Object> data) {
		Map<String, Object> json = new HashMap<String, Object>();
		json.put(DecryptDataFilter.API_KEY_CODE, errorCode);
		json.put(DecryptDataFilter.API_KEY_MESSAGE, message);
		json.put(DecryptDataFilter.API_KEY_RESULT, data);
		return json;
	}
}
**/