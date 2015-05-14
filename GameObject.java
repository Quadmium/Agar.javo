import java.awt.Color;

public class GameObject
{
    private String name;
    private double x, y, radius;
    private Color playerColor;
    
    public GameObject(String name, double x, double y, Color playerColor, double radius)
    {
        this.name = name;
        this.x = x;
        this.y = y;
        this.playerColor = playerColor;
        this.radius = radius;
    }
    
    public double getRadius()
    {
        return radius;
    }
    
    public void setRadius(double radius)
    {
        this.radius = radius;
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