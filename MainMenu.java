import javax.swing.*;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Dimension;

/**
 * Menu to launch instances of the client or the server.
 * 
 * @author Quadmium
 */
public class MainMenu
{
    private JFrame frame;
    
    public static void main(String arg[])
    {
        new MainMenu();
    }
    
    /**
     * Sets up the menu.
     */
    public MainMenu()
    {
        try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception e){}
        frame = new JFrame("Agar.javo");
        frame.setSize(200, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        JPanel selectionPanel = new JPanel();
        setupComponents(selectionPanel);
        frame.add(selectionPanel);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
    
    private void setupComponents(JPanel panel)
    {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        panel.setPreferredSize(new Dimension(200, 300));
        JButton clientBtn = new JButton("Client");
            clientBtn.setMaximumSize(new Dimension(200, 70));
            clientBtn.setPreferredSize(new Dimension(200, 70));
            clientBtn.setAlignmentX(JComponent.CENTER_ALIGNMENT);
            clientBtn.setFont(new Font("Arial", Font.PLAIN, 30));
            clientBtn.addActionListener((e) -> {new ClientMenu();});
        JButton serverBtn = new JButton("Server");
            serverBtn.setMaximumSize(new Dimension(200, 70));
            serverBtn.setPreferredSize(new Dimension(200, 70));
            serverBtn.setAlignmentX(JComponent.CENTER_ALIGNMENT);
            serverBtn.setFont(new Font("Arial", Font.PLAIN, 30));
            serverBtn.addActionListener((e) -> {new ServerMenu();});
        JLabel labelTop = new JLabel("Agar.javo");
            labelTop.setAlignmentX(JComponent.CENTER_ALIGNMENT);
            labelTop.setFont(new Font("Arial", Font.PLAIN, 40));
        JLabel labelBottom = new JLabel("Created by Quadmium");
            labelBottom.setAlignmentX(JComponent.CENTER_ALIGNMENT);
            labelBottom.setFont(new Font("Arial", Font.PLAIN, 15));
            
        panel.add(labelTop);
        panel.add(Box.createVerticalGlue());
        panel.add(clientBtn);
        panel.add(Box.createVerticalGlue());
        panel.add(serverBtn);
        panel.add(Box.createVerticalGlue());
        panel.add(labelBottom);
    }
}