package ui;

import actions.RunCodeProfilerAction;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.playback.commands.ActionCommand;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class CodeProfilerToolWindow {

    private final ResourceBundle resourceBundle;
    private final SimpleToolWindowPanel panel;
    private JBHintTextField edgeWorkerURLValue;
    private ComboBox<String> eventHandlerDropdown;
    private JBHintTextField filePath;
    private JBHintTextField fileName;
    private DefaultTableModel tableModel;
    private JBTable headersTable;
    private boolean isLoading;
    private boolean shouldValidate;
    private Border defaultBorder;
    private JBLabel edgeWorkerURLValueErrorLabel;
    private JBLabel filePathErrorLabel;
    private JBLabel headersTableErrorLabel;


    public CodeProfilerToolWindow() {
        this.resourceBundle = ResourceBundle.getBundle("ActionBundle");
        this.panel = new SimpleToolWindowPanel(false, false);
        this.isLoading = false;
        this.shouldValidate = false;
        ActionManager actionManager = ActionManager.getInstance();
        if (null != actionManager.getAction(resourceBundle.getString("action.runCodeProfiler.id"))) {
            actionManager.unregisterAction(resourceBundle.getString("action.runCodeProfiler.id"));
        }
        actionManager.registerAction(resourceBundle.getString("action.runCodeProfiler.id"), new RunCodeProfilerAction(this));
    }

    public String getSelectedEventHandler() {
        return eventHandlerDropdown.getItem();
    }

    public String getEdgeWorkerURL() {
        return edgeWorkerURLValue.getText();
    }

    public String getFilePath() {
        if (!filePath.getText().isBlank()) {
            return filePath.getText();
        } else {
            return System.getProperty("java.io.tmpdir");
        }
    }

    public String getFileName() {
        if (!fileName.getText().isBlank()) {
            return fileName.getText();
        } else {
            return "codeProfile-" + System.currentTimeMillis();
        }
    }

    public ArrayList<String[]> getHeaders() {
        ArrayList<String[]> tableData = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String headerName = (String) tableModel.getValueAt(i, 0);
            String headerVal = (String) tableModel.getValueAt(i, 1);

            if (headerName.matches(".*:\\s*$")) {
                // remove trailing colon followed by 0 or more spaces
                headerName = headerName.split(":\\s*$")[0];
            }
            tableData.add(new String[]{headerName, headerVal});
        }
        return tableData;
    }

    public boolean getIsLoading() {
        return isLoading;
    }

    public void setIsLoading(boolean isLoading) {
        this.isLoading = isLoading;
    }

    private void addRow() {
        tableModel.addRow(new Object[]{null, null});
    }

    private void deleteSelectedRows() {
        int[] selectedRows = headersTable.getSelectedRows();
        if (selectedRows.length > 0) {
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                tableModel.removeRow(selectedRows[i]);
            }
        }
    }

    private void setEdgeWorkerURLValueError(String text) {
        edgeWorkerURLValueErrorLabel.setText(text);
        edgeWorkerURLValueErrorLabel.setVisible(true);
        edgeWorkerURLValue.setBorder(BorderFactory.createLineBorder(JBColor.RED));
    }

    private boolean validateEdgeWorkerUrlValue() {
        if (edgeWorkerURLValue.getText().isBlank()) {
            setEdgeWorkerURLValueError("The EdgeWorker URL cannot be empty");
        } else if (!edgeWorkerURLValue.getText().startsWith("http://") && !edgeWorkerURLValue.getText().startsWith("https://")) {
            setEdgeWorkerURLValueError("The EdgeWorker URL must contain the HTTP protocol");
        } else {
            // check if valid URI
            try {
                URI uri = new URI(edgeWorkerURLValue.getText()); // throws if violates RFC 2396
                if (uri.getHost() == null) {
                    throw new Exception();
                }
                edgeWorkerURLValueErrorLabel.setVisible(false);
                edgeWorkerURLValue.setBorder(defaultBorder);
                return true;
            } catch (Exception ex) {
                setEdgeWorkerURLValueError("The EdgeWorker URL must be a valid URL");
            }
        }
        return false;
    }

    private boolean validateFilePath() {
        boolean exists;
        try {
            exists = Files.exists(Paths.get(filePath.getText()));
        } catch (Exception ex) {
            exists = false;
        }
        filePathErrorLabel.setVisible(!exists);
        filePath.setBorder(exists ? defaultBorder : BorderFactory.createLineBorder(JBColor.RED));
        return exists;
    }

    private boolean validateHeaders() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String name = (String) tableModel.getValueAt(i, 0);
            String val = (String) tableModel.getValueAt(i, 1);
            if (name == null || val == null || name.isBlank() || val.isEmpty()) {
                // allow whitespace values but not names
                headersTableErrorLabel.setVisible(true);
                headersTable.setBorder(BorderFactory.createLineBorder(JBColor.RED));
                return false;
            }
        }
        headersTableErrorLabel.setVisible(false);
        headersTable.setBorder(defaultBorder);
        return true;
    }

    private void setupValidationListeners() {
        edgeWorkerURLValue.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {
                if (shouldValidate) validateEdgeWorkerUrlValue();

            }
        });

        filePath.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {
                if (shouldValidate) validateFilePath();

            }
        });

        tableModel.addTableModelListener(e -> {
            if (shouldValidate) validateHeaders();
        });

    }

    private boolean validateForm() {
        return (validateEdgeWorkerUrlValue() & validateFilePath() & validateHeaders()); // bitwise & to prevent short circuit
    }

    private void handleRun(Component contextComponent) {
        ActionManager actionManager = ActionManager.getInstance();
        String actionId = resourceBundle.getString("action.runCodeProfiler.id");
        if (validateForm()) {
            shouldValidate = false; // disable continuous validation after successful input
            actionManager.tryToExecute(actionManager.getAction(actionId), ActionCommand.getInputEvent(actionId), contextComponent, "", true);
        } else {
            shouldValidate = true;
        }
    }

    private void handleReset() {
        shouldValidate = false;
        edgeWorkerURLValue.resetText();
        eventHandlerDropdown.setItem(eventHandlerDropdown.getItemAt(0));
        filePath.resetText();
        fileName.resetText();
        tableModel.setRowCount(0);

        // clear errors
        edgeWorkerURLValueErrorLabel.setVisible(false);
        edgeWorkerURLValue.setBorder(defaultBorder);
        filePathErrorLabel.setVisible(false);
        filePath.setBorder(defaultBorder);
        headersTableErrorLabel.setVisible(false);
        headersTable.setBorder(defaultBorder);
    }

    public JPanel getContent() {
        // Panels
        panel.removeAll();
        JBScrollPane scrollPane = new JBScrollPane();
        JPanel layoutPanel = new JPanel();
        JPanel errorPanel = new JPanel();
        errorPanel.setLayout(new BoxLayout(errorPanel, BoxLayout.PAGE_AXIS));

        // Labels
        JBLabel ewNameLabel = new JBLabel("EdgeWorker URL:");
        JBLabel eventHandlerLabel = new JBLabel("Event Handler:");
        JBLabel filePathLabel = new JBLabel("File Path:");
        JBLabel fileNameLabel = new JBLabel("File Name:");
        JBLabel headersLabel = new JBLabel("Request Headers:");

        // Text Fields
        edgeWorkerURLValue = new JBHintTextField("eg: https://www.example.com", JBColor.gray);
        eventHandlerDropdown = new ComboBox<>(new String[]{"onClientRequest", "onOriginRequest", "onOriginResponse", "onClientResponse", "responseProvider"});
        filePath = new JBHintTextField("eg: /Users/myUser/Downloads", JBColor.gray);
        fileName = new JBHintTextField("eg: filename", JBColor.gray);
        defaultBorder = edgeWorkerURLValue.getBorder();

        // Error Labels
        edgeWorkerURLValueErrorLabel = new JBLabel("The EdgeWorker URL must be a valid URL");
        edgeWorkerURLValueErrorLabel.setVisible(false);
        edgeWorkerURLValueErrorLabel.setForeground(JBColor.red);
        filePathErrorLabel = new JBLabel("The File Path is invalid or does not exist");
        filePathErrorLabel.setVisible(false);
        filePathErrorLabel.setForeground(JBColor.red);
        headersTableErrorLabel = new JBLabel("Headers cannot contain null names or values");
        headersTableErrorLabel.setVisible(false);
        headersTableErrorLabel.setForeground(JBColor.red);
        errorPanel.add(edgeWorkerURLValueErrorLabel);
        errorPanel.add(filePathErrorLabel);
        errorPanel.add(headersTableErrorLabel);

        // Layout Panels
        panel.add(scrollPane, BorderLayout.CENTER);
        scrollPane.setViewportView(layoutPanel);
        GroupLayout layout = new GroupLayout(layoutPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        // Table
        tableModel = new DefaultTableModel(new Object[]{"Name", "Value"}, 0);
        headersTable = new JBTable(tableModel);
        JBScrollPane tablePanel = new JBScrollPane(headersTable);
        tablePanel.setMinimumSize(new Dimension(100, 50));

        // Row Buttons
        JButton deleteButton = new JButton("Delete Selected Headers");
        deleteButton.addActionListener(e -> deleteSelectedRows());
        deleteButton.setEnabled(false); // zero rows exist initially
        headersTable.getSelectionModel().addListSelectionListener(listSelectionEvent -> // disable delete if no rows are selected
                deleteButton.setEnabled(headersTable.getSelectedRows().length != 0));
        JButton addButton = new JButton("Add Header");
        addButton.addActionListener(e -> addRow());

        JPanel rowButtonsPanel = new JPanel(new BorderLayout());
        rowButtonsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, addButton.getPreferredSize().height));
        rowButtonsPanel.add(addButton, BorderLayout.WEST);
        rowButtonsPanel.add(deleteButton, BorderLayout.EAST);

        // Run & Reset Buttons
        JButton runButton = new JButton("Run Profiler");
        runButton.addActionListener(e -> handleRun(runButton));
        runButton.setEnabled(!isLoading);
        runButton.setDefaultCapable(true);
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> handleReset());

        JPanel submitPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        submitPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, runButton.getHeight()));
        submitPanel.add(resetButton);
        submitPanel.add(runButton);

        // Listeners
        setupValidationListeners();

        // Layout Components
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
                        .addComponent(rowButtonsPanel)
                        .addComponent(errorPanel)
                        .addComponent(submitPanel)
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
                        .addComponent(rowButtonsPanel)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(errorPanel)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(submitPanel)
                )
        );

        layoutPanel.setLayout(layout);
        return panel;
    }
}
