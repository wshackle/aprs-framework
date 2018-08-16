package aprs.cachedcomponents;

import aprs.misc.Utils;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.util.Vector;

public class CachedTable  {

    private final DefaultTableModel model;
    private final JTable jTable;
    private volatile Object data[][];
    private volatile int columnCount;
    private volatile int selectedRow;
    private final Class<?> columnClasses[];
    private final String columnNames[];


    @UIEffect
    @SuppressWarnings({"rawtypes","unchecked"})
    synchronized private void syncUiToCache() {
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

    private  final  TableModelListener tableModelListener = new TableModelListener() {
        @Override
        @UIEffect
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
        int newSelectedRows[] = new int[last - first];
        for (int i = 0; i < newSelectedRows.length; i++) {
            newSelectedRows[i] = i + first;
        }
        Utils.runOnDispatchThread(() -> setJTableSelectedInterval(first, last));
    }

    @UIEffect
    private void setJTableSelectedInterval(int newFirstSelectedRow, int newLastSelecteRow) {
        jTable.getSelectionModel().setSelectionInterval(newFirstSelectedRow, newLastSelecteRow);
    }

    @UIEffect
    public CachedTable(JTable jTable) {
        this((DefaultTableModel) jTable.getModel(),jTable);
    }

    public String getColumnName(int index) {
        return columnNames[index];
    }
    public Class<?> getColumnClass(int index) {
        return columnClasses[index];
    }

    @UIEffect
    @SuppressWarnings({"rawtypes","unchecked"})
    public CachedTable(DefaultTableModel model, JTable jTable) {
        this.jTable = jTable;
        this.model = model;
        selectedRow = jTable.getSelectedRow();

        jTable.getSelectionModel().addListSelectionListener(listSelectionListener);
        Object newData[][] = new Object[model.getRowCount()][];
        Vector v = model.getDataVector();
        columnCount = model.getColumnCount();
        columnClasses = new  Class[columnCount];
        columnNames = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnClasses[i] = model.getColumnClass(i);
            columnNames[i] = model.getColumnName(i);
        }
        for (int i = 0; i < newData.length; i++) {
            newData[i] = new Object[columnCount];
            Vector rowVector = (Vector) v.elementAt(i);
            System.arraycopy(rowVector.toArray(), 0, newData[i], 0, columnCount);
        }
        this.data = newData;
        model.addTableModelListener(tableModelListener);
    }

    public Object[][] getData() {
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
        Utils.runOnDispatchThread(() -> loadData(newData));
    }

    @Nullable
    public Object getValueAt(int row, int col) {
        return data[row][col];
    }

    public void setValueAt(Object value, int row, int col) {
        if (!value.equals(getValueAt(row, col))) {
            data[row][col] = value;
            Utils.runOnDispatchThread(() -> setModelValueAt(value, row, col));
        }
    }

    @UIEffect
    synchronized private void setModelValueAt(Object value, int row, int col) {
        model.removeTableModelListener(tableModelListener);
        model.setValueAt(value, row, col);
        model.addTableModelListener(tableModelListener);
        //syncDataUiToCache();
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
            Utils.runOnDispatchThread(() -> setModelRowCount(newCount));
        }
    }

    private Object [][] checkNewData(Object newData[][]) {
        for (int i = 0; i < newData.length; i++) {
            Object[] newRowData = newData[i];
            if(null == newRowData) {
                throw new IllegalArgumentException("newData["+i+"] == null");
            }
            if(newRowData.length != columnCount) {
                throw new IllegalArgumentException("newData["+i+"].length != columnCount : newData["+i+"].length = "+ newRowData.length+", columnCount="+columnCount);
            }
        }
       return newData;
    }

    synchronized public void addRow(Object[] rowData) {
        if(rowData.length != columnCount) {
            throw new IllegalArgumentException("rowData.length="+rowData.length+" but columnCount="+columnCount);
        }
        Object[][] newData = new Object[data.length + 1][];
        for (int i = 0; i < data.length && i < newData.length; i++) {
            newData[i] = new Object[columnCount];
            System.arraycopy(data[i], 0, newData[i], 0, columnCount);
        }
        newData[data.length] = new Object[columnCount];
        System.arraycopy(rowData, 0, newData[data.length], 0, columnCount);
        this.data = checkNewData(newData);
        Utils.runOnDispatchThread(() -> addRowToModel(rowData));
    }
    synchronized public void removeRow(int index) {

        Object[][] newData = new Object[data.length -1][];
        for (int i = 0; i < data.length && i < newData.length && i < index; i++) {
            newData[i] = new Object[columnCount];
            System.arraycopy(data[i], 0, newData[i], 0, columnCount);
        }
        for (int i = index; i < data.length && i < newData.length ; i++) {
            newData[i] = new Object[columnCount];
            System.arraycopy(data[i + 1], 0, newData[i], 0, columnCount);
        }
        this.data = checkNewData(newData);
        Utils.runOnDispatchThread(() -> removeRowFromModel(index));
    }

    @UIEffect
    synchronized private void setModelRowCount(int newRowCount) {
        model.removeTableModelListener(tableModelListener);
        model.setRowCount(newRowCount);
        model.addTableModelListener(tableModelListener);
        //syncDataUiToCache();
    }

    @UIEffect
    synchronized private void addRowToModel(Object[] data) {
        model.removeTableModelListener(tableModelListener);
        model.addRow(data);
        model.addTableModelListener(tableModelListener);
        //syncDataUiToCache();
    }

    @UIEffect
    synchronized private void removeRowFromModel(int index) {
        model.removeTableModelListener(tableModelListener);
        model.removeRow(index);
        model.addTableModelListener(tableModelListener);
        //syncDataUiToCache();
    }

    @UIEffect
    synchronized private void loadData(Object[][] newData) {
        model.removeTableModelListener(tableModelListener);
        model.setRowCount(0);
        for (int i = 0; i < data.length; i++) {
            model.addRow(newData[i]);
        }
        model.addTableModelListener(tableModelListener);
        //syncDataUiToCache();
    }


    @Nullable
    public JTable getjTable() {
        return jTable;
    }
}
