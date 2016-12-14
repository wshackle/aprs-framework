/*
 * This software is public domain software, however it is preferred
 * that the following disclaimers be attached.
 * Software Copywrite/Warranty Disclaimer
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
package aprs.framework.database.explore;

import static aprs.framework.Utils.autoResizeTableColWidths;
import aprs.framework.database.DbSetup;
import aprs.framework.database.DbSetupBuilder;
import aprs.framework.database.DbSetupListener;
import aprs.framework.database.DbType;
//import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Rectangle;
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
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class ExploreGraphDbJPanel extends javax.swing.JPanel implements DbSetupListener {

    /**
     * Creates new form ExplorteGraphDbJPanel
     */
    public ExploreGraphDbJPanel() {
        initComponents();
        jTableNodes.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updatePropsRels();
            }
        });
    }

    private Map<String, ?> objectToMap(Object o) {
        try {
            if (o instanceof Map) {
                return (Map) o;
            }
//            else if (o instanceof String) {
//                return stringToMap((String) o);
//            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return Collections.singletonMap("", o);
    }

//    private Object getResultSetObject(int index, ResultSet rs) {
//        try {
//            return rs.getInt(index);
//        }catch(Exception ex) {
//        }
//        try {
//            return stringToMap(rs.getString(index));
//        } catch(Exception ex) {
//        }
//        return null;
//    }
//    
    private void updatePropsRels() {
        try {
            int index = this.jTableNodes.getSelectedRow();
            if(jTableNodes.getColumnCount() < 1) {
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
            PreparedStatement outStatement = null;
            String name = jTextFieldSelectedNodeName.getText();
            if (col0Head.endsWith(".name")) {
                String getRelationShipsOutQuery
                        = "MATCH (n {name:{1} }) - [relationship] -> (to) RETURN type(relationship),relationship,id(to),labels(to),to";
                System.out.println("getRelationShipsOutQuery = " + getRelationShipsOutQuery);
                outStatement
                        = connection.prepareStatement(getRelationShipsOutQuery);
                outStatement.setString(1, s);
            } else {
                String getRelationShipsOutQuery
                        = "MATCH (n) - [relationship] -> (to) WHERE ID(n) = " + s + " RETURN type(relationship),relationship,id(to),labels(to),to";
                System.out.println("getRelationShipsOutQuery = " + getRelationShipsOutQuery);
                outStatement
                        = connection.prepareStatement(getRelationShipsOutQuery);
                if (jTableNodes.getColumnCount() > 1) {
                    String col1Head = this.jTableNodes.getColumnName(1);
                    if (col1Head.endsWith(".name")) {
                        name = Objects.toString(this.jTableNodes.getValueAt(index, 1));
                        if (!jTextFieldSelectedNodeName.getText().equals(name)) {
                            jTextFieldSelectedNodeName.setText(name);
                        }
                    }
                }
            }
            DefaultTableModel model = new DefaultTableModel();
            List<Object[]> resultList = new ArrayList<>();
            int colCount = -1;
            try (ResultSet rs = outStatement.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                colCount = meta.getColumnCount();
                for (int i = 1; i < meta.getColumnCount(); i++) {
                    String colName = meta.getColumnName(i);
//                    System.out.println("colName = " + colName);
//                    String columnClassName = meta.getColumnClassName(i);
//                    System.out.println("columnClassName = " + columnClassName);
//                    String columnType = meta.getColumnTypeName(i);
//                    System.out.println("columnType = " + columnType);
                    model.addColumn(colName);
                }
                int row = 0;
                List<String> l = new ArrayList<>();
                while (rs.next()) {
                    Object ao[] = new Object[meta.getColumnCount()];
                    for (int i = 0; i < ao.length; i++) {
                        ao[i] = rs.getObject(i + 1);
                    }
                    resultList.add(ao);
                }
            }
//                    for (int i = 1; i <= meta.getColumnCount(); i++) {
//                        String colName = meta.getColumnName(i);
//                        System.out.println("colName = " + colName);
//                        Object o = rs.getObject(i);
//                        System.out.println("o = " + o);
//                        System.out.println("o.getCLass() = " + o.getClass());
////                        model.addColumn(colName);
//                    }
//                    Map map = rs.getObject(1, Map.class);
//                    if (row == 0) {
//                        l.addAll(map.keySet());
//                        for (int i = 0; i < l.size(); i++) {
//                            model.addColumn(l.get(i));
//                        }
//                    }
            TreeSet<String> toKeys = new TreeSet<>();
            for (Object[] ao : resultList) {
                Object lastObject = ao[ao.length - 1];
                if (lastObject instanceof Map) {
                    toKeys.addAll(objectToMap(lastObject).keySet());
                }
            }
            List<String> keyList = new ArrayList<>();
            keyList.addAll(toKeys);
            if (keyList.contains("name")) {
                keyList.remove("name");
            }
            Collections.sort(keyList);
            keyList.add(0, "name");
            for (String key : keyList) {
                model.addColumn("to." + key);
            }
            final int outTableWidth = colCount - 1 + keyList.size();
            for (int rowIndex = 0; rowIndex < resultList.size(); rowIndex++) {
                Object ao[] = resultList.get(rowIndex);
                Object newArray[] = new Object[outTableWidth];
                System.arraycopy(ao, 0, newArray, 0, ao.length - 1);
                for (int i = ao.length - 1; i < outTableWidth; i++) {
                    Map<String, ?> map = objectToMap(ao[ao.length - 1]);
                    newArray[i] = map.get(keyList.get(i - (ao.length - 1)));
                }
                model.addRow(newArray);
            }
            jTableRelationshipsOut.setModel(model);
            autoResizeTableColWidths(jTableRelationshipsOut);
            PreparedStatement inStatement = null;
            if (col0Head.endsWith(".name")) {
                inStatement = connection.prepareStatement("MATCH (from) - [relationship] -> (n {name:{1} })  RETURN type(relationship),relationship,id(from),labels(from),from");
                inStatement.setString(1, s);
            } else {
                inStatement = connection.prepareStatement("MATCH (from) - [relationship] -> (n) WHERE ID(n) = " + s + " RETURN type(relationship),relationship,id(from),labels(from),from");
            }

            model = new DefaultTableModel();
            resultList = new ArrayList<>();
            colCount = -1;
            try (ResultSet rs = inStatement.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                colCount = meta.getColumnCount();
                for (int i = 1; i <= meta.getColumnCount() - 1; i++) {
                    String colName = meta.getColumnName(i);
//                    System.out.println("colName = " + colName);
//                    String columnClassName = meta.getColumnClassName(i);
//                    System.out.println("columnClassName = " + columnClassName);
//                    String columnType = meta.getColumnTypeName(i);
//                    System.out.println("columnType = " + columnType);
                    model.addColumn(colName);
                }
                int row = 0;
                List<String> l = new ArrayList<>();
                while (rs.next()) {
                    Object ao[] = new Object[meta.getColumnCount()];
                    for (int i = 0; i < ao.length; i++) {
                        ao[i] = rs.getObject(i + 1);
                    }
                    resultList.add(ao);
                }
            }
            TreeSet<String> fromKeys = new TreeSet<>();
            for (Object[] ao : resultList) {
                Object lastObject = ao[ao.length - 1];
                if (lastObject instanceof Map) {
                    fromKeys.addAll(((Map<String, ?>) lastObject).keySet());
                }
            }
            keyList = new ArrayList<>();
            keyList.addAll(fromKeys);
            if (keyList.contains("name")) {
                keyList.remove("name");
            }
            Collections.sort(keyList);
            keyList.add(0, "name");
            for (String key : keyList) {
                model.addColumn("from." + key);
            }
            final int inTableWidth = colCount - 1 + keyList.size();
            for (int rowIndex = 0; rowIndex < resultList.size(); rowIndex++) {
                Object ao[] = resultList.get(rowIndex);
                Object newArray[] = new Object[inTableWidth];
                System.arraycopy(ao, 0, newArray, 0, ao.length - 1);
                for (int i = ao.length - 1; i < inTableWidth; i++) {
                    Map<String, Object> map = (Map) ao[ao.length - 1];
                    newArray[i] = map.get(keyList.get(i - (ao.length - 1)));
                }
                model.addRow(newArray);
            }
            jTableRelationshipsIn.setModel(model);
            autoResizeTableColWidths(jTableRelationshipsIn);
//            jTableNodes.setModel(model);

        } catch (SQLException ex) {
            Logger.getLogger(ExploreGraphDbJPanel.class.getName()).log(Level.SEVERE, null, ex);
            jTextAreaErrors.setText(ex.toString());
        }
    }
    private Connection connection = null;

    /**
     * Get the value of connection
     *
     * @return the value of connection
     */
    public Connection getConnection() {
        return connection;
    }

    private boolean sharedConnection;

    /**
     * Get the value of sharedConnection
     *
     * @return the value of sharedConnection
     */
    public boolean isSharedConnection() {
        return sharedConnection;
    }

    /**
     * Set the value of sharedConnection
     *
     * @param sharedConnection new value of sharedConnection
     */
    public void setSharedConnection(boolean sharedConnection) {
        this.sharedConnection = sharedConnection;
    }

    public void closeConnection() throws SQLException {
        if (null != this.connection && !sharedConnection) {
            this.connection.close();
            this.connection = null;
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
            Logger.getLogger(ExploreGraphDbJPanel.class.getName()).log(Level.SEVERE, null, ex);
            jTextAreaErrors.setText(ex.toString());
        }
        this.connection = connection;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
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
        jTextAreaErrors.setFont(new java.awt.Font("Monospaced", 0, 15)); // NOI18N
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

    private void jButtonGetNodeLabelsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGetNodeLabelsActionPerformed
//        String query = "MATCH (n) RETURN distinct labels(n)";
        String query = "MATCH (n) WITH DISTINCT labels(n) AS labels UNWIND labels AS label RETURN DISTINCT label ORDER BY label";
        try {
            if (null == connection) {
                throw new IllegalStateException("connection is null");
            }
            PreparedStatement stmtn
                    = connection.prepareStatement(query);
            DefaultListModel model = new DefaultListModel();

            model.removeAllElements();
            Set<String> set = new TreeSet<>();
            try (ResultSet rs = stmtn.executeQuery()) {
                while (rs.next()) {
                    Object o = rs.getObject(1);
                    if (o instanceof List) {
                        set.addAll((List) o);
                    } else {
//                    model.addElement(o);
                        set.add(o.toString());
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

    private void jListNodeLabelsValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jListNodeLabelsValueChanged
        String s = this.jListNodeLabels.getSelectedValue();
        if (null != s && s.length() > 0) {
            String query = "MATCH (n:" + s + ") RETURN ID(n),n";
            jTextFieldQuery.setText(query);
            try {
                runQuery(query);
            } catch (SQLException ex) {
                logException(ex);
            }
        }
    }//GEN-LAST:event_jListNodeLabelsValueChanged

    private void jTextFieldQueryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldQueryActionPerformed
        String query = jTextFieldQuery.getText();
        try {
            runQuery(query);
        } catch (SQLException ex) {
            logException(ex);
        }
    }//GEN-LAST:event_jTextFieldQueryActionPerformed

    private void jButtonGotoFromActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGotoFromActionPerformed
        followTableEntry(this.jTableRelationshipsIn);
    }//GEN-LAST:event_jButtonGotoFromActionPerformed

    private void followTableEntry(JTable jTable) {
        int row = jTable.getSelectedRow();
        if (row >= 0) {
            Object tableObject = jTable.getValueAt(row, 3);
            String label = null;
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
            String query = "MATCH (n:" + label + ") RETURN ID(n),n";
            jTextFieldQuery.setText(query);
//        Map map = (Map) jTable.getValueAt(row, 2);
            String id = (String) jTable.getValueAt(row, 2).toString();
            try {
                runQuery(query);
            } catch (Exception ex) {
                logException(ex, "query=" + query);
            }
            selectById(id);
        }
    }

    private void jButtonGotoNextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGotoNextActionPerformed
        followTableEntry(this.jTableRelationshipsOut);
    }//GEN-LAST:event_jButtonGotoNextActionPerformed

    private void jTextFieldSelectedNodeIdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldSelectedNodeIdActionPerformed
        selectById(jTextFieldSelectedNodeId.getText());
    }//GEN-LAST:event_jTextFieldSelectedNodeIdActionPerformed

    private void jTextFieldSelectedNodeNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldSelectedNodeNameActionPerformed
        selectByName(jTextFieldSelectedNodeName.getText());
    }//GEN-LAST:event_jTextFieldSelectedNodeNameActionPerformed

    private void jButtonSaveDumpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSaveDumpActionPerformed
        browseSaveDump();
    }//GEN-LAST:event_jButtonSaveDumpActionPerformed

    private void jButtonLoadDumpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoadDumpActionPerformed
        browseLoadDump();
    }//GEN-LAST:event_jButtonLoadDumpActionPerformed

    private void browseSaveDump() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                saveDump(chooser.getSelectedFile());
            } catch (FileNotFoundException | SQLException ex) {
                Logger.getLogger(ExploreGraphDbJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void saveDump(File f) throws FileNotFoundException, SQLException {
        System.out.println("Saving to " + f + " ...");
        try (PrintStream ps = new PrintStream(new FileOutputStream(f))) {

            // Skip adding the contstraints
//            try (PreparedStatement stmtn
//                    = connection.prepareStatement("MATCH (n) WITH DISTINCT labels(n) AS labels UNWIND labels AS label RETURN DISTINCT label ORDER BY label");
//                    ResultSet rs = stmtn.executeQuery()) {
//                while (rs.next()) {
//                    String label = rs.getString(1);
//                    ps.println("CREATE CONSTRAINT ON (x:" + label + ") ASSERT x.origID IS UNIQUE");
//                }
//            }
            try (PreparedStatement stmtn
                    = connection.prepareStatement("MATCH(n) return n,labels(n),id(n)");
                    ResultSet rs = stmtn.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = getMapFromResultSet(rs, 1);
                    map.put("origID", rs.getString(3));
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
                    map.put("origID", rs.getString(3));
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
                        map.put("origID", rs.getString(8));
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
        System.out.println("Finished saving to " + f);
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
            List<String> keyList = new ArrayList<>();
            keyList.addAll(map.keySet());
            Collections.sort(keyList);
            boolean firstKey = true;
            for (String key : keyList) {
                String value = map.get(key).toString();
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
                Logger.getLogger(ExploreGraphDbJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void loadDump(File f) throws SQLException, IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line = null;
            while (null != (line = br.readLine())) {
                PreparedStatement stmtn
                        = connection.prepareStatement(line);
                while (!line.trim().endsWith(")")) {
                    line += br.readLine();
                }
                System.out.println("Executing line:" + line);
                boolean returnedResultSet = stmtn.execute();
                if (!returnedResultSet) {
                    int update_count = stmtn.getUpdateCount();
                    System.out.println("update_count = " + update_count);
                } else {
                    ResultSet rs = stmtn.getResultSet();
                    int row = 0;
                    while (rs.next()) {
                        row++;
                        System.out.println("row = " + row);
                        for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
                            System.out.println(rs.getMetaData().getColumnName(i + 1) + "=" + rs.getObject(i + 1, Object.class));
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
                String col0string = (String) this.jTableNodes.getValueAt(i, 0).toString();
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
        Logger.getLogger(ExploreGraphDbJPanel.class.getName()).log(Level.SEVERE, null, ex);
        jTextAreaErrors.setText(ex.toString());
        jTextAreaErrors.append("\nCaused by: \n" + ex.getCause() + "\n");
        if (null != ctx) {
            for (int i = 0; i < ctx.length; i++) {
                String string = ctx[i];
                System.err.println(string);
                jTextAreaErrors.append(string);
            }
        }

    }

//    private Map<String, Object> stringToMap(String str) throws IOException {
//        return new ObjectMapper().readValue(str, HashMap.class);
//    }
//
//    private List<String> stringToList(String str) throws IOException {
//        return new ObjectMapper().readValue(str, ArrayList.class);
//    }
    private Map<String, Object> getMapFromResultSet(ResultSet rs, int index) {
        Map<String, Object> result = null;
        try {
            if (rs.getMetaData().getColumnName(index).toUpperCase().startsWith("ID(")) {
                return Collections.singletonMap("", rs.getInt(index));
            }
            String str = rs.getString(index);
            result = rs.getObject(index, Map.class);//stringToMap(str);
        } catch (Exception ex) {
            Logger.getLogger(ExploreGraphDbJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    private List<String> getListFromResultSet(ResultSet rs, int index) {
        List<String> result = null;
        try {
            String str = rs.getString(index);
            result = rs.getObject(index, List.class);//stringToList(str);
        } catch (Exception ex) {
            Logger.getLogger(ExploreGraphDbJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    private void runQuery(String query) throws SQLException {
        PreparedStatement stmtn
                = connection.prepareStatement(query);
        DefaultTableModel model = new DefaultTableModel();
        int row = 0;
        List<Set<String>> listOfLabelsSets = new ArrayList<>();
        List<List<String>> listOfLabelsLists = new ArrayList<>();
        List<List<Map>> listOfListOfMaps = new ArrayList<>();
        int neededRowSize = 0;
        int metaColCount = 0;
        try (ResultSet rs = stmtn.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            metaColCount = meta.getColumnCount();
            while (rs.next()) {
                if (this.jCheckBoxDebug.isSelected()) {
                    for (int rsIndex = 1; rsIndex <= metaColCount; rsIndex++) {
                        try {
                            String colName = rs.getMetaData().getColumnName(rsIndex);
                            System.out.println("colName = " + colName);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                        try {
                            String colLabel = rs.getMetaData().getColumnLabel(rsIndex);
                            System.out.println("colLabel = " + colLabel);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }
                }
                listOfListOfMaps.add(new ArrayList<>());
                List<Map> thisRowListMap = listOfListOfMaps.get(row);
                if (this.jCheckBoxDebug.isSelected()) {
                    for (int rsIndex = 1; rsIndex <= metaColCount; rsIndex++) {
                        try {
                            String str = rs.getString(rsIndex);
                            System.out.println("row = " + row + ",rsIndex=" + rsIndex + ",str = " + str);
                        } catch (Exception excepion) {
                            excepion.printStackTrace();
                        }
                    }
                }
                for (int rsIndex = 1; rsIndex <= metaColCount; rsIndex++) {
                    String str = rs.getString(rsIndex);
                    if (this.jCheckBoxDebug.isSelected()) {
                        System.out.println("str = " + str);
                    }
//                    Object rsObject = rs.getObject(rsIndex, Object.class);
                    Map map = getMapFromResultSet(rs, rsIndex);
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
                for (int tableIndex = 0; tableIndex < sublist.size(); tableIndex++) {
                    model.addColumn(meta.getColumnName(rsIndex) + "." + sublist.get(tableIndex));
                }
            }
        }

        for (int rowIndex = 0; rowIndex < row; rowIndex++) {
            model.addRow(new Object[neededRowSize]);
            int tableOffset = 0;
            List<Map> thisRowListMap = listOfListOfMaps.get(rowIndex);
            for (int rsIndex = 1; rsIndex <= metaColCount; rsIndex++) {
                Map map = thisRowListMap.get(rsIndex - 1);
                List<String> sublist = listOfLabelsLists.get(rsIndex - 1);
                for (int tableIndex = 0; tableIndex < sublist.size(); tableIndex++) {
                    model.setValueAt(map.get(sublist.get(tableIndex)), rowIndex, tableOffset + tableIndex);
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

    private void updateNodes(DefaultTableModel model) {
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

    @Override
    public void accept(DbSetup setup) {
        try {
            if (setup.isConnected()) {
                if (setup.getDbType() == DbType.NEO4J || setup.getDbType() == DbType.NEO4J_BOLT) {
                    setConnection(DbSetupBuilder.connect(setup));
                    System.out.println("ExploreGraph connected to database of on host " + setup.getHost() + " with port " + setup.getPort());
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
            Logger.getLogger(ExploreGraphDbJPanel.class.getName()).log(Level.SEVERE, null, ex);
            jTextAreaErrors.setText(ex.toString());
        }
    }
}
