package com.receipt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

public class ReceiptTableModel extends AbstractTableModel {

    private static final String[] COLUMN_NAMES = { "Item", "Price", "Split" };
    private static final int COLUMN_NAME = 0;
    private static final int COLUMN_PRICE = 1;
    private static final int COLUMN_SPLIT = 2;

    private final int numPeople;
    private final IntFunction<String> personNameProvider;
    private final List<ReceiptScanner.LineItem> items = new ArrayList<>();

    /** splits.get(row)[personIndex]: true equals person shares cost, false otherwise. */
    private final List<boolean[]> splits = new ArrayList<>();

    public ReceiptTableModel(int numPeople, IntFunction<String> personNameProvider) {
        this.numPeople = numPeople;
        this.personNameProvider = personNameProvider;
    }

    public void setReceipt(ReceiptScanner.Receipt receipt) {
        items.clear();
        splits.clear();

        if (receipt != null) {
            items.addAll(receipt.items);

            for (int i = 0; i < items.size(); i++) {
                splits.add(defaultSplit());
            }
        }

        fireTableDataChanged();
    }

    public List<ReceiptScanner.LineItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem() {
        items.add(new ReceiptScanner.LineItem("", 0.00));
        splits.add(defaultSplit());

        int row = items.size() - 1;
        fireTableRowsInserted(row, row);
    }

    public void removeItem(int row) {
        if (row < 0 || row >= items.size()) {
            return;
        }

        items.remove(row);
        splits.remove(row);
        fireTableRowsDeleted(row, row);
    }

    // Returns a copy of the split for the given row.
    public boolean[] getSplit(int row) {
        if (row < 0 || row >= splits.size()) {
            return new boolean[numPeople];
        }

        return splits.get(row).clone();
    }

    public void setSplit(int row, boolean[] split) {
        if (row < 0 || row >= splits.size()) {
            return;
        }

        splits.set(row, split.clone());
        fireTableCellUpdated(row, COLUMN_SPLIT);
    }

    // Call after a name change so preview reflects the new name. 
    public void refreshSplitColumn() {
        if (!items.isEmpty()) {
            fireTableChanged(new TableModelEvent(this, 0, items.size() - 1, COLUMN_SPLIT));
        }
    }
    // numPeople Getter
    public int getNumPeople() {
        return numPeople;
    }

    private boolean[] defaultSplit() {
        boolean[] split = new boolean[numPeople];
        Arrays.fill(split, true);
        return split;
    }

    private String splitPreview(int row) {
        boolean[] split = getSplit(row);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numPeople; i++) {
            if (split[i]) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(personNameProvider.apply(i));
            }
        }

        return sb.length() == 0 ? "Unassigned" : sb.toString();
    }

    @Override
    public int getRowCount() {
        return items.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        switch (column) {
            case COLUMN_NAME:
                return String.class;
            case COLUMN_PRICE:
                return Double.class;
            case COLUMN_SPLIT:
                return String.class;
            default:
                return Object.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == COLUMN_NAME || columnIndex == COLUMN_PRICE;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ReceiptScanner.LineItem item = items.get(rowIndex);

        switch (columnIndex) {
            case COLUMN_NAME:
                return item.name;
            case COLUMN_PRICE:
                return item.price;
            case COLUMN_SPLIT:
                return splitPreview(rowIndex);
            default:
                return null;
        }
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        ReceiptScanner.LineItem oldItem = items.get(row);

        String name = oldItem.name;
        double price = oldItem.price;

        if (column == COLUMN_NAME) {
            name = value.toString().trim();
        } else if (column == COLUMN_PRICE) {
            try {
                price = Double.parseDouble(value.toString().trim());
            } catch (NumberFormatException e) {
                return;
            }
        } else {
            return;
        }

        items.set(row, new ReceiptScanner.LineItem(name, price));
        fireTableCellUpdated(row, column);
    }
}