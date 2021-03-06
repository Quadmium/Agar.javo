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
import java.util.HashMap;
import java.awt.MouseInfo;
import java.awt.Color;
import java.awt.Polygon;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.PriorityQueue;
import java.awt.AlphaComposite;

/**
 * Hosts the client code for the Agar game. The client is responsible for displaying the game as
 * specified by the server and sending the mouse coordinates.
 * 
 * @see ClientMenu
 * 
 * @author Quadmium
 */

public class AgarPanel extends JPanel implements KeyListener
{
    private volatile boolean connecting = true;
    private String ip;
    private volatile String status = "Waiting...";
    private String name;
    private Color playerColor;
    private volatile double radius;
    private volatile Vector2D position;
    private ClientMenu parent;
    private volatile Socket socket;
    private ArrayList<GameObject> userData = new ArrayList<GameObject>();
    private ArrayList<GameObject> lastUserData = new ArrayList<GameObject>();
    private ArrayList<GameObject> worldData = new ArrayList<GameObject>();
    private HashMap<String, GameObject> lastThrownData = new HashMap<String, GameObject>();
    private ArrayList<String> chatData = new ArrayList<String>();
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

    private volatile boolean spacePressed = false;
    private volatile boolean lastSpacePressed = false;
    private volatile boolean wPressed = false;
    private volatile boolean lastWPressed = false;

    private volatile boolean poison = false;
    private static final int TIMEOUT = 2;
    
    private volatile boolean sendMsg = false;
    private volatile boolean typing = false;
    private volatile String inProgressChat;

    /**
     * Setup a new game panel.
     * 
     * @param ip the IP of the server
     * @param playerColor the player's requested color
     * @param parent the parent MainMenu
     */
    public AgarPanel(String ip, String name, Color playerColor, ClientMenu parent)
    {
        super();
        setDoubleBuffered(true);
        this.ip = ip;
        this.parent = parent;
        this.name = name;
        this.playerColor = playerColor;
        addKeyListener(this);
        setFocusable(true);
        requestFocusInWindow();
    }
    
    public void close()
    {
        try{socket.close();}catch(Exception e){}
        poison = true;
    }

    /**
     * Paints the screen.
     */
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
                lastFrameRendered = System.nanoTime();
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
                Vector2D estimatedPlayerPosition_unshifted = computeDeltaP(position, (System.nanoTime() - lastUpdate) / 1000000000.0, dataIndex, -1);

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
                double a1 = Math.cos(foodAngle), a2 = Math.cos(foodAngle + Math.PI / 2),
                       a3 = Math.cos(foodAngle + Math.PI), a4 = Math.cos(foodAngle + 3 * Math.PI / 2),
                       a5 = Math.sin(foodAngle), a6 = Math.sin(foodAngle + Math.PI / 2),
                       a7 = Math.sin(foodAngle + Math.PI), a8 = Math.sin(foodAngle + 3 * Math.PI / 2);
                double b1 = GameConstants.FOOD_RADIUS * scale * a1, b2 = GameConstants.FOOD_RADIUS * scale * a2,
                       b3 = GameConstants.FOOD_RADIUS * scale * a3, b4 = GameConstants.FOOD_RADIUS * scale * a4,
                       b5 = GameConstants.FOOD_RADIUS * scale * a5, b6 = GameConstants.FOOD_RADIUS * scale * a6,
                       b7 = GameConstants.FOOD_RADIUS * scale * a7, b8 = GameConstants.FOOD_RADIUS * scale * a8;
                for(GameObject obj : worldData)
                {
                    if(!GameConstants.insideBoundRadius(position, boundRadius, obj))
                        continue;

                    Vector2D objPosition_unshifted = new Vector2D(obj.getX(), obj.getY());
                    if(obj.getName().startsWith("T"))
                    {
                        objPosition_unshifted = computeDeltaP(objPosition_unshifted, (System.nanoTime() - lastUpdate) / 1000000000.0, obj.getName());
                    }
                    Vector2D shiftedPosition = new Vector2D(objPosition_unshifted.getX() - (estimatedPlayerPosition_unshifted.getX() - boundRadius), 
                            objPosition_unshifted.getY() - (estimatedPlayerPosition_unshifted.getY() - boundRadius));

                    g.setColor(obj.getColor());
                    int x = (int)((shiftedPosition.getX() - obj.getRadius()) * scale);
                    int y = (int)((shiftedPosition.getY() - obj.getRadius()) * scale);

                    Polygon food = new Polygon();
                    if(obj.getRadius() == GameConstants.FOOD_RADIUS)
                    {
                        food.addPoint(offsetX + (int)(x + b1), offsetY + (int)(y + b5));
                        food.addPoint(offsetX + (int)(x + b2), offsetY + (int)(y + b6));
                        food.addPoint(offsetX + (int)(x + b3), offsetY + (int)(y + b7));
                        food.addPoint(offsetX + (int)(x + b4), offsetY + (int)(y + b8));
                    }
                    else
                    {
                        double r = obj.getRadius() * scale;
                        food.addPoint(offsetX + (int)(x + r * a1), offsetY + (int)(y + r * a5));
                        food.addPoint(offsetX + (int)(x + r * a2), offsetY + (int)(y + r * a6));
                        food.addPoint(offsetX + (int)(x + r * a3), offsetY + (int)(y + r * a7));
                        food.addPoint(offsetX + (int)(x + r * a4), offsetY + (int)(y + r * a8));
                    }
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

                    for(int i=-1; i<u.getSubObjectsSize(); i++)
                    {
                        if(i < 0 && u.getSubObjectsSize() > 0)
                            continue;

                        GameObject obj = i==-1? u : u.getSubObject(i);
                        Vector2D objPosition_unshifted = new Vector2D(obj.getX(), obj.getY());
                        Vector2D estimatedPosition_unshifted = computeDeltaP(objPosition_unshifted, (System.nanoTime() - lastUpdate) / 1000000000.0, index, i);
                        Vector2D shiftedPosition = new Vector2D(estimatedPosition_unshifted.getX() - (estimatedPlayerPosition_unshifted.getX() - boundRadius), 
                                estimatedPosition_unshifted.getY() - (estimatedPlayerPosition_unshifted.getY() - boundRadius));

                        g.setColor(new Color((int)(obj.getColor().getRed() * GameConstants.OUTER_CIRCLE_DARK_FACTOR),
                                             (int)(obj.getColor().getGreen() * GameConstants.OUTER_CIRCLE_DARK_FACTOR),
                                             (int)(obj.getColor().getBlue() * GameConstants.OUTER_CIRCLE_DARK_FACTOR)));
                        int x = (int)((shiftedPosition.getX() - obj.getRadius()) * scale);
                        int y = (int)((shiftedPosition.getY() - obj.getRadius()) * scale);
                        int outerRad = (int)(GameConstants.OUTER_CIRCLE_RADIUS * scale);
                        g.fillArc(offsetX + x - outerRad, 
                                  offsetY + y - outerRad,
                                  (int)(obj.getRadius() * 2 * scale) + outerRad * 2, 
                                  (int)(obj.getRadius() * 2 * scale) + outerRad * 2, 0, 360);
                        g.setColor(obj.getColor());
                        g.fillArc(offsetX + x, offsetY + y, (int)(obj.getRadius() * 2 * scale),
                                  (int)(obj.getRadius() * 2 * scale), 0, 360);

                        f = new Font("Arial",Font.BOLD,12);
                        g.setFont(f);
                        int sizeNeeded = SwingUtils.getMaxFittingFontSize(g, f, obj.getName(), (int)(obj.getRadius() * 1.8 * scale), (int)(obj.getRadius() * scale));
                        f = new Font("Arial",Font.BOLD,sizeNeeded);
                        g.setFont(f);
                        FontMetrics fm = g.getFontMetrics();
                        x = (int)(shiftedPosition.getX() * scale - fm.stringWidth(obj.getName())/2);
                        y = (int)(shiftedPosition.getY() * scale + fm.getHeight() / 3.7);

                        SwingUtils.outlineText(g, obj.getName(), offsetX + x, offsetY + y, Color.BLACK, Color.WHITE);
                    }

                    index++;
                }

                double score = GameConstants.calculateScore(userData.get(dataIndex));

                g.setColor(Color.BLACK);
                f = new Font("Arial",Font.BOLD,12);
                g.setFont(f);
                String s = "Score: " + (int)score;
                int sizeNeeded = SwingUtils.getMaxFittingFontSize(g, f, "Score: 1", (int)(4 * square / (2 * GameConstants.getBoundRadius(1))),
                        (int)(2 * square / (2 * GameConstants.getBoundRadius(1))));
                f = new Font("Arial",Font.BOLD,sizeNeeded);
                g.setFont(f);
                SwingUtils.outlineText(g, s, (int)(offsetX + g.getFontMetrics().getHeight() / 3.8), (int)(offsetY + square - g.getFontMetrics().getHeight() / 3.8), Color.BLACK, Color.WHITE);

                ((Graphics2D)g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                g.setColor(new Color(16, 16, 16));
                g.fillRect(offsetX + (int)(0.75 * square), offsetY, (int)(0.251 * square), offsetY + (int)(0.5 * square));
                PriorityQueue<GameObject> leaderboard = new PriorityQueue<GameObject>(8, (a,b) -> new Double(GameConstants.calculateScore(b)).compareTo(GameConstants.calculateScore(a)));
                for(GameObject u : userData)
                    leaderboard.add(u);

                g.setColor(new Color(255, 255, 255));
                sizeNeeded = SwingUtils.getMaxFittingFontSize(g, f, "Leaderboard:", (int)(square * 0.23), (int)(0.49 * square / 9.0));
                f = new Font("Arial",Font.BOLD,sizeNeeded);
                g.setFont(f);
                int x = offsetX + (int)(0.755 * square);
                int y = offsetY + (int)(g.getFontMetrics().getHeight() / 1.3);
                g.drawString("Leaderboard:", x, y);
                for(int i=0; i<8 && leaderboard.size() > 0; i++)
                {
                    String row = (i+1) + ". " + leaderboard.poll().getName();
                    if(row.length() == 3)
                        row = (i+1) + ". An unnamed cell";
                    sizeNeeded = SwingUtils.getMaxFittingFontSize(g, f, row, (int)(square * 0.23), (int)(0.49 * square / 9.0));
                    f = new Font("Arial",Font.BOLD,sizeNeeded);
                    g.setFont(f);
                    g.drawString(row, x, y + square / 18 * (i+1));
                }
                ((Graphics2D)g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

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
                FontMetrics fm = g.getFontMetrics();
                s = "FPS: " + (int)(1/deltaTime)
                + " (" + (int)position.getX() + "," + (int)position.getY() + ")";
                g.drawString(s, offsetX + square - fm.stringWidth(s) - 10, 
                    offsetY + square - g.getFontMetrics().getHeight());
                
                if(typing)
                {
                    f = new Font("Arial",Font.BOLD,(int)(0.04 * square));
                    g.setFont(f);
                    Color chatColor = new Color(Color.lightGray.getRed(), Color.lightGray.getGreen(), Color.lightGray.getBlue(), 200);
                    
                    g.setColor(chatColor);
                    g.fillRect(offsetX, d.height - (int)(0.05 * square), (int)(0.5 * square), (int)(0.05 * square));
                    g.setColor(Color.BLACK);
                    g.drawString(inProgressChat, offsetX + (int)(0.01 * square), d.height - (int)(0.01 * square));
                    
                    int chatOffset = (int)(0.1 * square);
                    int maxChat = 5;
                    for(int i=chatData.size() - 1; i >= (chatData.size() >= maxChat ? chatData.size() - maxChat : 0); i--)
                    {
                        g.setColor(Color.BLACK);
                        g.drawString(chatData.get(i), offsetX + (int)(0.01 * square), d.height - chatOffset);
                        chatOffset += (int)(0.05 * square);
                    }
                    
                    f = new Font("Arial",Font.BOLD,12);
                    g.setFont(f);
                }
            }
        }
    }

    /**
     * Computes the approximate change in position based on the last update (tangent velocity).
     * 
     * @param position the current position
     * @param deltaTime the change in time
     * @param index index of the player
     * @param subIndex index of the sub object if exists, otherwise -1
     * 
     * @return the approximate position
     */
    private Vector2D computeDeltaP(Vector2D position, double deltaTime, int index, int subIndex)
    {
        try
        {
            if(lastUserData.size() != userData.size())
                return position;

            if(subIndex >= 0 && (lastUserData.get(index).getSubObjectsSize() != userData.get(index).getSubObjectsSize()))
                return position;

            GameObject last = subIndex < 0 ? lastUserData.get(index) : lastUserData.get(index).getSubObject(subIndex);
            Vector2D dP = position.minus(new Vector2D(last.getX(), last.getY()));
            double dT = (lastUpdate - secondToLastUpdate) / 1000000000.0;
            Vector2D velocity = dP.scalarMult(1.0/dT);

            Vector2D predictedDP = velocity.scalarMult(deltaTime);
            return position.plus(predictedDP);
        }
        catch(Exception e)
        {
            return position;
        }
    }
    
    /**
     * Computes the approximate change in position based on the last update for a mass object (tangent velocity).
     * 
     * @param position the current position
     * @param deltaTime the change in time
     * @param name name of the object ("T" followed by a unique ID)
     * 
     * @return the approximate position
     */
    private Vector2D computeDeltaP(Vector2D position, double deltaTime, String name)
    {
        try
        {
            if(!lastThrownData.containsKey(name))
                return position;

            GameObject last = lastThrownData.get(name);
            Vector2D dP = position.minus(new Vector2D(last.getX(), last.getY()));
            double dT = (lastUpdate - secondToLastUpdate) / 1000000000.0;
            Vector2D velocity = dP.scalarMult(1.0/dT);
            Vector2D predictedDP = velocity.scalarMult(deltaTime);
            return position.plus(predictedDP);
        }
        catch(Exception e)
        {
            return position;
        }
    }

    /**
     * Starts a connection thread with the server.
     */
    public void connect()
    {
        (new Thread(new Connector())).start();
    }

    /**
     * Connects to the server. Starts I/O threads, timeout thread, and repaint thread.
     */
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
            (new Thread(new DisconnectWatcher())).start();
        }
    }

    /**
     * Adds the repaint call to the queue every 60th of a second. If calls are not able to be drawn as fast, they will
     * bunched together by Swing.
     */
    private class Repainter implements Runnable
    {
        public void run()
        {
            while(true)
            {
                try {
                    if(poison)
                        return;
                    AgarPanel.this.repaint();
                    Thread.sleep(1000/61);
                } catch(Exception e){}
            }
        }
    }

    /**
     * Reads info from the server and updates local info accordingly.
     */
    private class GameUpdaterIn implements Runnable
    {   
        public void run()
        {
            while(true)
            {
                try {
                    if(poison)
                        return;
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
                    else if(message.startsWith("CHAT "))
                    {
                        synchronized(LOCK)
                        {
                            chatData.add(message.substring(5));
                        }
                    }
                    else if(receivedWorld)
                    {
                        String[] messageContents = message.split("&", -1);
                        String[] messageContents_0 = messageContents[0].split(","); //User data
                        String[] messageContents_1 = messageContents[1].split(","); //World removed
                        String[] messageContents_2 = messageContents[2].split(","); //World added
                        String[] messageContents_3 = messageContents[3].split(","); //World moved
                        int serverWorldDataSize = Integer.parseInt(messageContents[4]); //Actual world size
                        dataIndex = Integer.parseInt(messageContents_0[0]);
                        synchronized(LOCK){
                            lastUserData = userData;
                            userData = new ArrayList<GameObject>();
                            for(int i=1; i<messageContents_0.length; i++)
                            {
                                String[] subObjects = messageContents_0[i].split("#");
                                String[] u = subObjects[0].split("\\|");
                                GameObject curUser = new GameObject(u[0], Double.parseDouble(u[1]), Double.parseDouble(u[2]),
                                        GameConstants.stringToColor(u[3]), Double.parseDouble(u[4]));
                                for(int j=1; j<subObjects.length; j++)
                                {
                                    String[] subUser = subObjects[j].split("\\|");
                                    curUser.addSubObject(new GameObject(subUser[0], Double.parseDouble(subUser[1]), Double.parseDouble(subUser[2]),
                                            GameConstants.stringToColor(subUser[3]), Double.parseDouble(subUser[4])));
                                }
                                userData.add(curUser);
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
                            
                            for(String move : messageContents_3)
                            {
                                if(move.equals(""))
                                    continue;
                                String[] objData = move.split("\\|");

                                for(int i=0; i<worldData.size(); i++)
                                    if(worldData.get(i).getName().equals(objData[0]))
                                    {
                                        GameObject moved = worldData.get(i);
                                        
                                        if(lastThrownData.containsKey(objData[0]))
                                        {
                                            lastThrownData.get(objData[0]).setX(moved.getX());
                                            lastThrownData.get(objData[0]).setY(moved.getY());
                                        }
                                        else
                                        {
                                            lastThrownData.put(moved.getName(), new GameObject(moved.getName(), moved.getX(), moved.getY(), moved.getColor(), moved.getRadius()));
                                        }
                                        
                                        moved.setX(Double.parseDouble(objData[1]));
                                        moved.setY(Double.parseDouble(objData[2]));
                                        break;
                                    }
                            }

                            if(Math.abs(worldData.size() - serverWorldDataSize) > GameConstants.WORLD_ALLOWANCE)
                            {
                                receivedWorld = false;
                            }
                            AgarPanel.this.radius = userData.get(dataIndex).getRadius();
                            AgarPanel.this.position = new Vector2D(userData.get(dataIndex).getX(), userData.get(dataIndex).getY());
                            secondToLastUpdate = lastUpdate;
                            lastUpdate = System.nanoTime();
                        }
                    }
                    Thread.sleep(1);
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }
    }

    /**
     * Sends info to the server.
     */
    private class GameUpdaterOut implements Runnable
    {   
        public void run()
        {
            while(true)
            {
                try {
                    if(poison)
                        return;
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
                    else if(sendMsg)
                    {
                        out.println("CHAT " + inProgressChat);
                        inProgressChat = "";
                        sendMsg = false;
                    }
                    else if(dataIndex != -1 && userData.size() > 0)
                    {
                        //Still send input if only updating world
                        if(!receivedWorld)
                            out.println("WORLD");

                        if(lastSpacePressed && !spacePressed)
                            out.println("SPLIT");
                        
                        double xPos = MouseInfo.getPointerInfo().getLocation().getX() - AgarPanel.this.getLocationOnScreen().getX();
                        double yPos = MouseInfo.getPointerInfo().getLocation().getY() - AgarPanel.this.getLocationOnScreen().getY();

                        synchronized(LOCK)
                        {
                            Vector2D velocity;
                            velocity = new Vector2D(xPos - AgarPanel.this.getSize().getWidth()/2, yPos - AgarPanel.this.getSize().getHeight()/2);
                            Vector2D adjVelocity = velocity.scalarMult(GameConstants.INITIAL_VELOCITY / 180.0);
                            out.println(adjVelocity.getX() + "," + adjVelocity.getY());
                              
                            if(lastWPressed && !wPressed)
                            {
                                double boundRadius = GameConstants.getBoundRadius(radius);
                                int square;
                                if(getWidth() > getHeight())
                                    square = getHeight();
                                else
                                    square = getWidth();
                                double scale = square / (boundRadius*2);
                                
                                Vector2D estPlayerPos = computeDeltaP(position, (System.nanoTime() - lastUpdate) / 1000000000.0, dataIndex, -1);
                                Vector2D target = estPlayerPos.plus(velocity.scalarMult(1.0 / scale));
                                out.println("THROW," + target.getX() + "," + target.getY());
                            }
                        }
                    }

                    lastSpacePressed = spacePressed;
                    lastWPressed = wPressed;
                    out.flush();
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }
    }
    
    /**
     * Watches for time passed between every update. Can close the game if timeout exceeds specified.
     */
    private class DisconnectWatcher extends Thread
    {
        public void run()
        {
            while(true)
            {
                double lag = (System.nanoTime() - lastUpdate) / 1000000000.0;
                if(lag > TIMEOUT)
                {
                    poison = true;
                    try{socket.close();}catch(Exception e){}
                    parent.endGame();
                    return;
                }
                try{Thread.sleep(500);}catch(Exception e){}
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) 
    {
    }

    public boolean isPrintableChar( char c ) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of( c );
        return (!Character.isISOControl(c)) &&
                c != KeyEvent.CHAR_UNDEFINED &&
                block != null &&
                block != Character.UnicodeBlock.SPECIALS;
    }
    
    @Override
    public void keyPressed(KeyEvent e) 
    {
        if(typing)
        {
            if(e.getKeyCode() == KeyEvent.VK_ESCAPE)
            {
                inProgressChat = "";
                typing = false;
            }
            else if(e.getKeyCode() == KeyEvent.VK_ENTER)
            {
                sendMsg = true;
                //typing = false;
            }
            else if(e.getKeyCode() == KeyEvent.VK_BACK_SPACE)
            {
                if(inProgressChat.length() > 0)
                    inProgressChat = inProgressChat.substring(0, inProgressChat.length() - 1);
            }
            else
            {
                if(isPrintableChar(e.getKeyChar()))
                    inProgressChat = inProgressChat + e.getKeyChar();
            }
            
            return;
        }
        
        if (e.getKeyCode() == KeyEvent.VK_T)
        {
            inProgressChat = "";
            typing = true;
        }
        
        if (e.getKeyCode() == KeyEvent.VK_SPACE)
            spacePressed = true;
        else if(e.getKeyCode() == KeyEvent.VK_W)
            wPressed = true;
    }

    @Override
    public void keyReleased(KeyEvent e) 
    {
        if (e.getKeyCode() == KeyEvent.VK_SPACE)
            spacePressed = false;
        else if(e.getKeyCode() == KeyEvent.VK_W)
            wPressed = false;
    }
}