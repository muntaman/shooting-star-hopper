package com.shootingstarhopper;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import net.runelite.client.ui.PluginPanel;

public class ShootingStarPanel extends PluginPanel
{
    private static final Pattern TIME_MINUTES_PATTERN = Pattern.compile("(\\d+)m");
    private static final DateTimeFormatter UPDATED_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    private final StarTableModel tableModel = new StarTableModel();
    private final JTable table = new JTable(tableModel);
    private final JLabel statusLabel = new JLabel("Not refreshed yet");
    private final JButton refreshButton = new JButton("Refresh");

    private final Consumer<ShootingStar> hopAction;
    private final Runnable refreshAction;

    public ShootingStarPanel(
        ShootingStarHopperConfig config,
        Consumer<ShootingStar> copyAction,
        Consumer<ShootingStar> hopAction,
        Runnable refreshAction
    )
    {
        super(false);
        this.hopAction = hopAction;
        this.refreshAction = refreshAction;

        setLayout(new BorderLayout(0, 8));
        buildTable();
        buildActions();
    }

    public void updateStars(List<ShootingStar> stars, Instant updatedAt)
    {
        tableModel.setStars(stars);
        statusLabel.setText(stars.size() + " stars - updated " + UPDATED_FORMATTER.format(updatedAt));
        refreshButton.setEnabled(true);
    }

    public void showRefreshing()
    {
        statusLabel.setText("Refreshing...");
        refreshButton.setEnabled(false);
    }

    public void showError(String message)
    {
        statusLabel.setText("Error: " + message);
        refreshButton.setEnabled(true);
    }

    public void showStatus(String message)
    {
        statusLabel.setText(message);
        refreshButton.setEnabled(true);
    }

    public void refreshActionMode()
    {
        revalidate();
        repaint();
    }

    private void buildTable()
    {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(false);

        TableRowSorter<StarTableModel> sorter = new TableRowSorter<>(tableModel);
        sorter.setComparator(0, Comparator.comparingInt(value -> (Integer) value));
        sorter.setComparator(1, Comparator.comparingInt(value -> (Integer) value));
        sorter.setComparator(2, Comparator.comparing(value -> value.toString(), String.CASE_INSENSITIVE_ORDER));
        sorter.setComparator(3, Comparator.comparingInt(value -> parseMinutes(value.toString())));
        table.setRowSorter(sorter);

        TableColumnModel columns = table.getColumnModel();
        columns.getColumn(0).setPreferredWidth(38);
        columns.getColumn(0).setMaxWidth(48);
        columns.getColumn(1).setPreferredWidth(54);
        columns.getColumn(1).setMaxWidth(64);
        columns.getColumn(2).setPreferredWidth(162);
        columns.getColumn(3).setPreferredWidth(48);
        columns.getColumn(3).setMaxWidth(60);
        columns.getColumn(1).setCellRenderer(new HopButtonRenderer());
        columns.getColumn(1).setCellEditor(new HopButtonEditor(table, tableModel, hopAction));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(225, 420));
        add(scrollPane, BorderLayout.CENTER);
    }

    private void buildActions()
    {
        JPanel top = new JPanel(new BorderLayout());
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        top.add(statusLabel, BorderLayout.CENTER);
        top.add(refreshButton, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        refreshButton.addActionListener(event -> refreshAction.run());
    }

    private static int parseMinutes(String value)
    {
        Matcher matcher = TIME_MINUTES_PATTERN.matcher(value == null ? "" : value);
        if (!matcher.find())
        {
            return Integer.MAX_VALUE;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static class StarTableModel extends AbstractTableModel
    {
        private final String[] columns = {"Tier", "World", "Location", "Time"};
        private List<ShootingStar> stars = new ArrayList<>();

        void setStars(List<ShootingStar> stars)
        {
            this.stars = new ArrayList<>(stars);
            fireTableDataChanged();
        }

        Optional<ShootingStar> getStar(int row)
        {
            if (row < 0 || row >= stars.size())
            {
                return Optional.empty();
            }
            return Optional.of(stars.get(row));
        }

        @Override
        public int getRowCount()
        {
            return stars.size();
        }

        @Override
        public int getColumnCount()
        {
            return columns.length;
        }

        @Override
        public String getColumnName(int column)
        {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            ShootingStar star = stars.get(rowIndex);
            switch (columnIndex)
            {
                case 0:
                    return star.getTier();
                case 1:
                    return star.getWorld();
                case 2:
                    return star.getLocation();
                case 3:
                    return star.getTimeAgo();
                default:
                    return "";
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return columnIndex == 1;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            if (columnIndex == 0 || columnIndex == 1)
            {
                return Integer.class;
            }
            return String.class;
        }
    }

    private static class HopButtonRenderer extends JButton implements TableCellRenderer
    {
        HopButtonRenderer()
        {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
        )
        {
            setText(String.valueOf(value));
            setToolTipText("Hop to world " + value);
            return this;
        }
    }

    private static class HopButtonEditor extends AbstractCellEditor implements TableCellEditor
    {
        private final JTable table;
        private final StarTableModel tableModel;
        private final Consumer<ShootingStar> hopAction;
        private final JButton button = new JButton();
        private int editingRow = -1;

        HopButtonEditor(JTable table, StarTableModel tableModel, Consumer<ShootingStar> hopAction)
        {
            this.table = table;
            this.tableModel = tableModel;
            this.hopAction = hopAction;
            button.addActionListener(event ->
            {
                int modelRow = table.convertRowIndexToModel(editingRow);
                tableModel.getStar(modelRow).ifPresent(hopAction);
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(
            JTable table,
            Object value,
            boolean isSelected,
            int row,
            int column
        )
        {
            editingRow = row;
            button.setText(String.valueOf(value));
            button.setToolTipText("Hop to world " + value);
            return button;
        }

        @Override
        public Object getCellEditorValue()
        {
            return "";
        }
    }
}
