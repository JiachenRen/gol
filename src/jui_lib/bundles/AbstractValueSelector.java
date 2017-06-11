package jui_lib.bundles;

import jui_lib.*;
import processing.core.PConstants;

/**
 * Created by Jiachen on 6/10/17.
 * TODO: add setSuffix(), like km, mm, or $, %
 */
public abstract class AbstractValueSelector extends VBox {
    private float titlePercentage;
    private Label titleLabel;
    private TextInput textInput;
    private Slider valueSlider;
    private int roundIndex = 1;
    private Runnable linkedMethod;


    AbstractValueSelector(float relativeW, float relativeH) {
        super(relativeW, relativeH);
        initComponents();
    }

    AbstractValueSelector() {
        super();
        initComponents();
    }

    AbstractValueSelector(float x, float y, float w, float h) {
        super(x, y, w, h);
        initComponents();
    }

    public void initComponents() {
        valueSlider.setScalingFactor(0.5f)
        .setRollerShape(PConstants.RECT)
        .setRange(0, 1)
        .setValue(0.5f)
        .onFocus(() -> {
            String formatted = this.round(valueSlider.getFloatValue());
            textInput.setStaticContent(formatted);
            this.update();
        });

        textInput.onSubmit(() -> {
            if (textInput.getFloatValue() < valueSlider.valueLow) {
                textInput.setStaticContent(valueSlider.valueLow + "");
                valueSlider.setValue(valueSlider.valueLow);
                return;
            } else if (textInput.getFloatValue() > valueSlider.valueHigh) {
                textInput.setStaticContent(valueSlider.valueHigh + "");
                valueSlider.setValue(valueSlider.valueHigh);
                return;
            }
            textInput.setStaticContent(round(Float.valueOf(textInput.getStaticContent())));
            valueSlider.setValue(textInput.getFloatValue());
            this.update();
        }).setContent(round(0.5f));
    }

    /**
     * The linked method is invoked so that every time when the value is changed, the method
     * is updated accordingly. It also updates its own value.
     *
     * @since June 10th
     */
    private void update() {
        if (linkedMethod != null)
            linkedMethod.run();
        this.setValue(getFloatValue());
    }

    /**
     * @param val the value that is going to be applied to both the
     *            slider and the text input. If the value is less than
     *            lower bound or upper bound, then the value is default
     *            to the min/max value of the slider.
     */
    public AbstractValueSelector setValue(float val) {
        valueSlider.setValue(val);
        textInput.setStaticContent(round(val));
        return this;
    }

    public AbstractValueSelector setRange(float low, float high) {
        valueSlider.setRange(low, high);
        return this;
    }

    /**
     * @param val the float value to be rounded
     * @return string the formatted String to the proper decimal place
     */
    private String round(float val) {
        String str = Float.toString(val);
        if (str.contains(".")) {
            int index = str.indexOf(".") + 1;
            return str.substring(0, index + roundIndex);
        } else {
            return str;
        }
    }

    /**
     * @param digit round to number of digits after decimal point
     *              if digit is set to -1, then the decimal point
     *              is truncated. Else if it is set to other negative
     *              values then the number itself is truncated.
     */
    public AbstractValueSelector roundTo(int digit) {
        roundIndex = digit;
        return this;
    }

    /**
     * @return the calculated float value from the slider
     */
    public float getFloatValue() {
        return valueSlider.getFloatValue();
    }

    /**
     * @return the calculated int value from the slider
     */
    public int getIntValue() {
        return valueSlider.getIntValue();
    }

    /**
     * @param runnable a segment of code that applies the value of this
     *                 value selector to a given variable.
     */
    public AbstractValueSelector link(Runnable runnable) {
        linkedMethod = runnable;
        return this;
    }

    public TextInput getTextInput() {
        return textInput;
    }

    public Label getTitleLabel() {
        return titleLabel;
    }

    public AbstractValueSelector setTitleLabel(Label titleLabel) {
        this.titleLabel = titleLabel;
        return this;
    }

    public AbstractValueSelector setTextInput(TextInput textInput) {
        this.textInput = textInput;
        return this;
    }

    public float getTitlePercentage() {
        return titlePercentage;
    }

    public AbstractValueSelector setTitlePercentage(float titlePercentage) {
        this.titlePercentage = titlePercentage;
        return this;
    }

    public AbstractValueSelector setTitle(String title) {
        titleLabel.setContent(title);
        return this;
    }

    public AbstractValueSelector setValueSlider(Slider slider) {
        this.valueSlider = slider;
        return this;
    }

    public Slider getValueSlider() {
        return valueSlider;
    }
}
