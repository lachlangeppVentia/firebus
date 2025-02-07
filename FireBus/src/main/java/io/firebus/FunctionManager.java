package io.firebus;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import io.firebus.exceptions.FunctionErrorException;
import io.firebus.information.FunctionInformation;
import io.firebus.information.ServiceInformation;
import io.firebus.information.StreamInformation;
import io.firebus.interfaces.BusFunction;
import io.firebus.interfaces.Consumer;
import io.firebus.interfaces.ServiceProvider;
import io.firebus.interfaces.StreamProvider;
import io.firebus.threads.FirebusThread;

public class FunctionManager
{
	private Logger logger = Logger.getLogger("io.firebus");
	protected NodeCore nodeCore;
	protected HashMap<String, FunctionEntry> functions;
	protected int totalExecutionCount;
	
	public FunctionManager(NodeCore nc)
	{
		nodeCore = nc;
		functions = new HashMap<String, FunctionEntry>();
		totalExecutionCount = 0;
	}
	
	public void addFunction(String functionName, BusFunction f, int mc)
	{
		logger.fine("Adding function to node : " + functionName);
		FunctionEntry e = functions.get(functionName);
		if(e == null)
		{
			e = new FunctionEntry(functionName, f, mc);
			functions.put(functionName, e);
		}
	}
	
	public void removeFunction(String functionName)
	{
		logger.fine("Adding function to node : " + functionName);
		if(functions.containsKey(functionName))
			functions.remove(functionName);
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
				if(f instanceof ServiceProvider)
					sb.append(nodeId + ",f,s," + functionName + "\r\n");
				if(f instanceof StreamProvider)
					sb.append(nodeId + ",f,t," + functionName + "\r\n");
				if(f instanceof Consumer)
					sb.append(nodeId + ",f,c," + functionName + "\r\n");
			}
		}
		return sb.toString();
	}
	
	public void processServiceInformationRequest(Message msg)
	{
		String functionName = msg.getSubject();
		if(functions.containsKey(functionName))
		{
			BusFunction f = functions.get(functionName).function;
			FunctionInformation fi = f instanceof ServiceProvider ? ((ServiceProvider)f).getServiceInformation() : (f instanceof StreamProvider ? ((StreamProvider)f).getStreamInformation() : null);
			if(fi == null)
				fi = f instanceof ServiceProvider ? new ServiceInformation(functionName) : (f instanceof StreamProvider ? new StreamInformation(functionName) : null);
			logger.finer("Responding to a function information request");
			Message outMsg = new Message(msg.getOriginatorId(), nodeCore.getNodeId(), Message.MSGTYPE_FUNCTIONINFORMATION, msg.getSubject(), new Payload(fi != null ? fi.serialise() : null));
			outMsg.setCorrelation(msg.getCorrelation(), 0);
			nodeCore.route(outMsg);
		}
	}
	
	protected boolean canRunOneMore()
	{
		return true; //totalExecutionCount < nodeCore.getThreadManager().getThreadCount() - 2;
	}
/*
	protected synchronized void increaseTotalExecutionCount()
	{
		totalExecutionCount++;
	}
	
	protected synchronized void decreaseTotalExecutionCount()
	{
		totalExecutionCount--;
	}
	*/
	public void executeFunction(Message msg)
	{
		String functionName = msg.getSubject();
		FunctionEntry fe = functions.get(functionName);
		Payload inPayload = msg.getPayload();
		if(fe != null)
		{
			if(this.canRunOneMore())
			{
				long executionId = fe.getExecutionId();
				if(executionId != -1) 
				{
					((FirebusThread)Thread.currentThread()).setFunctionExecutionId(executionId);
					if((msg.getType() == Message.MSGTYPE_REQUESTSERVICE || msg.getType() == Message.MSGTYPE_REQUESTSERVICEANDFORGET) && fe.function instanceof ServiceProvider)
					{
						logger.finer("Executing Service Provider " + functionName + " (correlation: " + msg.getCorrelation() + ")");
						Payload returnPayload = null;
						Message progressMsg = new Message(msg.getOriginatorId(), nodeCore.getNodeId(), Message.MSGTYPE_PROGRESS, msg.getSubject(), null);
						progressMsg.setCorrelation(msg.getCorrelation(), 0);
						nodeCore.enqueue(progressMsg);
						//fe.runStarted();
						//increaseTotalExecutionCount();
						try
						{
							returnPayload = ((ServiceProvider)fe.function).service(inPayload);
							logger.finer("Finished executing Service Provider " + functionName + " (correlation: " + msg.getCorrelation() + ")");
							
							if(msg.getType() == Message.MSGTYPE_REQUESTSERVICE) 
							{
								Message responseMsg = new Message(msg.getOriginatorId(), nodeCore.getNodeId(), Message.MSGTYPE_SERVICERESPONSE, msg.getSubject(), returnPayload);
								responseMsg.setCorrelation(msg.getCorrelation(), 1);
								nodeCore.route(responseMsg);
							}
						}
						catch(FunctionErrorException e)
						{
							if(msg.getType() == Message.MSGTYPE_REQUESTSERVICE) 
								sendError(e, msg.getOriginatorId(), Message.MSGTYPE_SERVICEERROR,  msg.getSubject(), msg.getCorrelation(), 1);
						}
						//decreaseTotalExecutionCount();
						//fe.runEnded();
					}
					else if(msg.getType() == Message.MSGTYPE_REQUESTSTREAM  && fe.function instanceof StreamProvider)
					{
						logger.finer("Executing Stream Provider " + functionName + " (correlation: " + msg.getCorrelation() + ")");
						StreamProvider streamProvider = (StreamProvider)fe.function;
						long idleTimeout = streamProvider.getStreamIdleTimeout();
						int localCorrelationId = nodeCore.getCorrelationManager().createEntry(idleTimeout);
						StreamEndpoint streamEndpoint = new StreamEndpoint(nodeCore, functionName, localCorrelationId, msg.getCorrelation(), 1, msg.getOriginatorId());
						//fe.runStarted();
						//increaseTotalExecutionCount();
						try
						{
							streamProvider.acceptStream(inPayload, streamEndpoint);
							logger.finer("Accepted stream " + functionName + " (correlation: " + msg.getCorrelation() + ")");
							
							ByteBuffer bb = ByteBuffer.allocate(12);
							bb.putInt(localCorrelationId);
							bb.putLong(idleTimeout);
							nodeCore.getCorrelationManager().setListenerOnEntry(localCorrelationId, streamEndpoint, idleTimeout);
							Message responseMsg = new Message(msg.getOriginatorId(), nodeCore.getNodeId(), Message.MSGTYPE_STREAMACCEPT, msg.getSubject(), new Payload(bb.array()));
							responseMsg.setCorrelation(msg.getCorrelation(), 0);
							nodeCore.route(responseMsg);
						}
						catch(FunctionErrorException e)
						{
							sendError(e, msg.getOriginatorId(), Message.MSGTYPE_STREAMERROR,  msg.getSubject(), msg.getCorrelation(), 1);
						}
						//decreaseTotalExecutionCount();
					}
					else if(msg.getType() == Message.MSGTYPE_PUBLISH  &&  fe.function instanceof Consumer)
					{
						logger.finer("Executing Consumer");
						((Consumer)fe.function).consume(inPayload);
					}	
					((FirebusThread)Thread.currentThread()).clearFunctionExecutionId();
					fe.releaseExecutionId(executionId);
				} else {
					logger.info("Cannot execute function " + functionName + " as maximum number of executions reached for this function (" + totalExecutionCount + ")");
					Message outMsg = new Message(msg.getOriginatorId(), nodeCore.getNodeId(), Message.MSGTYPE_FUNCTIONUNAVAILABLE, msg.getSubject(),new Payload(null,  "Maximum concurrent functions running".getBytes()));
					outMsg.setCorrelation(msg.getCorrelation(), 0);
					nodeCore.route(outMsg);					
				}
			}
			else
			{
				logger.info("Cannot execute function " + functionName + " as maximum number of executions reached for this node (" + totalExecutionCount + ")");
				Message outMsg = new Message(msg.getOriginatorId(), nodeCore.getNodeId(), Message.MSGTYPE_FUNCTIONUNAVAILABLE, msg.getSubject(),new Payload(null,  "Maximum concurrent functions running".getBytes()));
				outMsg.setCorrelation(msg.getCorrelation(), 0);
				nodeCore.route(outMsg);
			}
		}	
		else
		{
			logger.fine("Function " + functionName + " does not exist");
			Message outMsg = new Message(msg.getOriginatorId(), nodeCore.getNodeId(), Message.MSGTYPE_FUNCTIONUNAVAILABLE, msg.getSubject(),new Payload(null,  "No such function registered in this node".getBytes()));
			outMsg.setCorrelation(msg.getCorrelation(), 0);
			nodeCore.route(outMsg);
		}
	}
	
	
	protected void sendError(Throwable t, int dest, int msgType, String subject, int corr, int corrSeq)
	{
		String errorMessage = "";
		while(t != null)
		{
			if(errorMessage.length() > 0)
				errorMessage += " : ";
			errorMessage += t.getMessage();
			t = t.getCause();
		}
		Message errorMsg = new Message(dest, nodeCore.getNodeId(), msgType, subject, new Payload(errorMessage.getBytes()));
		errorMsg.setCorrelation(corr, corrSeq);
		nodeCore.route(errorMsg);		
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
