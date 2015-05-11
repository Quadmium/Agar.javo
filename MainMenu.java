import javax.swing.*;

public class MainMenu
{
    JTextField nameField;
    
    public static void main(String arg[])
    {
        MainMenu instance = new MainMenu();
    }
    
    public MainMenu()
    {
        JFrame frame = new JFrame("Agar.javo");
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        JPanel panel = new JPanel();
        setupComponents(panel);
        frame.add(panel);
        afterPacking(panel);
        
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    private void setupComponents(JPanel panel)
    {
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        
        JPanel leftPane = new JPanel();
            leftPane.setLayout(new BoxLayout(leftPane, BoxLayout.Y_AXIS));
            nameField = new JTextField();
                nameField.setColumns(20);
            GhostText nameText = new GhostText(nameField, "Enter a name...");
            leftPane.add(nameField);
            
            
        panel.add(leftPane);
        
    }
    
    private void afterPacking(JPanel panel)
    {
        nameField.setMaximumSize(nameField.getPreferredSize());
        
    }
}