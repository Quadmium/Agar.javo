import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.RenderingHints;

public class AgarPanel extends JPanel
{
    private boolean connecting = true;
    private String ip = "";
    private String status = "Waiting...";
    private MainMenu parent;
    
    public AgarPanel(String ip, MainMenu parent)
    {
        super();
        this.ip = ip;
        this.parent = parent;
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
    }
    
    public void connect()
    {
        
    }
}