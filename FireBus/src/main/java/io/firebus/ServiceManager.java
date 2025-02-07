package io.firebus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import io.firebus.exceptions.FunctionErrorException;
import io.firebus.interfaces.ServiceProvider;
import io.firebus.threads.FirebusThread;

public class ServiceManager extends ExecutionManager {
	private Logger logger = Logger.getLogger("io.firebus");
	protected HashMap<String, FunctionEntry> services;
	
	
	public ServiceManager(NodeCore nc)
	{
		super(nc);
		services = new HashMap<String, FunctionEntry>();
	}	
	
	public void addService(String name, ServiceProvider s, int mc)
	{
		logger.fine("Adding service to node : " + name);
		FunctionEntry e = services.get(name);
		if(e == null)
		{
			e = new FunctionEntry(name, s, mc);
			services.put(name, e);
		}
	}
	
	public void removeService(String name)
	{
		logger.fine("Adding service to node : " + name);
		if(services.containsKey(name))
			services.remove(name);
	}
	
	public boolean hasService(String n)
	{
		return services.containsKey(n);
	}
	

	protected List<FunctionEntry> getFunctionEntries() {
		List<FunctionEntry> list = new ArrayList<FunctionEntry>();
		Iterator<String> it = services.keySet().iterator();
		while(it.hasNext())
			list.add(services.get(it.next()));
		return list;
	}

	protected FunctionEntry getFunctionEntry(String name) {
		return services.get(name);
	}


	public void executeService(Message msg)
	{
		String name = msg.getSubject();
		FunctionEntry fe = services.get(name);
		Payload inPayload = msg.getPayload();
		if(fe != null)
		{
			/*if(increaseExecutionCount())
			{*/
				long executionId = fe.getExecutionId();
				if(executionId != -1) 
				{
					nodeCore.getExecutionThreads().enqueue(new Runnable() {
						public void run() {
							((FirebusThread)Thread.currentThread()).setFunctionExecutionId(executionId);
							logger.finer("Executing Service Provider " + name + " (correlation: " + msg.getCorrelation() + ")");
							sendMessage(msg.getOriginatorId(), msg.getCorrelation(), 0, Message.MSGTYPE_PROGRESS, msg.getSubject(), new Payload());
							try
							{
								Payload returnPayload = ((ServiceProvider)fe.function).service(inPayload);
								logger.finer("Finished executing Service Provider " + name + " (correlation: " + msg.getCorrelation() + ")");
								if(msg.getType() == Message.MSGTYPE_REQUESTSERVICE) 
									sendMessage(msg.getOriginatorId(), msg.getCorrelation(), 1, Message.MSGTYPE_SERVICERESPONSE, msg.getSubject(), returnPayload);
							}
							catch(FunctionErrorException e)
							{
								if(msg.getType() == Message.MSGTYPE_REQUESTSERVICE) 
									sendError(e, msg.getOriginatorId(), msg.getCorrelation(), 1, Message.MSGTYPE_SERVICEERROR,  msg.getSubject());
							}

							
							((FirebusThread)Thread.currentThread()).clearFunctionExecutionId();
							fe.releaseExecutionId(executionId);
							decreaseExecutionCount();
						}
					});
				} else {
					decreaseExecutionCount();
					logger.info("Cannot execute function " + name + " as maximum number of executions reached for this function (" + totalExecutionCount + ")");
					sendMessage(msg.getOriginatorId(), msg.getCorrelation(), 0, Message.MSGTYPE_FUNCTIONUNAVAILABLE, msg.getSubject(), "Maximum concurrent functions running");
				}
			/*}
			else
			{
				logger.info("Cannot execute function " + name + " as maximum number of executions reached for this node (" + totalExecutionCount + ")");
				sendMessage(msg.getOriginatorId(), msg.getCorrelation(), 0, Message.MSGTYPE_FUNCTIONUNAVAILABLE, msg.getSubject(), "Maximum concurrent functions running");
			}*/
		}	
		else
		{
			logger.fine("Function " + name + " does not exist");
			sendMessage(msg.getOriginatorId(), msg.getCorrelation(), 0, Message.MSGTYPE_FUNCTIONUNAVAILABLE, msg.getSubject(), "No such function registered in this node");
		}
	}

	
	

}
