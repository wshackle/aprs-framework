/*
 * This software is public domain software, however it is preferred
 * that the following disclaimers be attached.
 * Software Copyright/Warranty Disclaimer
 * 
 * This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of their
 * official duties. Pursuant to title 17 Section 105 of the United States
 * Code this software is not subject to copyright protection and is in the
 * public domain.
 * 
 * This software is experimental. NIST assumes no responsibility whatsoever 
 * for its use by other parties, and makes no guarantees, expressed or 
 * implied, about its quality, reliability, or any other characteristic. 
 * We would appreciate acknowledgement if the software is used. 
 * This software can be redistributed and/or modified freely provided 
 * that any derivative works bear some notice that they are derived from it, 
 * and any modified versions bear some notice that they have been modified.
 * 
 *  See http://www.copyright.gov/title17/92chap1.html#105
 * 
 */
package aprs.database.explore;

import aprs.system.AprsSystem;
import aprs.misc.Utils;
import aprs.database.DbSetup;
import aprs.database.DbSetupBuilder;
import aprs.database.DbSetupListener;
import aprs.database.DbType;
import static aprs.misc.AprsCommonLogger.println;
//import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import static aprs.misc.Utils.autoResizeTableColWidths;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings({"guieffect","serial"})
class ExploreGraphDbJPanel extends javax.swing.JPanel implements DbSetupListener {

    private final TableModelListener nodeTableModelListener;

    /**
     * Creates new form ExplorteGraphDbJPanel
     */
    @SuppressWarnings({"nullness","initialization"})
    @UIEffect
    public ExploreGraphDbJPanel() {
        initComponents();
        jTableNodes.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updatePropsRels();
            }
        });
        nodeTableModelListener = new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() != TableModelEvent.UPDATE) {
                    return;
                }
                int col = e.getColumn();
                if (col == TableModelEvent.ALL_COLUMNS) {
                    return;
                }
                if (col < 2) {
                    return;
                }
                int row = e.getFirstRow();
                if (row != e.getLastRow()) {
                    return;
                }
                String colName = jTableNodes.getColumnName(col);
                Object oValue = jTableNodes.getValueAt(row, col);
                if (oValue == null) {
                    return;
                }
                String sValue = oValue.toString();
                int id = (int) jTableNodes.getValueAt(row, 0);
                Object olabel = jTableNodes.getValueAt(row, 1);
                if (olabel == null) {
                    return;
                }
                String label = olabel.toString();
                if (olabel instanceof List) {
                    label = ((List) olabel).get(0).toString();
                }
                if (label.startsWith("[")) {
                    label = label.substring(1);
                }
                int lbindex = label.indexOf(']');
                if (lbindex > 0) {
                    label = label.substring(0, lbindex);
                }
                int lcindex = label.indexOf(',');
                if (lcindex > 0) {
                    label = label.substring(0, lcindex);
                }
                String name = (String) jTableNodes.getValueAt(row, 2);
                setDatabaseItemProperty(id, label, name, colName, sValue);
            }
        };
        jTableNodes.getModel().addTableModelListener(nodeTableModelListener);
    }

    private void setDatabaseItemProperty(int id, String label, String name, String propName, String value) {
        try {
            if (null == connection) {
                throw new IllegalStateException("connection is null");
            }
            String setDatabaseItemPropertyStatementString
                    = "MATCH (n:" + label + " { name: '" + name + "' })\n"
                    + "WHERE ID(n) = " + id + "\n"
                    + "SET " + propName + " = '" + value + "'\n"
                    + "RETURN n";
            println("setDatabaseItemPropertyStatementString = \n" + setDatabaseItemPropertyStatementString);
            if (JOptionPane.showConfirmDialog(this, "Set database property with : \n" + setDatabaseItemPropertyStatementString) == JOptionPane.YES_OPTION) {
                PreparedStatement setDatabaseItemPropertyStatement
                        = connection.prepareStatement(setDatabaseItemPropertyStatementString);
                setDatabaseItemPropertyStatement.execute();
                println("done");
            }
        } catch (SQLException ex) {
            Logger.getLogger(ExploreGraphDbJPanel.class.getName()).log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> objectToMap(Object o) {
        try {
            if (o instanceof Map) {
                return (Map<String, ?>) o;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return Collections.singletonMap("", o);
    }

    private void updatePropsRels() {
        try {
            if (null == connection) {
                throw new IllegalStateException("connection is null");
            }
            int index = this.jTableNodes.getSelectedRow();
            if (jTableNodes.getColumnCount() < 1) {
                return;
            }
            if (index < 0 || index >= this.jTableNodes.getRowCount()) {
                return;
            }
            String s = Objects.toString(this.jTableNodes.getValueAt(index, 0));
            if (!jTextFieldSelectedNodeId.getText().equals(s)) {
                jTextFieldSelectedNodeId.setText(s);
            }
            String col0Head = this.jTableNodes.getColumnName(0);
            final PreparedStatement outStatement;
            if (col0Head.endsWith(".name")) {
                String getRelationShipsOutQuery
                        = "MATCH (n {name:{1} }) - [relationship] -> (to) RETURN type(relationship),relationship,id(to),labels(to),to";
                println("getRelationShipsOutQuery = " + getRelationShipsOutQuery);
                outStatement
                        = connection.prepareStatement(getRelationShipsOutQuery);
                outStatement.setString(1, s);
            } else {
                String getRelationShipsOutQuery
                        = "MATCH (n) - [relationship] -> (to) WHERE ID(n) = " + s + " RETURN type(relationship),relationship,id(to),labels(to),to";
                println("getRelationShipsOutQuery = " + getRelationShipsOutQuery);
                outStatement
                        = connection.prepareStatement(getRelationShipsOutQuery);
                if (jTableNodes.getColumnCount() > 1) {
                    String col1Head = this.jTableNodes.getColumnName(1);
                    if (col1Head.endsWith(".name")) {
                        String name = Objects.toString(this.jTableNodes.getValueAt(index, 1));
                        if (!jTextFieldSelectedNodeName.getText().equals(name)) {
                            jTextFieldSelectedNodeName.setText(name);
                        }
                    }
                }
            }
            DefaultTableModel model = new DefaultTableModel();
            List<Object[]> resultList = new ArrayList<>();
            final int outStatementColCount;
            try (ResultSet rs = outStatement.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                outStatementColCount = meta.getColumnCount();
                for (int i = 1; i < meta.getColumnCount(); i++) {
                    String colName = meta.getColumnName(i);
                    model.addColumn(colName);
                }
                int row = 0;
                List<String> l = new ArrayList<>();
                while (rs.next()) {
                    Object ao[] = new Object[meta.getColumnCount()];
                    for (int i = 0; i < ao.length; i++) {
                        Object oi = rs.getObject(i + 1);
                        if (oi != null) {
                            ao[i] = oi;
                        }
                    }
                    resultList.add(ao);
                }
            }

            TreeSet<String> toKeys = new TreeSet<>();
            for (Object[] ao : resultList) {
                Object lastObject = ao[ao.length - 1];
                if (lastObject instanceof Map) {
                    toKeys.addAll(objectToMap(lastObject).keySet());
                }
            }
            List<String> keyList = new ArrayList<>(toKeys);
            keyList.remove("name");
            Collections.sort(keyList);
            keyList.add(0, "name");
            for (String key : keyList) {
                model.addColumn("to." + key);
            }
            final int outTableWidth = outStatementColCount - 1 + keyList.size();
            for (Object[] ao : resultList) {
                Object newArray[] = new Object[outTableWidth];
                System.arraycopy(ao, 0, newArray, 0, ao.length - 1);
                for (int i = ao.length - 1; i < outTableWidth; i++) {
                    Map<String, ?> map = objectToMap(ao[ao.length - 1]);
                    Object obj = map.get(keyList.get(i - (ao.length - 1)));
                    if (null != obj) {
                        newArray[i] = obj;
                    }
                }
                model.addRow(newArray);
            }
            jTableRelationshipsOut.setModel(model);
            autoResizeTableColWidths(jTableRelationshipsOut);
            final PreparedStatement inStatement;
            if (col0Head.endsWith(".name")) {
                inStatement = connection.prepareStatement("MATCH (from) - [relationship] -> (n {name:{1} })  RETURN type(relationship),relationship,id(from),labels(from),from");
                inStatement.setString(1, s);
            } else {
                inStatement = connection.prepareStatement("MATCH (from) - [relationship] -> (n) WHERE ID(n) = " + s + " RETURN type(relationship),relationship,id(from),labels(from),from");
            }

            model = new DefaultTableModel();
            resultList = new ArrayList<>();
            final int inStatementColCount;
            try (ResultSet rs = inStatement.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                inStatementColCount = meta.getColumnCount();
                for (int i = 1; i <= meta.getColumnCount() - 1; i++) {
                    String colName = meta.getColumnName(i);
//                    println("colName = " + colName);
//                    String columnClassName = meta.getColumnClassName(i);
//                    println("columnClassName = " + columnClassName);
//                    String columnType = meta.getColumnTypeName(i);
//                    println("columnType = " + columnType);
                    model.addColumn(colName);
                }
                int row = 0;
                List<String> l = new ArrayList<>();
                while (rs.next()) {
                    Object ao[] = new Object[meta.getColumnCount()];
                    for (int i = 0; i < ao.length; i++) {
                        Object obj = rs.getObject(i + 1);
                        if (null != obj) {
                            ao[i] = obj;
                        }
                    }
                    resultList.add(ao);
                }
            }
            TreeSet<String> fromKeys = new TreeSet<>();
            for (Object[] ao : resultList) {
                Object lastObject = ao[ao.length - 1];
                if (lastObject instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, ?> map = (Map<String, ?>) lastObject;
                    fromKeys.addAll(map.keySet());
                }
            }
            keyList = new ArrayList<>(fromKeys);
            keyList.remove("name");
            Collections.sort(keyList);
            keyList.add(0, "name");
            for (String key : keyList) {
                model.addColumn("from." + key);
            }
            final int inTableWidth = outStatementColCount - 1 + keyList.size();
            for (Object[] ao : resultList) {
                Object newArray[] = new Object[inTableWidth];
                System.arraycopy(ao, 0, newArray, 0, ao.length - 1);
                for (int i = ao.length - 1; i < inTableWidth; i++) {
                    Object lastObject = ao[ao.length - 1];
                    if (lastObject instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) lastObject;
                        Object obj = map.get(keyList.get(i - (ao.length - 1)));
                        if (null != obj) {
                            newArray[i] = obj;
                        }
                    }
                }
                model.addRow(newArray);
            }
            jTableRelationshipsIn.setModel(model);
            autoResizeTableColWidths(jTableRelationshipsIn);
//            jTableNodes.setModel(model);

        } catch (SQLException ex) {
            Logger.getLogger(ExploreGraphDbJPanel.class.getName()).log(Level.SEVERE, "", ex);
            jTextAreaErrors.setText(ex.toString());
        }
    }

    private @MonotonicNonNull
    Connection connection = null;

    private boolean sharedConnection;

    /**
     * Set the value of sharedConnection
     *
     * @param sharedConnection new value of sharedConnection
     */
    public void setSharedConnection(boolean sharedConnection) {
        this.sharedConnection = sharedConnection;
    }

    @SafeEffect
    public void closeConnection() throws SQLException {
        if (null != this.connection && !sharedConnection) {
            this.connection.close();
        }
    }

    /**
     * Set the value of connection
     *
     * @param connection new value of connection
     */
    public void setConnection(Connection connection) {
        try {
            this.closeConnection();
        } catch (SQLException ex) {
            Logger.getLogger(ExploreGraphDbJPanel.class.getName()).log(Level.SEVERE, "", ex);
            jTextAreaErrors.setText(ex.toString());
        }
        this.connection = connection;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "nullness"})
    @UIEffect
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jListNodeLabels = new javax.swing.JList<>();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTableNodes = new javax.swing.JTable();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTableRelationshipsIn = new javax.swing.JTable();
        jButtonGetNodeLabels = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jButtonGotoNext = new javax.swing.JButton();
        jButtonGotoFrom = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTableRelationshipsOut = new javax.swing.JTable();
        jLabel3 = new javax.swing.JLabel();
        jTextFieldQuery = new javax.swing.JTextField();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTextAreaErrors = new javax.swing.JTextArea();
        jTextFieldSelectedNodeId = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jTextFieldSelectedNodeName = new javax.swing.JTextField();
        jButtonSaveDump = new javax.swing.JButton();
        jButtonLoadDump = new javax.swing.JButton();
        jCheckBoxDebug = new javax.swing.JCheckBox();

        jListNodeLabels.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jListNodeLabels.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jListNodeLabelsValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(jListNodeLabels);

        jLabel1.setText("Node Labels:");

        jLabel2.setText("Nodes:");

        jTableNodes.setAutoCreateRowSorter(true);
        jTableNodes.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(jTableNodes);

        jTableRelationshipsIn.setAutoCreateRowSorter(true);
        jTableRelationshipsIn.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane3.setViewportView(jTableRelationshipsIn);

        jButtonGetNodeLabels.setText("Get Node Labels");
        jButtonGetNodeLabels.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGetNodeLabelsActionPerformed(evt);
            }
        });

        jLabel4.setText("In Relationships of Selected Node with ID= ");

        jLabel5.setText("Out Relationships of Selected Node");

        jButtonGotoNext.setText("Go To");
        jButtonGotoNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGotoNextActionPerformed(evt);
            }
        });

        jButtonGotoFrom.setText("Go To");
        jButtonGotoFrom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGotoFromActionPerformed(evt);
            }
        });

        jTableRelationshipsOut.setAutoCreateRowSorter(true);
        jTableRelationshipsOut.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane5.setViewportView(jTableRelationshipsOut);

        jLabel3.setText("Query: ");

        jTextFieldQuery.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldQueryActionPerformed(evt);
            }
        });

        jTextAreaErrors.setColumns(20);
        jTextAreaErrors.setFont(new java.awt.Font("Monospaced", Font.PLAIN, 15)); // NOI18N
        jTextAreaErrors.setRows(5);
        jScrollPane4.setViewportView(jTextAreaErrors);

        jTextFieldSelectedNodeId.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldSelectedNodeIdActionPerformed(evt);
            }
        });

        jLabel6.setText("Name:");

        jTextFieldSelectedNodeName.setText(" ");
        jTextFieldSelectedNodeName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldSelectedNodeNameActionPerformed(evt);
            }
        });

        jButtonSaveDump.setText("Save Dump");
        jButtonSaveDump.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveDumpActionPerformed(evt);
            }
        });

        jButtonLoadDump.setText("Load Dump");
        jButtonLoadDump.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLoadDumpActionPerformed(evt);
            }
        });

        jCheckBoxDebug.setText("Debug");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldQuery))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 684, Short.MAX_VALUE)
                                    .addComponent(jScrollPane5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 684, Short.MAX_VALUE)
                                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                        .addComponent(jLabel4)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextFieldSelectedNodeId, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel6)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextFieldSelectedNodeName)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButtonGotoFrom))
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                        .addComponent(jLabel5)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jButtonGotoNext)))
                                .addGap(18, 18, 18))))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButtonGetNodeLabels)
                            .addComponent(jButtonSaveDump)
                            .addComponent(jButtonLoadDump)
                            .addComponent(jCheckBoxDebug))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane4)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jButtonGetNodeLabels, jScrollPane1});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel3)
                    .addComponent(jTextFieldQuery, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(jTextFieldSelectedNodeId, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonGotoFrom)
                            .addComponent(jLabel6)
                            .addComponent(jTextFieldSelectedNodeName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(jButtonGotoNext))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 563, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButtonGetNodeLabels)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonSaveDump)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonLoadDump)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jCheckBoxDebug))
                    .addComponent(jScrollPane4))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    @UIEffect
    private void jButtonGetNodeLabelsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGetNodeLabelsActionPerformed
//        String query = "MATCH (n) RETURN distinct labels(n)";
        String query = "MATCH (n) WITH DISTINCT labels(n) AS labels UNWIND labels AS label RETURN DISTINCT label ORDER BY label";
        try {
            if (null == connection) {
                throw new IllegalStateException("connection is null");
            }
            PreparedStatement stmtn
                    = connection.prepareStatement(query);
            DefaultListModel<String> model = new DefaultListModel<>();

            model.removeAllElements();
            Set<String> set = new TreeSet<>();
            try (ResultSet rs = stmtn.executeQuery()) {
                while (rs.next()) {
                    Object o = rs.getObject(1);
                    if (o instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> l = (List<String>) o;
                        set.addAll(l);
                    } else {
//                    model.addElement(o);
                        if (o != null) {
                            set.add(o.toString());
                        }
                    }
                }
            }
            for (String s : set) {
                model.addElement(s);
            }
            jListNodeLabels.setModel(model);
        } catch (Exception ex) {
            logException(ex, "query=" + query);
        }

    }//GEN-LAST:event_jButtonGetNodeLabelsActionPerformed

    @UIEffect
    private void jListNodeLabelsValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jListNodeLabelsValueChanged
        String s = this.jListNodeLabels.getSelectedValue();
        if (null != s && s.length() > 0) {
            String query = "MATCH (n:" + s + ") RETURN ID(n),LABELS(n),n";
            jTextFieldQuery.setText(query);
            try {
                runQuery(query);
            } catch (SQLException ex) {
                logException(ex);
            }
        }
    }//GEN-LAST:event_jListNodeLabelsValueChanged

    @UIEffect
    private void jTextFieldQueryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldQueryActionPerformed
        String query = jTextFieldQuery.getText();
        try {
            runQuery(query);
        } catch (SQLException ex) {
            logException(ex);
        }
    }//GEN-LAST:event_jTextFieldQueryActionPerformed

    @UIEffect
    private void jButtonGotoFromActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGotoFromActionPerformed
        followTableEntry(this.jTableRelationshipsIn);
    }//GEN-LAST:event_jButtonGotoFromActionPerformed

    private void warn(String warning) {
        Logger.getLogger(ExploreGraphDbJFrame.class.getName()).warning(warning);
    }

    private void followTableEntry(JTable jTable) {
        int row = jTable.getSelectedRow();
        if (row >= 0) {
            Object tableObject = jTable.getValueAt(row, 3);
            if (null == tableObject) {
                warn("Trying to follow entry with null in table  at (" + row + ",3)");
                return;
            }
            String label;
            if (tableObject instanceof List) {
                label = ((List) tableObject).get(0).toString();
            } else {
                label = tableObject.toString();
            }
            if (label.startsWith("\"")) {
                label = label.substring(1);
            }
            if (label.endsWith("\"")) {
                label = label.substring(0, label.length() - 1);
            }
            if (label.startsWith("[")) {
                label = label.substring(1);
            }
            if (label.endsWith("]")) {
                label = label.substring(0, label.length() - 1);
            }
            String query = "MATCH (n:" + label + ") RETURN ID(n),LABELS(n),n";
            jTextFieldQuery.setText(query);
            Object idObject = jTable.getValueAt(row, 2);
            if (null == idObject) {
                warn("Trying to follow entry with null in table at (" + row + ",2)");
                return;
            }
            String id = idObject.toString();
            try {
                runQuery(query);
            } catch (Exception ex) {
                logException(ex, "query=" + query);
            }
            selectById(id);
        }
    }

    @UIEffect
    private void jButtonGotoNextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGotoNextActionPerformed
        followTableEntry(this.jTableRelationshipsOut);
    }//GEN-LAST:event_jButtonGotoNextActionPerformed

    @UIEffect
    private void jTextFieldSelectedNodeIdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldSelectedNodeIdActionPerformed
        selectById(jTextFieldSelectedNodeId.getText());
    }//GEN-LAST:event_jTextFieldSelectedNodeIdActionPerformed

    @UIEffect
    private void jTextFieldSelectedNodeNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldSelectedNodeNameActionPerformed
        selectByName(jTextFieldSelectedNodeName.getText());
    }//GEN-LAST:event_jTextFieldSelectedNodeNameActionPerformed

    @UIEffect
    private void jButtonSaveDumpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSaveDumpActionPerformed
        browseSaveDump();
    }//GEN-LAST:event_jButtonSaveDumpActionPerformed

    @UIEffect
    private void jButtonLoadDumpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoadDumpActionPerformed
        browseLoadDump();
    }//GEN-LAST:event_jButtonLoadDumpActionPerformed

    private void browseSaveDump() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                saveDump(chooser.getSelectedFile());
            } catch (FileNotFoundException | SQLException ex) {
                Logger.getLogger(ExploreGraphDbJPanel.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
    }

    private void saveDump(File f) throws FileNotFoundException, SQLException {
        if (null == connection) {
            throw new IllegalStateException("connection is null");
        }
        println("Saving to " + f + " ...");
        try (PrintStream ps = new PrintStream(new FileOutputStream(f))) {
            try (PreparedStatement stmtn
                    = connection.prepareStatement("MATCH(n) return n,labels(n),id(n)");
                    ResultSet rs = stmtn.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = getMapFromResultSet(rs, 1);
                    String idString = rs.getString(3);
                    if (null != idString) {
                        map.put("origID", idString);
                    }
                    if (map.keySet().size() > 0) {
                        List<String> labels = getListFromResultSet(rs, 2);
                        StringBuilder sb = new StringBuilder();
                        sb.append("CREATE (n");
                        appendNodeLabelsString(labels, sb);
                        appendPropsString(map, sb);
                        sb.append(")");
                        ps.println(sb.toString());
                    }
                }
            }
            try (PreparedStatement stmtn
                    = connection.prepareStatement("MATCH (n) -[r] -> (o) return n,labels(n),id(n),r,type(r),o,labels(o),id(o)");
                    ResultSet rs = stmtn.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>(); //getMapFromResultSet(rs, 1);
                    String idString = rs.getString(3);
                    if (null != idString) {
                        map.put("origID", idString);
                    }
                    if (map.keySet().size() > 0) {
                        List<String> labels = getListFromResultSet(rs, 2);
                        StringBuilder sb = new StringBuilder();
                        sb.append("MATCH (n");
                        appendNodeLabelsString(labels, sb);
                        appendPropsString(map, sb);
                        sb.append(" ) ,");

                        sb.append(" (o");
                        labels = getListFromResultSet(rs, 7);
                        appendNodeLabelsString(labels, sb);
//                        map = getMapFromResultSet(rs, 6);
                        map = new HashMap<>();
                        String secondIdString = rs.getString(8);
                        if (null != secondIdString) {
                            map.put("origID", secondIdString);
                        }
                        appendPropsString(map, sb);
                        sb.append(") CREATE (n) -[:");
                        String typer = rs.getString(5);
                        sb.append(typer);
                        map = getMapFromResultSet(rs, 4);
                        appendPropsString(map, sb);
                        sb.append("] -> (o)");
                        ps.println(sb.toString());
                    }
                }
            }
        }
        println("Finished saving to " + f);
    }

    private void appendNodeLabelsString(List<String> labels, StringBuilder sb) {
        if (labels.size() > 0) {
            for (String label : labels) {
                sb.append(':');
                sb.append(label);
            }
        }
    }

    private void appendPropsString(Map<String, Object> map, StringBuilder sb) {
        if (map.keySet().size() > 0) {
            sb.append(" { ");
            List<String> keyList = new ArrayList<>(map.keySet());
            Collections.sort(keyList);
            boolean firstKey = true;
            for (String key : keyList) {
                Object o = map.get(key);
                if (null == o) {
                    continue;
                }
                String value = o.toString();
                if (value.contains("\r") || value.contains("\n")) {
                    System.err.println("Skipping value of" + value + " for key" + key + " because it contains new-lines");
                    continue;
                }
                if (!firstKey) {
                    sb.append(", ");
                }
                sb.append(key);
                value = value.trim();
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                    value = value.trim();
                }
                sb.append(":'");
                sb.append(value);
                sb.append("'");
                firstKey = false;
            }
            sb.append(" }");
        }
    }

    private void browseLoadDump() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                loadDump(chooser.getSelectedFile());
            } catch (IOException | SQLException ex) {
                Logger.getLogger(ExploreGraphDbJPanel.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
    }

    private void loadDump(File f) throws SQLException, IOException {
        if (null == connection) {
            throw new IllegalStateException("connection is null");
        }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line = null;
            while (null != (line = br.readLine())) {
                PreparedStatement stmtn
                        = connection.prepareStatement(line);
                String nextline = null;
                while (!line.trim().endsWith(")") && (null != (nextline = br.readLine()))) {
                    line += nextline;
                }
                println("Executing line:" + line);
                boolean returnedResultSet = stmtn.execute();
                if (!returnedResultSet) {
                    int update_count = stmtn.getUpdateCount();
                    println("update_count = " + update_count);
                } else {
                    ResultSet rs = stmtn.getResultSet();
                    int row = 0;
                    while (rs.next()) {
                        row++;
                        println("row = " + row);
                        for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
                            println(rs.getMetaData().getColumnName(i + 1) + "=" + rs.getObject(i + 1, Object.class));
                        }
                    }
                }
            }
        }
    }

    private void selectByName(String name) {
        try {
            if (!jTextFieldSelectedNodeName.getText().equals(name)) {
                jTextFieldSelectedNodeName.setText(name);
            }
            for (int i = 0; i < this.jTableNodes.getRowCount(); i++) {
                String col1string = (String) this.jTableNodes.getValueAt(i, 1);
                if (Objects.equals(col1string, name)) {
                    this.jTableNodes.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    this.jTableNodes.getSelectionModel().setSelectionInterval(i, i);
                    this.jTableNodes.scrollRectToVisible(new Rectangle(this.jTableNodes.getCellRect(i, 0, true)));
                    this.updatePropsRels();
                    return;
                }
            }
            String msg = "name not found :" + name;
            this.jTextAreaErrors.setText(msg);
            System.err.println(msg);
        } catch (Exception e) {
            logException(e);
        }
    }

    private void selectById(String idString) {
        try {
            if (!jTextFieldSelectedNodeId.getText().equals(idString)) {
                jTextFieldSelectedNodeId.setText(idString);
            }
            for (int i = 0; i < this.jTableNodes.getRowCount(); i++) {
                Object col0Object = this.jTableNodes.getValueAt(i, 0);
                if (null == col0Object) {
                    System.err.println("null object in table (" + i + ",0) where id needed");
                    continue;
                }
                String col0string = col0Object.toString();
                if (Objects.equals(col0string, idString)) {
                    this.jTableNodes.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    this.jTableNodes.getSelectionModel().setSelectionInterval(i, i);
                    this.jTableNodes.scrollRectToVisible(new Rectangle(this.jTableNodes.getCellRect(i, 0, true)));
                    this.updatePropsRels();
                    return;
                }
            }
            String msg = "id not found :" + idString;
            this.jTextAreaErrors.setText(msg);
            System.err.println(msg);
        } catch (Exception e) {
            logException(e);
        }
    }

    private void logException(Exception ex, String... ctx) {
        Logger.getLogger(ExploreGraphDbJPanel.class.getName()).log(Level.SEVERE, "", ex);
        StringBuilder sb = new StringBuilder();
        sb.append(ex.toString());
        sb.append("\nCaused by: \n").append(ex.getCause()).append("\n");
        if (null != ctx) {
            for (String string : ctx) {
                System.err.println(string);
                sb.append(string);
            }
        }
        String sbString = sb.toString();
        if(sbString.length()> 10000) {
            jTextAreaErrors.setText(sbString.substring(0, 9999));
        } else {
            jTextAreaErrors.setText(sbString);
        }
    }

    @SuppressWarnings({"unchecked", "cast"})
    private Map<String, Object> getMapFromResultSet(ResultSet rs, int index) {
        Map<String, Object> result = null;
        try {
            if (rs.getMetaData().getColumnName(index).toUpperCase().startsWith("ID(")) {
                return Collections.singletonMap("", rs.getInt(index));
            }
            result = (Map<String, Object>) rs.getObject(index, Map.class);//stringToMap(str);
        } catch (ClassCastException cce) {
            try {
                List<?> l = (List<?>) rs.getObject(index, List.class);
                result = Collections.singletonMap("", l);
            } catch (Exception ex2) {
                Logger.getLogger(ExploreGraphDbJPanel.class.getName()).log(Level.SEVERE, "", ex2);
            }
        } catch (Exception ex) {
            Logger.getLogger(ExploreGraphDbJPanel.class.getName()).log(Level.SEVERE, "", ex);
        }
        if (result == null) {
            Logger.getLogger(ExploreGraphDbJPanel.class.getName()).log(Level.WARNING, "Returning empty map for getMapFromResultSet(...," + index);
            return Collections.emptyMap();
        }
        return result;
    }

    private List<String> getListFromResultSet(ResultSet rs, int index) {
        try {
            @SuppressWarnings("unchecked")
            List<String> result = (List<String>) rs.getObject(index, List.class);//stringToList(str);
            return result;
        } catch (Exception ex) {
            Logger.getLogger(ExploreGraphDbJPanel.class.getName()).log(Level.SEVERE, "", ex);
            return Collections.emptyList();
        }
    }

    private void runQuery(String query) throws SQLException {
        if (null == connection) {
            throw new IllegalStateException("connection is null");
        }
        PreparedStatement stmtn
                = connection.prepareStatement(query);
        DefaultTableModel model = new DefaultTableModel();
        int row = 0;
        List<Set<String>> listOfLabelsSets = new ArrayList<>();
        List<List<String>> listOfLabelsLists = new ArrayList<>();
        List<List<Map<String, Object>>> listOfListOfMaps = new ArrayList<>();
        int neededRowSize = 0;
        final int metaColCount;
        try (ResultSet rs = stmtn.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            metaColCount = meta.getColumnCount();
            while (rs.next()) {
                if (this.jCheckBoxDebug.isSelected()) {
                    for (int rsIndex = 1; rsIndex <= metaColCount; rsIndex++) {
                        try {
                            String colName = rs.getMetaData().getColumnName(rsIndex);
                            println("colName = " + colName);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                        try {
                            String colLabel = rs.getMetaData().getColumnLabel(rsIndex);
                            println("colLabel = " + colLabel);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }
                }
                listOfListOfMaps.add(new ArrayList<>());
                List<Map<String, Object>> thisRowListMap = listOfListOfMaps.get(row);
                if (this.jCheckBoxDebug.isSelected()) {
                    for (int rsIndex = 1; rsIndex <= metaColCount; rsIndex++) {
                        try {
                            String str = rs.getString(rsIndex);
                            println("row = " + row + ",rsIndex=" + rsIndex + ",str = " + str);
                        } catch (Exception excepion) {
                            excepion.printStackTrace();
                        }
                    }
                }
                for (int rsIndex = 1; rsIndex <= metaColCount; rsIndex++) {
                    String str = rs.getString(rsIndex);
                    if (this.jCheckBoxDebug.isSelected()) {
                        println("str = " + str);
                    }
//                    Object rsObject = rs.getObject(rsIndex, Object.class);
                    Map<String, Object> map = getMapFromResultSet(rs, rsIndex);
                    thisRowListMap.add(map);
                    if (row == 0) {
                        listOfLabelsLists.add(new ArrayList<>());
                        listOfLabelsSets.add(new HashSet<>());
                    }
                    List<String> sublist = listOfLabelsLists.get(rsIndex - 1);
                    Set<String> keySet = listOfLabelsSets.get(rsIndex - 1);
                    keySet.addAll(map.keySet());
                    sublist.clear();
                    sublist.addAll(keySet);
                    if (keySet.contains("name")) {
                        sublist.remove("name");
                    }
                    Collections.sort(sublist);
                    if (keySet.contains("name")) {
                        sublist.add(0, "name");
                    }
                    neededRowSize += sublist.size();
                }
                row++;
            }
            for (int rsIndex = 1; rsIndex <= metaColCount && rsIndex <= listOfLabelsLists.size(); rsIndex++) {
                List<String> sublist = listOfLabelsLists.get(rsIndex - 1);
                for (String aSublist : sublist) {
                    model.addColumn(meta.getColumnName(rsIndex) + "." + aSublist);
                }
            }
        }

        for (int rowIndex = 0; rowIndex < row; rowIndex++) {
            model.addRow(new Object[neededRowSize]);
            int tableOffset = 0;
            List<Map<String, Object>> thisRowListMap = listOfListOfMaps.get(rowIndex);
            for (int rsIndex = 1; rsIndex <= metaColCount; rsIndex++) {
                Map<String, Object> map = thisRowListMap.get(rsIndex - 1);
                List<String> sublist = listOfLabelsLists.get(rsIndex - 1);
                for (int tableIndex = 0; tableIndex < sublist.size(); tableIndex++) {
                    final String listElement = sublist.get(tableIndex);
                    final Object mapValue = map.get(listElement);
                    final int tableOffsetIndex = tableOffset + tableIndex;
                    if(mapValue != null) {
                        model.setValueAt(mapValue, rowIndex, tableOffsetIndex);
                    } else {
                         clearModelValue(model, rowIndex, tableOffsetIndex);
                    }
                }
                tableOffset += listOfLabelsLists.get(rsIndex - 1).size();
            }
        }
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            updateNodes(model);
        } else {
            javax.swing.SwingUtilities.invokeLater(() -> updateNodes(model));
        }
    }

    @SuppressWarnings("nullness")
    private void clearModelValue(DefaultTableModel model, int rowIndex, final int tableOffsetIndex) {
        model.setValueAt(null, rowIndex, tableOffsetIndex);
    }

    private void updateNodes(DefaultTableModel model) {
        jTableNodes.getModel().removeTableModelListener(nodeTableModelListener);
        jTableNodes.setModel(model);
        jTableNodes.getSelectionModel().setSelectionInterval(-1, -1);
        jTableNodes.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        autoResizeTableColWidths(jTableNodes);
        DefaultTableModel inModel = (DefaultTableModel) jTableRelationshipsIn.getModel();
        inModel.setRowCount(0);
        DefaultTableModel outModel = (DefaultTableModel) jTableRelationshipsOut.getModel();
        outModel.setRowCount(0);
        jTextFieldSelectedNodeId.setText("");
        jTextFieldSelectedNodeName.setText("");
        jTableNodes.getModel().addTableModelListener(nodeTableModelListener);
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonGetNodeLabels;
    private javax.swing.JButton jButtonGotoFrom;
    private javax.swing.JButton jButtonGotoNext;
    private javax.swing.JButton jButtonLoadDump;
    private javax.swing.JButton jButtonSaveDump;
    private javax.swing.JCheckBox jCheckBoxDebug;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JList<String> jListNodeLabels;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTable jTableNodes;
    private javax.swing.JTable jTableRelationshipsIn;
    private javax.swing.JTable jTableRelationshipsOut;
    private javax.swing.JTextArea jTextAreaErrors;
    private javax.swing.JTextField jTextFieldQuery;
    private javax.swing.JTextField jTextFieldSelectedNodeId;
    private javax.swing.JTextField jTextFieldSelectedNodeName;
    // End of variables declaration//GEN-END:variables

    private @MonotonicNonNull
    AprsSystem aprsSystem = null;

    /**
     * Set the value of aprsSystemInterface
     *
     * @param aprsSystem new value of aprsSystemInterface
     */
    public void setAprsSystem(AprsSystem aprsSystem) {
        this.aprsSystem = aprsSystem;
    }

    @Override
    @SafeEffect
    public void accept(DbSetup setup) {
        try {
            if (setup.isConnected()) {
                if (setup.getDbType() == DbType.NEO4J || setup.getDbType() == DbType.NEO4J_BOLT) {
                    final StackTraceElement stackTraceElemArray[] = Thread.currentThread().getStackTrace();
                    DbSetupBuilder.connect(setup)
                            .handle("ExploreGraphDb.handle db connect",
                                    (Connection c, Throwable ex) -> {
                                        if (null != c) {
                                            Utils.runOnDispatchThread(() -> {
                                                setConnection(c);
                                            });
                                        }
                                        if (null != ex) {
                                            Logger.getLogger(ExploreGraphDbJPanel.class.getName()).log(Level.SEVERE, "", ex);
                                            System.err.println("Called from :");
                                            for (StackTraceElement aStackTraceElemArray : stackTraceElemArray) {
                                                System.err.println(aStackTraceElemArray);
                                            }
                                            System.err.println();
                                            System.err.println("Exception handled at ");

                                            if (null != aprsSystem) {
                                                if (aprsSystem.isEnableDebugDumpStacks()) {
                                                    Thread.dumpStack();
                                                }
                                                aprsSystem.setTitleErrorString("Database error: " + ex.toString());
                                            }
                                        }
                                        return c;
                                    });

                    //.thenAccept(conn -> Utils.runOnDispatchThread(() -> setConnection(conn)));
                    println("ExploreGraph connected to database of on host " + setup.getHost() + " with port " + setup.getPort());
                } else {
                    closeConnection();
                    String msg = "The ExploreGraphDb frame only works with " + DbType.NEO4J + " but " + setup.getDbType() + " was selected.";
                    this.jTextAreaErrors.setText(msg);
                    System.err.println(msg);
                }
            } else {
                closeConnection();
            }
        } catch (SQLException ex) {
            Logger.getLogger(ExploreGraphDbJPanel.class.getName()).log(Level.SEVERE, "", ex);
            jTextAreaErrors.setText(ex.toString());
        }
    }
}
