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

import aprs.framework.system.AprsSystemInterface;
import aprs.framework.Utils;
import aprs.framework.Utils.RunnableWithThrow;
import static aprs.framework.Utils.autoResizeTableColWidths;
import static aprs.framework.Utils.autoResizeTableRowHeights;
import aprs.framework.database.DbSetup;
import aprs.framework.database.DbSetupBuilder;
import aprs.framework.database.DbSetupListener;
import aprs.framework.database.DbSetupPublisher;
import aprs.framework.database.PhysicalItem;
import aprs.framework.optaplanner.OpDisplayJPanel;
import aprs.framework.optaplanner.actionmodel.OpAction;
import aprs.framework.optaplanner.actionmodel.OpActionPlan;
import aprs.framework.optaplanner.actionmodel.score.EasyOpActionPlanScoreCalculator;
import aprs.framework.spvision.VisionToDBJPanel;
import crcl.base.CRCLCommandInstanceType;
import crcl.base.CRCLCommandType;
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
import crcl.ui.XFutureVoid;
import crcl.ui.client.PendantClientInner;
import crcl.ui.client.PendantClientJPanel;
import crcl.utils.CRCLException;
import crcl.utils.CRCLPosemath;
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
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.geom.Point2D;
import java.sql.Connection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import rcs.posemath.PmCartesian;
import rcs.posemath.PmException;
import rcs.posemath.PmRpy;
import static crcl.utils.CRCLPosemath.pose;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class PddlExecutorJPanel extends javax.swing.JPanel implements PddlExecutorDisplayInterface, PendantClientJPanel.ProgramLineListener {

    public static List<PddlAction> showActionsList(List<PddlAction> actionsIn) {
        JDialog diag = new JDialog();
        diag.setModal(true);
        PddlExecutorJPanel panel = new PddlExecutorJPanel();
        panel.loadActionsList(actionsIn);
        diag.add(panel);
        diag.pack();
        diag.setVisible(true);
        return panel.getActionsList();
    }

    /**
     * Creates new form ActionsToCrclJPanel
     */
    @SuppressWarnings("initialization")
    public PddlExecutorJPanel() {
        initComponents();
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
        jTablePddlOutput.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showPopup(e);
            }

            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    JPopupMenu jpmenu = new JPopupMenu("PDDL Action Menu ");
                    JMenuItem menuItem = new JMenuItem("Run Single");
                    menuItem.addActionListener(x -> runSingleRow());
                    jpmenu.add(menuItem);
                    jpmenu.setLocation(e.getPoint());
                    jpmenu.setVisible(true);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                showPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                showPopup(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                showPopup(e);
            }
        });
        this.pddlActionToCrclGenerator.setParentPddlExecutorJPanel(this);
        setToolOffsetTableModelListener();
        setTrayAttachOffsetTableModelListener();
    }

    @Nullable public String getSelectedToolName() {
        return pddlActionToCrclGenerator.getToolName();
    }

    @Nullable private String selectedToolNameFileName = null;

    /**
     * Get the value of selectedToolNameFileName
     *
     * @return the value of selectedToolNameFileName
     */
    @Nullable public String getSelectedToolNameFileName() {
        if (null == selectedToolNameFileName) {
            try {
                return getDefaultSelectedToolNameFile();
            } catch (IOException ex) {
                Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return selectedToolNameFileName;
    }

    /**
     * Set the value of selectedToolNameFileName
     *
     * @param selectedToolNameFileName new value of selectedToolNameFileName
     */
    public void setSelectedToolNameFileName(String selectedToolNameFileName) {
        this.selectedToolNameFileName = selectedToolNameFileName;
    }

    private String getDefaultSelectedToolNameFile() throws IOException {
        return propertiesFile.getName() + ".selectedToolName.txt";
    }

    @Nullable private String readSelectedToolNameFile() throws IOException {
        String filename = getSelectedToolNameFileName();
        if (null == filename || filename.length() < 1) {
            return null;
        }
        return readSelectedToolNameFile(filename);
    }

    @Nullable private String readSelectedToolNameFile(String filename) throws IOException {
        if (null == filename) {
            throw new IllegalArgumentException("filename == null");
        }
        File file = new File(propertiesFile.getParentFile(), filename);
        if (!file.exists() || !file.canRead()) {
            return null;
        }
        if (file.isDirectory()) {
            throw new IllegalStateException(filename + " is a directory.");
        }
        return readSelectedToolNameFile(file);
    }

    private String readSelectedToolNameFile(File file) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while (null != (line = br.readLine())) {
                line = line.trim();
                if (line.length() > 1 && !line.startsWith("#")) {
                    return line;
                }
            }
        }
        return "";
    }

    public void setSelectedToolName(String newToolName) {
        try {
            if (null == newToolName) {
                return;
            }
            jTextFieldCurrentToolName.setText(newToolName);
            String currentToolName = pddlActionToCrclGenerator.getToolName();
            if (!Objects.equals(currentToolName, newToolName)) {
                pddlActionToCrclGenerator.setToolName(newToolName);
            }
            PoseType newPose = pddlActionToCrclGenerator.getToolOffsetPose();
            if (null != newPose) {
                PmRpy rpy = CRCLPosemath.toPmRpy(newPose);
                PmCartesian tran = CRCLPosemath.toPmCartesian(newPose.getPoint());
                String offsetText
                        = String.format("X=%.3f,Y=%.3f,Z=%.3f,roll=%.3f,pitch=%.3f,yaw=%.3f",
                                tran.x, tran.y, tran.z,
                                Math.toDegrees(rpy.r), Math.toDegrees(rpy.p), Math.toDegrees(rpy.y));
                jTextFieldCurrentToolOffset.setText(offsetText);
            }
            String filename = getSelectedToolNameFileName();
            if (null == filename) {
                filename = getDefaultSelectedToolNameFile();
                setSelectedToolNameFileName(filename);
            }
            String curSavedToolName = readSelectedToolNameFile(filename);
            if (!Objects.equals(curSavedToolName, newToolName)) {
                try (PrintWriter pw = new PrintWriter(new FileWriter(new File(propertiesFile.getParentFile(), filename)))) {
                    pw.println(newToolName);
                } catch (IOException ex) {
                    Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, null, exception);
        }
    }

    public void setObtionsTableEntry(String key, String value) {
        DefaultTableModel model = (DefaultTableModel) jTableOptions.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            Object keyFromTable = model.getValueAt(i, 0);
            if (Objects.equals(keyFromTable, key)) {
                model.setValueAt(value, i, 1);
                return;
            }
        }
        model.addRow(new Object[]{key, value});
        pddlActionToCrclGenerator.setOptions(getTableOptions());
    }

    public void setLookForXYZ(double x, double y, double z) {
        try {
            String valueString = String.format("%.3f,%.3f,%.3f", x, y, z);
            Utils.runAndWaitOnDispatchThread("setLookForXYZ",
                    () -> setObtionsTableEntry("lookForXYZ", valueString));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void setToolOffsetTableModelListener() {
        jTableToolOffsets.getModel().addTableModelListener(toolOffsetsModelListener);
    }

    private void clearToolOffsetTableModelListener() {
        jTableToolOffsets.getModel().removeTableModelListener(toolOffsetsModelListener);
    }

    private void setTrayAttachOffsetTableModelListener() {
        jTableTrayAttachOffsets.getModel().addTableModelListener(trayAttachOffsetsModelListener);
    }

    private void clearTrayAttachOffsetTableModelListener() {
        jTableTrayAttachOffsets.getModel().removeTableModelListener(trayAttachOffsetsModelListener);
    }

    private class TrayAttachOffsetModelListenerClass implements TableModelListener {

        @Override
        public void tableChanged(TableModelEvent e) {
            Utils.autoResizeTableColWidths(jTableTrayAttachOffsets);
            saveTrayAttachOffsetPoseMap();
            loadTrayAttachOffsetsTableToMap();
        }
    }

    private final TableModelListener trayAttachOffsetsModelListener = new TrayAttachOffsetModelListenerClass();

    private class ToolOffsetModelListenerClass implements TableModelListener {

        @Override
        public void tableChanged(TableModelEvent e) {
            Utils.autoResizeTableColWidths(jTableToolOffsets);
            saveToolOffsetPoseMap();
            loadToolOffsetsTableToMap();
        }
    }

    private final TableModelListener toolOffsetsModelListener = new ToolOffsetModelListenerClass();

    private void setToolHolderContentsTableModelListener() {
        jTableHolderContents.getModel().addTableModelListener(toolHolderContentsModelListener);
    }

    private void clearToolHolderContentsTableModelListener() {
        jTableHolderContents.getModel().removeTableModelListener(toolHolderContentsModelListener);
    }

    private class ToolHolderContentsModelListenerClass implements TableModelListener {

        @Override
        public void tableChanged(TableModelEvent e) {
            Utils.autoResizeTableColWidths(jTableHolderContents);
            saveToolHolderContentsMap();
        }
    }

    private final TableModelListener toolHolderContentsModelListener = new ToolHolderContentsModelListenerClass();

    private void runSingleRow() {
        this.aprsJFrame.abortCrclProgram();
        int row = jTablePddlOutput.getSelectedRow();
        currentActionIndex = row;
        setReplanFromIndex(row);
        stepping = true;
        continueActionListPrivate();
    }

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

    private void handlePlacePartCompleted(PddlActionToCrclGenerator.PlacePartInfo ppi) {
        if (null == ppi) {
            throw new IllegalArgumentException("ppi == null");
        }
        int sarc = safeAbortRequestCount.get();
        int ppiSarc = ppi.getStartSafeAbortRequestCount();
        boolean requestCountDiffer = ppiSarc != sarc;
        boolean aborting = aprsJFrame.isAborting();
        aprsJFrame.logEvent("handlePlacePartCompleted", "requestCountDiffer=" + requestCountDiffer + ",aboring=" + aborting + ", ppi.getPddlActionIndex()=" + ppi.getPddlActionIndex() + ",action=" + ppi.getAction().asPddlLine());
        if (requestCountDiffer || aborting) {
            pddlActionToCrclGenerator.takeSnapshots("exec", "safeAbortRequested" + sarc + ":" + safeAboutCount.get() + ".ppi=" + ppi, null, null);
            CrclCommandWrapper wrapper = ppi.getWrapper();
            if (null == wrapper) {
                throw new IllegalArgumentException("ppi.getWrapper() == null");
            }
            if (null == wrapper.getCurProgram()) {
                throw new IllegalArgumentException(" ppi.getWrapper().getCurProgram() == null");
            }
            List<MiddleCommandType> l = wrapper.getCurProgram().getMiddleCommand();
            int programIndex = wrapper.getCurProgramIndex();
            while (l.size() > programIndex + 1) {
                l.remove(programIndex + 1);
            }
            pddlActionToCrclGenerator.addMoveToLookForPosition(l, false);
            setReplanFromIndex(ppi.getPddlActionIndex() + 1, true);
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

    private long lastActionMillis = -1;

    private void setCost(int index, double cost) {
        DefaultTableModel m = (DefaultTableModel) jTablePddlOutput.getModel();
        if (m.getRowCount() > index) {
            m.setValueAt(cost, index, 5);
        }
    }

    private void handleActionCompleted(PddlActionToCrclGenerator.ActionCallbackInfo actionInfo) {
        if (currentActionIndex != actionInfo.getActionIndex()) {
            LOGGER.log(Level.FINE, "(currentActionIndex != actionInfo.getActionIndex())");
            LOGGER.log(Level.FINE, "actionInfo = " + actionInfo);
            LOGGER.log(Level.FINE, "currentActionIndex = " + currentActionIndex);
        }
        if (currentActionIndex < actionInfo.getActionIndex() + 1) {
            currentActionIndex = actionInfo.getActionIndex() + 1;
            updateSelectionInterval();
            if (null != aprsJFrame) {
                Utils.runOnDispatchThread(aprsJFrame::updateTitle);
                if (stepping) {
                    pause();
                }
            }
            long nowMillis = System.currentTimeMillis();
            if (lastActionMillis > 0 && lastActionMillis < nowMillis) {
                long diff = nowMillis - lastActionMillis;
                final double cost = diff * 1e-3;
                final int index = actionInfo.getActionIndex();
                Utils.runOnDispatchThread(() -> this.setCost(index, cost));
            }
            lastActionMillis = nowMillis;
        }
    }

    public void updateSelectionInterval() {
        int startIndex = Math.max(0, currentActionIndex);
        int endIndex = Math.max(startIndex, getReplanFromIndex() - 1);
        jTablePddlOutput.getSelectionModel().setSelectionInterval(startIndex, endIndex);
        jTablePddlOutput.scrollRectToVisible(new Rectangle(jTablePddlOutput.getCellRect(startIndex, 0, true)));
        if (currentActionIndex > 0 && currentActionIndex < jTablePddlOutput.getRowCount()) {
            Object o = jTablePddlOutput.getValueAt(startIndex, 1);
            if (o instanceof Integer) {
                int crclIndex = ((Integer) o) - 2;
                if (crclIndex > 0 && crclIndex < jTableCrclProgram.getRowCount()) {
                    jTableCrclProgram.getSelectionModel().setSelectionInterval(crclIndex, crclIndex);
                    jTableCrclProgram.scrollRectToVisible(new Rectangle(jTableCrclProgram.getCellRect(crclIndex, 0, true)));
                }
            }
        }
    }

    private static Object @Nullable [] getTableRow(JTable table, int row) {
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

    /**
     * Get the value of externalGetPoseFunction
     *
     * @return the value of externalGetPoseFunction
     */
    public PddlActionToCrclGenerator.@Nullable PoseProvider getExternalPoseProvider() {
        return pddlActionToCrclGenerator.getExternalPoseProvider();
    }

    /**
     * Set the value of externalGetPoseFunction
     *
     * @param externalGetPoseFunction new value of externalGetPoseFunction
     */
    public void setExternalPoseProvider(PddlActionToCrclGenerator.PoseProvider externalGetPoseFunction) {
        this.pddlActionToCrclGenerator.setExternalPoseProvider(externalGetPoseFunction);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "rawtypes", "nullness", "deprecation"})
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel6 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTablePddlOutput = new javax.swing.JTable();
        jButtonLoadPddlActionsFromFile = new javax.swing.JButton();
        jTextFieldPddlOutputActions = new javax.swing.JTextField();
        jButtonLoad = new javax.swing.JButton();
        jButtonGenerateCRCL = new javax.swing.JButton();
        jButtonPddlOutputViewEdit = new javax.swing.JButton();
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
        jPanelToolChange = new javax.swing.JPanel();
        jButtonGotoToolChangerApproach = new javax.swing.JButton();
        jLabel13 = new javax.swing.JLabel();
        jTextFieldToolChangerApproachZOffset = new javax.swing.JTextField();
        jButtonGotoToolChangerPose = new javax.swing.JButton();
        jButtonDropTool = new javax.swing.JButton();
        jButtonPickupTool = new javax.swing.JButton();
        jTabbedPaneToolChangeInner = new javax.swing.JTabbedPane();
        jScrollPaneHolderContents = new javax.swing.JScrollPane();
        jTableHolderContents = new javax.swing.JTable();
        jPanelToolHolderPositions = new javax.swing.JPanel();
        jScrollPaneToolHolderPositions = new javax.swing.JScrollPane();
        jTableToolHolderPositions = new javax.swing.JTable();
        jButtonRecordToolHolderPose = new javax.swing.JButton();
        jButtonRecordToolHolderApproach = new javax.swing.JButton();
        jButtonDeleteToolHolderPose = new javax.swing.JButton();
        jButtonAddToolHolderPose = new javax.swing.JButton();
        jPanelToolOffsets = new javax.swing.JPanel();
        jButtonAddToolOffset = new javax.swing.JButton();
        jButtonDeleteToolOffset = new javax.swing.JButton();
        jScrollPaneToolOffsets = new javax.swing.JScrollPane();
        jTableToolOffsets = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jButtonAddTrayAttach = new javax.swing.JButton();
        jButtonDeleteTrayAttach = new javax.swing.JButton();
        jScrollPaneToolOffsets1 = new javax.swing.JScrollPane();
        jTableTrayAttachOffsets = new javax.swing.JTable();
        jLabel7 = new javax.swing.JLabel();
        jTextFieldCurrentToolName = new javax.swing.JTextField();
        jButtonSetCurrentTool = new javax.swing.JButton();
        jLabel21 = new javax.swing.JLabel();
        jTextFieldCurrentToolOffset = new javax.swing.JTextField();
        jPanelContainerPositionMap = new javax.swing.JPanel();
        positionMapJPanel1 = new aprs.framework.pddl.executor.PositionMapJPanel();
        jScrollPaneExternalControl = new javax.swing.JScrollPane();
        jPanelExternalControl = new javax.swing.JPanel();
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
        jPanelCrcl = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableCrclProgram = new javax.swing.JTable();
        jPanelContainerPoseCache = new javax.swing.JPanel();
        jScrollPanePositionTable = new javax.swing.JScrollPane();
        jTablePositionCache = new javax.swing.JTable();
        jButtonClearPoseCache = new javax.swing.JButton();
        jButtonUpdatePoseCache = new javax.swing.JButton();
        jPanelOpOuter = new javax.swing.JPanel();
        opDisplayJPanelInput = new aprs.framework.optaplanner.OpDisplayJPanel();
        opDisplayJPanelSolution = new aprs.framework.optaplanner.OpDisplayJPanel();
        jButtonClear = new javax.swing.JButton();
        jCheckBoxDebug = new javax.swing.JCheckBox();
        jButtonAbort = new javax.swing.JButton();
        jButtonGenerateAndRun = new javax.swing.JButton();
        jLabel17 = new javax.swing.JLabel();
        jTextFieldCurrentPart = new javax.swing.JTextField();
        jButtonStep = new javax.swing.JButton();
        jButtonContinue = new javax.swing.JButton();
        jButtonPause = new javax.swing.JButton();
        jCheckBoxForceFakeTake = new javax.swing.JCheckBox();
        jCheckBoxEnableOptaPlanner = new javax.swing.JCheckBox();

        jLabel6.setText("Pddl Output Actions");

        jTablePddlOutput.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "#", "CRCLIndex", "Label", "Type", "Args", "Time/Cost", "TakenPart"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.String.class
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
                {"testTransSpeed", "50.0"},
                {"fastTransSpeed", "200.0"},
                {"slowTransSpeed", "75"},
                {"verySlowTransSpeed", "25"},
                {"lookDwellTime", "5.0"},
                {"firstLookDwellTime", "5.0"},
                {"lastLookDwellTime", "1.0"},
                {"skipLookDwellTime", "5.0"},
                {"afterMoveToLookForDwellTime", "5.0"},
                {"rotSpeed", "30.0"},
                {"placePartSlotArgIndex", "0"},
                {"takePartArgIndex", "0"},
                {"settleDwellTime", "0.1"},
                {"useJointLookFor", "false"},
                {"jointSpeed", "5.0"},
                {"jointAccel", "100.0"},
                {"takeSnapshots", "false"},
                {"doInspectKit", "false"},
                {"kitInspectDistThreshold", "20.0"},
                {"requireNewPoses", "false"},
                {"visionCycleNewDiffThreshold", "3"},
                {"pauseInsteadOfRecover", "false"},
                {"skipMissingParts", "false"}
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
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBoxManualObjectName, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonTake)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonLookFor)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonTest)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonReturn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonRecord)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonPlacePart)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel20)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBoxManualSlotName, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE))
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
                        .addComponent(jButtonContRandomTest)
                        .addGap(90, 90, 90)
                        .addComponent(jButtonStopRandomTest)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonRecordLookForJoints))
                    .addGroup(jPanelInnerManualControlLayout.createSequentialGroup()
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldGridSize, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonGridTest)))
                .addContainerGap(239, Short.MAX_VALUE))
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
                    .addComponent(jButtonTest)
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelInnerManualControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(jTextFieldGridSize, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonGridTest))
                .addContainerGap(87, Short.MAX_VALUE))
        );

        jScrollPane2.setViewportView(jPanelInnerManualControl);

        javax.swing.GroupLayout jPanelOuterManualControlLayout = new javax.swing.GroupLayout(jPanelOuterManualControl);
        jPanelOuterManualControl.setLayout(jPanelOuterManualControlLayout);
        jPanelOuterManualControlLayout.setHorizontalGroup(
            jPanelOuterManualControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelOuterManualControlLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2)
                .addContainerGap())
        );
        jPanelOuterManualControlLayout.setVerticalGroup(
            jPanelOuterManualControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelOuterManualControlLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Manual Pickup Return", jPanelOuterManualControl);

        jButtonGotoToolChangerApproach.setText("Goto Tool Changer Approach");
        jButtonGotoToolChangerApproach.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGotoToolChangerApproachActionPerformed(evt);
            }
        });

        jLabel13.setText("Z Offset:");

        jTextFieldToolChangerApproachZOffset.setText("150.0");
        jTextFieldToolChangerApproachZOffset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldToolChangerApproachZOffsetActionPerformed(evt);
            }
        });

        jButtonGotoToolChangerPose.setText("Goto Tool Changer Pose");
        jButtonGotoToolChangerPose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGotoToolChangerPoseActionPerformed(evt);
            }
        });

        jButtonDropTool.setText("Drop Tool");
        jButtonDropTool.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDropToolActionPerformed(evt);
            }
        });

        jButtonPickupTool.setText("Pickup Tool");
        jButtonPickupTool.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPickupToolActionPerformed(evt);
            }
        });

        jTableHolderContents.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Holder Position Name", "Contents", "Possible Contents", "Comment"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPaneHolderContents.setViewportView(jTableHolderContents);

        jTabbedPaneToolChangeInner.addTab("Holder Contents", jScrollPaneHolderContents);

        jTableToolHolderPositions.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "X (mm)", "Y (mm)", "Z (mm)", "Rx (deg)", "Ry (deg)", "Rz (deg)", "Approach", "Joints"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPaneToolHolderPositions.setViewportView(jTableToolHolderPositions);

        jButtonRecordToolHolderPose.setText("Record Tool Holder Pose");
        jButtonRecordToolHolderPose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRecordToolHolderPoseActionPerformed(evt);
            }
        });

        jButtonRecordToolHolderApproach.setText("Record Tool Holder Approach");
        jButtonRecordToolHolderApproach.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRecordToolHolderApproachActionPerformed(evt);
            }
        });

        jButtonDeleteToolHolderPose.setText("Delete Tool Holder Pose");
        jButtonDeleteToolHolderPose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteToolHolderPoseActionPerformed(evt);
            }
        });

        jButtonAddToolHolderPose.setText("Add Tool Holder Pose");
        jButtonAddToolHolderPose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddToolHolderPoseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelToolHolderPositionsLayout = new javax.swing.GroupLayout(jPanelToolHolderPositions);
        jPanelToolHolderPositions.setLayout(jPanelToolHolderPositionsLayout);
        jPanelToolHolderPositionsLayout.setHorizontalGroup(
            jPanelToolHolderPositionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelToolHolderPositionsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButtonRecordToolHolderPose)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonRecordToolHolderApproach)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonAddToolHolderPose)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonDeleteToolHolderPose)
                .addContainerGap(546, Short.MAX_VALUE))
            .addGroup(jPanelToolHolderPositionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanelToolHolderPositionsLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jScrollPaneToolHolderPositions, javax.swing.GroupLayout.DEFAULT_SIZE, 1226, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        jPanelToolHolderPositionsLayout.setVerticalGroup(
            jPanelToolHolderPositionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelToolHolderPositionsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelToolHolderPositionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonRecordToolHolderPose)
                    .addComponent(jButtonRecordToolHolderApproach)
                    .addComponent(jButtonDeleteToolHolderPose)
                    .addComponent(jButtonAddToolHolderPose))
                .addContainerGap())
            .addGroup(jPanelToolHolderPositionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelToolHolderPositionsLayout.createSequentialGroup()
                    .addGap(40, 40, 40)
                    .addComponent(jScrollPaneToolHolderPositions, javax.swing.GroupLayout.DEFAULT_SIZE, 115, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        jTabbedPaneToolChangeInner.addTab("Holder Positions", jPanelToolHolderPositions);

        jButtonAddToolOffset.setText("Add Tool");
        jButtonAddToolOffset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddToolOffsetActionPerformed(evt);
            }
        });

        jButtonDeleteToolOffset.setText("Delete Tool");
        jButtonDeleteToolOffset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteToolOffsetActionPerformed(evt);
            }
        });

        jTableToolOffsets.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ToolName", "X (mm)", "Y (mm)", "Z (mm)", "Rx (deg)", "Ry (deg)", "Rz (deg)", "Comment"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPaneToolOffsets.setViewportView(jTableToolOffsets);

        javax.swing.GroupLayout jPanelToolOffsetsLayout = new javax.swing.GroupLayout(jPanelToolOffsets);
        jPanelToolOffsets.setLayout(jPanelToolOffsetsLayout);
        jPanelToolOffsetsLayout.setHorizontalGroup(
            jPanelToolOffsetsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelToolOffsetsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButtonAddToolOffset)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonDeleteToolOffset)
                .addContainerGap(1059, Short.MAX_VALUE))
            .addGroup(jPanelToolOffsetsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanelToolOffsetsLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jScrollPaneToolOffsets, javax.swing.GroupLayout.DEFAULT_SIZE, 1226, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        jPanelToolOffsetsLayout.setVerticalGroup(
            jPanelToolOffsetsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelToolOffsetsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelToolOffsetsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonAddToolOffset)
                    .addComponent(jButtonDeleteToolOffset))
                .addContainerGap())
            .addGroup(jPanelToolOffsetsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelToolOffsetsLayout.createSequentialGroup()
                    .addGap(42, 42, 42)
                    .addComponent(jScrollPaneToolOffsets, javax.swing.GroupLayout.DEFAULT_SIZE, 113, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        jTabbedPaneToolChangeInner.addTab("Tool Offsets", jPanelToolOffsets);

        jButtonAddTrayAttach.setText("Add");
        jButtonAddTrayAttach.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddTrayAttachActionPerformed(evt);
            }
        });

        jButtonDeleteTrayAttach.setText("Delete");

        jTableTrayAttachOffsets.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "TrayName", "X (mm)", "Y (mm)", "Z (mm)", "Rx (deg)", "Ry (deg)", "Rz (deg)", "Comment"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPaneToolOffsets1.setViewportView(jTableTrayAttachOffsets);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPaneToolOffsets1, javax.swing.GroupLayout.DEFAULT_SIZE, 1226, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jButtonAddTrayAttach)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonDeleteTrayAttach)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonAddTrayAttach)
                    .addComponent(jButtonDeleteTrayAttach))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPaneToolOffsets1, javax.swing.GroupLayout.DEFAULT_SIZE, 115, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPaneToolChangeInner.addTab("Tray Attach Locations", jPanel1);

        jLabel7.setText("Current Tool Name: ");

        jTextFieldCurrentToolName.setEditable(false);

        jButtonSetCurrentTool.setText("Set Current Tool");
        jButtonSetCurrentTool.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSetCurrentToolActionPerformed(evt);
            }
        });

        jLabel21.setText("Offset: ");

        jTextFieldCurrentToolOffset.setEditable(false);

        javax.swing.GroupLayout jPanelToolChangeLayout = new javax.swing.GroupLayout(jPanelToolChange);
        jPanelToolChange.setLayout(jPanelToolChangeLayout);
        jPanelToolChangeLayout.setHorizontalGroup(
            jPanelToolChangeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelToolChangeLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelToolChangeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPaneToolChangeInner)
                    .addGroup(jPanelToolChangeLayout.createSequentialGroup()
                        .addComponent(jButtonGotoToolChangerPose)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel13)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldToolChangerApproachZOffset, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonGotoToolChangerApproach)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonDropTool)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonPickupTool)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanelToolChangeLayout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldCurrentToolName, javax.swing.GroupLayout.PREFERRED_SIZE, 199, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonSetCurrentTool)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel21)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldCurrentToolOffset)))
                .addContainerGap())
        );
        jPanelToolChangeLayout.setVerticalGroup(
            jPanelToolChangeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelToolChangeLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelToolChangeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonGotoToolChangerApproach)
                    .addComponent(jLabel13)
                    .addComponent(jTextFieldToolChangerApproachZOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonGotoToolChangerPose)
                    .addComponent(jButtonDropTool)
                    .addComponent(jButtonPickupTool))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelToolChangeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jTextFieldCurrentToolName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonSetCurrentTool)
                    .addComponent(jLabel21)
                    .addComponent(jTextFieldCurrentToolOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPaneToolChangeInner, javax.swing.GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Tool Change", jPanelToolChange);

        javax.swing.GroupLayout jPanelContainerPositionMapLayout = new javax.swing.GroupLayout(jPanelContainerPositionMap);
        jPanelContainerPositionMap.setLayout(jPanelContainerPositionMapLayout);
        jPanelContainerPositionMapLayout.setHorizontalGroup(
            jPanelContainerPositionMapLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelContainerPositionMapLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(positionMapJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 1168, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelContainerPositionMapLayout.setVerticalGroup(
            jPanelContainerPositionMapLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelContainerPositionMapLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(positionMapJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Error Map", jPanelContainerPositionMap);

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

        javax.swing.GroupLayout jPanelExternalControlLayout = new javax.swing.GroupLayout(jPanelExternalControl);
        jPanelExternalControl.setLayout(jPanelExternalControlLayout);
        jPanelExternalControlLayout.setHorizontalGroup(
            jPanelExternalControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelExternalControlLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelExternalControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 1162, Short.MAX_VALUE)
                    .addGroup(jPanelExternalControlLayout.createSequentialGroup()
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
        jPanelExternalControlLayout.setVerticalGroup(
            jPanelExternalControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelExternalControlLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelExternalControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
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

        jScrollPaneExternalControl.setViewportView(jPanelExternalControl);

        jTabbedPane1.addTab("External Control", jScrollPaneExternalControl);

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

        javax.swing.GroupLayout jPanelCrclLayout = new javax.swing.GroupLayout(jPanelCrcl);
        jPanelCrcl.setLayout(jPanelCrclLayout);
        jPanelCrclLayout.setHorizontalGroup(
            jPanelCrclLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCrclLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addContainerGap())
        );
        jPanelCrclLayout.setVerticalGroup(
            jPanelCrclLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCrclLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("CRCL", jPanelCrcl);

        jTablePositionCache.setAutoCreateRowSorter(true);
        jTablePositionCache.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "X", "Y", "Z", "Roll", "Pitch", "Yaw", "Comment"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPanePositionTable.setViewportView(jTablePositionCache);

        jButtonClearPoseCache.setText("Clear Cache");
        jButtonClearPoseCache.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonClearPoseCacheActionPerformed(evt);
            }
        });

        jButtonUpdatePoseCache.setText("Update Cache");
        jButtonUpdatePoseCache.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonUpdatePoseCacheActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelContainerPoseCacheLayout = new javax.swing.GroupLayout(jPanelContainerPoseCache);
        jPanelContainerPoseCache.setLayout(jPanelContainerPoseCacheLayout);
        jPanelContainerPoseCacheLayout.setHorizontalGroup(
            jPanelContainerPoseCacheLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelContainerPoseCacheLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButtonClearPoseCache)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonUpdatePoseCache)
                .addContainerGap(1034, Short.MAX_VALUE))
            .addGroup(jPanelContainerPoseCacheLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanelContainerPoseCacheLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jScrollPanePositionTable, javax.swing.GroupLayout.DEFAULT_SIZE, 1238, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        jPanelContainerPoseCacheLayout.setVerticalGroup(
            jPanelContainerPoseCacheLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelContainerPoseCacheLayout.createSequentialGroup()
                .addContainerGap(237, Short.MAX_VALUE)
                .addGroup(jPanelContainerPoseCacheLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonClearPoseCache)
                    .addComponent(jButtonUpdatePoseCache))
                .addContainerGap())
            .addGroup(jPanelContainerPoseCacheLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelContainerPoseCacheLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jScrollPanePositionTable, javax.swing.GroupLayout.DEFAULT_SIZE, 227, Short.MAX_VALUE)
                    .addGap(38, 38, 38)))
        );

        jTabbedPane1.addTab("Pose Cache", jPanelContainerPoseCache);

        opDisplayJPanelInput.setLabel("Input");
        opDisplayJPanelInput.setLabelFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        opDisplayJPanelInput.setLabelPos(new java.awt.Point(200, 20));

        javax.swing.GroupLayout opDisplayJPanelInputLayout = new javax.swing.GroupLayout(opDisplayJPanelInput);
        opDisplayJPanelInput.setLayout(opDisplayJPanelInputLayout);
        opDisplayJPanelInputLayout.setHorizontalGroup(
            opDisplayJPanelInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 578, Short.MAX_VALUE)
        );
        opDisplayJPanelInputLayout.setVerticalGroup(
            opDisplayJPanelInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        opDisplayJPanelSolution.setLabel("Output");
        opDisplayJPanelSolution.setLabelFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        opDisplayJPanelSolution.setLabelPos(new java.awt.Point(200, 20));

        javax.swing.GroupLayout opDisplayJPanelSolutionLayout = new javax.swing.GroupLayout(opDisplayJPanelSolution);
        opDisplayJPanelSolution.setLayout(opDisplayJPanelSolutionLayout);
        opDisplayJPanelSolutionLayout.setHorizontalGroup(
            opDisplayJPanelSolutionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 578, Short.MAX_VALUE)
        );
        opDisplayJPanelSolutionLayout.setVerticalGroup(
            opDisplayJPanelSolutionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 260, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanelOpOuterLayout = new javax.swing.GroupLayout(jPanelOpOuter);
        jPanelOpOuter.setLayout(jPanelOpOuterLayout);
        jPanelOpOuterLayout.setHorizontalGroup(
            jPanelOpOuterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelOpOuterLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(opDisplayJPanelInput, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(opDisplayJPanelSolution, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelOpOuterLayout.setVerticalGroup(
            jPanelOpOuterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelOpOuterLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelOpOuterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(opDisplayJPanelSolution, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(opDisplayJPanelInput, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jTabbedPane1.addTab("OptaPlanner", jPanelOpOuter);

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
        jTextFieldCurrentPart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldCurrentPartActionPerformed(evt);
            }
        });

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

        jCheckBoxForceFakeTake.setText("Force Fake Take");
        jCheckBoxForceFakeTake.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxForceFakeTakeActionPerformed(evt);
            }
        });

        jCheckBoxEnableOptaPlanner.setText("Enable OptaPlanner");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane4)
                    .addGroup(layout.createSequentialGroup()
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
                        .addComponent(jCheckBoxForceFakeTake)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxEnableOptaPlanner)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jCheckBoxDebug)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonAbort)
                        .addGap(11, 11, 11))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
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
                                .addComponent(jButtonClear))
                            .addGroup(layout.createSequentialGroup()
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
                                .addComponent(jTextFieldCurrentPart, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
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
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 183, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxNeedLookFor)
                    .addComponent(jTextFieldIndex, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel17)
                    .addComponent(jTextFieldCurrentPart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(jCheckBoxReplan))
                .addGap(1, 1, 1)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonPause)
                    .addComponent(jButtonStep)
                    .addComponent(jButtonContinue)
                    .addComponent(jButtonGenerateAndRun)
                    .addComponent(jButtonGenerateCRCL)
                    .addComponent(jCheckBoxForceFakeTake)
                    .addComponent(jCheckBoxDebug)
                    .addComponent(jButtonAbort)
                    .addComponent(jCheckBoxEnableOptaPlanner))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    @Override
    public void browseActionsFile() throws IOException {
        String text = jTextFieldPddlOutputActions.getText();
        File actionsFile = null;
        File actionsFileParent = null;
        if (null != text) {
            actionsFile = new File(text);
            if (null != actionsFile) {
                actionsFileParent = actionsFile.getParentFile();
            }
        }

        JFileChooser chooser = null;
        if (null != actionsFileParent) {
            chooser = new JFileChooser(actionsFileParent);
        } else {
            chooser = new JFileChooser();
        }
        if (null != propertiesFile) {
            File propertiesFileParent = propertiesFile.getParentFile();
            if (null != propertiesFileParent) {
                chooser.setCurrentDirectory(propertiesFileParent);
            }
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            jTextFieldPddlOutputActions.setText(f.getCanonicalPath());
            saveProperties();
        }
    }

    private final AtomicInteger actionSetsCompleted = new AtomicInteger();
    private final AtomicInteger actionSetsStarted = new AtomicInteger();

    private AtomicInteger doingActionsStarted = new AtomicInteger();
    private AtomicInteger doingActionsFinished = new AtomicInteger();

    private boolean isRunningProgram() {
        return runningProgramFuture != null
                && !runningProgramFuture.isCancelled()
                && !runningProgramFuture.isDone()
                && !runningProgramFuture.isCompletedExceptionally();
    }

    private boolean isContinuingActions() {
        return lastContinueActionFuture != null
                && !lastContinueActionFuture.isCancelled()
                && !lastContinueActionFuture.isDone()
                && !lastContinueActionFuture.isCompletedExceptionally();
    }

    public boolean isDoingActions() {
        return doingActionsStarted.get() > doingActionsFinished.get()
                || isRunningProgram() || isContinuingActions();
    }

    public boolean doActions(String comment, int startAbortCount) {
        final int start = doingActionsStarted.incrementAndGet();
        this.abortProgram();
        try {
            setReplanFromIndex(0);
            actionSetsStarted.incrementAndGet();
            autoStart = true;
            lastActionMillis = System.currentTimeMillis();
            jCheckBoxReplan.setSelected(true);
            if (null != runningProgramFuture) {
                runningProgramFuture.cancel(true);
            }
            boolean ret = generateCrcl(comment, startAbortCount);
            if (ret && atLastAction()) {
                actionSetsCompleted.set(actionSetsStarted.get());
            }
            if (ret && !checkSafeAbort(startAbortCount)) {
                warnIfNewActionsNotReady();
            }
            return ret;
        } catch (CRCLException | PmException | IOException | IllegalStateException | SQLException | JAXBException | InterruptedException | ExecutionException | PendantClientInner.ConcurrentBlockProgramsException ex) {
            LOGGER.log(Level.SEVERE, "Exception in doActions(" + comment + "," + startAbortCount + ")", ex);
            abortProgram();
            showExceptionInProgram(ex);
            throw new RuntimeException("Exception in doActions(" + comment + "," + startAbortCount + "):" + ex.getMessage(), ex);
        } finally {
            doingActionsFinished.incrementAndGet();
        }
    }

    public int getActionSetsCompleted() {
        return actionSetsCompleted.get();
    }

    public XFuture<Boolean> startActions() {
        this.abortProgram();
        try {
            setReplanFromIndex(0);
            autoStart = true;
            lastActionMillis = System.currentTimeMillis();
            jCheckBoxReplan.setSelected(true);
            if (null != runningProgramFuture) {
                runningProgramFuture.cancel(true);
            }
            runningProgramFuture = null;
            XFuture<Boolean> ret = generateCrclAsync()
                    .thenApply(x -> {
                        if (x && atLastAction()) {
                            actionSetsCompleted.set(actionSetsStarted.get());
                        }
                        return x;
                    });
            runningProgramFuture = ret;
            return ret;
        } catch (IOException | IllegalStateException | SQLException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            abortProgram();
            throw new RuntimeException(ex);
        }
    }

    private final PddlActionToCrclGenerator pddlActionToCrclGenerator = new PddlActionToCrclGenerator();

    public PddlActionToCrclGenerator getPddlActionToCrclGenerator() {
        return pddlActionToCrclGenerator;
    }

//    public void setPddlActionToCrclGenerator(PddlActionToCrclGenerator pddlActionToCrclGenerator) {
//        this.pddlActionToCrclGenerator = pddlActionToCrclGenerator;
//    }
    private final List<PddlAction> actionsList = Collections.synchronizedList(new ArrayList<>());
    private volatile List<PddlAction> readOnlyActionsList = Collections.unmodifiableList(new ArrayList<>(actionsList));

    @Nullable
    private volatile Thread resetReadOnlyActionsListThread = null;
    private volatile StackTraceElement resetReadOnlyActionsListTrace @Nullable []  = null;

    private void resetReadOnlyActionsList() {
        final Thread curThread = Thread.currentThread();
        if (null == resetReadOnlyActionsListThread) {
            resetReadOnlyActionsListThread = curThread;
            resetReadOnlyActionsListTrace = curThread.getStackTrace();
        } else if (curThread != resetReadOnlyActionsListThread) {
            LOGGER.log(Level.FINE, "resetReadOnlyActionsList from new thread {0}", curThread.getName());
        }
        readOnlyActionsList = Collections.unmodifiableList(new ArrayList<>(actionsList));
    }
    private static final Logger LOGGER = Logger.getLogger(PddlExecutorJPanel.class.getName());

    /**
     * Get the value of actionsList
     *
     * @return the value of actionsList
     */
    @Override
    public List<PddlAction> getActionsList() {
        return readOnlyActionsList;
    }

    @Override
    public void clearActionsList() {
        synchronized (actionsList) {
            if (actionsList.size() > 0) {
                actionsList.clear();
                resetReadOnlyActionsList();
            }
        }
        Utils.runOnDispatchThread(() -> {
            DefaultTableModel model = (DefaultTableModel) jTablePddlOutput.getModel();
            model.setRowCount(0);
        });
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
    @Override
    public void setPropertiesFile(File propertiesFile) {
        this.propertiesFile = propertiesFile;
        if (null != propertiesFile) {
            File propertiesFileParent = propertiesFile.getParentFile();
            if (null != propertiesFileParent) {
                this.positionMapJPanel1.setStartingDirectory(propertiesFileParent);
            }
        }
    }

    private static final String PDDLOUTPUT = "pddl.output";
    private static final String REVERSE_PDDLOUTPUT = "pddl.reverse_output";
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
            if (str.startsWith("..")) {
                return str;
            }
            File strFile = new File(str);
            if (!strFile.exists()) {
                return str;
            }
            String canString = strFile.getCanonicalPath();
            File parentFile = f.getParentFile();
            if (null == parentFile) {
                return str;
            }
            String relString = Paths.get(parentFile.getCanonicalPath()).relativize(Paths.get(canString)).toString();
            if (relString.length() <= canString.length()) {
                return relString;
            }
            return canString;
        } catch (IOException iOException) {
        }
        return str;
    }

    @Nullable
    private String actionsFileString = null;
    @Nullable
    private String reverseActionsFileString = null;

    public void saveProperties() throws IOException {
        Map<String, String> propsMap = new HashMap<>();
        updateActionFileStrings();
        if (reverseActionsFileString != null && reverseActionsFileString.length() > 0) {
            String relPath = makeShortPath(propertiesFile, reverseActionsFileString);
            System.out.println("relPath = " + relPath);
            File chkFile = new File(relPath);
            if (!chkFile.isDirectory()) {
                propsMap.put(REVERSE_PDDLOUTPUT, relPath);
            }
        }
        if (actionsFileString != null && actionsFileString.length() > 0) {
            String relPath = makeShortPath(propertiesFile, actionsFileString);
            System.out.println("relPath = " + relPath);
            File chkFile = new File(relPath);
            if (!chkFile.isDirectory()) {
                propsMap.put(PDDLOUTPUT, relPath);
            }
        }
        propsMap.put("reverseFlag", Boolean.toString(reverseFlag));
        Properties props = new Properties();
        props.putAll(propsMap);
        props.putAll(getTableOptions());
        props.put(MANUAL_PART_NAMES, Arrays.toString(getComboPartNames(10)));
        props.put(MANUAL_SLOT_NAMES, Arrays.toString(getComboSlotNames(10)));
        props.put(POS_ERROR_MAP_FILES, Arrays.toString(getRelPathPositionMapFileNames()));
        props.put(ENABLE_OPTA_PLANNER, Boolean.toString(jCheckBoxEnableOptaPlanner.isSelected()));
        Utils.saveProperties(propertiesFile, props);
    }

    @Nullable
    private String[] getRelPathPositionMapFileNames() {
        String[] origNames = positionMapJPanel1.getPositionMapFileNames();
        String[] newNames = new String[origNames.length];
        for (int i = 0; i < newNames.length; i++) {
            String origName = origNames[i];
            if (null == origName) {
                return Arrays.copyOfRange(newNames, 0, i);
            }
            String newName = makeShortPath(propertiesFile, origName);
            newNames[i] = newName;
        }
        return newNames;
    }

    private void updateActionFileStrings() {
        if (reverseFlag) {
            this.reverseActionsFileString = jTextFieldPddlOutputActions.getText();
        } else {
            this.actionsFileString = jTextFieldPddlOutputActions.getText();
        }
    }
    private static final String POS_ERROR_MAP_FILES = "positionMapFileNames";
    private static final String MANUAL_PART_NAMES = "manualPartNames";
    private static final String MANUAL_SLOT_NAMES = "manualSlotNames";

    @Override
    public void addAction(PddlAction action) {
        if (null != action) {
            this.actionsList.add(action);

            double cost = 0.0;
            try {
                cost = Double.parseDouble(action.getCost());
            } catch (NumberFormatException ex) {
                // ignore 
            }
            final double finalCost = cost;
            Utils.runOnDispatchThread(() -> {
                DefaultTableModel model = (DefaultTableModel) jTablePddlOutput.getModel();
                model.addRow(new Object[]{model.getRowCount(), -1, action.getLabel(), action.getType(), Arrays.toString(action.getArgs()), finalCost});
            });
        }
    }

    @Override
    public void processActions() {
        try {
            if (null != runningProgramFuture) {
                runningProgramFuture.cancel(true);
            }
            runningProgramFuture = generateCrclAsync();
        } catch (IOException | IllegalStateException | SQLException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            abortProgram();
        }
    }

    private static final SolverFactory<OpActionPlan> solverFactory = createSolverFactory();

    static private SolverFactory<OpActionPlan> createSolverFactory() {
        return SolverFactory.createFromXmlResource(
                "aprs/framework/optaplanner/actionmodel/actionModelSolverConfig.xml");
    }

    @MonotonicNonNull
    private Solver<OpActionPlan> solver = null;

    private static volatile boolean firstLoad = true;

    private volatile int lastReadyReturnPos = 0;

    public boolean readyForNewActionsList() {
        if (readOnlyActionsList == null || readOnlyActionsList.isEmpty()) {
            lastReadyReturnPos = 1;
            return true;
        }
        if (null != pddlActionToCrclGenerator) {
            if (pddlActionToCrclGenerator.getLastIndex() == 0) {
                lastReadyReturnPos = 2;
                return true;
            }
            if (pddlActionToCrclGenerator.atLastIndex()) {
                lastReadyReturnPos = 3;
                return true;
            }
        }
        return false;
    }

    public void warnIfNewActionsNotReady() {
        if (!readyForNewActionsList()) {
            LOGGER.log(Level.WARNING,
                    "loading new actions when not ready");
        }
    }

    public void loadActionsList(Iterable<? extends PddlAction> newActions) {
        warnIfNewActionsNotReady();
        synchronized (actionsList) {
            clearActionsList();
            for (PddlAction action : newActions) {
                addAction(action);
            }
            resetReadOnlyActionsList();
        }
        finishLoadActionsList(jTextFieldPddlOutputActions.getText());
    }

    public void loadActionsFile(File f, boolean showInOptaPlanner) throws IOException {
        warnIfNewActionsNotReady();
        if (null != f && f.exists()) {
            if (f.isDirectory()) {
                LOGGER.log(Level.SEVERE, "Can not loadActionsFile \"" + f + "\" : it is a directory instead of a text file.");
                return;
            }
            if (!f.canRead()) {
                LOGGER.log(Level.SEVERE, "Can not loadActionsFile \"" + f + "\" : it is not readable.");
                return;
            }

            List<String> lines = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while (null != (line = br.readLine())) {
                    lines.add(line);
                }
            }
            synchronized (actionsList) {
                clearActionsList();
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line.length() < 1) {
                        continue;
                    }
                    addAction(PddlAction.parse(line));
                }
                resetReadOnlyActionsList();
            }
            try {
                if (showInOptaPlanner && pddlActionToCrclGenerator.isConnected()) {
                    showLoadedPlanOptaPlanner();
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
            String canonName = f.getCanonicalPath();
            finishLoadActionsList(canonName);
        }
    }

    private void finishLoadActionsList(String canonName) {
        setReplanFromIndex(0);
        Utils.runOnDispatchThread(() -> {
            autoResizeTableColWidthsPddlOutput();
            jTextFieldIndex.setText("0");
            updateComboPartModel();
            updateComboSlotModel();

            if (!jTextFieldPddlOutputActions.getText().equals(canonName)) {
                jTextFieldPddlOutputActions.setText(canonName);
                updateActionFileStrings();
            }
        });
    }

    private void showLoadedPlanOptaPlanner() throws SQLException {
        pddlActionToCrclGenerator.clearPoseCache();
        pddlActionToCrclGenerator.setOptions(getTableOptions());
        PointType lookForPt = pddlActionToCrclGenerator.getLookForXYZ();
        if (null != lookForPt && jCheckBoxEnableOptaPlanner.isSelected()) {
            if (firstLoad) {
                OpDisplayJPanel.clearColorMap();
                firstLoad = false;
            }
            List<OpAction> opActions;
            int startIndex = 0;
            for (int i = 0; i < actionsList.size(); i++) {
                if (actionsList.get(i).getType() == "look-for-parts") {
                    startIndex = i + 1;
                    break;
                }
            }
            synchronized (actionsList) {
                opActions = pddlActionToCrclGenerator.pddlActionsToOpActions(actionsList, 0);
                resetReadOnlyActionsList();
            }
            if (null == opActions || opActions.size() < 2) {
                return;
            }
            OpActionPlan worstPlan = null;
            double worstScore = Double.POSITIVE_INFINITY;
            OpActionPlan bestPlan = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            if (null == solver) {
                synchronized (solverFactory) {
                    solver = solverFactory.buildSolver();
                }
            }
            solver.addEventListener(e -> System.out.println(e.getTimeMillisSpent() + ", " + e.getNewBestScore()));

            for (int i = 0; i < 10; i++) {

                Collections.shuffle(opActions);
                OpActionPlan inputPlan = new OpActionPlan();
                inputPlan.setActions(opActions);

                inputPlan.getEndAction().setLocation(new Point2D.Double(lookForPt.getX(), lookForPt.getY()));
                inputPlan.initNextActions();

                EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
                HardSoftLongScore score = calculator.calculateScore(inputPlan);
                double inScore = (score.getSoftScore() / 1000.0);
                if (inScore > bestScore) {
                    bestPlan = inputPlan;
                    bestScore = inScore;
                }
                if (inScore < worstScore) {
                    worstPlan = inputPlan;
                    worstScore = inScore;
                }
                OpActionPlan solvedPlan = solver.solve(inputPlan);
                HardSoftLongScore hardSoftLongScore = solvedPlan.getScore();
                assert (null != hardSoftLongScore) : "solvedPlan.getScore() returned null";
                double solveScore = (hardSoftLongScore.getSoftScore() / 1000.0);
                if (solveScore > bestScore) {
                    bestPlan = solvedPlan;
                    bestScore = solveScore;
                }
                if (solveScore < worstScore) {
                    worstPlan = solvedPlan;
                    worstScore = solveScore;
                }
            }
            if (null != worstPlan) {
                this.opDisplayJPanelInput.setOpActionPlan(worstPlan);
            }
            if (null != bestPlan) {
                this.opDisplayJPanelSolution.setOpActionPlan(bestPlan);
            }
            this.opDisplayJPanelInput.setLabel("Input : " + String.format("%.1f mm ", -worstScore));
            this.opDisplayJPanelSolution.setLabel("Output : " + String.format("%.1f mm ", -bestScore));
//                        OpDisplayJPanel.showPlan(solvedPlan, "Output : " + solvedPlan.getScore());
        }
    }

    @Override
    public void autoResizeTableColWidthsPddlOutput() {
        autoResizeTableColWidths(jTablePddlOutput);
    }

    public void refresh() {
        pddlActionToCrclGenerator.reset();
        String origErrorString = this.getErrorString();
        String fname = jTextFieldPddlOutputActions.getText();
        File f = new File(fname);
        if (f.exists() && f.canRead()) {
            try {
                loadActionsFile(f, true);
                if (this.getErrorString() == origErrorString) {
                    setErrorString(null);
                }
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    private void jButtonLoadPddlActionsFromFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoadPddlActionsFromFileActionPerformed
        try {
            browseActionsFile();
            loadActionsFile(new File(jTextFieldPddlOutputActions.getText()), true);

        } catch (IOException ex) {
            Logger.getLogger(AprsSystemInterface.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonLoadPddlActionsFromFileActionPerformed

    private void jButtonLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoadActionPerformed
        try {
            loadActionsFile(new File(jTextFieldPddlOutputActions.getText()), true);

        } catch (IOException ex) {
            Logger.getLogger(AprsSystemInterface.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonLoadActionPerformed

    private void jTextFieldPddlOutputActionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldPddlOutputActionsActionPerformed
        try {
            loadActionsFile(new File(jTextFieldPddlOutputActions.getText()), true);

        } catch (IOException ex) {
            Logger.getLogger(AprsSystemInterface.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jTextFieldPddlOutputActionsActionPerformed

    private DbSetupPublisher dbSetupPublisher;

    @MonotonicNonNull
    private Callable<DbSetupPublisher> dbSetupSupplier = null;

    @Nullable
    public Callable<DbSetupPublisher> getDbSetupSupplier() {
        return dbSetupSupplier;
    }

    private final DbSetupListener dbSetupListener = new DbSetupListener() {
        @Override
        public void accept(DbSetup setup) {
            handleNewDbSetup(setup);
        }
    };

    public void setDbSetupSupplier(Callable<DbSetupPublisher> dbSetupSupplier) {
        this.dbSetupSupplier = dbSetupSupplier;
        try {
            dbSetupPublisher = dbSetupSupplier.call();
            dbSetupPublisher.addDbSetupListener(dbSetupListener);
        } catch (Exception ex) {
            Logger.getLogger(VisionToDBJPanel.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public long incrementAndGetCommandId() {
        return pddlActionToCrclGenerator.incrementAndGetCommandId();
    }

    private CRCLProgramType createEmptyProgram() {
        CRCLProgramType program = new CRCLProgramType();
        InitCanonType initCmd = new InitCanonType();
        setCommandId(initCmd);
        program.setInitCanon(initCmd);
        EndCanonType endCmd = new EndCanonType();
//        setCommandId(endCmd);
        program.setEndCanon(endCmd);
        return program;
    }

    private void setCommandId(CRCLCommandType cmd) {
        Utils.setCommandID(cmd, incrementAndGetCommandId());
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

//    List<JTextArea> crclAreas = new ArrayList<>();
    JTextArea editTableArea = new JTextArea();

    private String trimXml(String in) {
        int start = in.indexOf("?>");
        if (start < 0) {
            start = 0;
        } else {
            start = start + 2;
        }
        int instIndex = in.indexOf("<CRCLCommandInstance>", Math.max(0, start));
        if (instIndex > 0) {
            start = instIndex + "<CRCLCommandInstance>".length();
        }
        int end = in.indexOf("</CRCLCommandInstance>", Math.max(0, start));
        if (end <= 0) {
            end = in.length();
        }
        return in.substring(start, end).trim();
    }
    private Color progColor = Color.white;

    private void loadProgramToTable(CRCLProgramType crclProgram) {
        jTableCrclProgram.setBackground(Color.white);
        DefaultTableModel model = (DefaultTableModel) jTableCrclProgram.getModel();
        jTableCrclProgram.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, @Nullable Object value, boolean isSelected, boolean hasFocus, int row, int column) {
//                while (crclAreas.size() <= row) {
//                    JTextArea area = new JTextArea();
//                    area.setOpaque(true);
//                    area.setVisible(true);
//                    crclAreas.add(area);
//                }
                JTextArea area = new JTextArea();
                area.setOpaque(true);
                area.setVisible(true);
//                JTextArea area = crclAreas.get(row);
//                if (area != null) {
                area.setText(Objects.toString(value));
//                }
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
                model.addRow(new Object[]{initCanon.getCommandID(),
                    trimXml(crclSocket.commandInstanceToPrettyString(instance, true))
                });

            } catch (JAXBException | CRCLException ex) {
                model.addRow(new Object[]{initCanon.getCommandID(),
                    ex.getMessage()
                });
                LOGGER.log(Level.SEVERE, null, ex);
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
                    model.addRow(new Object[]{midCmd.getCommandID(),
                        trimXml(crclSocket.commandInstanceToPrettyString(instance, true))
                    });

                } catch (JAXBException | CRCLException ex) {
                    model.addRow(new Object[]{midCmd.getCommandID(),
                        ex.getMessage()
                    });
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        }
        EndCanonType endCanon = crclProgram.getEndCanon();
        if (null != endCanon) {
            try {
                instance.setCRCLCommand(endCanon);
                model.addRow(new Object[]{endCanon.getCommandID(),
                    trimXml(crclSocket.commandInstanceToPrettyString(instance, true))
                });

            } catch (JAXBException | CRCLException ex) {
                model.addRow(new Object[]{endCanon.getCommandID(),
                    ex.getMessage()
                });
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
        autoResizeTableRowHeights(jTableCrclProgram);
        autoResizeTableColWidths(jTableCrclProgram);
    }

    private boolean autoStart = true;

    @Nullable
    private volatile CRCLProgramType unstartedProgram = null;

    private AprsSystemInterface aprsJFrame;

    /**
     * Get the value of aprsJFrame
     *
     * @return the value of aprsJFrame
     */
    public AprsSystemInterface getAprsSystemInterface() {
        return aprsJFrame;
    }

    /**
     * Set the value of aprsJFrame
     *
     * @param aprsJFrame new value of aprsJFrame
     */
    public void setAprsSystemInterface(AprsSystemInterface aprsJFrame) {
        this.aprsJFrame = aprsJFrame;
        if (null != pddlActionToCrclGenerator) {
            pddlActionToCrclGenerator.setAprsSystemInterface(aprsJFrame);
        }
    }

    public boolean getForceFakeTakeFlag() {
        return jCheckBoxForceFakeTake.isSelected();
    }

    public void setForceFakeTakeFlag(boolean _force) {
        if (_force != jCheckBoxForceFakeTake.isSelected()) {
            jCheckBoxForceFakeTake.setSelected(_force);
        }
        if (null != aprsJFrame) {
            aprsJFrame.setForceFakeTakeFlag(_force);
        }
    }

    private String getActionsCrclName() {
        String actionsName = jTextFieldPddlOutputActions.getText();
        int sindex = actionsName.lastIndexOf('/');
        if (sindex > 0 && sindex < actionsName.length() - 1) {
            actionsName = actionsName.substring(sindex + 1);
        }
        sindex = actionsName.lastIndexOf('\\');
        if (sindex > 0 && sindex < actionsName.length() - 1) {
            actionsName = actionsName.substring(sindex + 1);
        }
        return actionsName + ":" + currentActionIndex + "_" + pddlActionToCrclGenerator.getLastIndex() + ":" + pddlActionToCrclGenerator.getCrclNumber();
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
            if (crclProgram.getName() == null || crclProgram.getName().length() < 1) {
                crclProgram.setName(getActionsCrclName());
            }
            Utils.runOnDispatchThreadWithCatch(() -> loadProgramToTable(crclProgram));
            if (null != aprsJFrame) {
                aprsJFrame.addProgramLineListener(this);
                aprsJFrame.setCRCLProgram(crclProgram);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public XFuture<Boolean> continueCurrentCrclProgram() {
        return aprsJFrame.continueCrclProgram();
    }

    private volatile long startCrclProgramTime = 0;
    private final AtomicInteger startCrclProgramCount = new AtomicInteger(0);

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
            prepCrclProgram(crclProgram);
            return aprsJFrame.startCRCLProgram(crclProgram);

        } catch (Exception ex) {
            XFuture<Boolean> future = new XFuture<>("startCrclProgramException");
            LOGGER.log(Level.SEVERE, null, ex);
            future.completeExceptionally(ex);
            return future;
        }
    }

    private void prepCrclProgram(CRCLProgramType crclProgram1) throws IllegalStateException {
        startCrclProgramTime = System.currentTimeMillis();
        startCrclProgramCount.incrementAndGet();
        this.crclProgram = crclProgram1;
        unstartedProgram = null;
        Utils.runOnDispatchThreadWithCatch(() -> loadProgramToTable(crclProgram1));
        this.runningProgram = true;
        if (null == aprsJFrame) {
            throw new IllegalStateException("Can't start crcl program with null aprsJFrame reference.");
        }
        aprsJFrame.addProgramLineListener(this);
        if (crclProgram1.getName() == null || crclProgram1.getName().length() < 1) {
            crclProgram1.setName(getActionsCrclName());
        }
    }

    public boolean runCrclProgram(CRCLProgramType crclProgram) throws JAXBException {
        prepCrclProgram(crclProgram);
        boolean ret = aprsJFrame.runCRCLProgram(crclProgram);
        try {
            aprsJFrame.saveLastProgramRunDataListToCsv(aprsJFrame.createTempFile("programRunData", ".csv"));
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
//        System.out.println("runCrclProgram returned = " + ret);
        return ret;
    }

    private void jButtonGenerateCRCLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGenerateCRCLActionPerformed

        try {
            autoStart = false;
            setReplanFromIndex(0);
            cancelRunProgramFuture();
            runningProgramFuture = generateCrclAsync();
        } catch (IOException | IllegalStateException | SQLException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            abortProgram();
        }
    }//GEN-LAST:event_jButtonGenerateCRCLActionPerformed

    private void cancelRunProgramFuture() {
        if (null != runningProgramFuture) {
            runningProgramFuture.cancelAll(true);
        }
    }

    private void jButtonPddlOutputViewEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPddlOutputViewEditActionPerformed
        try {
            Desktop.getDesktop().open(new File(jTextFieldPddlOutputActions.getText()));

        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonPddlOutputViewEditActionPerformed

    private int getReplanFromIndex() {
        return replanFromIndex.get();
    }

    @Nullable
    private String currentPart = null;
    private volatile StackTraceElement setReplanFromIndexLastTrace @Nullable []  = null;
    private volatile StackTraceElement prevSetReplanFromIndexLastTrace @Nullable []  = null;

    @Nullable
    private volatile Thread setReplanFromIndexLastThread = null;
    @Nullable
    private volatile Thread prevSetReplanFromIndexLastThread = null;

    private void setReplanFromIndex(int replanFromIndex) {
        setReplanFromIndex(replanFromIndex, false);
    }

    private void setReplanFromIndex(int replanFromIndex, boolean aborting) {
        int oldRpi = this.replanFromIndex.getAndSet(replanFromIndex);
        if (oldRpi != replanFromIndex) {
            prevSetReplanFromIndexLastThread = setReplanFromIndexLastThread;
            setReplanFromIndexLastThread = Thread.currentThread();
            prevSetReplanFromIndexLastTrace = setReplanFromIndexLastTrace;
            setReplanFromIndexLastTrace = Thread.currentThread().getStackTrace();
            if (!aborting && oldRpi > replanFromIndex) {
                if (replanFromIndex != 0 || !readyForNewActionsList()) {
                    System.err.println("Reducing replanFromIndex when generater not readyForNewActionsList: oldRpi=" + oldRpi + ", new replanFromIndex=" + replanFromIndex + ",  pddlActionToCrclGenerator.getLastIndex()=" + pddlActionToCrclGenerator.getLastIndex());
                }
            }
            @Nullable
            String[] names = this.pddlActionToCrclGenerator.getActionToCrclTakenPartsNames();
            if (replanFromIndex == 0) {
                currentPart = null;
            } else if (names != null && names.length >= replanFromIndex && replanFromIndex > 0) {
                currentPart = names[replanFromIndex - 1];
            } else {
                currentPart = null;
            }
            Utils.runOnDispatchThread(() -> {
                updateSelectionInterval();
                jTablePddlOutput.scrollRectToVisible(new Rectangle(jTablePddlOutput.getCellRect(replanFromIndex, 0, true)));
                if (replanFromIndex == 0) {
                    jTextFieldCurrentPart.setText("");
                } else if (null != currentPart) {
                    jTextFieldCurrentPart.setText(currentPart);
                } else {
                    jTextFieldCurrentPart.setText("");
                }
            });
        }
    }


    private void jTextFieldIndexActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldIndexActionPerformed
        setReplanFromIndex(Integer.parseInt(jTextFieldIndex.getText()));
    }//GEN-LAST:event_jTextFieldIndexActionPerformed

    private void jButtonClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearActionPerformed
        clearAll();
    }//GEN-LAST:event_jButtonClearActionPerformed

    private final AtomicInteger clearAllCount = new AtomicInteger(0);
    private volatile long clearAllTime = 0;

    public void clearAll() {
        warnIfNewActionsNotReady();
        clearAllCount.incrementAndGet();
        clearAllTime = System.currentTimeMillis();
        clearActionsList();
        DefaultTableModel model = (DefaultTableModel) jTableCrclProgram.getModel();
        model.setRowCount(0);
        setReplanFromIndex(0);
        safeAbortRunnablesVector.clear();
        abortProgram();
        lastActionMillis = System.currentTimeMillis();
        setErrorString(null);
        aprsJFrame.setTitleErrorString(null);
        clearPoseCache();
        lastContinueActionFuture = null;
        lastSafeAbortFuture = null;
    }

    private AtomicInteger abortProgramCount = new AtomicInteger(0);
    private volatile long abortProgramTime = 0;

    /**
     * Abort the currently running CRCL program.
     */
    public void abortProgram() {
        boolean rps = replanStarted.getAndSet(true);
        if (null != customRunnables) {
            customRunnables.clear();
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
        completeSafeAbort();
        replanStarted.set(rps);
        runningProgram = false;
        abortProgramTime = System.currentTimeMillis();
        abortProgramCount.incrementAndGet();
    }

    public void completeSafeAbort() {
        Runnable r;
        while (null != (r = safeAbortRunnablesVector.pollFirst())) {
            r.run();
        }
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
            autoStart = true;
            String part = getComboPart();
            cancelRunProgramFuture();
            if (null == part) {
                warnDialog("part to take is null");
                return;
            }
            runningProgramFuture = this.takePart(part);
        } catch (CRCLException | PmException | IOException | IllegalStateException | SQLException | InterruptedException | PendantClientInner.ConcurrentBlockProgramsException | ExecutionException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            abortProgram();
            showExceptionInProgram(ex);
        }
    }//GEN-LAST:event_jButtonTakeActionPerformed

    private void warnDialog(String msg) throws HeadlessException {
        LOGGER.log(Level.WARNING, msg);
        JOptionPane.showMessageDialog(this, msg);
    }

    @Nullable
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

    private void updateComboPartModel() {
        DefaultComboBoxModel<String> cbm = (DefaultComboBoxModel<String>) jComboBoxManualObjectName.getModel();
        boolean first = true;
        synchronized (actionsList) {
            for (PddlAction action : actionsList) {
                switch (action.getType()) {
                    case "fake-take-part":
                    case "take-part":
                        if (action.getArgs().length > 0) {
                            boolean found = false;
                            String part = action.getArgs()[0];
                            for (int i = 0; i < cbm.getSize(); i++) {
                                if (cbm.getElementAt(i).equals(part)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                cbm.insertElementAt(part, 0);
                            }
                            if (first) {
                                cbm.setSelectedItem(part);
                                first = false;
                            }
                        }
                        break;
                }
            }
        }
    }

    private void updateComboSlotModel() {
        DefaultComboBoxModel<String> cbm = (DefaultComboBoxModel<String>) jComboBoxManualSlotName.getModel();
        boolean first = true;
        synchronized (actionsList) {
            for (PddlAction action : actionsList) {
                switch (action.getType()) {
                    case "place-part":
                        if (action.getArgs().length > 0) {
                            boolean found = false;
                            String slot = action.getArgs()[0];
                            for (int i = 0; i < cbm.getSize(); i++) {
                                if (cbm.getElementAt(i).equals(slot)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                cbm.insertElementAt(slot, 0);
                            }
                            if (first) {
                                cbm.setSelectedItem(slot);
                                first = false;
                            }
                        }
                        break;
                }
            }
        }
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
            cancelRunProgramFuture();
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
            cancelRunProgramFuture();
            if (null == part) {
                warnDialog("part to return is null");
                return;
            }
            runningProgramFuture = this.returnPart(part);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
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
                    String offsetString
                            = (testDropOffPose.getPoint().getX() - poseFromDb.getPoint().getX())
                            + "," + (testDropOffPose.getPoint().getY() - poseFromDb.getPoint().getY())
                            + "," + (testDropOffPose.getPoint().getZ() - poseFromDb.getPoint().getZ());
                    System.out.println("offsetString = " + offsetString);
                    writeCorrectionCsv(recordCsvName,
                            System.currentTimeMillis() + ", " + partName + ", " + randomPoseString + ", " + poseFromDbString + ", " + offsetString);
                }
                return this.randomPickup();
            }
            return XFuture.completedFuture(false);
        } catch (CRCLException | PmException | IOException | SQLException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            XFuture<Boolean> future = new XFuture<>("recordAndCompletTestPickupException");
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
                        testDropOffPose.getPoint().getX(),
                        testDropOffPose.getPoint().getY(),
                        testDropOffPose.getPoint().getZ());
        System.out.println("randomPoseString = " + randomPoseString);
        System.out.println("randomDropOffCount = " + randomDropOffCount);
        customRunnables.clear();
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
                .thenCompose("randomTest.lookForParts",
                        x -> recursiveSupplyBoolean(x, () -> this.lookForParts()))
                .thenCompose("randomTest.recordAndCompletTestPickup",
                        x -> recursiveSupplyBoolean(x, () -> this.recordAndCompletTestPickup()))
                .thenCompose("randomTest.randomDropOff",
                        x -> recursiveSupplyBoolean(x, () -> this.randomDropOff()));
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
                            testDropOffPose.getPoint().getX(),
                            testDropOffPose.getPoint().getY(),
                            testDropOffPose.getPoint().getZ());
            System.out.println("randomPoseString = " + randomPoseString);
            System.out.println("randomDropOffCount = " + randomDropOffCount);
            customRunnables.clear();
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
            if (null == poseFromDb) {
                String msg = "Unable to get pose for " + partName + " from the database.";
                Logger.getLogger(VisionToDBJPanel.class
                        .getName()).log(Level.WARNING, msg);
                JOptionPane.showMessageDialog(this, msg);
                return;
            }
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
            if (null == poseFromDb) {
                String msg = "Unable to get pose for " + partName + " from the database.";
                Logger.getLogger(VisionToDBJPanel.class
                        .getName()).log(Level.WARNING, msg);
                JOptionPane.showMessageDialog(this, msg);
                return;
            }
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
            cancelRunProgramFuture();
            runningProgramFuture = generateCrclAsync();
        } catch (IOException | IllegalStateException | SQLException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            abortProgram();
        }
    }//GEN-LAST:event_jButtonGenerateAndRunActionPerformed

    @Nullable
    private ServerSocket externalControlSocket = null;
    @Nullable
    private Thread externalControlAcceptThread = null;
    final private List<Socket> externalControlClientSockets = new ArrayList<>();
    final private List<Thread> externalControlClientThreads = new ArrayList<>();

    private static void sendSocket(Socket s, String msg) {
        try {
            s.getOutputStream().write(msg.getBytes());
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
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
            this.startSafeAbort("external from " + s + ":" + line).thenAccept(b -> sendSocket(s, "Done\r\n"));
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
            LOGGER.log(Level.SEVERE, null, ex);
        }
        try {
            s.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void runExternalControlMainServerThread() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                if (null == externalControlSocket) {
                    break;
                }
                final Socket s = externalControlSocket.accept();
                externalControlClientSockets.add(s);
                Thread t = new Thread(() -> runMainExternalControlClientHandler(s),
                        "externalControlClientThread_" + s.getRemoteSocketAddress());
                t.start();
                externalControlClientThreads.add(t);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public void setExternalControlPortEnabled(boolean on) throws IOException {
        closeExternalControlService();
        externalControlSocket = new ServerSocket(Integer.parseInt(jTextFieldExternalControlPort.getText()));
        externalControlClientSockets.clear();
        externalControlClientThreads.clear();
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
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private static void closeThread(Thread t) {
        try {
            t.interrupt();
            t.join(100);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void closeExternalControlService() {
        if (null != externalControlSocket) {
            try {
                externalControlSocket.close();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
        if (null != externalControlAcceptThread) {
            externalControlAcceptThread.interrupt();
            try {
                externalControlAcceptThread.join(100);
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
        if (externalControlClientSockets != null) {
            externalControlClientSockets.forEach(s -> closeSocket(s));
            externalControlClientSockets.clear();
        }
        if (externalControlClientThreads != null) {
            externalControlClientThreads.forEach(t -> closeThread(t));
            externalControlClientThreads.clear();
        }
    }

    private void jCheckBoxEnableExternalControlPortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxEnableExternalControlPortActionPerformed
        try {
            setExternalControlPortEnabled(jCheckBoxEnableExternalControlPort.isSelected());
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxEnableExternalControlPortActionPerformed

    @Nullable
    private volatile XFuture<Boolean> runningProgramFuture = null;

    private void jButtonStepActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonStepActionPerformed

        runSingleRow();
//        stepping = true;
//        continueActionListPrivate();
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

    private final AtomicInteger safeAboutCount = new AtomicInteger(0);
    private final AtomicInteger safeAbortRequestCount = new AtomicInteger(0);

    private void incSafeAbortCount() {
        final int count = safeAboutCount.incrementAndGet();
        Utils.runOnDispatchThread(() -> jTextFieldSafeAbortCount.setText(Integer.toString(count)));
    }

    private void incSafeAbortRequestCount() {
        final int count = safeAbortRequestCount.incrementAndGet();
        Utils.runOnDispatchThread(() -> jTextFieldSafeAbortRequestCount.setText(Integer.toString(count)));
    }

    public void debugAction() {
        long curTime = System.currentTimeMillis();
        System.out.println("curTime = " + curTime);
        System.out.println("runningProgram = " + runningProgram);
        System.out.println("safeAbortRunnablesVector = " + safeAbortRunnablesVector);
        System.out.println("startSafeAbortRunningProgram = " + startSafeAbortRunningProgram);
        System.out.println("startSafeAbortRunningProgramFuture = " + startSafeAbortRunningProgramFuture);
        if (null != startSafeAbortRunningProgramFuture) {
            startSafeAbortRunningProgramFuture.printStatus();
        }
        System.out.println("startSafeAbortRunningProgramFutureDone = " + startSafeAbortRunningProgramFutureDone);

        System.out.println("lastCheckAbortCurrentPart = " + lastCheckAbortCurrentPart);
        System.out.println("lastCheckAbortSafeAbortRequested = " + lastCheckAbortSafeAbortRequested);
        System.out.println("lastCheckSafeAbortTime = " + lastCheckSafeAbortTime);
        System.out.println("lastReplanAfterCrclBlock = " + lastReplanAfterCrclBlock);

//        private volatile String lastCheckAbortCurrentPart = null;
//    private volatile boolean lastCheckAbortSafeAbortRequested = false;
//    private volatile long lastCheckSafeAbortTime = 0;
        System.out.println("continueActionsCount = " + continueActionsCount);
        System.out.println("continueActionsListTime = " + continueActionsListTime);
        System.out.println("clearAllCount = " + clearAllCount);
        System.out.println("clearAllTime = " + clearAllTime);
        System.out.println("startSafeAbortTime = " + startSafeAbortTime);
        System.out.println("startCrclProgramTime = " + startCrclProgramTime);
        System.out.println("startCrclProgramCount = " + startCrclProgramCount);
        System.out.println("doSafeAbortCount = " + doSafeAbortCount);
        System.out.println("doSafeAbortTime = " + doSafeAbortTime);
        System.out.println("runningProgramFuture = " + runningProgramFuture);
        System.out.println("runProgramCompleteRunnablesTime = " + runProgramCompleteRunnablesTime);
        System.out.println("(curTime - lastCheckSafeAbortTime) = " + (curTime - lastCheckSafeAbortTime));
        System.out.println("(curTime - doSafeAbortTime)        = " + (curTime - doSafeAbortTime));
        System.out.println("(curTime - startCrclProgramTime)   = " + (curTime - startCrclProgramTime));
        System.out.println("(curTime - startSafeAbortTime)     = " + (curTime - startSafeAbortTime));
        System.out.println("(curTime - clearAllTime)           = " + (curTime - clearAllTime));
        System.out.println("(curTime - runProgramCompleteRunnablesTime)           = " + (curTime - runProgramCompleteRunnablesTime));

        if (null != runningProgramFuture) {
            runningProgramFuture.printStatus(System.out);
        }

        System.out.println("lastSafeAbortFuture=" + lastSafeAbortFuture);
        if (null != lastSafeAbortFuture) {
            lastSafeAbortFuture.printStatus(System.out);
        }
        System.out.println("lastContinueActionFuture = " + lastContinueActionFuture);
        if (null != lastContinueActionFuture) {
            lastContinueActionFuture.printStatus(System.out);
        }

        System.out.println("checkDbSupplierPublisherFuturesList = " + checkDbSupplierPublisherFuturesList);
    }

    private volatile boolean startSafeAbortRunningProgram = false;
    private volatile long startSafeAbortTime = 0;

    @Nullable
    private volatile XFuture<Void> lastSafeAbortFuture = null;
    @Nullable
    private volatile XFuture<Boolean> startSafeAbortRunningProgramFuture = null;
    private volatile boolean startSafeAbortRunningProgramFutureDone = false;

    @Nullable
    private volatile CRCLProgramType startSafeAbortProgram = null;
    @Nullable
    private volatile String startSafeAbortProgramName = null;
    private volatile boolean startSafeAbortIsRunningCrclProgram = false;

    private void completeSafeAbortFuture(XFuture<Void> f) {
        incSafeAbortCount();
        pddlActionToCrclGenerator.takeSnapshots("", "completeSafeAbortFuture." + f, null, null);
        f.complete(null);
    }

    public XFuture<Void> startSafeAbort(String name) {
        final int startSafeAbortRequestCount = safeAbortRequestCount.get();
        startSafeAbortTime = System.currentTimeMillis();
        synchronized (this) {
//            if (null != lastSafeAbortFuture && !lastSafeAbortFuture.isDone()) {
//                return lastSafeAbortFuture;
//            }

            startSafeAbortProgram = aprsJFrame.getCrclProgram();
            if (null != startSafeAbortProgram) {
                startSafeAbortProgramName = startSafeAbortProgram.getName();
            }
            startSafeAbortIsRunningCrclProgram = aprsJFrame.isRunningCrclProgram();
            startSafeAbortRunningProgram = runningProgram;
            startSafeAbortRunningProgramFuture = runningProgramFuture;
            startSafeAbortRunningProgramFutureDone
                    = startSafeAbortRunningProgramFuture != null
                    && startSafeAbortRunningProgramFuture.isDone();
            int runnablesSize = this.safeAbortRunnablesVector.size();
            if (runnablesSize > 0) {
                System.out.println("safeAbortRunnablesVector.size() = " + runnablesSize);
            }
            incSafeAbortRequestCount();
            if (!startSafeAbortRunningProgram) {
                incSafeAbortCount();
                return XFuture.completedFutureWithName("!startSafeAbortRunningProgram" + startSafeAbortRequestCount + ":" + safeAboutCount.get() + ":" + name + ":pddlExecutorStartSafeAbort." + aprsJFrame.getRunName(), null);
            }
            if (startSafeAbortRunningProgramFutureDone) {
                incSafeAbortCount();
                return XFuture.completedFutureWithName("startSafeAbortRunningProgramFutureDone" + startSafeAbortRequestCount + ":" + safeAboutCount.get() + ":" + name + ":pddlExecutorStartSafeAbort." + aprsJFrame.getRunName(), null);
            }
            if (!startSafeAbortIsRunningCrclProgram) {
                incSafeAbortCount();
                return XFuture.completedFutureWithName("!startSafeAbortIsRunningCrclProgram" + startSafeAbortRequestCount + ":" + safeAboutCount.get() + ":" + name + ":pddlExecutorStartSafeAbort." + aprsJFrame.getRunName(), null);
            }

            final XFuture<Void> ret = new XFuture<>(startSafeAbortRequestCount + ":" + safeAboutCount.get() + ":" + name + ":pddlExecutorStartSafeAbort." + aprsJFrame.getRunName());

            this.safeAbortRunnablesVector.add(() -> completeSafeAbortFuture(ret));
            lastSafeAbortFuture = ret;
            return ret;
        }
    }

    private void jButtonSafeAbortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSafeAbortActionPerformed
        this.startSafeAbort("user");
    }//GEN-LAST:event_jButtonSafeAbortActionPerformed

    private void jButtonContinueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonContinueActionPerformed
        this.aprsJFrame.abortCrclProgram();
        int row = jTablePddlOutput.getSelectedRow();
        currentActionIndex = row;
        setReplanFromIndex(row);
        stepping = false;
        continueActionListPrivate();
    }//GEN-LAST:event_jButtonContinueActionPerformed

    @Nullable
    private volatile XFuture<Void> lastContinueActionFuture = null;

    public int getSafeAbortRequestCount() {
        return safeAbortRequestCount.get();
    }

    public boolean completeActionList(String comment, int startSafeAbortRequestCount) {
        try {
            doingActionsStarted.incrementAndGet();
            autoStart = true;
            boolean ret = generateCrcl(comment, startSafeAbortRequestCount);
            if (ret && atLastAction()) {
                actionSetsCompleted.set(actionSetsStarted.get());
            }
            return ret;
        } catch (CRCLException | PmException | IOException | IllegalStateException | SQLException | JAXBException | InterruptedException | ExecutionException | PendantClientInner.ConcurrentBlockProgramsException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            abortProgram();
            showExceptionInProgram(ex);
            throw new RuntimeException(ex);
        } finally {
            doingActionsFinished.incrementAndGet();
        }
    }

    public XFuture<Void> continueActionList() {
        XFuture<Void> ret = new XFuture<>("pddlExecutorContinueActionList");
        lastContinueActionFuture = ret;
        addProgramCompleteRunnable(() -> {
            ret.complete(null);
        });
        continueActionListPrivate();
        return ret;
    }

    private final AtomicInteger continueActionsCount = new AtomicInteger(0);
    private volatile long continueActionsListTime = 0;

    private void continueActionListPrivate() {
        continueActionsCount.incrementAndGet();
        continueActionsListTime = System.currentTimeMillis();
        autoStart = true;
        final int rpi = getReplanFromIndex();
        if (rpi < 0 || rpi >= actionsList.size()) {
            setReplanFromIndex(0);
        }
        jCheckBoxReplan.setSelected(true);
        if (null != unstartedProgram) {
            runningProgramFuture = startCrclProgram(unstartedProgram);
        } else if (null != runningProgramFuture
                && !runningProgramFuture.isDone()
                && !runningProgramFuture.isCancelled()) {
            runningProgramFuture
                    = continueCurrentCrclProgram();
        } else {
            try {
                runningProgramFuture = generateCrclAsync();
            } catch (IOException | IllegalStateException | SQLException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
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
            autoStart = true;
//            this.jTextFieldTakeCount.setText(Integer.toString(takePartCount));
            String part = getComboPart();
            if (null == part) {
                warnDialog("part to take is null");
                return;
            }
            String slot = getComboSlot();
            if (null == slot) {
                warnDialog("slot to place in is null");
                return;
            }
            cancelRunProgramFuture();
            runningProgramFuture = this.placePartSlot(part, slot);
        } catch (CRCLException | PmException | IOException | IllegalStateException | SQLException | InterruptedException | ExecutionException | PendantClientInner.ConcurrentBlockProgramsException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
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
            cancelRunProgramFuture();
            if (null == part) {
                warnDialog("part to take is null");
                return;
            }
            runningProgramFuture = this.testPartPosition(part);
        } catch (CRCLException | PmException | IOException | IllegalStateException | SQLException | InterruptedException | ExecutionException | PendantClientInner.ConcurrentBlockProgramsException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
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
                    assert (null != curPose) :
                            "aprsJFrame.getCurrentPose() returned null : @AssumeAssertion(nullness)";
                    String curPoseString
                            = String.format("%.1f, %.1f, %.1f",
                                    curPose.getPoint().getX(),
                                    curPose.getPoint().getY(),
                                    curPose.getPoint().getZ());
                    String offsetString
                            = (curPose.getPoint().getX() - poseFromDb.getPoint().getX())
                            + "," + (curPose.getPoint().getY() - poseFromDb.getPoint().getY())
                            + "," + (curPose.getPoint().getZ() - poseFromDb.getPoint().getZ());
                    System.out.println("offsetString = " + offsetString);
                    writeCorrectionCsv(recordCsvName,
                            System.currentTimeMillis() + ", " + partName + ", " + curPoseString + ", " + poseFromDbString + ", " + offsetString);
                }
            } catch (SQLException | IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jButtonRecordActionPerformed

    private void jButtonPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPauseActionPerformed
        pause();
    }//GEN-LAST:event_jButtonPauseActionPerformed

    private void pause() {
        aprsJFrame.pauseCrclProgram();
    }

    public void showPaused(boolean paused) {
        jButtonDropTool.setEnabled(!paused);
        jButtonLookFor.setEnabled(!paused);
        jButtonPickupTool.setEnabled(!paused);
        jButtonGridTest.setEnabled(!paused);
        jButtonPlacePart.setEnabled(!paused);
        jButtonRandDropOff.setEnabled(!paused);
        jButtonTake.setEnabled(!paused);
        jButtonTest.setEnabled(!paused);
        jButtonTestPickup.setEnabled(!paused);
        jButtonGotoToolChangerApproach.setEnabled(!paused);
        jButtonGotoToolChangerPose.setEnabled(!paused);
    }

    private void updateLookForJoints(CRCLStatusType stat) {
        if (null != stat && null != stat.getJointStatuses()) {
            List<JointStatusType> jointList = stat.getJointStatuses().getJointStatus();
            String jointVals
                    = jointStatusListToString(jointList);
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

    private String jointStatusListToString(List<JointStatusType> jointList) {
        String jointVals
                = jointList
                        .stream()
                        .sorted(Comparator.comparing(JointStatusType::getJointNumber))
                        .map(JointStatusType::getJointPosition)
                        .map(Objects::toString)
                        .collect(Collectors.joining(","));
        return jointVals;
    }

    private void jButtonRecordLookForJointsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRecordLookForJointsActionPerformed
        CRCLStatusType status = aprsJFrame.getCurrentStatus();
        if (null != status) {
            this.updateLookForJoints(status);
        }
    }//GEN-LAST:event_jButtonRecordLookForJointsActionPerformed

    private void jButtonClearPoseCacheActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearPoseCacheActionPerformed
        clearPoseCache();
    }//GEN-LAST:event_jButtonClearPoseCacheActionPerformed

    private void jTextFieldCurrentPartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldCurrentPartActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldCurrentPartActionPerformed

    private void jCheckBoxForceFakeTakeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxForceFakeTakeActionPerformed
        setForceFakeTakeFlag(jCheckBoxForceFakeTake.isSelected());
    }//GEN-LAST:event_jCheckBoxForceFakeTakeActionPerformed

//    @Nullable
//    private volatile PoseType toolChangerPose = null;
//    @Nullable
//    private volatile String toolChangerPoseName = null;
//    private final Map<String, PoseType> toolChangerPoseMap = new ConcurrentHashMap<>();
//    private final Map<String, PoseType> toolOffsetPoseMap = new ConcurrentHashMap<>();
    @Nullable
    private String toolChangerPoseMapFileName = null;

    private void loadToolChangerPoseMap() {
        if (null == propertiesFile || !propertiesFile.exists()) {
            return;
        }
        toolChangerPoseMapFileName = propertiesFile.getName() + ".toolChangerPoses.csv";
        File f = new File(propertiesFile.getParent(), toolChangerPoseMapFileName);
        if (!f.exists()) {
            return;
        }
        int lineNumber = 0;
        pddlActionToCrclGenerator.clearToolChangerJointVals();
        DefaultTableModel dtm = (DefaultTableModel) jTableToolHolderPositions.getModel();
        dtm.setRowCount(0);
        ConcurrentHashMap<String, PoseType> toolHolderPoseMap
                = pddlActionToCrclGenerator.getToolHolderPoseMap();
        try (CSVParser parser = new CSVParser(new FileReader(f), Utils.preferredCsvFormat())) {
            Map<String, Integer> headerMap = parser.getHeaderMap();
            if (null == headerMap) {
                throw new IllegalArgumentException(f.getCanonicalPath() + " does not have header");
            }
            List<CSVRecord> records = parser.getRecords();
            int skipRows = 0;
            for (int i = 0; i < records.size(); i++) {
                CSVRecord rec = records.get(i);
                String colName = dtm.getColumnName(0);
                Integer colIndex = headerMap.get(colName);
                if (null == colIndex) {
                    throw new IllegalArgumentException(f.getCanonicalPath() + " does not have field :" + colName);
                }
                String val0 = rec.get(colIndex);
                if (!val0.equals(colName) && val0.length() > 0) {
                    break;
                }
                skipRows++;
            }
            dtm.setRowCount(records.size() - skipRows);
            ROW_LOOP:
            for (int i = skipRows; i < records.size(); i++) {
                CSVRecord rec = records.get(i);
                for (int j = 0; j < dtm.getColumnCount(); j++) {
                    String colName = dtm.getColumnName(j);
                    Integer colIndex = headerMap.get(colName);
                    if (null == colIndex) {
                        continue;
                    }
                    String val = rec.get(colIndex);
                    try {
                        if (null != val) {
                            if (val.equals(colName) || (j == 0 && val.length() < 1)) {
                                continue ROW_LOOP;
                            }
                            Class<?> colClass = dtm.getColumnClass(j);
                            if (colClass == Double.class) {
                                dtm.setValueAt(Double.valueOf(val), i - skipRows, j);
                            } else if (colClass == Boolean.class) {
                                dtm.setValueAt(Boolean.valueOf(val), i - skipRows, j);
                            } else {
                                dtm.setValueAt(val, i - skipRows, j);
                            }
                        }
                    } catch (Exception exception) {
                        String msg = "colName=" + colName + ", colIndex=" + colIndex + ", val=" + val + ", rec=" + rec;
                        LOGGER.log(Level.SEVERE, msg, exception);
                        throw new RuntimeException(msg, exception);
                    }
                }
                try {
                    String name = rec.get("Name");
                    addHolderContentsRowIfNameNotFound(name);
                    PoseType pose = CRCLPosemath.toPoseType(
                            new PmCartesian(
                                    Double.parseDouble(rec.get(X_COLUMN_HEADER)),
                                    Double.parseDouble(rec.get(Y_COLUMN_HEADER)),
                                    Double.parseDouble(rec.get(Z_COLUMN_HEADER))
                            ),
                            new PmRpy(
                                    Math.toRadians(Double.parseDouble(rec.get(RX_COLUMN_HEADER))),
                                    Math.toRadians(Double.parseDouble(rec.get(RY_COLUMN_HEADER))),
                                    Math.toRadians(Double.parseDouble(rec.get(RZ_COLUMN_HEADER)))
                            ));
                    boolean approach = Boolean.parseBoolean(rec.get(APPROACH_COLUMN_HEADER));
                    String jointVals = rec.get(JOINTS_COLUMN_HEADER);
                    if (null != jointVals && jointVals.length() > 0 && approach) {
                        pddlActionToCrclGenerator.putToolChangerJointVals(name, jointVals);
                    }
                    if (!approach) {
                        toolHolderPoseMap.put(name, pose);
                    }
                } catch (Exception exception) {
                    LOGGER.log(Level.SEVERE, "rec=" + rec, exception);
                    throw new RuntimeException(exception);
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        clearEmptyToolChangerPoseRows();
    }

    private void loadHolderContentsMap() {
        if (null == propertiesFile || !propertiesFile.exists()) {
            return;
        }
        String filename = propertiesFile.getName() + ".holderContents.csv";
        File f = new File(propertiesFile.getParent(), filename);
        if (!f.exists()) {
            return;
        }
        clearToolHolderContentsTableModelListener();
        int lineNumber = 0;
        DefaultTableModel dtm = (DefaultTableModel) jTableHolderContents.getModel();
        readCsvPoseFileToTableAndMap(dtm, f, null, null);
        setToolHolderContentsTableModelListener();
    }

    private void loadToolOffsetMap() {
        if (null == propertiesFile || !propertiesFile.exists()) {
            return;
        }
        String filename = propertiesFile.getName() + ".toolOffsets.csv";
        File f = new File(propertiesFile.getParent(), filename);
        if (!f.exists()) {
            return;
        }
        ConcurrentHashMap<String, PoseType> toolOffsetMap
                = pddlActionToCrclGenerator.getToolOffsetMap();
        clearToolOffsetTableModelListener();
        int lineNumber = 0;
        DefaultTableModel dtm = (DefaultTableModel) jTableToolOffsets.getModel();
        readCsvPoseFileToTableAndMap(dtm, f, "ToolName", toolOffsetMap);
        clearEmptyToolOffsetPoseRows();
        loadToolOffsetsTableToMap();
        setToolOffsetTableModelListener();
    }

    private void loadTrayAttachOffsetMap() {
        if (null == propertiesFile || !propertiesFile.exists()) {
            return;
        }
        String filename = propertiesFile.getName() + ".trayAttachOffsets.csv";
        File f = new File(propertiesFile.getParent(), filename);
        if (!f.exists()) {
            return;
        }
        clearTrayAttachOffsetTableModelListener();
        int lineNumber = 0;
        DefaultTableModel dtm = (DefaultTableModel) jTableTrayAttachOffsets.getModel();
        readCsvPoseFileToTableAndMap(dtm, f, "TrayName", pddlActionToCrclGenerator.getTrayAttachOffsetsMap());
        loadTrayAttachOffsetsTableToMap();
        setTrayAttachOffsetTableModelListener();
    }

    private void readCsvPoseFileToTableAndMap(DefaultTableModel dtm, File f, @Nullable String nameRecord, @Nullable Map<String, PoseType> map) {
        dtm.setRowCount(0);
        try (CSVParser parser = new CSVParser(new FileReader(f), Utils.preferredCsvFormat())) {
            Map<String, Integer> headerMap = parser.getHeaderMap();
            List<CSVRecord> records = parser.getRecords();
            int skipRows = 0;
            for (int i = 0; i < records.size(); i++) {
                CSVRecord rec = records.get(i);
                String colName = dtm.getColumnName(0);
                Integer colIndex = headerMap.get(colName);
                if (colIndex == null) {
                    throw new IllegalArgumentException(f + " does not have field " + colName);
                }
                String val0 = rec.get(colIndex);
                if (!val0.equals(colName) && val0.length() > 0) {
                    break;
                }
                skipRows++;
            }
            dtm.setRowCount(records.size() - skipRows);
            ROW_LOOP:
            for (int i = skipRows; i < records.size(); i++) {
                CSVRecord rec = records.get(i);
                for (int j = 0; j < dtm.getColumnCount(); j++) {
                    String colName = dtm.getColumnName(j);
                    Integer colIndex = headerMap.get(colName);
                    if (colIndex == null) {
                        continue ROW_LOOP;
                    }
                    String val = rec.get(colIndex);
                    try {
                        if (null != val) {
                            if (val.equals(colName) || (j == 0 && val.length() < 1)) {
                                continue ROW_LOOP;
                            }
                            Class<?> colClass = dtm.getColumnClass(j);
                            if (colClass == Double.class) {
                                dtm.setValueAt(Double.valueOf(val), i - skipRows, j);
                            } else if (colClass == Boolean.class) {
                                dtm.setValueAt(Boolean.valueOf(val), i - skipRows, j);
                            } else {
                                dtm.setValueAt(val, i - skipRows, j);
                            }
                        }
                    } catch (Exception exception) {
                        String msg = "colName=" + colName + ", colIndex=" + colIndex + ", val=" + val + ", rec=" + rec;
                        LOGGER.log(Level.SEVERE, msg, exception);
                        throw new RuntimeException(msg, exception);
                    }
                }
                try {
                    if (null != nameRecord && null != map) {
                        String name = rec.get(nameRecord);
                        PoseType pose = CRCLPosemath.toPoseType(
                                new PmCartesian(
                                        Double.parseDouble(rec.get(X_COLUMN_HEADER)),
                                        Double.parseDouble(rec.get(Y_COLUMN_HEADER)),
                                        Double.parseDouble(rec.get(Z_COLUMN_HEADER))
                                ),
                                new PmRpy(
                                        Math.toRadians(Double.parseDouble(rec.get(RX_COLUMN_HEADER))),
                                        Math.toRadians(Double.parseDouble(rec.get(RY_COLUMN_HEADER))),
                                        Math.toRadians(Double.parseDouble(rec.get(RZ_COLUMN_HEADER)))
                                ));

                        map.put(name, pose);
                    }
                } catch (Exception exception) {
                    LOGGER.log(Level.SEVERE, "rec=" + rec, exception);
                    throw new RuntimeException(exception);
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private static final String JOINTS_COLUMN_HEADER = "Joints";
    private static final String RZ_COLUMN_HEADER = "Rz (deg)";
    private static final String RY_COLUMN_HEADER = "Ry (deg)";
    private static final String RX_COLUMN_HEADER = "Rx (deg)";
    private static final String Z_COLUMN_HEADER = "Z (mm)";
    private static final String Y_COLUMN_HEADER = "Y (mm)";
    private static final String X_COLUMN_HEADER = "X (mm)";

    private void saveToolChangerPoseMap() {
        try {
            clearEmptyToolChangerPoseRows();

            ConcurrentHashMap<String, PoseType> toolHolderPoseMap
                    = pddlActionToCrclGenerator.getToolHolderPoseMap();
            if (toolHolderPoseMap.isEmpty()) {
                return;
            }
            if (null == propertiesFile || !propertiesFile.exists()) {
                return;
            }
            toolChangerPoseMapFileName = propertiesFile.getName() + ".toolChangerPoses.csv";
            Utils.saveJTable(new File(propertiesFile.getParentFile(), toolChangerPoseMapFileName), jTableToolHolderPositions);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void saveToolHolderContentsMap() {
        try {
            if (null == propertiesFile || !propertiesFile.exists()) {
                return;
            }
            String fileName = propertiesFile.getName() + ".toolHolderContents.csv";
            Utils.saveJTable(new File(propertiesFile.getParentFile(), fileName), jTableHolderContents);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void saveToolOffsetPoseMap() {
        try {
            if (null == propertiesFile || !propertiesFile.exists()) {
                return;
            }
            String fileName = propertiesFile.getName() + ".toolOffsets.csv";
            Utils.saveJTable(new File(propertiesFile.getParentFile(), fileName), jTableToolOffsets);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void saveTrayAttachOffsetPoseMap() {
        try {
            if (null == propertiesFile || !propertiesFile.exists()) {
                return;
            }
            String fileName = propertiesFile.getName() + ".trayAttachOffsets.csv";
            Utils.saveJTable(new File(propertiesFile.getParentFile(), fileName), jTableTrayAttachOffsets);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Nullable
    public String getSelectedToolChangerPoseName() {
        int r = jTableToolHolderPositions.getSelectedRow();
        if (r < 0) {
            return null;
        }
        if (r >= jTableToolHolderPositions.getRowCount()) {
            return null;
        }
        return (String) jTableToolHolderPositions.getValueAt(r, 0);
    }

    public String[] getToolChangerNames() {
        Set<String> names = new TreeSet<>();
        names.add("");
        for (int i = 0; i < jTableToolHolderPositions.getRowCount(); i++) {
            Object o = jTableToolHolderPositions.getValueAt(i, 0);
            if (o instanceof String) {
                String s = (String) o;
                if (s.length() > 0) {
                    names.add(s);
                }
            }
        }
        return names.toArray(new String[names.size()]);
    }

    public String[] getToolNames() {
        Set<String> names = new TreeSet<>();
        names.add("");
        for (int i = 0; i < jTableToolOffsets.getRowCount(); i++) {
            Object o = jTableToolOffsets.getValueAt(i, 0);
            if (o instanceof String) {
                String s = (String) o;
                if (s.length() > 0) {
                    names.add(s);
                }
            }
        }
        return names.toArray(new String[names.size()]);
    }

    public String queryUserForToolHolderPosName(String qname) {
        return (String) JOptionPane.showInputDialog(
                this, // parentComponent
                "Tool Holder Pose Name?", // Object message
                aprsJFrame.getTaskName() + " " + aprsJFrame.getRobotName() + " " + qname + " choice", //  String title
                JOptionPane.QUESTION_MESSAGE, // messageType
                null,// icon 
                getToolChangerNames(), // selectionValues
                "" // initialSelectionValue
        );
    }

    public String queryUserForToolName(String qname) {
        return (String) JOptionPane.showInputDialog(
                this, // parentComponent
                "Tool Name?", // Object message
                aprsJFrame.getTaskName() + " " + aprsJFrame.getRobotName() + " " + qname + " choice", //  String title
                JOptionPane.QUESTION_MESSAGE, // messageType
                null,// icon 
                getToolNames(), // selectionValues
                "" // initialSelectionValue
        );
    }

    public int getToolChangerRow(String name, boolean approach) {
        for (int j = 0; j < jTableToolHolderPositions.getColumnCount(); j++) {
            System.out.println("j = " + j);
            String colName = jTableToolHolderPositions.getColumnName(j);
            System.out.println("colName = " + colName);
            TableColumn col = jTableToolHolderPositions.getColumn(colName);
            System.out.println("col = " + col);
        }
        for (int i = 0; i < jTableToolHolderPositions.getRowCount(); i++) {
            String entryName = (String) jTableToolHolderPositions.getValueAt(i, 0);
            Object entryApproachObject = jTableToolHolderPositions.getValueAt(i, APPROACH_COLUMN_INDEX);
            if (null != entryName && null != entryApproachObject) {
                boolean entryApproach = (boolean) entryApproachObject;
                if (entryApproach == approach && entryName.equals(name)) {
                    return i;
                }
            } else {
                System.err.println("Bad table entry: " + i + ", entryName=" + entryName + ", entryApproachObject=" + entryApproachObject);
            }
        }
        return -1;
    }

    public int getHolderContentsRow(String name) {
        for (int i = 0; i < jTableHolderContents.getRowCount(); i++) {
            String entryName = (String) jTableHolderContents.getValueAt(i, 0);
            if (null != entryName && entryName.equals(name)) {
                return i;
            }
        }
        return -1;
    }
    private static final int APPROACH_COLUMN_INDEX = 7;
    private static final String APPROACH_COLUMN_HEADER = "Approach";

    @Nullable
    private String getJointValsString() {
        CRCLStatusType stat = aprsJFrame.getCurrentStatus();
        if (null != stat && null != stat.getJointStatuses()) {
            List<JointStatusType> jointList = stat.getJointStatuses().getJointStatus();
            String jointVals
                    = jointStatusListToString(jointList);
            return jointVals;
        }
        return null;
    }


    private void jButtonRecordToolHolderPoseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRecordToolHolderPoseActionPerformed
        try {
            ConcurrentHashMap<String, PoseType> toolHolderPoseMap
                    = pddlActionToCrclGenerator.getToolHolderPoseMap();
            PoseType pose = aprsJFrame.getCurrentPose();
            if (null == pose) {
                JOptionPane.showMessageDialog(this, "Can not read current pose.");
                return;
            }
            String toolHolderPoseName = queryUserForToolHolderPosName("Record Pose");
            if (null == toolHolderPoseName
                    || toolHolderPoseName.length() < 1) {
                toolHolderPoseName = "toolChangerPose" + (jTableToolHolderPositions.getRowCount() + 1);
            }
//            toolChangerPose = pose;
            String name = toolHolderPoseName;
            PmRpy rpy = CRCLPosemath.toPmRpy(pose);
            String jointString = getJointValsString();
            updateToolChangePose(name, false, pose, rpy, jointString);
            toolHolderPoseMap.put(name, pose);
            PoseType approachPose = pddlActionToCrclGenerator.approachPoseFromToolChangerPose(pose);
            updateToolChangePose(name, true, approachPose, rpy, null);
            pddlActionToCrclGenerator.removeToolChangerJointVals(name);
            saveToolChangerPoseMap();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonRecordToolHolderPoseActionPerformed

    private void clearEmptyToolOffsetPoseRows() {
        DefaultTableModel dtm = (DefaultTableModel) jTableToolOffsets.getModel();
        for (int i = 0; i < dtm.getRowCount(); i++) {
            Object val = dtm.getValueAt(i, 0);
            if (val == null) {
                dtm.removeRow(i);
                i--;
                continue;
            }
            String valString = val.toString();
            if (valString.length() < 1) {
                dtm.removeRow(i);
                i--;
            }
        }
    }

    private void clearEmptyToolChangerPoseRows() {
        DefaultTableModel dtm = (DefaultTableModel) jTableToolHolderPositions.getModel();
        for (int i = 0; i < dtm.getRowCount(); i++) {
            Object val = dtm.getValueAt(i, 0);
            if (val == null) {
                dtm.removeRow(i);
                i--;
                continue;
            }
            String valString = val.toString();
            if (valString.length() < 1) {
                dtm.removeRow(i);
                i--;
            }
        }
    }

    private void updateToolChangePose(String name, boolean approach, PoseType pose, PmRpy rpy, @Nullable String jointString) {
        clearEmptyToolChangerPoseRows();
        if (name == null || name.length() < 1) {
            return;
        }
        int tableRowIndex = getToolChangerRow(name, approach);
        DefaultTableModel dtm = (DefaultTableModel) jTableToolHolderPositions.getModel();
        if (tableRowIndex < 0) {
            dtm.addRow(new Object[]{
                name,
                pose.getPoint().getX(),
                pose.getPoint().getY(),
                pose.getPoint().getZ(),
                Math.toDegrees(rpy.r),
                Math.toDegrees(rpy.p),
                Math.toDegrees(rpy.y),
                approach,
                jointString
            });
        } else {
            dtm.setValueAt(pose.getPoint().getX(), tableRowIndex, 1);
            dtm.setValueAt(pose.getPoint().getY(), tableRowIndex, 2);
            dtm.setValueAt(pose.getPoint().getZ(), tableRowIndex, 3);
            dtm.setValueAt(Math.toDegrees(rpy.r), tableRowIndex, 4);
            dtm.setValueAt(Math.toDegrees(rpy.p), tableRowIndex, 5);
            dtm.setValueAt(Math.toDegrees(rpy.y), tableRowIndex, 6);
            dtm.setValueAt(jointString, tableRowIndex, 8);
        }
        addHolderContentsRowIfNameNotFound(name);
        clearEmptyToolChangerPoseRows();
    }

    private void addHolderContentsRowIfNameNotFound(String name) {
        int holderContentsTableRowIndex = getHolderContentsRow(name);
        DefaultTableModel hcdtm = (DefaultTableModel) jTableHolderContents.getModel();
        if (holderContentsTableRowIndex < 0) {
            hcdtm.addRow(new Object[]{
                name,
                false,
                ""
            });
        }
    }

    private void jButtonGotoToolChangerApproachActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGotoToolChangerApproachActionPerformed
        try {
            if (null != currentPart) {
                this.jComboBoxManualObjectName.setSelectedItem(currentPart);
            }
            setReplanFromIndex(0);
            abortProgram();
            lookForCount++;
            clearAll();
            autoStart = true;
            cancelRunProgramFuture();
            String name = queryUserForToolHolderPosName("Goto Approach");
            if (null == name) {
                return;
            }
//            toolChangerPoseName = name;
            ConcurrentHashMap<String, PoseType> toolHolderPoseMap
                    = pddlActionToCrclGenerator.getToolHolderPoseMap();
            PoseType pose = toolHolderPoseMap.get(name);
            if (null == pose) {
                JOptionPane.showMessageDialog(this, "no pose for " + name + " in " + toolHolderPoseMap);
                return;
            }
//            toolChangerPose = pose;
            runningProgramFuture = this.gotoToolChangerApproach(name, pose);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, null, e);
            showExceptionInProgram(e);
        }
    }//GEN-LAST:event_jButtonGotoToolChangerApproachActionPerformed

    private void jButtonGotoToolChangerPoseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGotoToolChangerPoseActionPerformed
        try {
            if (null != currentPart) {
                this.jComboBoxManualObjectName.setSelectedItem(currentPart);
            }
            setReplanFromIndex(0);
            abortProgram();
            lookForCount++;
            clearAll();
            autoStart = true;
            cancelRunProgramFuture();
            String name = queryUserForToolHolderPosName("Goto Changer");
            if (null == name) {
                return;
            }
            ConcurrentHashMap<String, PoseType> toolHolderPoseMap
                    = pddlActionToCrclGenerator.getToolHolderPoseMap();
//            toolChangerPoseName = name;
            PoseType pose = toolHolderPoseMap.get(name);
            if (null == pose) {
                JOptionPane.showMessageDialog(this, "no pose for " + name + " in " + toolHolderPoseMap);
                return;
            }
//            toolChangerPose = pose;
            runningProgramFuture = this.gotoToolChangerPose(name, pose);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, null, e);
            showExceptionInProgram(e);
        }
    }//GEN-LAST:event_jButtonGotoToolChangerPoseActionPerformed

    private void jButtonDropToolActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDropToolActionPerformed
        try {
            if (null != currentPart) {
                this.jComboBoxManualObjectName.setSelectedItem(currentPart);
            }
            setReplanFromIndex(0);
            abortProgram();
            lookForCount++;
            clearAll();
            autoStart = true;
            cancelRunProgramFuture();
            String toolHolderPoseName = queryUserForToolHolderPosName("Drop Tool");
            if (null == toolHolderPoseName || toolHolderPoseName.equals("Default") || toolHolderPoseName.length() < 1) {
                return;
            }
            ConcurrentHashMap<String, PoseType> toolHolderPoseMap
                    = pddlActionToCrclGenerator.getToolHolderPoseMap();
            PoseType toolHolderPose = toolHolderPoseMap.get(toolHolderPoseName);
            if (null == toolHolderPose) {
                JOptionPane.showMessageDialog(this, "No pose known for " + toolHolderPoseName + " in " + toolHolderPoseMap);
                return;
            }
            runningProgramFuture = this.dropTool(toolHolderPoseName, toolHolderPose);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, null, e);
            showExceptionInProgram(e);
        }
    }//GEN-LAST:event_jButtonDropToolActionPerformed

    private void jButtonPickupToolActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPickupToolActionPerformed
        try {
            if (null != currentPart) {
                this.jComboBoxManualObjectName.setSelectedItem(currentPart);
            }
            setReplanFromIndex(0);
            abortProgram();
            lookForCount++;
            clearAll();
            autoStart = true;
            cancelRunProgramFuture();
            String holderPosName = queryUserForToolHolderPosName("Pickup Tool: Which tool holder position? ");
            if (null == holderPosName || holderPosName.length() < 1) {
                return;
            }
            String newToolName = queryUserForToolName("What tool will be in the robot?");
            if (null == newToolName || newToolName.length() < 1) {
                return;
            }
//            toolChangerPoseName = holderPosName;
            ConcurrentHashMap<String, PoseType> toolHolderPoseMap
                    = pddlActionToCrclGenerator.getToolHolderPoseMap();
            PoseType pose = toolHolderPoseMap.get(holderPosName);
            if (null == pose) {
                JOptionPane.showMessageDialog(this, "no pose for " + holderPosName + " in " + toolHolderPoseMap);
                return;
            }
            runningProgramFuture = this.pickupTool(holderPosName, pose, newToolName);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, null, e);
            showExceptionInProgram(e);
        }
    }//GEN-LAST:event_jButtonPickupToolActionPerformed

    private void jTextFieldToolChangerApproachZOffsetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldToolChangerApproachZOffsetActionPerformed
        pddlActionToCrclGenerator.setApproachToolChangerZOffset(Double.parseDouble(jTextFieldToolChangerApproachZOffset.getText()));
    }//GEN-LAST:event_jTextFieldToolChangerApproachZOffsetActionPerformed

    private void jButtonDeleteToolHolderPoseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeleteToolHolderPoseActionPerformed
        clearEmptyToolChangerPoseRows();
        String nameToDelete = queryUserForToolHolderPosName("Delete Pose");
        DefaultTableModel model = (DefaultTableModel) jTableToolHolderPositions.getModel();
        for (int i = 0; i < jTableToolHolderPositions.getRowCount(); i++) {
            String nameFromTable = (String) jTableToolHolderPositions.getValueAt(i, 0);
            if (null == nameFromTable || nameFromTable.equals(nameToDelete)) {
                model.removeRow(i);
                i--;
            }
        }
        ConcurrentHashMap<String, PoseType> toolHolderPoseMap
                = pddlActionToCrclGenerator.getToolHolderPoseMap();
        toolHolderPoseMap.remove(nameToDelete);
        clearEmptyToolChangerPoseRows();
        Utils.autoResizeTableColWidths(jTableToolHolderPositions);
    }//GEN-LAST:event_jButtonDeleteToolHolderPoseActionPerformed

    private void jButtonAddToolHolderPoseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddToolHolderPoseActionPerformed
        try {
            clearEmptyToolChangerPoseRows();
            PoseType pose = aprsJFrame.getCurrentPose();
            if (null == pose || null == pose.getPoint()) {
                JOptionPane.showMessageDialog(this, "Can not read current pose.");
                return;
            }
            String nameToAdd = JOptionPane.showInputDialog("New tool changer position name");
            ConcurrentHashMap<String, PoseType> toolHolderPoseMap
                    = pddlActionToCrclGenerator.getToolHolderPoseMap();
            if (nameToAdd != null && nameToAdd.length() > 0) {
                if (toolHolderPoseMap.containsKey(nameToAdd) || Arrays.stream(getToolChangerNames()).anyMatch(x -> nameToAdd.equals(x))) {
                    JOptionPane.showMessageDialog(this, nameToAdd + " already added.");
                    return;
                }
                PmRpy rpy = CRCLPosemath.toPmRpy(pose);
                String jointString = getJointValsString();
                updateToolChangePose(nameToAdd, false, pose, rpy, jointString);
                toolHolderPoseMap.put(nameToAdd, pose);
                PoseType approachPose = pddlActionToCrclGenerator.approachPoseFromToolChangerPose(pose);
                updateToolChangePose(nameToAdd, true, approachPose, rpy, null);
                clearEmptyToolChangerPoseRows();
                Utils.autoResizeTableColWidths(jTableToolHolderPositions);
                saveToolChangerPoseMap();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonAddToolHolderPoseActionPerformed

    private void jButtonRecordToolHolderApproachActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRecordToolHolderApproachActionPerformed
        try {
            PoseType pose = aprsJFrame.getCurrentPose();
            if (null == pose) {
                JOptionPane.showMessageDialog(this, "Can not read current pose.");
                return;
            }
            String toolHolderPoseName = queryUserForToolHolderPosName("Record Approach");
            if (null == toolHolderPoseName
                    || toolHolderPoseName.length() < 1) {
                toolHolderPoseName = "toolChangerPose" + (jTableToolHolderPositions.getRowCount() + 1);
            }

            PmRpy rpy = CRCLPosemath.toPmRpy(pose);
            String jointString = getJointValsString();
            updateToolChangePose(toolHolderPoseName, true, pose, rpy, jointString);
            if (null != jointString) {
                pddlActionToCrclGenerator.putToolChangerJointVals(toolHolderPoseName, jointString);
            }
            saveToolChangerPoseMap();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonRecordToolHolderApproachActionPerformed

    private static double getDoubleValueAt(DefaultTableModel dtm, int row, int col) {
        Object o = dtm.getValueAt(row, col);
        if (o == null) {
            throw new IllegalStateException("null value in table at " + row + "," + col);
        }
        if (o instanceof java.lang.Double) {
            return ((java.lang.Double) o);
        }
        throw new IllegalStateException(" value in table at " + row + "," + col + " is not of class Double : o=" + o);
    }

    private void loadTrayAttachOffsetsTableToMap() {
        try {
            DefaultTableModel dtm = (DefaultTableModel) jTableTrayAttachOffsets.getModel();
            Map<String, PoseType> map = pddlActionToCrclGenerator.getTrayAttachOffsetsMap();
            map.clear();
            for (int i = 0; i < dtm.getRowCount(); i++) {
                Object v0 = dtm.getValueAt(i, 0);
                if (null != v0 && v0 instanceof String) {
                    String name = (String) v0;
                    name = name.trim();
                    if (name.length() < 1) {
                        continue;
                    }
                    double x = getDoubleValueAt(dtm, i, 1);
                    double y = getDoubleValueAt(dtm, i, 2);
                    double z = getDoubleValueAt(dtm, i, 3);
                    double roll = getDoubleValueAt(dtm, i, 4);
                    roll = Math.toRadians(roll);
                    double pitch = getDoubleValueAt(dtm, i, 5);
                    pitch = Math.toRadians(pitch);
                    double yaw = getDoubleValueAt(dtm, i, 6);
                    yaw = Math.toRadians(yaw);
                    PoseType pose = CRCLPosemath.toPoseType(new PmCartesian(x, y, z), new PmRpy(roll, pitch, yaw));
                    map.put(name, pose);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, null, e);
        }
    }

    private void loadToolOffsetsTableToMap() {
        try {
            DefaultTableModel dtm = (DefaultTableModel) jTableToolOffsets.getModel();
            Map<String, PoseType> map = pddlActionToCrclGenerator.getToolOffsetMap();
            map.clear();
            for (int i = 0; i < dtm.getRowCount(); i++) {
                Object v0 = dtm.getValueAt(i, 0);
                if (null != v0 && v0 instanceof String) {
                    String name = (String) v0;
                    name = name.trim();
                    if (name.length() < 1) {
                        continue;
                    }
                    double x = getDoubleValueAt(dtm, i, 1);
                    double y = getDoubleValueAt(dtm, i, 2);
                    double z = getDoubleValueAt(dtm, i, 3);
                    double roll = getDoubleValueAt(dtm, i, 4);
                    roll = Math.toRadians(roll);
                    double pitch = getDoubleValueAt(dtm, i, 5);
                    pitch = Math.toRadians(pitch);
                    double yaw = getDoubleValueAt(dtm, i, 6);
                    yaw = Math.toRadians(yaw);
                    PoseType pose = CRCLPosemath.toPoseType(new PmCartesian(x, y, z), new PmRpy(roll, pitch, yaw));
                    map.put(name, pose);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, null, e);
        }
    }

    private void jButtonAddToolOffsetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddToolOffsetActionPerformed
        DefaultTableModel dtm = (DefaultTableModel) jTableToolOffsets.getModel();
        dtm.addRow(new Object[]{"tool" + (dtm.getRowCount() + 1), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, ""});
        clearToolOffsetTableModelListener();
        Utils.autoResizeTableColWidths(jTableToolOffsets);
        saveToolOffsetPoseMap();
        loadToolOffsetsTableToMap();
        setToolOffsetTableModelListener();
    }//GEN-LAST:event_jButtonAddToolOffsetActionPerformed

    private void jButtonDeleteToolOffsetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeleteToolOffsetActionPerformed
        String nameToDelete = queryUserForToolName("Delete Pose");
        DefaultTableModel model = (DefaultTableModel) jTableToolOffsets.getModel();
        for (int i = 0; i < jTableToolOffsets.getRowCount(); i++) {
            String nameFromTable = (String) jTableToolOffsets.getValueAt(i, 0);
            if (null == nameFromTable || nameFromTable.equals(nameToDelete)) {
                model.removeRow(i);
                i--;
            }
        }
        clearToolOffsetTableModelListener();
        Utils.autoResizeTableColWidths(jTableToolOffsets);
        saveToolOffsetPoseMap();
        loadToolOffsetsTableToMap();
        setToolOffsetTableModelListener();
    }//GEN-LAST:event_jButtonDeleteToolOffsetActionPerformed

    private void jButtonUpdatePoseCacheActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonUpdatePoseCacheActionPerformed
        try {
            pddlActionToCrclGenerator.clearPoseCache();
            List<PhysicalItem> newItems = pddlActionToCrclGenerator.newPoseItems("userRequestedPoseUpdate");
            DefaultComboBoxModel<String> objectCbm
                    = (DefaultComboBoxModel<String>) jComboBoxManualObjectName.getModel();
            objectCbm.removeAllElements();
            DefaultComboBoxModel<String> slotCbm
                    = (DefaultComboBoxModel<String>) jComboBoxManualSlotName.getModel();
            slotCbm.removeAllElements();
            newItems = new ArrayList<>(newItems);
            Collections.sort(newItems, Comparators.fromFunctions(PhysicalItem::getFullName));
            for (PhysicalItem item : newItems) {
                String fullName = item.getFullName();
                if (null != fullName) {
                    switch (item.getType()) {
                        case "P":
                        case "KT":
                        case "PT":
                            objectCbm.addElement(fullName);
                            break;

                        case "ES":
                        case "SLOT":
                            slotCbm.addElement(fullName);
                            break;
                    }
                }
            }
            updatePositionCacheTable();
            jComboBoxManualObjectName.setModel(objectCbm);
            jComboBoxManualSlotName.setModel(slotCbm);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonUpdatePoseCacheActionPerformed

    private void jButtonSetCurrentToolActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSetCurrentToolActionPerformed
        try {
            String newToolName = queryUserForToolName("Which tool is currently in the robot? ");
            if (null != newToolName && newToolName.length() > 0) {
                pddlActionToCrclGenerator.setToolName(newToolName);
//                jTextFieldCurrentToolName.setText(newToolName);
//                PoseType newPose = pddlActionToCrclGenerator.getToolOffsetMap().get(newToolName);
//                if (null != newPose) {
//                    PmRpy rpy = CRCLPosemath.toPmRpy(newPose);
//                    PmCartesian tran = CRCLPosemath.toPmCartesian(newPose.getPoint());
//                    String offsetText
//                            = String.format("X=%.3f,Y=%.3f,Z=%.3f,roll=%.3f,pitch=%.3f,yaw=%.3f",
//                                    tran.x, tran.y, tran.z,
//                                    Math.toDegrees(rpy.r), Math.toDegrees(rpy.p), Math.toDegrees(rpy.y));
//                    jTextFieldCurrentToolOffset.setText(offsetText);
//                    pddlActionToCrclGenerator.setToolOffsetPose(newPose);
//                }
            }
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, null, exception);
        }
    }//GEN-LAST:event_jButtonSetCurrentToolActionPerformed

    private void jButtonAddTrayAttachActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddTrayAttachActionPerformed
        DefaultTableModel dtm = (DefaultTableModel) jTableTrayAttachOffsets.getModel();
        dtm.addRow(new Object[]{"tray" + (dtm.getRowCount() + 1), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, ""});
        clearTrayAttachOffsetTableModelListener();
        Utils.autoResizeTableColWidths(jTableToolOffsets);
        saveTrayAttachOffsetPoseMap();
        loadTrayAttachOffsetsTableToMap();
        setTrayAttachOffsetTableModelListener();
    }//GEN-LAST:event_jButtonAddTrayAttachActionPerformed

    private void clearPoseCache() {
        pddlActionToCrclGenerator.clearPoseCache();
        updatePositionCacheTable();
    }

    private void queryLogFileName() {
        if (!new File(recordCsvName).exists()) {
            newLogFileName();
        }
    }

    private void newLogFileName() throws HeadlessException {
        recordCsvName = jTextFieldLogFilename.getText();
        JFileChooser chooser = new JFileChooser();
        if (null != propertiesFile) {
            File parentFile = propertiesFile.getParentFile();
            if (null != parentFile) {
                chooser.setCurrentDirectory(parentFile);
            }
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = chooser.getSelectedFile();
                recordCsvName = f.getCanonicalPath();
                if (!recordCsvName.endsWith(".csv")) {
                    recordCsvName += ".csv";
                }
                jTextFieldLogFilename.setText(recordCsvName);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    private int @Nullable [] crclIndexes = null;

    public void setCrclIndexes(int indexes[]) {
        DefaultTableModel model = (DefaultTableModel) jTablePddlOutput.getModel();
        for (int i = 0; i < indexes.length; i++) {
            if (i >= model.getRowCount()) {
                break;
            }
            if (!Objects.equals(model.getValueAt(i, 1), indexes[i])) {
                model.setValueAt(indexes[i], i, 1);
            }
        }
        this.crclIndexes = indexes;
    }

    public void setPddlLabelss(String labels[]) {
        DefaultTableModel model = (DefaultTableModel) jTablePddlOutput.getModel();
        for (int i = 0; i < labels.length; i++) {
            if (i >= model.getRowCount()) {
                break;
            }
            if (null != labels[i]) {
                if (!Objects.equals(model.getValueAt(i, 2), labels[i])) {
                    model.setValueAt(labels[i], i, 2);
                }
            }
        }
    }

    public void setPddlTakenParts(@Nullable String parts[]) {
        DefaultTableModel model = (DefaultTableModel) jTablePddlOutput.getModel();
        for (int i = 0; i < parts.length; i++) {
            if (i >= model.getRowCount()) {
                break;
            }
            if (null != parts[i]) {
                if (!Objects.equals(model.getValueAt(i, 6), parts[i])) {
                    model.setValueAt(parts[i], i, 6);
                }
            }
        }
    }

    public void reloadPddlActions(List<PddlAction> l) {
        DefaultTableModel model = (DefaultTableModel) jTablePddlOutput.getModel();
        for (int i = 0; i < l.size(); i++) {
            if (i >= model.getRowCount()) {
                break;
            }
            PddlAction act = l.get(i);
            if (null != act) {
                if (!Objects.equals(model.getValueAt(i, 2), act.getType())) {
                    model.setValueAt(act.getType(), i, 2);
                }
                String argsString = Arrays.toString(act.getArgs());
                if (!Objects.equals(model.getValueAt(i, 3), argsString)) {
                    model.setValueAt(argsString, i, 3);
                }
            }
        }
    }

    boolean started = false;

    private final ConcurrentLinkedDeque<Runnable> safeAbortRunnablesVector = new ConcurrentLinkedDeque<>();

    private void generateCrclAsyncWithCatch() {
        try {
            if (null != runningProgramFuture) {
                runningProgramFuture.cancel(true);
            }
            runningProgramFuture = generateCrclAsync();
        } catch (IOException | IllegalStateException | SQLException ex) {
            replanStarted.set(false);
            abortProgram();
            showExceptionInProgram(ex);
//            actionToCrclLabels[lastIndex] = "Error";
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Nullable
    private String errorString;

    /**
     * Get the value of errorString
     *
     * @return the value of errorString
     */
    public String getErrorString() {
        return errorString != null ? errorString : "";
    }

    /**
     * Set the value of errorString
     *
     * @param errorString new value of errorString
     */
    public void setErrorString(@Nullable String errorString) {
        if (null == errorString) {
            pddlActionToCrclGenerator.reset();
        }
        this.errorString = errorString;
    }

    private void showExceptionInProgram(final java.lang.Exception ex) {
        CRCLProgramType program = createEmptyProgram();
        List<MiddleCommandType> cmds = program.getMiddleCommand();
        MessageType message = new MessageType();
        setCommandId(message);
        message.setMessage(ex.toString());
        cmds.add(message);
        setEndCanonCmdId(program);
        loadProgramToTable(program);
        jTableCrclProgram.setBackground(Color.red);
        jTabbedPane1.setSelectedComponent(jPanelCrcl);
        setErrorString(ex.toString());
        if (null != aprsJFrame) {
            aprsJFrame.setTitleErrorString(errorString);
        }
    }

    @Nullable
    private String crclProgName = null;
    @Nullable
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
                LOGGER.log(Level.SEVERE, null, ex);
                XFuture<Boolean> ret = new XFuture<Boolean>("recursiveSupplyBoolean");
                ret.completeExceptionally(ex);
                return ret;
            }
        } else {
            return XFuture.completedFuture(false);
        }
    }

    public static XFuture<Boolean> ifOk(boolean ok, Supplier<XFuture<Boolean>> thenSupplier) {
        return ok ? thenSupplier.get() : XFuture.completedFuture(false);
    }

    private XFuture<Boolean> recursiveApplyGenerateCrcl(boolean prevSuccess) {
        if (prevSuccess) {
            try {
                return generateCrclAsync();
            } catch (IOException | IllegalStateException | SQLException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
                XFuture<Boolean> ret = new XFuture<Boolean>("recursiveApplyGenerateCrclException");
                ret.completeExceptionally(ex);
                return ret;
            }
        } else {
            return XFuture.completedFuture(false);
        }
    }
    private int crclStartActionIndex = -1;
    private int crclEndActionIndex = -1;

    private void takeSimViewSnapshot(File f, @Nullable PoseType pose, @Nullable String label) throws IOException {
        if (null != aprsJFrame) {
            aprsJFrame.takeSimViewSnapshot(f, pose, label);
        }
    }

    private volatile long doSafeAbortTime = 0;
    private final AtomicInteger doSafeAbortCount = new AtomicInteger(0);
    @Nullable
    private volatile String lastCheckAbortCurrentPart = null;
    private volatile boolean lastCheckAbortSafeAbortRequested = false;
    private volatile long lastCheckSafeAbortTime = 0;

    public XFuture<Boolean> checkSafeAbortAsync(Supplier<XFuture<Boolean>> supplier, int startSafeAbortRequestCount) {

        if (aprsJFrame.isRunningCrclProgram()) {
            System.err.println("crclProgramStill Running");
            System.out.println("aprsJFrame.isRunningCrclProgram() = " + aprsJFrame.isRunningCrclProgram());
            System.out.println("aprsJFrame.getCrclRunProgramFuture() = " + aprsJFrame.getCrclRunProgramFuture());
            System.out.println("aprsJFrame.getCrclRunProgramThread() = " + aprsJFrame.getCrclRunProgramThread());
            throw new IllegalStateException("crclProgramStill Running");
        }
        boolean doSafeAbort = checkSafeAbort(startSafeAbortRequestCount);
        if (doSafeAbort) {
            return XFuture.completedFuture(false);
        }
        return supplier.get();
    }

    private boolean checkSafeAbort(int startSafeAbortRequestCount) {
        boolean doSafeAbort;
        synchronized (this) {
            lastCheckAbortCurrentPart = currentPart;
            boolean safeAbortRequested = (safeAbortRequestCount.get() != startSafeAbortRequestCount);
            if (safeAbortRequested != aprsJFrame.isAborting()) {
                System.err.println("safeAbortRequested=" + safeAbortRequested + ", aprsJFrame.isAborting()=" + aprsJFrame.isAborting());
                safeAbortRequested = true;
            }
            lastCheckAbortSafeAbortRequested = safeAbortRequested;
            lastCheckSafeAbortTime = System.currentTimeMillis();
            if (safeAbortRequested && null == currentPart) {
                safeAbortRequested = false;
                autoStart = false;
                doSafeAbort = true;
            } else {
                doSafeAbort = false;
            }
        }
        if (doSafeAbort) {
            doSafeAbortTime = System.currentTimeMillis();
            doSafeAbortCount.incrementAndGet();
            this.abortProgram();
            try {
                if (pddlActionToCrclGenerator.isTakeSnapshots() && aprsJFrame.snapshotsEnabled()) {
                    takeSimViewSnapshot(aprsJFrame.createTempFile("-safe-abort-", ".PNG"), null, "");
                }
            } catch (IOException iOException) {
                iOException.printStackTrace();
            }
        }
        return doSafeAbort;
    }

    @Nullable
    private ExecutorService generateCrclService = null;

    @Nullable
    public ExecutorService getGenerateCrclService() {
        return generateCrclService;
    }

    public void setGenerateCrclService(ExecutorService generateCrclService) {
        this.generateCrclService = generateCrclService;
    }

    private boolean generateCrcl(String comment, int startSafeAbortRequestCount)
            throws IOException, IllegalStateException, SQLException, JAXBException, InterruptedException, ExecutionException, PendantClientInner.ConcurrentBlockProgramsException, CRCLException, PmException {
        boolean doSafeAbort = checkSafeAbort(startSafeAbortRequestCount);
        if (doSafeAbort) {
            return atLastAction();
        }
        checkDbSupplierPublisher();
        int abortReplanFromIndex = getReplanFromIndex();
        if (jCheckBoxEnableOptaPlanner.isSelected() && abortReplanFromIndex == 0) {
            this.opDisplayJPanelInput.setOpActionPlan(null);
            this.opDisplayJPanelSolution.setOpActionPlan(null);
            this.opDisplayJPanelInput.setLabel("Input");
            this.opDisplayJPanelSolution.setLabel("Output");
        }
        List<Integer> lil = new ArrayList<>();
        final int li0 = pddlActionToCrclGenerator.getLastIndex();
        lil.add(li0);
        final int rpi0 = getReplanFromIndex();
//        if (rpi0 < li0 && (rpi0 != 0 || !readyForNewActionsList())) {
//            System.err.println("replanFromIndex less than lastIndex unexpectedly,  li0=" + li0 + ",rpi0=" + rpi0);
//        }
        CRCLProgramType program = pddlActionSectionToCrcl(0);
        final int li1 = pddlActionToCrclGenerator.getLastIndex();
        lil.add(li1);
        if (li1 < li0 && li1 < rpi0) {
            System.err.println("lastIndex decreased: li0=" + li0 + ",li1=" + li1);
        }
        final int rpi1 = getReplanFromIndex();
        if (rpi1 <= li1 && rpi1 != actionsList.size() - 1) {
            System.err.println("replanFromIndex <= lastIndex: replanFromIndex=" + rpi1 + ",li1=" + li1);
        }
        if (!autoStart) {
            setCrclProgram(crclProgram);
            return true;
        }
        boolean replanAfterCrclBlock
                = (!pddlActionToCrclGenerator.atLastIndex())
                && jCheckBoxReplan.isSelected();
        lastReplanAfterCrclBlock = replanAfterCrclBlock;
        int sectionNumber = 1;
        while (replanAfterCrclBlock && autoStart) {
            final int rpi2 = getReplanFromIndex();
            doSafeAbort = checkSafeAbort(startSafeAbortRequestCount);
            if (rpi2 != getReplanFromIndex()) {
                System.err.println("replanFromIndex changed unexpectedly : replanFromIndex=" + replanFromIndex + ",rpi1=" + rpi2);
            }
            if (doSafeAbort) {
                setReplanFromIndex(abortReplanFromIndex, true);
                return atLastAction();
            }
            boolean emptyProgram = program.getMiddleCommand().isEmpty();
            boolean nextReplanAfterCrclBlock
                    = pddlActionToCrclGenerator.getLastIndex() < actionsList.size() - 1
                    && jCheckBoxReplan.isSelected();
            if (emptyProgram) {
                if (!nextReplanAfterCrclBlock) {
                    break;
                } else {
                    System.out.println("pddlActionToCrclGenerator.getLastIndex() = " + pddlActionToCrclGenerator.getLastIndex());
                    System.out.println("actionsList = " + actionsList);
                    System.err.println("CRCL Program was empty but actions not complete.");
                }
            } else if (!runCrclProgram(program)) {
                checkSafeAbort(startSafeAbortRequestCount);
                return false;
            }

            doSafeAbort = checkSafeAbort(startSafeAbortRequestCount);
            if (doSafeAbort) {
                return atLastAction();
            }
            if (rpi2 != getReplanFromIndex()) {
                System.err.println("replanFromIndex changed unexpectedly : replanFromIndex=" + replanFromIndex + ",rpi1=" + rpi2);
            }
            abortReplanFromIndex = getReplanFromIndex();
            final int li3 = pddlActionToCrclGenerator.getLastIndex();
            program = pddlActionSectionToCrcl(sectionNumber++);
            final int li4 = pddlActionToCrclGenerator.getLastIndex();
            lil.add(li4);
            if (li4 < li3) {
                System.err.println("lastIndex decreased: li3=" + li3 + ",li4=" + li4);
            }
            final int rpi3 = getReplanFromIndex();
            if (rpi3 <= li4 && rpi3 != actionsList.size() - 1) {
                System.err.println("replanFromIndex <= lastIndex: replanFromIndex=" + rpi3 + ",li4=" + li4);
            }
            replanAfterCrclBlock
                    = nextReplanAfterCrclBlock;
            lastReplanAfterCrclBlock = replanAfterCrclBlock;
        }
        if (!replanAfterCrclBlock && autoStart) {
            doSafeAbort = checkSafeAbort(startSafeAbortRequestCount);
            if (doSafeAbort) {
                setReplanFromIndex(abortReplanFromIndex, true);
                return atLastAction();
            }
            if (!runCrclProgram(program)) {
                checkSafeAbort(startSafeAbortRequestCount);
                return false;
            }
        }
        if (!checkSafeAbort(startSafeAbortRequestCount)) {
            warnIfNewActionsNotReady();
        }
        return true;
    }

    private boolean atLastAction() {
        boolean ret = pddlActionToCrclGenerator.atLastIndex();
//        if (ret) {
//            System.out.println("pddlActionToCrclGenerator.getLastIndex() = " + pddlActionToCrclGenerator.getLastIndex());
//            System.out.println("actionsList.size() = " + actionsList.size());
//        }
        return ret;

    }

    private XFuture<Boolean> generateCrclAsync() throws IOException, IllegalStateException, SQLException {

        int startSafeAbortRequestCount = safeAbortRequestCount.get();

        ExecutorService service = this.generateCrclService;
        if (null == service) {
            service = aprsJFrame.getRunProgramService();
            this.generateCrclService = service;
        }
        ExecutorService genCrclService = service;
        String taskName = "generateCrcl(" + aprsJFrame.getTaskName() + ").doPddlActionsSection(" + pddlActionToCrclGenerator.getLastIndex() + " out of " + actionsList.size() + ")";

        return checkSafeAbortAsync(() -> {
            try {
                return checkDbSupplierPublisherAsync()
                        .thenComposeAsync(taskName,
                                x -> doPddlActionsSectionAsync(startSafeAbortRequestCount, 0),
                                genCrclService);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
                XFuture<Boolean> xf = new XFuture<>("generateCrclException");
                xf.completeExceptionally(ex);
                return xf;
            }
        }, startSafeAbortRequestCount
        );
    }

    private CRCLProgramType pddlActionSectionToCrcl(int sectionNumber) throws IllegalStateException, SQLException, InterruptedException, ExecutionException, ExecutionException, ExecutionException, IOException, PendantClientInner.ConcurrentBlockProgramsException, CRCLException, PmException {
        Map<String, String> options = getTableOptions();
        final int rpi = getReplanFromIndex();
        if (rpi < 0 || rpi > actionsList.size()) {
            setReplanFromIndex(0);
        }
        pddlActionToCrclGenerator.setPositionMaps(getPositionMaps());
        if (jCheckBoxEnableOptaPlanner.isSelected()) {
            if (null == solver) {
                synchronized (solverFactory) {
                    solver = solverFactory.buildSolver();
                }
            }
            pddlActionToCrclGenerator.setSolver(solver);
            pddlActionToCrclGenerator.setOpDisplayJPanelInput(opDisplayJPanelInput);
            pddlActionToCrclGenerator.setOpDisplayJPanelSolution(opDisplayJPanelSolution);
        } else {
            pddlActionToCrclGenerator.setSolver(null);
        }
        crclStartActionIndex = this.getReplanFromIndex();
        currentActionIndex = crclStartActionIndex;
        if (null != aprsJFrame) {
            aprsJFrame.updateTitle();
        }
        CRCLProgramType program = createEmptyProgram();
        List<MiddleCommandType> cmds;
        final int startReplanFromIndex = this.getReplanFromIndex();
        synchronized (actionsList) {
            cmds = pddlActionToCrclGenerator.generate(actionsList, startReplanFromIndex, options, safeAbortRequestCount.get());
            resetReadOnlyActionsList();
        }
        int indexes[] = pddlActionToCrclGenerator.getActionToCrclIndexes();
        int indexesCopy[] = Arrays.copyOf(indexes, indexes.length);
        String labels[] = pddlActionToCrclGenerator.getActionToCrclLabels();
        final String labelsCopy[] = Utils.copyOfNonNullsOnly(String.class, labels);
        @Nullable
        String takenPartNames[] = pddlActionToCrclGenerator.getActionToCrclTakenPartsNames();
        final @Nullable
        String takenPartNamesCopy[] = Arrays.copyOf(takenPartNames, takenPartNames.length);
        javax.swing.SwingUtilities.invokeLater(() -> {
            setCrclIndexes(indexesCopy);
            setPddlLabelss(labelsCopy);
            setPddlTakenParts(takenPartNamesCopy);
            reloadPddlActions(readOnlyActionsList);
            autoResizeTableColWidths(jTablePddlOutput);
        });

        program.setName(getActionsCrclName());
        lastCrclProgName = crclProgName;
        crclProgName = program.getName();
        crclEndActionIndex = pddlActionToCrclGenerator.getLastIndex();
        final int lastIndex = pddlActionToCrclGenerator.getLastIndex();
        if (lastIndex < 0) {
            throw new IllegalStateException("lastIndex=" + lastIndex);
        }
        if (lastIndex < startReplanFromIndex) {
            throw new IllegalStateException("lastIndex=" + lastIndex + ",startReplanFromIndex=" + startReplanFromIndex);
        }
        if (lastIndex < actionsList.size() - 1) {
            setReplanFromIndex(lastIndex + 1);
        } else {
            setReplanFromIndex(actionsList.size() - 1);
        }
        jTextFieldIndex.setText(Integer.toString(getReplanFromIndex()));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        setEndCanonCmdId(program);
        updatePositionCacheTable();
        return program;
    }

    private void setEndCanonCmdId(CRCLProgramType program) {
        setCommandId(program.getEndCanon());
        long initCmdId = program.getInitCanon().getCommandID();
        long endCmdId = program.getEndCanon().getCommandID();
        int midSize = program.getMiddleCommand().size();
        if (midSize > 0) {
            long firstMidCmdId = program.getMiddleCommand().get(0).getCommandID();
            if (firstMidCmdId != initCmdId + 1) {
                System.err.println("firstMidCmdId != initCmdId+1 : " + firstMidCmdId + "!= " + (initCmdId + 1));
            }
            long lastMidCmdId = program.getMiddleCommand().get(midSize - 1).getCommandID();
            if (lastMidCmdId != initCmdId + midSize) {
                System.err.println("lastMidCmdId != initCmdId+midSize : " + lastMidCmdId + "!= " + (initCmdId + midSize));
            }
        }
        long expectedEndCmdId = initCmdId + midSize + 1;
        if (endCmdId != expectedEndCmdId) {
            System.err.println("EndCanon Id " + endCmdId + " doesn't match InitCanon id " + initCmdId + " + 1+ size of middle commands " + midSize);
        }
    }

    private void updatePositionCacheTable() {
        Map<String, PoseType> map = pddlActionToCrclGenerator.getPoseCache();
        DefaultTableModel model = (DefaultTableModel) jTablePositionCache.getModel();
        model.setRowCount(0);
        for (Map.Entry<String, PoseType> entry : map.entrySet()) {
            PoseType pose = entry.getValue();
            if (null != pose) {
                PointType point = pose.getPoint();
                if (null != point) {
                    PmRpy rpy;
                    try {
                        rpy = CRCLPosemath.toPmRpy(pose);
                        model.addRow(new Object[]{entry.getKey(), point.getX(), point.getY(), point.getZ(), Math.toDegrees(rpy.r), Math.toDegrees(rpy.p), Math.toDegrees(rpy.y), ""});
                    } catch (PmException ex) {
                        model.addRow(new Object[]{entry.getKey(), point.getX(), point.getY(), point.getZ(), Double.NaN, Double.NaN, Double.NaN, ex.toString()});
                        Logger.getLogger(PddlExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        Utils.autoResizeTableColWidths(jTablePositionCache);
    }

    private boolean lastReplanAfterCrclBlock = false;

    private XFuture<Boolean> doPddlActionsSectionAsync(int startSafeAbortRequestCount, int sectionNumber) {
        try {
            CRCLProgramType program = pddlActionSectionToCrcl(sectionNumber);

            if (autoStart) {
                boolean replanAfterCrclBlock
                        = pddlActionToCrclGenerator.getLastIndex() < actionsList.size() - 1
                        && jCheckBoxReplan.isSelected();
                lastReplanAfterCrclBlock = replanAfterCrclBlock;
                if (replanAfterCrclBlock) {
                    return startCrclProgram(program)
                            .thenCompose("doPddlActionsSection.recursiveApplyGenerateCrcl(" + pddlActionToCrclGenerator.getLastIndex() + " out of " + actionsList.size() + ")",
                                    this::recursiveApplyGenerateCrcl);
                } else {
                    return startCrclProgram(program)
                            .thenApply("doPddlActionsSection.runProgramCompleteRunnables",
                                    x2 -> {
                                        runProgramCompleteRunnables(startSafeAbortRequestCount);
                                        return x2;
                                    });
                }
            } else {
                setCrclProgram(program);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            showExceptionInProgram(ex);
            XFuture<Boolean> ret = new XFuture<>("doPddlActionsSectionException");
            ret.completeExceptionally(ex);
            return ret;
        } finally {
            started = autoStart;
            replanStarted.set(false);
        }
        return XFuture.completedFuture(false);
    }

    private volatile long runProgramCompleteRunnablesTime = 0;

    public void runProgramCompleteRunnables(int startSafeAbortRequestCount) {
        checkSafeAbortAsync(() -> XFuture.completedFuture(false), startSafeAbortRequestCount);
        List<Runnable> runnables = new ArrayList<>();
        synchronized (this) {
            runProgramCompleteRunnablesTime = System.currentTimeMillis();
            this.runningProgram = false;
            runnables.addAll(programCompleteRunnablesList);
            programCompleteRunnablesList.clear();
        }
        for (Runnable r : runnables) {
            r.run();
        }
    }

    public XFuture<Boolean> placePartSlot(String part, String slot) throws IOException, IllegalStateException, SQLException, InterruptedException, ExecutionException, PendantClientInner.ConcurrentBlockProgramsException, CRCLException, PmException {
        pddlActionToCrclGenerator.partialReset();
        Map<String, String> options = getTableOptions();
        setReplanFromIndex(0);
        List<PddlAction> placePartActionsList = new ArrayList<>();
        PddlAction placePartAction = new PddlAction("", "place-part",
                new String[]{slot}, "cost");
        placePartActionsList.add(placePartAction);
        pddlActionToCrclGenerator.setPositionMaps(getPositionMaps());
        CRCLProgramType program = createEmptyProgram();
        pddlActionToCrclGenerator.setManualAction(true);
        List<MiddleCommandType> cmds = pddlActionToCrclGenerator.generate(placePartActionsList, 0, options, safeAbortRequestCount.get());
        pddlActionToCrclGenerator.setManualAction(false);
        jTextFieldIndex.setText(Integer.toString(getReplanFromIndex()));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        setEndCanonCmdId(program);
        XFuture<Boolean> ret = startCrclProgram(program);
        replanStarted.set(false);
        return ret;
    }

    public XFuture<Boolean> testPartPosition(String part) throws IOException, IllegalStateException, SQLException, InterruptedException, ExecutionException, PendantClientInner.ConcurrentBlockProgramsException, CRCLException, PmException {
        clearAll();
        Map<String, String> options = getTableOptions();
        setReplanFromIndex(0);
        List<PddlAction> testPartPositionActionList = new ArrayList<>();
        PddlAction takePartAction = new PddlAction("", "test-part-position",
                new String[]{part}, "cost");
        testPartPositionActionList.add(takePartAction);
        pddlActionToCrclGenerator.setPositionMaps(getPositionMaps());
        CRCLProgramType program = createEmptyProgram();
        List<MiddleCommandType> cmds = pddlActionToCrclGenerator.generate(testPartPositionActionList, 0, options, safeAbortRequestCount.get());
        jTextFieldIndex.setText(Integer.toString(getReplanFromIndex()));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        setEndCanonCmdId(program);

        for (PositionMap positionMap : getPositionMaps()) {
            if (null != positionMap) {
                PointType offset = positionMap.getLastOffset();
                if (null != offset) {
                    jTextFieldOffset.setText(String.format("%.1f,%.1f", offset.getX(), offset.getY()));
                }
                PointType testPoint = positionMap.getLastPointOut();
                if (null != testPoint) {
                    String testPoseString
                            = String.format("%.1f, %.1f, %.1f",
                                    testPoint.getX(),
                                    testPoint.getY(),
                                    testPoint.getZ());
                    jTextFieldAdjPose.setText(testPoseString);
                }
                PointType origPoint = positionMap.getLastPointIn();
                if (null != origPoint) {
                    String origPoseString
                            = String.format("%.1f, %.1f, %.1f",
                                    origPoint.getX(),
                                    origPoint.getY(),
                                    origPoint.getZ());
                    this.jTextFieldTestPose.setText(origPoseString);
                }
            }
        }

        replanStarted.set(false);
        return startCrclProgram(program);
    }

    public XFuture<Boolean> takePart(String part) throws IOException, IllegalStateException, SQLException, InterruptedException, ExecutionException, PendantClientInner.ConcurrentBlockProgramsException, CRCLException, PmException {
        pddlActionToCrclGenerator.partialReset();
        Map<String, String> options = getTableOptions();
        setReplanFromIndex(0);
        List<PddlAction> takePartActionsList = new ArrayList<>();
        PddlAction takePartAction = new PddlAction("", "take-part",
                new String[]{part}, "cost");
        takePartActionsList.add(takePartAction);
        pddlActionToCrclGenerator.setPositionMaps(getPositionMaps());
        CRCLProgramType program = createEmptyProgram();
        List<MiddleCommandType> cmds = pddlActionToCrclGenerator.generate(takePartActionsList, 0, options, safeAbortRequestCount.get());
        jTextFieldIndex.setText(Integer.toString(getReplanFromIndex()));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        setEndCanonCmdId(program);

        for (PositionMap positionMap : getPositionMaps()) {
            if (null != positionMap) {
                PointType offset = positionMap.getLastOffset();
                if (null != offset) {
                    jTextFieldOffset.setText(String.format("%.1f,%.1f", offset.getX(), offset.getY()));
                }
                PointType testPoint = positionMap.getLastPointOut();
                if (null != testPoint) {
                    String testPoseString
                            = String.format("%.1f, %.1f, %.1f",
                                    testPoint.getX(),
                                    testPoint.getY(),
                                    testPoint.getZ());
                    jTextFieldAdjPose.setText(testPoseString);
                }
                PointType origPoint = positionMap.getLastPointIn();
                if (null != origPoint) {
                    String origPoseString
                            = String.format("%.1f, %.1f, %.1f",
                                    origPoint.getX(),
                                    origPoint.getY(),
                                    origPoint.getZ());
                    this.jTextFieldTestPose.setText(origPoseString);
                }
            }
        }

        replanStarted.set(false);
        return startCrclProgram(program);
    }

    public XFuture<Boolean> returnPart(String part) throws IOException {
        clearAll();
        Map<String, String> options = getTableOptions();
        setReplanFromIndex(0);
        List<MiddleCommandType> cmds = new ArrayList<>();
        CRCLProgramType program = createEmptyProgram();
        pddlActionToCrclGenerator.setOptions(options);
        pddlActionToCrclGenerator.returnPart(part, cmds);

        jTextFieldIndex.setText(Integer.toString(getReplanFromIndex()));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        setEndCanonCmdId(program);
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
     * Modify the given pose by applying all of the currently added position
     * maps.
     *
     * @param poseIn the pose to correct or transform
     * @return pose after being corrected by all currently added position maps
     */
    public PoseType correctPose(PoseType poseIn) {
        PoseType pout = poseIn;
        for (PositionMap pm : getPositionMaps()) {
            if (null != pm) {
                pout = pm.correctPose(pout);
            }
        }
        return pout;
    }

    /**
     * Modify the given point by applying all of the currently added position
     * maps.
     *
     * @param ptIn the point to correct or transform
     * @return point after being corrected by all currently added position maps
     */
    public PointType correctPoint(PointType ptIn) {
        PointType pout = ptIn;
        for (PositionMap pm : getPositionMaps()) {
            if (null != pm) {
                pout = pm.correctPoint(ptIn);
            }
        }
        return pout;
    }

    /**
     * Apply inverses of currently added position maps in reverse order.
     *
     * @param ptIn point to reverse correction
     * @return point in original vision/database coordinates
     */
    public PointType reverseCorrectPoint(PointType ptIn) {
        PointType pout = ptIn;
        List<PositionMap> l = getReversePositionMaps();
        for (PositionMap pm : l) {
            if (null != pm) {
                pout = pm.correctPoint(ptIn);
            }
        }
        return pout;
    }

    public XFuture<Boolean> randomDropOff() {
        Map<String, String> options = getTableOptions();
        setReplanFromIndex(0);
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
        jTextFieldIndex.setText(Integer.toString(getReplanFromIndex()));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        setEndCanonCmdId(program);

        String randomPoseString
                = String.format("%.1f, %.1f, %.1f",
                        testDropOffPose.getPoint().getX(),
                        testDropOffPose.getPoint().getY(),
                        testDropOffPose.getPoint().getZ());
        String origPoseString
                = String.format("%.1f, %.1f, %.1f",
                        origPose.getPoint().getX(),
                        origPose.getPoint().getY(),
                        origPose.getPoint().getZ());
        System.out.println("randomPoseString = " + randomPoseString);
        jTextFieldOffset.setText(String.format("%.1f,%.1f", offset.getX(), offset.getY()));
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
            if (null != pm) {
                out = pm.getOffset(out.getX(), out.getY(), out.getZ());
            }
        }
        return out;
    }

    public void gridDropOff() throws IOException {
        if (gridTestCurrentY > gridTestMaxY + 0.001) {
            this.clearAll();
            return;
        }
        Map<String, String> options = getTableOptions();
        setReplanFromIndex(0);
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
        jTextFieldIndex.setText(Integer.toString(getReplanFromIndex()));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        setEndCanonCmdId(program);
        setCrclProgram(program);
        String gridPoseString
                = String.format("%.1f, %.1f, %.1f",
                        testDropOffPose.getPoint().getX(),
                        testDropOffPose.getPoint().getY(),
                        testDropOffPose.getPoint().getZ());
        String origPoseString
                = String.format("%.1f, %.1f, %.1f",
                        origPose.getPoint().getX(),
                        origPose.getPoint().getY(),
                        origPose.getPoint().getZ());
        System.out.println("gridPoseString = " + gridPoseString);
        jTextFieldOffset.setText(String.format("%.1f,%.1f,%.1f", offset.getX(), offset.getY(), offset.getZ()));
        jTextFieldAdjPose.setText(gridPoseString);
        this.jTextFieldTestPose.setText(origPoseString);
        replanStarted.set(false);
    }

    public XFuture<Boolean> randomPickup() throws CRCLException, PmException {
        Map<String, String> options = getTableOptions();
        setReplanFromIndex(0);
        List<MiddleCommandType> cmds = new ArrayList<>();
        pddlActionToCrclGenerator.setOptions(options);
        pddlActionToCrclGenerator.takePartByPose(cmds, testDropOffPose);
        CRCLProgramType program = createEmptyProgram();
        jTextFieldIndex.setText(Integer.toString(getReplanFromIndex()));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        setEndCanonCmdId(program);
        replanStarted.set(false);
        return startCrclProgram(program);
    }

    private XFuture<Boolean> lookForParts() {
        try {
            CRCLProgramType program = createLookForPartsProgram();
            return startCrclProgram(program);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            XFuture<Boolean> future = new XFuture<>("lookForPartsException");
            future.completeExceptionally(ex);
            return future;
        }
    }

    public CRCLProgramType createLookForPartsProgram() {
        try {
            return createLookForPartsProgramInternal();
        } catch (CRCLException | PmException | ExecutionException | IOException | InterruptedException | PendantClientInner.ConcurrentBlockProgramsException | SQLException | IllegalStateException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    public CRCLProgramType createLookForPartsProgramInternal() throws ExecutionException, IOException, InterruptedException, PendantClientInner.ConcurrentBlockProgramsException, SQLException, IllegalStateException, CRCLException, PmException {
        Map<String, String> options = getTableOptions();
        setReplanFromIndex(0);
        List<PddlAction> lookForActionsList = new ArrayList<>();
        PddlAction lookForAction = new PddlAction("", "look-for-part",
                new String[]{}, "cost");
        lookForActionsList.add(lookForAction);
        pddlActionToCrclGenerator.clearPoseCache();
        pddlActionToCrclGenerator.clearLastRequiredPartsMap();
        CRCLProgramType program = createEmptyProgram();
        List<MiddleCommandType> cmds = pddlActionToCrclGenerator.generate(lookForActionsList, 0, options, safeAbortRequestCount.get());
        jTextFieldIndex.setText(Integer.toString(getReplanFromIndex()));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        setEndCanonCmdId(program);
        replanStarted.set(false);
        return program;
    }

    public XFuture<Boolean> gotoToolChangerApproach(String poseName, PoseType pose) {
        try {
            Map<String, String> options = getTableOptions();
            setReplanFromIndex(0);
            List<PddlAction> gototToolChangerApproachActionsList = new ArrayList<>();
            PddlAction gototToolChangerApproachAction
                    = new PddlAction("",
                            "goto-tool-changer-approach",
                            new String[]{poseName}, "cost");
            gototToolChangerApproachActionsList.add(gototToolChangerApproachAction);
            pddlActionToCrclGenerator.clearPoseCache();
            pddlActionToCrclGenerator.clearLastRequiredPartsMap();
            pddlActionToCrclGenerator.putPoseCache(poseName, pose);
            pddlActionToCrclGenerator.setApproachToolChangerZOffset(Double.parseDouble(jTextFieldToolChangerApproachZOffset.getText()));
            List<MiddleCommandType> cmds = pddlActionToCrclGenerator.generate(gototToolChangerApproachActionsList, 0, options, safeAbortRequestCount.get());
            CRCLProgramType program = createEmptyProgram();
            jTextFieldIndex.setText(Integer.toString(getReplanFromIndex()));
            program.getMiddleCommand().clear();
            program.getMiddleCommand().addAll(cmds);
            setEndCanonCmdId(program);
            replanStarted.set(false);
            return startCrclProgram(program);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            XFuture<Boolean> future = new XFuture<>("gototToolChangerApproachPartsException");
            future.completeExceptionally(ex);
            return future;
        }
    }

    public XFuture<Boolean> gotoToolChangerPose(String poseName, PoseType pose) {
        try {
            Map<String, String> options = getTableOptions();
            setReplanFromIndex(0);
            List<PddlAction> gototToolChangerApproachActionsList = new ArrayList<>();
            PddlAction gototToolChangerApproachAction
                    = new PddlAction("",
                            "goto-tool-changer-pose",
                            new String[]{poseName}, "cost");
            gototToolChangerApproachActionsList.add(gototToolChangerApproachAction);
            pddlActionToCrclGenerator.clearPoseCache();
            pddlActionToCrclGenerator.clearLastRequiredPartsMap();
            pddlActionToCrclGenerator.putPoseCache(poseName, pose);
            List<MiddleCommandType> cmds = pddlActionToCrclGenerator.generate(gototToolChangerApproachActionsList, 0, options, safeAbortRequestCount.get());
            CRCLProgramType program = createEmptyProgram();
            jTextFieldIndex.setText(Integer.toString(getReplanFromIndex()));
            program.getMiddleCommand().clear();
            program.getMiddleCommand().addAll(cmds);
            setEndCanonCmdId(program);
            replanStarted.set(false);
            return startCrclProgram(program);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            XFuture<Boolean> future = new XFuture<>("gototToolChangerApproachPartsException");
            future.completeExceptionally(ex);
            return future;
        }
    }

    public XFuture<Boolean> dropTool(String poseName, PoseType pose) {
        try {
            Map<String, String> options = getTableOptions();
            setReplanFromIndex(0);
            List<PddlAction> gototToolChangerApproachActionsList = new ArrayList<>();
            PddlAction gototToolChangerApproachAction
                    = new PddlAction("",
                            "drop-tool",
                            new String[]{poseName}, "cost");
            gototToolChangerApproachActionsList.add(gototToolChangerApproachAction);
            pddlActionToCrclGenerator.clearPoseCache();
            pddlActionToCrclGenerator.clearLastRequiredPartsMap();
            pddlActionToCrclGenerator.putPoseCache(poseName, pose);
            pddlActionToCrclGenerator.setApproachToolChangerZOffset(Double.parseDouble(jTextFieldToolChangerApproachZOffset.getText()));
            List<MiddleCommandType> cmds = pddlActionToCrclGenerator.generate(gototToolChangerApproachActionsList, 0, options, safeAbortRequestCount.get());
            CRCLProgramType program = createEmptyProgram();
            jTextFieldIndex.setText(Integer.toString(getReplanFromIndex()));
            program.getMiddleCommand().clear();
            program.getMiddleCommand().addAll(cmds);
            setEndCanonCmdId(program);
            replanStarted.set(false);
            return startCrclProgram(program);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            XFuture<Boolean> future = new XFuture<>("gototToolChangerApproachPartsException");
            future.completeExceptionally(ex);
            return future;
        }
    }

    public XFuture<Boolean> pickupTool(String poseName, PoseType pose, String newToolName) {
        try {
            Map<String, String> options = getTableOptions();
            setReplanFromIndex(0);
            List<PddlAction> gototToolChangerApproachActionsList = new ArrayList<>();
            PddlAction gototToolChangerApproachAction
                    = new PddlAction("",
                            "pickup-tool",
                            new String[]{poseName, newToolName}, "cost");
            gototToolChangerApproachActionsList.add(gototToolChangerApproachAction);
            pddlActionToCrclGenerator.clearPoseCache();
            pddlActionToCrclGenerator.clearLastRequiredPartsMap();
            pddlActionToCrclGenerator.putPoseCache(poseName, pose);
            pddlActionToCrclGenerator.setApproachToolChangerZOffset(Double.parseDouble(jTextFieldToolChangerApproachZOffset.getText()));
            List<MiddleCommandType> cmds = pddlActionToCrclGenerator.generate(gototToolChangerApproachActionsList, 0, options, safeAbortRequestCount.get());
            CRCLProgramType program = createEmptyProgram();
            jTextFieldIndex.setText(Integer.toString(getReplanFromIndex()));
            program.getMiddleCommand().clear();
            program.getMiddleCommand().addAll(cmds);
            setEndCanonCmdId(program);
            replanStarted.set(false);
            return startCrclProgram(program);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            XFuture<Boolean> future = new XFuture<>("gototToolChangerApproachPartsException");
            future.completeExceptionally(ex);
            return future;
        }
    }

    private volatile List<Future<?>> checkDbSupplierPublisherFuturesList = Collections.emptyList();

    private void checkDbSupplierPublisher() throws IOException {
        assert (null != pddlActionToCrclGenerator) : "null == pddlActionToCrclGenerator";
        assert (null != dbSetupSupplier) : "null == dbSetupSupplier";
        if (pddlActionToCrclGenerator.isConnected()) {
            return;
        }
        if (null != dbSetupSupplier) {
            try {
                dbSetupPublisher = dbSetupSupplier.call();
                dbSetupPublisher.addDbSetupListener(dbSetupListener);

            } catch (Exception ex) {
                Logger.getLogger(VisionToDBJPanel.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        dbSetupPublisher.setDbSetup(new DbSetupBuilder().setup(dbSetupPublisher.getDbSetup()).connected(true).build());
        checkDbSupplierPublisherFuturesList = dbSetupPublisher.notifyAllDbSetupListeners();
        if (!pddlActionToCrclGenerator.isConnected()) {
            throw new IllegalStateException("Failed to connnect to database");
        }
    }

    @Nullable
    private String getConnnectionURL() throws SQLException {
        Connection con = pddlActionToCrclGenerator.getDbConnection();
        if (null == con) {
            throw new IllegalStateException("connection is null");
        }
        return con.getMetaData().getURL();
    }

    private XFuture<Void> checkDbSupplierPublisherAsync() throws IOException {
        if (null == this.pddlActionToCrclGenerator) {
            XFuture<Void> ret = new XFuture<Void>("checkDbSupplierPublisher(null==pddlActionToCrclGenerator)");
            ret.completeExceptionally(new IllegalStateException("checkDbSupplierPublisher(null==pddlActionToCrclGenerator)"));
            return ret;
        }
        if (pddlActionToCrclGenerator.isConnected()) {
            try {
                return XFutureVoid.completedFutureWithName("checkDbSupplierPublisher.alreadyConnected." + getConnnectionURL());
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
                XFuture<Void> ret = new XFuture<Void>("checkDbSupplierPublisher.alreadyConnected.withException");
                ret.completeExceptionally(ex);
                return ret;
            }
        }
        XFuture<Void> f1 = new XFuture<>("checkDbSupplierPublisher.f1");
        newDbSetupFutures.add(f1);
        if (null != dbSetupSupplier) {
            try {
                dbSetupPublisher = dbSetupSupplier.call();
                dbSetupPublisher.addDbSetupListener(dbSetupListener);

            } catch (Exception ex) {
                Logger.getLogger(VisionToDBJPanel.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (null != dbSetupPublisher) {
            dbSetupPublisher.setDbSetup(new DbSetupBuilder().setup(dbSetupPublisher.getDbSetup()).connected(true).build());
            checkDbSupplierPublisherFuturesList = dbSetupPublisher.notifyAllDbSetupListeners();
        } else {
            System.err.println("dbSetupPublisher == null");
            f1.completeExceptionally(new IllegalStateException("dbSetupPublisher == null"));
        }
        return f1; //XFuture.allOfWithName("checkDbSupplierPublisher.all", f1,f2);
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
    private javax.swing.JButton jButtonAddToolHolderPose;
    private javax.swing.JButton jButtonAddToolOffset;
    private javax.swing.JButton jButtonAddTrayAttach;
    private javax.swing.JButton jButtonClear;
    private javax.swing.JButton jButtonClearPoseCache;
    private javax.swing.JButton jButtonContRandomTest;
    private javax.swing.JButton jButtonContinue;
    private javax.swing.JButton jButtonDeleteToolHolderPose;
    private javax.swing.JButton jButtonDeleteToolOffset;
    private javax.swing.JButton jButtonDeleteTrayAttach;
    private javax.swing.JButton jButtonDropTool;
    private javax.swing.JButton jButtonGenerateAndRun;
    private javax.swing.JButton jButtonGenerateCRCL;
    private javax.swing.JButton jButtonGotoToolChangerApproach;
    private javax.swing.JButton jButtonGotoToolChangerPose;
    private javax.swing.JButton jButtonGridTest;
    private javax.swing.JButton jButtonLoad;
    private javax.swing.JButton jButtonLoadPddlActionsFromFile;
    private javax.swing.JButton jButtonLookFor;
    private javax.swing.JButton jButtonNewLogFile;
    private javax.swing.JButton jButtonPause;
    private javax.swing.JButton jButtonPddlOutputViewEdit;
    private javax.swing.JButton jButtonPickupTool;
    private javax.swing.JButton jButtonPlacePart;
    private javax.swing.JButton jButtonRandDropOff;
    private javax.swing.JButton jButtonRecord;
    private javax.swing.JButton jButtonRecordFail;
    private javax.swing.JButton jButtonRecordLookForJoints;
    private javax.swing.JButton jButtonRecordSuccess;
    private javax.swing.JButton jButtonRecordToolHolderApproach;
    private javax.swing.JButton jButtonRecordToolHolderPose;
    private javax.swing.JButton jButtonReset;
    private javax.swing.JButton jButtonReturn;
    private javax.swing.JButton jButtonSafeAbort;
    private javax.swing.JButton jButtonSetCurrentTool;
    private javax.swing.JButton jButtonStep;
    private javax.swing.JButton jButtonStopRandomTest;
    private javax.swing.JButton jButtonTake;
    private javax.swing.JButton jButtonTest;
    private javax.swing.JButton jButtonTestPickup;
    private javax.swing.JButton jButtonUpdatePoseCache;
    private javax.swing.JCheckBox jCheckBoxDebug;
    private javax.swing.JCheckBox jCheckBoxEnableExternalControlPort;
    private javax.swing.JCheckBox jCheckBoxEnableOptaPlanner;
    private javax.swing.JCheckBox jCheckBoxForceFakeTake;
    private javax.swing.JCheckBox jCheckBoxNeedLookFor;
    private javax.swing.JCheckBox jCheckBoxReplan;
    private javax.swing.JComboBox<String> jComboBoxManualObjectName;
    private javax.swing.JComboBox<String> jComboBoxManualSlotName;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
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
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanelContainerPoseCache;
    private javax.swing.JPanel jPanelContainerPositionMap;
    private javax.swing.JPanel jPanelCrcl;
    private javax.swing.JPanel jPanelExternalControl;
    private javax.swing.JPanel jPanelInnerManualControl;
    private javax.swing.JPanel jPanelOpOuter;
    private javax.swing.JPanel jPanelOuterManualControl;
    private javax.swing.JPanel jPanelToolChange;
    private javax.swing.JPanel jPanelToolHolderPositions;
    private javax.swing.JPanel jPanelToolOffsets;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPaneExternalControl;
    private javax.swing.JScrollPane jScrollPaneHolderContents;
    private javax.swing.JScrollPane jScrollPaneOptions;
    private javax.swing.JScrollPane jScrollPanePositionTable;
    private javax.swing.JScrollPane jScrollPaneToolHolderPositions;
    private javax.swing.JScrollPane jScrollPaneToolOffsets;
    private javax.swing.JScrollPane jScrollPaneToolOffsets1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTabbedPane jTabbedPaneToolChangeInner;
    private javax.swing.JTable jTableCrclProgram;
    private javax.swing.JTable jTableHolderContents;
    private javax.swing.JTable jTableOptions;
    private javax.swing.JTable jTablePddlOutput;
    private javax.swing.JTable jTablePositionCache;
    private javax.swing.JTable jTableToolHolderPositions;
    private javax.swing.JTable jTableToolOffsets;
    private javax.swing.JTable jTableTrayAttachOffsets;
    private javax.swing.JTextArea jTextAreaExternalCommads;
    private javax.swing.JTextField jTextFieldAdjPose;
    private javax.swing.JTextField jTextFieldCurrentPart;
    private javax.swing.JTextField jTextFieldCurrentToolName;
    private javax.swing.JTextField jTextFieldCurrentToolOffset;
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
    private javax.swing.JTextField jTextFieldToolChangerApproachZOffset;
    private aprs.framework.optaplanner.OpDisplayJPanel opDisplayJPanelInput;
    private aprs.framework.optaplanner.OpDisplayJPanel opDisplayJPanelSolution;
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
            loadComboModels(props);
            this.actionsFileString = props.getProperty(PDDLOUTPUT);
            this.reverseActionsFileString = props.getProperty(REVERSE_PDDLOUTPUT);
            reloadActionsFile();
            String autostartString = props.getProperty(PDDLCRCLAUTOSTART);
            if (null != autostartString) {
                this.autoStart = Boolean.valueOf(autostartString);
            }
            String enableOptaPlannerString = props.getProperty(ENABLE_OPTA_PLANNER);
            if (null != enableOptaPlannerString) {
                boolean enableOptaPlanner = Boolean.valueOf(enableOptaPlannerString);
                jCheckBoxEnableOptaPlanner.setSelected(enableOptaPlanner);
            }
            for (String name : props.stringPropertyNames()) {
                if (!name.equals(PDDLCRCLAUTOSTART)
                        && !name.equals(PDDLOUTPUT)
                        && !name.equals(MANUAL_PART_NAMES)
                        && !name.equals(MANUAL_SLOT_NAMES)) {
                    DefaultTableModel model = (DefaultTableModel) jTableOptions.getModel();
                    boolean foundit = false;
                    for (int i = 0; i < model.getRowCount(); i++) {
                        Object tableValue = model.getValueAt(i, 0);
                        if (tableValue == null) {
                            continue;
                        }
                        String nameFromTable = tableValue.toString();
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

            String errorMapFiles = props.getProperty(POS_ERROR_MAP_FILES, "");
            if (null != errorMapFiles && errorMapFiles.length() > 0) {
                loadErrorMapFiles(errorMapFiles);
            }
            loadHolderContentsMap();
            loadToolChangerPoseMap();
            loadToolOffsetMap();
            loadTrayAttachOffsetMap();
            String filename = getSelectedToolNameFileName();
            if (null != filename && filename.length() > 1) {
                String selectedTool = readSelectedToolNameFile(filename);
                if (null != selectedTool) {
                    setSelectedToolName(selectedTool);
                }
            }
        }
    }

    private void loadErrorMapFiles(String errorMapFiles) throws IOException {
        positionMapJPanel1.clearCurrentMap();
        String errorMapFilesArray[] = errorMapFiles.split("[\t,\\[\\]\\{\\}" + File.pathSeparator + "]+");
        for (String emf : errorMapFilesArray) {
            if (null == emf) {
                continue;
            }
            String fname = emf.trim();
            if (fname.length() < 1 || "null".equals(fname)) {
                continue;
            }
            File f = new File(fname);
            if (f.exists()) {
                try {
                    positionMapJPanel1.addPositionMapFile(f);
                } catch (PositionMap.BadErrorMapFormatException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            } else {
                File parentFile = propertiesFile.getParentFile();
                if (null == parentFile) {
                    String errString = "Can't load errorMapFile : " + fname + ", parentFile is null";
                    setErrorString(errString);
                    System.err.println(errString);
                    return;
                }
                String fullPath = parentFile.toPath().resolve(fname).normalize().toString();
                f = new File(fullPath);
                if (f.exists()) {
                    try {
                        positionMapJPanel1.addPositionMapFile(f);
                    } catch (PositionMap.BadErrorMapFormatException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                    }
                } else {
                    String errString = "Can't load errorMapFile : " + fname + "   or " + fullPath;
                    setErrorString(errString);
                    System.err.println(errString);
                }
            }
        }
    }

    private static final String ENABLE_OPTA_PLANNER = "enableOptaPlanner";

    private void loadComboModels(Properties props) {
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

    public void reloadActionsFile() throws IOException {
        String output = reverseFlag ? reverseActionsFileString : actionsFileString;
        if (null != output) {
            File f = new File(output);
            if (f.exists() && f.canRead() && !f.isDirectory()) {
                loadActionsFile(f, true);
                jTextFieldPddlOutputActions.setText(f.getCanonicalPath());
            } else {
                File parentFile = propertiesFile.getParentFile();
                if (null == parentFile) {
                    return;
                }
                String fullPath = parentFile.toPath().resolve(output).normalize().toString();
                f = new File(fullPath);
                if (f.exists() && f.canRead() && !f.isDirectory()) {
                    loadActionsFile(f, true);
                    jTextFieldPddlOutputActions.setText(f.getCanonicalPath());
                } else {
                    String fullPath2 = parentFile.toPath().resolveSibling(output).normalize().toString();
                    f = new File(fullPath2);
                    if (f.exists() && f.canRead() && !f.isDirectory()) {
                        jTextFieldPddlOutputActions.setText(f.getCanonicalPath());
                        loadActionsFile(f, true);
                    }
                }
            }
        }
    }

    @Override
    public void close() throws Exception {
    }

    final ConcurrentLinkedDeque<XFuture<Void>> newDbSetupFutures = new ConcurrentLinkedDeque<>();

    private void handleNewDbSetup(DbSetup setup) {
        if (null != pddlActionToCrclGenerator) {
            pddlActionToCrclGenerator.setDbSetup(setup)
                    .thenRun(() -> {
                        XFuture<Void> f = newDbSetupFutures.poll();
                        while (f != null) {
                            f.complete(null);
                            f = newDbSetupFutures.poll();
                        }
                    });
        }
    }

    private boolean needReplan = false;
    private final AtomicInteger replanFromIndex = new AtomicInteger(-1);
    private final AtomicBoolean replanStarted = new AtomicBoolean();

    javax.swing.@Nullable Timer replanActionTimer = null;
    private final Runnable defaultReplanRunnable = new Runnable() {
        @Override
        public void run() {
//            Utils.runOnDispatchThread(PddlExecutorJPanel.this::generateCrclWithCatch);
            replanActionTimer
                    = new javax.swing.Timer(200, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            generateCrclAsyncWithCatch();
                        }
                    });
            replanActionTimer.setRepeats(false);
            replanActionTimer.start();
        }
    };

    final private List<RunnableWithThrow> customRunnables = new ArrayList<>();
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
                LOGGER.log(Level.SEVERE, null, ex);
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

    private volatile boolean runningProgram = false;

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

    @Nullable
    PddlAction actionFromIndex(int index) {
        if (null != actionsList && actionsList.size() >= index && index >= 0) {
            return actionsList.get(index);
        }
        return null;
    }

    @Override
    public void accept(PendantClientJPanel panel, int line, CRCLProgramType program, CRCLStatusType status) {

//        currentActionIndex = actionIndexFromCrclLine(line);
//        if (null != aprsJFrame) {
//            aprsJFrame.updateTitle();
//        }
        if (this.debug && null != program) {
            int sz = program.getMiddleCommand().size();
            System.out.println("replanStarted = " + replanStarted);
            System.out.println("replanRunnable = " + replanRunnable);
            System.out.println("jCheckBoxReplan.isSelected() = " + jCheckBoxReplan.isSelected());
            System.out.println("sz = " + sz);
            System.out.println("line = " + line);
            CommandStateEnumType state = status.getCommandStatus().getCommandState();

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
