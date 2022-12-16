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

    private String hint;
    private Color defaultColor;
    private Color hintColor;
    private boolean showingHint;


    public JBHintTextField(String hint, int columns) {
        super(columns);
        init(hint);
    }

    public JBHintTextField(String hint) {
        super();
        init(hint);
    }

    private void init(String hint) {
        this.hint = hint;
        this.defaultColor = super.getForeground();
        this.hintColor = JBColor.gray;
        super.addFocusListener(this);
        showHintText();
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
        if (t == null || t.isEmpty()) {
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