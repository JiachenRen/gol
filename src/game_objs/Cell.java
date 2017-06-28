package game_objs;

import jui_lib.Displayable;
import jui_lib.Event;
import processing.core.PApplet;

import java.util.ArrayList;

/**
 * Created by Jiachen on 6/8/17.
 * This class represents an individual cell in Game of Life
 */
public class Cell extends Displayable {
    private boolean alive;
    private boolean prevState;
    private static boolean highlightingMotion;
    static Context context;
    int row;
    int col;

    /**
     * constructs a cell obj
     *
     * @param alive whether or not the cell initializes to be alive
     */
    Cell(boolean alive, int row, int col) {
        super();
        setAlive(alive);
        setBackgroundColor(mouseOverBackgroundColor);
        setRounded(false);
        setContourVisible(false);
        initEventListeners();
        this.row = row;
        this.col = col;
    }

    private void initEventListeners() {
        this.addEventListener("@TOGGLE", Event.MOUSE_PRESSED, () -> {
            if (context.selecting) {
                context.selecting = false;
                context.selected = new ArrayList<>();
            }
            if (!context.isInsertingConfig()) {
                if ((getParent().keyPressed && getParent().keyCode == PApplet.SHIFT)) {
                    this.setAlive(!isAlive());
                } else {
                    context.selecting = true;
                    context.startingPos = new int[]{row, col};
                }

            } else {
                context.insertConfig(row, col);
            }
        });
        this.addEventListener("@TOGGLE", Event.MOUSE_HELD, () -> {
            if ((getParent().keyPressed && getParent().keyCode == PApplet.SHIFT))
                this.setAlive(!prevState);
            else if (context.selecting) {
                context.highlight(context.startingPos, new int[]{row, col});
            }
        });
        this.addEventListener("@TOGGLE", Event.MOUSE_RELEASED, () -> {
            if (context.selecting) {
                context.select(context.startingPos, new int[]{row, col});
                context.endingPos = new int[]{row, col};
            }
        });
    }

    @Override
    public void mouseReleased() {
        super.mouseReleased();
        prevState = isAlive();
    }


    /**
     * returns true if the cell is alive
     */
    boolean isAlive() {
        return alive;
    }

    Cell setAlive(boolean alive) {
        if (this.alive != alive) {
            if (highlightingMotion)
                setBackgroundColor(255, 0, 0);
            else setBackgroundColor(255, 0, 0, 200);
            this.alive = alive;
        }
        if (alive) context.setActive(row, col);
        setBackgroundVisible(highlightingMotion || alive);
        return this;
    }

    @Override
    public void resize(float w, float h) {
        super.resize(w, h);
        this.setRounding(w);
    }

    @Override
    public void run() {
        super.run();
        if (alive) {
            setBackgroundColor(getParent().lerpColor(backgroundColor, getParent().color(100), 0.1f));
        } else if (highlightingMotion) {
            setBackgroundColor(getParent().lerpColor(backgroundColor, getParent().color(255), 0.1f));
        }

    }

    @Override
    public void display() {
        super.display();
        if (context.isInsertingConfig() && this.isMouseOver()) context.displayConfig(row, col);
        else if (this.isMouseOver() && !context.selecting) {
            highlight();
        }
    }

    void highlight() {
        context.dummyCell.resize(this.w, this.h);
        context.dummyCell.relocate(this.x, this.y);
        context.dummyCell.display();
    }

    public static void setHighlightingMotion(boolean temp) {
        Cell.highlightingMotion = temp;
    }

    public static boolean isHighlightingMotion() {
        return Cell.highlightingMotion;
    }

}
