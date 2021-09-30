package ui;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.Nullable;
import utils.EdgeworkerWrapper;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Map;

public class ActivateEdgeWorkerDialog extends DialogWrapper {

    private ComboBox edgeWorkersListDropdown;
    private ComboBox edgeWorkerVersionsDropdown;
    private ComboBox networkDropdown;
    private String edgeWorkerId;
    private String edgeWorkerVersion;

    private EdgeworkerWrapper edgeworkerWrapper;

    public String getSelectedEdgeWorkerID() throws Exception{
        if(null!=edgeWorkersListDropdown && null!=edgeWorkersListDropdown.getItem()){
            String edgeWorker = (String) edgeWorkersListDropdown.getItem();
            return edgeWorker.split("-")[0].trim();
        }
        return null;
    }

    public void setEdgeWorkersIDInDropdown(String eid) {
        if(null!=this.edgeWorkersListDropdown) {
            this.edgeWorkersListDropdown.setItem(eid);
        }
    }

    public String getSelectedEdgeWorkerVersion() throws Exception{
        if(null!=edgeWorkerVersionsDropdown){
            String edgeWorkerVersion = (String) edgeWorkerVersionsDropdown.getItem();
            return edgeWorkerVersion;
        }
        return null;
    }

    public void setEdgeWorkerVersionInDropdown(String version){
        if(null!=this.edgeWorkerVersionsDropdown) {
            this.edgeWorkerVersionsDropdown.setItem(version);
        }
    }

    public String getSelectedNetwork() throws Exception{
        if(null!=networkDropdown){
            String network = (String) networkDropdown.getItem();
            return network;
        }
        return null;
    }

    public ActivateEdgeWorkerDialog(@Nullable Project project, String edgeWorkerId, String edgeWorkerVersion) {
        super(project);
        this.edgeWorkerId = edgeWorkerId;
        this.edgeWorkerVersion = edgeWorkerVersion;
        setTitle("Activate EdgeWorker");
        init();
        setOKButtonText("Activate");
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel();
        GroupLayout layout = new GroupLayout(dialogPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        JBLabel ewIDLabel = new JBLabel("EdgeWorker ID:");
        JBLabel ewVersionLabel = new JBLabel("EdgeWorker Version:");
        JBLabel networkLabel = new JBLabel("Network:");
        JBLabel stagingActiveVersionLabel = new JBLabel("Version Active on Staging:");
        JBLabel prodActiveVersionLabel = new JBLabel("Version Active on Production:");
        JBLabel stagingActiveVersionValue = new JBLabel("");
        JBLabel prodActiveVersionValue = new JBLabel("");
        edgeworkerWrapper = new EdgeworkerWrapper();
        edgeWorkersListDropdown = new ComboBox();
        edgeWorkerVersionsDropdown = new ComboBox();
        networkDropdown = new ComboBox();
        networkDropdown.addItem("Staging");
        networkDropdown.addItem("Production");
        ProgressManager.getInstance()
                .runProcessWithProgressSynchronously(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ArrayList<Map<String, String>> edgeWorkersIdsList = edgeworkerWrapper.getEdgeWorkersIdsList();
                            for(Map<String, String> map: edgeWorkersIdsList){
                                edgeWorkersListDropdown.addItem(map.get("edgeWorkerId")+" - "+map.get("name"));
                            }

                            edgeWorkersListDropdown.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    ProgressManager.getInstance()
                                            .runProcessWithProgressSynchronously(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        String eid = getSelectedEdgeWorkerID();
                                                        if(null!=eid){
                                                            fillEdgeWorkerVersionsDropdown(eid);
                                                            fillActiveEdgeWorkerVersionOnStagingAndProd(eid, stagingActiveVersionValue, prodActiveVersionValue);
                                                        }
                                                    } catch (Exception exception) {
                                                        exception.printStackTrace();
                                                    }
                                                }
                                            },"Loading...", false, null);
                                }
                            });

                            if(null!=edgeWorkerId && null!=edgeWorkerVersion){
                                setEdgeWorkersIDInDropdown(edgeWorkerId);
                                setEdgeWorkerVersionInDropdown(edgeWorkerVersion);
                            }else{
                                String eid = getSelectedEdgeWorkerID();
                                if(null!=eid){
                                    fillEdgeWorkerVersionsDropdown(eid);
                                    fillActiveEdgeWorkerVersionOnStagingAndProd(eid, stagingActiveVersionValue, prodActiveVersionValue);
                                }
                            }
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }
                },"Loading...", false, null);

        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(stagingActiveVersionLabel)
                        .addComponent(prodActiveVersionLabel)
                        .addComponent(ewIDLabel)
                        .addComponent(ewVersionLabel)
                        .addComponent(networkLabel)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(stagingActiveVersionValue)
                        .addComponent(prodActiveVersionValue)
                        .addComponent(edgeWorkersListDropdown)
                        .addComponent(edgeWorkerVersionsDropdown)
                        .addComponent(networkDropdown)
                )
        );

        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(stagingActiveVersionLabel)
                        .addComponent(stagingActiveVersionValue)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(prodActiveVersionLabel)
                        .addComponent(prodActiveVersionValue)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(ewIDLabel)
                        .addComponent(edgeWorkersListDropdown)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(ewVersionLabel)
                        .addComponent(edgeWorkerVersionsDropdown)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(networkLabel)
                        .addComponent(networkDropdown)
                )
        );

        dialogPanel.setLayout(layout);
        return dialogPanel;
    }

    public void fillEdgeWorkerVersionsDropdown(String eid) throws Exception {
        edgeWorkerVersionsDropdown.removeAllItems();
        ArrayList<Map<String, String>> edgeWorkersIdsList = edgeworkerWrapper.getEdgeWorkerVersionsList(eid);
        for(Map<String, String> map: edgeWorkersIdsList){
            edgeWorkerVersionsDropdown.addItem(map.get("version"));
        }
    }

    public void fillActiveEdgeWorkerVersionOnStagingAndProd(String eid, JBLabel stagingActiveVersionValue, JBLabel prodActiveVersionValue) throws Exception {
        ArrayList<Map<String, String>> activeVersion = edgeworkerWrapper.getActiveEdgeWorkerVersionsOnStagingAndProd(eid);
        System.out.println(activeVersion);
        stagingActiveVersionValue.setText("");
        prodActiveVersionValue.setText("");
        for(Map<String, String> map: activeVersion){
            if(map.get("network").equals("STAGING")){
                stagingActiveVersionValue.setText(map.get("version"));
            }
            if(map.get("network").equals("PRODUCTION")){
                prodActiveVersionValue.setText(map.get("version"));
            }
        }
    }
}
