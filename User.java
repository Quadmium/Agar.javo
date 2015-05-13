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
    private static final int USER_THROTTLE = 33;
    private int MAX_VELOCITY = 3;
    private Socket socket;
    private boolean connected;
    private Inport inport;
    private int dataIndex;
    private volatile ArrayList<GameObject> userData;

    private Vector2D position = new Vector2D(200.0, 200.0);
    private Vector2D velocity = new Vector2D();
    private volatile double accelerationX = 0.0;
    private volatile double accelerationY = 0.0;
    
    public User(Socket newSocket, ArrayList<GameObject> userData)
    {
        // Set properties
        socket = newSocket;
        connected = true;
        // Get input
        inport = new Inport();
        inport.start();
        this.userData = userData;
        
        userData.add(new GameObject("", 200.0, 200.0));
        dataIndex = userData.size() - 1;
    }
    
    private class Inport extends Thread
    {
        private BufferedReader in;
        private PrintWriter out;
        public void run()
        {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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
                try
                {
                    String message = in.readLine();
                    if(message.length() > 0)
                    {
                        accelerationX = Double.parseDouble(message.split(",")[0]);
                        accelerationY = Double.parseDouble(message.split(",")[1]);
                    }
                    out.println(getBoardData());
                    Thread.sleep(USER_THROTTLE);
                }
                catch(Exception e)
                {
                    System.out.println(toString()+" has input interrupted.");
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
        Vector2D acceleration = new Vector2D(accelerationX, accelerationY);
        Vector2D deltaV = acceleration.scalarMult(deltaTime);
        velocity = velocity.plus(deltaV);
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

