package ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.net.InetAddress;

public class CodeProfilerAdvancedDialog extends DialogWrapper {
    private final String edgeIpInitialValue;
    private JBHintTextField edgeIp;

    /**
     * Create a new Code Profiler Advanced Dialog
     * @param edgeIP Initial Value for edgeIp field, or null if blank
     */
    public CodeProfilerAdvancedDialog(String edgeIP) {
        super(true);
        setTitle("Code Profiler Advanced Options");
        setOKButtonText("Save");

        this.edgeIpInitialValue = edgeIP;

        init();
    }

    public String getEdgeIp() {
        return edgeIp.getText();
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        String ip = getEdgeIp();
        if (ip.isEmpty()) {
            return null;
        } else {
            try {
                InetAddress.getByName(ip);
            } catch (Exception ex) {
                return new ValidationInfo("Invalid IP address", edgeIp);
            }
        }
        return null;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel();
        GroupLayout layout = new GroupLayout(dialogPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        // Labels
        JBLabel edgeIpLabel = new JBLabel("Edge IP Override:");

        // Fields
        edgeIp = new JBHintTextField("Enter edge server IP address");
        edgeIp.setText(edgeIpInitialValue);
        edgeIp.setMinimumSize(new Dimension(200, 30));
        edgeIp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        // Layout
        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(edgeIpLabel)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(edgeIp)
                )
        );

        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(edgeIpLabel)
                        .addComponent(edgeIp)
                )
        );

        dialogPanel.setLayout(layout);
        return dialogPanel;
    }
}
