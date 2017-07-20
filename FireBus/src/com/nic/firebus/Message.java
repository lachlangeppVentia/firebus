package com.nic.firebus;

import java.nio.ByteBuffer;

public class Message 
{
	protected Connection connection;
	protected byte[] encodedMessage;
	protected boolean decoded;
	protected boolean encoded;
	protected int id;
	protected int type;
	protected int originator;
	protected int repeater;
	protected int destination;
	protected String subject;
	protected byte[] payload;
	
	public static int MSGTYPE_ADVERTISE = 0;
	public static int MSGTYPE_QUERYNODE = 1;
	public static int MSGTYPE_FINDSERVICE = 2;
	public static int MSGTYPE_FINDPUBLISHER = 3;
	public static int MSGTYPE_FINDSUBSCRIBER = 4;
	public static int MSGTYPE_REQUESTSERVICE = 5;
	public static int MSGTYPE_SERVICERESPONSE = 6;
	public static int MSGTYPE_PUBLISH = 7;
	public static int MSGTYPE_RECALL = 8;
	
	protected static int nextId = 0;
	
	public Message(byte[] b, Connection c)
	{
		encodedMessage = b;
		connection = c;
		decoded = false;
		encoded = true;
	}
	
	public Message(int d, int o, int r, int t, String s, byte[] p)
	{
		id = nextId++;
		destination = d;
		originator = o;
		repeater = r;
		type = t;
		subject = s;
		payload = p;
		decoded = true;
		encoded = false;
	}
	
	public void decode()
	{
		ByteBuffer bb = ByteBuffer.wrap(encodedMessage);
		id = bb.getInt();
		destination = bb.getInt();
		originator = bb.getInt();
		repeater = bb.getInt();
		type = bb.getInt();
		int subjectLen = bb.getInt();
		subject = new String(encodedMessage, bb.position(), subjectLen);
		bb.position(bb.position() + subjectLen);
		System.arraycopy(encodedMessage, bb.position(), payload, 0, bb.remaining());
		decoded = true;
	}
	
	public void encode()
	{
		int len = 24;
		if(subject != null)
			len += subject.length();
		if(payload != null)
			len += payload.length;
		ByteBuffer bb = ByteBuffer.allocate(len);
		bb.putInt(id);
		bb.putInt(destination);
		bb.putInt(originator);
		bb.putInt(repeater);
		bb.putInt(type);
		if(subject != null)
		{
			bb.putInt(subject.length());
			bb.put(subject.getBytes(), 0, subject.length());
		}
		else
		{
			bb.putInt(0);
		}
		if(payload != null)
		{
			bb.put(payload);	
		}
		encodedMessage = bb.array();
		encoded = true;
	}
	
	public int getid()
	{
		return id;
	}
	
	public long getUniversalId()
	{
		return ((((long)originator) << 32) + id);
	}

	public int getType()
	{
		return type;
	}
	
	public int getOriginator()
	{
		return originator;
	}
	
	public int getRepeater()
	{
		return repeater;
	}
	
	public int getDestination()
	{
		return destination;
	}
	
	public String getSubject()
	{
		return subject;
	}
	
	public byte[] getPayload()
	{
		return payload;
	}
	
	public Connection getConnection()
	{
		return connection;
	}
	
	public byte[] getEncodedMessage()
	{
		if(!encoded)
			encode();
		return encodedMessage;
	}
}
