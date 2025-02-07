package io.firebus.threads;

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.logging.Logger;

import io.firebus.NodeCore;
import io.firebus.utils.Queue;

public class ThreadManager
{
	private static Logger logger = Logger.getLogger("io.firebus");
	
	protected NodeCore nodeCore;
	protected Queue<Runnable> queue;
	protected boolean quit;
	protected ArrayList<FirebusThread> threads;
	protected int threadCount;
	
	public ThreadManager(NodeCore c)
	{
		nodeCore = c;
		quit = false;
		threadCount = 10;
		threads = new ArrayList<FirebusThread>();
		queue = new Queue<Runnable>(1024);
	}
	
	public void setThreadCount(int tc)
	{
		threadCount = tc;
	}
	
	public int getThreadCount()
	{
		return threadCount;
	}
	
	public int getQueueDepth()
	{
		return queue.getDepth();
	}
	
	public synchronized void enqueue(Runnable rbl)
	{
		queue.push(rbl);
		logger.finest("Dropped runnable in thread queue (depth: " + queue.getDepth() + ")");
		
		FirebusThread thread = null;
		for(int i = 0; i < threads.size(); i++)
		{
			FirebusThread t = threads.get(i);
			State s = t.getState();
			if(s == State.WAITING && t.ready)
			{
				thread = t;
				synchronized(thread) {
					logger.finest("Notifying thread " + thread.getId() + " to start");
					thread.notify();
				}
				break;
			}
		}
		
		if(thread == null && threads.size() < threadCount && !quit)
		{
			thread = new FirebusThread(this, nodeCore);
			threads.add(thread);
			thread.start();
			logger.finest("Added thread " + thread.getId() + " ");
		}

	}
	
	public synchronized Runnable getNext()
	{
		return queue.pop();
	}
	
	
	public void close()
	{
		quit = true;
		for(int i = 0; i < threads.size(); i++)
			threads.get(i).close();
	}
	
}
