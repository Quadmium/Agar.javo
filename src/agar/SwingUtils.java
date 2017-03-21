import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import javax.swing.*;

/**
 * Utility class for the drawing part of the client.
 * 
 * @see AgarPanel
 */
public final class SwingUtils
{
    public static JPanel center(JComponent component)
    {
        JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        flowPanel.add(component);
        return flowPanel;
    }
    
    public static void outlineText(Graphics g, String text, int x, int y, Color color1, Color color2)
    {
        g.setColor(color1);
        g.drawString(text, ShiftWest(x, 1), ShiftNorth(y, 1));
        g.drawString(text, ShiftWest(x, 1), ShiftSouth(y, 1));
        g.drawString(text, ShiftEast(x, 1), ShiftNorth(y, 1));
        g.drawString(text, ShiftEast(x, 1), ShiftSouth(y, 1));
        g.setColor(color2);
        g.drawString(text, x, y);
    }
        
    public static int ShiftNorth(int p, int distance) {
       return (p - distance);
    }
    public static int ShiftSouth(int p, int distance) {
       return (p + distance);
    }
    public static int ShiftEast(int p, int distance) {
       return (p + distance);
    }
    public static int ShiftWest(int p, int distance) {
       return (p - distance);
    }

    public static int getMaxFittingFontSize(Graphics g, Font font, String string, int width, int height){
        int minSize = 0;
        int maxSize = 288;
        int curSize = font.getSize();

        while (maxSize - minSize > 2){
            FontMetrics fm = g.getFontMetrics(new Font(font.getName(), font.getStyle(), curSize));
            int fontWidth = fm.stringWidth(string);
            int fontHeight = fm.getLeading() + fm.getMaxAscent() + fm.getMaxDescent();

            if ((fontWidth > width) || (fontHeight > height)){
                maxSize = curSize;
                curSize = (maxSize + minSize) / 2;
            }
            else{
                minSize = curSize;
                curSize = (minSize + maxSize) / 2;
            }
        }

        return curSize;
    }
}