import java.net.Socket;
import java.io.IOException;
import java.io.ObjectInputStream;

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

    private Vector2D position = new Vector2D();
    private Vector2D velocity = new Vector2D();
    private Vector2D acceleration = new Vector2D();

    /**
     * Handles all incoming data from this user.
     */
    private class Inport extends Thread
    {
        private ObjectInputStream in;
        public void run()
        {
            // Open the InputStream
            try
            {
                in = new ObjectInputStream(socket.getInputStream());
            }
            catch(IOException e)
            {
                System.out.println("Could not get input stream from "+toString());
                return;
            }
            // Announce
            System.out.println(socket+" has connected input.");
            // Enter process loop
            while(true)
            {
                move(USER_THROTTLE);
                try
                {
                    Thread.sleep(USER_THROTTLE);
                }
                catch(Exception e)
                {
                    System.out.println(toString()+" has input interrupted.");
                }
            }
        }
    }
    /**
     * Creates a new Client User with the socket from the newly connected client.
     *
     * @param newSocket  The socket from the connected client.
     */
    public User(Socket newSocket)
    {
        // Set properties
        socket = newSocket;
        connected = true;
        // Get input
        inport = new Inport();
        inport.start();
    }

    /**
     * Gets the connection status of this user.
     *
     * @return  If this user is still connected.
     */
    public boolean isConnected()
    {
        return connected;
    }

    /**
     * Purges this user from connection.
     */
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

    /**
     * Returns the String representation of this user.
     *
     * @return  A string representation.
     */
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
        Vector2D deltaV = acceleration.scalarMult(deltaTime);
        velocity = velocity.plus(deltaV);
        if(velocity.length() > MAX_VELOCITY)
            velocity = velocity.unitVector().scalarMult(MAX_VELOCITY);
        
        Vector2D deltaP = velocity.scalarMult(deltaTime);
        position = position.plus(deltaP);
    }
}

