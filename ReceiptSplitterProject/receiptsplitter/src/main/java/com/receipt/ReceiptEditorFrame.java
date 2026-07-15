package com.receipt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;

import net.sourceforge.tess4j.TesseractException;

public class ReceiptEditorFrame extends JFrame {

    // HARDCODED PARTY SIZE VALUE, default 5
    public final int NUM_PEOPLE;

    private static final int PRICE_COLUMN_INDEX = 1;
    private static final int SPLIT_COLUMN_INDEX = 2;

    // OBJECT DEFS
    private final ReceiptScanner scanner;
    private final ReceiptTableModel tableModel;
    private final JTable table;

    private final JTextField subtotalField;
    private final JTextField taxField;
    private final JTextField totalField;

    private final PlaceholderTextField[] personNameFields;
    private final JCheckBox[] splitCheckBoxes;
    private final JLabel[] breakdownLabels;
    private final JLabel unassignedLabel;
    private final double[] lastPersonTotals;

    public static Color RGB = new Color(82, 110, 135);

    // boolean against checkbox listeners firing while loading row split. */
    private boolean loadingSelection = false;

    // CONSTRUCTOR
    public ReceiptEditorFrame(String tessDataPath, int peeps) {
        super("Matt's Receipt Splitter App || VERSION 1.0 || July 13th, 2026");
        //getContentPane().setBackground(RGB);
        //Color C = new COLOR(245, 245, 250);
        NUM_PEOPLE = peeps;
        this.personNameFields = new PlaceholderTextField[NUM_PEOPLE];
        this.splitCheckBoxes = new JCheckBox[NUM_PEOPLE];
        this.breakdownLabels = new JLabel[NUM_PEOPLE];
        this.unassignedLabel = new JLabel(" ");
        this.lastPersonTotals = new double[NUM_PEOPLE];

        scanner = new ReceiptScanner(tessDataPath);
        tableModel = new ReceiptTableModel(NUM_PEOPLE, this::personName);
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(PRICE_COLUMN_INDEX).setCellRenderer(new PlaceholderTextField.PriceCellRenderer());
        table.getColumnModel().getColumn(PRICE_COLUMN_INDEX).setCellEditor(new PlaceholderTextField.PriceCellEditor());
        DefaultTableCellRenderer splitRenderer = new DefaultTableCellRenderer();
        splitRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(SPLIT_COLUMN_INDEX).setCellRenderer(splitRenderer);
        table.getColumnModel().getColumn(SPLIT_COLUMN_INDEX).setPreferredWidth(180);
        table.getModel().addTableModelListener(e -> updateTotals());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSplitForSelectedRow();
            }
        });



        JButton scanButton = new JButton("Scan Receipt");
        scanButton.setBackground(RGB);
        scanButton.setForeground(Color.WHITE);
        scanButton.setFocusPainted(false);


        JButton addButton = new JButton("Add Item");
        addButton.setBackground(RGB);
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);


        JButton removeButton = new JButton("Remove Selected");
        removeButton.setBackground(RGB);
        removeButton.setForeground(Color.WHITE);
        removeButton.setFocusPainted(false);


        JButton continueButton = new JButton("Continue");
        continueButton.setBackground(RGB);
        continueButton.setForeground(Color.WHITE);
        continueButton.setFocusPainted(false);

        subtotalField = new JTextField("0.00", 8);
        taxField = new JTextField("0.00", 8);
        totalField = new JTextField("0.00", 8);

        subtotalField.setEditable(false);
        totalField.setEditable(false);

        JPanel topPanel = new JPanel();
        topPanel.add(scanButton);

        JPanel bottomPanel = new JPanel(new FlowLayout());
        bottomPanel.add(addButton);
        bottomPanel.add(removeButton);
        bottomPanel.add(new JLabel("Subtotal:"));
        bottomPanel.add(subtotalField);
        bottomPanel.add(new JLabel("Tax:"));
        bottomPanel.add(taxField);
        bottomPanel.add(new JLabel("Total:"));
        bottomPanel.add(totalField);
        bottomPanel.add(continueButton);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        add(buildSidePanel(), BorderLayout.EAST);

        scanButton.addActionListener(e -> scanReceipt());

        addButton.addActionListener(e -> {
            tableModel.addItem();
            updateTotals();
        });

        removeButton.addActionListener(e -> {
            tableModel.removeItem(table.getSelectedRow());
            loadSplitForSelectedRow();
            updateTotals();
        });

        taxField.addActionListener(e -> updateTotals());

        continueButton.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Continue functionality not implemented yet."));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 550);
        setLocationRelativeTo(null);

        loadSplitForSelectedRow();
        updateTotals();

        setVisible(true);
    }

    // Side Panel Builder 
    private JPanel buildSidePanel() {
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        sidePanel.add(buildPeoplePanel());
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(buildSplitPanel());
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(buildBreakdownPanel());

        return sidePanel;
    }

    // People Panel Builder with listeners for names
    private JPanel buildPeoplePanel() {
        JPanel panel = new JPanel(new GridLayout(NUM_PEOPLE, 2, 4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("People"));

        for (int i = 0; i < NUM_PEOPLE; i++) {
            final int index = i;
            PlaceholderTextField nameField = new PlaceholderTextField("Person " + (i + 1));
            personNameFields[i] = nameField;

            nameField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) { onNameChanged(index); }

                @Override
                public void removeUpdate(DocumentEvent e) { onNameChanged(index); }

                @Override
                public void changedUpdate(DocumentEvent e) { onNameChanged(index); }
            });

            panel.add(new JLabel("Person " + (i + 1) + ":"));
            panel.add(nameField);
        }

        return panel;
    }


    // Split Panel Builder with logic for splits
    private JPanel buildSplitPanel() {
        JPanel panel = new JPanel(new GridLayout(NUM_PEOPLE + 1, 1, 2, 2));
        panel.setBorder(BorderFactory.createTitledBorder("Split Selected Item"));

        for (int i = 0; i < NUM_PEOPLE; i++) {
            final int index = i;
            JCheckBox checkBox = new JCheckBox(personName(i));
            splitCheckBoxes[i] = checkBox;

            checkBox.addActionListener(e -> {
                if (loadingSelection) {
                    return;
                }

                int row = table.getSelectedRow();
                if (row == -1) {
                    return;
                }

                boolean[] split = tableModel.getSplit(row);
                split[index] = checkBox.isSelected();
                tableModel.setSplit(row, split);
                updateTotals();
            });

            panel.add(checkBox);
        }

        JPanel quickButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton allButton = new JButton("All");
        allButton.setBackground(RGB);
        allButton.setForeground(Color.WHITE);
        allButton.setFocusPainted(false);


        JButton noneButton = new JButton("None");
        noneButton.setBackground(RGB);
        noneButton.setForeground(Color.WHITE);
        noneButton.setFocusPainted(false);

        allButton.addActionListener(e -> setSelectedRowSplit(true));
        noneButton.addActionListener(e -> setSelectedRowSplit(false));

        quickButtons.add(allButton);
        quickButtons.add(noneButton);
        panel.add(quickButtons);

        return panel;
    }

    // Cost Breakdown Panel builder
    private JPanel buildBreakdownPanel() {
        JPanel panel = new JPanel(new GridLayout(NUM_PEOPLE + 1, 1, 2, 2));
        panel.setBorder(BorderFactory.createTitledBorder("Per-Person Total"));

        for (int i = 0; i < NUM_PEOPLE; i++) {
            breakdownLabels[i] = new JLabel(personName(i) + ": $0.00");
            panel.add(breakdownLabels[i]);
        }

        panel.add(unassignedLabel);

        return panel;
    }

    // split setter
    private void setSelectedRowSplit(boolean value) {
        int row = table.getSelectedRow();
        if (row == -1) {
            return;
        }

        boolean[] split = new boolean[NUM_PEOPLE];
        Arrays.fill(split, value);
        tableModel.setSplit(row, split);
        loadSplitForSelectedRow();
        updateTotals();
    }
    // recall split
    private void loadSplitForSelectedRow() {
        int row = table.getSelectedRow();
        boolean hasSelection = row != -1;

        loadingSelection = true;
        boolean[] split = hasSelection ? tableModel.getSplit(row) : new boolean[NUM_PEOPLE];

        for (int i = 0; i < NUM_PEOPLE; i++) {
            splitCheckBoxes[i].setSelected(split[i]);
            splitCheckBoxes[i].setEnabled(hasSelection);
        }
        loadingSelection = false;
    }

    private void onNameChanged(int index) {
        splitCheckBoxes[index].setText(personName(index));
        breakdownLabels[index].setText(personName(index) + ": " + formatCurrency(lastPersonTotals[index]));
        tableModel.refreshSplitColumn();
        updateTotals();
    }

    private String personName(int index) {
        String name = personNameFields[index] == null ? "" : personNameFields[index].getText().trim();
        return name.isEmpty() ? "Person " + (index + 1) : name;
    }


    // Totals updator to call and set text
    private void updateTotals() {
        double subtotal = 0.0;
        double[] personSubtotal = new double[NUM_PEOPLE];
        double unassigned = 0.0;

        List<ReceiptScanner.LineItem> items = tableModel.getItems();

        for (int row = 0; row < items.size(); row++) {
            double price = items.get(row).price;
            subtotal += price;

            boolean[] split = tableModel.getSplit(row);
            int count = 0;
            for (boolean assigned : split) {
                if (assigned) {
                    count++;
                }
            }

            if (count == 0) {
                unassigned += price;
            } else {
                double share = price / count;
                for (int i = 0; i < NUM_PEOPLE; i++) {
                    if (split[i]) {
                        personSubtotal[i] += share;
                    }
                }
            }
        }

        subtotalField.setText(String.format("%.2f", subtotal));

        double tax = parseOrZero(taxField.getText());
        taxField.setText(String.format("%.2f", tax));

        double total = subtotal + tax;
        totalField.setText(String.format("%.2f", total));

        for (int i = 0; i < NUM_PEOPLE; i++) {
            double personTax = subtotal > 0 ? (personSubtotal[i] / subtotal) * tax : 0.0;
            double personTotal = personSubtotal[i] + personTax;
            lastPersonTotals[i] = personTotal;
            breakdownLabels[i].setText(personName(i) + ": " + formatCurrency(personTotal));
        }

        if (unassigned > 0.0) {
            unassignedLabel.setText("Unassigned: " + formatCurrency(unassigned));
        } else {
            unassignedLabel.setText(" ");
        }
    }

    private String formatCurrency(double amount) {
        return String.format("$%.2f", amount);
    }

    private double parseOrZero(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // CALL for Receipt Scanner and to add info to UI
    private void scanReceipt() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Image Files", "jpg", "jpeg", "png", "bmp", "tif", "tiff"));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File image = chooser.getSelectedFile();

        try {
            ReceiptScanner.Receipt receipt = scanner.scan(image);
            tableModel.setReceipt(receipt);

            if (receipt.tax != null) {
                taxField.setText(String.format("%.2f", receipt.tax));
            }

            loadSplitForSelectedRow();
            updateTotals();

        } catch (TesseractException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "OCR Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}