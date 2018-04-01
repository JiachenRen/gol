package jui;

public class Switch extends Button {
    private String contentOff;
    private boolean isOn;
    private int backgroundColorOff;
    private int contourColorOff;
    private int textColorOff;
    private boolean pressedOnTarget;
    private String contentOn;
    private Condition metWhenOn;
    private Condition metWhenOff;

    public Switch(float x, float y, float w, float h) {
        super(x, y, w, h);
    }

    public Switch(float relativeW, float relativeH) {
        super(relativeW, relativeH);
    }

    public Switch() {
        super();
    }

    public interface Condition {
        boolean satisfied();
    }

    @Override
    public void init() {
        super.init();
        backgroundColorOff = backgroundColor;
        contourColorOff = contourColor;
        textColorOff = getTextColor();
        this.addEventListener("@RESERVED", Event.MOUSE_PRESSED, () -> pressedOnTarget = true);
        this.addEventListener("@RESERVED", Event.MOUSE_LEFT, () -> pressedOnTarget = false);
        this.addEventListener("@RESERVED_FLIP", Event.MOUSE_RELEASED, () -> {
            if (pressedOnTarget) setState(!isOn);
            pressedOnTarget = false;
        });
    }

    public Switch setContentOff(String contentOff) {
        this.contentOff = contentOff;
        super.setContent(contentOff);
        return this;
    }

    public boolean isOn() {
        return isOn;
    }

    public Switch setBackgroundColorOff(int color) {
        this.backgroundColorOff = color;
        return this;
    }

    public Switch setContourColorOff(int color) {
        this.contourColorOff = color;
        return this;
    }

    public Switch setTextColorOff(int color) {
        this.textColorOff = color;
        return this;
    }

    @SuppressWarnings("deprecation")
    private Switch updateState() {
        setContent(isOn ? contentOn : contentOff);
        setBackgroundColor(isOn ? backgroundColor : backgroundColorOff);
        setTextColor(isOn ? getTextColor() : textColorOff);
        setContourColor(isOn ? contourColor : contourColorOff);
        return this;
    }

    public Switch setState(boolean isOn) {
        if (isOn && metWhenOn != null && !metWhenOn.satisfied())
            return this;
        else if (!isOn && metWhenOff != null && !metWhenOff.satisfied())
            return this;
        this.isOn = isOn;
        updateState();
        return this;
    }

    public Switch setContentOn(String s) {
        super.setContent(s);
        this.contentOn = s;
        return this;
    }

    public Switch setContent(String contentOn, String contentOff) {
        setContentOn(contentOn);
        setContentOff(contentOff);
        return this;
    }

    @Deprecated
    public Switch setContent(String s) {
        super.setContent(s);
        return this;
    }

    @Override
    public Switch onClick(Runnable runnable) {
        super.onClick(runnable);
        return this;
    }

    public Switch setConditionOn(Condition conditionOn) {
        this.metWhenOn = conditionOn;
        return this;
    }

    public Switch setConditionOff(Condition conditionOff) {
        this.metWhenOn = conditionOff;
        return this;
    }

    public Switch manualSwitch() {
        pressedOnTarget = true;
        this.getEventListener("@RESERVED_FLIP").invoke();
        pressedOnTarget = false;
        return this;
    }
}
