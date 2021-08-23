package actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;
import utils.EdgeworkerWrapper;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.ExpandVetoException;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;

public class ListEdgeWorkersAction extends AnAction {

    private SimpleToolWindowPanel panel;
    private ResourceBundle resourceBundle;
    private boolean loading;

    public ListEdgeWorkersAction(SimpleToolWindowPanel panel) {
        super();
        this.panel = panel;
        this.loading=false;
        resourceBundle = ResourceBundle.getBundle("ActionBundle");

    }

    public void update(@NotNull AnActionEvent e) {
        final Presentation presentation = e.getPresentation();
        final Icon icon = loading ? new AnimatedIcon.Default() : AllIcons.Actions.Refresh;
        presentation.setIcon(icon);
        presentation.setEnabled(!loading);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e){
        loading=true;
        update(e);
        ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
            @Override
            public void run() {
                DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(resourceBundle.getString("listEdgeWorkersToolWindow.panel.title"));
                Tree tree = new Tree(rootNode);
                EdgeworkerWrapper edgeworkerWrapper = new EdgeworkerWrapper();
                ArrayList<Map<String, String>> edgeWorkersList = null;
                try {
                    edgeWorkersList = edgeworkerWrapper.getEdgeWorkersIdsList();
                    for(Map<String, String> map: edgeWorkersList){
                        DefaultMutableTreeNode node = new DefaultMutableTreeNode(map.get("edgeWorkerId")+" - "+map.get("name"));
                        node.add(new DefaultMutableTreeNode("Versions"));
                        rootNode.add(node);
                    }

                    addTreeSpeedSearch(tree);
                    addColoredTreeCellRenderer(tree);
                    addTreeWillExpandListener(tree, edgeworkerWrapper, e.getProject());

                    //invokeLater UI update related code
                    ApplicationManager.getApplication().invokeLater(() -> {
                        JScrollPane scrollPane = new JBScrollPane();
                        for(Component c: panel.getComponents()){
                            if(c instanceof JBScrollPane){
                                //remove old scrollpane from the panel
                                panel.remove(c);
                            }
                        }
                        panel.add(scrollPane, BorderLayout.CENTER);
                        panel.revalidate();
                        panel.repaint();
                        scrollPane.setViewportView(tree);
                        loading = false;
                    });
                } catch (Exception exception) {
                    exception.printStackTrace();
                    loading=false;
                }
            }
        }, "Refreshing...", false, e.getProject());
    }

    private void addTreeSpeedSearch(Tree tree){
        TreeSpeedSearch treeSpeedSearch = new TreeSpeedSearch(tree);
    }

    private void addColoredTreeCellRenderer(Tree tree){
        ColoredTreeCellRenderer coloredTreeCellRenderer = new ColoredTreeCellRenderer() {
            @Override
            public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                append(value + "");
            }
        };
        tree.setCellRenderer(coloredTreeCellRenderer);
    }

    private void addTreeWillExpandListener(Tree tree, EdgeworkerWrapper edgeworkerWrapper, Project project){
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) {
                ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
                    @Override
                    public void run() {
                        if(null != event.getPath().getParentPath() && event.getPath().getParentPath().toString().equals("["+resourceBundle.getString("listEdgeWorkersToolWindow.panel.title")+"]")){
                            String eid = event.getPath().getLastPathComponent().toString().split("-")[0].trim();
                            try{
                                ArrayList<Map<String, String>> maps = edgeworkerWrapper.getEdgeWorkerVersionsList(eid);
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)event.getPath().getLastPathComponent();
                                    node.removeAllChildren();
                                    for(Map<String, String> map: maps){
                                        node.add(new DefaultMutableTreeNode(map.get("version")));
                                    }
                                });
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }}, "Loading...", false, project);
            }
            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
            }
        });
    }
}
