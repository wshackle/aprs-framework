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
package aprs.framework.spvision;

import aprs.framework.AprsJFrame;
import aprs.framework.Utils;
import static aprs.framework.Utils.autoResizeTableColWidths;
import static aprs.framework.Utils.runOnDispatchThread;
import aprs.framework.database.AcquireEnum;
import aprs.framework.database.DbQueryEnum;
import aprs.framework.database.DbQueryInfo;
import aprs.framework.database.DbSetup;
import aprs.framework.database.DbSetupBuilder;
import aprs.framework.database.DbSetupListener;
import aprs.framework.database.DbSetupPublisher;
import aprs.framework.database.DbType;
import aprs.framework.database.DetectedItem;
import aprs.framework.database.DetectedItemJPanel;
import aprs.framework.database.PoseQueryElem;
import aprs.framework.database.SocketLineReader;
import crcl.base.PoseType;
import crcl.ui.XFuture;
import crcl.ui.misc.MultiLineStringJPanel;
import crcl.utils.CRCLPosemath;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.TransferHandler;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import static crcl.utils.CRCLPosemath.pose;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;
import java.awt.Desktop;
import java.io.BufferedWriter;
import javax.swing.JTable;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import static crcl.utils.CRCLPosemath.pose;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;
import static crcl.utils.CRCLPosemath.pose;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;
import static crcl.utils.CRCLPosemath.pose;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;
import static crcl.utils.CRCLPosemath.pose;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;
import static crcl.utils.CRCLPosemath.pose;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;
import static crcl.utils.CRCLPosemath.pose;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;
import static crcl.utils.CRCLPosemath.pose;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;
import static crcl.utils.CRCLPosemath.pose;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;
import static crcl.utils.CRCLPosemath.pose;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;
import static crcl.utils.CRCLPosemath.pose;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;
import static crcl.utils.CRCLPosemath.pose;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;
import static crcl.utils.CRCLPosemath.pose;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;
import static crcl.utils.CRCLPosemath.pose;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;
import static crcl.utils.CRCLPosemath.pose;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;
import static crcl.utils.CRCLPosemath.pose;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class VisionToDBJPanel extends javax.swing.JPanel implements VisionToDBJFrameInterface, DbSetupListener, VisionSocketClient.VisionSocketClientListener {

    private DbSetupPublisher dbSetupPublisher;

    /**
     * Creates new form VisionToDBJPanel
     */
    public VisionToDBJPanel() {
        initComponents();
//        jSpinnerLogLines.setValue(100);
        loadProperties();
        oldDbType = null;
        jTableTransform.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                updateTransformFromTable();
            }
        });
        jTableFromVision.getSelectionModel().addListSelectionListener(x -> {
            jButtonForceSingleUpdate.setEnabled(jTableFromVision.getSelectedRow() >= 0 && null != dpu);
        });
    }

    private double transform(int row, int col) {
        return (double) jTableTransform.getValueAt(row, col);
    }

    private VisionSocketClient visionClient;

    /**
     * Get the value of visionClient
     *
     * @return the value of visionClient
     */
    public VisionSocketClient getVisionClient() {
        return visionClient;
    }

    /**
     * Set the value of visionClient
     *
     * @param visionClient new value of visionClient
     */
    public void setVisionClient(VisionSocketClient visionClient) {
        this.visionClient = visionClient;
    }

    private DatabasePoseUpdater dpu;

    /**
     * Get the value of dpu
     *
     * @return the value of dpu
     */
    public DatabasePoseUpdater getDpu() {
        return dpu;
    }

    private void updateTransformFromTable() {
        try {
            PoseType pose = getTransformPose();
            if (null != dpu) {
                dpu.getUpdateResultsMap().clear();
                boolean origForceUpdates = dpu.isForceUpdates();
                dpu.setForceUpdates(true);
                if (null != visionClient) {
                    visionClient.setDebug(this.jCheckBoxDebug.isSelected());
                    visionClient.setTransform(pose);
//                    visionClient.publishVisionList(dpu, this);

                }
                dpu.setForceUpdates(origForceUpdates);
            }

        } catch (Exception e) {
            addLogMessage(e);
        }
    }

    public PoseType getTransformPose() {
        PoseType pose = pose(point(transform(0, 1), transform(0, 2), transform(0, 3)),
                vector(transform(1, 1), transform(1, 2), transform(1, 3)),
                vector(transform(2, 2), transform(2, 2), transform(2, 3)));
        return pose;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jButtonDisconnectVision = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jButtonConnectVision = new javax.swing.JButton();
        jTextFieldVisionHost = new javax.swing.JTextField();
        jTextFieldVisionPort = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jTextFieldPoseUpdatesParsed = new javax.swing.JTextField();
        jTextFieldPoseUpdatesProcessed = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jLabelDatabaseStatus = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabelVisionStatus = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabelCommnandStatus = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jTextFieldCmdPort = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        jTextFieldLastCommand = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        jTextFieldAcquire = new javax.swing.JTextField();
        jCheckBoxAddRepeatCountsToDatabaseNames = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableTransform = new javax.swing.JTable();
        jLabel3 = new javax.swing.JLabel();
        jTextFieldPerformance = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jTextFieldRotationOffset = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextAreaLog = new javax.swing.JTextArea();
        jLabel11 = new javax.swing.JLabel();
        jCheckBoxDebug = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        jPanelTableFromDatabase = new javax.swing.JPanel();
        jScrollPaneTableFromDatabase = new javax.swing.JScrollPane();
        jTableFromDatabase = new javax.swing.JTable();
        jLabel20 = new javax.swing.JLabel();
        jButtonCheck = new javax.swing.JButton();
        jButtonAddItem = new javax.swing.JButton();
        jButtonDelete = new javax.swing.JButton();
        jButtonCsvFromDatabase = new javax.swing.JButton();
        jButtonCheckNewItemsOnly = new javax.swing.JButton();
        jButtonShowImage = new javax.swing.JButton();
        jPanelTableFromVision = new javax.swing.JPanel();
        jScrollPaneTableFromVision = new javax.swing.JScrollPane();
        jTableFromVision = new javax.swing.JTable();
        jLabel19 = new javax.swing.JLabel();
        jButtonForceSingleUpdate = new javax.swing.JButton();
        jButtonForceAll = new javax.swing.JButton();
        jButtonCsv = new javax.swing.JButton();
        jPanelTableUpdateResults = new javax.swing.JPanel();
        jScrollPaneTableUpdateResults = new javax.swing.JScrollPane();
        jTableUpdateResults = new javax.swing.JTable();
        jLabel21 = new javax.swing.JLabel();
        jButtonUpdateResultDetails = new javax.swing.JButton();
        jCheckBoxVerifyUpdates = new javax.swing.JCheckBox();
        jCheckBoxForceUpdates = new javax.swing.JCheckBox();
        jSpinnerLogLines = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();

        jPanel1.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jButtonDisconnectVision.setText("Disconnect From Vision");
        jButtonDisconnectVision.setEnabled(false);
        jButtonDisconnectVision.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDisconnectVisionActionPerformed(evt);
            }
        });

        jLabel6.setText("Vision Host:");

        jLabel7.setText("Vision Port: ");

        jButtonConnectVision.setText("Connect to Vision");
        jButtonConnectVision.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonConnectVisionActionPerformed(evt);
            }
        });

        jTextFieldVisionHost.setText("localhost");
        jTextFieldVisionHost.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldVisionHostActionPerformed(evt);
            }
        });

        jTextFieldVisionPort.setText("4000");

        jLabel8.setText("Poses Received from Vision:");

        jLabel9.setText("Pose Updates Processed:");

        jTextFieldPoseUpdatesParsed.setEditable(false);
        jTextFieldPoseUpdatesParsed.setText("0");

        jTextFieldPoseUpdatesProcessed.setEditable(false);
        jTextFieldPoseUpdatesProcessed.setText("0");

        jLabel10.setText("Vision Status: ");

        jLabelDatabaseStatus.setBackground(new java.awt.Color(239, 12, 12));
        jLabelDatabaseStatus.setText("DISCONNECTED");

        jLabel12.setText("Database Status: ");

        jLabelVisionStatus.setBackground(new java.awt.Color(239, 12, 12));
        jLabelVisionStatus.setText("DISCONNECTED");

        jLabel14.setText("Command Port:");

        jLabelCommnandStatus.setBackground(new java.awt.Color(239, 12, 12));
        jLabelCommnandStatus.setText("DISCONNECTED");

        jLabel15.setText("Command Port:");

        jTextFieldCmdPort.setText("4001");
        jTextFieldCmdPort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldCmdPortActionPerformed(evt);
            }
        });

        jLabel17.setText("Last Command Recieved:");

        jTextFieldLastCommand.setEditable(false);

        jLabel18.setText("Acquire State:");

        jTextFieldAcquire.setText("ON");
        jTextFieldAcquire.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldAcquireActionPerformed(evt);
            }
        });

        jCheckBoxAddRepeatCountsToDatabaseNames.setText("Add Repeat Counts To Database Names");
        jCheckBoxAddRepeatCountsToDatabaseNames.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxAddRepeatCountsToDatabaseNamesActionPerformed(evt);
            }
        });

        jLabel2.setText("Transform:");

        jTableTransform.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Point",  new Double(0.0),  new Double(0.0),  new Double(0.0)},
                {"Xaxis",  new Double(1.0),  new Double(0.0),  new Double(0.0)},
                {"Zaxis",  new Double(0.0),  new Double(0.0),  new Double(1.0)}
            },
            new String [] {
                "Label", "x/i", "y/j", "z/k"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTableTransform);

        jLabel3.setText("Performance:");

        jTextFieldPerformance.setEditable(false);

        jLabel4.setText("Rotation Offset");

        jTextFieldRotationOffset.setText("0.0");
        jTextFieldRotationOffset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldRotationOffsetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel10)
                                    .addComponent(jLabel14)
                                    .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel12)
                                    .addComponent(jLabel17)
                                    .addComponent(jLabel18)
                                    .addComponent(jButtonConnectVision, javax.swing.GroupLayout.PREFERRED_SIZE, 203, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextFieldAcquire, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                            .addComponent(jTextFieldLastCommand, javax.swing.GroupLayout.DEFAULT_SIZE, 213, Short.MAX_VALUE)
                            .addComponent(jLabelCommnandStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabelVisionStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabelDatabaseStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jTextFieldCmdPort)
                            .addComponent(jTextFieldPoseUpdatesProcessed)
                            .addComponent(jTextFieldPoseUpdatesParsed)
                            .addComponent(jButtonDisconnectVision, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jTextFieldVisionPort)
                            .addComponent(jTextFieldVisionHost)))
                    .addComponent(jTextFieldPerformance)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCheckBoxAddRepeatCountsToDatabaseNames)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jTextFieldRotationOffset, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jButtonDisconnectVision, jLabelCommnandStatus, jLabelDatabaseStatus, jLabelVisionStatus, jTextFieldAcquire, jTextFieldCmdPort, jTextFieldLastCommand, jTextFieldPoseUpdatesParsed, jTextFieldPoseUpdatesProcessed, jTextFieldVisionHost, jTextFieldVisionPort});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jTextFieldVisionHost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jTextFieldVisionPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonConnectVision)
                    .addComponent(jButtonDisconnectVision))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(jTextFieldPoseUpdatesParsed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jTextFieldPoseUpdatesProcessed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(jTextFieldCmdPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelDatabaseStatus)
                    .addComponent(jLabel12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jLabelVisionStatus))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14)
                    .addComponent(jLabelCommnandStatus))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(jTextFieldLastCommand, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18)
                    .addComponent(jTextFieldAcquire, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxAddRepeatCountsToDatabaseNames)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jTextFieldRotationOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldPerformance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTextAreaLog.setEditable(false);
        jTextAreaLog.setColumns(20);
        jTextAreaLog.setRows(5);
        jTextAreaLog.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jTextAreaLogMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jTextAreaLogMouseReleased(evt);
            }
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTextAreaLogMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(jTextAreaLog);

        jLabel11.setText("Log: ");

        jCheckBoxDebug.setText("Debug");
        jCheckBoxDebug.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxDebugActionPerformed(evt);
            }
        });

        jPanel2.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jTableFromDatabase.setAutoCreateRowSorter(true);
        jTableFromDatabase.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "X", "Y", "Z", "Rotation", "Vision Cycle"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPaneTableFromDatabase.setViewportView(jTableFromDatabase);

        jLabel20.setText("From Database: ");

        jButtonCheck.setText("Check");
        jButtonCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCheckActionPerformed(evt);
            }
        });

        jButtonAddItem.setText("Add Item");
        jButtonAddItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddItemActionPerformed(evt);
            }
        });

        jButtonDelete.setText("Delete");
        jButtonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteActionPerformed(evt);
            }
        });

        jButtonCsvFromDatabase.setText("csv");
        jButtonCsvFromDatabase.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCsvFromDatabaseActionPerformed(evt);
            }
        });

        jButtonCheckNewItemsOnly.setText("New");
        jButtonCheckNewItemsOnly.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCheckNewItemsOnlyActionPerformed(evt);
            }
        });

        jButtonShowImage.setText("Image");
        jButtonShowImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonShowImageActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelTableFromDatabaseLayout = new javax.swing.GroupLayout(jPanelTableFromDatabase);
        jPanelTableFromDatabase.setLayout(jPanelTableFromDatabaseLayout);
        jPanelTableFromDatabaseLayout.setHorizontalGroup(
            jPanelTableFromDatabaseLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTableFromDatabaseLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelTableFromDatabaseLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPaneTableFromDatabase, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(jPanelTableFromDatabaseLayout.createSequentialGroup()
                        .addComponent(jLabel20)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonCsvFromDatabase)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonDelete)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonAddItem)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonCheck)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonCheckNewItemsOnly)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonShowImage)))
                .addContainerGap())
        );
        jPanelTableFromDatabaseLayout.setVerticalGroup(
            jPanelTableFromDatabaseLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTableFromDatabaseLayout.createSequentialGroup()
                .addGroup(jPanelTableFromDatabaseLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20)
                    .addComponent(jButtonCheck)
                    .addComponent(jButtonAddItem)
                    .addComponent(jButtonDelete)
                    .addComponent(jButtonCsvFromDatabase)
                    .addComponent(jButtonCheckNewItemsOnly)
                    .addComponent(jButtonShowImage))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPaneTableFromDatabase, javax.swing.GroupLayout.DEFAULT_SIZE, 112, Short.MAX_VALUE)
                .addContainerGap())
        );

        jScrollPaneTableFromVision.setMinimumSize(new java.awt.Dimension(200, 200));
        jScrollPaneTableFromVision.setPreferredSize(new java.awt.Dimension(200, 200));

        jTableFromVision.setAutoCreateRowSorter(true);
        jTableFromVision.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Position", "Name", "Repeats", "Rotation", "X", "Y", "Z", "Score", "Type", "In PT?", "In KT?", "FullName", "VisionCycle", "Query"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPaneTableFromVision.setViewportView(jTableFromVision);

        jLabel19.setText("From Vision:");

        jButtonForceSingleUpdate.setText("Force Single Update");
        jButtonForceSingleUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonForceSingleUpdateActionPerformed(evt);
            }
        });

        jButtonForceAll.setText("Force All");
        jButtonForceAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonForceAllActionPerformed(evt);
            }
        });

        jButtonCsv.setText("csv");
        jButtonCsv.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCsvActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelTableFromVisionLayout = new javax.swing.GroupLayout(jPanelTableFromVision);
        jPanelTableFromVision.setLayout(jPanelTableFromVisionLayout);
        jPanelTableFromVisionLayout.setHorizontalGroup(
            jPanelTableFromVisionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTableFromVisionLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelTableFromVisionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPaneTableFromVision, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanelTableFromVisionLayout.createSequentialGroup()
                        .addComponent(jLabel19)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonCsv)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonForceAll)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonForceSingleUpdate)))
                .addContainerGap())
        );
        jPanelTableFromVisionLayout.setVerticalGroup(
            jPanelTableFromVisionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTableFromVisionLayout.createSequentialGroup()
                .addGroup(jPanelTableFromVisionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel19)
                    .addComponent(jButtonForceSingleUpdate)
                    .addComponent(jButtonForceAll)
                    .addComponent(jButtonCsv))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPaneTableFromVision, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTableUpdateResults.setAutoCreateRowSorter(true);
        jTableUpdateResults.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Verified", "Exception Occured", "Total Update Count"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPaneTableUpdateResults.setViewportView(jTableUpdateResults);

        jLabel21.setText("Update Results: ");

        jButtonUpdateResultDetails.setText("Details");
        jButtonUpdateResultDetails.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonUpdateResultDetailsActionPerformed(evt);
            }
        });

        jCheckBoxVerifyUpdates.setText("Verify ");
        jCheckBoxVerifyUpdates.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxVerifyUpdatesActionPerformed(evt);
            }
        });

        jCheckBoxForceUpdates.setSelected(true);
        jCheckBoxForceUpdates.setText("Force all updates");
        jCheckBoxForceUpdates.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxForceUpdatesActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelTableUpdateResultsLayout = new javax.swing.GroupLayout(jPanelTableUpdateResults);
        jPanelTableUpdateResults.setLayout(jPanelTableUpdateResultsLayout);
        jPanelTableUpdateResultsLayout.setHorizontalGroup(
            jPanelTableUpdateResultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTableUpdateResultsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelTableUpdateResultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPaneTableUpdateResults, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(jPanelTableUpdateResultsLayout.createSequentialGroup()
                        .addComponent(jLabel21)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jCheckBoxForceUpdates)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxVerifyUpdates)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonUpdateResultDetails)))
                .addContainerGap())
        );
        jPanelTableUpdateResultsLayout.setVerticalGroup(
            jPanelTableUpdateResultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTableUpdateResultsLayout.createSequentialGroup()
                .addGroup(jPanelTableUpdateResultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel21)
                    .addComponent(jButtonUpdateResultDetails)
                    .addComponent(jCheckBoxVerifyUpdates)
                    .addComponent(jCheckBoxForceUpdates))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPaneTableUpdateResults, javax.swing.GroupLayout.DEFAULT_SIZE, 159, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanelTableFromDatabase, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelTableUpdateResults, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelTableFromVision, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelTableFromDatabase, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelTableUpdateResults, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelTableFromVision, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSpinnerLogLines.setValue(200);

        jLabel1.setText("Lines to Keep:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxDebug)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSpinnerLogLines, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(jCheckBoxDebug)
                    .addComponent(jSpinnerLogLines, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 73, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonDisconnectVisionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDisconnectVisionActionPerformed
        disconnectVision();
    }//GEN-LAST:event_jButtonDisconnectVisionActionPerformed

    public void disconnectVision() {
        stopVisionStartThread();
        closeVision();
    }

    public void closeVision() {
        if (null != visionClient) {
            try {
                visionClient.close();
            } catch (Exception ex) {
                Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        visionClient = null;
        runOnDispatchThread(() -> setVisionConnected(false));
    }

    private void stopVisionStartThread() {
        if (null != startVisionThread) {
            startVisionThread.interrupt();
            try {
                startVisionThread.join(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
            startVisionThread = null;
        }
    }

    private volatile boolean updatingFromArgs = false;

    public void updateFromArgs(Map<String, String> _argsMap) {
        try {
            updatingFromArgs = true;
            String addRepeatCountsToDatabaseNamesString = _argsMap.get(ADD_REPEAT_COUNTS_TO_DATABASE_NAMES);
            if (addRepeatCountsToDatabaseNamesString != null) {
                boolean b = Boolean.valueOf(addRepeatCountsToDatabaseNamesString);
                if (jCheckBoxAddRepeatCountsToDatabaseNames.isSelected() != b) {
                    jCheckBoxAddRepeatCountsToDatabaseNames.setSelected(b);
                }
                this.addRepeatCountsToDatabaseNames = b;
            }
            String visionPortString = _argsMap.get("--visionport");
            if (visionPortString != null) {
                jTextFieldVisionPort.setText(visionPortString);
            }
            String visionHostString = _argsMap.get("--visionhost");
            if (visionHostString != null) {
                jTextFieldVisionHost.setText(visionHostString);
            }
            DefaultTableModel model = (DefaultTableModel) jTableTransform.getModel();
            String ptXString = _argsMap.get("transform.point.x");
            if (null != ptXString) {
                double x = Double.parseDouble(ptXString);
                model.setValueAt(x, 0, 1);
            }
            String ptYString = _argsMap.get("transform.point.y");
            if (null != ptYString) {
                double y = Double.parseDouble(ptYString);
                model.setValueAt(y, 0, 2);
            }
            String ptZString = _argsMap.get("transform.point.z");
            if (null != ptZString) {
                double z = Double.parseDouble(ptZString);
                model.setValueAt(z, 0, 3);
            }
            String xAxisIStriing = _argsMap.get("transform.xaxis.i");
            if (null != xAxisIStriing) {
                double xi = Double.parseDouble(xAxisIStriing);
                model.setValueAt(xi, 1, 1);
            }
            String xAxisJStriing = _argsMap.get("transform.xaxis.j");
            if (null != xAxisJStriing) {
                double xj = Double.parseDouble(xAxisJStriing);
                model.setValueAt(xj, 1, 2);
            }
            String xAxisKStriing = _argsMap.get("transform.xaxis.k");
            if (null != xAxisKStriing) {
                double xk = Double.parseDouble(xAxisKStriing);
                model.setValueAt(xk, 1, 3);
            }
            String zAxisIStriing = _argsMap.get("transform.zaxis.i");
            if (null != zAxisIStriing) {
                double zi = Double.parseDouble(zAxisIStriing);
                model.setValueAt(zi, 2, 1);
            }
            String zAxisJStriing = _argsMap.get("transform.zaxis.j");
            if (null != zAxisJStriing) {
                double zj = Double.parseDouble(zAxisJStriing);
                model.setValueAt(zj, 2, 2);
            }
            String zAxisKStriing = _argsMap.get("transform.zaxis.k");
            if (null != zAxisKStriing) {
                double zk = Double.parseDouble(zAxisKStriing);
                model.setValueAt(zk, 2, 3);
            }

            String rotationOffsetString = _argsMap.get("rotationOffset");
            if (null != rotationOffsetString) {
                double ro = Double.parseDouble(rotationOffsetString);
                jTextFieldRotationOffset.setText(Double.toString(ro));
                if (null != dpu) {
                    dpu.setRotationOffset(ro);
                }
            }

        } finally {
            updatingFromArgs = false;
        }
//        props.put(this.getDbType() + ".host", this.jTextFieldDBHost.getText());
//        props.put(this.getDbType() + "."+this.jTextFieldDBHost.getText()+".port", this.jTextFieldDBPort.getText());
//        props.put(this.getDbType() + "."+this.jTextFieldDBHost.getText()+".name", this.jTextFieldDBName.getText());
//        props.put(this.getDbType() + "."+this.jTextFieldDBHost.getText()+".user", this.jTextFieldDBUser.getText());
//        props.put(this.getDbType() + "."+this.jTextFieldDBHost.getText()+".passwd", this.jPasswordFieldDBPassword.getPassword());
//        
    }

    private void setText(Map<String, String> argsMap, JTextField fld, String key) {
        if (argsMap.containsKey(key)) {
            fld.setText(argsMap.get(key));
        }
    }

    int update_info_count = 0;

    @Override
    public void updataPoseQueryInfo(final List<PoseQueryElem> _list) {
//        this.pq_list = _list;
        DefaultTableModel tm = (DefaultTableModel) this.jTableFromDatabase.getModel();
//        TableColumnModel tcm = this.jTableFromCognex.getColumnModel();
        tm.setRowCount(0);
        for (int i = 0; i < _list.size(); i++) {
            PoseQueryElem pqe = _list.get(i);
            if (tm.getRowCount() <= i) {
                tm.addRow(new Object[]{pqe.getName(), pqe.getX(), pqe.getY(), pqe.getZ(), pqe.getRot(), pqe.getVisioncycle()});
                continue;
            }
            tm.setValueAt(pqe.getName(), i, 0);
            tm.setValueAt(pqe.getX(), i, 1);
            tm.setValueAt(pqe.getY(), i, 2);
            tm.setValueAt(pqe.getZ(), i, 3);
            tm.setValueAt(pqe.getRot(), i, 4);
            tm.setValueAt(pqe.getVisioncycle(), i, 5);
        }
        autoResizeTableColWidths(jTableFromDatabase);
    }

    public boolean isDebug() {
        return this.jCheckBoxDebug.isSelected();
    }

    private List<String> logLines = new LinkedList<>();

    private void appendLogDisplay(String txt) {
        int maxLines = 100;
        try {
            maxLines = (int) jSpinnerLogLines.getValue();
        } catch (Exception ex) {

        }
//        System.out.println("maxLines = " + maxLines);
//        System.out.println("logLines.size() = " + logLines.size());
//        System.out.println("jTextAreaLog.getText().length() = " + jTextAreaLog.getText().length());
        if (logLines.size() < maxLines) {
            logLines.add(txt);
            jTextAreaLog.append(txt);
        } else {
            while (logLines.size() >= maxLines) {
                logLines.remove(0);
            }
            logLines.add(txt);
            StringBuilder sb = new StringBuilder();

            for (String oldTxt : logLines) {
                sb.append(oldTxt);
            }
            jTextAreaLog.setText(sb.toString());
        }
        jTextAreaLog.setCaretPosition(jTextAreaLog.getText().length());
    }

    public void updateInfo(List<DetectedItem> visionList, String line) {
        DefaultTableModel tm = (DefaultTableModel) this.jTableFromVision.getModel();
//        TableColumnModel tcm = this.jTableFromCognex.getColumnModel();
        tm.setRowCount(0);
        if (null != visionList) {
            for (int i = 0; i < visionList.size(); i++) {
                DetectedItem ci = visionList.get(i);
                if (ci.fullName == null || ci.fullName.length() < 1) {
                    System.err.println("bad ci fullname " + ci);
                }
                if (tm.getRowCount() <= i) {
                    tm.addRow(new Object[]{i, ci.name, ci.repeats, ci.rotation, ci.x, ci.y, ci.z, ci.score, ci.type, ci.insidePartsTray, ci.insideKitTray, ci.fullName, ci.visioncycle, ci.setQuery});
                    continue;
                }
                tm.setValueAt(ci.index, i, 0);
                tm.setValueAt(ci.name, i, 1);
                tm.setValueAt(ci.repeats, i, 2);
                tm.setValueAt(ci.rotation, i, 3);
                tm.setValueAt(ci.x, i, 4);
                tm.setValueAt(ci.y, i, 5);
                tm.setValueAt(ci.z, i, 6);
                tm.setValueAt(ci.score, i, 7);
                tm.setValueAt(ci.type, i, 8);
                tm.setValueAt(ci.insidePartsTray, i, 9);
                tm.setValueAt(ci.insideKitTray, i, 10);
                tm.setValueAt(ci.fullName, i, 11);
                tm.setValueAt(ci.visioncycle, i, 11);
                tm.setValueAt(ci.setQuery, i, 12);
            }
        }
        if (this.jCheckBoxDebug.isSelected()) {
            appendLogDisplay(line + "\r\n");
        }
        this.jTextFieldPoseUpdatesProcessed.setText(Integer.toString(DatabasePoseUpdater.poses_updated));
        if (null != visionClient) {
            this.jTextFieldPoseUpdatesParsed.setText(Integer.toString(visionClient.getPoseUpdatesParsed()));
        }
        update_info_count++;
        if (this.jCheckBoxDebug.isSelected()) {
            appendLogDisplay("\nupdateInfo(\n\t_list=" + visionList + ",\n\tline =" + line + "\n\t)\r\n");
        }
        autoResizeTableColWidths(jTableFromVision);
    }

//    private final TableModelListener tableModelListener;
    public void setVisionConnected(boolean _val) {
        this.jButtonConnectVision.setEnabled(!_val);
        this.jButtonDisconnectVision.setEnabled(_val);
        this.jLabelVisionStatus.setText(_val ? "CONNECTED" : "DISCONNECTED");
        this.jLabelVisionStatus.setBackground(_val ? Color.GREEN : Color.RED);
        if (null != aprsJFrame) {
            aprsJFrame.setShowVisionConnected(_val);
        }
    }

    public void setDBConnected(boolean _val) {
        try {
            dbSetupPublisher.setDbSetup(new DbSetupBuilder().setup(dbSetupPublisher.getDbSetup()).connected(_val).build());
            this.jLabelDatabaseStatus.setText(_val ? "CONNECTED" : "DISCONNECTED");
            this.jLabelDatabaseStatus.setBackground(_val ? Color.GREEN : Color.RED);
            if (_val) {
                if (null != visionClient) {
                    visionClient.setDebug(this.jCheckBoxDebug.isSelected());
                    visionClient.setTransform(this.getTransformPose());
                    visionClient.updateListeners();
                }
            }
            this.jButtonAddItem.setEnabled(_val);
            this.jButtonCheck.setEnabled(_val);
            this.jButtonForceAll.setEnabled(_val);
            this.jButtonForceSingleUpdate.setEnabled(_val && jTableFromVision.getSelectedRow() >= 0);
            if (null != dpu) {
                this.jCheckBoxForceUpdates.setSelected(dpu.isForceUpdates());
            }
        } catch (Exception ex) {
            this.jLabelDatabaseStatus.setText("DISCONNECTED");
            this.jLabelDatabaseStatus.setBackground(Color.RED);
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setCommandConnected(boolean _val) {
//        this.jButtonConnectDB.setEnabled(!_val);
//        this.jButtonDisconnectDB.setEnabled(_val);
        this.jLabelCommnandStatus.setText(_val ? "CONNECTED" : "DISCONNECTED");
        this.jLabelCommnandStatus.setBackground(_val ? Color.GREEN : Color.RED);
    }

//    /**
//     * @param args the command line arguments
//     */
//    public static void main(String args[]) {
//        /* Set the Nimbus look and feel */
//        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
//        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
//         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
//         */
//        try {
//            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//                if ("Nimbus".equals(info.getName())) {
//                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
//                    break;
//                }
//            }
//        } catch (ClassNotFoundException ex) {
//            java.util.logging.Logger.getLogger(MainJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (InstantiationException ex) {
//            java.util.logging.Logger.getLogger(MainJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            java.util.logging.Logger.getLogger(MainJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
//            java.util.logging.Logger.getLogger(MainJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        }
//        //</editor-fold>
//        //</editor-fold>
//
//        /* Create and display the form */
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                new MainJFrame().setVisible(true);
//            }
//        });
//    }
    public int log_count = 0;

    public void addLogMessage(String stmnt) {
        log_count++;
        System.out.println(stmnt);
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            appendLogDisplay(stmnt + "\r\n");
        } else {
            javax.swing.SwingUtilities.invokeLater(() -> appendLogDisplay(stmnt + "\r\n"));
        }
    }

    public void addLogMessage(Exception exception) {
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        addLogMessage(sw.toString());
    }

    public void setLastCommand(String c) {
        appendLogDisplay(c + "\n");
        this.jTextFieldLastCommand.setText(c);
    }

    public void setAquiring(String s) {
        this.jTextFieldAcquire.setText(s);
    }

    public void connectDB(Map<DbQueryEnum, DbQueryInfo> queriesMap) {
        try {
            Map<String, String> argsMap = updateArgsMap();
            closeDB();
            DbType type = DbType.valueOf(argsMap.get("--dbtype"));
            double ro = Math.toRadians(Double.parseDouble(jTextFieldRotationOffset.getText()));
            DatabasePoseUpdater.createDatabasePoseUpdater(argsMap.get("--dbhost"),
                    Short.valueOf(argsMap.get("--dbport")),
                    argsMap.get("--dbname"),
                    argsMap.get("--dbuser"),
                    argsMap.get("--dbpasswd"),
                    type,
                    queriesMap,
                    isDebug())
                    .thenAccept((DatabasePoseUpdater x) -> {
                        if (null != x) {
                            x.setRotationOffset(ro);
                        }
                        VisionToDBJPanel.this.dpu = x;
                    });
            if (null != dpu) {
                dpu.setRotationOffset(ro);
            }
            setDBConnected(true);
        } catch (Exception exception) {

            StringWriter sw = new StringWriter();
            exception.printStackTrace(new PrintWriter(sw));
            this.addLogMessage("connectDB failed :" + System.lineSeparator() + sw);
            System.err.println(exception.getLocalizedMessage());
            System.err.println("Connect to database failed.");
        }
    }

    @Override
    public Map<String, String> updateArgsMap() {
        return updateArgsMap(dbSetupPublisher.getDbSetup().getDbType());
    }

    private final Map<String, String> argsMap = DbSetupBuilder.getDefaultArgsMap();

    public Map<String, String> updateArgsMap(DbType dbtype) {

        DbSetup curSetup = dbSetupPublisher.getDbSetup();
        if (null != curSetup) {
            argsMap.put("--dbhost", curSetup.getHost());
            argsMap.put("--dbport", Integer.toString(curSetup.getPort()));
            argsMap.put("--dbname", curSetup.getDbName());
            argsMap.put("--dbuser", curSetup.getDbUser());
            argsMap.put("--dbpasswd",
                    new String(curSetup.getDbPassword()));
            argsMap.put("--dbtype", curSetup.getDbType().toString());
        }
        argsMap.put("transform.point.x", Double.toString(transform(0, 1)));
        argsMap.put("transform.point.y", Double.toString(transform(0, 2)));
        argsMap.put("transform.point.z", Double.toString(transform(0, 3)));
        argsMap.put("transform.xaxis.i", Double.toString(transform(1, 1)));
        argsMap.put("transform.xaxis.j", Double.toString(transform(1, 2)));
        argsMap.put("transform.xaxis.k", Double.toString(transform(1, 3)));
        argsMap.put("transform.zaxis.i", Double.toString(transform(2, 1)));
        argsMap.put("transform.zaxis.j", Double.toString(transform(2, 2)));
        argsMap.put("transform.zaxis.k", Double.toString(transform(2, 3)));
        argsMap.put("--visionhost", this.jTextFieldVisionHost.getText());
        argsMap.put("--visionport", this.jTextFieldVisionPort.getText());
        argsMap.put(ADD_REPEAT_COUNTS_TO_DATABASE_NAMES,
                Boolean.toString(this.jCheckBoxAddRepeatCountsToDatabaseNames.isSelected()));
        return argsMap;
    }
    public static final String ADD_REPEAT_COUNTS_TO_DATABASE_NAMES = "AddRepeatCountsToDatabaseNames";

    private Thread startVisionThread = null;
    private List<DetectedItem> transformedVisionList = null;

    public static List<DetectedItem> transformList(List<DetectedItem> in, PoseType transform) {
        List<DetectedItem> out = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) {
            DetectedItem inItem = in.get(i);
            PoseType newPose = CRCLPosemath.multiply(transform, inItem.toCrclPose());
            DetectedItem outItem = new DetectedItem(inItem.origName, newPose, inItem.visioncycle);
            outItem.name = inItem.name;
            outItem.repeats = inItem.repeats;
            outItem.index = inItem.index;
            outItem.fullName = inItem.fullName;
            outItem.name = inItem.name;
            outItem.score = inItem.score;
            outItem.type = inItem.type;
            outItem.rotation = inItem.rotation;
            outItem.slotForSkuName = inItem.slotForSkuName;
            outItem.emptySlotsCount = inItem.emptySlotsCount;
            outItem.tray = inItem.tray;
            outItem.emptySlotsList = transformList(inItem.emptySlotsList, transform);
            outItem.totalSlotsCount = inItem.totalSlotsCount;
            outItem.timestamp = inItem.timestamp;
            outItem.maxSlotDist = inItem.maxSlotDist;
            out.add(outItem);
        }
        return out;
    }
    private volatile boolean addRepeatCountsToDatabaseNames = false;

    public List<DetectedItem> getSlotOffsets(String name) {
        if (null == dpu) {
            return null;
        }
        return dpu.getSlotOffsets(name);
    }

    public List<DetectedItem> getSlots(DetectedItem item) {
        if (null == dpu) {
            return null;
        }
        return dpu.getSlots(item);
    }

    @Override
    public void visionClientUpdateRecieved(List<DetectedItem> visionList, String line) {
        if (acquire == AcquireEnum.OFF) {
            return;
        }
        if (acquire == AcquireEnum.ONCE) {
            acquire = AcquireEnum.OFF;
            if (null != commandReplyPrintStream) {
                commandReplyPrintStream.println("acquire=" + acquire);
            }
        }
        PoseType transform = getTransformPose();
        if (null != dpu && null != dpu.getSqlConnection()) {
            dpu.setDisplayInterface(this);
            List<DetectedItem> visionListWithEmptySlots = dpu.addEmptyTraySlots(visionList);
            if (null != transform) {
                transformedVisionList = transformList(visionListWithEmptySlots, transform);
                List<DetectedItem> l = dpu.updateVisionList(transformedVisionList, addRepeatCountsToDatabaseNames, false);
                runOnDispatchThread(() -> this.updateInfo(l, line));
            } else {
                List<DetectedItem> l = dpu.updateVisionList(visionListWithEmptySlots, addRepeatCountsToDatabaseNames, false);
                runOnDispatchThread(() -> this.updateInfo(l, line));
            }
        }

    }

    private void startVisionInternal(Map<String, String> argsMap) {
        closeVision();
        if (null == visionClient) {
            visionClient = new VisionSocketClient();
        }
        visionClient.setDisplayInterface(this);
//        visionClient.setDpu(dpu);
        visionClient.setDebug(this.isDebug());
        visionClient.setReplyPs(System.out);
        visionClient.addListener(this);
        visionClient.start(argsMap);
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            finishConnectVision();
        } else {
            javax.swing.SwingUtilities.invokeLater(this::finishConnectVision);
        }
    }

    public void connectVision() {
        try {
            argsMap.put("--visionhost", this.jTextFieldVisionHost.getText());
            argsMap.put("--visionport", this.jTextFieldVisionPort.getText());
            stopVisionStartThread();
            startVisionThread = new Thread(() -> startVisionInternal(argsMap), "startVisionThread");
            startVisionThread.setDaemon(true);
            startVisionThread.start();

        } catch (Exception exception) {
            addLogMessage(exception);
        }
    }

    private AprsJFrame aprsJFrame = null;

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

    private void finishConnectVision() {
        if (null != visionClient) {
            visionClient.setDebug(this.jCheckBoxDebug.isSelected());
//            visionClient.setAddRepeatCountsToDatabaseNames(this.jCheckBoxAddRepeatCountsToDatabaseNames.isSelected());
            setVisionConnected(visionClient.isConnected());
        }
        saveProperties();
        updateTransformFromTable();
        if (null != aprsJFrame) {
            aprsJFrame.setShowVisionConnected(visionClient.isConnected());
        }
    }

    private void jButtonConnectVisionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonConnectVisionActionPerformed
        connectVision();
    }//GEN-LAST:event_jButtonConnectVisionActionPerformed

    public void closeDB() {
        try {
            if (null != dpu) {
                dpu.close();
                dpu = null;
            }
            runOnDispatchThread(() -> setDBConnected(false));
        } catch (Exception ex) {
            addLogMessage(ex);
        }
    }

    private void jTextFieldVisionHostActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldVisionHostActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldVisionHostActionPerformed

    private SocketLineReader commandSlr = null;

    private void closeCommand() {
//        System.out.println("closeCommand() called.");
        if (null != commandSlr) {
            commandSlr.close();
        }
        runOnDispatchThread(() -> setCommandConnected(false));
        commandSlr = null;
    }

    void startCommand(Map<String, String> argsMap) {
        try {
            closeCommand();
            commandSlr = SocketLineReader.startServer(
                    Short.valueOf(argsMap.get("--commandport")),
                    "commandReader",
                    this::handleCommand);
            runOnDispatchThread(() -> setCommandConnected(true));
        } catch (Exception exception) {
            System.err.println(exception.getLocalizedMessage());
            System.err.println("Starting server for command port failed.");
        }
    }

    private volatile AcquireEnum acquire = AcquireEnum.ON;

    public AcquireEnum getAcquire() {
        return acquire;
    }

    public void setAcquire(AcquireEnum acquire) {
        this.acquire = acquire;
    }

    private volatile PrintStream commandReplyPrintStream = null;

    public void handleCommand(String line, PrintStream os) {
        runOnDispatchThread(() -> setLastCommand(line));
        if (null == dpu) {
            os.println("Database not connected.");
            return;
        }
        if (null == visionClient) {
            os.println("Vision not connected.");
            return;
        }
        String fa[] = line.trim().split(" ");
        if (fa.length < 1) {
            os.println("Not recognized: " + line);
            return;
        }
        if (fa[0].trim().toUpperCase().compareTo("ON") == 0) {
            setAcquire(AcquireEnum.ON);
            runOnDispatchThread(() -> {
                setAquiring(AcquireEnum.ON.toString());
            });

            os.println("Acquire Status: " + getAcquire());
        } else if (fa[0].trim().toUpperCase().compareTo("ONCE") == 0) {
            setAcquire(AcquireEnum.ONCE);
            runOnDispatchThread(() -> {
                setAquiring(AcquireEnum.ONCE.toString());
            });
            os.println("Acquire Status: " + AcquireEnum.ONCE);
        } else if (fa[0].trim().toUpperCase().compareTo("OFF") == 0) {
            setAcquire(AcquireEnum.OFF);
            runOnDispatchThread(() -> {

                setAquiring(AcquireEnum.OFF.toString());
            });
            os.println("Acquire Status: " + AcquireEnum.OFF);
            commandReplyPrintStream = os;
            if (null != visionClient) {
                visionClient.setReplyPs(commandReplyPrintStream);
            }
        } else {
            os.println("Not recognized: " + line);
            return;
        }
    }


    private void jTextFieldCmdPortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldCmdPortActionPerformed
        try {
            closeCommand();
            argsMap.put("--commandport", this.jTextFieldCmdPort.getText());
            startCommand(argsMap);
        } catch (Exception exception) {
            addLogMessage(exception);
        }
    }//GEN-LAST:event_jTextFieldCmdPortActionPerformed

    private Window getParentWindow() {
        Container container = this.getParent();
        while (null != container) {
            if (container instanceof Window) {
                return (Window) container;
            }
            container = container.getParent();
        }
        return null;
    }


    private void jTextFieldAcquireActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldAcquireActionPerformed
        setAcquire(AcquireEnum.valueOf(this.jTextFieldAcquire.getText()));
    }//GEN-LAST:event_jTextFieldAcquireActionPerformed


    private void jButtonCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCheckActionPerformed
        try {
            DefaultTableModel tm = (DefaultTableModel) this.jTableFromDatabase.getModel();
            tm.setRowCount(0);
            queryDatabase();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonCheckActionPerformed

    private XFuture<Void> queryDatabase() throws InterruptedException, ExecutionException {
        if (null != dpu) {
            return dpu.queryDatabase().thenCompose(l -> runOnDispatchThread(() -> updataPoseQueryInfo(l)));
        }
        return XFuture.completedFuture(null);
    }

    private XFuture<Void> startQueryDatabaseNew() {
        if (null != dpu) {
            return dpu.queryDatabaseNew().thenCompose(l -> runOnDispatchThread(() -> updataPoseQueryInfo(l)));
        }
        return XFuture.completedFuture(null);
    }

    private void jButtonAddItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddItemActionPerformed
        try {
            Window parentWindow = getParentWindow();
            Map<String, Object> map = DetectedItemJPanel.showDetectedItemDialog(parentWindow, "Add Item with Pose to database", Dialog.ModalityType.APPLICATION_MODAL, null);
            if (isDebug()) {
                addLogMessage("Detected item to add map = " + map);
            }
            DetectedItem item = DetectedItemJPanel.mapToItem(map, null);
            if (isDebug()) {
                addLogMessage("Detected item to add = " + item);
            }
            List<DetectedItem> singletonList = Collections.singletonList(item);
            PoseType pose = getTransformPose();
            if (null != pose) {
                List<DetectedItem> transformedList = transformList(singletonList, pose);
                System.out.println("transformedList = " + transformedList);
                dpu.updateVisionList(transformedList, jCheckBoxAddRepeatCountsToDatabaseNames.isSelected(), true);
            } else {
                dpu.updateVisionList(singletonList, jCheckBoxAddRepeatCountsToDatabaseNames.isSelected(), true);
            }
            this.queryDatabase();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonAddItemActionPerformed

    private Callable<DbSetupPublisher> dbSetupSupplier = null;

    public Callable<DbSetupPublisher> getDbSetupSupplier() {
        return dbSetupSupplier;
    }

    public void setDbSetupSupplier(Callable<DbSetupPublisher> dbSetupSupplier) {
        this.dbSetupSupplier = dbSetupSupplier;
        try {
            if (null != dbSetupSupplier) {
                dbSetupPublisher = dbSetupSupplier.call();
                if (null != dbSetupPublisher) {
                    dbSetupPublisher.addDbSetupListener(this);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    private void jCheckBoxAddRepeatCountsToDatabaseNamesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxAddRepeatCountsToDatabaseNamesActionPerformed

        if (null != visionClient) {
            visionClient.setDebug(this.jCheckBoxDebug.isSelected());
            addRepeatCountsToDatabaseNames = this.jCheckBoxAddRepeatCountsToDatabaseNames.isSelected();
        }
    }//GEN-LAST:event_jCheckBoxAddRepeatCountsToDatabaseNamesActionPerformed

    private void jCheckBoxDebugActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxDebugActionPerformed
        if (null != visionClient) {
            visionClient.setDebug(this.jCheckBoxDebug.isSelected());
        }
    }//GEN-LAST:event_jCheckBoxDebugActionPerformed

    private void jButtonUpdateResultDetailsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonUpdateResultDetailsActionPerformed
        int index = jTableUpdateResults.getSelectedRow();
        if (index >= 0) {
            String name = (String) jTableUpdateResults.getValueAt(index, 0);
            String value = resultsMap.get(name).toString();
            MultiLineStringJPanel.showText("Latest update attempt for " + name + ":\r\n" + value);
        }
    }//GEN-LAST:event_jButtonUpdateResultDetailsActionPerformed

    private JPopupMenu popMenu = new JPopupMenu();

    {
        JMenuItem copyMenuItem = new JMenuItem("Copy");
        copyMenuItem.addActionListener(e -> copyText());
    }

    private void copyText() {
        this.jTextAreaLog.getTransferHandler().exportToClipboard(this.jTextAreaLog,
                Toolkit.getDefaultToolkit().getSystemClipboard(),
                TransferHandler.COPY);
        popMenu.setVisible(false);
    }

    public void showPopup(Component comp, int x, int y) {
        popMenu.show(comp, x, y);
    }

    private void jTextAreaLogMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTextAreaLogMouseClicked
        if (evt.isPopupTrigger()) {
            showPopup(evt.getComponent(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_jTextAreaLogMouseClicked

    private void jTextAreaLogMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTextAreaLogMousePressed
        if (evt.isPopupTrigger()) {
            showPopup(evt.getComponent(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_jTextAreaLogMousePressed

    private void jTextAreaLogMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTextAreaLogMouseReleased
        if (evt.isPopupTrigger()) {
            showPopup(evt.getComponent(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_jTextAreaLogMouseReleased

    private void jCheckBoxVerifyUpdatesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxVerifyUpdatesActionPerformed
        if (null != dpu) {
            dpu.setVerify(jCheckBoxVerifyUpdates.isSelected());
        }
    }//GEN-LAST:event_jCheckBoxVerifyUpdatesActionPerformed

    private void jButtonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeleteActionPerformed
        try {
            for (int i : jTableFromDatabase.getSelectedRows()) {
                String name = (String) jTableFromDatabase.getModel().getValueAt(i, 0);
                try {

                    if (name != null && name.length() > 0) {
                        dpu.deletePose(name);
                    }

                } catch (SQLException ex) {
                    Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
                    appendLogDisplay("\nDelete " + name + " failed :" + ex + "\n");
                }
            }
            jTableFromDatabase.getSelectionModel().clearSelection();
            DefaultTableModel tm = (DefaultTableModel) this.jTableFromDatabase.getModel();
            tm.setRowCount(0);
            queryDatabase();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonDeleteActionPerformed

    private void jButtonForceSingleUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonForceSingleUpdateActionPerformed
        int row = jTableFromVision.getSelectedRow();
        if (row >= 0) {
            forceUpdateSingle(row);
        }
    }//GEN-LAST:event_jButtonForceSingleUpdateActionPerformed

    private void jButtonForceAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonForceAllActionPerformed
        forceAllUpdates();
    }//GEN-LAST:event_jButtonForceAllActionPerformed

    public void saveTableToFile(File f, JTable table) throws IOException {
        try (CSVPrinter printer = new CSVPrinter(new BufferedWriter(new FileWriter(f)), CSVFormat.DEFAULT)) {
            List<String> colHeaderList = new ArrayList<>();
            for (int i = 0; i < table.getColumnCount(); i++) {
                String colHeader = table.getColumnName(i);
                colHeaderList.add(colHeader);
            }
            printer.printRecord(colHeaderList);
            for (int i = 0; i < table.getRowCount(); i++) {
                List<String> rowList = new ArrayList<>();
                for (int j = 0; j < table.getColumnCount(); j++) {
                    Object value = table.getValueAt(i, j);
                    rowList.add(value.toString());
                }
                printer.printRecord(rowList);
            }
        }
    }

    public void toCsv(String name, JTable table) throws IOException {
        File f = File.createTempFile(name, ".csv");
        saveTableToFile(f, table);
        Desktop.getDesktop().open(f);
    }

    private void jButtonCsvActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCsvActionPerformed
        try {
            toCsv("FromVision", this.jTableFromVision);
        } catch (IOException ex) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonCsvActionPerformed

    private void jButtonCsvFromDatabaseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCsvFromDatabaseActionPerformed
        try {
            toCsv("FromDatabase", this.jTableFromDatabase);
        } catch (IOException ex) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonCsvFromDatabaseActionPerformed

    private void jTextFieldRotationOffsetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldRotationOffsetActionPerformed
        double ro = Math.toRadians(Double.parseDouble(jTextFieldRotationOffset.getText()));
        if (null != dpu) {
            dpu.setRotationOffset(ro);
        }
    }//GEN-LAST:event_jTextFieldRotationOffsetActionPerformed

    private void jCheckBoxForceUpdatesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxForceUpdatesActionPerformed
        if (null != dpu) {
            dpu.setForceUpdates(jCheckBoxForceUpdates.isSelected());
        }
    }//GEN-LAST:event_jCheckBoxForceUpdatesActionPerformed

    private void callShowDatabaseTableImage() {
        Utils.runOnDispatchThread(this::showDatabaseTableImage);
    }

    private void showDatabaseTableImage() {
        try {
            File f = File.createTempFile("newDataBaseItems_", ".png");
            takeSnapshot(f);
            Desktop.getDesktop().open(f);
        } catch (IOException ex) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void takeSnapshot(File f) {
        try {
            List<DetectedItem> list = new ArrayList<>();
            DefaultTableModel tm = (DefaultTableModel) this.jTableFromDatabase.getModel();
            for (int i = 0; i < tm.getRowCount(); i++) {
                list.add(new DetectedItem(tm.getValueAt(i, 0).toString(),
                        Double.parseDouble(tm.getValueAt(i, 4).toString()),
                        Double.parseDouble(tm.getValueAt(i, 1).toString()),
                        Double.parseDouble(tm.getValueAt(i, 2).toString())));
            }
            if (list.isEmpty()) {
                System.err.println("takeSnapshot(" + f + ") called when table is empty");
                return;
            }
            aprsJFrame.takeSimViewSnapshot(f, list);
        } catch (IOException ex) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void takeNewItemsSnapshot(File f) {
        startNewItemsCheck()
                .thenRun(() -> takeSnapshot(f));
    }

    public XFuture<Void> startTakeSnapshot(File f) {
        return Utils.runOnDispatchThread(() -> takeSnapshot(f));
    }

    public XFuture<Void> startNewItemsImageSave(File f) {
        return Utils.runOnDispatchThread(() -> takeNewItemsSnapshot(f));
    }

    private void jButtonCheckNewItemsOnlyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCheckNewItemsOnlyActionPerformed
        startNewItemsCheck();
    }//GEN-LAST:event_jButtonCheckNewItemsOnlyActionPerformed

    private XFuture<Void> startNewItemsCheck() {

        DefaultTableModel tm = (DefaultTableModel) this.jTableFromDatabase.getModel();
        tm.setRowCount(0);
        return startQueryDatabaseNew();
    }

    private void jButtonShowImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonShowImageActionPerformed
//        try {
////            DefaultTableModel tm = (DefaultTableModel) this.jTableFromDatabase.getModel();
////            tm.setRowCount(0);
////            queryDatabaseNew().thenRun(this::callShowDatabaseTableImage);
//            
//        } catch (InterruptedException | ExecutionException ex) {
//            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
//        }
        showDatabaseTableImage();
    }//GEN-LAST:event_jButtonShowImageActionPerformed

    public void forceAllUpdates() throws NumberFormatException {
        try {
            DefaultTableModel tm = (DefaultTableModel) this.jTableFromDatabase.getModel();
            tm.setRowCount(0);
            for (int i = 0; i < jTableFromVision.getRowCount(); i++) {
                forceUpdateSingle(i);
            }
            queryDatabase();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void forceUpdateSingle(int row) throws NumberFormatException {
        String name = (String) jTableFromVision.getValueAt(row, 1);
        DetectedItem item = new DetectedItem(name);
        item.rotation = Double.parseDouble(jTableFromVision.getValueAt(row, 3).toString());
        item.x = Double.parseDouble(jTableFromVision.getValueAt(row, 4).toString());
        item.y = Double.parseDouble(jTableFromVision.getValueAt(row, 5).toString());
        item.z = Double.parseDouble(jTableFromVision.getValueAt(row, 6).toString());
        item.score = Double.parseDouble(jTableFromVision.getValueAt(row, 7).toString());
        item.type = (String) jTableFromVision.getValueAt(row, 8);
        item.insidePartsTray = (Boolean) jTableFromVision.getValueAt(row, 9);
        item.insideKitTray = (Boolean) jTableFromVision.getValueAt(row, 10);
        item.fullName = (String) jTableFromVision.getValueAt(row, 11);
        item.visioncycle = Integer.parseInt(jTableFromVision.getValueAt(row, 12).toString());
        System.out.println("item = " + item);
        List<DetectedItem> singletonList = Collections.singletonList(item);
        System.out.println("singletonList = " + singletonList);
        boolean origForceUpdates = dpu.isForceUpdates();
        dpu.setForceUpdates(true);
        boolean isDebug = jCheckBoxDebug.isSelected();
        jCheckBoxDebug.setSelected(true);
//        PoseType pose = getTransformPose();
//        if (null != pose) {
//            List<DetectedItem> transformedList = transformList(singletonList, pose);
//            System.out.println("transformedList = " + transformedList);
//            dpu.updateVisionList(transformedList, jCheckBoxAddRepeatCountsToDatabaseNames.isSelected());
//        } else {
//            dpu.updateVisionList(singletonList, jCheckBoxAddRepeatCountsToDatabaseNames.isSelected());
//        }
        dpu.updateVisionList(singletonList, jCheckBoxAddRepeatCountsToDatabaseNames.isSelected(), true);
        dpu.setForceUpdates(origForceUpdates);
        jCheckBoxDebug.setSelected(isDebug);
    }

    private DbType oldDbType = null;


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAddItem;
    private javax.swing.JButton jButtonCheck;
    private javax.swing.JButton jButtonCheckNewItemsOnly;
    private javax.swing.JButton jButtonConnectVision;
    private javax.swing.JButton jButtonCsv;
    private javax.swing.JButton jButtonCsvFromDatabase;
    private javax.swing.JButton jButtonDelete;
    private javax.swing.JButton jButtonDisconnectVision;
    private javax.swing.JButton jButtonForceAll;
    private javax.swing.JButton jButtonForceSingleUpdate;
    private javax.swing.JButton jButtonShowImage;
    private javax.swing.JButton jButtonUpdateResultDetails;
    private javax.swing.JCheckBox jCheckBoxAddRepeatCountsToDatabaseNames;
    private javax.swing.JCheckBox jCheckBoxDebug;
    private javax.swing.JCheckBox jCheckBoxForceUpdates;
    private javax.swing.JCheckBox jCheckBoxVerifyUpdates;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelCommnandStatus;
    private javax.swing.JLabel jLabelDatabaseStatus;
    private javax.swing.JLabel jLabelVisionStatus;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanelTableFromDatabase;
    private javax.swing.JPanel jPanelTableFromVision;
    private javax.swing.JPanel jPanelTableUpdateResults;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPaneTableFromDatabase;
    private javax.swing.JScrollPane jScrollPaneTableFromVision;
    private javax.swing.JScrollPane jScrollPaneTableUpdateResults;
    private javax.swing.JSpinner jSpinnerLogLines;
    private javax.swing.JTable jTableFromDatabase;
    private javax.swing.JTable jTableFromVision;
    private javax.swing.JTable jTableTransform;
    private javax.swing.JTable jTableUpdateResults;
    private javax.swing.JTextArea jTextAreaLog;
    private javax.swing.JTextField jTextFieldAcquire;
    private javax.swing.JTextField jTextFieldCmdPort;
    private javax.swing.JTextField jTextFieldLastCommand;
    private javax.swing.JTextField jTextFieldPerformance;
    private javax.swing.JTextField jTextFieldPoseUpdatesParsed;
    private javax.swing.JTextField jTextFieldPoseUpdatesProcessed;
    private javax.swing.JTextField jTextFieldRotationOffset;
    private javax.swing.JTextField jTextFieldVisionHost;
    private javax.swing.JTextField jTextFieldVisionPort;
    // End of variables declaration//GEN-END:variables

    @Override
    public void dispose() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private File propertiesFile = new File(System.getProperty("user.home"), ".visiontodb_properties.txt");

    @Override
    public void setPropertiesFile(File f) {
        propertiesFile = f;
    }

    @Override
    public File getPropertiesFile() {
        return propertiesFile;
    }
    private volatile boolean savingProperties = false;

    @Override
    public void saveProperties() {
        DbSetup setup = dbSetupPublisher.getDbSetup();
        saveProperties(setup.getDbType(), setup.getHost(), setup.getPort());
    }

    public void saveProperties(DbType dbtype, String host, int port) {
        try {
            savingProperties = true;
            propertiesFile.getParentFile().mkdirs();
            Properties props = new Properties();
            if (propertiesFile.exists()) {
                try (FileReader fr = new FileReader(propertiesFile)) {
                    props.load(fr);
                } catch (IOException ex) {
                    Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            props.putAll(updateArgsMap(dbtype));
            DbSetup setup = dbSetupPublisher.getDbSetup();
            if (host == null) {
                host = setup.getHost();
            }
            if (port < 1) {
                port = setup.getPort();
            }
            props.put(dbtype + ".host", host);
            props.put(dbtype + "." + host + ".port", Integer.toString(port));
            String dbHostPort = String.format("%s.%s_%d", dbtype.toString(), host, port);
            props.put(dbHostPort + ".name", setup.getDbName());
            props.put(dbHostPort + ".user", setup.getDbUser());
            props.put(dbHostPort + ".passwd", new String(setup.getDbPassword()));
            props.put(ADD_REPEAT_COUNTS_TO_DATABASE_NAMES,
                    Boolean.toString(this.jCheckBoxAddRepeatCountsToDatabaseNames.isSelected()));
            props.put("--visionport", jTextFieldVisionPort.getText());
            props.put("--visionhost", jTextFieldVisionHost.getText());
            props.put("rotationOffset", jTextFieldRotationOffset.getText());
//            try (FileWriter fw = new FileWriter(propertiesFile)) {
//                props.store(fw, "");
//            } catch (IOException ex) {
//                Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
//            }
            Utils.saveProperties(propertiesFile, props);
        } finally {
            savingProperties = false;
        }
    }

//    public DbSetupPublisher getDbSetupPublisher() {
//        return dbSetupPublisher;
//    }
    private volatile boolean restoringProperties = false;

    public final void restoreProperties(DbType dbtype, String host, int port) {
        try {
            restoringProperties = true;
            if (null != propertiesFile && propertiesFile.exists()) {
                Properties props = new Properties();
                try (FileReader fr = new FileReader(propertiesFile)) {
                    props.load(fr);
                } catch (IOException ex) {
                    Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
                for (String propName : props.stringPropertyNames()) {
                    argsMap.put(propName, props.getProperty(propName));
                }
                updateFromArgs(argsMap);
            }
        } catch (Exception e) {
            addLogMessage(e);
        } finally {
            restoringProperties = false;
        }
    }

    @Override
    public final void loadProperties() {
        try {
            restoringProperties = true;
            if (null != propertiesFile && propertiesFile.exists()) {
                Properties props = new Properties();
                try (FileReader fr = new FileReader(propertiesFile)) {
                    props.load(fr);
                } catch (IOException ex) {
                    Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
                for (String propName : props.stringPropertyNames()) {
                    argsMap.put(propName, props.getProperty(propName));
                }
                updateFromArgs(argsMap);
            }
        } catch (Exception e) {
            addLogMessage(e);
        } finally {
            restoringProperties = false;
        }
    }

    @Override
    public Connection getSqlConnection() {
        return dpu.getSqlConnection();
    }

    private void closeDatabasePoseUpdater() {
        if (null != dpu) {
            dpu.close();
        }
        dpu = null;
    }

    @Override
    public void setSqlConnection(Connection connection, DbType dbtype) throws SQLException {
        try {
            closeDatabasePoseUpdater();
            dpu = new DatabasePoseUpdater(connection, dbtype, true,
                    dbSetupPublisher.getDbSetup().getQueriesMap());
            dpu.setVerify(this.jCheckBoxVerifyUpdates.isSelected());
            dbSetupPublisher.setDbSetup(new DbSetupBuilder().setup(dbSetupPublisher.getDbSetup()).type(dbtype).build());
        } catch (Exception ex) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
            closeDatabasePoseUpdater();
        }
    }

    @Override
    public DbType getDbType() {
        if (null != dpu) {
            DbType dupDbType = dpu.getDbType();
            DbSetup curSetup = dbSetupPublisher.getDbSetup();
            if (dupDbType != curSetup.getDbType()) {
                try {
                    this.oldDbType = null;
                    dbSetupPublisher.setDbSetup(new DbSetupBuilder().setup(curSetup).type(dupDbType).build());
                } catch (Exception ex) {
                    Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return dupDbType;
        }
        return dbSetupPublisher.getDbSetup().getDbType();
    }

    private DbSetup lastSetup = null;

    @Override
    public void accept(DbSetup setup) {
        if (setup.isConnected()) {
            if (lastSetup == null || !lastSetup.isConnected()) {
                connectDB(setup.getQueriesMap());
                lastSetup = setup;
                oldDbType = setup.getDbType();
                return;
            }
        } else if (lastSetup != null && lastSetup.isConnected()) {
            closeDB();
            lastSetup = setup;
            oldDbType = setup.getDbType();
            return;
        }

        try {
            if (!savingProperties && !restoringProperties && !updatingFromArgs) {

                DbType newDbType = setup.getDbType();
                if (oldDbType != newDbType) {
                    this.closeDB();
                    if (oldDbType != null) {
                        saveProperties(oldDbType, setup.getHost(), setup.getPort());
                    }
                    restoreProperties(newDbType, null, -1);
                    oldDbType = newDbType;
                }
            }
        } catch (Exception exception) {
            addLogMessage(exception);
        }
    }
    private Map<String, UpdateResults> resultsMap;

    private void updateResultsMapInternal(Map<String, UpdateResults> _map) {
        DefaultTableModel model = (DefaultTableModel) jTableUpdateResults.getModel();
        model.setRowCount(0);
        for (Entry<String, UpdateResults> entry : _map.entrySet()) {
            model.addRow(new Object[]{
                entry.getKey(),
                entry.getValue().isVerified(),
                ((entry.getValue().getException() != null) || (entry.getValue().getVerifyException() != null)),
                entry.getValue().getTotalUpdateCount(),});
        }
        resultsMap = _map;
        if (null != dpu && dpu.getTotalListUpdates() > 0) {
            this.jTextFieldPerformance.setText("Avg update time:"
                    + (dpu.getTotalUpdateTimeMillis() / dpu.getTotalListUpdates())
                    + " ms, worst=" + dpu.getMaxUpdateTimeMillis() + " ms");
        }
    }

    @Override
    public void updateResultsMap(Map<String, UpdateResults> _map) {
        runOnDispatchThread(() -> this.updateResultsMapInternal(_map));
    }

}
