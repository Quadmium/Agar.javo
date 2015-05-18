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
import java.awt.Polygon;

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
    private ArrayList<GameObject> worldData = new ArrayList<GameObject>();
    private final Object LOCK = new Object();
    private volatile int dataIndex = -1;
    private volatile PrintWriter out;
    private volatile BufferedReader in;
    private volatile boolean willBroadcastName = false;
    private volatile boolean willBroadcastColor = false;
    private volatile boolean receivedWorld = false;
    private volatile double lastUpdate = System.nanoTime();
    private volatile double secondToLastUpdate = System.nanoTime()-10;
    private volatile double lastFrameRendered = System.nanoTime();
    private static final double FOOD_ROTATE_PERIOD = 2;
    private volatile double foodFrame = 0;
    
    //TODO: Kill threads on agarPanel end
    
    public AgarPanel(String ip, String name, Color playerColor, MainMenu parent)
    {
        super();
        setDoubleBuffered(true);
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
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
RenderingHints.VALUE_ANTIALIAS_ON);
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_INTERPOLATION,
RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        
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
            synchronized(LOCK)
            {
                double deltaTime = (System.nanoTime() - lastFrameRendered) / 1000000000.0;
                double boundRadius = GameConstants.getBoundRadius(radius);
                int offsetX = 0;
                int offsetY = 0;
                int square;
                if(getWidth() > getHeight())
                {
                    square = getHeight();
                    offsetX = (getWidth() - square) / 2;
                }
                else
                {
                    square = getWidth();
                    offsetY = (getHeight() - square) / 2;
                }
                double scale = square / (boundRadius*2);
                Vector2D estimatedPlayerPosition_unshifted = computeDeltaP(position, (System.nanoTime() - lastUpdate) / 1000000000.0, dataIndex);
                
                g.setColor(Color.WHITE);
                g.fillRect(offsetX,offsetY,square,square);
                g.setColor(new Color(232, 232, 232));
                double firstGridCol = ((boundRadius - estimatedPlayerPosition_unshifted.getX()) % 2.0) * scale;
                double firstGridRow = ((boundRadius - estimatedPlayerPosition_unshifted.getY()) % 2.0) * scale;
                for(int i=0; i < boundRadius*2 + 2; i+=2)
                {
                    g.fillRect(offsetX + (int)(firstGridCol) + (int)(i * scale), offsetY, 1, square);
                    g.fillRect(offsetX, offsetY + (int)(firstGridRow) + (int)(i * scale), square, 1);
                }
                
                if(foodFrame > FOOD_ROTATE_PERIOD)
                    foodFrame -= FOOD_ROTATE_PERIOD;
                
                double foodAngle = 2 * Math.PI * foodFrame / FOOD_ROTATE_PERIOD;
                for(GameObject obj : worldData)
                {
                    if(!GameConstants.insideBoundRadius(position, boundRadius, obj))
                        continue;
                        
                    Vector2D objPosition_unshifted = new Vector2D(obj.getX(), obj.getY());
                    Vector2D shiftedPosition = new Vector2D(objPosition_unshifted.getX() - (estimatedPlayerPosition_unshifted.getX() - boundRadius), 
                                                              objPosition_unshifted.getY() - (estimatedPlayerPosition_unshifted.getY() - boundRadius));
                                                              
                    g.setColor(obj.getColor());
                    int x = (int)((shiftedPosition.getX() - obj.getRadius()) * scale);
                    int y = (int)((shiftedPosition.getY() - obj.getRadius()) * scale);
                    
                    double r = obj.getRadius() * scale;
                    Polygon food = new Polygon();
                    food.addPoint(offsetX + (int)(x + r * Math.cos(foodAngle)), offsetY + (int)(y + r * Math.sin(foodAngle)));
                    food.addPoint(offsetX + (int)(x + r * Math.cos(foodAngle + Math.PI / 2)), offsetY + (int)(y + r * Math.sin(foodAngle + Math.PI / 2)));
                    food.addPoint(offsetX + (int)(x + r * Math.cos(foodAngle + Math.PI)), offsetY + (int)(y + r * Math.sin(foodAngle + Math.PI)));
                    food.addPoint(offsetX + (int)(x + r * Math.cos(foodAngle + 3 * Math.PI / 2)), offsetY + (int)(y + r * Math.sin(foodAngle + 3 * Math.PI / 2)));
                    g.fillPolygon(food);
                }
                foodFrame += deltaTime;
                
                int index = 0;
                for(GameObject u : userData)
                {
                    if(!GameConstants.insideBoundRadius(position, boundRadius, u) || u.getRadius() == 0)
                    {
                        index++;
                        continue;
                    }
                    
                    Vector2D uPosition_unshifted = new Vector2D(u.getX(), u.getY());
                    Vector2D estimatedPosition_unshifted = computeDeltaP(uPosition_unshifted, (System.nanoTime() - lastUpdate) / 1000000000.0, index);
                    Vector2D shiftedPosition = new Vector2D(estimatedPosition_unshifted.getX() - (estimatedPlayerPosition_unshifted.getX() - boundRadius), 
                                                              estimatedPosition_unshifted.getY() - (estimatedPlayerPosition_unshifted.getY() - boundRadius));
                                                              
                    g.setColor(u.getColor());
                    int x = (int)((shiftedPosition.getX() - u.getRadius()) * scale);
                    int y = (int)((shiftedPosition.getY() - u.getRadius()) * scale);
                    g.fillArc(offsetX + x, offsetY + y, (int)(u.getRadius() * 2 * scale), (int)(u.getRadius() * 2 * scale), 0, 360);
                    
                    f = new Font("Arial",Font.BOLD,12);
                    g.setFont(f);
                    int sizeNeeded = SwingUtils.getMaxFittingFontSize(g, f, u.getName(), (int)(u.getRadius() * 1.8 * scale), (int)(u.getRadius() * scale));
                    f = new Font("Arial",Font.BOLD,sizeNeeded);
                    g.setFont(f);
                    FontMetrics fm = g.getFontMetrics();
                    x = (int)(shiftedPosition.getX() * scale - fm.stringWidth(u.getName())/2);
                    y = (int)(shiftedPosition.getY() * scale + fm.getHeight() / 3.7);
                    
                    SwingUtils.outlineText(g, u.getName(), offsetX + x, offsetY + y, Color.BLACK, Color.WHITE);
                    index++;
                }
                
                g.setColor(Color.LIGHT_GRAY);
                if(getWidth() > getHeight())
                {
                    g.fillRect(0,0,offsetX,getHeight());
                    g.fillRect(offsetX+square,0,getWidth(),getHeight());
                }
                else
                {
                    g.fillRect(0,0,getWidth(),offsetY);
                    g.fillRect(0,offsetY+square,getWidth(),getHeight());
                }
                
                g.setColor(Color.BLACK);
                f = new Font("Arial",Font.BOLD,12);
                g.setFont(f);
                g.drawString("FPS: " + (int)(1/((System.nanoTime() - lastFrameRendered) / 1000000000.0))
                                + " (" + (int)position.getX() + "," + (int)position.getY() + ")", offsetX + 10, 
                                offsetY + square - g.getFontMetrics().getHeight());
            }
        }
        lastFrameRendered = System.nanoTime();
    }
    
    private Vector2D computeDeltaP(Vector2D position, double deltaTime, int index)
    {
        if(lastUserData == null || userData == null || lastUserData.size() <= index || userData.size() <= index ||
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
                socket.setTcpNoDelay(true);
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
                    Thread.sleep(1000/61);
                } catch(Exception e){}
            }
        }
    }
    
    private class GameUpdaterIn implements Runnable
    {   
        public void run()
        {
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
                    else if(message.startsWith("WORLDFULL "))
                    {
                        synchronized(LOCK){
                            worldData = new ArrayList<GameObject>();
                            String[] messageContents = message.substring(10).split(",");
                            for(String obj : messageContents)
                            {
                                String[] objData = obj.split("\\|");
                                worldData.add(new GameObject(objData[0], Double.parseDouble(objData[1]), Double.parseDouble(objData[2]),
                                                                    GameConstants.stringToColor(objData[3]), Double.parseDouble(objData[4])));
                            }
                        }
                        receivedWorld = true;
                    }
                    else if(receivedWorld)
                    {
                        String[] messageContents = message.split("&", -1);
                        String[] messageContents_0 = messageContents[0].split(","); //User data
                        String[] messageContents_1 = messageContents[1].split(","); //World removed
                        String[] messageContents_2 = messageContents[2].split(","); //World added
                        int serverWorldDataSize = Integer.parseInt(messageContents[3]); //Actual world size
                        dataIndex = Integer.parseInt(messageContents_0[0]);
                        synchronized(LOCK){
                            lastUserData = userData;
                            userData = new ArrayList<GameObject>();
                            for(int i=1; i<messageContents_0.length; i++)
                            {
                                String[] u = messageContents_0[i].split("\\|");
                                userData.add(new GameObject(u[0], Double.parseDouble(u[1]), Double.parseDouble(u[2]),
                                                            GameConstants.stringToColor(u[3]), Double.parseDouble(u[4])));
                            }
                            
                            for(String rm : messageContents_1)
                            {
                                if(rm.equals(""))
                                    continue;
                                String[] objData = rm.split("\\|");
                                GameObject search = new GameObject(objData[0], Double.parseDouble(objData[1]), Double.parseDouble(objData[2]),
                                                                    GameConstants.stringToColor(objData[3]), Double.parseDouble(objData[4]));
                                                                    
                                for(int i=0; i<worldData.size(); i++)
                                    if(worldData.get(i).equalsData(search))
                                    {
                                        worldData.remove(i);
                                        break;
                                    }
                            }
                            
                            for(String add : messageContents_2)
                            {
                                if(add.equals(""))
                                    continue;
                                String[] objData = add.split("\\|");
                                worldData.add(new GameObject(objData[0], Double.parseDouble(objData[1]), Double.parseDouble(objData[2]),
                                                                    GameConstants.stringToColor(objData[3]), Double.parseDouble(objData[4])));
                            }
                            
                            if(worldData.size() != serverWorldDataSize)
                            {
                                receivedWorld = false;
                                System.out.println("WHOOPS");
                            }
                            AgarPanel.this.radius = userData.get(dataIndex).getRadius();
                            AgarPanel.this.position = new Vector2D(userData.get(dataIndex).getX(), userData.get(dataIndex).getY());
                            secondToLastUpdate = lastUpdate;
                            lastUpdate = System.nanoTime();
                        }
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
                    else if(!receivedWorld && worldData.size() == 0)
                    {
                        out.println("WORLD");
                    }
                    else if(dataIndex != -1 && userData.size() > 0)
                    {
                        //Still send input if only updating world
                        if(!receivedWorld)
                            out.println("WORLD");
                            
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