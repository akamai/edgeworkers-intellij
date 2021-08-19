package ui;

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

    public ListEdgeWorkersToolWindow(){
        resourceBundle = ResourceBundle.getBundle("ActionBundle");
    }


    public JPanel getContent() throws Exception{
        SimpleToolWindowPanel panel = new SimpleToolWindowPanel(true, false);
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(resourceBundle.getString("listEdgeWorkersToolWindow.panel.title"));
        Tree tree = new Tree(rootNode);
        JScrollPane scrollPane = new JBScrollPane();
        panel.add(scrollPane, BorderLayout.CENTER);
        scrollPane.setViewportView(tree);
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        ActionManager actionManager = ActionManager.getInstance();
        actionManager.registerAction(resourceBundle.getString("action.listEdgeWorkers.id"), new ListEdgeWorkersAction(panel));
        actionGroup.addAction(actionManager.getAction(resourceBundle.getString("action.listEdgeWorkers.id")), Constraints.FIRST);
        ActionToolbar actionToolbar = actionManager.createActionToolbar("", actionGroup, true);
        actionToolbar.setTargetComponent(panel);
        panel.setToolbar(actionToolbar.getComponent());
        return panel;
    }
}
