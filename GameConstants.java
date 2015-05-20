import java.awt.Color;
import java.util.ArrayList;

public final class GameConstants
{
    public static final double BOARD_WIDTH = 500, BOARD_HEIGHT = 500;
    public static final int FOOD_GOAL = 4000;
    public static final double FOOD_RADIUS = 0.6;
    public static final double FOOD_VOLUME = 1;
    public static final double EAT_RATIO = 1.1;
    public static final double MIN_SPLIT_RADIUS = 1.3;
    public static final int MAX_SPLIT = 8;
    public static final int IDEAL_FPS = 60;
    
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
    public static final double FINAL_RADIUS_VELOCITY = 10;
    public static final double FINAL_VELOCITY = 4;

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
    
    public static final double SPLIT_BONUS_VELOCITY_RATIO = 1.2;
    
    public static double maximumVelocity(double radius)
    {
        double k = INITIAL_VELOCITY;
        double n = Math.log(FINAL_VELOCITY / INITIAL_VELOCITY) / Math.log(FINAL_RADIUS_VELOCITY);
        
        return k * Math.pow(radius, n);
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
}