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
package aprs.framework.pddl.executor;

import aprs.framework.AprsJFrame;
import aprs.framework.PddlAction;
import aprs.framework.Utils;
import aprs.framework.Utils.RunnableWithThrow;
import static aprs.framework.Utils.autoResizeTableColWidths;
import static aprs.framework.Utils.autoResizeTableRowHeights;
import aprs.framework.database.DbSetup;
import aprs.framework.database.DbSetupBuilder;
import aprs.framework.database.DbSetupPublisher;
import aprs.framework.spvision.VisionToDBJPanel;
import crcl.base.CRCLCommandInstanceType;
import crcl.base.CRCLProgramType;
import crcl.base.CRCLStatusType;
import crcl.base.CommandStateEnumType;
import crcl.base.EndCanonType;
import crcl.base.InitCanonType;
import crcl.base.JointStatusType;
import crcl.base.MessageType;
import crcl.base.MiddleCommandType;
import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.utils.CrclCommandWrapper;
import crcl.ui.XFuture;
import crcl.ui.client.PendantClientJPanel;
import crcl.utils.CRCLException;
import crcl.utils.CRCLSocket;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import static java.lang.Integer.max;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import javax.xml.bind.JAXBException;
import java.awt.HeadlessException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Vector;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import static crcl.utils.CRCLPosemath.pose;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class PddlExecutorJPanel extends javax.swing.JPanel implements PddlExecutorDisplayInterface, PendantClientJPanel.ProgramLineListener {

    /**
     * Creates new form ActionsToCrclJPanel
     */
    public PddlExecutorJPanel() {
        initComponents();
//        jTableTraySlotDesign.getModel().addTableModelListener(traySlotModelListener);
        jCheckBoxDebug.setSelected(debug);
        progColor = jTableCrclProgram.getBackground();
        pddlActionToCrclGenerator.addPlacePartConsumer(this::handlePlacePartCompleted);
        pddlActionToCrclGenerator.addActionCompletedListener(this::handleActionCompleted);
        jTablePddlOutput.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (row == currentActionIndex && isSelected) {
                    c.setBackground(Color.YELLOW);
                    c.setForeground(Color.BLACK);
                } else if (!isSelected) {
                    c.setBackground(Color.GRAY);
                } else {
                    c.setBackground(Color.BLUE);
                }
                return c;
            }
        });
    }

    private void handlePlacePartCompleted(PddlActionToCrclGenerator.PlacePartInfo ppi) {
        if (safeAbortRequested) {
            if (null == ppi) {
                throw new IllegalArgumentException("ppi == null");
            }
            if (null == ppi.getWrapper()) {
                throw new IllegalArgumentException("ppi.getWrapper() == null");
            }
            if (null == ppi.getWrapper().getCurProgram()) {
                throw new IllegalArgumentException(" ppi.getWrapper().getCurProgram() == null");
            }
            List<MiddleCommandType> l = ppi.getWrapper().getCurProgram().getMiddleCommand();
            while (l.size() > ppi.getOutIndex() + 1) {
                l.remove(ppi.getOutIndex() + 1);
            }
            pddlActionToCrclGenerator.addMoveToLookForPosition(l);
            this.replanFromIndex = ppi.getPddlActionIndex() + 1;
        }
    }

    private boolean stepping;

    /**
     * Get the value of stepping
     *
     * @return the value of stepping
     */
    public boolean isStepping() {
        return stepping;
    }

    /**
     * Set the value of stepping
     *
     * @param stepping new value of stepping
     */
    public void setStepping(boolean stepping) {
        this.stepping = stepping;
    }

    private void handleActionCompleted(PddlActionToCrclGenerator.ActionCallbackInfo actionInfo) {
        if (currentActionIndex < actionInfo.getActionIndex() + 1) {
            currentActionIndex = actionInfo.getActionIndex() + 1;
            updateSelectionInterval();
            if (null != aprsJFrame) {
                aprsJFrame.updateTitle();
                if (stepping) {
                    aprsJFrame.pauseCrclProgram();
                }
            }
        }
    }

    public void updateSelectionInterval() {
        int startIndex = Math.max(0, currentActionIndex);
        int endIndex = Math.max(startIndex, replanFromIndex - 1);
        jTablePddlOutput.getSelectionModel().setSelectionInterval(startIndex, endIndex);
    }

    private static Object[] getTableRow(JTable table, int row) {
        final int colCount = table.getModel().getColumnCount();
        if (colCount < 1) {
            return null;
        }
        Object out[] = new Object[table.getModel().getColumnCount()];
        for (int i = 0; i < colCount; i++) {
            out[i] = table.getModel().getValueAt(row, i);
        }
        return out;
    }

//    private final TableModelListener traySlotModelListener = new TableModelListener() {
//        @Override
//        public void tableChanged(TableModelEvent e) {
//            System.out.println("tableChanged : e = " + e);
//            for (int i = e.getFirstRow(); i <= e.getLastRow(); i++) {
//                if (updatingTraySlotTable) {
//                    return;
//                }
//                commitTraySlotDesign(getTableRow(jTableTraySlotDesign, i));
//            }
//        }
//    };
//
//    private void commitTraySlotDesign(Object[] data) {
//        try {
//            TraySlotDesign tsd = new TraySlotDesign((int) data[0]);
//            tsd.setTrayDesignName((String) data[1]);
//            tsd.setPartDesignName((String) data[2]);
//            tsd.setX_OFFSET((double) data[3]);
//            tsd.setY_OFFSET((double) data[4]);
//            pddlActionToCrclGenerator.setSingleTraySlotDesign(tsd);
//        } catch (SQLException ex) {
//            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTablePddlOutput = new javax.swing.JTable();
        jButtonLoadPddlActionsFromFile = new javax.swing.JButton();
        jTextFieldPddlOutputActions = new javax.swing.JTextField();
        jButtonLoad = new javax.swing.JButton();
        jButtonGenerateCRCL = new javax.swing.JButton();
        jButtonPddlOutputViewEdit = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableCrclProgram = new javax.swing.JTable();
        jCheckBoxNeedLookFor = new javax.swing.JCheckBox();
        jTextFieldIndex = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jCheckBoxReplan = new javax.swing.JCheckBox();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jScrollPaneOptions = new javax.swing.JScrollPane();
        jTableOptions = new javax.swing.JTable();
        jPanelOuterManualControl = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jPanelInnerManualControl = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jButtonTake = new javax.swing.JButton();
        jButtonLookFor = new javax.swing.JButton();
        jButtonReturn = new javax.swing.JButton();
        jComboBoxManualObjectName = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        jTextFieldTestXMin = new javax.swing.JTextField();
        jTextFieldTestXMax = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jTextFieldTestYMin = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jTextFieldTestYMax = new javax.swing.JTextField();
        jButtonRandDropOff = new javax.swing.JButton();
        jButtonTestPickup = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        jTextFieldTestZ = new javax.swing.JTextField();
        jTextFieldRandomPickupCount = new javax.swing.JTextField();
        jTextFieldRandomDropoffCount = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jTextFieldTestPose = new javax.swing.JTextField();
        jButtonContRandomTest = new javax.swing.JButton();
        jButtonStopRandomTest = new javax.swing.JButton();
        jButtonReset = new javax.swing.JButton();
        jButtonRecordFail = new javax.swing.JButton();
        jButtonRecordSuccess = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        jTextFieldLogFilename = new javax.swing.JTextField();
        jButtonNewLogFile = new javax.swing.JButton();
        jTextFieldRecordFailCount = new javax.swing.JTextField();
        jTextFieldRecordSuccessCount = new javax.swing.JTextField();
        jButtonGridTest = new javax.swing.JButton();
        jLabel12 = new javax.swing.JLabel();
        jTextFieldGridSize = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jTextFieldOffset = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        jTextFieldAdjPose = new javax.swing.JTextField();
        jComboBoxManualSlotName = new javax.swing.JComboBox<>();
        jLabel20 = new javax.swing.JLabel();
        jButtonPlacePart = new javax.swing.JButton();
        jButtonTest = new javax.swing.JButton();
        jButtonRecord = new javax.swing.JButton();
        jButtonRecordLookForJoints = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        positionMapJPanel1 = new aprs.framework.pddl.executor.PositionMapJPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        jPanel4 = new javax.swing.JPanel();
        jLabel16 = new javax.swing.JLabel();
        jTextFieldExternalControlPort = new javax.swing.JTextField();
        jCheckBoxEnableExternalControlPort = new javax.swing.JCheckBox();
        jScrollPane6 = new javax.swing.JScrollPane();
        jTextAreaExternalCommads = new javax.swing.JTextArea();
        jButtonSafeAbort = new javax.swing.JButton();
        jTextFieldSafeAbortCount = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jTextFieldSafeAbortRequestCount = new javax.swing.JTextField();
        jButtonClear = new javax.swing.JButton();
        jCheckBoxDebug = new javax.swing.JCheckBox();
        jButtonAbort = new javax.swing.JButton();
        jButtonGenerateAndRun = new javax.swing.JButton();
        jLabel17 = new javax.swing.JLabel();
        jTextFieldCurrentPart = new javax.swing.JTextField();
        jButtonStep = new javax.swing.JButton();
        jButtonContinue = new javax.swing.JButton();
        jButtonPause = new javax.swing.JButton();

        jLabel6.setText("Pddl Output Actions");

        jLabel7.setText("CRCL");

        jTablePddlOutput.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "#", "CRCLIndex", "Label", "Type", "Args", "Cost", "TakenPart"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane4.setViewportView(jTablePddlOutput);

        jButtonLoadPddlActionsFromFile.setText("Browse");
        jButtonLoadPddlActionsFromFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLoadPddlActionsFromFileActionPerformed(evt);
            }
        });

        jTextFieldPddlOutputActions.setEditable(false);
        jTextFieldPddlOutputActions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldPddlOutputActionsActionPerformed(evt);
            }
        });

        jButtonLoad.setText("Load");
        jButtonLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLoadActionPerformed(evt);
            }
        });

        jButtonGenerateCRCL.setText("Generate");
        jButtonGenerateCRCL.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGenerateCRCLActionPerformed(evt);
            }
        });

        jButtonPddlOutputViewEdit.setText("View/Edit");
        jButtonPddlOutputViewEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPddlOutputViewEditActionPerformed(evt);
            }
        });

        jTableCrclProgram.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                { new Integer(1), null},
                { new Integer(2), null},
                { new Integer(3), null},
                { new Integer(4), null}
            },
            new String [] {
                "ID", "Text"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTableCrclProgram);

        jCheckBoxNeedLookFor.setText("Skip LookFor");

        jTextFieldIndex.setText("0");
        jTextFieldIndex.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldIndexActionPerformed(evt);
            }
        });

        jLabel2.setText("Index");

        jCheckBoxReplan.setText("Continue");

        jTableOptions.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"rpy", "175.0,0.0,0.0"},
                {"lookForXYZ", "0.0,0.0,0.0"},
                {"approachZOffset", "50.0"},
                {"placeZOffset", "5.0"},
                {"takeZOffset", "0.0"},
                {"fastTransSpeed", "200.0"},
                {"slowTransSpeed", "75"},
                {"lookDwellTime", "3.0"},
                {"rotSpeed", "30.0"},
                {"placePartSlotArgIndex", "0"},
                {"takePartArgIndex", "0"},
                {"settleDwellTime", "0.1"},
                {"useJointLookFor", "false"},
                {"jointSpeed", "5.0"},
                {"jointAccel", "100.0"},
                {"takeSnapshots", "false"}
            },
            new String [] {
                "Name", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPaneOptions.setViewportView(jTableOptions);

        jTabbedPane1.addTab("Options", jScrollPaneOptions);

        jLabel1.setText("Object:");

        jButtonTake.setText("Take");
        jButtonTake.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonTakeActionPerformed(evt);
            }
        });

        jButtonLookFor.setText("Look-For");
        jButtonLookFor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLookForActionPerformed(evt);
            }
        });

        jButtonReturn.setText("Return");
        jButtonReturn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReturnActionPerformed(evt);
            }
        });

        jComboBoxManualObjectName.setEditable(true);

        jLabel3.setText("XMin:");

        jTextFieldTestXMin.setText("250.0 ");

        jTextFieldTestXMax.setText("650.0 ");

        jLabel4.setText("XMax:");

        jLabel5.setText("YMin:");

        jTextFieldTestYMin.setText("-220.0 ");

        jLabel8.setText("YMax:");

        jTextFieldTestYMax.setText("220.0 ");

        jButtonRandDropOff.setText("Random Drop-off");
        jButtonRandDropOff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRandDropOffActionPerformed(evt);
            }
        });

        jButtonTestPickup.setText("Pickup From Drop Off");
        jButtonTestPickup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonTestPickupActionPerformed(evt);
            }
        });

        jLabel9.setText("Z:");

        jTextFieldTestZ.setText("-149.0");

        jTextFieldRandomPickupCount.setEditable(false);
        jTextFieldRandomPickupCount.setText("0   ");

        jTextFieldRandomDropoffCount.setEditable(false);
        jTextFieldRandomDropoffCount.setText("0   ");

        jLabel10.setText("Pose:");

        jTextFieldTestPose.setEditable(false);
        jTextFieldTestPose.setText("0,0,0");

        jButtonContRandomTest.setText("Continous Random Test");
        jButtonContRandomTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonContRandomTestActionPerformed(evt);
            }
        });

        jButtonStopRandomTest.setText("Stop");
        jButtonStopRandomTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonStopRandomTestActionPerformed(evt);
            }
        });

        jButtonReset.setText("Reset");
        jButtonReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonResetActionPerformed(evt);
            }
        });

        jButtonRecordFail.setText("Record Fail");
        jButtonRecordFail.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRecordFailActionPerformed(evt);
            }
        });

        jButtonRecordSuccess.setText("Record Success");
        jButtonRecordSuccess.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRecordSuccessActionPerformed(evt);
            }
        });

        jLabel11.setText("Log File: ");

        jTextFieldLogFilename.setText("log.csv");

        jButtonNewLogFile.setText("New Log File");
        jButtonNewLogFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonNewLogFileActionPerformed(evt);
            }
        });

        jTextFieldRecordFailCount.setEditable(false);
        jTextFieldRecordFailCount.setText("0   ");

        jTextFieldRecordSuccessCount.setEditable(false);
        jTextFieldRecordSuccessCount.setText("0   ");

        jButtonGridTest.setText("Grid Test");
        jButtonGridTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGridTestActionPerformed(evt);
            }
        });

        jLabel12.setText("Grid Size");

        jTextFieldGridSize.setText("1,1");

        jLabel14.setText("Offset:");

        jTextFieldOffset.setText("0,0");

        jLabel15.setText("Adj. Pose:");

        jTextFieldAdjPose.setText("0,0,0");

        jComboBoxManualSlotName.setEditable(true);

        jLabel20.setText("Slot");

        jButtonPlacePart.setText("Place");
        jButtonPlacePart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPlacePartActionPerformed(evt);
            }
        });

        jButtonTest.setText("Test");
        jButtonTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonTestActionPerformed(evt);
            }
        });

        jButtonRecord.setText("Record");
        jButtonRecord.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRecordActionPerformed(evt);
            }
        });

        jButtonRecordLookForJoints.setText("Record LookFor  Joints");
        jButtonRecordLookForJoints.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRecordLookForJointsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelInnerManualControlLayout = new javax.swing.GroupLayout(jPanelInnerManualControl);
        jPanelInnerManualControl.setLayout(jPanelInnerManualControlLayout);
        jPanelInnerManualControlLayout.setHorizontalGroup(
            jPanelInnerManualControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInnerManualControlLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelInnerManualControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelInnerManualControlLayout.createSequentialGroup()
                        .addComponent(jButtonReset)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonRecordFail)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldRecordFailCount, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonRecordSuccess)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldRecordSuccessCount, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldLogFilename, javax.swing.GroupLayout.PREFERRED_SIZE, 323, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonNewLogFile))
                    .addGroup(jPanelInnerManualControlLayout.createSequentialGroup()
                        .addComponent(jButtonRandDropOff)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldRandomDropoffCount, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonTestPickup)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldRandomPickupCount, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldTestPose, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldOffset, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel15)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldAdjPose, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelInnerManualControlLayout.createSequentialGroup()
                        .addGroup(jPanelInnerManualControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelInnerManualControlLayout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jComboBoxManualObjectName, javax.swing.GroupLayout.PREFERRED_SIZE, 222, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonTake)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonLookFor)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonTest)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonReturn)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonRecord))
                            .addGroup(jPanelInnerManualControlLayout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldTestXMin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldTestXMax, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldTestYMin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldTestYMax, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel9)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldTestZ, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonContRandomTest)))
                        .addGap(6, 6, 6)
                        .addGroup(jPanelInnerManualControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelInnerManualControlLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(jButtonGridTest)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonStopRandomTest)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonRecordLookForJoints))
                            .addGroup(jPanelInnerManualControlLayout.createSequentialGroup()
                                .addComponent(jLabel12)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldGridSize, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonPlacePart)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel20)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jComboBoxManualSlotName, javax.swing.GroupLayout.PREFERRED_SIZE, 234, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap())
        );

        jPanelInnerManualControlLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jTextFieldAdjPose, jTextFieldOffset, jTextFieldTestPose});

        jPanelInnerManualControlLayout.setVerticalGroup(
            jPanelInnerManualControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInnerManualControlLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelInnerManualControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jButtonTake)
                    .addComponent(jButtonLookFor)
                    .addComponent(jButtonReturn)
                    .addComponent(jComboBoxManualObjectName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12)
                    .addComponent(jButtonTest)
                    .addComponent(jTextFieldGridSize, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonPlacePart)
                    .addComponent(jLabel20)
                    .addComponent(jComboBoxManualSlotName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonRecord))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelInnerManualControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanelInnerManualControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel3)
                        .addComponent(jTextFieldTestXMin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel4)
                        .addComponent(jTextFieldTestXMax, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel5)
                        .addComponent(jTextFieldTestYMin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel8)
                        .addComponent(jTextFieldTestYMax, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelInnerManualControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jTextFieldTestZ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jButtonContRandomTest)
                        .addComponent(jButtonStopRandomTest)
                        .addComponent(jLabel9)
                        .addComponent(jButtonGridTest)
                        .addComponent(jButtonRecordLookForJoints)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelInnerManualControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelInnerManualControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jButtonRandDropOff)
                        .addComponent(jButtonTestPickup)
                        .addComponent(jTextFieldRandomPickupCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel10)
                        .addComponent(jTextFieldTestPose, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jTextFieldRandomDropoffCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelInnerManualControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jTextFieldOffset, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel15)
                        .addComponent(jTextFieldAdjPose, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel14)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelInnerManualControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonReset)
                    .addComponent(jButtonRecordFail)
                    .addComponent(jButtonRecordSuccess)
                    .addComponent(jLabel11)
                    .addComponent(jTextFieldLogFilename, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonNewLogFile)
                    .addComponent(jTextFieldRecordFailCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldRecordSuccessCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(21, Short.MAX_VALUE))
        );

        jScrollPane2.setViewportView(jPanelInnerManualControl);

        javax.swing.GroupLayout jPanelOuterManualControlLayout = new javax.swing.GroupLayout(jPanelOuterManualControl);
        jPanelOuterManualControl.setLayout(jPanelOuterManualControlLayout);
        jPanelOuterManualControlLayout.setHorizontalGroup(
            jPanelOuterManualControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelOuterManualControlLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 966, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelOuterManualControlLayout.setVerticalGroup(
            jPanelOuterManualControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelOuterManualControlLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 114, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Manual Pickup Return", jPanelOuterManualControl);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(positionMapJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 966, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(positionMapJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 114, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Error Map", jPanel3);

        jLabel16.setText("Port:");

        jTextFieldExternalControlPort.setText("9999");

        jCheckBoxEnableExternalControlPort.setText("Enable");
        jCheckBoxEnableExternalControlPort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxEnableExternalControlPortActionPerformed(evt);
            }
        });

        jTextAreaExternalCommads.setColumns(20);
        jTextAreaExternalCommads.setRows(5);
        jScrollPane6.setViewportView(jTextAreaExternalCommads);

        jButtonSafeAbort.setText("Safe Abort");
        jButtonSafeAbort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSafeAbortActionPerformed(evt);
            }
        });

        jTextFieldSafeAbortCount.setEditable(false);
        jTextFieldSafeAbortCount.setText("0");

        jLabel18.setText("Safe Aborts Completed: ");

        jLabel19.setText("SafeAbortsRequested:");

        jTextFieldSafeAbortRequestCount.setEditable(false);
        jTextFieldSafeAbortRequestCount.setText("0");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 945, Short.MAX_VALUE)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel16)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldExternalControlPort, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxEnableExternalControlPort)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonSafeAbort)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel19)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldSafeAbortRequestCount, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel18)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldSafeAbortCount, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(jTextFieldExternalControlPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckBoxEnableExternalControlPort)
                    .addComponent(jButtonSafeAbort)
                    .addComponent(jTextFieldSafeAbortCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel18)
                    .addComponent(jLabel19)
                    .addComponent(jTextFieldSafeAbortRequestCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane6))
        );

        jScrollPane5.setViewportView(jPanel4);

        jTabbedPane1.addTab("External Control", jScrollPane5);

        jButtonClear.setText("Clear");
        jButtonClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonClearActionPerformed(evt);
            }
        });

        jCheckBoxDebug.setText("Debug");
        jCheckBoxDebug.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxDebugActionPerformed(evt);
            }
        });

        jButtonAbort.setText("Abort");
        jButtonAbort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAbortActionPerformed(evt);
            }
        });

        jButtonGenerateAndRun.setText("Run");
        jButtonGenerateAndRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGenerateAndRunActionPerformed(evt);
            }
        });

        jLabel17.setText("Part:");

        jTextFieldCurrentPart.setText(" ");

        jButtonStep.setText("Step");
        jButtonStep.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonStepActionPerformed(evt);
            }
        });

        jButtonContinue.setText("Continue");
        jButtonContinue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonContinueActionPerformed(evt);
            }
        });

        jButtonPause.setText("Pause");
        jButtonPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPauseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane4)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldPddlOutputActions, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonLoad)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonLoadPddlActionsFromFile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonPddlOutputViewEdit)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonClear)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxReplan)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldIndex, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxNeedLookFor)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel17)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldCurrentPart, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonPause)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonStep)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonContinue)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonGenerateAndRun)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonGenerateCRCL)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxDebug)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonAbort)
                        .addGap(11, 11, 11)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jButtonLoadPddlActionsFromFile)
                    .addComponent(jTextFieldPddlOutputActions, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonLoad)
                    .addComponent(jButtonPddlOutputViewEdit)
                    .addComponent(jButtonClear))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 138, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 156, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel7)
                        .addComponent(jCheckBoxReplan)
                        .addComponent(jLabel2)
                        .addComponent(jTextFieldIndex, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jCheckBoxNeedLookFor)
                        .addComponent(jLabel17)
                        .addComponent(jTextFieldCurrentPart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jButtonGenerateCRCL)
                        .addComponent(jCheckBoxDebug)
                        .addComponent(jButtonAbort)
                        .addComponent(jButtonGenerateAndRun)
                        .addComponent(jButtonStep)
                        .addComponent(jButtonContinue)
                        .addComponent(jButtonPause)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    @Override
    public void browseActionsFile() throws IOException {
        JFileChooser chooser = new JFileChooser(new File(jTextFieldPddlOutputActions.getText()).getParent());
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            jTextFieldPddlOutputActions.setText(f.getCanonicalPath());
            saveProperties();
        }
    }

    public XFuture<Boolean> startActions() {
        this.abortProgram();
        try {
            setReplanFromIndex(0);
            autoStart = true;
            jCheckBoxReplan.setSelected(true);
            if (null != runningProgramFuture) {
                runningProgramFuture.cancel(true);
            }
            runningProgramFuture = generateCrcl();
        } catch (IOException | IllegalStateException | SQLException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
            abortProgram();
        }
        return runningProgramFuture;
    }

    private final PddlActionToCrclGenerator pddlActionToCrclGenerator = new PddlActionToCrclGenerator();

    public PddlActionToCrclGenerator getPddlActionToCrclGenerator() {
        return pddlActionToCrclGenerator;
    }

//    public void setPddlActionToCrclGenerator(PddlActionToCrclGenerator pddlActionToCrclGenerator) {
//        this.pddlActionToCrclGenerator = pddlActionToCrclGenerator;
//    }
    private List<PddlAction> actionsList;

    /**
     * Get the value of actionsList
     *
     * @return the value of actionsList
     */
    public List<PddlAction> getActionsList() {
        return actionsList;
    }

    /**
     * Set the value of actionsList
     *
     * @param actionsList new value of actionsList
     */
    public void setActionsList(List<PddlAction> actionsList) {
        this.actionsList = actionsList;
        DefaultTableModel model = (DefaultTableModel) jTablePddlOutput.getModel();
        model.setNumRows(0);
    }

    private File propertiesFile;

    /**
     * Get the value of propertiesFile
     *
     * @return the value of propertiesFile
     */
    public File getPropertiesFile() {
        return propertiesFile;
    }

    /**
     * Set the value of propertiesFile
     *
     * @param propertiesFile new value of propertiesFile
     */
    public void setPropertiesFile(File propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    private static final String PDDLOUTPUT = "pddl.output";
    private static final String PDDLCRCLAUTOSTART = "pddl.crcl.autostart";

    public String[] getComboPartNames(int maxlen) {
        DefaultComboBoxModel<String> cbm
                = (DefaultComboBoxModel<String>) jComboBoxManualObjectName.getModel();
        String ret[] = new String[max(maxlen, cbm.getSize())];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = cbm.getElementAt(i);
        }
        return ret;
    }

    public String[] getComboSlotNames(int maxlen) {
        DefaultComboBoxModel<String> cbm
                = (DefaultComboBoxModel<String>) jComboBoxManualSlotName.getModel();
        String ret[] = new String[max(maxlen, cbm.getSize())];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = cbm.getElementAt(i);
        }
        return ret;
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

    public void saveProperties() throws IOException {
        Map<String, String> propsMap = new HashMap<>();
        String txtString = jTextFieldPddlOutputActions.getText();
        if (txtString != null && txtString.length() > 0) {
            String relPath = makeShortPath(propertiesFile, txtString);
            System.out.println("relPath = " + relPath);
            File chkFile = new File(relPath);
            if (!chkFile.isDirectory()) {
                propsMap.put(PDDLOUTPUT, relPath);
            }
        }
        Properties props = new Properties();
        props.putAll(propsMap);
        props.putAll(getTableOptions());
        props.put(MANUAL_PART_NAMES, Arrays.toString(getComboPartNames(10)));
        props.put(MANUAL_SLOT_NAMES, Arrays.toString(getComboSlotNames(10)));
//        try (FileWriter fw = new FileWriter(propertiesFile)) {
//            props.store(fw, "");
//        }
        Utils.saveProperties(propertiesFile, props);
    }
    private static final String MANUAL_PART_NAMES = "manualPartNames";
    private static final String MANUAL_SLOT_NAMES = "manualSlotNames";

    @Override
    public void addAction(PddlAction action) {
        if (null != action) {
            this.getActionsList().add(action);
            DefaultTableModel model = (DefaultTableModel) jTablePddlOutput.getModel();
            model.addRow(new Object[]{model.getRowCount(), -1, action.getLabel(), action.getType(), Arrays.toString(action.getArgs()), action.getCost()});
        }
    }

    @Override
    public void processActions() {
        try {
            if (null != runningProgramFuture) {
                runningProgramFuture.cancel(true);
            }
            runningProgramFuture = generateCrcl();
        } catch (IOException | IllegalStateException | SQLException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
            abortProgram();
        }
    }

    public void loadActionsFile(File f) throws IOException {
        if (null != f && f.exists()) {
            if (f.isDirectory()) {
                System.err.println("Can not loadActionsFile \"" + f + "\" : it is a directory instead of a text file.");
                return;
            }
            if (!f.canRead()) {
                System.err.println("Can not loadActionsFile \"" + f + "\" : it is not readable.");
                return;
            }
            this.setActionsList(new ArrayList<>());
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while (null != (line = br.readLine())) {
                    addAction(PddlAction.parse(line));
                }
            }
            autoResizeTableColWidthsPddlOutput();
            jCheckBoxReplan.setSelected(false);
            setReplanFromIndex(0);
            jTextFieldIndex.setText("0");
        }
    }

    @Override
    public void autoResizeTableColWidthsPddlOutput() {
        autoResizeTableColWidths(jTablePddlOutput);
    }

    public void refresh() {
        String fname = jTextFieldPddlOutputActions.getText();
        File f = new File(fname);
        if (f.exists() && f.canRead()) {
            try {
                loadActionsFile(f);
            } catch (IOException ex) {
                Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void jButtonLoadPddlActionsFromFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoadPddlActionsFromFileActionPerformed
        try {
            browseActionsFile();
            loadActionsFile(new File(jTextFieldPddlOutputActions.getText()));

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonLoadPddlActionsFromFileActionPerformed

    private void jButtonLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoadActionPerformed
        try {
            loadActionsFile(new File(jTextFieldPddlOutputActions.getText()));

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonLoadActionPerformed

    private void jTextFieldPddlOutputActionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldPddlOutputActionsActionPerformed
        try {
            loadActionsFile(new File(jTextFieldPddlOutputActions.getText()));

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jTextFieldPddlOutputActionsActionPerformed

    private DbSetupPublisher dbSetupPublisher;

    private Callable<DbSetupPublisher> dbSetupSupplier = null;

    public Callable<DbSetupPublisher> getDbSetupSupplier() {
        return dbSetupSupplier;
    }

    public void setDbSetupSupplier(Callable<DbSetupPublisher> dbSetupSupplier) {
        this.dbSetupSupplier = dbSetupSupplier;
        try {
            dbSetupPublisher = dbSetupSupplier.call();
            dbSetupPublisher.addDbSetupListener(this::handleNewDbSetup);
        } catch (Exception ex) {
            Logger.getLogger(VisionToDBJPanel.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private CRCLProgramType createEmptyProgram() {
        CRCLProgramType program = new CRCLProgramType();
        InitCanonType initCmd = program.getInitCanon();
        if (null == initCmd) {
            initCmd = new InitCanonType();
            if (null == initCmd.getCommandID()) {
                initCmd.setCommandID(BigInteger.ONE);
            }
        }
        program.setInitCanon(initCmd);
        EndCanonType endCmd = program.getEndCanon();
        if (null == endCmd) {
            endCmd = new EndCanonType();
            if (null == endCmd.getCommandID()) {
                endCmd.setCommandID(BigInteger.ONE);
            }
        }
        program.setEndCanon(endCmd);
        return program;
    }

    private CRCLProgramType crclProgram;

    /**
     * Get the value of crclProgram
     *
     * @return the value of crclProgram
     */
    public CRCLProgramType getCrclProgram() {
        return crclProgram;
    }

    List<JTextArea> crclAreas = new ArrayList<>();
    JTextArea editTableArea = new JTextArea();

    private String trimXml(String in) {
        int endHeaderIndex = in.indexOf("?>");
        if (endHeaderIndex > 0) {
            return in.substring(endHeaderIndex + 2).trim();
        } else {
            return in.trim();
        }
    }
    private Color progColor = Color.white;

    private void loadProgramToTable(CRCLProgramType crclProgram) {
        jTableCrclProgram.setBackground(Color.white);
        DefaultTableModel model = (DefaultTableModel) jTableCrclProgram.getModel();
        jTableCrclProgram.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                while (crclAreas.size() <= row) {
                    JTextArea area = new JTextArea();
                    area.setOpaque(true);
                    area.setVisible(true);
                    crclAreas.add(area);
                }
                JTextArea area = crclAreas.get(row);
                if (area != null) {
                    area.setText(Objects.toString(value));
                }
                return area;
            }

        });
        jTableCrclProgram.getColumnModel().getColumn(1).setCellEditor(new TableCellEditor() {

            private List<CellEditorListener> listeners = new ArrayList<>();

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                editTableArea.setOpaque(true);
                editTableArea.setVisible(true);
                editTableArea.setText(value.toString());
                return editTableArea;
            }

            @Override
            public Object getCellEditorValue() {
                return editTableArea.getText();
            }

            @Override
            public boolean isCellEditable(EventObject anEvent) {
                return true;
            }

            @Override
            public boolean shouldSelectCell(EventObject anEvent) {
                return true;
            }

            @Override
            public boolean stopCellEditing() {
                for (int i = 0; i < listeners.size(); i++) {
                    CellEditorListener l = listeners.get(i);
                    if (null != l) {
                        l.editingStopped(new ChangeEvent(jTableCrclProgram));
                    }
                }
                return true;
            }

            @Override
            public void cancelCellEditing() {
                for (int i = 0; i < listeners.size(); i++) {
                    CellEditorListener l = listeners.get(i);
                    if (null != l) {
                        l.editingCanceled(new ChangeEvent(jTableCrclProgram));
                    }
                }
            }

            @Override
            public void addCellEditorListener(CellEditorListener l) {
                listeners.add(l);
            }

            @Override
            public void removeCellEditorListener(CellEditorListener l) {
                listeners.remove(l);
            }
        });
        model.setRowCount(0);
        CRCLSocket crclSocket = CRCLSocket.getUtilSocket();
        CRCLCommandInstanceType instance = new CRCLCommandInstanceType();
        InitCanonType initCanon = crclProgram.getInitCanon();
        if (null != initCanon) {
            try {
                instance.setCRCLCommand(initCanon);
                model.addRow(new Object[]{initCanon.getCommandID().intValue(),
                    trimXml(crclSocket.commandInstanceToPrettyString(instance, true))
                });

            } catch (JAXBException | CRCLException ex) {
                model.addRow(new Object[]{initCanon.getCommandID().intValue(),
                    ex.getMessage()
                });
                Logger.getLogger(PddlExecutorJPanel.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        for (MiddleCommandType midCmd : crclProgram.getMiddleCommand()) {
            if (midCmd instanceof CrclCommandWrapper) {
                CrclCommandWrapper wrapper = (CrclCommandWrapper) midCmd;
                midCmd = wrapper.getWrappedCommand();
            }
            if (null != midCmd) {
                try {
                    instance.setCRCLCommand(midCmd);
                    model.addRow(new Object[]{midCmd.getCommandID().intValue(),
                        trimXml(crclSocket.commandInstanceToPrettyString(instance, true))
                    });

                } catch (JAXBException | CRCLException ex) {
                    model.addRow(new Object[]{midCmd.getCommandID().intValue(),
                        ex.getMessage()
                    });
                    Logger.getLogger(PddlExecutorJPanel.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        EndCanonType endCanon = crclProgram.getEndCanon();
        if (null != endCanon) {
            try {
                instance.setCRCLCommand(endCanon);
                model.addRow(new Object[]{endCanon.getCommandID().intValue(),
                    trimXml(crclSocket.commandInstanceToPrettyString(instance, true))
                });

            } catch (JAXBException | CRCLException ex) {
                model.addRow(new Object[]{endCanon.getCommandID().intValue(),
                    ex.getMessage()
                });
                Logger.getLogger(PddlExecutorJPanel.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        autoResizeTableRowHeights(jTableCrclProgram);
        autoResizeTableColWidths(jTableCrclProgram);
    }

    private boolean autoStart = true;

    private volatile CRCLProgramType unstartedProgram = null;

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
        if (null != pddlActionToCrclGenerator) {
            pddlActionToCrclGenerator.setAprsJFrame(aprsJFrame);
        }
    }

    /**
     * Set the value of crclProgram
     *
     * @param crclProgram new value of crclProgram
     */
    public void setCrclProgram(CRCLProgramType crclProgram) {
        try {
            this.crclProgram = crclProgram;
            unstartedProgram = crclProgram;
            Utils.runOnDispatchThreadWithCatch(() -> loadProgramToTable(crclProgram));
            if (null != aprsJFrame) {
                aprsJFrame.addProgramLineListener(this);
                aprsJFrame.setCRCLProgram(crclProgram);
            }
        } catch (Exception ex) {
            Logger.getLogger(PddlExecutorJPanel.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public XFuture<Boolean> continueCurrentCrclProgram() {
        return aprsJFrame.continueCurrentCrclProgram();
    }

    /**
     * Start executing a CRCL Program.
     *
     * The program will be run asynchronously in another thread after this
     * method has returned. The task can be modified, canceled or extended with
     * the returned future. The boolean contained within the future will be true
     * if the program completed successfully and false for non exceptional
     * errors.
     *
     * @param crclProgram program to be started
     * @return future that can be used to monitor, cancel or extend the
     * underlying task
     */
    public XFuture<Boolean> startCrclProgram(CRCLProgramType crclProgram) {
        try {
            this.crclProgram = crclProgram;
            unstartedProgram = null;
            Utils.runOnDispatchThreadWithCatch(() -> loadProgramToTable(crclProgram));
            this.runningProgram = true;
            if (null == aprsJFrame) {
                throw new IllegalStateException("Can't start crcl program with null aprsJFrame reference.");
            }
            aprsJFrame.addProgramLineListener(this);
            return aprsJFrame.startCRCLProgram(crclProgram);

        } catch (Exception ex) {
            XFuture<Boolean> future = new XFuture<>();
            Logger.getLogger(PddlExecutorJPanel.class
                    .getName()).log(Level.SEVERE, null, ex);
            future.completeExceptionally(ex);
            return future;
        }
    }

    private void jButtonGenerateCRCLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGenerateCRCLActionPerformed

        try {
            autoStart = false;
            setReplanFromIndex(0);
            if (null != runningProgramFuture) {
                runningProgramFuture.cancel(true);
            }
            runningProgramFuture = generateCrcl();
        } catch (IOException | IllegalStateException | SQLException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
            abortProgram();
        }
    }//GEN-LAST:event_jButtonGenerateCRCLActionPerformed

    private void jButtonPddlOutputViewEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPddlOutputViewEditActionPerformed
        try {
            Desktop.getDesktop().open(new File(jTextFieldPddlOutputActions.getText()));

        } catch (IOException ex) {
            Logger.getLogger(PddlExecutorJPanel.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonPddlOutputViewEditActionPerformed

    public int getReplanFromIndex() {
        return replanFromIndex;
    }

    private String currentPart = null;

    public void setReplanFromIndex(int replanFromIndex) {
        this.replanFromIndex = replanFromIndex;
        if (replanFromIndex == 0) {
            currentPart = null;
            jTextFieldCurrentPart.setText("");
        }
        updateSelectionInterval();
        jTablePddlOutput.scrollRectToVisible(new Rectangle(jTablePddlOutput.getCellRect(replanFromIndex, 0, true)));
        String[] names = this.pddlActionToCrclGenerator.getActionToCrclTakenPartsNames();
        if (names != null && names.length >= replanFromIndex && replanFromIndex > 0) {
            currentPart = names[replanFromIndex - 1];

        } else {
            currentPart = null;
        }
        if (null != currentPart) {
            jTextFieldCurrentPart.setText(currentPart);
        } else {
            jTextFieldCurrentPart.setText("");
        }
    }


    private void jTextFieldIndexActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldIndexActionPerformed
        setReplanFromIndex(Integer.parseInt(jTextFieldIndex.getText()));
    }//GEN-LAST:event_jTextFieldIndexActionPerformed

//    private boolean updatingTraySlotTable = false;
//
//    private void updateTraySlotTable() {
//        try {
//            updatingTraySlotTable = true;
//            jTableTraySlotDesign.getModel().removeTableModelListener(traySlotModelListener);
//            checkDbSupplierPublisher().thenRun(() -> {
//                Utils.runOnDispatchThread(() -> {
//                    try {
//                        List<TraySlotDesign> designs = pddlActionToCrclGenerator.getAllTraySlotDesigns();
//                        DefaultTableModel model = (DefaultTableModel) jTableTraySlotDesign.getModel();
//                        model.setRowCount(0);
//                        for (TraySlotDesign d : designs) {
//                            model.addRow(new Object[]{d.getID(), d.getPartDesignName(), d.getTrayDesignName(), d.getX_OFFSET(), d.getY_OFFSET()});
//                        }
//                        jTableTraySlotDesign.getModel().addTableModelListener(traySlotModelListener);
//                        updatingTraySlotTable = false;
//                    } catch (SQLException ex) {
//                        Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                });
//            });
//        } catch (IOException ex) {
//            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//    }
//
//    private void newTraySlotTable() {
//        try {
//            updatingTraySlotTable = true;
//            jTableTraySlotDesign.getModel().removeTableModelListener(traySlotModelListener);
//            checkDbSupplierPublisher().thenRun(() -> {
//                Utils.runOnDispatchThread(() -> {
//                    try {
//                        TraySlotDesign tsd = new TraySlotDesign((-99)); // ID ignored on new operation
//                        tsd.setPartDesignName("partDesignName");
//                        tsd.setTrayDesignName("trayDesignName");
//                        tsd.setX_OFFSET(0.0);
//                        tsd.setY_OFFSET(0.0);
//                        pddlActionToCrclGenerator.newSingleTraySlotDesign(tsd);
//                        List<TraySlotDesign> designs = pddlActionToCrclGenerator.getAllTraySlotDesigns();
//                        DefaultTableModel model = (DefaultTableModel) jTableTraySlotDesign.getModel();
//                        model.setRowCount(0);
//                        for (TraySlotDesign d : designs) {
//                            model.addRow(new Object[]{d.getID(), d.getPartDesignName(), d.getTrayDesignName(), d.getX_OFFSET(), d.getY_OFFSET()});
//                        }
//                    } catch (SQLException ex) {
//                        Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                });
//            });
//
//        } catch (IOException ex) {
//            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        jTableTraySlotDesign.getModel().addTableModelListener(traySlotModelListener);
//        updatingTraySlotTable = false;
//    }
//
//    private JPopupMenu traySlotPopup = new JPopupMenu();
//
//    {
//        JMenuItem updateMenuItem = new JMenuItem("Update");
//        updateMenuItem.addActionListener(e -> {
//            traySlotPopup.setVisible(false);
//            updateTraySlotTable();
//        });
//        traySlotPopup.add(updateMenuItem);
//        JMenuItem newRowMenuItem = new JMenuItem("New");
//        newRowMenuItem.addActionListener(e -> {
//            traySlotPopup.setVisible(false);
//            newTraySlotTable();
//        });
//        traySlotPopup.add(newRowMenuItem);
//    }
//
//    private void showTraySlotPopup(Component comp, int x, int y) {
//        traySlotPopup.show(comp, x, y);
//    }

    private void jButtonClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearActionPerformed
        clearAll();
    }//GEN-LAST:event_jButtonClearActionPerformed

    public void clearAll() {
        this.setActionsList(new ArrayList<>());
        DefaultTableModel model = (DefaultTableModel) jTableCrclProgram.getModel();
        model.setRowCount(0);
        setReplanFromIndex(0);
        safeAbortRequested = false;
        safeAbortRunnablesVector.clear();
        abortProgram();
    }

    /**
     * Abort the currently running CRCL program.
     */
    public void abortProgram() {
        boolean rps = replanStarted.getAndSet(true);
        if (null != customRunnables) {
            customRunnables.clear();
            customRunnables = null;
        }
        customRunnablesIndex = -1;
        if (null != replanActionTimer) {
            replanActionTimer.stop();
            replanActionTimer = null;
        }
        this.replanRunnable = this.defaultReplanRunnable;
        if (null != aprsJFrame) {
            aprsJFrame.abortCrclProgram();
        }
        this.safeAbortRequested = false;
        replanStarted.set(rps);
        runningProgram = false;
    }

    private int takePartCount = 0;

    private void jButtonTakeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonTakeActionPerformed
        try {
            if (null != currentPart) {
                this.jComboBoxManualObjectName.setSelectedItem(currentPart);
            }
            setReplanFromIndex(0);
            abortProgram();
            takePartCount++;
            clearAll();
            autoStart = true;
//            this.jTextFieldTakeCount.setText(Integer.toString(takePartCount));
            String part = getComboPart();
            if (null != runningProgramFuture) {
                runningProgramFuture.cancelAll(true);
            }
            runningProgramFuture = this.takePart(part);
        } catch (IOException | IllegalStateException | SQLException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
            abortProgram();
            showExceptionInProgram(ex);
        }
    }//GEN-LAST:event_jButtonTakeActionPerformed

    public String getComboPart() {
        Object object = jComboBoxManualObjectName.getSelectedItem();
        if (null == object) {
            return null;
        }
        String part = object.toString();
        DefaultComboBoxModel<String> cbm = (DefaultComboBoxModel<String>) jComboBoxManualObjectName.getModel();
        boolean partfound = false;
        for (int i = 0; i < cbm.getSize(); i++) {
            String parti = cbm.getElementAt(i).toString();
            if (parti.equals(part)) {
                partfound = true;
                break;
            }
        }
        if (!partfound) {
            cbm.insertElementAt(part, 0);
        }
        return part;
    }

    public String getComboSlot() {
        String slot = jComboBoxManualSlotName.getSelectedItem().toString();
        DefaultComboBoxModel<String> cbm = (DefaultComboBoxModel<String>) jComboBoxManualSlotName.getModel();
        boolean partfound = false;
        for (int i = 0; i < cbm.getSize(); i++) {
            String parti = cbm.getElementAt(i).toString();
            if (parti.equals(slot)) {
                partfound = true;
                break;
            }
        }
        if (!partfound) {
            cbm.insertElementAt(slot, 0);
        }
        return slot;
    }

    private int lookForCount = 0;
    private void jButtonLookForActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLookForActionPerformed
        try {
            if (null != currentPart) {
                this.jComboBoxManualObjectName.setSelectedItem(currentPart);
            }
            setReplanFromIndex(0);
            abortProgram();
            lookForCount++;
            clearAll();
            autoStart = true;
//            this.jTextFieldLookForCount.setText(Integer.toString(lookForCount));
            String part = getComboPart();
            if (null != runningProgramFuture) {
                runningProgramFuture.cancelAll(true);
            }
            runningProgramFuture = this.lookForParts();
        } catch (Exception e) {
            showExceptionInProgram(e);
        }
    }//GEN-LAST:event_jButtonLookForActionPerformed

    private int returnCount = 0;
    private void jButtonReturnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonReturnActionPerformed
        try {
            if (null != currentPart) {
                this.jComboBoxManualObjectName.setSelectedItem(currentPart);
            }
            setReplanFromIndex(0);
            abortProgram();
            returnCount++;
            clearAll();
            autoStart = true;
//            this.jTextFieldReturnCount.setText(Integer.toString(returnCount));
            String part = getComboPart();
            if (null != runningProgramFuture) {
                runningProgramFuture.cancelAll(true);
            }
            runningProgramFuture = this.returnPart(part);
        } catch (IOException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
            showExceptionInProgram(ex);
        }
    }//GEN-LAST:event_jButtonReturnActionPerformed

    int randomDropOffCount = 0;

    private void jButtonRandDropOffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRandDropOffActionPerformed
        randomDropOffCount++;
        clearAll();
        this.jTextFieldRandomDropoffCount.setText(Integer.toString(randomDropOffCount));
        if (null != runningProgramFuture) {
            runningProgramFuture.cancel(true);
        }
        runningProgramFuture = this.randomDropOff();
        System.out.println("randomDropOffCount = " + randomDropOffCount);
    }//GEN-LAST:event_jButtonRandDropOffActionPerformed

    int randomPickupCount = 0;

    public void writeCorrectionCsv(String filename, String line) throws IOException {
        File f = new File(filename);
        System.out.println("f.getCanonicalPath() = " + f.getCanonicalPath());
        boolean newFile = !f.exists();
        System.out.println("newFile = " + newFile);
        try (PrintWriter pw = new PrintWriter(new FileWriter(f, true))) {
            if (newFile) {
                pw.println("Time,PartName,Robot_X,Robot_Y,Robot_Z,Db_X,Db_Y,Db_Z,Offset_X,Offset_Y,Offset_Z");
            }
            pw.println(line);
        }
    }

    private void jButtonTestPickupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonTestPickupActionPerformed
        clearAll();
        queryLogFileName();
        recordAndCompletTestPickup();
    }//GEN-LAST:event_jButtonTestPickupActionPerformed

    private String recordCsvName = "corrections.csv";

    private XFuture<Boolean> recordAndCompletTestPickup() {
        try {
            randomPickupCount++;
            this.jTextFieldRandomPickupCount.setText(Integer.toString(randomPickupCount));

            String randomPoseString = testDropOffPose.getPoint().getX()
                    + "," + testDropOffPose.getPoint().getY()
                    + "," + testDropOffPose.getPoint().getZ();
            System.out.println("randomPoseString = " + randomPoseString);
            System.out.println("randomPickupCount = " + randomPickupCount);
            String partName = (String) jComboBoxManualObjectName.getSelectedItem();
            if (null != partName && partName.length() > 0) {
                PoseType poseFromDb = pddlActionToCrclGenerator.getPose(partName);
                if (null != poseFromDb) {
                    String poseFromDbString = poseFromDb.getPoint().getX()
                            + "," + poseFromDb.getPoint().getY()
                            + "," + poseFromDb.getPoint().getZ();
                    System.out.println("poseFromDbString = " + poseFromDbString);
                    String offsetString = testDropOffPose.getPoint().getX().subtract(poseFromDb.getPoint().getX())
                            + "," + testDropOffPose.getPoint().getY().subtract(poseFromDb.getPoint().getY())
                            + "," + testDropOffPose.getPoint().getZ().subtract(poseFromDb.getPoint().getZ());
                    System.out.println("offsetString = " + offsetString);
                    writeCorrectionCsv(recordCsvName,
                            System.currentTimeMillis() + ", " + partName + ", " + randomPoseString + ", " + poseFromDbString + ", " + offsetString);
                }
                return this.randomPickup();
            }
            XFuture<Boolean> future = new XFuture<>();
            future.complete(false);
            return future;
        } catch (IOException | SQLException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
            XFuture<Boolean> future = new XFuture<>();
            future.completeExceptionally(ex);
            return future;
        }
    }

    private void jButtonContRandomTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonContRandomTestActionPerformed
        startRandomTest();
    }//GEN-LAST:event_jButtonContRandomTestActionPerformed

    public XFuture<Boolean> startRandomTest() {
//        try {
        clearAll();
        queryLogFileName();
        jCheckBoxReplan.setSelected(true);
        autoStart = true;
//            jCheckBoxAutoStartCrcl.setSelected(true);
        final String partName = (String) jComboBoxManualObjectName.getSelectedItem();

        String randomPoseString
                = String.format("%.1f, %.1f, %.1f",
                        testDropOffPose.getPoint().getX().doubleValue(),
                        testDropOffPose.getPoint().getY().doubleValue(),
                        testDropOffPose.getPoint().getZ().doubleValue());
        System.out.println("randomPoseString = " + randomPoseString);
        System.out.println("randomDropOffCount = " + randomDropOffCount);
        customRunnables = new ArrayList<>();
        customRunnables.add(() -> {
            System.out.println("Continuing with lookFor");
            this.lookForParts();
        });
        customRunnables.add(() -> {
            System.out.println("Continuing with recordAndCompletRandomPickup");
            this.recordAndCompletTestPickup();
        });
        customRunnables.add(() -> {
            System.out.println("Continuing with randomDropOff");
            this.randomDropOff();
        });
        this.customRunnablesIndex = 0;
        this.replanRunnable = this.customReplanRunnable;
        return this.randomDropOff()
                .thenCompose(x -> recursiveSupplyBoolean(x, () -> this.lookForParts()))
                .thenCompose(x -> recursiveSupplyBoolean(x, () -> this.recordAndCompletTestPickup()))
                .thenCompose(x -> recursiveSupplyBoolean(x, () -> this.randomDropOff()));
//        } catch (IOException ex) {
//            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
//            XFuture<Boolean> ret = new XFuture<>();
//            ret.completeExceptionally(ex);
//            return ret;
//        }
    }

    public void startGridTest() throws HeadlessException {

        try {
            clearAll();
            String gridSizeString = jTextFieldGridSize.getText();
            String fa[] = gridSizeString.split("[ ,]+");
            if (fa.length != 2) {
                System.err.println("Bad gridSizeStrng = " + gridSizeString);
                return;
            }
            try {
                int xmax = Integer.parseInt(fa[0]);
                if (xmax < 1) {
                    System.err.println("Bad gridSizeStrng = " + gridSizeString);
                    return;
                }
                int ymax = Integer.parseInt(fa[1]);
                if (ymax < 1) {
                    System.err.println("Bad gridSizeStrng = " + gridSizeString);
                    return;
                }
                this.gridTestMaxX = (double) xmax;
                this.gridTestMaxY = (double) ymax;
            } catch (Exception e) {
                System.err.println("Bad gridSizeStrng = " + gridSizeString);
                return;
            }
            this.gridTestCurrentX = 0.0;
            this.gridTestCurrentY = 0.0;
            System.out.println("gridTestMaxX = " + gridTestMaxX);
            System.out.println("gridTestMaxY = " + gridTestMaxY);
            queryLogFileName();
            jCheckBoxReplan.setSelected(true);
            autoStart = true;
//            jCheckBoxAutoStartCrcl.setSelected(true);
            final String partName = (String) jComboBoxManualObjectName.getSelectedItem();
            this.gridDropOff();
            String randomPoseString
                    = String.format("%.1f, %.1f, %.1f",
                            testDropOffPose.getPoint().getX().doubleValue(),
                            testDropOffPose.getPoint().getY().doubleValue(),
                            testDropOffPose.getPoint().getZ().doubleValue());
            System.out.println("randomPoseString = " + randomPoseString);
            System.out.println("randomDropOffCount = " + randomDropOffCount);
            customRunnables = new ArrayList<>();
            customRunnables.add(() -> {
                System.out.println("Continuing with lookFor");
                this.lookForParts();
            });
            customRunnables.add(() -> {
                System.out.println("Continuing with recordAndCompletRandomPickup");
                this.recordAndCompletTestPickup();
            });
            customRunnables.add(() -> {
                System.out.println("Continuing with gridDropOff");
                this.gridDropOff();
            });
            this.customRunnablesIndex = 0;
            this.replanRunnable = this.customReplanRunnable;
        } catch (IOException iOException) {
        }
    }

    private void jButtonStopRandomTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonStopRandomTestActionPerformed
        this.clearAll();
    }//GEN-LAST:event_jButtonStopRandomTestActionPerformed

    private void jCheckBoxDebugActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxDebugActionPerformed
        this.setDebug(this.jCheckBoxDebug.isSelected());
    }//GEN-LAST:event_jCheckBoxDebugActionPerformed

    private void jButtonResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonResetActionPerformed
        this.clearAll();
        takePartCount = 0;
//        this.jTextFieldTakeCount.setText("0");
        lookForCount = 0;
//        this.jTextFieldLookForCount.setText("0");
        returnCount = 0;
//        this.jTextFieldReturnCount.setText("0");
        recordSuccessCount = 0;
        this.jTextFieldRecordSuccessCount.setText("0");
        recordFailCount = 0;
        this.jTextFieldRecordFailCount.setText("0");

    }//GEN-LAST:event_jButtonResetActionPerformed

    private void addFailLogCsvHeader(File f) throws IOException {
        if (!f.exists()) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(f, false))) {
                pw.println("time,status,object,x,y");
            }
        }
    }
    int recordFailCount = 0;
    private void jButtonRecordFailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRecordFailActionPerformed
        recordFailCount++;
        jTextFieldRecordFailCount.setText(Integer.toString(recordFailCount));
        try {
            File f = new File(jTextFieldLogFilename.getText());
            addFailLogCsvHeader(f);
            String partName = (String) jComboBoxManualObjectName.getSelectedItem();
            PoseType poseFromDb = pddlActionToCrclGenerator.getPose(partName);
            String poseFromDbString = poseFromDb.getPoint().getX()
                    + "," + poseFromDb.getPoint().getY();
            try (PrintWriter pw = new PrintWriter(new FileWriter(f, true))) {
                pw.println(System.currentTimeMillis() + ",FAIL," + partName + "," + poseFromDbString);
            }
        } catch (Exception ex) {
            Logger.getLogger(VisionToDBJPanel.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonRecordFailActionPerformed

    int recordSuccessCount = 0;
    private void jButtonRecordSuccessActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRecordSuccessActionPerformed
        recordSuccessCount++;
        jTextFieldRecordSuccessCount.setText(Integer.toString(recordSuccessCount));
        try {
            File f = new File(jTextFieldLogFilename.getText());
            addFailLogCsvHeader(f);
            String partName = (String) jComboBoxManualObjectName.getSelectedItem();
            PoseType poseFromDb = pddlActionToCrclGenerator.getPose(partName);
            String poseFromDbString = poseFromDb.getPoint().getX()
                    + "," + poseFromDb.getPoint().getY();
            try (PrintWriter pw = new PrintWriter(new FileWriter(f, true))) {
                pw.println(System.currentTimeMillis() + ",SUCCESS," + partName + "," + poseFromDbString);
            }
        } catch (Exception ex) {
            Logger.getLogger(VisionToDBJPanel.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonRecordSuccessActionPerformed


    private void jButtonGridTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGridTestActionPerformed
        this.startGridTest();
    }//GEN-LAST:event_jButtonGridTestActionPerformed

    private void jButtonAbortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAbortActionPerformed
        stepping = false;
        if (null != currentPart) {
            this.jComboBoxManualObjectName.setSelectedItem(currentPart);
        }
        setReplanFromIndex(0);
        abortProgram();
    }//GEN-LAST:event_jButtonAbortActionPerformed

    private void jButtonGenerateAndRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGenerateAndRunActionPerformed
        try {
            stepping = false;
            setReplanFromIndex(0);
            autoStart = true;
            jCheckBoxReplan.setSelected(true);
            if (null != runningProgramFuture) {
                runningProgramFuture.cancel(true);
            }
            runningProgramFuture = generateCrcl();
        } catch (IOException | IllegalStateException | SQLException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
            abortProgram();
        }
    }//GEN-LAST:event_jButtonGenerateAndRunActionPerformed

    private ServerSocket externalControlSocket = null;
    private Thread externalControlAcceptThread = null;
    private List<Socket> externalControlClientSockets = null;
    private List<Thread> externalControlClientThreads = null;

    private static void sendSocket(Socket s, String msg) {
        try {
            s.getOutputStream().write(msg.getBytes());
        } catch (IOException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void handleExternalCommand(Socket s, String line) {
        jTextAreaExternalCommads.append(new Date() + ":" + s.getRemoteSocketAddress() + ": " + line + "\r\n");
        if (line.startsWith("safe_abort")) {
//            if (this.safeAbort()) {
//                sendSocket(s, "Done\r\n");
//            } else {
//                this.safeAbortRunnablesVector.add(() -> sendSocket(s, "Done\r\n"));
//            }
            this.safeAbort().thenAccept(b -> sendSocket(s, "Done\r\n"));
        }
        jTextAreaExternalCommads.setCaretPosition(jTextAreaExternalCommads.getText().length() - 1);
    }

    private void runMainExternalControlClientHandler(final Socket s) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            String line = null;
            while (null != (line = br.readLine()) && !Thread.currentThread().isInterrupted()) {
                if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
                    final String finalLine = line;
                    javax.swing.SwingUtilities.invokeAndWait(() -> handleExternalCommand(s, finalLine));
                } else {
                    handleExternalCommand(s, line);
                }
            }
        } catch (IOException | InterruptedException | InvocationTargetException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            s.close();
        } catch (IOException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void runExternalControlMainServerThread() {
        try {
            while (!Thread.currentThread().isInterrupted()) {

                final Socket s = externalControlSocket.accept();
                externalControlClientSockets.add(s);
                Thread t = new Thread(() -> runMainExternalControlClientHandler(s),
                        "externalControlClientThread_" + s.getRemoteSocketAddress());
                t.start();
                externalControlClientThreads.add(t);
            }
        } catch (IOException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setExternalControlPortEnabled(boolean on) throws IOException {
        closeExternalControlService();
        externalControlSocket = new ServerSocket(Integer.parseInt(jTextFieldExternalControlPort.getText()));
        externalControlClientSockets = new ArrayList<>();
        externalControlClientThreads = new ArrayList<>();
        externalControlAcceptThread = new Thread(this::runExternalControlMainServerThread, "externalControlMainServerThread");
        externalControlAcceptThread.start();
        if (jCheckBoxEnableExternalControlPort.isSelected() != on) {
            jCheckBoxEnableExternalControlPort.setSelected(on);
        }
    }

    private static void closeSocket(Socket s) {
        try {
            s.close();
        } catch (IOException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void closeThread(Thread t) {
        try {
            t.interrupt();
            t.join(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void closeExternalControlService() {
        if (null != externalControlSocket) {
            try {
                externalControlSocket.close();
            } catch (IOException ex) {
                Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (null != externalControlAcceptThread) {
            externalControlAcceptThread.interrupt();
            try {
                externalControlAcceptThread.join(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (externalControlClientSockets != null) {
            externalControlClientSockets.forEach(s -> closeSocket(s));
            externalControlClientSockets.clear();
            externalControlClientSockets = null;
        }
        if (externalControlClientThreads != null) {
            externalControlClientThreads.forEach(t -> closeThread(t));
            externalControlClientThreads.clear();
            externalControlClientThreads = null;
        }
    }

    private void jCheckBoxEnableExternalControlPortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxEnableExternalControlPortActionPerformed
        try {
            setExternalControlPortEnabled(jCheckBoxEnableExternalControlPort.isSelected());
        } catch (IOException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxEnableExternalControlPortActionPerformed

    private volatile XFuture<Boolean> runningProgramFuture = null;

    private void jButtonStepActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonStepActionPerformed

        stepping = true;
        continueActionListPrivate();
//        try {
//            stepping = true;
//            autoStart = !started;
//            if (null != unstartedProgram) {
//                if (autoStart) {
//                    if (null != runningProgramFuture) {
//                        runningProgramFuture.cancel(true);
//                    }
//                    runningProgramFuture = startCrclProgram(unstartedProgram);
//                } else {
//                    setCrclProgram(unstartedProgram);
//                }
//            } else {
//                if (null != runningProgramFuture) {
//                    runningProgramFuture.cancel(true);
//                }
//                runningProgramFuture = generateCrcl();
//            }
//            autoStart = false;
//        } catch (IOException | IllegalStateException | SQLException ex) {
//            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
//            abortProgram();
//        }
    }//GEN-LAST:event_jButtonStepActionPerformed

    private int safeAboutCount = 0;
    private int safeAbortRequestCount = 0;

    private void incSafeAbortCount() {
        safeAboutCount++;
        jTextFieldSafeAbortCount.setText(Integer.toString(safeAboutCount));
    }

    private void incSafeAbortRequestCount() {
        safeAbortRequestCount++;
        jTextFieldSafeAbortRequestCount.setText(Integer.toString(safeAbortRequestCount));
    }

    public XFuture<Void> safeAbort() {
        final XFuture<Void> ret = new XFuture<>();
        if (!runningProgram) {
            ret.complete(null);
            return ret;
        }
        incSafeAbortRequestCount();
        synchronized (this) {
            this.safeAbortRequested = true;
            this.safeAbortRunnablesVector.add(this::incSafeAbortCount);
            this.safeAbortRunnablesVector.add(() -> ret.complete(null));
        }
        return ret;
    }

    private void jButtonSafeAbortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSafeAbortActionPerformed
        this.safeAbort();
    }//GEN-LAST:event_jButtonSafeAbortActionPerformed

    private void jButtonContinueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonContinueActionPerformed
        stepping = false;
        continueActionListPrivate();
    }//GEN-LAST:event_jButtonContinueActionPerformed

    public XFuture<Void> continueActionList() {

        XFuture<Void> ret = new XFuture<>();
        addProgramCompleteRunnable(() -> {
            ret.complete(null);
        });
        Utils.runOnDispatchThread(() -> {
            continueActionListPrivate();
        });
        return ret;
    }

    private void continueActionListPrivate() {
//        if(aprsJFrame.isCrclProgramPaused() && aprsJFrame.getCrclProgram() != null) {
//            runningProgramFuture = continueCurrentCrclProgram();
//            return;
//        }
        autoStart = true;
        safeAbortRequested = false;
        safeAbortRunnablesVector.clear();
        if (replanFromIndex < 0 || replanFromIndex >= actionsList.size()) {
            setReplanFromIndex(0);
        }
        jCheckBoxReplan.setSelected(true);
        if (null != runningProgramFuture) {
            runningProgramFuture.cancelAll(true);
        }
        if (null != unstartedProgram) {
            runningProgramFuture = startCrclProgram(unstartedProgram);
        } else {

            runningProgramFuture
                    = continueCurrentCrclProgram().thenCompose(x -> {
                        try {
                            return generateCrcl();
                        } catch (IOException | IllegalStateException | SQLException ex) {
                            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
                            abortProgram();
                            return XFuture.completedFuture(false);
                        }
                    });
        }
    }

    private int placePartCount = 0;
    private void jButtonPlacePartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPlacePartActionPerformed
        try {
            if (null != currentPart) {
                this.jComboBoxManualObjectName.setSelectedItem(currentPart);
            }
            setReplanFromIndex(0);
            abortProgram();
            placePartCount++;
            clearAll();
            autoStart = true;
//            this.jTextFieldTakeCount.setText(Integer.toString(takePartCount));
            String part = getComboPart();
            String slot = getComboSlot();
            if (null != runningProgramFuture) {
                runningProgramFuture.cancelAll(true);
            }
            runningProgramFuture = this.placePartSlot(part, slot);
        } catch (IOException | IllegalStateException | SQLException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
            abortProgram();
            showExceptionInProgram(ex);
        }
    }//GEN-LAST:event_jButtonPlacePartActionPerformed

    private void jButtonTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonTestActionPerformed
        try {
            if (null != currentPart) {
                this.jComboBoxManualObjectName.setSelectedItem(currentPart);
            }
            setReplanFromIndex(0);
            abortProgram();
            clearAll();
            autoStart = true;
//            this.jTextFieldTakeCount.setText(Integer.toString(takePartCount));
            String part = getComboPart();
            if (null != runningProgramFuture) {
                runningProgramFuture.cancelAll(true);
            }
            runningProgramFuture = this.testPartPosition(part);
        } catch (IOException | IllegalStateException | SQLException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
            abortProgram();
            showExceptionInProgram(ex);
        }
    }//GEN-LAST:event_jButtonTestActionPerformed

    private void jButtonNewLogFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonNewLogFileActionPerformed
        newLogFileName();
    }//GEN-LAST:event_jButtonNewLogFileActionPerformed

    private void jButtonRecordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRecordActionPerformed
        queryLogFileName();
        String partName = (String) jComboBoxManualObjectName.getSelectedItem();
        if (null != partName && partName.length() > 0) {
            try {
                PoseType poseFromDb = pddlActionToCrclGenerator.getPose(partName);
                if (null != poseFromDb) {
                    String poseFromDbString = poseFromDb.getPoint().getX()
                            + "," + poseFromDb.getPoint().getY()
                            + "," + poseFromDb.getPoint().getZ();
                    System.out.println("poseFromDbString = " + poseFromDbString);
                    PoseType curPose = aprsJFrame.getCurrentPose();
                    String curPoseString
                            = String.format("%.1f, %.1f, %.1f",
                                    curPose.getPoint().getX().doubleValue(),
                                    curPose.getPoint().getY().doubleValue(),
                                    curPose.getPoint().getZ().doubleValue());
                    String offsetString = curPose.getPoint().getX().subtract(poseFromDb.getPoint().getX())
                            + "," + curPose.getPoint().getY().subtract(poseFromDb.getPoint().getY())
                            + "," + curPose.getPoint().getZ().subtract(poseFromDb.getPoint().getZ());
                    System.out.println("offsetString = " + offsetString);
                    writeCorrectionCsv(recordCsvName,
                            System.currentTimeMillis() + ", " + partName + ", " + curPoseString + ", " + poseFromDbString + ", " + offsetString);
                }
            } catch (SQLException | IOException ex) {
                Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jButtonRecordActionPerformed

    private void jButtonPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPauseActionPerformed
        aprsJFrame.pauseCrclProgram();
    }//GEN-LAST:event_jButtonPauseActionPerformed

    private void updateLookForJoints(CRCLStatusType stat) {
        if (null != stat && null != stat.getJointStatuses()) {
            List<JointStatusType> jointList = stat.getJointStatuses().getJointStatus();
            String jointVals
                    = jointList
                            .stream()
                            .sorted(Comparator.comparing(JointStatusType::getJointNumber))
                            .map(JointStatusType::getJointPosition)
                            .map(Objects::toString)
                            .collect(Collectors.joining(","));
            System.out.println("jointVals = " + jointVals);
            DefaultTableModel model = (DefaultTableModel) jTableOptions.getModel();
            boolean keyFound = false;
            for (int i = 0; i < model.getRowCount(); i++) {
                if (Objects.equals("lookForJoints", model.getValueAt(i, 0))) {
                    model.setValueAt(jointVals, i, 1);
                    keyFound = true;
                }
            }
            if (!keyFound) {
                model.addRow(new Object[]{"lookForJoints", jointVals});
            }
            pddlActionToCrclGenerator.setOptions(getTableOptions());
        }
    }

    private void jButtonRecordLookForJointsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRecordLookForJointsActionPerformed
        CRCLStatusType status = aprsJFrame.getCurrentStatus();
        if (null != status) {
            this.updateLookForJoints(status);
        }
    }//GEN-LAST:event_jButtonRecordLookForJointsActionPerformed

    private void queryLogFileName() {
        if (!new File(recordCsvName).exists()) {
            newLogFileName();
        }
    }

    private void newLogFileName() throws HeadlessException {
        recordCsvName = jTextFieldLogFilename.getText();
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = chooser.getSelectedFile();
                recordCsvName = f.getCanonicalPath();
                if (!recordCsvName.endsWith(".csv")) {
                    recordCsvName += ".csv";
                }
                jTextFieldLogFilename.setText(recordCsvName);
            } catch (IOException ex) {
                Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private int[] crclIndexes = null;

    public void setCrclIndexes(int indexes[]) {
        DefaultTableModel model = (DefaultTableModel) jTablePddlOutput.getModel();
        for (int i = 0; i < indexes.length; i++) {
            if (i >= model.getRowCount()) {
                break;
            }
            model.setValueAt(indexes[i], i, 1);
        }
        autoResizeTableColWidths(jTablePddlOutput);
        this.crclIndexes = indexes;
    }

    public void setPddlLabelss(String labels[]) {
        DefaultTableModel model = (DefaultTableModel) jTablePddlOutput.getModel();
        for (int i = 0; i < labels.length; i++) {
            if (i >= model.getRowCount()) {
                break;
            }
            if (null != labels[i]) {
                model.setValueAt(labels[i], i, 2);
            }
        }
        autoResizeTableColWidths(jTablePddlOutput);
    }

    public void setPddlTakenParts(String parts[]) {
        DefaultTableModel model = (DefaultTableModel) jTablePddlOutput.getModel();
        for (int i = 0; i < parts.length; i++) {
            if (i >= model.getRowCount()) {
                break;
            }
            if (null != parts[i]) {
                model.setValueAt(parts[i], i, 6);
            }
        }
        autoResizeTableColWidths(jTablePddlOutput);
    }

    boolean started = false;

    private volatile boolean safeAbortRequested = false;
    private final Vector<Runnable> safeAbortRunnablesVector = new Vector<Runnable>();

    private void generateCrclWithCatch() {
        try {
            if (null != runningProgramFuture) {
                runningProgramFuture.cancel(true);
            }
            runningProgramFuture = generateCrcl();
        } catch (IOException | IllegalStateException | SQLException ex) {
            replanStarted.set(false);
            abortProgram();
            showExceptionInProgram(ex);
//            actionToCrclLabels[lastIndex] = "Error";
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void showExceptionInProgram(final java.lang.Exception ex) {
        CRCLProgramType program = createEmptyProgram();
        List<MiddleCommandType> cmds = program.getMiddleCommand();
        MessageType message = new MessageType();
        message.setCommandID(BigInteger.valueOf(cmds.size() + 2));
        message.setMessage(ex.toString());
        cmds.add(message);
        loadProgramToTable(program);
        jTableCrclProgram.setBackground(Color.red);

    }

    private String crclProgName = null;
    private String lastCrclProgName = null;

    private final List<Runnable> programCompleteRunnablesList = new ArrayList<>();

    public void addProgramCompleteRunnable(Runnable r) {
        synchronized (programCompleteRunnablesList) {
            programCompleteRunnablesList.add(r);
        }
    }

    public void removeProgramCompleteRunnable(Runnable r) {
        synchronized (programCompleteRunnablesList) {
            programCompleteRunnablesList.remove(r);
        }
    }

    private XFuture<Boolean> recursiveSupplyBoolean(boolean prevSuccess,
            Supplier<XFuture<Boolean>> supplier) {
        if (prevSuccess) {
            try {
                return Utils.composeOnDispatchThread(supplier);
            } catch (Exception ex) {
                Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
                XFuture<Boolean> ret = new XFuture<Boolean>();
                ret.completeExceptionally(ex);
                return ret;
            }
        } else {
            XFuture<Boolean> ret = new XFuture<Boolean>();
            ret.complete(false);
            return ret;
        }
    }

    public static XFuture<Boolean> ifOk(boolean ok, Supplier<XFuture<Boolean>> thenSupplier) {
        return ok ? thenSupplier.get() : XFuture.completedFuture(false);
    }

    private XFuture<Boolean> recursiveApplyGenerateCrcl(boolean prevSuccess) {
        if (prevSuccess) {
            try {
                return (XFuture<Boolean>) generateCrcl();
            } catch (IOException | IllegalStateException | SQLException ex) {
                Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
                XFuture<Boolean> ret = new XFuture<Boolean>();
                ret.completeExceptionally(ex);
                return ret;
            }
        } else {
            XFuture<Boolean> ret = new XFuture<Boolean>();
            ret.complete(false);
            return ret;
        }
    }
    private int crclStartActionIndex = -1;
    private int crclEndActionIndex = -1;

    private void takeSimViewSnapshot(File f, PoseType pose, String label) throws IOException {
        if (null != aprsJFrame) {
            aprsJFrame.takeSimViewSnapshot(f, pose, label);
        }
    }

    private XFuture<Boolean> generateCrcl() throws IOException, IllegalStateException, SQLException {
        boolean doSafeAbort;

        synchronized (this) {
            if (safeAbortRequested && null == currentPart) {
                safeAbortRequested = false;
                autoStart = false;
                doSafeAbort = true;
            } else {
                doSafeAbort = false;
            }
        }
        if (doSafeAbort) {
            this.abortProgram();
            while (safeAbortRunnablesVector.size() > 0) {
                Runnable r = safeAbortRunnablesVector.remove(0);
                if (null != r) {
                    r.run();
                }
            }
            if (pddlActionToCrclGenerator.isTakeSnapshots()) {
                takeSimViewSnapshot(File.createTempFile(pddlActionToCrclGenerator.getRunPrefix() + "-safe-abort-", ".PNG"), null, "");
            }
            return XFuture.completedFuture(false);
        }
        return checkDbSupplierPublisher()
                .thenCompose(x -> {
                    return Utils.supplyOnDispatchThread(() -> {
                        return doPddlActionsSection();
                    });
                }).thenCompose(x -> x);
    }

    private CRCLProgramType pddlActionSectionToCrcl() throws IllegalStateException, SQLException {
        Map<String, String> options = getTableOptions();
        if (replanFromIndex < 0 || replanFromIndex > actionsList.size()) {
            replanFromIndex = 0;
        }
        pddlActionToCrclGenerator.setPositionMaps(getPositionMaps());
        crclStartActionIndex = this.replanFromIndex;
        currentActionIndex = crclStartActionIndex;
        if (null != aprsJFrame) {
            aprsJFrame.updateTitle();
        }
        List<MiddleCommandType> cmds = pddlActionToCrclGenerator.generate(actionsList, this.replanFromIndex, options);
        int indexes[] = pddlActionToCrclGenerator.getActionToCrclIndexes();
        indexes = Arrays.copyOf(indexes, indexes.length);
        setCrclIndexes(indexes);
        setPddlLabelss(pddlActionToCrclGenerator.getActionToCrclLabels());
        setPddlTakenParts(pddlActionToCrclGenerator.getActionToCrclTakenPartsNames());
        CRCLProgramType program = createEmptyProgram();
        program.setName("pddl_" + replanFromIndex + "_" + pddlActionToCrclGenerator.getLastIndex());
        lastCrclProgName = crclProgName;
        crclProgName = program.getName();
        crclEndActionIndex = pddlActionToCrclGenerator.getLastIndex();
        if (pddlActionToCrclGenerator.getLastIndex() < actionsList.size() - 1) {
            setReplanFromIndex(pddlActionToCrclGenerator.getLastIndex() + 1);
//            replanAfterCrclBlock = jCheckBoxReplan.isSelected();
        } else {
            setReplanFromIndex(actionsList.size() - 1);
        }
        jTextFieldIndex.setText(Integer.toString(replanFromIndex));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        program.getEndCanon().setCommandID(BigInteger.valueOf(cmds.size() + 2));
        return program;
    }

    private XFuture<Boolean> doPddlActionsSection() {
        try {
            CRCLProgramType program = pddlActionSectionToCrcl();

            if (autoStart) {
                boolean replanAfterCrclBlock
                        = pddlActionToCrclGenerator.getLastIndex() < actionsList.size() - 1
                        && jCheckBoxReplan.isSelected();
                if (replanAfterCrclBlock) {
                    return startCrclProgram(program)
                            .thenCompose(this::recursiveApplyGenerateCrcl);
                } else {
                    return startCrclProgram(program).thenApply(x2 -> {
                        runProgramCompleteRunnables();
                        return x2;
                    });
                }
            } else {
                setCrclProgram(program);
            }
        } catch (IllegalStateException | SQLException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
            showExceptionInProgram(ex);
            XFuture<Boolean> ret = new XFuture<>();
            ret.completeExceptionally(ex);
            return ret;
        } finally {
            started = autoStart;
            replanStarted.set(false);
        }
        return XFuture.completedFuture(false);
    }

    private void runProgramCompleteRunnables() {
        this.runningProgram = false;
        List<Runnable> runnables = new ArrayList<>();
        synchronized (programCompleteRunnablesList) {
            runnables.addAll(programCompleteRunnablesList);
            programCompleteRunnablesList.clear();
        }
        for (Runnable r : runnables) {
            r.run();
        }
    }

    public XFuture<Boolean> placePartSlot(String part, String slot) throws IOException, IllegalStateException, SQLException {
        clearAll();
        Map<String, String> options = getTableOptions();
        replanFromIndex = 0;
        List<PddlAction> placePartActionsList = new ArrayList<>();
        PddlAction placePartAction = new PddlAction("", "place-part",
                new String[]{slot}, "cost");
        placePartActionsList.add(placePartAction);
        pddlActionToCrclGenerator.setPositionMaps(getPositionMaps());
        List<MiddleCommandType> cmds = pddlActionToCrclGenerator.generate(placePartActionsList, this.replanFromIndex, options);
        CRCLProgramType program = createEmptyProgram();
        jTextFieldIndex.setText(Integer.toString(replanFromIndex));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        program.getEndCanon().setCommandID(BigInteger.valueOf(cmds.size() + 2));
        XFuture<Boolean> ret = startCrclProgram(program);
        replanStarted.set(false);
        return ret;
    }

    public XFuture<Boolean> testPartPosition(String part) throws IOException, IllegalStateException, SQLException {
        clearAll();
        Map<String, String> options = getTableOptions();
        replanFromIndex = 0;
        List<PddlAction> testPartPositionActionList = new ArrayList<>();
        PddlAction takePartAction = new PddlAction("", "test-part-position",
                new String[]{part}, "cost");
        testPartPositionActionList.add(takePartAction);
        pddlActionToCrclGenerator.setPositionMaps(getPositionMaps());
        List<MiddleCommandType> cmds = pddlActionToCrclGenerator.generate(testPartPositionActionList, this.replanFromIndex, options);
        CRCLProgramType program = createEmptyProgram();
        jTextFieldIndex.setText(Integer.toString(replanFromIndex));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        program.getEndCanon().setCommandID(BigInteger.valueOf(cmds.size() + 2));

        for (PositionMap positionMap : getPositionMaps()) {
            PointType offset = positionMap.getLastOffset();
            if (null != offset) {
                jTextFieldOffset.setText(String.format("%.1f,%.1f", offset.getX().doubleValue(), offset.getY().doubleValue()));
            }
            PointType testPoint = positionMap.getLastPointOut();
            if (null != testPoint) {
                String testPoseString
                        = String.format("%.1f, %.1f, %.1f",
                                testPoint.getX().doubleValue(),
                                testPoint.getY().doubleValue(),
                                testPoint.getZ().doubleValue());
                jTextFieldAdjPose.setText(testPoseString);
            }
            PointType origPoint = positionMap.getLastPointIn();
            if (null != origPoint) {
                String origPoseString
                        = String.format("%.1f, %.1f, %.1f",
                                origPoint.getX().doubleValue(),
                                origPoint.getY().doubleValue(),
                                origPoint.getZ().doubleValue());
                this.jTextFieldTestPose.setText(origPoseString);
            }
        }

        replanStarted.set(false);
        return startCrclProgram(program);
    }

    public XFuture<Boolean> takePart(String part) throws IOException, IllegalStateException, SQLException {
        clearAll();
        Map<String, String> options = getTableOptions();
        replanFromIndex = 0;
        List<PddlAction> takePartActionsList = new ArrayList<>();
        PddlAction takePartAction = new PddlAction("", "take-part",
                new String[]{part}, "cost");
        takePartActionsList.add(takePartAction);
        pddlActionToCrclGenerator.setPositionMaps(getPositionMaps());
        List<MiddleCommandType> cmds = pddlActionToCrclGenerator.generate(takePartActionsList, this.replanFromIndex, options);
        CRCLProgramType program = createEmptyProgram();
        jTextFieldIndex.setText(Integer.toString(replanFromIndex));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        program.getEndCanon().setCommandID(BigInteger.valueOf(cmds.size() + 2));

        for (PositionMap positionMap : getPositionMaps()) {
            PointType offset = positionMap.getLastOffset();
            if (null != offset) {
                jTextFieldOffset.setText(String.format("%.1f,%.1f", offset.getX().doubleValue(), offset.getY().doubleValue()));
            }
            PointType testPoint = positionMap.getLastPointOut();
            if (null != testPoint) {
                String testPoseString
                        = String.format("%.1f, %.1f, %.1f",
                                testPoint.getX().doubleValue(),
                                testPoint.getY().doubleValue(),
                                testPoint.getZ().doubleValue());
                jTextFieldAdjPose.setText(testPoseString);
            }
            PointType origPoint = positionMap.getLastPointIn();
            if (null != origPoint) {
                String origPoseString
                        = String.format("%.1f, %.1f, %.1f",
                                origPoint.getX().doubleValue(),
                                origPoint.getY().doubleValue(),
                                origPoint.getZ().doubleValue());
                this.jTextFieldTestPose.setText(origPoseString);
            }
        }

        replanStarted.set(false);
        return startCrclProgram(program);
    }

    public XFuture<Boolean> returnPart(String part) throws IOException {
        clearAll();
        Map<String, String> options = getTableOptions();
        replanFromIndex = 0;
        List<MiddleCommandType> cmds = new ArrayList<>();
        pddlActionToCrclGenerator.setOptions(options);
        pddlActionToCrclGenerator.returnPart(part, cmds);
        CRCLProgramType program = createEmptyProgram();
        jTextFieldIndex.setText(Integer.toString(replanFromIndex));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        program.getEndCanon().setCommandID(BigInteger.valueOf(cmds.size() + 2));

        replanStarted.set(false);
        return startCrclProgram(program);
    }

    Random random = new Random();
    PoseType testDropOffPose;

    public PoseType getTestDropOffPose() {
        return testDropOffPose;
    }

    /**
     * Add a position map.
     *
     * The position map is similar to a transform in that it may offset
     * positions output by the executor but may also be used to change scaling
     * or correct for non uniform distortions from the sensor system or
     * imperfect kinematic functions in the robot. Multiple position maps may be
     * stacked to account for different sources of error or transformation.
     *
     * @param pm position map to be added
     */
    public void addPositionMap(PositionMap pm) {
        positionMapJPanel1.addPositionMap(pm);
        pddlActionToCrclGenerator.setPositionMaps(getPositionMaps());
    }

    /**
     * Remove a previously added position map.
     * 
     * @param pm position map to be removed.
     */
    public void removePositionMap(PositionMap pm) {
        positionMapJPanel1.removePositionMap(pm);
        pddlActionToCrclGenerator.setPositionMaps(getPositionMaps());
    }

    /**
     * Modify the given pose by applying all of the currently added position maps.
     * 
     * @param poseIn the pose to correct or transform
     * @return pose after being corrected by all currently added position maps
     */
    public PoseType correctPose(PoseType poseIn) {
        PoseType pout = poseIn;
        for (PositionMap pm : getPositionMaps()) {
            pout = pm.correctPose(pout);
        }
        return pout;
    }

    /**
     * Modify the given point by applying all of the currently added position maps.
     * 
     * @param ptIn the point to correct or transform
     * @return point after being corrected by all currently added position maps
     */
    public PointType correctPoint(PointType ptIn) {
        PointType pout = ptIn;
        for (PositionMap pm : getPositionMaps()) {
            pout = pm.correctPoint(ptIn);
        }
        return pout;
    }

    /**
     * Apply inverses of currently added position maps in reverse order.
     * @param ptIn point to reverse correction
     * @return point in original vision/database coordinates
     */
    public PointType reverseCorrectPoint(PointType ptIn) {
        PointType pout = ptIn;
        List<PositionMap> l = getReversePositionMaps();
        for (PositionMap pm : l) {
            pout = pm.correctPoint(ptIn);
        }
        return pout;
    }

    public XFuture<Boolean> randomDropOff() {
        Map<String, String> options = getTableOptions();
        replanFromIndex = 0;
        List<MiddleCommandType> cmds = new ArrayList<>();
        pddlActionToCrclGenerator.setOptions(options);
        double xMin = Double.parseDouble(jTextFieldTestXMin.getText());
        double xMax = Double.parseDouble(jTextFieldTestXMax.getText());
        double yMin = Double.parseDouble(jTextFieldTestYMin.getText());
        double yMax = Double.parseDouble(jTextFieldTestYMax.getText());
        double x = (xMax - xMin) * random.nextDouble() + xMin;
        double y = (yMax - yMin) * random.nextDouble() + yMin;
        double z = Double.parseDouble(jTextFieldTestZ.getText());
        jTextFieldTestPose.setText(String.format("%.1f,%.1f,%.1f", x, y, z));
        PoseType origPose = pose(point(x, y, z), vector(1.0, 0.0, 0.0), vector(0.0, 0.0, -1.0));
        PointType offset = getPositionMaps().get(0).getOffset(x, y, 0);
        testDropOffPose = correctPose(origPose);

        pddlActionToCrclGenerator.placePartByPose(cmds, testDropOffPose);
        CRCLProgramType program = createEmptyProgram();
        jTextFieldIndex.setText(Integer.toString(replanFromIndex));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        program.getEndCanon().setCommandID(BigInteger.valueOf(cmds.size() + 2));

        String randomPoseString
                = String.format("%.1f, %.1f, %.1f",
                        testDropOffPose.getPoint().getX().doubleValue(),
                        testDropOffPose.getPoint().getY().doubleValue(),
                        testDropOffPose.getPoint().getZ().doubleValue());
        String origPoseString
                = String.format("%.1f, %.1f, %.1f",
                        origPose.getPoint().getX().doubleValue(),
                        origPose.getPoint().getY().doubleValue(),
                        origPose.getPoint().getZ().doubleValue());
        System.out.println("randomPoseString = " + randomPoseString);
        jTextFieldOffset.setText(String.format("%.1f,%.1f", offset.getX().doubleValue(), offset.getY().doubleValue()));
        jTextFieldAdjPose.setText(randomPoseString);
        this.jTextFieldTestPose.setText(origPoseString);
        replanStarted.set(false);
        return startCrclProgram(program);
    }

    private double gridTestCurrentX = 0;
    private double gridTestCurrentY = 0;
    private double gridTestMaxX = 1;
    private double gridTestMaxY = 1;

    private PointType getOffset(double x, double y, double z) {
        PointType out = point(x, y, z);
        for (PositionMap pm : getPositionMaps()) {
            out = pm.getOffset(out.getX().doubleValue(), out.getY().doubleValue(), out.getZ().doubleValue());
        }
        return out;
    }

    public void gridDropOff() throws IOException {
        if (gridTestCurrentY > gridTestMaxY + 0.001) {
            this.clearAll();
            return;
        }
        Map<String, String> options = getTableOptions();
        replanFromIndex = 0;
        List<MiddleCommandType> cmds = new ArrayList<>();
        pddlActionToCrclGenerator.setOptions(options);
        double xMin = Double.parseDouble(jTextFieldTestXMin.getText());
        double xMax = Double.parseDouble(jTextFieldTestXMax.getText());
        double yMin = Double.parseDouble(jTextFieldTestYMin.getText());
        double yMax = Double.parseDouble(jTextFieldTestYMax.getText());
        double x = (xMax - xMin) * (gridTestCurrentX / gridTestMaxX) + xMin;
        double y = (yMax - yMin) * (gridTestCurrentY / gridTestMaxY) + yMin;
        double z = Double.parseDouble(jTextFieldTestZ.getText());
        gridTestCurrentX += 1.0;
        if (gridTestCurrentX > gridTestMaxX + 0.001) {
            gridTestCurrentY += 1.0;
            gridTestCurrentX = 0.0;
        }
        System.out.println("gridTestCurrentX = " + gridTestCurrentX);
        System.out.println("gridTestCurrentY = " + gridTestCurrentY);
        PoseType origPose = pose(point(x, y, z), vector(1.0, 0.0, 0.0), vector(0.0, 0.0, -1.0));
        PointType offset = getOffset(x, y, z);
        testDropOffPose = correctPose(origPose);
        pddlActionToCrclGenerator.placePartByPose(cmds, testDropOffPose);
        CRCLProgramType program = createEmptyProgram();
        jTextFieldIndex.setText(Integer.toString(replanFromIndex));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        program.getEndCanon().setCommandID(BigInteger.valueOf(cmds.size() + 2));
        setCrclProgram(program);
        String gridPoseString
                = String.format("%.1f, %.1f, %.1f",
                        testDropOffPose.getPoint().getX().doubleValue(),
                        testDropOffPose.getPoint().getY().doubleValue(),
                        testDropOffPose.getPoint().getZ().doubleValue());
        String origPoseString
                = String.format("%.1f, %.1f, %.1f",
                        origPose.getPoint().getX().doubleValue(),
                        origPose.getPoint().getY().doubleValue(),
                        origPose.getPoint().getZ().doubleValue());
        System.out.println("gridPoseString = " + gridPoseString);
        jTextFieldOffset.setText(String.format("%.1f,%.1f,%.1f", offset.getX().doubleValue(), offset.getY().doubleValue(), offset.getZ().doubleValue()));
        jTextFieldAdjPose.setText(gridPoseString);
        this.jTextFieldTestPose.setText(origPoseString);
        replanStarted.set(false);
    }

    public XFuture<Boolean> randomPickup() {
        Map<String, String> options = getTableOptions();
        replanFromIndex = 0;
        List<MiddleCommandType> cmds = new ArrayList<>();
        pddlActionToCrclGenerator.setOptions(options);
        pddlActionToCrclGenerator.takePartByPose(cmds, testDropOffPose);
        CRCLProgramType program = createEmptyProgram();
        jTextFieldIndex.setText(Integer.toString(replanFromIndex));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        program.getEndCanon().setCommandID(BigInteger.valueOf(cmds.size() + 2));
        replanStarted.set(false);
        return startCrclProgram(program);
    }

    public XFuture<Boolean> lookForParts() {
        try {
            Map<String, String> options = getTableOptions();
            replanFromIndex = 0;
            List<PddlAction> lookForActionsList = new ArrayList<>();
            PddlAction lookForAction = new PddlAction("", "look-for-part",
                    new String[]{}, "cost");
            lookForActionsList.add(lookForAction);
            List<MiddleCommandType> cmds = pddlActionToCrclGenerator.generate(lookForActionsList, this.replanFromIndex, options);
            CRCLProgramType program = createEmptyProgram();
            jTextFieldIndex.setText(Integer.toString(replanFromIndex));
            program.getMiddleCommand().clear();
            program.getMiddleCommand().addAll(cmds);
            program.getEndCanon().setCommandID(BigInteger.valueOf(cmds.size() + 2));
            replanStarted.set(false);
            return startCrclProgram(program);
        } catch (IllegalStateException | SQLException ex) {
            Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
            XFuture<Boolean> future = new XFuture<>();
            future.completeExceptionally(ex);
            return future;
        }
    }

    private XFuture<Void> checkDbSupplierPublisher() throws IOException {
        XFuture<Void> ret = new XFuture<>();
        if (null != this.pddlActionToCrclGenerator && pddlActionToCrclGenerator.isConnected()) {
            ret.complete(null);
            return ret;
        }
        if (null != dbSetupSupplier) {
            try {
                dbSetupPublisher = dbSetupSupplier.call();
                dbSetupPublisher.addDbSetupListener(this::handleNewDbSetup);

            } catch (Exception ex) {
                Logger.getLogger(VisionToDBJPanel.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (null != dbSetupPublisher) {
            dbSetupPublisher.setDbSetup(new DbSetupBuilder().setup(dbSetupPublisher.getDbSetup()).connected(true).build());
            List<Future<?>> futures = dbSetupPublisher.notifyAllDbSetupListeners();
            return XFuture.runAsync(() -> {
                for (Future<?> f : futures) {
                    if (!f.isDone() && !f.isCancelled()) {
                        try {
                            f.get();

                        } catch (InterruptedException | ExecutionException ex) {
                            Logger.getLogger(PddlExecutorJPanel.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });
        } else {
            System.err.println("dbSetupPublisher == null");
        }
        ret.complete(null);
        return ret;
    }

    public void setOption(String key, String val) {
        TableModel model = jTableOptions.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            Object keyCheck = model.getValueAt(i, 0);
            if (keyCheck.equals(key)) {
                model.setValueAt(val, i, 1);
                break;
            }
        }
        pddlActionToCrclGenerator.setOptions(getTableOptions());
    }

    public Map<String, String> getTableOptions() {
        Map<String, String> options = new HashMap<>();
        TableModel model = jTableOptions.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            Object key = model.getValueAt(i, 0);
            Object val = model.getValueAt(i, 1);
            if (null != key && null != val) {
                options.put(key.toString(), val.toString());
            }
        }
        return options;
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAbort;
    private javax.swing.JButton jButtonClear;
    private javax.swing.JButton jButtonContRandomTest;
    private javax.swing.JButton jButtonContinue;
    private javax.swing.JButton jButtonGenerateAndRun;
    private javax.swing.JButton jButtonGenerateCRCL;
    private javax.swing.JButton jButtonGridTest;
    private javax.swing.JButton jButtonLoad;
    private javax.swing.JButton jButtonLoadPddlActionsFromFile;
    private javax.swing.JButton jButtonLookFor;
    private javax.swing.JButton jButtonNewLogFile;
    private javax.swing.JButton jButtonPause;
    private javax.swing.JButton jButtonPddlOutputViewEdit;
    private javax.swing.JButton jButtonPlacePart;
    private javax.swing.JButton jButtonRandDropOff;
    private javax.swing.JButton jButtonRecord;
    private javax.swing.JButton jButtonRecordFail;
    private javax.swing.JButton jButtonRecordLookForJoints;
    private javax.swing.JButton jButtonRecordSuccess;
    private javax.swing.JButton jButtonReset;
    private javax.swing.JButton jButtonReturn;
    private javax.swing.JButton jButtonSafeAbort;
    private javax.swing.JButton jButtonStep;
    private javax.swing.JButton jButtonStopRandomTest;
    private javax.swing.JButton jButtonTake;
    private javax.swing.JButton jButtonTest;
    private javax.swing.JButton jButtonTestPickup;
    private javax.swing.JCheckBox jCheckBoxDebug;
    private javax.swing.JCheckBox jCheckBoxEnableExternalControlPort;
    private javax.swing.JCheckBox jCheckBoxNeedLookFor;
    private javax.swing.JCheckBox jCheckBoxReplan;
    private javax.swing.JComboBox<String> jComboBoxManualObjectName;
    private javax.swing.JComboBox<String> jComboBoxManualSlotName;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanelInnerManualControl;
    private javax.swing.JPanel jPanelOuterManualControl;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPaneOptions;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTableCrclProgram;
    private javax.swing.JTable jTableOptions;
    private javax.swing.JTable jTablePddlOutput;
    private javax.swing.JTextArea jTextAreaExternalCommads;
    private javax.swing.JTextField jTextFieldAdjPose;
    private javax.swing.JTextField jTextFieldCurrentPart;
    private javax.swing.JTextField jTextFieldExternalControlPort;
    private javax.swing.JTextField jTextFieldGridSize;
    private javax.swing.JTextField jTextFieldIndex;
    private javax.swing.JTextField jTextFieldLogFilename;
    private javax.swing.JTextField jTextFieldOffset;
    private javax.swing.JTextField jTextFieldPddlOutputActions;
    private javax.swing.JTextField jTextFieldRandomDropoffCount;
    private javax.swing.JTextField jTextFieldRandomPickupCount;
    private javax.swing.JTextField jTextFieldRecordFailCount;
    private javax.swing.JTextField jTextFieldRecordSuccessCount;
    private javax.swing.JTextField jTextFieldSafeAbortCount;
    private javax.swing.JTextField jTextFieldSafeAbortRequestCount;
    private javax.swing.JTextField jTextFieldTestPose;
    private javax.swing.JTextField jTextFieldTestXMax;
    private javax.swing.JTextField jTextFieldTestXMin;
    private javax.swing.JTextField jTextFieldTestYMax;
    private javax.swing.JTextField jTextFieldTestYMin;
    private javax.swing.JTextField jTextFieldTestZ;
    private aprs.framework.pddl.executor.PositionMapJPanel positionMapJPanel1;
    // End of variables declaration//GEN-END:variables

    @Override
    public void loadProperties() throws IOException {
        if (null != propertiesFile && propertiesFile.exists()) {
            if (propertiesFile.isDirectory()) {
                System.err.println("Can not loadProperties file \"" + propertiesFile + "\" : It is a directory instead of text file.");
                return;
            }
            if (!propertiesFile.canRead()) {
                System.err.println("Can not loadProperties file \"" + propertiesFile + "\" : file is not readable.");
                return;
            }
            Properties props = new Properties();
            try (FileReader fr = new FileReader(propertiesFile)) {
                props.load(fr);
            }
            String output = props.getProperty(PDDLOUTPUT);
            if (null != output) {
                File f = new File(output);
                if (f.exists() && f.canRead() && !f.isDirectory()) {
                    loadActionsFile(f);
                    jTextFieldPddlOutputActions.setText(f.getCanonicalPath());
                } else {
                    String fullPath = propertiesFile.getParentFile().toPath().resolve(output).normalize().toString();
//                    System.out.println("fullPath = " + fullPath);
                    f = new File(fullPath);
                    if (f.exists() && f.canRead() && !f.isDirectory()) {
                        loadActionsFile(f);
                        jTextFieldPddlOutputActions.setText(f.getCanonicalPath());
                    } else {
                        String fullPath2 = propertiesFile.getParentFile().toPath().resolveSibling(output).normalize().toString();
//                        System.out.println("fullPath = " + fullPath2);
                        f = new File(fullPath2);
                        if (f.exists() && f.canRead() && !f.isDirectory()) {
                            jTextFieldPddlOutputActions.setText(f.getCanonicalPath());
                            loadActionsFile(f);
                        }
                    }
                }
            }
            String autostartString = props.getProperty(PDDLCRCLAUTOSTART);
            if (null != autostartString) {
                this.autoStart = Boolean.valueOf(autostartString);
            }
            for (String name : props.stringPropertyNames()) {
                if (!name.equals(PDDLCRCLAUTOSTART)
                        && !name.equals(PDDLOUTPUT)
                        && !name.equals(MANUAL_PART_NAMES)
                        && !name.equals(MANUAL_SLOT_NAMES)) {
                    DefaultTableModel model = (DefaultTableModel) jTableOptions.getModel();
                    boolean foundit = false;
                    for (int i = 0; i < model.getRowCount(); i++) {
                        String nameFromTable = model.getValueAt(i, 0).toString();
                        if (nameFromTable.equals(name)) {
                            model.setValueAt(props.getProperty(name), i, 1);
                            foundit = true;
                            break;
                        }
                    }
                    if (!foundit) {
                        model.addRow(new Object[]{name, props.getProperty(name)});
                    }
                }
            }
            String manualPartNames = props.getProperty(MANUAL_PART_NAMES, "");
            String pna[] = manualPartNames.split("[ \t,\\[\\]\\{\\}]+");
            DefaultComboBoxModel<String> cbm
                    = (DefaultComboBoxModel<String>) jComboBoxManualObjectName.getModel();
            cbm.removeAllElements();
            for (int i = 0; i < pna.length; i++) {
                if (null != pna[i] && pna[i].length() > 0
                        && !pna[i].equals("null")) {
                    cbm.addElement(pna[i]);
                }
            }

            String manualSlotNames = props.getProperty(MANUAL_SLOT_NAMES, "");
            String sna[] = manualSlotNames.split("[ \t,\\[\\]\\{\\}]+");
            cbm
                    = (DefaultComboBoxModel<String>) jComboBoxManualSlotName.getModel();
            cbm.removeAllElements();
            for (int i = 0; i < sna.length; i++) {
                if (null != sna[i] && sna[i].length() > 0
                        && !sna[i].equals("null")) {
                    cbm.addElement(sna[i]);
                }
            }
        }
    }

    @Override
    public void close() throws Exception {
    }

    private void handleNewDbSetup(DbSetup setup) {
        if (null != pddlActionToCrclGenerator) {
            pddlActionToCrclGenerator.accept(setup);
        }
    }

    private boolean needReplan = false;
    private int replanFromIndex = -1;
    private final AtomicBoolean replanStarted = new AtomicBoolean();

    javax.swing.Timer replanActionTimer = null;
    private final Runnable defaultReplanRunnable = new Runnable() {
        @Override
        public void run() {
//            Utils.runOnDispatchThread(PddlExecutorJPanel.this::generateCrclWithCatch);
            replanActionTimer
                    = new javax.swing.Timer(200, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            generateCrclWithCatch();
                        }
                    });
            replanActionTimer.setRepeats(false);
            replanActionTimer.start();
        }
    };

    private List<RunnableWithThrow> customRunnables = null;
    private int customRunnablesIndex = -1;

    private void runAllCustomRunnables() {

        if (null != customRunnables
                && customRunnablesIndex >= 0
                && customRunnables.size() > 0
                && customRunnablesIndex < customRunnables.size()) {
            try {
                System.out.println("customRunnablesIndex = " + customRunnablesIndex);
                RunnableWithThrow runnable = customRunnables.get(customRunnablesIndex);
                customRunnablesIndex = (customRunnablesIndex + 1) % customRunnables.size();
                if (null != runnable) {
                    Utils.runOnDispatchThreadWithCatch(runnable);
                }
            } catch (Exception ex) {
                Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
                clearAll();
            }
        }
    }
    private final Runnable customReplanRunnable = new Runnable() {
        @Override
        public void run() {
            if (null != customRunnables
                    && customRunnablesIndex >= 0
                    && customRunnables.size() > 0
                    && customRunnablesIndex < customRunnables.size()) {
                Utils.runOnDispatchThread(PddlExecutorJPanel.this::runAllCustomRunnables);
            }
        }
    };

    private Runnable replanRunnable = defaultReplanRunnable;

    private boolean debug;

    /**
     * Get the value of debug
     *
     * @return the value of debug
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Set the value of debug
     *
     * @param debug new value of debug
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
        this.pddlActionToCrclGenerator.setDebug(debug);
    }

    private boolean runningProgram = false;

    private int currentActionIndex = -1;

    public int getCurrentActionIndex() {
        return currentActionIndex;
    }

    private int actionIndexFromCrclLine(int crclLine) {
        if (null != crclIndexes) {
            for (int i = crclStartActionIndex; i < crclIndexes.length && i <= crclEndActionIndex; i++) {
                if (crclLine <= crclIndexes[i]) {
                    return i;
                }
            }
        }
        return crclEndActionIndex;
    }

    PddlAction actionFromIndex(int index) {
        if (null != actionsList && actionsList.size() >= index && index >= 0) {
            return actionsList.get(index);
        }
        return null;
    }

    @Override
    public void accept(PendantClientJPanel panel, int line, CRCLProgramType program, CRCLStatusType status) {
        CommandStateEnumType state = status.getCommandStatus().getCommandState();
        int sz = program.getMiddleCommand().size();
//        currentActionIndex = actionIndexFromCrclLine(line);
//        if (null != aprsJFrame) {
//            aprsJFrame.updateTitle();
//        }
        if (this.debug) {
            System.out.println("replanStarted = " + replanStarted);
            System.out.println("replanRunnable = " + replanRunnable);
            System.out.println("jCheckBoxReplan.isSelected() = " + jCheckBoxReplan.isSelected());
            System.out.println("sz = " + sz);
            System.out.println("line = " + line);
            System.out.println("state = " + state);
            System.out.println("crclProgName = " + crclProgName);
            System.out.println("lastCrclProgName = " + lastCrclProgName);
            System.out.println("program.getName() = " + program.getName());
        }
    }

    /**
     * @return the errorMap
     */
    public List<PositionMap> getPositionMaps() {
        return positionMapJPanel1.getPositionMaps();
    }

    public List<PositionMap> getReversePositionMaps() {
        return positionMapJPanel1.getReversePositionMaps();
    }

}
