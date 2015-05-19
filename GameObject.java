import java.awt.Color;
import java.util.ArrayList;

public class GameObject
{
    private String name;
    private double x, y, radius;
    private Color playerColor;
    private ArrayList<GameObject> subObjects = new ArrayList<GameObject>();
    
    public GameObject(String name, double x, double y, Color playerColor, double radius)
    {
        this.name = name;
        this.x = x;
        this.y = y;
        this.playerColor = playerColor;
        this.radius = radius;
    }
    
    public boolean equalsData(GameObject g)
    {
        return radius == g.getRadius() &&
               playerColor == g.getColor() &&
               x == g.getX() && y == g.getY() &&
               name.equals(g.getName());
    }
    
    public void addSubObject(GameObject obj)
    {
        subObjects.add(obj);
    }
    
    public void removeSubObject(int index)
    {
        subObjects.remove(index);
    }
    
    public void clearSubObjects()
    {
        subObjects.clear();
    }
    
    public int getSubObjectsSize()
    {
        return subObjects.size();
    }
    
    public GameObject getSubObject(int index)
    {
        return subObjects.get(index);
    }
    
    public int getSubObjectIndex(GameObject obj)
    {
        for(int i=0; i<subObjects.size(); i++)
        {
            if(subObjects.get(i).equalsData(obj))
                return i;
        }
        
        return -1;
    }
    
    public Vector2D getPosition()
    {
        return new Vector2D(x, y);
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