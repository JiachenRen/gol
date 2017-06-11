
import game_objs.Context;
import jui_lib.*;
import jui_lib.bundles.AbstractValueSelector;
import jui_lib.bundles.CompositeValueSelector;
import processing.core.PApplet;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class Main extends PApplet {
    private static int rows = 60;
    private static int cols = 80;
    private static String sketchRenderer;
    private static boolean smooth;
    private static boolean retina;
    private static Label modelLabel;

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
        JNode.DISPLAY_CONTOUR = false;
        JNode.CONTOUR_THICKNESS = 0.5f;
        JNode.BACKGROUND_COLOR = color(255);
        JNode.ROUNDED = false;
        JNode.ROUNDING = 20;

        //setting up models
        modelLabel = (Label) new Label().setAlign(CENTER).setContourVisible(false).setBackgroundColor(0, 0, 0, 25);


        HBox parent = new HBox();
        parent.matchWindowDimension(true)
                .setCollapseInvisible(true);
        JNode.add(parent);


        VBox leftUiPanel = (VBox) new VBox(0, 1.0f).setCollapseInvisible(true);
        leftUiPanel.attachMethod(() -> {
            if (mousePressed) return;
            if (mouseX < 10) {
                leftUiPanel.setVisible(true);
                if (leftUiPanel.getRelativeW() < 0.1f)
                    leftUiPanel.setRelativeW(leftUiPanel.getRelativeW() + 0.01f);
                Container.refresh();
            } else if (leftUiPanel.isVisible() && mouseX > leftUiPanel.w + parent.spacing) {
                if (leftUiPanel.getRelativeW() == 0) {
                    leftUiPanel.setVisible(false);
                }
                if (leftUiPanel.getRelativeW() > 0) {
                    leftUiPanel.setRelativeW(leftUiPanel.getRelativeW() - 0.01f);
                    if (leftUiPanel.getRelativeW() < 0)
                        leftUiPanel.setRelativeW(0);
                }
                Container.refresh();
            }
        }).setVisible(false);
        parent.add(leftUiPanel);

        leftUiPanel.add(new Label(1.0f, 0.05f).setContent("Load Saved").inheritOutlook(modelLabel));

        TextInput filterer = new TextInput(1.0f, 0.05f).setDefaultContent("");
        filterer.setIsFocusedOn(true).onKeyTyped(() -> {
            ArrayList<Button> filtered = new ArrayList<>();
            ArrayList<Button> savedButtons = getSavedButtons();
            savedButtons.forEach(button -> {
                if (button.getContent().startsWith(filterer.getContent()))
                    if (!filtered.contains(button))
                        filtered.add(button);
            });
            savedButtons.forEach(button -> {
                if (button.getContent().contains(filterer.getContent()))
                    if (!filtered.contains(button))
                        filtered.add(button);
            });
            savedButtons.forEach(button -> button.setVisible(false));
            filtered.forEach(button -> button.setVisible(true));
        });
        leftUiPanel.add(filterer);

        int maxSavedNum = 0;
        for (File file : Context.listOfFiles()) {
            if (!file.isFile()) continue;
            if (file.getName().startsWith("saved_game_")) {
                int num = Integer.valueOf(file.getName().substring(11));
                maxSavedNum = num > maxSavedNum ? num : maxSavedNum;
            }
            leftUiPanel.add(new Button(1.0f, 0.05f)
                    .setContent(file.getName())
                    .onClick(() -> {
                        getContext().load(file);
                    }).setVisible(false)
                    .setId("@SAVED")
            );
        }

        leftUiPanel.add(new SpaceHolder());
        leftUiPanel.add(new Label(1.0f, 0.05f).setContent("File Name").inheritOutlook(modelLabel));

        TextInput fileName = new TextInput(1.0f, 0.05f).setDefaultContent("saved_game_" + (maxSavedNum + 1));
        leftUiPanel.add(fileName.setId("@FILE_NAME"));

        Button save = new Button(1.0f, 0.05f).setContent("Save").onClick(() -> {
            getContext().save(fileName.getContent());
            ArrayList<Button> savedButtons = this.getSavedButtons();
            boolean overridden = false;
            for (Button button : savedButtons)
                if (button.getId().equals(fileName.getContent()))
                    overridden = true;

            if (overridden) return;
            for (File file : Context.listOfFiles())
                if (file.getName().equals(fileName.getContent()))
                    leftUiPanel.add(2, new Button(1.0f, 0.05f)
                            .setContent(fileName.getContent())
                            .onClick(() -> {
                                getContext().load(file);
                            }).setVisible(true)
                            .setId("@SAVED")
                    );

            if (fileName.getDefaultContent().equals(fileName.getContent())) return;
            int newFileIndex = parseInt(fileName.getDefaultContent().substring(11)) + 1;
            fileName.setDefaultContent("saved_game_" + newFileIndex);
        });
        leftUiPanel.add(save);


        Context gameContext = new Context("#CONTEXT", rows, cols);
        parent.add(gameContext);

        VBox uiPanel = new VBox(0, 1.0f);
        uiPanel.setDebugEnabled(false);
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

        AbstractValueSelector speed = new CompositeValueSelector(1.0f, 0.1f)
                .setTitlePercentage(0.3f)
                .roundTo(-1)
                .setTitle("ms/i")
                .setRange(0, 1000)
                .setValue(10);
        speed.link(() -> {
            getContext().setMillisPerIteration(speed.getIntValue());
        }).getTitleLabel().inheritOutlook(modelLabel);
        uiPanel.add(speed);

        uiPanel.add(new SpaceHolder());

        AbstractValueSelector frameRate = new CompositeValueSelector(1.0f, 0.1f)
                .setTitlePercentage(0.3f)
                .roundTo(-1)
                .setTitle("fps")
                .setRange(60, 300)
                .setValue(60);
        frameRate.link(() -> {
            frameRate(frameRate.getIntValue());
        }).getTitleLabel().inheritOutlook(modelLabel);
        uiPanel.add(frameRate);

        uiPanel.add(new SpaceHolder());

        AbstractValueSelector rows = new CompositeValueSelector(1.0f, 0.1f)
                .setTitlePercentage(0.3f)
                .roundTo(-1)
                .setTitle("rows")
                .setRange(10, 300)
                .setValue(Main.rows);
        rows.getTitleLabel().inheritOutlook(modelLabel);
        uiPanel.add(rows.link(() -> {
            Main.rows = rows.getIntValue();
        }));

        AbstractValueSelector cols = new CompositeValueSelector(1.0f, 0.1f)
                .setTitlePercentage(0.3f)
                .roundTo(-1)
                .setTitle("cols")
                .setRange(10, 300)
                .setValue(Main.cols);
        cols.getTitleLabel().inheritOutlook(modelLabel);
        uiPanel.add(cols.link(() -> Main.cols = cols.getIntValue()));

        uiPanel.add(new Button(1.0f, 0.05f).setContent("Update").onClick(() -> {
            getContext().setDimension(Main.rows, Main.cols);
        })).attachMethod(() -> {
            if (mousePressed) return;
            if (Math.abs(mouseX - width) < 10) {
                uiPanel.setVisible(true);
                if (uiPanel.getRelativeW() < 0.1f)
                    uiPanel.setRelativeW(uiPanel.getRelativeW() + 0.01f);
                //System.out.println(uiPanel.relativeW);
                Container.refresh();
            } else if (width - parent.spacing * 2 - uiPanel.w > mouseX) {
                if (!uiPanel.isVisible()) return;
                if (uiPanel.getRelativeW() == 0) {
                    uiPanel.setVisible(false);
                }
                if (uiPanel.getRelativeW() > 0) {
                    uiPanel.setRelativeW(uiPanel.getRelativeW() - 0.01f);
                    if (uiPanel.getRelativeW() < 0)
                        uiPanel.setRelativeW(0);
                }
                Container.refresh();
            }
        }).setVisible(false);


    }

    @SuppressWarnings("unchecked")
    private ArrayList<Button> getSavedButtons() {
        return (ArrayList<Button>) JNode.get("@SAVED");
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
