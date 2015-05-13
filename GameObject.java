import java.awt.Color;

public class GameObject
{
    private String name;
    private double x, y;
    private Color playerColor;
    
    public GameObject(String name, double x, double y, Color playerColor)
    {
        this.name = name;
        this.x = x;
        this.y = y;
        this.playerColor = playerColor;
    }
    
    public Color getColor()
    {
        return playerColor;
    }
    
    public void setColor(Color playerColor)
    {
        this.playerColor = playerColor;
    }
    
    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public double getX()
    {
        return x;
    }
    
    public void setX(double x)
    {
        this.x = x;
    }
    
    public double getY()
    {
        return y;
    }
    
    public void setY(double y)
    {
        this.y = y;
    }
}