package com.receipt;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * A JTextField that displays light-gray hint text when it is empty.
 *
 * Also bundled is the table renderer/editor pair that give the Price column
 */
public class PlaceholderTextField extends JTextField {

    private final String placeholder;


    // CONSTRUCTORS
    public PlaceholderTextField(String placeholder) {
        super();
        this.placeholder = placeholder;
    }

    public PlaceholderTextField(String placeholder, int columns) {
        super(columns);
        this.placeholder = placeholder;
    }




    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (placeholder == null || !getText().isEmpty()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.GRAY);
        g2.setFont(getFont());

        int textBaseline = (getHeight() + g2.getFontMetrics().getAscent()) / 2 - 2;
        g2.drawString(placeholder, getInsets().left, textBaseline);
        g2.dispose();
    }

    /**
     * Renders a numeric table cell right-aligned, showing a gray "0.00"
     * placeholder when the value hasn't been set yet, and the real value
     * in normal text color otherwise.
     */
    public static class PriceCellRenderer extends DefaultTableCellRenderer {

        public PriceCellRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            double price = (value instanceof Double) ? (Double) value : 0.0;
            boolean isPlaceholder = price == 0.0;

            setText(isPlaceholder ? "0.00" : String.format("%.2f", price));

            if (!isSelected) {
                setForeground(isPlaceholder ? Color.GRAY : Color.BLACK);
            }

            return c;
        }
    }

    /**
     * Editor for a numeric table cell. Opens with an empty box (showing the
     * "0.00" hint via PlaceholderTextField) when the value hasn't been set
     * yet, instead of a literal "0.00" the user has to delete first. Leaving
     * it blank commits as 0.00.
     */
    public static class PriceCellEditor extends DefaultCellEditor {

        private final PlaceholderTextField field;

        public PriceCellEditor() {
            super(new PlaceholderTextField("0.00"));
            field = (PlaceholderTextField) getComponent();
            field.setHorizontalAlignment(JTextField.RIGHT);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            double price = (value instanceof Double) ? (Double) value : 0.0;
            field.setText(price == 0.0 ? "" : String.format("%.2f", price));
            return field;
        }

        @Override
        public Object getCellEditorValue() {
            String text = field.getText().trim();

            if (text.isEmpty()) {
                return 0.0;
            }

            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
    }
}