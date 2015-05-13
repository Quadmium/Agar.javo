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

public class AgarPanel extends JPanel
{
    private boolean connecting = true;
    private String ip;
    private String status = "Waiting...";
    private String name;
    private MainMenu parent;
    private Socket socket;
    private ArrayList<GameObject> userData = new ArrayList<GameObject>();
    private final Object LOCK = new Object();
    private volatile int dataIndex = -1;
    private PrintWriter out;
    private BufferedReader in;
    private boolean willBroadcastName = false;
    
    public AgarPanel(String ip, String name, MainMenu parent)
    {
        super();
        this.ip = ip;
        this.parent = parent;
        this.name = name;
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
        else
        {
            f = new Font("Arial",Font.PLAIN,12);
            g.setFont(f);
            synchronized(LOCK)
            {
                for(GameObject u : userData)
                {
                    g.fillArc((int)u.getX(), (int)u.getY(), 10, 10, 0, 360);
                    g.drawString(u.getName(), (int)u.getX(), (int)u.getY() + 25);
                }
            }
        }
    }
    
    public void connect()
    {
        status = "Connecting...";
        try{
            socket = new Socket(ip, 40124);
            out = new PrintWriter(socket.getOutputStream(), 
                     true);
            in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
        } catch (Exception e) {
            System.out.println("Unknown host");
            parent.endGame();
        }
        
        status = "Connected.";
        connecting = false;
        (new Thread(new GameUpdaterIn())).start();
        (new Thread(new GameUpdaterOut())).start();
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
                    else
                    {
                        String[] messageContents = message.split(",");
                        dataIndex = Integer.parseInt(messageContents[0]);
                        synchronized(LOCK){
                            userData = new ArrayList<GameObject>();
                            for(int i=1; i<messageContents.length; i++)
                            {
                                String[] u = messageContents[i].split("\\|");
                                userData.add(new GameObject(u[0], Double.parseDouble(u[1]), Double.parseDouble(u[2])));
                            }
                        }
                        AgarPanel.this.repaint();
                    }
                    Thread.sleep(10);
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
                    else
                    {
                        double xPos = MouseInfo.getPointerInfo().getLocation().getX() - AgarPanel.this.getLocationOnScreen().getX();
                        double yPos = MouseInfo.getPointerInfo().getLocation().getY() - AgarPanel.this.getLocationOnScreen().getY();
                        
                        Vector2D acceleration;
                        synchronized(LOCK){
                            acceleration = new Vector2D(xPos - userData.get(dataIndex).getX(), yPos - userData.get(dataIndex).getY());
                        }
                        out.println(acceleration.getX() + "," + acceleration.getY());
                    }
                    Thread.sleep(10);
                } catch (Exception e) {System.out.println(e);}
            }
        }
    }
}