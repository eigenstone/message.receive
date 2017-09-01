package com.lumi.bigdata.family.emq.send.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;
import com.lumi.bigdata.family.emq.send.filter.DecryptDataFilter;
import com.lumi.bigdata.family.emq.send.filter.ErrorCode;

public class ReceiveServlet extends HttpServlet{
	
	private static final long serialVersionUID = 597726977686713706L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		//获取解密的数据
		String data = req.getParameter("data");
		//处理
		System.out.println(data);
		//返回
		Map<String, Object> retData = responseResult(ErrorCode.OK);
		//resp.getWriter().write(JSON.toJSONString(retData));
		resp.getOutputStream().write(JSON.toJSONString(retData).getBytes());
	}
	
	/**
	 * 返回单条消息
	 * @param errorCode
	 * @return
	 */
	private Map<String, Object> responseResult(int errorCode) {
		Map<String, Object> json = new HashMap<String, Object>();
		json.put(DecryptDataFilter.API_KEY_CODE, errorCode);
		json.put(DecryptDataFilter.API_KEY_MESSAGE, ErrorCode.getMsg(errorCode));
		json.put(DecryptDataFilter.API_KEY_RESULT, "".intern());
		return json;
	}
}