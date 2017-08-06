package com.nic.firebus;

import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import com.nic.firebus.exceptions.FirebusFunctionException;

public class FunctionManager implements FunctionListener
{
	protected class FunctionEntry
	{
		protected FunctionInformation functionInformation;
		protected BusFunction function;
		protected int maxConcurrent;
		protected int currentCount;
		
		public FunctionEntry(FunctionInformation fi, BusFunction f, int mc)
		{
			functionInformation = fi;
			function = f;
			maxConcurrent = mc;
		}
	}
	
	private Logger logger = Logger.getLogger(FunctionManager.class.getName());
	protected FunctionListener functionListener;;
	protected HashMap<String, FunctionEntry> functions;
	
	public FunctionManager(FunctionListener fl)
	{
		functionListener = fl;
		functions = new HashMap<String, FunctionEntry>();
	}
	
	public void addFunction(FunctionInformation fi, BusFunction f, int mc)
	{
		FunctionEntry e = new FunctionEntry(fi, f, mc);
		functions.put(fi.getName(), e);
	}
	
	public boolean hasFunction(String n)
	{
		return functions.containsKey(n);
	}
	
	public String getFunctionStateString(int nodeId)
	{
		StringBuilder sb = new StringBuilder();
		Iterator<String> it = functions.keySet().iterator();
		while(it.hasNext())
		{
			String functionName = it.next();
			BusFunction f = functions.get(functionName).function;
			if(f != null)
			{
				sb.append(nodeId + ",f,");
				if(f instanceof ServiceProvider)
					sb.append("s,");
				if(f instanceof Publisher)
					sb.append("p,");
				if(f instanceof Consumer)
					sb.append("c,");
				sb.append(functionName);
				sb.append("\r\n");
			}
		}
		return sb.toString();
	}
	
	public void requestService(Message inboundMessage) throws FirebusFunctionException
	{
		logger.fine("Starting Service");
		String functionName = inboundMessage.getSubject();
		FunctionEntry fe = functions.get(functionName);
		if(fe != null)
		{
			BusFunction f = fe.function;
			if(f instanceof ServiceProvider)
			{
				if(fe.currentCount < fe.maxConcurrent)
				{
					new FunctionWorker(f, inboundMessage, this);
					fe.currentCount++;
				}
				else
				{
					throw new FirebusFunctionException("Maximum concurrent functions running");
				}
			}
		}
	}

	public void functionCallback(Message inboundMessage, byte[] payload)
	{
		String functionName = inboundMessage.getSubject();
		FunctionEntry fe = functions.get(functionName);
		if(fe != null)
		{
			fe.currentCount--;
		}
		functionListener.functionCallback(inboundMessage, payload);
		
	}

	public void consume(Message publishMessage) throws FirebusFunctionException
	{
		String consumerName = publishMessage.getSubject();
		FunctionEntry fe = functions.get(consumerName);
		if(fe != null)
		{
			BusFunction f = fe.function;
			if(f instanceof Consumer)
			{
				if(fe.currentCount < fe.maxConcurrent)
				{
					new FunctionWorker(f, publishMessage, null);
					fe.currentCount++;
				}
				else
				{
					throw new FirebusFunctionException("Maximum concurrent functions running");
				}
			}
		}
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		Iterator<String> it = functions.keySet().iterator();
		while(it.hasNext())
		{
			String fn = it.next();
			sb.append(fn + "  " + functions.get(fn) + "\r\n");
		}
		return sb.toString();
	}

}
