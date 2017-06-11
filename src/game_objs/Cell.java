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
    private boolean prevState;
    private Context context;
    int row;
    int col;

    /**
     * constructs a cell obj
     *
     * @param alive whether or not the cell initializes to be alive
     */
    public Cell(Context context, boolean alive, int row, int col) {
        super();
        setAlive(alive);
        setBackgroundColor(mouseOverBackgroundColor);
        setRounded(false);
        setContourVisible(false);
        initEventListeners();
        this.context = context;
        this.row = row;
        this.col = col;
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
        if (alive) context.setActive(row,col);
        setBackgroundVisible(alive);
    }

    @Override
    public void resize(float w, float h){
        super.resize(w,h);
        this.setRounding(w);
    }
}
