package game_objs;

import jui_lib.Displayable;
import jui_lib.Event;
import jui_lib.EventListener;
import jui_lib.JNode;
import processing.core.PApplet;
import processing.core.PConstants;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * Created by Jiachen on 6/8/17.
 * This class represents the game context that manages the matrix of cells and executes
 * the game logic. It also makes use of some apparatus of the super class Displayable
 * (designed by Jiachen) to manage its relative position on screen so that it works well
 * when integrated with JUI.
 */
public class Context extends Displayable {
    private Cell[][] cellMatrix;
    private int rows;
    private int columns;
    private int millisPerIteration;
    private long lastIteratedMillis;
    private float gridRootX[];
    private float gridRootY[];
    private boolean cellGridVisible;
    private ArrayList<Cell> activeCells;
    private static boolean runningAsApplication;

    /**
     * constructs a dimensionless Context obj that hosts the Game of Life.
     *
     * @param rows    number of rows
     * @param columns number of columns
     */
    public Context(String id, int rows, int columns) {
        super();
        this.setId(id);
        this.rows = rows;
        this.columns = columns;
        millisPerIteration = 10; //defaults to one iteration per 100 milliseconds.
        cellGridVisible = true;
        setBackgroundVisible(false);
        setContourThickness(0.2f);
        //setContourVisible(true);
        initializeCellMatrix();
        initEventListeners();
    }

    /**
     * initializes the iteration event listener. The event listener is invoked
     * by JNode each time the game context is iterated. The event listener updates
     * the cell matrix so that one iteration of Conway's Game of Life is performed.
     */
    private void initEventListeners() {
        this.addEventListener(new EventListener("@ITERATOR", Event.CONTINUOUS).attachMethod(() -> {
            if (System.currentTimeMillis() - lastIteratedMillis > millisPerIteration) {
                iterate();
                lastIteratedMillis = System.currentTimeMillis();
            }
        }).setDisabled(true));
    }

    /**
     * Constructs the cell matrix (using a 2-dimensional array); cells in
     * the matrix are registered into JNode to accept transferred events.
     */
    private void initializeCellMatrix() {
        cellMatrix = new Cell[rows][columns];
        activeCells = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                cellMatrix[r][c] = new Cell(this, false, r, c);
                cellMatrix[r][c].setId("#" + this.id + " " + r + " " + c);
                cellMatrix[r][c].setRelative(false);
                JNode.add(cellMatrix[r][c]);
            }
        }
    }

    /**
     * finalize() is called by the garbage collector when it determines that all
     * of the references to the object have disappeared. By overriding this method
     * the cells created by this context are effectively removed from the heap as
     * the references to the cells are removed from JNode.
     */
    @Override
    public void finalize() throws Throwable {
        super.finalize();
        this.dispose();
    }

    private void dispose() {
        getParent().noLoop();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                //JNode.remove("#" + this.id + " " + r + " " + c);
                JNode.remove(cellMatrix[r][c]);
            }
        }
        getParent().loop();
    }

    /**
     * @return the width of the cell in pixels derived from the number of rows v.s.
     * height and number of columns v.s. height. The cell width is calculated
     * so that all of the cells are of equal width and that the matrix formed by
     * the collection of all cells is contained in the declared dimension of the
     * context object.
     */
    private float cellWidth() {
        float widthCols = getWidth() / columns;
        float widthRows = getHeight() / rows;
        return widthCols > widthRows ? widthRows : widthCols;
    }

    /**
     * @return the width in pixels of the cell matrix
     */
    private float cellMatrixWidth() {
        return columns * cellWidth();
    }

    /**
     * @return the height in pixels of the cell matrix.
     */
    private float cellMatrixHeight() {
        return rows * cellWidth();
    }


    /**
     * @param w new width in pixels
     * @param h new height in pixels
     */
    @Override
    public void resize(float w, float h) {
        super.resize(w, h);
        resizeCells();
    }

    /**
     * Resize the cells contained
     */
    private void resizeCells() {
        float cellWidth = cellWidth();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                cellMatrix[r][c].resize(cellWidth, cellWidth);
            }
        }
    }

    @Override
    public void relocate(float x, float y) {
        super.relocate(x, y);
        relocateCells();
    }

    /**
     * Rearrange each of the cells to designated coordinate on screen
     * and updates the root coordinates for drawing the grid. Wastes a little
     * bit of memory to enhance performance.
     */
    private void relocateCells() {
        float cellWidth = cellWidth();
        float offsetX = getCellMatrixX();
        float offsetY = getCellMatrixY();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                cellMatrix[r][c].relocate(offsetX + c * cellWidth, offsetY + r * cellWidth);
            }
        }

        //updates the grid root coordinates.
        gridRootX = new float[columns - 1];
        for (int c = 1; c < columns; c++) {
            gridRootX[c - 1] = offsetX + c * cellWidth;
        }

        gridRootY = new float[rows - 1];
        for (int r = 1; r < rows; r++) {
            gridRootY[r - 1] = offsetY + r * cellWidth;
        }
    }

    private float getCellMatrixX() {
        float diffWidth = Math.abs(w - cellMatrixWidth());
        float diffHeight = Math.abs(h - cellMatrixHeight());
        return !(diffWidth > diffHeight) ? x : x + w / 2 - cellMatrixWidth() / 2;
    }

    private float getCellMatrixY() {
        float diffWidth = Math.abs(w - cellMatrixWidth());
        float diffHeight = Math.abs(h - cellMatrixHeight());
        return !(diffWidth > diffHeight) ? y + h / 2 - cellMatrixHeight() / 2 : y;
    }

    /**
     * render the cells
     */
    @Override
    public void display() {
        if (cellGridVisible)
            drawMatrixGrid();
    }

    /**
     * NOTE: rows and columns are indexed from 0; if an index
     * if -1 or larger than the available indices in rows/columns,
     * the index would be automatically corrected by rolling over.
     *
     * @param row the row of the designated cell
     * @param col the column of the designated cell
     * @return cell at the designated position
     */
    private Cell getCell(int row, int col) {
        row = row == this.rows ? 0 : row;
        row = row == -1 ? this.rows - 1 : row;
        col = col == this.columns ? 0 : col;
        col = col == -1 ? this.columns - 1 : col;
        return cellMatrix[row][col];
    }

    /**
     * @param row row in which the center cell is located
     * @param col column in which the center cell is located
     * @return the number of living cells around the center cell
     */
    private int numCellsAlive(int row, int col) {
        int count = 0;
        for (int i = -1; i <= 1; i++) {
            for (int q = -1; q <= 1; q++) {
                if (i == 0 && q == 0) continue;
                if (getCell(row + i, col + q).isAlive())
                    count++;
            }
        }
        return count;
    }

    /**
     * this method performs a single iteration of the Conway's Game of Life.
     * The logic is very simple:
     * 1. a living cell with zero or one living neighbours will die.
     * 2. a living cell with two or three live neighbour will remain alive.
     * 3. a living cell with four or more living neighbours will die.
     * 4. a dead cell with exactly three living neighbours comes alive.
     */
    public void iterate() {
        boolean[][] result = new boolean[rows][columns];

        //generate the result of this iteration
        for (Cell cell : activeCells) {
            int numAlive = numCellsAlive(cell.row, cell.col);
            if (cell.isAlive())
                result[cell.row][cell.col] = numAlive > 1 && numAlive < 4;
            else result[cell.row][cell.col] = numAlive == 3;
        }
        //System.out.println(activeCells.size());



        /*
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                int numAlive = numCellsAlive(r, c);
                if (cellMatrix[r][c].isAlive())
                    result[r][c] = numAlive > 1 && numAlive < 4;
                else result[r][c] = numAlive == 3;
            }
        }
        */

        activeCells = new ArrayList<>();
        //updates the cells according to the result obtained
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < columns; c++) {
                cellMatrix[r][c].setAlive(result[r][c]);
                if (result[r][c]) setActive(r, c);
            }
    }

    void setActive(int row, int col) {
        for (int i = -1; i <= 1; i++) {
            for (int q = -1; q <= 1; q++) {
                Cell designated = getCell(row + i, col + q);
                if (!activeCells.contains(designated))
                    activeCells.add(designated);
            }
        }
    }

    public void setMillisPerIteration(int millis) {
        this.millisPerIteration = millis;
    }

    public void toggleAutoIteration() {
        EventListener iterator = this.getEventListener("@ITERATOR");
        iterator.setDisabled(!iterator.isDisabled());
    }

    /**
     * updates the dimension of the cell matrix.
     *
     * @param rows number of rows
     * @param cols number of columns
     */
    public void setDimension(int rows, int cols) {
        //System.out.println("disposing");
        this.dispose(); //TODO: too slow!
        //System.out.println("disposed");
        this.rows = rows;
        this.columns = cols;
        initializeCellMatrix();
        requestUpdate();
    }

    /**
     * draws the gird for the cell matrix.
     */
    private void drawMatrixGrid() {
        super.applyBackgroundStyle();
        float offsetX = getCellMatrixX();
        float offsetY = getCellMatrixY();
        float matrixHeight = cellMatrixHeight();
        float matrixWidth = cellMatrixWidth();

        getParent().strokeWeight(contourThickness);
        getParent().rectMode(PConstants.CORNER);
        getParent().rect(offsetX, offsetY, matrixWidth, matrixHeight);
        for (float i : gridRootX) {
            getParent().line(i, offsetY, i, offsetY + matrixHeight);
        }
        for (float i : gridRootY) {
            getParent().line(offsetX, i, offsetX + matrixWidth, i);
        }
    }

    @Override
    public Displayable setContourThickness(float temp) {
        if (temp > 1) throw new IllegalArgumentException("grid thickness cannot be larger than 1");
        super.setContourThickness(temp);
        return this;
    }

    /**
     * TODO: implement, java doc
     *
     * @param file the file (configuration or saved game) to be loaded
     */
    public void load(File file) {
        String lines[] = PApplet.loadStrings(file);
        if (lines[0].equals("#saved")) {
            System.out.println("loaded saved game:" + file.getName());
            String dim[] = lines[1].substring(lines[1].indexOf(":") + 1).split(",");
            this.setDimension(Integer.valueOf(dim[0]), Integer.valueOf(dim[1]));
            String[] activeCellCoordinates = lines[2].substring(lines[2].indexOf(":") + 1).split(";");
            for (String coordinate : activeCellCoordinates) {
                String[] pos = coordinate.split(",");
                if (pos[0].equals("") || pos[1].equals("")) break;
                cellMatrix[Integer.valueOf(pos[0])][Integer.valueOf(pos[1])].setAlive(true);
            }
        }
    }

    public void save(String fileName) {
        System.out.println("saved: " + fileName);
        try {
            String path = runningAsApplication ? getAlternativePath("saved") : getFilesPath("saved");
            PrintWriter writer = new PrintWriter(path + "/" + fileName, "UTF-8");
            writer.println("#saved");
            writer.println("~ dim:" + rows + "," + columns);
            writer.print("~ pos:");
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < columns; c++) {
                    if (cellMatrix[r][c].isAlive())
                        writer.print(r + "," + c + ";");
                }
            }
            writer.close();
        } catch (IOException e) {
            // do something
        }

    }

    private static String getFilesPath(String fileName) {
        return JNode.getParent().sketchPath() + "/src/game_objs/" + fileName;
    }

    private static String getAlternativePath(String fileName) {
        runningAsApplication = true;
        try {
            String pathToPApplet = PApplet.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            int index = pathToPApplet.indexOf("Contents");
            return pathToPApplet.substring(0, index + "Contents".length()) + "/saved_configs/" + fileName;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static File[] listOfFiles() {
        File folder = new File(Context.getFilesPath("saved"));
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null) {
            @SuppressWarnings("ConstantConditions") File alternateFolder = new File(Context.getAlternativePath("saved"));
            listOfFiles = alternateFolder.listFiles();
        }
        assert listOfFiles != null;
        return listOfFiles;
    }
}
