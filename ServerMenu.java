import javax.swing.*;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Dimension;

/**
 * Menu to view server.
 * 
 * @author Quadmium
 */
public class ServerMenu
{
    private JFrame frame;
    private Server serverBackend;
    
    /**
     * Sets up the menu.
     */
    public ServerMenu()
    {
        try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception e){}
        frame = new JFrame("Agar.javo");
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        JPanel selectionPanel = new JPanel();
        setupComponents(selectionPanel);
        frame.add(selectionPanel);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                serverBackend.close();
            }
        });
    }
    
    private void setupComponents(JPanel panel)
    {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        panel.setPreferredSize(new Dimension(500, 500));
        JTextArea labelOut = new JTextArea("Server log: \n");
            labelOut.setAlignmentX(JComponent.CENTER_ALIGNMENT);
            labelOut.setFont(new Font("Arial", Font.PLAIN, 12));
            labelOut.setPreferredSize(new Dimension(500, 500));
            labelOut.setMaximumSize(new Dimension(500, 500));
            labelOut.setLineWrap(true);
            labelOut.setEditable(false);
        JScrollPane scroll = new JScrollPane(labelOut);
            scroll.setPreferredSize(new Dimension(500, 500));
            scroll.setMaximumSize(new Dimension(500, 500));
            
        panel.add(scroll);
        
        serverBackend = new Server(labelOut);
    }
}