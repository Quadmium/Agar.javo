import java.awt.Color;

public final class GameConstants
{
    public static final double BOARD_WIDTH = 500, BOARD_HEIGHT = 500;
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

    public static double getBoundRadius(double radius)
    {
        double slope = (FINAL_BOUNDRADIUS - INITIAL_BOUNDRADIUS) / 
            (FINAL_RADIUS - INITIAL_RADIUS);

        return slope * (radius - INITIAL_RADIUS) + INITIAL_BOUNDRADIUS;
    }
}