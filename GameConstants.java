import java.awt.Color;
import java.util.ArrayList;

/**
 * Class to calculate constants and store game properties.
 */
public final class GameConstants
{
    public static final int BOARD_WIDTH = 500, BOARD_HEIGHT = 500;
    public static final int FOOD_GOAL = 4000;
    public static final double FOOD_RADIUS = 0.6;
    public static final double FOOD_VOLUME = 1;
    public static final double EAT_RATIO = 1.1;
    public static final double MIN_SPLIT_RADIUS = 1.5;
    public static final int MAX_SPLIT = 8;
    public static final double PUSH_FACTOR = 0.1;
    public static final double THROW_MASS_VOLUME = 8;
    public static final double THROW_MASS_SPEED = 20;
    public static final double THROW_MASS_DECELERATION = 20;
    public static final long EATABLE_DELAY = 2000;
    public static final int GRID_SIZE = BOARD_WIDTH / 10;
    public static final int WORLD_ALLOWANCE = 5;
    
    public static final Color[] ALLOWED_COLORS = {
            Color.BLUE,
            Color.CYAN,
            Color.GREEN,
            Color.LIGHT_GRAY,
            Color.MAGENTA,
            Color.PINK,
            Color.RED,
            Color.YELLOW
        };
    public static final String[] COLOR_STRINGS = {
            "BLUE",
            "CYAN",
            "GREEN",
            "LIGHT_GRAY",
            "MAGENTA",
            "PINK",
            "RED",
            "YELLOW"
        };

    public static String colorToString(Color color)
    {
        for(int i=0; i<ALLOWED_COLORS.length; i++)
        {
            if(ALLOWED_COLORS[i].equals(color))
                return COLOR_STRINGS[i];
        }

        return "BLACK";
    }

    public static Color stringToColor(String color)
    {
        for(int i=0; i<COLOR_STRINGS.length; i++)
        {
            if(COLOR_STRINGS[i].equals(color))
                return ALLOWED_COLORS[i];
        }

        return Color.BLACK;
    }

    public static final double INITIAL_RADIUS = 1;
    public static final double INITIAL_BOUNDRADIUS = 10;
    public static final double FINAL_RADIUS = 250;
    public static final double FINAL_BOUNDRADIUS = 500;
    
    public static final double INITIAL_RADIUS_VELOCITY = 1; //Do not change
    public static final double INITIAL_VELOCITY = 12;
    public static final double VELOCITY_FLOOR = 4;
    public static final double VELOCITY_CURVATURE = 1.1;

    public static double getBoundRadius(double radius)
    {
        double slope = (FINAL_BOUNDRADIUS - INITIAL_BOUNDRADIUS) / 
            (FINAL_RADIUS - INITIAL_RADIUS);

        return slope * (radius - INITIAL_RADIUS) + INITIAL_BOUNDRADIUS;
    }
    
    public static boolean insideBoundRadius(Vector2D position, double boundRadius, GameObject u)
    {
        double leftWall = position.getX() - boundRadius - 2 * u.getRadius();
        double rightWall = position.getX() + boundRadius + 2 * u.getRadius();
        double bottomWall = position.getY() - boundRadius - 2 * u.getRadius();
        double topWall = position.getY() + boundRadius + 2 * u.getRadius();
        
        return u.getX() > leftWall && u.getX() < rightWall &&
                u.getY() > bottomWall && u.getY() < topWall;
    }
    
    public static double distance(Vector2D A, Vector2D B)
    {
        return Vector2D.distance(A.getX(), A.getY(), B.getX(), B.getY());
    }
    
    public static final double SPLIT_VELOCITY_BOOST = 12;
    public static final double SPLIT_DECELERATION =  24;
    public static final long MERGE_DELAY = 15*1000;
    
    public static double maximumVelocity(double radius)
    {
        return VELOCITY_FLOOR + (INITIAL_VELOCITY - VELOCITY_FLOOR) / (Math.pow(VELOCITY_CURVATURE, radius - INITIAL_RADIUS_VELOCITY));
    }
    
    public static double calculateCombinedRadius(ArrayList<GameObject> objects)
    {
        double furthestDistance = Double.MIN_VALUE;
        for(int i=0; i<objects.size(); i++)
        {
            GameObject a = objects.get(i);
            for(int j=i; j<objects.size(); j++)
            {
                GameObject b = objects.get(j);
                double distance2 = distance(a.getPosition(), b.getPosition()) + a.getRadius() + b.getRadius();
                if(distance2 > furthestDistance)
                    furthestDistance = distance2;
            }
        }
        
        return furthestDistance / 2.0;
    }
    
    public static double calculateScore(GameObject obj)
    {
        double score = 0;
        if(obj.getSubObjectsSize() == 0)
            score += Math.PI * Math.pow(obj.getRadius(), 2);
        else
            for(int i=0; i<obj.getSubObjectsSize(); i++)
                score += Math.PI * Math.pow(obj.getSubObject(i).getRadius(), 2);
                
        return score;
    }
    
    public static int[] gridBucket(Vector2D position)
    {
        int x = (int)(position.getX() * GRID_SIZE / BOARD_HEIGHT);
        int y = (int)(position.getY() * GRID_SIZE / BOARD_HEIGHT);
        
        if(x < 0)
            x = 0;
        if(x > GameConstants.GRID_SIZE - 1)
            x = GameConstants.GRID_SIZE - 1;
        if(y < 0)
            y = 0;
        if(y > GameConstants.GRID_SIZE - 1)
            y = GameConstants.GRID_SIZE - 1;
            
        int[] result = {x,y};
        return result;
    }
    
    public static void constrainToBoard(GameObject g)
    {
        if(g.getPosition().getX() + g.getRadius() > GameConstants.BOARD_WIDTH)
            g.setPosition(new Vector2D(GameConstants.BOARD_WIDTH - g.getRadius(), g.getPosition().getY()));
        if(g.getPosition().getY() + g.getRadius() > GameConstants.BOARD_HEIGHT)
            g.setPosition(new Vector2D(g.getPosition().getX(), GameConstants.BOARD_HEIGHT - g.getRadius()));
        if(g.getPosition().getX() - g.getRadius() < 0)
            g.setPosition(new Vector2D(g.getRadius(), g.getPosition().getY()));
        if(g.getPosition().getY() - g.getRadius() < 0)
            g.setPosition(new Vector2D(g.getPosition().getX(), g.getRadius()));
    }
}