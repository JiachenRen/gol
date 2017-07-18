package game_objs;

import com.sun.istack.internal.Nullable;
import jui_lib.*;
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
public class Context extends Displayable implements KeyControl {
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
    private boolean insertingConfig;
    private Config currentConfig;
    private static int availableProcessors;
    Displayable dummyCell;
    private boolean[][] resultMatrix;
    int[] startingPos;
    int[] endingPos;
    boolean selecting;
    ArrayList<Cell> selected;

    {
        Cell.context = this;
        availableProcessors = Runtime.getRuntime().availableProcessors();
    }

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
        initializeCellMatrix();
        initEventListeners();
        initDummyCell();
        selected = new ArrayList<>();
    }

    private void initDummyCell() {
        dummyCell = new Displayable()
                .setBackgroundColor(255, 0, 0, 100)
                //.setContourThickness(2)
                .setContourVisible(false)
                .setContourColor(255, 0, 0)
                .setVisible(true);
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
                cellMatrix[r][c] = new Cell(false, r, c);
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
                JNode.ripOff(cellMatrix[r][c]);
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

    void highlight(int[] beginPos, int[] endPos) {
        getParent().pushStyle();
        Cell begin = this.getCell(beginPos[0], beginPos[1]);
        Cell end = this.getCell(endPos[0], endPos[1]);

        getParent().stroke(255, 0, 0);
        getParent().fill(JNode.MOUSE_OVER_BACKGROUND_COLOR, 100);
        getParent().rectMode(PConstants.CORNER);
        float tx = begin.x < end.x ? begin.x : end.x;
        float ty = begin.y < end.y ? begin.y : end.y;
        float tw = Math.abs(begin.x - end.x) + begin.w;
        float th = Math.abs(begin.y - end.y) + begin.w;
        getParent().rect(tx, ty, tw, th);
        getParent().popStyle();
        for (Cell cell : activeCells)
            if (cell.isAlive() && cell.x >= tx && cell.y >= ty)
                if (cell.x + cell.w <= tx + tw && cell.y + cell.w <= ty + th)
                    cell.highlight();
    }

    void select(int[] beginPos, int[] endPos) {
        for (Cell cell : activeCells)
            if (cell.isAlive() && cell.row >= beginPos[0] && cell.row <= endPos[0])
                if (cell.col >= beginPos[1] && cell.col <= endPos[1])
                    selected.add(cell);
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
        selected.forEach(Cell::highlight);
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
        int pos[] = getCorrectedPos(row, col);
        return cellMatrix[pos[0]][pos[1]];
    }

    private int[] getCorrectedPos(int row, int col) {
        row = row >= this.rows ? row - rows : row;
        row = row <= -1 ? this.rows + row : row;
        col = col >= this.columns ? col - columns : col;
        col = col <= -1 ? this.columns + col : col;
        return new int[]{row, col};
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
                int[] pos = getCorrectedPos(row + i, col + q);
                if (getCell(pos[0], pos[1]).isAlive())
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
        resultMatrix = new boolean[rows][columns];

        //generate the result of the iteration using multi-threading techniques.
        ThreadGroup threadGroup = new ThreadGroup("computation");
        ArrayList<Computation> computations = new ArrayList<>();
        int indexSpan = activeCells.size() / availableProcessors + 1; //TODO: multi-threading actually made it slower!
        //System.out.println(getParent().frameRate);
        int i = 0;
        while (true) {
            if (i + indexSpan >= activeCells.size()) {
                computations.add(new Computation("", threadGroup, i, activeCells.size() - 1));
                break;
            } else {
                computations.add(new Computation("", threadGroup, i, i + indexSpan));
                i += indexSpan + 1;
            }
        }
        for (Computation computation : computations)
            computation.start();

        while (threadGroup.activeCount() > 0) {
            //
        }


        activeCells = new ArrayList<>();

        //updates the cells according to the result obtained
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < columns; c++) {
                cellMatrix[r][c].setAlive(resultMatrix[r][c]);
            }

    }

    /*
    private void applyRule30(boolean[][] input) {
        for (Cell cell : activeCells) {
            boolean pos00 = getCell(cell.row - 1, cell.col - 1).isAlive();
            boolean pos01 = getCell(cell.row - 1, cell.col).isAlive();
            boolean pos02 = getCell(cell.row - 1, cell.col + 1).isAlive();
            input[cell.row][cell.col] = !(pos00 && pos01 && pos02) && !(pos00 && pos01) && (pos00 && pos02 || pos00 || pos01 && pos02 || pos01 || pos02);
        }
    }
    */

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
     * @since June 13th: acceleration
     */
    public void setDimension(int rows, int cols) {
        if (rows == this.rows && cols == this.columns)
            return;
        if (rows >= this.rows && cols >= this.columns) {
            Cell[][] updatedMatrix = new Cell[rows][cols];
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (r < this.rows && c < this.columns)
                        updatedMatrix[r][c] = cellMatrix[r][c].setAlive(false);
                    else {
                        updatedMatrix[r][c] = new Cell(false, r, c);
                        updatedMatrix[r][c].setRelative(false);
                        JNode.add(updatedMatrix[r][c]);
                    }
                }
            }
            cellMatrix = updatedMatrix;
        } else if (rows <= this.rows && cols <= this.columns) {
            for (int r = 0; r < this.rows; r++) {
                for (int c = 0; c < this.columns; c++) {
                    if (r >= rows || c >= cols)
                        JNode.ripOff(cellMatrix[r][c]);
                }
            }
            activeCells = new ArrayList<>();
        } else {
            this.dispose();//resource expensive
            this.rows = rows;
            this.columns = cols;
            initializeCellMatrix();
        }
        this.rows = rows;
        this.columns = cols;
        this.requestUpdate();
    }

    public void clear() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < columns; c++)
                cellMatrix[r][c].setAlive(false);
        resultMatrix = new boolean[rows][columns];
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
     * loads a file that contains a saved game to the current game object.
     *
     * @param file the saved game file to be loaded
     */
    public void load(File file) {
        String lines[] = PApplet.loadStrings(file);
        if (!lines[0].equals("#saved")) return;
        String dim[] = lines[1].substring(lines[1].indexOf(":") + 1).split(",");
        this.setDimension(Integer.valueOf(dim[0]), Integer.valueOf(dim[1]));
        this.clear();
        String[] activeCellCoordinates = lines[2].substring(lines[2].indexOf(":") + 1).split(";");
        for (String coordinate : activeCellCoordinates) {
            String[] pos = coordinate.split(",");
            if (pos[0].equals("") || pos[1].equals("")) break;
            cellMatrix[Integer.valueOf(pos[0])][Integer.valueOf(pos[1])].setAlive(true);
        }
        System.out.println("loaded saved game:" + file.getName());
    }

    public static void importConfig(File file) {
        String lines[] = PApplet.loadStrings(file);
        if (lines[0].equals("#configs")) {
            String dim[] = lines[1].substring(lines[1].indexOf(":") + 1).split(",");
            Config config = new Config(file.getName(), parseInt(dim[0]), parseInt(dim[1]));
            String[] activeCellCoordinates = lines[2].substring(lines[2].indexOf(":") + 1).split(";");
            for (String coordinate : activeCellCoordinates) {
                String[] pos = coordinate.split(",");
                if (pos[0].equals("") || pos[1].equals("")) break;
                config.add(parseInt(pos[0]), parseInt(pos[1]));
            }
            Config.register(config);
        } else if (lines[0].equals("#configs-p") && lines.length > 1) {
            int cols = lines[1].length(), rows = lines.length - 1;
            Config config = new Config(file.getName(), rows, cols);
            for (int i = 1; i < lines.length; i++) {
                for (int q = 0; q < lines[i].length(); q++) {
                    if (lines[i].charAt(q) == '*') {
                        config.add(i - 1, q);
                    }
                }
            }
            Config.register(config);
        } else if (lines[0].equals("#configs-lif")) {
            int anchorRow = 0, anchorCol = 0, staringIndex = 0;
            ArrayList<int[]> coordinates = new ArrayList<>();
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                if (line.startsWith("#P ")) {
                    String temp[] = line.substring(3).split(" ");
                    anchorRow = parseInt(temp[0]);
                    anchorCol = parseInt(temp[1]);
                    staringIndex = i;
                } else for (int q = 0; q < line.length(); q++) {
                    if (line.charAt(q) == '*') {
                        coordinates.add(new int[]{anchorRow + i - staringIndex - 1, anchorCol + q});
                    }
                }
            }
            int minRow = Integer.MAX_VALUE, minCol = Integer.MAX_VALUE;
            for (int[] pos : coordinates) {
                minRow = pos[0] < minRow ? pos[0] : minRow;
                minCol = pos[1] < minCol ? pos[1] : minCol;
            }
            int maxRow = Integer.MIN_VALUE, maxCol = Integer.MIN_VALUE;
            for (int i = 0; i < coordinates.size(); i++) {
                int[] pos = coordinates.get(i);
                int[] updated = new int[]{pos[0] - minRow, pos[1] - minCol};
                maxRow = pos[0] > maxRow ? pos[0] : maxRow;
                maxCol = pos[1] > maxCol ? pos[1] : maxCol;
                coordinates.set(i, updated);
            }
            Config config = new Config(file.getName(), maxRow, maxCol);
            coordinates.forEach(coordinate -> config.add(coordinate[0], coordinate[1]));
            Config.register(config);
        }
        System.out.println("loaded config: " + file.getName());

    }

    /**
     * use "saved" for saved games, use "configs" for saving permanent configurations.
     * if it is saved as a configuration, then it would be available for manipulation.
     *
     * @param fileName the name of the file to be saved
     * @param type     saved as a configuration or a plain "saved game"
     */
    @SuppressWarnings("ConstantConditions")
    public void save(String fileName, String type) {
        try {
            String path = runningAsApplication ? getAlternativePath(type) : getFilesPath(type);
            PrintWriter writer = new PrintWriter(path + "/" + fileName, "UTF-8");
            writer.println("#" + type);
            if (type.equals("saved")) {
                writer.println("~ dim:" + rows + "," + columns);
                writer.print("~ pos:");
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < columns; c++) {
                        if (cellMatrix[r][c].isAlive())
                            writer.print(r + "," + c + ";");
                    }
                }
            } else {
                int lowestCol = this.columns, lowestRow = this.rows;
                int largestCol = 0, largestRow = 0;
                ArrayList<Cell> aliveCells = new ArrayList<>();
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < columns; c++) {
                        Cell cell = cellMatrix[r][c];
                        if (!cell.isAlive()) continue;
                        lowestCol = c < lowestCol ? c : lowestCol;
                        lowestRow = r < lowestRow ? r : lowestRow;
                        largestCol = c > largestCol ? c : largestCol;
                        largestRow = r > largestRow ? r : largestRow;
                        aliveCells.add(cell);
                    }
                }
                writer.println("~ dim:" + (largestRow - lowestRow) + "," + (largestCol - lowestCol));
                writer.print("~ pos:");
                final int finalLowestCol = lowestCol;
                final int finalLowestRow = lowestRow;
                aliveCells.forEach(cell -> writer.print((cell.row - finalLowestRow) + "," + (cell.col - finalLowestCol) + ";"));
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("saved: " + fileName);
    }


    /**
     * TODO: improve
     */
    public void keyPressed() {
        if (currentConfig != null && isInsertingConfig()) {
            switch (getParent().keyCode) {
                case PApplet.UP:
                    currentConfig.flip(Config.Dir.VERTICAL);
                    break;
                case PApplet.DOWN:
                    currentConfig.flip(Config.Dir.HORIZONTAL);
                case PApplet.RIGHT:
                    currentConfig.flip(Config.Dir.ROTATE);
            }
        } else if (selecting || selected.size() > 0) {
            switch (getParent().keyCode) {
                case 8:
                    delete();
                    break;
            }
            switch (getParent().key) {
                case 'C':
                    paste();
                    break;
                case 'X':
                    paste();
                    delete();
                    break;
            }
            selecting = false;
            selected = new ArrayList<>();
        }
    }

    private void delete() {
        selected.forEach(cell -> cell.setAlive(false));
    }

    private void paste() {
        int rows = Math.abs(startingPos[0] - endingPos[0]);
        int cols = Math.abs(startingPos[1] - endingPos[1]);
        int sx = startingPos[0] < endingPos[0] ? startingPos[0] : endingPos[0];
        int sy = startingPos[1] < endingPos[1] ? startingPos[1] : endingPos[1];
        Config temp = new Config("temp", rows, cols);
        selected.forEach(cell -> temp.add(cell.row - sx, cell.col - sy));
        this.currentConfig = temp;
        ((Switch) JNode.get("@INSERT").get(0)).setState(true).activate();
    }


    public void keyReleased() {
        //if (getParent().keyCode == PApplet.SHIFT)
        //   this.selecting = false;
    }

    private static String getFilesPath(String fileName) {
        return JNode.getParent().sketchPath() + "/src/game_objs/" + fileName;
    }

    private static int parseInt(String s) {
        return PApplet.parseInt(s);
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

    public static File[] listOfFiles(String dir) {
        File folder = new File(Context.getFilesPath(dir));
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null) {
            @SuppressWarnings("ConstantConditions") File alternateFolder = new File(Context.getAlternativePath(dir));
            listOfFiles = alternateFolder.listFiles();
        }
        assert listOfFiles != null;
        return listOfFiles;
    }

    public void setCurrentConfig(String configName) {
        currentConfig = Config.extract(configName);
        System.out.println("current config: " + configName);
    }

    public void setInsertingConfig(boolean temp) {
        this.insertingConfig = temp;
    }

    boolean isInsertingConfig() {
        return insertingConfig;
    }

    public Config getCurrentConfig() {
        return currentConfig;
    }

    void displayConfig(int row, int col) {
        ArrayList<int[]> cellCoordinates = currentConfig.translatedCoordinate(row, col);
        dummyCell.resize(cellMatrix[0][0].w, cellMatrix[0][0].h);
        for (int[] pos : cellCoordinates) {
            Cell cell = this.getCell(pos[0], pos[1]);
            dummyCell.relocate(cell.x, cell.y);
            dummyCell.display();
        }
    }

    void insertConfig(int row, int col) {
        ArrayList<int[]> cellCoordinates = currentConfig.translatedCoordinate(row, col);
        for (int[] pos : cellCoordinates) {
            Cell cell = getCell(pos[0], pos[1]);
            cell.setAlive(!cell.isAlive());
        }
    }


    private static class Config {
        private static ArrayList<Config> configs;
        private ArrayList<int[]> coordinates;
        private String name;
        private int rows;
        private int cols;

        private enum Dir {
            HORIZONTAL,
            VERTICAL,
            ROTATE,
        }

        static {
            configs = new ArrayList<>();
        }

        {
            coordinates = new ArrayList<>();
        }

        private Config(String name, int rows, int cols) {
            this.rows = rows;
            this.cols = cols;
            this.name = name;
        }

        private void add(int row, int col) {
            coordinates.add(new int[]{row, col});
        }

        private static void register(Config config) {
            configs.add(config);
        }


        /**
         * inserts this configuration of cells into the designated context at a specific location.
         * <p>
         * //@param context the context in which the configuration is going to be inserted.
         *
         * @param row starting row
         * @param col starting column
         */
        private ArrayList<int[]> translatedCoordinate(int row, int col) {
            ArrayList<int[]> translated = new ArrayList<>();
            for (int[] coordinate : coordinates)
                translated.add(new int[]{row + coordinate[0], col + coordinate[1]});
            return translated;
        }

        @Nullable
        private static Config extract(String name) {
            for (Config config : configs)
                if (config.name.equals(name))
                    return config;
            return null;
        }

        private void flip(Dir dir) {
            System.out.println(rows + " " + cols);
            if (dir == Dir.HORIZONTAL) {
                for (int i = 0; i < coordinates.size(); i++) {
                    int[] coordinate = coordinates.get(i);
                    coordinates.set(i, new int[]{cols - coordinate[1], coordinate[0]});
                }
                revertDimension();
            } else if (dir == Dir.VERTICAL) {
                for (int i = 0; i < coordinates.size(); i++) {
                    int[] coordinate = coordinates.get(i);
                    coordinates.set(i, new int[]{rows - coordinate[0], coordinate[1]});
                }
            } else if (dir == Dir.ROTATE) {
                for (int i = 0; i < coordinates.size(); i++) {
                    int[] coordinate = coordinates.get(i);
                    coordinates.set(i, new int[]{coordinate[1], coordinate[0]});
                }
                revertDimension();
            }
        }

        private void revertDimension() {
            int temp = rows;
            rows = cols;
            cols = temp;
        }

    }

    /**
     * spawn random cells in the cell matrix
     *
     * @param chance a number between 0 and 1 that indicates whether or
     *               not the cell would be alive.
     */
    public void spawnRandom(float chance) {
        for (Cell[] cells : cellMatrix)
            for (Cell cell : cells)
                if (Math.random() < chance)
                    cell.setAlive(!cell.isAlive());
    }

    private class Computation extends Thread {
        private int startIndex, endIndex;

        private Computation(String name, ThreadGroup threadGroup, int startIndex, int endIndex) {
            super(threadGroup, name);
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        private void compute() {
            for (int i = startIndex; i <= endIndex; i++) {
                Cell cell = activeCells.get(i);
                int numAlive = numCellsAlive(cell.row, cell.col);
                if (cell.isAlive())
                    resultMatrix[cell.row][cell.col] = numAlive > 1 && numAlive < 4;
                else resultMatrix[cell.row][cell.col] = numAlive == 3;
            }
        }

        @Override
        public void run() {
            this.compute();
        }
    }
}
