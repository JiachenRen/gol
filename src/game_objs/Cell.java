package game_objs;

import jui_lib.Displayable;
import jui_lib.Event;
import jui_lib.JNode;

/**
 * Created by Jiachen on 6/8/17.
 * This class represents an individual cell in Game of Life
 */
public class Cell extends Displayable {
    private boolean alive;
    private static int colorAlive;
    private static int colorDead;
    private boolean prevState;

    /*
    default color for a cell that is alive/dead.
     */
    static {
        colorAlive = JNode.getParent().color(0);
        colorDead = JNode.getParent().color(255);
    }

    /**
     * constructs a cell obj
     *
     * @param alive whether or not the cell initializes to be alive
     */
    public Cell(boolean alive) {
        super();
        setAlive(alive);
        setContourVisible(true);
        setContourThickness(0.2f);
        setRounded(false);
        initEventListeners();
    }

    private void initEventListeners() {
        this.addEventListener("@TOGGLE", Event.MOUSE_PRESSED, () -> this.setAlive(!isAlive()));
        this.addEventListener("@TOGGLE", Event.MOUSE_DRAGGED, () -> this.setAlive(!prevState));
    }

    @Override
    public void mouseReleased() {
        super.mouseReleased();
        prevState = isAlive();
    }


    /**
     * returns true if the cell is alive
     */
    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
        setBackgroundColor(alive ? colorAlive : colorDead);
    }

    public static void setColorAlive(int color) {
        Cell.colorAlive = color;
    }

    public static void setColorDead(int color) {
        Cell.colorDead = color;
    }


}
