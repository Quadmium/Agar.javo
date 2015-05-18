import java.net.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;

public class Server
{
    private static final int PORT = 40124;
    private static final int ROOM_TICK = 10;
    private static final int CLIENTADDER_THROTTLE = 200;
    private ServerSocket serverSocket;
    private InetAddress hostAddress;
    private Socket socket;
    private ArrayList<User> users = new ArrayList<User>();
    private ArrayList<GameObject> userData = new ArrayList<GameObject>();
    private ArrayList<GameObject> worldData = new ArrayList<GameObject>();
    private final Object LOCK = new Object();
    
    public Server()
    {
        try {
           hostAddress = InetAddress.getLocalHost();
        } catch (Exception e) {System.out.println(e); return;}
        
        try{
            serverSocket = new ServerSocket(PORT, 0, hostAddress);
        } catch (IOException e) {System.out.println(e); return;}
        
        System.out.println("Server running: " + serverSocket);
        (new Thread(new worldUpdateThread())).start();
        (new Thread(new ClientAdderThread())).start();
        (new Thread(new ClientRemoverThread())).start();
    }
    
    private class worldUpdateThread implements Runnable
    {
        //Update all clients' positions
        //Update food
        //Tell them to send info back to player
        
        public void run()
        {
            long lastUpdate = System.nanoTime();
            synchronized(LOCK) {
                while(worldData.size() < GameConstants.FOOD_GOAL)
                {
                    GameObject food = new GameObject("F", Math.random() * GameConstants.BOARD_WIDTH,
                                                      Math.random() * GameConstants.BOARD_HEIGHT,
                                                      GameConstants.ALLOWED_COLORS[(int)(Math.random() * GameConstants.ALLOWED_COLORS.length)],
                                                      GameConstants.FOOD_RADIUS);
                    worldData.add(food);
                }
            }
            
            while(true)
            {
                double deltaTime = (System.nanoTime() - lastUpdate) / 1000000000.0;
                synchronized(LOCK) {
                    ArrayList<GameObject> removed = new ArrayList<GameObject>();
                    ArrayList<GameObject> added = new ArrayList<GameObject>();
                    
                    for(GameObject u : userData)
                    {
                        for(int i=0; i<worldData.size(); i++)
                        {
                            GameObject food = worldData.get(i);
                            if(GameConstants.distance(u.getPosition(), food.getPosition()) < u.getRadius())
                            {
                                u.setRadius(Math.sqrt((Math.PI * u.getRadius() * u.getRadius() + GameConstants.FOOD_VOLUME) / Math.PI));
                                removed.add(food);
                                worldData.remove(i);
                                i--;
                            }
                        }
                    }
                    
                    for(GameObject u : userData)
                    {
                        for(GameObject enemy : userData)
                        {
                            if(u == enemy)
                                continue;
                                
                            if(u.getRadius() > enemy.getRadius() * GameConstants.EAT_RATIO && GameConstants.distance(u.getPosition(), enemy.getPosition()) < u.getRadius())
                            {
                                u.setRadius(Math.sqrt((Math.PI * u.getRadius() * u.getRadius() + Math.PI * enemy.getRadius() * enemy.getRadius()) / Math.PI));
                                enemy.setRadius(0);
                            }
                        }
                    }
                    
                    while(worldData.size() < GameConstants.FOOD_GOAL)
                    {
                        GameObject food = new GameObject("F", Math.random() * GameConstants.BOARD_WIDTH,
                                                          Math.random() * GameConstants.BOARD_HEIGHT,
                                                          GameConstants.ALLOWED_COLORS[(int)(Math.random() * GameConstants.ALLOWED_COLORS.length)],
                                                          GameConstants.FOOD_RADIUS);
                        worldData.add(food);
                        added.add(food);
                    }
                    
                    for(User u : users)
                    {
                        for(GameObject g : removed)
                            u.addToWorldRemoved(g);
                        for(GameObject g : added)
                            u.addToWorldAdded(g);
                            
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
                // Get a client trying to connect
                try {
                    socket = serverSocket.accept();
                    socket.setTcpNoDelay(true);
                }
                catch(IOException e) {System.out.println(e);}
                System.out.println("Client "+socket+" has connected.");
                synchronized(LOCK) {
                    User newUsr = new User(socket, userData, LOCK);
                    users.add(newUsr);
                    newUsr.setWorld(worldData);
                }
                try {
                    Thread.sleep(CLIENTADDER_THROTTLE);
                } catch(InterruptedException e) {System.out.println(e);}
            }
        }
    }
    
    private class ClientRemoverThread implements Runnable
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
                            users.get(i).purge();
                            try{Thread.sleep(15);}catch(Exception e){}
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
                try {
                    Thread.sleep(CLIENTADDER_THROTTLE);
                } catch(InterruptedException e) {System.out.println(e);}
            }
        }
    }
}