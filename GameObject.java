import java.awt.Color;
import java.util.ArrayList;

/**
 * Stores data for an object, such as position, color, and name.
 */
public class GameObject
{
    private String name;
    private double x, y, radius;
    private Vector2D velocity = new Vector2D(0,0), prevPos = new Vector2D(0,0);
    private Color playerColor;
    private ArrayList<GameObject> subObjects = new ArrayList<GameObject>();
    private boolean merge = true, eatable = true;
    
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
    
    public boolean eatable()
    {
        return eatable;
    }
    
    public void setEatable(boolean eatable)
    {
        this.eatable = eatable;
    }
    
    public boolean canMerge()
    {
        return merge;
    }
    
    public void setMerge(boolean status)
    {
        merge = status;
    }
    
    public Vector2D getVelocity()
    {
        return velocity;
    }
    
    public void setVelocity(Vector2D velocity)
    {
        this.velocity = velocity;
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
    
    public void setPosition(Vector2D position)
    {
        x = position.getX();
        y = position.getY();
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