import java.net.Socket;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.awt.Color;

/**
 * This object handles the execution for a single user.
 */
public class User
{
    private static final int USER_THROTTLE = 100;
    private static final double MAX_VELOCITY = 6;
    private static final double MIN_VELOCITY = 0.5;
    private Socket socket;
    private boolean connected;
    private InputHandler inputHandler;
    private OutputHandler outputHandler;
    private volatile int dataIndex;
    private ArrayList<GameObject> userData;
    private ArrayList<GameObject> worldData = new ArrayList<GameObject>();
    private ArrayList<GameObject> worldRemoved = new ArrayList<GameObject>();
    private ArrayList<GameObject> worldAdded = new ArrayList<GameObject>();
    private volatile String name = "";
    private volatile boolean receivedNameRequest = false;
    private volatile boolean receivedColorRequest = false;
    private volatile boolean poison = false;
    private volatile Color playerColor = Color.BLACK;
    private double radius = 1.0;//GameConstants.INITIAL_RADIUS;//(int)(Math.random() * 10) + 1;//GameConstants.INITIAL_RADIUS;
    private volatile boolean willBroadcastWorld = false;
    private volatile boolean receivedInitialWorldFromServer = false;
    
    private Vector2D position;
    private Vector2D velocity = new Vector2D();
    private volatile double velocityX = 0.0;
    private volatile double velocityY = 0.0;
    private volatile long lastMessage = System.nanoTime();
    private final double TIMEOUT = 2.0;
    private final Object LOCK;
    private final Object LOCK2 = new Object();
    
    public User(Socket newSocket, ArrayList<GameObject> userData, Object LOCK)
    {
        // Set properties
        socket = newSocket;
        connected = true;
        this.userData = userData;
        this.LOCK = LOCK;
        position = new Vector2D(Math.random() * GameConstants.BOARD_WIDTH, Math.random() * GameConstants.BOARD_HEIGHT);
        userData.add(new GameObject(name, position.getX(), position.getY(), playerColor, radius));
        dataIndex = userData.size() - 1;
        inputHandler = new InputHandler();
        inputHandler.start();
        outputHandler = new OutputHandler();
        outputHandler.start();
        (new Thread(new DisconnectWatcher())).start();
    }
    
    private class DisconnectWatcher extends Thread
    {
        public void run()
        {
            while(true)
            {
                double lag = (System.nanoTime() - lastMessage) / 1000000000.0;
                if(lag > TIMEOUT)
                {
                    User.this.purge();
                    return;
                }
                try{Thread.sleep(500);}catch(Exception e){}
            }
        }
    }
    
    private class InputHandler extends Thread
    {
        private BufferedReader in;
        public void run()
        {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            }
            catch(IOException e)
            {
                System.out.println("Could not get stream from "+toString());
                return;
            }
            // Announce
            System.out.println(socket+" has connected input.");
            // Enter process loop
            while(true)
            {
                if(poison)
                    return;
                try
                {
                    String message = in.readLine();
                    lastMessage = System.nanoTime();
                    if(message.startsWith("NAME "))
                    {
                        name = message.substring(5);
                        if(name.contains("|") || name.contains(",") || name.contains("&"))
                        {
                            User.this.purge();
                            return;
                        }
                        synchronized(LOCK)
                        {
                            userData.get(dataIndex).setName(name);
                            if(name.equals("Cheater"))
                            {
                                radius += 10;
                                userData.get(dataIndex).setRadius(radius);
                            }
                        }
                        receivedNameRequest = true;
                    }
                    else if(message.startsWith("COLOR "))
                    {
                        playerColor = GameConstants.stringToColor(message.substring(6));
                        synchronized(LOCK)
                        {
                            userData.get(dataIndex).setColor(playerColor);
                        }
                        receivedColorRequest = true;
                    }
                    else if(message.startsWith("WORLD"))
                    {
                        willBroadcastWorld = true;
                    }
                    else if(message.length() > 0)
                    {
                        velocityX = Double.parseDouble(message.split(",")[0]);
                        velocityY = Double.parseDouble(message.split(",")[1]);
                    }
                    Thread.sleep(1);
                }
                catch(Exception e)
                {
                    //System.out.println(toString()+" has input interrupted.");
                }
            }
        }
    }
    
    private class OutputHandler extends Thread
    {
        private PrintWriter out;
        public void run()
        {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
            }
            catch(IOException e)
            {
                System.out.println("Could not get stream from "+toString());
                return;
            }
            // Announce
            System.out.println(socket+" has connected input.");
            // Enter process loop
            while(true)
            {
                if(poison)
                    return;
                try
                {
                    if(!receivedNameRequest)
                    {
                        out.println("NAME");
                    }
                    else if(!receivedColorRequest)
                    {
                        out.println("COLOR");
                    }
                    else if(receivedInitialWorldFromServer && willBroadcastWorld)
                    {
                        out.println("WORLDFULL " + getWorldData());
                        willBroadcastWorld = false;
                    }
                    else
                    {
                        out.println(getBoardData());
                    }
                    Thread.sleep(USER_THROTTLE);
                }
                catch(Exception e)
                {
                    //System.out.println(toString()+" has input interrupted.");
                }
            }
        }
    }
    
    public boolean isConnected()
    {
        return connected;
    }
    
    public void purge()
    {
        // Close everything
        try
        {
            connected = false;
            socket.close();
            poison = true;
        }
        catch(IOException e)
        {
            System.out.println("Could not purge "+socket+".");
        }
    }
    
    public String toString()
    {
        return new String(socket.toString());
    }

    public void setPos(double x, double y)
    {
        position = new Vector2D(x, y);
        userData.get(dataIndex).setX(position.getX());
        userData.get(dataIndex).setY(position.getY());
    }
    
    public void move(double deltaTime)
    {   
        double INITIAL_RADIUS = 1.0; //Do not change
        double INITIAL_VELOCITY = 6.0;
        double FINAL_RADIUS = 10.0;
        double FINAL_VELOCITY = 3.0;
        double k = INITIAL_VELOCITY;
        double n = Math.log(FINAL_VELOCITY / INITIAL_VELOCITY) / Math.log(FINAL_RADIUS);
        
        double maxV;
        synchronized(LOCK2)
        {
            maxV = k * Math.pow(radius, n);
        }
        
        velocity = new Vector2D(velocityX, velocityY);
        if(velocity.length() > maxV)
            velocity = velocity.unitVector().scalarMult(maxV);
            
        Vector2D deltaP = velocity.scalarMult(deltaTime);
        position = position.plus(deltaP);
        
        synchronized(LOCK2)
        {
            if(position.getX() + radius > GameConstants.BOARD_WIDTH)
                position = new Vector2D(GameConstants.BOARD_WIDTH - radius, position.getY());
            if(position.getY() + radius > GameConstants.BOARD_HEIGHT)
                position = new Vector2D(position.getX(), GameConstants.BOARD_HEIGHT - radius);
            if(position.getX() - radius < 0)
                position = new Vector2D(radius, position.getY());
            if(position.getY() - radius < 0)
                position = new Vector2D(position.getX(), radius);
        }
            
        userData.get(dataIndex).setX(position.getX());
        userData.get(dataIndex).setY(position.getY());
    }
    
    public void setWorld(ArrayList<GameObject> worldData)
    {
        receivedInitialWorldFromServer = true;
        this.worldData = worldData;
    }
    
    public void addToWorldRemoved(GameObject worldObj)
    {
        worldRemoved.add(worldObj);
    }
    
    public void addToWorldAdded(GameObject worldObj)
    {
        worldAdded.add(worldObj);
    }
    
    public void setIndex(int index)
    {
        this.dataIndex = index;
    }
    
    private String getBoardData()
    {
        StringBuilder result = new StringBuilder("" + dataIndex + ",");
        synchronized(LOCK)
        {
            boolean changed = false;
            for(GameObject userObj : userData)
            {
                changed = true;
                result.append(userObj.getName() + "|" + userObj.getX() + "|" + userObj.getY() + "|" 
                            + GameConstants.colorToString(userObj.getColor()) + "|" + userObj.getRadius() + ",");
            }
            if(changed)
                result.deleteCharAt(result.length()-1);
            result.append("&");
            
            changed = false;
            for(GameObject worldObj : worldRemoved)
            {
                changed = true;
                result.append(worldObj.getName() + "|" + worldObj.getX() + "|" + worldObj.getY() + "|" 
                            + GameConstants.colorToString(worldObj.getColor()) + "|" + worldObj.getRadius() + ",");
            }
            if(changed)
                result.deleteCharAt(result.length()-1);
            result.append("&");
            
            changed = false;
            for(GameObject worldObj : worldAdded)
            {
                changed = true;
                result.append(worldObj.getName() + "|" + worldObj.getX() + "|" + worldObj.getY() + "|" 
                            + GameConstants.colorToString(worldObj.getColor()) + "|" + worldObj.getRadius() + ",");
            }
            if(changed)
                result.deleteCharAt(result.length()-1);
                
            worldRemoved.clear();
            worldAdded.clear();
        }
        return result.toString();
    }
    
    private String getWorldData()
    {
        StringBuilder result = new StringBuilder();
        synchronized(LOCK)
        {
            for(GameObject worldObj : worldData)
            {
                result.append(worldObj.getName() + "|" + worldObj.getX() + "|" + worldObj.getY() + "|" 
                            + GameConstants.colorToString(worldObj.getColor()) + "|" + worldObj.getRadius() + ",");
            }
        }
        return result.substring(0, result.length()-1);
    }
}

