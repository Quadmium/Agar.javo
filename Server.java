import java.net.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;

/**
 * Runs a server for the Agar game.
 * 
 * @author Quadmium
 */
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
    
    /**
     * Starts world update thread, a client adder thread, and a client remover thread.
     */
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
    
    /*
     * Updates the game world and updates the clients' information.
     */
    private class worldUpdateThread implements Runnable
    {
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
                    ArrayList<GameObject> moved = new ArrayList<GameObject>();
                    
                    for(User u : users)
                    {
                        for(GameObject g : u.getWorldAdditions())
                        {
                            worldData.add(g);
                            added.add(g);
                        }
                    }
                    
                    for(GameObject food : worldData)
                    {
                        if(food.getName().startsWith("T") && food.getVelocity().length() > 0.1)
                        {
                            Vector2D deltaP = food.getVelocity().scalarMult(deltaTime);
                            food.setX(food.getX() + deltaP.getX());
                            food.setY(food.getY() + deltaP.getY());
                            if(food.getPosition().getX() + food.getRadius() > GameConstants.BOARD_WIDTH)
                                food.setPosition(new Vector2D(GameConstants.BOARD_WIDTH - food.getRadius(), food.getPosition().getY()));
                            if(food.getPosition().getY() + food.getRadius() > GameConstants.BOARD_HEIGHT)
                                food.setPosition(new Vector2D(food.getPosition().getX(), GameConstants.BOARD_HEIGHT - food.getRadius()));
                            if(food.getPosition().getX() - food.getRadius() < 0)
                                food.setPosition(new Vector2D(food.getRadius(), food.getPosition().getY()));
                            if(food.getPosition().getY() - food.getRadius() < 0)
                                food.setPosition(new Vector2D(food.getPosition().getX(), food.getRadius()));
                            food.setVelocity(food.getVelocity().unitVector().scalarMult(food.getVelocity().length() - GameConstants.THROW_MASS_DECELERATION * deltaTime));
                            moved.add(food);
                        }
                    }
                    
                    for(GameObject u : userData)
                    {
                        for(int i=0; i<worldData.size(); i++)
                        {
                            GameObject food = worldData.get(i);
                            
                            for(int j=-1; j<u.getSubObjectsSize(); j++)
                            {
                                if(j < 0 && u.getSubObjectsSize() > 0)
                                    continue;
                                    
                                GameObject player = j==-1 ? u : u.getSubObject(j);
                                if(GameConstants.distance(player.getPosition(), food.getPosition()) < player.getRadius())
                                {
                                    double volume = 0;
                                    if(food.getName().equals("F"))
                                        volume += GameConstants.FOOD_VOLUME;
                                    else if(food.getName().startsWith("T"))
                                    {
                                        if(!food.eatable())
                                            continue;
                                        volume += GameConstants.THROW_MASS_VOLUME;
                                    }
                                        
                                    player.setRadius(Math.sqrt((Math.PI * player.getRadius() * player.getRadius() + volume) / Math.PI));
                                    removed.add(food);
                                    worldData.remove(i);
                                    i--;
                                }
                            }
                        }
                    }
                    
                    for(GameObject u : userData)
                    {
                        for(GameObject enemy : userData)
                        {
                            if(u == enemy)
                                continue;
                                
                            if(u.getSubObjectsSize() == 0 && enemy.getSubObjectsSize() == 0 &&
                               u.getRadius() > enemy.getRadius() * GameConstants.EAT_RATIO && GameConstants.distance(u.getPosition(), enemy.getPosition()) < u.getRadius())
                            {
                                u.setRadius(Math.sqrt((Math.PI * u.getRadius() * u.getRadius() + Math.PI * enemy.getRadius() * enemy.getRadius()) / Math.PI));
                                enemy.setRadius(0);
                            }
                            else if(u.getSubObjectsSize() == 0)
                            {
                                for(int i=0; i<enemy.getSubObjectsSize(); i++)
                                {
                                    if(u.getRadius() > enemy.getSubObject(i).getRadius() * GameConstants.EAT_RATIO && GameConstants.distance(u.getPosition(), enemy.getSubObject(i).getPosition()) < u.getRadius())
                                    {
                                        u.setRadius(Math.sqrt((Math.PI * u.getRadius() * u.getRadius() + Math.PI * enemy.getSubObject(i).getRadius() * enemy.getSubObject(i).getRadius()) / Math.PI));
                                        enemy.removeSubObject(i);
                                        if(enemy.getSubObjectsSize() == 0)
                                            enemy.setRadius(0);
                                   }
                                }
                            }
                            else if(enemy.getSubObjectsSize() == 0)
                            {
                                for(int i=0; i<u.getSubObjectsSize(); i++)
                                {
                                    if(u.getSubObject(i).getRadius() > enemy.getRadius() * GameConstants.EAT_RATIO && GameConstants.distance(u.getSubObject(i).getPosition(), enemy.getPosition()) < u.getSubObject(i).getRadius())
                                    {
                                        u.getSubObject(i).setRadius(Math.sqrt((Math.PI * u.getSubObject(i).getRadius() * u.getSubObject(i).getRadius() + Math.PI * enemy.getRadius() * enemy.getRadius()) / Math.PI));
                                        enemy.setRadius(0);
                                    }
                                }
                            }
                            else
                            {
                                for(int i=0; i<u.getSubObjectsSize(); i++)
                                {
                                    for(int j=0; j<enemy.getSubObjectsSize(); j++)
                                    {
                                        if(u.getSubObject(i).getRadius() > enemy.getSubObject(j).getRadius() * GameConstants.EAT_RATIO && GameConstants.distance(u.getSubObject(i).getPosition(), enemy.getSubObject(j).getPosition()) < u.getSubObject(i).getRadius())
                                        {
                                            u.getSubObject(i).setRadius(Math.sqrt((Math.PI * u.getSubObject(i).getRadius() * u.getSubObject(i).getRadius() + Math.PI * enemy.getSubObject(j).getRadius() * enemy.getSubObject(j).getRadius()) / Math.PI));
                                            enemy.removeSubObject(j);
                                            if(enemy.getSubObjectsSize() == 0)
                                                enemy.setRadius(0);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    for(GameObject u : userData)
                    {
                        if(u.canMerge())
                        {
                            for(int i=0; i<u.getSubObjectsSize(); i++)
                            {
                                for(int j=i+1; j<u.getSubObjectsSize(); j++)
                                {
                                    if(GameConstants.distance(u.getSubObject(i).getPosition(), u.getSubObject(j).getPosition()) - 0.00001 <=
                                    (u.getSubObject(i).getRadius() > u.getSubObject(j).getRadius() ? u.getSubObject(i).getRadius() : u.getSubObject(j).getRadius()))
                                    {
                                        if(u.getSubObjectsSize() > 2)
                                        {
                                            int biggerIndex = u.getSubObject(i).getRadius() > u.getSubObject(j).getRadius() ? i : j;
                                            int smallerIndex = biggerIndex == i ? j : i;
                                            u.getSubObject(biggerIndex).setRadius(Math.sqrt((Math.PI * Math.pow(u.getSubObject(biggerIndex).getRadius(),2) + Math.PI * Math.pow(u.getSubObject(smallerIndex).getRadius(),2)) / Math.PI));
                                            u.removeSubObject(smallerIndex);
                                        }
                                        else
                                        {
                                            u.setRadius(Math.sqrt((Math.PI * u.getSubObject(i).getRadius() * u.getSubObject(i).getRadius() + Math.PI * u.getSubObject(j).getRadius() * u.getSubObject(j).getRadius()) / Math.PI));
                                            Vector2D biggerRadiusPosition = u.getSubObject(i).getRadius() > u.getSubObject(j).getRadius() ? u.getSubObject(i).getPosition() : u.getSubObject(j).getPosition();
                                            u.setX(biggerRadiusPosition.getX());
                                            u.setY(biggerRadiusPosition.getY());
                                            u.clearSubObjects();
                                        }
                                    }
                                }
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
                        for(GameObject g : moved)
                            u.addToWorldMoved(g);
                            
                        u.move(deltaTime);
                        u.setWorld(worldData);
                    }
                }
                lastUpdate = System.nanoTime();
                try {
                    Thread.sleep(ROOM_TICK);
                } catch(InterruptedException e) {System.out.println(e);}
            }
        }
    }
    
    /**
     * Adds clients to the world with a new User object. Waits for new clients to connect.
     */
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
    
    /**
     * Removes timed out clients.
     */
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