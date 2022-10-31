package ui;

import javax.swing.*;
import java.awt.*;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.table.DefaultTableModel;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Paths;

public class GetEdgeWorkerProfilingDataDialog extends DialogWrapper {
    private JBHintTextField edgeWorkerURLValue;
    private ComboBox<String> eventHandlerDropdown;
    private JBHintTextField filePath;
    private JBHintTextField fileName;
    private DefaultTableModel tableModel;
    private JBTable headersTable;


    public GetEdgeWorkerProfilingDataDialog() {
        super(true);
        setTitle("EdgeWorkers Code Profiler");
        init();
        setOKButtonText("Run Profiler");
    }

    public String getSelectedEventHandler() {
        return eventHandlerDropdown.getItem();
    }

    public String getEdgeWorkerURL() {
        return edgeWorkerURLValue.getText();
    }

    public String getFilePath() {
        if (!filePath.getText().isBlank()){
            return filePath.getText();
        } else {
            return System.getProperty("java.io.tmpdir");
        }
    }

    public String getFileName() {
        if (!fileName.getText().isBlank()){
            return fileName.getText();
        } else {
            return "codeProfile-" + System.currentTimeMillis();
        }
    }

    public String[][] getHeaders() {
        ArrayList<String[]> tableData = new ArrayList<>();
        for (int i = 0; i < this.tableModel.getRowCount(); i++) {
            String[] header = new String[2];
            header[0] = (String) this.tableModel.getValueAt(i, 0);
            header[1] = (String) this.tableModel.getValueAt(i, 1);
            tableData.add(header);
        }
        return tableData.toArray(new String[0][]);
    }

    private void addRow() {
        this.tableModel.addRow(new Object[]{null, null});
    }

    private void deleteSelectedRows() {
        int[] selectedRows = this.headersTable.getSelectedRows();
        if (selectedRows.length > 0) {
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                this.tableModel.removeRow(selectedRows[i]);
            }
        }
    }

    private JPanel getTableButtonPanel(){
        JButton deleteButton = new JButton("Delete Selected Headers");
        deleteButton.addActionListener(e -> this.deleteSelectedRows());
        deleteButton.setEnabled(false); // zero rows exist initially
        headersTable.getSelectionModel().addListSelectionListener(listSelectionEvent -> // disable delete if no rows are selected
                deleteButton.setEnabled(headersTable.getSelectedRows().length != 0));

        JButton addButton = new JButton("Add Header");
        addButton.addActionListener(e -> this.addRow());

        int height = addButton.getPreferredSize().height;
        JPanel centerPadding = new JPanel();
        centerPadding.setMinimumSize(new Dimension(10, height));

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        buttonPanel.add(addButton, BorderLayout.WEST);
        buttonPanel.add(centerPadding, BorderLayout.CENTER);
        buttonPanel.add(deleteButton, BorderLayout.EAST);

        return buttonPanel;
    }

    private boolean areHeadersValid() {
        for (int i = 0; i < this.tableModel.getRowCount(); i++) {
            String name = (String) this.tableModel.getValueAt(i, 0);
            String val = (String) this.tableModel.getValueAt(i, 1);
            if (name == null || val == null || name.isBlank() || val.isEmpty() ){
                // allow whitespace values but not names
                return false;
            }
        }
        return true;
    }

    private boolean isValidPath() {
        try {
            Paths.get(this.filePath.getText());
        } catch (InvalidPathException | NullPointerException ex) {
            return false;
        }
        return true;
    }

    @Override
    protected @NotNull List<ValidationInfo> doValidateAll() {
        List<ValidationInfo> list = new ArrayList<>();
        if (this.edgeWorkerURLValue.getText().isBlank()){
            list.add(new ValidationInfo("The EdgeWorker URL cannot be empty", edgeWorkerURLValue));
        }
        if (!isValidPath()){
            list.add(new ValidationInfo("The File Path must be a valid path format", filePath));
        }
        if (!this.areHeadersValid()){
            list.add(new ValidationInfo("Headers cannot contain null names or values", headersTable));
            headersTable.setBorder(BorderFactory.createLineBorder(JBColor.RED));
        } else {
            headersTable.setBorder(null);
        }

        return list;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel();
        GroupLayout layout = new GroupLayout(dialogPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        JBLabel ewNameLabel = new JBLabel("EdgeWorker Name:");
        JBLabel eventHandlerLabel = new JBLabel("Event Handler:");
        JBLabel filePathLabel = new JBLabel("File Path:");
        JBLabel fileNameLabel = new JBLabel("File Name:");
        JBLabel headersLabel = new JBLabel("Request Headers:");

        eventHandlerDropdown = new ComboBox<>();
        eventHandlerDropdown.addItem("onClientRequest");
        eventHandlerDropdown.addItem("onOriginRequest");
        eventHandlerDropdown.addItem("onOriginResponse");
        eventHandlerDropdown.addItem("onClientResponse");
        eventHandlerDropdown.addItem("responseProvider");

        edgeWorkerURLValue = new JBHintTextField("eg: https://www.example.com", JBColor.gray);
        filePath = new JBHintTextField("eg: /Users/$USERID/Downloads", JBColor.gray);
        fileName = new JBHintTextField("eg: filename", JBColor.gray);

        tableModel = new DefaultTableModel(new Object[]{"Name", "Value"}, 0);
        headersTable = new JBTable(tableModel);
        JBScrollPane tablePanel = new JBScrollPane(headersTable);
        tablePanel.setMinimumSize(new Dimension(100, 125));
        JPanel tableButtonPanel = getTableButtonPanel();

        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(ewNameLabel)
                        .addComponent(eventHandlerLabel)
                        .addComponent(filePathLabel)
                        .addComponent(fileNameLabel)
                        .addComponent(headersLabel)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(edgeWorkerURLValue)
                        .addComponent(eventHandlerDropdown)
                        .addComponent(filePath)
                        .addComponent(fileName)
                        .addComponent(tablePanel)
                        .addComponent(tableButtonPanel)
                )
        );

        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(ewNameLabel)
                        .addComponent(edgeWorkerURLValue)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(eventHandlerLabel)
                        .addComponent(eventHandlerDropdown)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(filePathLabel)
                        .addComponent(filePath)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(fileNameLabel)
                        .addComponent(fileName)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(headersLabel)
                        .addComponent(tablePanel)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(tableButtonPanel)
                )

        );

        dialogPanel.setLayout(layout);
        return dialogPanel;
    }
}
