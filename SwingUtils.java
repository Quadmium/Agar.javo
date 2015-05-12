import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Dimension;
import javax.swing.*;

public class SwingUtils
{
    public static JPanel center(JComponent component)
    {
        JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        flowPanel.add(component);
        return flowPanel;
    }
}