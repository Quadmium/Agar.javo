import java.net.Socket;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * This object handles the execution for a single user.
 */
public class User
{
    private static final int USER_THROTTLE = 10;
    private int MAX_VELOCITY = 30;
    private Socket socket;
    private boolean connected;
    private InputHandler inputHandler;
    private OutputHandler outputHandler;
    private int dataIndex;
    private volatile ArrayList<GameObject> userData;
    private String name = "";
    private boolean receivedNameRequest = false;
    private boolean poison = false;
    
    private Vector2D position = new Vector2D(200.0, 200.0);
    private Vector2D velocity = new Vector2D();
    private volatile double accelerationX = 0.0;
    private volatile double accelerationY = 0.0;
    private volatile long lastMessage = System.nanoTime();
    private final double TIMEOUT = 5.0;
    
    public User(Socket newSocket, ArrayList<GameObject> userData)
    {
        // Set properties
        socket = newSocket;
        connected = true;
        inputHandler = new InputHandler();
        inputHandler.start();
        outputHandler = new OutputHandler();
        outputHandler.start();
        this.userData = userData;
        
        userData.add(new GameObject(name, 200.0, 200.0));
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
                        userData.get(dataIndex).setName(name);
                        receivedNameRequest = true;
                    }
                    else if(message.length() > 0)
                    {
                        accelerationX = Double.parseDouble(message.split(",")[0]);
                        accelerationY = Double.parseDouble(message.split(",")[1]);
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
        /*
        Vector2D acceleration = new Vector2D(accelerationX, accelerationY);
        Vector2D deltaV = acceleration.scalarMult(deltaTime);
        velocity = velocity.plus(deltaV);
        if(velocity.length() > MAX_VELOCITY)
            velocity = velocity.unitVector().scalarMult(MAX_VELOCITY);*/
        
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
            result += userObj.getName() + "|" + userObj.getX() + "|" + userObj.getY() + ",";
        }
        result = result.substring(0, result.length()-1);
        return result;
    }
}

