import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.RenderingHints;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.awt.MouseInfo;
import java.awt.Color;
import java.lang.reflect.Field;

public class AgarPanel extends JPanel
{
    private volatile boolean connecting = true;
    private String ip;
    private volatile String status = "Waiting...";
    private String name;
    private Color playerColor;
    private volatile double radius;
    private volatile Vector2D position;
    private MainMenu parent;
    private volatile Socket socket;
    private ArrayList<GameObject> userData = new ArrayList<GameObject>();
    private ArrayList<GameObject> lastUserData = new ArrayList<GameObject>();
    private final Object LOCK = new Object();
    private final Object LOCK2 = new Object();
    private volatile int dataIndex = -1;
    private volatile PrintWriter out;
    private volatile BufferedReader in;
    private volatile boolean willBroadcastName = false;
    private volatile boolean willBroadcastColor = false;
    private volatile double lastUpdate = System.nanoTime();
    private volatile double secondToLastUpdate = System.nanoTime()-10;
    private volatile double lastFrameRendered = System.nanoTime();
    
    public AgarPanel(String ip, String name, Color playerColor, MainMenu parent)
    {
        super();
        this.ip = ip;
        this.parent = parent;
        this.name = name;
        this.playerColor = playerColor;
    }
    
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Dimension d = getSize();
        Font f = new Font("Arial",Font.BOLD,24);
        g.setFont(f);
        ((Graphics2D)g).setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        if(connecting)
        {
            String s = "Connecting to " + ip;
            FontMetrics fm = g.getFontMetrics();
            int x = d.width/2 - fm.stringWidth(s)/2;
            int y = d.height/2 - fm.getHeight();
           
            g.drawString(s,x,y);
            
            x = d.width/2 - fm.stringWidth("Status: " + status)/2;
            g.drawString("Status: " + status,x,y+50);
        }
        else if(position != null)
        {
            double boundRadius = GameConstants.getBoundRadius(radius);
            double scale = GameConstants.BOARD_WIDTH / (boundRadius*2);
            Vector2D estimatedPlayerPosition_unshifted = computeDeltaP(position, (System.nanoTime() - lastUpdate) / 1000000000.0, dataIndex);
            
            g.setColor(Color.WHITE);
            g.fillRect(0,0,(int)GameConstants.BOARD_WIDTH,(int)GameConstants.BOARD_HEIGHT);
            g.setColor(Color.LIGHT_GRAY);
            double firstGridCol = ((boundRadius - estimatedPlayerPosition_unshifted.getX()) % 2.0) * scale;
            double firstGridRow = ((boundRadius - estimatedPlayerPosition_unshifted.getY()) % 2.0) * scale;
            for(int i=0; i < boundRadius*2 + 1; i+=2)
            {
                g.fillRect((int)Math.round(firstGridCol + i * scale), 0, 1, (int)GameConstants.BOARD_HEIGHT);
                g.fillRect(0, (int)Math.round(firstGridRow + i * scale), (int)GameConstants.BOARD_WIDTH, 1);
            }
            
            synchronized(LOCK)
            {
                int index = 0;
                for(GameObject u : userData)
                {
                    //Broken ... fix sometime soon
                    //if(!insideBoundRadius(position, boundRadius, u))
                    //    continue;
                        
                    Vector2D uPosition_unshifted = new Vector2D(u.getX(), u.getY());
                    Vector2D estimatedPosition_unshifted = computeDeltaP(uPosition_unshifted, (System.nanoTime() - lastUpdate) / 1000000000.0, index);
                    Vector2D shiftedPosition = new Vector2D(estimatedPosition_unshifted.getX() - (estimatedPlayerPosition_unshifted.getX() - boundRadius), 
                                                              estimatedPosition_unshifted.getY() - (estimatedPlayerPosition_unshifted.getY() - boundRadius));
                    g.setColor(u.getColor());
                    int x = (int)((shiftedPosition.getX() - u.getRadius()) * scale);
                    int y = (int)((shiftedPosition.getY() - u.getRadius()) * scale);
                    g.fillArc(x, y, (int)(u.getRadius() * 2 * scale), (int)(u.getRadius() * 2 * scale), 0, 360);
                    
                    f = new Font("Arial",Font.BOLD,12);
                    g.setFont(f);
                    int sizeNeeded = SwingUtils.getMaxFittingFontSize(g, f, u.getName(), (int)(radius * 2 * scale), (int)(radius * 0.5 * scale));
                    f = new Font("Arial",Font.BOLD,sizeNeeded);
                    g.setFont(f);
                    FontMetrics fm = g.getFontMetrics();
                    x = (int)(shiftedPosition.getX() * scale - fm.stringWidth(u.getName())/2);
                    y = (int)(shiftedPosition.getY() * scale + fm.getHeight() / 2);
                    
                    SwingUtils.outlineText(g, u.getName(), x, y, Color.BLACK, Color.WHITE);
                    index++;
                }
            }
            
            g.setColor(Color.BLACK);
            g.drawString("FPS: " + (int)(1/((System.nanoTime() - lastFrameRendered) / 1000000000.0))
                            + " (" + (int)position.getX() + "," + (int)position.getY() + ")", 10, 
                            (int)GameConstants.BOARD_HEIGHT - g.getFontMetrics().getHeight());
        }
        lastFrameRendered = System.nanoTime();
    }
    
    private boolean insideBoundRadius(Vector2D position, double boundRadius, GameObject u)
    {
        double leftWall = position.getX() - boundRadius - u.getRadius();
        double rightWall = position.getX() + boundRadius + u.getRadius();
        double bottomWall = position.getY() - boundRadius - u.getRadius();
        double topWall = position.getY() + boundRadius + u.getRadius();
        
        return u.getX() > leftWall && u.getX() < rightWall &&
                u.getY() > bottomWall && u.getY() < topWall;
    }
    
    private Vector2D computeDeltaP(Vector2D position, double deltaTime, int index)
    {
        if(lastUserData == null || lastUserData.size() == 0 || 
           !lastUserData.get(index).getName().equals(userData.get(index).getName()))
            return position;
        
        Vector2D dP = position.minus(new Vector2D(lastUserData.get(index).getX(), lastUserData.get(index).getY()));
        double dT = (lastUpdate - secondToLastUpdate) / 1000000000.0;
        Vector2D velocity = dP.scalarMult(1.0/dT);
        
        Vector2D predictedDP = velocity.scalarMult(deltaTime);
        return position.plus(predictedDP);
    }
    
    public void connect()
    {
        (new Thread(new Connector())).start();
    }
    
    private class Connector implements Runnable
    {
        public void run()
        {
            status = "Connecting...";
            try{
                Thread.sleep(100);
                socket = new Socket(ip, 40124);
                out = new PrintWriter(socket.getOutputStream(), 
                         true);
                in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
            } catch (Exception e) {
                System.out.println("Unknown host");
                status = "Error, unknown host.";
                AgarPanel.this.repaint();
                try{Thread.sleep(2000);}catch(Exception ex){}
                parent.endGame();
                return;
            }
            
            status = "Connected.";
            connecting = false;
            (new Thread(new GameUpdaterIn())).start();
            (new Thread(new GameUpdaterOut())).start();
            (new Thread(new Repainter())).start();
        }
    }
    
    private class Repainter implements Runnable
    {
        public void run()
        {
            while(true)
            {
                try {
                    AgarPanel.this.repaint();
                    Thread.sleep(15);
                } catch(Exception e){}
            }
        }
    }
    
    private class GameUpdaterIn implements Runnable
    {   
        public void run()
        {
            double accel = 5;
            while(true)
            {
                try{
                    String message = in.readLine();
                    if(message.equals("NAME"))
                    {
                        willBroadcastName = true;
                    }
                    else if(message.equals("COLOR"))
                    {
                        willBroadcastColor = true;
                    }
                    else
                    {
                        String[] messageContents = message.split(",");
                        dataIndex = Integer.parseInt(messageContents[0]);
                        double radius = AgarPanel.this.radius;
                        Vector2D position = AgarPanel.this.position;
                        synchronized(LOCK){
                            lastUserData = userData;
                            userData = new ArrayList<GameObject>();
                            for(int i=1; i<messageContents.length; i++)
                            {
                                String[] u = messageContents[i].split("\\|");
                                userData.add(new GameObject(u[0], Double.parseDouble(u[1]), Double.parseDouble(u[2]),
                                                            GameConstants.stringToColor(u[3]), Double.parseDouble(u[4])));
                            }
                            radius = userData.get(dataIndex).getRadius();
                            position = new Vector2D(userData.get(dataIndex).getX(), userData.get(dataIndex).getY());
                        }
                        AgarPanel.this.radius = radius;
                        AgarPanel.this.position = position;
                        secondToLastUpdate = lastUpdate;
                        lastUpdate = System.nanoTime();
                    }
                    Thread.sleep(1);
                } catch (Exception e) {System.out.println(e);}
            }
        }
    }
    
    private class GameUpdaterOut implements Runnable
    {   
        public void run()
        {
            while(true)
            {
                try{
                    if(willBroadcastName)
                    {
                        out.println("NAME " + name);
                        willBroadcastName = false;
                    }
                    else if(willBroadcastColor)
                    {
                        out.println("COLOR " + GameConstants.colorToString(playerColor));
                        willBroadcastColor = false;
                    }
                    else if(dataIndex != -1 && userData.size() > 0)
                    {
                        double xPos = MouseInfo.getPointerInfo().getLocation().getX() - AgarPanel.this.getLocationOnScreen().getX();
                        double yPos = MouseInfo.getPointerInfo().getLocation().getY() - AgarPanel.this.getLocationOnScreen().getY();
                        
                        Vector2D velocity;
                        synchronized(LOCK){
                            velocity = new Vector2D(xPos - AgarPanel.this.getSize().getWidth()/2, yPos - AgarPanel.this.getSize().getHeight()/2);
                        }
                        velocity = velocity.scalarMult(1.0/30.0);
                        out.println(velocity.getX() + "," + velocity.getY());
                    }
                    Thread.sleep(100);
                } catch (Exception e) {System.out.println(e);}
            }
        }
    }
}