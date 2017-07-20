package com.nic.firebus;

import java.util.ArrayList;
import java.util.Random;

public class NodeInformation 
{
	protected ArrayList<ServiceInformation> services;
	protected int nodeId;
	protected Address address;
	protected Connection connection;
	protected ArrayList<Integer> repeaters;
	
	public NodeInformation(int ni)
	{
		nodeId = ni;
		services = new ArrayList<ServiceInformation>();
		repeaters = new ArrayList<Integer>();
	}
	
	public void setConnection(Connection c)
	{
		connection = c;
	}
	
	public void setInetAddress(Address a)
	{
		address = a;
	}
	
	public void addRepeater(int id)
	{
		if(!repeaters.contains(id))
			repeaters.add(id);
	}
	
	public void addService(ServiceInformation si)
	{
		services.add(si);
	}
	
	public int getNodeId()
	{
		return nodeId;
	}
	
	public Address getAddress()
	{
		return address;
	}
		
	public Connection getConnection()
	{
		return connection;
	}
	
	public int getRandomRepeater()
	{
		Random r = new Random();
		if(repeaters.size() > 0)
			return repeaters.get(r.nextInt(repeaters.size()));
		return 0;
	}
	
	public ServiceInformation getService(String sn)
	{
		for(int i = 0; i < services.size(); i++)
			if(services.get(i).getServiceName().equals(sn))
				return services.get(i);
		return null;
	}

}
