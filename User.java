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
    private int MAX_VELOCITY = 30;
    private Socket socket;
    private boolean connected;
    private InputHandler inputHandler;
    private OutputHandler outputHandler;
    private volatile int dataIndex;
    private ArrayList<GameObject> userData;
    private volatile String name = "";
    private volatile boolean receivedNameRequest = false;
    private volatile boolean receivedColorRequest = false;
    private volatile boolean poison = false;
    private volatile Color playerColor = Color.BLACK;
    
    private Vector2D position;
    private Vector2D velocity = new Vector2D();
    private volatile double accelerationX = 0.0;
    private volatile double accelerationY = 0.0;
    private volatile long lastMessage = System.nanoTime();
    private final double TIMEOUT = 2.0;
    private final Object LOCK;
    
    public User(Socket newSocket, ArrayList<GameObject> userData, Object LOCK)
    {
        // Set properties
        socket = newSocket;
        connected = true;
        inputHandler = new InputHandler();
        inputHandler.start();
        outputHandler = new OutputHandler();
        outputHandler.start();
        this.userData = userData;
        this.LOCK = LOCK;
        
        position = new Vector2D(Math.random() * GameConstants.BOARD_WIDTH, Math.random() * GameConstants.BOARD_HEIGHT);
        userData.add(new GameObject(name, position.getX(), position.getY(), playerColor));
        dataIndex = userData.size() - 1;
        (new Thread(new DisconnectWatcher(this))).start();
    }
    
    private class DisconnectWatcher extends Thread
    {
        private User user;
        
        public DisconnectWatcher(User user)
        {
            this.user = user;
        }
        
        public void run()
        {
            while(true)
            {
                double lag = (System.nanoTime() - lastMessage) / 1000000000;
                if(lag > TIMEOUT)
                {
                    user.purge();
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
                        synchronized(LOCK)
                        {
                            userData.get(dataIndex).setName(name);
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
                    else if(message.length() > 0)
                    {
                        accelerationX = Double.parseDouble(message.split(",")[0]);
                        accelerationY = Double.parseDouble(message.split(",")[1]);
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
    }
    
    public void move(double deltaTime)
    {   
        velocity = new Vector2D(accelerationX, accelerationY);
        if(velocity.length() > MAX_VELOCITY)
            velocity = velocity.unitVector().scalarMult(MAX_VELOCITY);
            
        Vector2D deltaP = velocity.scalarMult(deltaTime);
        position = position.plus(deltaP);
        
        userData.get(dataIndex).setX(position.getX());
        userData.get(dataIndex).setY(position.getY());
    }
    
    public void setIndex(int index)
    {
        this.dataIndex = index;
    }
    
    private String getBoardData()
    {
        String result = "" + dataIndex + ",";
        for(GameObject userObj : userData)
        {
            result += userObj.getName() + "|" + userObj.getX() + "|" + userObj.getY() + "|" + GameConstants.colorToString(userObj.getColor()) + ",";
        }
        result = result.substring(0, result.length()-1);
        return result;
    }
}

