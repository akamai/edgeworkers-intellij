package ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.KeyStrokeAdapter;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;

public class EdgeWorkerIdInputDialog extends DialogWrapper {

    private JFormattedTextField inputField;

    public Long getEdgeWorkerId() throws Exception{
        if(null!=inputField){
            return (Long) inputField.getValue();
        }
        return null;
    }

    public EdgeWorkerIdInputDialog() {
        super(true);
        setTitle("Enter EdgeWorker Identifier");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new FlowLayout());
        JBLabel label = new JBLabel("EdgeWorker ID:");
        NumberFormat numberFormat = NumberFormat.getIntegerInstance();
        numberFormat.setParseIntegerOnly(true);
        inputField = new JFormattedTextField(numberFormat);
        inputField.requestFocusInWindow();
        inputField.addKeyListener(new KeyStrokeAdapter(){
            @Override
            public void keyPressed(KeyEvent event) {
                if (Character.isDigit(event.getKeyChar()) || event.getKeyChar()==KeyEvent.VK_BACK_SPACE || event.getKeyChar()==KeyEvent.VK_DELETE) {
                    inputField.setEditable(true);
                } else {
                    inputField.setEditable(false);
                }
            }
        });
        dialogPanel.add(label);
        dialogPanel.add(inputField);
        return dialogPanel;
    }
}
