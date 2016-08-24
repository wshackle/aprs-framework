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

import aprs.framework.database.DbSetup;
import aprs.framework.database.DbSetupBuilder;
import aprs.framework.database.DbSetupListener;
import aprs.framework.database.DbType;
import com.google.gwt.thirdparty.guava.common.base.Objects;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

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

    public void autoResizeTableColWidths(JTable table) {

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        int fullsize = 0;
        Container parent = table.getParent();
        if (null != parent) {
            fullsize = Math.max(parent.getPreferredSize().width, parent.getSize().width);
        }
        int sumWidths = 0;
        for (int i = 0; i < table.getColumnCount(); i++) {
            DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
            TableColumn col = colModel.getColumn(i);
            int width = 0;

            TableCellRenderer renderer = col.getHeaderRenderer();
            if (renderer == null) {
                renderer = table.getTableHeader().getDefaultRenderer();
            }
            Component headerComp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(),
                    false, false, 0, i);
            width = Math.max(width, headerComp.getPreferredSize().width);
            for (int r = 0; r < table.getRowCount(); r++) {
                renderer = table.getCellRenderer(r, i);
                Component comp = renderer.getTableCellRendererComponent(table, table.getValueAt(r, i),
                        false, false, r, i);
                width = Math.max(width, comp.getPreferredSize().width);
            }
            if (i == table.getColumnCount() - 1) {
                if (width < fullsize - sumWidths) {
                    width = fullsize - sumWidths;
                }
            }
            col.setPreferredWidth(width + 2);
            sumWidths += width + 2;
        }
    }

    private void updatePropsRels() {
        try {
            int index = this.jTableNodes.getSelectedRow();
            if (index < 0 || index >= this.jTableNodes.getRowCount()) {
                return;
            }
            String s = this.jTableNodes.getValueAt(index, 0).toString();
            if (!jTextFieldSelectedNodeId.getText().equals(s)) {
                jTextFieldSelectedNodeId.setText(s);
            }
            String col0Head = this.jTableNodes.getColumnName(0);
            PreparedStatement outStatement = null;
            if (col0Head.endsWith(".name")) {
                outStatement
                        = connection.prepareStatement("MATCH (n {name:{1} }) - [relationship] -> (to) RETURN type(relationship),relationship,id(to),labels(to),to");
                outStatement.setString(1, s);
            } else {
                outStatement
                        = connection.prepareStatement("MATCH (n) - [relationship] -> (to) WHERE ID(n) = " + s + " RETURN type(relationship),relationship,id(to),labels(to),to");
            }
            DefaultTableModel model = new DefaultTableModel();
            try (ResultSet rs = outStatement.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
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
                    model.addRow(new Object[meta.getColumnCount()]);
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        model.setValueAt(rs.getObject(i), row, i - 1);
                    }
                    row++;
                }
            }
            jTableRelationshipsOut.setModel(model);
            this.autoResizeTableColWidths(jTableRelationshipsOut);
            PreparedStatement inStatement = null;
            if (col0Head.endsWith(".name")) {
                inStatement = connection.prepareStatement("MATCH (from) - [relationship] -> (n {name:{1} })  RETURN type(relationship),relationship,id(from),labels(from),from");
                inStatement.setString(1, s);
            } else {
                inStatement = connection.prepareStatement("MATCH (from) - [relationship] -> (n) WHERE ID(n) = " + s + " RETURN type(relationship),relationship,id(from),labels(from),from");
            }

            model = new DefaultTableModel();
            try (ResultSet rs = inStatement.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
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
                    model.addRow(new Object[meta.getColumnCount()]);
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        model.setValueAt(rs.getObject(i), row, i - 1);
                    }
                    row++;
                }
            }
            jTableRelationshipsIn.setModel(model);
            this.autoResizeTableColWidths(jTableRelationshipsIn);
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

        jLabel4.setText("In Relationships of Selected Node:");

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
                                    .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 663, Short.MAX_VALUE)
                                    .addComponent(jScrollPane5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 663, Short.MAX_VALUE)
                                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                        .addComponent(jLabel4)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextFieldSelectedNodeId, javax.swing.GroupLayout.PREFERRED_SIZE, 304, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jButtonGotoFrom))
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                        .addComponent(jLabel5)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jButtonGotoNext)))
                                .addGap(18, 18, 18))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButtonGetNodeLabels)
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
                            .addComponent(jButtonGotoFrom))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(jButtonGotoNext))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 428, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonGetNodeLabels)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonGetNodeLabelsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGetNodeLabelsActionPerformed
        try {
            if (null == connection) {
                throw new IllegalStateException("connection is null");
            }
            PreparedStatement stmtn
                    = connection.prepareStatement("MATCH (n) RETURN distinct labels(n)");
            DefaultListModel model = new DefaultListModel();

            model.removeAllElements();
            Set<String> set = new TreeSet<>();
            try (ResultSet rs = stmtn.executeQuery()) {
                while (rs.next()) {
                    Object o = rs.getObject(1);
                    if (o instanceof List) {
                        set.addAll((List)o);
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
        } catch (SQLException ex) {
            logException(ex);
        }

    }//GEN-LAST:event_jButtonGetNodeLabelsActionPerformed

    private void jListNodeLabelsValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jListNodeLabelsValueChanged
        String s = this.jListNodeLabels.getSelectedValue();
        String query = "MATCH (n:" + s + ") RETURN ID(n),n";
        jTextFieldQuery.setText(query);
        try {
            runQuery(query);
        } catch (SQLException ex) {
            logException(ex);
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
            String query = "MATCH (n:" + label + ") RETURN ID(n),n";
            jTextFieldQuery.setText(query);
//        Map map = (Map) jTable.getValueAt(row, 2);
            String id = (String) jTable.getValueAt(row, 2).toString();
            try {
                runQuery(query);
            } catch (SQLException ex) {
                logException(ex);
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

    private void selectById(String idString) {
        if (!jTextFieldSelectedNodeId.getText().equals(idString)) {
            jTextFieldSelectedNodeId.setText(idString);
        }
        for (int i = 0; i < this.jTableNodes.getRowCount(); i++) {
            String col0string = (String) this.jTableNodes.getValueAt(i, 0);
            if (Objects.equal(col0string, idString)) {
                this.jTableNodes.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                this.jTableNodes.getSelectionModel().setSelectionInterval(i, i);
                this.jTableNodes.scrollRectToVisible(new Rectangle(this.jTableNodes.getCellRect(i, 0, true)));
                this.updatePropsRels();
                break;
            }
        }
    }

    private void logException(SQLException ex) {
        Logger.getLogger(ExploreGraphDbJPanel.class.getName()).log(Level.SEVERE, null, ex);
        jTextAreaErrors.setText(ex.toString());
        jTextAreaErrors.append("\nCaused by: \n" + ex.getCause());
    }

    private void runQuery(String query) throws SQLException {
        PreparedStatement stmtn
                = connection.prepareStatement(query);
//            stmtn.setString(1, s);
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
                listOfListOfMaps.add(new ArrayList<>());
                List<Map> thisRowListMap = listOfListOfMaps.get(row);

                for (int rsIndex = 1; rsIndex <= metaColCount; rsIndex++) {
                    Object rsObject = rs.getObject(rsIndex, Object.class);
                    Map map = null;
                    if (rsObject instanceof Map) {
                        map = (Map) rsObject;
                    } else {
                        map = Collections.singletonMap("", rsObject);
                    }
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
            for (int rsIndex = 1; rsIndex <= metaColCount; rsIndex++) {
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

        jTableNodes.setModel(model);
        this.autoResizeTableColWidths(jTableNodes);
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonGetNodeLabels;
    private javax.swing.JButton jButtonGotoFrom;
    private javax.swing.JButton jButtonGotoNext;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
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
    // End of variables declaration//GEN-END:variables

    @Override
    public void accept(DbSetup setup) {
        try {
            if (setup.isConnected()) {
                if (setup.getDbType() == DbType.NEO4J) {
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
