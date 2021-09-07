package ui;

import actions.DownloadEdgeWorkerAction;
import actions.ListEdgeWorkersAction;
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
        actionManager.registerAction(resourceBundle.getString("action.downloadEdgeWorker.id"), new DownloadEdgeWorkerAction(resourceBundle.getString("action.downloadEdgeWorker.title"), resourceBundle.getString("action.downloadEdgeWorker.desc"), null));
        actionManager.registerAction(resourceBundle.getString("action.listEdgeWorkers.id"), new ListEdgeWorkersAction(panel));
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
        ActionToolbar actionToolbar = actionManager.createActionToolbar("", actionGroup, true);
        actionToolbar.setTargetComponent(panel);
        panel.setToolbar(actionToolbar.getComponent());
        return panel;
    }
}
