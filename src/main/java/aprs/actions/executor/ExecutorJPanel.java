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
package aprs.actions.executor;

import static aprs.actions.executor.ActionType.CHECK_KITS;
import static aprs.actions.executor.ActionType.DROP_TOOL_ANY;

import aprs.cachedcomponents.CachedCheckBox;
import aprs.cachedcomponents.CachedComboBox;
import aprs.cachedcomponents.CachedTable;
import aprs.cachedcomponents.CachedTextField;
import aprs.misc.Utils;
import aprs.misc.Utils.RunnableWithThrow;

import static aprs.misc.Utils.autoResizeTableColWidths;
import static aprs.misc.Utils.autoResizeTableRowHeights;

import aprs.database.DbSetup;
import aprs.database.DbSetupBuilder;
import aprs.database.DbSetupListener;
import aprs.database.DbSetupPublisher;
import aprs.database.PhysicalItem;
import aprs.database.ToolHolder;

import static aprs.actions.executor.ActionType.GOTO_TOOL_CHANGER_APPROACH;
import static aprs.actions.executor.ActionType.GOTO_TOOL_CHANGER_POSE;
import static aprs.actions.executor.ActionType.LOOK_FOR_PARTS;
import static aprs.actions.executor.ActionType.PLACE_PART;
import static aprs.actions.executor.ActionType.TAKE_PART;
import static aprs.actions.executor.ActionType.TEST_PART_POSITION;

import aprs.actions.optaplanner.display.OpDisplayJPanel;
import aprs.actions.optaplanner.actionmodel.OpAction;
import aprs.actions.optaplanner.actionmodel.OpActionPlan;
import aprs.actions.optaplanner.actionmodel.score.EasyOpActionPlanScoreCalculator;
import aprs.database.vision.VisionToDBJPanel;
import aprs.system.AprsSystem;
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

import java.awt.*;
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
import javax.xml.bind.JAXBException;
import java.awt.event.ActionEvent;
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
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import javax.swing.JOptionPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.guieffect.qual.UIType;
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

import java.util.Map.Entry;
import javax.swing.JMenu;

import static aprs.actions.executor.ActionType.DROP_TOOL_BY_HOLDER;
import static aprs.actions.executor.ActionType.PICKUP_TOOL_BY_HOLDER;
import static aprs.actions.executor.ActionType.PICKUP_TOOL_BY_TOOL;
import static aprs.actions.executor.ActionType.SWITCH_TOOL;

import aprs.database.Tool;

import javax.swing.JRadioButtonMenuItem;

import static aprs.misc.Utils.readCsvFileToTable;
import static aprs.misc.Utils.readCsvFileToTableAndMap;

import crcl.base.JointStatusesType;

import java.util.HashSet;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings({"CanBeFinal", "UnusedReturnValue", "MagicConstant", "unused"})
public class ExecutorJPanel extends javax.swing.JPanel implements ExecutorDisplayInterface, PendantClientJPanel.ProgramLineListener {

    private final JMenu toolMenu;
    private final JMenu toolDropByHolderMenu;
    private final JMenu toolPickupByHolderMenu;
    private final JMenuItem toolDropCurrentToolMenuItem;
    private final JMenu toolPickupByToolMenu;
    private final JMenu toolSetToolMenu;
    private final JMenu toolSwitchToolMenu;

    /**
     * Create an empty ExecutorJPanel. This should only be used by Netbeans GUI
     * builder. All other users should use constructor with AprsSystem argument.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @SuppressWarnings({"initialization", "nullness"})
    @UIEffect
    public ExecutorJPanel() throws InterruptedException, ExecutionException {
        this(null);
    }

    @UIType
    @SuppressWarnings({"guieffect", "nullness"})
    private class PddlOutputTableCellRendererer extends DefaultTableCellRenderer {

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
    }

    @UIType
    @SuppressWarnings("guieffect")
    private class PddlOutputTableMouseListener implements MouseListener {

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
    }

    /**
     * Creates new form ActionsToCrclJPanel
     *
     * @param aprsSystem1 AprsSystem that this will be a part of.
     */
    @SuppressWarnings("initialization")
    @UIEffect
    public ExecutorJPanel(AprsSystem aprsSystem1) {
        this.aprsSystem = aprsSystem1;
        initComponents();
        jCheckBoxDebug.setSelected(debug);
        progColor = jTableCrclProgram.getBackground();
        this.crclGenerator = new CrclGenerator(this);
        crclGenerator.addPlacePartConsumer(this::handlePlacePartCompleted);
        crclGenerator.addActionCompletedListener(this::handleActionCompleted);
        jTablePddlOutput.setDefaultRenderer(Object.class, new PddlOutputTableCellRendererer());
        jTablePddlOutput.addMouseListener(new PddlOutputTableMouseListener());
        setToolOffsetTableModelListener();
        setTrayAttachOffsetTableModelListener();
        toolMenu = new JMenu("Tools");
        toolDropByHolderMenu = new JMenu("Drop By Holder");
        toolPickupByHolderMenu = new JMenu("Pickup By Holder");
        toolDropCurrentToolMenuItem = new JMenuItem("Drop Current Tool");
        toolDropCurrentToolMenuItem.setEnabled(false);
        toolDropCurrentToolMenuItem.addActionListener(e -> dropToolAny());
        toolPickupByToolMenu = new JMenu("Pickup by Tool");
        toolSwitchToolMenu = new JMenu("Switch (Drop and Pickup) Tool");
        toolSetToolMenu = new JMenu("Set Tool");

        toolMenu.add(toolDropByHolderMenu);
        toolMenu.add(toolPickupByHolderMenu);
        toolMenu.add(toolDropCurrentToolMenuItem);
        toolMenu.add(toolPickupByHolderMenu);
        toolMenu.add(toolPickupByToolMenu);
        toolMenu.add(toolSwitchToolMenu);
        toolMenu.add(toolSetToolMenu);
        optionsCachedTable = new CachedTable((DefaultTableModel) jTableOptions.getModel(), jTableOptions);
        enableOptaplannerCachedCheckBox = new CachedCheckBox(jCheckBoxEnableOptaPlanner);
        pddlOutputActionsCachedText = new CachedTextField(jTextFieldPddlOutputActions);
        pddlOutputCachedTableModel = new CachedTable((DefaultTableModel) jTablePddlOutput.getModel(), jTablePddlOutput);
        replanCachedCheckBox = new CachedCheckBox(jCheckBoxReplan);
        forceFakeTakeFlagCachedCheckBox = new CachedCheckBox(jCheckBoxForceFakeTake);
        crclProgramCachedTable = new CachedTable((DefaultTableModel) jTableCrclProgram.getModel(), jTableCrclProgram);
        manualObjectCachedComboBox = new CachedComboBox<>(String.class, jComboBoxManualObjectName);
        manualSlotCachedComboBox = new CachedComboBox<>(String.class, jComboBoxManualSlotName);
        toolHolderPositionsCachedTable = new CachedTable(jTableToolHolderPositions);
        holderContentsCachedTable = new CachedTable(jTableHolderContents);
        toolOffsetsCachedTable = new CachedTable(jTableToolOffsets);
        trayAttachOffsetsCachedTable = new CachedTable(jTableTrayAttachOffsets);
        positionCacheCachedTable = new CachedTable(jTablePositionCache);
        indexCachedTextField = new CachedTextField(jTextFieldIndex);

    }

    public JMenu getToolMenu() {
        return toolMenu;
    }

    public String getSelectedToolName() {
        return crclGenerator.getCurrentToolName();
    }

    @SuppressWarnings({"nullness", "keyfor"})
    public Set<String> getPossibleToolNames() {
        return crclGenerator.getToolOffsetMap().keySet();
    }

    @Nullable
    private String selectedToolNameFileName = null;

    /**
     * Get the value of selectedToolNameFileName
     *
     * @return the value of selectedToolNameFileName
     */
    @Nullable
    private String getSelectedToolNameFileName() {
        if (null == selectedToolNameFileName) {
            try {
                return getDefaultSelectedToolNameFile();
            } catch (Exception ex) {
                Logger.getLogger(ExecutorJPanel.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
        return selectedToolNameFileName;
    }

    /**
     * Set the value of selectedToolNameFileName
     *
     * @param selectedToolNameFileName new value of selectedToolNameFileName
     */
    private void setSelectedToolNameFileName(String selectedToolNameFileName) {
        this.selectedToolNameFileName = selectedToolNameFileName;
    }

    private String getDefaultSelectedToolNameFile() {
        return propertiesFile.getName() + ".selectedToolName.txt";
    }

    @Nullable
    private String readSelectedToolNameFile() throws IOException {
        String filename = getSelectedToolNameFileName();
        if (null == filename || filename.length() < 1) {
            return null;
        }
        return readSelectedToolNameFile(filename);
    }

    @Nullable
    private String readSelectedToolNameFile(String filename) throws IOException {
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

    private final ConcurrentLinkedQueue<Consumer<String>> selectedToolNameListeners = new ConcurrentLinkedQueue<>();

    public void addSelectedToolNameListener(Consumer<String> listener) {
        selectedToolNameListeners.add(listener);
    }

    public void removeSelectedToolNameListener(Consumer<String> listener) {
        selectedToolNameListeners.add(listener);
    }

    public void setSelectedToolName(String newToolName) {
        try {
            if (null == newToolName) {
                return;
            }
            String currentToolName = crclGenerator.getCurrentToolName();
            if (!Objects.equals(currentToolName, newToolName)) {
                crclGenerator.setCurrentToolName(newToolName);
            }
            PoseType newPose = crclGenerator.getToolOffsetPose();
            String offsetText;
            if (null != newPose) {
                PmRpy rpy = CRCLPosemath.toPmRpy(newPose);
                PointType newPosePoint = requireNonNull(newPose.getPoint(), "newPose.getPoint()");
                PmCartesian tran = CRCLPosemath.toPmCartesian(newPosePoint);

                offsetText = String.format("X=%.3f,Y=%.3f,Z=%.3f,roll=%.3f,pitch=%.3f,yaw=%.3f",
                        tran.x, tran.y, tran.z,
                        Math.toDegrees(rpy.r), Math.toDegrees(rpy.p), Math.toDegrees(rpy.y));

            } else {
                offsetText = "X=0,Y=0,Z=0,roll=0,pitch=0,yaw=0";
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
                    Logger.getLogger(ExecutorJPanel.class.getName()).log(Level.SEVERE, "", ex);
                }
            }
            Utils.runOnDispatchThread(() -> setCurrentToolNamOnDisplay(newToolName, offsetText));
            for (Consumer<String> listener : selectedToolNameListeners) {
                listener.accept(newToolName);
            }
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "", exception);
        }
    }

    @UIEffect
    private void setCurrentToolNamOnDisplay(String newToolName, String offsetText) {
        jTextFieldCurrentToolOffset.setText(offsetText);
        jTextFieldCurrentToolName.setText(newToolName);
        loadToolMenus();
    }

    private void setOptionsTableEntry(String key, String value) {
        for (int i = 0; i < optionsCachedTable.getRowCount(); i++) {
            Object keyFromTable = optionsCachedTable.getValueAt(i, 0);
            if (Objects.equals(keyFromTable, key)) {
                optionsCachedTable.setValueAt(value, i, 1);
                return;
            }
        }
        optionsCachedTable.addRow(new Object[]{key, value});
        crclGenerator.setOptions(getTableOptions());
    }

    public XFutureVoid setLookForXYZ(double x, double y, double z) {
        try {
            String valueString = String.format("%.3f,%.3f,%.3f", x, y, z);
            return Utils.runOnDispatchThread("setLookForXYZ",
                    () -> setOptionsTableEntry("lookForXYZ", valueString));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }

    private volatile boolean toolOffsetTablemModelListenerEnabled = false;

    private void setToolOffsetTableModelListener() {
        toolOffsetTablemModelListenerEnabled = true;
        Utils.runOnDispatchThread(this::setToolOffsetTableModelListenerOnDisplay);
    }

    @UIEffect
    private void setToolOffsetTableModelListenerOnDisplay() {
        jTableToolOffsets.getModel().addTableModelListener(toolOffsetsModelListener);
    }

    private void clearToolOffsetTableModelListener() {
        toolOffsetTablemModelListenerEnabled = false;
        Utils.runOnDispatchThread(this::clearToolOffsetTableModelListenerOnDisplay);
    }

    @UIEffect
    private void clearToolOffsetTableModelListenerOnDisplay() {
        jTableToolOffsets.getModel().removeTableModelListener(toolOffsetsModelListener);
    }

    private volatile boolean trayAttachOffsetTablemModelListenerEnabled = false;

    private void setTrayAttachOffsetTableModelListener() {
        trayAttachOffsetTablemModelListenerEnabled = true;
        Utils.runOnDispatchThread(this::setTrayAttachOffsetTableModelListenerOnDisplay);
    }

    private void clearTrayAttachOffsetTableModelListener() {
        trayAttachOffsetTablemModelListenerEnabled = false;
        Utils.runOnDispatchThread(this::clearTrayAttachOffsetTableModelListenerOnDisplay);
    }

    @UIEffect
    private void setTrayAttachOffsetTableModelListenerOnDisplay() {
        jTableTrayAttachOffsets.getModel().addTableModelListener(trayAttachOffsetsModelListener);
    }

    @UIEffect
    private void clearTrayAttachOffsetTableModelListenerOnDisplay() {
        jTableTrayAttachOffsets.getModel().removeTableModelListener(trayAttachOffsetsModelListener);
    }

    private final AtomicInteger setReverseTrueCount = new AtomicInteger();
    private final AtomicInteger setReverseFalseCount = new AtomicInteger();

    private volatile StackTraceElement setReverseFlagTrueTrace @Nullable []  = null;
    private volatile StackTraceElement setReverseFlagFalseTrace @Nullable []  = null;

    public synchronized void setReverseFlag(boolean reverseFlag) {
        try {
            if (this.reverseFlag != reverseFlag) {
                warnIfNewActionsNotReady();
                if (reverseFlag) {
                    
                    int tc = setReverseTrueCount.incrementAndGet();
                    if (tc > 1) {
                        System.out.println("tc = " + tc);
                        
                    }
                    setReverseFlagTrueTrace = Thread.currentThread().getStackTrace();
                } else {
                    setReverseFalseCount.incrementAndGet();
                    setReverseFlagFalseTrace = Thread.currentThread().getStackTrace();
                }
                this.reverseFlag = reverseFlag;
            }
        } catch (Exception e) {
            System.err.println("setReverseFlagTrueTrace = " + Arrays.toString(setReverseFlagTrueTrace));
            System.err.println("setReverseFlagFalseTrace = " + Arrays.toString(setReverseFlagFalseTrace));
            System.err.println("setReverseTrueCount = " + setReverseTrueCount);
            System.err.println("setReverseFalseCount = " + setReverseFalseCount);
            throw new RuntimeException(e);
        }

    }

    @UIType
    @SuppressWarnings("guieffect")
    private class TrayAttachOffsetModelListenerClass implements TableModelListener {

        @Override
        public void tableChanged(TableModelEvent e) {
            if (trayAttachOffsetTablemModelListenerEnabled) {
                Utils.autoResizeTableColWidths(jTableTrayAttachOffsets);
                saveTrayAttachOffsetPoseMap();
                loadTrayAttachOffsetsTableToMap();
            }
        }
    }

    private final TableModelListener trayAttachOffsetsModelListener = new TrayAttachOffsetModelListenerClass();

    @UIType
    @SuppressWarnings("guieffect")
    private class ToolOffsetModelListenerClass implements TableModelListener {

        @Override
        public void tableChanged(TableModelEvent e) {
            if (toolOffsetTablemModelListenerEnabled) {
                Utils.autoResizeTableColWidths(jTableToolOffsets);
                saveToolOffsetPoseMap();
                loadToolOffsetsTableToMap();
            }
        }
    }

    private final TableModelListener toolOffsetsModelListener = new ToolOffsetModelListenerClass();

    private volatile boolean toolHolderContentsModelListenerEnabled = false;

    private void setToolHolderContentsTableModelListener() {
        toolHolderContentsModelListenerEnabled = true;
        Utils.runOnDispatchThread(this::setToolHolderContentsTableModelListenerOnDisplay);
    }

    private void clearToolHolderContentsTableModelListener() {
        toolHolderContentsModelListenerEnabled = false;
        Utils.runOnDispatchThread(this::clearToolHolderContentsTableModelListenerOnDisplay);
    }

    @UIEffect
    private void setToolHolderContentsTableModelListenerOnDisplay() {
        jTableHolderContents.getModel().addTableModelListener(toolHolderContentsModelListener);
    }

    @UIEffect
    private void clearToolHolderContentsTableModelListenerOnDisplay() {
        jTableHolderContents.getModel().removeTableModelListener(toolHolderContentsModelListener);
    }

    @UIType
    @SuppressWarnings("guieffect")
    private class ToolHolderContentsModelListenerClass implements TableModelListener {

        @Override
        public void tableChanged(TableModelEvent e) {
            handleToolHolderContentsChange();
        }

    }

    @UIEffect
    private void handleToolHolderContentsChange() {
        if (toolHolderContentsModelListenerEnabled) {
            syncPanelToGeneratorToolData();
            Utils.autoResizeTableColWidths(jTableHolderContents);
            saveToolHolderContentsMap();
            loadToolMenus();
        }
    }

    private final TableModelListener toolHolderContentsModelListener = new ToolHolderContentsModelListenerClass();

    private CachedTable pddlOutputCachedTableModel;

    private void runSingleRow() {
        this.aprsSystem.abortCrclProgram();
        int row = pddlOutputCachedTableModel.getSelectedRow();
        currentActionIndex = row;
        setReplanFromIndex(row);
        stepping = true;
        continueActionListPrivate();
    }

    private volatile boolean reverseFlag = false;

    /**
     * Get the value of reverseFlag
     *
     * @return the value of reverseFlag
     */
    public boolean isReverseFlag() {
        return reverseFlag;
    }

    private void handlePlacePartCompleted(CrclGenerator.PlacePartInfo ppi) {
        if (null == ppi) {
            throw new IllegalArgumentException("ppi == null");
        }
        int sarc = safeAbortRequestCount.get();
        int ppiSarc = ppi.getStartSafeAbortRequestCount();
        boolean requestCountDiffer = ppiSarc != sarc;
        boolean aborting = aprsSystem.isAborting();
        aprsSystem.logEvent("handlePlacePartCompleted", "requestCountDiffer=" + requestCountDiffer + ",aboring=" + aborting + ", ppi.getPddlActionIndex()=" + ppi.getPddlActionIndex() + ",action=" + ppi.getAction().asPddlLine());
        if (requestCountDiffer || aborting) {
            crclGenerator.takeSnapshots("exec", "safeAbortRequested" + sarc + ":" + safeAboutCount.get() + ".ppi=" + ppi, null, null);
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
            crclGenerator.addMoveToLookForPosition(l, false);
            Action parentAction = ppi.getParentAction();
            if(null != parentAction && parentAction.getType() == CHECK_KITS) {
                setReplanFromIndex(ppi.getParentActionIndex(), true);
            }
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
        if (pddlOutputCachedTableModel.getRowCount() > index) {
            pddlOutputCachedTableModel.setValueAt(cost, index, 5);
        }
    }

    private void handleActionCompleted(ActionCallbackInfo actionInfo) {
        if (currentActionIndex != actionInfo.getActionIndex()) {
            LOGGER.log(Level.FINE, "(currentActionIndex != actionInfo.getActionIndex())");
            LOGGER.log(Level.FINE, "actionInfo = " + actionInfo);
            LOGGER.log(Level.FINE, "currentActionIndex = " + currentActionIndex);
        }
        if (currentActionIndex < actionInfo.getActionIndex() + 1) {
            currentActionIndex = actionInfo.getActionIndex() + 1;
            updateSelectionInterval();
            if (null != aprsSystem) {
                Utils.runOnDispatchThread(aprsSystem::updateTitle);
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

    private void updateSelectionInterval() {
        Utils.runOnDispatchThread(this::updateSelectionIntervalOnDisplay);
    }

    @UIEffect
    private void updateSelectionIntervalOnDisplay() {
        int startIndex = Math.max(0, currentActionIndex);
        int endIndex = Math.max(startIndex, getReplanFromIndex() - 1);
        pddlOutputCachedTableModel.setSelectionInterval(startIndex, endIndex);
        jTablePddlOutput.scrollRectToVisible(new Rectangle(jTablePddlOutput.getCellRect(startIndex, 0, true)));
        if (currentActionIndex > 0 && currentActionIndex < pddlOutputCachedTableModel.getRowCount()) {
            Object o = pddlOutputCachedTableModel.getValueAt(startIndex, 1);
            if (o instanceof Integer) {
                int crclIndex = ((Integer) o) - 2;
                if (crclIndex > 0 && crclIndex < crclProgramCachedTable.getRowCount()) {
                    crclProgramCachedTable.setSelectionInterval(crclIndex, crclIndex);
                    jTableCrclProgram.scrollRectToVisible(new Rectangle(jTableCrclProgram.getCellRect(crclIndex, 0, true)));
                }
            }
        }
    }

    /**
     * Get the value of externalGetPoseFunction
     *
     * @return the value of externalGetPoseFunction
     */
    public CrclGenerator.@Nullable PoseProvider getExternalPoseProvider() {
        return crclGenerator.getExternalPoseProvider();
    }

    /**
     * Set the value of externalGetPoseFunction
     *
     * @param externalGetPoseFunction new value of externalGetPoseFunction
     */
    public void setExternalPoseProvider(CrclGenerator.PoseProvider externalGetPoseFunction) {
        this.crclGenerator.setExternalPoseProvider(externalGetPoseFunction);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "rawtypes", "nullness", "deprecation"})
    @UIEffect
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
        jButtonRenameToolHolderPose = new javax.swing.JButton();
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
        positionMapJPanel1 = new aprs.actions.executor.PositionMapJPanel();
        jPanelCrcl = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableCrclProgram = new javax.swing.JTable();
        jPanelContainerPoseCache = new javax.swing.JPanel();
        jScrollPanePositionTable = new javax.swing.JScrollPane();
        jTablePositionCache = new javax.swing.JTable();
        jButtonClearPoseCache = new javax.swing.JButton();
        jButtonUpdatePoseCache = new javax.swing.JButton();
        jPanelOpOuter = new javax.swing.JPanel();
        opDisplayJPanelInput = new aprs.actions.optaplanner.display.OpDisplayJPanel();
        opDisplayJPanelSolution = new aprs.actions.optaplanner.display.OpDisplayJPanel();
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
                new Object[][]{
                        {null, null, null, null, null, null, null},
                        {null, null, null, null, null, null, null},
                        {null, null, null, null, null, null, null},
                        {null, null, null, null, null, null, null}
                },
                new String[]{
                        "#", "CRCLIndex", "Label", "Type", "Args", "Time/Cost", "TakenPart"
                }
        ) {
            Class[] types = new Class[]{
                    java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean[]{
                    false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
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

        jTableOptions.setAutoCreateRowSorter(true);
        jTableOptions.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{
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
                        {"skipMissingParts", "false"},
                        {"useJointMovesForToolHolderApproach", "true"},
                        {"joint0DiffTolerance", "20.0"}
                },
                new String[]{
                        "Name", "Value"
                }
        ) {
            Class[] types = new Class[]{
                    java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean[]{
                    false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
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

        jButtonContRandomTest.setText("Continuous Random Test");
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

        jPanelInnerManualControlLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[]{jTextFieldAdjPose, jTextFieldOffset, jTextFieldTestPose});

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

        jTableHolderContents.setAutoCreateRowSorter(true);
        jTableHolderContents.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{

                },
                new String[]{
                        "Holder Position Name", "Contents", "Possible Contents", "Comment"
                }
        ) {
            Class[] types = new Class[]{
                    java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }
        });
        jScrollPaneHolderContents.setViewportView(jTableHolderContents);

        jTabbedPaneToolChangeInner.addTab("Holder Contents", jScrollPaneHolderContents);

        jTableToolHolderPositions.setAutoCreateRowSorter(true);
        jTableToolHolderPositions.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{

                },
                new String[]{
                        "Name", "X (mm)", "Y (mm)", "Z (mm)", "Rx (deg)", "Ry (deg)", "Rz (deg)", "Approach", "Joints"
                }
        ) {
            Class[] types = new Class[]{
                    java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
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

        jButtonRenameToolHolderPose.setText("Rename");
        jButtonRenameToolHolderPose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRenameToolHolderPoseActionPerformed(evt);
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
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonRenameToolHolderPose)
                                .addContainerGap(461, Short.MAX_VALUE))
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
                                        .addComponent(jButtonAddToolHolderPose)
                                        .addComponent(jButtonRenameToolHolderPose))
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

        jTableToolOffsets.setAutoCreateRowSorter(true);
        jTableToolOffsets.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{

                },
                new String[]{
                        "ToolName", "X (mm)", "Y (mm)", "Z (mm)", "Rx (deg)", "Ry (deg)", "Rz (deg)", "Comment"
                }
        ) {
            Class[] types = new Class[]{
                    java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
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

        jTableTrayAttachOffsets.setAutoCreateRowSorter(true);
        jTableTrayAttachOffsets.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{

                },
                new String[]{
                        "TrayName", "X (mm)", "Y (mm)", "Z (mm)", "Rx (deg)", "Ry (deg)", "Rz (deg)", "Comment"
                }
        ) {
            Class[] types = new Class[]{
                    java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
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
        jTextFieldCurrentToolOffset.setText("X=0,Y=0,Z=0,roll=0,pitch=0,yaw=0");

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

        jTableCrclProgram.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{
                        {new Integer(1), null},
                        {new Integer(2), null},
                        {new Integer(3), null},
                        {new Integer(4), null}
                },
                new String[]{
                        "ID", "Text"
                }
        ) {
            Class[] types = new Class[]{
                    java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean[]{
                    false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
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
                new Object[][]{

                },
                new String[]{
                        "Name", "X", "Y", "Z", "Roll", "Pitch", "Yaw", "Comment"
                }
        ) {
            Class[] types = new Class[]{
                    java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean[]{
                    false, false, false, false, false, false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
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

        jTextFieldCurrentPart.setEditable(false);
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

    @UIEffect
    public void browseActionsFile() throws IOException {
        String text = pddlOutputActionsCachedText.getText();
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
            pddlOutputActionsCachedText.setText(f.getCanonicalPath());
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

        try {
            checkReverse();
            final int start = doingActionsStarted.incrementAndGet();
            this.abortProgram();
            setReplanFromIndex(0);
            actionSetsStarted.incrementAndGet();
            autoStart = true;
            lastActionMillis = System.currentTimeMillis();
            replanCachedCheckBox.setSelected(true);
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
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Exception in doActions(" + comment + "," + startAbortCount + ") : " + aprsSystem.getRunName(), ex);
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
        checkReverse();
        this.abortProgram();
        this.abortProgram();
        try {
            setReplanFromIndex(0);
            autoStart = true;
            lastActionMillis = System.currentTimeMillis();
            replanCachedCheckBox.setSelected(true);
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
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            abortProgram();
            throw new RuntimeException(ex);
        }
    }

    private void checkReverse() throws IllegalStateException {
        boolean revFlag = isReverseFlag();
        if (aprsSystem.isReverseFlag() != revFlag) {
            throw new IllegalStateException("aprsSystem.isReverseFlag() " + aprsSystem.isReverseFlag() + "!= isReverseFlag() " + revFlag);
        }

        if (revFlag != resetReadOnlyActionsListReverseFlag) {
            System.err.println("setReverseFlagTrueTrace = " + Arrays.toString(setReverseFlagTrueTrace));
            System.err.println("setReverseFlagFalseTrace = " + Arrays.toString(setReverseFlagFalseTrace));
            System.err.println("revFlag = " + revFlag);
            System.err.println("resetReadOnlyActionsListReverseFlag = " + resetReadOnlyActionsListReverseFlag);
            System.err.println("resetReadOnlyActionsListThread = " + resetReadOnlyActionsListThread);
            System.err.println("resetReadOnlyActionsListTrace = " + Arrays.toString(resetReadOnlyActionsListTrace));
            System.err.println("readOnlyActionsList = " + readOnlyActionsList);
            throw new IllegalStateException("revFlag != resetReadOnlyActionsListReverseFlag");
        }
        synchronized (actionsList) {
            for (int i = 0; i < actionsList.size(); i++) {
                Action action = actionsList.get(i);
                switch (action.getType()) {
                    case TAKE_PART:
                        String partname = action.getArgs()[0];
                        if (partname.contains("_in_pt_") && revFlag) {
                            System.err.println("i = " + i);
                            System.err.println("resetReadOnlyActionsListReverseFlag = " + resetReadOnlyActionsListReverseFlag);
                            System.err.println("resetReadOnlyActionsListThread = " + resetReadOnlyActionsListThread);
                            System.err.println("resetReadOnlyActionsListTrace = " + Arrays.toString(resetReadOnlyActionsListTrace));
                            System.err.println("readOnlyActionsList = " + readOnlyActionsList);
                            throw new IllegalStateException("taking part in parttray when in reverse : " + partname);
                        }
                        if (partname.contains("_in_kt_") && !revFlag) {
                            System.err.println("i = " + i);
                            System.err.println("resetReadOnlyActionsListReverseFlag = " + resetReadOnlyActionsListReverseFlag);
                            System.err.println("resetReadOnlyActionsListThread = " + resetReadOnlyActionsListThread);
                            System.err.println("resetReadOnlyActionsListTrace = " + Arrays.toString(resetReadOnlyActionsListTrace));
                            System.err.println("readOnlyActionsList = " + readOnlyActionsList);
                            throw new IllegalStateException("taking part in kittray when not in reverse : " + partname);
                        }
                        break;

                    default:
                        break;
                }
            }
        }
    }

    private final CrclGenerator crclGenerator;

    public CrclGenerator getCrclGenerator() {
        return crclGenerator;
    }

    private final List<Action> actionsList = Collections.synchronizedList(new ArrayList<>());
    private volatile List<Action> readOnlyActionsList = Collections.unmodifiableList(new ArrayList<>(actionsList));

    @Nullable
    private volatile Thread resetReadOnlyActionsListThread = null;
    private volatile StackTraceElement resetReadOnlyActionsListTrace@Nullable []  = null;
    private volatile boolean resetReadOnlyActionsListReverseFlag = false;

    private List<Action> resetReadOnlyActionsList(boolean newReverseFlag) {
        final Thread curThread = Thread.currentThread();

        if (null == resetReadOnlyActionsListThread) {
            resetReadOnlyActionsListThread = curThread;
            resetReadOnlyActionsListTrace = curThread.getStackTrace();
        } else if (curThread != resetReadOnlyActionsListThread) {
            LOGGER.log(Level.FINE, "resetReadOnlyActionsList from new thread {0}", curThread.getName());
        }
        List<Action> newReadOnlyActionsList = Collections.unmodifiableList(new ArrayList<>(actionsList));
        if (newReadOnlyActionsList.isEmpty()) {
            warnIfNewActionsNotReady();
        }
        this.readOnlyActionsList = newReadOnlyActionsList;
        resetReadOnlyActionsListReverseFlag = newReverseFlag;
        checkReverse();
        return newReadOnlyActionsList;
    }

    private static final Logger LOGGER = Logger.getLogger(ExecutorJPanel.class.getName());

    /**
     * Get the value of actionsList
     *
     * @return the value of actionsList
     */
    @Override
    public List<Action> getActionsList() {
        return readOnlyActionsList;
    }

    @Override
    public void clearActionsList() {
        warnIfNewActionsNotReady();
        synchronized (actionsList) {
            if (actionsList.size() > 0) {
                actionsList.clear();
                resetReadOnlyActionsList(reverseFlag);
            }
        }
        pddlOutputCachedTableModel.setRowCount(0);
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

//    @UIEffect
//    private String[] getComboPartNames(int maxlen) {
//        String names[] = manualObjectCachedComboBox.getItems();
//        if (names.length > maxlen) {
//            return Arrays.copyOfRange(names, 0, maxlen);
//        }
//        return names;
//    }
    @UIEffect
    private String[] getComboSlotNames(int maxlen) {
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
        } catch (Exception exception) {
            Logger.getLogger(ExecutorJPanel.class
                    .getName()).log(Level.SEVERE, "", exception);
        }
        return str;
    }

    @Nullable
    private String actionsFileString = null;
    @Nullable
    private String reverseActionsFileString = null;

    public void saveProperties() {
        Map<String, String> propsMap = new HashMap<>();
        try {
            updateActionFileStrings();
        } catch (HeadlessException | IllegalStateException | IOException ex) {
            Logger.getLogger(ExecutorJPanel.class.getName()).log(Level.SEVERE, "", ex);
        }
        final String reversActionsPath = reverseActionsFileString;
        if (reversActionsPath != null && reversActionsPath.length() > 0) {
            if (!isTempDir(reversActionsPath)) {
                String relPath = makeShortPath(propertiesFile, reversActionsPath);
                logDebug("relPath = " + relPath);
                File chkFile = new File(relPath);
                if (!chkFile.isDirectory()) {
                    propsMap.put(REVERSE_PDDLOUTPUT, relPath);
                }
            }
        }
        final String actionsPath = actionsFileString;
        if (actionsPath != null && actionsPath.length() > 0) {
            if (!isTempDir(actionsPath)) {
                String relPath = makeShortPath(propertiesFile, actionsPath);
                logDebug("relPath = " + relPath);
                File chkFile = new File(relPath);
                if (!chkFile.isDirectory()) {
                    propsMap.put(PDDLOUTPUT, relPath);
                }
            }
        }
        propsMap.put(REVERSE_FLAG, Boolean.toString(reverseFlag));
        Properties props = new Properties();
        props.putAll(propsMap);
        props.putAll(getTableOptions());
//        props.put(MANUAL_PART_NAMES, Arrays.toString(getComboPartNames(10)));
//        props.put(MANUAL_SLOT_NAMES, Arrays.toString(getComboSlotNames(10)));
        props.put(POS_ERROR_MAP_FILES, Arrays.toString(getRelPathPositionMapFileNames()));
        props.put(ENABLE_OPTA_PLANNER, Boolean.toString(enableOptaplannerCachedCheckBox.isSelected()));
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

    private void updateActionFileStrings() throws HeadlessException, IllegalStateException, IOException {
        if (reverseFlag) {
            this.reverseActionsFileString = pddlOutputActionsCachedText.getText();
            checkFilename(reverseActionsFileString);

        } else {
            this.actionsFileString = pddlOutputActionsCachedText.getText();
            checkFilename(actionsFileString);
        }
    }

    private void checkFilename(@Nullable String filename) throws HeadlessException, IllegalStateException, IOException {
        if (null != filename && filename.length() > 0) {
            File f = new File(filename);
            if (!f.exists()) {
                System.out.println("f.getCanonicalPath() = " + f.getCanonicalPath());
                Utils.showMessageDialog(this, filename + " does not exist");
                throw new IllegalStateException(filename + " does not exist");
            }
            if (!f.canRead()) {
                System.out.println("f.getCanonicalPath() = " + f.getCanonicalPath());
                Utils.showMessageDialog(this, filename + " can not be read");
                throw new IllegalStateException(filename + " can not be read");
            }
            if (f.isDirectory()) {
                System.out.println("f.getCanonicalPath() = " + f.getCanonicalPath());
                Utils.showMessageDialog(this, filename + " is a directory");
                throw new IllegalStateException(filename + " is a directory");
            }
        }
    }

    private static final String POS_ERROR_MAP_FILES = "positionMapFileNames";
    private static final String MANUAL_PART_NAMES = "manualPartNames";
    private static final String MANUAL_SLOT_NAMES = "manualSlotNames";

    @Override
    public void addAction(Action action) {
        if (null != action) {
            this.actionsList.add(action);
            double cost = 0.0;
            try {
                cost = Double.parseDouble(action.getCost());
            } catch (NumberFormatException ex) {
                // ignore 
            }
            final double finalCost = cost;
            int rowCount = pddlOutputCachedTableModel.getRowCount();
            pddlOutputCachedTableModel.addRow(new Object[]{rowCount, -1, action.getLabel(), action.getType(), Arrays.toString(action.getArgs()), finalCost, ""});
        }
    }

    @Override
    public void processActions() {
        checkReverse();
        try {
            if (null != runningProgramFuture) {
                runningProgramFuture.cancel(true);
            }
            runningProgramFuture = generateCrclAsync();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            abortProgram();
        }
    }

    private static final SolverFactory<OpActionPlan> solverFactory = createSolverFactory();

    static private SolverFactory<OpActionPlan> createSolverFactory() {
        return SolverFactory.createFromXmlResource(
                "aprs/actions/optaplanner/actionmodel/actionModelSolverConfig.xml");
    }

    @MonotonicNonNull
    private Solver<OpActionPlan> solver = null;

    @MonotonicNonNull
    private Solver<OpActionPlan> showSolver = null;

    private static volatile boolean firstLoad = true;

    private volatile int lastReadyReturnPos = 0;

    public boolean readyForNewActionsList() {
        if (readOnlyActionsList == null || readOnlyActionsList.isEmpty()) {
            lastReadyReturnPos = 1;
            return true;
        }
        if (null != crclGenerator) {
            if (crclGenerator.getLastIndex() == 0) {
                lastReadyReturnPos = 2;
                return true;
            }
            if (crclGenerator.atLastIndex()) {
                lastReadyReturnPos = 3;
                return true;
            }
        }
        return false;
    }

    public void warnIfNewActionsNotReady() {
        if (!readyForNewActionsList()) {
            System.err.println("readOnlyActionsList = " + readOnlyActionsList);
            System.err.println("crclGenerator.getLastIndex() = " + crclGenerator.getLastIndex());
            System.err.println("crclGenerator.atLastIndex() = " + crclGenerator.atLastIndex());
            System.err.println("lastReadyReturnPos=" + lastReadyReturnPos);
            throw new IllegalStateException(
                    "loading new actions when not ready ");
        }
    }

    public List<Action> loadActionsList(Iterable<? extends Action> newActions, boolean newReverseFlag) {
        warnIfNewActionsNotReady();
        setReverseFlag(newReverseFlag);
        List<Action> ret;
        synchronized (actionsList) {
            clearActionsList();
            for (Action action : newActions) {
                addAction(action);
            }
            ret = resetReadOnlyActionsList(newReverseFlag);
        }
        finishLoadActionsList(pddlOutputActionsCachedText.getText());
        return ret;
    }

    public List<Action> loadActionsFile(File f, boolean showInOptaPlanner, boolean newReverseFlag) throws IOException {

        warnIfNewActionsNotReady();
        setReverseFlag(newReverseFlag);
        if (null == f) {
            throw new IllegalArgumentException("f == null");
        }
        if (!f.exists()) {
            throw new IllegalArgumentException("\"" + f + "\"  does not exist.");
        }
        if (f.isDirectory()) {
            throw new IllegalArgumentException("Can not loadActionsFile \"" + f + "\" : it is a directory instead of a text file.");
        }
        if (!f.canRead()) {
            throw new IllegalArgumentException("Can not loadActionsFile \"" + f + "\" : it is not readable.");
        }

        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while (null != (line = br.readLine())) {
                lines.add(line);
            }
        }
        List<Action> ret;
        synchronized (actionsList) {
            clearActionsList();
            for (String line : lines) {
                if (line.length() < 1) {
                    continue;
                }
                addAction(Action.parse(line));
            }
            ret = resetReadOnlyActionsList(newReverseFlag);
        }
        try {
            if (showInOptaPlanner && crclGenerator.isConnected()) {
                showLoadedPlanOptaPlanner(newReverseFlag);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
        }
        String canonName = f.getCanonicalPath();
        finishLoadActionsList(canonName);
        return ret;
    }

    private final CachedTextField indexCachedTextField;

    private void finishLoadActionsList(String canonName) {
        setReplanFromIndex(0);
        autoResizeTableColWidthsPddlOutput();
        indexCachedTextField.setText("0");
        updateComboPartModel();
        updateComboSlotModel();
        String origActionsName = pddlOutputActionsCachedText.getText();
        if (!origActionsName.equals(canonName)) {
            try {
                pddlOutputActionsCachedText.setText(canonName);
                updateActionFileStrings();
            } catch (Exception ex) {
                Logger.getLogger(ExecutorJPanel.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
    }

    private void showLoadedPlanOptaPlanner(boolean newReverseFlag) throws SQLException {
        crclGenerator.clearPoseCache();
        crclGenerator.setOptions(getTableOptions());
        PointType lookForPt = crclGenerator.getLookForXYZ();
        if (null != lookForPt && enableOptaplannerCachedCheckBox.isSelected()) {

            List<OpAction> opActions;
            int startIndex = 0;
            for (int i = 0; i < actionsList.size(); i++) {
                if (actionsList.get(i).getType() == LOOK_FOR_PARTS) {
                    startIndex = i + 1;
                    break;
                }
            }
            synchronized (actionsList) {
                opActions = crclGenerator.pddlActionsToOpActions(actionsList, 0);
                resetReadOnlyActionsList(newReverseFlag);
            }
            if (null == opActions || opActions.size() < 2) {
                return;
            }
            OpActionPlan worstPlan = null;
            double worstScore = Double.POSITIVE_INFINITY;
            OpActionPlan bestPlan = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            if (null == showSolver) {
                synchronized (solverFactory) {
                    showSolver = solverFactory.buildSolver();
                }
            }
            showSolver.addEventListener(e -> logDebug(e.getTimeMillisSpent() + ", " + e.getNewBestScore()));

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
                OpActionPlan solvedPlan = showSolver.solve(inputPlan);
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
            if (null != bestPlan && null != worstPlan) {
                OpActionPlan worstPlanFinal = worstPlan;
                double worstScoreFinal = worstScore;
                OpActionPlan bestPlanFinal = bestPlan;
                double bestScoreFinal = bestScore;
                Utils.runOnDispatchThread(() -> showLoadedPlanOptaPlannerOnDisplay(worstPlanFinal, worstScoreFinal, bestPlanFinal, bestScoreFinal));
            }
        }
    }

    @UIEffect
    private void showLoadedPlanOptaPlannerOnDisplay(OpActionPlan worstPlan, double worstScore, OpActionPlan bestPlan, double bestScore) {
        if (firstLoad) {
            OpDisplayJPanel.clearColorMap();
            firstLoad = false;
        }
        if (null != worstPlan) {
            this.opDisplayJPanelInput.setOpActionPlan(worstPlan);
        }
        if (null != bestPlan) {
            this.opDisplayJPanelSolution.setOpActionPlan(bestPlan);
        }
        this.opDisplayJPanelInput.setLabel("Input : " + String.format("%.1f mm ", -worstScore));
        this.opDisplayJPanelSolution.setLabel("Output : " + String.format("%.1f mm ", -bestScore));
    }

    void autoResizeTableColWidthsPddlOutput() {
        autoResizeTableColWidths(jTablePddlOutput);
    }

    public void refresh() {
        crclGenerator.reset();
        String origErrorString = this.getErrorString();
        String fname = pddlOutputActionsCachedText.getText();
        File f = new File(fname);
        if (f.exists() && f.canRead()) {
            try {
                loadActionsFile(f, true, reverseFlag);
                if (Objects.equals(this.getErrorString(), origErrorString)) {
                    setErrorString(null);
                }
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "", ex);
            }
        }
    }

    @UIEffect
    private void jButtonLoadPddlActionsFromFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoadPddlActionsFromFileActionPerformed
        try {
            browseActionsFile();
            loadActionsFile(new File(pddlOutputActionsCachedText.getText()), true, reverseFlag);
        } catch (IOException ex) {
            Logger.getLogger(AprsSystem.class
                    .getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jButtonLoadPddlActionsFromFileActionPerformed

    @UIEffect
    private void jButtonLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoadActionPerformed
        try {
            loadActionsFile(new File(pddlOutputActionsCachedText.getText()), true, reverseFlag);

        } catch (IOException ex) {
            Logger.getLogger(AprsSystem.class
                    .getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jButtonLoadActionPerformed

    @UIEffect
    private void jTextFieldPddlOutputActionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldPddlOutputActionsActionPerformed
        try {
            loadActionsFile(new File(pddlOutputActionsCachedText.getText()), true, reverseFlag);

        } catch (IOException ex) {
            Logger.getLogger(AprsSystem.class
                    .getName()).log(Level.SEVERE, "", ex);
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
                    .getName()).log(Level.SEVERE, "", ex);
        }
    }

    public long incrementAndGetCommandId() {
        return crclGenerator.incrementAndGetCommandId();
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
    private JTextArea editTableArea = new JTextArea();

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

    private XFutureVoid loadProgramToTable(CRCLProgramType crclProgram) {
        return Utils.runOnDispatchThread(() -> loadProgramToTableInternal(crclProgram));
    }

    @UIType
    @SuppressWarnings("guieffect")
    private class CrclTableCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, @Nullable Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JTextArea area = new JTextArea();
            area.setOpaque(true);
            area.setVisible(true);
            area.setText(Objects.toString(value));
            return area;
        }
    }

    @UIType
    @SuppressWarnings("guieffect")
    private class CrclTableCellEditor implements TableCellEditor {

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
            for (CellEditorListener l : listeners) {
                if (null != l) {
                    l.editingStopped(new ChangeEvent(jTableCrclProgram));
                }
            }
            return true;
        }

        @Override
        public void cancelCellEditing() {
            for (CellEditorListener l : listeners) {
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
    }

    @UIEffect
    private void loadProgramToTableInternal(CRCLProgramType crclProgram) {
        jTableCrclProgram.setBackground(Color.white);
        jTableCrclProgram.getColumnModel().getColumn(1).setCellRenderer(new CrclTableCellRenderer());
        jTableCrclProgram.getColumnModel().getColumn(1).setCellEditor(new CrclTableCellEditor());
        crclProgramCachedTable.setRowCount(0);
        CRCLSocket crclSocket = CRCLSocket.getUtilSocket();
        CRCLCommandInstanceType instance = new CRCLCommandInstanceType();
        InitCanonType initCanon = crclProgram.getInitCanon();
        if (null != initCanon) {
            try {
                instance.setCRCLCommand(initCanon);
                crclProgramCachedTable.addRow(new Object[]{initCanon.getCommandID(),
                    trimXml(crclSocket.commandInstanceToPrettyString(instance, true))
                });

            } catch (JAXBException | CRCLException ex) {
                crclProgramCachedTable.addRow(new Object[]{initCanon.getCommandID(),
                    ex.getMessage()
                });
                LOGGER.log(Level.SEVERE, "", ex);
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
                    crclProgramCachedTable.addRow(new Object[]{midCmd.getCommandID(),
                        trimXml(crclSocket.commandInstanceToPrettyString(instance, true))
                    });

                } catch (JAXBException | CRCLException ex) {
                    crclProgramCachedTable.addRow(new Object[]{midCmd.getCommandID(),
                        ex.getMessage()
                    });
                    LOGGER.log(Level.SEVERE, "", ex);
                }
            }
        }
        EndCanonType endCanon = crclProgram.getEndCanon();
        if (null != endCanon) {
            try {
                instance.setCRCLCommand(endCanon);
                crclProgramCachedTable.addRow(new Object[]{endCanon.getCommandID(),
                    trimXml(crclSocket.commandInstanceToPrettyString(instance, true))
                });

            } catch (JAXBException | CRCLException ex) {
                crclProgramCachedTable.addRow(new Object[]{endCanon.getCommandID(),
                    ex.getMessage()
                });
                LOGGER.log(Level.SEVERE, "", ex);
            }
        }
        autoResizeTableRowHeights(jTableCrclProgram);
        autoResizeTableColWidths(jTableCrclProgram);
    }

    private boolean autoStart = true;

    @Nullable
    private volatile CRCLProgramType unstartedProgram = null;

    final private AprsSystem aprsSystem;

    /**
     * Get the value of aprsSystemInterface
     *
     * @return the value of aprsSystemInterface
     */
    public AprsSystem getAprsSystem() {
        return aprsSystem;
    }

    private final CachedCheckBox forceFakeTakeFlagCachedCheckBox;

    public boolean getForceFakeTakeFlag() {
        return forceFakeTakeFlagCachedCheckBox.isSelected();
    }

    private void setCheckBoxForceFakeTakeSelected(boolean selected) {
        forceFakeTakeFlagCachedCheckBox.setSelected(selected);
    }

    public void setForceFakeTakeFlag(boolean _force) {
        if (forceFakeTakeFlagCachedCheckBox.isSelected() != _force) {
            forceFakeTakeFlagCachedCheckBox.setSelected(_force);
            if (null != aprsSystem) {
                aprsSystem.setExecutorForceFakeTakeFlag(_force);
            }
        }
    }

    private final CachedTextField pddlOutputActionsCachedText;

    private String getActionsCrclName() {
        String actionsName = pddlOutputActionsCachedText.getText();
        int sindex = actionsName.lastIndexOf('/');
        if (sindex > 0 && sindex < actionsName.length() - 1) {
            actionsName = actionsName.substring(sindex + 1);
        }
        sindex = actionsName.lastIndexOf('\\');
        if (sindex > 0 && sindex < actionsName.length() - 1) {
            actionsName = actionsName.substring(sindex + 1);
        }
        return toNonColonizedName(actionsName + ":" + currentActionIndex + "_" + crclGenerator.getLastIndex() + ":" + crclGenerator.getCrclNumber());
    }

    private String toNonColonizedName(String name) {
        return name.trim().replace(' ', '_').replace('=', '_').replace(':', '.');
    }

    /**
     * Set the value of crclProgram
     *
     * @param crclProgram new value of crclProgram
     */
    private void setCrclProgram(CRCLProgramType crclProgram) {
        try {
            this.crclProgram = crclProgram;
            unstartedProgram = crclProgram;
            String crclProgramName = crclProgram.getName();
            if (crclProgramName == null || crclProgramName.length() < 1) {
                crclProgram.setName(getActionsCrclName());
            }
            loadProgramToTable(crclProgram);
            if (null != aprsSystem) {
                aprsSystem.addProgramLineListener(this);
                aprsSystem.setCRCLProgram(crclProgram);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
        }
    }

    private XFuture<Boolean> continueCurrentCrclProgram() {
        return aprsSystem.continueCrclProgram();
    }

    private volatile long startCrclProgramTime = 0;
    private final AtomicInteger startCrclProgramCount = new AtomicInteger(0);

    /**
     * Start executing a CRCL Program.
     * <p>
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
    private XFuture<Boolean> startCrclProgram(CRCLProgramType crclProgram) {
        try {
            prepCrclProgram(crclProgram);
            return aprsSystem.startCRCLProgram(crclProgram);
        } catch (Exception ex) {
            XFuture<Boolean> future = new XFuture<>("startCrclProgramException");
            LOGGER.log(Level.SEVERE, "", ex);
            future.completeExceptionally(ex);
            return future;
        }
    }

    private void prepCrclProgram(CRCLProgramType crclProgram1) throws IllegalStateException {
        startCrclProgramTime = System.currentTimeMillis();
        startCrclProgramCount.incrementAndGet();
        this.crclProgram = crclProgram1;
        unstartedProgram = null;
        loadProgramToTable(crclProgram1);
        this.runningProgram = true;
        if (null == aprsSystem) {
            throw new IllegalStateException("Can't start crcl program with null aprsSystemInterface reference.");
        }
        aprsSystem.addProgramLineListener(this);
        String crclProgramName = crclProgram.getName();
        if (crclProgramName == null || crclProgramName.length() < 1) {
            crclProgram1.setName(getActionsCrclName());
        }
    }

    private boolean runCrclProgram(CRCLProgramType crclProgram) throws JAXBException {
        prepCrclProgram(crclProgram);
        boolean ret = aprsSystem.runCRCLProgram(crclProgram);
        try {
            aprsSystem.saveLastProgramRunDataListToCsv(aprsSystem.createTempFile("programRunData", ".csv"));
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "", ex);
        }
//        logDebug("runCrclProgram returned = " + ret);
        return ret;
    }

    @UIEffect
    private void jButtonGenerateCRCLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGenerateCRCLActionPerformed

        try {
            autoStart = false;
            setReplanFromIndex(0);
            cancelRunProgramFuture();
            runningProgramFuture = generateCrclAsync();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            abortProgram();
        }
    }//GEN-LAST:event_jButtonGenerateCRCLActionPerformed

    private void cancelRunProgramFuture() {
        if (null != runningProgramFuture) {
            runningProgramFuture.cancelAll(true);
        }
    }

    @UIEffect
    private void jButtonPddlOutputViewEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPddlOutputViewEditActionPerformed
        try {
            Desktop.getDesktop().open(new File(pddlOutputActionsCachedText.getText()));

        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jButtonPddlOutputViewEditActionPerformed

    private int getReplanFromIndex() {
        return replanFromIndex.get();
    }

    @Nullable
    private String currentPart = null;
    private volatile StackTraceElement setReplanFromIndexLastTrace@Nullable []  = null;
    private volatile StackTraceElement prevSetReplanFromIndexLastTrace@Nullable []  = null;

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
                    logDebug("Reducing replanFromIndex when generater not readyForNewActionsList: oldRpi=" + oldRpi + ", new replanFromIndex=" + replanFromIndex + ",  pddlActionToCrclGenerator.getLastIndex()=" + crclGenerator.getLastIndex());
                }
            }
            @Nullable
            String[] names = this.crclGenerator.getActionToCrclTakenPartsNames();
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

    @UIEffect
    private void jTextFieldIndexActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldIndexActionPerformed
        setReplanFromIndex(Integer.parseInt(jTextFieldIndex.getText()));
    }//GEN-LAST:event_jTextFieldIndexActionPerformed

    @UIEffect
    private void jButtonClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearActionPerformed
        clearAll();
    }//GEN-LAST:event_jButtonClearActionPerformed

    private final AtomicInteger clearAllCount = new AtomicInteger(0);
    private volatile long clearAllTime = 0;

    private final CachedTable crclProgramCachedTable;

    private void clearAll() {
        warnIfNewActionsNotReady();
        clearAllCount.incrementAndGet();
        clearAllTime = System.currentTimeMillis();
        clearActionsList();
        crclProgramCachedTable.setRowCount(0);
        setReplanFromIndex(0);
        safeAbortRunnablesVector.clear();
        abortProgram();
        lastActionMillis = System.currentTimeMillis();
        setErrorString(null);
        aprsSystem.setTitleErrorString(null);
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
            Utils.runOnDispatchThread(this::stopReplanActionTimer);
        }
        this.replanRunnable = this.defaultReplanRunnable;
        if (null != aprsSystem) {
            aprsSystem.abortCrclProgram();
        }
        completeSafeAbort();
        replanStarted.set(rps);
        runningProgram = false;
        abortProgramTime = System.currentTimeMillis();
        abortProgramCount.incrementAndGet();
    }

    private void stopReplanActionTimer() {
        if (null != replanActionTimer) {
            Utils.runOnDispatchThread(this::stopReplanActionTimerOnDisplay);
        }
    }

    @UIEffect
    private void stopReplanActionTimerOnDisplay() {
        if (null != replanActionTimer) {
            replanActionTimer.stop();
            replanActionTimer = null;
        }
    }

    private void completeSafeAbort() {
        Runnable r;
        while (null != (r = safeAbortRunnablesVector.pollFirst())) {
            r.run();
        }
    }

    private int takePartCount = 0;

    @UIEffect
    private void jButtonTakeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonTakeActionPerformed
        try {
            setSelectedManualObjectName();
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
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            abortProgram();
            showExceptionInProgram(ex);
        }
    }//GEN-LAST:event_jButtonTakeActionPerformed

    private final CachedComboBox<String> manualObjectCachedComboBox;

    private void setSelectedManualObjectName() {
        if (null != currentPart) {
            this.manualObjectCachedComboBox.setSelectedItem(currentPart);
        }
    }

    private void warnDialog(String msg) throws HeadlessException {
        LOGGER.log(Level.WARNING, msg);
        Utils.showMessageDialog(this, msg);
    }

    @Nullable
    private String getComboPart() {
        Object object = manualObjectCachedComboBox.getSelectedItem();
        if (null == object) {
            return null;
        }
        String part = object.toString();
        boolean partfound = false;
        for (int i = 0; i < manualObjectCachedComboBox.getSize(); i++) {
            String parti = manualObjectCachedComboBox.getElementAt(i);
            if (parti.equals(part)) {
                partfound = true;
                break;
            }
        }
        if (!partfound) {
            manualObjectCachedComboBox.insertElementAt(part, 0);
        }
        return part;
    }

    private void updateComboPartModel() {
        boolean first = true;
        synchronized (actionsList) {
            for (Action action : actionsList) {
                switch (action.getType()) {
                    case FAKE_TAKE_PART:
                    case TAKE_PART:
                        if (action.getArgs().length > 0) {
                            boolean found = false;
                            String part = action.getArgs()[0];
                            for (int i = 0; i < manualObjectCachedComboBox.getSize(); i++) {
                                if (manualObjectCachedComboBox.getElementAt(i).equals(part)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                manualObjectCachedComboBox.insertElementAt(part, 0);
                            }
                            if (first) {
                                manualObjectCachedComboBox.setSelectedItem(part);
                                first = false;
                            }
                        }
                        break;
                }
            }
        }
    }

    private final CachedComboBox<String> manualSlotCachedComboBox;

    private void updateComboSlotModel() {
        boolean first = true;
        synchronized (actionsList) {
            for (Action action : actionsList) {
                switch (action.getType()) {
                    case PLACE_PART:
                        if (action.getArgs().length > 0) {
                            boolean found = false;
                            String slot = action.getArgs()[0];
                            for (int i = 0; i < manualSlotCachedComboBox.getSize(); i++) {
                                if (manualSlotCachedComboBox.getElementAt(i).equals(slot)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                manualSlotCachedComboBox.insertElementAt(slot, 0);
                            }
                            if (first) {
                                manualSlotCachedComboBox.setSelectedItem(slot);
                                first = false;
                            }
                        }
                        break;
                }
            }
        }
    }

    @Nullable
    private String getComboSlot() {
        String slot = manualSlotCachedComboBox.getSelectedItem();
        if (null != slot) {
            boolean partfound = false;
            for (int i = 0; i < manualSlotCachedComboBox.getSize(); i++) {
                String parti = manualSlotCachedComboBox.getElementAt(i);
                if (parti.equals(slot)) {
                    partfound = true;
                    break;
                }
            }
            if (!partfound) {
                manualSlotCachedComboBox.insertElementAt(slot, 0);
            }
        }
        return slot;
    }

    private int lookForCount = 0;

    @UIEffect
    private void jButtonLookForActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLookForActionPerformed
        try {
            setSelectedManualObjectName();
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

    @UIEffect
    private void jButtonReturnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonReturnActionPerformed
        try {
            setSelectedManualObjectName();
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
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            showExceptionInProgram(ex);
        }
    }//GEN-LAST:event_jButtonReturnActionPerformed

    private int randomDropOffCount = 0;

    @UIEffect
    private void jButtonRandDropOffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRandDropOffActionPerformed
        randomDropOffCount++;
        clearAll();
        this.jTextFieldRandomDropoffCount.setText(Integer.toString(randomDropOffCount));
        if (null != runningProgramFuture) {
            runningProgramFuture.cancel(true);
        }
        runningProgramFuture = this.randomDropOff();
        logDebug("randomDropOffCount = " + randomDropOffCount);
    }//GEN-LAST:event_jButtonRandDropOffActionPerformed

    private int randomPickupCount = 0;

    private void writeCorrectionCsv(String filename, String line) throws IOException {
        File f = new File(filename);
        logDebug("f.getCanonicalPath() = " + f.getCanonicalPath());
        boolean newFile = !f.exists();
        logDebug("newFile = " + newFile);
        try (PrintWriter pw = new PrintWriter(new FileWriter(f, true))) {
            if (newFile) {
                pw.println("Time,PartName,Robot_X,Robot_Y,Robot_Z,Db_X,Db_Y,Db_Z,Offset_X,Offset_Y,Offset_Z");
            }
            pw.println(line);
        }
    }

    @UIEffect
    private void jButtonTestPickupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonTestPickupActionPerformed
        clearAll();
        queryLogFileName();
        recordAndCompletTestPickup();
    }//GEN-LAST:event_jButtonTestPickupActionPerformed

    private String recordCsvName = "corrections.csv";

    @Nullable
    private String getComboBoxManualObjectSelectedItem() {
        return manualObjectCachedComboBox.getSelectedItem();
    }

    private XFuture<Boolean> recordAndCompletTestPickup() {
        try {
            randomPickupCount++;
            this.jTextFieldRandomPickupCount.setText(Integer.toString(randomPickupCount));
            PointType testDropOffPosePoint = requireNonNull(testDropOffPose.getPoint(), "testDropOffPose.getPoint()");
            String randomPoseString = testDropOffPosePoint.getX()
                    + "," + testDropOffPosePoint.getY()
                    + "," + testDropOffPosePoint.getZ();
            logDebug("randomPoseString = " + randomPoseString);
            logDebug("randomPickupCount = " + randomPickupCount);
            String partName = getComboBoxManualObjectSelectedItem();
            if (null != partName && partName.length() > 0) {
                PoseType poseFromDb = crclGenerator.getPose(partName);
                if (null != poseFromDb) {
                    PointType poseFromDbPoint = requireNonNull(poseFromDb.getPoint(), "poseFromDb.getPoint()");
                    String poseFromDbString = poseFromDbPoint.getX()
                            + "," + poseFromDbPoint.getY()
                            + "," + poseFromDbPoint.getZ();
                    logDebug("poseFromDbString = " + poseFromDbString);
                    String offsetString
                            = (testDropOffPosePoint.getX() - poseFromDbPoint.getX())
                            + "," + (testDropOffPosePoint.getY() - poseFromDbPoint.getY())
                            + "," + (testDropOffPosePoint.getZ() - poseFromDbPoint.getZ());
                    logDebug("offsetString = " + offsetString);
                    writeCorrectionCsv(recordCsvName,
                            System.currentTimeMillis() + ", " + partName + ", " + randomPoseString + ", " + poseFromDbString + ", " + offsetString);
                }
                return this.randomPickup();
            }
            return XFuture.completedFuture(false);
        } catch (CRCLException | PmException | IOException | SQLException ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            XFuture<Boolean> future = new XFuture<>("recordAndCompletTestPickupException");
            future.completeExceptionally(ex);
            return future;
        }
    }

    @UIEffect
    private void jButtonContRandomTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonContRandomTestActionPerformed
        //startRandomTest();
    }//GEN-LAST:event_jButtonContRandomTestActionPerformed

    /*private XFuture<Boolean> startRandomTest() {
        clearAll();
        queryLogFileName();
        jCheckBoxReplan.setSelected(true);
        autoStart = true;

        PointType testDropOffPosePoint = requireNonNull(testDropOffPose.getPoint(), "testDropOffPose.getPoint()");
        String randomPoseString
                = String.format("%.1f, %.1f, %.1f",
                testDropOffPosePoint.getX(),
                testDropOffPosePoint.getY(),
                testDropOffPosePoint.getZ());
        logDebug("randomPoseString = " + randomPoseString);
        logDebug("randomDropOffCount = " + randomDropOffCount);
        customRunnables.clear();
        customRunnables.add(() -> {
            logDebug("Continuing with lookFor");
            this.lookForParts();
        });
        customRunnables.add(() -> {
            logDebug("Continuing with recordAndCompletRandomPickup");
            this.recordAndCompletTestPickup();
        });
        customRunnables.add(() -> {
            logDebug("Continuing with randomDropOff");
            this.randomDropOff();
        });
        this.customRunnablesIndex = 0;
        this.replanRunnable = this.customReplanRunnable;
        return this.randomDropOff()
                .thenCompose("randomTest.lookForParts",
                        x -> recursiveSupplyBoolean(x, this::lookForParts))
                .thenCompose("randomTest.recordAndCompletTestPickup",
                        x -> recursiveSupplyBoolean(x, this::recordAndCompletTestPickup))
                .thenCompose("randomTest.randomDropOff",
                        x -> recursiveSupplyBoolean(x, this::randomDropOff));
    }*/
    @UIEffect
    private void startGridTest() throws HeadlessException {

        try {
            clearAll();
            String gridSizeString = jTextFieldGridSize.getText();
            String fa[] = gridSizeString.split("[ ,]+");
            if (fa.length != 2) {
                logDebug("Bad gridSizeStrng = " + gridSizeString);
                return;
            }
            try {
                int xmax = Integer.parseInt(fa[0]);
                if (xmax < 1) {
                    logDebug("Bad gridSizeStrng = " + gridSizeString);
                    return;
                }
                int ymax = Integer.parseInt(fa[1]);
                if (ymax < 1) {
                    logDebug("Bad gridSizeStrng = " + gridSizeString);
                    return;
                }
                this.gridTestMaxX = (double) xmax;
                this.gridTestMaxY = (double) ymax;
            } catch (Exception e) {
                logDebug("Bad gridSizeStrng = " + gridSizeString);
                return;
            }
            this.gridTestCurrentX = 0.0;
            this.gridTestCurrentY = 0.0;
            logDebug("gridTestMaxX = " + gridTestMaxX);
            logDebug("gridTestMaxY = " + gridTestMaxY);
            queryLogFileName();
            replanCachedCheckBox.setSelected(true);
            autoStart = true;
//            jCheckBoxAutoStartCrcl.setSelected(true);
            final String partName = manualObjectCachedComboBox.getSelectedItem();
            this.gridDropOff();
            PointType testDropOffPosePoint = requireNonNull(testDropOffPose.getPoint(), "testDropOffPose.getPoint()");
            String randomPoseString
                    = String.format("%.1f, %.1f, %.1f",
                            testDropOffPosePoint.getX(),
                            testDropOffPosePoint.getY(),
                            testDropOffPosePoint.getZ());
            logDebug("randomPoseString = " + randomPoseString);
            logDebug("randomDropOffCount = " + randomDropOffCount);
            customRunnables.clear();
            customRunnables.add(() -> {
                logDebug("Continuing with lookFor");
                this.lookForParts();
            });
            customRunnables.add(() -> {
                logDebug("Continuing with recordAndCompletRandomPickup");
                this.recordAndCompletTestPickup();
            });
            customRunnables.add(() -> {
                logDebug("Continuing with gridDropOff");
                this.gridDropOff();
            });
            this.customRunnablesIndex = 0;
            this.replanRunnable = this.customReplanRunnable;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
        }
    }

    @UIEffect
    private void jButtonStopRandomTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonStopRandomTestActionPerformed
        this.clearAll();
    }//GEN-LAST:event_jButtonStopRandomTestActionPerformed

    @UIEffect
    private void jCheckBoxDebugActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxDebugActionPerformed
        this.setDebug(this.jCheckBoxDebug.isSelected());
    }//GEN-LAST:event_jCheckBoxDebugActionPerformed

    @UIEffect
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

    private int recordFailCount = 0;

    @UIEffect
    private void jButtonRecordFailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRecordFailActionPerformed
        recordFailCount++;
        jTextFieldRecordFailCount.setText(Integer.toString(recordFailCount));
        try {
            File f = new File(jTextFieldLogFilename.getText());
            addFailLogCsvHeader(f);
            String partName = manualObjectCachedComboBox.getSelectedItem();
            if (null == partName) {
                String msg = "No partName selected.";
                Logger.getLogger(VisionToDBJPanel.class
                        .getName()).log(Level.WARNING, msg);
                warnDialog(msg);
                return;
            }
            PoseType poseFromDb = crclGenerator.getPose(partName);
            if (null == poseFromDb) {
                String msg = "Unable to get pose for " + partName + " from the database.";
                Logger.getLogger(VisionToDBJPanel.class
                        .getName()).log(Level.WARNING, msg);
                warnDialog(msg);
                return;
            }
            PointType poseFromDbPoint = requireNonNull(poseFromDb.getPoint(), "poseFromDb.getPoint()");
            String poseFromDbString
                    = poseFromDbPoint.getX()
                    + "," + poseFromDbPoint.getY();
            try (PrintWriter pw = new PrintWriter(new FileWriter(f, true))) {
                pw.println(System.currentTimeMillis() + ",FAIL," + partName + "," + poseFromDbString);
            }
        } catch (Exception ex) {
            Logger.getLogger(VisionToDBJPanel.class
                    .getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jButtonRecordFailActionPerformed

    private int recordSuccessCount = 0;

    @UIEffect
    private void jButtonRecordSuccessActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRecordSuccessActionPerformed
        recordSuccessCount++;
        jTextFieldRecordSuccessCount.setText(Integer.toString(recordSuccessCount));
        try {
            File f = new File(jTextFieldLogFilename.getText());
            addFailLogCsvHeader(f);
            String partName = manualObjectCachedComboBox.getSelectedItem();
            if (null == partName) {
                String msg = "No object selected.";
                Logger.getLogger(VisionToDBJPanel.class
                        .getName()).log(Level.WARNING, msg);
                warnDialog(msg);
                return;
            }
            PoseType poseFromDb = crclGenerator.getPose(partName);
            if (null == poseFromDb) {
                String msg = "Unable to get pose for " + partName + " from the database.";
                Logger.getLogger(VisionToDBJPanel.class
                        .getName()).log(Level.WARNING, msg);
                warnDialog(msg);
                return;
            }
            PointType poseFromDbPoint = requireNonNull(poseFromDb.getPoint(), "poseFromDb.getPoint()");
            String poseFromDbString
                    = poseFromDbPoint.getX()
                    + "," + poseFromDbPoint.getY();
            try (PrintWriter pw = new PrintWriter(new FileWriter(f, true))) {
                pw.println(System.currentTimeMillis() + ",SUCCESS," + partName + "," + poseFromDbString);
            }
        } catch (Exception ex) {
            Logger.getLogger(VisionToDBJPanel.class
                    .getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jButtonRecordSuccessActionPerformed

    @UIEffect
    private void jButtonGridTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGridTestActionPerformed
        this.startGridTest();
    }//GEN-LAST:event_jButtonGridTestActionPerformed

    @UIEffect
    private void jButtonAbortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAbortActionPerformed
        stepping = false;
        setSelectedManualObjectName();
        setReplanFromIndex(0);
        abortProgram();
    }//GEN-LAST:event_jButtonAbortActionPerformed

    @UIEffect
    private void jButtonGenerateAndRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGenerateAndRunActionPerformed
        try {
            stepping = false;
            setReplanFromIndex(0);
            autoStart = true;
            replanCachedCheckBox.setSelected(true);
            cancelRunProgramFuture();
            runningProgramFuture = generateCrclAsync();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            abortProgram();
        }
    }//GEN-LAST:event_jButtonGenerateAndRunActionPerformed

    @Nullable
    private volatile XFuture<Boolean> runningProgramFuture = null;

    @UIEffect
    private void jButtonStepActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonStepActionPerformed

        runSingleRow();
    }//GEN-LAST:event_jButtonStepActionPerformed

    private final AtomicInteger safeAboutCount = new AtomicInteger(0);
    private final AtomicInteger safeAbortRequestCount = new AtomicInteger(0);

    private void incSafeAbortCount() {
        final int count = safeAboutCount.incrementAndGet();
//        Utils.runOnDispatchThread(() -> jTextFieldSafeAbortCount.setText(Integer.toString(count)));
    }

    private void incSafeAbortRequestCount() {
        final int count = safeAbortRequestCount.incrementAndGet();
//        Utils.runOnDispatchThread(() -> jTextFieldSafeAbortRequestCount.setText(Integer.toString(count)));
    }

    public void debugAction() {
        long curTime = System.currentTimeMillis();
        logDebug("curTime = " + curTime);
        logDebug("runningProgram = " + runningProgram);
        logDebug("safeAbortRunnablesVector = " + safeAbortRunnablesVector);
        logDebug("startSafeAbortRunningProgram = " + startSafeAbortRunningProgram);
        logDebug("startSafeAbortRunningProgramFuture = " + startSafeAbortRunningProgramFuture);
        if (null != startSafeAbortRunningProgramFuture) {
            startSafeAbortRunningProgramFuture.printStatus();
        }
        logDebug("startSafeAbortRunningProgramFutureDone = " + startSafeAbortRunningProgramFutureDone);

        logDebug("lastCheckAbortCurrentPart = " + lastCheckAbortCurrentPart);
        logDebug("lastCheckAbortSafeAbortRequested = " + lastCheckAbortSafeAbortRequested);
        logDebug("lastCheckSafeAbortTime = " + lastCheckSafeAbortTime);
        logDebug("lastReplanAfterCrclBlock = " + lastReplanAfterCrclBlock);

//        private volatile String lastCheckAbortCurrentPart = null;
//    private volatile boolean lastCheckAbortSafeAbortRequested = false;
//    private volatile long lastCheckSafeAbortTime = 0;
        logDebug("continueActionsCount = " + continueActionsCount);
        logDebug("continueActionsListTime = " + continueActionsListTime);
        logDebug("clearAllCount = " + clearAllCount);
        logDebug("clearAllTime = " + clearAllTime);
        logDebug("startSafeAbortTime = " + startSafeAbortTime);
        logDebug("startCrclProgramTime = " + startCrclProgramTime);
        logDebug("startCrclProgramCount = " + startCrclProgramCount);
        logDebug("doSafeAbortCount = " + doSafeAbortCount);
        logDebug("doSafeAbortTime = " + doSafeAbortTime);
        logDebug("runningProgramFuture = " + runningProgramFuture);
        logDebug("runProgramCompleteRunnablesTime = " + runProgramCompleteRunnablesTime);
        logDebug("(curTime - lastCheckSafeAbortTime) = " + (curTime - lastCheckSafeAbortTime));
        logDebug("(curTime - doSafeAbortTime)        = " + (curTime - doSafeAbortTime));
        logDebug("(curTime - startCrclProgramTime)   = " + (curTime - startCrclProgramTime));
        logDebug("(curTime - startSafeAbortTime)     = " + (curTime - startSafeAbortTime));
        logDebug("(curTime - clearAllTime)           = " + (curTime - clearAllTime));
        logDebug("(curTime - runProgramCompleteRunnablesTime)           = " + (curTime - runProgramCompleteRunnablesTime));

        if (null != runningProgramFuture) {
            runningProgramFuture.printStatus(System.out);
        }

        logDebug("lastSafeAbortFuture=" + lastSafeAbortFuture);
        if (null != lastSafeAbortFuture) {
            lastSafeAbortFuture.printStatus(System.out);
        }
        logDebug("lastContinueActionFuture = " + lastContinueActionFuture);
        if (null != lastContinueActionFuture) {
            lastContinueActionFuture.printStatus(System.out);
        }

        logDebug("checkDbSupplierPublisherFuturesList = " + checkDbSupplierPublisherFuturesList);
    }

    private volatile boolean startSafeAbortRunningProgram = false;
    private volatile long startSafeAbortTime = 0;

    @Nullable
    private volatile XFutureVoid lastSafeAbortFuture = null;
    @Nullable
    private volatile XFuture<Boolean> startSafeAbortRunningProgramFuture = null;
    private volatile boolean startSafeAbortRunningProgramFutureDone = false;

    @Nullable
    private volatile CRCLProgramType startSafeAbortProgram = null;
    @Nullable
    private volatile String startSafeAbortProgramName = null;
    private volatile boolean startSafeAbortIsRunningCrclProgram = false;

    private void completeSafeAbortFuture(XFutureVoid f) {
        incSafeAbortCount();
        crclGenerator.takeSnapshots("", "completeSafeAbortFuture." + f, null, null);
        f.complete(null);
    }

    public XFutureVoid startSafeAbort(String name) {
        final int startSafeAbortRequestCount = safeAbortRequestCount.get();
        startSafeAbortTime = System.currentTimeMillis();
        synchronized (this) {
//            if (null != lastSafeAbortFuture && !lastSafeAbortFuture.isDone()) {
//                return lastSafeAbortFuture;
//            }

            startSafeAbortProgram = aprsSystem.getCrclProgram();
            if (null != startSafeAbortProgram) {
                startSafeAbortProgramName = startSafeAbortProgram.getName();
            }
            startSafeAbortIsRunningCrclProgram = aprsSystem.isRunningCrclProgram();
            startSafeAbortRunningProgram = runningProgram;
            startSafeAbortRunningProgramFuture = runningProgramFuture;
            startSafeAbortRunningProgramFutureDone
                    = startSafeAbortRunningProgramFuture != null
                    && startSafeAbortRunningProgramFuture.isDone();
            int runnablesSize = this.safeAbortRunnablesVector.size();
            if (runnablesSize > 0) {
                logDebug("safeAbortRunnablesVector.size() = " + runnablesSize);
            }
            incSafeAbortRequestCount();
            if (!startSafeAbortRunningProgram) {
                incSafeAbortCount();
                return XFutureVoid.completedFutureWithName("!startSafeAbortRunningProgram" + startSafeAbortRequestCount + ":" + safeAboutCount.get() + ":" + name + ":pddlExecutorStartSafeAbort." + aprsSystem.getRunName());
            }
            if (startSafeAbortRunningProgramFutureDone) {
                incSafeAbortCount();
                return XFutureVoid.completedFutureWithName("startSafeAbortRunningProgramFutureDone" + startSafeAbortRequestCount + ":" + safeAboutCount.get() + ":" + name + ":pddlExecutorStartSafeAbort." + aprsSystem.getRunName());
            }
            if (!startSafeAbortIsRunningCrclProgram) {
                incSafeAbortCount();
                return XFutureVoid.completedFutureWithName("!startSafeAbortIsRunningCrclProgram" + startSafeAbortRequestCount + ":" + safeAboutCount.get() + ":" + name + ":pddlExecutorStartSafeAbort." + aprsSystem.getRunName());
            }

            final XFutureVoid ret = new XFutureVoid(startSafeAbortRequestCount + ":" + safeAboutCount.get() + ":" + name + ":pddlExecutorStartSafeAbort." + aprsSystem.getRunName());

            this.safeAbortRunnablesVector.add(() -> completeSafeAbortFuture(ret));
            lastSafeAbortFuture = ret;
            return ret;
        }
    }

    @UIEffect
    private void jButtonContinueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonContinueActionPerformed
        this.aprsSystem.abortCrclProgram();
        int row = pddlOutputCachedTableModel.getSelectedRow();
        currentActionIndex = row;
        setReplanFromIndex(row);
        stepping = false;
        continueActionListPrivate();
    }//GEN-LAST:event_jButtonContinueActionPerformed

    @Nullable
    private volatile XFutureVoid lastContinueActionFuture = null;

    public int getSafeAbortRequestCount() {
        return safeAbortRequestCount.get();
    }
    
    public int getActionSetsStarted() {
        return actionSetsStarted.get();
    }

    public boolean completeActionList(String comment, int startSafeAbortRequestCount) {
        try {
            checkReverse();
            doingActionsStarted.incrementAndGet();
            autoStart = true;
            boolean ret = generateCrcl(comment, startSafeAbortRequestCount);
            if (ret && atLastAction()) {
                actionSetsCompleted.set(actionSetsStarted.get());
            }
            return ret;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            abortProgram();
            showExceptionInProgram(ex);
            throw new RuntimeException(ex);
        } finally {
            doingActionsFinished.incrementAndGet();
        }
    }

    public XFutureVoid continueActionList() {
        XFutureVoid ret = new XFutureVoid("pddlExecutorContinueActionList");
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
        checkReverse();
        continueActionsCount.incrementAndGet();
        continueActionsListTime = System.currentTimeMillis();
        autoStart = true;
        final int rpi = getReplanFromIndex();
        if (rpi < 0 || rpi >= actionsList.size()) {
            setReplanFromIndex(0);
        }
        replanCachedCheckBox.setSelected(true);
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
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "", ex);
            }
        }
    }

    private int placePartCount = 0;

    @UIEffect
    private void jButtonPlacePartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPlacePartActionPerformed
        try {
            setSelectedManualObjectName();
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
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            abortProgram();
            showExceptionInProgram(ex);
        }
    }//GEN-LAST:event_jButtonPlacePartActionPerformed

    @UIEffect
    private void jButtonTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonTestActionPerformed
        try {
            setSelectedManualObjectName();
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
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            abortProgram();
            showExceptionInProgram(ex);
        }
    }//GEN-LAST:event_jButtonTestActionPerformed

    @UIEffect
    private void jButtonNewLogFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonNewLogFileActionPerformed
        newLogFileName();
    }//GEN-LAST:event_jButtonNewLogFileActionPerformed

    @UIEffect
    private void jButtonRecordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRecordActionPerformed
        queryLogFileName();
        String partName = manualObjectCachedComboBox.getSelectedItem();
        if (null != partName && partName.length() > 0) {
            try {
                PoseType poseFromDb = crclGenerator.getPose(partName);
                if (null != poseFromDb) {
                    PointType poseFromDbPoint = requireNonNull(poseFromDb.getPoint(), "poseFromDb.getPoint()");
                    String poseFromDbString
                            = poseFromDbPoint.getX()
                            + "," + poseFromDbPoint.getY()
                            + "," + poseFromDbPoint.getZ();
                    logDebug("poseFromDbString = " + poseFromDbString);
                    PoseType curPose = requireNonNull(aprsSystem.getCurrentPose(), "aprsSystem.getCurrentPose()");
                    PointType curPosePoint = requireNonNull(curPose.getPoint(), "curPose.getPoint()");
                    String curPoseString
                            = String.format("%.1f, %.1f, %.1f",
                                    curPosePoint.getX(),
                                    curPosePoint.getY(),
                                    curPosePoint.getZ());
                    String offsetString
                            = (curPosePoint.getX() - poseFromDbPoint.getX())
                            + "," + (curPosePoint.getY() - poseFromDbPoint.getY())
                            + "," + (curPosePoint.getZ() - poseFromDbPoint.getZ());
                    logDebug("offsetString = " + offsetString);
                    writeCorrectionCsv(recordCsvName,
                            System.currentTimeMillis() + ", " + partName + ", " + curPoseString + ", " + poseFromDbString + ", " + offsetString);
                }
            } catch (SQLException | IOException ex) {
                LOGGER.log(Level.SEVERE, "", ex);
            }
        }
    }//GEN-LAST:event_jButtonRecordActionPerformed

    @UIEffect
    private void jButtonPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPauseActionPerformed
        pause();
    }//GEN-LAST:event_jButtonPauseActionPerformed

    private void pause() {
        aprsSystem.pauseCrclProgram();
    }

    public void showPaused(boolean paused) {
        Utils.runOnDispatchThread(() -> showPausedOnDisplay(paused));
    }

    @UIEffect
    private void showPausedOnDisplay(boolean paused) {
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
        if (null != stat) {
            JointStatusesType jointStatuses = stat.getJointStatuses();
            if (null != jointStatuses) {
                List<JointStatusType> jointList = jointStatuses.getJointStatus();
                String jointVals
                        = jointStatusListToString(jointList);
                logDebug("jointVals = " + jointVals);
                boolean keyFound = false;
                for (int i = 0; i < optionsCachedTable.getRowCount(); i++) {
                    if (Objects.equals("lookForJoints", optionsCachedTable.getValueAt(i, 0))) {
                        optionsCachedTable.setValueAt(jointVals, i, 1);
                        keyFound = true;
                    }
                }
                if (!keyFound) {
                    optionsCachedTable.addRow(new Object[]{"lookForJoints", jointVals});
                }
                crclGenerator.setOptions(getTableOptions());
            }
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

    @UIEffect
    private void jButtonRecordLookForJointsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRecordLookForJointsActionPerformed
        CRCLStatusType status = aprsSystem.getCurrentStatus();
        if (null != status) {
            this.updateLookForJoints(status);
        }
    }//GEN-LAST:event_jButtonRecordLookForJointsActionPerformed

    @UIEffect
    private void jButtonClearPoseCacheActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearPoseCacheActionPerformed
        clearPoseCache();
    }//GEN-LAST:event_jButtonClearPoseCacheActionPerformed

    @UIEffect
    private void jCheckBoxForceFakeTakeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxForceFakeTakeActionPerformed
        setForceFakeTakeFlag(jCheckBoxForceFakeTake.isSelected());
    }//GEN-LAST:event_jCheckBoxForceFakeTakeActionPerformed

    @Nullable
    private String toolChangerPoseMapFileName = null;

    private final CachedTable toolHolderPositionsCachedTable;

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
        crclGenerator.clearToolChangerJointVals();
        toolHolderPositionsCachedTable.setRowCount(0);
        Map<String, PoseType> toolHolderPoseMap
                = crclGenerator.getToolHolderPoseMap();
        try (CSVParser parser = new CSVParser(new FileReader(f), Utils.preferredCsvFormat())) {
            Map<String, Integer> headerMap = parser.getHeaderMap();
            if (null == headerMap) {
                throw new IllegalArgumentException(f.getCanonicalPath() + " does not have header");
            }
            List<CSVRecord> records = parser.getRecords();
            int skipRows = 0;
            for (CSVRecord rec : records) {
                String colName = toolHolderPositionsCachedTable.getColumnName(0);
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
            toolHolderPositionsCachedTable.setRowCount(records.size() - skipRows);
            ROW_LOOP:
            for (int i = skipRows; i < records.size(); i++) {
                CSVRecord rec = records.get(i);
                for (int j = 0; j < toolHolderPositionsCachedTable.getColumnCount(); j++) {
                    String colName = toolHolderPositionsCachedTable.getColumnName(j);
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
                            Class<?> colClass = toolHolderPositionsCachedTable.getColumnClass(j);
                            if (colClass == Double.class) {
                                toolHolderPositionsCachedTable.setValueAt(Double.valueOf(val), i - skipRows, j);
                            } else if (colClass == Boolean.class) {
                                toolHolderPositionsCachedTable.setValueAt(Boolean.valueOf(val), i - skipRows, j);
                            } else {
                                toolHolderPositionsCachedTable.setValueAt(val, i - skipRows, j);
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
                        crclGenerator.putToolChangerJointVals(name, jointVals);
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
            LOGGER.log(Level.SEVERE, "", ex);
        }
        clearEmptyToolChangerPoseRows();
    }

    private final CachedTable holderContentsCachedTable;

    private void loadHolderContentsMap() {
        if (null == propertiesFile || !propertiesFile.exists()) {
            setToolHolderContentsTableModelListener();
            return;
        }
        String filename = propertiesFile.getName() + TOOL_HOLDER_CONTENTS_CSV_EXTENSION;
        File f = new File(propertiesFile.getParent(), filename);
        if (!f.exists()) {
            setToolHolderContentsTableModelListener();
            return;
        }
        clearToolHolderContentsTableModelListener();
        int lineNumber = 0;
        readCsvFileToTable(holderContentsCachedTable, f);
        clearEmptyRows(holderContentsCachedTable);
        clearRedundantRows(holderContentsCachedTable);
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
        Map<String, PoseType> toolOffsetMap
                = crclGenerator.getToolOffsetMap();
        clearToolOffsetTableModelListener();
        int lineNumber = 0;
        readCsvFileToTableAndMap(toolOffsetsCachedTable, f, "ToolName", toolOffsetMap, ExecutorJPanel::recordToPose);
        clearEmptyToolOffsetPoseRows();
        loadToolOffsetsTableToMap();
        setToolOffsetTableModelListener();
    }

    private final CachedTable trayAttachOffsetsCachedTable;

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
        readCsvFileToTableAndMap(trayAttachOffsetsCachedTable, f, "TrayName", crclGenerator.getTrayAttachOffsetsMap(), ExecutorJPanel::recordToPose);
        loadTrayAttachOffsetsTableToMap();
        setTrayAttachOffsetTableModelListener();
    }

    public static PoseType recordToPose(CSVRecord rec) {
        PoseType pose = null;
        try {
            pose = CRCLPosemath.toPoseType(
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
            return pose;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
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

            Map<String, PoseType> toolHolderPoseMap
                    = crclGenerator.getToolHolderPoseMap();
            if (null == propertiesFile || !propertiesFile.exists()) {
                return;
            }
            toolChangerPoseMapFileName = propertiesFile.getName() + ".toolChangerPoses.csv";
            Utils.autoResizeTableColWidths(toolHolderPositionsCachedTable);
            File properitesParent = propertiesFile.getParentFile();
            if (null != properitesParent && null != toolChangerPoseMapFileName) {
                Utils.saveCachedTable(new File(propertiesFile.getParentFile(), toolChangerPoseMapFileName), toolHolderPositionsCachedTable);
            } else {
                throw new IllegalStateException("properitesParent=" + properitesParent + ", toolChangerPoseMapFileName=" + toolChangerPoseMapFileName);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "", ex);
        }
    }

    private void saveToolHolderContentsMap() {
        try {
            if (null == propertiesFile || !propertiesFile.exists() || holderContentsCachedTable.getRowCount() < 1) {
                return;
            }
            String fileName = propertiesFile.getName() + TOOL_HOLDER_CONTENTS_CSV_EXTENSION;
            Utils.saveCachedTable(new File(propertiesFile.getParentFile(), fileName), holderContentsCachedTable);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "", ex);
        }
    }

    private static final String TOOL_HOLDER_CONTENTS_CSV_EXTENSION = ".toolHolderContents.csv";

    private void saveToolOffsetPoseMap() {
        try {
            if (null == propertiesFile || !propertiesFile.exists()) {
                return;
            }
            String fileName = propertiesFile.getName() + ".toolOffsets.csv";
            Utils.saveCachedTable(new File(propertiesFile.getParentFile(), fileName), toolOffsetsCachedTable);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "", ex);
        }
    }

    private void saveTrayAttachOffsetPoseMap() {
        try {
            if (null == propertiesFile || !propertiesFile.exists()) {
                return;
            }
            String fileName = propertiesFile.getName() + ".trayAttachOffsets.csv";
            Utils.saveCachedTable(new File(propertiesFile.getParentFile(), fileName), trayAttachOffsetsCachedTable);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "", ex);
        }
    }

    @Nullable
    public String getSelectedToolChangerPoseName() {
        int r = toolHolderPositionsCachedTable.getSelectedRow();
        if (r < 0) {
            return null;
        }
        if (r >= toolHolderPositionsCachedTable.getRowCount()) {
            return null;
        }
        return (String) toolHolderPositionsCachedTable.getValueAt(r, 0);
    }

    private String[] getToolChangerNames() {
        Set<String> names = new TreeSet<>();
        names.add("");
        for (int i = 0; i < toolHolderPositionsCachedTable.getRowCount(); i++) {
            Object o = toolHolderPositionsCachedTable.getValueAt(i, 0);
            if (o instanceof String) {
                String s = (String) o;
                if (s.length() > 0) {
                    names.add(s);
                }
            }
        }
        return names.toArray(new String[0]);
    }

    private String[] getEmptyToolChangerNames() {
        Set<String> names = new TreeSet<>();
        names.add("");
        for (int i = 0; i < toolHolderPositionsCachedTable.getRowCount(); i++) {
            Object o = toolHolderPositionsCachedTable.getValueAt(i, 0);
            if (o instanceof String) {
                String s = (String) o;
                String expectedContents = crclGenerator.getExpectedToolHolderContentsMap().get(s);
                if (s.length() > 0 && CrclGenerator.isEmptyTool(expectedContents)) {
                    names.add(s);
                }
            }
        }
        return names.toArray(new String[0]);
    }

    private String[] getFullToolChangerNames() {
        Set<String> names = new TreeSet<>();
        names.add("");
        for (int i = 0; i < toolHolderPositionsCachedTable.getRowCount(); i++) {
            Object o = toolHolderPositionsCachedTable.getValueAt(i, 0);
            if (o instanceof String) {
                String s = (String) o;
                String expectedContents = crclGenerator.getExpectedToolHolderContentsMap().get(s);
                if (s.length() > 0 && !CrclGenerator.isEmptyTool(expectedContents)) {
                    names.add(s);
                }
            }
        }
        return names.toArray(new String[0]);
    }

    private String[] getToolNames() {
        Set<String> names = new TreeSet<>();
        names.add("");
        names.add("empty");
        for (int i = 0; i < toolOffsetsCachedTable.getRowCount(); i++) {
            Object o = toolOffsetsCachedTable.getValueAt(i, 0);
            if (o instanceof String) {
                String s = (String) o;
                if (s.length() > 0) {
                    names.add(s);
                }
            }
        }
        return names.toArray(new String[0]);
    }

    @UIEffect
    private String queryUserForToolHolderPosName(String qname) {
        return (String) JOptionPane.showInputDialog(
                this, // parentComponent
                "Tool Holder Pose Name?", // Object message
                aprsSystem.getTaskName() + " " + aprsSystem.getRobotName() + " " + qname + " choice", //  String title
                JOptionPane.QUESTION_MESSAGE, // messageType
                null,// icon 
                getToolChangerNames(), // selectionValues
                "" // initialSelectionValue
        );
    }

    @UIEffect
    private String queryUserForToolName(String qname) {
        return (String) JOptionPane.showInputDialog(
                this, // parentComponent
                "Tool Name?", // Object message
                aprsSystem.getTaskName() + " " + aprsSystem.getRobotName() + " " + qname + " choice", //  String title
                JOptionPane.QUESTION_MESSAGE, // messageType
                null,// icon 
                getToolNames(), // selectionValues
                "" // initialSelectionValue
        );
    }

    private int getToolChangerRow(String name, boolean approach) {
        for (int j = 0; j < toolHolderPositionsCachedTable.getColumnCount(); j++) {
            logDebug("j = " + j);
            String colName = toolHolderPositionsCachedTable.getColumnName(j);
            logDebug("colName = " + colName);
//            TableColumn col = toolHolderPositionsCachedTable.getColumn(colName);
//            logDebug("col = " + col);
        }
        for (int i = 0; i < toolHolderPositionsCachedTable.getRowCount(); i++) {
            String entryName = (String) toolHolderPositionsCachedTable.getValueAt(i, 0);
            Object entryApproachObject = toolHolderPositionsCachedTable.getValueAt(i, APPROACH_COLUMN_INDEX);
            if (null != entryName && null != entryApproachObject) {
                boolean entryApproach = (boolean) entryApproachObject;
                if (entryApproach == approach && entryName.equals(name)) {
                    return i;
                }
            } else {
                logDebug("Bad table entry: " + i + ", entryName=" + entryName + ", entryApproachObject=" + entryApproachObject);
            }
        }
        return -1;
    }

    private int getHolderContentsRow(String name) {
        for (int i = 0; i < holderContentsCachedTable.getRowCount(); i++) {
            String entryName = (String) holderContentsCachedTable.getValueAt(i, 0);
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
        CRCLStatusType stat = aprsSystem.getCurrentStatus();
        if (null != stat) {
            JointStatusesType jointStatuses = stat.getJointStatuses();
            if (null != jointStatuses) {
                List<JointStatusType> jointList = jointStatuses.getJointStatus();
                String jointVals
                        = jointStatusListToString(jointList);
                return jointVals;
            }
        }
        return null;
    }

    @UIEffect
    private void jButtonRecordToolHolderPoseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRecordToolHolderPoseActionPerformed
        try {
            Map<String, PoseType> toolHolderPoseMap
                    = crclGenerator.getToolHolderPoseMap();
            PoseType pose = aprsSystem.getCurrentPose();
            if (null == pose) {
                warnDialog("Can not read current pose.");
                return;
            }
            String toolHolderPoseName = queryUserForToolHolderPosName("Record Pose");
            if (null == toolHolderPoseName
                    || toolHolderPoseName.length() < 1) {
                toolHolderPoseName = "toolChangerPose" + (toolHolderPositionsCachedTable.getRowCount() + 1);
            }
//            toolChangerPose = pose;
            String name = toolHolderPoseName;
            PmRpy rpy = CRCLPosemath.toPmRpy(pose);
            String jointString = getJointValsString();
            updateToolChangePose(name, false, pose, rpy, jointString);
            toolHolderPoseMap.put(name, pose);
            PoseType approachPose = crclGenerator.approachPoseFromToolChangerPose(pose);
            updateToolChangePose(name, true, approachPose, rpy, null);
            crclGenerator.removeToolChangerJointVals(name);
            saveToolChangerPoseMap();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jButtonRecordToolHolderPoseActionPerformed

    private final CachedTable toolOffsetsCachedTable;

    private void clearEmptyToolOffsetPoseRows() {
        clearEmptyRows(toolOffsetsCachedTable);
    }

    private void clearEmptyToolChangerPoseRows() {
        clearEmptyRows(toolHolderPositionsCachedTable);
        Utils.autoResizeTableColWidths(toolHolderPositionsCachedTable);
    }

    private void clearEmptHolderContentsRows() {
        clearEmptyRows(holderContentsCachedTable);
        clearRedundantRows(holderContentsCachedTable);
    }

    private void clearEmptyRows(CachedTable cachedTable) {
        for (int i = 0; i < cachedTable.getRowCount(); i++) {
            Object val = cachedTable.getValueAt(i, 0);
            if (val == null) {
                cachedTable.removeRow(i);
                i--;
                continue;
            }
            String valString = val.toString();
            if (valString.length() < 1) {
                cachedTable.removeRow(i);
                i--;
            }
        }
    }

    private void clearRedundantRows(CachedTable cachedTable) {
        Set<String> valStringSet = new HashSet<>();
        for (int i = 0; i < cachedTable.getRowCount(); i++) {
            Object val = cachedTable.getValueAt(i, 0);
            if (val == null) {
                cachedTable.removeRow(i);
                i--;
                continue;
            }
            String valString = val.toString();
            if (valString.length() < 1) {
                cachedTable.removeRow(i);
                i--;
            }
            if (valStringSet.contains(valString)) {
                cachedTable.removeRow(i);
                i--;
            } else {
                valStringSet.add(valString);
            }
        }
    }

    private void updateToolChangePose(String name, boolean approach, PoseType pose, PmRpy rpy, @Nullable String jointString) {
        clearEmptyToolChangerPoseRows();
        if (name == null || name.length() < 1) {
            return;
        }
        int tableRowIndex = getToolChangerRow(name, approach);
        PointType posePoint = requireNonNull(pose.getPoint(), "pose.getPoint()");
        if (tableRowIndex < 0) {
            this.toolHolderPositionsCachedTable.addRow(new Object[]{
                name,
                posePoint.getX(),
                posePoint.getY(),
                posePoint.getZ(),
                Math.toDegrees(rpy.r),
                Math.toDegrees(rpy.p),
                Math.toDegrees(rpy.y),
                approach,
                jointString
            });
        } else {
            toolHolderPositionsCachedTable.setValueAt(posePoint.getX(), tableRowIndex, 1);
            toolHolderPositionsCachedTable.setValueAt(posePoint.getY(), tableRowIndex, 2);
            toolHolderPositionsCachedTable.setValueAt(posePoint.getZ(), tableRowIndex, 3);
            toolHolderPositionsCachedTable.setValueAt(Math.toDegrees(rpy.r), tableRowIndex, 4);
            toolHolderPositionsCachedTable.setValueAt(Math.toDegrees(rpy.p), tableRowIndex, 5);
            toolHolderPositionsCachedTable.setValueAt(Math.toDegrees(rpy.y), tableRowIndex, 6);
            toolHolderPositionsCachedTable.setValueAt(jointString, tableRowIndex, 8);
        }
        addHolderContentsRowIfNameNotFound(name);
        clearEmptyToolChangerPoseRows();
    }

    private void addHolderContentsRowIfNameNotFound(String name) {
        clearToolHolderContentsTableModelListener();
        int holderContentsTableRowIndex = getHolderContentsRow(name);
        if (holderContentsTableRowIndex < 0) {
            holderContentsCachedTable.addRow(new Object[]{
                name,
                "empty",
                ""
            });
        }
        setToolHolderContentsTableModelListener();
    }

    @UIEffect
    private void jButtonGotoToolChangerApproachActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGotoToolChangerApproachActionPerformed
        try {
            setSelectedManualObjectName();
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
            Map<String, PoseType> toolHolderPoseMap
                    = crclGenerator.getToolHolderPoseMap();
            PoseType pose = toolHolderPoseMap.get(name);
            if (null == pose) {
                warnDialog("no pose for " + name + " in " + toolHolderPoseMap);
                return;
            }
//            toolChangerPose = pose;
            runningProgramFuture = this.gotoToolChangerApproach(name, pose);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "", e);
            showExceptionInProgram(e);
        }
    }//GEN-LAST:event_jButtonGotoToolChangerApproachActionPerformed

    @UIEffect
    private void jButtonGotoToolChangerPoseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGotoToolChangerPoseActionPerformed
        try {
            setSelectedManualObjectName();
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
            Map<String, PoseType> toolHolderPoseMap
                    = crclGenerator.getToolHolderPoseMap();
//            toolChangerPoseName = name;
            PoseType pose = toolHolderPoseMap.get(name);
            if (null == pose) {
                warnDialog("no pose for " + name + " in " + toolHolderPoseMap);
                return;
            }
//            toolChangerPose = pose;
            runningProgramFuture = this.gotoToolChangerPose(name, pose);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "", e);
            showExceptionInProgram(e);
        }
    }//GEN-LAST:event_jButtonGotoToolChangerPoseActionPerformed

    @UIEffect
    private void jButtonDropToolActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDropToolActionPerformed
        try {
            setSelectedManualObjectName();
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
            syncPanelToGeneratorToolData();
            Map<String, PoseType> toolHolderPoseMap
                    = crclGenerator.getToolHolderPoseMap();
            PoseType toolHolderPose = toolHolderPoseMap.get(toolHolderPoseName);
            if (null == toolHolderPose) {
                warnDialog("No pose known for " + toolHolderPoseName + " in " + toolHolderPoseMap);
                return;
            }

            runningProgramFuture = this.dropToolByHolder(toolHolderPoseName);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "", e);
            showExceptionInProgram(e);
        }
    }//GEN-LAST:event_jButtonDropToolActionPerformed

    private void syncPanelToGeneratorToolData() {
        Utils.runOnDispatchThread(this::syncPanelToGeneratorToolDataOnDisplay);
    }

    @UIEffect
    private void syncPanelToGeneratorToolDataOnDisplay() {
        String toolName = jTextFieldCurrentToolName.getText();
        if (null == toolName || toolName.length() < 1) {
            warnDialog("Invalid toolName =" + toolName);
            throw new IllegalStateException("Invalid toolName =" + toolName);
        }
        crclGenerator.setCurrentToolName(toolName);
        crclGenerator.setExpectedToolName(toolName);
        for (int i = 0; i < holderContentsCachedTable.getRowCount(); i++) {
            String holderName = (String) holderContentsCachedTable.getValueAt(i, 0);
            if (holderName == null || holderName.length() < 1) {
                continue;
            }
            String toolForHolder = (String) holderContentsCachedTable.getValueAt(i, 1);
            if (toolForHolder == null || toolForHolder.length() < 1) {
                toolForHolder = "empty";
            }
            String toolsListString = (String) holderContentsCachedTable.getValueAt(i, 2);
            crclGenerator.getCurrentToolHolderContentsMap().put(holderName, toolForHolder);
            crclGenerator.getExpectedToolHolderContentsMap().put(holderName, toolForHolder);
            if (null != toolsListString) {
                String toolsArray[] = toolsListString.split("[ ,\t\r\n]+");
                Set<String> toolsSet = new TreeSet<>(Arrays.asList(toolsArray));
                crclGenerator.getPossibleToolHolderContentsMap().put(holderName, toolsSet);
            }
        }
    }

    @UIEffect
    private void jButtonPickupToolActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPickupToolActionPerformed
        try {
            if (null != currentPart) {
                manualObjectCachedComboBox.setSelectedItem(currentPart);
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
            syncPanelToGeneratorToolData();
//            String newToolName = queryUserForToolName("What tool will be in the robot?");
//            if (null == newToolName || newToolName.length() < 1) {
//                return;
//            }
//            toolChangerPoseName = holderPosName;
            Map<String, PoseType> toolHolderPoseMap
                    = crclGenerator.getToolHolderPoseMap();
            PoseType pose = toolHolderPoseMap.get(holderPosName);
            if (null == pose) {
                warnDialog("no pose for " + holderPosName + " in " + toolHolderPoseMap);
                return;
            }
            runningProgramFuture = this.pickupToolByHolder(holderPosName);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "", e);
            showExceptionInProgram(e);
        }
    }//GEN-LAST:event_jButtonPickupToolActionPerformed

    @UIEffect
    private void jTextFieldToolChangerApproachZOffsetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldToolChangerApproachZOffsetActionPerformed
        crclGenerator.setApproachToolChangerZOffset(Double.parseDouble(jTextFieldToolChangerApproachZOffset.getText()));
    }//GEN-LAST:event_jTextFieldToolChangerApproachZOffsetActionPerformed

    @UIEffect
    private void jButtonDeleteToolHolderPoseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeleteToolHolderPoseActionPerformed
        clearEmptyToolChangerPoseRows();
        clearEmptHolderContentsRows();
        String nameToDelete = queryUserForToolHolderPosName("Holder name of pose to delete");
        deleteFromToolHolderPositionsTable(nameToDelete);
        deleteFromToolHolderContentsTable(nameToDelete);
        Utils.autoResizeTableColWidths(toolHolderPositionsCachedTable);
        Utils.autoResizeTableColWidths(holderContentsCachedTable);
        syncPanelToGeneratorToolData();
        saveToolChangerPoseMap();
        saveToolHolderContentsMap();
    }//GEN-LAST:event_jButtonDeleteToolHolderPoseActionPerformed

    private void deleteFromToolHolderPositionsTable(String nameToDelete) {
        deleteMatchingRowsFromTable(toolHolderPositionsCachedTable, nameToDelete);
        Map<String, PoseType> toolHolderPoseMap
                = crclGenerator.getToolHolderPoseMap();
        toolHolderPoseMap.remove(nameToDelete);
    }

    private void renameFromToolHolderPositionsTable(String oldName, String newName) {
        Map<String, PoseType> toolHolderPoseMap
                = crclGenerator.getToolHolderPoseMap();
        PoseType pose = toolHolderPoseMap.get(oldName);
        renameMatchingRowsFromTable(toolHolderPositionsCachedTable, oldName, newName);
        if (null != pose) {
            toolHolderPoseMap.put(newName, pose);
        }
        toolHolderPoseMap.remove(oldName);
    }

    private void deleteMatchingRowsFromTable(CachedTable cachedTable, String nameToDelete) {
        for (int i = 0; i < cachedTable.getRowCount(); i++) {
            String nameFromTable = (String) cachedTable.getValueAt(i, 0);
            if (null == nameFromTable || nameFromTable.equals(nameToDelete)) {
                cachedTable.removeRow(i);
                i--;
            }
        }
        clearEmptyRows(cachedTable);
        Utils.autoResizeTableColWidths(cachedTable);
    }

    private void renameMatchingRowsFromTable(CachedTable cachedTable, String oldName, String newName) {
        for (int i = 0; i < cachedTable.getRowCount(); i++) {
            String nameFromTable = (String) cachedTable.getValueAt(i, 0);
            if (null != nameFromTable && nameFromTable.equals(oldName)) {
                cachedTable.setValueAt(newName, i, 0);
                i--;
            }
        }
        clearEmptyRows(cachedTable);
        Utils.autoResizeTableColWidths(cachedTable);
    }

    private void renameFromToolHolderContentsTable(String oldName, String newName) {
        Map<String, String> expectedToolHolderContentsMap
                = crclGenerator.getExpectedToolHolderContentsMap();
        String oldExpectedContents = expectedToolHolderContentsMap.get(oldName);
        Map<String, String> currentToolHolderContentsMap
                = crclGenerator.getCurrentToolHolderContentsMap();
        String oldCurrentContents = currentToolHolderContentsMap.get(oldName);
        renameMatchingRowsFromTable(holderContentsCachedTable, oldName, newName);
        if (null != oldExpectedContents) {
            expectedToolHolderContentsMap.put(newName, oldExpectedContents);
        }
        if (null != oldCurrentContents) {
            currentToolHolderContentsMap.put(newName, oldCurrentContents);
        }
        expectedToolHolderContentsMap.remove(oldName);
        currentToolHolderContentsMap.remove(oldName);
    }

    private void deleteFromToolHolderContentsTable(String nameToDelete) {
        deleteMatchingRowsFromTable(holderContentsCachedTable, nameToDelete);
        Map<String, String> expectedToolHolderContentsMap
                = crclGenerator.getExpectedToolHolderContentsMap();
        expectedToolHolderContentsMap.remove(nameToDelete);
        Map<String, String> currentToolHolderContentsMap
                = crclGenerator.getCurrentToolHolderContentsMap();
        currentToolHolderContentsMap.remove(nameToDelete);
    }

    @UIEffect
    private void jButtonAddToolHolderPoseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddToolHolderPoseActionPerformed
        try {
            clearEmptyToolChangerPoseRows();
            PoseType pose = aprsSystem.getCurrentPose();
            if (null == pose || null == pose.getPoint()) {
                warnDialog("Can not read current pose.");
                return;
            }
            String nameToAdd = JOptionPane.showInputDialog("New tool changer position name");
            Map<String, PoseType> toolHolderPoseMap
                    = crclGenerator.getToolHolderPoseMap();
            if (nameToAdd != null && nameToAdd.length() > 0) {
                if (toolHolderPoseMap.containsKey(nameToAdd) || Arrays.stream(getToolChangerNames()).anyMatch(nameToAdd::equals)) {
                    warnDialog(nameToAdd + " already added.");
                    return;
                }
                PmRpy rpy = CRCLPosemath.toPmRpy(pose);
                String jointString = getJointValsString();
                updateToolChangePose(nameToAdd, false, pose, rpy, jointString);
                toolHolderPoseMap.put(nameToAdd, pose);
                PoseType approachPose = crclGenerator.approachPoseFromToolChangerPose(pose);
                updateToolChangePose(nameToAdd, true, approachPose, rpy, null);
                clearEmptyToolChangerPoseRows();
                Utils.autoResizeTableColWidths(toolHolderPositionsCachedTable);
                saveToolChangerPoseMap();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jButtonAddToolHolderPoseActionPerformed

    @UIEffect
    private void jButtonRecordToolHolderApproachActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRecordToolHolderApproachActionPerformed
        try {
            PoseType pose = aprsSystem.getCurrentPose();
            if (null == pose) {
                warnDialog("Can not read current pose.");
                return;
            }
            String toolHolderPoseName = queryUserForToolHolderPosName("Record Approach");
            if (null == toolHolderPoseName
                    || toolHolderPoseName.length() < 1) {
                toolHolderPoseName = "toolChangerPose" + (toolHolderPositionsCachedTable.getRowCount() + 1);
            }

            PmRpy rpy = CRCLPosemath.toPmRpy(pose);
            String jointString = getJointValsString();
            updateToolChangePose(toolHolderPoseName, true, pose, rpy, jointString);
            if (null != jointString) {
                crclGenerator.putToolChangerJointVals(toolHolderPoseName, jointString);
            }
            saveToolChangerPoseMap();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jButtonRecordToolHolderApproachActionPerformed

    private static double getDoubleValueAt(CachedTable cachedTable, int row, int col) {
        Object o = cachedTable.getValueAt(row, col);
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
            Map<String, PoseType> map = crclGenerator.getTrayAttachOffsetsMap();
            map.clear();
            for (int i = 0; i < trayAttachOffsetsCachedTable.getRowCount(); i++) {
                Object v0 = trayAttachOffsetsCachedTable.getValueAt(i, 0);
                if (v0 instanceof String) {
                    String name = (String) v0;
                    name = name.trim();
                    if (name.length() < 1) {
                        continue;
                    }
                    double x = getDoubleValueAt(trayAttachOffsetsCachedTable, i, 1);
                    double y = getDoubleValueAt(trayAttachOffsetsCachedTable, i, 2);
                    double z = getDoubleValueAt(trayAttachOffsetsCachedTable, i, 3);
                    double roll = getDoubleValueAt(trayAttachOffsetsCachedTable, i, 4);
                    roll = Math.toRadians(roll);
                    double pitch = getDoubleValueAt(trayAttachOffsetsCachedTable, i, 5);
                    pitch = Math.toRadians(pitch);
                    double yaw = getDoubleValueAt(trayAttachOffsetsCachedTable, i, 6);
                    yaw = Math.toRadians(yaw);
                    PoseType pose = CRCLPosemath.toPoseType(new PmCartesian(x, y, z), new PmRpy(roll, pitch, yaw));
                    map.put(name, pose);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "", e);
        }
    }

    private void loadToolOffsetsTableToMap() {
        try {
            Map<String, PoseType> map = crclGenerator.getToolOffsetMap();
            map.clear();
            for (int i = 0; i < toolOffsetsCachedTable.getRowCount(); i++) {
                Object v0 = toolOffsetsCachedTable.getValueAt(i, 0);
                if (v0 instanceof String) {
                    String name = (String) v0;
                    name = name.trim();
                    if (name.length() < 1) {
                        continue;
                    }
                    double x = getDoubleValueAt(toolOffsetsCachedTable, i, 1);
                    double y = getDoubleValueAt(toolOffsetsCachedTable, i, 2);
                    double z = getDoubleValueAt(toolOffsetsCachedTable, i, 3);
                    double roll = getDoubleValueAt(toolOffsetsCachedTable, i, 4);
                    roll = Math.toRadians(roll);
                    double pitch = getDoubleValueAt(toolOffsetsCachedTable, i, 5);
                    pitch = Math.toRadians(pitch);
                    double yaw = getDoubleValueAt(toolOffsetsCachedTable, i, 6);
                    yaw = Math.toRadians(yaw);
                    PoseType pose = CRCLPosemath.toPoseType(new PmCartesian(x, y, z), new PmRpy(roll, pitch, yaw));
                    map.put(name, pose);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "", e);
        }
    }

    @UIEffect
    private void jButtonAddToolOffsetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddToolOffsetActionPerformed
        DefaultTableModel dtm = (DefaultTableModel) jTableToolOffsets.getModel();
        dtm.addRow(new Object[]{"tool" + (dtm.getRowCount() + 1), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, ""});
        clearToolOffsetTableModelListener();
        Utils.autoResizeTableColWidths(jTableToolOffsets);
        saveToolOffsetPoseMap();
        loadToolOffsetsTableToMap();
        setToolOffsetTableModelListener();
    }//GEN-LAST:event_jButtonAddToolOffsetActionPerformed

    @UIEffect
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

    @UIEffect
    private void jButtonUpdatePoseCacheActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonUpdatePoseCacheActionPerformed
        try {
            crclGenerator.clearPoseCache();
            List<PhysicalItem> newItems = crclGenerator.newPoseItems("userRequestedPoseUpdate");
            manualObjectCachedComboBox.removeAllElements();
            manualSlotCachedComboBox.removeAllElements();
            newItems = new ArrayList<>(newItems);
            newItems.sort(Comparators.fromFunctions(PhysicalItem::getFullName));
            for (PhysicalItem item : newItems) {
                String fullName = item.getFullName();
                if (null != fullName) {
                    switch (item.getType()) {
                        case "P":
                        case "KT":
                        case "PT":
                            manualObjectCachedComboBox.addElement(fullName);
                            break;

                        case "ES":
                        case "SLOT":
                            manualSlotCachedComboBox.addElement(fullName);
                            break;
                    }
                }
            }
            updatePositionCacheTable();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jButtonUpdatePoseCacheActionPerformed

    @UIEffect
    private void jButtonSetCurrentToolActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSetCurrentToolActionPerformed
        try {
            String newToolName = queryUserForToolName("Which tool is currently in the robot? ");
            if (null != newToolName && newToolName.length() > 0) {
                crclGenerator.setCurrentToolName(newToolName);
                syncPanelToGeneratorToolData();
//                jTextFieldCurrentToolName.setText(newToolName);
//                PoseType newPose = crclGenerator.getToolOffsetMap().get(newToolName);
//                if (null != newPose) {
//                    PmRpy rpy = CRCLPosemath.toPmRpy(newPose);
//                    PmCartesian tran = CRCLPosemath.toPmCartesian(newPose.getPoint());
//                    String offsetText
//                            = String.format("X=%.3f,Y=%.3f,Z=%.3f,roll=%.3f,pitch=%.3f,yaw=%.3f",
//                                    tran.x, tran.y, tran.z,
//                                    Math.toDegrees(rpy.r), Math.toDegrees(rpy.p), Math.toDegrees(rpy.y));
//                    jTextFieldCurrentToolOffset.setText(offsetText);
//                    crclGenerator.setToolOffsetPose(newPose);
//                }
            }
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "", exception);
        }
    }//GEN-LAST:event_jButtonSetCurrentToolActionPerformed

    @UIEffect
    private void jButtonAddTrayAttachActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddTrayAttachActionPerformed
        DefaultTableModel dtm = (DefaultTableModel) jTableTrayAttachOffsets.getModel();
        dtm.addRow(new Object[]{"tray" + (dtm.getRowCount() + 1), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, ""});
        clearTrayAttachOffsetTableModelListener();
        Utils.autoResizeTableColWidths(jTableToolOffsets);
        saveTrayAttachOffsetPoseMap();
        loadTrayAttachOffsetsTableToMap();
        setTrayAttachOffsetTableModelListener();
    }//GEN-LAST:event_jButtonAddTrayAttachActionPerformed

    @UIEffect
    private void jButtonRenameToolHolderPoseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRenameToolHolderPoseActionPerformed
        clearEmptyToolChangerPoseRows();
        clearEmptHolderContentsRows();
        String oldName = queryUserForToolHolderPosName("old holder name to rename");
        String newName = JOptionPane.showInputDialog(
                this, // parentComponent
                "Tool Holder Pose Name?", // Object message
                aprsSystem.getTaskName() + " " + aprsSystem.getRobotName() + " new holder name for " + oldName, //  String title
                JOptionPane.QUESTION_MESSAGE // messageType
        );
        renameFromToolHolderPositionsTable(oldName, newName);
        renameFromToolHolderContentsTable(oldName, newName);
        Utils.autoResizeTableColWidths(toolHolderPositionsCachedTable);
        Utils.autoResizeTableColWidths(holderContentsCachedTable);
        syncPanelToGeneratorToolData();
        saveToolChangerPoseMap();
        saveToolHolderContentsMap();
    }//GEN-LAST:event_jButtonRenameToolHolderPoseActionPerformed

    private void clearPoseCache() {
        crclGenerator.clearPoseCache();
        updatePositionCacheTable();
    }

    @UIEffect
    private void queryLogFileName() {
        if (!new File(recordCsvName).exists()) {
            newLogFileName();
        }
    }

    @UIEffect
    private void newLogFileName() throws HeadlessException {
        recordCsvName = jTextFieldLogFilename.getText();
        JFileChooser chooser = new JFileChooser();
        if (null != propertiesFile) {
            File parentFile = propertiesFile.getParentFile();
            if (null != parentFile) {
                chooser.setCurrentDirectory(parentFile);
            }
        }
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = chooser.getSelectedFile();
                recordCsvName = f.getCanonicalPath();
                if (!recordCsvName.endsWith(".csv")) {
                    recordCsvName += ".csv";
                }
                jTextFieldLogFilename.setText(recordCsvName);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "", ex);
            }
        }
    }

    private int @Nullable [] crclIndexes = null;

    private void setCrclIndexes(int indexes[]) {
        for (int i = 0; i < indexes.length; i++) {
            if (i >= pddlOutputCachedTableModel.getRowCount()) {
                break;
            }
            if (!Objects.equals(pddlOutputCachedTableModel.getValueAt(i, 1), indexes[i])) {
                pddlOutputCachedTableModel.setValueAt(indexes[i], i, 1);
            }
        }
        this.crclIndexes = indexes;
    }

    private void setPddlLabelss(String labels[]) {
        for (int i = 0; i < labels.length; i++) {
            if (i >= pddlOutputCachedTableModel.getRowCount()) {
                break;
            }
            if (null != labels[i]) {
                if (!Objects.equals(pddlOutputCachedTableModel.getValueAt(i, 2), labels[i])) {
                    pddlOutputCachedTableModel.setValueAt(labels[i], i, 2);
                }
            }
        }
    }

    private void setPddlTakenParts(@Nullable String parts[]) {
        for (int i = 0; i < parts.length; i++) {
            if (i >= pddlOutputCachedTableModel.getRowCount()) {
                break;
            }
            if (null != parts[i]) {
                if (!Objects.equals(pddlOutputCachedTableModel.getValueAt(i, 6), parts[i])) {
                    pddlOutputCachedTableModel.setValueAt(parts[i], i, 6);
                }
            }
        }
    }

    private void reloadPddlActions(List<Action> l) {
        for (int i = 0; i < l.size(); i++) {
            if (i >= pddlOutputCachedTableModel.getRowCount()) {
                break;
            }
            Action act = l.get(i);
            if (null != act) {
                if (!Objects.equals(pddlOutputCachedTableModel.getValueAt(i, 3), act.getType())) {
                    pddlOutputCachedTableModel.setValueAt(act.getType(), i, 3);
                }
                String argsString = Arrays.toString(act.getArgs());
                if (!Objects.equals(pddlOutputCachedTableModel.getValueAt(i, 4), argsString)) {
                    pddlOutputCachedTableModel.setValueAt(argsString, i, 4);
                }
            }
        }
    }

    private boolean started = false;

    private final ConcurrentLinkedDeque<Runnable> safeAbortRunnablesVector = new ConcurrentLinkedDeque<>();

    private void generateCrclAsyncWithCatch() {
        try {
            if (null != runningProgramFuture) {
                runningProgramFuture.cancel(true);
            }
            runningProgramFuture = generateCrclAsync();
        } catch (Exception ex) {
            replanStarted.set(false);
            abortProgram();
            showExceptionInProgram(ex);
//            actionToCrclLabels[lastIndex] = "Error";
            LOGGER.log(Level.SEVERE, "", ex);
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
            crclGenerator.reset();
        }
        this.errorString = errorString;
    }

    private XFutureVoid showExceptionInProgram(final java.lang.Exception ex) {
        return Utils.runOnDispatchThread(() -> showExceptionInProgramInternalOnDisplay(ex));
    }

    @UIEffect
    private void showExceptionInProgramInternalOnDisplay(final java.lang.Exception ex) {
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
        if (null != aprsSystem) {
            aprsSystem.setTitleErrorString(errorString);
        }
    }

    @Nullable
    private String crclProgName = null;
    @Nullable
    private String lastCrclProgName = null;

    private final List<Runnable> programCompleteRunnablesList = new ArrayList<>();

    private void addProgramCompleteRunnable(Runnable r) {
        synchronized (programCompleteRunnablesList) {
            programCompleteRunnablesList.add(r);
        }
    }

    public void removeProgramCompleteRunnable(Runnable r) {
        synchronized (programCompleteRunnablesList) {
            programCompleteRunnablesList.remove(r);
        }
    }

    /*@SafeEffect
    private XFuture<Boolean> recursiveSupplyBoolean(boolean prevSuccess,
                                                    UiSupplier<XFuture<Boolean>> supplier) {
        if (prevSuccess) {
            try {
                return Utils.composeOnDispatchThread(supplier);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "", ex);
                XFuture<Boolean> ret = new XFuture<>("recursiveSupplyBoolean");
                ret.completeExceptionally(ex);
                return ret;
            }
        } else {
            return XFuture.completedFuture(false);
        }
    }*/
    public static XFuture<Boolean> ifOk(boolean ok, Supplier<XFuture<Boolean>> thenSupplier) {
        return ok ? thenSupplier.get() : XFuture.completedFuture(false);
    }

    private XFuture<Boolean> recursiveApplyGenerateCrcl(boolean prevSuccess) {
        checkReverse();
        if (prevSuccess) {
            try {
                return generateCrclAsync();
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "", ex);
                XFuture<Boolean> ret = new XFuture<>("recursiveApplyGenerateCrclException");
                ret.completeExceptionally(ex);
                return ret;
            }
        } else {
            return XFuture.completedFuture(false);
        }
    }

    private int crclStartActionIndex = -1;
    private int crclEndActionIndex = -1;

    private void takeSimViewSnapshot(File f, @Nullable PoseType pose, @Nullable String label) {
        if (null != aprsSystem) {
            aprsSystem.takeSimViewSnapshot(f, pose, label);
        }
    }

    private volatile long doSafeAbortTime = 0;
    private final AtomicInteger doSafeAbortCount = new AtomicInteger(0);
    @Nullable
    private volatile String lastCheckAbortCurrentPart = null;
    private volatile boolean lastCheckAbortSafeAbortRequested = false;
    private volatile long lastCheckSafeAbortTime = 0;

    private XFuture<Boolean> checkSafeAbortAsync(Supplier<XFuture<Boolean>> supplier, int startSafeAbortRequestCount) {

        if (aprsSystem.isRunningCrclProgram()) {
            logDebug("crclProgramStill Running");
            logDebug("aprsSystemInterface.isRunningCrclProgram() = " + aprsSystem.isRunningCrclProgram());
            logDebug("aprsSystemInterface.getCrclRunProgramFuture() = " + aprsSystem.getCrclRunProgramFuture());
            logDebug("aprsSystemInterface.getCrclRunProgramThread() = " + aprsSystem.getCrclRunProgramThread());
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
            if (safeAbortRequested != aprsSystem.isAborting()) {
                logDebug("safeAbortRequested=" + safeAbortRequested + ", aprsSystemInterface.isAborting()=" + aprsSystem.isAborting());
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
                if (crclGenerator.isTakeSnapshots() && aprsSystem.snapshotsEnabled()) {
                    takeSimViewSnapshot(aprsSystem.createTempFile("-safe-abort-", ".PNG"), null, "");
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
            throws IllegalStateException, SQLException, JAXBException, InterruptedException, ExecutionException, PendantClientInner.ConcurrentBlockProgramsException, CRCLException, PmException {

        checkReverse();
        boolean doSafeAbort = checkSafeAbort(startSafeAbortRequestCount);
        if (doSafeAbort) {
            return atLastAction();
        }
        checkDbSupplierPublisher();
        int abortReplanFromIndex = getReplanFromIndex();
        if (abortReplanFromIndex == 0) {
            Utils.runOnDispatchThread(this::clearOpDisplay);
        }
        List<Integer> lil = new ArrayList<>();
        final int li0 = crclGenerator.getLastIndex();
        lil.add(li0);
        final int rpi0 = getReplanFromIndex();
//        if (rpi0 < li0 && (rpi0 != 0 || !readyForNewActionsList())) {
//            logDebug("replanFromIndex less than lastIndex unexpectedly,  li0=" + li0 + ",rpi0=" + rpi0);
//        }
        CRCLProgramType program = pddlActionSectionToCrcl(0);
        final int li1 = crclGenerator.getLastIndex();
        lil.add(li1);
        if (li1 < li0 && li1 < rpi0) {
            logDebug("lastIndex decreased: li0=" + li0 + ",li1=" + li1);
        }
        final int rpi1 = getReplanFromIndex();
        if (rpi1 <= li1 && rpi1 != actionsList.size() - 1) {
            logDebug("replanFromIndex <= lastIndex: replanFromIndex=" + rpi1 + ",li1=" + li1);
        }
        if (!autoStart) {
            setCrclProgram(crclProgram);
            return true;
        }
        boolean replanAfterCrclBlock
                = (!crclGenerator.atLastIndex())
                && replanCachedCheckBox.isSelected();
        lastReplanAfterCrclBlock = replanAfterCrclBlock;
        int sectionNumber = 1;
        while (replanAfterCrclBlock && autoStart) {
            final int rpi2 = getReplanFromIndex();
            doSafeAbort = checkSafeAbort(startSafeAbortRequestCount);
            if (rpi2 != getReplanFromIndex()) {
                logDebug("replanFromIndex changed unexpectedly : replanFromIndex=" + replanFromIndex + ",rpi1=" + rpi2);
            }
            if (doSafeAbort) {
                setReplanFromIndex(abortReplanFromIndex, true);
                return atLastAction();
            }
            boolean emptyProgram = program.getMiddleCommand().isEmpty();
            boolean nextReplanAfterCrclBlock
                    = crclGenerator.getLastIndex() < actionsList.size() - 1
                    && replanCachedCheckBox.isSelected();
            if (emptyProgram) {
                if (!nextReplanAfterCrclBlock) {
                    break;
                } else {
                    logDebug("pddlActionToCrclGenerator.getLastIndex() = " + crclGenerator.getLastIndex());
                    logDebug("actionsList = " + actionsList);
                    logDebug("CRCL Program was empty but actions not complete.");
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
                logDebug("replanFromIndex changed unexpectedly : replanFromIndex=" + replanFromIndex + ",rpi1=" + rpi2);
            }
            abortReplanFromIndex = getReplanFromIndex();
            final int li3 = crclGenerator.getLastIndex();
            program = pddlActionSectionToCrcl(sectionNumber++);
            final int li4 = crclGenerator.getLastIndex();
            lil.add(li4);
            if (li4 < li3) {
                logDebug("lastIndex decreased: li3=" + li3 + ",li4=" + li4);
            }
            final int rpi3 = getReplanFromIndex();
            if (rpi3 <= li4 && rpi3 != actionsList.size() - 1) {
                logDebug("replanFromIndex <= lastIndex: replanFromIndex=" + rpi3 + ",li4=" + li4);
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

    @UIEffect
    private void clearOpDisplay() {
        if (enableOptaplannerCachedCheckBox.isSelected()) {
            this.opDisplayJPanelInput.setOpActionPlan(null);
            this.opDisplayJPanelSolution.setOpActionPlan(null);
            this.opDisplayJPanelInput.setLabel("Input");
            this.opDisplayJPanelSolution.setLabel("Output");
        }
    }

    public boolean atLastAction() {
        boolean ret = crclGenerator.atLastIndex();
//        if (ret) {
//            logDebug("crclGenerator.getLastIndex() = " + crclGenerator.getLastIndex());
//            logDebug("actionsList.size() = " + actionsList.size());
//        }
        return ret;
    }

    private XFuture<Boolean> generateCrclAsync() throws IllegalStateException {

        checkReverse();
        int startSafeAbortRequestCount = safeAbortRequestCount.get();

        ExecutorService service = this.generateCrclService;
        if (null == service) {
            service = aprsSystem.getRunProgramService();
            this.generateCrclService = service;
        }
        ExecutorService genCrclService = service;
        String taskName = "generateCrcl(" + aprsSystem.getTaskName() + ").doPddlActionsSection(" + crclGenerator.getLastIndex() + " out of " + actionsList.size() + ")";

        return checkSafeAbortAsync(() -> {
            try {
                return checkDbSupplierPublisherAsync()
                        .thenComposeAsync(taskName,
                                x -> doPddlActionsSectionAsync(startSafeAbortRequestCount, 0),
                                genCrclService);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "", ex);
                XFuture<Boolean> xf = new XFuture<>("generateCrclException");
                xf.completeExceptionally(ex);
                return xf;
            }
        }, startSafeAbortRequestCount
        );
    }

    private final CachedCheckBox enableOptaplannerCachedCheckBox;

    private CRCLProgramType pddlActionSectionToCrcl(int sectionNumber) throws IllegalStateException, SQLException, InterruptedException, ExecutionException, PendantClientInner.ConcurrentBlockProgramsException, CRCLException, PmException {
        Map<String, String> options = getTableOptions();
        final int rpi = getReplanFromIndex();
        if (rpi < 0 || rpi > actionsList.size()) {
            setReplanFromIndex(0);
        }
        syncCrclGeneratorPositionMaps();
        if (enableOptaplannerCachedCheckBox.isSelected()) {
            if (null == solver) {
                synchronized (solverFactory) {
                    solver = solverFactory.buildSolver();
                }
            }
            crclGenerator.setSolver(solver);
            crclGenerator.setOpDisplayJPanelInput(opDisplayJPanelInput);
            crclGenerator.setOpDisplayJPanelSolution(opDisplayJPanelSolution);
        } else {
            crclGenerator.setSolver(null);
        }
        crclStartActionIndex = this.getReplanFromIndex();
        currentActionIndex = crclStartActionIndex;
        if (null != aprsSystem) {
            aprsSystem.updateTitle();
        }
        CRCLProgramType program = createEmptyProgram();
        List<MiddleCommandType> cmds;
        final int startReplanFromIndex = this.getReplanFromIndex();
        checkReverse();
        synchronized (actionsList) {
            cmds = crclGenerator.generate(actionsList, startReplanFromIndex, options, safeAbortRequestCount.get());
            resetReadOnlyActionsList(reverseFlag);
        }
        int indexes[] = crclGenerator.getActionToCrclIndexes();
        int indexesCopy[] = Arrays.copyOf(indexes, indexes.length);
        String labels[] = crclGenerator.getActionToCrclLabels();
        final String labelsCopy[] = Utils.copyOfNonNullsOnly(String.class, labels);
        @Nullable
        String takenPartNames[] = crclGenerator.getActionToCrclTakenPartsNames();
        final @Nullable
        String takenPartNamesCopy[] = Arrays.copyOf(takenPartNames, takenPartNames.length);
        javax.swing.SwingUtilities.invokeLater(() -> {
            updatePddlActionSectionToCrclDisplay(indexesCopy, labelsCopy, takenPartNamesCopy);
        });

        program.setName(getActionsCrclName());
        lastCrclProgName = crclProgName;
        crclProgName = program.getName();
        crclEndActionIndex = crclGenerator.getLastIndex();
        final int lastIndex = crclGenerator.getLastIndex();
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
        indexCachedTextField.setText(Integer.toString(getReplanFromIndex()));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        setEndCanonCmdId(program);
        updatePositionCacheTable();
        return program;
    }

    @UIEffect
    private void updatePddlActionSectionToCrclDisplay(int[] indexesCopy, String[] labelsCopy, @Nullable String[] takenPartNamesCopy) {
        setCrclIndexes(indexesCopy);
        setPddlLabelss(labelsCopy);
        setPddlTakenParts(takenPartNamesCopy);
        reloadPddlActions(readOnlyActionsList);
        autoResizeTableColWidths(jTablePddlOutput);
    }

    private void logDebug(String string) {
        if (debug) {
            LOGGER.log(Level.INFO, string);
        }
    }

    private void setEndCanonCmdId(CRCLProgramType program) {
        setCommandId(program.getEndCanon());
        long initCmdId = program.getInitCanon().getCommandID();
        long endCmdId = program.getEndCanon().getCommandID();
        int midSize = program.getMiddleCommand().size();
        if (midSize > 0) {
            long firstMidCmdId = program.getMiddleCommand().get(0).getCommandID();
            if (firstMidCmdId != initCmdId + 1) {
                logDebug("firstMidCmdId != initCmdId+1 : " + firstMidCmdId + "!= " + (initCmdId + 1));
            }
            long lastMidCmdId = program.getMiddleCommand().get(midSize - 1).getCommandID();
            if (lastMidCmdId != initCmdId + midSize) {
                logDebug("lastMidCmdId != initCmdId+midSize : " + lastMidCmdId + "!= " + (initCmdId + midSize));
            }
        }
        long expectedEndCmdId = initCmdId + midSize + 1;
        if (endCmdId != expectedEndCmdId) {
            logDebug("EndCanon Id " + endCmdId + " doesn't match InitCanon id " + initCmdId + " + 1+ size of middle commands " + midSize);
        }
    }

    private void updatePositionCacheTable() {
        Map<String, PoseType> map = crclGenerator.getPoseCache();
        Utils.runOnDispatchThread(() -> updatePositionCacheTableOnDisplay(map));
    }

    private final CachedTable positionCacheCachedTable;

    private void updatePositionCacheTableOnDisplay(Map<String, PoseType> map) {
        positionCacheCachedTable.setRowCount(0);
        for (Map.Entry<String, PoseType> entry : map.entrySet()) {
            PoseType pose = entry.getValue();
            if (null != pose) {
                PointType point = pose.getPoint();
                if (null != point) {
                    PmRpy rpy;
                    try {
                        rpy = CRCLPosemath.toPmRpy(pose);
                        positionCacheCachedTable.addRow(new Object[]{entry.getKey(), point.getX(), point.getY(), point.getZ(), Math.toDegrees(rpy.r), Math.toDegrees(rpy.p), Math.toDegrees(rpy.y), ""});
                    } catch (PmException ex) {
                        positionCacheCachedTable.addRow(new Object[]{entry.getKey(), point.getX(), point.getY(), point.getZ(), Double.NaN, Double.NaN, Double.NaN, ex.toString()});
                        Logger.getLogger(ExecutorJPanel.class.getName()).log(Level.SEVERE, "", ex);
                    }
                }
            }
        }
        Utils.autoResizeTableColWidths(positionCacheCachedTable);
    }

    private boolean lastReplanAfterCrclBlock = false;
    private final CachedCheckBox replanCachedCheckBox;

    private boolean isReplanCheckBoxSelected() {
        return replanCachedCheckBox.isSelected();
    }

    private XFuture<Boolean> doPddlActionsSectionAsync(int startSafeAbortRequestCount, int sectionNumber) {
        try {
            checkReverse();
            CRCLProgramType program = pddlActionSectionToCrcl(sectionNumber);

            if (autoStart) {
                boolean replanAfterCrclBlock
                        = crclGenerator.getLastIndex() < actionsList.size() - 1
                        && isReplanCheckBoxSelected();
                lastReplanAfterCrclBlock = replanAfterCrclBlock;
                if (replanAfterCrclBlock) {
                    return startCrclProgram(program)
                            .thenCompose("doPddlActionsSection.recursiveApplyGenerateCrcl(" + crclGenerator.getLastIndex() + " out of " + actionsList.size() + ")",
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
            LOGGER.log(Level.SEVERE, "", ex);
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

    private void runProgramCompleteRunnables(int startSafeAbortRequestCount) {
        checkSafeAbortAsync(() -> XFuture.completedFuture(false), startSafeAbortRequestCount);
        List<Runnable> runnables;
        synchronized (this) {
            runProgramCompleteRunnablesTime = System.currentTimeMillis();
            this.runningProgram = false;
            runnables = new ArrayList<>(programCompleteRunnablesList);
            programCompleteRunnablesList.clear();
        }
        for (Runnable r : runnables) {
            r.run();
        }
    }

    private XFuture<Boolean> placePartSlot(String part, String slot) throws IllegalStateException, SQLException, InterruptedException, ExecutionException, PendantClientInner.ConcurrentBlockProgramsException, CRCLException, PmException {
        crclGenerator.partialReset();
        Map<String, String> options = getTableOptions();
        setReplanFromIndex(0);
        List<Action> placePartActionsList = new ArrayList<>();
        Action placePartAction
                = new Action(
                        PLACE_PART, // type
                        slot // arg
                );
        placePartActionsList.add(placePartAction);
        syncCrclGeneratorPositionMaps();
        CRCLProgramType program = createEmptyProgram();
        crclGenerator.setManualAction(true);
        List<MiddleCommandType> cmds = crclGenerator.generate(placePartActionsList, 0, options, safeAbortRequestCount.get());
        crclGenerator.setManualAction(false);
        indexCachedTextField.setText(Integer.toString(getReplanFromIndex()));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        setEndCanonCmdId(program);
        XFuture<Boolean> ret = startCrclProgram(program);
        replanStarted.set(false);
        return ret;
    }

    private XFuture<Boolean> testPartPosition(String part) throws IllegalStateException, SQLException, InterruptedException, ExecutionException, PendantClientInner.ConcurrentBlockProgramsException, CRCLException, PmException {
        clearAll();
        Map<String, String> options = getTableOptions();
        setReplanFromIndex(0);
        List<Action> testPartPositionActionList = new ArrayList<>();
        Action takePartAction = new Action(
                TEST_PART_POSITION,
                part
        );
        testPartPositionActionList.add(takePartAction);
        syncCrclGeneratorPositionMaps();
        CRCLProgramType program = createEmptyProgram();
        List<MiddleCommandType> cmds = crclGenerator.generate(testPartPositionActionList, 0, options, safeAbortRequestCount.get());
        indexCachedTextField.setText(Integer.toString(getReplanFromIndex()));
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

    private XFuture<Boolean> takePart(String part) throws IllegalStateException, SQLException, InterruptedException, ExecutionException, PendantClientInner.ConcurrentBlockProgramsException, CRCLException, PmException {
        crclGenerator.partialReset();
        Map<String, String> options = getTableOptions();
        setReplanFromIndex(0);
        List<Action> takePartActionsList = new ArrayList<>();
        Action takePartAction
                = new Action(
                        TAKE_PART, // type
                        part // arg
                );
        takePartActionsList.add(takePartAction);
        syncCrclGeneratorPositionMaps();
        CRCLProgramType program = createEmptyProgram();
        List<MiddleCommandType> cmds = crclGenerator.generate(takePartActionsList, 0, options, safeAbortRequestCount.get());
        indexCachedTextField.setText(Integer.toString(getReplanFromIndex()));
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

    private XFuture<Boolean> returnPart(String part) {
        clearAll();
        Map<String, String> options = getTableOptions();
        setReplanFromIndex(0);
        List<MiddleCommandType> cmds = new ArrayList<>();
        CRCLProgramType program = createEmptyProgram();
        crclGenerator.setOptions(options);
        crclGenerator.returnPart(part, cmds);

        indexCachedTextField.setText(Integer.toString(getReplanFromIndex()));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        setEndCanonCmdId(program);
        replanStarted.set(false);
        return startCrclProgram(program);
    }

    private Random random = new Random();
    private PoseType testDropOffPose;

    public PoseType getTestDropOffPose() {
        return testDropOffPose;
    }

    /**
     * Add a position map.
     * <p>
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
        syncCrclGeneratorPositionMaps();
    }

    private void syncCrclGeneratorPositionMaps() {
        if (isRunningProgram()) {
            throw new IllegalStateException("Attempting to change position maps when program running.");
        }
        crclGenerator.setPositionMaps(getPositionMaps());
    }

    /**
     * Remove a previously added position map.
     *
     * @param pm position map to be removed.
     */
    public void removePositionMap(PositionMap pm) {
        positionMapJPanel1.removePositionMap(pm);
        syncCrclGeneratorPositionMaps();
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

    @UIEffect
    private XFuture<Boolean> randomDropOff() {
        Map<String, String> options = getTableOptions();
        setReplanFromIndex(0);
        List<MiddleCommandType> cmds = new ArrayList<>();
        crclGenerator.setOptions(options);
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

        crclGenerator.placePartByPose(cmds, testDropOffPose);
        CRCLProgramType program = createEmptyProgram();
        indexCachedTextField.setText(Integer.toString(getReplanFromIndex()));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        setEndCanonCmdId(program);

        PointType testDropOffPosePoint
                = requireNonNull(testDropOffPose.getPoint(), "testDropOffPose.getPoint()");
        String randomPoseString
                = String.format("%.1f, %.1f, %.1f",
                        testDropOffPosePoint.getX(),
                        testDropOffPosePoint.getY(),
                        testDropOffPosePoint.getZ());
        PointType origPosePoint
                = requireNonNull(origPose.getPoint(), "origPose.getPoint()");
        String origPoseString
                = String.format("%.1f, %.1f, %.1f",
                        origPosePoint.getX(),
                        origPosePoint.getY(),
                        origPosePoint.getZ());
        logDebug("randomPoseString = " + randomPoseString);
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

    @UIEffect
    private void gridDropOff() {
        if (gridTestCurrentY > gridTestMaxY + 0.001) {
            this.clearAll();
            return;
        }
        Map<String, String> options = getTableOptions();
        setReplanFromIndex(0);
        List<MiddleCommandType> cmds = new ArrayList<>();
        crclGenerator.setOptions(options);
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
        logDebug("gridTestCurrentX = " + gridTestCurrentX);
        logDebug("gridTestCurrentY = " + gridTestCurrentY);
        PoseType origPose = pose(point(x, y, z), vector(1.0, 0.0, 0.0), vector(0.0, 0.0, -1.0));
        PointType offset = getOffset(x, y, z);
        testDropOffPose = correctPose(origPose);
        crclGenerator.placePartByPose(cmds, testDropOffPose);
        CRCLProgramType program = createEmptyProgram();
        indexCachedTextField.setText(Integer.toString(getReplanFromIndex()));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        setEndCanonCmdId(program);
        setCrclProgram(program);
        PointType testDropOffPosePoint
                = requireNonNull(testDropOffPose.getPoint(), "testDropOffPose.getPoint()");
        String gridPoseString
                = String.format("%.1f, %.1f, %.1f",
                        testDropOffPosePoint.getX(),
                        testDropOffPosePoint.getY(),
                        testDropOffPosePoint.getZ());
        PointType origPosePoint
                = requireNonNull(origPose.getPoint(), "origPose.getPoint()");
        String origPoseString
                = String.format("%.1f, %.1f, %.1f",
                        origPosePoint.getX(),
                        origPosePoint.getY(),
                        origPosePoint.getZ());
        logDebug("gridPoseString = " + gridPoseString);
        jTextFieldOffset.setText(String.format("%.1f,%.1f,%.1f", offset.getX(), offset.getY(), offset.getZ()));
        jTextFieldAdjPose.setText(gridPoseString);
        this.jTextFieldTestPose.setText(origPoseString);
        replanStarted.set(false);
    }

    private XFuture<Boolean> randomPickup() throws CRCLException, PmException {
        Map<String, String> options = getTableOptions();
        setReplanFromIndex(0);
        List<MiddleCommandType> cmds = new ArrayList<>();
        crclGenerator.setOptions(options);
        crclGenerator.takePartByPose(cmds, testDropOffPose, "testDropOffPose");
        CRCLProgramType program = createEmptyProgram();
        indexCachedTextField.setText(Integer.toString(getReplanFromIndex()));
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
            LOGGER.log(Level.SEVERE, "", ex);
            XFuture<Boolean> future = new XFuture<>("lookForPartsException");
            future.completeExceptionally(ex);
            return future;
        }
    }

    public CRCLProgramType createLookForPartsProgram() {
        try {
            return createLookForPartsProgramInternal();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }
    
    public boolean recheckKitsOnly() {
         try {
           return crclGenerator.recheckKitsOnly();
         } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }

    private CRCLProgramType createLookForPartsProgramInternal() throws ExecutionException, InterruptedException, PendantClientInner.ConcurrentBlockProgramsException, SQLException, IllegalStateException, CRCLException, PmException {
        checkDbSupplierPublisher();
        Map<String, String> options = getTableOptions();
        setReplanFromIndex(0);
        List<Action> lookForActionsList = new ArrayList<>();
        Action lookForAction = new Action(
                LOOK_FOR_PARTS,
                new String[]{},
                0
        );
        lookForActionsList.add(lookForAction);
        crclGenerator.clearPoseCache();
        crclGenerator.clearLastRequiredPartsMap();
        CRCLProgramType program = createEmptyProgram();
        List<MiddleCommandType> cmds = crclGenerator.generate(lookForActionsList, 0, options, safeAbortRequestCount.get());
        indexCachedTextField.setText(Integer.toString(getReplanFromIndex()));
        program.getMiddleCommand().clear();
        program.getMiddleCommand().addAll(cmds);
        setEndCanonCmdId(program);
        replanStarted.set(false);
        return program;
    }

    @UIEffect
    private XFuture<Boolean> gotoToolChangerApproach(String poseName, PoseType pose) {
        try {
            Map<String, String> options = getTableOptions();
            setReplanFromIndex(0);
            List<Action> gototToolChangerApproachActionsList = new ArrayList<>();
            Action gototToolChangerApproachAction
                    = new Action(
                            GOTO_TOOL_CHANGER_APPROACH,
                            poseName
                    );
            gototToolChangerApproachActionsList.add(gototToolChangerApproachAction);
            crclGenerator.clearPoseCache();
            crclGenerator.clearLastRequiredPartsMap();
            crclGenerator.putPoseCache(poseName, pose);
            crclGenerator.setApproachToolChangerZOffset(Double.parseDouble(jTextFieldToolChangerApproachZOffset.getText()));
            return executeActions(gototToolChangerApproachActionsList, options);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            XFuture<Boolean> future = new XFuture<>("gototToolChangerApproachPartsException");
            future.completeExceptionally(ex);
            return future;
        }
    }

    private XFuture<Boolean> gotoToolChangerPose(String poseName, PoseType pose) {
        try {
            Map<String, String> options = getTableOptions();
            setReplanFromIndex(0);
            List<Action> gototToolChangerApproachActionsList = new ArrayList<>();
            Action gototToolChangerApproachAction
                    = new Action(
                            GOTO_TOOL_CHANGER_POSE,
                            poseName
                    );
            gototToolChangerApproachActionsList.add(gototToolChangerApproachAction);
            crclGenerator.clearPoseCache();
            crclGenerator.clearLastRequiredPartsMap();
            crclGenerator.putPoseCache(poseName, pose);
            return executeActions(gototToolChangerApproachActionsList, options);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            XFuture<Boolean> future = new XFuture<>("gototToolChangerApproachPartsException");
            future.completeExceptionally(ex);
            return future;
        }
    }

    void updateCurrentToolHolderContentsMap(String toolChangerPosName, String toolName) {
        Utils.runOnDispatchThread(() -> updateCurrentToolHolderJTable(toolChangerPosName, toolName));
    }

    private final ConcurrentLinkedQueue<BiConsumer<String, String>> updateToolHolderContentsListeners = new ConcurrentLinkedQueue<>();

    public void addToolHolderContentsListener(BiConsumer<String, String> listener) {
        updateToolHolderContentsListeners.add(listener);
    }

    public void removeToolHolderContentsListener(BiConsumer<String, String> listener) {
        updateToolHolderContentsListeners.add(listener);
    }

    @UIEffect
    private synchronized void updateCurrentToolHolderJTable(String toolChangerPosName, String toolName) {
        try {
            clearToolHolderContentsTableModelListener();
            for (int i = 0; i < holderContentsCachedTable.getRowCount(); i++) {
                String holderName = (String) holderContentsCachedTable.getValueAt(i, 0);
                if (toolChangerPosName.equals(holderName)) {
                    String origToolName = (String) holderContentsCachedTable.getValueAt(i, 1);
                    if (!toolName.equals(origToolName)) {
                        holderContentsCachedTable.setValueAt(toolName, i, 1);
                        for (BiConsumer<String, String> listener : updateToolHolderContentsListeners) {
                            listener.accept(toolChangerPosName, toolName);
                        }
                        handleToolHolderContentsChange();
                    }
                    break;
                }
            }
        } finally {
            setToolHolderContentsTableModelListener();
        }
    }

    public void putInToolHolderContentsMap(String holder, String contents) {
        crclGenerator.getCurrentToolHolderContentsMap().put(holder, contents);
        updateCurrentToolHolderContentsMap(holder, contents);
    }

    public Map<String, String> getCurrentToolHolderContentsMap() {
        return Collections.unmodifiableMap(crclGenerator.getCurrentToolHolderContentsMap());
    }

    public Map<String, Set<String>> getPossibleToolHolderContentsMap() {
        return crclGenerator.getPossibleToolHolderContentsMap();
    }

    @UIEffect
    private XFuture<Boolean> dropToolByHolder(String holderName) {
        try {
            Map<String, String> options = getTableOptions();
            setReplanFromIndex(0);
            List<Action> newActionsList = new ArrayList<>();
            Action dropToolByHolderAction
                    = new Action(
                            DROP_TOOL_BY_HOLDER,
                            holderName);
            newActionsList.add(dropToolByHolderAction);
            crclGenerator.clearPoseCache();
            crclGenerator.clearLastRequiredPartsMap();
            crclGenerator.setApproachToolChangerZOffset(Double.parseDouble(jTextFieldToolChangerApproachZOffset.getText()));
            return executeActions(newActionsList, options);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            XFuture<Boolean> future = new XFuture<>("gototToolChangerApproachPartsException");
            future.completeExceptionally(ex);
            return future;
        }
    }

    @UIEffect
    private XFuture<Boolean> dropToolAny() {
        try {
            Map<String, String> options = getTableOptions();
            setReplanFromIndex(0);
            List<Action> newActionsList = new ArrayList<>();
            Action dropToolAnyAction
                    = new Action(DROP_TOOL_ANY);
            newActionsList.add(dropToolAnyAction);
            crclGenerator.clearPoseCache();
            crclGenerator.clearLastRequiredPartsMap();
            crclGenerator.setApproachToolChangerZOffset(Double.parseDouble(jTextFieldToolChangerApproachZOffset.getText()));
            return executeActions(newActionsList, options);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            XFuture<Boolean> future = new XFuture<>("gototToolChangerApproachPartsException");
            future.completeExceptionally(ex);
            return future;
        }
    }

    private XFuture<Boolean> executeActions(List<Action> actionsList, Map<String, String> options) {
        this.loadActionsList(actionsList, reverseFlag);
        ExecutorService service = this.generateCrclService;
        if (null == service) {
            service = aprsSystem.getRunProgramService();
            this.generateCrclService = service;
        }
        return checkDbSupplierPublisherAsync()
                .thenComposeAsync("executeActions", x -> executeActionsInternal(actionsList, options), service);
    }

    private void showException(Exception ex) {
        showExceptionInProgram(ex);
        if (null != aprsSystem) {
            aprsSystem.showException(ex);
        }
    }

    private XFuture<Boolean> executeActionsInternal(List<Action> actionsList, Map<String, String> options) {
        try {
            CRCLProgramType program = createEmptyProgram();
            List<MiddleCommandType> cmds
                    = crclGenerator.generate(
                            actionsList,
                            0,
                            options,
                            safeAbortRequestCount.get());
            indexCachedTextField.setText(Integer.toString(getReplanFromIndex()));
            program.getMiddleCommand().clear();
            program.getMiddleCommand().addAll(cmds);
            setEndCanonCmdId(program);
            replanStarted.set(false);
            return startCrclProgram(program);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            showException(ex);
            throw new RuntimeException(ex);
        }
    }

    @UIEffect
    private XFuture<Boolean> pickupToolByHolder(String holderName) {
        try {
            Map<String, String> options = getTableOptions();
            setReplanFromIndex(0);
            List<Action> newActionsList = new ArrayList<>();
            Action pickupToolByHolderAction
                    = new Action(
                            PICKUP_TOOL_BY_HOLDER,
                            new String[]{holderName});
            newActionsList.add(pickupToolByHolderAction);
            crclGenerator.clearLastRequiredPartsMap();
            crclGenerator.setApproachToolChangerZOffset(Double.parseDouble(jTextFieldToolChangerApproachZOffset.getText()));
            return executeActions(newActionsList, options);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            XFuture<Boolean> future = new XFuture<>("pickupToolByHolder." + holderName);
            future.completeExceptionally(ex);
            return future;
        }
    }

    @UIEffect
    private XFuture<Boolean> pickupToolByTool(String toolName) {
        try {
            Map<String, String> options = getTableOptions();
            setReplanFromIndex(0);
            List<Action> newActionsList = new ArrayList<>();
            Action pickupToolByToolAction
                    = new Action(
                            PICKUP_TOOL_BY_TOOL,
                            new String[]{toolName});
            newActionsList.add(pickupToolByToolAction);
            crclGenerator.clearLastRequiredPartsMap();
            crclGenerator.setApproachToolChangerZOffset(Double.parseDouble(jTextFieldToolChangerApproachZOffset.getText()));
            return executeActions(newActionsList, options);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            XFuture<Boolean> future = new XFuture<>("pickupToolByTool." + toolName);
            future.completeExceptionally(ex);
            return future;
        }
    }

    @UIEffect
    private XFuture<Boolean> switchTool(String toolName) {
        try {
            Map<String, String> options = getTableOptions();
            setReplanFromIndex(0);
            List<Action> newActionsList = new ArrayList<>();
            Action switchToolAction
                    = new Action(
                            SWITCH_TOOL,
                            new String[]{toolName});
            newActionsList.add(switchToolAction);
            crclGenerator.clearLastRequiredPartsMap();
            crclGenerator.setApproachToolChangerZOffset(Double.parseDouble(jTextFieldToolChangerApproachZOffset.getText()));
            return executeActions(newActionsList, options);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            XFuture<Boolean> future = new XFuture<>("pickupToolByTool." + toolName);
            future.completeExceptionally(ex);
            return future;
        }
    }

    private volatile List<XFutureVoid> checkDbSupplierPublisherFuturesList = Collections.emptyList();

    private void checkDbSupplierPublisher() {
        assert (null != crclGenerator) : "null == pddlActionToCrclGenerator";
        assert (null != dbSetupSupplier) : "null == dbSetupSupplier";
        if (crclGenerator.isConnected()) {
            return;
        }
        if (null != dbSetupSupplier) {
            try {
                dbSetupPublisher = dbSetupSupplier.call();
                dbSetupPublisher.addDbSetupListener(dbSetupListener);

            } catch (Exception ex) {
                Logger.getLogger(VisionToDBJPanel.class
                        .getName()).log(Level.SEVERE, "", ex);
            }
        }
        dbSetupPublisher.setDbSetup(new DbSetupBuilder().setup(dbSetupPublisher.getDbSetup()).connected(true).build());
        checkDbSupplierPublisherFuturesList = dbSetupPublisher.notifyAllDbSetupListeners(null);
        if (!crclGenerator.isConnected()) {
            throw new IllegalStateException("Failed to connnect to database");
        }
    }

    @Nullable
    private String getConnnectionURL() throws SQLException {
        Connection con = crclGenerator.getDbConnection();
        if (null == con) {
            throw new IllegalStateException("connection is null");
        }
        return con.getMetaData().getURL();
    }

    private XFutureVoid checkDbSupplierPublisherAsync() {
        if (null == this.crclGenerator) {
            XFutureVoid ret = new XFutureVoid("checkDbSupplierPublisher(null==pddlActionToCrclGenerator)");
            ret.completeExceptionally(new IllegalStateException("checkDbSupplierPublisher(null==pddlActionToCrclGenerator)"));
            return ret;
        }
        if (crclGenerator.isConnected()) {
            try {
                return XFutureVoid.completedFutureWithName("checkDbSupplierPublisher.alreadyConnected." + getConnnectionURL());
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "", ex);
                XFutureVoid ret = new XFutureVoid("checkDbSupplierPublisher.alreadyConnected.withException");
                ret.completeExceptionally(ex);
                return ret;
            }
        }
        XFutureVoid f1 = new XFutureVoid("checkDbSupplierPublisher.f1");
        newDbSetupFutures.add(f1);
        if (null != dbSetupSupplier) {
            try {
                dbSetupPublisher = dbSetupSupplier.call();
                dbSetupPublisher.addDbSetupListener(dbSetupListener);

            } catch (Exception ex) {
                Logger.getLogger(VisionToDBJPanel.class
                        .getName()).log(Level.SEVERE, "", ex);
            }
        }
        if (null != dbSetupPublisher) {
            dbSetupPublisher.setDbSetup(new DbSetupBuilder().setup(dbSetupPublisher.getDbSetup()).connected(true).build());
            checkDbSupplierPublisherFuturesList = dbSetupPublisher.notifyAllDbSetupListeners(generateCrclService);
        } else {
            logDebug("dbSetupPublisher == null");
            f1.completeExceptionally(new IllegalStateException("dbSetupPublisher == null"));
        }
        return f1; //XFuture.allOfWithName("checkDbSupplierPublisher.all", f1,f2);
    }

    public void setOption(String key, String val) {
        for (int i = 0; i < optionsCachedTable.getRowCount(); i++) {
            Object keyCheck = optionsCachedTable.getValueAt(i, 0);
            if (Objects.equals(keyCheck, key)) {
                optionsCachedTable.setValueAt(val, i, 1);
                break;
            }
        }
        crclGenerator.setOptions(getTableOptions());
    }

    public void setToolHolderOperationEnabled(boolean enable) {
        crclGenerator.setToolHolderOperationEnabled(enable);
    }

    public boolean isToolHolderOperationEnabled() {
        return crclGenerator.isToolHolderOperationEnabled();
    }

    private final CachedTable optionsCachedTable;

    public Map<String, String> getTableOptions() {
        Map<String, String> options = new HashMap<>();
        @Nullable
        Object[][] optionsData = optionsCachedTable.getData();
        for (int i = 0; i < optionsData.length; i++) {
            Object key = optionsData[i][0];
            Object val = optionsData[i][1];
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
    private javax.swing.JButton jButtonRenameToolHolderPose;
    private javax.swing.JButton jButtonReset;
    private javax.swing.JButton jButtonReturn;
    private javax.swing.JButton jButtonSetCurrentTool;
    private javax.swing.JButton jButtonStep;
    private javax.swing.JButton jButtonStopRandomTest;
    private javax.swing.JButton jButtonTake;
    private javax.swing.JButton jButtonTest;
    private javax.swing.JButton jButtonTestPickup;
    private javax.swing.JButton jButtonUpdatePoseCache;
    private javax.swing.JCheckBox jCheckBoxDebug;
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
    private javax.swing.JLabel jLabel17;
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
    private javax.swing.JPanel jPanelInnerManualControl;
    private javax.swing.JPanel jPanelOpOuter;
    private javax.swing.JPanel jPanelOuterManualControl;
    private javax.swing.JPanel jPanelToolChange;
    private javax.swing.JPanel jPanelToolHolderPositions;
    private javax.swing.JPanel jPanelToolOffsets;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
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
    private javax.swing.JTextField jTextFieldAdjPose;
    private javax.swing.JTextField jTextFieldCurrentPart;
    private javax.swing.JTextField jTextFieldCurrentToolName;
    private javax.swing.JTextField jTextFieldCurrentToolOffset;
    private javax.swing.JTextField jTextFieldGridSize;
    private javax.swing.JTextField jTextFieldIndex;
    private javax.swing.JTextField jTextFieldLogFilename;
    private javax.swing.JTextField jTextFieldOffset;
    private javax.swing.JTextField jTextFieldPddlOutputActions;
    private javax.swing.JTextField jTextFieldRandomDropoffCount;
    private javax.swing.JTextField jTextFieldRandomPickupCount;
    private javax.swing.JTextField jTextFieldRecordFailCount;
    private javax.swing.JTextField jTextFieldRecordSuccessCount;
    private javax.swing.JTextField jTextFieldTestPose;
    private javax.swing.JTextField jTextFieldTestXMax;
    private javax.swing.JTextField jTextFieldTestXMin;
    private javax.swing.JTextField jTextFieldTestYMax;
    private javax.swing.JTextField jTextFieldTestYMin;
    private javax.swing.JTextField jTextFieldTestZ;
    private javax.swing.JTextField jTextFieldToolChangerApproachZOffset;
    private aprs.actions.optaplanner.display.OpDisplayJPanel opDisplayJPanelInput;
    private aprs.actions.optaplanner.display.OpDisplayJPanel opDisplayJPanelSolution;
    private aprs.actions.executor.PositionMapJPanel positionMapJPanel1;
    // End of variables declaration//GEN-END:variables

    @Nullable
    private String propsGetFileName(Properties props, String key) throws IOException {
        String filename = props.getProperty(key);
        if (null == filename || !filename.startsWith("..") || null == propertiesFile) {
            return filename;
        }
        String newfilename = filename;
        File dir = propertiesFile.getParentFile();
        while (null != dir && (newfilename.startsWith("../") || newfilename.startsWith("..\\"))) {
            newfilename = newfilename.substring(3);
            dir = dir.getParentFile();
        }
        File f = new File(dir, newfilename);
        if (!f.exists() && (new File(dir, newfilename.replace('\\', File.separatorChar))).exists()) {
            newfilename = newfilename.replace('\\', File.separatorChar);
            f = new File(dir, newfilename);
        } else if (!f.exists() && (new File(dir, newfilename.replace('/', File.separatorChar))).exists()) {
            newfilename = newfilename.replace('/', File.separatorChar);
            f = new File(dir, newfilename);
        }
        String fullfilename = f.getCanonicalPath();
        return fullfilename;
    }

    @Nullable
    private static final String TEMP_DIR;

    static {
        String tempDirName = null;
        try {
            File parentFile = File.createTempFile("test_temp", ".txt").getParentFile();
            if(null == parentFile) {
                throw new IllegalStateException("null == parentFile");
            }
            tempDirName = parentFile.getCanonicalPath();
            if (tempDirName.endsWith("\\1")
                    || tempDirName.endsWith("\\2")
                    || tempDirName.endsWith("\\3")
                    || tempDirName.endsWith("\\4")
                    || tempDirName.endsWith("\\5")
                    || tempDirName.endsWith("\\6")
                    || tempDirName.endsWith("\\7")
                    || tempDirName.endsWith("\\8")
                    || tempDirName.endsWith("\\9")) {
                tempDirName = tempDirName.substring(0, tempDirName.length() - 1);
            }
        } catch (IOException ex) {
            Logger.getLogger(ExecutorJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        TEMP_DIR = tempDirName;
    }

    public void loadProperties() throws IOException {
        if (null != propertiesFile && propertiesFile.exists()) {
            if (propertiesFile.isDirectory()) {
                logDebug("Can not loadProperties file \"" + propertiesFile + "\" : It is a directory instead of text file.");
                return;
            }
            if (!propertiesFile.canRead()) {
                logDebug("Can not loadProperties file \"" + propertiesFile + "\" : file is not readable.");
                return;
            }
            Properties props = new Properties();
            try (FileReader fr = new FileReader(propertiesFile)) {
                props.load(fr);
            }
            loadComboModels(props);
            String propsActionsFileString = propsGetFileName(props, PDDLOUTPUT);
            if (isTempDir(propsActionsFileString)) {
                propsActionsFileString = null;
            } else {
                checkFilename(propsActionsFileString);
            }
            this.actionsFileString = propsActionsFileString;

            String propsReverseActionsFileString = propsGetFileName(props, REVERSE_PDDLOUTPUT);
            if (isTempDir(propsReverseActionsFileString)) {
                propsReverseActionsFileString = null;
            } else {
                checkFilename(propsReverseActionsFileString);
            }
            this.reverseActionsFileString = propsReverseActionsFileString;
            boolean reverseFlagFromProperty = false;
            String reverseFlagProperty = props.getProperty(REVERSE_FLAG);
            if (null != reverseFlagProperty) {
                reverseFlagFromProperty = Boolean.parseBoolean(reverseFlagProperty);
            }
            setReverseFlag(reverseFlagFromProperty);
            reloadActionsFile(reverseFlagFromProperty, true);
            String autostartString = props.getProperty(PDDLCRCLAUTOSTART);
            if (null != autostartString) {
                this.autoStart = Boolean.valueOf(autostartString);
            }
            String enableOptaPlannerString = props.getProperty(ENABLE_OPTA_PLANNER);
            if (null != enableOptaPlannerString) {
                boolean enableOptaPlanner = Boolean.valueOf(enableOptaPlannerString);
                enableOptaplannerCachedCheckBox.setSelected(enableOptaPlanner);
            }
            for (String name : props.stringPropertyNames()) {
                if (!name.equals(PDDLCRCLAUTOSTART)
                        && !name.equals(PDDLOUTPUT)
                        && !name.equals(MANUAL_PART_NAMES)
                        && !name.equals(MANUAL_SLOT_NAMES)) {
                    boolean foundit = false;
                    for (int i = 0; i < optionsCachedTable.getRowCount(); i++) {
                        Object tableValue = optionsCachedTable.getValueAt(i, 0);
                        if (tableValue == null) {
                            continue;
                        }
                        String nameFromTable = tableValue.toString();
                        if (nameFromTable.equals(name)) {
                            optionsCachedTable.setValueAt(props.getProperty(name), i, 1);
                            foundit = true;
                            break;
                        }
                    }
                    if (!foundit) {
                        optionsCachedTable.addRow(new Object[]{name, props.getProperty(name)});
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

            Utils.runOnDispatchThread(this::completeLoadPropertiesOnDisplay);
        }
    }

    private static boolean isTempDir(@Nullable String propsReverseActionsFileString) {
        return null != TEMP_DIR
                && TEMP_DIR.length() > 0
                && null != propsReverseActionsFileString
                && propsReverseActionsFileString.startsWith(TEMP_DIR);
    }

    @UIEffect
    private void completeLoadPropertiesOnDisplay() {
        syncPanelToGeneratorToolData();
        loadToolMenus();
    }

    private static final String REVERSE_FLAG = "reverseFlag";

    @UIEffect
    private void loadToolMenus() {
        toolDropByHolderMenu.removeAll();
        String[] emptyToolChangerNames = getEmptyToolChangerNames();
        String currentToolName = crclGenerator.getCurrentToolName();
        if (emptyToolChangerNames.length < 1 || CrclGenerator.isEmptyTool(currentToolName)) {
            toolDropByHolderMenu.setEnabled(false);
            toolDropCurrentToolMenuItem.setEnabled(false);
        } else {
            toolDropByHolderMenu.setEnabled(true);
            for (String holderName : emptyToolChangerNames) {
                JMenuItem mi = new JMenuItem(holderName);
                mi.addActionListener(e -> dropToolByHolder(holderName));
                toolDropByHolderMenu.add(mi);
            }
            toolDropCurrentToolMenuItem.setEnabled(true);
        }
        toolPickupByHolderMenu.removeAll();
        toolPickupByToolMenu.removeAll();
        String[] fullToolChangerNames = getFullToolChangerNames();
        if (fullToolChangerNames.length < 1 || !CrclGenerator.isEmptyTool(currentToolName)) {
            toolPickupByHolderMenu.setEnabled(false);
            toolPickupByToolMenu.setEnabled(false);
        } else {
            toolPickupByHolderMenu.setEnabled(true);
            toolPickupByToolMenu.setEnabled(true);
            for (String holderName : fullToolChangerNames) {
                JMenuItem holderMi = new JMenuItem(holderName);
                holderMi.addActionListener(e -> pickupToolByHolder(holderName));
                toolPickupByHolderMenu.add(holderMi);
                String toolName = crclGenerator.getExpectedToolHolderContentsMap().get(holderName);
                if (null != toolName) {
                    JMenuItem toolMi = new JMenuItem(toolName);
                    toolMi.addActionListener(e -> pickupToolByTool(toolName));
                    toolPickupByToolMenu.add(toolMi);
                }
            }
        }
        toolSetToolMenu.removeAll();
        toolSwitchToolMenu.removeAll();
        String toolNames[] = getToolNames();
        boolean currentToolFound = false;
        for (String toolName : toolNames) {
            if (toolName.trim().length() < 1) {
                continue;
            }
            JRadioButtonMenuItem toolMi = new JRadioButtonMenuItem(toolName);
            toolSetToolMenu.add(toolMi);
            if (Objects.equals(toolName, currentToolName) && !currentToolFound) {
                toolMi.setSelected(true);
                toolMi.setEnabled(false);
                currentToolFound = true;
            } else {
                toolMi.addActionListener(e -> crclGenerator.setCurrentToolName(toolName));
                JMenuItem switchToolMi = new JMenuItem(toolName);
                switchToolMi.addActionListener(e -> switchTool(toolName));
                toolSwitchToolMenu.add(switchToolMi);
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
                    LOGGER.log(Level.SEVERE, "", ex);
                }
            } else {
                File parentFile = propertiesFile.getParentFile();
                if (null == parentFile) {
                    String errString = "Can't load errorMapFile : " + fname + ", parentFile is null";
                    setErrorString(errString);
                    logDebug(errString);
                    return;
                }
                String fullPath = parentFile.toPath().resolve(fname).normalize().toString();
                f = new File(fullPath);
                if (f.exists()) {
                    try {
                        positionMapJPanel1.addPositionMapFile(f);
                    } catch (PositionMap.BadErrorMapFormatException ex) {
                        LOGGER.log(Level.SEVERE, "", ex);
                    }
                } else {
                    String errString = "Can't load errorMapFile : " + fname + "   or " + fullPath;
                    setErrorString(errString);
                    logDebug(errString);
                }
            }
        }
    }

    private static final String ENABLE_OPTA_PLANNER = "enableOptaPlanner";

    private void loadComboModels(Properties props) {
        String manualPartNames = props.getProperty(MANUAL_PART_NAMES, "");
        String pna[] = manualPartNames.split("[ \t,\\[\\]\\{\\}]+");
        manualObjectCachedComboBox.setItems(pna);
//        manualObjectCachedComboBox.removeAllElements();
//        for (String aPna : pna) {
//            if (null != aPna && aPna.length() > 0
//                    && !aPna.equals("null")) {
//                manualObjectCachedComboBox.addElement(aPna);
//            }
//        }

        String manualSlotNames = props.getProperty(MANUAL_SLOT_NAMES, "");
        String sna[] = manualSlotNames.split("[ \t,\\[\\]\\{\\}]+");
        manualSlotCachedComboBox.setItems(sna);

//        manualSlotCachedComboBox.removeAllElements();
//        for (String aSna : sna) {
//            if (null != aSna && aSna.length() > 0
//                    && !aSna.equals("null")) {
//                manualSlotCachedComboBox.addElement(aSna);
//            }
//        }
    }

    @Nullable
    public List<Action> reloadActionsFile(boolean newReverseFlag, boolean showInOptaplanner) throws IOException {
        setReverseFlag(newReverseFlag);
        String output = newReverseFlag ? reverseActionsFileString : actionsFileString;
        if (null == output) {
            System.err.println("newReverseFlag = " + newReverseFlag);
            System.err.println("reverseActionsFileString = " + reverseActionsFileString);
            System.err.println("actionsFileString = " + actionsFileString);
            checkReverse();
            return null;
        }
        checkFilename(output);
        File f = new File(output);
        List<Action> ret;
        if (f.exists() && f.canRead() && !f.isDirectory()) {
            ret = loadActionsFile(f, showInOptaplanner, newReverseFlag);
            pddlOutputActionsCachedText.setText(f.getCanonicalPath());
        } else {
            File parentFile = propertiesFile.getParentFile();
            if (null == parentFile) {
                System.err.println("newReverseFlag = " + newReverseFlag);
                System.err.println("reverseActionsFileString = " + reverseActionsFileString);
                System.err.println("actionsFileString = " + actionsFileString);
                throw new IllegalStateException("propertiesFile \"" + propertiesFile + "\" has no parent");
            }
            String fullPath = parentFile.toPath().resolve(output).normalize().toString();
            f = new File(fullPath);
            if (f.exists() && f.canRead() && !f.isDirectory()) {
                ret = loadActionsFile(f, showInOptaplanner, newReverseFlag);
                pddlOutputActionsCachedText.setText(f.getCanonicalPath());
            } else {
                String fullPath2 = parentFile.toPath().resolveSibling(output).normalize().toString();
                f = new File(fullPath2);
                if (f.exists() && f.canRead() && !f.isDirectory()) {
                    pddlOutputActionsCachedText.setText(f.getCanonicalPath());
                    ret = loadActionsFile(f, showInOptaplanner, newReverseFlag);
                } else {
                    System.err.println("newReverseFlag = " + newReverseFlag);
                    System.err.println("reverseActionsFileString = " + reverseActionsFileString);
                    System.err.println("actionsFileString = " + actionsFileString);
                    throw new IllegalStateException("can not read file \"" + output + "\" f=" + f);
                }
            }
        }
        return ret;
    }

    private final ConcurrentLinkedDeque<XFutureVoid> newDbSetupFutures = new ConcurrentLinkedDeque<>();

    private void handleNewDbSetup(DbSetup setup) {
        if (null != crclGenerator) {
            crclGenerator.setDbSetup(setup)
                    .thenRun(() -> {
                        XFutureVoid f = newDbSetupFutures.poll();
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

    private javax.swing.@Nullable Timer replanActionTimer = null;
    private final Runnable defaultReplanRunnable = new Runnable() {
        @Override
        @SuppressWarnings("guieffect")
        public void run() {
            replanActionTimer
                    = new javax.swing.Timer(200, (ActionEvent e) -> {
                        generateCrclAsyncWithCatch();
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
                logDebug("customRunnablesIndex = " + customRunnablesIndex);
                RunnableWithThrow runnable = customRunnables.get(customRunnablesIndex);
                customRunnablesIndex = (customRunnablesIndex + 1) % customRunnables.size();
                if (null != runnable) {
                    Utils.runOnDispatchThreadWithCatch(runnable);
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "", ex);
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
                Utils.runOnDispatchThread(ExecutorJPanel.this::runAllCustomRunnables);
            }
        }
    };

    private Runnable replanRunnable = defaultReplanRunnable;
    private static final boolean DEFAULT_DEBUG = Boolean.getBoolean("ExecutorJPanel.debug");

    private boolean debug = DEFAULT_DEBUG;

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
    private void setDebug(boolean debug) {
        this.debug = debug;
        this.crclGenerator.setDebug(debug);
    }

    private volatile boolean runningProgram = false;

    private int currentActionIndex = -1;

    public int getCurrentActionIndex() {
        return currentActionIndex;
    }

    @SuppressWarnings("unused")
    @Override
    public void accept(PendantClientJPanel panel, int line, CRCLProgramType program, CRCLStatusType status) {
        if (this.debug && null != program) {
            int sz = program.getMiddleCommand().size();
            logDebug("replanStarted = " + replanStarted);
            logDebug("replanRunnable = " + replanRunnable);
            logDebug("jCheckBoxReplan.isSelected() = " + replanCachedCheckBox.isSelected());
            logDebug("sz = " + sz);
            logDebug("line = " + line);
            CommandStateEnumType state = status.getCommandStatus().getCommandState();

            logDebug("state = " + state);
            logDebug("crclProgName = " + crclProgName);
            logDebug("lastCrclProgName = " + lastCrclProgName);
            logDebug("program.getName() = " + program.getName());
        }
    }

    /**
     * @return the errorMap
     */
    private List<PositionMap> getPositionMaps() {
        return positionMapJPanel1.getPositionMaps();
    }

    private List<PositionMap> getReversePositionMaps() {
        return positionMapJPanel1.getReversePositionMaps();
    }

    @Nullable
    private volatile List<PhysicalItem> availableToolHolders = null;

    public List<PhysicalItem> getAvailableToolHolders() {
        if (null == availableToolHolders) {
            Map<String, PoseType> toolHolderPoseMap
                    = crclGenerator.getToolHolderPoseMap();
            List<PhysicalItem> newList = new ArrayList<>();
            for (Entry<String, PoseType> entry : toolHolderPoseMap.entrySet()) {
                newList.add(new ToolHolder(entry.getKey(), entry.getValue()));
            }
            availableToolHolders = newList;
        }
        return availableToolHolders;
    }

    public List<PhysicalItem> getToolsInHolders() {
        Map<String, PoseType> toolHolderPoseMap
                = crclGenerator.getToolHolderPoseMap();
        Map<String, String> toolHolderContentsMap
                = crclGenerator.getCurrentToolHolderContentsMap();
        List<PhysicalItem> newList = new ArrayList<>();
        for (Entry<String, PoseType> entry : toolHolderPoseMap.entrySet()) {
            String contents = toolHolderContentsMap.get(entry.getKey());
            if (null != contents && !CrclGenerator.isEmptyTool(contents)) {
                newList.add(new Tool(contents, entry.getValue()));
            }
        }
        return newList;
    }

}
