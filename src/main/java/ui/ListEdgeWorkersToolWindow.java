package ui;

import actions.ActivateEdgeWorkerAction;
import actions.DownloadEdgeWorkerAction;
import actions.ListEdgeWorkersAction;
import actions.UploadEdgeWorkerAction;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.ResourceBundle;

public class ListEdgeWorkersToolWindow {

    private ResourceBundle resourceBundle;
    private SimpleToolWindowPanel panel;

    public ListEdgeWorkersToolWindow(){
        resourceBundle = ResourceBundle.getBundle("ActionBundle");
        panel = new SimpleToolWindowPanel(true, false);
        ActionManager actionManager = ActionManager.getInstance();
        if(null != actionManager.getAction(resourceBundle.getString("action.listEdgeWorkers.id"))){
            actionManager.unregisterAction(resourceBundle.getString("action.listEdgeWorkers.id"));
        }
        if(null != actionManager.getAction(resourceBundle.getString("action.downloadEdgeWorker.id"))){
            actionManager.unregisterAction(resourceBundle.getString("action.downloadEdgeWorker.id"));
        }
        if(null != actionManager.getAction(resourceBundle.getString("action.uploadEdgeWorker.id"))){
            actionManager.unregisterAction(resourceBundle.getString("action.uploadEdgeWorker.id"));
        }
        if(null != actionManager.getAction(resourceBundle.getString("action.activateEdgeWorker.id"))){
            actionManager.unregisterAction(resourceBundle.getString("action.activateEdgeWorker.id"));
        }
        //register download and activate EdgeWorker actions before registering listEdgeWorker action
        actionManager.registerAction(resourceBundle.getString("action.downloadEdgeWorker.id"), new DownloadEdgeWorkerAction(resourceBundle.getString("action.downloadEdgeWorker.title"), resourceBundle.getString("action.downloadEdgeWorker.desc"), AllIcons.Actions.Download));
        actionManager.registerAction(resourceBundle.getString("action.activateEdgeWorker.id"), new ActivateEdgeWorkerAction(resourceBundle.getString("action.activateEdgeWorker.title"), resourceBundle.getString("action.activateEdgeWorker.desc"), AllIcons.Actions.Install));
        actionManager.registerAction(resourceBundle.getString("action.listEdgeWorkers.id"), new ListEdgeWorkersAction(panel));
        actionManager.registerAction(resourceBundle.getString("action.uploadEdgeWorker.id"), new UploadEdgeWorkerAction(resourceBundle.getString("action.uploadEdgeWorker.title"), resourceBundle.getString("action.uploadEdgeWorker.desc"), AllIcons.Actions.Upload));
    }

    public JPanel getContent() throws Exception{
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(resourceBundle.getString("listEdgeWorkersToolWindow.panel.title"));
        JScrollPane scrollPane = new JBScrollPane();
        panel.removeAll();
        panel.add(scrollPane, BorderLayout.CENTER);
        scrollPane.setViewportView(new Tree(rootNode));
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        ActionManager actionManager = ActionManager.getInstance();
        actionGroup.addAction(actionManager.getAction(resourceBundle.getString("action.listEdgeWorkers.id")), Constraints.FIRST);
        actionGroup.addAction(actionManager.getAction(resourceBundle.getString("action.uploadEdgeWorker.id")), Constraints.LAST);
        actionGroup.addAction(actionManager.getAction(resourceBundle.getString("action.activateEdgeWorker.id")), Constraints.LAST);
        ActionToolbar actionToolbar = actionManager.createActionToolbar("", actionGroup, true);
        actionToolbar.setTargetComponent(panel);
        panel.setToolbar(actionToolbar.getComponent());
        return panel;
    }
}
