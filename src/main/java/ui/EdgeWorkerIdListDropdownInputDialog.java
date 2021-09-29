package ui;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.Nullable;
import utils.EdgeworkerWrapper;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Map;

public class EdgeWorkerIdListDropdownInputDialog extends DialogWrapper{

    private ComboBox comboBox;

    public String getSelectedItem() throws Exception{
        if(null!=comboBox && null!=comboBox.getItem()){
            String edgeWorker = (String) comboBox.getItem();
            return edgeWorker.split("-")[0].trim();
        }
        return null;
    }

    public EdgeWorkerIdListDropdownInputDialog() {
        super(true);
        setTitle("Select EdgeWorker Identifier");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel();
        GroupLayout layout = new GroupLayout(dialogPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        JBLabel label = new JBLabel("EdgeWorker ID:");
        comboBox = new ComboBox();
        ProgressManager.getInstance()
                .runProcessWithProgressSynchronously(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            EdgeworkerWrapper edgeworkerWrapper = new EdgeworkerWrapper();
                            ArrayList<Map<String, String>> edgeWorkersIdsList = edgeworkerWrapper.getEdgeWorkersIdsList();
                            for(Map<String, String> map: edgeWorkersIdsList){
                                comboBox.addItem(map.get("edgeWorkerId")+" - "+map.get("name"));
                            }
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }
                },"Loading...", false, null);

        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(label)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(comboBox)
                )
        );

        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(label)
                        .addComponent(comboBox)
                )
        );
        dialogPanel.setLayout(layout);
        return dialogPanel;
    }
}
