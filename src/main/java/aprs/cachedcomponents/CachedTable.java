package aprs.cachedcomponents;

import aprs.actions.executor.ExecutorJPanel;
import crcl.utils.CRCLUtils;
import crcl.utils.XFutureVoid;
import java.lang.reflect.InvocationTargetException;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

public class CachedTable extends CachedComponentBase {

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
        jTable = null;
        selectedRow = -1;
        columnCount = columnNames.length;
        this.editable = false;
    }

    @UIEffect
    @SuppressWarnings({"rawtypes", "unchecked"})
    synchronized private void syncUiToCache() {
        DefaultTableModel model = (DefaultTableModel) this.jTable.getModel();
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
        @SuppressWarnings({"initialization", "nullness"})
        public void tableChanged(TableModelEvent e) {
            if (CachedTable.this.editable) {
                syncUiToCache();
            }
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
        this((DefaultTableModel) jTable.getModel(), jTable, false);
    }

    @UIEffect
    public CachedTable(JTable jTable, boolean editable) {
        this((DefaultTableModel) jTable.getModel(), jTable, editable);
    }

    public String getColumnName(int index) {
        return columnNames[index];
    }

    public Class<?> getColumnClass(int index) {
        return columnClasses[index];
    }

    @UIEffect
    @SuppressWarnings({"rawtypes", "unchecked", "initialization", "nullness"})
    public CachedTable(DefaultTableModel model, JTable jTable, boolean editable) {
        this.jTable = jTable;
        this.editable = editable;
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
        if (editable) {
            model.addTableModelListener(tableModelListener);
        }
        setEditableInternal(editable);
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
        if (null != jTable) {
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
            if (null != jTable) {
                return runOnDispatchThread(() -> setModelValueAt(value, row, col));
            }
        }
        return XFutureVoid.completedFuture();
    }

    @UIEffect
    synchronized private void setModelValueAt(@Nullable Object value, int row, int col) {
        DefaultTableModel model = (DefaultTableModel) this.jTable.getModel();
        if (null != jTable) {
//            model.removeTableModelListener(tableModelListener);
            int modelIndex = jTable.convertRowIndexToView(row);
            if (null != value) {
                jTable.setValueAt(value, modelIndex, col);
            } else {
                clearModelValueAt(modelIndex, col);
            }
//            model.addTableModelListener(tableModelListener);
        }
        //syncDataUiToCache();
    }

    @SuppressWarnings("nullness")
    private void clearModelValueAt(int row, int col) {
        jTable.setValueAt(null, row, col);
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
            if (null != jTable) {
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
            if (null != jTable) {
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
        if (null != jTable) {
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
        if (null != jTable) {
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
        if (null != jTable) {
            runOnDispatchThread(() -> removeRowFromModel(index));
        }
    }

    private boolean editable;

    /**
     * Get the value of editable
     *
     * @return the value of editable
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Set the value of editable
     *
     * @param editable new value of editable
     */
    public void setEditable(boolean editable) throws InterruptedException, InvocationTargetException {
        if (null == this.jTable) {
            this.editable = false;
            return;
        }
        if (editable != this.editable) {
            if (!SwingUtilities.isEventDispatchThread() && !CRCLUtils.isGraphicsEnvironmentHeadless()) {
                javax.swing.SwingUtilities.invokeAndWait(() -> {
                    setEditableInternal(editable);
                });
            } else {
                setEditableInternal(editable);
            }
        }
    }

    private void setEditableInternal(boolean editable1) throws RuntimeException {
        try {
            if (null == this.jTable) {
                this.editable = false;
                return;
            }
            this.editable = editable1;
            final TableModel oldModel = jTable.getModel();
            final TableColumnModel oldColumnModel = jTable.getColumnModel();
            oldModel.removeTableModelListener(tableModelListener);
            
            final Object[][] data = new Object[oldModel.getRowCount()][];
            final int columnCount = oldModel.getColumnCount();
            boolean newModelNeeded = false;
            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < columnCount; j++) {
                    if(oldModel.isCellEditable(i, j) != editable1) {
                        newModelNeeded = true;
                        break;
                    }
                }
            }
            if(!newModelNeeded) {
                return;
            }
            for (int i = 0; i < data.length; i++) {
                data[i] = new Object[columnCount];
                for (int j = 0; j < columnCount; j++) {
                    data[i][j] = oldModel.getValueAt(i, j);
                }
            }
            final String colNames[] = new String[columnCount];
            final Class<?> colClasses[] = new Class[columnCount];
            final boolean editables[] = new boolean[columnCount];
            final TableColumn oldColumns[] = new TableColumn[columnCount];
            for (int i = 0; i < colNames.length; i++) {
                colNames[i] = oldModel.getColumnName(i);
                colClasses[i] = oldModel.getColumnClass(i);
                editables[i] = editable1;
                oldColumns[i] = oldColumnModel.getColumn(i);
                
            }
            final TableModel newModel = new javax.swing.table.DefaultTableModel(
                    data,
                    colNames
            ) {
                Class[] types = colClasses;
                boolean[] canEdit = editables;

                public Class getColumnClass(int columnIndex) {
                    return types[columnIndex];
                }

                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return canEdit[columnIndex];
                }
            };
            
            jTable.setModel(newModel);
            for (int i = 0; i < oldColumns.length; i++) {
                final TableColumn oldColumnI = oldColumns[i];
                final TableColumn newColumnI = jTable.getColumnModel().getColumn(i);
                newColumnI.setCellEditor(oldColumnI.getCellEditor());
                newColumnI.setCellRenderer(oldColumnI.getCellRenderer());
                newColumnI.setHeaderRenderer(oldColumnI.getHeaderRenderer());
                newColumnI.setHeaderValue(oldColumnI.getHeaderValue());
                newColumnI.setMaxWidth(oldColumnI.getMaxWidth());
                newColumnI.setMinWidth(oldColumnI.getMinWidth());
                newColumnI.setPreferredWidth(oldColumnI.getPreferredWidth());
                newColumnI.setResizable(oldColumnI.getResizable());
                newColumnI.setWidth(oldColumnI.getWidth());
            }
            this.data = data;
            if (editable1) {
                jTable.getModel().addTableModelListener(tableModelListener);
            }
        } catch (Exception e) {
            Logger.getLogger(CachedTable.class.getName()).log(Level.SEVERE, "setEditable(" + editable1 + ")", e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @UIEffect
    synchronized private void setModelRowCount(int newRowCount) {
        if (null != this.jTable) {
            DefaultTableModel model = (DefaultTableModel) this.jTable.getModel();
            if (null != model) {
                model.removeTableModelListener(tableModelListener);
                model.setRowCount(newRowCount);
                if (this.editable) {
                    model.addTableModelListener(tableModelListener);
                }
            }
        }
        //syncDataUiToCache();
    }

    @UIEffect
    synchronized private void setModelRowColumnCount(int newRowCount, int newColumnCount) {
        if (null != this.jTable) {
            DefaultTableModel model = (DefaultTableModel) this.jTable.getModel();
            if (null != model) {
                model.removeTableModelListener(tableModelListener);
                model.setRowCount(newRowCount);
                model.setColumnCount(newColumnCount);
                syncColumnNamesClasses(model);
                if (editable) {
                    model.addTableModelListener(tableModelListener);
                }
            }
        }
        //syncDataUiToCache();
    }

    @UIEffect
    synchronized private void modelAddColumn(String columnName) {
        if (null != this.jTable) {
            DefaultTableModel model = (DefaultTableModel) this.jTable.getModel();
            if (null != model) {
                if (null != tableModelListener) {
                    model.removeTableModelListener(tableModelListener);
                }
                model.addColumn(columnName);
                syncColumnNamesClasses(model);
                if (null != tableModelListener && this.editable) {
                    model.addTableModelListener(tableModelListener);
                }
            }
        }
        //syncDataUiToCache();
    }

    @UIEffect
    @SuppressWarnings("nullness")
    synchronized private void addRowToModel(@Nullable Object[] data) {
        if (null != this.jTable) {
            DefaultTableModel model = (DefaultTableModel) this.jTable.getModel();
            if (null != model) {
                if (null != tableModelListener) {
                    model.removeTableModelListener(tableModelListener);
                }
                model.addRow(data);
                if (null != tableModelListener && editable) {
                    model.addTableModelListener(tableModelListener);
                }
            }
        }
        //syncDataUiToCache();
    }

    @UIEffect
    synchronized private void removeRowFromModel(int index) {
        if (null != this.jTable) {
            DefaultTableModel model = (DefaultTableModel) this.jTable.getModel();
            if (null != model) {
                if (null != tableModelListener) {
                    model.removeTableModelListener(tableModelListener);
                }
                model.removeRow(index);
                if (null != tableModelListener && this.editable) {
                    model.addTableModelListener(tableModelListener);
                }
            }
        }
        //syncDataUiToCache();
    }

    @UIEffect
    synchronized private void loadData(Object[][] newData) {
        if (null != this.jTable) {
            DefaultTableModel model = (DefaultTableModel) this.jTable.getModel();
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
                if (null != tableModelListener && this.editable) {
                    model.addTableModelListener(tableModelListener);
                }
            }
        }
        //syncDataUiToCache();
    }

    public @Nullable
    JTable getjTable() {
        return jTable;
    }
}
