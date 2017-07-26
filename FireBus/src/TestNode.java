import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.nic.firebus.Consumer;
import com.nic.firebus.Node;
import com.nic.firebus.ServiceProvider;


public class TestNode
{
	public static void main(String[] args)
	{
		Node n = new Node();
		if(args.length > 0)
		{
			if(args[0].equals("requestor"))
			{
				if(args.length > 1)
				{
					boolean quit = false;
					BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
					while(!quit)
					{
						try
						{
							String in = br.readLine();
							byte[] ret = n.requestService(args[1], in.getBytes());
							System.out.println(new String(ret));
						} 
						catch (IOException e) {}
					}
				}
			}
			
			if(args[0].equals("provider"))
			{
				if(args.length > 2)
				{
					final String prefix = args[2];
					n.registerServiceProvider(args[1], new ServiceProvider() {
						public byte[] requestService(byte[] payload)
						{
							System.out.println("Providing Service");
							return (prefix + " " + new String(payload)).getBytes();
						}
					});
					System.out.println("Service Provider Registered");
				}
			}
			
			if(args[0].equals("publisher"))
			{
				if(args.length > 1)
				{
					boolean quit = false;
					BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
					while(!quit)
					{
						try
						{
							String in = br.readLine();
							n.publish(args[1], in.getBytes());
						} 
						catch (IOException e) {}
					}
				}
			}
			
			if(args[0].equals("consumer"))
			{
				if(args.length > 1)
				{
					n.registerConsumer(args[1], new Consumer(){
						public void consume(byte[] payload)
						{
							System.out.println(new String(payload));
						}
					});
					System.out.println("Consumer Registered");
				}
			}
		}
	}
}
