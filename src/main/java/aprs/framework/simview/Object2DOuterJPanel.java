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
package aprs.framework.simview;

import aprs.framework.AprsJFrame;
import static aprs.framework.Utils.autoResizeTableColWidths;
import aprs.framework.database.DetectedItem;
import aprs.framework.database.DbSetupBuilder;
import static aprs.framework.simview.DisplayAxis.POS_X_POS_Y;
import aprs.framework.spvision.VisionSocketClient;
import aprs.framework.spvision.VisionSocketServer;
import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.ui.client.PendantClientJPanel;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JFileChooser;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class Object2DOuterJPanel extends javax.swing.JPanel implements Object2DJFrameInterface, VisionSocketClient.VisionSocketClientListener, PendantClientJPanel.CurrentPoseListener {

    public List<DetectedItem> getItems() {
        return object2DJPanel1.getItems();
    }

    private volatile boolean settingItems = false;

    public void setItems(List<DetectedItem> items) {
        try {
            settingItems = true;
            if (javax.swing.SwingUtilities.isEventDispatchThread()) {
                setItemsInternal(items);
            } else {
                javax.swing.SwingUtilities.invokeLater(() -> setItemsInternal(items));
            }
            if (null != visionSocketServer && !this.jCheckBoxPause.isSelected()) {
                visionSocketServer.publishList(items);
            }
        } finally {
            settingItems = false;
        }
    }

    private void setItemsInternal(List<DetectedItem> items) {
        object2DJPanel1.setItems(items);
        ListSelectionModel lsm = jTableItems.getSelectionModel();
        int origSelectedRow = jTableItems.getSelectedRow();
        int origSelectedRowIndex
                = (origSelectedRow >= 0 && origSelectedRow < jTableItems.getRowCount())
                        ? (int) jTableItems.getValueAt(origSelectedRow, 0) : -1;

        DefaultTableModel model = (DefaultTableModel) jTableItems.getModel();
        model.setRowCount(0);
        for (int i = 0; i < items.size(); i++) {
            DetectedItem item = items.get(i);
            model.addRow(new Object[]{i, item.name, item.x, item.y, Math.toDegrees(item.rotation), item.type});
//            model.setValueAt(item.name, i, 0);
//            model.setValueAt(item.x, i, 1);
//            model.setValueAt(item.y, i, 2);
//            model.setValueAt(Math.toDegrees(item.rotation), i, 3);
//            model.setValueAt(item.type, i, 4);
        }
        autoResizeTableColWidths(jTableItems);

        RowSorter rowSorter = jTableItems.getRowSorter();
        if (null != rowSorter) {
            rowSorter.allRowsChanged();
//            List<RowSorter.SortKey> keys = rowSorter.getSortKeys();
//            for(RowSorter.SortKey k : keys) {
//                rowSorter.toggleSortOrder(k.getColumn());
//                rowSorter.toggleSortOrder(k.getColumn());
//                break;
//            }
        }
        int newSelectedRowIndex
                = (origSelectedRow >= 0 && origSelectedRow < jTableItems.getRowCount())
                        ? (int) jTableItems.getValueAt(origSelectedRow, 0) : -1;
//        System.out.println("newSelectedRowIndex = " + newSelectedRowIndex);
//        System.out.println("origSelectedRowIndex = " + origSelectedRowIndex);
        if (newSelectedRowIndex > 0 && newSelectedRowIndex == origSelectedRowIndex) {
            DefaultListSelectionModel dlsm;
            lsm = jTableItems.getSelectionModel();
            if(lsm instanceof DefaultListSelectionModel) {
                dlsm = (DefaultListSelectionModel) lsm;
            } else {
                dlsm = new DefaultListSelectionModel();
            }
            dlsm.setSelectionInterval(origSelectedRow, origSelectedRow);
            dlsm.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            jTableItems.setSelectionModel(dlsm);
        }
    }

    /**
     * Creates new form Object2DOuterJPanel
     */
    public Object2DOuterJPanel() {
        initComponents();
        this.setItems(object2DJPanel1.getItems());
        jTableItems.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                try {
                    boolean changeFound = false;

                    if (!settingItems) {
                        List<DetectedItem> l = new ArrayList<>();
                        l.addAll(getItems());
                        DetectedItem item = null;
                        for (int i = 0; i < jTableItems.getRowCount(); i++) {
                            int listIndex = (int) jTableItems.getValueAt(i, 0);
                            if (jTableItems.getValueAt(i, 1) == null || jTableItems.getValueAt(i, 1).toString().length() < 1) {
                                continue;
                            }
                            if (listIndex < l.size()) {
                                item = l.get(listIndex);
                            } else {
                                item = null;
                            }
                            if (item == null || item.name == null
                                    || !Objects.equals(item.type, jTableItems.getValueAt(i, 5))
                                    || !Objects.equals(item.name, jTableItems.getValueAt(i, 1))
                                    || Math.abs(item.x - Double.parseDouble(jTableItems.getValueAt(i, 2).toString())) > 0.001
                                    || Math.abs(item.y - Double.parseDouble(jTableItems.getValueAt(i, 3).toString())) > 0.001
                                    || Math.abs(item.rotation - Double.parseDouble(jTableItems.getValueAt(i, 4).toString())) > 0.001) {
                                changeFound = true;
                            } else {
                                continue;
                            }
                            if (item == null) {
                                item = new DetectedItem();
                            }
                            item.name = Objects.toString(jTableItems.getValueAt(i, 1));
                            item.x = Double.parseDouble(jTableItems.getValueAt(i, 2).toString());
                            item.y = Double.parseDouble(jTableItems.getValueAt(i, 3).toString());
                            item.rotation = Math.toRadians(Double.parseDouble(jTableItems.getValueAt(i, 4).toString()));
                            item.type = Objects.toString(jTableItems.getValueAt(i, 5));
                            while (l.size() < listIndex) {
                                l.add(null);
                            }
                            l.set(listIndex, item);
                        }
                        if (changeFound) {
                            setItems(l);
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        });
        jTableItems.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                int selectedRow = jTableItems.getSelectedRow();
                if (selectedRow >= 0 && selectedRow < jTableItems.getRowCount()) {
                    object2DJPanel1.setSelectedItemIndex(
                            (int) (jTableItems.getValueAt(selectedRow, 0)));
                }
            }
        });
//        jComboBoxDisplayAxis.setModel( new DefaultComboBoxModel<>(DisplayAxis.values()));
        setMaxXMaxYText(jTextFieldMaxXMaxY.getText());
        setMinXMinYText(jTextFieldMinXMinY.getText());
        object2DJPanel1.setShowCurrentXY(jCheckBoxShowCurrent.isSelected());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        object2DJPanel1 = new aprs.framework.simview.Object2DJPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableItems = new javax.swing.JTable();
        jButtonAdd = new javax.swing.JButton();
        jButtonDelete = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jCheckBoxSimulated = new javax.swing.JCheckBox();
        jTextFieldHost = new javax.swing.JTextField();
        jTextFieldPort = new javax.swing.JTextField();
        jCheckBoxConnected = new javax.swing.JCheckBox();
        jLabelHost = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jCheckBoxDebug = new javax.swing.JCheckBox();
        jCheckBoxPause = new javax.swing.JCheckBox();
        jButtonRefresh = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jTextFieldMinXMinY = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jTextFieldMaxXMaxY = new javax.swing.JTextField();
        jButtonReset = new javax.swing.JButton();
        jCheckBoxShowRotations = new javax.swing.JCheckBox();
        jComboBoxDisplayAxis = new javax.swing.JComboBox<>();
        jTextFieldFilename = new javax.swing.JTextField();
        jButtonSave = new javax.swing.JButton();
        jButtonLoad = new javax.swing.JButton();
        jButtonCurrent = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jTextFieldCurrentXY = new javax.swing.JTextField();
        jCheckBoxShowCurrent = new javax.swing.JCheckBox();

        object2DJPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        object2DJPanel1.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                object2DJPanel1MouseDragged(evt);
            }
        });
        object2DJPanel1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                object2DJPanel1MousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                object2DJPanel1MouseReleased(evt);
            }
        });

        javax.swing.GroupLayout object2DJPanel1Layout = new javax.swing.GroupLayout(object2DJPanel1);
        object2DJPanel1.setLayout(object2DJPanel1Layout);
        object2DJPanel1Layout.setHorizontalGroup(
            object2DJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 503, Short.MAX_VALUE)
        );
        object2DJPanel1Layout.setVerticalGroup(
            object2DJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jTableItems.setAutoCreateRowSorter(true);
        jTableItems.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "Index", "Name", "X", "Y", "Rotation", "Type"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTableItems.setMaximumSize(new java.awt.Dimension(400, 64));
        jScrollPane1.setViewportView(jTableItems);

        jButtonAdd.setText("Add");
        jButtonAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddActionPerformed(evt);
            }
        });

        jButtonDelete.setText("Delete");
        jButtonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Connection"));
        jPanel1.setMaximumSize(new java.awt.Dimension(407, 32767));

        jCheckBoxSimulated.setText("Simulated");
        jCheckBoxSimulated.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxSimulatedActionPerformed(evt);
            }
        });

        jTextFieldHost.setText("localhost");

        jTextFieldPort.setText("4000");

        jCheckBoxConnected.setText("Connected");
        jCheckBoxConnected.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxConnectedActionPerformed(evt);
            }
        });

        jLabelHost.setText("Host:");

        jLabel1.setText("Port:");

        jCheckBoxDebug.setText("Debug");
        jCheckBoxDebug.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxDebugActionPerformed(evt);
            }
        });

        jCheckBoxPause.setText("Pause");
        jCheckBoxPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxPauseActionPerformed(evt);
            }
        });

        jButtonRefresh.setText("Refresh");
        jButtonRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRefreshActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jCheckBoxSimulated)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxConnected, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxDebug)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxPause))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelHost)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldHost, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonRefresh))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxSimulated)
                    .addComponent(jCheckBoxConnected)
                    .addComponent(jCheckBoxDebug)
                    .addComponent(jCheckBoxPause))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelHost)
                    .addComponent(jTextFieldHost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonRefresh))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel2.setText("Xmin,Ymin : ");

        jTextFieldMinXMinY.setText("200.0, -315.0");
        jTextFieldMinXMinY.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldMinXMinYActionPerformed(evt);
            }
        });

        jLabel3.setText("Xmax,Ymax");

        jTextFieldMaxXMaxY.setText("700.0, 315.0");
        jTextFieldMaxXMaxY.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldMaxXMaxYActionPerformed(evt);
            }
        });

        jButtonReset.setText("Reset");
        jButtonReset.setEnabled(false);
        jButtonReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonResetActionPerformed(evt);
            }
        });

        jCheckBoxShowRotations.setText("Show Rotations");
        jCheckBoxShowRotations.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxShowRotationsActionPerformed(evt);
            }
        });

        jComboBoxDisplayAxis.setModel(new DefaultComboBoxModel<>(DisplayAxis.values()));
        jComboBoxDisplayAxis.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxDisplayAxisActionPerformed(evt);
            }
        });

        jButtonSave.setText("Save");
        jButtonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveActionPerformed(evt);
            }
        });

        jButtonLoad.setText("Load");
        jButtonLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLoadActionPerformed(evt);
            }
        });

        jButtonCurrent.setText("Current");
        jButtonCurrent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCurrentActionPerformed(evt);
            }
        });

        jLabel4.setText("Current");

        jTextFieldCurrentXY.setText("0.0,0.0");
        jTextFieldCurrentXY.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldCurrentXYActionPerformed(evt);
            }
        });

        jCheckBoxShowCurrent.setText("Show");
        jCheckBoxShowCurrent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxShowCurrentActionPerformed(evt);
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
                        .addComponent(jTextFieldFilename)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonSave)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonLoad))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(object2DJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jButtonReset)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonCurrent)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonDelete)
                                .addGap(4, 4, 4)
                                .addComponent(jButtonAdd))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(jCheckBoxShowRotations)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jComboBoxDisplayAxis, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGroup(layout.createSequentialGroup()
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel2)
                                        .addGroup(layout.createSequentialGroup()
                                            .addComponent(jCheckBoxShowCurrent)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(jLabel4))
                                        .addComponent(jLabel3))
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jTextFieldMaxXMaxY, javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(jTextFieldCurrentXY, javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(jTextFieldMinXMinY, javax.swing.GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE))))
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldFilename, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonSave)
                    .addComponent(jButtonLoad))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 129, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jCheckBoxShowRotations)
                            .addComponent(jComboBoxDisplayAxis, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(jTextFieldCurrentXY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jCheckBoxShowCurrent))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(jTextFieldMaxXMaxY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(jTextFieldMinXMinY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButtonDelete)
                            .addComponent(jButtonAdd)
                            .addComponent(jButtonReset)
                            .addComponent(jButtonCurrent))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(object2DJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jButtonAdd, jButtonDelete});

    }// </editor-fold>//GEN-END:initComponents


    private void jCheckBoxSimulatedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxSimulatedActionPerformed
        this.jCheckBoxConnected.setSelected(false);
        if (jCheckBoxSimulated.isSelected()) {
            jTextFieldHost.setEditable(false);
            jTextFieldHost.setEnabled(false);
            jLabelHost.setEnabled(false);
            jButtonAdd.setEnabled(true);
            jButtonDelete.setEnabled(true);
            jButtonReset.setEnabled(true);
        } else {
            jTextFieldHost.setEditable(true);
            jTextFieldHost.setEnabled(true);
            jButtonAdd.setEnabled(false);
            jButtonDelete.setEnabled(false);
            jLabelHost.setEnabled(true);
            jButtonReset.setEnabled(false);
        }
        if (null != visionSocketServer) {
            visionSocketServer.close();
            visionSocketServer = null;
        }

    }//GEN-LAST:event_jCheckBoxSimulatedActionPerformed

    private VisionSocketServer visionSocketServer = null;
    private VisionSocketClient visionSocketClient = null;

    private void jCheckBoxConnectedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxConnectedActionPerformed
        jButtonReset.setEnabled(false);
        if (this.jCheckBoxConnected.isSelected()) {
            connect();
        } else {
            if (this.jCheckBoxSimulated.isSelected()) {
                jButtonReset.setEnabled(true);
            }
            disconnect();
        }
    }//GEN-LAST:event_jCheckBoxConnectedActionPerformed

    private void disconnect() {
        if (null != visionSocketClient) {
            try {
                visionSocketClient.close();
            } catch (Exception ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
            visionSocketClient.removeListListener(this);
            visionSocketClient = null;
        }
        if (null != visionSocketServer) {
            visionSocketServer.close();
            visionSocketServer = null;
        }
    }

    private void connect() throws NumberFormatException {
        if (this.jCheckBoxSimulated.isSelected()) {
            try {
                int port = Integer.parseInt(this.jTextFieldPort.getText());
                if (null != visionSocketServer && visionSocketServer.getPort() != port) {
                    disconnect();
                }
                if (null == visionSocketServer) {
                    visionSocketServer = new VisionSocketServer(port);
                }
                visionSocketServer.setDebug(this.jCheckBoxDebug.isSelected());
                visionSocketServer.publishList(getItems());
            } catch (IOException ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            int port = Integer.valueOf(jTextFieldPort.getText());
            String host = jTextFieldHost.getText();
            if (null != visionSocketClient) {
                if (visionSocketClient.isConnected()
                        && port == visionSocketClient.getPort()
                        && Objects.equals(visionSocketClient.getHost(), host)) {
                    return;
                }
                try {
                    visionSocketClient.close();
                } catch (Exception ex) {
                    Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            visionSocketClient = new VisionSocketClient();
            Map<String, String> argsMap = DbSetupBuilder.getDefaultArgsMap();
            argsMap.put("--visionport", jTextFieldPort.getText());
            argsMap.put("--visionhost", host);
            visionSocketClient.setDebug(this.jCheckBoxDebug.isSelected());
            visionSocketClient.start(argsMap);
            if (!visionSocketClient.isConnected()) {
                jCheckBoxConnected.setSelected(false);
                try {
                    visionSocketClient.close();
                } catch (Exception ex) {
                    Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
                visionSocketClient = null;
                return;
            }
            visionSocketClient.addListener(this);
        }
    }

    private void jButtonAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddActionPerformed
        List<DetectedItem> l = new ArrayList<>();
        l.addAll(getItems());
        DetectedItem item = new DetectedItem("item_" + (l.size() + 1), 0,
                (object2DJPanel1.getMaxX() + object2DJPanel1.getMinX()) / 2.0,
                (object2DJPanel1.getMaxY() + object2DJPanel1.getMinY()) / 2.0
        );
        l.add(item);
        setItems(l);
    }//GEN-LAST:event_jButtonAddActionPerformed

    private void jButtonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeleteActionPerformed
        int row = jTableItems.getSelectedRow();
        List<DetectedItem> oldList = getItems();
        if (row >= 0 && row < oldList.size()) {
            List<DetectedItem> l = new ArrayList<>();
            l.addAll(getItems());
            l.remove(jTableItems.getSelectedRow());
            setItems(l);
        }
    }//GEN-LAST:event_jButtonDeleteActionPerformed


    private void object2DJPanel1MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MouseDragged
        double scale = object2DJPanel1.getScale();
        double min_x = object2DJPanel1.getMinX();
        double max_x = object2DJPanel1.getMaxX();
        double min_y = object2DJPanel1.getMinY();
        double max_y = object2DJPanel1.getMaxY();
        if (null != draggedItem) {
            switch (object2DJPanel1.getDisplayAxis()) {
                case POS_X_POS_Y:
                    draggedItem.x = ((evt.getX() - 15) / scale) + min_x;
                    draggedItem.y = max_y - ((evt.getY() - 20) / scale);
                    break;

                case POS_Y_NEG_X:
                    draggedItem.x = ((evt.getY() - 20) / scale) + min_x;
                    draggedItem.y = ((evt.getX() - 15) / scale) + min_y;
                    break;

                case NEG_X_NEG_Y:
                    draggedItem.x = max_x - ((evt.getX() - 15) / scale);
                    draggedItem.y = ((evt.getY() - 20) / scale) + min_y;
                    break;

                case NEG_Y_POS_X:
                    draggedItem.x = max_x - ((evt.getY() - 20) / scale);
                    draggedItem.y = max_y - ((evt.getX() - 15) / scale);
                    break;
            }
//            draggedItem.x = ((evt.getX() - 15) / scale) + min_x;
//            draggedItem.y = max_y - ((evt.getY() - 20) / scale);
            this.setItems(this.getItems());
        }
    }//GEN-LAST:event_object2DJPanel1MouseDragged

    private DetectedItem draggedItem = null;

    private boolean insideItem(DetectedItem item, int x, int y) {
        if (null == item || null == item.displayRect || null == item.displayTransform) {
            return false;
        }
        boolean inside = false;
        try {
            Point2D newPoint = item.relTransform.inverseTransform(new Point2D.Double(x, y), null);
            System.out.println("newPoint = " + newPoint.getX() + ", " + newPoint.getY());
            inside = item.displayRect.contains(newPoint);
        } catch (NoninvertibleTransformException ex) {
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        return inside;
    }

    private void object2DJPanel1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MousePressed
        List<DetectedItem> items = this.getItems();
        double scale = object2DJPanel1.getScale();
        double min_x = object2DJPanel1.getMinX();
        double max_y = object2DJPanel1.getMaxY();
        draggedItem = null;
        DetectedItem closestItem = null;
        double minDist = Double.POSITIVE_INFINITY;
        int minIndex = -1;
        for (int i = 0; i < items.size(); i++) {
            DetectedItem item = items.get(i);
            double rel_x = (item.x - min_x) * scale + 15;
            double rel_y = (max_y - item.y) * scale + 20;
            int x = evt.getX();
            int y = evt.getY();

            double diff_x = rel_x - x;
            double diff_y = rel_y - y;
            double dist = Math.sqrt(diff_x * diff_x + diff_y * diff_y);
            if (dist < minDist) {
                if (insideItem(item, x, y)) {
                    minDist = dist;
                    closestItem = item;
                    minIndex = i;
                }
            }
        }
        if (minIndex >= 0) {
            jTableItems.getSelectionModel().setSelectionInterval(minIndex, minIndex);
            object2DJPanel1.setSelectedItemIndex(minIndex);
        }
        draggedItem = closestItem;
    }//GEN-LAST:event_object2DJPanel1MousePressed

    private void object2DJPanel1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MouseReleased
        draggedItem = null;
    }//GEN-LAST:event_object2DJPanel1MouseReleased

    private void jTextFieldMaxXMaxYActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldMaxXMaxYActionPerformed
        String txt = jTextFieldMaxXMaxY.getText();
        setMaxXMaxYText(txt);
    }//GEN-LAST:event_jTextFieldMaxXMaxYActionPerformed

    public void setMaxXMaxYText(String txt) throws NumberFormatException {
        String vals[] = txt.split(",");
        if (vals.length == 2) {
            double newMaxX = Double.valueOf(vals[0]);
            double newMaxY = Double.valueOf(vals[1]);
            object2DJPanel1.setMaxX(newMaxX);
            object2DJPanel1.setMaxY(newMaxY);
        } else {
            System.err.println("Bad xmax,ymax = " + txt);
        }
    }

    private void jTextFieldMinXMinYActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldMinXMinYActionPerformed
        String txt = jTextFieldMinXMinY.getText();
        setMinXMinYText(txt);
    }//GEN-LAST:event_jTextFieldMinXMinYActionPerformed

    private void jCheckBoxDebugActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxDebugActionPerformed
        if (null != visionSocketServer) {
            visionSocketServer.setDebug(this.jCheckBoxDebug.isSelected());
        }
        if (null != visionSocketClient) {
            visionSocketClient.setDebug(this.jCheckBoxDebug.isSelected());
        }
    }//GEN-LAST:event_jCheckBoxDebugActionPerformed

    private void jButtonResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonResetActionPerformed
        this.setItems(Object2DJPanel.EXAMPLES_ITEMS_LIST);
    }//GEN-LAST:event_jButtonResetActionPerformed

    private void jCheckBoxShowRotationsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxShowRotationsActionPerformed
        object2DJPanel1.setViewRotations(this.jCheckBoxShowRotations.isSelected());
    }//GEN-LAST:event_jCheckBoxShowRotationsActionPerformed

    private void jCheckBoxPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxPauseActionPerformed
        if (!this.jCheckBoxPause.isSelected()) {
            if (null != visionSocketServer && !this.jCheckBoxPause.isSelected()) {
                visionSocketServer.publishList(this.getItems());
            }
        }
    }//GEN-LAST:event_jCheckBoxPauseActionPerformed

    private void jButtonRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRefreshActionPerformed
        if (null != visionSocketServer && !this.jCheckBoxPause.isSelected()) {
            this.setItems(object2DJPanel1.getItems());
            visionSocketServer.publishList(this.getItems());
        }
    }//GEN-LAST:event_jButtonRefreshActionPerformed

    private void jComboBoxDisplayAxisActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxDisplayAxisActionPerformed
        object2DJPanel1.setDisplayAxis((DisplayAxis) jComboBoxDisplayAxis.getSelectedItem());
    }//GEN-LAST:event_jComboBoxDisplayAxisActionPerformed

    private void loadFile(File f) throws IOException {
        String line = Files.lines(f.toPath()).skip(1).map(String::trim).collect(Collectors.joining(","));
        this.setItems(VisionSocketClient.lineToList(line));
        jTextFieldFilename.setText(f.getCanonicalPath());
    }

    private void saveFile(File f) throws IOException {

        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            pw.println("name,rotation,x,y,score,type");
            for (DetectedItem item : getItems()) {
                pw.println(item.name + "," + item.rotation + "," + item.x + "," + item.y + "," + item.score + "," + item.type);
            }
        }
        jTextFieldFilename.setText(f.getCanonicalPath());
    }

    private void jButtonLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoadActionPerformed
        String fname = jTextFieldFilename.getText();
        File dir = new File(System.getProperty("user.home"));
        File f = null;
        if (null != fname && fname.length() > 0) {
            f = new File(fname);
            if (f.getParentFile().exists()) {
                dir = f.getParentFile();
            }
        }
        JFileChooser chooser = new JFileChooser(dir);
        if (null != f && f.exists()) {
            chooser.setSelectedFile(f);
        }
        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(this)) {
            try {
                loadFile(chooser.getSelectedFile());
            } catch (IOException ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jButtonLoadActionPerformed

    private void jButtonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSaveActionPerformed
        String fname = jTextFieldFilename.getText();
        File dir = new File(System.getProperty("user.home"));
        File f = null;
        if (null != fname && fname.length() > 0) {
            f = new File(fname);
            if (f.getParentFile().exists()) {
                dir = f.getParentFile();
            }
        }
        JFileChooser chooser = new JFileChooser(dir);
        if (null != f && f.exists()) {
            chooser.setSelectedFile(f);
        }
        if (JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(this)) {
            try {
                saveFile(chooser.getSelectedFile());
            } catch (IOException ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jButtonSaveActionPerformed

    private void jTextFieldCurrentXYActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldCurrentXYActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldCurrentXYActionPerformed

    double currentX = 0.0;
    double currentY = 0.0;

    private AprsJFrame aprsJFrame;

    /**
     * Get the value of aprsJFrame
     *
     * @return the value of aprsJFrame
     */
    public AprsJFrame getAprsJFrame() {
        return aprsJFrame;
    }

    /**
     * Set the value of aprsJFrame
     *
     * @param aprsJFrame new value of aprsJFrame
     */
    public void setAprsJFrame(AprsJFrame aprsJFrame) {
        this.aprsJFrame = aprsJFrame;
    }

    public void connectCurrentPosition() {
        if (null != aprsJFrame) {
            aprsJFrame.addCurrentPoseListener(this);
        }
    }

    public void disconnectCurrentPosition() {
        if (null != aprsJFrame) {
            aprsJFrame.removeCurrentPoseListener(this);
        }
    }

    private void jButtonCurrentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCurrentActionPerformed
        List<DetectedItem> items = this.getItems();
        int selectedIndex = object2DJPanel1.getSelectedItemIndex();
        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            DetectedItem item = items.get(selectedIndex);
            item.x = currentX;
            item.y = currentY;
        }
    }//GEN-LAST:event_jButtonCurrentActionPerformed

    private void jCheckBoxShowCurrentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxShowCurrentActionPerformed
        setTrackCurrentPos(jCheckBoxShowCurrent.isSelected());
    }//GEN-LAST:event_jCheckBoxShowCurrentActionPerformed

    public void setTrackCurrentPos(boolean v) {
        object2DJPanel1.setShowCurrentXY(v);
        if (v) {
            connectCurrentPosition();
        } else {
            disconnectCurrentPosition();
        }
    }

    public void setMinXMinYText(String txt) throws NumberFormatException {
        String vals[] = txt.split(",");
        if (vals.length == 2) {
            double newMinX = Double.valueOf(vals[0]);
            double newMinY = Double.valueOf(vals[1]);
            object2DJPanel1.setMinX(newMinX);
            object2DJPanel1.setMinY(newMinY);
        } else {
            System.err.println("Bad MinX,MinY = " + txt);
        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAdd;
    private javax.swing.JButton jButtonCurrent;
    private javax.swing.JButton jButtonDelete;
    private javax.swing.JButton jButtonLoad;
    private javax.swing.JButton jButtonRefresh;
    private javax.swing.JButton jButtonReset;
    private javax.swing.JButton jButtonSave;
    private javax.swing.JCheckBox jCheckBoxConnected;
    private javax.swing.JCheckBox jCheckBoxDebug;
    private javax.swing.JCheckBox jCheckBoxPause;
    private javax.swing.JCheckBox jCheckBoxShowCurrent;
    private javax.swing.JCheckBox jCheckBoxShowRotations;
    private javax.swing.JCheckBox jCheckBoxSimulated;
    private javax.swing.JComboBox<DisplayAxis> jComboBoxDisplayAxis;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabelHost;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTableItems;
    private javax.swing.JTextField jTextFieldCurrentXY;
    private javax.swing.JTextField jTextFieldFilename;
    private javax.swing.JTextField jTextFieldHost;
    private javax.swing.JTextField jTextFieldMaxXMaxY;
    private javax.swing.JTextField jTextFieldMinXMinY;
    private javax.swing.JTextField jTextFieldPort;
    private aprs.framework.simview.Object2DJPanel object2DJPanel1;
    // End of variables declaration//GEN-END:variables

    @Override
    public void dispose() {
        System.err.println("com.github.wshackle.visiontodb.Object2DOuterJPanel.dispose default implementation called.");
    }

    private File propertiesFile;

    @Override
    public void setPropertiesFile(File f) {
        this.propertiesFile = f;
    }

    private static String makeShortPath(File f, String str) {
        try {
            String canString = new File(str).getCanonicalPath();
            String relString = Paths.get(f.getParentFile().getCanonicalPath()).relativize(Paths.get(canString)).toString();
            if (relString.length() <= canString.length()) {
                return relString;
            }
            return canString;
        } catch (IOException iOException) {
        }
        return str;
    }

    @Override
    public void saveProperties() throws IOException {
        if (null != propertiesFile) {
            propertiesFile.getParentFile().mkdirs();
            Properties props = new Properties();
            props.put("--visionport", jTextFieldPort.getText());
            props.put("--visionhost", jTextFieldHost.getText());
            props.put("simulated", Boolean.toString(jCheckBoxSimulated.isSelected()));
            props.put("connected", Boolean.toString(jCheckBoxConnected.isSelected()));
            props.put("trackcurrentpos", Boolean.toString(jCheckBoxShowCurrent.isSelected()));
            props.put("showrotations", Boolean.toString(jCheckBoxShowRotations.isSelected()));
            props.put("xmaxymax", jTextFieldMaxXMaxY.getText());
            props.put("xminymin", jTextFieldMinXMinY.getText());
            String dataFileTxt = jTextFieldFilename.getText();
            String datafileShort = makeShortPath(propertiesFile, dataFileTxt);
            props.put("datafile", datafileShort);
            DisplayAxis displayAxis = object2DJPanel1.getDisplayAxis();
            props.put("displayAxis", displayAxis.toString());
            List<DetectedItem> l = getItems();
            if (null != l && l.size() > 0) {
                props.put(ITEMS_PROPERTY_NAME, VisionSocketServer.listToLine(l));
            }
            try (FileWriter fw = new FileWriter(propertiesFile)) {
                props.store(fw, "");
            }
        }
    }
    private static final String ITEMS_PROPERTY_NAME = "items";

    @Override
    public void loadProperties() throws IOException {
        if (null != propertiesFile && propertiesFile.exists()) {
            Properties props = new Properties();
            try (FileReader fr = new FileReader(propertiesFile)) {
                props.load(fr);
            }
            String itemsLine = props.getProperty(ITEMS_PROPERTY_NAME);
            if (null != itemsLine && itemsLine.length() > 0) {
                List<DetectedItem> l = VisionSocketClient.lineToList(itemsLine);
                if (null != l && l.size() > 0) {
                    setItems(l);
                }
            }
            String portString = props.getProperty("--visionport");
            try {
                if (null != portString && portString.length() > 0) {
                    int port = Integer.parseInt(portString);
                    jTextFieldPort.setText(Integer.toString(port));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            String hostString = props.getProperty("--visionhost");
            try {
                if (null != hostString && hostString.length() > 0) {
                    jTextFieldHost.setText(hostString);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            String simulatedString = props.getProperty("simulated");
            if (null != simulatedString && simulatedString.length() > 0) {
                jCheckBoxSimulated.setSelected(Boolean.valueOf(simulatedString));
            }
            String datafileString = props.getProperty("datafile");
            if (null != datafileString && datafileString.length() > 0) {
                jTextFieldFilename.setText(datafileString);
                File f = new File(datafileString);
                if (f.exists() && f.canRead()) {
                    jTextFieldFilename.setText(f.getCanonicalPath());
                    loadFile(f);
                } else {
                    String fullPath = propertiesFile.getParentFile().toPath().resolve(datafileString).normalize().toString();
//                    System.out.println("fullPath = " + fullPath);
                    f = new File(fullPath);
                    if (f.exists() && f.canRead()) {
                        jTextFieldFilename.setText(f.getCanonicalPath());
                        loadFile(f);
                    } else {
                        String fullPath2 = propertiesFile.getParentFile().toPath().resolveSibling(datafileString).normalize().toString();
//                        System.out.println("fullPath = " + fullPath2);
                        f = new File(fullPath2);
                        if (f.exists() && f.canRead()) {
                            jTextFieldFilename.setText(f.getCanonicalPath());
                            loadFile(f);
                        }
                    }
                }
            }
            String xmaxymaxString = props.getProperty("xmaxymax");
            if (null != xmaxymaxString) {
                setMaxXMaxYText(xmaxymaxString);
                jTextFieldMaxXMaxY.setText(xmaxymaxString);
            }
            String xminyminString = props.getProperty("xminymin");
            if (null != xminyminString) {
                setMinXMinYText(xminyminString);
                jTextFieldMinXMinY.setText(xminyminString);
            }
            String connectedString = props.getProperty("connected");
            if (null != connectedString && connectedString.length() > 0) {
                boolean connected = Boolean.valueOf(connectedString);
                jCheckBoxConnected.setSelected(connected);
                if (connected) {
                    connect();
                }
            }
            String displayAxisString = props.getProperty("displayAxis");
            if (displayAxisString != null && displayAxisString.length() > 0) {
                DisplayAxis displayAxis = DisplayAxis.valueOf(displayAxisString);
                jComboBoxDisplayAxis.setSelectedItem(displayAxis);
                object2DJPanel1.setDisplayAxis(displayAxis);
            }
            String trackCurrentPosString = props.getProperty("trackcurrentpos");
            if (trackCurrentPosString != null && trackCurrentPosString.length() > 0) {
                boolean trackCurrentPos = Boolean.valueOf(trackCurrentPosString);
                jCheckBoxShowCurrent.setSelected(trackCurrentPos);
                this.setTrackCurrentPos(trackCurrentPos);
            }
            //showrotations
            String showRotationsString = props.getProperty("showrotations");
            if (showRotationsString != null && showRotationsString.length() > 0) {
                boolean showRotations = Boolean.valueOf(showRotationsString);
                jCheckBoxShowRotations.setSelected(showRotations);
                object2DJPanel1.setViewRotations(showRotations);
            }
        }
    }

    @Override
    public File getPropertiesFile() {
        return propertiesFile;
    }

    @Override
    public void visionClientUpdateRecieved(List<DetectedItem> l, String line) {
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            setItems(l);
        } else {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    setItems(l);
                }
            });
        }
    }

    boolean lastIsHoldingObjectExpected = false;
    int captured_item_index = -1;

    @Override
    public void accept(PendantClientJPanel panel, PoseType pose) {
        PointType ptIn = pose.getPoint();

        PointType uncorrectedPoint = aprsJFrame.reverseCorrectPoint(ptIn);
        currentX = uncorrectedPoint.getX().doubleValue();
        currentY = uncorrectedPoint.getY().doubleValue();
        jTextFieldCurrentXY.setText(String.format("%.3f,%.3f", currentX, currentY));
        object2DJPanel1.setCurrentX(currentX);
        object2DJPanel1.setCurrentY(currentY);
        boolean isHoldingObjectExpected = panel.isHoldingObjectExpected();
        List<DetectedItem> l = getItems();
        if (this.jCheckBoxSimulated.isSelected()) {

            if (isHoldingObjectExpected && !lastIsHoldingObjectExpected) {

                double min_dist = Double.POSITIVE_INFINITY;
                int min_dist_index = -1;
                for (int i = 0; i < l.size(); i++) {
                    DetectedItem item = l.get(i);
                    double dist = item.dist(currentX, currentY);
                    if (dist < min_dist) {
                        min_dist_index = i;
                        min_dist = dist;
                    }
                }
                if (min_dist < 3.0 && min_dist_index >= 0) {
                    captured_item_index = min_dist_index;
                }
            }
        }
        if (!isHoldingObjectExpected) {
            captured_item_index = -1;
        }
        if (this.jCheckBoxSimulated.isSelected()) {
            if (captured_item_index >= 0 && captured_item_index < l.size()) {
                DetectedItem item = l.get(captured_item_index);
                item.x = currentX;
                item.y = currentY;
                l.set(captured_item_index, item);
                setItems(l);
            }
        }
    }
}
