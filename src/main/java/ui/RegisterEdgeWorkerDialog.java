package ui;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.Nullable;
import utils.EdgeworkerWrapper;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Map;

public class RegisterEdgeWorkerDialog extends DialogWrapper {

    private EdgeworkerWrapper edgeworkerWrapper;
    private ComboBox groupListDropdown;
    private ComboBox resourceTierDropdown;
    private JBTextField edgeWorkerNameValue;

    public RegisterEdgeWorkerDialog() {
        super(true);
        setTitle("Register EdgeWorker");
        init();
        setOKButtonText("Register");
    }

    public String getSelectedGroupId() throws Exception{
        if(null!=groupListDropdown && null!=groupListDropdown.getItem()){
            String group = (String) groupListDropdown.getItem();
            return group.split("-")[0].trim();
        }
        return null;
    }

    public String getEdgeWorkerName() {
        if(null!=edgeWorkerNameValue){
            return edgeWorkerNameValue.getText();
        }
        return null;
    }

    public Integer getSelectedResourceTierId() throws Exception{
        if(null!=resourceTierDropdown){
            String resourceTierId = (String) resourceTierDropdown.getItem();
            if(resourceTierId.equals("Basic Compute")){
                return 100;
            }else if(resourceTierId.equals("Dynamic Compute")){
                return 200;
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
        JBLabel ewNameLabel = new JBLabel("EdgeWorker Name:");
        JBLabel groupLabel = new JBLabel("Group:");
        JBLabel resourceTierLabel = new JBLabel("Resource Tier:");
        edgeworkerWrapper = new EdgeworkerWrapper();
        groupListDropdown = new ComboBox();
        resourceTierDropdown = new ComboBox();
        resourceTierDropdown.addItem("Basic Compute");
        resourceTierDropdown.addItem("Dynamic Compute");
        edgeWorkerNameValue = new JBTextField();
        ProgressManager.getInstance()
                .runProcessWithProgressSynchronously(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ArrayList<Map<String, String>> groupsList = edgeworkerWrapper.getGroupsList();
                            for(Map<String, String> map: groupsList){
                                groupListDropdown.addItem(map.get("groupId")+" - "+map.get("groupName"));
                            }
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }
                },"Loading...", false, null);

        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(ewNameLabel)
                        .addComponent(groupLabel)
                        .addComponent(resourceTierLabel)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(edgeWorkerNameValue)
                        .addComponent(groupListDropdown)
                        .addComponent(resourceTierDropdown)
                )
        );

        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(ewNameLabel)
                        .addComponent(edgeWorkerNameValue)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(groupLabel)
                        .addComponent(groupListDropdown)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(resourceTierLabel)
                        .addComponent(resourceTierDropdown)
                )
        );

        dialogPanel.setLayout(layout);
        return dialogPanel;
    }
}
