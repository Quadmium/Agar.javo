import java.net.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Timer;
import java.util.Arrays;
import java.util.List;
import java.awt.Rectangle;

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
    private HashSet<GameObject> worldData = new HashSet<GameObject>();
    private ArrayList<ArrayList<HashSet<GameObject>>> grid = new ArrayList<ArrayList<HashSet<GameObject>>>();
    private HashSet<GameObject> movingWorldData = new HashSet<GameObject>();
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

        for(int i=0; i<GameConstants.GRID_SIZE; i++)
            grid.add(new ArrayList<HashSet<GameObject>>());
        for(ArrayList<HashSet<GameObject>> a : grid)
            for(int i=0; i<GameConstants.GRID_SIZE; i++)
                a.add(new HashSet<GameObject>());

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
            populateFood();

            while(true)
            {
                double deltaTime = (System.nanoTime() - lastUpdate) / 1000000000.0;
                synchronized(LOCK) {
                    ArrayList<GameObject> removed = new ArrayList<GameObject>();
                    ArrayList<GameObject> added = new ArrayList<GameObject>();
                    ArrayList<GameObject> moved = new ArrayList<GameObject>();

                    for(GameObject food : populateFood())
                        added.add(food);

                    for(User u : users)
                    {
                        for(GameObject g : u.getWorldAdditions())
                        {
                            added.add(g);
                            addToWorldData(g);
                            if(g.getVelocity().length() > 0.1)
                                movingWorldData.add(g);
                        }
                    }

                    for(Iterator<GameObject> i = movingWorldData.iterator(); i.hasNext();)
                    {
                        GameObject g = i.next();
                        if(g.getVelocity().length() < 0.1)
                        {
                            i.remove();
                            continue;
                        }

                        int[] bucket = GameConstants.gridBucket(g.getPosition());
                        Vector2D deltaP = g.getVelocity().scalarMult(deltaTime);
                        g.setPosition(g.getPosition().plus(deltaP));
                        GameConstants.constrainToBoard(g);
                        g.setVelocity(g.getVelocity().unitVector().scalarMult(g.getVelocity().length() - GameConstants.THROW_MASS_DECELERATION * deltaTime));
                        moved.add(g);
                        updateBucket(g, bucket);
                    }

                    for(GameObject u : userData)
                    {
                        for(int i=-1; i<u.getSubObjectsSize(); i++)
                        {
                            if(i < 0 && u.getSubObjectsSize() > 0)
                                continue;

                            GameObject player = i==-1 ? u : u.getSubObject(i);

                            int[] topLeftBucket = GameConstants.gridBucket(player.getPosition().plus(new Vector2D(-player.getRadius(), -player.getRadius())));
                            int[] bottomRightBucket = GameConstants.gridBucket(player.getPosition().plus(new Vector2D(player.getRadius(), player.getRadius())));

                            for(int j=topLeftBucket[0]; j <= bottomRightBucket[0]; j++)
                            {
                                for(int k=topLeftBucket[1]; k <= bottomRightBucket[1]; k++)
                                {
                                    for(Iterator<GameObject> iter = grid.get(j).get(k).iterator(); iter.hasNext();)
                                    {
                                        GameObject food = iter.next();

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
                                            worldData.remove(food);
                                            iter.remove();
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Quadtree quad = new Quadtree(0, new Rectangle(0,0,GameConstants.BOARD_WIDTH,GameConstants.BOARD_HEIGHT));

                    for(GameObject u : userData)
                        if(u.getSubObjectsSize() == 0)
                            quad.insert(u);
                        else
                            for(int i=0; i<u.getSubObjectsSize(); i++)
                                quad.insert(u.getSubObject(i));

                    List<GameObject> returnObjects = new ArrayList<GameObject>();
                    ArrayList<ArrayList<GameObject>> foundPairs = new ArrayList<ArrayList<GameObject>>();
                    for (GameObject u : userData)
                    {
                        for(int i=-1; i<u.getSubObjectsSize(); i++)
                        {
                            if(i < 0 && u.getSubObjectsSize() > 0)
                                continue;

                            GameObject player = i==-1 ? u : u.getSubObject(i);

                            returnObjects.clear();
                            quad.retrieve(returnObjects, player);

                            for (int x = 0; x < returnObjects.size(); x++) {
                                GameObject enemy = returnObjects.get(x);
                                
                                if(player == enemy)
                                    continue;
                                    
                                GameObject bigger = player.getRadius() > enemy.getRadius() ? player : enemy;
                                GameObject smaller = bigger == player ? enemy : player;
                                
                                boolean alreadyCollided = false;
                                for(ArrayList<GameObject> pair : foundPairs)
                                    if(pair.get(0) == bigger && pair.get(1) == smaller)
                                        alreadyCollided = true;
                                if(alreadyCollided)
                                    continue;
                                else
                                {
                                    ArrayList<GameObject> pair = new ArrayList<GameObject>();
                                    pair.add(bigger);
                                    pair.add(smaller);
                                    foundPairs.add(pair);
                                }
                                
                                if(bigger.getParent() != null && bigger.getParent() == smaller.getParent())
                                {
                                    //Collision between subobjects
                                    if(GameConstants.distance(bigger.getPosition(), smaller.getPosition()) > bigger.getRadius())
                                        continue;
                                    
                                    GameObject parent = player.getParent();
                                    
                                    if(parent.canMerge())
                                    {
                                        if(parent.getSubObjectsSize() > 2)
                                        {
                                            bigger.setRadius(Math.sqrt((Math.PI * Math.pow(bigger.getRadius(),2) + Math.PI * Math.pow(smaller.getRadius(),2)) / Math.PI));
                                            parent.removeSubObject(smaller);
                                        }
                                        else
                                        {
                                            parent.setRadius(Math.sqrt((Math.PI * Math.pow(bigger.getRadius(),2) + Math.PI * Math.pow(smaller.getRadius(),2)) / Math.PI));
                                            parent.setPosition(bigger.getPosition());
                                            parent.clearSubObjects();
                                        }
                                    }
                                }
                                else
                                {
                                    //Collision between bigger and smaller
                                    if(!GameConstants.canEat(bigger, smaller))
                                        continue;
                                        
                                    if(smaller.getParent() == null)
                                    {
                                        //Smaller has no parent

                                        bigger.setRadius(Math.sqrt((Math.PI * Math.pow(bigger.getRadius(),2) + 
                                                    Math.PI * Math.pow(smaller.getRadius(),2)) / Math.PI));
                                        smaller.setRadius(0);
                                    }
                                    else
                                    {
                                        //Smaller is a subObject

                                        bigger.setRadius(Math.sqrt((Math.PI * Math.pow(bigger.getRadius(),2) + 
                                                    Math.PI * Math.pow(smaller.getRadius(),2)) / Math.PI));
                                        GameObject smallerParent = smaller.getParent();
                                        smallerParent.removeSubObject(smaller);
                                        if(smallerParent.getSubObjectsSize() == 1)
                                        {
                                            smallerParent.setRadius(smallerParent.getSubObject(0).getRadius());
                                            smallerParent.setPosition(smallerParent.getSubObject(0).getPosition());
                                            smallerParent.clearSubObjects();
                                        }
                                    }
                                }
                            }
                        }
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

        private ArrayList<GameObject> populateFood()
        {
            synchronized(LOCK) {
                ArrayList<GameObject> added = new ArrayList<GameObject>();
                while(worldData.size() < GameConstants.FOOD_GOAL)
                {
                    GameObject food = new GameObject("F", Math.random() * GameConstants.BOARD_WIDTH,
                            Math.random() * GameConstants.BOARD_HEIGHT,
                            GameConstants.ALLOWED_COLORS[(int)(Math.random() * GameConstants.ALLOWED_COLORS.length)],
                            GameConstants.FOOD_RADIUS);
                    addToWorldData(food);
                    added.add(food);
                }
                return added;
            }
        }

        private void addToWorldData(GameObject g)
        {
            worldData.add(g);
            int[] bucket = GameConstants.gridBucket(g.getPosition());
            grid.get(bucket[0]).get(bucket[1]).add(g);
        }

        private void removeFromWorldData(GameObject g)
        {
            worldData.remove(g);
            int[] bucket = GameConstants.gridBucket(g.getPosition());
            grid.get(bucket[0]).get(bucket[1]).remove(g);
        }

        private void updateBucket(GameObject g, int[] bucket)
        {
            int[] realBucket = GameConstants.gridBucket(g.getPosition());
            grid.get(bucket[0]).get(bucket[1]).remove(g);
            grid.get(realBucket[0]).get(realBucket[1]).add(g);
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