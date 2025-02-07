package io.firebus.adapters.http.outbound;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;

import io.firebus.Firebus;
import io.firebus.Payload;
import io.firebus.adapters.http.OutboundHandler;
import io.firebus.utils.DataException;
import io.firebus.utils.DataMap;

public class PostHandler extends OutboundHandler 
{
	public PostHandler(DataMap c, Firebus f) 
	{
		super(c, f);
	}

	protected HttpUriRequest processRequest(Payload payload) throws ServletException, IOException, DataException 
	{
		String path = this.baseUrl;
		if(path.endsWith("/"))
			path = path.substring(0, path.length() - 1);
		String relativePath = payload.metadata.get("path");
		if(relativePath != null) 
		{
			if(relativePath.startsWith("/"))
				relativePath = relativePath.substring(1);
			path = path + "/" + relativePath;
		}
		HttpPost httppost = new HttpPost(path);
		httppost.setEntity(new ByteArrayEntity(payload.data));
		return httppost;
	}


	protected Payload processResponse(HttpEntity response) throws ServletException, IOException, DataException 
	{
		String responseStr = EntityUtils.toString(response);
		EntityUtils.consume(response);
		Payload payload = new Payload(responseStr);
		return payload;
	}

}
