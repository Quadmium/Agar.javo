
import javax.swing.*;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Dimension;

/**
 * Shows the server selection screen and transitions to the Agar panel.
 * All visible parts occur inside this JFrame.
 * 
 * @author Quadmium
 */
public class ClientMenu
{
    private JTextField nameField;
    private DefaultListModel<String> serverList = new DefaultListModel<>();
    private JPanel selectionPanel;
    private AgarPanel agar;
    private JFrame frame;
    private JList centerList;
    
    /**
     * Sets up the server selection menu.
     */
    public ClientMenu()
    {
        try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception e){}
        frame = new JFrame("Agar.javo");
        frame.setSize((int)GameConstants.BOARD_WIDTH, (int)GameConstants.BOARD_HEIGHT);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        selectionPanel = new JPanel();
        setupComponents(selectionPanel);
        frame.add(selectionPanel);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if(agar != null)
                    agar.close();
            }
        });
    }
    
    private void setupComponents(JPanel panel)
    {
        panel.setLayout(new BorderLayout());
        
            JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                nameField = new JTextField();
                    nameField.setColumns(15);
                GhostText nameText = new GhostText(nameField, "Enter a name");
                topPanel.add(nameField);
                JButton connectButton = new JButton("Connect");
                    connectButton.addActionListener((e) -> {
                        onConnectButton();
                    });
                topPanel.add(connectButton);
            
        panel.add(topPanel, BorderLayout.NORTH);
        
            centerList = new JList<>(serverList);
                centerList.setFont(new Font("Arial",Font.BOLD,14));
                
        panel.add(new JScrollPane(centerList), BorderLayout.CENTER);
        
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                JTextField serverIPField = new JTextField();
                    serverIPField.setColumns(15);
                GhostText serverIPText = new GhostText(serverIPField, "Enter custom IP");
                bottomPanel.add(serverIPField);
                JButton addCustomButton = new JButton("Add");
                    addCustomButton.addActionListener((e) -> {
                        String ip = serverIPField.getText();
                        if(serverList.contains(ip) || ip.equals("Enter custom IP"))
                            JOptionPane.showMessageDialog(panel, "IP already in list.");
                        else
                            serverList.add(serverList.size(), serverIPField.getText());
                    });
                bottomPanel.add(addCustomButton);
            
        panel.add(bottomPanel, BorderLayout.SOUTH);
    }
    
    /**
     * When connect is clicked, this void shows a new instance of the AgarPanel class.
     */
    private void onConnectButton()
    {
        String ip = (String)centerList.getSelectedValue();
        
        selectionPanel.setVisible(false);
        agar = new AgarPanel(ip, nameField.getText().equals("Enter a name") ? "" : nameField.getText(), 
                                GameConstants.ALLOWED_COLORS[(int)(Math.random()*GameConstants.ALLOWED_COLORS.length)], this);
        frame.add(agar);
        frame.getContentPane().setPreferredSize(new Dimension((int)GameConstants.BOARD_WIDTH, (int)GameConstants.BOARD_HEIGHT));
        frame.pack();
        frame.setVisible(true);
        frame.setResizable(true);
        agar.connect();
        agar.requestFocus();
    }
    
    /**
     * Removes the AgarPanel component and shows the original server selection screen.
     */
    public void endGame()
    {
        frame.remove(agar);
        selectionPanel.setVisible(true);
        frame.setSize(500, 500);
        frame.setResizable(false);
    }
}