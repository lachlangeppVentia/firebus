package com.nic.firebus;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class DiscoveryManager extends Thread
{
	protected boolean quit;
	protected int nodeId;
	protected String networkName;
	protected Address address;
	protected long lastRequestSent;
	protected NodeCore nodeCore;
	protected MulticastSocket socket;
	protected InetAddress multicastGroup;
	
	public DiscoveryManager(NodeCore nc, int id, String n, Address a)
	{
		nodeCore = nc;
		nodeId = id;
		networkName = n;
		address = a;
		quit = false;
		try
		{
			socket = new MulticastSocket(1900);
			multicastGroup = InetAddress.getByName("239.255.255.255");
			socket.joinGroup(multicastGroup);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		setName("Firebus Discovery Manager");
		start();
	}
	
	public void run()
	{
		sendDiscoveryRequest();
		while(!quit)
		{
			try 
			{
				byte[] buf = new byte[256];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
			    socket.receive(packet);
			    String received = new String(packet.getData()).trim();

			    if(received.startsWith("Firebus discovery request from"))
			    {
			    	String[] parts = received.split(" ");
			    	int id = Integer.parseInt(parts[4]);
			    	String net = parts[5];
			    	if(id != nodeId  &&  net.equals(networkName))
			    		sendAdvertisement();
			    }
			    if(received.startsWith("Firebus anouncement from"))
			    {
			    	String[] parts = received.split(" ");
			    	int id = Integer.parseInt(parts[3]);
			    	String net = parts[4];
			    	String ad = parts[5];
			    	int port = Integer.parseInt(parts[6]);
			    	Address address = new Address(ad, port);
			    	if(id != nodeId  &&  net.equals(networkName))
			    		nodeCore.nodeDiscovered(id, address);
			    }
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		socket.close();
	}
	
	public void sendDiscoveryRequest()
	{
		long currentTime = System.currentTimeMillis();
		if(currentTime > (lastRequestSent + 10000))
		{
			try 
			{
				String val = "Firebus discovery request from " + nodeId + " " + networkName;
				multicastSend(val);
	        }
	        catch (IOException e) 
			{
	            e.printStackTrace();
	        }
			
			lastRequestSent = currentTime;
		}
	}
	
	public void sendAdvertisement()
	{
		try 
		{
			String val = "Firebus anouncement from " + nodeId + " " + networkName + " " + address.getIPAddress() + " " + address.getPort();
			multicastSend(val);
		}
        catch (IOException e) 
		{
            e.printStackTrace();
        }
		
	}	
	
	protected void multicastSend(String value) throws IOException
	{
		byte[] buf = value.getBytes();
		DatagramPacket packet = new DatagramPacket(buf, buf.length, multicastGroup, 1900);
		socket.send(packet);
	}
}
