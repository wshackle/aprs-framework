package aprs.cachedcomponents;

import crcl.utils.XFutureVoid;
import java.util.Objects;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.util.Vector;

public class CachedTable extends CachedComponentBase {

    private final @Nullable
    DefaultTableModel model;

    private final @Nullable
    JTable jTable;

    private volatile @Nullable
    Object data[][];

    private volatile int columnCount;
    private volatile int selectedRow;
    private volatile Class<?> columnClasses[];
    private volatile String columnNames[];

    public CachedTable(Object[][] data, Class<?>[] columnClasses, String[] columnNames) {
        this.data = data;
        this.columnClasses = columnClasses;
        this.columnNames = columnNames;
        model = null;
        jTable = null;
        selectedRow = -1;
        columnCount = columnNames.length;
    }

    @UIEffect
    @SuppressWarnings({"rawtypes", "unchecked"})
    synchronized private void syncUiToCache() {
        DefaultTableModel model = this.model;
        if (null != model) {
            Object newData[][] = new Object[model.getRowCount()][];
            Vector v = model.getDataVector();
            columnCount = model.getColumnCount();
            for (int i = 0; i < newData.length; i++) {
                newData[i] = new Object[columnCount];
                Vector rowVector = (Vector) v.elementAt(i);
                System.arraycopy(rowVector.toArray(), 0, newData[i], 0, columnCount);
            }
            this.data = checkNewData(newData);
        }
    }

    private final TableModelListener tableModelListener = new TableModelListener() {
        @Override
        @UIEffect
        @SuppressWarnings({"initialization","nullness"})
        public void tableChanged(TableModelEvent e) {
            syncUiToCache();
        }
    };

    private final ListSelectionListener listSelectionListener = new ListSelectionListener() {
        @Override
        @UIEffect
        public void valueChanged(ListSelectionEvent e) {
            selectedRow = e.getFirstIndex();
        }
    };

    public int getSelectedRow() {
        return selectedRow;
    }

    public void setSelectionInterval(int first, int last) {
        this.selectedRow = first;
        runOnDispatchThread(() -> setJTableSelectedInterval(first, last));
    }

    @UIEffect
    private void setJTableSelectedInterval(int newFirstSelectedRow, int newLastSelecteRow) {
        if (null != jTable) {
            jTable.getSelectionModel().setSelectionInterval(newFirstSelectedRow, newLastSelecteRow);
        }
    }

    @UIEffect
    public CachedTable(JTable jTable) {
        this((DefaultTableModel) jTable.getModel(), jTable);
    }

    public String getColumnName(int index) {
        return columnNames[index];
    }

    public Class<?> getColumnClass(int index) {
        return columnClasses[index];
    }

    @UIEffect
    @SuppressWarnings({"rawtypes", "unchecked", "initialization","nullness"})
    public CachedTable(DefaultTableModel model, JTable jTable) {
        this.jTable = jTable;
        this.model = model;
        selectedRow = jTable.getSelectedRow();

        jTable.getSelectionModel().addListSelectionListener(listSelectionListener);
        Object newData[][] = new Object[model.getRowCount()][];
        Vector v = model.getDataVector();
        columnCount = model.getColumnCount();
        syncColumnNamesClasses(model);
        for (int i = 0; i < newData.length; i++) {
            newData[i] = new Object[columnCount];
            Vector rowVector = (Vector) v.elementAt(i);
            System.arraycopy(rowVector.toArray(), 0, newData[i], 0, columnCount);
        }
        this.data = newData;
        model.addTableModelListener(tableModelListener);
    }

    @SuppressWarnings({"rawtypes", "unchecked", "initialization"})
    private void syncColumnNamesClasses(DefaultTableModel model1) {
        columnClasses = new Class[columnCount];
        columnNames = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnClasses[i] = model1.getColumnClass(i);
            columnNames[i] = model1.getColumnName(i);
        }
    }

    public @Nullable
    Object[][] getData() {
        return data;
    }

    public int getRowCount() {
        return data.length;
    }

    public int getColumnCount() {
        return columnCount;
    }

    synchronized public void setData(Object[][] newData) {
        this.data = checkNewData(newData);
        if (null != model) {
            runOnDispatchThread(() -> loadData(newData));
        }
    }

    public @Nullable
    Object getValueAt(int row, int col) {
        return data[row][col];
    }

    public XFutureVoid setValueAt(@Nullable Object value, int row, int col) {
        if (!Objects.equals(value, getValueAt(row, col))) {
            data[row][col] = value;
            if (null != model) {
                return runOnDispatchThread(() -> setModelValueAt(value, row, col));
            }
        }
        return XFutureVoid.completedFuture();
    }

    @UIEffect
    synchronized private void setModelValueAt(@Nullable Object value, int row, int col) {
        if (null != model) {
            model.removeTableModelListener(tableModelListener);
            if (null != value) {
                model.setValueAt(value, row, col);
            } else {
                clearModelValueAt(row, col);
            }
            model.addTableModelListener(tableModelListener);
        }
        //syncDataUiToCache();
    }

    @SuppressWarnings("nullness")
    private void clearModelValueAt(int row, int col) {
        model.setValueAt(null, row, col);
    }

    synchronized public void setRowCount(int newCount) {
        if (newCount != data.length) {
            Object[][] newData = new Object[newCount][];
            for (int i = 0; i < data.length && i < newData.length; i++) {
                newData[i] = new Object[columnCount];
                System.arraycopy(data[i], 0, newData[i], 0, columnCount);
            }
            for (int i = data.length; i < newData.length; i++) {
                newData[i] = new Object[columnCount];
            }
            this.data = checkNewData(newData);
            if (null != model) {
                runOnDispatchThread(() -> setModelRowCount(newCount));
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    synchronized public void setRowColumnCount(int newRowCount, int newColumnCount) {
        if (newRowCount != data.length || newColumnCount != columnCount) {
            int minColumnCount = Math.min(columnCount, newColumnCount);
            Object[][] newData = new Object[newRowCount][];
            for (int i = 0; i < data.length && i < newData.length; i++) {
                newData[i] = new Object[newColumnCount];
                System.arraycopy(data[i], 0, newData[i], 0, minColumnCount);
            }
            for (int i = data.length; i < newData.length; i++) {
                newData[i] = new Object[newColumnCount];
            }
            String newColumnNames[] = new String[newColumnCount];
            System.arraycopy(columnNames, 0, newColumnNames, 0, minColumnCount);
            Class<?> newColumnClasses[] = new Class[newColumnCount];
            System.arraycopy(columnClasses, 0, newColumnClasses, 0, minColumnCount);
            for (int i = columnCount; i < newColumnClasses.length; i++) {
                newColumnClasses[i] = Object.class;
                newColumnNames[i] = "";
            }
            this.columnClasses = newColumnClasses;
            this.columnNames = newColumnNames;
            this.columnCount = newColumnCount;
            this.data = checkNewData(newData);
            if (null != model) {
                runOnDispatchThread(() -> setModelRowColumnCount(newRowCount, newColumnCount));
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    synchronized public void addColumn(String columnName) {
        int newColumnCount = columnCount + 1;
        Object[][] newData = new Object[data.length][];
        for (int i = 0; i < data.length && i < newData.length; i++) {
            newData[i] = new Object[newColumnCount];
            System.arraycopy(data[i], 0, newData[i], 0, data.length);
        }
        for (int i = data.length; i < newData.length; i++) {
            newData[i] = new Object[newColumnCount];
        }
        String newColumnNames[] = new String[newColumnCount];
        System.arraycopy(columnNames, 0, newColumnNames, 0, columnCount);
        Class<?> newColumnClasses[] = new Class[newColumnCount];
        System.arraycopy(columnClasses, 0, newColumnClasses, 0, columnCount);
        newColumnClasses[columnCount] = Object.class;
        newColumnNames[columnCount] = columnName;

        this.columnClasses = newColumnClasses;
        this.columnNames = newColumnNames;
        this.columnCount = newColumnCount;
        this.data = checkNewData(newData);
        if (null != model) {
            runOnDispatchThread(() -> modelAddColumn(columnName));
        }
    }

    private @Nullable
    Object[][] checkNewData(@Nullable Object newData[][]) {
        for (int i = 0; i < newData.length; i++) {
            @Nullable
            Object[] newRowData = newData[i];
            if (null == newRowData) {
                throw new IllegalArgumentException("newData[" + i + "] == null");
            }
            if (newRowData.length != columnCount) {
                throw new IllegalArgumentException("newData[" + i + "].length != columnCount : newData[" + i + "].length = " + newRowData.length + ", columnCount=" + columnCount);
            }
        }
        return newData;
    }

    synchronized public void addRow(@Nullable Object[] rowData) {
        if (rowData.length != columnCount) {
            throw new IllegalArgumentException("rowData.length=" + rowData.length + " but columnCount=" + columnCount);
        }
        Object[][] newData = new Object[data.length + 1][];
        for (int i = 0; i < data.length && i < newData.length; i++) {
            newData[i] = new Object[columnCount];
            System.arraycopy(data[i], 0, newData[i], 0, columnCount);
        }
        newData[data.length] = new Object[columnCount];
        System.arraycopy(rowData, 0, newData[data.length], 0, columnCount);
        this.data = checkNewData(newData);
        if (null != model) {
            runOnDispatchThread(() -> addRowToModel(rowData));
        }
    }

    synchronized public void removeRow(int index) {

        Object[][] newData = new Object[data.length - 1][];
        for (int i = 0; i < data.length && i < newData.length && i < index; i++) {
            newData[i] = new Object[columnCount];
            System.arraycopy(data[i], 0, newData[i], 0, columnCount);
        }
        for (int i = index; i < data.length && i < newData.length; i++) {
            newData[i] = new Object[columnCount];
            System.arraycopy(data[i + 1], 0, newData[i], 0, columnCount);
        }
        this.data = checkNewData(newData);
        if (null != model) {
            runOnDispatchThread(() -> removeRowFromModel(index));
        }
    }

    @UIEffect
    synchronized private void setModelRowCount(int newRowCount) {
        if (null != model) {
            model.removeTableModelListener(tableModelListener);
            model.setRowCount(newRowCount);
            model.addTableModelListener(tableModelListener);
        }
        //syncDataUiToCache();
    }

    @UIEffect
    synchronized private void setModelRowColumnCount(int newRowCount, int newColumnCount) {
        if (null != model) {
            model.removeTableModelListener(tableModelListener);
            model.setRowCount(newRowCount);
            model.setColumnCount(newColumnCount);
            syncColumnNamesClasses(model);
            model.addTableModelListener(tableModelListener);
        }
        //syncDataUiToCache();
    }

    @UIEffect
    synchronized private void modelAddColumn(String columnName) {
        if (null != model) {
            if (null != tableModelListener) {
                model.removeTableModelListener(tableModelListener);
            }
            model.addColumn(columnName);
            syncColumnNamesClasses(model);
            if (null != tableModelListener) {
                model.addTableModelListener(tableModelListener);
            }
        }
        //syncDataUiToCache();
    }

    @UIEffect
    @SuppressWarnings("nullness")
    synchronized private void addRowToModel(@Nullable Object[] data) {
        if (null != model) {
            if (null != tableModelListener) {
                model.removeTableModelListener(tableModelListener);
            }
            model.addRow(data);
            if (null != tableModelListener) {
                model.addTableModelListener(tableModelListener);
            }
        }
        //syncDataUiToCache();
    }

    @UIEffect
    synchronized private void removeRowFromModel(int index) {
        if (null != model) {
            if (null != tableModelListener) {
                model.removeTableModelListener(tableModelListener);
            }
            model.removeRow(index);
            if (null != tableModelListener) {
                model.addTableModelListener(tableModelListener);
            }
        }
        //syncDataUiToCache();
    }

    @UIEffect
    synchronized private void loadData(Object[][] newData) {
        if (null != model) {
            if (null != tableModelListener) {
                model.removeTableModelListener(tableModelListener);
            }
            if (newData.length == model.getRowCount()) {
                for (int i = 0; i < newData.length; i++) {
                    Object[] objects = newData[i];
                    for (int j = 0; j < objects.length; j++) {
                        Object object = objects[j];
                        model.setValueAt(object, i, j);
                    }
                }
            } else {
                model.setRowCount(0);
                for (int i = 0; i < newData.length; i++) {
                    model.addRow(newData[i]);
                }
            }
            if (null != tableModelListener) {
                model.addTableModelListener(tableModelListener);
            }
        }
        //syncDataUiToCache();
    }

    public @Nullable
    JTable getjTable() {
        return jTable;
    }
}
