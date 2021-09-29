package actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;
import ui.ActivateEdgeWorkerDialog;
import utils.EdgeworkerWrapper;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.ExpandVetoException;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;

public class ListEdgeWorkersAction extends AnAction {

    private SimpleToolWindowPanel panel;
    private ResourceBundle resourceBundle;
    private boolean loading;
    DefaultActionGroup versionsListActionGroup;

    public ListEdgeWorkersAction(SimpleToolWindowPanel panel) {
        super();
        this.panel = panel;
        this.loading=false;
        resourceBundle = ResourceBundle.getBundle("ActionBundle");
        ActionManager actionManager = ActionManager.getInstance();
        versionsListActionGroup = new DefaultActionGroup();
        if(null != actionManager.getAction(resourceBundle.getString("action.downloadEdgeWorker.id"))){
            versionsListActionGroup.addAction(actionManager.getAction(resourceBundle.getString("action.downloadEdgeWorker.id")), Constraints.FIRST);
        }
        if(null != actionManager.getAction(resourceBundle.getString("action.activateEdgeWorker.id"))){
            versionsListActionGroup.addAction(actionManager.getAction(resourceBundle.getString("action.activateEdgeWorker.id")), Constraints.LAST);
        }
    }

    public void update(@NotNull AnActionEvent e) {
        final Presentation presentation = e.getPresentation();
        final Icon icon = loading ? new AnimatedIcon.Default() : AllIcons.Actions.Refresh;
        presentation.setIcon(icon);
        presentation.setEnabled(!loading);
    }

    public void addRightClickMouseEventListener(Tree tree, AnActionEvent e){
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (SwingUtilities.isRightMouseButton(mouseEvent) && null!= tree.getSelectionModel() && tree.getSelectionModel().getSelectionPath().getPath().length==3) {
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            String edgeWorkerId = tree.getSelectionModel().getSelectionPath().getPath()[1].toString().split("-")[0].strip();
                            String edgeWorkerVersion = tree.getSelectionModel().getSelectionPath().getPath()[2].toString();

                            DownloadEdgeWorkerAction downloadEdgeWorkerAction = (DownloadEdgeWorkerAction) ActionManager.getInstance().getAction(resourceBundle.getString("action.downloadEdgeWorker.id"));
                            downloadEdgeWorkerAction.setEdgeWorkerId(edgeWorkerId);
                            downloadEdgeWorkerAction.setEdgeWorkerVersion(edgeWorkerVersion);

                            ActivateEdgeWorkerAction activateEdgeWorkerAction = (ActivateEdgeWorkerAction) ActionManager.getInstance().getAction(resourceBundle.getString("action.activateEdgeWorker.id"));
                            ActivateEdgeWorkerDialog dialog = new ActivateEdgeWorkerDialog();
                            //[edgeWorkerId - edgeWorkerName] value should be same in tree nodes and dropdown list
                            dialog.setEdgeWorkersIDInDropdown(tree.getSelectionModel().getSelectionPath().getPath()[1].toString());
                            dialog.setEdgeWorkerVersionInDropdown(edgeWorkerVersion);
                            activateEdgeWorkerAction.setDialog(dialog);
                        }
                    });
                    ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                            resourceBundle.getString("listEdgeWorkersToolWindow.listPopup.title"),
                            versionsListActionGroup,
                            e.getDataContext(), JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
                    popup.setMinimumSize(new Dimension(240, -1));
                    popup.showInScreenCoordinates(mouseEvent.getComponent(), mouseEvent.getLocationOnScreen());
                }
            }
        });
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e){
        if(new EdgeworkerWrapper().checkIfAkamaiCliInstalled()==false){
            return;
        }
        loading=true;
        update(e);
        ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
            @Override
            public void run() {
                DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(resourceBundle.getString("listEdgeWorkersToolWindow.panel.title"));
                Tree tree = new Tree(rootNode);
                addRightClickMouseEventListener(tree, e);
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
                    addTreeWillExpandListener(tree, edgeworkerWrapper, e);
                    addTreeSelectionListener(tree, edgeworkerWrapper, e);

                    //wrap UI update related code inside invokeAndWait
                    ApplicationManager.getApplication().invokeAndWait(() -> {
                        JScrollPane scrollPane = new JBScrollPane();
                        for(Component c: panel.getComponents()){
                            if(c instanceof JBScrollPane){
                                //remove old scrollPane from the panel
                                panel.remove(c);
                            }
                        }
                        panel.add(scrollPane, BorderLayout.CENTER);
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

    private void addTreeWillExpandListener(Tree tree, EdgeworkerWrapper edgeworkerWrapper, AnActionEvent actionEvent){
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
//                                ApplicationManager.getApplication().invokeLater(() -> {
                                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)event.getPath().getLastPathComponent();
                                    node.removeAllChildren();
                                    for(Map<String, String> map: maps){
                                        node.add(new DefaultMutableTreeNode(map.get("version")));
                                    }
//                                });
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }}, "Loading...", false, actionEvent.getProject());
            }
            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                if(event.getPath().getPathCount()==3){
                    //EdgeWorker version node collapsed
                    String eid = event.getPath().getPath()[1].toString().split("-")[0].strip().trim();
                    String version = event.getPath().getPath()[2].toString().trim();
                    File directory = new File(FileUtil.getTempDirectory()+"/tempEdgeWorkersDownload_"+eid+"_"+version);
                    //delete all edgeWorker files present inside tempEdgeWorkersDownload_ folder when version treeNode collapse
                    if(directory.exists()){
                        for(File file: directory.listFiles()){
                            file.delete();
                        }
                        directory.delete();
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode)event.getPath().getLastPathComponent();
                        node.removeAllChildren();
                    }
                }
            }
        });
    }

    private void addTreeSelectionListener(Tree tree, EdgeworkerWrapper edgeworkerWrapper, AnActionEvent eve){
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                if(null!= e.getNewLeadSelectionPath()){
                    if(e.getNewLeadSelectionPath().getPathCount()==3){
                        //EdgeWorker version node selected
                        String eid = e.getNewLeadSelectionPath().getPath()[1].toString().split("-")[0].strip();
                        String version = e.getNewLeadSelectionPath().getPath()[2].toString();
                        DefaultMutableTreeNode versionNode = (DefaultMutableTreeNode) e.getNewLeadSelectionPath().getPath()[2];
                        ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    File tempDirectory = new File(FileUtil.getTempDirectory()+"/tempEdgeWorkersDownload_"+eid+"_"+version);
                                    //delete all edgeWorker files inside tempEdgeWorkersDownload_ directory and download them again
                                    if(tempDirectory.exists()){
                                        for(File file: tempDirectory.listFiles()){
                                            file.delete();
                                        }
                                    }else{
                                        tempDirectory = FileUtil.createTempDirectory("tempEdgeWorkersDownload_"+eid+"_"+version, "", true);
                                    }
                                    versionNode.removeAllChildren();
                                    Integer exitCode = edgeworkerWrapper.downloadEdgeWorker(eid, version, tempDirectory.getCanonicalPath());
                                    if(null == exitCode || !exitCode.equals(0)){
                                        System.out.println("Error in downloading EdgeWorker.");
                                    }
                                    if (tempDirectory.listFiles().length == 1) {
                                        String tgzFileName = tempDirectory.listFiles()[0].getName();
                                        edgeworkerWrapper.extractTgzFile(tempDirectory.listFiles()[0].getCanonicalPath(), tempDirectory.getCanonicalPath());
//                                           VfsUtil.markDirtyAndRefresh(false, false, true, tempFile);
                                        for (File file : tempDirectory.listFiles()) {
                                            if (!file.getName().equals(tgzFileName)) {
                                                versionNode.add(new DefaultMutableTreeNode(file.getName()));
                                            }
                                        }
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        },"Loading...", false, eve.getProject());

                    }else if(e.getNewLeadSelectionPath().getPathCount()==4){
                        String eid = e.getNewLeadSelectionPath().getPath()[1].toString().split("-")[0].strip().trim();
                        String version = e.getNewLeadSelectionPath().getPath()[2].toString().trim();
                        String fileName = e.getNewLeadSelectionPath().getPath()[3].toString().trim();
                        File directory = new File(FileUtil.getTempDirectory()+"/tempEdgeWorkersDownload_"+eid+"_"+version);
                        if(directory.exists()){
                            for(File file: directory.listFiles()){
                                if(file.getName().equals(fileName)){
                                    VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(file);
                                    if(null == vf){
                                        System.out.println("Error in getting VirtualFile from file");
                                        break;
                                    }
                                    Document document = FileDocumentManager.getInstance().getDocument(vf);
                                    EditorTextField editorTextField = new EditorTextField(document, eve.getProject(), FileTypes.UNKNOWN, true);
                                    editorTextField.setOneLineMode(false);
                                    editorTextField.setRequestFocusEnabled(false);
                                    editorTextField.setPreferredWidth(400);
                                    document.setReadOnly(true);

                                    DialogBuilder builder = new DialogBuilder(eve.getProject());
                                    builder.setDimensionServiceKey("TextControl");
                                    builder.setCenterPanel(editorTextField);
                                    builder.setPreferredFocusComponent(editorTextField);
                                    builder.setTitle("EdgeWorker - "+fileName);
                                    builder.addCloseButton();
                                    builder.setCenterPanel(new JBScrollPane(editorTextField, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS));
                                    builder.show();
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    public SimpleToolWindowPanel getPanel() {
        return panel;
    }
}
