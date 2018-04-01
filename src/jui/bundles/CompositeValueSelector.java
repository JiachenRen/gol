package jui.bundles;

import jui.*;
import processing.core.PConstants;

/**
 * Created on April 23rd. Value selector bundle
 * Code refactored June 10th, created ADT AbstractValueSelector
 */
public class CompositeValueSelector extends AbstractValueSelector {
    public CompositeValueSelector(float relativeW, float relativeH) {
        super(relativeW, relativeH);
    }

    public CompositeValueSelector() {
        super();
    }

    public CompositeValueSelector(float x, float y, float w, float h) {
        super(x, y, w, h);
    }

    @Override
    public void initComponents() {
        this.setup();
        initUi();
        super.initComponents();
    }

    private void setup() {
        this.setMargins(0, 0);
        this.setAlignV(PConstants.DOWN);
        setTitlePercentage(0.35f);
    }

    private void initUi() {
        HBox titleWrapper = new HBox();
        titleWrapper.setId("titleWrapper");
        titleWrapper.setMargins(0, 0);
        this.add(titleWrapper);

        setTitleLabel(new Label(getTitlePercentage(), 1.0f).setContent("Var"));
        titleWrapper.add(getTitleLabel());

        setValueSlider((HSlider) new HSlider().setId("valueSlider"));
        this.add(getValueSlider());

        setTextInput(new TextInput());
        titleWrapper.add(getTextInput());
    }
}
