
import game_objs.Cell;
import game_objs.Context;
import jui_lib.*;
import jui_lib.bundles.AbstractValueSelector;
import jui_lib.bundles.CompositeValueSelector;
import processing.core.PApplet;

import java.io.File;
import java.util.ArrayList;

public class Main extends PApplet {
    private static int rows = 60;
    private static int cols = 80;
    private static String sketchRenderer;
    private static boolean smooth;
    private static boolean retina;

    private class CustomButton extends Button {
        private boolean isShowing;

        private CustomButton(float relativeW, float relativeH) {
            super(relativeW, relativeH);
        }

        @Override
        public CustomButton setVisible(boolean temp) {
            super.setVisible(temp && isShowing);
            return this;
        }

        @Override
        public boolean isVisible() {
            return isShowing && isVisible;
        }

        private CustomButton setIsShowing(boolean temp) {
            this.isShowing = temp;
            return this;
        }
    }

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

    private void initJNode() {
        JNode.init(this); // always to be used as the first line inside setup
        JNode.DISPLAY_CONTOUR = false;
        JNode.CONTOUR_THICKNESS = 0.5f;
        JNode.BACKGROUND_COLOR = color(235, 100);
        JNode.ROUNDED = false;
        JNode.ROUNDING = 20;
    }

    private void updateFilteredButtons(ArrayList<Button> allButtons, ArrayList<Button> filtered) {
        allButtons.forEach(button -> ((CustomButton) button)
                .setIsShowing(false)
                .setVisible(false));
        filtered.forEach(button -> ((CustomButton) button)
                .setIsShowing(true)
                .setVisible(true));
    }

    public void setup() {
        surface.setResizable(true);
        surface.setTitle("Game of Life");

        initJNode();

        //setting up models
        Label modelLabel = (Label) new Label().setAlign(CENTER).setContourVisible(false).setBackgroundColor(0, 0, 0, 25);


        VBox parent = new VBox();
        parent.matchWindowDimension(true);
        //parent.setMargins(0,0);
        JNode.add(parent);


        HBox upUiPanel = (HBox) new HBox(1.0f, 0).setCollapseInvisible(true);
        upUiPanel.attachMethod(() -> {
            if (mousePressed) return;
            if (mouseY < 5) {
                upUiPanel.setVisible(true);
                if (upUiPanel.getRelativeH() < 0.05f)
                    upUiPanel.setRelativeH(upUiPanel.getRelativeH() + 0.01f);
                Container.refresh();
            } else if (upUiPanel.isVisible() && mouseY > upUiPanel.y + upUiPanel.h + parent.spacing) {
                if (upUiPanel.getRelativeH() == 0) {
                    upUiPanel.setVisible(false);
                }
                if (upUiPanel.getRelativeH() > 0) {
                    upUiPanel.setRelativeH(upUiPanel.getRelativeH() - 0.01f);
                    if (upUiPanel.getRelativeH() < 0)
                        upUiPanel.setRelativeH(0);
                }
                Container.refresh();
            }
        }).setVisible(false);
        parent.add(upUiPanel);

        upUiPanel.add(new Label(0.1f, 1).setContent("Insert").inheritOutlook(modelLabel));

        TextInput configNameFilterer = new TextInput(0.1f, 1);
        upUiPanel.add(configNameFilterer);
        configNameFilterer.onKeyTyped(() -> {
            ArrayList<Button> configButtons = getConfigButtons();
            ArrayList<Button> filtered = filterButtons(configButtons, configNameFilterer.getContent(), 5);
            updateFilteredButtons(configButtons, filtered);
        });

        File[] listOfFiles = Context.listOfFiles("configs");
        for (int i = 0; i < listOfFiles.length; i++) {
            File file = listOfFiles[i];
            Context.importConfig(file);
            upUiPanel.add(new CustomButton(0.1f, 1)
                    .setIsShowing(i < 5)
                    .setContent(file.getName())
                    .onClick(() -> {
                        getContext().setCurrentConfig(file.getName());
                        Switch flip = ((Switch) JNode.get("@INSERT").get(0));
                        flip.setState(true).activate();
                    }).setVisible(true)
                    .setId("@CONFIG")
            );
        }

        upUiPanel.add(new SpaceHolder());

        Switch insert = (Switch) new Switch(0.1f, 1).setContentOff("Off").setContentOn("Activated")
                .setConditionOn(() -> getContext().getCurrentConfig() != null)
                .setState(false)
                .setId("@INSERT");
        insert.onClick(() -> getContext().setInsertingConfig(insert.isOn()));
        upUiPanel.add(insert);


        TextInput configFileName = new TextInput(0.1f, 1);
        configFileName.onSubmit(() -> {
            configFileName.setVisible(false);
            String name = configFileName.getContent();
            getContext().save(name, "configs");
            for (File file : Context.listOfFiles("configs"))
                if (file.getName().equals(name)) {
                    Context.importConfig(file);
                    break;
                }

            upUiPanel.add(2, new CustomButton(0.1f, 1)
                    .setIsShowing(true)
                    .setContent(name)
                    .onClick(() -> {
                        getContext().setCurrentConfig(name);
                        Switch flip = ((Switch) JNode.get("@INSERT").get(0));
                        flip.setState(true).activate();
                    }).setVisible(true)
                    .setId("@SAVED")
            );
            //TODO: consider overriding
        }).setVisible(false).setIgnoreWhenInvisible(true);
        upUiPanel.add(configFileName);

        Button saveConfig = new Button(0.05f, 1).setContent("Save");
        saveConfig.onClick(() -> configFileName.setVisible(true));
        upUiPanel.add(saveConfig);

        HBox std = new HBox();
        std.setCollapseInvisible(true).setMargins(0, 0);
        parent.add(std);

        VBox leftUiPanel = (VBox) new VBox(0, 1.0f).setCollapseInvisible(true);
        leftUiPanel.attachMethod(() -> {
            if (mousePressed) return;
            if (mouseX < 5) {
                leftUiPanel.setVisible(true);
                if (leftUiPanel.getRelativeW() < 0.1f)
                    leftUiPanel.setRelativeW(leftUiPanel.getRelativeW() + 0.01f);
                Container.refresh();
            } else if (leftUiPanel.isVisible() && mouseX > leftUiPanel.w + std.spacing) {
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
        std.add(leftUiPanel);

        leftUiPanel.add(new Label(1.0f, 0.05f).setContent("Load Saved").inheritOutlook(modelLabel));

        TextInput filterer = new TextInput(1.0f, 0.05f).setDefaultContent("");
        filterer.setIsFocusedOn(true).onKeyTyped(() -> {
            ArrayList<Button> savedButtons = getSavedButtons();
            ArrayList<Button> filtered = filterButtons(savedButtons, filterer.getContent(), 10);
            updateFilteredButtons(savedButtons, filtered);
        });
        leftUiPanel.add(filterer);

        int maxSavedNum = 0;
        File[] listOfFiles1 = Context.listOfFiles("saved");
        for (int i = 0; i < listOfFiles1.length; i++) {
            File file = listOfFiles1[i];
            if (!file.isFile()) continue;
            if (file.getName().startsWith("saved_game_")) {
                int num = Integer.valueOf(file.getName().substring(11));
                maxSavedNum = num > maxSavedNum ? num : maxSavedNum;
            }
            leftUiPanel.add(new CustomButton(1.0f, 0.05f)
                    .setIsShowing(i < 10)
                    .setContent(file.getName())
                    .onClick(() -> getContext().load(file))
                    .setVisible(true)
                    .setId("@SAVED")
            );
        }

        leftUiPanel.add(new SpaceHolder());
        leftUiPanel.add(new Label(1.0f, 0.05f).setContent("File Name").inheritOutlook(modelLabel));

        TextInput fileName = new TextInput(1.0f, 0.05f).setDefaultContent("saved_game_" + (maxSavedNum + 1));
        leftUiPanel.add(fileName.setId("@FILE_NAME"));

        Button save = new Button(1.0f, 0.05f).setContent("Save").onClick(() -> {
            getContext().save(fileName.getContent(), "saved");
            ArrayList<Button> savedButtons = this.getSavedButtons();
            boolean overridden = false;
            for (Button button : savedButtons)
                if (button.getContent().equals(fileName.getContent()))
                    overridden = true;

            if (overridden) return;
            for (File file : Context.listOfFiles("saved"))
                if (file.getName().equals(fileName.getContent()))
                    leftUiPanel.add(2, new CustomButton(1.0f, 0.05f)
                            .setIsShowing(true)
                            .setContent(fileName.getContent())
                            .onClick(() -> getContext().load(file))
                            .setVisible(true)
                            .setId("@SAVED")
                    );

            //if (fileName.getDefaultContent().equals(fileName.getContent())) return;
            int newFileIndex = parseInt(fileName.getDefaultContent().substring(11)) + 1;
            fileName.setDefaultContent("saved_game_" + newFileIndex);
        });
        leftUiPanel.add(save);


        Context gameContext = new Context("#CONTEXT", rows, cols);
        std.add(gameContext);

        VBox uiPanel = new VBox(0, 1.0f);
        uiPanel.setDebugEnabled(false);
        std.add(uiPanel);

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
        speed.link(() -> getContext().setMillisPerIteration(speed.getIntValue())).getTitleLabel().inheritOutlook(modelLabel);
        uiPanel.add(speed);

        AbstractValueSelector frameRate = new CompositeValueSelector(1.0f, 0.1f)
                .setTitlePercentage(0.3f)
                .roundTo(-1)
                .setTitle("fps")
                .setRange(60, 300)
                .setValue(60);
        frameRate.link(() -> frameRate(frameRate.getIntValue())).getTitleLabel().inheritOutlook(modelLabel);
        uiPanel.add(frameRate);

        uiPanel.add(new Label(1.0f, 0.05f).setContent("Motion").inheritOutlook(modelLabel));
        uiPanel.add(new Switch(1.0f, 0.05f).setContentOff("Off")
                .setContentOn("On")
                .onClick(() -> Cell.setHighlightingMotion(!Cell.isHighlightingMotion()))
                .setState(false));

        uiPanel.add(new SpaceHolder());

        AbstractValueSelector rows = new CompositeValueSelector(1.0f, 0.1f)
                .setTitlePercentage(0.3f)
                .roundTo(-1)
                .setTitle("rows")
                .setRange(10, 600)
                .setValue(Main.rows);
        rows.getTitleLabel().inheritOutlook(modelLabel);
        uiPanel.add(rows.link(() -> Main.rows = rows.getIntValue()));

        AbstractValueSelector cols = new CompositeValueSelector(1.0f, 0.1f)
                .setTitlePercentage(0.3f)
                .roundTo(-1)
                .setTitle("cols")
                .setRange(10, 800)
                .setValue(Main.cols);
        cols.getTitleLabel().inheritOutlook(modelLabel);
        uiPanel.add(cols.link(() -> Main.cols = cols.getIntValue()));

        uiPanel.add(new Button(1.0f, 0.05f)
                .setContent("Update")
                .onClick(() -> getContext().setDimension(Main.rows, Main.cols))).attachMethod(() -> {
            if (mousePressed) return;
            if (Math.abs(mouseX - width) < 5) {
                uiPanel.setVisible(true);
                if (uiPanel.getRelativeW() < 0.1f)
                    uiPanel.setRelativeW(uiPanel.getRelativeW() + 0.01f);
                //System.out.println(uiPanel.relativeW);
                Container.refresh();
            } else if (width - std.spacing * 2 - uiPanel.w > mouseX) {
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

        uiPanel.add(new SpaceHolder());

        uiPanel.add(new Button(1, 0.05f).setContent("Clear").onClick(getContext()::clear));

        uiPanel.add(new SpaceHolder());

        AbstractValueSelector chance = new CompositeValueSelector(1, 0.1f)
                .setRange(0, 1)
                .setTitlePercentage(0.3f)
                .setTitle("%alv.")
                .setValue(0.5f)
                .roundTo(1);
        chance.getTitleLabel().inheritOutlook(modelLabel);
        uiPanel.add(chance);

        uiPanel.add(new Button(1, 0.05f)
                .setContent("Spawn").onClick(() -> getContext().spawnRandom(chance.getFloatValue()))
        );

    }

    private ArrayList<Button> filterButtons(ArrayList<Button> candidates, String name, int num) {
        ArrayList<Button> filtered = new ArrayList<>();
        candidates.forEach(button -> {
            if (filtered.size() == num) return;
            if (button.getContent().startsWith(name))
                if (!filtered.contains(button))
                    filtered.add(button);
        });
        candidates.forEach(button -> {
            if (filtered.size() == num) return;
            if (button.getContent().contains(name))
                if (!filtered.contains(button))
                    filtered.add(button);
        });
        return filtered;
    }

    @SuppressWarnings("unchecked")
    private ArrayList<Button> getSavedButtons() {
        return (ArrayList<Button>) JNode.get("@SAVED");
    }

    @SuppressWarnings("unchecked")
    private ArrayList<Button> getConfigButtons() {
        return (ArrayList<Button>) JNode.get("@CONFIG");
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
