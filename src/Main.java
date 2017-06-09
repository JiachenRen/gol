
import game_objs.Context;
import jui_lib.*;
import processing.core.PApplet;

public class Main extends PApplet {

    public static void main(String[] args) {
        String sketch = Thread.currentThread().getStackTrace()[1].getClassName();
        Thread proc = new Thread(() -> PApplet.main(sketch));
        proc.start();
    }

    public void settings() {
        size(800, 600, FX2D);
    }

    public void setup() {
        JNode.init(this); // always to be used as the first line inside setup

        HBox parent = new HBox();
        parent.matchWindowDimension(true);
        JNode.add(parent);

        Context gameContext = new Context(60, 80);
        parent.add(gameContext);

        VBox uiPanel = new VBox(0.2f, 1.0f);
        parent.add(uiPanel);

        Button step = new Button(1.0f, 0.05f)
                .setContent("Step")
                .onClick(gameContext::iterate);
        uiPanel.add(step);

        uiPanel.add(new SpaceHolder());

        Switch flowControl = (Switch) new Switch()
                .setContent("Iterating", "Paused")
                .setState(false)
                .onClick(gameContext::toggleAutoIteration)
                .inheritDisplayProperties(step);
        uiPanel.add(flowControl);


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
