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
import aprs.framework.Utils;
import static aprs.framework.Utils.autoResizeTableColWidths;
import aprs.framework.database.DetectedItem;
import aprs.framework.database.DbSetupBuilder;
import static aprs.framework.simview.DisplayAxis.POS_X_POS_Y;
import aprs.framework.spvision.VisionSocketClient;
import aprs.framework.spvision.VisionSocketServer;
import crcl.base.CRCLCommandType;
import crcl.base.CRCLStatusType;
import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.ui.client.PendantClientJPanel;
import crcl.utils.CRCLPosemath;
import crcl.utils.CRCLSocket;
import static diagapplet.CodeGen.ServerInfo.count;
import java.awt.event.ActionEvent;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.xml.bind.JAXBException;
import rcs.posemath.PmCartesian;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class Object2DOuterJPanel extends javax.swing.JPanel implements Object2DJFrameInterface, VisionSocketClient.VisionSocketClientListener, PendantClientJPanel.CurrentPoseListener {

    public List<DetectedItem> getItems() {
        return object2DJPanel1.getItems();
    }

    public List<DetectedItem> getOutputItems() {
        return object2DJPanel1.getOutputItems();
    }

    private volatile boolean settingItems = false;

    @Override
    public void takeSnapshot(File f, PoseType pose, String label) throws IOException {
        try {
            this.object2DJPanel1.takeSnapshot(f, pose, label);
            File csvDir = new File(f.getParentFile(), "csv");
            csvDir.mkdirs();
            saveFile(new File(csvDir, f.getName() + ".csv"));
            File xmlDir = new File(f.getParentFile(), "crclStatusXml");
            xmlDir.mkdirs();
            String xmlString = CRCLSocket.getUtilSocket().statusToPrettyString(aprsJFrame.getCurrentStatus(), false);
            File xmlFile = new File(xmlDir, f.getName() + "-status.xml");
            try (FileWriter fw = new FileWriter(xmlFile)) {
                fw.write(xmlString);
            }
        } catch (JAXBException ex) {
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void refresh(boolean loadFile) {
        if (jCheckBoxSimulated.isSelected()) {
            boolean fileLoaded = false;
            if (loadFile) {
                String fname = jTextFieldFilename.getText().trim();
                File f = new File(fname);
                if (f.exists() && f.canRead()) {
                    try {
                        loadFile(f);
                        fileLoaded = true;
                    } catch (IOException ex) {
                        Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            if (!fileLoaded && null != visionSocketServer && !this.jCheckBoxPause.isSelected()) {
                this.setItems(object2DJPanel1.getItems());
                publishCurrentItems();
            }
        }
    }

    public void setItems(List<DetectedItem> items) {
        setItems(items, true);
    }

//    static public Map<String, Integer> countNames(List<DetectedItem> l) {
//        return l.stream()
//                .collect(Collectors.groupingBy(DetectedItem::getName, Collectors.summingInt(x -> 1)));
//    }

    private volatile Map<String, Integer> origNamesMap = null;

    public void setItems(List<DetectedItem> items, boolean publish) {
        settingItems = true;
//        Map<String, Integer> namesMap = countNames(items);
//        if (null != origNamesMap) {
//            for (Entry<String, Integer> entry : namesMap.entrySet()) {
//                String name = entry.getKey();
//                int count = entry.getValue();
//                int origCount = origNamesMap.getOrDefault(name, 0);
//                if (count > origCount) {
//                    System.err.println("name =" + name + ", count = " + count + ", origCount=" + origCount);
//                }
//            }
//        } else {
//            origNamesMap = namesMap;
//        }
        Utils.runOnDispatchThread(() -> {
            setItemsInternal(items);
            settingItems = false;
        });
        if (publish) {
            if (null != visionSocketServer && !this.jCheckBoxPause.isSelected()) {
                visionSocketServer.publishList(items);
            }
        }
    }

    public void setOutputItems(List<DetectedItem> items) {
        settingItems = true;
        Utils.runOnDispatchThread(() -> {
            setOutputItemsInternal(items);
            settingItems = false;
        });
    }

    private void setItemsInternal(List<DetectedItem> items) {
        object2DJPanel1.setItems(items);
        if (!object2DJPanel1.isShowOutputItems()) {
            if(object2DJPanel1.isShowAddedSlotPositions()) {
                loadItemsToTable(object2DJPanel1.getItemsWithAddedSlots(), jTableItems);
            } else {
                loadItemsToTable(items, jTableItems);
            }
        }
    }

    private void setOutputItemsInternal(List<DetectedItem> items) {
        object2DJPanel1.setOutputItems(items);
        if (object2DJPanel1.isShowOutputItems()) {
            if(object2DJPanel1.isShowAddedSlotPositions()) {
                loadItemsToTable(object2DJPanel1.getOutputItemsWithAddedSlots(), jTableItems);
            } else {
                loadItemsToTable(items, jTableItems);
            }
        }
    }

    private void loadItemsToTable(List<DetectedItem> items, JTable jtable) {
        int origSelectedRow = jtable.getSelectedRow();
        int origSelectedRowIndex
                = (origSelectedRow >= 0 && origSelectedRow < jtable.getRowCount())
                ? (int) jtable.getValueAt(origSelectedRow, 0) : -1;

        RowSorter rowSorter = jtable.getRowSorter();
        if (null != rowSorter) {
            jtable.setRowSorter(null);
        }
        DefaultTableModel model = (DefaultTableModel) jtable.getModel();
        model.setRowCount(0);
        for (int i = 0; i < items.size(); i++) {
            DetectedItem item = items.get(i);
            model.addRow(new Object[]{i, item.getName(), item.x, item.y, Math.toDegrees(item.getRotation()), item.getType(), item.getScore()});
        }
        autoResizeTableColWidths(jtable);
        if (null != rowSorter) {
            jtable.setRowSorter(rowSorter);
            rowSorter.allRowsChanged();
        }
        int newSelectedRowIndex
                = (origSelectedRow >= 0 && origSelectedRow < jtable.getRowCount())
                ? (int) jtable.getValueAt(origSelectedRow, 0) : -1;
        if (newSelectedRowIndex > 0 && newSelectedRowIndex == origSelectedRowIndex) {
            DefaultListSelectionModel dlsm;
            ListSelectionModel lsm = jtable.getSelectionModel();
            if (lsm instanceof DefaultListSelectionModel) {
                dlsm = (DefaultListSelectionModel) lsm;
            } else {
                dlsm = new DefaultListSelectionModel();
            }
            dlsm.setSelectionInterval(origSelectedRow, origSelectedRow);
            dlsm.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            jtable.setSelectionModel(dlsm);
        }
    }

    public List<DetectedItem> computeAbsSlotPositions(List<DetectedItem> l) {
        return object2DJPanel1.computeAbsSlotPositions(l);
    }
    
    /**
     * Creates new form Object2DOuterJPanel
     */
    public Object2DOuterJPanel() {
        initComponents();
        this.setItemsInternal(object2DJPanel1.getItems());
        jTableItems.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                try {
                    boolean changeFound = false;

                    if (!settingItems && !object2DJPanel1.isShowOutputItems()) {
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
                            if (item == null || item.getName() == null
                                    || !Objects.equals(item.getType(), jTableItems.getValueAt(i, 5))
                                    || !Objects.equals(item.getName(), jTableItems.getValueAt(i, 1))
                                    || Math.abs(item.x - Double.parseDouble(jTableItems.getValueAt(i, 2).toString())) > 0.001
                                    || Math.abs(item.y - Double.parseDouble(jTableItems.getValueAt(i, 3).toString())) > 0.001
                                    || Math.abs(item.getRotation() - Double.parseDouble(jTableItems.getValueAt(i, 4).toString())) > 0.001
                                    || Math.abs(item.getScore() - Double.parseDouble(jTableItems.getValueAt(i, 6).toString())) > 0.001) {
                                changeFound = true;
                            } else {
                                continue;
                            }
                            String name = Objects.toString(jTableItems.getValueAt(i, 1));
                            if (item == null || !item.getName().equals(name)) {
                                item = new DetectedItem(Objects.toString(jTableItems.getValueAt(i, 1)));
                            }
                            item.x = Double.parseDouble(jTableItems.getValueAt(i, 2).toString());
                            item.y = Double.parseDouble(jTableItems.getValueAt(i, 3).toString());
                            item.setRotation(Math.toRadians(Double.parseDouble(jTableItems.getValueAt(i, 4).toString())));
                            item.setType(Objects.toString(jTableItems.getValueAt(i, 5)));
                            item.setScore(Double.parseDouble(jTableItems.getValueAt(i, 6).toString()));
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
        setMaxXMaxYText(jTextFieldMaxXMaxY.getText().trim());
        setMinXMinYText(jTextFieldMinXMinY.getText().trim());
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
        jTextFieldFilename = new javax.swing.JTextField();
        jButtonSave = new javax.swing.JButton();
        jButtonLoad = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanelOptionsTab = new javax.swing.JPanel();
        jTextFieldCurrentXY = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jButtonOffsetAll = new javax.swing.JButton();
        jButtonReset = new javax.swing.JButton();
        jCheckBoxShowRotations = new javax.swing.JCheckBox();
        jButtonCurrent = new javax.swing.JButton();
        jCheckBoxShowCurrent = new javax.swing.JCheckBox();
        jComboBoxDisplayAxis = new javax.swing.JComboBox<>();
        jButtonAdd = new javax.swing.JButton();
        jTextFieldMaxXMaxY = new javax.swing.JTextField();
        jCheckBoxSeparateNames = new javax.swing.JCheckBox();
        jCheckBoxAutoscale = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jTextFieldMinXMinY = new javax.swing.JTextField();
        jButtonDelete = new javax.swing.JButton();
        jCheckBoxAddSlots = new javax.swing.JCheckBox();
        jPanelConnectionsTab = new javax.swing.JPanel();
        jCheckBoxSimulated = new javax.swing.JCheckBox();
        jTextFieldHost = new javax.swing.JTextField();
        jTextFieldPort = new javax.swing.JTextField();
        jCheckBoxConnected = new javax.swing.JCheckBox();
        jLabelHost = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jCheckBoxDebug = new javax.swing.JCheckBox();
        jCheckBoxPause = new javax.swing.JCheckBox();
        jButtonRefresh = new javax.swing.JButton();
        jPanelSimulationTab = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jTextFieldSimulationUpdateTime = new javax.swing.JTextField();
        jCheckBoxShuffleSimulatedUpdates = new javax.swing.JCheckBox();
        jCheckBoxSimulationUpdateAsNeeded = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        jTextFieldSimDropRate = new javax.swing.JTextField();
        jCheckBoxAddPosNoise = new javax.swing.JCheckBox();
        jLabel7 = new javax.swing.JLabel();
        jTextFieldPosNoise = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jTextFieldRotNoise = new javax.swing.JTextField();
        jCheckBoxViewOutput = new javax.swing.JCheckBox();
        jLabel9 = new javax.swing.JLabel();
        jTextFieldPickupDist = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jTextFieldDropOffThreshold = new javax.swing.JTextField();

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
            .addGap(0, 442, Short.MAX_VALUE)
        );
        object2DJPanel1Layout.setVerticalGroup(
            object2DJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jTableItems.setAutoCreateRowSorter(true);
        jTableItems.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "Index", "Name", "X", "Y", "Rotation", "Type", "Score"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true, true, true, true
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

        jTextFieldCurrentXY.setText("0.0,0.0");
        jTextFieldCurrentXY.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldCurrentXYActionPerformed(evt);
            }
        });

        jLabel2.setText("Xmin,Ymin : ");

        jButtonOffsetAll.setText("Offset All");
        jButtonOffsetAll.setEnabled(false);
        jButtonOffsetAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOffsetAllActionPerformed(evt);
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

        jButtonCurrent.setText("Current");
        jButtonCurrent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCurrentActionPerformed(evt);
            }
        });

        jCheckBoxShowCurrent.setText("Show");
        jCheckBoxShowCurrent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxShowCurrentActionPerformed(evt);
            }
        });

        jComboBoxDisplayAxis.setModel(new DefaultComboBoxModel<>(DisplayAxis.values()));
        jComboBoxDisplayAxis.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxDisplayAxisActionPerformed(evt);
            }
        });

        jButtonAdd.setText("Add");
        jButtonAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddActionPerformed(evt);
            }
        });

        jTextFieldMaxXMaxY.setText("700.0, 315.0");
        jTextFieldMaxXMaxY.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldMaxXMaxYActionPerformed(evt);
            }
        });

        jCheckBoxSeparateNames.setSelected(true);
        jCheckBoxSeparateNames.setText("Sep. Names");
        jCheckBoxSeparateNames.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxSeparateNamesActionPerformed(evt);
            }
        });

        jCheckBoxAutoscale.setSelected(true);
        jCheckBoxAutoscale.setText("Auto Scale");
        jCheckBoxAutoscale.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxAutoscaleActionPerformed(evt);
            }
        });

        jLabel4.setText("Current");

        jLabel3.setText("Xmax,Ymax");

        jTextFieldMinXMinY.setText("200.0, -315.0");
        jTextFieldMinXMinY.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldMinXMinYActionPerformed(evt);
            }
        });

        jButtonDelete.setText("Delete");
        jButtonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteActionPerformed(evt);
            }
        });

        jCheckBoxAddSlots.setText("Add Slots");
        jCheckBoxAddSlots.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxAddSlotsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelOptionsTabLayout = new javax.swing.GroupLayout(jPanelOptionsTab);
        jPanelOptionsTab.setLayout(jPanelOptionsTabLayout);
        jPanelOptionsTabLayout.setHorizontalGroup(
            jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                            .addComponent(jCheckBoxShowRotations)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jComboBoxDisplayAxis, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                            .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel2)
                                .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                                    .addComponent(jCheckBoxShowCurrent)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jLabel4))
                                .addComponent(jLabel3))
                            .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jTextFieldMaxXMaxY, javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(jTextFieldCurrentXY, javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(jTextFieldMinXMinY, javax.swing.GroupLayout.PREFERRED_SIZE, 199, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelOptionsTabLayout.createSequentialGroup()
                        .addComponent(jCheckBoxAddSlots)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxSeparateNames)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxAutoscale))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelOptionsTabLayout.createSequentialGroup()
                        .addComponent(jButtonOffsetAll)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonReset)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonCurrent)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonDelete)
                        .addGap(4, 4, 4)
                        .addComponent(jButtonAdd)))
                .addContainerGap())
        );
        jPanelOptionsTabLayout.setVerticalGroup(
            jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxAutoscale)
                    .addComponent(jCheckBoxSeparateNames)
                    .addComponent(jCheckBoxAddSlots))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxShowRotations)
                    .addComponent(jComboBoxDisplayAxis, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jTextFieldCurrentXY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckBoxShowCurrent))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jTextFieldMaxXMaxY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jTextFieldMinXMinY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonDelete)
                    .addComponent(jButtonAdd)
                    .addComponent(jButtonReset)
                    .addComponent(jButtonCurrent)
                    .addComponent(jButtonOffsetAll))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelOptionsTabLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jButtonAdd, jButtonDelete});

        jTabbedPane1.addTab("Options", jPanelOptionsTab);

        jPanelConnectionsTab.setMaximumSize(new java.awt.Dimension(407, 32767));

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

        javax.swing.GroupLayout jPanelConnectionsTabLayout = new javax.swing.GroupLayout(jPanelConnectionsTab);
        jPanelConnectionsTab.setLayout(jPanelConnectionsTabLayout);
        jPanelConnectionsTabLayout.setHorizontalGroup(
            jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                        .addComponent(jCheckBoxSimulated)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxConnected, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(142, 142, 142))
                    .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                        .addGroup(jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabelHost)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldHost, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonRefresh))
                            .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                                .addComponent(jCheckBoxDebug)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCheckBoxPause)))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanelConnectionsTabLayout.setVerticalGroup(
            jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxSimulated)
                    .addComponent(jCheckBoxConnected))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelHost)
                    .addComponent(jTextFieldHost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonRefresh))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxDebug)
                    .addComponent(jCheckBoxPause))
                .addContainerGap(107, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Connections", jPanelConnectionsTab);

        jLabel5.setText("Simulated Frequency (in ms)  :");

        jTextFieldSimulationUpdateTime.setEditable(false);
        jTextFieldSimulationUpdateTime.setText("50   ");
        jTextFieldSimulationUpdateTime.setEnabled(false);
        jTextFieldSimulationUpdateTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldSimulationUpdateTimeActionPerformed(evt);
            }
        });

        jCheckBoxShuffleSimulatedUpdates.setText("Shuffle simulated updates");
        jCheckBoxShuffleSimulatedUpdates.setEnabled(false);

        jCheckBoxSimulationUpdateAsNeeded.setSelected(true);
        jCheckBoxSimulationUpdateAsNeeded.setText("Simulate Updates only as needed.");
        jCheckBoxSimulationUpdateAsNeeded.setEnabled(false);
        jCheckBoxSimulationUpdateAsNeeded.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxSimulationUpdateAsNeededActionPerformed(evt);
            }
        });

        jLabel6.setText("Simulated Drop Out Rate ( 0.0 to 1.0)  : ");

        jTextFieldSimDropRate.setEditable(false);
        jTextFieldSimDropRate.setText("0.0    ");
        jTextFieldSimDropRate.setEnabled(false);
        jTextFieldSimDropRate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldSimDropRateActionPerformed(evt);
            }
        });

        jCheckBoxAddPosNoise.setText("Add Position Noise");
        jCheckBoxAddPosNoise.setEnabled(false);
        jCheckBoxAddPosNoise.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxAddPosNoiseActionPerformed(evt);
            }
        });

        jLabel7.setText("Pos Noise: ");

        jTextFieldPosNoise.setEditable(false);
        jTextFieldPosNoise.setText("1.0    ");
        jTextFieldPosNoise.setEnabled(false);
        jTextFieldPosNoise.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldPosNoiseActionPerformed(evt);
            }
        });

        jLabel8.setText("Rotation Noise: ");

        jTextFieldRotNoise.setEditable(false);
        jTextFieldRotNoise.setText("1.0         ");
        jTextFieldRotNoise.setEnabled(false);
        jTextFieldRotNoise.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldRotNoiseActionPerformed(evt);
            }
        });

        jCheckBoxViewOutput.setText("View Output");
        jCheckBoxViewOutput.setEnabled(false);
        jCheckBoxViewOutput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxViewOutputActionPerformed(evt);
            }
        });

        jLabel9.setText("Pickup Dist: ");

        jTextFieldPickupDist.setEditable(false);
        jTextFieldPickupDist.setText("5.0   ");
        jTextFieldPickupDist.setEnabled(false);
        jTextFieldPickupDist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldPickupDistActionPerformed(evt);
            }
        });

        jLabel10.setText("Drop off Threshold: ");

        jTextFieldDropOffThreshold.setEditable(false);
        jTextFieldDropOffThreshold.setText("25.0     ");
        jTextFieldDropOffThreshold.setEnabled(false);
        jTextFieldDropOffThreshold.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldDropOffThresholdActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelSimulationTabLayout = new javax.swing.GroupLayout(jPanelSimulationTab);
        jPanelSimulationTab.setLayout(jPanelSimulationTabLayout);
        jPanelSimulationTabLayout.setHorizontalGroup(
            jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldSimulationUpdateTime)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxViewOutput))
                    .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldSimDropRate))
                    .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldPosNoise, javax.swing.GroupLayout.DEFAULT_SIZE, 118, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel8)
                        .addGap(120, 120, 120))
                    .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                        .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCheckBoxSimulationUpdateAsNeeded)
                            .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                                .addComponent(jCheckBoxShuffleSimulatedUpdates)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCheckBoxAddPosNoise)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldPickupDist)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextFieldDropOffThreshold)
                            .addComponent(jTextFieldRotNoise))))
                .addContainerGap())
        );
        jPanelSimulationTabLayout.setVerticalGroup(
            jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jTextFieldSimulationUpdateTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckBoxViewOutput))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxSimulationUpdateAsNeeded)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jTextFieldSimDropRate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxShuffleSimulatedUpdates)
                    .addComponent(jCheckBoxAddPosNoise))
                .addGap(3, 3, 3)
                .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jTextFieldPosNoise, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8)
                    .addComponent(jTextFieldRotNoise, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jTextFieldPickupDist, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10)
                    .addComponent(jTextFieldDropOffThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Simulation", jPanelSimulationTab);

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
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 432, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 432, javax.swing.GroupLayout.PREFERRED_SIZE))))
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
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 234, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 217, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addComponent(object2DJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private double posNoise = 1.0;

    /**
     * Get the value of posNoise
     *
     * @return the value of posNoise
     */
    public double getPosNoise() {
        return posNoise;
    }

    /**
     * Set the value of posNoise
     *
     * @param posNoise new value of posNoise
     */
    public void setPosNoise(double posNoise) {
        updateTextFieldDouble(posNoise, jTextFieldPosNoise, 0.01);
        this.posNoise = posNoise;
    }

    private double rotNoise = 1.0;

    /**
     * Get the value of rotNoise
     *
     * @return the value of rotNoise
     */
    public double getRotNoise() {
        return rotNoise;
    }

    private void updateTextFieldDouble(double value, JTextField textField, double threshold) {
        if (Math.abs(value - Double.parseDouble(textField.getText().trim())) > threshold) {
            textField.setText(String.format("%.3f", value));
        }
    }

    /**
     * Set the value of rotNoise
     *
     * @param rotNoise new value of rotNoise
     */
    public void setRotNoise(double rotNoise) {
        updateTextFieldDouble(rotNoise, jTextFieldRotNoise, 0.01);
        this.rotNoise = rotNoise;
    }

    private void setSimulatedInternal(boolean simulated) {

        jButtonAdd.setEnabled(simulated);
        jButtonDelete.setEnabled(simulated);
        jButtonReset.setEnabled(simulated);
        jButtonOffsetAll.setEnabled(simulated);
        jTextFieldSimulationUpdateTime.setEditable(simulated && !jCheckBoxSimulationUpdateAsNeeded.isSelected());
        jTextFieldSimulationUpdateTime.setEnabled(simulated && !jCheckBoxSimulationUpdateAsNeeded.isSelected());
        jCheckBoxSimulationUpdateAsNeeded.setEnabled(simulated);
        jPanelSimulationTab.setEnabled(simulated);
        jTextFieldSimDropRate.setEnabled(simulated);
        jTextFieldSimDropRate.setEditable(simulated);
        jCheckBoxShuffleSimulatedUpdates.setEnabled(simulated);
        jCheckBoxAddPosNoise.setEnabled(simulated);
        jCheckBoxViewOutput.setEnabled(simulated);
        jTextFieldPosNoise.setEditable(simulated && jCheckBoxAddPosNoise.isSelected());
        jTextFieldPosNoise.setEnabled(simulated && jCheckBoxAddPosNoise.isSelected());
        jTextFieldRotNoise.setEditable(simulated && jCheckBoxAddPosNoise.isSelected());
        jTextFieldRotNoise.setEnabled(simulated && jCheckBoxAddPosNoise.isSelected());
        object2DJPanel1.setShowOutputItems(simulated && jCheckBoxViewOutput.isSelected());
        if (simulated) {
            jTextFieldHost.setEditable(false);
            jTextFieldHost.setEnabled(false);
            jLabelHost.setEnabled(false);
            setupSimUpdateTimer();
        } else {
            jTextFieldHost.setEditable(true);
            jTextFieldHost.setEnabled(true);
            if (null != visionSocketServer) {
                visionSocketServer.close();
                visionSocketServer = null;
            }
            if (null != simUpdateTimer) {
                simUpdateTimer.stop();
                simUpdateTimer = null;
            }
        }
    }

    private void jCheckBoxSimulatedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxSimulatedActionPerformed
        this.jCheckBoxConnected.setSelected(false);
        setSimulatedInternal(this.jCheckBoxSimulated.isSelected());
        disconnect();
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
                int port = Integer.parseInt(this.jTextFieldPort.getText().trim());
                if (null != visionSocketServer && visionSocketServer.getPort() != port) {
                    disconnect();
                }
                if (null == visionSocketServer) {
                    visionSocketServer = new VisionSocketServer(port);
                }
                visionSocketServer.setDebug(this.jCheckBoxDebug.isSelected());
                publishCurrentItems();
            } catch (IOException ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            int port = Integer.parseInt(jTextFieldPort.getText().trim());
            String host = jTextFieldHost.getText().trim();
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
            argsMap.put("--visionport", jTextFieldPort.getText().trim());
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

    private double simulatedDropRate = 0.0;

    /**
     * Get the value of simulatedDropRate
     *
     * @return the value of simulatedDropRate
     */
    public double getSimulatedDropRate() {
        return simulatedDropRate;
    }

    /**
     * Set the value of simulatedDropRate
     *
     * @param simulatedDropRate new value of simulatedDropRate
     */
    public void setSimulatedDropRate(double simulatedDropRate) {
        if (simulatedDropRate > 1.0 || simulatedDropRate < -Double.MIN_VALUE) {
            throw new IllegalArgumentException("simulatedDropRate must be between 0 and 1.0 but was " + simulatedDropRate);
        }
        if(simulatedDropRate < 0.001) {
            simulatedDropRate =0;
        }
        updateTextFieldDouble(simulatedDropRate, jTextFieldSimDropRate, 0.001);
        this.simulatedDropRate = simulatedDropRate;
    }

    private final Random dropRandom = new Random();

    private boolean dropFilter(Object x) {
        if (simulatedDropRate < 0.001) {
            return true;
        }
        return dropRandom.nextDouble() > simulatedDropRate;
    }

    private final Random posRandom = new Random();

    private DetectedItem noiseFilter(DetectedItem in) {
        if (!jCheckBoxAddPosNoise.isSelected()) {
            return in;
        }
        DetectedItem out = new DetectedItem(in.getName(),
                in.getRotation() + posRandom.nextGaussian() * Math.toRadians(rotNoise),
                in.x + posRandom.nextGaussian() * posNoise,
                in.y + posRandom.nextGaussian() * posNoise, in.getScore(), in.getType());
        out.setFullName(in.getFullName());
        return out;
    }

    private void publishCurrentItems() {
        if (jCheckBoxShuffleSimulatedUpdates.isSelected() || simulatedDropRate > 0.01 || jCheckBoxAddPosNoise.isSelected()) {
            List<DetectedItem> l = new ArrayList<>();
            List<DetectedItem> origList = getItems();
            l.addAll(origList);
            if (simulatedDropRate > 0.01  || jCheckBoxAddPosNoise.isSelected()) {
                l = l.stream()
                        .filter(this::dropFilter)
                        .map(this::noiseFilter)
                        .collect(Collectors.toList());
            }
            if (jCheckBoxShuffleSimulatedUpdates.isSelected()) {
                Collections.shuffle(l);
            }
            visionSocketServer.publishList(l);
            setOutputItems(l);
        } else {
            visionSocketServer.publishList(getItems());
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
        if (null == item || null == item.getDisplayRect() || null == item.getDisplayTransform()) {
            return false;
        }
        boolean inside = false;
        try {
            Point2D newPoint = item.getRelTransform().inverseTransform(new Point2D.Double(x, y), null);
            System.out.println("newPoint = " + newPoint.getX() + ", " + newPoint.getY());
            inside = item.getDisplayRect().contains(newPoint);
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
        String txt = jTextFieldMaxXMaxY.getText().trim();
        setMaxXMaxYText(txt);
    }//GEN-LAST:event_jTextFieldMaxXMaxYActionPerformed

    public void setMaxXMaxYText(String txt) throws NumberFormatException {
        String vals[] = txt.split(",");
        if (vals.length == 2) {
            double newMaxX = Double.parseDouble(vals[0]);
            double newMaxY = Double.parseDouble(vals[1]);
            object2DJPanel1.setMaxX(newMaxX);
            object2DJPanel1.setMaxY(newMaxY);
        } else {
            System.err.println("Bad xmax,ymax = " + txt);
        }
    }

    private void jTextFieldMinXMinYActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldMinXMinYActionPerformed
        String txt = jTextFieldMinXMinY.getText().trim();
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
                publishCurrentItems();
            }
        }
    }//GEN-LAST:event_jCheckBoxPauseActionPerformed

    private void jButtonRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRefreshActionPerformed
        if (null != visionSocketServer && !this.jCheckBoxPause.isSelected()) {
            this.setItems(object2DJPanel1.getItems());
            publishCurrentItems();
        }
    }//GEN-LAST:event_jButtonRefreshActionPerformed

    private void jComboBoxDisplayAxisActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxDisplayAxisActionPerformed
        object2DJPanel1.setDisplayAxis((DisplayAxis) jComboBoxDisplayAxis.getSelectedItem());
    }//GEN-LAST:event_jComboBoxDisplayAxisActionPerformed

    private void loadFile(File f) throws IOException {

        try {
            takeSnapshot(aprsJFrame.createTempFile("before_loadFile_" + f.getName() + "_", ".PNG"), null, "");
        } catch (IOException ex) {
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (f.isDirectory()) {
            System.err.println("Can not load file \"" + f + "\" : It is a directory when a text/csv file is expected.");
            return;
        }
        String line = Files.lines(f.toPath()).skip(1).map(String::trim).collect(Collectors.joining(","));
        this.setItems(VisionSocketClient.lineToList(line));
        jTextFieldFilename.setText(f.getCanonicalPath());
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                takeSnapshot(aprsJFrame.createTempFile("loadFile_" + f.getName() + "_", ".PNG"), null, "");
            } catch (IOException ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }

    private void saveFile(File f) throws IOException {

        saveFile(f, getItems());

    }

    private void saveFile(File f, List<DetectedItem> items) throws IOException {

        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            pw.println("name,rotation,x,y,score,type");
            for (DetectedItem item : items) {
                pw.println(item.getName() + "," + item.getRotation() + "," + item.x + "," + item.y + "," + item.getScore() + "," + item.getType());
            }
        }

    }

    private void jButtonLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoadActionPerformed
        String fname = jTextFieldFilename.getText().trim();
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
        String fname = jTextFieldFilename.getText().trim();
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
                File newFile = chooser.getSelectedFile();
                saveFile(newFile);
                jTextFieldFilename.setText(newFile.getCanonicalPath());
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
        this.object2DJPanel1.setAprsJFrame(aprsJFrame);
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

    private void jCheckBoxSeparateNamesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxSeparateNamesActionPerformed
        object2DJPanel1.setUseSeparateNames(jCheckBoxSeparateNames.isSelected());
    }//GEN-LAST:event_jCheckBoxSeparateNamesActionPerformed

    private void jCheckBoxAutoscaleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxAutoscaleActionPerformed
        object2DJPanel1.setAutoscale(this.jCheckBoxAutoscale.isSelected());
    }//GEN-LAST:event_jCheckBoxAutoscaleActionPerformed

    PmCartesian getMinOffset() {
        PmCartesian minDiffCart = new PmCartesian();
        PointType current = aprsJFrame.getCurrentPosePoint();
        double min_diff = Double.POSITIVE_INFINITY;
        if (null != current) {
            PmCartesian currentCart = CRCLPosemath.toPmCartesian(current);
            for (DetectedItem item : this.getItems()) {
                PmCartesian diffCart = item.subtract(currentCart);
                diffCart.z = 0;
                double diffMag = diffCart.mag();
                if (min_diff > diffMag) {
                    min_diff = diffMag;
                    minDiffCart = diffCart;
                }
            }
        }
        return minDiffCart;
    }

    private void jButtonOffsetAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOffsetAllActionPerformed
        offsetAll();
    }//GEN-LAST:event_jButtonOffsetAllActionPerformed

    private void jTextFieldSimulationUpdateTimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldSimulationUpdateTimeActionPerformed
        setSimRefreshMillis(Integer.parseInt(jTextFieldSimulationUpdateTime.getText().trim()));
        setupSimUpdateTimer();
    }//GEN-LAST:event_jTextFieldSimulationUpdateTimeActionPerformed

    private void jCheckBoxSimulationUpdateAsNeededActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxSimulationUpdateAsNeededActionPerformed
        jTextFieldSimulationUpdateTime.setEditable(jCheckBoxSimulated.isSelected() && !jCheckBoxSimulationUpdateAsNeeded.isSelected());
        jTextFieldSimulationUpdateTime.setEnabled(jCheckBoxSimulated.isSelected() && !jCheckBoxSimulationUpdateAsNeeded.isSelected());
        setupSimUpdateTimer();
    }//GEN-LAST:event_jCheckBoxSimulationUpdateAsNeededActionPerformed

    private void jTextFieldRotNoiseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldRotNoiseActionPerformed
        setRotNoise(Double.parseDouble(jTextFieldRotNoise.getText().trim()));
    }//GEN-LAST:event_jTextFieldRotNoiseActionPerformed

    private void jTextFieldPosNoiseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldPosNoiseActionPerformed
        setPosNoise(Double.parseDouble(jTextFieldPosNoise.getText().trim()));
    }//GEN-LAST:event_jTextFieldPosNoiseActionPerformed

    private void jCheckBoxAddPosNoiseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxAddPosNoiseActionPerformed
        final boolean enable = jCheckBoxSimulated.isSelected() && jCheckBoxAddPosNoise.isSelected();
        jTextFieldPosNoise.setEditable(enable);
        jTextFieldPosNoise.setEnabled(enable);
        jTextFieldRotNoise.setEditable(enable);
        jTextFieldRotNoise.setEnabled(enable);
    }//GEN-LAST:event_jCheckBoxAddPosNoiseActionPerformed

    private void jCheckBoxViewOutputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxViewOutputActionPerformed
        object2DJPanel1.setShowOutputItems(jCheckBoxViewOutput.isSelected() && jCheckBoxViewOutput.isSelected());
    }//GEN-LAST:event_jCheckBoxViewOutputActionPerformed

    private void jTextFieldSimDropRateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldSimDropRateActionPerformed
        setSimulatedDropRate(Double.parseDouble(jTextFieldSimDropRate.getText().trim()));
    }//GEN-LAST:event_jTextFieldSimDropRateActionPerformed

    private void jTextFieldPickupDistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldPickupDistActionPerformed
        setPickupDist(Double.parseDouble(jTextFieldPickupDist.getText().trim()));
    }//GEN-LAST:event_jTextFieldPickupDistActionPerformed

    private void jTextFieldDropOffThresholdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldDropOffThresholdActionPerformed
        setDropOffThreshold(Double.parseDouble(jTextFieldDropOffThreshold.getText().trim()));
    }//GEN-LAST:event_jTextFieldDropOffThresholdActionPerformed

    private void jCheckBoxAddSlotsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxAddSlotsActionPerformed
       object2DJPanel1.setShowAddedSlotPositions(jCheckBoxAddSlots.isSelected());
       refresh(false);
    }//GEN-LAST:event_jCheckBoxAddSlotsActionPerformed

    javax.swing.Timer simUpdateTimer = null;

    private int simRefreshMillis = 50;

    /**
     * Get the value of simRefreshMillis
     *
     * @return the value of simRefreshMillis
     */
    public int getSimRefreshMillis() {
        return simRefreshMillis;
    }

    /**
     * Set the value of simRefreshMillis
     *
     * @param simRefreshMillis new value of simRefreshMillis
     */
    public void setSimRefreshMillis(int simRefreshMillis) {
        if (Integer.parseInt(jTextFieldSimulationUpdateTime.getText().trim()) != simRefreshMillis) {
            jTextFieldSimulationUpdateTime.setText(Integer.toString(simRefreshMillis));
        }
        this.simRefreshMillis = simRefreshMillis;
    }

    private void simUpdateAction(ActionEvent evt) {
        if (jCheckBoxSimulationUpdateAsNeeded.isSelected()) {
            return;
        }
        refresh(false);
    }

    private void setupSimUpdateTimer() {
        if (null != simUpdateTimer) {
            simUpdateTimer.stop();
            simUpdateTimer = null;
        }
        if (jCheckBoxSimulationUpdateAsNeeded.isSelected()) {
            return;
        }
        simUpdateTimer = new javax.swing.Timer(simRefreshMillis, this::simUpdateAction);
        simUpdateTimer.start();
    }

    private void offsetAll() {
        try {
            PmCartesian minOffset = getMinOffset();
            String offsetString = JOptionPane.showInputDialog("Offset to apply to all items:", minOffset.toString());
            if (offsetString != null) {
                String fa[] = offsetString.split("[{} ,]+");
                double x = 0;
                double y = 0;
                for (String s : fa) {
                    if (s.startsWith("x=")) {
                        x = Double.parseDouble(s.substring(2));
                    } else if (s.startsWith("y=")) {
                        y = Double.parseDouble(s.substring(2));
                    }
                }
                if (fa.length >= 2) {
                    List<DetectedItem> inItems = getItems();
                    List<DetectedItem> newItems = new ArrayList<>();
                    for (DetectedItem item : inItems) {
                        DetectedItem newItem = new DetectedItem(item.getName(), item.getRotation(), item.x - x, item.y - y, item.getScore(), item.getType());
                        newItem.setVisioncycle(item.getVisioncycle());
                        newItems.add(newItem);
                    }
                    setItems(newItems, true);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

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
            double newMinX = Double.parseDouble(vals[0]);
            double newMinY = Double.parseDouble(vals[1]);
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
    private javax.swing.JButton jButtonOffsetAll;
    private javax.swing.JButton jButtonRefresh;
    private javax.swing.JButton jButtonReset;
    private javax.swing.JButton jButtonSave;
    private javax.swing.JCheckBox jCheckBoxAddPosNoise;
    private javax.swing.JCheckBox jCheckBoxAddSlots;
    private javax.swing.JCheckBox jCheckBoxAutoscale;
    private javax.swing.JCheckBox jCheckBoxConnected;
    private javax.swing.JCheckBox jCheckBoxDebug;
    private javax.swing.JCheckBox jCheckBoxPause;
    private javax.swing.JCheckBox jCheckBoxSeparateNames;
    private javax.swing.JCheckBox jCheckBoxShowCurrent;
    private javax.swing.JCheckBox jCheckBoxShowRotations;
    private javax.swing.JCheckBox jCheckBoxShuffleSimulatedUpdates;
    private javax.swing.JCheckBox jCheckBoxSimulated;
    private javax.swing.JCheckBox jCheckBoxSimulationUpdateAsNeeded;
    private javax.swing.JCheckBox jCheckBoxViewOutput;
    private javax.swing.JComboBox<DisplayAxis> jComboBoxDisplayAxis;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelHost;
    private javax.swing.JPanel jPanelConnectionsTab;
    private javax.swing.JPanel jPanelOptionsTab;
    private javax.swing.JPanel jPanelSimulationTab;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTableItems;
    private javax.swing.JTextField jTextFieldCurrentXY;
    private javax.swing.JTextField jTextFieldDropOffThreshold;
    private javax.swing.JTextField jTextFieldFilename;
    private javax.swing.JTextField jTextFieldHost;
    private javax.swing.JTextField jTextFieldMaxXMaxY;
    private javax.swing.JTextField jTextFieldMinXMinY;
    private javax.swing.JTextField jTextFieldPickupDist;
    private javax.swing.JTextField jTextFieldPort;
    private javax.swing.JTextField jTextFieldPosNoise;
    private javax.swing.JTextField jTextFieldRotNoise;
    private javax.swing.JTextField jTextFieldSimDropRate;
    private javax.swing.JTextField jTextFieldSimulationUpdateTime;
    private aprs.framework.simview.Object2DJPanel object2DJPanel1;
    // End of variables declaration//GEN-END:variables

    @Override
    public void dispose() {
        if (null != this.visionSocketClient) {
            try {
                visionSocketClient.close();
            } catch (Exception ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
            visionSocketClient = null;
        }
        if (null != this.visionSocketServer) {
            visionSocketServer.close();
            visionSocketServer = null;
        }
    }

    private File propertiesFile;

    @Override
    public void setPropertiesFile(File f) {
        this.propertiesFile = f;
    }

    private static String makeShortPath(File f, String str) {
        try {
            if (str.startsWith("..")) {
                return str;
            }
            File strFile = new File(str);
            if (!strFile.exists()) {
                return str;
            }
            String canString = strFile.getCanonicalPath();
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
            props.put("--visionport", jTextFieldPort.getText().trim());
            props.put("--visionhost", jTextFieldHost.getText().trim());
            props.put("simulated", Boolean.toString(jCheckBoxSimulated.isSelected()));
            props.put("viewOutput", Boolean.toString(jCheckBoxViewOutput.isSelected()));
            props.put("simulationUpdateAsNeeded", Boolean.toString(jCheckBoxSimulationUpdateAsNeeded.isSelected()));
            props.put("shuffleSimulatedUpdates", Boolean.toString(jCheckBoxShuffleSimulatedUpdates.isSelected()));
            props.put("simulatedDropRate", String.format("%.3f", simulatedDropRate));
            props.put("addPosNoise", Boolean.toString(jCheckBoxAddPosNoise.isSelected()));
            props.put("pickupDist", String.format("%.2f", pickupDist));
            props.put("dropOffThreshold", String.format("%.2f", dropOffThreshold));
            props.put("posNoise", String.format("%.2f", posNoise));
            props.put("rotNoise", String.format("%.2f", rotNoise));
            props.put("simRefreshMillis", Integer.toString(simRefreshMillis));
            props.put("connected", Boolean.toString(jCheckBoxConnected.isSelected()));
            props.put("autoscale", Boolean.toString(jCheckBoxAutoscale.isSelected()));
            props.put("trackcurrentpos", Boolean.toString(jCheckBoxShowCurrent.isSelected()));
            props.put("showrotations", Boolean.toString(jCheckBoxShowRotations.isSelected()));
            props.put("separatenames", Boolean.toString(jCheckBoxSeparateNames.isSelected()));
            props.put("xmaxymax", jTextFieldMaxXMaxY.getText().trim());
            props.put("xminymin", jTextFieldMinXMinY.getText().trim());
            if (reverseFlag) {
                this.reverseDataFileString = jTextFieldFilename.getText().trim();
            } else {
                this.dataFileString = jTextFieldFilename.getText().trim();
            }
            if (null != reverseDataFileString && reverseDataFileString.length() > 0) {
                String datafileShort = makeShortPath(propertiesFile, reverseDataFileString);
                props.put("reverse_datafile", datafileShort);
            }
            if (null != dataFileString && dataFileString.length() > 0) {
                String datafileShort = makeShortPath(propertiesFile, dataFileString);
                props.put("datafile", datafileShort);
            }
            props.put("reverseFlag", Boolean.toString(reverseFlag));
            DisplayAxis displayAxis = object2DJPanel1.getDisplayAxis();
            props.put("displayAxis", displayAxis.toString());
            List<DetectedItem> l = getItems();
            if (null != l && l.size() > 0) {
                props.put(ITEMS_PROPERTY_NAME, VisionSocketServer.listToLine(l));
            }
//            try (FileWriter fw = new FileWriter(propertiesFile)) {
//                props.store(fw, "");
//            }
            Utils.saveProperties(propertiesFile, props);
        }
    }
    private static final String ITEMS_PROPERTY_NAME = "items";

    private boolean reverseFlag = false;

    /**
     * Get the value of reverseFlag
     *
     * @return the value of reverseFlag
     */
    public boolean isReverseFlag() {
        return reverseFlag;
    }

    /**
     * Set the value of reverseFlag
     *
     * @param reverseFlag new value of reverseFlag
     */
    public void setReverseFlag(boolean reverseFlag) {
        this.reverseFlag = reverseFlag;
    }

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

            String simulationUpdateAsNeededString = props.getProperty("simulationUpdateAsNeeded");
            if (null != simulationUpdateAsNeededString && simulationUpdateAsNeededString.length() > 0) {
                boolean simulationUpdateAsNeeded = Boolean.valueOf(simulationUpdateAsNeededString);
                jCheckBoxSimulationUpdateAsNeeded.setSelected(simulationUpdateAsNeeded);
                jTextFieldSimulationUpdateTime.setEditable(jCheckBoxSimulated.isSelected() && !jCheckBoxSimulationUpdateAsNeeded.isSelected());
                jTextFieldSimulationUpdateTime.setEnabled(jCheckBoxSimulated.isSelected() && !jCheckBoxSimulationUpdateAsNeeded.isSelected());
            }

            String shuffleSimulatedUpdatesString = props.getProperty("shuffleSimulatedUpdates");
            if (null != shuffleSimulatedUpdatesString && shuffleSimulatedUpdatesString.length() > 0) {
                boolean shuffleSimulatedUpdates = Boolean.valueOf(shuffleSimulatedUpdatesString);
                jCheckBoxShuffleSimulatedUpdates.setSelected(shuffleSimulatedUpdates);
            }

            String viewOutputString = props.getProperty("viewOutput");
            if (null != viewOutputString && viewOutputString.length() > 0) {
                boolean viewOutput = Boolean.valueOf(viewOutputString);
                jCheckBoxViewOutput.setSelected(viewOutput);
            }

            String addPosNoiseString = props.getProperty("addPosNoise");
            if (null != addPosNoiseString && addPosNoiseString.length() > 0) {
                boolean addPosNoise = Boolean.valueOf(addPosNoiseString);
                jCheckBoxAddPosNoise.setSelected(addPosNoise);
            }
            String simulatedDropRateString = props.getProperty("simulatedDropRate");
            if (null != simulatedDropRateString && simulatedDropRateString.length() > 0) {
                double simDropRate = Double.parseDouble(simulatedDropRateString);
                if(simDropRate < 0.001) {
                    simDropRate = 0;
                }
                setSimulatedDropRate(simDropRate);
            }

            String pickupDistString = props.getProperty("pickupDist");
            if (null != pickupDistString && pickupDistString.length() > 0) {
                double simPickupDist = Double.parseDouble(pickupDistString);
                setPickupDist(simPickupDist);
            }

            String dropOffThresholdString = props.getProperty("dropOffThreshold");
            if (null != dropOffThresholdString && dropOffThresholdString.length() > 0) {
                double simDropOffThreshold = Double.parseDouble(dropOffThresholdString);
                setDropOffThreshold(simDropOffThreshold);
            }

            String posNoiseString = props.getProperty("posNoise");
            if (null != posNoiseString && posNoiseString.length() > 0) {
                double simPosNoise = Double.parseDouble(posNoiseString);
                setPosNoise(simPosNoise);
            }

            String rotNoiseString = props.getProperty("rotNoise");
            if (null != rotNoiseString && rotNoiseString.length() > 0) {
                double simRotNoise = Double.parseDouble(rotNoiseString);
                setRotNoise(simRotNoise);
            }
            String simRefreshMillisString = props.getProperty("simRefreshMillis");
            if (null != simRefreshMillisString && simRefreshMillisString.length() > 0) {
                int simRefreshMs = Integer.parseInt(simRefreshMillisString);
                setSimRefreshMillis(simRefreshMs);
            }

            String simulatedString = props.getProperty("simulated");
            if (null != simulatedString && simulatedString.length() > 0) {
                boolean simulated = Boolean.valueOf(simulatedString);
                jCheckBoxSimulated.setSelected(simulated);
                setSimulatedInternal(simulated);
            }

//            props.put("simulationUpdateAsNeeded", Boolean.toString(jCheckBoxSimulationUpdateAsNeeded.isSelected()));
//            props.put("shuffleSimulatedUpdates", Boolean.toString(jCheckBoxShuffleSimulatedUpdates.isSelected()));
//            props.put("simulatedDropRate", String.format("%.2f", simulatedDropRate));
//            props.put("simRefreshMillis", Integer.toString(simRefreshMillis));
            String autoscaleString = props.getProperty("autoscale");
            if (null != autoscaleString && autoscaleString.length() > 0) {
                boolean autoscale = Boolean.valueOf(autoscaleString);
                jCheckBoxAutoscale.setSelected(autoscale);
                object2DJPanel1.setAutoscale(autoscale);
            }
            reverseDataFileString = props.getProperty("reverse_datafile");
            dataFileString = props.getProperty("datafile");
            reloadDataFile();
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
            String useSeparateNamesString = props.getProperty("separatenames");
            if (useSeparateNamesString != null && useSeparateNamesString.length() > 0) {
                boolean useSeparateNames = Boolean.valueOf(useSeparateNamesString);
                jCheckBoxSeparateNames.setSelected(useSeparateNames);
                object2DJPanel1.setUseSeparateNames(useSeparateNames);
            }
        }
    }

    private String dataFileString = null;
    private String reverseDataFileString = null;

    public void reloadDataFile() throws IOException {
        String currentDataFileString = reverseFlag ? this.reverseDataFileString : this.dataFileString;
        if (null != currentDataFileString && currentDataFileString.length() > 0) {
            File f = new File(currentDataFileString);
            if (f.exists() && f.canRead() && !f.isDirectory()) {
                jTextFieldFilename.setText(f.getCanonicalPath());
                loadFile(f);
            } else {
                String fullPath = propertiesFile.getParentFile().toPath().resolve(currentDataFileString).normalize().toString();
                f = new File(fullPath);
                if (f.exists() && f.canRead()) {
                    jTextFieldFilename.setText(f.getCanonicalPath());
                    loadFile(f);
                } else {
                    String fullPath2 = propertiesFile.getParentFile().toPath().resolveSibling(currentDataFileString).normalize().toString();
                    f = new File(fullPath2);
                    if (f.exists() && f.canRead()) {
                        jTextFieldFilename.setText(f.getCanonicalPath());
                        loadFile(f);
                    }
                }
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

    private volatile boolean lastIsHoldingObjectExpected = false;
    private volatile int captured_item_index = -1;

    public void takeSnapshot(File f, List<DetectedItem> itemsToPaint) throws IOException {
        this.object2DJPanel1.takeSnapshot(f, itemsToPaint);
        File csvDir = new File(f.getParentFile(), "csv");
        csvDir.mkdirs();
        saveFile(new File(csvDir, f.getName() + ".csv"), itemsToPaint);
    }

    private volatile long lastIsHoldingObjectExpectedTime = -1;
    private volatile long lastNotIsHoldingObjectExpectedTime = -1;

    private double pickupDist = 5.0;

    /**
     * Get the value of pickupDist
     *
     * @return the value of pickupDist
     */
    public double getPickupDist() {
        return pickupDist;
    }

    /**
     * Set the value of pickupDist
     *
     * @param pickupDist new value of pickupDist
     */
    public void setPickupDist(double pickupDist) {
        updateTextFieldDouble(pickupDist, jTextFieldPickupDist, 0.005);
        this.pickupDist = pickupDist;
    }

    private double dropOffThreshold = 25.0;

    /**
     * Get the value of dropOffThreshold
     *
     * @return the value of dropOffThreshold
     */
    public double getDropOffThreshold() {
        return dropOffThreshold;
    }

    /**
     * Set the value of dropOffThreshold
     *
     * @param dropOffThreshold new value of dropOffThreshold
     */
    public void setDropOffThreshold(double dropOffThreshold) {
        updateTextFieldDouble(pickupDist, jTextFieldDropOffThreshold, 0.005);
        this.dropOffThreshold = dropOffThreshold;
    }

    @Override
    public void handlePoseUpdate(PendantClientJPanel panel, PoseType pose, CRCLStatusType stat, CRCLCommandType cmd, boolean isHoldingObjectExpected) {
        PointType ptIn = pose.getPoint();

        PointType uncorrectedPoint = aprsJFrame.reverseCorrectPoint(ptIn);
        currentX = uncorrectedPoint.getX();
        currentY = uncorrectedPoint.getY();
        jTextFieldCurrentXY.setText(String.format("%.3f,%.3f", currentX, currentY));
        object2DJPanel1.setCurrentX(currentX);
        object2DJPanel1.setCurrentY(currentY);
        List<DetectedItem> l = new ArrayList<>();
        l.addAll(getItems());
        double min_dist = Double.POSITIVE_INFINITY;
        int min_dist_index = -1;
        for (int i = 0; i < l.size(); i++) {
            if (i == captured_item_index) {
                continue;
            }
            DetectedItem item = l.get(i);
            if (!item.getType().equals("P")) {
                continue;
            }
            double dist = item.dist(currentX, currentY);
            if (dist < min_dist) {
                min_dist_index = i;
                min_dist = dist;
            }
        }
        long time = System.currentTimeMillis();
        if (min_dist < dropOffThreshold
                && lastIsHoldingObjectExpected && !isHoldingObjectExpected
                && min_dist_index != captured_item_index) {
            DetectedItem captured_item = (captured_item_index >= 0 && captured_item_index < l.size()) ? l.get(captured_item_index) : null;
            System.out.println("captured_item = " + captured_item);
            System.out.println("(time-lastIsHoldingObjectExpectedTime) = " + (time - lastIsHoldingObjectExpectedTime));
            System.out.println("(time-lastNotIsHoldingObjectExpectedTime) = " + (time - lastNotIsHoldingObjectExpectedTime));
            String errString
                    = "Dropping item on to another item min_dist=" + min_dist
                    + ", min_dist_index=" + min_dist_index
                    + ", captured_item_index=" + captured_item_index
                    + ", bottom item at min_dist_index =" + l.get(min_dist_index)
                    + ", captured_item  =" + captured_item;
            this.aprsJFrame.setTitleErrorString(errString);
            this.aprsJFrame.pause();
        }
        if (isHoldingObjectExpected) {
            lastIsHoldingObjectExpectedTime = time;
        } else {
            lastNotIsHoldingObjectExpectedTime = time;
        }

        if (this.jCheckBoxSimulated.isSelected()) {
            if (isHoldingObjectExpected && !lastIsHoldingObjectExpected) {
                if (min_dist < pickupDist && min_dist_index >= 0) {
                    captured_item_index = min_dist_index;
                    if (true) {
                        System.out.println("Captured item with index " + captured_item_index + " at " + currentX + "," + currentY);
                        try {
                            takeSnapshot(aprsJFrame.createTempFile("capture_" + captured_item_index + "_at_" + currentX + "_" + currentY + "_", ".PNG"), null, "");
                        } catch (IOException ex) {
                            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } else {
                    try {
                        takeSnapshot(aprsJFrame.createTempFile("failed_to_capture_part_at_" + currentX + "_" + currentY + "_", ".PNG"), null, "");
                    } catch (IOException ex) {
                        Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    System.err.println("Tried to capture item but min_dist=" + min_dist + ", min_dist_index=" + min_dist_index);
                    this.aprsJFrame.setTitleErrorString("Tried to capture item but min_dist=" + min_dist + ", min_dist_index=" + min_dist_index);
                    this.aprsJFrame.pause();
                }
            } else if (!isHoldingObjectExpected && lastIsHoldingObjectExpected) {
                if (true) {
                    System.out.println("Dropping item with index " + captured_item_index + " at " + currentX + "," + currentY);
                    try {
                        takeSnapshot(aprsJFrame.createTempFile("dropping_" + captured_item_index + "_at_" + currentX + "_" + currentY + "_", ".PNG"), null, "");
                    } catch (IOException ex) {
                        Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if (captured_item_index < 0) {
                    try {
                        takeSnapshot(aprsJFrame.createTempFile("failed_to_drop_part_", ".PNG"), null, "");
                    } catch (IOException ex) {
                        Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    System.err.println("Should be dropping item but no item captured");
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
                setItems(l, (isHoldingObjectExpected != lastIsHoldingObjectExpected) && jCheckBoxSimulationUpdateAsNeeded.isSelected());
            } else if (isHoldingObjectExpected != lastIsHoldingObjectExpected) {
                setItems(l);
            }
        }
        lastIsHoldingObjectExpected = isHoldingObjectExpected;
    }

}
