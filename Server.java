import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;

public class Server
{
    private static final int PORT = 40124;
    private static final int ROOM_TICK = 100;
    private static final int CLIENTADDER_THROTTLE = 200;
    private ServerSocket serverSocket;
    private InetAddress hostAddress;
    private Socket socket;
    private ArrayList<User> users = new ArrayList<User>();
    private volatile ArrayList<GameObject> userData = new ArrayList<GameObject>();
    private volatile ArrayList<GameObject> boardData = new ArrayList<GameObject>();
    private final Object LOCK = new Object();
    
    public Server()
    {
        try {
            hostAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {System.out.println(e); return;}
        
        try{
            serverSocket = new ServerSocket(PORT, 0, hostAddress);
        } catch (IOException e) {System.out.println(e); return;}
        
        System.out.println("Server running: " + serverSocket);
        (new Thread(new worldUpdateThread())).start();
        (new Thread(new ClientAdderThread())).start();
    }
    
    private class worldUpdateThread implements Runnable
    {
        //Update all clients' positions
        //Tell them to send info back to player
        public void run()
        {
            long lastUpdate = System.nanoTime();
            
            while(true)
            {
                double deltaTime = (System.nanoTime() - lastUpdate) / 1000000000.0;
                synchronized(LOCK) {
                    for(User u : users)
                    {
                        u.move(deltaTime);
                    }
                }
                lastUpdate = System.nanoTime();
                try {
                    Thread.sleep(ROOM_TICK);
                } catch(InterruptedException e) {System.out.println(e);}
            }
        }
    }
    
    private class ClientAdderThread implements Runnable
    {
        public void run()
        {
            while(true)
            {
                synchronized(LOCK) {
                    for(int i = 0; i<users.size(); i++)
                    {
                        // Check connection, remove on dead
                        if(!users.get(i).isConnected())
                        {
                            System.out.println(users.get(i)+" removed due to lack of connection.");
                            users.remove(i);
                            userData.remove(i);
                            i--;
                        }
                    }
                    
                    for(int j=0; j<users.size(); j++)
                    {
                        users.get(j).setIndex(j);
                    }
                }
                // Get a client trying to connect
                try {
                    socket = serverSocket.accept();
                }
                catch(IOException e) {System.out.println(e);}
                System.out.println("Client "+socket+" has connected.");
                synchronized(LOCK) {
                    users.add(new User(socket, userData));
                }
                try {
                    Thread.sleep(CLIENTADDER_THROTTLE);
                } catch(InterruptedException e) {System.out.println(e);}
            }
        }
    }
}