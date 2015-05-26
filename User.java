import java.net.Socket;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.awt.Color;
import java.util.Timer;
import java.util.TimerTask;

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
    private ArrayList<GameObject> subObjects = new ArrayList<GameObject>();
    private volatile String name = "";
    private volatile boolean receivedNameRequest = false;
    private volatile boolean receivedColorRequest = false;
    private volatile boolean poison = false;
    private volatile Color playerColor = Color.BLACK;
    private double radius = GameConstants.INITIAL_RADIUS;
    private volatile boolean willBroadcastWorld = false;
    private volatile boolean receivedInitialWorldFromServer = false;

    private Vector2D position;
    private Vector2D velocity = new Vector2D();
    private volatile double velocityX = 0.0;
    private volatile double velocityY = 0.0;
    private volatile long lastMessage = System.nanoTime();
    private final double TIMEOUT = 2.0;
    private final Object LOCK;
    
    private int mergeTimerID = 0;

    public User(Socket newSocket, ArrayList<GameObject> userData, Object LOCK)
    {
        // Set properties
        socket = newSocket;
        connected = true;
        this.userData = userData;
        this.LOCK = LOCK;
        position = new Vector2D(450,450);//Math.random() * GameConstants.BOARD_WIDTH, Math.random() * GameConstants.BOARD_HEIGHT);
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
                        if(!java.util.Arrays.asList(GameConstants.COLOR_STRINGS).contains(message.substring(6)))
                        {
                            User.this.purge();
                            return;
                        }
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
                    else if(message.equals("SPLIT"))
                    {
                        splitCharacter();
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
                    out.flush();
                    Thread.sleep(USER_THROTTLE);
                }
                catch(Exception e)
                {
                    System.out.println(e);
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
        synchronized(LOCK)
        {
            double maxV;
            velocity = new Vector2D(velocityX, velocityY);
            //Others can change subobjects
            if(userData.get(dataIndex).getSubObjectsSize() != subObjects.size())
            {
                subObjects.clear();
                for(int i=0; i<userData.get(dataIndex).getSubObjectsSize(); i++)
                    subObjects.add(userData.get(dataIndex).getSubObject(i));
            }
            
            //Others can change radius
            radius = userData.get(dataIndex).getRadius();
            if(radius == 0)
            {
                purge();
                return;
            }

            if(subObjects.size() == 0)
            {
                maxV = GameConstants.maximumVelocity(radius);

                if(velocity.length() > maxV)
                    velocity = velocity.unitVector().scalarMult(maxV);

                Vector2D deltaP = velocity.scalarMult(deltaTime);
                position = position.plus(deltaP);

                if(position.getX() + radius > GameConstants.BOARD_WIDTH)
                    position = new Vector2D(GameConstants.BOARD_WIDTH - radius, position.getY());
                if(position.getY() + radius > GameConstants.BOARD_HEIGHT)
                    position = new Vector2D(position.getX(), GameConstants.BOARD_HEIGHT - radius);
                if(position.getX() - radius < 0)
                    position = new Vector2D(radius, position.getY());
                if(position.getY() - radius < 0)
                    position = new Vector2D(position.getX(), radius);

                userData.get(dataIndex).setX(position.getX());
                userData.get(dataIndex).setY(position.getY());
            }
            else
            {
                Vector2D avgPos = new Vector2D(0,0);
                int index = 0;
                for(GameObject g : subObjects)
                {
                    avgPos = avgPos.plus(g.getPosition());
                    maxV = GameConstants.maximumVelocity(g.getRadius());
                    Vector2D localVelocity = new Vector2D(velocity);
                    if(localVelocity.length() > maxV)
                        localVelocity = localVelocity.unitVector().scalarMult(maxV);
                        
                    if(g.getVelocity().length() > 0)
                    {
                        localVelocity = localVelocity.plus(g.getVelocity());
                        g.setVelocity(g.getVelocity().unitVector().scalarMult(g.getVelocity().length() - GameConstants.SPLIT_DECELERATION * g.getRadius() * deltaTime));
                        if(g.getVelocity().length() < 0)
                            g.setVelocity(new Vector2D(0,0));
                    }
                    
                    Vector2D deltaP = localVelocity.scalarMult(deltaTime);
                    Vector2D localPosition = g.getPosition().plus(deltaP);

                    if(localPosition.getX() + g.getRadius() > GameConstants.BOARD_WIDTH)
                        localPosition = new Vector2D(GameConstants.BOARD_WIDTH - g.getRadius(), localPosition.getY());
                    if(localPosition.getY() + g.getRadius() > GameConstants.BOARD_HEIGHT)
                        localPosition = new Vector2D(localPosition.getX(), GameConstants.BOARD_HEIGHT - g.getRadius());
                    if(localPosition.getX() - g.getRadius() < 0)
                        localPosition = new Vector2D(g.getRadius(), localPosition.getY());
                    if(localPosition.getY() - g.getRadius() < 0)
                        localPosition = new Vector2D(localPosition.getX(), g.getRadius());

                    g.setX(localPosition.getX());
                    g.setY(localPosition.getY());
                    userData.get(dataIndex).getSubObject(index).setX(g.getPosition().getX());
                    userData.get(dataIndex).getSubObject(index).setY(g.getPosition().getY());
                    index++;
                }

                avgPos = avgPos.scalarMult(1.0 / index);
                userData.get(dataIndex).setX(avgPos.getX());
                userData.get(dataIndex).setY(avgPos.getY());
                userData.get(dataIndex).setRadius(GameConstants.calculateCombinedRadius(subObjects));
            }
        }
    }

    private void splitCharacter()
    {
        synchronized(LOCK)
        {
            if(subObjects.size() == 0)
            {
                if(radius < GameConstants.MIN_SPLIT_RADIUS)
                    return;
                    
                double dx = 0;//velocity.unitVector().scalarMult(radius / Math.sqrt(2)).getX();
                double dy = 0;//velocity.unitVector().scalarMult(radius / Math.sqrt(2)).getY();
                /*dx *= 1.2;
                dy *= 1.2;*/
                subObjects.add(new GameObject(name, position.getX() + dx, position.getY() + dy, playerColor, radius / Math.sqrt(2)));
                subObjects.add(new GameObject(name, position.getX() - dx, position.getY() - dy, playerColor, radius / Math.sqrt(2)));
                subObjects.get(0).setVelocity(velocity.unitVector().scalarMult(GameConstants.SPLIT_VELOCITY_BOOST * subObjects.get(0).getRadius()));
                syncSubObjects();
                mergeTimerID++;
                userData.get(dataIndex).setMerge(false);
                startMergeTimer(mergeTimerID);
            }
            else
            {
                if(subObjects.size() >= GameConstants.MAX_SPLIT)
                    return;
                    
                int size = subObjects.size();
                for(int i=0; i<size; i++)
                {
                    GameObject obj = subObjects.get(i);
                    if(obj.getRadius() < GameConstants.MIN_SPLIT_RADIUS)
                        continue;
                    double dx = 0;//velocity.unitVector().scalarMult(obj.getRadius() / Math.sqrt(2)).getX();
                    double dy = 0;//velocity.unitVector().scalarMult(obj.getRadius() / Math.sqrt(2)).getY();
                    /*dx *= 1.2;
                    dy *= 1.2;*/
                    GameObject objSplit = new GameObject(obj.getName(), obj.getPosition().getX() - dx, obj.getPosition().getY() - dy, obj.getColor(), obj.getRadius() / Math.sqrt(2));
                    obj.setX(obj.getPosition().getX() + dx);
                    obj.setY(obj.getPosition().getY() + dy);
                    obj.setRadius(obj.getRadius() / Math.sqrt(2));
                    obj.setVelocity(velocity.unitVector().scalarMult(GameConstants.SPLIT_VELOCITY_BOOST * obj.getRadius()));
                    subObjects.add(objSplit);
                }
                syncSubObjects();
                mergeTimerID++;
                userData.get(dataIndex).setMerge(false);
                startMergeTimer(mergeTimerID);
            }
        }
    }
    
    private void startMergeTimer(int ID)
    {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized(LOCK)
                {
                    if(mergeTimerID == ID)
                        userData.get(dataIndex).setMerge(true);
                }
            }
        }, GameConstants.MERGE_DELAY);
    }
    
    private void syncSubObjects()
    {
        radius = GameConstants.calculateCombinedRadius(subObjects);
        userData.get(dataIndex).clearSubObjects();
        for(GameObject g : subObjects)
            userData.get(dataIndex).addSubObject(g);
        userData.get(dataIndex).setRadius(radius);
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
                    + GameConstants.colorToString(userObj.getColor()) + "|" + userObj.getRadius() + "#");

                boolean changed2 = false;
                for(int i=0; i<userObj.getSubObjectsSize(); i++)
                {
                    changed2 = true;
                    GameObject sub = userObj.getSubObject(i);
                    result.append(sub.getName() + "|" + sub.getX() + "|" + sub.getY() + "|" 
                        + GameConstants.colorToString(sub.getColor()) + "|" + sub.getRadius() + "#");
                }
                if(changed2)
                    result.deleteCharAt(result.length()-1);

                result.append(",");
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

            result.append("&" + worldData.size());

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

