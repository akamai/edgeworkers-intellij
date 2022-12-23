package ui;

import actions.RunCodeProfilerAction;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.playback.commands.ActionCommand;
import com.intellij.ui.CollapsiblePanel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import utils.Constants;

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
    private JBRadioButton cpuButton;
    private JBRadioButton memoryButton;
    private JBHintTextField edgeWorkerURLValue;
    private ComboBox<String> eventHandlerDropdown;
    private ComboBox<String> methodDropdown;
    private JBHintTextField samplingInterval;
    private JBHintTextField filePath;
    private JBHintTextField fileName;
    private DefaultTableModel tableModel;
    private JBTable headersTable;
    private JBHintTextField edgeIpOverride;
    private boolean isLoading;
    private boolean shouldValidate;
    private Border defaultBorder;
    private JBLabel edgeWorkerURLValueErrorLabel;
    private JBLabel samplingIntervalErrorLabel;
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

    public String getProfilingMode() {
        return cpuButton.isSelected() ? Constants.CPU_PROFILING : Constants.MEM_PROFILING;
    }

    public String getSelectedEventHandler() {
        return eventHandlerDropdown.getItem();
    }

    public String getEdgeWorkerURL() {
        return edgeWorkerURLValue.getText();
    }

    public String getHttpMethod() {
        return methodDropdown.getItem();
    }

    public String getSamplingInterval() {
        if (!samplingInterval.getText().isBlank()) {
            return samplingInterval.getText();
        } else {
            return Constants.EW_DEFAULT_SAMPLING_SIZE;
        }
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

    /**
     * Get the edge ip override
     *
     * @return String containing user inputted edge IP, or null if no IP has been entered
     */
    public String getEdgeIpOverride() {
        return edgeIpOverride.getText().isEmpty() ? null : edgeIpOverride.getText();
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
        String ewUrl = getEdgeWorkerURL();
        if (ewUrl.isBlank()) {
            setEdgeWorkerURLValueError("The EdgeWorker URL cannot be empty");
        } else if (!ewUrl.startsWith("http://") && !ewUrl.startsWith("https://")) {
            setEdgeWorkerURLValueError("The EdgeWorker URL must contain the HTTP protocol");
        } else {
            // check if valid URI
            try {
                URI uri = new URI(ewUrl); // throws if violates RFC 2396
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

    private boolean validateSamplingSize() {
        boolean isValid;
        try {
            isValid = Integer.parseInt(getSamplingInterval()) > 0;
        } catch (NumberFormatException ex) {
            isValid = false;
        }
        samplingIntervalErrorLabel.setVisible(!isValid);
        samplingInterval.setBorder(isValid ? defaultBorder : BorderFactory.createLineBorder(JBColor.RED));
        return isValid;
    }

    private boolean validateFilePath() {
        boolean exists;
        try {
            exists = Files.exists(Paths.get(getFilePath()));
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

        samplingInterval.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (shouldValidate) validateSamplingSize();
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
        // bitwise & to prevent short circuit
        return (validateEdgeWorkerUrlValue() & validateSamplingSize() & validateFilePath() & validateHeaders());
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
        // clear fields, dropdowns, table
        shouldValidate = false;
        edgeWorkerURLValue.reset();
        methodDropdown.setItem(methodDropdown.getItemAt(0));
        eventHandlerDropdown.setItem(eventHandlerDropdown.getItemAt(0));
        samplingInterval.reset();
        filePath.reset();
        fileName.reset();
        tableModel.setRowCount(0);
        edgeIpOverride.reset();

        // clear errors
        edgeWorkerURLValueErrorLabel.setVisible(false);
        edgeWorkerURLValue.setBorder(defaultBorder);
        samplingIntervalErrorLabel.setVisible(false);
        samplingInterval.setBorder(defaultBorder);
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
        JBLabel stagingLabel = new JBLabel("Profile an EdgeWorker deployed to the Akamai staging network");
        JBLabel ewNameLabel = new JBLabel("EdgeWorker URL:");
        JBLabel eventHandlerLabel = new JBLabel("Event Handler:");
        JBLabel samplingSizeLabel = new JBLabel("Sampling Interval (μs):");
        JBLabel filePathLabel = new JBLabel("File Path:");
        JBLabel fileNameLabel = new JBLabel("File Name:");
        JBLabel edgeIpOverrideLabel = new JBLabel("Edge IP Override:");
        JBLabel headersLabel = new JBLabel("Request Headers:");

        // Text Fields
        edgeWorkerURLValue = new JBHintTextField("https://www.example.com");
        methodDropdown = new ComboBox<>(Constants.EW_HTTP_METHODS);
        eventHandlerDropdown = new ComboBox<>(new String[]{"onClientRequest", "onOriginRequest", "onOriginResponse", "onClientResponse", "responseProvider"});
        samplingInterval = new JBHintTextField("default: " + Constants.EW_DEFAULT_SAMPLING_SIZE + " μs");
        filePath = new JBHintTextField("eg: /Users/myUser/Downloads");
        fileName = new JBHintTextField("eg: filename");
        edgeIpOverride = new JBHintTextField("Enter edge server IP address", 18);
        defaultBorder = edgeWorkerURLValue.getBorder();

        // Radio Group
        cpuButton = new JBRadioButton("CPU Profiling");
        memoryButton = new JBRadioButton("Memory Profiling");
        cpuButton.setSelected(true);
        JPanel modeButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        modeButtons.add(cpuButton);
        modeButtons.add(memoryButton);
        ButtonGroup radioGroup = new ButtonGroup();
        radioGroup.add(cpuButton);
        radioGroup.add(memoryButton);

        // Error Labels
        edgeWorkerURLValueErrorLabel = new JBLabel("The EdgeWorker URL must be a valid URL");
        edgeWorkerURLValueErrorLabel.setVisible(false);
        edgeWorkerURLValueErrorLabel.setForeground(JBColor.red);
        samplingIntervalErrorLabel = new JBLabel("The Sampling Interval must be a positive integer");
        samplingIntervalErrorLabel.setVisible(false);
        samplingIntervalErrorLabel.setForeground(JBColor.red);
        filePathErrorLabel = new JBLabel("The File Path is invalid or does not exist");
        filePathErrorLabel.setVisible(false);
        filePathErrorLabel.setForeground(JBColor.red);
        headersTableErrorLabel = new JBLabel("Headers cannot contain null names or values");
        headersTableErrorLabel.setVisible(false);
        headersTableErrorLabel.setForeground(JBColor.red);
        errorPanel.add(edgeWorkerURLValueErrorLabel);
        errorPanel.add(samplingIntervalErrorLabel);
        errorPanel.add(filePathErrorLabel);
        errorPanel.add(headersTableErrorLabel);

        // Layout Panels
        panel.add(scrollPane, BorderLayout.CENTER);
        scrollPane.setViewportView(layoutPanel);
        GroupLayout layout = new GroupLayout(layoutPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        // Sub Panels
        // Top Row Panel
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, cpuButton.getPreferredSize().height));
        topRow.add(stagingLabel, BorderLayout.WEST);
        topRow.add(modeButtons, BorderLayout.EAST);

        // URL + Method Panel
        JPanel urlMethodPanel = new JPanel(new BorderLayout());
        urlMethodPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, methodDropdown.getPreferredSize().height));
        urlMethodPanel.add(methodDropdown, BorderLayout.WEST);
        urlMethodPanel.add(edgeWorkerURLValue, BorderLayout.CENTER);

        // Table Panel
        tableModel = new DefaultTableModel(new Object[]{"Name", "Value"}, 0);
        headersTable = new JBTable(tableModel);
        JBScrollPane tablePanel = new JBScrollPane(headersTable);
        tablePanel.setMinimumSize(new Dimension(100, 50));

        // Row Buttons Panel
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

        // Bottom Row Panel
        //      Buttons
        JButton runButton = new JButton("Run Profiler");
        runButton.addActionListener(e -> handleRun(runButton));
        runButton.setEnabled(!isLoading);
        runButton.setDefaultCapable(true);
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> handleReset());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, runButton.getPreferredSize().height));
        buttons.add(resetButton);
        buttons.add(runButton);

        JPanel submitPanel = new JPanel(new BorderLayout());
        submitPanel.add(buttons, BorderLayout.SOUTH);

        //      Advanced Section
        JPanel advancedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        GroupLayout advancedLayout = new GroupLayout(advancedPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        advancedLayout.setHorizontalGroup(advancedLayout.createSequentialGroup()
                .addGroup(advancedLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(edgeIpOverrideLabel)
                )
                .addGroup(advancedLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(edgeIpOverride)
                )
        );
        advancedLayout.setVerticalGroup(advancedLayout.createSequentialGroup()
                .addGroup(advancedLayout.createParallelGroup()
                        .addComponent(edgeIpOverrideLabel)
                        .addComponent(edgeIpOverride)
                )
        );
        CollapsiblePanel collapsibleAdvanced = new CollapsiblePanel(advancedPanel, true, true, AllIcons.Actions.Collapseall, AllIcons.Actions.Expandall, "");

        //      Panel For the whole row
        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, collapsibleAdvanced.getPreferredSize().height));
        bottomRow.add(collapsibleAdvanced, BorderLayout.CENTER);
        bottomRow.add(submitPanel, BorderLayout.EAST);

        // Listeners
        setupValidationListeners();

        // Layout Components
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(topRow, GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(ewNameLabel)
                                .addComponent(eventHandlerLabel)
                                .addComponent(samplingSizeLabel)
                                .addComponent(filePathLabel)
                                .addComponent(fileNameLabel)
                                .addComponent(headersLabel)
                        )
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(urlMethodPanel)
                                .addComponent(eventHandlerDropdown)
                                .addComponent(samplingInterval)
                                .addComponent(filePath)
                                .addComponent(fileName)
                                .addComponent(tablePanel)
                                .addComponent(rowButtonsPanel)
                                .addComponent(errorPanel)
                                .addComponent(bottomRow)
                        )
                )
        );

        layout.setVerticalGroup(layout.createSequentialGroup()
                .addComponent(topRow)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(ewNameLabel)
                        .addComponent(urlMethodPanel)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(eventHandlerLabel)
                        .addComponent(eventHandlerDropdown)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(samplingSizeLabel)
                        .addComponent(samplingInterval)
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
                .addComponent(rowButtonsPanel)
                .addComponent(errorPanel)
                .addComponent(bottomRow)
        );

        layoutPanel.setLayout(layout);
        return panel;
    }
}
