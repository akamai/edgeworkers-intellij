package ui;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTextField;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Class that adds a "hint" text to a JBTextField which appears when no input has been entered
 */
public class JBHintTextField extends JBTextField implements FocusListener {

    private final String hint;
    private final Color defaultColor;
    private final Color hintColor;
    private boolean showingHint;

    /**
     * Create a new JBHintTextField
     *
     * @param hint Hint text to be shown when no input has been entered
     * @param hintColor Color of the hint text
     */
    public JBHintTextField(String hint, Color hintColor) {
        this.hint = hint;
        this.defaultColor = super.getForeground();
        this.hintColor = hintColor;
        super.addFocusListener(this);
        showHintText();
    }

    public JBHintTextField(String hint) {
        this(hint, JBColor.gray);
    }

    @Override
    public void focusGained(FocusEvent e) {
        if (showingHint) {
            hideHintText();
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        if (super.getText().isEmpty()) {
            showHintText();
        }
    }

    private void showHintText() {
        showingHint = true;
        super.setText(hint);
        super.setForeground(hintColor);
    }

    private void hideHintText() {
        showingHint = false;
        super.setText("");
        super.setForeground(defaultColor);
    }

    @Override
    public String getText() {
        return showingHint ? "" : super.getText();
    }

    @Override
    public void setText(String t) {
        if (t == null || t.isEmpty()){
            showHintText();
        } else {
            showingHint = false;
            super.setText(t);
            super.setForeground(defaultColor);
        }
    }

    public void resetText() {
        showHintText();
    }
}