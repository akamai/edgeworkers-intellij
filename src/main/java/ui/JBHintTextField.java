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


    /**
     * Create a new hint text field
     *
     * @param hint    Hint to be shown when no text has been entered
     * @param columns Width of the text field
     */
    public JBHintTextField(String hint, int columns) {
        super(columns);
        init(hint);
    }

    /**
     * Create a new hint text field
     *
     * @param hint Hint to be shown when no text has been entered
     */
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

    /**
     * Show the hint text
     */
    private void showHintText() {
        showingHint = true;
        super.setText(hint);
        super.setForeground(hintColor);
    }

    /**
     * Hide the hint text and show an empty text field instead
     */
    private void hideHintText() {
        showingHint = false;
        super.setText("");
        super.setForeground(defaultColor);
    }

    /**
     * Hide the hint text and show a text field instead
     *
     * @param t The new text to be shown
     */
    private void hideHintText(String t) {
        showingHint = false;
        super.setText(t);
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
            hideHintText(t);
        }
    }

    /**
     * Reset the text field and display the hint text
     */
    public void reset() {
        showHintText();
    }
}