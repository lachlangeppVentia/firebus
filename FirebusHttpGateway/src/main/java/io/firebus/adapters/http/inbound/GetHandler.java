package io.firebus.adapters.http.inbound;

import java.io.OutputStream;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.firebus.Firebus;
import io.firebus.Payload;
import io.firebus.adapters.http.InboundReqRespHandler;
import io.firebus.utils.DataMap;

public class GetHandler extends InboundReqRespHandler 
{
	public GetHandler(DataMap c, Firebus f) 
	{
		super(c, f);
	}

	protected Payload processRequest(HttpServletRequest req) throws Exception
	{
		DataMap fbReq = new DataMap();
		String path = req.getRequestURI();
		int handlerPathLen = req.getContextPath().length() + getHttpHandlerPath().length();
		String shortPath = handlerPathLen <= path.length() ? path.substring(handlerPathLen) : "/";
		fbReq.put("get", shortPath);
		Enumeration<String> en = req.getParameterNames();
		while(en.hasMoreElements())
		{
			String paramName = en.nextElement();
			fbReq.put(paramName, req.getParameter(paramName));
		}
		Payload payload = new Payload(fbReq.toString().getBytes());
		payload.metadata.put("mime", "application/json");
		return payload;
	}

	protected void processResponse(HttpServletResponse resp, Payload payload) throws Exception
	{
		OutputStream os = resp.getOutputStream();
		os.write(payload.getBytes());
		os.flush();
		os.close();
	}	

}
