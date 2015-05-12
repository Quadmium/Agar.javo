import javax.swing.*;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

public class MainMenu
{
    private JTextField nameField;
    private DefaultListModel<String> serverList = new DefaultListModel<>();
    private JPanel selectionPanel;
    private AgarPanel agar;
    private JFrame frame;
    private JList centerList;
    
    public static void main(String arg[])
    {
        MainMenu instance = new MainMenu();
    }
    
    public MainMenu()
    {
        try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception e){}
        frame = new JFrame("Agar.javo");
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        selectionPanel = new JPanel();
        setupComponents(selectionPanel);
        frame.add(selectionPanel);
        
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
    
    private void setupComponents(JPanel panel)
    {
        panel.setLayout(new BorderLayout());
        
            JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                JTextField nameField = new JTextField();
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
    
    private void onConnectButton()
    {
        String ip = (String)centerList.getSelectedValue();
        
        agar = new AgarPanel(ip, this);
        selectionPanel.setVisible(false);
        frame.add(agar);
        frame.setResizable(false);
        agar.connect();
    }
    
    public void endGame()
    {
        frame.remove(agar);
        selectionPanel.setVisible(true);
        frame.setSize(500, 500);
        frame.setResizable(false);
    }
}