
import game_objs.Context;
import jui_lib.*;
import jui_lib.bundles.ValueSelector;
import processing.core.PApplet;

public class Main extends PApplet {
    private static int rows = 60;
    private static int cols = 80;
    private static String sketchRenderer;
    private static boolean smooth;
    private static boolean retina;

    public static void main(String[] args) {
        sketchRenderer = args.length > 0 ? args[0] : "processing.awt.PGraphicsJava2D";
        smooth = args.length > 1 && args[1].equals("1");
        retina = args.length > 2 && args[2].equals("1");
        String sketch = Thread.currentThread().getStackTrace()[1].getClassName();
        Thread proc = new Thread(() -> PApplet.main(sketch));
        proc.start();
    }

    public void settings() {
        size(900, 600, sketchRenderer);
        System.out.println(sketchRenderer());
        if (retina) pixelDensity(2);
        if (!smooth) noSmooth(); //sometimes noSmooth works better.
    }

    public void setup() {
        surface.setResizable(true);
        surface.setTitle("Game of Life");

        JNode.init(this); // always to be used as the first line inside setup

        HBox parent = new HBox();
        parent.matchWindowDimension(true)
                .setCollapseInvisible(true);
        JNode.add(parent);

        Context gameContext = new Context("#CONTEXT", rows, cols);
        parent.add(gameContext);

        VBox uiPanel = new VBox(0.1f, 1.0f);
        parent.add(uiPanel);

        Button step = new Button(1.0f, 0.05f)
                .setContent("Step")
                .onClick(getContext()::iterate);
        uiPanel.add(step);

        uiPanel.add(new SpaceHolder());

        Switch flowControl = (Switch) new Switch()
                .setContent("Iterating", "Paused")
                .setState(false)
                .onClick(getContext()::toggleAutoIteration)
                .inheritDisplayProperties(step);
        uiPanel.add(flowControl);

        uiPanel.add(new SpaceHolder());

        ValueSelector speed = new ValueSelector(1.0f, 0.1f)
                .setTitlePercentage(0.3f)
                .roundTo(-1)
                .setTitle("ms/i")
                .setRange(0, 1000)
                .setValue(10);
        speed.link(() -> {
            getContext().setMillisPerIteration(speed.getIntValue());
        });
        uiPanel.add(speed);

        uiPanel.add(new SpaceHolder());

        ValueSelector frameRate = new ValueSelector(1.0f, 0.1f)
                .setTitlePercentage(0.3f)
                .roundTo(-1)
                .setTitle("fps")
                .setRange(60, 300)
                .setValue(60);
        frameRate.link(() -> {
            frameRate(frameRate.getIntValue());
        });
        uiPanel.add(frameRate);

        uiPanel.add(new SpaceHolder());

        ValueSelector rows = new ValueSelector(1.0f, 0.1f)
                .setTitlePercentage(0.3f)
                .roundTo(-1)
                .setTitle("rows")
                .setRange(10, 120)
                .setValue(Main.rows);
        uiPanel.add(rows.link(() -> Main.rows = rows.getIntValue()));

        ValueSelector cols = new ValueSelector(1.0f, 0.1f)
                .setTitlePercentage(0.3f)
                .roundTo(-1)
                .setTitle("cols")
                .setRange(10, 160)
                .setValue(Main.cols);
        uiPanel.add(cols.link(() -> Main.cols = cols.getIntValue()));

        uiPanel.add(new Button(1.0f, 0.05f).setContent("Update").onClick(() -> {
            getContext().setDimension(Main.rows, Main.cols);
        })).attachMethod(() -> {
            if (!mousePressed && !uiPanel.isVisible && Math.abs(mouseX - width) < 10) {
                uiPanel.setVisible(true);
                Container.refresh();
            } else if (uiPanel.isVisible && width - parent.spacing * 2 - uiPanel.w > mouseX) {
                uiPanel.setVisible(false);
                Container.refresh();
            }
        }).setVisible(false);


    }

    private Context getContext() {
        return (Context) JNode.get("#CONTEXT").get(0);
    }

    public void draw() {
        background(255);
        JNode.run();
    }

    public void keyPressed() {
        JNode.keyPressed();
    }

    public void keyReleased() {
        JNode.keyReleased();
    }
}
