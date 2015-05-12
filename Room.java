import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import java.util.ArrayList;
/**
 * This handles all of the clients in the room.
 * It accepts new connections and adds them to the room.
 * It is a runnable thread and when the start method is called, it will accept clients.
 */
public class Room extends Thread
{
	private static final int PORT = 40124;
	private static final int ROOM_THROTTLE = 200;
	private ServerSocket serverSocket;
	private InetAddress hostAddress;
	private Socket socket;
	private ArrayList<User> users = new ArrayList<User>();
	/**
	 * Creates a new room for clients to connect to.
	 */
	public Room()
	{
		// Attempt to get the host address
		try
		{
			hostAddress = InetAddress.getLocalHost();
		}
		catch(UnknownHostException e)
		{
			System.out.println("Could not get the host address.");
			return;
		}
		// Announce the host address
		System.out.println("Server host address is: "+hostAddress);
		// Attempt to create server socket
		try
		{
			serverSocket = new ServerSocket(PORT,0,hostAddress);
		}
		catch(IOException e)
		{
			System.out.println("Could not open server socket.");
			return;
		}
		// Announce the socket creation
		System.out.println("Socket "+serverSocket+" created.");
	}
	/**
	 * Starts the client accepting process.
	 */
	public void run()
	{
		// Announce the starting of the process
		System.out.println("Room has been started.");
		// Enter the main loop
		while(true)
		{
			// Remove all disconnected clients
			for(int i = 0;i < users.size();i++)
			{
				// Check connection, remove on dead
				if(!users.get(i).isConnected())
				{
					System.out.println(users.get(i)+" removed due to lack of connection.");
					users.remove(i);
				}
			}
			// Get a client trying to connect
			try
			{
				socket = serverSocket.accept();
			}
			catch(IOException e)
			{
				System.out.println("Could not get a client.");
			}
			// Client has connected
			System.out.println("Client "+socket+" has connected.");
			// Add user to list
			users.add(new User(socket));
			// Sleep
			try
			{
				Thread.sleep(ROOM_THROTTLE);
			}
			catch(InterruptedException e)
			{
				System.out.println("Room has been interrupted.");
			}
		}
	}
}

