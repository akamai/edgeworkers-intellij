package config;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;
import java.util.ResourceBundle;


public class EdgeWorkersConfiguration implements Configurable, Configurable.NoScroll {

    private JPanel configPanel;
    private TextFieldWithBrowseButton edgercFilePath;
    private JBTextField edgercSection;
    private JBTextField accountKey;
    private JLabel edgercSectionLabel;
    private JLabel accountKeyLabel;
    private JLabel edgercPathLabel;
    private JLabel configPanelDesc;
    private ResourceBundle resourceBundle;

    public EdgeWorkersConfiguration(){
        resourceBundle = ResourceBundle.getBundle("ActionBundle");
        FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor();
        fileChooserDescriptor.setTitle(resourceBundle.getString("config.edgerc.filechooser.title"));
        fileChooserDescriptor.setDescription(resourceBundle.getString("config.edgerc.filechooser.desc"));
        fileChooserDescriptor.setShowFileSystemRoots(true);
        fileChooserDescriptor.setForcedToUseIdeaFileChooser(true);
        edgercFilePath.addBrowseFolderListener(new TextBrowseFolderListener(fileChooserDescriptor));
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return resourceBundle.getString("config.displayName");
    }

    @Override
    public String getHelpTopic() {
        return resourceBundle.getString("config.helpTopic");
    }

    @Override
    public @Nullable JComponent createComponent() {
        return configPanel;
    }

    @Override
    public boolean isModified() {
        EdgeWorkersConfig edgeWorkersConfig = SettingsService.getInstance().getState();
        if(edgeWorkersConfig.getEdgercFilePath().equals(edgercFilePath.getText()) &&
           edgeWorkersConfig.getEdgercSectionName().equals(edgercSection.getText()) &&
           edgeWorkersConfig.getAccountKey().equals(accountKey.getText())){
            return false;
        }
        return true;
    }

    @Override
    public void apply() throws ConfigurationException {
        SettingsService settingsService = SettingsService.getInstance();
        EdgeWorkersConfig edgeWorkersConfig = new EdgeWorkersConfig();
        edgeWorkersConfig.setAccountKey(accountKey.getText());
        edgeWorkersConfig.setEdgercSectionName(edgercSection.getText());
        edgeWorkersConfig.setEdgercFilePath(edgercFilePath.getText());
        settingsService.updateConfig(edgeWorkersConfig);
    }

    @Override
    public void reset() {
        SettingsService settingsService = SettingsService.getInstance();
        EdgeWorkersConfig edgeWorkersConfig = settingsService.getState();
        accountKey.setText(edgeWorkersConfig.getAccountKey());
        edgercSection.setText(edgeWorkersConfig.getEdgercSectionName());
        edgercFilePath.setText(edgeWorkersConfig.getEdgercFilePath());
    }

}
