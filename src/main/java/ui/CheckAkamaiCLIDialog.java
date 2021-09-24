package ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ResourceBundle;

public class CheckAkamaiCLIDialog extends DialogWrapper {

    private ResourceBundle resourceBundle;

    public CheckAkamaiCLIDialog() {
        super(true);
        resourceBundle =  ResourceBundle.getBundle("ActionBundle");
        setTitle(resourceBundle.getString("ui.checkAkamaiCliDialog.title"));
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());
        JBLabel label = new JBLabel(resourceBundle.getString("ui.checkAkamaiCliDialog.label"));
        JButton hyperlink = new JButton(resourceBundle.getString("ui.checkAkamaiCliDialog.button"));
        JBLabel configSettings = new JBLabel(resourceBundle.getString("ui.checkAkamaiCliDialog.edgeworkerConfigLabel"));
        hyperlink.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(resourceBundle.getString("ui.checkAkamaiCliDialog.hyperlink")));
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                } catch (URISyntaxException uriSyntaxException) {
                    uriSyntaxException.printStackTrace();
                }
            }
        });
        dialogPanel.add(label, BorderLayout.NORTH);
        dialogPanel.add(hyperlink, BorderLayout.CENTER);
        dialogPanel.add(configSettings, BorderLayout.SOUTH);
        return dialogPanel;
    }

}
