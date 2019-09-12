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
package aprs.database.vision;

import aprs.cachedcomponents.CachedCheckBox;
import aprs.cachedcomponents.CachedTable;
import aprs.cachedcomponents.CachedTextField;
import aprs.system.AprsSystem;
import aprs.misc.Utils;
import aprs.database.AcquireEnum;
import aprs.database.DbSetup;
import aprs.database.DbSetupBuilder;
import aprs.database.DbSetupListener;
import aprs.database.DbSetupPublisher;
import aprs.database.DbType;
import aprs.database.PhysicalItem;
import aprs.database.DetectedItemJPanel;
import aprs.database.PoseQueryElem;
import aprs.database.SocketLineReader;
import aprs.database.PartsTray;
import crcl.base.PoseType;
import crcl.utils.XFuture;
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
import java.awt.Desktop;
import javax.swing.JTable;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import aprs.database.Slot;
import aprs.database.Tray;
import static aprs.misc.AprsCommonLogger.println;
import static aprs.misc.Utils.traceToString;
import crcl.utils.XFutureVoid;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.JTextArea;
import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import static aprs.misc.Utils.autoResizeTableColWidths;
import aprs.simview.Object2DJFrame;
import static crcl.utils.CRCLPosemath.pose;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;
import java.awt.Frame;
import java.awt.event.WindowEvent;
import java.util.concurrent.CompletableFuture;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings({"CanBeFinal", "UnusedReturnValue"})
public class VisionToDBJPanel extends javax.swing.JPanel implements VisionToDBJFrameInterface, DbSetupListener, VisionSocketClient.VisionSocketClientListener {

    private DbSetupPublisher dbSetupPublisher;

    /**
     * Get the most recent list of parts and kit trays from the vision system.
     * This will not block waiting for the vision system or database but could
     * return null or an empty list if the vision system has not been connected
     * or no frame has been received.
     *
     * @return list of trays
     */
    @SafeEffect
    public List<PartsTray> getPartsTrayList() {
        assert (null != dpu) : "dpu == null";
        return dpu.getPartsTrayList();
    }

    private final ConcurrentLinkedDeque<Consumer<Integer>> incrementCountListeners = new ConcurrentLinkedDeque<>();

    public void addLineCountListener(Consumer<Integer> l) {
        if (null != visionClient) {
            visionClient.addCountListener(l);
        } else {
            incrementCountListeners.add(l);
        }
    }

    public void removeLineCountListener(Consumer<Integer> l) {
        if (null != visionClient) {
            visionClient.removeCountListener(l);
        }
    }

    public int getLineCount() {
        return (null != visionClient) ? visionClient.getLineCount() : -1;
    }

    public Slot absSlotFromTrayAndOffset(PhysicalItem tray, Slot offsetItem) {
        assert (null != dpu) : "dpu == null";
        return dpu.absSlotFromTrayAndOffset(tray, offsetItem);
    }

    public Slot absSlotFromTrayAndOffset(PhysicalItem tray, Slot offsetItem, double rotationOffset) {
        assert (null != dpu) : "dpu == null";
        return dpu.absSlotFromTrayAndOffset(tray, offsetItem, rotationOffset);
    }

    boolean isDbConnected() {
        return null != dpu && dpu.isConnected();
    }

    /**
     * Creates new form VisionToDBJPanel This 0 args constructor should only be
     * used for the GUI Builder or testing. it bypasses the check to make sure
     * the aprsSystem is not null.
     */
    @SuppressWarnings({"initialization", "nullness"})
    @UIEffect
    public VisionToDBJPanel() {
        this(null);
    }

    @SuppressWarnings("initialization")
    @UIEffect
    public VisionToDBJPanel(AprsSystem aprsSystem1) {
        this.aprsSystem = aprsSystem1;
        initComponents();
        loadProperties();
        oldDbType = null;
        if (null != jTableTransform) {
            jTableTransform.getModel().addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    updateTransformFromTable();
                }
            });
        }
        if (null != jTableFromVision) {
            jTableFromVision.getSelectionModel().addListSelectionListener(x -> {
                jButtonForceSingleUpdate.setEnabled(jTableFromVision.getSelectedRow() >= 0 && null != dpu);
            });
        }
        transformCachedTable = new CachedTable(jTableTransform);
        debugCachedCheckBox = new CachedCheckBox(jCheckBoxDebug);
        addRepeatCountsToDatabaseNamesCachedCheckBox = new CachedCheckBox(jCheckBoxAddRepeatCountsToDatabaseNames);
        fromDatabaseCachedTable = new CachedTable(jTableFromDatabase);
        rotationOffsetCachedTextField = new CachedTextField(jTextFieldRotationOffset);
        visionHostCachedTextField = new CachedTextField(jTextFieldVisionHost);
        visionPortCachedTextField = new CachedTextField(jTextFieldVisionPort);
        cmdPortCachedTextField = new CachedTextField(jTextFieldCmdPort);
        fromVisionCachedTable = new CachedTable(jTableFromVision);
        verifyUpdatesCachedCheckBox = new CachedCheckBox(jCheckBoxVerifyUpdates);
        updateResultsCachedTable = new CachedTable(jTableUpdateResults);
    }

    public double getRotationOffset() {
        assert (null != dpu) :
                ("dpu == null");

        return dpu.getRotationOffset();
    }

    private final CachedTable transformCachedTable;

    private double transform(int row, int col) {
        Double value = (Double) transformCachedTable.getValueAt(row, col);
        if (value == null) {
            throw new IllegalStateException("table has null in location (" + row + "," + col + ")");
        }
        return value;
    }

    private @Nullable
    VisionSocketClient visionClient;

    /**
     * Get the value of visionClient
     *
     * @return the value of visionClient
     */
    public @Nullable
    VisionSocketClient getVisionClient() {
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

    private @MonotonicNonNull
    DatabasePoseUpdater dpu;

    /**
     * Get the value of dpu
     *
     * @return the value of dpu
     */
    public @Nullable
    DatabasePoseUpdater getDpu() {
        return dpu;
    }

    Map<String, UpdateResults> getUpdatesResultMap() {
        if (null == dpu) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(dpu.getUpdateResultsMap());
    }

    private final CachedCheckBox debugCachedCheckBox;

    private void updateTransformFromTable() {
        try {
            PoseType pose = getTransformPose();
            if (null != dpu) {
                dpu.getUpdateResultsMap().clear();
                boolean origForceUpdates = dpu.isForceUpdates();
                dpu.setForceUpdates(true);
                if (null != visionClient) {
                    visionClient.setDebug(isDebug());
                    visionClient.setTransform(pose);
//                    visionClient.publishVisionList(dpu, this);

                }
                dpu.setForceUpdates(origForceUpdates);
            }

        } catch (Exception e) {
            addLogMessage(e);
        }
    }

    private PoseType getTransformPose() {
        return pose(point(transform(0, 1), transform(0, 2), transform(0, 3)),
                vector(transform(1, 1), transform(1, 2), transform(1, 3)),
                vector(transform(2, 2), transform(2, 2), transform(2, 3)));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @UIEffect
    @SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuItemSaveProperties = new javax.swing.JMenuItem();
        jMenuItemReloadProperties = new javax.swing.JMenuItem();
        jMenuItemSetPropertiesFile = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
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
        jCheckBoxDbUpdateEnabled = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        jTextFieldRequiredParts = new javax.swing.JTextField();

        jMenuFile.setText("File");
        jMenuFile.add(jSeparator1);

        jMenuItemSaveProperties.setText("Save Properties");
        jMenuItemSaveProperties.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSavePropertiesActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemSaveProperties);

        jMenuItemReloadProperties.setText("Reload Properties");
        jMenuItemReloadProperties.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemReloadPropertiesActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemReloadProperties);

        jMenuItemSetPropertiesFile.setText("Set Properites File");
        jMenuItemSetPropertiesFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSetPropertiesFileActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemSetPropertiesFile);
        jMenuFile.add(jSeparator2);

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

        jLabel17.setText("Last Command Received:");

        jTextFieldLastCommand.setEditable(false);

        jLabel18.setText("Acquire State:");

        jTextFieldAcquire.setText("ON");
        jTextFieldAcquire.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldAcquireActionPerformed(evt);
            }
        });

        jCheckBoxAddRepeatCountsToDatabaseNames.setText("Add Repeat Counts To Database Names");

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
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jTextFieldRotationOffset, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextFieldPerformance, javax.swing.GroupLayout.PREFERRED_SIZE, 428, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jCheckBoxAddRepeatCountsToDatabaseNames)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3))
                        .addGap(0, 0, Short.MAX_VALUE)))
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

        jCheckBoxDbUpdateEnabled.setText("Db Update Enabled");
        jCheckBoxDbUpdateEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxDbUpdateEnabledActionPerformed(evt);
            }
        });

        jLabel5.setText("Required Parts:");

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
                        .addComponent(jSpinnerLogLines, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxDbUpdateEnabled)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldRequiredParts))
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
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel11)
                        .addComponent(jCheckBoxDebug)
                        .addComponent(jSpinnerLogLines, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel1))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jCheckBoxDbUpdateEnabled)
                        .addComponent(jLabel5)
                        .addComponent(jTextFieldRequiredParts, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 73, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    @UIEffect
    private void jButtonDisconnectVisionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDisconnectVisionActionPerformed
        disconnectVision();
    }//GEN-LAST:event_jButtonDisconnectVisionActionPerformed

    private void setTitleErrorString(String errString) {
        if (null != aprsSystem) {
            aprsSystem.setTitleErrorString(errString);
        }
    }

    void disconnectVision() {
        stopVisionStartThread();
        closeVision();
        if (null != aprsSystem && !aprsSystem.isClosing()) {
            Exception ex = new IllegalStateException("visionDisconnected");
            notifySingleUpdateListenersExceptionally(ex);
            setTitleErrorString("vision disconnected");
        } else {
            cancelListenersAndDisableUpdates();
        }
    }

    private void closeVision() {
        if (null != visionClient) {
            try {
                visionClient.close();
            } catch (Exception ex) {
                Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
        visionClient = null;
        aprsSystem.runOnDispatchThread(() -> setVisionConnectedOnDisplay(false));
    }

    private void stopVisionStartThread() {
        if (null != startVisionThread) {
            startVisionThread.interrupt();
            try {
                startVisionThread.join(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
            }
            startVisionThread = null;
        }
    }

    private volatile boolean updatingFromArgs = false;
    private final CachedCheckBox addRepeatCountsToDatabaseNamesCachedCheckBox;

    public void updateFromArgs(Map<String, String> _argsMap) {
        try {
            updatingFromArgs = true;
            String addRepeatCountsToDatabaseNamesString = _argsMap.get(ADD_REPEAT_COUNTS_TO_DATABASE_NAMES);
            if (addRepeatCountsToDatabaseNamesString != null) {
                boolean b = Boolean.valueOf(addRepeatCountsToDatabaseNamesString);
                if (addRepeatCountsToDatabaseNamesCachedCheckBox.isSelected() != b) {
                    addRepeatCountsToDatabaseNamesCachedCheckBox.setSelected(b);
                }
            }
            String visionPortString = _argsMap.get("--visionport");
            if (visionPortString != null) {
                visionPortCachedTextField.setText(visionPortString);
            }
            String commandPortString = _argsMap.get("--commandport");
            if (commandPortString != null) {
                cmdPortCachedTextField.setText(commandPortString);
            }
            String visionHostString = _argsMap.get("--visionhost");
            if (visionHostString != null) {
                visionHostCachedTextField.setText(visionHostString);
            }
            String ptXString = _argsMap.get("transform.point.x");
            if (null != ptXString) {
                double x = Double.parseDouble(ptXString);
                transformCachedTable.setValueAt(x, 0, 1);
            }
            String ptYString = _argsMap.get("transform.point.y");
            if (null != ptYString) {
                double y = Double.parseDouble(ptYString);
                transformCachedTable.setValueAt(y, 0, 2);
            }
            String ptZString = _argsMap.get("transform.point.z");
            if (null != ptZString) {
                double z = Double.parseDouble(ptZString);
                transformCachedTable.setValueAt(z, 0, 3);
            }
            String xAxisIStriing = _argsMap.get("transform.xaxis.i");
            if (null != xAxisIStriing) {
                double xi = Double.parseDouble(xAxisIStriing);
                transformCachedTable.setValueAt(xi, 1, 1);
            }
            String xAxisJStriing = _argsMap.get("transform.xaxis.j");
            if (null != xAxisJStriing) {
                double xj = Double.parseDouble(xAxisJStriing);
                transformCachedTable.setValueAt(xj, 1, 2);
            }
            String xAxisKStriing = _argsMap.get("transform.xaxis.k");
            if (null != xAxisKStriing) {
                double xk = Double.parseDouble(xAxisKStriing);
                transformCachedTable.setValueAt(xk, 1, 3);
            }
            String zAxisIStriing = _argsMap.get("transform.zaxis.i");
            if (null != zAxisIStriing) {
                double zi = Double.parseDouble(zAxisIStriing);
                transformCachedTable.setValueAt(zi, 2, 1);
            }
            String zAxisJStriing = _argsMap.get("transform.zaxis.j");
            if (null != zAxisJStriing) {
                double zj = Double.parseDouble(zAxisJStriing);
                transformCachedTable.setValueAt(zj, 2, 2);
            }
            String zAxisKStriing = _argsMap.get("transform.zaxis.k");
            if (null != zAxisKStriing) {
                double zk = Double.parseDouble(zAxisKStriing);
                transformCachedTable.setValueAt(zk, 2, 3);
            }

            String rotationOffsetString = _argsMap.get("rotationOffset");
            if (null != rotationOffsetString) {
                double ro = Double.parseDouble(rotationOffsetString);
                rotationOffsetCachedTextField.setText(Double.toString(ro));
                if (null != dpu) {
                    dpu.setRotationOffset(Math.toRadians(ro));
                }
            }

        } finally {
            updatingFromArgs = false;
        }
    }

    private void setText(Map<String, String> argsMap, JTextField fld, String key) {
        if (argsMap.containsKey(key)) {
            fld.setText(argsMap.get(key));
        }
    }

    private int update_info_count = 0;

    private final CachedTable fromDatabaseCachedTable;

    @Override
    public void updataPoseQueryInfo(final List<PoseQueryElem> _list) {
//        this.pq_list = _list;
//        TableColumnModel tcm = this.jTableFromCognex.getColumnModel();
        fromDatabaseCachedTable.setRowCount(0);
        for (int i = 0; i < _list.size(); i++) {
            PoseQueryElem pqe = _list.get(i);
            if (fromDatabaseCachedTable.getRowCount() <= i) {
                fromDatabaseCachedTable.addRow(new Object[]{pqe.getName(), pqe.getX(), pqe.getY(), pqe.getZ(), pqe.getRot(), pqe.getVisioncycle()});
                continue;
            }
            fromDatabaseCachedTable.setValueAt(pqe.getName(), i, 0);
            fromDatabaseCachedTable.setValueAt(pqe.getX(), i, 1);
            fromDatabaseCachedTable.setValueAt(pqe.getY(), i, 2);
            fromDatabaseCachedTable.setValueAt(pqe.getZ(), i, 3);
            fromDatabaseCachedTable.setValueAt(pqe.getRot(), i, 4);
            fromDatabaseCachedTable.setValueAt(pqe.getVisioncycle(), i, 5);
        }
        autoResizeTableColWidths(fromDatabaseCachedTable);
    }

    public boolean isDebug() {
        return debugCachedCheckBox.isSelected();
    }

    private List<String> logLines = new LinkedList<>();

    private void appendLogDisplay(String txt) {
        aprsSystem.runOnDispatchThread(() -> appendLogDisplayOnDisplay(txt));
    }

    @UIEffect
    private void appendLogDisplayOnDisplay(String txt) {
        int maxLines = 100;
        try {
            maxLines = (int) jSpinnerLogLines.getValue();
        } catch (Exception ex) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
        }
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

    @UIEffect
    private void updateInfoOnDisplay(List<PhysicalItem> visionList, String line) {
        fromVisionCachedTable.setRowCount(0);
        if (null != visionList) {
            for (int i = 0; i < visionList.size(); i++) {
                PhysicalItem ci = visionList.get(i);
                String ciFullName = ci.getFullName();
                if (ciFullName == null || ciFullName.length() < 1 || !ci.isFullNameSet()) {
                    System.err.println("bad ci fullname " + ci);
                }
                if (fromVisionCachedTable.getRowCount() <= i) {
                    fromVisionCachedTable.addRow(new Object[]{i, ci.getName(), ci.getRepeats(), ci.getRotation(), ci.x, ci.y, ci.z, ci.getScore(), ci.getType(), ci.isInsidePartsTray(), ci.isInsideKitTray(), ci.getFullName(), ci.getVisioncycle(), ci.getSetQuery()});
                    continue;
                }
                fromVisionCachedTable.setValueAt(ci.getIndex(), i, 0);
                fromVisionCachedTable.setValueAt(ci.getName(), i, 1);
                fromVisionCachedTable.setValueAt(ci.getRepeats(), i, 2);
                fromVisionCachedTable.setValueAt(ci.getRotation(), i, 3);
                fromVisionCachedTable.setValueAt(ci.x, i, 4);
                fromVisionCachedTable.setValueAt(ci.y, i, 5);
                fromVisionCachedTable.setValueAt(ci.z, i, 6);
                fromVisionCachedTable.setValueAt(ci.getScore(), i, 7);
                fromVisionCachedTable.setValueAt(ci.getType(), i, 8);
                fromVisionCachedTable.setValueAt(ci.isInsidePartsTray(), i, 9);
                fromVisionCachedTable.setValueAt(ci.isInsideKitTray(), i, 10);
                fromVisionCachedTable.setValueAt(ci.getFullName(), i, 11);
                fromVisionCachedTable.setValueAt(ci.getVisioncycle(), i, 11);
                fromVisionCachedTable.setValueAt(ci.getSetQuery(), i, 12);
            }
        }
        if (isDebug()) {
            appendLogDisplay(line + "\r\n");
        }
        this.jTextFieldPoseUpdatesProcessed.setText(Integer.toString(DatabasePoseUpdater.poses_updated.get()));
        if (null != visionClient) {
            this.jTextFieldPoseUpdatesParsed.setText(Integer.toString(visionClient.getPoseUpdatesParsed()));
        }
        update_info_count++;
        if (isDebug()) {
            appendLogDisplay("\nupdateInfo(\n\t_list=" + visionList + ",\n\tline =" + line + "\n\t)\r\n");
        }
        autoResizeTableColWidths(jTableFromVision);
    }

    public void setVisionConnected(boolean _val) {
        aprsSystem.runOnDispatchThread(() -> setVisionConnectedOnDisplay(_val));
    }

    @UIEffect
    private void setVisionConnectedOnDisplay(boolean _val) {
        this.jButtonConnectVision.setEnabled(!_val);
        this.jButtonDisconnectVision.setEnabled(_val);
        this.jLabelVisionStatus.setText(_val ? "CONNECTED" : "DISCONNECTED");
        this.jLabelVisionStatus.setBackground(_val ? Color.GREEN : Color.RED);
        if (null != aprsSystem) {
            aprsSystem.setShowVisionConnected(_val);
        }
    }

    private volatile boolean lastSetDbConnectedVal = false;

    @UIEffect
    private void setDBConnectedOnDisplay(boolean _val) {
        try {

            this.jLabelDatabaseStatus.setText(_val ? "CONNECTED" : "DISCONNECTED");
            this.jLabelDatabaseStatus.setBackground(_val ? Color.GREEN : Color.RED);
            if (_val) {
                if (null != visionClient) {
                    visionClient.setDebug(isDebug());
                    visionClient.setTransform(this.getTransformPose());
                    visionClient.updateListeners();
                }
            }
            this.jButtonAddItem.setEnabled(_val);
            this.jButtonCheck.setEnabled(_val);
            this.jButtonForceAll.setEnabled(_val);
            this.jButtonForceSingleUpdate.setEnabled(_val && fromVisionCachedTable.getSelectedRow() >= 0);
            if (null != dpu) {
                this.jCheckBoxForceUpdates.setSelected(dpu.isForceUpdates());
            }
            lastSetDbConnectedVal = _val;
        } catch (Exception ex) {
            this.jLabelDatabaseStatus.setText("DISCONNECTED");
            this.jLabelDatabaseStatus.setBackground(Color.RED);
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    @UIEffect
    private void setCommandConnectedOnDisplay(boolean _val) {
        this.jLabelCommnandStatus.setText(_val ? "CONNECTED" : "DISCONNECTED");
        this.jLabelCommnandStatus.setBackground(_val ? Color.GREEN : Color.RED);
    }

    @Override
    public void addLogMessage(String stmnt) {
        println(stmnt);
        appendLogDisplay(stmnt + "\r\n");
    }

    @Override
    public void addLogMessage(Exception exception) {
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        addLogMessage(sw.toString());
    }

    @Override
    public void setLastCommand(String c) {
        appendLogDisplay(c + "\n");
        this.jTextFieldLastCommand.setText(c);
    }

    @Override
    public void setAquiring(String s) {
        this.jTextFieldAcquire.setText(s);
    }

    private final CachedTextField rotationOffsetCachedTextField;

    private void connectDB(DbSetup dbSetup) {
        try {
            Map<String, String> argsMap = updateArgsMap();
            closeDB();
            String argsMapType = argsMap.get("--dbtype");
            if (null == argsMapType) {
                throw new IllegalStateException("argsMap.get(\"--dbtype\") returned null");
            }
            DbType type = DbType.valueOf(argsMapType);
            double ro = Math.toRadians(Double.parseDouble(rotationOffsetCachedTextField.getText()));
            String argsMapHost = argsMap.get("--dbhost");
            if (null == argsMapHost) {
                throw new IllegalStateException("argsMap.get(\"--dbhost\") returned null");
            }
            String argsMapPort = argsMap.get("--dbport");
            if (null == argsMapPort) {
                throw new IllegalStateException("argsMap.get(\"--dbport\") returned null");
            }
            String argsMapName = argsMap.get("--dbname");
            if (null == argsMapName) {
                throw new IllegalStateException("argsMap.get(\"--dbname\") returned null");
            }
            String argsMapUser = argsMap.get("--dbuser");
            if (null == argsMapUser) {
                throw new IllegalStateException("argsMap.get(\"--dbuser\") returned null");
            }
            String argsMapPasswd = argsMap.get("--dbpasswd");
            if (null == argsMapPasswd) {
                throw new IllegalStateException("argsMap.get(\"--dbpasswd\") returned null");
            }
            DatabasePoseUpdater.createDatabasePoseUpdater(argsMapHost,
                    Short.valueOf(argsMapPort),
                    argsMapName,
                    argsMapUser,
                    argsMapPasswd,
                    type,
                    dbSetup,
                    isDebug())
                    .thenAccept((DatabasePoseUpdater x) -> {
                        if (null != x) {
                            x.setRotationOffset(ro);
                            VisionToDBJPanel.this.dpu = x;
                        }
                        boolean newConnectedVal = null != dbSetup && dbSetup.isConnected() && checkConnected();
                        if (null == dbSetupPublisher.getDbSetup() || dbSetupPublisher.getDbSetup().isConnected() != newConnectedVal) {
                            dbSetupPublisher.setDbSetup(new DbSetupBuilder().setup(dbSetupPublisher.getDbSetup()).connected(newConnectedVal).build())
                                    .thenRun(() -> aprsSystem.runOnDispatchThread(() -> setDBConnectedOnDisplay(newConnectedVal)));
                        } else {
                            aprsSystem.runOnDispatchThread(() -> setDBConnectedOnDisplay(newConnectedVal));
                        }
                    });
            if (null != dpu) {
                dpu.setRotationOffset(ro);
            }
            if (null == dbSetup || !dbSetup.isConnected()) {
                aprsSystem.runOnDispatchThread(() -> setDBConnectedOnDisplay(false));
            }
        } catch (Exception exception) {

            StringWriter sw = new StringWriter();
            exception.printStackTrace(new PrintWriter(sw));
            this.addLogMessage("connectDB failed :" + System.lineSeparator() + sw);
            System.err.println(exception.getLocalizedMessage());
            System.err.println("Connect to database failed.");
        }
    }

    private boolean checkConnected() {
        try {
            if (null == dpu) {
                return false;
            }
            Connection con = dpu.getSqlConnection();
            if (con == null) {
                return false;
            }
            return !con.isClosed();
        } catch (SQLException ex) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
        }
        return false;
    }

    public Map<String, String> updateArgsMap() {
        return updateArgsMap(dbSetupPublisher.getDbSetup().getDbType());
    }

    private final Map<String, String> argsMap = DbSetupBuilder.getDefaultArgsMap();

    private final CachedTextField visionHostCachedTextField;
    private final CachedTextField visionPortCachedTextField;
    private final CachedTextField cmdPortCachedTextField;

    private Map<String, String> updateArgsMap(DbType dbtype) {

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
        argsMap.put("--visionhost", visionHostCachedTextField.getText());
        argsMap.put("--visionport", visionPortCachedTextField.getText());
        argsMap.put("--commandport", cmdPortCachedTextField.getText());
        argsMap.put(ADD_REPEAT_COUNTS_TO_DATABASE_NAMES,
                Boolean.toString(this.addRepeatCountsToDatabaseNamesCachedCheckBox.isSelected()));
        return argsMap;
    }
    private static final String ADD_REPEAT_COUNTS_TO_DATABASE_NAMES = "AddRepeatCountsToDatabaseNames";

    private @Nullable
    Thread startVisionThread = null;
    private List<PhysicalItem> transformedVisionList = Collections.emptyList();

    @SuppressWarnings("unchecked")
    private static <T extends PhysicalItem> List<T> transformList(List<T> in, PoseType transform) {

        if (null == in) {
            throw new IllegalArgumentException("null == in");
        }
        List<T> out = new ArrayList<>();
        for (T inItem : in) {
            PoseType newPose = CRCLPosemath.multiply(transform, inItem.getPose());
            T outItem = (T) inItem.clone();
            outItem.setFromCrclPoseType(newPose);
            outItem.setEmptySlotsList(transformList(inItem.getEmptySlotsList(), transform));

            if (inItem instanceof Tray) {
                Tray inTray = (Tray) inItem;
                Tray outTray = (Tray) outItem;
                List<Slot> inTrayAbsSlotList = inTray.getAbsSlotList();
                if (null != inTrayAbsSlotList) {
                    outTray.setAbsSlotList(transformList(inTrayAbsSlotList, transform));
                }
            }
            if (inItem instanceof Slot) {
                Slot inSlot = (Slot) inItem;
                Slot outSlot = (Slot) outItem;
                outSlot.setDiameter(inSlot.getDiameter());
                outSlot.setX_OFFSET(inSlot.getX_OFFSET());
                outSlot.setY_OFFSET(inSlot.getY_OFFSET());
            }
            out.add(outItem);
        }
        return out;
    }

    @Override
    @SafeEffect
    public List<Slot> getSlotOffsets(String name, boolean ignoreEmpty) {
        if (null == dpu) {
            return Collections.emptyList();
        }
        return dpu.getSlotOffsets(name, ignoreEmpty);
    }

    public @Nullable
    List<Slot> getSlots(Tray item) {
        if (null == dpu) {
            return null;
        }
        return dpu.getSlots(item);
    }

    private volatile @Nullable
    List<XFuture<List<PhysicalItem>>> lastIsEnableDatabaseUpdateListeners = null;
    private volatile int lastIsEnableDatabaseUpdateListenersUpdateBeginCount = -1;
    private volatile int lastIsEnableDatabaseUpdateListenersUpdateEndCount = -1;

    public boolean isEnableDatabaseUpdates() {
        boolean ret;
        synchronized (singleUpdateListeners) {
            lastIsEnableDatabaseUpdateListenersUpdateBeginCount = notifySingleListenersUpdateBeginCount.get();
            lastIsEnableDatabaseUpdateListenersUpdateEndCount = notifySingleListenersUpdateBeginCount.get();
            lastIsEnableDatabaseUpdateListeners = new ArrayList<>(singleUpdateListeners);
            if (null == dpu) {
                ret = false;
            } else {
                ret = dpu.isEnableDatabaseUpdates();
            }
        }

        if (!ret && isDebug()) {
            println("lastIsEnableDatabaseUpdateListenersUpdateEndCount = " + lastIsEnableDatabaseUpdateListenersUpdateEndCount);
            println("lastIsEnableDatabaseUpdateListenersUpdateBeginCount = " + lastIsEnableDatabaseUpdateListenersUpdateBeginCount);
        }
        return ret;
    }

    private Map<String, Integer> requiredParts = Collections.emptyMap();

    /**
     * Get the value of requiredParts
     *
     * @return the value of requiredParts
     */
    public Map<String, Integer> getRequiredParts() {
        return requiredParts;
    }

    @UIEffect
    private void setRequiredPartsStringOnDisplay(String requiredPartsString) {
        if (!jTextFieldRequiredParts.getText().equals(requiredPartsString)) {
            jTextFieldRequiredParts.setText(requiredPartsString);
        }
    }

    private volatile StackTraceElement setRequiredPartsTrace @Nullable []  = null;

    /**
     * Set the value of requiredParts
     *
     * @param requiredParts new value of requiredParts
     */
    private void setRequiredParts(@Nullable Map<String, Integer> requiredParts) {
        if ((null == requiredParts || requiredParts.isEmpty())
                && (null == this.requiredParts || this.requiredParts.isEmpty())) {
            return;
        }
        setRequiredPartsTrace = Thread.currentThread().getStackTrace();
        if (null == requiredParts || requiredParts.isEmpty()) {
            this.requiredParts = Collections.emptyMap();
            String requiredPartsString = "";
            aprsSystem.runOnDispatchThread(() -> setRequiredPartsStringOnDisplay(requiredPartsString));
        } else {
            this.requiredParts = new TreeMap<>(requiredParts);
            String requiredPartsString = requiredParts.toString();
            aprsSystem.runOnDispatchThread(() -> setRequiredPartsStringOnDisplay(requiredPartsString));
        }
    }

    private final AtomicInteger checkRequiredPartFailures = new AtomicInteger();

    private int maxRequiredPartFailures = 5;

    /**
     * Get the value of maxRequiredPartFailures
     *
     * @return the value of maxRequiredPartFailures
     */
    public int getMaxRequiredPartFailures() {
        return maxRequiredPartFailures;
    }

    /**
     * Set the value of maxRequiredPartFailures
     *
     * @param maxRequiredPartFailures new value of maxRequiredPartFailures
     */
    public void setMaxRequiredPartFailures(int maxRequiredPartFailures) {
        this.maxRequiredPartFailures = maxRequiredPartFailures;
    }

    private File createTempFile(String prefix, String suffix) throws IOException {
        AprsSystem af = this.aprsSystem;
        if (null != af) {
            return af.createTempFile(prefix, suffix);
        }
        return Utils.createTempFile(prefix, suffix);
    }

    private File createTempFile(String prefix, String suffix, File dir) throws IOException {
        AprsSystem af = this.aprsSystem;
        if (null != af) {
            return af.createTempFile(prefix, suffix, dir);
        }
        return Utils.createTempFile(prefix, suffix, dir);
    }

    private void takeSimViewSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint) {
        AprsSystem af = this.aprsSystem;
        if (null != af) {
            af.takeSimViewSnapshot(f, itemsToPaint);
        }
    }

    private boolean checkRequiredParts(String listName, List<PhysicalItem> list) {
        return checkRequiredParts(listName, list, false);
    }

    private boolean checkRequiredParts(String listName, List<PhysicalItem> list, boolean rechecking) {
        List<PhysicalItem> foundRequiredParts = new ArrayList<>();
        List<PhysicalItem> partsNotRequired = new ArrayList<>(list);
        if (null != requiredParts && !requiredParts.isEmpty()) {
            for (Entry<String, Integer> entry : requiredParts.entrySet()) {
                String name = entry.getKey();
                if (name.startsWith("sku_") && name.length() > 4) {
                    name = name.substring(4);
                }
                String matchName = name;
                int required = entry.getValue();
                List<PhysicalItem> matchList
                        = list
                                .stream()
                                .filter(item -> item.getName().startsWith(matchName) || item.getName().startsWith("sku_" + matchName))
                                .collect(Collectors.toList());
                for (int i = 0; i < matchList.size() && i < required; i++) {
                    PhysicalItem matchedItem = matchList.get(i);
                    foundRequiredParts.add(matchedItem);
                    partsNotRequired.remove(matchedItem);
                }
                long found = matchList.size();
                if (required > found) {

                    List<String> namesList = list.stream().map(PhysicalItem::getName).collect(Collectors.toList());
                    if (rechecking) {
                        return false;
                    }
                    int failures = checkRequiredPartFailures.incrementAndGet();
                    String msg = "Found only " + found + " of " + name + " when " + required + " needed."
                            + " : failures = " + failures + " out of " + maxRequiredPartFailures + "_ : list.siz()=" + list.size() + ", namesList=" + namesList;
                    if (null != aprsSystem && aprsSystem.snapshotsEnabled()) {
                        try {
                            takeSimViewSnapshot(createTempFile("checkRequiredParts_" + msg, ".PNG"), list);
                        } catch (IOException ex) {
                            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
                        }
                    }
                    if (failures > maxRequiredPartFailures) {
                        System.err.println("setRequiredPartsTrace = " + traceToString(setRequiredPartsTrace));
                        System.err.println("requiredParts = " + requiredParts);
                        if (!rechecking) {
                            try {
                                aprsSystem.takeSimViewSnapshot("F.checkRequiredParts." + listName + ".foundRequiredParts", foundRequiredParts);
                                aprsSystem.takeSimViewSnapshot("F.checkRequiredParts." + listName + ".partsNotRequired", partsNotRequired);
                            } catch (Exception exception) {
                                Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", exception);
                            }
                        }
                        IllegalStateException ex = new IllegalStateException(msg);

                        notifySingleUpdateListenersExceptionally(ex);
                        setTitleErrorString(msg);
                        throw ex;
                    } else {
                        if (!rechecking) {
                            try {
                                aprsSystem.takeSimViewSnapshot("F.checkRequiredParts." + listName + ".foundRequiredParts", foundRequiredParts);
                                aprsSystem.takeSimViewSnapshot("F.checkRequiredParts." + listName + ".partsNotRequired", partsNotRequired);
                            } catch (Exception exception) {
                                Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", exception);
                            }
                        }
                        System.err.println(msg);
                        return false;
                    }
                }
            }
        }
        if (!rechecking) {
            try {
                aprsSystem.takeSimViewSnapshot("T.checkRequiredParts." + listName + ".foundRequiredParts", foundRequiredParts);
                aprsSystem.takeSimViewSnapshot("T.checkRequiredParts." + listName + ".partsNotRequired", partsNotRequired);
            } catch (Exception exception) {
                Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", exception);
            }
        }
        checkRequiredPartFailures.set(0);
        return true;
    }

    @SuppressWarnings("guieffect")
    public boolean databasesUpdatesEnabled() {
        return jCheckBoxDbUpdateEnabled.isSelected();
    }

    /**
     * Set the value of enableDatabaseUpdates
     *
     * @param enableDatabaseUpdates new value of enableDatabaseUpdates
     */
    private void setEnableDatabaseUpdates(boolean enableDatabaseUpdates) {

        synchronized (singleUpdateListeners) {
            if (!enableDatabaseUpdates && !singleUpdateListeners.isEmpty()) {
                System.out.println("");
                System.out.flush();
                System.err.println("singleUpdateListeners=" + singleUpdateListeners);
                for (XFuture<List<PhysicalItem>> sul : singleUpdateListeners) {
                    System.err.println("sul=" + sul);
                    System.err.println("sul.createTrace=" + Utils.traceToString(sul.getCreateTrace()));
                    System.err.println("sul.createThread=" + sul.getCreateThread());
                }
                System.err.println("Thread.currentThread()=" + Thread.currentThread());
                Thread.dumpStack();
                System.out.println("");
                System.out.flush();
                System.err.println("");
                System.err.flush();
                throw new IllegalStateException("attempt to disable database updates while single listners are waiting");
            }
        }
        if (null != dpu) {
            dpu.setEnableDatabaseUpdates(enableDatabaseUpdates);
            if (enableDatabaseUpdates) {
                setEnableDatabaseUpdatesTrueStacktraceArray = Thread.currentThread().getStackTrace();
                setEnableDatabaseUpdatesTrueTime = System.currentTimeMillis();
            } else {
                setEnableDatabaseUpdatesFalseStacktraceArray = Thread.currentThread().getStackTrace();
                setEnableDatabaseUpdatesFalseTime = System.currentTimeMillis();
            }
        } else if (enableDatabaseUpdates) {
            throw new IllegalStateException("Database not connected (dpu == null) ");
        }
        aprsSystem.runOnDispatchThread(() -> {
            if (enableDatabaseUpdates != jCheckBoxDbUpdateEnabled.isSelected()) {
                jCheckBoxDbUpdateEnabled.setSelected(enableDatabaseUpdates);
            }
        });
    }

    private volatile StackTraceElement setEnableDatabaseUpdatesTrueStacktraceArray @Nullable []  = null;
    private volatile StackTraceElement setEnableDatabaseUpdatesFalseStacktraceArray @Nullable []  = null;
    private volatile long setEnableDatabaseUpdatesTrueTime = 0;
    private volatile long setEnableDatabaseUpdatesFalseTime = 0;

    void setEnableDatabaseUpdates(boolean enableDatabaseUpdates, Map<String, Integer> requiredParts) {
        if (enableDatabaseUpdates || null != requiredParts) {
            this.setRequiredParts(requiredParts);
            checkRequiredPartFailures.set(0);
        }
        setEnableDatabaseUpdates(enableDatabaseUpdates);
    }

    private final ConcurrentLinkedDeque<XFuture<List<PhysicalItem>>> singleUpdateListeners
            = new ConcurrentLinkedDeque<>();

    private final ConcurrentLinkedDeque<XFuture<List<PhysicalItem>>> rawUpdateListeners
            = new ConcurrentLinkedDeque<>();

    private @Nullable
    String getDpuUrl() {
        if (null != dpu) {
            return dpu.getURL();
        }
        return null;
    }

    private volatile long lastUpdateTime = -1;

    @SafeEffect
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    private volatile long lastNotifySingleUpdateListenersTime = -1;

    @SafeEffect
    long getNotifySingleUpdateListenersTime() {
        return lastNotifySingleUpdateListenersTime;
    }

    /* Get a list of items with names and poses from the simulation.
     *
     * @return list of items as generated by the simulation
     * @throws IllegalStateException Object 2D view was not opened.
     */
    public List<PhysicalItem> getLastVisItemsData() {
        return lastVisItemsData;
    }

    public void clearVisionRequiredParts() {
        requiredParts = Collections.emptyMap();
        aprsSystem.runOnDispatchThread(() -> setRequiredPartsStringOnDisplay(""));
    }

    /**
     * Asynchronously get a list of PhysicalItems updated in one frame from the
     * vision system.
     *
     * @return future with list of items updated in the next frame from the
     * vision
     */
    @SafeEffect
    public XFuture<List<PhysicalItem>> getNewSingleUpdate() {

        XFuture<List<PhysicalItem>> ret = new XFuture<>("getNewSingleUpdate " + updateNotifyCountInfo() + " database=" + getDpuUrl() + " visionSocket=" + visionClient);
        synchronized (singleUpdateListeners) {
            singleUpdateListeners.add(ret);
        }
        return ret;
    }

    public boolean removeSingleUpdate(XFuture<List<PhysicalItem>> future) {
        synchronized (singleUpdateListeners) {
            boolean removed = singleUpdateListeners.remove(future);
            if (!removed) {
                for (CompletableFuture<?> cf : future.getAlsoCancel()) {
                    if (cf instanceof XFuture) {
                        removed = this.removeSingleUpdate((XFuture) cf);
                        if (removed) {
                            return true;
                        }
                    }
                }
            }
            return removed;
        }
    }

    /**
     * Asynchronously get a list of PhysicalItems updated in one frame from the
     * vision system.
     *
     * @return future with list of items updated in the next frame from the
     * vision
     */
    @SafeEffect
    public XFuture<List<PhysicalItem>> getRawUpdate() {

        XFuture<List<PhysicalItem>> ret = new XFuture<>("getRawUpdate " + updateNotifyCountInfo() + " database=" + getDpuUrl() + " visionSocket=" + visionClient);
        synchronized (rawUpdateListeners) {
            rawUpdateListeners.add(ret);
        }
        return ret;
    }

    /**
     * Asynchronously get a list of PhysicalItems updated in one frame from the
     * vision system.
     *
     * @return future with list of items updated in the next frame from the
     * vision
     */
    @SafeEffect
    public XFuture<List<PhysicalItem>> getSingleUpdate() {

        if (null != lastVisItemsData) {
            return XFuture.completedFuture(lastVisItemsData);
        }
        return getNewSingleUpdate();
    }

    private String updateNotifyCountInfo() {
        return notifySingleListenersUpdateBeginCount.get() + "," + notifySingleListenersUpdateEndCount.get();
    }

    private final AtomicInteger notifySingleListenersUpdateBeginCount = new AtomicInteger();
    private final AtomicInteger notifySingleListenersUpdateEndCount = new AtomicInteger();

    private String getRunName() {
        if (null != aprsSystem) {
            return aprsSystem.getRunName();
        }
        return "";
    }

    private void notifySingleUpdateListeners(List<PhysicalItem> l) {
        if (l.isEmpty()) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.WARNING, getRunName() + ": notifySingleUpdateListeners passed empty list");
        }
        for (int i = 0; i < l.size(); i++) {
            PhysicalItem item = l.get(i);
            if (item instanceof Tray) {
                Tray tray = (Tray) item;
                if (tray.getAbsSlotList().isEmpty()) {
                    throw new IllegalStateException("tray.getAbsSlotList().isEmpty()");
                }
            }
        }
        List<PhysicalItem> unmodifiableList = Collections.unmodifiableList(new ArrayList<>(l));
        this.lastVisItemsData = unmodifiableList;
        notifySingleListenersUpdateBeginCount.incrementAndGet();
        List<XFuture<List<PhysicalItem>>> listeners = copyListenersAndDisableUpdates();
        for (XFuture<List<PhysicalItem>> future : listeners) {
            future.complete(unmodifiableList);
        }
        notifySingleListenersUpdateEndCount.incrementAndGet();
        lastNotifySingleUpdateListenersTime = System.currentTimeMillis();
    }

    private void notifyRawListeners(List<PhysicalItem> l) {
        if (l.isEmpty()) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.WARNING, getRunName() + ": notifySingleUpdateListeners passed empty list");
        }
        List<PhysicalItem> unmodifiableList = Collections.unmodifiableList(new ArrayList<>(l));
        List<XFuture<List<PhysicalItem>>> listeners = copyRawListeners();
        for (XFuture<List<PhysicalItem>> future : listeners) {
            future.complete(unmodifiableList);
        }
    }

    private volatile @Nullable
    List<XFuture<List<PhysicalItem>>> lastCopyListenersAndDisableUpdatesListeners = null;

    private List<XFuture<List<PhysicalItem>>> copyListenersAndDisableUpdates() {
        List<XFuture<List<PhysicalItem>>> listeners;
        synchronized (singleUpdateListeners) {
            listeners = new ArrayList<>(singleUpdateListeners);
            singleUpdateListeners.clear();
            setEnableDatabaseUpdates(false);
            lastCopyListenersAndDisableUpdatesListeners = listeners;
        }
        return listeners;
    }

    private List<XFuture<List<PhysicalItem>>> copyRawListeners() {
        List<XFuture<List<PhysicalItem>>> listeners;
        synchronized (rawUpdateListeners) {
            listeners = new ArrayList<>(rawUpdateListeners);
            rawUpdateListeners.clear();
        }
        return listeners;
    }

    private volatile List<PhysicalItem> lastVisItemsData;

    private volatile List<PhysicalItem> lastRawVisItemsData;

    public List<PhysicalItem> getLastRawVisItemsData() {
        return lastRawVisItemsData;
    }

    private void cancelListenersAndDisableUpdates() {
        synchronized (singleUpdateListeners) {
            for (XFuture<List<PhysicalItem>> xf : singleUpdateListeners) {
                if (!xf.isDone()) {
                    Thread.dumpStack();
                    System.err.println("VisionToDBJPanel.cancelListenersAndDisableUpdates : cancelling from singleUpdateListeners xf=" + xf);
                    xf.cancelAll(true);
                }
            }
            singleUpdateListeners.clear();
            setEnableDatabaseUpdates(false);
        }
        synchronized (rawUpdateListeners) {
            for (XFuture<List<PhysicalItem>> xf : rawUpdateListeners) {
                if (!xf.isDone()) {
                    Thread.dumpStack();
                    System.err.println("VisionToDBJPanel.cancelListenersAndDisableUpdates : cancelling from  rawUpdateListeners xf=" + xf);
                    xf.cancelAll(true);
                }
            }
            rawUpdateListeners.clear();
        }
    }

    private void notifySingleUpdateListenersExceptionally(Throwable ex) {
        List<XFuture<List<PhysicalItem>>> listeners = copyListenersAndDisableUpdates();
        System.err.println("notifySingleUpdateListenersExceptionally : listeners.size()=" + listeners.size());
        for (XFuture<List<PhysicalItem>> future : listeners) {
            future.completeExceptionally(ex);
        }
    }

    private volatile boolean updating = false;

    private volatile @Nullable
    List<PhysicalItem> lastVisionClientUpdateList = null;
    private volatile @Nullable
    List<PhysicalItem> lastVisionClientUpdateListCopy = null;
    private volatile @Nullable
    String lastVisionClientUpdateLine = null;

    private final AtomicInteger visionClientUpdateCount = new AtomicInteger();
    private final AtomicInteger visionClientUpdateAquireOffCount = new AtomicInteger();
    private final AtomicInteger visionClientUpdateNoCheckRequiredPartsCount = new AtomicInteger();
    private final AtomicInteger visionClientUpdateSingleUpdateListenersEmptyCount = new AtomicInteger();

    public int getVisionClientSkippedCount() {
        if (null == visionClient) {
            return 0;
        }
        return visionClient.getSkippedLineCount();
    }

    public int getVisionClientIgnoreCount() {
        if (null == visionClient) {
            return 0;
        }
        return visionClient.getIgnoreCount();
    }

    public int getVisionClientUpdateCount() {
        return visionClientUpdateCount.get();
    }

    public int getVisionClientUpdateAquireOffCount() {
        return visionClientUpdateAquireOffCount.get();
    }

    public int getVisionClientUpdateNoCheckRequiredPartsCount() {
        return visionClientUpdateNoCheckRequiredPartsCount.get();
    }

    public int getVisionClientUpdateSingleUpdateListenersEmptyCount() {
        return visionClientUpdateSingleUpdateListenersEmptyCount.get();
    }

    @Override
    @SafeEffect
    public XFutureVoid visionClientUpdateReceived(List<PhysicalItem> visionList, String line, boolean ignored) {
        try {
            if (ignored) {
                return XFutureVoid.completedFuture();
            }
            visionClientUpdateCount.incrementAndGet();
            if (acquire == AcquireEnum.OFF) {
                visionClientUpdateAquireOffCount.incrementAndGet();
                return XFutureVoid.completedFuture();
            }
            if (acquire == AcquireEnum.ONCE) {
                acquire = AcquireEnum.OFF;
                if (null != commandReplyPrintStream) {
                    commandReplyPrintStream.println("acquire=" + acquire);
                }
            }
            lastVisionClientUpdateLine = line;
            lastVisionClientUpdateList = visionList;
            lastVisionClientUpdateListCopy = new ArrayList<>(visionList);
            if (null != dpu && null != dpu.getSqlConnection()) {
                boolean addRepeatCounts = addRepeatCountsToDatabaseNamesCachedCheckBox.isSelected();
                boolean origEnableDbUpdates = dpu.isEnableDatabaseUpdates();
                updating = true;
                PoseType transform = getTransformPose();
                List<PhysicalItem> visionListWithEmptySlots = dpu.addEmptyTraySlots(visionList);
                if (!rawUpdateListeners.isEmpty()) {
                    List<PhysicalItem> transformedRawList;
                    if (null != transform) {
                        transformedRawList = transformList(visionList, transform);
                    } else {
                        transformedRawList = new ArrayList<>(visionList);
                    }
                    notifyRawListeners(transformedRawList);
                    lastRawVisItemsData = Collections.unmodifiableList(new ArrayList<>(transformedRawList));
                }
                final boolean doRequiredPartsCheck = !singleUpdateListeners.isEmpty() || (origEnableDbUpdates && dpu.isEnableDatabaseUpdates());

                if (doRequiredPartsCheck) {
                    if (!checkRequiredParts("visionList", visionList)) {
                        boolean chkAgain = checkRequiredParts("visionList", visionList, true);
                        chkAgain = checkRequiredParts("visionList", visionList, true);
                        System.err.println("checkRequiredPart(" + visionList + ") false");
                        visionClientUpdateNoCheckRequiredPartsCount.incrementAndGet();
                        return XFutureVoid.completedFuture();
                    }
                }
                dpu.setDisplayInterface(this);
                if (doRequiredPartsCheck) {
                    if (!checkRequiredParts("visionListWithEmptySlots", visionListWithEmptySlots)) {
                        System.err.println("dpu.getUpdateResultsMap()=" + dpu.getUpdateResultsMap());
                        System.err.println("checkRequiredPart(" + visionListWithEmptySlots + ") false but checkRequiredParts(" + visionList + ") true");
                        visionClientUpdateNoCheckRequiredPartsCount.incrementAndGet();
                        return XFutureVoid.completedFuture();
                    }
                }
                if (null != transform) {
                    transformedVisionList = transformList(visionListWithEmptySlots, transform);
                    if (doRequiredPartsCheck) {
                        if (!checkRequiredParts("transformedVisionList", transformedVisionList)) {
                            System.err.println("dpu.getUpdateResultsMap()=" + dpu.getUpdateResultsMap());
                            System.err.println("checkRequiredPart(" + transformedVisionList + ") false but checkRequiredParts(" + visionList + ") true");
                            visionClientUpdateNoCheckRequiredPartsCount.incrementAndGet();
                            return XFutureVoid.completedFuture();
                        }
                    }
                    List<PhysicalItem> l = dpu.updateVisionList(transformedVisionList, addRepeatCounts, false);
                    List<PhysicalItem> trays
                            = l.stream()
                                    .filter((PhysicalItem item) -> item.getType().equals("KT") || item.getType().equals("PT"))
                                    .collect(Collectors.toList());
                    List<PhysicalItem> overlappingTrays
                            = aprsSystem.filterNonOverLapping(trays);
                    if (!overlappingTrays.isEmpty()) {
                        aprsSystem.takeSimViewSnapshot("overlappingTrays", overlappingTrays);
                        return XFutureVoid.completedFuture();
                    }
                    if (!singleUpdateListeners.isEmpty()) {
                        notifySingleUpdateListeners(l);
                    } else {
                        visionClientUpdateSingleUpdateListenersEmptyCount.incrementAndGet();
                    }
                    lastVisItemsData = Collections.unmodifiableList(new ArrayList<>(l));
                    aprsSystem.runOnDispatchThread(() -> this.updateInfoOnDisplay(l, line));
                } else {
                    List<PhysicalItem> l = dpu.updateVisionList(visionListWithEmptySlots, addRepeatCounts, false);
                    List<PhysicalItem> trays
                            = l.stream()
                                    .filter((PhysicalItem item) -> item.getType().equals("KT") || item.getType().equals("PT"))
                                    .collect(Collectors.toList());
                    List<PhysicalItem> overlappingTrays
                            = aprsSystem.filterNonOverLapping(trays);
                    if (!overlappingTrays.isEmpty()) {
                        aprsSystem.takeSimViewSnapshot("overlappingTrays", overlappingTrays);
                        return XFutureVoid.completedFuture();
                    }
                    notifySingleUpdateListeners(l);
                    lastVisItemsData = Collections.unmodifiableList(new ArrayList<>(l));
                    aprsSystem.runOnDispatchThread(() -> this.updateInfoOnDisplay(l, line));
                }
            }
        } catch (Exception exception) {
            println("line = " + line);
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", exception);
            notifySingleUpdateListenersExceptionally(exception);
            setTitleErrorString(exception.toString());
            if (null != aprsSystem) {
                aprsSystem.showException(exception);
            }
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else {
                throw new RuntimeException(exception);
            }
        } finally {
            lastUpdateTime = System.currentTimeMillis();
            updating = false;
        }
        return XFutureVoid.completedFuture();
    }

    private XFutureVoid startVisionInternal(Map<String, String> argsMap) {
        closeVision();
        VisionSocketClient visionClientLocal = this.visionClient;
        if (null == visionClientLocal) {
            visionClientLocal = new VisionSocketClient();
            this.visionClient = visionClientLocal;
        }
        visionClientLocal.setDisplayInterface(this);
        visionClientLocal.setDebug(isDebug());
        visionClientLocal.setReplyPs(System.out);
        visionClientLocal.addListener(this);
        for (Consumer<Integer> l : this.incrementCountListeners) {
            visionClientLocal.addCountListener(l);
        }
        return visionClientLocal.start(argsMap)
                .thenComposeToVoid(() -> aprsSystem.runOnDispatchThread(this::finishConnectVision));
    }

    private boolean ignoreLosingItemsLists = false;

    /**
     * Get the value of ignoreLosingItemsLists
     *
     * @return the value of ignoreLosingItemsLists
     */
    public boolean isIgnoreLosingItemsLists() {
        if (null != visionClient) {
            boolean ret = visionClient.isIgnoreLosingItemsLists();
            this.ignoreLosingItemsLists = ret;
            return ret;
        }
        return ignoreLosingItemsLists;
    }

    /**
     * Set the value of ignoreLosingItemsLists
     *
     * @param ignoreLosingItemsLists new value of ignoreLosingItemsLists
     */
    public void setIgnoreLosingItemsLists(boolean ignoreLosingItemsLists) {
        if (null != visionClient) {
            visionClient.setIgnoreLosingItemsLists(ignoreLosingItemsLists);
        }
        this.ignoreLosingItemsLists = ignoreLosingItemsLists;
    }

    public XFutureVoid connectVision() {
        String visionPortString = visionPortCachedTextField.getText();
        println("Starting connectVision ... (port = " + visionPortString + ") ");
        XFutureVoid ret = new XFutureVoid("connectVision" + visionPortString);
        Thread connectThread = Thread.currentThread();
        StackTraceElement connectTrace[] = connectThread.getStackTrace();
        try {

            argsMap.put("--visionhost", visionHostCachedTextField.getText());
            argsMap.put("--visionport", visionPortString);
            argsMap.put("ignoreLosingItemsLists", Boolean.toString(ignoreLosingItemsLists));
            stopVisionStartThread();
            startVisionThread = new Thread(() -> {
                try {
                    startVisionInternal(argsMap)
                            .thenRun(() -> ret.complete());
                } catch (Exception e) {
                    System.err.println("connectThread = " + connectThread);
                    System.err.println("connectTrace = " + Arrays.toString(connectTrace));
                    System.err.println("argsMap = " + argsMap);
                    println("aprsSystemInterface = " + aprsSystem);
                    Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", e);
                    ret.completeExceptionally(e);
                    throw new RuntimeException(e);
                }
            }, "startVisionThread");
            startVisionThread.setDaemon(true);
            startVisionThread.start();
        } catch (Exception exception) {
            addLogMessage(exception);
            throw new RuntimeException(exception);
        }
        return ret;
    }

    private final AprsSystem aprsSystem;

    /**
     * Get the value of aprsSystemInterface
     *
     * @return the value of aprsSystemInterface
     */
    public AprsSystem getAprsSystem() {
        return aprsSystem;
    }

    private void finishConnectVision() {
        if (null != visionClient) {
            visionClient.setDebug(isDebug());
//            visionClient.setAddRepeatCountsToDatabaseNames(this.jCheckBoxAddRepeatCountsToDatabaseNames.isSelected());
            setVisionConnected(visionClient.isConnected());
        }
        updateTransformFromTable();
        if (null != aprsSystem && null != visionClient) {
            aprsSystem.setShowVisionConnected(visionClient.isConnected());
        }
    }

    @UIEffect
    private void jButtonConnectVisionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonConnectVisionActionPerformed
        connectVision();
    }//GEN-LAST:event_jButtonConnectVisionActionPerformed

    private void closeDB() {
        try {
            if (null != dpu) {
                dpu.close();
                aprsSystem.runOnDispatchThread(() -> setDBConnectedOnDisplay(false));
            } else if (lastSetDbConnectedVal) {
                aprsSystem.runOnDispatchThread(() -> setDBConnectedOnDisplay(false));
            }
        } catch (Exception ex) {
            addLogMessage(ex);
        }
    }

    @UIEffect
    private void jTextFieldVisionHostActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldVisionHostActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldVisionHostActionPerformed

    private @MonotonicNonNull
    SocketLineReader commandSlr = null;

    private void closeCommand() {
        if (null != commandSlr) {
            commandSlr.close();
        }
        aprsSystem.runOnDispatchThread(() -> setCommandConnectedOnDisplay(false));
    }

    void startCommand(Map<String, String> argsMap) {
        try {
            closeCommand();
            String cmdPortString = argsMap.get("--commandport");
            if (null == cmdPortString) {
                throw new IllegalArgumentException("argsMap.get(\"--commandport\") returned null for argsMap=" + argsMap);
            }
            short port = Short.parseShort(cmdPortString);
            commandSlr = SocketLineReader.startServer(
                    port,
                    "commandReader",
                    this::handleCommand);
            aprsSystem.runOnDispatchThread(() -> setCommandConnectedOnDisplay(true));
        } catch (Exception exception) {
            System.err.println(exception.getLocalizedMessage());
            System.err.println("Starting server for command port failed.");
        }
    }

    private volatile AcquireEnum acquire = AcquireEnum.ON;

    private AcquireEnum getAcquire() {
        return acquire;
    }

    private void setAcquire(AcquireEnum acquire) {
        this.acquire = acquire;
    }

    private volatile @MonotonicNonNull
    PrintStream commandReplyPrintStream = null;

    private void handleCommand(String line, PrintStream os) {
        aprsSystem.runOnDispatchThread(() -> setLastCommand(line));
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
            aprsSystem.runOnDispatchThread(() -> setAquiring(AcquireEnum.ON.toString()));

            os.println("Acquire Status: " + getAcquire());
        } else if (fa[0].trim().toUpperCase().compareTo("ONCE") == 0) {
            setAcquire(AcquireEnum.ONCE);
            aprsSystem.runOnDispatchThread(() -> setAquiring(AcquireEnum.ONCE.toString()));
            os.println("Acquire Status: " + AcquireEnum.ONCE);
        } else if (fa[0].trim().toUpperCase().compareTo("OFF") == 0) {
            setAcquire(AcquireEnum.OFF);
            aprsSystem.runOnDispatchThread(() -> setAquiring(AcquireEnum.OFF.toString()));
            os.println("Acquire Status: " + AcquireEnum.OFF);
            commandReplyPrintStream = os;
            if (null != visionClient) {
                visionClient.setReplyPs(commandReplyPrintStream);
            }
        } else {
            os.println("Not recognized: " + line);
        }
    }

    @UIEffect
    private void jTextFieldCmdPortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldCmdPortActionPerformed
        try {
            closeCommand();
            argsMap.put("--commandport", this.jTextFieldCmdPort.getText());
            startCommand(argsMap);
        } catch (Exception exception) {
            addLogMessage(exception);
        }
    }//GEN-LAST:event_jTextFieldCmdPortActionPerformed

    @Nullable
    @UIEffect
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

    @UIEffect
    private void jTextFieldAcquireActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldAcquireActionPerformed
        setAcquire(AcquireEnum.valueOf(this.jTextFieldAcquire.getText()));
    }//GEN-LAST:event_jTextFieldAcquireActionPerformed

    @UIEffect
    private void jButtonCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCheckActionPerformed
        try {
            DefaultTableModel tm = (DefaultTableModel) this.jTableFromDatabase.getModel();
            tm.setRowCount(0);
            queryDatabase();
        } catch (Exception ex) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jButtonCheckActionPerformed

    private XFutureVoid queryDatabase() {
        if (null != dpu) {
            return dpu.queryDatabase()
                    .thenComposeToVoid("VisionToDB.queryDatabase.updataPoseQueryInfo",
                            l -> aprsSystem.runOnDispatchThread(() -> updataPoseQueryInfo(l)));
        }
        return XFutureVoid.completedFutureWithName("queryDatabase.null==dpu");
    }

    private XFutureVoid startQueryDatabaseNew() {
        if (null != dpu) {
            return dpu.queryDatabaseNew()
                    .thenComposeToVoid("VisionToDB.startQueryDatabaseNew.updataPoseQueryInfo",
                            l -> aprsSystem.runOnDispatchThread(() -> updataPoseQueryInfo(l)));
        }
        return XFutureVoid.completedFutureWithName("startQueryDatabaseNew.null==dpu");
    }

    @UIEffect
    private void jButtonAddItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddItemActionPerformed
        try {
            if (null == dpu) {
                throw new IllegalStateException("dpu == null");
            }
            Window parentWindow = getParentWindow();
            Map<String, Object> map
                    = DetectedItemJPanel.showDetectedItemDialog(parentWindow,
                            "Add Item with Pose to database",
                            Dialog.ModalityType.APPLICATION_MODAL,
                            null);
            if (isDebug()) {
                addLogMessage("Detected item to add map = " + map);
            }
            PhysicalItem item = DetectedItemJPanel.mapToItem(map, null);
            if (isDebug()) {
                addLogMessage("Detected item to add = " + item);
            }
            List<PhysicalItem> singletonList = Collections.singletonList(item);
            PoseType pose = getTransformPose();
            if (null != pose) {
                List<PhysicalItem> transformedList = transformList(singletonList, pose);
                println("transformedList = " + transformedList);
                dpu.updateVisionList(transformedList, addRepeatCountsToDatabaseNamesCachedCheckBox.isSelected(), true);
            } else {
                dpu.updateVisionList(singletonList, addRepeatCountsToDatabaseNamesCachedCheckBox.isSelected(), true);
            }
            this.queryDatabase();
        } catch (Exception ex) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
            MultiLineStringJPanel.showText(ex.toString());
        }
    }//GEN-LAST:event_jButtonAddItemActionPerformed

    private @MonotonicNonNull
    Callable<DbSetupPublisher> dbSetupSupplier = null;

    public @Nullable
    Callable<DbSetupPublisher> getDbSetupSupplier() {
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
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    @UIEffect
    private void jCheckBoxDebugActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxDebugActionPerformed
        if (null != visionClient) {
            visionClient.setDebug(isDebug());
        }
    }//GEN-LAST:event_jCheckBoxDebugActionPerformed

    @UIEffect
    private void jButtonUpdateResultDetailsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonUpdateResultDetailsActionPerformed
        int index = jTableUpdateResults.getSelectedRow();
        if (index >= 0) {
            String name = (String) jTableUpdateResults.getValueAt(index, 0);
            UpdateResults ur = resultsMap.get(name);
            if (null == ur) {
                MultiLineStringJPanel.showText("no results for " + name);
                return;
            }
            String value = ur.toString();
            MultiLineStringJPanel.showText("Latest update attempt for " + name + ":\r\n" + value);
        }
    }//GEN-LAST:event_jButtonUpdateResultDetailsActionPerformed

    private volatile @Nullable
    JPopupMenu popMenu = null;

    @UIEffect
    private void copyText() {
        JTextArea area = jTextAreaLog;
        if (area != null) {
            area.getTransferHandler().exportToClipboard(area,
                    Toolkit.getDefaultToolkit().getSystemClipboard(),
                    TransferHandler.COPY);
        }
        if (null != popMenu) {
            popMenu.setVisible(false);
        }
    }

    @UIEffect
    private void showPopup(Component comp, int x, int y) {
        JPopupMenu newPopMenu = popMenu;
        if (newPopMenu == null) {
            newPopMenu = new JPopupMenu();
            JMenuItem copyMenuItem = new JMenuItem("Copy");
            copyMenuItem.addActionListener(e -> copyText());
            newPopMenu.add(copyMenuItem);
            popMenu = newPopMenu;
        }
        newPopMenu.show(comp, x, y);
    }

    @UIEffect
    private void jTextAreaLogMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTextAreaLogMouseClicked
        if (evt.isPopupTrigger()) {
            showPopup(evt.getComponent(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_jTextAreaLogMouseClicked

    @UIEffect
    private void jTextAreaLogMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTextAreaLogMousePressed
        if (evt.isPopupTrigger()) {
            showPopup(evt.getComponent(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_jTextAreaLogMousePressed

    @UIEffect
    private void jTextAreaLogMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTextAreaLogMouseReleased
        if (evt.isPopupTrigger()) {
            showPopup(evt.getComponent(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_jTextAreaLogMouseReleased

    @UIEffect
    private void jCheckBoxVerifyUpdatesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxVerifyUpdatesActionPerformed
        if (null != dpu) {
            dpu.setVerify(jCheckBoxVerifyUpdates.isSelected());
        }
    }//GEN-LAST:event_jCheckBoxVerifyUpdatesActionPerformed

    @UIEffect
    private void jButtonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeleteActionPerformed
        try {
            if (null == dpu) {
                return;
            }
            for (int i : jTableFromDatabase.getSelectedRows()) {
                String name = (String) jTableFromDatabase.getModel().getValueAt(i, 0);
                try {
                    if (name != null && name.length() > 0) {
                        dpu.deletePose(name);
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
                    appendLogDisplay("\nDelete " + name + " failed :" + ex + "\n");
                }
            }
            jTableFromDatabase.getSelectionModel().clearSelection();
            DefaultTableModel tm = (DefaultTableModel) this.jTableFromDatabase.getModel();
            tm.setRowCount(0);
            fromDatabaseCachedTable.setRowCount(0);
            queryDatabase();
        } catch (Exception ex) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jButtonDeleteActionPerformed

    @UIEffect
    private void jButtonForceSingleUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonForceSingleUpdateActionPerformed
        int row = jTableFromVision.getSelectedRow();
        if (row >= 0) {
            forceUpdateSingle(row);
        }
    }//GEN-LAST:event_jButtonForceSingleUpdateActionPerformed

    @UIEffect
    private void jButtonForceAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonForceAllActionPerformed
        forceAllUpdates();
    }//GEN-LAST:event_jButtonForceAllActionPerformed

    @UIEffect
    private void toCsv(String name, JTable table) throws IOException {
        File f = createTempFile(name, ".csv");
        Utils.saveJTable(f, table);
//        saveTableToFile(f, table);
        Desktop.getDesktop().open(f);
    }

    @UIEffect
    private void jButtonCsvActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCsvActionPerformed
        try {
            toCsv("FromVision", this.jTableFromVision);
        } catch (IOException ex) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jButtonCsvActionPerformed

    @UIEffect
    private void jButtonCsvFromDatabaseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCsvFromDatabaseActionPerformed
        try {
            toCsv("FromDatabase", this.jTableFromDatabase);
        } catch (IOException ex) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jButtonCsvFromDatabaseActionPerformed

    @UIEffect
    private void jTextFieldRotationOffsetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldRotationOffsetActionPerformed
        double ro = Math.toRadians(Double.parseDouble(jTextFieldRotationOffset.getText()));
        if (null != dpu) {
            dpu.setRotationOffset(ro);
        }
    }//GEN-LAST:event_jTextFieldRotationOffsetActionPerformed

    @UIEffect
    private void jCheckBoxForceUpdatesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxForceUpdatesActionPerformed
        if (null != dpu) {
            dpu.setForceUpdates(jCheckBoxForceUpdates.isSelected());
        }
    }//GEN-LAST:event_jCheckBoxForceUpdatesActionPerformed

    private void showDatabaseTableImage() {
        try {
            File f = createTempFile("newDataBaseItems_", ".png");
            takeSnapshot(f);
            Desktop.getDesktop().open(f);
        } catch (IOException ex) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    private void takeSnapshot(File f) {
        try {
            if (null == dpu) {
                System.err.println("Can't take snapshot(" + f + ") when database not connected.");
                return;
            }
            List<PhysicalItem> list = new ArrayList<>();
            for (int i = 0; i < fromDatabaseCachedTable.getRowCount(); i++) {
                Object tableValue0 = fromDatabaseCachedTable.getValueAt(i, 0);
                if (null == tableValue0) {
                    continue;
                }
                Object tableValue1 = fromDatabaseCachedTable.getValueAt(i, 1);
                if (null == tableValue1) {
                    continue;
                }
                Object tableValue2 = fromDatabaseCachedTable.getValueAt(i, 2);
                if (null == tableValue2) {
                    continue;
                }
                Object tableValue4 = fromDatabaseCachedTable.getValueAt(i, 4);
                if (null == tableValue4) {
                    continue;
                }
                list.add(PhysicalItem.newPhysicalItemNameRotXY(tableValue0.toString(),
                        Double.parseDouble(tableValue4.toString()),
                        Double.parseDouble(tableValue1.toString()),
                        Double.parseDouble(tableValue2.toString())));
            }
            if (list.isEmpty()) {
                System.err.println("takeSnapshot(" + f + ") called when table is empty");
                return;
            }
            File dbLogDir = new File(f.getParentFile(), "db_log_dir");
            dbLogDir.mkdirs();
            String csvFnameBase = f.getName();
            File csvFile = createTempFile(csvFnameBase + "_db", ".csv", dbLogDir);
            Utils.saveCachedTable(csvFile, fromDatabaseCachedTable);
            saveLastEnabledUpdateCsv(f);
            takeSimViewSnapshot(f, list);
        } catch (IOException ex) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    private void saveLastEnabledUpdateCsv(File f) {
        if (null == dpu) {
            return;
        }
        List<PhysicalItem> lastInput = dpu.getLastEnabledUpdateList();
        if (null != lastInput && !lastInput.isEmpty()) {
            try {
                File dbInputLogDir = new File(f.getParentFile(), "visionToDb_input_dir");
                dbInputLogDir.mkdirs();
                File csvInputFile = Utils.createTempFile(f.getName() + "_visiontToDb", ".csv", dbInputLogDir);
                try (PrintWriter pw = new PrintWriter(new FileWriter(csvInputFile))) {
                    pw.println("name,rotation,x,y,score,type,visioncycle,repeats,fullname");
                    for (PhysicalItem item : lastInput) {
                        pw.println(item.getName() + "," + item.getRotation() + "," + item.x + "," + item.y + "," + item.getScore() + "," + item.getType() + "," + item.getVisioncycle() + "," + item.getRepeats() + "," + item.getFullName());
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
    }

    private void checkPhysicalItemCollectionNames(Collection<? extends PhysicalItem> itemsToPaint) throws RuntimeException {
        for (PhysicalItem pi : itemsToPaint) {
            if (pi.getName().contains("_in_pt_in_pt")) {
                throw new RuntimeException("bad name for item in collection : " + pi);
            }
            if (pi.getFullName().contains("_in_pt_in_pt")) {
                throw new RuntimeException("bad name for item in collection : " + pi);
            }
            if (pi.getName().contains("_in_kt_in_kt")) {
                throw new RuntimeException("bad name for item in collection : " + pi);
            }
            if (pi.getFullName().contains("_in_kt_in_kt")) {
                throw new RuntimeException("bad name for item in collection : " + pi);
            }
        }
    }

    private void checkPoseQueryElemCollectionNames(Collection<? extends PoseQueryElem> itemsToPaint) throws RuntimeException {
        for (PoseQueryElem pi : itemsToPaint) {
            if (pi.getName().contains("_in_pt_in_pt")) {
                throw new RuntimeException("bad name for item in collection : " + pi);
            }
            if (pi.getName().contains("_in_kt_in_kt")) {
                throw new RuntimeException("bad name for item in collection : " + pi);
            }
        }
    }

    private List<PhysicalItem> poseQueryToPhysicalItemList(List<PoseQueryElem> listIn) {
        checkPoseQueryElemCollectionNames(listIn);
        List<PhysicalItem> listOut = new ArrayList<>();
        for (PoseQueryElem pqe : listIn) {
            listOut.add(PhysicalItem.newPhysicalItemNameRotXY(pqe.getName(),
                    pqe.getRot(),
                    pqe.getX(),
                    pqe.getY()));
        }
        checkPhysicalItemCollectionNames(listOut);
        return listOut;
    }

    XFutureVoid startNewItemsImageSave(File f) {
        if (null == dpu) {
            throw new NullPointerException("null == dpu");
        }
        return dpu.queryDatabaseNew()
                .thenApply(this::poseQueryToPhysicalItemList)
                .thenAccept(l -> {
                    takeSimViewSnapshot(f, l);
                    saveLastEnabledUpdateCsv(f);
                });
    }

    public XFutureVoid startTakeSnapshot(File f) {
        return aprsSystem.runOnDispatchThread(() -> takeSnapshot(f));
    }

    @UIEffect
    private void jButtonCheckNewItemsOnlyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCheckNewItemsOnlyActionPerformed
        startNewItemsCheck();
    }//GEN-LAST:event_jButtonCheckNewItemsOnlyActionPerformed

    private XFutureVoid startNewItemsCheck() {
        fromDatabaseCachedTable.setRowCount(0);
        return startQueryDatabaseNew();
    }

    @UIEffect
    private void jButtonShowImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonShowImageActionPerformed
        if (null != aprsSystem) {
            boolean snapShotsEnabled = aprsSystem.isSnapshotsSelected();
            if (!snapShotsEnabled) {
                aprsSystem.setSnapshotsSelected(true);
            }
            showDatabaseTableImage();
            if (!snapShotsEnabled) {
                aprsSystem.setSnapshotsSelected(false);
            }
        }
    }//GEN-LAST:event_jButtonShowImageActionPerformed

    @UIEffect
    private void jCheckBoxDbUpdateEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxDbUpdateEnabledActionPerformed
        this.setEnableDatabaseUpdates(jCheckBoxDbUpdateEnabled.isSelected());
    }//GEN-LAST:event_jCheckBoxDbUpdateEnabledActionPerformed

    @UIEffect
    private void jMenuItemSavePropertiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSavePropertiesActionPerformed
        this.saveProperties();
    }//GEN-LAST:event_jMenuItemSavePropertiesActionPerformed

    @UIEffect
    private void jMenuItemReloadPropertiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemReloadPropertiesActionPerformed
        this.loadProperties();
    }//GEN-LAST:event_jMenuItemReloadPropertiesActionPerformed

    @UIEffect
    private void jMenuItemSetPropertiesFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSetPropertiesFileActionPerformed
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            this.setPropertiesFile(chooser.getSelectedFile());
        }
    }//GEN-LAST:event_jMenuItemSetPropertiesFileActionPerformed

    private final CachedTable fromVisionCachedTable;

    private void forceAllUpdates() throws NumberFormatException {
        try {
            fromDatabaseCachedTable.setRowCount(0);
            for (int i = 0; i < fromVisionCachedTable.getRowCount(); i++) {
                forceUpdateSingle(i);
            }
            queryDatabase();
        } catch (Exception ex) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    private void forceUpdateSingle(int row) throws NumberFormatException {
        if (null == dpu) {
            return;
        }
        String name = (String) fromVisionCachedTable.getValueAt(row, 1);
        if (null == name) {
            return;
        }
        Object tableValue3 = fromVisionCachedTable.getValueAt(row, 3);
        if (null == tableValue3) {
            return;
        }
        Object tableValue4 = fromVisionCachedTable.getValueAt(row, 4);
        if (null == tableValue4) {
            return;
        }
        Object tableValue5 = fromVisionCachedTable.getValueAt(row, 5);
        if (null == tableValue5) {
            return;
        }
        Object tableValue6 = fromVisionCachedTable.getValueAt(row, 6);
        if (null == tableValue6) {
            return;
        }
        Object tableValue7 = fromVisionCachedTable.getValueAt(row, 7);
        if (null == tableValue7) {
            return;
        }
        Object tableValue9 = fromVisionCachedTable.getValueAt(row, 9);
        if (null == tableValue9) {
            return;
        }
        Object tableValue10 = fromVisionCachedTable.getValueAt(row, 10);
        if (null == tableValue10) {
            return;
        }
        Object tableValue11 = fromVisionCachedTable.getValueAt(row, 11);
        if (null == tableValue11) {
            return;
        }
        Object tableValue12 = fromVisionCachedTable.getValueAt(row, 12);
        if (null == tableValue12) {
            return;
        }
        double rot = Double.parseDouble(tableValue3.toString());
        double x = Double.parseDouble(tableValue4.toString());
        double y = Double.parseDouble(tableValue5.toString());
        double z = Double.parseDouble(tableValue6.toString());
        double score = Double.parseDouble(tableValue7.toString());
        String type = (String) fromVisionCachedTable.getValueAt(row, 8);
        if (null == type) {
            return;
        }
        PhysicalItem item = PhysicalItem.newPhysicalItemNameRotXYScoreType(name, rot, x, y, score, type);
        item.z = z;
        item.setInsidePartsTray((Boolean) tableValue9);
        item.setInsideKitTray((Boolean) tableValue10);
        item.setFullName((String) tableValue11);
        item.setVisioncycle(Integer.parseInt(tableValue12.toString()));
        List<PhysicalItem> singletonList = Collections.singletonList(item);
        boolean origForceUpdates = dpu.isForceUpdates();
        dpu.setForceUpdates(true);
        boolean isDebug = debugCachedCheckBox.isSelected();
        debugCachedCheckBox.setSelected(true);
        dpu.updateVisionList(singletonList, addRepeatCountsToDatabaseNamesCachedCheckBox.isSelected(), true);
        dpu.setForceUpdates(origForceUpdates);
        debugCachedCheckBox.setSelected(isDebug);
    }

    private @Nullable
    DbType oldDbType;


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
    private javax.swing.JCheckBox jCheckBoxDbUpdateEnabled;
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
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelCommnandStatus;
    private javax.swing.JLabel jLabelDatabaseStatus;
    private javax.swing.JLabel jLabelVisionStatus;
    final javax.swing.JMenu jMenuFile = new javax.swing.JMenu();
    private javax.swing.JMenuItem jMenuItemReloadProperties;
    private javax.swing.JMenuItem jMenuItemSaveProperties;
    private javax.swing.JMenuItem jMenuItemSetPropertiesFile;
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
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
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
    private javax.swing.JTextField jTextFieldRequiredParts;
    private javax.swing.JTextField jTextFieldRotationOffset;
    private javax.swing.JTextField jTextFieldVisionHost;
    private javax.swing.JTextField jTextFieldVisionPort;
    // End of variables declaration//GEN-END:variables

    public void dispose() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private @MonotonicNonNull
    File propertiesFile = null;

    public void setPropertiesFile(File f) {
        propertiesFile = f;
    }

    public @Nullable
    File getPropertiesFile() {
        return propertiesFile;
    }
    private volatile boolean savingProperties = false;

    public void saveProperties() {
        DbSetup setup = dbSetupPublisher.getDbSetup();
        saveProperties(setup.getDbType(), setup.getHost(), setup.getPort());
    }

    private void saveProperties(DbType dbtype, String host, int port) {
        try {
            savingProperties = true;
            if (null == propertiesFile) {
                throw new IllegalStateException("null == propertiesFile");
            }
            File parentFile = propertiesFile.getParentFile();
            if (null != parentFile) {
                parentFile.mkdirs();
            }
            Properties props = new Properties();
            if (propertiesFile.exists()) {
                try (FileReader fr = new FileReader(propertiesFile)) {
                    props.load(fr);
                } catch (IOException ex) {
                    Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
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
                    Boolean.toString(addRepeatCountsToDatabaseNamesCachedCheckBox.isSelected()));
            props.put("--visionport", visionPortCachedTextField.getText());
            props.put("--visionhost", visionHostCachedTextField.getText());
            props.put("rotationOffset", rotationOffsetCachedTextField.getText());
            props.put("--commandport", cmdPortCachedTextField.getText());
//            try (FileWriter fw = new FileWriter(propertiesFile)) {
//                props.store(fw, "");
//            } catch (IOException ex) {
//                Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
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

    private void restoreProperties(DbType dbtype) {
        try {
            restoringProperties = true;
            if (null != propertiesFile && propertiesFile.exists()) {
                Properties props = new Properties();
                try (FileReader fr = new FileReader(propertiesFile)) {
                    props.load(fr);
                } catch (IOException ex) {
                    Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
                }
                for (String propName : props.stringPropertyNames()) {
                    String value = props.getProperty(propName);
                    if (null != value) {
                        argsMap.put(propName, value);
                    } else {
                        argsMap.remove(propName);
                    }
                }
                updateFromArgs(argsMap);
            }
        } catch (Exception e) {
            addLogMessage(e);
        } finally {
            restoringProperties = false;
        }
    }

    public final void loadProperties() {
        try {
            restoringProperties = true;
            if (null != propertiesFile && propertiesFile.exists()) {
                Properties props = new Properties();
                try (FileReader fr = new FileReader(propertiesFile)) {
                    props.load(fr);
                } catch (IOException ex) {
                    Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
                }
                for (String propName : props.stringPropertyNames()) {
                    String value = props.getProperty(propName);
                    if (null != value) {
                        argsMap.put(propName, value);
                    } else {
                        argsMap.remove(propName);
                    }
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
    @Nullable
    @SafeEffect
    public Connection getSqlConnection() {
        if (null == dpu) {
            return null;
        }
        return dpu.getSqlConnection();
    }

    private void closeDatabasePoseUpdater() {
        if (null != dpu) {
            dpu.close();
        }
    }

    private final CachedCheckBox verifyUpdatesCachedCheckBox;

    @Override
    @SafeEffect
    public void setSqlConnection(Connection connection, DbType dbtype) {
        try {
            closeDatabasePoseUpdater();
            dpu = new DatabasePoseUpdater(connection, dbtype, true,
                    dbSetupPublisher.getDbSetup());
            dpu.setVerify(verifyUpdatesCachedCheckBox.isSelected());
            dbSetupPublisher.setDbSetup(new DbSetupBuilder().setup(dbSetupPublisher.getDbSetup()).type(dbtype).build());
        } catch (Exception ex) {
            Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
            closeDatabasePoseUpdater();
        }
    }

    @Override
    @SafeEffect
    public DbType getDbType() {
        if (null != dpu) {
            DbType dupDbType = dpu.getDbType();
            DbSetup curSetup = dbSetupPublisher.getDbSetup();
            if (dupDbType != curSetup.getDbType()) {
                try {
                    this.oldDbType = null;
                    dbSetupPublisher.setDbSetup(new DbSetupBuilder().setup(curSetup).type(dupDbType).build());
                } catch (Exception ex) {
                    Logger.getLogger(VisionToDBJPanel.class.getName()).log(Level.SEVERE, "", ex);
                }
            }
            return dupDbType;
        }
        return dbSetupPublisher.getDbSetup().getDbType();
    }

    private @Nullable
    DbSetup lastSetup = null;

    @Override
    @SafeEffect
    public void accept(DbSetup setup) {
        if (setup.isConnected()) {
            if (lastSetup == null || !lastSetup.isConnected() || !checkConnected()) {
                connectDB(setup);
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
                    restoreProperties(newDbType);
                    oldDbType = newDbType;
                }
            }
        } catch (Exception exception) {
            addLogMessage(exception);
        }
    }
    private Map<String, UpdateResults> resultsMap;

    private final CachedTable updateResultsCachedTable;

    private void updateResultsMapInternal(Map<String, UpdateResults> _map) {
        updateResultsCachedTable.setRowCount(0);
        for (Entry<String, UpdateResults> entry : _map.entrySet()) {
            updateResultsCachedTable.addRow(new Object[]{
                entry.getKey(),
                entry.getValue().isVerified(),
                ((entry.getValue().getException() != null) || (entry.getValue().getVerifyException() != null)),
                entry.getValue().getTotalUpdateCount(),});
        }
        resultsMap = _map;
        updatePerformanceLine();
    }

    private volatile @MonotonicNonNull
    String performanceLine = null;

    public @Nullable
    String getPerformanceLine() {
        return performanceLine;
    }

    public void updatePerformanceLine() {
        String dbPerf = "DB: NA";
        if (null != dpu && dpu.getTotalListUpdates() > 0) {
            dbPerf = "DB: Avg="
                    + (dpu.getTotalUpdateTimeMillis() / dpu.getTotalListUpdates())
                    + " ms, max=" + dpu.getMaxUpdateTimeMillis() + " ms";
        }
        String clntPerf = " Input : NA";
        if (null != visionClient) {
            clntPerf = " Input : "
                    + "count=" + visionClient.getLineCount() + ", "
                    + "skipped=" + visionClient.getSkippedLineCount() + ", "
                    + "ignored=" + visionClient.getIgnoreCount();
        }
        String pl = dbPerf + clntPerf;
        this.performanceLine = pl;
        this.jTextFieldPerformance.setText(pl);
    }

    @Override
    @SafeEffect
    public void updateResultsMap(Map<String, UpdateResults> _map) {
        Map<String, UpdateResults> mapCopy = new HashMap<>(_map);
        aprsSystem.runOnDispatchThread(() -> this.updateResultsMapInternal(Collections.unmodifiableMap(mapCopy)));
    }

}
