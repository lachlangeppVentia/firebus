package io.firebus;

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.firebus.exceptions.FunctionErrorException;
import io.firebus.information.ServiceInformation;
import io.firebus.interfaces.ServiceProvider;
import io.firebus.interfaces.ServiceRequestor;
import io.firebus.logging.FirebusSimpleFormatter;

public class SRAsyncTest {
	
	public static void main(String[] args) 
	{
		Logger.getLogger("").removeHandler(Logger.getLogger("").getHandlers()[0]);
		try
		{
			FileHandler fh = new FileHandler("ServiceAsyncTest.log");
			fh.setFormatter(new FirebusSimpleFormatter());
			fh.setLevel(Level.FINEST);
			Logger logger = Logger.getLogger("io.firebus");
			logger.addHandler(fh);
			logger.setLevel(Level.FINEST);
			
			Firebus firebus = new Firebus();
			firebus.registerServiceProvider("thirdservice", new ServiceProvider() {
				public Payload service(Payload payload) throws FunctionErrorException {
					try{Thread.sleep(1000);} catch(Exception e) {}
					return new Payload("Third service " + payload.getString());
				}

				public ServiceInformation getServiceInformation() {
					return null;
				}
			}, 10);

			firebus.registerServiceProvider("secondservice", new ServiceProvider() {
				public Payload service(Payload payload) throws FunctionErrorException {
					String s = "";
					try{
						Payload resp = firebus.requestService("thirdservice", new Payload(payload.getBytes()));
						s = resp.getString();
						//Thread.sleep(1000);
					} catch(Exception e) {}
					return new Payload("Second service " + s);
				}

				public ServiceInformation getServiceInformation() {
					return null;
				}
			}, 10);

		
			firebus.registerServiceProvider("firstservice", new ServiceProvider() {
				public Payload service(Payload payload) throws FunctionErrorException {
					String s = "";
					try{
						Payload resp = firebus.requestService("secondservice", new Payload(payload.getBytes()));
						s = resp.getString();
						//Thread.sleep(1000);
					} catch(Exception e) {}
					return new Payload("First service " + s);
				}

				public ServiceInformation getServiceInformation() {
					return null;
				}
			}, 10);
			
			firebus.requestService("firstservice", new Payload("Nicolas"), new ServiceRequestor() {

				public void response(Payload payload) {
					System.out.println("Async response is " + payload.getString());
				}

				public void error(FunctionErrorException e) {
					System.out.println("Async error is " + e.getMessage());
				}

				public void timeout() {
					System.out.println("Async timeout");
				}
				
			}, 10000);
			Thread.sleep(2000);
			firebus.close();			
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		

	}

}
