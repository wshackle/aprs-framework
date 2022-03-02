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
 * This software can be redistributed and/or modified freely provided show
 * that any derivative works bear some notice that they are derived from it,
 * and any modified versions bear some notice that they have been modified.
 *
 *  See http://www.copyright.gov/title17/92chap1.html#105
 *
 */
package aprs.supervisor.main;

import aprs.actions.executor.Action;
import aprs.actions.executor.ExecutorOption;
import aprs.actions.executor.PositionMap;
import aprs.actions.executor.PositionMapJPanel;
import aprs.cachedcomponents.CachedTable;
import aprs.database.PhysicalItem;
import aprs.database.Slot;
import aprs.launcher.ProcessLauncherJFrame;
import aprs.misc.*;
import aprs.simview.Object2DOuterJPanel;
import aprs.supervisor.screensplash.SplashScreen;
import aprs.system.AprsSystem;
import crcl.base.CRCLStatusType;
import crcl.base.CommandStateEnumType;
import crcl.base.CommandStatusType;
import crcl.ui.misc.MultiLineStringJPanel;
import crcl.utils.CRCLUtils;
import crcl.utils.XFuture;
import crcl.utils.XFutureVoid;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static aprs.misc.AprsCommonLogger.println;
import static aprs.misc.Utils.*;
import static crcl.utils.CRCLUtils.requireNonNull;

import aprs.actions.executor.ExecutorOption.ForBoolean;

/**
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("keyfor")
public class Supervisor {

    private final @MonotonicNonNull AprsSupervisorDisplayJFrame displayJFrame;

    private final long startSupervisorTime;

    private Supervisor() {
        this(null);
    }

    private void updateTestLog(int cycle_count, long timeDiff) throws IOException {
        long timeDiffPerCycle = cycle_count > 0 ? timeDiff / cycle_count : -1;

        File f = Utils.file(Utils.getAprsUserHomeDir(), "aprs_test_logs.csv");
        int disableCount = this.getTotalDisableCount();
        println("disableCount = " + disableCount);
        long disableTime = this.getTotalDisableTime();
        println("disableTime = " + disableTime);
        boolean alreadyExists = f.exists();
        long currentTotalRandomDelays = this.getTotalRandomDelays();
        println("currentTotalRandomDelays = " + currentTotalRandomDelays);
        int currentIgnoredToggles = this.getIgnoredToggles();
        println("currentIgnoredToggles = " + currentIgnoredToggles);
        Utils.saveTestLogEntry(f, alreadyExists, cycle_count, timeDiff, timeDiffPerCycle, disableCount, disableTime,
                currentTotalRandomDelays, currentIgnoredToggles);
    }

    @UIEffect
    @SuppressWarnings("guieffect")
    public XFuture<Supervisor> completePrevMulti() {
        this.startColorTextReader();
        return this.loadAllPrevFiles(null).thenSupply(() -> {
            Supervisor.this.setVisible(true);
            if (Utils.arePlayAlertsEnabled()) {
                PlayAlert();
            }
            return Supervisor.this;
        });
    }

    @SuppressWarnings("guieffect")
    public XFuture<?> multiCycleTestNoDisables(long startTime, int maxCycles, boolean useConveyor) {
        XFuture<?> completePrevMultiFuture = completePrevMulti();

        this.setShowFullScreenMessages(false);
        this.setMax_cycles(maxCycles);
        XFutureVoid startScanAllFuture;
        if (useConveyor) {
            startScanAllFuture = completePrevMultiFuture
                    .thenComposeAsyncToVoid(x -> {
                        String convTaskName = getConveyorClonedViewSystemTaskName();
                        AprsSystem sys = getSysByTask(convTaskName);
                        if (null == sys) {
                            throw new RuntimeException("getSysByTask(" + convTaskName + ") returned null");
                        }
                        sys.setAlertLimitsCheckBoxSelected(false);
                        setupSystemForConveyorTest(sys);
                        setEnableTestRandomDelayMillis(60000);
                        setEnableTestMinRandomDelayMillis(15000);
                        if (null == displayJFrame) {
                            throw new RuntimeException("displayJFrame is null");
                        }
                        return displayJFrame.conveyorTestPrep(sys);
                    }, supervisorExecutorService)
                    .thenComposeAsyncToVoid(this::startScanAll, supervisorExecutorService);
        } else {
            startScanAllFuture = completePrevMultiFuture
                    .thenComposeAsyncToVoid((x) -> Supervisor.this.startScanAll(), supervisorExecutorService);
        }
        XFuture<?> xf2 = startScanAllFuture.thenRun(() -> {
            if (!startScanAllFuture.isDone()) {
                System.err.println("wtf");
            }
//                    this.displayUpdatesDisabled = true;
        });
        XFuture<?> xf3 = xf2.thenCompose(x -> {
            if (!startScanAllFuture.isDone()) {
                System.err.println("wtf");
            }
            if (!startScanAllFuture.isDone()) {
                System.err.println("wtf");
            }
            return this.startContinuousDemoRevFirst();
        });
        XFuture<?> xf4 = xf3.alwaysRun(() -> {
            try {
                long endTime = System.currentTimeMillis();
                long timeDiff = endTime - startTime;
                int cycle_count = this.getContiousDemoCycleCount();
                if (cycle_count != maxCycles) {
                    throw new RuntimeException("cycle_count = " + cycle_count + ", maxCycles=" + maxCycles);
                }
                updateTestLog(cycle_count, timeDiff);
                if (!xf2.isDone()) {
                    System.err.println("wtf");
                }
                if (!xf3.isDone()) {
                    System.err.println("wtf");
                }
                this.displayUpdatesDisabled = false;
                Utils.printOnlyOnDispatchCallers();
                runOnDispatchThread(() -> {

                    println("timeDiff = " + timeDiff);
                    PlayAlert();
                    println();
                    println("===============================================================");
                    println();
                    String msg = String.format("Test took %.3f seconds  or %02d:%02d:%02d for %d cycles",
                            (timeDiff / 1000.0), (timeDiff / 3600000), (timeDiff / 60000) % 60,
                            ((timeDiff / 1000)) % 60,
                            cycle_count);
                    println(msg);
                    println();
                    println("===============================================================");
                    println();
                    this.close();
                    if (null != displayJFrame) {
                        displayJFrame.close();
                    }
                    CRCLUtils.systemExit(0);
                });
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
        return xf4;
    }

    private volatile @Nullable XFuture<?> lastCompletePrevMultiFuture = null;
    private volatile @Nullable XFuture<?> lastCompletePrevFlipFMFuture = null;

    private volatile @Nullable XFuture<?> lastCompleteMultiCycleTestFuture = null;

    public @Nullable
    XFuture<?> getLastCompleteMultiCycleTestFuture() {
        return lastCompleteMultiCycleTestFuture;
    }

    @SuppressWarnings("guieffect")
    public XFuture<MultiCycleResults> multiCycleTest(long startTime, int numCycles, boolean useConveyor)
            throws IOException {

        XFuture<?> completePrevMultiFuture = completePrevMulti();
        lastCompletePrevMultiFuture = completePrevMultiFuture;

        XFuture<MultiCycleResults> ret = completePrevMultiFuture
                .thenCompose(x -> completeMultiCycleTest(startTime, numCycles, useConveyor));
        lastCompleteMultiCycleTestFuture = ret;
        return ret;
    }

    /**
     * Test multiple cycles of filling and emptying kit trays. The test is
     * performed asynchronously in another thread.
     *
     * @param setupFile CSV file with tasks and robots
     * @param positionMappingsFile CSV file with other filenames for transforms
     * between robots
     * @param teachFile CSV file with positions of parts in trays to teach the
     * goal configuration
     * @param startTime start time in ms since 1970 for performance log
     * @param numCycles number of times to move parts back and forth between kit
     * trays and parts trays
     * @param useConveyor include use of the conveyor in the test
     * @return future for determining when the test is done
     * @throws IOException couldn't open one of the files or connect to one of
     * the network servers
     */
    @SuppressWarnings("guieffect")
    public XFuture<MultiCycleResults> multiCycleTest(
            File setupFile,
            File positionMappingsFile,
            File teachFile,
            long startTime,
            int numCycles,
            boolean useConveyor) throws IOException {

        String dir = setupFile.getParent();
        StackTraceElement trace[] = Thread.currentThread().getStackTrace();
        if (!CRCLUtils.isGraphicsEnvironmentHeadless()) {
            startColorTextReader();
        }
        XFutureVoid f1 = loadSetupFile(setupFile);
        XFutureVoid f2 = f1.thenRun(() -> {
            try {
                loadPositionMappingsFilesFile(positionMappingsFile);
            } catch (Exception ex) {
                Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "trace=" + trace, ex);
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                } else {
                    throw new RuntimeException(ex);
                }
            }
        });
        XFutureVoid f3 = f2.thenComposeAsyncToVoid(() -> {
            try {
                return loadTeachPropertiesFile(teachFile);
            } catch (Exception ex) {
                Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "trace=" + trace, ex);
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                } else {
                    throw new RuntimeException(ex);
                }
            }
        }, supervisorExecutorService);
        lastCompletePrevMultiFuture = f3;

        XFuture<MultiCycleResults> ret = f3
                .thenCompose(x -> completeMultiCycleTest(startTime, numCycles, useConveyor));
        lastCompleteMultiCycleTestFuture = ret;
        return ret;
    }

    /**
     * Test having Fanuc flip gear over with help of Motoman. The test is
     * performed asynchronously in another thread.
     *
     * @param setupFile CSV file with tasks and robots
     * @param positionMappingsFile CSV file with other filenames for transforms
     * between robots
     * @param startTime start time in ms since 1970 for performance log
     * @return future for determining when the test is done
     * @throws IOException couldn't open one of the files or connect to one of
     * the network servers
     */
    @SuppressWarnings("guieffect")
    public XFuture<Boolean> flipFMTest(
            File setupFile,
            File positionMappingsFile,
            long startTime,
            File fanucSimItemsFile) throws IOException {

        String dir = setupFile.getParent();
        StackTraceElement trace[] = Thread.currentThread().getStackTrace();
        if (!CRCLUtils.isGraphicsEnvironmentHeadless()) {
            startColorTextReader();
        }
        XFutureVoid f1 = loadSetupFile(setupFile);
        XFutureVoid f2 = f1.thenRun(() -> {
            try {
                loadPositionMappingsFilesFile(positionMappingsFile);
                AprsSystem fanucSys = getSysByTask("Fanuc Cart");
                if (null != fanucSys) {
                    if (fanucSys.isObjectViewSimulated()) {
                        final Object2DOuterJPanel objectViewPanel = fanucSys.getObjectViewPanel();
                        if (null != objectViewPanel) {
                            objectViewPanel.loadFile(fanucSimItemsFile);
                        }
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "trace=" + trace, ex);
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                } else {
                    throw new RuntimeException(ex);
                }
            }
        });
        lastCompletePrevFlipFMFuture = f2;

        XFuture<Boolean> ret = f2
                .thenComposeAsync(x -> connectAll(), supervisorExecutorService)
                .thenComposeAsync(x -> startFlipFM(), supervisorExecutorService);
        lastCompletePrevFlipFMFuture = ret;
        return ret;
    }

    public @Nullable
    AprsSystem getConveyorVisClonedSystem() {
        if (null == displayJFrame) {
            throw new NullPointerException("displayJFrame");
        }
        return displayJFrame.getConveyorVisClonedSystem();
    }

    public void setupSystemForConveyorTest(AprsSystem sys) {
        sys.setAlternativeForwardStartActions(() -> conveyorTestAlternativeForwardStartActions(sys));
        sys.setAlternativeForwardContinueActions(() -> conveyorTestAlternativeForwardContinueActions(sys));
        sys.setAlternativeReverseStartActions(() -> conveyorTestAlternativeReverseStartActions(sys));
        sys.setAlternativeReverseContinueActions(() -> conveyorTestAlternativeReverseContinueActions(sys));
    }

    private XFuture<Boolean> conveyorTestAlternativeReverseContinueActions(AprsSystem sys) {
        int startAbortCount = sys.getSafeAbortRequestCount();
        int startEnableChangeCount = enableChangeCount.get();
        return this.conveyorBack(sys, startAbortCount, startEnableChangeCount);
    }

    private XFuture<Boolean> conveyorTestAlternativeReverseStartActions(AprsSystem sys) {
        int startAbortCount = sys.getSafeAbortRequestCount();
        conveyorVisPrevCount.set(0);
        int startEnableChangeCount = enableChangeCount.get();
        return this.conveyorBack(sys, startAbortCount, startEnableChangeCount);
    }

    private XFuture<Boolean> conveyorTestAlternativeForwardContinueActions(AprsSystem sys) {
        int startAbortCount = sys.getSafeAbortRequestCount();
        int startEnableChangeCount = enableChangeCount.get();
        return this.conveyorForward(sys, startAbortCount, startEnableChangeCount);
    }

    private XFuture<Boolean> conveyorTestAlternativeForwardStartActions(AprsSystem sys) {
        int startAbortCount = sys.getSafeAbortRequestCount();
        conveyorVisNextCount.set(0);
        int startEnableChangeCount = enableChangeCount.get();
        return this.conveyorForward(sys, startAbortCount, startEnableChangeCount);
    }

    public static class MultiCycleResults {

        public long startTime;
        public int numCycles;
        public boolean useConveyor;
        public int stepsDone = 0;
        public int cyclesComplete;
        public long timeDiff;
        public String msg;

    }

    public XFuture<MultiCycleResults> completeMultiCycleTest(long startTime, int numCycles, boolean useConveyor) {
        XFutureVoid supervisorScanAllFuture;
        this.setShowFullScreenMessages(false);
        final MultiCycleResults results = new MultiCycleResults();
        results.startTime = startTime;
        results.numCycles = numCycles;
        results.useConveyor = useConveyor;
        this.setMax_cycles(numCycles);
        if (useConveyor) {
            if (null != displayJFrame) {
                displayJFrame.enableConveyor(true);
            }
            String convTaskName = getConveyorClonedViewSystemTaskName();
            if (null != convTaskName && convTaskName.length() > 0 && null != displayJFrame) {
                displayJFrame.setConveyorClonedViewSystemTaskName(convTaskName);
            }
            AprsSystem sys = getSysByTask(convTaskName);
            if (null == sys) {
                throw new RuntimeException("getSysByTask(" + convTaskName + ") returned null");
            }
            sys.setAlertLimitsCheckBoxSelected(false);
            setupSystemForConveyorTest(sys);
            setEnableTestRandomDelayMillis(60000);
            setEnableTestMinRandomDelayMillis(15000);
            if (null == displayJFrame) {
                throw new NullPointerException("displayJFrame");
            }
            supervisorScanAllFuture = displayJFrame.conveyorTestPrep(sys)
                    .thenComposeAsyncToVoid(convTaskName,
                            this::startScanAll,
                            supervisorExecutorService);
        } else {
            supervisorScanAllFuture = startScanAll();
        }
        XFutureVoid xf2 = supervisorScanAllFuture.thenRun(() -> {
            if (!supervisorScanAllFuture.isDone()) {
                System.err.println("wtf");
            }
            results.stepsDone++;
        });
        XFutureVoid randomTestFirstActionReversedFuture = xf2.thenComposeAsyncToVoid(x -> {
            if (!supervisorScanAllFuture.isDone()) {
                System.err.println("wtf");
            }
            if (!supervisorScanAllFuture.isDone()) {
                System.err.println("wtf");
            }
            results.stepsDone++;
            return this.startRandomTestFirstActionReversed();
        }, supervisorExecutorService);
        XFuture<?> xf4 = randomTestFirstActionReversedFuture
                .alwaysCompose(
                        () -> {
                            return multiCycleTestLastStep(supervisorScanAllFuture, xf2,
                                    randomTestFirstActionReversedFuture, numCycles, startTime, results);
                        });
        XFuture<MultiCycleResults> ret = xf4
                .thenApply(x -> results);
        return ret;
    }

    private XFuture<MultiCycleResults> multiCycleTestLastStep(XFutureVoid supervisorScanAllFuture, XFuture<?> xf2,
            XFuture<?> randomTestFirstActionReversedFuture, int numCycles, long startTime,
            final MultiCycleResults results) throws RuntimeException {
        try {
            println("supervisorScanAllFuture = " + supervisorScanAllFuture);
            if (supervisorScanAllFuture.isCompletedExceptionally()) {
                final Function<Throwable, Void> printExFunction = (Throwable throwable) -> {
                    Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, throwable);
                    return (Void) null;
                };
                supervisorScanAllFuture.exceptionally(printExFunction);
            }
            println("xf2 = " + xf2);
            println("randomTestFirstActionReversedFuture = " + randomTestFirstActionReversedFuture);
            int cycle_count = this.getContiousDemoCycleCount();
            if (cycle_count != numCycles) {
                throw new RuntimeException("cycle_count = " + cycle_count + ", numCycles=" + numCycles);
            }
            long endTime = System.currentTimeMillis();
            results.timeDiff = endTime - startTime;
            updateTestLog(cycle_count, results.timeDiff);
            if (!xf2.isDone()) {
                System.err.println("wtf");
            }
            if (!randomTestFirstActionReversedFuture.isDone()) {
                System.err.println("wtf");
            }
            Utils.printOnlyOnDispatchCallers();
            results.stepsDone++;
            results.cyclesComplete = cycle_count;
            XFutureVoid dispatchFuture = runOnDispatchThread(() -> {

                println("timeDiff = " + results.timeDiff);
                PlayAlert();
                println();
                println("===============================================================");
                println();
                results.msg = String.format("Test took %.3f seconds  or %02d:%02d:%02d for %d cycles",
                        (results.timeDiff / 1000.0),
                        (results.timeDiff / 3600000),
                        (results.timeDiff / 60000) % 60,
                        ((results.timeDiff / 1000)) % 60,
                        cycle_count);
                println(results.msg);
                println();
                println("===============================================================");
                println();
                this.close();
                if (null != displayJFrame) {
                    displayJFrame.close();
                }
                results.stepsDone++;

            });
            return dispatchFuture.thenApply(x -> results);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }

    private final Object2DOuterJPanel object2DOuterJPanel1;
    // private final JTable jTableRobots;
    private final CachedTable selectedPosMapFileCachedTable;
    private final CachedTable positionMappingsFilesCachedTable;
    private final CachedTable tasksCachedTable;
    private final CachedTable sharedToolCachedTable;

    public ConcurrentHashMap<String, AprsSystem> getSlotProvidersMap() {
        return slotProvidersMap;
    }

    private SlotOffsetProvider getSlotOffsetProvider() {
        return slotOffsetProvider;
    }

    @UIEffect
    public void syncToolsFromRobots() {
        sharedToolCachedTable.setRowCount(0);
        for (AprsSystem sys : aprsSystems) {
            String sysName = sys.getTaskName();
            sharedToolCachedTable.addRow(new String[]{sysName, "Current", sys.getSelectedToolName(),
                sys.getPossibleToolNames().toString(), ""});
            Map<String, String> toolHolderContentsMap = sys.getCurrentToolHolderContentsMap();
            Map<String, Set<String>> possibleToolHolderContentsMap = sys.getPossibleToolHolderContentsMap();
            for (String holderName : toolHolderContentsMap.keySet()) {
                String contents = toolHolderContentsMap.get(holderName);
                Set<String> possibleContentsMap = possibleToolHolderContentsMap.get(holderName);
                String possibleContentsString = (possibleContentsMap != null) ? possibleContentsMap.toString() : "";
                sharedToolCachedTable
                        .addRow(new String[]{sysName, holderName, contents, possibleContentsString, ""});
            }
            sys.addSelectedToolNameListener((String tool) -> updateSharedToolsTable(sysName, "Current", tool));
            sys.addToolHolderContentsListener(
                    (String holder, String tool) -> updateSharedToolsTable(sysName, holder, tool));
        }
    }

    private void updateSharedToolsTable(String sysName, String holder, String tool) {
        runOnDispatchThread(() -> updateSharedToolsTableInternal(sysName, holder, tool));
    }

    private void enableSharedToolTableModelListener() {
        if (null != displayJFrame) {
            displayJFrame.enableSharedToolTableModelListener();
        }
    }

    public boolean isSharedToolTableModelListenerEnabled() {
        if (null != displayJFrame) {
            return displayJFrame.isSharedToolTableModelListenerEnabled();
        }
        return false;
    }

    public void disableSharedToolTableModelListener() {
        if (null != displayJFrame) {
            displayJFrame.disableSharedToolTableModelListener();
        }
    }

    @UIEffect
    private synchronized void updateSharedToolsTableInternal(String sysName, String holder, String tool) {
        boolean sharedToolTableModelListenerEnabled = isSharedToolTableModelListenerEnabled();
        try {
            disableSharedToolTableModelListener();
            for (int i = 0; i < sharedToolCachedTable.getRowCount(); i++) {
                Object valuei0 = sharedToolCachedTable.getValueAt(i, 0);
                if (!(valuei0 instanceof String)) {
                    throw new IllegalStateException("sharedToolStable(" + i + ",0) contains :" + valuei0);
                }
                Object valuei1 = sharedToolCachedTable.getValueAt(i, 1);
                if (!(valuei1 instanceof String)) {
                    throw new IllegalStateException("sharedToolStable(" + i + ",1) contains :" + valuei1);
                }
                String sysNameFromTable = (String) valuei0;
                boolean namesEqual = sysNameFromTable.equals(sysName);
                if (!namesEqual && "Current".equals(holder)) {
                    continue;
                }
                String holderFromTable = (String) valuei1;
                if (!holderFromTable.equals(holder)) {
                    continue;
                }
                String origTool = (String) sharedToolCachedTable.getValueAt(i, 2);
                if (!tool.equals(origTool)) {
                    sharedToolCachedTable.setValueAt(tool, i, 2);
                    if (!namesEqual) {
                        for (AprsSystem sys : aprsSystems) {
                            if (sys.getTaskName().equals(sysNameFromTable)) {
                                sys.putInToolHolderContentsMap(holder, tool);
                            }
                        }
                    }
                }
            }
        } finally {
            if (sharedToolTableModelListenerEnabled) {
                enableSharedToolTableModelListener();
            }
        }
    }

    /**
     * Creates new form AprsMulitSupervisorJFrame
     */
    @SuppressWarnings({"nullness", "initialization"})
    private Supervisor(@Nullable AprsSupervisorDisplayJFrame displayJFrame) {
        this.startSupervisorTime = System.currentTimeMillis();
        this.displayJFrame = displayJFrame;

        this.sharedToolCachedTable = (displayJFrame != null) ? new CachedTable(displayJFrame.getSharedToolsTable())
                : newSharedToolsTable();

        this.object2DOuterJPanel1 = (displayJFrame != null) ? displayJFrame.getObject2DOuterJPanel1()
                : new Object2DOuterJPanel();
        this.positionMappingsFilesCachedTable = (displayJFrame != null)
                ? new CachedTable(displayJFrame.getPositionMappingsFilesTable())
                : newPositionMappingsTable();

        this.tasksCachedTable = (displayJFrame != null) ? new CachedTable(displayJFrame.getTasksTable())
                : newTasksTable();

        this.selectedPosMapFileCachedTable = (displayJFrame != null)
                ? new CachedTable(displayJFrame.getSelectedPosMapFileTable())
                : newSelectedPosMapFileTable();

        AtomicInteger newWaitForTogglesFutureCount = new AtomicInteger();
        waitForTogglesFutureCount = newWaitForTogglesFutureCount;
        ConcurrentLinkedDeque<XFutureVoid> newWaitForTogglesFutures = new ConcurrentLinkedDeque<>();
        waitForTogglesFutures = newWaitForTogglesFutures;
        togglesAllowedXfuture = new AtomicReference<>(
                createFirstWaitForTogglesFuture(newWaitForTogglesFutures, newWaitForTogglesFutureCount));
        if (null != displayJFrame) {
            displayJFrame.setRobotTaskMap(robotTaskMap);
            displayJFrame.setRecordLiveImageMovieSelected(recordLiveImageMovieSelected);
            object2DOuterJPanel1.setSlotOffsetProvider(this.getSlotOffsetProvider());
        }
    }

    @SuppressWarnings("nullness")
    private static CachedTable newPositionMappingsTable() {
        try {
            File r1r2CsvFile = Utils.file("R1R2.csv");
            Object[][] data = new Object[][]{
                {"System", "Robot1", "Robot2"},
                {"Robot1", null, r1r2CsvFile},
                {"Robot2", r1r2CsvFile, null}};
            return new CachedTable(
                    data,
                    new Class<?>[]{String.class, String.class, String.class},
                    new String[]{"", "", ""});
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }

    public @Nullable
    XFuture<?> getLFR() {
        return lastFutureReturned;
    }

    private synchronized @Nullable
    XFuture<?> getLastFutureReturned() {
        if (this.supervisorThread != Thread.currentThread()) {
            throw new RuntimeException("called from wrong thread =" + Thread.currentThread());
        }
        return lastFutureReturned;
    }

    @SuppressWarnings("serial")
    private static class PositionMappingTableModel extends DefaultTableModel {

        @SuppressWarnings({"rawtypes", "nullness"})
        public PositionMappingTableModel(@Nullable Object[][] data, Object[] columnNames) {
            super(data, columnNames);
        }
    }

    @SuppressWarnings({"rawtypes", "nullness"})
    public static TableModel defaultPositionMappingsModel() {
        try {
            File r1r2CsvFile = Utils.file("R1R2.csv");
            Object[][] data = new Object[][]{
                {"System", "Robot1", "Robot2"},
                {"Robot1", null, r1r2CsvFile},
                {"Robot2", r1r2CsvFile, null}
            };
            return new PositionMappingTableModel(
                    data,
                    new Object[]{"", "", ""});
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static CachedTable newTasksTable() {
        return new CachedTable(new Object[][]{},
                new Class[]{java.lang.Integer.class, java.lang.String.class, java.lang.String.class,
                    java.lang.Object.class, java.lang.Object.class, java.lang.String.class,
                    java.lang.String.class},
                new String[]{"Priority", "Task(s)", "Robot(s)", "Scan Image", "Live Image", "Details",
                    "PropertiesFile"});
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static CachedTable newSelectedPosMapFileTable() {
        return new CachedTable(new Object[][]{},
                new Class[]{Double.class, Double.class, Double.class, Double.class, Double.class, Double.class,
                    Double.class, Double.class, Double.class, String.class},
                new String[]{"Xin", "Yin", "Zin", "Xout", "Yout", "Zout", "Offset_X", "Offset_Y", "Offset_Z",
                    "Label"});
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static CachedTable newSharedToolsTable() {
        return new CachedTable(new Object[][]{},
                new Class[]{java.lang.String.class, java.lang.String.class, java.lang.String.class,
                    java.lang.String.class, java.lang.String.class},
                new String[]{"Robot", "Tools", "Holders", "Aborting Robots", "Comment"});
    }

    private final ConcurrentHashMap<String, AprsSystem> slotProvidersMap = new ConcurrentHashMap<>();

    public boolean isDebug() {
        return debug;
    }

    void setDebug(boolean debug) {
        this.debug = debug;
    }

    boolean isTogglesAllowed() {
        return togglesAllowed && toggleBlockerMap.isEmpty();
    }

    public void setTogglesAllowed(boolean togglesAllowed) {
        this.togglesAllowed = togglesAllowed;
    }

    private class AprsSupervisorSlotOffsetProvider implements SlotOffsetProvider {

        /**
         * Get a list of slots with names and relative position offsets for a
         * given kit or parts tray name.
         *
         * @param name name of the type of kit or slot tray
         * @param ignoreEmpty if false no slots being found logs a verbose error
         * message and throws IllegalStateException (good for fail fast) or if
         * true simply returns an empty list (good or display or when multiple
         * will be checked.
         * @return list of slots with relative position offsets.
         */
        @SuppressWarnings("WeakerAccess")
        @Override
        public List<Slot> getSlotOffsets(String name, boolean ignoreEmpty) {
            for (AprsSystem aprsSystem : aprsSystems) {
                try {
                    AprsSystem sys = aprsSystem;
                    List<Slot> l = sys.getSlotOffsets(name, true);
                    if (null != l && !l.isEmpty()) {
                        slotProvidersMap.put(name, sys);
                        return l;
                    }
                } catch (IllegalStateException e) {
                    // ignoring trays that can't be found, must be for another system.
                }
            }
            return Collections.emptyList();
        }

        @Override
        public Slot absSlotFromTrayAndOffset(PhysicalItem tray, Slot offsetItem) {
            return absSlotFromTrayAndOffset(tray, offsetItem, 0);
        }

        @Override
        public Slot absSlotFromTrayAndOffset(PhysicalItem tray, Slot offsetItem, double rotationOffset) {
            AprsSystem sys = slotProvidersMap.get(tray.origName);
            if (null == sys) {
                throw new NullPointerException("lotProvidersMap.get(" + tray.origName + ") slotProvidersMap.keys()= "
                        + slotProvidersMap.keys());
            }
            return sys.absSlotFromTrayAndOffset(tray, offsetItem, rotationOffset);
        }
    }

    private final AprsSupervisorSlotOffsetProvider slotOffsetProvider = new AprsSupervisorSlotOffsetProvider();

    /**
     * Start a reader so that the text and color of the panels at the bottom
     * right showing the status of the robots can be remotely controlled through
     * a simple socket.
     */
    public void startColorTextReader() {
        if (null != displayJFrame) {
            displayJFrame.startColorTextReader();
        }
    }

    private void stopColorTextReader() {
        if (null != displayJFrame) {
            displayJFrame.stopColorTextReader();
        }
    }

    /**
     * Get the location of the last CSV Setup file used.
     *
     * @param dirName optional directory name to look for file in
     * @return setup file location
     * @throws IOException setup files location can not be read
     */
    public static @Nullable
    File getLastSetupFile(@Nullable String dirName) throws IOException {
        return readPathFromFileFile(LAST_SETUP_FILE_FILE, dirName);
    }

    /**
     * Get the location of the last CSV Setup file used.
     *
     * @param dirName optional directory to check first for path storing file.
     * @return setup file location
     * @throws IOException setup files location can not be read
     */
    public static @Nullable
    File getLastSharedToolsFile(@Nullable String dirName) throws IOException {
        return readPathFromFileFile(LAST_SHARED_TOOLS_FILE_FILE, dirName);
    }

    /**
     * Get the location of the last simulated teach file used. The CSV file
     * contains the name, type and position of objects which can be used to
     * create action lists to fill kits in a similar manner.
     *
     * @return last simulate file location
     * @throws IOException file location can not be read
     */
    static @Nullable
    File getStaticLastSimTeachFile(@Nullable String dirName) throws IOException {
        return readPathFromFileFile(LAST_SIM_TEACH_FILE_FILE, dirName);
    }

    /**
     * Get the location of the last simulated teach file used. The CSV file
     * contains the name, type and position of objects which can be used to
     * create action lists to fill kits in a similar manner.
     *
     * @return last simulate file location
     * @throws IOException file location can not be read
     */
    @Nullable
    File getLastSimTeachFile() throws IOException {
        File f = getLocalLastSimTeachFile();
        if (null != f) {
            return f;
        } else {
            return Supervisor.getStaticLastSimTeachFile(null);
        }
    }

    private @Nullable
    File getLocalLastSimTeachFile() throws IOException {
        if (null == setupFile || null == setupFile.getParentFile()) {
            return null;
        }
        return readRelativePathFromFileFile(Utils.file(setupFile.getParentFile(), LAST_APRS_SIM_TEACH_FILETXT));
    }

    /**
     * Get the location of the last teach properties file used. The properties
     * file contains settings on how to display the teach objects.
     *
     * @return last teach properties file
     * @throws IOException file location can not be read
     */
    static @Nullable
    File getLastTeachPropertiesFile(@Nullable String dirName) throws IOException {
        return readPathFromFileFile(LAST_SIM_TEACH_PROPERTIES_FILE_FILE, dirName);
    }

    private static @Nullable
    File readPathFromFileFile(File fileFile, @Nullable String dirName) throws IOException {
        try {
            if (null != dirName && dirName.length() > 0) {
                File dir = Utils.file(dirName);
                if (dir.exists()) {
                    File f0 = Utils.file(dir, fileFile.getName());
                    if (f0.exists()) {
                        File f1 = readRelativePathFromFileFile(f0);
                        if (f1 != null && f1.exists()) {
                            return f1;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "", ex);
        }
        if (fileFile.exists()) {
            String firstLine = readFirstLine(fileFile);
            if (null != firstLine && firstLine.length() > 0) {
                return Utils.file(firstLine);
            }
        }
        return null;
    }

    private static @Nullable
    File readRelativePathFromFileFile(File fileFile) throws IOException {
        if (fileFile.exists()) {
            String firstLine = readFirstLine(fileFile);
            if (null != firstLine && firstLine.length() > 0) {
                return Utils.file(fileFile.getParentFile(), firstLine.trim());
            }
        }
        return null;
    }

    /**
     * Get the location of the last posmap file. The posmap file is a CSV file
     * that points to other csv files with infomation needed to transform
     * coordinates from one robot to another.
     *
     * @return last posmap file location
     * @throws IOException file location can not be read
     */
    static @Nullable
    File getLastPositionMappingsFilesFile(@Nullable String dirName) throws IOException {
        return readPathFromFileFile(LAST_POSMAP_FILE_FILE, dirName);
    }

    public XFutureVoid loadAllPrevFiles(@Nullable String dirName) {
        return this.startLoadPrevSetup().thenComposeAsyncToVoid(() -> {
            this.loadPrevPositionMappingsFilesFile(dirName);
            this.loadPrevSimTeach();
            return this.loadPrevTeachProperties(dirName);
        }, supervisorExecutorService);
//        this.loadPrevSharedTools();
    }

    /**
     * Reload the last saved/used setup.
     *
     * @return an XFutureVoid for determining when the previous setup has been
     * loaded.
     */
    public XFutureVoid startLoadPrevSetup() {
        try {
            File readPrevSetupFile = getLastSetupFile(null);
            if (null != readPrevSetupFile && readPrevSetupFile.exists()) {
                return loadSetupFile(readPrevSetupFile);
            }
            throw new IllegalStateException("readPrevSetupFile=" + readPrevSetupFile);
        } catch (Exception ex) {
            log(Level.SEVERE, "", ex);
            try {
                closeAllAprsSystems();
            } catch (Exception ex1) {
                log(Level.SEVERE, "", ex1);
            }
            throw new RuntimeException(ex);
        }
    }

    /**
     * Load the given simulated teach file. The CSV file contains the name, type
     * and position of objects which can be used to create action lists to fill
     * kits in a similar manner.
     *
     * @param f file to read
     * @throws IOException file can not be read
     */
    public void loadSimTeachFile(File f) throws IOException {
        object2DOuterJPanel1.loadFile(f);
    }

    /**
     * Load the last teach properties file. The properties file contains
     * settings on how to display the teach objects.
     *
     * @param f file to read
     * @throws IOException file location can not be read
     */
    private XFutureVoid loadTeachPropertiesFile(File f) throws IOException {
        object2DOuterJPanel1.setPropertiesFile(f);
        return runOnDispatchThread(() -> {
            try {
                object2DOuterJPanel1.loadPropertiesOnDisplay();
            } catch (Exception exception) {
                log(Level.SEVERE, "", exception);
                throw new RuntimeException(exception);
            }
        });
    }

    /**
     * Reload the last simulated teach file read/saved.
     */
    public void loadPrevSimTeach() {
        try {
            File simTeach = getLastSimTeachFile();
            if (null != simTeach && simTeach.exists()) {
                loadSimTeachFile(simTeach);
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "", ex);
            try {
                closeAllAprsSystems();
            } catch (Exception ex1) {
                log(Level.SEVERE, "", ex1);
            }
        }
    }

    /**
     * Reload the last teach properties file read/saved.
     */
    public XFutureVoid loadPrevTeachProperties(@Nullable String dirName) {
        try {
            File teachProps = getLastTeachPropertiesFile(dirName);
            if (null != teachProps && teachProps.exists()) {
                return loadTeachPropertiesFile(teachProps);
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "", ex);
            try {
                closeAllAprsSystems();
            } catch (Exception ex1) {
                log(Level.SEVERE, "", ex1);
            }
        }
        return XFutureVoid.completedFuture();
    }

    /**
     * Reload the last posmap file read/saved.
     */
    public void loadPrevPositionMappingsFilesFile(@Nullable String dirName) {
        try {
            File posMapsFilesFile = getLastPositionMappingsFilesFile(dirName);
            if (null != posMapsFilesFile && posMapsFilesFile.exists()) {
                loadPositionMappingsFilesFile(posMapsFilesFile);
            }
        } catch (IOException ex) {
            log(Level.SEVERE, "", ex);
        }
    }

    @Nullable
    AprsSystem findSystemWithRobot(String robot) {
        for (AprsSystem aj : aprsSystems) {
            String robotName = aj.getRobotName();
            if (robotName != null && robotName.equals(robot)) {
                return aj;
            }
        }
        return null;
    }

    private final List<List<PositionMapJPanel>> positionMapJPanels = new ArrayList<>();

    // private List<PositionMapJPanel> getPositionMapRow(int row) {
//		while (positionMapJPanels.size() <= row) {
//			positionMapJPanels.add(new ArrayList<>());
//		}
//		return positionMapJPanels.get(row);
//	}
//	private PositionMapJPanel getPositionMap(int row, int col) {
//		List<PositionMapJPanel> lrow = getPositionMapRow(row);
//		while (lrow.size() <= col) {
//			lrow.add(new PositionMapJPanel());
//		}
//		return lrow.get(col);
//	}
    private final AtomicReference<@Nullable XFutureVoid> stealRobotFuture = new AtomicReference<>(null);
    private final AtomicReference<@Nullable XFutureVoid> unStealRobotFuture = new AtomicReference<>(null);
    private final AtomicReference<@Nullable XFutureVoid> cancelStealRobotFuture = new AtomicReference<>(null);
    private final AtomicReference<@Nullable XFutureVoid> cancelUnStealRobotFuture = new AtomicReference<>(null);

    private final AtomicInteger checkLastReturnedFutureCount = new AtomicInteger();

    @SuppressWarnings("keyfor")
    synchronized private XFuture<?> checkLastReturnedFuture(@Nullable XFuture<?> inFuture,
            @Nullable String blockerName) {
        if (this.supervisorThread != Thread.currentThread()) {
            throw new RuntimeException("called from wrong thread =" + Thread.currentThread());
        }
        final XFuture<?> lfr = this.getLastFutureReturned();
        if (null != lfr && lfr != inFuture && !lfr.isDone()) {
            int count = checkLastReturnedFutureCount.incrementAndGet();
            final String info = "checkLastReturnedFuture : count=" + count + ", lfr=" + lfr.getName() + ",ecc="
                    + enableChangeCount.get() + ",blocker=" + blockerName;
            try {
                takeAllSnapshots("START_" + info);
            } catch (IOException ex) {
                Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
            }
            return lfr.thenComposeAsync("checkLastReturnedFuture" + count + "_" + lfr.getName(),
                    x -> checkLastReturnedFuture(lfr, blockerName), supervisorExecutorService).thenRun(() -> {
                        try {
                            takeAllSnapshots("END_" + info);
                        } catch (IOException ex) {
                            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    });
        } else {
            int count = checkLastReturnedFutureCount.get();
            final String lfrname = (null != lfr) ? lfr.getName() : "null";
            try {
                takeAllSnapshots("SKIP checkLastReturnedFuture : count=" + count + ",lfr=" + lfrname + ",blockerName="
                        + blockerName);
            } catch (IOException ex) {
                Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
            }
            clearLFR();
            if (null != blockerName && blockerName.length() > 0) {
                return disallowToggles(blockerName);
            }
            return XFuture.completedFutureWithName("checkLastReturnedFuture2", null);
        }
    }

    private synchronized void clearLFR() {
        XFuture<?> oldLFr = this.getLastFutureReturned();
        if (null != oldLFr) {
            logEvent("clearLFR called from  " + Thread.currentThread().getStackTrace()[2] + ", oldLFr=" + oldLFr);
            this.lastFutureReturned = null;
            if (!oldLFr.isDone()) {
                oldLFr.cancelAll(false);
                throw new IllegalStateException("oldLfr=" + oldLFr);
            }
        }
    }

    private synchronized void setLastFunctionReturned(XFuture<?> newLFR) {
        XFuture<?> oldLFr = this.lastFutureReturned;
        XFuture<?> nextLFR;
        logEvent("setLFR called  with  newLFr=" + newLFR + ", oldLFr=" + oldLFr + " from  "
                + Thread.currentThread().getStackTrace()[2]);
        if (null != newLFR) {
            oldLfrs.add(newLFR);
            if (null == oldLFr || oldLFr.isDone()) {
                nextLFR = newLFR;
            } else {
                nextLFR = oldLFr.thenCompose(x -> newLFR);
                logEvent("setLFR called  with  nextLFR=" + nextLFR);
            }
        } else {
            nextLFR = newLFR;
        }
        this.lastFutureReturned = nextLFR;
    }

    private final List<XFuture<?>> oldLfrs = new ArrayList<>();

    public List<XFuture<?>> getOldLfrs() {
        return oldLfrs;
    }

    private final ConcurrentHashMap<String, Integer> robotEnableCountMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Integer> robotDisableCountMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Long> robotDisableStartMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Long> robotDisableTotalTimeMap = new ConcurrentHashMap<>();

    public int getRobotDisableCount(String robotName) {
        return robotDisableCountMap.getOrDefault(robotName, 0);
    }

    public long getRobotDisableTotalTime(String robotName) {
        return robotDisableTotalTimeMap.getOrDefault(robotName, 0L);
    }

    private final AtomicInteger enableChangeCount = new AtomicInteger();

    private int getAndIncrementEnableCount(String robotName, boolean enabled) {
        try {
            long time = System.currentTimeMillis();
            if (enabled) {
                robotDisableTotalTimeMap.compute(robotName, (k, v) -> {
                    long diff = time - robotDisableStartMap.getOrDefault(robotName, time);
                    if (null == v) {
                        return diff;
                    } else {
                        return v + diff;
                    }
                });
                return robotEnableCountMap.compute(robotName, (k, v) -> {
                    if (null == v) {
                        return 1;
                    } else {
                        return v + 1;
                    }
                });
            } else {
                robotDisableStartMap.put(robotName, time);
                return robotDisableCountMap.compute(robotName, (k, v) -> {
                    if (null == v) {
                        return 1;
                    } else {
                        return v + 1;
                    }
                });
            }
        } catch (Exception e) {
            log(Level.SEVERE, "", e);
            if (e instanceof RuntimeException) {
                throw ((RuntimeException) e);
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    public long getTotalDisableTime() {
        return robotDisableTotalTimeMap.values().stream().mapToLong(x -> x).sum();
    }

    public int getTotalDisableCount() {
        return robotDisableCountMap.values().stream().mapToInt(x -> x).sum();
    }

    private final AtomicInteger skippedRobotEnabledCount = new AtomicInteger();

    synchronized void setRobotEnabled(String robotName, Boolean enabled, @Nullable XFutureVoid futureToComplete) {
        try {
            if (closing) {
                return;
            }
            if (null == robotName) {
                throw new IllegalArgumentException("robotName=" + robotName);
            }
            if (null == enabled) {
                throw new IllegalArgumentException("robotName=" + enabled);
            }
            if (enabled != stealingRobots && !keepDisabled) {
                System.out.println("");
                System.out.flush();
                System.err.println("");
                System.err.flush();
                Thread.dumpStack();
                logEvent("SKIPPING setRobotEnabled(" + robotName + "," + enabled + ") : stealingRobots= "
                        + stealingRobots + ", enableChangeCount.get()=" + enableChangeCount.get()
                        + ",stealRobotNumber.get()=" + stealRobotNumber.get());
                logEvent("setRobotEnabled: futureToComplete=" + futureToComplete);
                if (null != futureToComplete) {
                    futureToComplete.complete();
                }
                if (!stealingRobots) {
                    println("clearStealingRobotsFlagTrace = " + Utils.traceToString(clearStealingRobotsFlagTrace));
                } else {
                    println("setStealingRobotsFlagTrace = " + Utils.traceToString(setStealingRobotsFlagTrace));
                }
                println("lastReturnRobots1Comment=" + lastReturnRobots1Comment);
                println("lastReturnRobots1Trace=" + Utils.traceToString(lastReturnRobots1Trace));
                println("displayJFrame=" + displayJFrame);
                if (null != displayJFrame) {
                    println("set01TrueTrace=" + Utils.traceToString(displayJFrame.getSet01TrueTrace()));
                    println("set01FalseTrace=" + Utils.traceToString(displayJFrame.getSet01FalseTrace()));
                }
                try {
                    if (null != futureToComplete) {
                        futureToComplete.cancelAll(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                pause();
                System.out.println("");
                System.out.flush();
                System.err.println("");
                System.err.flush();
                throw new RuntimeException("enabled != stealingRobots");
            }
            final int ecc = enableChangeCount.incrementAndGet();

            long time = System.currentTimeMillis();
            XFuture<?> prevLFR = this.getLastFutureReturned();
            XFuture<?> stealUnstealFuture;
            final int count = getAndIncrementEnableCount(robotName, enabled);
            final String timeDiffString = runTimeToString(time - robotDisableStartMap.getOrDefault(robotName, time));
            final String totalTimeString = runTimeToString(robotDisableTotalTimeMap.getOrDefault(robotName, 0L));
            final String info = "setEnabled(" + robotName + "," + enabled + ") : ecc=" + ecc + ", count =" + count
                    + ", diff=" + timeDiffString + ", totalTime=" + totalTimeString + ",prevLFR=" + prevLFR
                    + ", toggleBlockerMap.keySet()=" + toggleBlockerMap.keySet();
            takeAllSnapshots("START " + info);
            final XFutureVoid origUnstealFuture = unStealRobotFuture.getAndSet(null);
            final XFutureVoid origCancelUnstealFuture = cancelUnStealRobotFuture.getAndSet(null);
            final XFutureVoid origStealFuture = stealRobotFuture.getAndSet(null);
            final XFutureVoid origCancelStealFuture = cancelStealRobotFuture.getAndSet(null);
//            if (enabled) {
//                for (AprsSystem sys : aprsSystems) {
//                    if (sys.getOrigRobotName().equals(robotName)) {
//                        robotTaskMap.put(robotName, sys.getTaskName());
//                        break;
//                    }
//                }
//                String newTask = robotTaskMap.get(robotName);
//                String swappedRobotName = null;
//                for (Entry<String, String> entry : robotTaskMap.entrySet()) {
//                    if (entry.getValue().equals(newTask) && !entry.getKey().equals(robotName)) {
//                        swappedRobotName = entry.getKey();
//                        break;
//                    }
//                }
//                if (null != swappedRobotName) {
//                    for (AprsSystem sys : aprsSystems) {
//                        if (sys.getOrigRobotName().equals(swappedRobotName)) {
//                            robotTaskMap.put(swappedRobotName, sys.getTaskName());
//                            break;
//                        }
//                    }
//                }
//            } else {
//                robotTaskMap.remove(robotName);
//            }
//            refreshRobotsTable();
            if (!enabled) {
                final XFutureVoid srfCopy = stealRobotFuture.get();
                if (stealingRobots || srfCopy != null) {
                    logEvent("SKIPPING setEnabled stealingRobots=" + stealingRobots + ", stealRobotFuture.get()="
                            + srfCopy + ",origStealFuture=" + origStealFuture);
                    if (null != futureToComplete) {
                        futureToComplete.complete();
                    }
                    return;
                } else {
                    stealUnstealFuture = startSetRobotEnabledFalse(robotName, ecc);
                }
            } else {
                final XFutureVoid usrfCopy = unStealRobotFuture.get();
                if (!stealingRobots || usrfCopy != null) {
                    logEvent("SKIPPING setEnabled stealingRobots=" + stealingRobots + ", unStealRobotFuture.get()="
                            + usrfCopy + ",origUnstealFuture=" + origUnstealFuture);
                    if (null != futureToComplete) {
                        futureToComplete.complete();
                    }
                    return;
                } else {
                    clearStealingRobotsFlag();
                    stealUnstealFuture = startSetRobotEnabledTrue(robotName, ecc);
                }
            }
            addStealUnstealList(stealUnstealFuture);
            XFuture<?> nextLFR = stealUnstealFuture.peekNoCancelException(this::handleXFutureException)
                    .alwaysRun("END:ecc=" + ecc + ",enabled=" + enabled, () -> {
                        try {
                            takeAllSnapshots("END " + info);
                        } catch (IOException ex) {
                            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        logEvent("setRobotEnabled: futureToComplete=" + futureToComplete);
                        if (null != futureToComplete) {
                            futureToComplete.complete();
                        }
                    });
            if (null == futureToComplete) {
                XFuture<?> newLastFutureReturned;
                if (null != prevLFR && !prevLFR.isDone()) {
                    logEvent("composing prefLFR=" + prevLFR + ", with nextLFR=" + nextLFR);
                    newLastFutureReturned = prevLFR
                            .thenCompose("ecc=" + ecc + ":prevLFR.thenCompose" + nextLFR.getName(), x -> nextLFR);
                } else {
                    newLastFutureReturned = nextLFR;
                }
                setLastFunctionReturned(newLastFutureReturned);
            }
            XFutureVoid.runAsync("completeAndCancelOrigFutures", () -> {
                if (null != origCancelUnstealFuture && !origCancelUnstealFuture.isDone()) {
                    logEvent("Completing origUnstealFuture = " + origUnstealFuture);
                    origCancelUnstealFuture.complete();
                }
                if (null != origCancelStealFuture && !origCancelStealFuture.isDone()) {
                    logEvent("Completing origUnstealFuture = " + origCancelStealFuture);
                    origCancelStealFuture.complete();
                }
                logEvent(" \t Completed or cancelled origFutures for from setEnabled(" + robotName + "," + enabled
                        + ") : ecc=" + ecc + ", count =" + count + ", diff=" + timeDiffString + ", totalTime="
                        + totalTimeString + ",prevLFR=" + prevLFR + ", toggleBlockerMap.keySet()="
                        + toggleBlockerMap.keySet());
            }, supervisorExecutorService);
        } catch (Exception e) {
            log(Level.SEVERE, "", e);
            logEventErr(e.getMessage());
            logEvent("setRobotEnabled: futureToComplete=" + futureToComplete);
            if (null != futureToComplete) {
                futureToComplete.complete();
            }
            if (e instanceof RuntimeException) {
                throw ((RuntimeException) e);
            } else {
                throw new RuntimeException(e);
            }
        }

    }

    private volatile StackTraceElement clearStealingRobotsFlagTrace@Nullable []  = null;

    private synchronized void clearStealingRobotsFlag() {
        boolean oldFlag = stealingRobots;
        stealingRobots = false;
        clearStealingRobotsFlagTrace = Thread.currentThread().getStackTrace();
        logEvent("clearStealingRobotsFlag() : oldFlag=" + oldFlag);
    }

    private XFuture<?> startSetRobotEnabledTrue(String robotName, final int ecc) throws IllegalStateException {
        boolean enabled = true;
        XFuture<?> nextLFR;

        final XFutureVoid beginUnstealFuture = unStealRobots(ecc);
        if (null == beginUnstealFuture) {
            throw new IllegalStateException("unstealRobots() returned null");
        }
        addToAllFuturesSet(beginUnstealFuture);
        logEvent("unStealRobotFuture set to " + beginUnstealFuture);
        this.unStealRobotFuture.set(beginUnstealFuture);
        final XFutureVoid cancelFuture = new XFutureVoid("cancelUnStealRobotFuture");
        if (!this.cancelUnStealRobotFuture.compareAndSet(null, cancelFuture)) {
            throw new IllegalStateException("cancelUnStealRobotFuture already set.");
        }
        final XFutureVoid completeUnstealFuture = beginUnstealFuture
                .handle("setRobotEnabled(" + robotName + "," + enabled + ").handle1", (Void x, Throwable t) -> {
                    if (t == null) {
                        return "";
                    } else {
                        if (!(t instanceof CancellationException) && !(t.getCause() instanceof CancellationException)) {
                            logEvent(t.toString());
                            log(Level.SEVERE, "", t);
                            setAbortTimeCurrent();
                            pause();
                            MultiLineStringJPanel.showText(t.toString());
                        }
                        return t.toString();
                    }
                }).thenComposeAsyncToVoid("setRobotEnabled(" + robotName + "," + enabled + ").handle2",
                x -> {
                    logEvent("completeUnstealFuture:  setEnabled(" + robotName + "," + enabled + ") : ecc=" + ecc
                            + ",x=" + x);
                    if (x == null || x.length() < 1) {
                        return XFutureVoid.completedFutureWithName(
                                "setRobotEnabled(" + robotName + "," + enabled + ").completedFuture2");
                    } else {
                        logEvent("Returning xfuture which will never complete x=\"" + x + "\"");
                        if (!x.contains("CancellationException")) {
                            Thread.dumpStack();
                        }
                        return new XFuture<>(x + ".neverComplete");
                    }
                },
                supervisorExecutorService);
        nextLFR = XFuture.anyOfWithName("setRobotEnabled(" + robotName + "," + enabled + ").anyOf(unsteal,cancel)",
                completeUnstealFuture, cancelFuture);
        return nextLFR;
    }

    private boolean blockRobotTransfers = false;

    /**
     * Get the value of blockRobotTransfers
     *
     * @return the value of blockRobotTransfers
     */
    public boolean isBlockRobotTransfers() {
        return blockRobotTransfers;
    }

    /**
     * Set the value of blockRobotTransfers
     *
     * @param blockRobotTransfers new value of blockRobotTransfers
     */
    public void setBlockRobotTransfers(boolean blockRobotTransfers) {
        this.blockRobotTransfers = blockRobotTransfers;
    }

    private XFuture<?> startSetRobotEnabledFalse(String robotName, final int ecc)
            throws IllegalStateException, PositionMap.BadErrorMapFormatException, IOException {
        XFuture<?> nextLFR;
        boolean enabled = false;

        if (blockRobotTransfers) {
            return disableRobotNoTransfer(robotName, ecc);
        }
        final XFutureVoid stealRobotFuture = stealRobot(robotName, ecc);
        if (null == stealRobotFuture) {
            logEventErr(" stealRobot(" + robotName + ") returned null");
            XFutureVoid future2 = stealRobot(robotName, ecc);
            throw new IllegalStateException("stealRobot(" + robotName + ") returned null");
        }
        logEvent("stealRobotFuture set to " + stealRobotFuture);
        this.stealRobotFuture.set(stealRobotFuture);
        final XFutureVoid cancelFuture = new XFutureVoid("cancelStealRobotFuture");
        if (!this.cancelStealRobotFuture.compareAndSet(null, cancelFuture)) {
            throw new IllegalStateException("cancelStealRobotFuture already set.");
        }
        final XFuture<String> handledStealRobotFuture = stealRobotFuture.handle(
                "setRobotEnabled(" + robotName + "," + enabled + ").anyOf(steal,cancel).handle:ecc=" + ecc,
                (Void x, Throwable t) -> {
                    if (t != null) {
                        if (!(t instanceof CancellationException) && !(t.getCause() instanceof CancellationException)) {
                            log(Level.SEVERE, "", t);
                            logEvent(t.toString());
                            setAbortTimeCurrent();
                            pause();
                            MultiLineStringJPanel.showText(t.toString());
                        }
                        return t.toString();
                    } else {
                        return "";
                    }
                });
        final XFutureVoid checkedStealRobotFuture = handledStealRobotFuture
                .thenComposeAsyncToVoid(
                        "setRobotEnabled(" + robotName + "," + enabled + ").checkForExceptions:ecc=" + ecc,
                        (String x) -> {
                            logEvent("After handle setEnabled(" + robotName + "," + enabled + ") : ecc=" + ecc + ",x=" + x
                                    + ", toggleBlockerMap.keySet()" + toggleBlockerMap.keySet());
                            if (x == null || x.length() < 1) {
                                return XFutureVoid.completedFutureWithName(
                                        "setRobotEnabled(" + robotName + "," + enabled + ").alreadyComplete");
                            } else {
                                logEvent("Returning xfuture which will never complete x=\"" + x + "\"");
                                if (!x.contains("CancellationException")) {
                                    Thread.dumpStack();
                                }
                                return new XFutureVoid(x + ".neverComplete");
                            }
                        }, supervisorExecutorService);
        nextLFR = XFuture.anyOfWithName(
                "setRobotEnabled(" + robotName + "," + enabled + ").anyOf(steal,cancel):ecc=" + ecc,
                checkedStealRobotFuture, cancelFuture);
        return nextLFR;
    }

    private static class RefreshRobotsInfo {

        private final Map<String, String> robotTaskMapCopy;
        private final Map<String, Integer> robotDisableCountMapCopy;
        private final Map<String, Long> robotDisableTotalTimeMapCopy;
        final AprsSupervisorDisplayJFrame checkedDisplayJFrame;
        private final StackTraceElement callerTrace[];

        public RefreshRobotsInfo(Map<String, String> robotTaskMap, Map<String, Integer> robotDisableCountMap,
                Map<String, Long> robotDisableTotalTimeMap, AprsSupervisorDisplayJFrame checkedDisplayJFrame) {
            this.robotTaskMapCopy = Collections.unmodifiableMap(new HashMap<>(robotTaskMap));
            this.robotDisableCountMapCopy = Collections.unmodifiableMap(new HashMap<>(robotDisableCountMap));
            this.robotDisableTotalTimeMapCopy = Collections.unmodifiableMap(new HashMap<>(robotDisableTotalTimeMap));
            this.checkedDisplayJFrame = checkedDisplayJFrame;
            this.callerTrace = Thread.currentThread().getStackTrace();
        }

        public void update() {
            checkedDisplayJFrame.refreshRobotsTable(robotTaskMapCopy, robotDisableCountMapCopy,
                    robotDisableTotalTimeMapCopy, callerTrace);
        }
    }

    private static final Consumer<RefreshRobotsInfo> refreshRobotsConsumer = RefreshRobotsInfo::update;

    private XFutureVoid refreshRobotsTable() {
        checkAllUniques();
        if (null != displayJFrame) {
            RefreshRobotsInfo refreshRobotsInfo = new RefreshRobotsInfo(robotTaskMap, robotDisableCountMap,
                    robotDisableTotalTimeMap, displayJFrame);
            return submitDisplayConsumer(refreshRobotsConsumer, refreshRobotsInfo);
        } else {
            return XFutureVoid.completedFuture();
        }
    }

    void setAbortTimeCurrent() {
        abortEventTime = System.currentTimeMillis();
    }

    private final ConcurrentLinkedDeque<XFuture<?>> stealUnstealList = new ConcurrentLinkedDeque<>();

    private void addStealUnstealList(XFuture<?> f) {
        logEvent("addStealUnstealList " + f);
        stealUnstealList.add(f);
    }

    private XFutureVoid cancelledEcc(Integer ecc) {
        final String name = "cancelledEcc(" + ecc + ") enableChangeCount=" + enableChangeCount.get();
        logEvent(name);
        XFutureVoid ret = new XFutureVoid(name);
        ret.cancelAll(false);
        return ret;
    }

    private <T> XFuture<T> cancelledEcc(Integer ecc, Class<T> clazz) {
        final String name = "cancelledEcc(" + ecc + ") enableChangeCount=" + enableChangeCount.get();
        logEvent(name);
        XFuture<T> ret = new XFuture<>(name);
        ret.cancelAll(false);
        return ret;
    }

    private XFutureVoid cancelledSrn(int srn) {
        final String name = "cancelledSrn(" + srn + ") stealRobotNumber=" + stealRobotNumber.get();
        logEvent(name);
        XFutureVoid ret = new XFutureVoid(name);
        ret.cancelAll(false);
        return ret;
    }

    private <T> XFuture<T> cancelledSrn(int srn, Class<T> clazz) {
        final String name = "cancelledSrn(" + srn + ") stealRobotNumber=" + stealRobotNumber.get();
        logEvent(name);
        XFuture<T> ret = new XFuture<>(name);
        ret.cancelAll(false);
        return ret;
    }

    private XFutureVoid disableRobotNoTransfer(String robotName, int ecc)
            throws IOException, PositionMap.BadErrorMapFormatException {
        Set<String> names = new HashSet<>();
        for (int i = 0; i < aprsSystems.size() - 1; i++) {
            AprsSystem sys = aprsSystems.get(i);
            if (null != sys) {
                String sysRobotName = sys.getRobotName();
                if (null != sysRobotName) {
                    names.add(sysRobotName);
                    if (Objects.equals(sysRobotName, robotName)) {
                        if (checkEcc(ecc)) {
                            return cancelledEcc(ecc);
                        }
                        XFutureVoid f = aprsSystems.get(i).startSafeAbortAndDisconnect(
                                "disableRobotNoTransfer: robot=" + robotName + ", ecc=" + ecc).thenRun(() -> {
                                    robotTaskMapRemove(robotName);
                                });
//                        addStealUnstealList(f);
                        return f;
                    }
                }
            }
        }
        String errMsg = "Robot " + robotName + " not found in " + names;
        println("aprsSystems = " + aprsSystems);
        logEventErr(errMsg);
        showErrorSplash(errMsg);
        throw new IllegalStateException(errMsg);
//        return XFuture.completedFutureWithName("stealRobot(" + robotName + ").completedFuture", null);
    }

    private XFutureVoid stealRobot(String robotName, int ecc)
            throws IOException, PositionMap.BadErrorMapFormatException {
        Set<String> names = new HashSet<>();
        for (int i = 0; i < aprsSystems.size() - 1; i++) {
            AprsSystem sys = aprsSystems.get(i);
            if (null != sys) {
                String sysRobotName = sys.getRobotName();
                if (null != sysRobotName) {
                    names.add(sysRobotName);
                    if (Objects.equals(sysRobotName, robotName)) {
                        if (checkEcc(ecc)) {
                            return cancelledEcc(ecc);
                        }
                        XFutureVoid f = stealRobot(aprsSystems.get(i + 1), aprsSystems.get(i), ecc);
//                        addStealUnstealList(f);
                        return f;
                    }
                }
            }
        }
        String errMsg = "Robot " + robotName + " not found in " + names;
        println("aprsSystems = " + aprsSystems);
        logEventErr(errMsg);
        showErrorSplash(errMsg);
        throw new IllegalStateException(errMsg);
//        return XFuture.completedFutureWithName("stealRobot(" + robotName + ").completedFuture", null);
    }

    private boolean checkEcc(Integer ecc) {
        if (closing) {
            return true;
        }
        boolean ret = ecc != null && ecc != -1 && ecc != enableChangeCount.get();
        if (ret) {
            logEvent("checkEcc(" + ecc + ") returning true");
        }
        return ret;
    }

    final private static String transferrableOptions[] = new String[]{"rpy", "lookForXYZ", "slowTransSpeed",
        "jointAccel", "jointSpeed", "rotSpeed", "fastTransSpeed", "settleDwellTime", "lookForJoints",
        "useJointLookFor"};

    private void copyOptions(String options[], Map<ExecutorOption, ?> mapIn, Map<ExecutorOption, Object> mapOut) {
        for (String optString : options) {
            final ExecutorOption exOptOf = ExecutorOption.of(optString);
            if (mapIn.containsKey(exOptOf)) {
                mapOut.put(exOptOf, mapIn.get(exOptOf));
            }
        }
    }

    private final AtomicReference<@Nullable NamedFunction<Integer, XFutureVoid>> returnRobotFunction = new AtomicReference<>();

    @SuppressWarnings("unchecked")
    private NamedFunction<Integer, XFutureVoid> setReturnRobotRunnable(String name, Function<Integer, XFutureVoid> r,
            AprsSystem... systems) {
        NamedFunction<Integer, XFutureVoid> namedR = new NamedFunction<>(r, name, systems);
        returnRobotFunction.set(namedR);
        return namedR;
    }

    long getFirstEventTime() {
        return firstEventTime;
    }

    void setFirstEventTime(long firstEventTime) {
        this.firstEventTime = firstEventTime;
    }

    public long getAbortEventTime() {
        return abortEventTime;
    }

    void setAbortEventTime(long abortEventTime) {
        this.abortEventTime = abortEventTime;
    }

    void checkUniqueRobotPorts() {
        Set<Integer> set = new HashSet<>();
        for (AprsSystem sys : aprsSystems) {
            if (sys.isConnected()) {
                int port = sys.getRobotCrclPort();
                if (set.contains(port)) {
                    debugAction();
                    setAbortTimeCurrent();
                    pause();
                    String msg = "two systems connected to " + port;
                    logEvent(msg);
                    throw new IllegalStateException(msg);
                }
                set.add(port);
            }
        }
    }

    void checkUniqueTaskNames() {
        Set<String> set = new HashSet<>();
        for (AprsSystem sys : aprsSystems) {
            String taskName = sys.getTaskName();
            if (set.contains(taskName)) {
                debugAction();
                setAbortTimeCurrent();
                pause();
                String msg = "two systems with task name \"" + taskName + "\"";
                logEvent(msg);
                throw new IllegalStateException(msg);
            }
            set.add(taskName);
        }
    }

    void checkUniqueRobotTaskNames() {
        Set<String> set = new HashSet<>();
        for (String taskName : robotTaskMap.values()) {
            if (taskName == null) {
                continue;
            }
            if (set.contains(taskName)) {
                debugAction();
                setAbortTimeCurrent();
                pause();
                String msg = "two systems with task name \"" + taskName + "\"";
                logEvent(msg);
                throw new IllegalStateException(msg);
            }
            set.add(taskName);
        }
    }

    void checkUniqueRobotNames() {
        Set<String> set = new HashSet<>();
        for (AprsSystem sys : aprsSystems) {
            if (!sys.isConnected()) {
                continue;
            }
            String robotName = sys.getRobotName();
            if (robotName == null || robotName.equalsIgnoreCase("none")) {
                continue;
            }
            String robotTaskName = robotTaskMap.get(robotName);
            if (null != robotTaskName && !sys.getTaskName().equals(robotTaskName)) {
                String msg = "sys=" + sys + ", robotTaskName=" + robotTaskName;
                logEvent(msg);
                throw new IllegalStateException(msg);
            }
            if (set.contains(robotName)) {
                debugAction();
                setAbortTimeCurrent();
                pause();
                String msg = "two systems with task name \"" + robotName + "\"";
                logEvent(msg);
                throw new IllegalStateException(msg);
            }
            set.add(robotName);
        }
    }

    private final StackTraceElement @Nullable [] returnRobotsStackTrace = null;
    private volatile @Nullable Thread returnRobotsThread = null;
    private volatile long returnRobotsTime = -1;

    void printReturnRobotTraceInfo() {
        println("returnRobotsThread = " + returnRobotsThread);
        println("returnRobotsStackTrace = " + Arrays.toString(returnRobotsStackTrace));
        println("returnRobotsTime = " + (returnRobotsTime - System.currentTimeMillis()));
    }

    private final AtomicInteger returnRobotsCount = new AtomicInteger();
    private final AtomicLong returnRobotsTotalTime = new AtomicLong();

    private volatile @Nullable String lastReturnRobots1Comment = null;

    private volatile StackTraceElement lastReturnRobots1Trace@Nullable []  = null;

    private XFutureVoid returnRobots1(String comment, @Nullable AprsSystem stealFrom, @Nullable AprsSystem stealFor,
            int srn, int ecc) {
        try {
            lastReturnRobots1Comment = comment;
            StackTraceElement returnRobots1Trace[] = Thread.currentThread().getStackTrace();
            lastReturnRobots1Trace = returnRobots1Trace;
            if (ecc != -1 || srn != -1) {
                if (checkEcc(ecc)) {
                    return cancelledEcc(ecc);
                }
                if (checkSrn(srn)) {
                    return cancelledSrn(ecc);
                }
            }
            long startTime = System.currentTimeMillis();
            AprsSystem systems[] = (null != stealFor && null != stealFrom) ? new AprsSystem[]{stealFor, stealFrom}
                    : aprsSystems.toArray(new AprsSystem[0]);
            String blockername = "returnRobots:comment=" + comment + ",srn=" + srn;
            boolean origBlockConveyorMoves = blockConveyorMoves;
            XFuture<LockInfo> disallowTogglesFuture = disallowToggles(blockername, systems);
            setBlockConveyorMoves(blockername);
            return disallowTogglesFuture.thenComposeAsyncToVoid((LockInfo lockinfo) -> {
                if (checkEcc(ecc)) {
                    return cancelledEcc(ecc);
                }
                if (checkSrn(srn)) {
                    return cancelledSrn(ecc);
                }
                if (null != stealFrom) {
                    checkRunningOrDoingActions(stealFrom, srn, "returnRobots(" + comment + ")");
                }
                if (null != stealFor) {
                    checkRunningOrDoingActions(stealFor, srn, "returnRobots(" + comment + ")");
                }
                if (null == stealFrom || null == stealFor) {
                    for (int i = 0; i < this.aprsSystems.size(); i++) {
                        checkRunningOrDoingActions(this.aprsSystems.get(i), srn, "returnRobots(" + comment + ")");
                    }
                }
                return returnRobots2(returnRobotFunction.getAndSet(null), comment, ecc)
                        .peekException((Throwable throwable) -> {
                            System.out.println("");
                            System.out.flush();
                            System.err.println("blockername = " + blockername);
                            System.err.println("returnRobots1Trace = " + CRCLUtils.traceToString(returnRobots1Trace));
                            throw new RuntimeException(throwable);
                        });
            }, supervisorExecutorService).alwaysComposeAsyncToVoid(() -> {
                XFutureVoid ret = allowToggles(blockername, new AprsSystem[0]);
                clearBlockConveryorMoves();
                long timeDiff = System.currentTimeMillis() - startTime;
                long totalTime = returnRobotsTotalTime.addAndGet(timeDiff);
                int count = returnRobotsCount.incrementAndGet();
                logEvent("returnRobots:comment=" + comment + ",srn=" + srn + ",timeDiff=" + timeDiff + ",totalTime="
                        + totalTime + ",count=" + count);
                return ret;
            }, supervisorExecutorService);
        } catch (Exception exception) {
            logException(exception);
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", exception);
            throw asRuntimeException(exception);
        }
    }

    void handleXFutureException(Throwable throwable) {
        if (null != throwable) {
            if (throwable != lastLoggedException && !closing && !preClosing) {
                logException(throwable);
                Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE,
                        "Supervisor.handleXFutureException:" + throwable.getMessage(), throwable);
            }
            throw asRuntimeException(throwable);
        }
    }

    private boolean checkSrn(int srn) {
        return srn != -1 && srn != stealRobotNumber.get();
    }

    private final AtomicInteger returnRobotsNumber = new AtomicInteger();

    private volatile StackTraceElement lastReturnRobots2Trace@Nullable []  = null;

    void checkAllUniques() {
        checkUniqueRobotPorts();
        checkUniqueRobotNames();
        checkUniqueTaskNames();
        checkUniqueRobotTaskNames();
    }

    private XFutureVoid returnRobots2(@Nullable NamedFunction<Integer, XFutureVoid> func, String comment, int ecc) {
        checkAllUniques();
        if (func != null) {
            Thread curThread = Thread.currentThread();
            returnRobotsThread = curThread;
            returnRobotsTime = System.currentTimeMillis();
            final StackTraceElement[] returnRobots2StackTrace = curThread.getStackTrace();
            this.lastReturnRobots2Trace = returnRobots2StackTrace;
            String blockerName = "returnRobots" + returnRobotsNumber.incrementAndGet() + "_" + ecc;
            try {
                XFuture<LockInfo> disallowTogglesFuture = disallowToggles(blockerName, func.getSystems());
                return disallowTogglesFuture.thenComposeAsyncToVoid(x -> {
                    try {
                        clearStealingRobotsFlag();
                        return completeReturnRobots(func, comment, blockerName, ecc)
                                .peekException((Throwable throwable) -> {
                                    System.out.println("");
                                    System.out.flush();
                                    System.err.println("blockerName = " + blockerName);
                                    System.err.println("returnRobots2StackTrace = "
                                            + CRCLUtils.traceToString(returnRobots2StackTrace));
                                    throw new RuntimeException(throwable);
                                });
                    } catch (Exception e) {
                        System.out.println("");
                        System.out.flush();
                        System.err.println("blockerName = " + blockerName);
                        System.err.println(
                                "returnRobots2StackTrace = " + CRCLUtils.traceToString(returnRobots2StackTrace));
                        throw new RuntimeException(e);
                    }
                }, supervisorExecutorService);
            } catch (Exception ex) {
                allowToggles(blockerName, func.getSystems());
                Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", ex);
                throw asRuntimeException(ex);
            }
        } else {
            clearStealingRobotsFlag();
            logReturnRobotsNullRunnable(comment);
            return XFutureVoid.completedFuture();
        }
    }

    private XFutureVoid completeReturnRobots(NamedFunction<Integer, XFutureVoid> func, String comment,
            String blockerName, int ecc) {
        try {
            logEvent("completeReturnRobots " + func.getName() + ", comment=" + comment + ",ecc=" + ecc);
            AprsSystem[] systems = func.getSystems();
            return func.apply(ecc).peekNoCancelException(this::handleXFutureException)
                    .alwaysComposeAsyncToVoid(() -> allowToggles(blockerName, systems), supervisorExecutorService);
        } catch (Exception ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", ex);
            throw asRuntimeException(ex);
        }
    }

    @Nullable
    XFutureVoid getStealAbortFuture() {
        return stealAbortFuture;
    }

    public void setStealAbortFuture(XFutureVoid stealAbortFuture) {
        this.stealAbortFuture = stealAbortFuture;
    }

    @Nullable
    XFutureVoid getUnstealAbortFuture() {
        return unstealAbortFuture;
    }

    public void setUnstealAbortFuture(XFutureVoid unstealAbortFuture) {
        this.unstealAbortFuture = unstealAbortFuture;
    }

    void logReturnRobotsNullRunnable(String comment) {
        switch (comment) {
            case "prepActions":
            case "enableAndCheckAllRobots":
                break;

            default:
                logEvent("returnRobots: runnable=null,comment=" + comment);
        }
    }

    private final AtomicReference<@Nullable Function<Integer, XFutureVoid>> unStealRobotsFunction = new AtomicReference<>(
            null);

    private final AtomicInteger unstealRobotsCount = new AtomicInteger();

    private XFutureVoid unStealRobots(int ecc) {
        int usrc = unstealRobotsCount.incrementAndGet();
        if (restoringOrigRobotInfo) {
            throw new IllegalStateException("restoringOrigRobotInfo");
        }
        Function<Integer, XFutureVoid> function = unStealRobotsFunction.getAndSet(null);
        logEvent("START unStealRobots():  supplier=" + function + ", blockConveyorMoves=" + blockConveyorMoves
                + ", enableChangeCount=" + enableChangeCount.get() + ",usrc=" + usrc);
        if (null == function) {
            return XFutureVoid.completedFutureWithName("unStealRobots.null==supplier");
        }
        if (checkEcc(ecc)) {
            return cancelledEcc(ecc);
        }
        setBlockConveyorMoves("unStealRobots:ecc=" + ecc);
        return function.apply(ecc).thenRun(() -> {
            clearBlockConveryorMoves();
            logEvent("END unStealRobots():  supplier=" + function + ", blockConveyorMoves=" + blockConveyorMoves
                    + ", enableChangeCount=" + enableChangeCount.get() + ",usrc=" + usrc);
        });
    }

    private volatile boolean pauseSelected = false;

    private void setPauseSelected(boolean selected) {
        if (null != displayJFrame) {
            displayJFrame.setPauseSelected(selected);
        }
        this.pauseSelected = selected;
    }

    private boolean isPauseSelected() {
        if (null != displayJFrame) {
            boolean ret = displayJFrame.isPauseSelected();
            this.pauseSelected = ret;
            return ret;
        }
        return this.pauseSelected;
    }

    private volatile boolean pauseAllForOneSelected = false;

    public void setPauseAllForOneSelected(boolean selected) {
        if (null != displayJFrame) {
            displayJFrame.setPauseAllForOneSelected(selected);
        }
        this.pauseAllForOneSelected = selected;
    }

    private boolean isPauseAllForOneSelected() {
        if (null != displayJFrame) {
            boolean ret = displayJFrame.isPauseAllForOneSelected();
            this.pauseAllForOneSelected = ret;
            return ret;
        }
        return this.pauseAllForOneSelected;
    }

    private volatile boolean ContinuousDemoSelected = false;

    private void setContinuousDemoSelected(boolean selected) {
        if (null != displayJFrame) {
            displayJFrame.setContinuousDemoSelected(selected);
        }
        this.ContinuousDemoSelected = selected;
    }

    private boolean isContinuousDemoSelected() {
        if (null != displayJFrame) {
            boolean ret = displayJFrame.isContinuousDemoSelected();
            this.ContinuousDemoSelected = ret;
            return ret;
        }
        return this.ContinuousDemoSelected;
    }

    private volatile boolean useTeachCameraSelected = true;

    public void setUseTeachCameraSelected(boolean selected) {
        if (null != displayJFrame) {
            displayJFrame.setUseTeachCameraSelected(selected);
        }
        this.useTeachCameraSelected = selected;
    }

    private boolean isUseTeachCameraSelected() {
        if (null != displayJFrame) {
            boolean ret = displayJFrame.isUseTeachCameraSelected();
            this.useTeachCameraSelected = ret;
            return ret;
        }
        return this.useTeachCameraSelected;
    }

    private volatile boolean indContinuousDemoSelected = false;

    private void setIndContinuousDemoSelected(boolean selected) {
        if (null != displayJFrame) {
            displayJFrame.setIndContinuousDemoSelected(selected);
        }
        this.indContinuousDemoSelected = selected;
    }

    private boolean isIndContinuousDemoSelected() {
        if (null != displayJFrame) {
            boolean ret = displayJFrame.isIndContinuousDemoSelected();
            this.indContinuousDemoSelected = ret;
            return ret;
        }
        return this.indContinuousDemoSelected;
    }

    private volatile boolean indRandomToggleTestSelected = false;

    private void setIndRandomToggleTestSelected(boolean selected) {
        if (null != displayJFrame) {
            displayJFrame.setIndRandomToggleTestSelected(selected);
        }
        this.indRandomToggleTestSelected = selected;
    }

    private boolean isIndRandomToggleTestSelected() {
        if (null != displayJFrame) {
            boolean ret = displayJFrame.isIndRandomToggleTestSelected();
            this.indRandomToggleTestSelected = ret;
            return ret;
        }
        return this.indRandomToggleTestSelected;
    }

    private volatile boolean randomTestSelected = false;

    private void setRandomTestSelected(boolean selected) {
        if (null != displayJFrame) {
            displayJFrame.setRandomTestSelected(selected);
        }
        this.randomTestSelected = selected;
    }

    private boolean isRandomTestSelected() {
        if (null != displayJFrame) {
            boolean ret = displayJFrame.isRandomTestSelected();
            this.randomTestSelected = ret;
            return ret;
        }
        return this.randomTestSelected;
    }

    private volatile boolean pauseResumeTestSelected = false;

    public void setPauseResumeTestSelected(boolean selected) {
        if (null != displayJFrame) {
            displayJFrame.setPauseResumeTestSelected(selected);
        }
        this.pauseResumeTestSelected = selected;
    }

    public boolean isPauseResumeTestSelected() {
        if (null != displayJFrame) {
            boolean ret = displayJFrame.isPauseResumeTestSelected();
            this.pauseResumeTestSelected = ret;
            return ret;
        }
        return this.pauseResumeTestSelected;
    }

    // jCheckBoxMenuItemKeepAndDisplayXFutureProfiles
    private volatile boolean ContinuousDemoRevFirstSelected = false;

    private void setContinuousDemoRevFirstSelected(boolean selected) {
        if (null != displayJFrame) {
            displayJFrame.setContinuousDemoRevFirstSelected(selected);
        }
        this.ContinuousDemoRevFirstSelected = selected;
    }

    private boolean isContinuousDemoRevFirstSelected() {
        if (null != displayJFrame) {
            boolean ret = displayJFrame.isContinuousDemoRevFirstSelected();
            this.ContinuousDemoRevFirstSelected = ret;
            return ret;
        }
        return this.ContinuousDemoRevFirstSelected;
    }

    private volatile boolean keepAndDisplayXFutureProfilesSelected = false;

    public void setKeepAndDisplayXFutureProfilesSelected(boolean selected) {
        if (null != displayJFrame) {
            displayJFrame.setKeepAndDisplayXFutureProfilesSelected(selected);
        }
        this.keepAndDisplayXFutureProfilesSelected = selected;
    }

    private boolean isKeepAndDisplayXFutureProfilesSelected() {
        if (null != displayJFrame) {
            boolean ret = displayJFrame.isKeepAndDisplayXFutureProfilesSelected();
            this.keepAndDisplayXFutureProfilesSelected = ret;
            return ret;
        }
        return this.keepAndDisplayXFutureProfilesSelected;
    }

    private XFutureVoid showCheckEnabledErrorSplash() {
        return showErrorSplash("Not all robots\n could be enabled.")
                .thenComposeAsyncToVoid(
                        x -> {
                            return runOnDispatchThread(() -> {
                                setContinuousDemoSelected(false);
                                setContinuousDemoRevFirstSelected(false);
                                if (null != ContinuousDemoFuture) {
                                    ContinuousDemoFuture.cancelAll(true);
                                    ContinuousDemoFuture = null;
                                }
                            });
                        }, supervisorExecutorService);
    }

    private XFutureVoid showErrorSplash(String errMsgString) {
        if (null != displayJFrame) {
            return displayJFrame.showErrorSplash(errMsgString);
        } else {
            return XFutureVoid.completedFutureWithName("showErrorSplash " + errMsgString);
        }
    }

    private @Nullable XFutureVoid stealAbortFuture = null;
    private @Nullable XFutureVoid unstealAbortFuture = null;

    private volatile boolean stealingRobots = false;

    /**
     * Show a message in full screen mode with flashing colors. (It is intended
     * to be visible and attention grabbing across the room.) Note: there is a
     * checkbox in the menu that can be used to disable these messages, in which
     * case the message will only be logged to the events tab.
     *
     * @param message string to display
     * @param fontSize font size
     * @param image image to show under text
     * @param colors colors to flash in order
     * @param graphicsDevice device to display on
     * @return future that can be used to take action after the message has been
     * shown
     */
    private XFutureVoid showMessageFullScreen(String message, float fontSize, @Nullable Image image, List<Color> colors,
            @Nullable GraphicsDevice graphicsDevice) {
        if (null != displayJFrame && null != graphicsDevice) {
            return displayJFrame.showMessageFullScreen(message, fontSize, image, colors, graphicsDevice);
        } else {
            return XFutureVoid.completedFutureWithName("showMessageFullScreen " + message);
        }
    }

    private @MonotonicNonNull AprsSystem posMapInSys = null;
    private @MonotonicNonNull AprsSystem posMapOutSys = null;

    @Nullable
    AprsSystem getPosMapInSys() {
        return posMapInSys;
    }

    void setPosMapInSys(AprsSystem posMapInSys) {
        this.posMapInSys = posMapInSys;
    }

    @Nullable
    AprsSystem getPosMapOutSys() {
        return posMapOutSys;
    }

    public void setPosMapOutSys(AprsSystem posMapOutSys) {
        this.posMapOutSys = posMapOutSys;
    }

    private final AtomicInteger stealRobotNumber = new AtomicInteger();
    private final AtomicInteger reverseRobotTransferNumber = new AtomicInteger();

    XFutureVoid stealRobot(AprsSystem stealFrom, AprsSystem stealFor, int ecc)
            throws IOException, PositionMap.BadErrorMapFormatException {

        String stealForRobotName = stealFor.getRobotName();
        if (null == stealForRobotName) {
            throw new IllegalArgumentException("stealFor has null robotName");
        }
        String stealFromRobotName = stealFrom.getRobotName();
        if (null == stealFromRobotName) {
            throw new IllegalArgumentException("stealFrom has null robotName");
        }
        String stealFromOrigCrclHost = stealFrom.getRobotCrclHost();
        if (null == stealFromOrigCrclHost) {
            throw new IllegalArgumentException("stealFrom has null robotCrclHost");
        }
        if (checkEcc(ecc)) {
            return cancelledEcc(ecc);
        }
        return stealRobotsInternal(stealFrom, stealFor, stealFromOrigCrclHost, ecc);
    }

    private void writeToColorTextSocket(byte[] bytes) {
        if (null != displayJFrame) {
            displayJFrame.writeToColorTextSocket(bytes);
        }
    }

    private volatile @Nullable XFutureVoid lastStealRobotsInternalBeforeAllowTogglesFuture = null;
    private volatile @Nullable XFuture<Boolean> lastStealRobotsInternalPart1 = null;

    private volatile @Nullable XFutureVoid lastStealRobotsInternalPart2 = null;

    private volatile @Nullable XFutureVoid lastStealRobotsInternalPart3 = null;

    private volatile @Nullable XFutureVoid lastStealRobotsInternalPart4 = null;

    private volatile @Nullable XFutureVoid lastStealRobotsInternalPart5 = null;

    private volatile @Nullable XFuture<Boolean> lastStealRobotsInternalPart6 = null;

    private volatile @Nullable XFutureVoid lastStealRobotsInternalPart7 = null;

    private volatile @Nullable XFutureVoid lastStealRobotsInternalPart8 = null;

    private final boolean blockConveyorMoves = false;

    private XFutureVoid stealRobotsInternal(AprsSystem stealFrom, AprsSystem stealFor, String stealFromOrigCrclHost,
            int ecc) throws IOException, PositionMap.BadErrorMapFormatException {

        if (checkEcc(ecc)) {
            return cancelledEcc(ecc);
        }
        boolean origBlockConveyorMoves = blockConveyorMoves;
        if (!isTogglesAllowed()) {
            throw new IllegalStateException("toggles not allowed");
        }
        XFutureVoid origStealRobotFuture = stealRobotFuture.get();
        if (origStealRobotFuture != null) {
            println("calling stealrRobot when already stealingRobots");
            return origStealRobotFuture;
        }
        final int srn = stealRobotNumber.incrementAndGet();
        logEvent("Transferring " + stealFrom.getRobotName() + " to " + stealFor.getTaskName() + " : srn=" + srn
                + ",ecc=" + ecc);
        String blocker = "stealRobot,srn=" + srn + ",ecc=" + ecc;
        setBlockConveyorMoves(blocker);
        setStealingRobotsFlag();
        int startingStealRobotsInternalAbortCount = abortCount.get();
        final GraphicsDevice gd = graphicsDevice();
        XFuture<LockInfo> disallowTogglesFuture = disallowToggles(blocker, stealFrom, stealFor);
        XFutureVoid beforeAllowTogglesFuture = disallowTogglesFuture.thenComposeAsyncToVoid(x -> {
            if (checkEcc(ecc)) {
                clearBlockConveryorMoves();
                return cancelledEcc(ecc);
            }
            return stealRobotsInternalBeforeAllowToggles(gd, srn, stealFrom, stealFor, stealFromOrigCrclHost, ecc);
        }, supervisorExecutorService);
        lastStealRobotsInternalBeforeAllowTogglesFuture = beforeAllowTogglesFuture;
        XFutureVoid withAllowTogglesFuture = beforeAllowTogglesFuture
                .alwaysComposeAsyncToVoid(() -> allowToggles(blocker, stealFor), supervisorExecutorService)
                .alwaysRun(() -> clearBlockConveryorMoves());
        if (!isContinuousDemoSelected() && !isContinuousDemoRevFirstSelected()) {
            return withAllowTogglesFuture;
        }
        XFuture<Boolean> part1 = withAllowTogglesFuture
                .thenComposeAsync("continueAfterSwitch" + " : srn=" + srn, (Void ignore3) -> {
                    clearBlockConveryorMoves();
                    int curSrn = stealRobotNumber.get();
                    if (checkEcc(ecc)) {
                        return cancelledEcc(ecc, boolean.class);
                    }
                    if (srn != curSrn) {
                        logEvent("continueAfterSwitch srn=" + srn + ", curSrn=" + curSrn);
                        return XFuture.completedFutureWithName(
                                "continueAfterSwitch.srn != stealRobotNumber.get()" + " : srn=" + srn, false);
                    }
                    if (stealFor.isAborting()) {
                        logEvent("continueAfterSwitch stealFor.isAborting() : srn=" + srn + ", curSrn=" + curSrn
                                + ", stealFor=" + stealFrom);
                        return XFuture.completedFutureWithName(
                                "continueAfterSwitch.stealFor.isAborting()" + " : srn=" + srn, false);
                    }

                    logEvent("Continue actions after switch for " + stealFor.getTaskName() + " with "
                            + stealFor.getRobotName() + " : srn=" + srn + ",ecc=" + ecc);
                    if (stealFor.isReverseFlag() != lastStartAllActionsReverseFlag) {
                        stealFor.printSetReverseTraces();
                        throw new IllegalStateException("stealFor.isReverseFlag()=" + stealFor.isReverseFlag()
                                + ",lastStartAllActionsReverseFlag=" + lastStartAllActionsReverseFlag);
                    }
                    int curAbortCount = abortCount.get();
                    if (curAbortCount != startingStealRobotsInternalAbortCount) {
                        logEvent("curAbortCount=" + curAbortCount + ", startingStealRobotsInternalAbortCount="
                                + startingStealRobotsInternalAbortCount);
                        XFuture<Boolean> xfb = new XFuture<>("abortedSteal");
                        xfb.cancelAll(false);
                        return xfb;
                    }
                    XFuture<Boolean> stealForContinueFuture = stealFor
                            .continueActionList("stealFor.continueAfterSwitch" + " : srn=" + srn + ",ecc=" + ecc);
                    XFuture<Boolean> stealForCompletedContinueFuture = stealForContinueFuture
                            .thenComposeAsync((Boolean x4) -> {
                                logEvent("continueAfterSwitch " + stealFor.getRunName()
                                        + " completed action list after robot switch " + x4 + " : srn=" + srn + ",ecc="
                                        + ecc);
                                if (checkEcc(ecc)) {
                                    return cancelledEcc(ecc, Boolean.class);
                                }
                                return finishAction(stealFor).thenApply(x5 -> {
                                    logEvent("finish continueAfterSwitch " + stealFor.getRunName()
                                            + " completed action list " + x4 + " : srn=" + srn + ",ecc=" + ecc);
                                    if (checkEcc(ecc)) {
                                        return x4;
                                    }
                                    if (x4) {
                                        completeSystemsContinueIndFuture(stealFor, !stealFor.isReverseFlag(), ecc);
                                    }
                                    return x4;
                                });
                            }, supervisorExecutorService);
                    return stealForCompletedContinueFuture;
                }, supervisorExecutorService).peekNoCancelException(this::handleXFutureException);
        lastStealRobotsInternalPart1 = part1;
        if (isIndContinuousDemoSelected() || isIndRandomToggleTestSelected()) {
            return part1.thenRunAsync("stealRobot :  Checking systemContinueMap " + " : srn=" + srn, () -> {
                final String stealForRobotName = requireNonNull(stealFor.getRobotName(),
                        "stealFor.getRobotname() : stealFor=" + stealFor);
                final String stealForRobotNameTask = robotTaskMap.get(stealForRobotName);
                logEvent("completing stealRobot: stealingRobots=" + stealingRobots + ", stealFor=" + stealFor + ",srn="
                        + srn + ",ecc=" + ecc + ", stealRobotNumber=" + stealRobotNumber.get() + "robotEnableMap.get("
                        + stealForRobotName + ")=" + stealForRobotNameTask);
                if (checkEcc(ecc)) {
                    return;
                }
                if (stealingRobots && srn == stealRobotNumber.get()) {
                    boolean enabled = stealForRobotNameTask != null;
                    if (!enabled) {
                        completeSystemsContinueIndFuture(stealFor, !stealFor.isReverseFlag(), ecc);
                    }
                }
            }, supervisorExecutorService);
        }

        XFutureVoid part2 = part1
                .thenComposeAsyncToVoid("startSafeAbortAfterSwitch" + " : srn=" + srn,
                        (Boolean ignored) -> {
                            if (checkEcc(ecc)) {
                                return cancelledEcc(ecc);
                            }
                            if (checkSrn(srn)) {
                                return cancelledSrn(ecc);
                            }
                            return stealFor.startSafeAbort("startSafeAbortAfterSwitch" + " : srn=" + srn + ",ecc=" + ecc);
                        }, supervisorExecutorService);
        lastStealRobotsInternalPart2 = part2;
        XFutureVoid part3 = part2
                .thenRun(() -> logEvent("Safe abort completed for " + stealFor + " : srn=" + srn + ",ecc=" + ecc));
        lastStealRobotsInternalPart3 = part3;
        XFutureVoid part4 = part3.thenComposeAsyncToVoid("showReturning" + " : srn=" + srn + ",ecc=" + ecc,
                (Void ignored4) -> {
                    if (checkEcc(ecc)) {
                        return cancelledEcc(ecc);
                    }
                    if (checkSrn(srn)) {
                        return cancelledSrn(ecc);
                    }
                    return showMessageFullScreen("Returning \n" + stealFrom.getRobotName(), 80.0f,
                            SplashScreen.getRobotArmImage(), SplashScreen.getBlueWhiteGreenColorList(), gd);
                }, supervisorExecutorService);
        lastStealRobotsInternalPart4 = part4;
        XFutureVoid part5 = part4.thenComposeAsyncToVoid("returnRobots2" + " : srn=" + srn,
                (Void ignored5) -> returnRobots1("returnRobots2" + " : srn=" + srn + ",ecc=" + ecc, stealFor, stealFrom,
                        srn, ecc),
                supervisorExecutorService);
        lastStealRobotsInternalPart5 = part5;
        XFuture<Boolean> part6 = part5.thenComposeAsync("continueAfterReturn" + " : srn=" + srn, (Void ignored6) -> {
            logEvent("Continue actions for " + stealFor.getTaskName() + " with " + stealFor.getRobotName() + " : srn="
                    + srn + ",ecc=" + ecc);
            if (checkEcc(ecc)) {
                return cancelledEcc(ecc, Boolean.class);
            }
            if (checkSrn(srn)) {
                return cancelledSrn(ecc, Boolean.class);
            }
            int curAbortCount = abortCount.get();
            if (curAbortCount != startingStealRobotsInternalAbortCount) {
                logEvent("curAbortCount=" + curAbortCount + ", startingStealRobotsInternalAbortCount="
                        + startingStealRobotsInternalAbortCount);
                XFuture<Boolean> xfv = new XFuture<>("abortedSteal");
                xfv.cancelAll(false);
                return xfv;
            }
            return stealFrom.continueActionList("stealFrom.continueAfterReturn" + " : srn=" + srn + ",ecc=" + ecc);
        }, supervisorExecutorService);
        lastStealRobotsInternalPart6 = part6;
        XFutureVoid part7 = part6.thenComposeAsyncToVoid("stealRobotsInternal : finishAction", (Boolean x5) -> {
            logEvent("stealFrom.continueAfterReturn " + stealFrom.getRunName() + " completed action list after return "
                    + x5 + " : srn=" + srn + ",ecc=" + ecc);
            if (checkEcc(ecc)) {
                return cancelledEcc(ecc);
            }
            if (checkSrn(srn)) {
                return cancelledSrn(ecc);
            }
            int curAbortCount2 = abortCount.get();
            if (curAbortCount2 != startingStealRobotsInternalAbortCount) {
                logEvent("curAbortCount=" + curAbortCount2 + ", startingStealRobotsInternalAbortCount="
                        + startingStealRobotsInternalAbortCount);
                XFutureVoid xfv = new XFutureVoid("abortedSteal");
                xfv.cancelAll(false);
                return xfv;
            }
            return finishAction(stealFrom).thenApply((Void x6) -> {
                logEvent("finish stealFrom.continueAfterReturn " + stealFrom.getRunName() + " completed action list "
                        + x5 + " : srn=" + srn + ",ecc=" + ecc);
                if (checkEcc(ecc)) {
                    return ((Void) null);
                }
                if (checkSrn(srn)) {
                    return ((Void) null);
                }
                if (x5) {
                    completeSystemsContinueIndFuture(stealFrom, !stealFrom.isReverseFlag(), ecc);
                }
                return ((Void) null);
            });
        }, supervisorExecutorService);
        lastStealRobotsInternalPart7 = part7;
        XFutureVoid part8 = part7.alwaysRunAsync("stealRobotsInternal.logFinish", () -> {
            int curAbortCount3 = abortCount.get();
            int currentEcc = enableChangeCount.get();
            logEvent("stealRobotsInternal.logFinish ecc=" + ecc + ",currentEcc=" + currentEcc + ",curAbortCount3="
                    + curAbortCount3 + ", startingStealRobotsInternalAbortCount="
                    + startingStealRobotsInternalAbortCount);
        }, supervisorExecutorService);
        lastStealRobotsInternalPart8 = part8;
        return part8;
    }

    @SuppressWarnings("EmptyMethod")
    private void clearBlockConveryorMoves() {
//        boolean oldValue = blockConveyorMoves;
//        logEvent("clearBlockConveyorMoves,oldValue=" + oldValue);
//        blockConveyorMoves = false;
    }
//    private volatile String setBlockConveyorMovesComment = null;
//    private volatile StackTraceElement setBlockConveyorMovesTrace[] = null;

    @SuppressWarnings("EmptyMethod")
    private void setBlockConveyorMoves(String comment) {
//        boolean oldValue = blockConveyorMoves;
//        logEvent("setBlockConveyorMoves:" + comment + ",oldValue=" + oldValue);
//        if (!oldValue) {
//            setBlockConveyorMovesComment = comment;
//            setBlockConveyorMovesTrace = Thread.currentThread().getStackTrace();
//            blockConveyorMoves = true;
//        }
    }

    private volatile StackTraceElement setStealingRobotsFlagTrace@Nullable []  = null;

    synchronized private void setStealingRobotsFlag() {
        boolean oldFlag = stealingRobots;
        stealingRobots = true;
        setStealingRobotsFlagTrace = Thread.currentThread().getStackTrace();
        logEvent("setStealingRobotsFlag() : oldFlag=" + oldFlag);
    }

    private volatile @Nullable XFutureVoid lastStealRobotsInternalBeforeAllowToggles = null;
    private volatile StackTraceElement lastStealRobotsInternalBeforeAllowTogglesTrace@Nullable []  = null;

    private XFutureVoid stealRobotsInternalBeforeAllowToggles(@Nullable GraphicsDevice gd, int srn,
            AprsSystem stealFrom, AprsSystem stealFor, String stealFromOrigCrclHost, int ecc) {
        try {

            String stealForRobotName = stealFor.getRobotName();
            if (null == stealForRobotName) {
                throw new NullPointerException("stealForRobotName");
            }
            String stealFromRobotName = stealFrom.getRobotName();
            if (null == stealFromRobotName) {
                throw new NullPointerException("stealFromRobotName");
            }
            File f = getPosMapFile(stealForRobotName, stealFromRobotName);
            PositionMap pm = (f != null && !f.getName().equals("null")) ? new PositionMap(f)
                    : PositionMap.emptyPositionMap();

            initColorTextSocket();

            int stealFromOrigCrclPort = stealFrom.getRobotCrclPort();

            Map<ExecutorOption, Object> stealFromOptionsCopy = new HashMap<>();
            Map<ExecutorOption, ?> stealFromOptionsOrig = stealFrom.getExecutorOptions();
            if (null != stealFromOptionsOrig) {
                copyOptions(transferrableOptions, stealFromOptionsOrig, stealFromOptionsCopy);
            }

            Map<ExecutorOption, Object> stealForOptionsCopy = new HashMap<>();
            Map<ExecutorOption, ?> stealForOptionsOrig = stealFor.getExecutorOptions();
            if (null != stealForOptionsOrig) {
                copyOptions(transferrableOptions, stealForOptionsOrig, stealForOptionsCopy);
            }
            NamedFunction<Integer, XFutureVoid> returnRobot = setupReturnRobots(srn, ecc, stealFor, stealFrom,
                    stealForOptionsCopy, pm);

            setupUnstealRobots(srn, stealFor, stealFrom, stealForRobotName, gd);
            setStealingRobotsFlag();
            logEvent("Starting safe abort and disconnect for " + " : srn=" + srn + " " + stealFor);
            logEvent("    and starting safe abort and disconnect for " + " : srn=" + srn + " " + stealFrom);
            XFutureVoid stealFromFuture1 = stealFrom
                    .startSafeAbortAndDisconnect("stealAbortAllOf.stealFrom" + " : srn=" + srn);
            XFutureVoid stealFromSafeAbortFuture = stealFromFuture1
                    .thenRunAsync(
                            () -> logEvent("Safe abort and disconnect completed for " + stealFrom + " "
                                    + stealFromRobotName + " needed for " + stealFor + " : srn=" + srn),
                            supervisorExecutorService);
            XFutureVoid stealForFuture1 = stealFor.startSafeAbortAndDisconnect("stealAbortAllOf.stealFor" + " : srn="
                    + srn + "toggleBlockerMap.keySet() = " + toggleBlockerMap.keySet()).thenRun(() -> {
                if (stealFor.isConnected()) {
                    throw new RuntimeException("stealFor.isConnected() : stealFor=" + stealFor);
                }
                final String stealForRobotNameTask = requireNonNull(robotTaskMap.get(stealForRobotName),
                        "robotTaskMap.get(" + stealForRobotName + ")");
                if (!stealForRobotNameTask.equals(stealFor.getTaskName())) {
                    throw new RuntimeException("stealForRobotName=" + stealForRobotName
                            + ",stealForRobotNameTask=" + stealForRobotNameTask + ",stealFor=" + stealFor);
                }
                robotTaskMapRemove(stealForRobotName);
                logEvent("stealForAbortedAndDisconnected");
            });
            XFuture<Void> stealForAbortAndDisconnectFuture = stealForFuture1
                    .thenComposeAsync("showDisabledMessage." + stealForRobotName, (Void ignore) -> {
                        logEvent("Safe abort and disconnect completed for " + stealFor + ", " + stealForRobotName
                                + " being disabled. " + " : srn=" + srn);
                        writeToColorTextSocket("0xFF0000, 0x00FF00\r\n".getBytes());
                        if (null != gd) {
                            return showMessageFullScreen(stealForRobotName + "\n Disabled", 80.0f,
                                    SplashScreen.getDisableImageImage(), SplashScreen.getRedYellowColorList(), gd);
                        } else {
                            return XFutureVoid.completedFutureWithName(
                                    "showMessageFullScreen " + stealForRobotName + " Disabled");
                        }
                    }, supervisorExecutorService);
            stealAbortFuture = XFuture.allOfWithName("stealAbortAllOf", stealFromSafeAbortFuture,
                    stealForAbortAndDisconnectFuture);
            XFutureVoid beforeAllowTogglesFuture = stealAbortFuture
                    .thenComposeAsync("transfer" + " : srn=" + srn, (Void ignore) -> {
                        if (stealFor.isConnected()) {
                            throw new RuntimeException("stealForFuture1=" + stealForFuture1 + ", stealFor=" + stealFor);
                        }
                        logEvent("transfer : " + stealFor + " connectRobot(" + stealFromRobotName + ","
                                + stealFromOrigCrclHost + "," + stealFromOrigCrclPort + ")" + " : srn=" + srn);
                        return stealFor.startAddPositionMap(pm);
                    }, supervisorExecutorService)
                    .thenComposeAsyncToVoid("showSwitching" + " : srn=" + srn,
                            (Void ignore2) -> {
                                for (String optString : transferrableOptions) {
                                    final ExecutorOption exOptOf = ExecutorOption.of(optString);
                                    if (stealFromOptionsCopy.containsKey(exOptOf)) {
                                        stealFor.setExecutorOption(exOptOf, stealFromOptionsCopy.get(exOptOf));
                                    }
                                }
                                stealFor.setToolHolderOperationEnabled(false);
                                String stealForRpy = ExecutorOption.ForString
                                        .map(stealFor.getExecutorOptions())
                                        .get(ExecutorOption.ForString.RPY);
                                logEvent("stealForRpy=" + stealForRpy);
                                robotTaskMapPut(stealFromRobotName, stealFor);
                                return stealFor.connectRobot(stealFromRobotName, stealFromOrigCrclHost, stealFromOrigCrclPort)
                                        .thenCompose(() -> refreshRobotsTable());
                            }, supervisorExecutorService)
                    .thenComposeAsyncToVoid("showSwitching" + " : srn=" + srn,
                            (Void ignore2) -> {
                                return showMessageFullScreen("Switching to \n" + stealFromRobotName, 80.0f,
                                        SplashScreen.getRobotArmImage(), SplashScreen.getBlueWhiteGreenColorList(), gd);
                            }, supervisorExecutorService);
            lastStealRobotsInternalBeforeAllowToggles = beforeAllowTogglesFuture;
            lastStealRobotsInternalBeforeAllowTogglesTrace = Thread.currentThread().getStackTrace();
            return beforeAllowTogglesFuture;
        } catch (Exception ex) {
            logException(ex);
            throw asRuntimeException(ex);
        }
    }

    public void robotTaskMapRemove(String robotToDisable) {
        logEvent("removing " + robotToDisable + " from " + robotTaskMap);
        robotTaskMap.remove(robotToDisable);
        refreshRobotsTable();
    }

    private RuntimeException asRuntimeException(Throwable ex) {
        if (ex instanceof RuntimeException) {
            return (RuntimeException) ex;
        } else {
            return new RuntimeException(ex);
        }
    }

    private @Nullable
    GraphicsDevice graphicsDevice() {
        return (null != displayJFrame) ? displayJFrame.getGraphicsDevice() : null;
    }

    private NamedFunction<Integer, XFutureVoid> setupReturnRobots(final int srn, int ecc, AprsSystem stealFor,
            AprsSystem stealFrom, Map<ExecutorOption, ?> stealForOptions, PositionMap pm) {
        String stealFromOrigCrclHost = stealFrom.getRobotCrclHost();
        if (null == stealFromOrigCrclHost) {
            throw new IllegalStateException("null robotCrclHost in stealFrom =" + stealFrom);
        }
        String stealFromRobotName = stealFrom.getRobotName();
        if (null == stealFromRobotName) {
            throw new IllegalStateException("null robotName in stealFrom =" + stealFrom);
        }

        String stealForOrigCrclHost = stealFor.getRobotCrclHost();
        if (null == stealForOrigCrclHost) {
            throw new IllegalStateException("null robotCrclHost in stealFor =" + stealFor);
        }
        String stealForRobotName = stealFor.getRobotName();
        if (null == stealForRobotName) {
            throw new IllegalStateException("null robot name in stealFor =" + stealFor);
        }
        return setupRobotReturnInternal(stealFrom, stealFor, srn, ecc, stealForRobotName, stealFromRobotName,
                stealFromOrigCrclHost, stealForOptions, pm, stealForOrigCrclHost);
    }

    private final AtomicInteger ignoredToggles = new AtomicInteger();

    private final AtomicLong totalRandomDelays = new AtomicLong();
    private final AtomicInteger randomDelayCount = new AtomicInteger();

    private volatile StackTraceElement lastSetupRobotReturnInternalTrace0@Nullable []  = null;
    private volatile StackTraceElement lastSetupRobotReturnInternalTrace1@Nullable []  = null;

    private NamedFunction<Integer, XFutureVoid> setupRobotReturnInternal(
            AprsSystem stealFrom,
            AprsSystem stealFor,
            final int srn, int setup_ecc,
            String stealForRobotName, String stealFromRobotName,
            String stealFromOrigCrclHost,
            Map<ExecutorOption, ?> stealForOptions,
            PositionMap pm,
            String stealForOrigCrclHost) {
        int stealFromOrigCrclPort = stealFrom.getRobotCrclPort();
        int stealForOrigCrclPort = stealFor.getRobotCrclPort();
        String returnName = "Return  : srn=" + srn + " " + stealForRobotName + "-> " + stealFor.getTaskName() + " , "
                + stealFromRobotName + "->" + stealFrom.getTaskName();
        final StackTraceElement setupRobotReturnInternalTrace0[] = Thread.currentThread().getStackTrace();
        this.lastSetupRobotReturnInternalTrace0 = setupRobotReturnInternalTrace0;
        return setReturnRobotRunnable(returnName, (Integer new_ecc) -> {
            final StackTraceElement setupRobotReturnInternalTrace1[] = Thread.currentThread().getStackTrace();
            this.lastSetupRobotReturnInternalTrace1 = setupRobotReturnInternalTrace1;
            if (null != new_ecc && -1 != new_ecc) {
                if (checkEcc(new_ecc)) {
                    return cancelledEcc(new_ecc);
                }
                if (checkSrn(srn)) {
                    return cancelledSrn(srn);
                }
            }
            if (stealFor.isPaused()) {
                logEvent(stealFor.getTaskName() + " is paused when " + returnName + " : srn=" + srn + ",setup_ecc="
                        + setup_ecc);
            }
            if (stealFrom.isPaused()) {
                logEvent(stealFrom.getTaskName() + " is paused when " + returnName + " : srn=" + srn + ",setup_ecc="
                        + setup_ecc);
            }
            if (Objects.equals(stealFrom.getRobotName(), stealFromRobotName)) {
                logEvent(stealFromRobotName + " already assigned to " + stealFrom + " : srn=" + srn + ",setup_ecc="
                        + setup_ecc);
            }
            if (Objects.equals(stealFor.getRobotName(), stealForRobotName)) {
                logEvent(stealForRobotName + " already assigned to " + stealFor + " : srn=" + srn + ",setup_ecc="
                        + setup_ecc);
            }
            if (Objects.equals(stealFrom.getRobotName(), stealFromRobotName)
                    && Objects.equals(stealFor.getRobotName(), stealForRobotName)) {
                return XFutureVoid.completedFutureWithName(
                        "returnRobot.alreadyReturned" + " : srn=" + srn + ",setup_ecc=" + setup_ecc);
            }
            checkRunningOrDoingActions(stealFor, srn, returnName);
            checkRunningOrDoingActions(stealFrom, srn, returnName);
            logEvent("Disconnect robot from " + stealFor);
            if (resetting) {
                stealFor.clearErrors();
                stealFor.resume();
            }
            return stealFor.disconnectRobot()
                    .thenComposeAsync("returnRobot." + stealFrom.getTaskName() + " connect to " + stealFromRobotName
                            + " at " + stealFromOrigCrclHost + ":" + stealFromOrigCrclPort + " : srn=" + srn
                            + ",setup_ecc=" + setup_ecc, x -> {

                                logEvent(stealForRobotName + " disconnnected from " + stealFor + " : srn=" + srn);
                                logEvent("start returnRobot." + stealFrom.getTaskName() + " connect to "
                                        + stealFromRobotName + " at " + stealFromOrigCrclHost + ":"
                                        + stealFromOrigCrclPort + " : srn=" + srn + ",setup_ecc=" + setup_ecc);
                                if (stealFor.isConnected()) {
                                    System.out.println("");
                                    System.out.flush();
                                    System.err.println("setupRobotReturnInternalTrace0 = "
                                            + CRCLUtils.traceToString(setupRobotReturnInternalTrace0));
                                    System.err.println("setupRobotReturnInternalTrace1 = "
                                            + CRCLUtils.traceToString(setupRobotReturnInternalTrace1));
                                    throw new IllegalStateException("stealFor.isConnected() : stealFor" + stealFor);
                                }
                                if (null != new_ecc && -1 != new_ecc) {
                                    if (checkEcc(new_ecc)) {
                                        return cancelledEcc(new_ecc);
                                    }
                                    if (checkSrn(srn)) {
                                        return cancelledSrn(srn);
                                    }
                                }
                                robotTaskMapPut(stealFromRobotName, stealFrom);
                                return stealFrom
                                        .connectRobot(stealFromRobotName, stealFromOrigCrclHost, stealFromOrigCrclPort)
                                        .thenCompose(() -> refreshRobotsTable());
                            }, supervisorExecutorService)
                    .thenComposeAsync("returnRobot.transferOption", x -> {
                        try {
                            logEvent(stealFrom.getTaskName() + " connected to " + stealFromRobotName + " at "
                                    + stealFromOrigCrclHost + ":" + stealFromOrigCrclPort + " : srn=" + srn
                                    + ",setup_ecc="
                                    + setup_ecc);
                            if (stealFor.isConnected()) {
                                System.out.println("");
                                System.out.flush();
                                System.err.println("setupRobotReturnInternalTrace0 = "
                                        + CRCLUtils.traceToString(setupRobotReturnInternalTrace0));
                                System.err.println("setupRobotReturnInternalTrace1 = "
                                        + CRCLUtils.traceToString(setupRobotReturnInternalTrace1));
                                throw new IllegalStateException("stealFor.isConnected() : stealFor" + stealFor);
                            }
                            if (null != new_ecc && -1 != new_ecc) {
                                if (checkEcc(new_ecc)) {
                                    return cancelledEcc(new_ecc);
                                }
                                if (checkSrn(srn)) {
                                    return cancelledSrn(srn);
                                }
                            }
                            for (String optString : transferrableOptions) {
                                ExecutorOption exOptOf = ExecutorOption.of(optString);
                                if (stealForOptions.containsKey(exOptOf)) {
                                    stealFor.setExecutorOption(exOptOf, stealForOptions.get(exOptOf));
                                }
                            }
                            stealFor.setToolHolderOperationEnabled(true);
                            stealFor.removePositionMap(pm);
                            String stealForRpy = ExecutorOption.ForString
                                    .map(stealFor.getExecutorOptions())
                                    .get(ExecutorOption.ForString.RPY);
                            logEvent("stealForRpy=" + stealForRpy);
                            final String stealForTaskName = stealFor.getTaskName();
                            logEvent("start returnRobot." + stealForTaskName + " connect to " + stealForRobotName
                                    + " at "
                                    + stealForOrigCrclHost + ":" + stealForOrigCrclPort + " : srn=" + srn + ",ecc="
                                    + setup_ecc);
                            robotTaskMapPut(stealForRobotName, stealFor);
                            return stealFor.connectRobot(stealForRobotName, stealForOrigCrclHost, stealForOrigCrclPort)
                                    .thenCompose(() -> refreshRobotsTable());
                        } catch (Exception ex) {
                            logEventErr(ex.getMessage());
                            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", ex);
                            throw new RuntimeException(ex);
                        }
                    }, supervisorExecutorService).thenRun(() -> {
                logEvent(stealFor.getTaskName() + " connected to " + stealForRobotName + " at "
                        + stealForOrigCrclHost + ":" + stealForOrigCrclPort + " : srn=" + srn + ",setup_ecc="
                        + setup_ecc);
                checkAllUniques();
            });
        }, stealFor, stealFrom);
    }

    private void checkAllRunningOrDoingActions(int srn, String whenString) throws IllegalStateException {
        for (int i = 0; i < aprsSystems.size(); i++) {
            checkRunningOrDoingActions(aprsSystems.get(i), srn, whenString);
        }
    }

    private void checkRunningOrDoingActions(AprsSystem sys, int srn, String whenString) throws IllegalStateException {
        if (sys.isRunningCrclProgram()) {
            String msg = sys.getTaskName() + " is running crcl program when " + whenString + " : srn=" + srn
                    + ", toggleBlockerMap.keySet()=" + toggleBlockerMap.keySet();
            System.err.println("sys.getLastRunningProgramTrueInfo=" + sys.getLastRunningProgramTrueInfo());
            printSysDoingError(sys, msg);
            logEventErr(msg);
            throw new IllegalStateException(msg);
        }
        if (sys.isAborting()) {
            String msg = sys.getTaskName() + " is aborting when " + whenString + " : srn=" + srn
                    + ". toggleBlockerMap.keySet()=" + toggleBlockerMap.keySet();
            printSysDoingError(sys, msg);
            logEventErr(msg);
            throw new IllegalStateException(msg);
        }
        if (sys.isDoingActions()) {
            String msg = sys.getTaskName() + " is doing actions when " + whenString + " : srn=" + srn
                    + ", toggleBlockerMap.keySet()=" + toggleBlockerMap.keySet();
            printSysDoingError(sys, msg);
            logEventErr(msg);
            String doingActionsInfo = sys.getIsDoingActionsInfo();
            println("");
            println("doingActionsInfo = " + doingActionsInfo);
            println("disallowTogglesCount = " + disallowTogglesCount);
            println("toggleBlockerMap.keySet() = " + toggleBlockerMap.keySet());
            println("allowTogglesCount = " + allowTogglesCount);
            println("togglesAllowed = " + togglesAllowed);
            println("lastDisallowTogglesFuture = " + lastDisallowTogglesFuture);
            println("lastDisallowTogglesTrace = " + Arrays.toString(lastDisallowTogglesTrace));
            println("");

            System.err.println("");
            System.err.println("doingActionsInfo = " + doingActionsInfo);
            System.err.println("disallowTogglesCount = " + disallowTogglesCount);
            System.err.println("toggleBlockerMap.keySet() = " + toggleBlockerMap.keySet());
            System.err.println("allowTogglesCount = " + allowTogglesCount);
            System.err.println("togglesAllowed = " + togglesAllowed);
            System.err.println("lastDisallowTogglesFuture = " + lastDisallowTogglesFuture);
            System.err.println("lastDisallowTogglesTrace = " + Arrays.toString(lastDisallowTogglesTrace));
            System.err.println("");

            throw new IllegalStateException(msg);
        }
    }

    private void printSysDoingError(AprsSystem sys, String msg) {
        println("");
        System.err.println("");
        System.err.println("printSysDoingError: msg=" + msg);
        Thread.dumpStack();
        println("");
        System.err.println("sys = " + sys);
        XFuture<Boolean> continueFuture = sys.getContinueActionListFuture();
        XFuture<Boolean> startFuture = sys.getLastStartActionsFuture();
        sys.printAbortingInfo(System.err);
        if (null != lastStealRobotsInternalBeforeAllowToggles && !lastStealRobotsInternalBeforeAllowToggles.isDone()
                && !lastStealRobotsInternalBeforeAllowToggles.isCompletedExceptionally()
                && !lastStealRobotsInternalBeforeAllowToggles.isCancelled()) {
            System.err
                    .println("lastStealRobotsInternalBeforeAllowToggles=" + lastStealRobotsInternalBeforeAllowToggles);
            System.err.println("lastStealRobotsInternalBeforeAllowTogglesTrace="
                    + Utils.traceToString(lastStealRobotsInternalBeforeAllowTogglesTrace));
        }
        if (null != lastStartReverseActionsFuture && !lastStartReverseActionsFuture.isDone()
                && !lastStartReverseActionsFuture.isCompletedExceptionally()
                && !lastStartReverseActionsFuture.isCancelled()) {
            System.err.println("lastStartReverseActionsFuture=" + lastStartReverseActionsFuture);
            System.err.println("lastStartReverseActionsTrace=" + Utils.traceToString(lastStartReverseActionsTrace));
        }
        if (null != startAllFuture && !startAllFuture.isDone() && !startAllFuture.isCompletedExceptionally()
                && !startAllFuture.isCancelled()) {
            System.err.println("startAllFuture=" + startAllFuture);
            System.err.println("startAllTrace=" + Utils.traceToString(startAllTrace));
        }
        if (null != startAllActionsFuture && !startAllActionsFuture.isDone()
                && !startAllActionsFuture.isCompletedExceptionally() && !startAllActionsFuture.isCancelled()) {
            System.err.println("startAllActionsFuture=" + startAllActionsFuture);
            System.err.println("startAllActionsTrace=" + Utils.traceToString(startAllActionsTrace));
        }
        System.err.println("sys.getLastStartActionsFuture() = " + startFuture);
        if (null != startFuture && !startFuture.isDone()) {
            System.err.println("startFuture.forExceptionString() = " + startFuture.forExceptionString());
        }
        System.err.println("sys.getContinueActionListFuture() = " + continueFuture);
        if (null != continueFuture && !continueFuture.isDone()) {
            System.err.println("continueFuture.forExceptionString() = " + continueFuture.forExceptionString());
        }

        println("");
        System.err.println("");
        debugAction();
        println("");
        System.err.println("");
    }

    private void setupUnstealRobots(int srn, AprsSystem stealFor, AprsSystem stealFrom, String stealForRobotName,
            @Nullable GraphicsDevice gd) {
        unStealRobotsFunction
                .set((Integer ecc) -> executeUnstealRobots(srn, stealFor, stealFrom, stealForRobotName, gd, ecc));
    }

    private volatile @Nullable XFutureVoid executeUnstealRobotsFuture = null;

    private final ConcurrentLinkedDeque<XFuture<?>> allFuturesDeque = new ConcurrentLinkedDeque<>();

    private void cleanAllFuturesSet() {
        List<XFuture<?>> futuresToRemove = new ArrayList<>();
        for (XFuture<?> future : allFuturesDeque) {
            if (future.isCancelled() || future.isCompletedExceptionally() || future.isDone()) {
                futuresToRemove.add(future);
            }
        }
        allFuturesDeque.removeAll(futuresToRemove);
    }

    public void addToAllFuturesSet(XFuture<?> future) {
        cleanAllFuturesSet();
        allFuturesDeque.add(future);
    }

    public void cancelAllFuturesSet() {
        for (XFuture<?> future : allFuturesDeque) {
            future.cancelAll(false);
        }
        cleanAllFuturesSet();
    }

    private XFutureVoid executeUnstealRobots(final int srn, AprsSystem stealFor, AprsSystem stealFrom,
            String stealForRobotName, @Nullable GraphicsDevice gd, int ecc) {
        if (restoringOrigRobotInfo) {
            throw new IllegalStateException("restoringOrigRobotInfo");
        }
        if (checkEcc(ecc)) {
            return cancelledEcc(ecc);
        }
        if (checkSrn(srn)) {
            return cancelledSrn(srn);
        }
        String revBlocker = "reverseRobotTransfer" + reverseRobotTransferNumber.incrementAndGet() + ",srn=" + srn
                + ",ecc=" + ecc;
        logEvent("Reversing robot transfer after robot reenabled." + " : srn=" + srn + ",ecc=" + ecc);
        XFuture<LockInfo> disallowTogglesFuture = disallowToggles(revBlocker, stealFor, stealFrom);
        addToAllFuturesSet(disallowTogglesFuture);
        XFutureVoid ret = disallowTogglesFuture.thenComposeAsyncToVoid((LockInfo ignored) -> {
            if (checkEcc(ecc)) {
                return cancelledEcc(ecc);
            }
            if (checkSrn(srn)) {
                return cancelledSrn(srn);
            }
            return completeExecuteUnstealRobots(revBlocker, srn, stealFor, stealFrom, stealForRobotName, gd, ecc);
        }, supervisorExecutorService);
        executeUnstealRobotsFuture = ret;
        addToAllFuturesSet(ret);
        return ret;
    }

    private final AtomicInteger completeExecuteUnstealRobotsCount = new AtomicInteger();

    private synchronized XFutureVoid completeExecuteUnstealRobots(String revBlocker, final int srn, AprsSystem stealFor,
            AprsSystem stealFrom, String stealForRobotName, @Nullable GraphicsDevice gd, int ecc) {
        clearStealingRobotsFlag();
        int count = completeExecuteUnstealRobotsCount.incrementAndGet();
        logEvent("Start completeExecuteUnstealRobots(" + revBlocker + "," + srn + "," + stealFor + "," + stealFrom + ","
                + stealForRobotName + ",...) : count =" + count + ",ecc=" + ecc);
        logEvent("Starting safe abort and disconnect for " + stealFor + " : srn=" + srn + ",ecc=" + ecc);
        logEvent("    and starting safe abort and disconnect for" + stealFrom + " : srn=" + srn + ",ecc=" + ecc);
        final XFutureVoid disconnectBothFuture = unstealAbortAndDisconnectBoth(stealFor, srn, ecc, stealForRobotName,
                gd, stealFrom);
        this.unstealAbortFuture = disconnectBothFuture;
        XFutureVoid part1Future = disconnectBothFuture
                .thenComposeAsync("unsteal.returnRobots1" + " : srn=" + srn, x -> {
                    return returnRobots1("unsteal.returnRobots1" + " : srn=" + srn, stealFor, stealFrom, srn, ecc);
                }, supervisorExecutorService).thenRun("unsteal.connectAll" + " : srn=" + srn, this::connectAll)
                .alwaysComposeAsyncToVoid(() -> {
                    return allowToggles(revBlocker, stealFor, stealFrom);
                }, supervisorExecutorService);
        return part1Future
                .thenComposeAsyncToVoid("unsteal.continueAllOf" + " : srn=" + srn + ",ecc=" + ecc,
                        x -> completeUnstealAbortFuture(srn, stealFrom, stealFor, ecc),
                        supervisorExecutorService)
                .thenRun(() -> {
                    logEvent("Finished completeExecuteUnstealRobots(" + revBlocker + "," + srn + "," + stealFor + ","
                            + stealFrom + "," + stealForRobotName + ",...): count =" + count);
                });
    }

    private XFutureVoid unstealAbortAndDisconnectBoth(AprsSystem stealFor, final int srn, int ecc,
            String stealForRobotName, @Nullable GraphicsDevice gd, AprsSystem stealFrom) {
        final XFuture<Void> stealForDisconnectFuture = disconnectStealFor(stealFor, srn, ecc, stealForRobotName, gd);
        final XFutureVoid stealFromDisconnectFuture = disconnectStealFrom(stealFrom, srn, ecc);
        final XFutureVoid disconnectBothFuture = XFuture.allOfWithName("unStealAbortAllOf", stealFromDisconnectFuture,
                stealForDisconnectFuture);
        return disconnectBothFuture;
    }

    private XFutureVoid disconnectStealFrom(AprsSystem stealFrom, final int srn, int ecc) {
        final XFutureVoid stealFromDisconnectFuture = stealFrom
                .startSafeAbortAndDisconnect(
                        "unStealAbortAllOf.stealFrom" + stealFrom + " : srn=" + srn + ",ecc=" + ecc)
                .thenRunAsync(() -> logEvent(
                "Safe abort and disconnect completed for " + stealFrom + " : srn=" + srn + ",ecc=" + ecc),
                        supervisorExecutorService);
        return stealFromDisconnectFuture;
    }

    private XFuture<Void> disconnectStealFor(AprsSystem stealFor, final int srn, int ecc, String stealForRobotName,
            @Nullable GraphicsDevice gd) {
        final XFuture<Void> stealForDisconnectFuture = stealFor
                .startSafeAbortAndDisconnect("unStealAbortAllOf.stealFor " + stealFor + " : srn=" + srn + ",ecc=" + ecc)
                .thenComposeAsync("unstealShowReenable", x -> {
                    logEvent("Safe abort and disconnect completed for " + stealFor + " : srn=" + srn + ",ecc=" + ecc);
                    if (checkEcc(ecc)) {
                        return cancelledEcc(ecc);
                    }
                    if (checkSrn(srn)) {
                        return cancelledSrn(srn);
                    }
                    boolean stillAborting = stealFor.isAborting();
                    if (stillAborting) {
                        String msg = "still aborting after safe abort and disconnect" + " : srn=" + srn;
                        logEvent(msg);
                        boolean doubleCheck = stealFor.isAborting();
                        throw new IllegalStateException(msg);
                    }
                    writeToColorTextSocket("0x00FF00, 0x00FF00\r\n".getBytes());
                    return showMessageFullScreen(stealForRobotName + "\n Enabled", 80.0f,
                            SplashScreen.getDisableImageImage(), SplashScreen.getBlueWhiteGreenColorList(), gd);
                }, supervisorExecutorService);
        return stealForDisconnectFuture;
    }

    private synchronized XFutureVoid completeUnstealAbortFuture(final int srn, AprsSystem stealFrom,
            AprsSystem stealFor, int ecc) {
        int curSrn = stealRobotNumber.get();
        if (srn != curSrn) {
            logEvent("unsteal.continueAllOf srn=" + srn + ", curSrn=" + curSrn);
            return XFutureVoid
                    .completedFutureWithName("unsteal.continueAllOf.srn != stealRobotNumber.get()" + " : srn=" + srn);
        }
        if (stealFrom.isAborting()) {
            logEvent("unsteal.continueAllOf stealFrom.isAborting() : srn=" + srn + ", curSrn=" + curSrn + ", stealFrom="
                    + stealFrom);
            return XFutureVoid
                    .completedFutureWithName("unsteal.continueAllOf.stealFrom.isAborting()" + " : srn=" + srn);
        }
        if (stealFor.isAborting()) {
            logEvent("unsteal.continueAllOf stealFor.isAborting() : srn=" + srn + ", curSrn=" + curSrn + ", stealFor="
                    + stealFrom);
            return XFutureVoid.completedFutureWithName("unsteal.continueAllOf.stealFor.isAborting()" + " : srn=" + srn);
        }
        logEvent("unsteal.continueAllOf Continue actions for " + stealFrom.getTaskName() + " with "
                + stealFrom.getRobotName() + " : srn=" + srn);
        logEvent("unsteal.continueAllOf Continue actions for " + stealFor.getTaskName() + " with "
                + stealFor.getRobotName() + " : srn=" + srn);
        XFuture<Boolean> stealFromFinishFuture = stealFrom.continueActionList("unsteal.stealFrom" + " : srn=" + srn)
                .thenComposeAsync(x2 -> {
                    logEvent("unsteal.stealFrom " + stealFrom.getRunName()
                            + " completed action list after return after robot reenabled. " + x2 + " : srn=" + srn);

                    return finishAction(stealFrom).thenApply(x3 -> {
                        logEvent("finish unsteal.stealFrom " + stealFrom.getRunName() + " completed action list " + x2
                                + " : srn=" + srn);
                        if (x2 && !stealFrom.isAborting() && srn == stealRobotNumber.get()) {
                            completeSystemsContinueIndFuture(stealFrom, !stealFrom.isReverseFlag(), ecc);
                        }
                        return x2;
                    });
                }, supervisorExecutorService);
        XFuture<Boolean> stealForFinishFuture = stealFor
                .continueActionList("unsteal.stealFor" + " : srn=" + srn + ",ecc=" + ecc)
                .peekNoCancelException(this::handleXFutureException).thenComposeAsync(x3 -> {
            logEvent("unsteal.stealFor " + stealFor.getRunName()
                    + " completed action list after return after robot reenabled. " + x3 + " : srn=" + srn
                    + ",ecc=" + ecc);
            return finishAction(stealFrom).thenApply(x4 -> {
                logEvent("finish unsteal.stealFor " + stealFor.getRunName() + " completed action list " + x3
                        + " : srn=" + srn);
                if (x3 && !stealFor.isAborting() && srn == stealRobotNumber.get()) {
                    completeSystemsContinueIndFuture(stealFor, !stealFor.isReverseFlag(), ecc);
                }
                return x3;
            });
        }, supervisorExecutorService);
        return XFuture.allOf(stealFromFinishFuture, stealForFinishFuture);
    }

    private String assertFail() {
        logEvent("assertFail");
        pause();
        return "";
    }

    private void completeSystemsContinueIndFuture(AprsSystem sys, boolean value, int ecc) {
        assert (null != sys) : assertFail() + "sys == null";
        String sysRobotName = sys.getRobotName();
        assert (sysRobotName != null) : assertFail() + "sys.getRobotName() == null: sys=" + sys + " @AssumeAssertion(nullness)";
        assert (sysRobotName.length() > 0) : assertFail() + "sys.getRobotName().length() <= 0 : sys=" + sys;
        assert (sys.isConnected()) : assertFail() + "!sys.isConnected() : sys=" + sys;
        assert (!sys.isAborting()) : assertFail() + "sys.isAborting() : sys=" + sys;
        checkAllUniques();
        if (!sys.readyForNewActionsList()) {
            System.err.println("Completing future for " + sys + " when not ready");
        }
        logEvent("Checking systemContinueMap for " + sys);
        AtomicReference<XFuture<Boolean>> ref = new AtomicReference<>();
        XFuture<Boolean> f = systemContinueMap.replace(sys.getMyThreadId(), new XFuture<>("systemContinueMap." + sys));
        if (null != f) {
            logEvent("Completing " + f + " with " + value + " for " + sys);
            f.complete(value);
        }
    }

    private static final DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    /**
     * Convert a timestamp in milliseconds since 1970 to the default time string
     * format.
     *
     * @param ms timestamp in milliseconds
     * @return formatted string
     */
    private static String getTimeString(long ms) {
        Date date = new Date(ms);
        return timeFormat.format(date);
    }

    private volatile @Nullable PrintStream logPrintStream = null;

    private int eventsDisplayMax = 500;

    /**
     * Get the value of eventsDisplayMax
     *
     * @return the value of eventsDisplayMax
     */
    public int getEventsDisplayMax() {
        return eventsDisplayMax;
    }

    /**
     * Set the value of eventsDisplayMax
     *
     * @param eventsDisplayMax new value of eventsDisplayMax
     */
    public void setEventsDisplayMax(int eventsDisplayMax) {
        this.eventsDisplayMax = eventsDisplayMax;
    }

    private volatile javax.swing.@Nullable Timer runTimeTimer = null;

    volatile int maxEventStringLen = 0;

    private void logEventPrivate(long time, String s, int blockerSize, int ecc, int cdc, int errs,
            StackTraceElement trace[], String threadname) {
        if (closing) {
            return;
        }
        if (firstEventTime > 0) {
            updateRunningTime();
            if (SwingUtilities.isEventDispatchThread()) {
                startUpdateRunningTimeTimerOnDisplay();
            } else {
                startUpdateRunningTimeTimer();
            }
        }
        String timeString = getTimeString(time);
        if (null == logPrintStream) {
            try {
                File supervisorDir = Utils.file(Utils.getlogFileDir(), "supervisor");
                supervisorDir.mkdirs();
                File logFile = Utils.createTempFile("events_log_", ".txt", supervisorDir);
                println("Supervisor event log file =" + logFile.getCanonicalPath());
                logPrintStream = new PrintStream(new FileOutputStream(logFile));
                logPrintStream.println("timeString \t blockers \t ecc \t cdc \t errs \t s \t threadname\ttrace");
            } catch (IOException ex) {
                Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
        String noRetsS = s.replaceAll("[\r\n\t]+", "_");
        String fullLogString = timeString + " \t" + blockerSize + " \t" + ecc + " \t" + cdc + " \t" + errs + " \t\""
                + Utils.clipString(noRetsS, 350) + "\" \t\"" + Utils.clipString(threadname, 40) + "\" \t\""
                + Utils.traceToShortString(trace, 60) + "\"";
        if (null != logPrintStream) {
            logPrintStream.println(fullLogString);
        }
        AprsCommonLogger.instance().getOrigSystemOut().println(fullLogString);
        if (null != displayJFrame) {
            displayJFrame.addOldEventToTable(time, blockerSize, ecc, cdc, errs, s, threadname,
                    Utils.traceToShortString(trace, 60));
        }
    }

    private XFutureVoid startUpdateRunningTimeTimer() {
        if (closing) {
            return XFutureVoid.completedFuture();
        }
        if (runTimeTimer == null) {
            return runOnDispatchThread(() -> {
                startUpdateRunningTimeTimerOnDisplay();
            });
        } else {
            return XFutureVoid.completedFuture();
        }
    }

    @UIEffect
    private void startUpdateRunningTimeTimerOnDisplay() {
        assert SwingUtilities.isEventDispatchThread();
        if (closing) {
            return;
        }
        if (runTimeTimer == null) {
            Timer newRunTimeTimer = new Timer(2000, x -> updateRunningTime());
            newRunTimeTimer.start();
            runTimeTimer = newRunTimeTimer;
        }
    }

    private void updateRunningTime() {
        if (firstEventTime > 0 && !isPauseSelected()) {

            if (null != displayJFrame) {
                displayJFrame.updateRunningTime();
            }
        }
    }

    // private void scrollToEnd(JTable jTable) {
//        int index = jTable.getRowCount() - 1;
//        jTable.getSelectionModel().setSelectionInterval(index, index);
//        jTable.scrollRectToVisible(new Rectangle(jTable.getCellRect(index, 0, true)));
//    }
    private volatile long firstEventTime = -1;
    private volatile long abortEventTime = -1;

    private static class LogEventInfo {

        private final long t;
        private final int blockersSize;
        private final int ecc;
        private final int cdc;
        private final int errs;
        private final String threadname;
        private final String text;
        private final Supervisor supervisor;
        private final StackTraceElement trace[];

        public LogEventInfo(long t, int blockersSize, int ecc, int cdc, int errs, String threadname, String text,
                StackTraceElement trace[], Supervisor supervisor) {
            this.t = t;
            this.blockersSize = blockersSize;
            this.ecc = ecc;
            this.cdc = cdc;
            this.errs = errs;
            this.threadname = threadname;
            this.text = text;
            this.trace = trace;
            this.supervisor = supervisor;
        }

        public void update() {
            supervisor.logEventPrivate(t, text, blockersSize, ecc, cdc, errs, trace, threadname);
        }
    }

    private final ConcurrentLinkedDeque<LogEventInfo> logEvents = new ConcurrentLinkedDeque<>();

    private static final Consumer<ConcurrentLinkedDeque<LogEventInfo>> logEventsConsumer = (
            ConcurrentLinkedDeque<LogEventInfo> logEvents) -> {
        LogEventInfo eventInfo = logEvents.pollFirst();
        while (null != eventInfo) {
            eventInfo.update();
            eventInfo = logEvents.pollFirst();
        }
    };

    public XFutureVoid logEvent(boolean isError, String s) {
        if (isError) {
            return logEventErr(s);
        } else {
            return logEvent(s);
        }
    }

    /**
     * Log an event string to be displayed with timestamp in event table.
     *
     * @param s string to log
     */
    public XFutureVoid logEvent(String s) {
        if (closing) {
            return XFutureVoid.completedFuture();
        }
        long t = System.currentTimeMillis();
        if (firstEventTime < 0) {
            firstEventTime = t;
        }
        int blockersSize = getToggleBlockerMapSize();
        int ecc = enableChangeCount.get();
        int cdc = ContinuousDemoCycle.get();
        int errs = logEventErrCount.get();
        String threadname = Thread.currentThread().getName();
        StackTraceElement trace[] = Thread.currentThread().getStackTrace();
        LogEventInfo eventInfo = new LogEventInfo(t, blockersSize, ecc, cdc, errs, threadname, s, trace, this);
        if (displayUpdatesDisabled) {
            logEventPrivate(t, s, blockersSize, ecc, cdc, errs, trace, threadname);
            return XFutureVoid.completedFuture();
        }
        logEvents.add(eventInfo);
        if (SwingUtilities.isEventDispatchThread()) {
            logEventsConsumer.accept(logEvents);
            return XFutureVoid.completedFuture();
        } else {
            return submitDisplayConsumer(logEventsConsumer, logEvents);
        }
    }

    private int getToggleBlockerMapSize() {
        return toggleBlockerMap.keySet().size();
    }

    private void initColorTextSocket() throws IOException {
        if (null != displayJFrame) {
            displayJFrame.initColorTextSocket();
        }
    }

    private final Map<String, String> robotTaskMap = new HashMap<>();

    private static File initLastSetupFileFile() {
        try {
            final String aprsUserHomeDir = Utils.getAprsUserHomeDir();
            final String property = System.getProperty("aprsLastMultiSystemSetupFile",
                    aprsUserHomeDir + File.separator + ".lastAprsSetupFile.txt");
            return Utils.file(property);
        } catch (Exception ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }

    private final static File LAST_SETUP_FILE_FILE = initLastSetupFileFile();
    private final static File LAST_SHARED_TOOLS_FILE_FILE = initLastSharedToolsFileFile();

    private static File initLastSharedToolsFileFile() {
        try {
            return Utils.file(
                    System.getProperty("aprsLastMultiSystemSharedToolsFile",
                            Utils.getAprsUserHomeDir() + File.separator + ".lastAprsSharedToolsFile.txt"));
        } catch (Exception ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }

    private static final String LAST_APRS_SIM_TEACH_FILETXT = ".lastAprsSimTeachFile.txt";
    private final static File LAST_SIM_TEACH_FILE_FILE = initLastSimTeachFile();

    private static File initLastSimTeachFile() {
        try {
            String fileName = System.getProperty("aprsLastMultiSystemSimTeachFile",
                    Utils.getAprsUserHomeDir() + File.separator + LAST_APRS_SIM_TEACH_FILETXT);
            return Utils.file(fileName);
        } catch (Exception ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }

    private final static File LAST_SIM_TEACH_PROPERTIES_FILE_FILE = initLastSimTeachPropertiesFile();

    private static File initLastSimTeachPropertiesFile() {
        try {
            return Utils.file(
                    System.getProperty("aprsLastMultiSystemTeachPropertiesFile",
                            Utils.getAprsUserHomeDir() + File.separator + ".lastAprsTeachPropertiesFile.txt"));
        } catch (Exception ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }

    private final static File LAST_POSMAP_FILE_FILE = initLastPosMapFile();

    private static File initLastPosMapFile() {
        try {
            return Utils.file(System.getProperty("aprsLastMultiSystemPosMapFile",
                    Utils.getAprsUserHomeDir() + File.separator + ".lastAprsPosMapFile.txt"));
        } catch (Exception ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }

    private @Nullable
    File chooseFileForSaveAs(@Nullable File prevChooserFile) throws HeadlessException, IOException {
        if (null == displayJFrame) {
            throw new IllegalStateException("null == displayJFrame");
        }
        return displayJFrame.chooseFileForSaveAs(lastSetupFile);
    }

    /**
     * Query the user to select a file to save setup data in.
     *
     * @throws IOException
     * @throws HeadlessException
     */
    public void browseSaveSetupAs() throws HeadlessException, IOException {
        File chosenFile = chooseFileForSaveAs(lastSetupFile);
        if (null != chosenFile) {
            try {
                saveSetupFile(chosenFile);

            } catch (IOException ex) {
                log(Level.SEVERE, "", ex);
            }
        }
    }

    /**
     * Add a system to show and update the tasks and robots tables.
     *
     * @param sys system to add
     */
    void addAprsSystem(AprsSystem sys) {
        sys.setPriority(aprsSystems.size() + 1);
        sys.setVisible(true);
        sys.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        sys.setSupervisorEventLogger(this::logEvent);
        sys.setSupervisor(this);
        aprsSystems.add(sys);
        sys.getTitleUpdateRunnables().add(() -> {
            updateTasksTableOnSupervisorService();
        });
        updateTasksTable();
    }

    private static String canonicalPathOrBuildPath(@Nullable File f, String dirName, String filename)
            throws IOException {
        if (null != f) {
            return f.getCanonicalPath();
        }
        return dirName + File.separator + filename;
    }

    public File getSetupFile() throws IOException {
        if (null != setupFile) {
            return setupFile;
        }
        File ret = Utils.file(getSetupFilePathString());
        setSetupFile(ret);
        return ret;
    }

    private volatile @MonotonicNonNull String setupFileDirName = null;

    public String getSetupFileDirName() throws IOException {
        if (null != setupFileDirName) {
            return setupFileDirName;
        }
        if (null != setupFile) {
            setupFileDirName = getDirNameOrHome(setupFile);
            return setupFileDirName;
        }
        File f = Supervisor.getLastSetupFile(null);
        setupFileDirName = getDirNameOrHome(f);
        return setupFileDirName;
    }

    public static String getStaticSetupFileDirName() throws IOException {
        File f = Supervisor.getLastSetupFile(null);
        return getDirNameOrHome(f);
    }

    public String getSetupFilePathString() throws IOException {
        if (null != setupFile) {
            return setupFile.getCanonicalPath();
        }
        return getStaticSetupFilePathString();
    }

    @UIEffect
    public static XFuture<Supervisor> openAll(@Nullable Supervisor supervisor, @Nullable Frame parent,
            @Nullable String dirName) throws IOException {
        Map<String, MultiFileDialogInputFileInfo> filesMapIn = new HashMap<>();
        if (null != supervisor) {
            filesMapIn.put("Setup", new MultiFileDialogInputFileInfo(supervisor.getSetupFilePathString()));
            filesMapIn.put("PosMap", new MultiFileDialogInputFileInfo(supervisor.getPosMapFilePathString()));
            filesMapIn.put("SimTeach", new MultiFileDialogInputFileInfo(supervisor.getSimTeachFilePathString()));
            filesMapIn.put("TeachProps", new MultiFileDialogInputFileInfo(supervisor.getTeachPropsFilePathString()));
            filesMapIn.put("SharedTools", new MultiFileDialogInputFileInfo(supervisor.getSharedToolsFilePathString()));
        } else {
            if (null == dirName) {
                dirName = Supervisor.getStaticSetupFileDirName();
                if (null != parent) {
                    JFileChooser chooser = new JFileChooser(dirName);
                    chooser.setDialogTitle("Choose APRS Multi Supervisor Base Directory.");
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                        final File selectedFile = requireNonNull(chooser.getSelectedFile(),
                                "chooser.getSelectedFile()");
                        dirName = selectedFile.getCanonicalPath();
                    }
                }
            }
            filesMapIn.put("Setup", new MultiFileDialogInputFileInfo(Supervisor.getStaticSetupFilePathString(dirName)));
            filesMapIn.put("PosMap",
                    new MultiFileDialogInputFileInfo(Supervisor.getStaticPosMapFilePathString(dirName)));
            filesMapIn.put("SimTeach",
                    new MultiFileDialogInputFileInfo(Supervisor.getStaticSimTeachFilePathString(dirName)));
            filesMapIn.put("TeachProps",
                    new MultiFileDialogInputFileInfo(Supervisor.getStaticTeachPropsFilePathString(dirName)));
            filesMapIn.put("SharedTools",
                    new MultiFileDialogInputFileInfo(Supervisor.getStaticSharedToolsFilePathString(dirName)));
        }

        Map<String, String> filesMapOut = MultiFileDialogJPanel.showMultiFileDialog(parent, "Open All ...", true,
                filesMapIn);
        if (null != supervisor) {
            if (null != filesMapOut) {
                String setup = filesMapOut.get("Setup");
                if (null != setup) {
                    Map<String, String> filesMapOutNN = filesMapOut;
                    Supervisor supervisorNN = supervisor;
                    return supervisorNN.loadSetupFile(Utils.file(setup)).thenRun(() -> {
                        supervisorNN.loadFromFilesMap(filesMapOutNN);
                    }).thenApply(x -> supervisorNN);
                } else {
                    throw new RuntimeException("No supervisor setup file selected.");
                }
            } else {
                throw new RuntimeException("No supervisor setup file selected.");
            }
        } else if (null != filesMapOut) {
            return Supervisor.createAprsSupervisorWithSwingDisplay(filesMapOut);
        } else {
            throw new RuntimeException("No supervisor setup file selected.");
        }
    }

    private void loadFromFilesMap(Map<String, String> filesMapOutNN) throws RuntimeException {
        try {
            String posMapsFilesFile = filesMapOutNN.get("PosMap");
            if (null != posMapsFilesFile) {
                loadPositionMappingsFilesFile(Utils.file(posMapsFilesFile));
            }
            String simTeach = filesMapOutNN.get("SimTeach");
            if (null != simTeach) {
                loadSimTeach(Utils.file(simTeach));
            }

            String teachProps = filesMapOutNN.get("TeachProps");
            if (null != teachProps) {
                loadTeachProps(Utils.file(teachProps));
            }
            String sharedTools = filesMapOutNN.get("SharedTools");
            if (null != sharedTools) {
                loadSharedTools(Utils.file(sharedTools));
            }
        } catch (Exception ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", ex);
            throw asRuntimeException(ex);
        }
    }

    public static String getStaticSetupFilePathString() throws IOException {
        File lastFile = Supervisor.getLastSetupFile(null);
        String dirName = getDirNameOrHome(lastFile);
        return canonicalPathOrBuildPath(lastFile, dirName, SETUPCSV);
    }

    private static final String SETUPCSV = "setup.csv";

    public static String getStaticSetupFilePathString(String dirName) throws IOException {
        File lastFile = Supervisor.getLastSetupFile(dirName);
        return canonicalPathOrBuildPath(lastFile, dirName, SETUPCSV);
    }

    public String getPosMapFilePathString() throws IOException {
        String dirName = getSetupFileDirName();
        File f = Supervisor.getLastPositionMappingsFilesFile(dirName);
        return canonicalPathOrBuildPath(f, dirName, POSMAPCSV);
    }

    private static final String POSMAPCSV = "posmap.csv";

    public static String getStaticPosMapFilePathString(String dirName) throws IOException {
        File f = Supervisor.getLastPositionMappingsFilesFile(dirName);
        return canonicalPathOrBuildPath(f, dirName, POSMAPCSV);
    }

    private volatile @MonotonicNonNull File posMapFile = null;

    public File getPosMapFile() throws IOException {
        if (null != posMapFile) {
            return posMapFile;
        }
        posMapFile = Utils.file(getPosMapFilePathString());
        return posMapFile;
    }

    public String getSimTeachFilePathString() throws IOException {
        String dirName = getSetupFileDirName();
        File f = getLastSimTeachFile();
        return canonicalPathOrBuildPath(f, dirName, SIM_TEACHCSV);
    }

    private static final String SIM_TEACHCSV = "simTeach.csv";

    public static String getStaticSimTeachFilePathString(String dirName) throws IOException {
//        String dirName = getStaticSetupFileDirName();
        File f = Supervisor.getStaticLastSimTeachFile(dirName);
        return canonicalPathOrBuildPath(f, dirName, SIM_TEACHCSV);
    }

    private volatile @MonotonicNonNull File simTeachFile = null;

    public File getSimTeachFile() throws IOException {
        if (null != simTeachFile) {
            return simTeachFile;
        }
        simTeachFile = Utils.file(getSimTeachFilePathString());
        return simTeachFile;
    }

    public String getTeachPropsFilePathString() throws IOException {
        String dirName = getSetupFileDirName();
        File f = Supervisor.getLastTeachPropertiesFile(dirName);
        return canonicalPathOrBuildPath(f, dirName, TEACH_PROPSTXT);
    }

    private static final String TEACH_PROPSTXT = "teachProps.txt";

    public static String getStaticTeachPropsFilePathString(String dirName) throws IOException {
//        String dirName = getStaticSetupFileDirName();
        File f = Supervisor.getLastTeachPropertiesFile(dirName);
        return canonicalPathOrBuildPath(f, dirName, TEACH_PROPSTXT);
    }

    private volatile @MonotonicNonNull File teachPropsFile = null;

    public File getTeachPropsFile() throws IOException {
        if (null != teachPropsFile) {
            return teachPropsFile;
        }
        teachPropsFile = Utils.file(getTeachPropsFilePathString());
        return teachPropsFile;
    }

    public String getSharedToolsFilePathString() throws IOException {
        String dirName = getSetupFileDirName();
        File f = Supervisor.getLastSharedToolsFile(dirName);
        return canonicalPathOrBuildPath(f, dirName, SHARED_TOOLSCSV);
    }

    private static final String SHARED_TOOLSCSV = "sharedTools.csv";

    public static String getStaticSharedToolsFilePathString(String dirName) throws IOException {
//        String dirName = getSetupFileDirName();
        File f = Supervisor.getLastSharedToolsFile(dirName);
        return canonicalPathOrBuildPath(f, dirName, SHARED_TOOLSCSV);
    }

    private volatile @MonotonicNonNull File sharedToolsFile = null;

    public File getSharedToolsFile() throws IOException {
        if (null != sharedToolsFile) {
            return sharedToolsFile;
        }
        sharedToolsFile = Utils.file(getSharedToolsFilePathString());
        return sharedToolsFile;
    }

    private volatile @Nullable XFuture<?> lastFutureReturned = null;

    // private AtomicInteger dispatchCount = new AtomicInteger();
//    private AtomicInteger dispatchPending = new AtomicInteger();
//    private AtomicLong dispatchTime = new AtomicLong();
//    private volatile long maxStartPending = 0;
    private volatile boolean displayUpdatesDisabled = false;
//
//    public class DisplayUpdater<T> {
//
//        private final Consumer<T> consumer;
//        private final AtomicReference<T> ref = new AtomicReference<>();
//        private volatile @Nullable
//        XFutureVoid lastFuture = null;
//
//        public DisplayUpdater(Consumer<T> consumer) {
//            this.consumer = consumer;
//        }
//
//        @SuppressWarnings("nullness")
//        public synchronized XFutureVoid submit(T val) {
//            if (lastFuture == null || lastFuture.isDone()) {
//                ref.set(val);
//                lastFuture = runOnDispatchThread(() -> {
//                    T latestVal = ref.getAndSet(null);
//                    if (latestVal != null) {
//                        consumer.accept(latestVal);
//                    }
//                });
//                return lastFuture;
//            }
//            if (ref.compareAndSet(null, val)) {
//                lastFuture = runOnDispatchThread(() -> {
//                    T latestVal = ref.getAndSet(null);
//                    if (latestVal != null) {
//                        consumer.accept(latestVal);
//                    }
//                });
//            }
//            return lastFuture;
//        }
//    }
//
//    private final IdentityHashMap<Consumer<?>, DisplayUpdater<?>> displayUpdatersMap = new IdentityHashMap<>();

    @SuppressWarnings({"unchecked", "nullness", "keyfor"})
    public <T> XFutureVoid submitDisplayConsumer(Consumer<T> consumer, T value) {
        return Utils.runOnDispatchThread(() -> {
            consumer.accept(value);
        });
//        synchronized (displayUpdatersMap) {
//            DisplayUpdater<T> displayUpdater
//                    = (DisplayUpdater<T>) displayUpdatersMap.computeIfAbsent(consumer, (Consumer<?> key) -> new DisplayUpdater<>(key));
//            return displayUpdater.submit(value);
//        }
    }

    // private static String getRunOnDispatchThreadCaller() {
//        StackTraceElement trace[] = Thread.currentThread().getStackTrace();
//        for (int i = 0; i < trace.length; i++) {
//            StackTraceElement stackTraceElement = trace[i];
//            if (stackTraceElement.getClassName().startsWith("java.lang")) {
//                continue;
//            }
//            final String elementString = stackTraceElement.toString();
//            if (elementString.contains("unOnDispatch")) {
//                continue;
//            }
//            if (elementString.contains("submitDisplayConsumer")) {
//                continue;
//            }
//            if (elementString.contains("DisplayUpdater")) {
//                continue;
//            }
//            return elementString;
//        }
//        return "";
//    }
//    private final ConcurrentHashMap<String, Integer> callerMap = new ConcurrentHashMap<>();
    public XFutureVoid runOnDispatchThread(final @UI Runnable r) {
        return runOnDispatchThread("runOnDispatchThread", r);
    }

    public XFutureVoid runOnDispatchThread(String name, final @UI Runnable r) {
        if (closing || displayUpdatesDisabled) {
            return XFutureVoid.completedFuture();
        }
        return aprs.misc.Utils.runOnDispatchThread(name, r);
    }

    private XFutureVoid prepAndFinishToXFutureVoidOnDispatch(UiSupplier<XFutureVoid> supplier) {
        final XFuture<XFutureVoid> uiFuture = prepActions()
                .thenCompose(x -> Utils.supplyXVoidOnDispatchThread(supplier));
        return uiFuture
                .thenComposeAsyncToVoid(x -> x, supervisorExecutorService);
    }

    XFutureVoid prepActions() {
        return XFuture.supplyAsync("prepActions", this::prepActionsInternal, supervisorExecutorService)
                .thenComposeAsyncToVoid(x -> x, supervisorExecutorService);
    }

    XFutureVoid prepActionsInternal() {
        boolean origIgnoreFlag = ignoreTitleErrors.getAndSet(true);
        if (null != lastSafeAbortAllFuture) {
            lastSafeAbortAllFuture.cancelAll(true);
            lastSafeAbortAllFuture = null;
        }
        final XFuture<?> lastFutureReturnedFinal = lastFutureReturned;
        if (null != lastFutureReturnedFinal) {
            if (!lastFutureReturnedFinal.isDone() && !closing) {
                System.err.println("prepActions: Cancelling lastFutureReturned=" + lastFutureReturnedFinal);
                lastFutureReturnedFinal.cancelAll(true);
            }
            clearLFR();
        }
        if (null != executeUnstealRobotsFuture) {
            executeUnstealRobotsFuture.cancelAll(false);
            executeUnstealRobotsFuture = null;
        }
        if (null != fillTraysAndNextRepeatingFuture) {
            fillTraysAndNextRepeatingFuture.cancelAll(true);
            fillTraysAndNextRepeatingFuture = null;
        }
        if (null != conveyorTestFuture) {
            XFutureVoid xfv = conveyorTestFuture;
            if (xfv == mainFuture) {
                mainFuture = null;
            }
            conveyorTestFuture = null;
            xfv.cancelAll(true);
        }

        firstEventTime = -1;
        clearAllToggleBlockers();
        clearAllErrors();
        setPauseSelected(false);
        XFutureVoid immediateAbortAllFuture = immediateAbortAll("prepActions", true);
        return immediateAbortAllFuture.thenComposeAsyncToVoid(() -> {
            resumeForPrepOnly();
            if (!origIgnoreFlag) {
                ignoreTitleErrors.set(false);
            }
            abortEventTime = -1;
            if (keepDisabled) {
                return XFutureVoid.completedFuture();
            }
            return returnRobots1("prepActions", null, null, -1, -1);
        }, supervisorExecutorService);
    }

    private @Nullable
    File choosePositionMappingsFileForSaveAs(@Nullable File prevChosenFile) throws HeadlessException {
        if (null == displayJFrame) {
            throw new IllegalStateException("(null == displayJFrame)");
        }
        return displayJFrame.choosePositionMappingsFileForSaveAs(prevChosenFile);
    }

    // private void browseAndSavePositionMappings() throws HeadlessException {
//        File chosenFile = choosePositionMappingsFileForSaveAs(lastPosMapFile);
//        if (null != chosenFile) {
//            try {
//                savePositionMaps(chosenFile);
//            } catch (IOException ex) {
//                log(Level.SEVERE, "", ex);
//            }
//        }
//    }
    private @Nullable
    File choosePosMapsFileToOpen(@Nullable File prevChosenFile) throws HeadlessException {
        if (null == displayJFrame) {
            throw new IllegalStateException("null == displayJFrame");
        }
        return displayJFrame.choosePosMapsFileToOpen(prevChosenFile);
    }

    /**
     * Query the user to select a posmap file to read. The posmap file is a CSV
     * file that points to other csv files with infomation needed to transform
     * coordinates from one robot to another.
     */
    public void browseOpenPositionMappingsFilesFile() {
        File chosenFile = choosePosMapsFileToOpen(lastPositionMappingsFilesFile);
        if (null != chosenFile) {
            try {
                loadPositionMappingsFilesFile(chosenFile);

            } catch (IOException ex) {
                log(Level.SEVERE, "", ex);
            }
        }
    }

    // private static void setChooserFile(@Nullable File file, JFileChooser chooser)
    // {
//        if (file != null) {
//            File parentFile = file.getParentFile();
//            if (null != parentFile) {
//                chooser.setCurrentDirectory(parentFile);
//                chooser.setSelectedFile(file);
//            }
//        }
//    }
    private volatile @Nullable XFutureVoid lastSafeAbortAllFuture = null;

    private volatile @Nullable NamedFunction<Integer, XFutureVoid> safeAbortReturnRobot = null;

    final private AtomicInteger abortCount = new AtomicInteger();

    private final AtomicBoolean ignoreTitleErrors = new AtomicBoolean(false);

    public @Nullable
    XFutureVoid getLastSafeAbortAllFuture() {
        return lastSafeAbortAllFuture;
    }

    private void setLastSafeAbortAllFuture(XFutureVoid lastSafeAbortAllFuture) {
        this.lastSafeAbortAllFuture = lastSafeAbortAllFuture;
    }

    XFutureVoid fullAbortAll() {
        incrementAndGetAbortCount();
        ignoreTitleErrors.set(true);
        final XFuture<?> lastFutureReturnedFinal = lastFutureReturned;
        if (null != lastFutureReturnedFinal) {
            if (!lastFutureReturnedFinal.isDone() && !closing) {
                System.err.println("fullAbortAll: Cancelling lastFutureReturned=" + lastFutureReturnedFinal);
                lastFutureReturnedFinal.cancelAll(true);
            }
            lastFutureReturned = null;
//            clearLFR();
        }
        if (null != fillTraysAndNextRepeatingFuture) {
            fillTraysAndNextRepeatingFuture.cancelAll(true);
            fillTraysAndNextRepeatingFuture = null;
        }
        if (null != conveyorTestFuture) {
            conveyorTestFuture.cancelAll(true);
            conveyorTestFuture = null;
        }

        if (null != lastSafeAbortAllFuture) {
            XFutureVoid xfv = lastSafeAbortAllFuture;
            lastSafeAbortAllFuture = null;
            xfv.cancelAll(true);
        }
        XFutureVoid xf = togglesAllowedXfuture.getAndSet(null);
        if (null != xf) {
            xf.cancelAll(true);
        }
        clearAllToggleBlockers();
        clearAllErrors();
        AprsSystem sysArray[] = systems().toArray(new AprsSystem[0]);
        final String blockerName = "fullAbortAll";
        Supplier<XFuture<LockInfo>> sup = () -> disallowToggles(blockerName, sysArray);
        XFuture<XFuture<LockInfo>> disallowTogglesFutureFuture = XFuture.supplyAsync("fullAbortDisableToggles", sup,
                supervisorExecutorService);
        XFuture<LockInfo> disallowTogglesFuture = disallowTogglesFutureFuture.thenCompose(x -> x);
        XFutureVoid immediateAbortAllFuture = disallowTogglesFuture
                .thenComposeAsyncToVoid(
                        x -> immediateAbortAll("fullAbortAll"),
                        supervisorExecutorService);
        return immediateAbortAllFuture.thenRun("fullAbortAll.after.immediateAbortAll",
                () -> {
                    clearCheckBoxes();
                    mainFuture = lastFutureReturnedFinal;
                    ignoreTitleErrors.set(false);
                    for (XFuture<Boolean> f : systemContinueMap.values()) {
                        f.cancelAll(stealingRobots);
                    }
                    systemContinueMap.clear();
                    for (XFutureVoid f : debugSystemContinueMap.values()) {
                        f.cancelAll(stealingRobots);
                    }
                    debugSystemContinueMap.clear();
                    completeScanTillNewInternalCount.set(0);
                    startScanTillNewInternalCount.set(0);
                    for (AprsSystem sys : aprsSystems) {
                        sys.clearKitsToCheck(sys.getSafeAbortRequestCount());
                    }
                }).alwaysComposeAsyncToVoid("allowTogglesFullAbort",
                        () -> allowTogglesNoCheck(blockerName),
                        supervisorExecutorService);
    }

    public XFutureVoid publicReturnRobot() {
        final XFuture<?> lastFutureReturnedFinal = lastFutureReturned;
        AprsSystem sysArray[] = systems().toArray(new AprsSystem[0]);
        final String blockerName = "publicReturnRobot";
        Supplier<XFuture<LockInfo>> sup = () -> disallowToggles(blockerName, sysArray);
        XFuture<XFuture<LockInfo>> disallowTogglesFutureFuture = XFuture.supplyAsync(blockerName, sup,
                supervisorExecutorService);
        XFuture<LockInfo> disallowTogglesFuture = disallowTogglesFutureFuture.thenCompose(x -> x);
        XFutureVoid immediateAbortAllFuture = disallowTogglesFuture
                .thenComposeAsyncToVoid(x -> publicReturnRobotStep2("publicReturnRobot", false), supervisorExecutorService);
        return immediateAbortAllFuture.thenRun("fullAbortAll.after.immediateAbortAll", () -> {
            clearCheckBoxes();
            mainFuture = lastFutureReturnedFinal;
            ignoreTitleErrors.set(false);
            for (XFuture<Boolean> f : systemContinueMap.values()) {
                f.cancelAll(stealingRobots);
            }
            systemContinueMap.clear();
            for (XFutureVoid f : debugSystemContinueMap.values()) {
                f.cancelAll(stealingRobots);
            }
            debugSystemContinueMap.clear();
            completeScanTillNewInternalCount.set(0);
            startScanTillNewInternalCount.set(0);
            for (AprsSystem sys : aprsSystems) {
                sys.clearKitsToCheck(sys.getSafeAbortRequestCount());
            }
        }).alwaysComposeAsyncToVoid(blockerName, () -> allowTogglesNoCheck(blockerName), supervisorExecutorService);
    }

    private void clearCheckBoxes() {
        if (null != displayJFrame) {
            displayJFrame.clearCheckBoxes();
        }
        setContinuousDemoSelected(false);
        setContinuousDemoRevFirstSelected(false);
        setIndContinuousDemoSelected(false);
        setIndRandomToggleTestSelected(false);
        setPauseSelected(false);
        setRandomTestSelected(false);

    }

    private static void printStatus(AtomicReference<@Nullable XFutureVoid> ref, PrintStream ps) {
        if (null != ref) {
            XFuture<?> xf = ref.get();
            printStatus(xf, ps);
        }
    }

    private static void printStatus(@Nullable XFuture<?> xf, PrintStream ps) {
        if (null != xf) {
            xf.printStatus(ps);
        }
    }

    private @Nullable
    NamedFunction<Integer, XFutureVoid> getReturnRobotNamedCallable() {
        return returnRobotFunction.get();
    }

    private final AtomicInteger debugActionCount = new AtomicInteger();

    public void debugAction() {
        logEvent("debugAction");
        int count = debugActionCount.incrementAndGet();
        println("Begin AprsSupervisorJFrame.debugAction()" + count);
        println("waitForTogglesFutures = " + waitForTogglesFutures);
        println("togglesAllowed = " + togglesAllowed);
        println("disallowTogglesCount = " + disallowTogglesCount);
        println("allowTogglesCount = " + allowTogglesCount);
        println("waitForTogglesFutureCount = " + waitForTogglesFutureCount);

        println("stealingRobots = " + stealingRobots);
        println("returnRobotRunnable = " + returnRobotFunction);

        println("lastFutureReturned = " + lastFutureReturned);
        printStatus(lastFutureReturned, System.out);
        println("ContinuousDemoFuture = " + ContinuousDemoFuture);
        printStatus(ContinuousDemoFuture, System.out);

        println("randomTest = " + randomTestFuture);
        printStatus(randomTestFuture, System.out);

        println("lastStealRobotsInternalBeforeAllowToggles = " + lastStealRobotsInternalBeforeAllowToggles);
        printStatus(lastStealRobotsInternalBeforeAllowToggles, System.out);

        println("lastStartReverseActionsFuture = " + lastStartReverseActionsFuture);
        printStatus(lastStartReverseActionsFuture, System.out);

        println("togglesAllowedXfuture = " + togglesAllowedXfuture);
        printStatus(togglesAllowedXfuture, System.out);

        println("stealRobotFuture = " + stealRobotFuture);
        printStatus(stealRobotFuture, System.out);

        println("unStealRobotFuture = " + unStealRobotFuture);
        printStatus(unStealRobotFuture, System.out);
        println("cancelStealRobotFuture = " + cancelStealRobotFuture);
        printStatus(cancelStealRobotFuture, System.out);

        println("cancelUnStealRobotFuture = " + cancelUnStealRobotFuture);
        printStatus(cancelUnStealRobotFuture, System.out);

        println("stealAbortFuture = " + stealAbortFuture);
        printStatus(stealAbortFuture, System.out);

        println("unstealAbortFuture = " + unstealAbortFuture);
        printStatus(unstealAbortFuture, System.out);

        println("lastCompletePrevMultiFuture = " + lastCompletePrevMultiFuture);
        printStatus(lastCompletePrevMultiFuture, System.out);

        println("lastCompleteMultiCycleTestFuture = " + lastCompleteMultiCycleTestFuture);
        printStatus(lastCompleteMultiCycleTestFuture, System.out);

        println("oldLfrs = " + oldLfrs);
        for (int i = 0; i < oldLfrs.size(); i++) {
            XFuture<?> xf = oldLfrs.get(i);
            if (!xf.isDone() || xf.isCancelled() || xf.isCompletedExceptionally()) {
                println("oldLfrs.get(" + i + ") = " + xf);
                printStatus(xf, System.out);
            }
        }

        XFuture<?> xfa[] = lastStartAllActionsArray;
        if (null != xfa) {
            println("lastStartAllActionsArray = " + Arrays.toString(xfa));
            for (int i = 0; i < xfa.length; i++) {
                XFuture<?> xf = xfa[i];
                if (!xf.isDone() || xf.isCancelled() || xf.isCompletedExceptionally()) {
                    println("oldLfrs.get(" + i + ") = " + xf);
                    printStatus(xf, System.out);
                }
            }
        }

        for (AprsSystem aprsSystem : aprsSystems) {
            aprsSystem.debugAction();
        }

        printReturnRobotTraceInfo();

        println("End AprsSupervisorJFrame.debugAction()" + count);
        println();
        logEventErr("");

    }

    private volatile boolean closing = false;

    public void setVisible(boolean visible) {
        if (null != displayJFrame) {
            runOnDispatchThread(() -> {
                if (null != displayJFrame) {
                    displayJFrame.setVisible(visible);
                }
            });
        }
    }

    public void close() {
        preCloseAllAprsSystems();
        if (null != logPrintStream) {
//            Set<Entry<String, Integer>> callerEntries = callerMap.entrySet();
//            List<Entry<String, Integer>> callerList = new ArrayList<>(callerEntries);
//            Collections.sort(callerList, Comparator.comparing(Entry::getValue));

            long t = System.currentTimeMillis();
            int blockersSize = getToggleBlockerMapSize();
            int ecc = enableChangeCount.get();
            int cdc = ContinuousDemoCycle.get();
            int errs = logEventErrCount.get();
//            int runDispatchCount = this.dispatchCount.get();
//            long totalRunDispatchTime = this.dispatchTime.get();
            String threadname = Thread.currentThread().getName();
            StackTraceElement trace[] = Thread.currentThread().getStackTrace();
//            for (Entry<String, Integer> entry : callerList) {
//                logEventPrivate(t, entry.getKey() + " \t= \t" + entry.getValue(), blockersSize, ecc, cdc, errs, threadname);
//            }
            long totalSupervisorRunTime = t - startSupervisorTime;
            String s = "closing ... totalSupervisorRunTime=" + totalSupervisorRunTime;
            logEventPrivate(t, s, blockersSize, ecc, cdc, errs, trace, threadname);
//            double totalRunDispatchTimePercent = 100.0 * ((double) totalRunDispatchTime / (double) totalSupervisorRunTime);
//            s = "closing ... runOnDispatchThread: runDispatchCount=" + runDispatchCount + ",totalRunDispatchTime=" + totalRunDispatchTime + ",totalRunDispatchTimePercent=" + totalRunDispatchTimePercent + ",maxStartPending=" + maxStartPending;
//            logEventPrivate(t, s, blockersSize, ecc, cdc, errs, trace, threadname);
            long totalConveyorMoveTime = conveyorMoveTime.get();
            double totalConveyorMoveTimePercent = 100.0
                    * ((double) totalConveyorMoveTime / (double) totalSupervisorRunTime);
            int totalConveyorMoveCount = conveyorMoveCount.incrementAndGet();
            s = "closing ...  totalConveyorMoveTime=" + totalConveyorMoveTime + ",totalConveyorMoveTimePercent="
                    + totalConveyorMoveTimePercent + ",totalConveyorMoveCount=" + totalConveyorMoveCount;
            logEventPrivate(t, s, blockersSize, ecc, cdc, errs, trace, threadname);
            logEvent("close called.").thenRun(() -> {
                if (null != logPrintStream) {
                    logPrintStream.close();
                    logPrintStream = null;
                }
                closing = true;
            });
        }
        closing = true;
        XFuture.setClosingMode(true);
        if (null != this.object2DOuterJPanel1) {
            this.object2DOuterJPanel1.disconnect();
            this.object2DOuterJPanel1.disconnectCurrentPosition();
        }

        try {
            finishEncodingLiveImageMovie();
        } catch (Exception ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", ex);
        }
        stopRunTimeTimer();
        stopColorTextReader();
        closeAllAprsSystems();
        if (null != aprsSystems) {
            for (AprsSystem sys : aprsSystems) {
                sys.forceClose();
            }
        }
        final XFuture<?> lastFutureReturnedFinal = lastFutureReturned;
        if (null != lastFutureReturnedFinal) {
            if (!lastFutureReturnedFinal.isDone()) {
                System.err.println("close: Cancelling lastFutureReturned=" + lastFutureReturnedFinal);
                lastFutureReturnedFinal.cancelAll(true);
            }
            lastFutureReturned = null;
        }
        if (null != fillTraysAndNextRepeatingFuture) {
            fillTraysAndNextRepeatingFuture.cancelAll(true);
            fillTraysAndNextRepeatingFuture = null;
        }
        if (null != conveyorTestFuture) {
            conveyorTestFuture.cancelAll(true);
            conveyorTestFuture = null;
        }

        for (XFutureVoid xf : waitForTogglesFutures) {
            xf.cancelAll(true);
        }
        if (null != ContinuousDemoFuture) {
            ContinuousDemoFuture.cancelAll(true);
        }
        if (null != togglesAllowedXfuture) {
            XFutureVoid xf = togglesAllowedXfuture.get();
            if (null != xf) {
                xf.cancelAll(true);
            }
        }
        if (null != stealAbortFuture) {
            if (!stealAbortFuture.isDone()) {
                stealAbortFuture.cancelAll(true);
            }
            stealAbortFuture = null;
        }
        if (null != unstealAbortFuture) {
            if (!unstealAbortFuture.isDone()) {
                unstealAbortFuture.cancelAll(true);
            }
            unstealAbortFuture = null;
        }
        if (null != stealRobotFuture) {
            XFutureVoid xf = stealRobotFuture.getAndSet(null);
            if (null != xf) {
                if (!xf.isDone()) {
                    xf.cancelAll(true);
                }
            }
        }
        if (null != unStealRobotFuture) {
            XFutureVoid xf = unStealRobotFuture.getAndSet(null);
            if (null != xf) {
                if (!xf.isDone()) {
                    xf.cancelAll(true);
                }
            }
        }
        if (null != cancelStealRobotFuture) {
            XFutureVoid xf = cancelStealRobotFuture.getAndSet(null);
            if (null != xf && !xf.isDone()) {
                xf.cancelAll(true);
            }
        }
        if (null != cancelUnStealRobotFuture) {
            XFutureVoid xf = cancelUnStealRobotFuture.getAndSet(null);
            if (null != xf && !xf.isDone()) {
                xf.cancelAll(true);
            }
        }
        if (null != mainFuture) {
            XFuture<?> xf = mainFuture;
            mainFuture = null;
            xf.cancelAll(true);
        }
        if (null != conveyorTestFuture) {
            XFutureVoid xfv = conveyorTestFuture;
            if (xfv == mainFuture) {
                mainFuture = null;
            }
            conveyorTestFuture = null;
            xfv.cancelAll(true);
        }
        if (null != randomTestFuture) {
            if (!randomTestFuture.isDone()) {
                randomTestFuture.cancelAll(true);
            }
            randomTestFuture = null;
        }

        this.setVisible(false);
        if (null != supervisorThread && null != supervisorExecutorService) {
            supervisorExecutorService.shutdownNow();
        }
        randomDelayExecutorService.shutdownNow();
        if (null != processLauncher) {
            processLauncher.close().thenRun(this::myExit);
        } else {
            myExit();
        }
    }

    private void myExit() {
        if (null != displayJFrame) {
            displayJFrame.closeAprsRemoteConsoleService();
            if (!CRCLUtils.isGraphicsEnvironmentHeadless()) {
                runOnDispatchThread(() -> {
                    if (null != displayJFrame && !CRCLUtils.isGraphicsEnvironmentHeadless()) {
                        displayJFrame.setVisible(false);

                        displayJFrame.removeAll();
                        displayJFrame.dispose();
                        CRCLUtils.systemExit(0);
                    }
                });
            }
        }
    }

    private volatile @Nullable XFutureVoid ContinuousDemoFuture = null;
    private volatile @Nullable XFuture<?> mainFuture = null;

    public void setContinuousDemoFuture(XFutureVoid ContinuousDemoFuture) {
        this.ContinuousDemoFuture = ContinuousDemoFuture;
    }

    public @Nullable
    XFutureVoid getContinuousDemoFuture() {
        return ContinuousDemoFuture;
    }

    @Nullable
    XFuture<?> getMainFuture() {
        return mainFuture;
    }

    void setMainFuture(XFuture<?> mainFuture) {
        this.mainFuture = mainFuture;
    }

    private final AtomicReference<@Nullable XFutureVoid> resumeFuture = new AtomicReference<>(null);

    private volatile @Nullable XFutureVoid randomTestFuture = null;

    @Nullable
    XFutureVoid getRandomTestFuture() {
        return randomTestFuture;
    }

    void setRandomTestFuture(@Nullable XFutureVoid randomTestFuture) {
        this.randomTestFuture = randomTestFuture;
    }

    void clearEventLog() {
        abortEventTime = -1;
        firstEventTime = -1;
        if (null != displayJFrame) {
            displayJFrame.clearEventLog();
        }
    }

    /**
     * Reset all systems, clearing errors, resetting states to defaults and
     * optionally reloading simulation files. This may occur in another thread.
     *
     * @param reloadSimFiles whether to reload simulation files
     * @return a future which can be used to determine when the resetAll action
     * is complete.
     */
    XFutureVoid resetAll(boolean reloadSimFiles) {
        logEventErrCount.set(0);
        final XFuture<?> lastFutureReturnedFinal = lastFutureReturned;
        if (null != lastFutureReturnedFinal) {
            if (!lastFutureReturnedFinal.isDone() && !closing) {
                System.err.println("resetAll: Cancelling lastFutureReturned=" + lastFutureReturnedFinal);
                lastFutureReturnedFinal.cancelAll(true);
            }
            clearLFR();
        }
        if (null != randomTestFuture) {
            randomTestFuture.cancelAll(true);
            randomTestFuture = null;
        }
        if (null != ContinuousDemoFuture) {
            ContinuousDemoFuture.cancelAll(true);
            ContinuousDemoFuture = null;
        }
        if (null != fillTraysAndNextRepeatingFuture) {
            fillTraysAndNextRepeatingFuture.cancelAll(true);
            fillTraysAndNextRepeatingFuture = null;
        }
        if (null != conveyorTestFuture) {
            conveyorTestFuture.cancelAll(true);
            conveyorTestFuture = null;
        }

        immediateAbortAll("resetAll");
        clearCheckBoxes();
        @SuppressWarnings("unchecked")
        XFutureVoid allResetFutures[] = new XFutureVoid[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystem sys = aprsSystems.get(i);
            allResetFutures[i] = sys.reset(reloadSimFiles);
            sys.setCorrectionMode(correctionMode);
        }
        abortEventTime = -1;
        firstEventTime = -1;
        return XFutureVoid.allOf(allResetFutures)
                .peekNoCancelException(this::handleXFutureException)
                .thenComposeAsyncToVoid(() -> this.waitSingleStep("resetAll"), supervisorExecutorService);
    }

    private volatile boolean correctionMode = true;

    public boolean isCorrectionMode() {
        return correctionMode;
    }

    public void setCorrectionMode(boolean correctionMode) {
        this.correctionMode = correctionMode;
        if (null != displayJFrame) {
            displayJFrame.setCheckBoxMenuItemUseCorrectionModeByDefaultSelected(correctionMode);
        }
    }

    private int resetMainPauseCount = 0;

    void resetMainPauseTestFuture() {
        resetMainPauseCount++;
        if (null == ContinuousDemoFuture) {
            if (null == randomTestFuture) {
                if (null == pauseTestFuture) {
                    mainFuture = (XFuture<?>) XFuture
                            .completedFutureWithName("resetMainPauseTestFuture" + resetMainPauseCount, null);
                    return;
                }
                mainFuture = (XFuture<?>) XFuture.allOfWithName("resetMainPauseTestFuture" + resetMainPauseCount,
                        pauseTestFuture);
                return;
            }
            if (null == pauseTestFuture) {
                mainFuture = (XFuture<?>) XFuture.allOfWithName("resetMainPauseTestFuture" + resetMainPauseCount,
                        randomTestFuture);
                return;
            }
            mainFuture = (XFuture<?>) XFuture.allOfWithName("resetMainPauseTestFuture" + resetMainPauseCount,
                    randomTestFuture, pauseTestFuture);
            return;
        }
        if (null == randomTestFuture) {
            if (null == pauseTestFuture) {
                mainFuture = (XFuture<?>) XFuture.allOfWithName("resetMainPauseTestFuture" + resetMainPauseCount,
                        ContinuousDemoFuture);
                return;
            }
            mainFuture = (XFuture<?>) XFuture.allOfWithName("resetMainPauseTestFuture" + resetMainPauseCount,
                    ContinuousDemoFuture, pauseTestFuture);
            return;
        }
        if (null == pauseTestFuture) {
            mainFuture = (XFuture<?>) XFuture.allOfWithName("resetMainPauseTestFuture" + resetMainPauseCount,
                    ContinuousDemoFuture, randomTestFuture);
            return;
        }
        mainFuture = (XFuture<?>) XFuture.allOfWithName("resetMainPauseTestFuture" + resetMainPauseCount,
                ContinuousDemoFuture, randomTestFuture, pauseTestFuture);
    }

    public XFutureVoid startContinuousDemoRevFirst() {
        setContinuousDemoSelected(false);
        return prepActions().thenComposeAsyncToVoid(this::startContinuousDemoRevFirstFinish, supervisorExecutorService);
    }

    private XFutureVoid startContinuousDemoRevFirstFinish() {
        immediateAbortAll("startContinuousDemoRevFirst");
        clearEventLog();
        clearAllErrors();
        connectAll();
        return enableAllRobots().thenComposeAsyncToVoid(this::completeStartContinousDemoRevFirstFinish,
                supervisorExecutorService);
    }

    private XFutureVoid completeStartContinousDemoRevFirstFinish() {
        ContinuousDemoCycle.set(0);
        if (!isContinuousDemoRevFirstSelected()) {
            setContinuousDemoRevFirstSelected(true);
        }
        XFutureVoid ret = startPrivateContinuousDemoRevFirst();
        ContinuousDemoFuture = ret;
        mainFuture = ContinuousDemoFuture;
        return ret;
    }

    public XFutureVoid startRandomTestFirstActionReversed() {
        try {
            setContinuousDemoRevFirstSelected(true);
            setRandomTestSelected(true);
            return prepAndFinishToXFutureVoidOnDispatch(() -> {
                try {
                    immediateAbortAll("jMenuItemRandomTestReverseFirstActionPerformed");
                    XFutureVoid outerRet = resetAll(false)
                            .thenComposeAsyncToVoid(x -> {
                                try {
                                    clearAllErrors();
                                    connectAll();
                                    return startRandomTestFirstActionReversed2();
                                } catch (Exception e) {
                                    Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", e);
                                    MultiLineStringJPanel.showText("Exception occurred: " + e);
                                    XFutureVoid ret = new XFutureVoid(
                                            "internal startRandomTestFirstActionReversed with exception " + e);
                                    ret.completeExceptionally(e);
                                    return ret;
                                }
                            }, supervisorExecutorService);
                    return outerRet
                            .peekNoCancelException(this::handleXFutureException)
                            .thenComposeAsyncToVoid(() -> this.waitSingleStep("startRandomTestFirstActionReversed"), supervisorExecutorService);
                } catch (Exception e) {
                    Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", e);
                    MultiLineStringJPanel.showText("Exception occurred: " + e);
                    XFutureVoid ret = new XFutureVoid(
                            "internal startRandomTestFirstActionReversed with exception " + e);
                    ret.completeExceptionally(e);
                    return ret;
                }
            });
        } catch (Exception e) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", e);
            MultiLineStringJPanel.showText("Exception occurred: " + e);
            XFutureVoid ret = new XFutureVoid("startRandomTestFirstActionReversed with exception " + e);
            ret.completeExceptionally(e);
            return ret;
        }
    }

    private XFutureVoid startRandomTestFirstActionReversed2() {
        return enableAllRobots()
                .thenComposeAsyncToVoid(this::startRandomTestFirstActionReversed3, supervisorExecutorService);
    }

    private XFutureVoid startRandomTestFirstActionReversed3() {
        ContinuousDemoCycle.set(0);
        clearRandomTestCount();
        setContinuousDemoRevFirstSelected(true);
        setRandomTestSelected(true);
        clearLFR();
        XFutureVoid ret = startRandomTest();
        mainFuture = ret;
        return ret;
    }

    private int resetMainRandomTestCount = 0;

    private void resetMainRandomTestFuture() {
        final XFutureVoid ContinuousDemoFutureFinal = ContinuousDemoFuture;
        final XFutureVoid randomTestFutureFinal = randomTestFuture;
        if (null != randomTestFutureFinal && null != ContinuousDemoFutureFinal) {
            resetMainRandomTestCount++;
            logEvent("resetMainRandomTestCount=" + resetMainRandomTestCount + ",randomTestFuture="
                    + randomTestFutureFinal.getName() + ",ContinuousDemoFuture=" + ContinuousDemoFutureFinal.getName());
            mainFuture = XFuture.allOfWithName("resetMainRandomTestFuture" + resetMainRandomTestCount,
                    randomTestFutureFinal, ContinuousDemoFutureFinal);
            mainFuture.exceptionally((thrown) -> {
                if (thrown != null) {
                    log(Level.SEVERE, "", thrown);
                }
                if (thrown instanceof RuntimeException) {
                    throw (RuntimeException) thrown;
                }
                throw new RuntimeException(thrown);
            });
        } else {
            logEvent("SKIP resetMainRandomTestCount=" + resetMainRandomTestCount + ",randomTestFuture="
                    + randomTestFutureFinal + ",ContinuousDemoFuture=" + ContinuousDemoFutureFinal);
        }
    }

    private int randomTestSeed = 959;

    int getRandomTestSeed() {
        return randomTestSeed;
    }

    void setRandomTestSeed(int randomTestSeed) {
        this.randomTestSeed = randomTestSeed;
    }

    private static String getDirNameOrHome(@Nullable File f) throws IOException {
        if (f != null) {
            File parentFile = f.getParentFile();
            if (null != parentFile) {
                return parentFile.getCanonicalPath();
            }
        }
        return Utils.getAprsUserHomeDir();
    }

    /**
     * Get the first system with a task name that starts with the given string.
     *
     * @param s name/prefix of task to look for
     * @return system with given task
     */
    public @Nullable
    AprsSystem getSysByTask(String s) {
        for (AprsSystem sys : aprsSystems) {
            if (sys.getTaskName().startsWith(s)) {
                return sys;
            }
        }
        return null;
    }

    public String[] getTaskArray() {
        String tasks[] = new String[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystem sys = aprsSystems.get(i);
            tasks[i] = sys.getTaskName();
        }
        return tasks;
    }

    private final String INIT_CUSTOM_CODE = "package custom;\n" + "import aprs.*; \n"
            + "import java.util.function.Consumer;\n\n"
            + "public class Custom\n\timplements Consumer<AprsSupervisorJFrame> {\n"
            + "\tpublic void accept(AprsSupervisorJFrame sup) {\n" + "\t\t// PUT YOUR CODE HERE:\n"
            + "\t\tprintln(\"sys = \"+sup.getSysByTask(\"Fanuc Cart\"));" + "\t}\n" + "}\n";

    private String customCode = INIT_CUSTOM_CODE;
    private volatile int max_cycles = -1;

    public void setShowFullScreenMessages(boolean showFullScreenMessages) {
        if (null != displayJFrame) {
            displayJFrame.setShowFullScreenMessages(showFullScreenMessages);
        }
    }

    // @SuppressWarnings("nullness")
//	private void runCustomCode() {
//		try {
//
//			customCode = MultiLineStringJPanel.editText(customCode);
//			File customDir = Paths.get(Utils.getAprsUserHomeDir(), ".aprs", "custom").toFile();
//			customDir.delete();
//			customDir.mkdirs();
//			File tmpFile = Utils.file(customDir, "Custom.java");
//			println("tmpFile = " + tmpFile.getCanonicalPath());
//			File[] files1 = { tmpFile };
//
//			Files.write(tmpFile.toPath(), customCode.getBytes());
//			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
//			if (null != compiler) {
//				ClassLoader cl = ClassLoader.getSystemClassLoader();
//
//				URL[] origUrls = ((URLClassLoader) cl).getURLs();
//
//				StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
//
//				Iterable<? extends JavaFileObject> compilationUnits1 = fileManager
//						.getJavaFileObjectsFromFiles(Arrays.asList(files1));
//				String classPath = Arrays.stream(origUrls).map(Objects::toString)
//						.map(s -> s.startsWith("file:") ? s.substring(4) : s)
//						.collect(Collectors.joining(File.pathSeparator));
//				println("classPath = " + classPath);
//				DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
//				compiler.getTask(null, fileManager, diagnostics, Arrays.asList("-cp", classPath), null,
//						compilationUnits1).call();
//				StringBuilder errBuilder = new StringBuilder();
//				for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
//					String err = String.format("%s:%d %s %n", diagnostic.getSource().toUri(),
//							diagnostic.getLineNumber(), diagnostic.getMessage(Locale.US));
//					errBuilder.append(err);
//				}
//				String fullErr = errBuilder.toString();
//				boolean origDisableShowText = crcl.ui.misc.MultiLineStringJPanel.disableShowText;
//				if (fullErr.length() > 0) {
//					crcl.ui.misc.MultiLineStringJPanel.disableShowText = false;
//					MultiLineStringJPanel.showText(fullErr)
//							.thenRun(() -> crcl.ui.misc.MultiLineStringJPanel.disableShowText = origDisableShowText);
//					if (!customCode.contains("class Custom")) {
//						customCode = INIT_CUSTOM_CODE;
//					}
//					return;
//				}
//				URL[] urls = new URL[origUrls.length + 1];
//				System.arraycopy(origUrls, 0, urls, 0, origUrls.length);
//				File parentFile = tmpFile.getAbsoluteFile().getParentFile();
//				if (null == parentFile) {
//					logEventErr("Temporary file " + tmpFile + " does not have parent.");
//					return;
//				}
//				File grandParentFile = parentFile.getParentFile();
//				if (null == grandParentFile) {
//					logEventErr("Temporary file " + tmpFile + " does not have grandparent.");
//					return;
//				}
//				urls[urls.length - 1] = grandParentFile.toURI().toURL();
//				// tmpFile.getAbsoluteFile().getParentFile().getParentFile().toURI().toURL()};
//				println("urls = " + Arrays.toString(urls));
//				Class<?> clss;
//				try (URLClassLoader loader = new URLClassLoader(urls)) {
//					clss = loader.loadClass("custom.Custom");
//				}
//				@SuppressWarnings("deprecation")
//				Object obj = clss.newInstance();
//				Method acceptMethod = clss.getMethod("accept", Supervisor.class);
//				ByteArrayOutputStream baos = new ByteArrayOutputStream();
//				PrintStream origOut = System.out;
//
//				try (PrintStream ps = new PrintStream(baos)) {
//					System.setOut(ps);
//					acceptMethod.invoke(obj, this);
//					String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
//					System.setOut(origOut);
//					println("content = " + content);
//					if (content.length() > 0) {
//						crcl.ui.misc.MultiLineStringJPanel.disableShowText = false;
//						MultiLineStringJPanel.showText(content).thenRun(
//								() -> crcl.ui.misc.MultiLineStringJPanel.disableShowText = origDisableShowText);
//
//					}
//				} finally {
//					crcl.ui.misc.MultiLineStringJPanel.disableShowText = origDisableShowText;
//					System.setOut(origOut);
//				}
//			}
//		} catch (Exception exception) {
//			Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", exception);
//			StringWriter sw = new StringWriter();
//			exception.printStackTrace(new PrintWriter(sw));
//			String trace = sw.toString();
//			boolean origDisableShowText = crcl.ui.misc.MultiLineStringJPanel.disableShowText;
//			crcl.ui.misc.MultiLineStringJPanel.disableShowText = false;
//			MultiLineStringJPanel.showText(trace)
//					.thenRun(() -> crcl.ui.misc.MultiLineStringJPanel.disableShowText = origDisableShowText);
//			if (!customCode.contains("class Custom")) {
//				customCode = INIT_CUSTOM_CODE;
//			}
//		}
//	}
    // private XFutureVoid setTeachSystemFilter(@Nullable AprsSystem sys) {
//        if (null == sys) {
//            object2DOuterJPanel1.setForceOutputFlag(false);
//            object2DOuterJPanel1.setShowOutputItems(false);
//            return object2DOuterJPanel1.setOutputItems(object2DOuterJPanel1.getItems());
//        } else {
//            object2DOuterJPanel1.setForceOutputFlag(true);
//            object2DOuterJPanel1.setSimulated(true);
//            object2DOuterJPanel1.setShowOutputItems(true);
//            return object2DOuterJPanel1.setOutputItems(filterForSystem(sys, object2DOuterJPanel1.getItems()));
//        }
//    }
    private static @Nullable
    PhysicalItem closestPart(double sx, double sy, List<PhysicalItem> items) {
        return items.stream().filter(x -> x.getType().equals("P"))
                .min(Comparator.comparing(pitem -> Math.hypot(sx - pitem.x, sy - pitem.y))).orElse(null);
    }

    private Stream<Slot> absSlotStreamFromTrayAndOffset(PhysicalItem tray, Slot offsetItem) {
        Slot slot = slotOffsetProvider.absSlotFromTrayAndOffset(tray, offsetItem, 0);
        if (null != slot) {
            return Stream.of(slot);
        }
        return Stream.empty();
    }

    private double getClosestSlotDist(Collection<Slot> slots, PhysicalItem item) {
        return slots.stream().mapToDouble(item::dist).min().orElse(Double.POSITIVE_INFINITY);
    }

    List<PhysicalItem> filterForSystem(AprsSystem sys, List<PhysicalItem> listIn) {

        Set<PhysicalItem> allTrays = listIn.stream().filter(x -> "KT".equals(x.getType()) || "PT".equals(x.getType()))
                .collect(Collectors.toSet());
        Set<PhysicalItem> kitTrays = listIn.stream().filter(x -> "KT".equals(x.getType())).collect(Collectors.toSet());
        Set<PhysicalItem> sysKitTrays = kitTrays.stream().filter(tray -> {
            List<Slot> l2 = sys.getSlotOffsets(tray.getName(), true);
            return l2 != null && !l2.isEmpty();
        }).collect(Collectors.toSet());
        Set<PhysicalItem> otherSysTrays = new HashSet<>(allTrays);
        otherSysTrays.removeAll(sysKitTrays);
        List<PhysicalItem> listOut = new ArrayList<>(sysKitTrays);
        List<PhysicalItem> allParts = new ArrayList<>();
        List<Slot> sysKitTraySlots = sysKitTrays.stream()
                .flatMap(kit -> slotOffsetProvider.getSlotOffsets(kit.getName(), false).stream()
                .flatMap(slotOffset -> absSlotStreamFromTrayAndOffset(kit, slotOffset)))
                .collect(Collectors.toList());
        List<Slot> otherKitTraySlots = otherSysTrays.stream()
                .flatMap(kit -> slotOffsetProvider.getSlotOffsets(kit.getName(), false).stream()
                .flatMap(slotOffset -> absSlotStreamFromTrayAndOffset(kit, slotOffset)))
                .collect(Collectors.toList());
        for (PhysicalItem item : listIn) {
            if ("P".equals(item.getType())) {
                allParts.add(item);
                double sysKitDist = getClosestSlotDist(sysKitTraySlots, item);
                double otherKitDist = getClosestSlotDist(otherKitTraySlots, item);
                if (sysKitDist < otherKitDist) {
                    listOut.add(item);
                }
            }
        }
        return listOut;
    }

    public XFutureVoid lookForPartsAll() {
        if (aprsSystems.isEmpty()) {
            throw new IllegalStateException("aprsSystems.isEmpty()");
        }
        XFuture<?> futures[] = new XFuture<?>[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystem aprsSys = aprsSystems.get(i);
            if (keepDisabled && !aprsSys.isConnected()) {
                futures[i] = XFuture.completedFuture(skipDisabled);
            } else {
                futures[i] = aprsSys.startLookForParts();
            }
        }
        return XFuture.allOfWithName("lookForPartsAll", futures).peekException(this::handleXFutureException);
    }

    private void completeScanAllInternal() {
        if (aprsSystems.isEmpty()) {
            throw new IllegalStateException("aprsSystems.isEmpty()");
        }
        List<PhysicalItem> teachItems = Collections.emptyList();
        if (isUseTeachCameraSelected()) {
            if (object2DOuterJPanel1.isSimulated()) {
                teachItems = object2DOuterJPanel1.getOutputItems();
            } else {
                teachItems = object2DOuterJPanel1.getItems();
            }
        }
        for (AprsSystem aprsSys : aprsSystems) {
            File actionListFile = completeScanOneInternal(aprsSys, teachItems);
        }
    }

    @Nullable
    File completeScanOneInternal(AprsSystem aprsSys, List<PhysicalItem> teachItems) throws RuntimeException {
        logEvent("completeScanOneInternal aprsSys=" + aprsSys + ",teachItems=" + teachItems);
        aprsSys.setCorrectionMode(correctionMode);
        File actionListFile;
        final File tempImageFile;
        final boolean useTeachTableLocal = isUseTeachCameraSelected() && aprsSys.getUseTeachTable();
        if (useTeachTableLocal) {
            final List<PhysicalItem> filteredTeachItems = filterForSystem(aprsSys, teachItems);
            if (filteredTeachItems.isEmpty()) {
                throw new RuntimeException("no teach items for " + aprsSys.getTaskName());
            }
            try {
                File supervisorDir = Utils.file(Utils.getlogFileDir(), "supervisor");
                supervisorDir.mkdirs();
                File subDir = Utils.file(supervisorDir, aprsSys.getTaskName());
                subDir.mkdirs();
                tempImageFile = Utils.createTempFile("filteredTeachItems_completeScanOneInternal_" + aprsSys.toString(),
                        ".PNG",
                        subDir);
                aprsSys.saveScanStyleImage(tempImageFile,
                        filteredTeachItems);
            } catch (IOException ex) {
                Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }
            actionListFile = aprsSys.createActionListFromVision(aprsSys.getObjectViewItems(), filteredTeachItems, true,
                    0, false, false, true, false);
        } else {
            try {
                File supervisorDir = Utils.file(Utils.getlogFileDir(), "supervisor");
                supervisorDir.mkdirs();
                File subDir = Utils.file(supervisorDir, aprsSys.getTaskName());
                subDir.mkdirs();
                tempImageFile = Utils.createTempFile("completeScanOneInternal_" + aprsSys.toString(), ".PNG", subDir);
                aprsSys.saveScanStyleImage(
                        tempImageFile,
                        aprsSys.getLastVisItemsData());
            } catch (IOException ex) {
                Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }
            actionListFile = aprsSys.createActionListFromVision();
        }
        if (null != actionListFile) {
            try {
                List<Action> loadedActions = aprsSys.loadActionsFileEx(actionListFile, // File f,
                        false, // boolean showInOptaPlanner,
                        false, // newReverseFlag
                        true, // boolean forceNameChange
                        null // options
                );
            } catch (IOException ex) {
                Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }
        }
//        else {
//            System.out.println("useTeachTableLocal = " + useTeachTableLocal);
//            try {
//                System.out.println("tempImageFile = " + tempImageFile.getCanonicalPath());
//            } catch (IOException ex) {
//                Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
//            }
//            System.out.println("teachItems = " + teachItems);
//            throw new RuntimeException("actionListFile==null: aprsSys.getTaskName()="+aprsSys.getTaskName());
//        }
        return actionListFile;
    }

    private volatile @Nullable TeachScanMonitor lastCompleteScanTillNewInternalTeachScanMonitor = null;

    private XFutureVoid completeScanTillNewInternal() {
        TeachScanMonitor oldMonitor = lastCompleteScanTillNewInternalTeachScanMonitor;
        if (null != oldMonitor) {
            XFutureVoid oldMonitorFuture = oldMonitor.getFuture();
            if (!oldMonitorFuture.isDone()) {
                logEvent("Stoping old TeachScanMonitor oldMonitorFuture=" + oldMonitorFuture);
            }
            oldMonitor.stop();
            lastCompleteScanTillNewInternalTeachScanMonitor = null;
        }
        TeachScanMonitor monitor = new TeachScanMonitor(aprsSystems, abortCount, isContinuousDemoSelected(),
                isUseTeachCameraSelected(), object2DOuterJPanel1, () -> closing, this::logEvent,
                this.supervisorExecutorService, this);
        lastCompleteScanTillNewInternalTeachScanMonitor = monitor;
        return monitor.getFuture();
    }

    private final AtomicInteger newTeachCount = new AtomicInteger();

    public int incrementAndGetNewTeachCount() {
        return newTeachCount.incrementAndGet();
    }

    private XFutureVoid scanAllInternal() {
        if (aprsSystems.isEmpty()) {
            throw new IllegalStateException("aprsSystems.isEmpty()");
        }
        return lookForPartsAll()
                .thenRunAsync("completeScanAllInternal", this::completeScanAllInternal, supervisorExecutorService)
                .thenComposeAsyncToVoid(this::showScanCompleteDisplay, supervisorExecutorService);
    }

    private final AtomicInteger startScanTillNewInternalCount = new AtomicInteger();
    private final AtomicInteger completeScanTillNewInternalCount = new AtomicInteger();

    private XFutureVoid scanTillNewInternal() {
        int sc = startScanTillNewInternalCount.incrementAndGet();
        int cc = completeScanTillNewInternalCount.get();
        if (cc < sc - 1) {
            throw new IllegalStateException();
        }
        return lookForPartsAll()
                .thenComposeAsyncToVoid("completeScanTillNewInternal" + sc + ":" + cc, this::completeScanTillNewInternal, supervisorExecutorService)
                .thenComposeAsyncToVoid("showScanCompleteDisplay", this::showScanCompleteDisplay, supervisorExecutorService)
                .thenRun(() -> {
                    int new_cc = completeScanTillNewInternalCount.incrementAndGet();
                    try {
                        takeAllSnapshots("scan : sc=" + sc + ",cc=" + new_cc);
                    } catch (IOException ex) {
                        Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
    }

    private XFutureVoid showScanCompleteDisplay() {
        logEvent("Scans Complete");
        setAbortTimeCurrent();
        if (null != displayJFrame) {
            return displayJFrame.showScanCompleteDisplay();
        } else {
            return XFutureVoid.completedFutureWithName("showScanCompleteDisplay");
        }
    }

    private XFutureVoid showAllTasksCompleteDisplay() {
        logEvent("All Tasks Complete");
        setAbortTimeCurrent();
        if (null != displayJFrame) {
            return displayJFrame.showAllTasksCompleteDisplay();
        } else {
            return XFutureVoid.completedFutureWithName("showAllTasksCompleteDisplay");
        }
    }

    private volatile XFuture<?> lastStartScanAllFutures@Nullable []  = null;

    public XFutureVoid startScanAllThenContinuousDemoRevFirstOnSupervisorService() {
        return XFuture
                .supplyAsync("startScanAllThenContinuousDemoRevFirstOnSupervisorService",
                        this::startScanAllThenContinuousDemoRevFirst, supervisorExecutorService)
                .thenComposeAsyncToVoid(x -> x, supervisorExecutorService);
    }

    public XFuture<Boolean> startFlipFMOnSupervisorService() {
        return XFuture
                .supplyAsync("startFlipOnSupervisorService",
                        this::startFlipFM, supervisorExecutorService)
                .thenCompose(x -> x);
    }

    public XFuture<Boolean> startFlipMFOnSupervisorService() {
        return XFuture
                .supplyAsync("startFlipOnSupervisorService",
                        this::startFlipMF, supervisorExecutorService)
                .thenCompose(x -> x);
    }

    private XFutureVoid startScanAllThenContinuousDemoRevFirst() {
        logEvent("startScanAllThenContinuousDemoRevFirst starting ...");
        XFutureVoid xf1 = this.safeAbortAll();
        XFutureVoid xf2 = xf1.thenComposeAsyncToVoid("startScanAllThenContinuousDemoRevFirst.step2",
                x -> {
                    logEvent("startScanAllThenContinuousDemoRevFirst.step2 : xf1=" + xf1);
                    return startScanAll();
                }, supervisorExecutorService);
        XFutureVoid xf3 = xf2.thenComposeAsyncToVoid("startScanAllThenContinuousDemoRevFirst.step3",
                x -> {
                    logEvent("startScanAllThenContinuousDemoRevFirst.step2 : xf2=" + xf2);
                    return startContinuousDemoRevFirst();
                }, supervisorExecutorService);
        return xf3;
    }

    public XFutureVoid lookForPartsAll(final boolean keepDisabled) {
        List<AprsSystem> aprsSystems = systems();
        XFuture<?> futures[] = new XFuture<?>[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystem aprsSys = aprsSystems.get(i);
            if (!aprsSys.isConnected() && keepDisabled) {
                futures[i] = XFuture.completedFuture(false);
            } else {
                futures[i] = aprsSys.startLookForParts();
            }
        }
        return XFuture.allOfWithName("lookForPartsAll", futures);
    }

    public XFuture<Boolean> startFlipFM() {
        logEvent("startFlip starting ...");
        XFutureVoid xf1 = this.safeAbortAll();
        AprsSystem fanucCartSys = getSysByTask("Fanuc Cart");
        logEvent("fanucCartSys = " + fanucCartSys);
        AprsSystem sharedTableSys = getSysByTask("Shared Table");
        logEvent("sharedTableSys = " + sharedTableSys);

        if (fanucCartSys.isAlertLimitsCheckBoxSelected() || sharedTableSys.isAlertLimitsCheckBoxSelected()
                || fanucCartSys.isEnforceMinMaxLimits() || sharedTableSys.isEnforceMinMaxLimits()) {
            final int confirm;
            if (!CRCLUtils.isGraphicsEnvironmentHeadless()) {
                confirm = JOptionPane.showConfirmDialog(this.displayJFrame,
                        "Disable Alert Limits on " + fanucCartSys + " and " + sharedTableSys);
            } else {
                confirm = JOptionPane.YES_OPTION;
            }
            if (confirm == JOptionPane.YES_OPTION) {
                sharedTableSys.setAlertLimitsCheckBoxSelected(false);
                fanucCartSys.setAlertLimitsCheckBoxSelected(false);
                sharedTableSys.setEnforceMinMaxLimits(false);
                fanucCartSys.setEnforceMinMaxLimits(false);
            } else {
                return XFuture.completedFuture(false);
            }
        }
        XFutureVoid xf2 = xf1.thenComposeAsyncToVoid("startFlip.step2",
                x -> {
                    logEvent("startFlip.step2 : xf1=" + xf1);
                    return lookForPartsAll(false);
                }, supervisorExecutorService);
        XFuture<Boolean> xf3 = xf2.thenCompose("startFlip.step3", x -> {
            logEvent("startFlip.step2 : xf2=" + xf2);
            return fanucCartSys.startActionsList("flip2",
                    Arrays.asList(new Action[]{
                Action.newTakePartAction("part_black_gear_in_pt_1"),
                Action.newMoveRecordedJoints("present_gear")
            }),
                    //		    ExecutorOption.ForBoolean.REVERSE.with(true),
                    ExecutorOption.ForBoolean.skipMissingParts.with(false),
                    ExecutorOption.ForBoolean.skipCheckHeldPart.with(true),
                    ExecutorOption.ForBoolean.reverseCheckDisabled.with(true));
        });
        XFuture<Boolean> xf4 = xf3.thenCompose("startFlip.step3", x -> {
            logEvent("startFlip.step3 : xf3=" + xf3);
            if (sharedTableSys.isObjectViewSimulated()) {
                final Object2DOuterJPanel objectViewPanel = sharedTableSys.getObjectViewPanel();
                if (null != objectViewPanel) {
                    objectViewPanel.setIgnoreMissedDropOffs(true);
                    objectViewPanel.setIgnoreMissedPickups(true);
                }
            }
            return sharedTableSys.startActionsList("flip3",
                    Arrays.asList(new Action[]{
                Action.newMoveRecordedPose("p1"),
                Action.newMoveRecordedPose("p2"),
                Action.newCloseGripper()
            }));
        });
        XFuture<Boolean> xf5 = xf4.thenCompose("startFlip.step4", x -> {
            logEvent("startFlip.step4 : xf4=" + xf4);
            return fanucCartSys.startActionsList("flip4",
                    Arrays.asList(new Action[]{
                Action.newOpenGripper()
            }));
        });
        XFuture<Boolean> xf6 = xf5.thenCompose("startFlip.step5", x -> {
            logEvent("startFlip.step5 : xf5=" + xf5);
            return sharedTableSys.startActionsList("flip5",
                    Arrays.asList(new Action[]{
                Action.newMoveRecordedPose("p3"),
                Action.newMoveRecordedPose("p4"),
                Action.newMoveRecordedPose("p5")
            }));
        });
        XFuture<Boolean> xf7 = xf6.thenCompose("startFlip.step6", x -> {
            logEvent("startFlip.step6 : xf6=" + xf6);
            return fanucCartSys.startActionsList("flip6",
                    Arrays.asList(new Action[]{
                Action.newCloseGripper()
            }));
        });
        XFuture<Boolean> xf8 = xf7.thenCompose("startFlip.step7", x -> {
            logEvent("startFlip.step7 : xf7=" + xf7);
            return sharedTableSys.startActionsList("flip7",
                    Arrays.asList(new Action[]{
                Action.newOpenGripper(),
                Action.newMoveRecordedPose("p6"),
                Action.newLookForParts()
            }));
        });

        XFuture<Boolean> xf9 = xf8.thenCompose("startFlip.step8", x -> {
            logEvent("startFlip.step8 : xf8=" + xf8);
            if (sharedTableSys.isObjectViewSimulated()) {
                final Object2DOuterJPanel objectViewPanel = sharedTableSys.getObjectViewPanel();
                if (null != objectViewPanel) {
                    objectViewPanel.setIgnoreMissedDropOffs(false);
                    objectViewPanel.setIgnoreMissedPickups(false);
                }
            }
            if (fanucCartSys.isObjectViewSimulated()) {
                final Object2DOuterJPanel objectViewPanel = fanucCartSys.getObjectViewPanel();
                if (null != objectViewPanel) {
                    objectViewPanel.setIgnoreMissedDropOffs(true);
                    objectViewPanel.setIgnoreMissedPickups(true);
                }
            }
            return fanucCartSys.startActionsList("flip8",
                    Arrays.asList(new Action[]{
                Action.newDisableOptimization(),
                Action.newMoveRecordedJoints("returning"),
                Action.newLookForParts(),
                Action.newPlacePartAction("empty_slot_for_large_gear_in_large_gear_vessel_1", "black_gear"),
                Action.newLookForParts()
            }));
        });
        return xf9.thenApply(x -> {
            if (fanucCartSys.isObjectViewSimulated()) {
                final Object2DOuterJPanel objectViewPanel = fanucCartSys.getObjectViewPanel();
                if (null != objectViewPanel) {
                    objectViewPanel.setIgnoreMissedDropOffs(false);
                    objectViewPanel.setIgnoreMissedPickups(false);
                }
            }
            logEvent("startFlipFM completed.");
            if (!CRCLUtils.isGraphicsEnvironmentHeadless()) {
                showMesssage("startFlipFM completed.");
            }
            return x;
        });
    }

    public XFuture<Boolean> startFlipMF() {
        logEvent("startFlip starting ...");
        XFutureVoid xf1 = this.safeAbortAll();
        AprsSystem fanucCartSys = getSysByTask("Fanuc Cart");
        logEvent("fanucCartSys = " + fanucCartSys);
        AprsSystem sharedTableSys = getSysByTask("Shared Table");
        logEvent("sharedTableSys = " + sharedTableSys);
        if (fanucCartSys.isAlertLimitsCheckBoxSelected() || sharedTableSys.isAlertLimitsCheckBoxSelected()
                || fanucCartSys.isEnforceMinMaxLimits() || sharedTableSys.isEnforceMinMaxLimits()) {
            int confirm = JOptionPane.showConfirmDialog(this.displayJFrame,
                    "Disable Alert Limits on " + fanucCartSys + " and " + sharedTableSys);
            if (confirm == JOptionPane.YES_OPTION) {
                sharedTableSys.setAlertLimitsCheckBoxSelected(false);
                fanucCartSys.setAlertLimitsCheckBoxSelected(false);
                sharedTableSys.setEnforceMinMaxLimits(false);
                fanucCartSys.setEnforceMinMaxLimits(false);
            } else {
                return XFuture.completedFuture(false);
            }
        }
        XFutureVoid xf2 = xf1
                .thenComposeAsyncToVoid("startFlipMF.step2", x -> {
                    logEvent("startFlipMF.step2 : xf1=" + xf1);
                    return lookForPartsAll(false);
                }, supervisorExecutorService);

        XFuture<Boolean> xf3 = xf2
                .thenCompose("startFlipMF.step3",
                        x -> {
                            logEvent("startFlipMF.step3 : xf2=" + xf2);
                            sharedTableSys.setExecutorOption(ExecutorOption.ForBoolean.skipMissingParts, false);
                            return sharedTableSys.startActionsList("flip2",
                                    Arrays.asList(new Action[]{
                                Action.newTakePartAction("part_black_gear_in_kt_1"),
                                Action.newMoveRecordedJoints("flipmf_present_gear_prep"),
                                Action.newMoveRecordedPose("flipmf_present_gear")
                            }),
                                    ExecutorOption.ForBoolean.reverseCheckDisabled.with(true),
                                    ExecutorOption.ForBoolean.skipCheckHeldPart.with(true));
                        });
        XFuture<Boolean> xf4 = xf3
                .thenCompose("startFlipMF.step4",
                        x -> {
                            logEvent("startFlipMF.step4 : xf3=" + xf3);
                            fanucCartSys.setExecutorOption(ExecutorOption.ForBoolean.skipMissingParts, false);
                            return fanucCartSys.startActionsList("flip4",
                                    Arrays.asList(new Action[]{
                                Action.newMoveRecordedJoints("prep_new_flip_2_2"),
                                Action.newMoveRecordedPose("new_flip_2_2"),
                                Action.newCloseGripper()
                            }),
                                    ExecutorOption.ForBoolean.reverseCheckDisabled.with(true),
                                    ExecutorOption.ForBoolean.skipCheckHeldPart.with(true));
                        });
        XFuture<Boolean> xf5 = xf4
                .thenCompose("startFlipMF.step5",
                        x -> {
                            logEvent("startFlipMF.step5 : xf4=" + xf4);
                            return sharedTableSys.startActionsList("flip5",
                                    Arrays.asList(new Action[]{
                                Action.newOpenGripper(),}),
                                    ExecutorOption.ForBoolean.reverseCheckDisabled.with(true),
                                    ExecutorOption.ForBoolean.skipCheckHeldPart.with(true));
                        });
        XFuture<Boolean> xf6 = xf5
                .thenCompose("startFlipMF.step6",
                        x -> {
                            logEvent("startFlipMF.step6 : xf5=" + xf5);
                            return fanucCartSys.startActionsList("flip6",
                                    Arrays.asList(new Action[]{
                                Action.newMoveRecordedPose("flip_2_3"),
                                Action.newMoveRecordedPose("flip_2_4"),
                                Action.newMoveRecordedPose("flip_2_5")
                            }),
                                    ExecutorOption.ForBoolean.reverseCheckDisabled.with(true),
                                    ExecutorOption.ForBoolean.skipCheckHeldPart.with(true));
                        });
        XFuture<Boolean> xf7 = xf6
                .thenCompose("startFlipMF.step7",
                        x -> {
                            logEvent("startFlipMF.step7 : xf6=" + xf6);
                            return sharedTableSys.startActionsList("flip7",
                                    Arrays.asList(new Action[]{
                                Action.newCloseGripper()
                            }),
                                    ExecutorOption.ForBoolean.reverseCheckDisabled.with(true),
                                    ExecutorOption.ForBoolean.skipCheckHeldPart.with(true));
                        });
        XFuture<Boolean> xf8 = xf7
                .thenCompose("startFlipMF.step8",
                        x -> {
                            logEvent("startFlipMF.step8 : xf7=" + xf7);
                            return fanucCartSys.startActionsList("flip8",
                                    Arrays.asList(new Action[]{
                                Action.newOpenGripper(),
                                Action.newMoveRecordedPose("flip_2_6"),
                                Action.newLookForParts()
                            }),
                                    ExecutorOption.ForBoolean.reverseCheckDisabled.with(true),
                                    ExecutorOption.ForBoolean.skipCheckHeldPart.with(true));
                        });
        XFuture<Boolean> xf9 = xf8
                .thenCompose("startFlipMF.step9",
                        x -> {
                            logEvent("startFlipMF.step9 : xf8=" + xf8);
                            sharedTableSys.setExecutorOption(ExecutorOption.ForBoolean.skipMissingParts, false);
                            return sharedTableSys.startActionsList("flip9",
                                    Arrays.asList(new Action[]{
                                Action.newMoveRecordedPose("flipmf_present_gear_prep"),
                                Action.newLookForParts(),
                                Action.newPlacePartAction("empty_slot_2_for_large_gear_in_kit_s2l2_vessel_1", "black_gear"),
                                Action.newLookForParts()
                            }),
                                    ExecutorOption.ForBoolean.reverseCheckDisabled.with(true),
                                    ExecutorOption.ForBoolean.skipCheckHeldPart.with(true));
                        });
        return xf9;
    }

    /**
     * Have each system scan the parts area to create an action list to fill
     * kits in a way similar to the current configuration. This may require
     * moving each robot out of the way of the vision system. The scans will
     * happen asynchronously in other threads.
     *
     * @return future that allows actions to be added after all scans are
     * complete.
     */
    public XFutureVoid startScanAll() {

        if (aprsSystems.isEmpty()) {
            throw new IllegalStateException("aprsSystems.isEmpty()");
        }
        @SuppressWarnings({"unchecked", "rawtypes"})
        XFuture xf[] = new XFuture[6];

        XFutureVoid resetAllXF = resetAll(false);
        xf[0] = resetAllXF;
        int startingAbortCount = getAbortCount();

        XFuture<Boolean> checkAndEnableXF = resetAllXF.thenComposeAsync(x -> {
            logEvent("Scan all started.");
            return startCheckAndEnableAllRobots(startingAbortCount);
        }, supervisorExecutorService);
        xf[1] = checkAndEnableXF;
        XFutureVoid step2Xf = checkAndEnableXF.thenComposeAsyncToVoid("scanAll2", ok -> {
            return checkOkElseToVoid(ok, this::scanAllInternal, this::showCheckEnabledErrorSplash);
        }, supervisorExecutorService);
        xf[2] = step2Xf;
        lastStartScanAllFutures = xf;
        return step2Xf
                .peekNoCancelException(this::handleXFutureException)
                .thenComposeAsyncToVoid(() -> this.waitSingleStep("startScanAll"), supervisorExecutorService);
    }

    private final AtomicInteger srtCount = new AtomicInteger();

    /**
     * Perform a test of the Continuous demo where the motoman robot will be
     * randomly enabled and disabled.
     *
     * @return a future that can be used to determine if the test failed or was
     * cancelled.
     */
    public XFutureVoid startRandomTest() {
        int startingAbortCount = getAbortCount();
        int c = srtCount.incrementAndGet();
        logEvent("Start Random Test : " + c);
        connectAll();
        return startCheckAndEnableAllRobots(startingAbortCount)
                .thenComposeAsyncToVoid("startRandomTest.checkOk" + c,
                        ok -> checkOkElseToVoid(ok, () -> startRandomTestStep2(startingAbortCount),
                                this::showCheckEnabledErrorSplash),
                        supervisorExecutorService)
                .peekNoCancelException(this::handleXFutureException)
                .thenComposeAsyncToVoid(() -> this.waitSingleStep("startRandomTest"),
                        supervisorExecutorService);
    }

    private volatile @Nullable XFutureVoid conveyorTestFuture = null;

    public @Nullable
    XFutureVoid getConveyorTestFuture() {
        return conveyorTestFuture;
    }

    public XFutureVoid reverseConveyorTest() {
        if (null == displayJFrame) {
            throw new NullPointerException("displayJFrame");
        }
        logEvent("Start reverseConveyorTest");
        conveyorVisNextCount.set(0);
        conveyorVisPrevCount.set(0);
        AprsSystem sys = displayJFrame.getConveyorVisClonedSystem();
        if (null == sys) {
            throw new NullPointerException("displayJFrame.getConveyorVisClonedSystem()");
        }
        int startAbortCount = sys.getSafeAbortRequestCount();
        int startEnableChangeCount = enableChangeCount.get();
        XFutureVoid ret = conveyorBack(sys, startAbortCount, startEnableChangeCount)
                .thenComposeAsyncToVoid(x -> finishConveyorTest().thenApply(x2 -> (Void) null), supervisorExecutorService);
        conveyorTestFuture = ret;
        return ret;
    }

    public XFutureVoid conveyorTest() {
        if (null == displayJFrame) {
            throw new NullPointerException("displayJFrame");
        }
        logEvent("Start ConveyorTest");
        conveyorVisNextCount.set(0);
        conveyorVisPrevCount.set(0);
        AprsSystem sys = displayJFrame.getConveyorVisClonedSystem();
        if (null == sys) {
            throw new NullPointerException("displayJFrame.getConveyorVisClonedSystem()");
        }
        int startAbortCount = sys.getSafeAbortRequestCount();
        int startEnableChangeCount = enableChangeCount.get();
        XFutureVoid ret = conveyorForward(sys, startAbortCount, startEnableChangeCount)
                .thenComposeAsyncToVoid(x -> finishConveyorTest().thenApply(x2 -> (Void) null),
                        supervisorExecutorService);
        conveyorTestFuture = ret;
        return ret;
    }

    private final AtomicInteger conveyorForwardCount = new AtomicInteger();

    public XFuture<Boolean> conveyorForward(AprsSystem sys, int startAbortCount, int startEnableChangeCount) {
        int count = conveyorForwardCount.incrementAndGet();
        if (sys.getSafeAbortRequestCount() != startAbortCount) {
            logEvent("sys.getSafeAbortRequestCount() != startAbortCount conveyorForward(" + sys + "," + startAbortCount
                    + "," + startEnableChangeCount + ") : count=" + count);
            return XFutureVoid.completedFuture(false);
        }
        int ecCount = enableChangeCount.get();
        if (ecCount != startEnableChangeCount) {
            logEvent("enableChangeCount != startEnableChangeCount conveyorForward(" + sys + "," + startAbortCount + ","
                    + startEnableChangeCount + ") : ecCount=" + ecCount);
            return XFutureVoid.completedFuture(false);
        }
        int prevCount = conveyorVisPrevCount.get();
        int nextCount = conveyorVisNextCount.get();
        final String info = "conveyorForward: count=" + count + ",prev=" + prevCount + ",next=" + nextCount
                + ",startAbortCount=" + startAbortCount + ",ecc=" + startEnableChangeCount;
        try {
            takeAllSnapshots("START_" + info);
        } catch (IOException ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
        }
        sys.takeSnapshots("conveyorForward:" + count);
        return fillTraysAndNextRepeating(sys, startAbortCount, startEnableChangeCount, false)
                .thenComposeAsync((Boolean ok) -> {
                    try {
                        final int currentSafeAbortRequestCount = sys.getSafeAbortRequestCount();
                        boolean abortCountMatch = startAbortCount == currentSafeAbortRequestCount;
                        logEvent("END conveyorForward: abortCountMatch=" + abortCountMatch
                                + ",sys.getSafeAbortRequestCount()=" + currentSafeAbortRequestCount);
                        takeAllSnapshots("END_ok=" + ok + "_" + info);
                    } catch (IOException ex) {
                        Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return checkedConveyorVisPrev(sys, startAbortCount, startEnableChangeCount, ok);
                }, supervisorExecutorService);
    }

    public XFutureVoid repeatingConveyorTest(int maxCount) {
        if (null == displayJFrame) {
            throw new NullPointerException("displayJFrame");
        }

        AprsSystem sys = displayJFrame.getConveyorVisClonedSystem();
        if (null == sys) {
            throw new NullPointerException("displayJFrame.getConveyorVisClonedSystem()");
        }
        int startAbortCount = sys.getSafeAbortRequestCount();
        int startEnableChangeCount = enableChangeCount.get();

        return continueRepeatingConveyorTest(sys, startAbortCount, startEnableChangeCount, maxCount, 0);
    }

    public XFutureVoid reverseRepeatingConveyorTest(int maxCount) {
        if (null == displayJFrame) {
            throw new NullPointerException("displayJFrame");
        }
        AprsSystem sys = displayJFrame.getConveyorVisClonedSystem();
        if (null == sys) {
            throw new NullPointerException("displayJFrame.getConveyorVisClonedSystem()");
        }
        int startAbortCount = sys.getSafeAbortRequestCount();
        int startEnableChangeCount = enableChangeCount.get();
        return continueReverseRepeatingConveyorTest(sys, startAbortCount, startEnableChangeCount, maxCount, 0);
    }

    private XFutureVoid continueRepeatingConveyorTest(AprsSystem sys, int startAbortCount, int startEnableChangeCount,
            int maxCount, int count) {
        final int sysSafeAbortRequestCount = sys.getSafeAbortRequestCount();
        if (sysSafeAbortRequestCount != startAbortCount) {
            logEvent("continueRepeatingConveyorTest:  sysSafeAbortRequestCount != startAbortCount");
            return XFutureVoid.completedFuture();
        }
        int ecCount = enableChangeCount.get();
        if (ecCount != startEnableChangeCount) {
            logEvent("continueRepeatingConveyorTest: enableChangeCount != startEnableChangeCount : ecCount=" + ecCount);
            return XFutureVoid.completedFuture();
        }
        if (maxCount > 0 && count > maxCount) {
            logEvent("continueRepeatingConveyorTest: maxCount > 0 && count > maxCount : count=" + count);
            return finishConveyorTest();
        }
        logEvent("continueRepeatingConveyorTest(" + sys + "," + startAbortCount + "," + startEnableChangeCount + ","
                + maxCount + "," + count + ")");
        conveyorVisNextCount.set(0);
        conveyorVisPrevCount.set(0);
        XFutureVoid ret = conveyorForward(sys, startAbortCount, startEnableChangeCount)
                .thenComposeAsyncToVoid(
                        x -> continueReverseRepeatingConveyorTest(sys, startAbortCount,
                                startEnableChangeCount, maxCount, count + 1),
                        supervisorExecutorService);
        conveyorTestFuture = ret;
        return ret;
    }

    private XFutureVoid continueReverseRepeatingConveyorTest(AprsSystem sys, int startAbortCount,
            int startEnableChangeCount, int maxCount, int count) {
        final int sysSafeAbortRequestCount = sys.getSafeAbortRequestCount();
        if (sysSafeAbortRequestCount != startAbortCount) {
            logEvent("continueReverseRepeatingConveyorTest:  sysSafeAbortRequestCount != startAbortCount");
            return XFutureVoid.completedFuture();
        }
        int ecCount = enableChangeCount.get();
        if (ecCount != startEnableChangeCount) {
            logEvent("continueReverseRepeatingConveyorTest: enableChangeCount != startEnableChangeCount : ecCount="
                    + ecCount);
            return XFutureVoid.completedFuture();
        }
        if (maxCount > 0 && count > maxCount) {
            logEvent("continueReverseRepeatingConveyorTest: maxCount > 0 && count > maxCount : count=" + count);
            return finishConveyorTest();
        }
        logEvent("continueReverseRepeatingConveyorTest(" + sys + "," + startAbortCount + "," + startEnableChangeCount
                + "," + maxCount + "," + count + ")");
        conveyorVisNextCount.set(0);
        conveyorVisPrevCount.set(0);
        XFutureVoid ret = conveyorBack(sys, startAbortCount, startEnableChangeCount)
                .thenComposeAsyncToVoid(
                        x -> continueRepeatingConveyorTest(sys, startAbortCount, startEnableChangeCount, maxCount, count + 1),
                        supervisorExecutorService);
        conveyorTestFuture = ret;
        return ret;
    }

    private final AtomicInteger conveyorBackCount = new AtomicInteger();

    public XFuture<Boolean> conveyorBack(AprsSystem sys, int startAbortCount, int startEnableChangeCount) {
        int count = conveyorBackCount.incrementAndGet();
        if (sys.getSafeAbortRequestCount() != startAbortCount) {
            logEvent("sys.getSafeAbortRequestCount() != startAbortCount conveyorBack(" + sys + "," + startAbortCount
                    + "," + startEnableChangeCount + ") : count=" + count);
            return XFutureVoid.completedFuture(false);
        }
        final int ecc = enableChangeCount.get();
        if (ecc != startEnableChangeCount) {
            logEvent("enableChangeCount.get() != startEnableChangeCount conveyorBack(" + sys + "," + startAbortCount
                    + "," + startEnableChangeCount + ") : ecc=" + ecc);
            return XFutureVoid.completedFuture(false);
        }
        int prevCount = conveyorVisPrevCount.get();
        int nextCount = conveyorVisNextCount.get();
        final String info = "conveyorBack: count=" + count + ",prev=" + prevCount + ",next=" + nextCount
                + ",startAbortCount=" + startAbortCount + ",ecc=" + startEnableChangeCount;
        try {
            takeAllSnapshots("START_" + info);
        } catch (IOException ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
        }
        sys.takeSnapshots("conveyorBack:" + count);
        return emptyTraysAndPrevRepeating(sys, startAbortCount, startEnableChangeCount, false)
                .thenComposeAsync((Boolean ok) -> {
                    try {
                        final int currentSafeAbortRequestCount = sys.getSafeAbortRequestCount();
                        boolean abortCountMatch = startAbortCount == currentSafeAbortRequestCount;
                        logEvent("END conveyorBack: abortCountMatch=" + abortCountMatch
                                + ",sys.getSafeAbortRequestCount()=" + currentSafeAbortRequestCount);
                        takeAllSnapshots("END_ok=" + ok + "_" + info);
                    } catch (IOException ex) {
                        Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return checkedConveyorVisNext(sys, startAbortCount, startEnableChangeCount, ok);
                }, supervisorExecutorService);
    }

    private static volatile @Nullable XFuture<Boolean> fillTraysAndNextRepeatingFuture = null;

    private static volatile @Nullable AprsSystem fillTraysAndNextRepeatingSys = null;

    private final AtomicInteger fillTraysAndNextRepeatingCount = new AtomicInteger();

    private XFuture<Boolean> fillTraysAndNextRepeating(AprsSystem sys, int startAbortCount, int startEnableChangeCount,
            boolean useUnassignedParts) {
        if (sys.getSafeAbortRequestCount() != startAbortCount) {
            logEvent("sys.getSafeAbortRequestCount() != startAbortCount fillTraysAndNextRepeating(" + sys + ","
                    + startAbortCount + "," + startEnableChangeCount + ") : sys.getSafeAbortRequestCount()="
                    + sys.getSafeAbortRequestCount());
            return XFutureVoid.completedFuture(false);
        }
        final int ecc = enableChangeCount.get();
        if (ecc != startEnableChangeCount) {
            logEvent("enableChangeCount.get() != startEnableChangeCount fillTraysAndNextRepeating(" + sys + ","
                    + startAbortCount + "," + startEnableChangeCount + ") : ecc=" + ecc);
            return XFutureVoid.completedFuture(false);
        }
        fillTraysAndNextRepeatingSys = sys;
        int count = fillTraysAndNextRepeatingCount.incrementAndGet();
        logEvent("fillTraysAndNextRepeating(" + sys + "," + startAbortCount + "," + startEnableChangeCount + ","
                + useUnassignedParts + ") : count=" + count);
        sys.clearVisionRequiredParts();
        String blocker = "fillTraysAndNextRepeating" + count;
        final AprsSystem[] systems = new AprsSystem[]{sys};
        XFuture<LockInfo> disallowTogglesFuture = disallowToggles(blocker, systems);
        XFuture<List<PhysicalItem>> itemsFuture = disallowTogglesFuture.thenCompose(x -> {
            logEvent("request vision update for fillTraysAndNextRepeating " + count);
            XFuture<List<PhysicalItem>> ret = sys.getSingleRawVisionUpdate();
            if (sys.isObjectViewSimulated()) {
                logEvent("refreshSimView for fillTraysAndNextRepeating " + count);
                final XFuture<Object2DOuterJPanel.SetItemsResult> future = sys.refreshSimView();
                future.thenAccept((Object2DOuterJPanel.SetItemsResult result) -> logEvent("refreshSimViewResult=" + result));
            }
            return ret;
        });
        final Function<List<PhysicalItem>, XFuture<Boolean>> fillTraysFunction = (List<PhysicalItem> l) -> {
            logEvent("recieved vision update for fillTraysAndNextRepeating " + count);
            return fillTraysAndNextInnerRepeat(l, sys, startAbortCount, startEnableChangeCount, useUnassignedParts);
        };
        final XFuture<List<PhysicalItem>> itemsFutureAfterAllowToggles = itemsFuture
                .alwaysComposeAsync(() -> allowToggles(blocker, systems), supervisorExecutorService)
                .thenApply((XFuture.ComposedPair<List<PhysicalItem>, Void> pair) -> pair.getInput());
        XFuture<Boolean> ret = itemsFutureAfterAllowToggles.thenComposeAsync(fillTraysFunction,
                supervisorExecutorService);
        fillTraysAndNextRepeatingFuture = ret;

        return ret;
    }

    private volatile double lastNonEmptyPos = Double.NaN;

    public XFuture<Boolean> fillTraysAndNextInnerRepeat(List<PhysicalItem> l, AprsSystem sys, int startAbortCount,
            int startEnableChangeCount, boolean useUnassignedParts) {
        if (sys.getSafeAbortRequestCount() != startAbortCount) {
            logEvent(
                    "fillTraysAndNextInnerRepeat: sys.getSafeAbortRequestCount() != startAbortCount fillTraysAndNextInnerRepeat("
                    + sys + "," + startAbortCount + "," + startEnableChangeCount
                    + ") : sys.getSafeAbortRequestCount()=" + sys.getSafeAbortRequestCount());
            return XFutureVoid.completedFuture(false);
        }
        final int ecc = enableChangeCount.get();
        if (ecc != startEnableChangeCount) {
            logEvent(
                    "fillTraysAndNextInnerRepeat: enableChangeCount.get() != startEnableChangeCount fillTraysAndNextInnerRepeat("
                    + sys + "," + startAbortCount + "," + startEnableChangeCount + ") : ecc=" + ecc);
            return XFutureVoid.completedFuture(false);
        }
        int nextCount = conveyorVisNextCount.get();
        logEvent("fillTraysAndNextInnerRepeat: conveyorVisNextCount=" + nextCount + ",pos=" + conveyorPos()
                + ",lastNonEmptyPos=" + lastNonEmptyPos);
        logEvent("fillTraysAndNextInnerRepeat: l = "
                + l.stream().map(PhysicalItem::getName).collect(Collectors.toList()));
        try {
            sys.takeSimViewSnapshot("fillTraysAndNextInnerRepeat:" + nextCount + ",pos=" + conveyorPos()
                    + ",lastNonEmptyPos=" + lastNonEmptyPos, l);
        } catch (Exception ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (!l.isEmpty()) {
            lastNonEmptyPos = conveyorPos();
        }
        if (!l.isEmpty() || nextCount < 2) {
            sys.setCorrectionMode(true);
            return fillTraysAndNextWithItemList(sys, startAbortCount, startEnableChangeCount, l, useUnassignedParts)
                    .thenCompose((Boolean fillCompleted) -> {
                        if (sys.getSafeAbortRequestCount() != startAbortCount) {
                            logEvent(
                                    "fillTraysAndNextInnerRepeat: sys.getSafeAbortRequestCount() != startAbortCount fillTraysAndNextInnerRepeat("
                                    + sys + "," + startAbortCount + "," + startEnableChangeCount
                                    + ") : sys.getSafeAbortRequestCount()=" + sys.getSafeAbortRequestCount());
                            return XFuture.completedFuture(false);
                        }
                        if (fillCompleted) {
                            return fillTraysAndNextRepeating(sys, startAbortCount, startEnableChangeCount,
                                    useUnassignedParts);
                        } else {
                            return XFuture.completedFuture(false);
                        }
                    });
        } else {
            return XFuture.completedFutureWithName(
                    "fillTraysAndNextRepeating : sys.getSingleVisionToDbUpdate().isEmpty()",
                    sys.getSafeAbortRequestCount() == startAbortCount);
        }
    }

    private static volatile @Nullable XFuture<Boolean> emptyTraysAndPrevRepeatingFuture = null;

    private static volatile @Nullable AprsSystem emptyTraysAndPrevRepeatingSys = null;

    private final AtomicInteger emptyTraysAndPrevRepeatingCount = new AtomicInteger();

    private XFuture<Boolean> emptyTraysAndPrevRepeating(AprsSystem sys, int startAbortCount, int startEnableChangeCount,
            boolean useUnassignedParts) {

        if (closing) {
            return XFutureVoid.completedFuture(false);
        }
        if (sys.getSafeAbortRequestCount() != startAbortCount) {
            logEvent("sys.getSafeAbortRequestCount() != startAbortCount emptyTraysAndPrevRepeating(" + sys + ","
                    + startAbortCount + "," + startEnableChangeCount + ") : sys.getSafeAbortRequestCount()="
                    + sys.getSafeAbortRequestCount());
            return XFutureVoid.completedFuture(false);
        }
        final int ecc = enableChangeCount.get();
        if (ecc != startEnableChangeCount) {
            logEvent("enableChangeCount.get() != startEnableChangeCount emptyTraysAndPrevRepeating(" + sys + ","
                    + startAbortCount + "," + startEnableChangeCount + ") : ecc=" + ecc);
            return XFutureVoid.completedFuture(false);
        }
        emptyTraysAndPrevRepeatingSys = sys;
        int count = emptyTraysAndPrevRepeatingCount.incrementAndGet();
        sys.clearVisionRequiredParts();
        logEvent("emptyTraysAndPrevRepeating(" + sys + "," + startAbortCount + "," + startEnableChangeCount + ","
                + useUnassignedParts + ") : count=" + count);
        String blocker = "emptyTraysAndPrevRepeating" + count;
        final AprsSystem[] systems = new AprsSystem[]{sys};
        XFuture<LockInfo> disallowTogglesFuture = disallowToggles(blocker, systems);
        XFuture<List<PhysicalItem>> itemsFuture = disallowTogglesFuture.thenCompose(x -> {
            logEvent("request vision update for emptyTraysAndPrevRepeating " + count);
            XFuture<List<PhysicalItem>> ret = sys.getSingleRawVisionUpdate();
            if (sys.isObjectViewSimulated()) {
                logEvent("refreshSimView for emptyTraysAndPrevRepeating " + count);
                final XFuture<Object2DOuterJPanel.SetItemsResult> future = sys.refreshSimView();
                future.thenAccept((Object2DOuterJPanel.SetItemsResult result) -> logEvent("refreshSimViewResult=" + result));
            }
            return ret;
        });
        XFuture<Boolean> ret = itemsFuture
                .alwaysComposeAsync(() -> allowToggles(blocker, systems), supervisorExecutorService)
                .thenApply((XFuture.ComposedPair<List<PhysicalItem>, Void> pair) -> pair.getInput())
                .thenComposeAsync((List<PhysicalItem> l) -> {
                    logEvent("recieved vision update for emptyTraysAndPrevRepeating " + count);
                    return emptyTraysAndPrevInnerRepeat(sys, startAbortCount, startEnableChangeCount, l,
                            useUnassignedParts);
                }, supervisorExecutorService);
        emptyTraysAndPrevRepeatingFuture = ret;
        return ret;
    }

    public XFuture<Boolean> emptyTraysAndPrevInnerRepeat(AprsSystem sys, int startAbortCount,
            int startEnableChangeCount, List<PhysicalItem> l, boolean useUnassignedParts) {
        if (sys.getSafeAbortRequestCount() != startAbortCount) {
            logEvent("sys.getSafeAbortRequestCount() != startAbortCount emptyTraysAndPrevInnerRepeat(" + sys + ","
                    + startAbortCount + "," + startEnableChangeCount + ") : sys.getSafeAbortRequestCount()="
                    + sys.getSafeAbortRequestCount());
            return XFutureVoid.completedFuture(false);
        }
        final int ecc = enableChangeCount.get();
        if (ecc != startEnableChangeCount) {
            logEvent("enableChangeCount.get() != startEnableChangeCount emptyTraysAndPrevInnerRepeat(" + sys + ","
                    + startAbortCount + "," + startEnableChangeCount + ") : ecc=" + ecc);
            return XFutureVoid.completedFuture(false);
        }
        int prevCount = conveyorVisPrevCount.get();
        logEvent("emptyTraysAndPrevInnerRepeat: conveyorVisPrevCount=" + prevCount + ",startAbortCount="
                + startAbortCount + ",startEnableChangeCount=" + startEnableChangeCount + ",sys=" + sys + ",pos="
                + conveyorPos() + ",lastNonEmptyPos=" + lastNonEmptyPos);
        logEvent("emptyTraysAndPrevInnerRepeat: l = "
                + l.stream().map(PhysicalItem::getName).collect(Collectors.toList()));
        try {
            sys.takeSimViewSnapshot("emptyTraysAndPrevInnerRepeat:" + prevCount, l);
        } catch (Exception ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (!l.isEmpty()) {
            lastNonEmptyPos = conveyorPos();
        }
        if (!l.isEmpty() || prevCount < 2) {
            sys.setCorrectionMode(true);
            return emptyTraysAndPrevWithItemList(sys, startAbortCount, startEnableChangeCount, l, useUnassignedParts)
                    .thenCompose((Boolean emptyCompleted) -> {
                        if (sys.getSafeAbortRequestCount() != startAbortCount) {
                            logEvent("sys.getSafeAbortRequestCount() != startAbortCount emptyTraysAndPrevInnerRepeat("
                                    + sys + "," + startAbortCount + "," + startEnableChangeCount
                                    + ") : sys.getSafeAbortRequestCount()=" + sys.getSafeAbortRequestCount());
                            return XFuture.completedFuture(false);
                        }
                        if (emptyCompleted) {
                            return emptyTraysAndPrevRepeating(sys, startAbortCount, startEnableChangeCount,
                                    useUnassignedParts);
                        } else {
                            return XFuture.completedFuture(emptyCompleted);
                        }
                    });
        } else {
            return XFuture.completedFutureWithName(
                    "emptyTraysAndPrevRepeating : sys.getSingleVisionToDbUpdate().isEmpty()",
                    sys.getSafeAbortRequestCount() == startAbortCount);
        }
    }

    private XFutureVoid finishConveyorTest() {
        logEvent("ConveyorTest finished");
        if (null != displayJFrame) {
            XFuture<Boolean> future = Utils.composeOnDispatchThread(() -> {
                if (null != displayJFrame) {
                    if (displayJFrame.isShowSplashMessagesSelected()) {
                        final GraphicsConfiguration graphicsConfiguration = requireNonNull(
                                displayJFrame.getGraphicsConfiguration(), "displayJFrame.getGraphicsConfiguration()");
                        final GraphicsDevice gd = graphicsConfiguration.getDevice();
                        return displayJFrame.showMessageFullScreen("ConveyorTest finished", 80.0f, null,
                                SplashScreen.getBlueWhiteGreenColorList(), gd).thenApply(x -> true);
                    } else {
                        return MultiLineStringJPanel.showText("ConveyorTest finished");
                    }
                } else {
                    return MultiLineStringJPanel.showText("ConveyorTest finished");
                }
            });
            return future.thenRun(() -> {
            });
        } else {
            return XFutureVoid.completedFuture();
        }
    }

    private final AtomicInteger fillTraysCount = new AtomicInteger();

    private XFuture<Boolean> fillTraysAndNextWithItemList(AprsSystem sys, int startAbortCount,
            int startEnableChangeCount, List<PhysicalItem> items, boolean useUnassignedParts) {
        if (sys.getSafeAbortRequestCount() != startAbortCount) {
            logEvent(
                    "fillTraysAndNextWithItemList: sys.getSafeAbortRequestCount() != startAbortCount fillTraysAndNextWithItemList("
                    + sys + "," + startAbortCount + "," + startEnableChangeCount
                    + ") : sys.getSafeAbortRequestCount()=" + sys.getSafeAbortRequestCount());
            return XFutureVoid.completedFuture(false);
        }
        final int ecc = enableChangeCount.get();
        if (ecc != startEnableChangeCount) {
            logEvent(
                    "fillTraysAndNextWithItemList: enableChangeCount.get() != startEnableChangeCount fillTraysAndNextWithItemList("
                    + sys + "," + startAbortCount + "," + startEnableChangeCount + ") : ecc=" + ecc);
            return XFutureVoid.completedFuture(false);
        }
        logEvent("Fill Kit Trays " + fillTraysCount.incrementAndGet());
        sys.clearVisionRequiredParts();
        try {
            sys.takeSimViewSnapshot("fillTraysAndNextWithItemList", items);
        } catch (Exception ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sys.fillKitTraysWithItemList(items, useUnassignedParts).thenComposeAsync((Boolean fillCompleted) -> {
            return checkedConveyorVisNext(sys, startAbortCount, startEnableChangeCount, fillCompleted);
        }, supervisorExecutorService);
    }

    private volatile @Nullable XFutureVoid lastConveyorVisNextRet = null;
    private volatile @Nullable XFuture<List<PhysicalItem>> lastCheckedConveyorVisNextSimViewUpdateRet = null;

    private XFuture<Boolean> checkedConveyorVisNext(AprsSystem sys, int startAbortCount, int startEnableChangeCount,
            Boolean lastStepOk) {
        final int convNextCount = conveyorVisNextCount.get();
        logEvent(
                "checkedConveyorVisNext(" + sys + "," + startAbortCount + "," + lastStepOk + ") : conveyorVisNextCount="
                + convNextCount + ",pos=" + conveyorPos() + ",lastNonEmptyPos=" + lastNonEmptyPos);
        if (sys.getSafeAbortRequestCount() != startAbortCount) {
            logEvent("checkedConveyorVisNext: sys.getSafeAbortRequestCount() != startAbortCount");
            return XFuture.completedFuture(false);
        }
        final int ecc = enableChangeCount.get();
        if (ecc != startEnableChangeCount) {
            logEvent("enableChangeCount.get() != startEnableChangeCount checkedConveyorVisNext(" + sys + ","
                    + startAbortCount + "," + startEnableChangeCount + ") : ecc=" + ecc);
            return XFutureVoid.completedFuture(false);
        }
        String titleErrorString = sys.getTitleErrorString();
        if (null != titleErrorString && titleErrorString.length() > 0) {
            logEvent("checkedConveyorVisNext: null != titleErrorString && titleErrorString.length() > 0");
            return XFuture.completedFuture(false);
        }
        boolean sysConnected = sys.isConnected();
        if (!sysConnected) {
            logEvent("checkedConveyorVisNext: !sysConnected");
            return XFuture.completedFuture(false);
        }
        boolean sysAborting = sys.isAborting();
        if (sysAborting) {
            logEvent("checkedConveyorVisNext: sysAborting");
            return XFuture.completedFuture(false);
        }
//        if (blockConveyorMoves) {
//            logEvent("checkedConveyorVisNext: blockConveyorMoves" + setBlockConveyorMovesComment);
//            System.err.println("setBlockConveyorMovesTrace = " + Utils.traceToString(setBlockConveyorMovesTrace));
//            return waitTogglesAllowed()
//                    .thenCompose(x -> waitResume())
//                    .thenComposeAsync(x -> checkedConveyorVisNext(sys, startAbortCount, startEnableChangeCount, lastStepOk), supervisorExecutorService);
//        }
        if (null != lastStepOk && lastStepOk) {
            AprsSystem systems[] = new AprsSystem[]{sys};
            String blockerName = "checkedConveyorVisNext" + convNextCount;
            XFuture<LockInfo> disallowTogglesFuture = disallowToggles(blockerName, systems);
            if (sys.isAborting() || blockConveyorMoves || sys.getSafeAbortRequestCount() != startAbortCount) {
                return disallowTogglesFuture.alwaysCompose(() -> allowToggles(blockerName, systems))
                        .thenApply(x -> false);
            }
            try {
                takeAllSnapshots("START conveyorVisNex:" + convNextCount + ",pos=" + conveyorPos() + " ");
            } catch (IOException ex) {
                Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
            }
            XFutureVoid conveyorVisNextRet = disallowTogglesFuture
                    .thenComposeAsyncToVoid(x -> {
                        sys.clearVisionRequiredParts();
                        return conveyorVisNext();
                    }, supervisorExecutorService)
                    .alwaysRun(() -> {
                        try {
                            takeAllSnapshots("END conveyorVisNex:" + convNextCount + ",pos=" + conveyorPos() + " ");
                        } catch (IOException ex) {
                            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    });
            lastConveyorVisNextRet = conveyorVisNextRet;
            XFuture<List<PhysicalItem>> simViewUpdateRet = conveyorVisNextRet.thenCompose(x -> sys.getSimViewUpdate());
            lastCheckedConveyorVisNextSimViewUpdateRet = simViewUpdateRet;
            return simViewUpdateRet.thenAccept((List<PhysicalItem> l) -> {
                try {
                    final String info = "checkedConveyorVisNext:" + convNextCount + ",pos=" + conveyorPos()
                            + ",lastNonEmptyPos=" + lastNonEmptyPos;
                    logEvent(info);
                    sys.takeSimViewSnapshot(info, l);
                } catch (Exception ex) {
                    Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }).thenApply(x -> lastStepOk && sys.getSafeAbortRequestCount() == startAbortCount)
                    .alwaysComposeAsyncToInput(() -> allowToggles(blockerName, systems), supervisorExecutorService);
        } else {
            return XFuture.completedFutureWithName("skippedConveyorVisNext", lastStepOk);
        }
    }

    private XFuture<Boolean> checkedConveyorVisPrev(AprsSystem sys, int startAbortCount, int startEnableChangeCount,
            Boolean lastStepOk) {
        final int convPrevCount = conveyorVisPrevCount.get();
        logEvent(
                "checkedConveyorVisPrev(" + sys + "," + startAbortCount + "," + lastStepOk + ") : conveyorVisPrevCount="
                + convPrevCount + ",pos=" + conveyorPos() + ",lastNonEmptyPos=" + lastNonEmptyPos);
        if (sys.getSafeAbortRequestCount() != startAbortCount) {
            logEvent("checkedConveyorVisPrev: sys.getSafeAbortRequestCount() != startAbortCount");
            return XFuture.completedFuture(false);
        }
        final int ecc = enableChangeCount.get();
        if (ecc != startEnableChangeCount) {
            logEvent("enableChangeCount.get() != startEnableChangeCount checkedConveyorVisPrev(" + sys + ","
                    + startAbortCount + "," + startEnableChangeCount + ") : ecc=" + ecc);
            return XFutureVoid.completedFuture(false);
        }
        String titleErrorString = sys.getTitleErrorString();
        if (null != titleErrorString && titleErrorString.length() > 0) {
            logEvent("checkedConveyorVisPrev: null != titleErrorString && titleErrorString.length() > 0");
            return XFuture.completedFuture(false);
        }
        boolean sysConnected = sys.isConnected();
        if (!sysConnected) {
            logEvent("checkedConveyorVisPrev: !sysConnected");
            return XFuture.completedFuture(false);
        }
        boolean sysAborting = sys.isAborting();
        if (sysAborting) {
            logEvent("checkedConveyorVisPrev: sysAborting");
            return XFuture.completedFuture(false);
        }
        if (null == lastStepOk) {
            logEvent("checkedConveyorVisPrev: null == lastStepOk");
            return XFuture.completedFuture(false);
        }
//        if (blockConveyorMoves) {
//            logEvent("checkedConveyorVisPrev: blockConveyorMoves" + setBlockConveyorMovesComment);
//            System.err.println("setBlockConveyorMovesTrace = " + Utils.traceToString(setBlockConveyorMovesTrace));
//            return waitTogglesAllowed()
//                    .thenCompose(() -> waitResume())
//                    .thenComposeAsync(x -> checkedConveyorVisPrev(sys, startAbortCount, startEnableChangeCount, lastStepOk), supervisorExecutorService);
//        }
        if (null != lastStepOk && lastStepOk) {
            AprsSystem systems[] = new AprsSystem[]{sys};

            String blockerName = "checkedConveyorVisPrev" + convPrevCount;
            XFuture<LockInfo> disallowTogglesFuture = disallowToggles(blockerName, systems);
            if (sys.isAborting() || blockConveyorMoves || sys.getSafeAbortRequestCount() != startAbortCount) {
                return disallowTogglesFuture.alwaysCompose(() -> allowToggles(blockerName, systems))
                        .thenApply(x -> false);
            }
            try {
                takeAllSnapshots("START conveyorVisPrev:" + convPrevCount + ",pos=" + conveyorPos() + " ");
            } catch (IOException ex) {
                Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
            }
            return disallowTogglesFuture.thenCompose(x -> {
                sys.clearVisionRequiredParts();
                return conveyorVisPrev();
            }).alwaysRun(() -> {
                try {
                    takeAllSnapshots("END conveyorVisPrev:" + convPrevCount + ",pos=" + conveyorPos() + " ");
                } catch (IOException ex) {
                    Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }).thenCompose(x -> sys.getSimViewUpdate()).thenAccept((List<PhysicalItem> l) -> {
                try {
                    final String info = "checkedConveyorVisPrev:" + convPrevCount + ",pos=" + conveyorPos()
                            + ",lastNonEmptyPos=" + lastNonEmptyPos;
                    logEvent(info);
                    sys.takeSimViewSnapshot(info, l);
                } catch (Exception ex) {
                    Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }).thenApply(x -> lastStepOk && sys.getSafeAbortRequestCount() == startAbortCount)
                    .alwaysComposeAsyncToInput(() -> allowToggles(blockerName, systems), supervisorExecutorService);
        } else {
            return XFutureVoid.completedFutureWithName("skippedConveyorVisPrev", lastStepOk);
        }
    }

    private final AtomicInteger emptyTraysCount = new AtomicInteger();

    private XFuture<Boolean> emptyTraysAndPrevWithItemList(AprsSystem sys, int startAbortCount,
            int startEnableChangeCount, List<PhysicalItem> items, boolean useUnassignedParts) {
        if (sys.getSafeAbortRequestCount() != startAbortCount) {
            logEvent("sys.getSafeAbortRequestCount() != startAbortCount emptyTraysAndPrevWithItemList(" + sys + ","
                    + startAbortCount + "," + startEnableChangeCount + ") : sys.getSafeAbortRequestCount()="
                    + sys.getSafeAbortRequestCount());
            return XFutureVoid.completedFuture(false);
        }
        final int ecc = enableChangeCount.get();
        if (ecc != startEnableChangeCount) {
            logEvent("enableChangeCount.get() != startEnableChangeCount emptyTraysAndPrevWithItemList(" + sys + ","
                    + startAbortCount + "," + startEnableChangeCount + ") : ecc=" + ecc);
            return XFutureVoid.completedFuture(false);
        }
        logEvent("Empty Kit Trays " + emptyTraysCount.incrementAndGet());
        sys.clearVisionRequiredParts();
        try {
            sys.takeSimViewSnapshot("emptyTraysAndPrevWithItemList", items);
        } catch (Exception ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sys.emptyKitTraysWithItemList(items).thenComposeAsync((Boolean fillCompleted) -> {
            return checkedConveyorVisPrev(sys, startAbortCount, startEnableChangeCount, fillCompleted);
        }, supervisorExecutorService);
    }

    private final AtomicInteger conveyorVisNextCount = new AtomicInteger();

    public double conveyorPos() {
        if (null == displayJFrame) {
            throw new NullPointerException("displayJFrame");
        }
        return displayJFrame.conveyorPos();
    }

    private volatile long conveyorVisNextStartTime = -1;
    private volatile long conveyorVisNextEndTime = -1;
    private volatile double conveyorVisNextStartPos = -1;
    private volatile double conveyorVisNextEndPos = -1;

    private XFutureVoid conveyorVisNext() {
        final AprsSupervisorDisplayJFrame displayJFrameFinal = displayJFrame;
        if (null == displayJFrameFinal) {
            throw new NullPointerException("displayJFrame");
        }
        conveyorVisNextStartTime = System.currentTimeMillis();
        conveyorVisNextStartPos = conveyorPos();
        int count = conveyorVisNextCount.incrementAndGet();
        logEvent("Conveyor Next Starting " + count);
        XFutureVoid ret = this.waitResume()
                .thenComposeAsyncToVoid(() -> displayJFrameFinal.conveyorVisNextTray(), supervisorExecutorService)
                .thenRun(() -> conveyorVisNextFinish(count))
                .peekNoCancelException(this::handleXFutureException)
                .thenComposeAsyncToVoid(() -> this.waitSingleStep("conveyorVisNext"), supervisorExecutorService);
        conveyorTestFuture = ret;
        return ret;
    }

    private void conveyorVisNextFinish(int count) {
        long now = System.currentTimeMillis();
        long timeDiff = now - conveyorVisNextStartTime;
        conveyorVisNextEndTime = now;
        long totalConveyorMoveTime = conveyorMoveTime.addAndGet(timeDiff);
        double pos = conveyorPos();
        double posDiff = pos - conveyorVisNextStartPos;
        conveyorVisNextEndPos = now;
        int moveCount = conveyorMoveCount.incrementAndGet();
        logEvent("Conveyor Next finished. count=" + count + ", timeDiff=" + timeDiff + ",pos=" + pos + ",posDiff="
                + posDiff + ",totalConveyorMoveTime=" + totalConveyorMoveTime + ",moveCount=" + moveCount);
    }

    private volatile boolean singleStepping = false;

    /**
     * Get the value of singleStepping
     *
     * @return the value of singleStepping
     */
    public boolean isSingleStepping() {
        return singleStepping;
    }

    // private AtomicReference<XFutureVoid> stepperFuture = new
    // AtomicReference<>(new XFutureVoid("supervisorStepperFuture"));
//    public void advanceSingleStep() {
//        if (singleStepping) {
//            stepperFuture.getAndSet(new XFutureVoid("supervisorStepperFuture")).complete();
//        }
//    }
    public static <T> T q(T t, T other) {
        return (t != null) ? t : other;
    }

    private XFutureVoid waitSingleStep(String info) {
        if (!singleStepping) {
            return STEPPER_COMPLETED_FUTURE;
        } else {
            System.out.println();
            System.out.flush();
            System.err.println();
            System.err.flush();
            logEvent("Single Step stopping for " + info);
            System.out.println();
            System.out.flush();
            System.err.println();
            System.err.flush();
            Thread.dumpStack();
            System.out.println();
            System.out.flush();
            System.err.println();
            System.err.flush();
            String message = "Single Step: " + info;
            return showMesssage(message)
                    .thenComposeAsyncToVoid(info, () -> logEvent("Single Step advancing " + info),
                            supervisorExecutorService);
        }
    }

    @SuppressWarnings("nullness")
    public XFutureVoid showMesssage(String message) {
        if (!CRCLUtils.isGraphicsEnvironmentHeadless()) {
            return Utils.runOnDispatchThread(() -> {
                JOptionPane.showMessageDialog(displayJFrame, message);
            });
        } else {
            return XFutureVoid.completedFuture();
        }
    }

    private static final XFutureVoid STEPPER_COMPLETED_FUTURE = XFutureVoid.completedFuture();

    /**
     * Set the value of singleStepping
     *
     * @param singleStepping new value of singleStepping
     */
    public void setSingleStepping(boolean singleStepping) {
//        if (!singleStepping) {
//            final XFutureVoid oldStepperFuture = stepperFuture.getAndSet(STEPPER_COMPLETED_FUTURE);
//            if (!oldStepperFuture.isDone()) {
//                oldStepperFuture.complete();
//            }
//        } else {
//            stepperFuture.set(new XFutureVoid("supervisorStepperFuture"));
//        }
        this.singleStepping = singleStepping;
    }

    private final AtomicInteger conveyorVisPrevCount = new AtomicInteger();
    private final AtomicInteger conveyorMoveCount = new AtomicInteger();
    private final AtomicLong conveyorMoveTime = new AtomicLong();
    private volatile long conveyorVisPrevStartTime = -1;
    private volatile long conveyorVisPrevEndTime = -1;
    private volatile double conveyorVisPrevStartPos = -1;
    private volatile double conveyorVisPrevEndPos = -1;

    private XFutureVoid conveyorVisPrev() {
        final AprsSupervisorDisplayJFrame displayJFrameFinal = displayJFrame;
        if (null == displayJFrameFinal) {
            throw new NullPointerException("displayJFrame");
        }

        conveyorVisPrevStartTime = System.currentTimeMillis();
        conveyorVisPrevStartPos = conveyorPos();
        int count = conveyorVisPrevCount.incrementAndGet();
        logEvent("Conveyor Prev Starting " + count);
        XFutureVoid ret = this.waitResume()
                .thenComposeAsyncToVoid(() -> displayJFrameFinal.conveyorVisPrevTray(), supervisorExecutorService)
                .thenRun(() -> conveyorVisPrevFinish(count))
                .peekNoCancelException(this::handleXFutureException)
                .thenComposeAsyncToVoid(() -> this.waitSingleStep("conveyorVisPrev"), supervisorExecutorService);
        conveyorTestFuture = ret;
        return ret;
    }

    private void conveyorVisPrevFinish(int count) {
        long now = System.currentTimeMillis();
        long timeDiff = now - conveyorVisPrevStartTime;
        conveyorVisPrevEndTime = now;
        long totalConveyorMove = conveyorMoveTime.addAndGet(timeDiff);
        double pos = conveyorPos();
        double posDiff = pos - conveyorVisPrevStartPos;
        conveyorVisPrevEndPos = now;
        int moveCount = conveyorMoveCount.incrementAndGet();
        logEvent("Conveyor Prev finished. count=" + count + ", timeDiff=" + timeDiff + ",pos=" + pos + ",posDiff="
                + posDiff + ",totalConveyorMove=" + totalConveyorMove + ",moveCount=" + moveCount);
    }

    private final AtomicInteger srts2Count = new AtomicInteger();

    private XFutureVoid startRandomTestStep2(int startingAbortCount) {
        int c = srts2Count.incrementAndGet();
        final boolean continuousDemoRevFirstSelected = isContinuousDemoRevFirstSelected();
        logEvent("Start Random Test Step 2 :" + c + ", continuousDemoRevFirstSelected="
                + continuousDemoRevFirstSelected);
        XFutureVoid f1;
        if (!continuousDemoRevFirstSelected) {
            f1 = startContinuousDemo();
        } else {
            f1 = startPrivateContinuousDemoRevFirst();
        }
        ContinuousDemoFuture = f1;
        setContinuousDemoSelected(true);
        XFutureVoid f2 = continueRandomTest(startingAbortCount);
        randomTestFuture = f2;
        resetMainRandomTestFuture();
        return XFuture.allOfWithName("startRandomTestStep2.allOff" + c, f2, f1)
                .peekNoCancelException(this::handleXFutureException)
                .thenComposeAsyncToVoid(() -> this.waitSingleStep("startRandomTestStep2"), supervisorExecutorService);
    }

    private XFutureVoid startIndRandomTestStep2(int startingAbortCount) {
        XFutureVoid f1 = startAllIndContinuousDemo();
        ContinuousDemoFuture = f1;
        setContinuousDemoSelected(true);
        XFutureVoid f2 = continueRandomTest(startingAbortCount);
        randomTestFuture = f2;
        resetMainRandomTestFuture();
        return XFuture.allOfWithName("startRandomTestStep2.allOff", f1, f2);
    }

    private Random random = new Random(System.currentTimeMillis());

    public Random getRandom() {
        return random;
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    public long getTotalRandomDelays() {
        return totalRandomDelays.get();
    }

    public int getRandomDelayCount() {
        return randomDelayCount.get();
    }

    public int getIgnoredToggles() {
        return ignoredToggles.get();
    }

    private XFutureVoid startRandomDelay(String name, final int millis, final int min_millis) {
        if (closing) {
            return XFutureVoid.completedFuture();
        }
        final long val = random.nextInt(millis) + 10 + min_millis;
        return XFuture.runAsync(name + ".randomDelay(" + millis + ":" + val + ")", () -> {
            try {
                if (!closing) {
                    totalRandomDelays.addAndGet(val);
                    randomDelayCount.incrementAndGet();
                    Thread.sleep(val);
                }
            } catch (InterruptedException ex) {
                log(Level.SEVERE, "", ex);
            }
        }, randomDelayExecutorService);

    }

    private static Logger getLogger() {
        return Logger.getLogger(Supervisor.class.getName());
    }

    private volatile boolean togglesAllowed = true;
    private final AtomicInteger waitForTogglesFutureCount;

    private final ConcurrentLinkedDeque<XFutureVoid> waitForTogglesFutures;

    private XFutureVoid createWaitForTogglesFuture(@Nullable XFutureVoid old) {
        if (null != old && !old.isDone()) {
            return old;
        }
        XFutureVoid xf = new XFutureVoid("waitForTogglesAllowed" + waitForTogglesFutureCount.incrementAndGet());
        waitForTogglesFutures.add(xf);
        return xf;
    }

    private static XFutureVoid createFirstWaitForTogglesFuture(Deque<XFutureVoid> waitForTogglesFutures,
            AtomicInteger waitForTogglesFutureCount) {
        XFutureVoid xf = new XFutureVoid("waitForTogglesAllowed" + waitForTogglesFutureCount.incrementAndGet());
        waitForTogglesFutures.add(xf);
        return xf;
    }

    private final AtomicReference<@Nullable XFutureVoid> togglesAllowedXfuture;
    private final AtomicInteger waitTogglesAllowedCount = new AtomicInteger();

    private XFutureVoid waitTogglesAllowed() {
        XFutureVoid xf = togglesAllowedXfuture.getAndSet(null);
        int count = waitTogglesAllowedCount.incrementAndGet();
        if (null != xf && !xf.isDone() && !checkMaxCycles()) {
            logEvent("START waitTogglesAllowed " + count);
            return xf.peekNoCancelException(this::handleXFutureException).alwaysRun(() -> {
                logEvent("END waitTogglesAllowed " + count);
            });
        } else {
            logEvent("SKIP waitTogglesAllowed " + count);
            return XFutureVoid.completedFutureWithName("completedWaitTogglesAllowed");
        }
    }

    private volatile String roboteEnableToggleBlockerText = "";

    private void setRobotEnableToggleBlockerText(String text) {
        if (null != displayJFrame) {
            displayJFrame.setRobotEnableToggleBlockerText(text);
        }
        roboteEnableToggleBlockerText = text;
    }

    private boolean keepDisabled = false;

    /**
     * Get the value of keepDisabled
     *
     * @return the value of keepDisabled
     */
    public boolean isKeepDisabled() {
        return keepDisabled;
    }

    /**
     * Set the value of keepDisabled
     *
     * @param keepDisabled new value of keepDisabled
     */
    public void setKeepDisabled(boolean keepDisabled) {
        this.keepDisabled = keepDisabled;
    }

    private boolean skipDisabled;

    /**
     * Get the value of skipDisabled
     *
     * @return the value of skipDisabled
     */
    public boolean isSkipDisabled() {
        return skipDisabled;
    }

    /**
     * Set the value of skipDisabled
     *
     * @param skipDisabled new value of skipDisabled
     */
    public void setSkipDisabled(boolean skipDisabled) {
        this.skipDisabled = skipDisabled;
    }

    private volatile StackTraceElement allowTogglesTrace@Nullable []  = null;

    private void clearAllToggleBlockers() {
        logEvent("clearAllToggleBlockers toggleBlockerMap.keySet()=" + toggleBlockerMap.keySet());

        allowTogglesCount.incrementAndGet();
        allowTogglesTrace = Thread.currentThread().getStackTrace();
        for (LockInfo li : toggleBlockerMap.values()) {
            li.getFuture().cancelAll(true);
        }
        toggleBlockerMap.clear();
        togglesAllowed = true;
        String blockerList = toggleBlockerMap.toString();
        runOnDispatchThread(() -> {
            showTogglesEnabled(true);
            setRobotEnableToggleBlockerText(blockerList);
        });
        XFutureVoid xf = togglesAllowedXfuture.get();
        if (null != xf) {
            xf.complete((Void) null);
        }
        while ((xf = waitForTogglesFutures.poll()) != null) {
            xf.complete((Void) null);
        }
    }

    private final AtomicLong totalBlockTime = new AtomicLong();

    synchronized XFutureVoid allowToggles(String blockerName, AprsSystem... systems) {
        return allowTogglesInternal(blockerName, true, systems);
    }

    synchronized XFutureVoid allowToggles(String blockerName) {
        return allowTogglesInternal(blockerName, true);
    }

    synchronized XFutureVoid allowTogglesNoCheck(@Nullable String... blockerNames) {

        if (null != blockerNames) {
            List<XFutureVoid> l = new ArrayList<>();
            for (String blockerName : blockerNames) {
                if (null != blockerName && blockerName.length() > 0) {
                    XFutureVoid f = allowTogglesInternal(blockerName, false);
                    l.add(f);
                }
            }
            return XFutureVoid.allOfWithName("allowTogglesNoCheck", l);
        }
        return XFutureVoid.completedFutureWithName("allowTogglesNoCheck");
    }

    private void blockerListConsumer(String blockerList) {
        if (closing) {
            return;
        }
        showTogglesEnabled(togglesAllowed);
        setRobotEnableToggleBlockerText(blockerList);
    }

    private synchronized XFutureVoid allowTogglesInternal(String blockerName, boolean withChecks,
            AprsSystem... systems) {

        if (this.supervisorThread != Thread.currentThread()) {
            throw new RuntimeException("called from wrong thread " + Thread.currentThread());
        }
        if (closing) {
            return XFutureVoid.completedFutureWithName("closing");
        }
        try {
            if (null != systems && systems.length > 0 && withChecks) {
                for (AprsSystem sys : systems) {
                    boolean sysConnected = sys.isConnected();
                    boolean sysAborting = sys.isAborting();
                    if (!checkMaxCycles()) {
                        return checkLastReturnedFuture(null, "checkMaxCycles").thenRun(() -> {
                            logEvent("checkMaxCycles hit in allowTogglesInternal " + blockerName);
                        });
                    } else if (((sys.getRobotName() == null || !sysConnected) && !keepDisabled) || sysAborting) {
                        debugAction();
                        System.err.println("sys.isConnected() = " + sysConnected);
                        System.err.println("sys.isAborting() = " + sysAborting);
                        System.err.println("sys.getRobotName()=" + sys.getRobotName());
                        Thread.dumpStack();
                        if (sysAborting) {
                            sys.printAbortingInfo(System.err);
                        }
                        System.out.println("returnRobotsThread = " + returnRobotsThread);
                        System.out.println("returnRobotsTime = " + returnRobotsTime);
                        System.out.println("returnRobotsStackTrace = " + Utils.traceToString(returnRobotsStackTrace));
                        String badStateMsg = "allowToggles(" + blockerName + ") : sysConnected=" + sysConnected
                                + ",keepDisabled=" + keepDisabled + ",sysAborting=" + sysAborting + ", bad state for "
                                + sys;
                        logEvent(badStateMsg);
                        sys.printRobotNameActivy(System.err);
                        throw new IllegalStateException(badStateMsg);
                    }
                }
            }
            boolean origTogglesAllowed = togglesAllowed;
            allowTogglesCount.incrementAndGet();
            allowTogglesTrace = Thread.currentThread().getStackTrace();
            LockInfo lockInfo = toggleBlockerMap.remove(blockerName);
            String blockerList = toggleBlockerMap.keySet().toString();

            if (null == lockInfo && withChecks) {
                final String errmsg = "allowToggle called for blocker \"" + blockerName + " \"not in toggleBlockerMap "
                        + toggleBlockerMap;
                logEventErr(errmsg);
                throw new RuntimeException(errmsg);
            } else {
                if (null != lockInfo) {
                    long time = lockInfo.getStartTime();
                    long blockTime = (System.currentTimeMillis() - time);
                    togglesAllowed = toggleBlockerMap.isEmpty();
                    if (togglesAllowed && !origTogglesAllowed) {
                        totalBlockTime.addAndGet(blockTime);
                    }
                    logEvent("allowToggles(" + blockerName + ") after " + blockTime + "ms : blockers=" + blockerList
                            + ", totalBlockTime=" + (totalBlockTime.get() / 1000) + "s");

                } else {
                    logEvent("allowToggles(" + blockerName + ") lockInfo == null,blockers=" + blockerList);
                }
                final boolean showTogglesEnabledArg = togglesAllowed;
                return submitDisplayConsumer(this::blockerListConsumer, blockerList).thenRunAsync(
                        "finishAllowToggle." + blockerName, () -> finishAllowToggles(lockInfo),
                        supervisorExecutorService);
            }

        } catch (Exception ex) {
            log(Level.SEVERE, "", ex);
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
        }
    }

    private void finishAllowToggles(@Nullable LockInfo lockInfo) {
        if (togglesAllowed) {
            XFutureVoid xf = togglesAllowedXfuture.get();
            if (null != xf) {
                xf.complete((Void) null);
            }
            while ((xf = waitForTogglesFutures.poll()) != null) {
                xf.complete((Void) null);
            }
        }
        if (null != lockInfo) {
            lockInfo.getFuture().complete(null);
        }
    }

    private final AtomicInteger allowTogglesCount = new AtomicInteger();
    private final AtomicInteger disallowTogglesCount = new AtomicInteger();
    private final ConcurrentHashMap<String, LockInfo> toggleBlockerMap = new ConcurrentHashMap<>();

    public synchronized Map<String, LockInfo> getToggleBlockerMap() {
        return Collections.unmodifiableMap(new TreeMap<>(toggleBlockerMap));
    }

    private volatile @Nullable XFuture<LockInfo> lastDisallowTogglesFuture = null;
    private volatile @Nullable StackTraceElement lastDisallowTogglesTrace@Nullable []  = null;

    private synchronized XFuture<LockInfo> disallowToggles(String blockerName) {

        LockInfo lockInfo = disallowTogglesPart1(blockerName);
        String blockerList = toggleBlockerMap.keySet().toString();
        XFuture<LockInfo> ret = completeDisallowToggles(blockerName, blockerList, lockInfo);
        return ret;
    }

    private XFuture<LockInfo> completeDisallowToggles(String blockerName, String blockerList, LockInfo lockInfo) {
        if (closing) {
            return XFuture.completedFuture(lockInfo);
        }
        XFutureVoid showTogglesFuture = submitDisplayConsumer(this::blockerListConsumer, blockerList);
        XFuture<LockInfo> ret = showTogglesFuture.thenApply(x -> lockInfo);
        lastDisallowTogglesFuture = ret;
        return ret;
    }

    private LockInfo disallowTogglesPart1(String blockerName) throws RuntimeException {
        if (this.supervisorThread != Thread.currentThread()) {
            throw new RuntimeException("disallowTogglesPart1 called from wrong thread =" + Thread.currentThread());
        }
        if (blockerName == null || blockerName.length() < 1 || toggleBlockerMap.containsKey(blockerName)) {
            throw new IllegalArgumentException(
                    "blockrName=" + blockerName + ",toggleBlockerMap.keySet()=" + toggleBlockerMap.keySet());
        }
        int dtc = disallowTogglesCount.incrementAndGet();
        lastDisallowTogglesTrace = Thread.currentThread().getStackTrace();
        LockInfo lockInfo = new LockInfo(blockerName);
        toggleBlockerMap.put(blockerName, lockInfo);
        int tc = togglesCount.get();
        String blockerList = toggleBlockerMap.keySet().toString();
        logEvent("disallowToggles(" + blockerName + ") togglesCount= " + tc + ", disallowTogglesCount= " + dtc
                + ", blockers=" + blockerList);
        togglesAllowed = false;
        togglesAllowedXfuture.updateAndGet(this::createWaitForTogglesFuture);
        return lockInfo;
    }

    synchronized XFuture<LockInfo> disallowToggles(String blockerName, AprsSystem... systems) {

        LockInfo lockInfo = disallowTogglesPart1(blockerName);
        if (null != systems) {
            for (AprsSystem sys : systems) {
                addFinishBlocker(sys.getMyThreadId(), lockInfo.getFuture());
            }
        }
        String blockerList = toggleBlockerMap.keySet().toString();

        XFuture<LockInfo> ret = completeDisallowToggles(blockerName, blockerList, lockInfo)
                .peekNoCancelException(this::handleXFutureException)
                .thenCompose(x -> this.waitSingleStep("disallowToggles(" + blockerName + ")").thenSupply(() -> x));
        return ret;
    }

    private void showTogglesEnabled(boolean enabled) {
        if (null != displayJFrame) {
            displayJFrame.showTogglesEnabled(enabled);
        }
    }

    // stupid hard-coded hack to match demo
    private static final String MOTOMAN_NAME = "motoman";
    private static final String SIM_MOTOMAN_NAME = "sim_motoman";
    private static final Set<String> robotsThatCanBeDisabled = new HashSet<>(
            Arrays.asList(MOTOMAN_NAME, SIM_MOTOMAN_NAME));

    private final AtomicInteger togglesCount = new AtomicInteger();

    private XFuture<Boolean> toggleRobotEnabled(String robotName, boolean wasEnabled) {
        if (closing) {
            return XFuture.completedFuture(false);
        }
        final String futureName = "toggleRobot_wasEnabled=" + wasEnabled + ",allowed=" + isTogglesAllowed()
                + ",togglesCount=" + togglesCount.get() + ",ignoredToggles=" + ignoredToggles.get();
        logEvent(futureName);
        XFutureVoid futureToComplete = new XFutureVoid(futureName);
        setLastFunctionReturned(futureToComplete);
        return XFuture.supplyAsync("toggleRobotEnabled(" + robotName + "," + wasEnabled + ")", () -> {
            if (closing) {
                return false;
            }
            final boolean enableMatchStealing = wasEnabled != stealingRobots;
            if (!enableMatchStealing) {
                logEvent("!enableMatchStealing: wasEnabled=" + wasEnabled + ",stealingRobots=" + stealingRobots);
            }
            if (isTogglesAllowed() && enableMatchStealing) {
                int tc = togglesCount.incrementAndGet();
                logEvent("togglesCount = " + tc);
                setRobotEnabled(robotName, !wasEnabled, futureToComplete);
                return true;
            }
            int itc = ignoredToggles.incrementAndGet();
            int tc = togglesCount.get();
            logEvent("ignored togglesCount = " + itc + ", togglesCount=" + tc);
            futureToComplete.complete();
            return false;
        }, supervisorExecutorService);
    }

    private XFuture<Boolean> toggleRobotEnabled() {
        if (closing || !checkMaxCycles()) {
            return XFuture.completedFuture(false);
        }
        final boolean pauseSelectedLocal = isPauseSelected();
        final boolean togglesAllowedLocal = togglesAllowed;
        logEvent("toggleRobotEnabled : pause=" + pauseSelectedLocal + ",allowed=" + togglesAllowedLocal
                + ",togglesCount=" + togglesCount.get() + ",igonredToggles=" + ignoredToggles.get());
        if (pauseSelectedLocal) {
            ignoredToggles.incrementAndGet();
            return waitResume().thenApply(x -> false);
        }
        if (!togglesAllowedLocal) {
            ignoredToggles.incrementAndGet();
            return XFuture.completedFutureWithName("!togglesAllowd", false);
        }
        List<String> robotsChecked = new ArrayList<>();
        for (AprsSystem sys : aprsSystems) {
            String robotName = sys.getRobotName();
            if (null != robotName) {
                final String robotNameLowerCase = robotName.toLowerCase();
                robotsChecked.add(robotNameLowerCase);
                if (robotsThatCanBeDisabled.contains(robotNameLowerCase)) {
                    boolean wasEnabled = robotTaskMap.get(robotName) != null;
                    return toggleRobotEnabled(robotName, wasEnabled);
                }
            }
            String origRobotName = sys.getOrigRobotName();
            if (null != origRobotName) {
                final String robotNameLowerCase = origRobotName.toLowerCase();
                robotsChecked.add(robotNameLowerCase);
                if (robotsThatCanBeDisabled.contains(robotNameLowerCase)) {
                    boolean wasEnabled = robotTaskMap.get(origRobotName) != null;
                    return toggleRobotEnabled(origRobotName, wasEnabled);
                }
            }
        }
        ignoredToggles.incrementAndGet();
        throw new IllegalStateException("no robot that can be disabled found. robotsChecked=" + robotsChecked
                + ", robotsThatCanBeDisabled=" + robotsThatCanBeDisabled + ",robotTaskMap=" + robotTaskMap);
    }

    private final AtomicInteger randomTestCount = new AtomicInteger();

    void clearRandomTestCount() {
        randomTestCount.set(0);
    }

    private XFutureVoid updateRandomTestCount() {
        int count = randomTestCount.incrementAndGet();
        if (null != displayJFrame) {
            final AprsSupervisorDisplayJFrame jfrm = displayJFrame;
            return jfrm.updateRandomTestCount(count);
        }
        return XFutureVoid.completedFutureWithName("updateRandomTest.displayJFrame==null");
    }

    private final AtomicInteger logEventErrCount = new AtomicInteger();

    // private static String splitLongMessage(String inString, int maxlen) {
//		String lines[] = inString.split("[\r\n]+");
//		StringBuilder sb = new StringBuilder();
//		for (int i = 0; i < lines.length; i++) {
//			String line = lines[i];
//			if (line.length() > maxlen) {
//				StringBuilder sbi = new StringBuilder();
//				for (int j = 0; j < line.length() / maxlen; j++) {
//					String line1 = line.substring(j * maxlen, Math.min(line.length(), (j + 1) * maxlen));
//					sbi.append(line1);
//					sbi.append("\r\n");
//				}
//				line = sbi.toString();
//			}
//			sb.append(line);
//		}
//		return sb.toString();
//	}
    private boolean pauseOnError = true;

    /**
     * Get the value of pauseOnError
     *
     * @return the value of pauseOnError
     */
    public boolean isPauseOnError() {
        return pauseOnError;
    }

    /**
     * Set the value of pauseOnError
     *
     * @param pauseOnError new value of pauseOnError
     */
    public void setPauseOnError(boolean pauseOnError) {
        this.pauseOnError = pauseOnError;
    }

    private XFutureVoid logEventErr(@Nullable String err) {
        if (closing) {
            return XFutureVoid.completedFuture();
        }
        int count = logEventErrCount.incrementAndGet();
        if (null != err) {
            System.err.println(err);
            XFutureVoid logEventFuture = logEvent("ERROR(" + count + "): " + err);
            if (null != displayJFrame) {
                runOnDispatchThread(() -> {
                    if (count < 5 && null != displayJFrame) {
                        if (displayJFrame.isShowSplashMessagesSelected()) {
                            displayJFrame.showErrorSplash(err);
                        } else {
                            MultiLineStringJPanel.showText(err);
                        }
                    }
                    setIconImageOnDisplay(IconImages.ERROR_IMAGE);
                });
            } else if (count < 5) {
                System.err.println("displayJFrame = " + displayJFrame);
            }
            if (pauseOnError) {
                System.err.println("pauseOnError = " + pauseOnError);
                pause();
            }
            return logEventFuture;
        }
        return XFutureVoid.completedFuture();
    }

    private volatile @Nullable Throwable lastLoggedException = null;
    private volatile StackTraceElement lastLoggedExceptionTrace@Nullable []  = null;

    private void logException(@Nullable Throwable throwable) {
        lastLoggedException = throwable;
        if (null == throwable) {
            return;
        }
        int count = logEventErrCount.incrementAndGet();
        System.out.println("");
        System.err.println("");
        System.out.flush();
        System.err.flush();
        Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "",throwable);
        System.out.println("");
        System.err.println("");
        System.out.flush();
        System.err.flush();
        Thread.dumpStack();
        StackTraceElement trace[] = Thread.currentThread().getStackTrace();
        String err = throwable.toString();
        if (null != err) {
            System.err.println(err);
            logEvent("ERROR(" + count + "): " + err);
            if (null != displayJFrame) {
                runOnDispatchThread(() -> {
                    if (count < 5 && null != displayJFrame) {
                        if (displayJFrame.isShowSplashMessagesSelected()) {
                            displayJFrame.showErrorSplash(err);
                        } else {
                            MultiLineStringJPanel.showException(throwable, trace);
                        }
                    }
                    setIconImageOnDisplay(IconImages.ERROR_IMAGE);
                    if (pauseOnError) {
                        System.err.println("pauseOnError = " + pauseOnError);
                        pause();
                    }
                });
            } else {
                if (count < 5) {
                    System.err.println("displayJFrame = " + displayJFrame);
                }
                if (pauseOnError) {
                    System.err.println("pauseOnError = " + pauseOnError);
                    pause();
                }
            }
        }
    }

    private boolean allSystemsOk() {
        for (AprsSystem sys : aprsSystems) {
            CRCLStatusType status = sys.getCurrentStatus();
            if (status != null) {
                final CommandStatusType commandStatus = status.getCommandStatus();
                if (commandStatus != null && commandStatus.getCommandState() == CommandStateEnumType.CRCL_ERROR) {
                    logEventErr("allSystemsOk failing: bad status for sys=" + sys);
                    return false;
                }
            }
            String titleErrorString = sys.getTitleErrorString();
            if (titleErrorString != null && titleErrorString.length() > 0) {
                logEventErr("allSystemsOk failing: bad titleErrorString (" + titleErrorString + ") for sys=" + sys);
                return false;
            }
            String clientErrorString = sys.getCrclClientErrorString();
            if (clientErrorString != null && clientErrorString.length() > 0) {
                logEventErr(
                        "allSystemsOk failing: bad rclClientErrorString (" + clientErrorString + ") for sys=" + sys);
                return false;
            }
        }
        return true;
    }

    private volatile @Nullable XFutureVoid pauseTestFuture = null;

    @Nullable
    XFutureVoid getPauseTestFuture() {
        return pauseTestFuture;
    }

    void setPauseTestFuture(@Nullable XFutureVoid pauseTestFuture) {
        this.pauseTestFuture = pauseTestFuture;
    }

    XFutureVoid continuePauseTest() {
        if (!allSystemsOk()) {
            logEventErr("allSystemsOk returned false forcing quitRandomTest");
            return quitRandomTest("allSystemsOk returned false forcing quitRandomTest");
        }
        if (!isContinuousDemoSelected() && !isContinuousDemoRevFirstSelected()) {
            logEventErr("isContinuousDemoSelected() returned false forcing quitRandomTest");
            return quitRandomTest("isContinuousDemoSelected() returned false forcing quitRandomTest");
        }
        if (!isRandomTestSelected()) {
            logEventErr("isRandomTestSelected().isSelected() returned false forcing quitRandomTest");
            return quitRandomTest("isRandomTestSelected().isSelected() returned false forcing quitRandomTest");
        }
        if (null == ContinuousDemoFuture || ContinuousDemoFuture.isCancelled() || ContinuousDemoFuture.isDone()
                || ContinuousDemoFuture.isCompletedExceptionally()) {
            println("ContinuousDemoCycle = " + ContinuousDemoCycle + " forcing quitRandomTest");
            printStatus(ContinuousDemoFuture, System.out);
            return quitRandomTest("ContinuousDemoCycle = " + ContinuousDemoCycle + " forcing quitRandomTest");
        }
        XFutureVoid ret = startRandomDelay("pauseTest", 30000, 20000)
                .thenComposeAsyncToVoid("pauseTest.pause" + pauseCount.get(), () -> runOnDispatchThread(this::pause), supervisorExecutorService)
                .thenComposeAsyncToVoid(x -> startRandomDelay("pauseTest", 1000, 1000), supervisorExecutorService)
                .thenComposeAsyncToVoid("pauseTest.resume" + pauseCount.get(), () -> runOnDispatchThread(this::resume), supervisorExecutorService);
        pauseTestFuture = ret;
        resetMainPauseTestFuture();
        ret.thenCompose("pauseTest.recurse" + pauseCount.get(), x -> continuePauseTest());
        pauseTestFuture = ret;
        return ret;
    }

    private int enableTestMinRandomDelayMillis = 5000;

    /**
     * Get the value of enableTestMinRandomDelayMillis
     *
     * @return the value of enableTestMinRandomDelayMillis
     */
    public int getEnableTestMinRandomDelayMillis() {
        return enableTestMinRandomDelayMillis;
    }

    /**
     * Set the value of enableTestMinRandomDelayMillis
     *
     * @param enableTestMinRandomDelayMillis new value of
     * enableTestMinRandomDelayMillis
     */
    public void setEnableTestMinRandomDelayMillis(int enableTestMinRandomDelayMillis) {
        this.enableTestMinRandomDelayMillis = enableTestMinRandomDelayMillis;
    }

    private int enableTestRandomDelayMillis = 20000;

    /**
     * Get the value of enableTestRandomDelayMillis
     *
     * @return the value of enableTestRandomDelayMillis
     */
    public int getEnableTestRandomDelayMillis() {
        return enableTestRandomDelayMillis;
    }

    /**
     * Set the value of enableTestRandomDelayMillis
     *
     * @param enableTestRandomDelayMillis new value of
     * enableTestRandomDelayMillis
     */
    public void setEnableTestRandomDelayMillis(int enableTestRandomDelayMillis) {
        this.enableTestRandomDelayMillis = enableTestRandomDelayMillis;
    }

    XFutureVoid continueRandomTest(int startingAbortCount) {
        try {
            if (closing || !checkMaxCycles()) {
                return XFutureVoid.completedFuture();
            }
            int cdc = ContinuousDemoCycle.get();
            logEvent("continueRandomTest " + cdc);
            if (!checkMaxCycles()) {
                return checkLastReturnedFuture(null, "continueRandomTest.!checkMaxCycles()").thenRun(() -> {
                    logEvent("continueRandomTest : checkMaxCycles");
                });
            }

            int currentAbortCount = getAbortCount();
            if (startingAbortCount != currentAbortCount) {
                String msg = "continueRandomTest quiting : startingAbortCount=" + startingAbortCount
                        + ", currentAbortCount=" + currentAbortCount;
                logEvent(msg);
                XFutureVoid ret2 = XFutureVoid.completedFutureWithName(msg);
                randomTestFuture = ret2;
                return ret2;
            }
            if (!allSystemsOk()) {
                logEventErr("allSystemsOk returned false forcing quitRandomTest");
                return quitRandomTest("allSystemsOk returned false forcing quitRandomTest");
            }
            if (!isContinuousDemoSelected() && !isContinuousDemoRevFirstSelected() && !isIndContinuousDemoSelected()
                    && !isIndRandomToggleTestSelected()) {
                logEventErr("isContinuousDemoSelected() returned false forcing quitRandomTest");
                return quitRandomTest("isContinuousDemoSelected() returned false forcing quitRandomTest");
            }
            if (!isRandomTestSelected() && !isIndRandomToggleTestSelected()) {
                logEventErr("isRandomTestSelected().isSelected() returned false forcing quitRandomTest");
                return quitRandomTest("isRandomTestSelected().isSelected() returned false forcing quitRandomTest");
            }
            XFuture<?> currentContinuousDemoFuture = this.ContinuousDemoFuture;
            if (null == currentContinuousDemoFuture || currentContinuousDemoFuture.isCancelled()
                    || currentContinuousDemoFuture.isDone() || currentContinuousDemoFuture.isCompletedExceptionally()) {
                println("ContinuousDemoCycle = " + ContinuousDemoCycle + " forcing quitRandomTest");
                printStatus(currentContinuousDemoFuture, System.out);
                return quitRandomTest("ContinuousDemoCycle = " + ContinuousDemoCycle + " forcing quitRandomTest");
            }
            XFutureVoid ret = startRandomDelay("enableTest", enableTestRandomDelayMillis,
                    enableTestMinRandomDelayMillis).thenCompose("checkForWaitResume1", x -> this.waitResume())
                    .thenCompose("waitTogglesAllowed", x -> this.waitTogglesAllowed())
                    .thenCompose("toggleRobotEnabled", x -> this.toggleRobotEnabled())
                    .thenComposeAsyncToVoid("updateRandomTestCount" + randomTestCount.get(), x -> {
                        if (x) {
                            return this.updateRandomTestCount();
                        } else {
                            return XFuture.completedFuture(null);
                        }
                    }, supervisorExecutorService)
                    .thenComposeAsyncToVoid("continueRandomTest.recurse" + randomTestCount.get(),
                            () -> continueRandomTest(startingAbortCount), randomDelayExecutorService);

            XFuture<?> handledRandomTestFuture = ret.exceptionally((Throwable throwable) -> {
                return handleRandomTestExcpeption(throwable);
            });
            randomTestFuture = ret;
            resetMainRandomTestFuture();
            return ret;
        } catch (Exception e) {
            if (!closing) {
                Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, e);
                pause();
            }
            throw new RuntimeException(e);
        }
    }

    private Void handleRandomTestExcpeption(Throwable throwable) throws RuntimeException {
        if (!closing) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, throwable);
            pause();
            if (null != throwable) {
                throw new RuntimeException(throwable);
            }
        }
        return ((Void) null);
    }

    private XFutureVoid quitRandomTest(String cause) {
        logEvent("quitRandomTest : " + cause);
        XFutureVoid xf = new XFutureVoid("quitRandomTest : " + cause);
        xf.cancel(false);
        println("continueRandomTest quit");
        setContinuousDemoSelected(false);
        setContinuousDemoRevFirstSelected(false);
        setRandomTestSelected(false);
        immediateAbortAll("quitRandomTest : " + cause);
        return xf;
    }

    // /**
//     * Set the reverseFlag for all systems. When the reverseFlag is set systems
//     * empty kit trays and put parts back in parts trays. This may occur in
//     * another thread.
//     *
//     * @param reverseFlag false to move parts from parts trays to kitTrays or
//     * true to move parts from kitTrays to partsTrays
//     *
//     * @return a future which can be used to determine when the all reverse
//     * flags and related actions are complete.
//     */
//    public XFutureVoid startSetAllReverseFlag(boolean reverseFlag) {
//        logEvent("setAllReverseFlag(" + reverseFlag + ")");
//        @SuppressWarnings("rawtypes")
//        XFuture fa[] = new XFuture[aprsSystems.size()];
//        for (int i = 0; i < aprsSystems.size(); i++) {
//            AprsSystem sys = aprsSystems.get(i);
//            if (sys.isReverseFlag() != reverseFlag) {
//                logEvent("setting reverseFlag for " + sys + " to " + reverseFlag);
//                fa[i] = sys.startSetReverseFlag(reverseFlag);
//            } else {
//                fa[i] = XFuture.completedFuture(null);
//            }
//        }
//        return XFuture.allOf(fa);
//    }
    private void disconnectAllNoLog() {
        for (AprsSystem aprsSystem : aprsSystems) {
            aprsSystem.setConnected(false);
        }
        runOnDispatchThread(() -> {
            if (logEventErrCount.get() == 0) {
                setIconImageOnDisplay(IconImages.DISCONNECTED_IMAGE);
            }
        });
    }

    /**
     * Disconnect all systems.
     */
    private void disconnectAll() {
        logEvent("disconnectAll");
        for (AprsSystem sys : aprsSystems) {
            if (sys.isConnected()) {
                logEvent("Disconnecting " + sys);
                sys.setConnected(false);
            }
        }
    }

    private final AtomicInteger scdrfCount = new AtomicInteger();

    /**
     * Start a continuous demo where kit trays will first be emptied and then
     * repeatedly filled and emptied indefinitely.
     *
     * @return future that can be used to determine if it fails or is cancelled
     */
    private XFutureVoid startPrivateContinuousDemoRevFirst() {
        int c = scdrfCount.incrementAndGet();
        int startingAbortCount = getAbortCount();
        logEvent("Start Continuous Demo (Reverse First) : " + c);
        connectAll();
        final XFuture<?> lfr = this.getLastFutureReturned();
        final int cdcCount = ContinuousDemoCycle.get();
        if (!checkMaxCycles()) {
            logEvent("continue Continuous Demo quitting because checkMaxCycles() returned false: ContinuousDemoCycle="
                    + cdcCount);
            return checkLastReturnedFuture(null, "continueRandomTest.!checkMaxCycles()").thenRun(() -> {
                logEvent("startPrivateContinuousDemoRevFirst : checkMaxCycles");
            });
        }
        if (!isContinuousDemoSelected() && !isContinuousDemoRevFirstSelected()) {
            String msg = "Continue Continuous Demo : " + cdcCount + " quiting because checkbox not set";
            logEvent(msg);
            return XFutureVoid.completedFutureWithName(msg);
        }
        String part1BlockerName = "part1StartPrivateContinuousDemoRevFirst" + cdcCount;
        String part2BlockerName = "part2StartPrivateContinuousDemoRevFirst" + cdcCount;
        String part3BlockerName = "part3StartPrivateContinuousDemoRevFirst" + cdcCount;
        XFuture<LockInfo> disallowTogglesFuture = disallowToggles(part1BlockerName);
        StackTraceElement trace[] = Thread.currentThread().getStackTrace();
        checkAllRunningOrDoingActions(-1, "startPrivateContinuousDemoRevFirst");
        if (this.stealingRobots) {
            System.err.println("trace = " + Utils.traceToString(trace));
            logEventErr("stealingRobots flag set when starting continueContinuousDemo");
        }
        XFutureVoid ret = disallowTogglesFuture.thenCompose(x -> startCheckAndEnableAllRobots(startingAbortCount))
                .thenComposeAsync("startContinuousDemoRevFirst.allowToggles" + part1BlockerName,
                        x -> allowToggles(part1BlockerName), supervisorExecutorService)
                .thenComposeAsync("startContinuousDemoRevFirst.checkLastReturnedFuture1",
                        x -> checkLastReturnedFuture(lfr, part2BlockerName), supervisorExecutorService)
                .thenComposeAsync("startContinuousDemoRevFirst.startReverseActions",
                        x -> startReverseActions(part2BlockerName, startingAbortCount), supervisorExecutorService)
                .thenComposeAsync("startContinuousDemoRevFirst.checkLastReturnedFuture2",
                        x -> checkLastReturnedFuture(lfr, part3BlockerName), supervisorExecutorService)
                .thenComposeAsyncToVoid("continueContinuousDemo.incrementContinuousDemoCycle",
                        x -> incrementContinuousDemoCycle("startPrivateContinuousDemoRevFirst", startingAbortCount),
                        supervisorExecutorService)
                .thenComposeAsync("startContinuousDemoRevFirst.enableAndCheckAllRobots",
                        x -> startCheckAndEnableAllRobots(startingAbortCount), supervisorExecutorService)
                .thenComposeAsyncToVoid("startContinuousDemoRevFirst",
                        ok -> checkOkElseToVoid(ok, () -> continueContinuousDemo(part3BlockerName, startingAbortCount),
                                this::showCheckEnabledErrorSplash),
                        supervisorExecutorService)
                .alwaysComposeAsyncToVoid(
                        () -> allowTogglesNoCheck(part1BlockerName, part2BlockerName, part3BlockerName),
                        supervisorExecutorService);
        ContinuousDemoFuture = ret;
        return ret;
    }

    /**
     * Start a continuous demo where kit trays will first be filled and then
     * emptied repeatedly. Systems will wait for all systems to be filled before
     * any begin emptying and vice versa.
     *
     * @return future that can be used to determine if it fails or is cancelled
     */
    XFutureVoid startContinuousScanAndRun() {
        int startingAbortCount = getAbortCount();
        logEvent("Start Continuous scan and run");
        connectAll();
        for (AprsSystem aprsSys : aprsSystems) {
            aprsSys.setLastCreateActionListFromVisionKitToCheckStrings(Collections.emptyList());
        }
        ContinuousDemoFuture = XFuture
                .supplyAsync("startCheckAndEnableAllRobots(" + startingAbortCount + ")",
                        () -> startCheckAndEnableAllRobots(startingAbortCount), supervisorExecutorService)
                .thenCompose(x -> x).thenComposeAsyncToVoid("startContinuousScanAndRun",
                ok -> checkOkElse(ok, () -> continueContinuousScanAndRun(null, startingAbortCount),
                        this::showCheckEnabledErrorSplash),
                supervisorExecutorService);
        return ContinuousDemoFuture;
    }

    /**
     * Start a continuous demo where kit trays will first be filled and then
     * emptied repeatedly. Systems will wait for all systems to be filled before
     * any begin emptying and vice versa.
     *
     * @return future that can be used to determine if it fails or is cancelled
     */
    public XFutureVoid startContinuousDemo() {
        int cdcCount = ContinuousDemoCycle.get();
        int startingAbortCount = getAbortCount();
        logEvent("Start Continuous demo " + cdcCount);
        connectAll();
        String blockerName = "startContinuousDemo" + cdcCount;
        AprsSystem sysArray[] = systems().toArray(new AprsSystem[0]);
        XFutureVoid ret = disallowToggles(blockerName, sysArray)
                .thenCompose(x -> startCheckAndEnableAllRobots(startingAbortCount))
                .thenComposeAsyncToVoid("startContinuousDemo" + cdcCount,
                        ok -> checkOkElse(ok, () -> continueContinuousDemo(blockerName, startingAbortCount),
                                this::showCheckEnabledErrorSplash),
                        supervisorExecutorService)
                .peekNoCancelException(this::handleXFutureException)
                .thenComposeAsyncToVoid(() -> this.waitSingleStep("startContinuousDemo"), supervisorExecutorService);
        ContinuousDemoFuture = ret;
        return ret;
    }

    /**
     * Start a continuous demo where kit trays will first be filled and then
     * emptied repeatedly. Systems will not wait for all systems to be filled
     * before any begin emptying and vice versa, so one might be emptying while
     * another is filling.
     *
     * @return future that can be used to determine if it fails or is canceled
     */
    XFutureVoid startIndependentContinuousDemo() {
        int startingAbortCount = getAbortCount();
        logEvent("Start Continuous demo");
        connectAll();
        XFutureVoid ret = startCheckAndEnableAllRobots(startingAbortCount)
                .thenComposeAsyncToVoid("startIndContinuousDemo",
                        ok -> checkOkElseToVoid(ok, this::startAllIndContinuousDemo, this::showCheckEnabledErrorSplash),
                        supervisorExecutorService);
        ContinuousDemoFuture = ret;
        if (null != randomTestFuture && isIndRandomToggleTestSelected()) {
            resetMainRandomTestFuture();
        }
        return ret;
    }

    /**
     * Start a continuous demo where kit trays will first be filled and then
     * emptied repeatedly. Systems will not wait for all systems to be filled
     * before any begin emptying and vice versa, so one might be emptying while
     * another is filling. In addition the motoman will be randomly enabled and
     * disabled for testing purposes.
     *
     * @return future that can be used to determine if it fails or is canceled
     */
    XFutureVoid startRandomEnableToggleIndependentContinuousDemo() {
        int startingAbortCount = getAbortCount();
        logEvent("Start Independent Random  Enable Toggle Test");
        connectAll();
        return startCheckAndEnableAllRobots(startingAbortCount)
                .thenComposeAsyncToVoid(
                        "startRandomEnableToggleIndependentContinuousDemo.checkOk", ok -> checkOkElseToVoid(ok,
                                () -> startIndRandomTestStep2(startingAbortCount), this::showCheckEnabledErrorSplash),
                        supervisorExecutorService);
    }

    private final AtomicInteger ContinuousDemoCycle = new AtomicInteger(0);

    public int getContiousDemoCycleCount() {
        return ContinuousDemoCycle.get();
    }

    ExecutorService getSupervisorExecutorService() {
        return supervisorExecutorService;
    }

    public ExecutorService getRandomDelayExecutorService() {
        return randomDelayExecutorService;
    }

    private XFutureVoid incrementContinuousDemoCycle(String comment, int startingAbortCount) {
        final int c = ContinuousDemoCycle.incrementAndGet();
        int ecc = enableChangeCount.get();
        String info = "incCdc : c=" + c + ",ecc=" + ecc + " " + comment;
        try {
            takeAllSnapshots(info);
        } catch (IOException ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
        }
        setTitleMessage(Integer.toString(c));
        int currentAbortCount = getAbortCount();
        if (startingAbortCount != currentAbortCount) {
            String msg = "incrementContinuousDemoCycle quiting : startingAbortCount=" + startingAbortCount
                    + ", currentAbortCount=" + currentAbortCount;
            logEvent(msg);
//            System.err.println("incrementAndGetAbortCountTrace = " + Utils.traceToString(incrementAndGetAbortCountTrace) + ", msg=" + msg);
            XFutureVoid ret2 = XFutureVoid.completedFutureWithName(msg);
            return ret2;
        }
        if (closing) {
            return XFutureVoid.completedFutureWithName("incrementContinuousDemoCycle: closing");
        }
        if (null != displayJFrame) {
            if (checkMaxCycles()) {
                return displayJFrame.setContinuousDemoCycle(c);
            } else {
                return displayJFrame.setContinuousDemoCycle(c)
                        .thenComposeAsync(() -> checkLastReturnedFuture(null, null), supervisorExecutorService)
                        .thenRun(() -> {
                            logEvent("incrementContinuousDemoCycle : checkMaxCycles()");
                        });
            }
        } else if (checkMaxCycles()) {
            return XFutureVoid.completedFutureWithName("incrementContinuousDemoCycle" + c);
        } else {
            return checkLastReturnedFuture(null, null).thenRun(() -> {
                logEvent("incrementContinuousDemoCycle : checkMaxCycles()");
            });
        }
    }

    private final AtomicInteger takeAllSnapshotsCount = new AtomicInteger();

    private volatile boolean takeAllSnapshotsRevFlag = false;

    public void takeAllSnapshots(String info) throws IOException {
        boolean prevTakeAllSnapshotsRevFlag = takeAllSnapshotsRevFlag;
        boolean newRevFlag = lastStartAllActionsReverseFlag != prevTakeAllSnapshotsRevFlag;
        if (newRevFlag) {
            info = "NEWREV_" + lastStartAllActionsReverseFlag + "_";
            takeAllSnapshotsRevFlag = lastStartAllActionsReverseFlag;
        }
        logEvent(info);
        File supervisorDir = Utils.file(Utils.getlogFileDir(), "supervisor");
        supervisorDir.mkdirs();
        int count = takeAllSnapshotsCount.incrementAndGet();
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystem aprsSys = aprsSystems.get(i);
            File subDir = Utils.file(supervisorDir, aprsSys.getTaskName());
            subDir.mkdirs();
            if (aprsSys.isObjectViewSimulated()) {
                aprsSys.takeSimViewSnapshot(Utils
                        .createTempFile(count + "_" + i + "_sim_" + info + "_" + aprsSys.toString(), ".PNG", subDir),
                        aprsSys.getSimItemsData());
            } else {
                aprsSys.takeSimViewSnapshot(
                        Utils.createTempFile(count + "_" + i + info + "_" + aprsSys.toString(), ".PNG", subDir),
                        aprsSys.getLastVisItemsData());
            }
        }
        if (newRevFlag) {
            File newRevDir = Utils.file(supervisorDir, "newrev");
            newRevDir.mkdirs();
            for (int i = 0; i < aprsSystems.size(); i++) {
                AprsSystem aprsSys = aprsSystems.get(i);
                File subDir = Utils.file(newRevDir, aprsSys.getTaskName());
                subDir.mkdirs();
                if (aprsSys.isObjectViewSimulated()) {
                    aprsSys.takeSimViewSnapshot(
                            Utils.createTempFile(count + "_" + i + "_sim_" + info + "_" + aprsSys.toString(),
                                    ".PNG",
                                    subDir),
                            aprsSys.getSimItemsData());
                } else {
                    aprsSys.takeSimViewSnapshot(
                            Utils.createTempFile(count + "_" + i + info + "_" + aprsSys.toString(),
                                    ".PNG",
                                    subDir),
                            aprsSys.getLastVisItemsData());
                }
            }
        }
    }

    final static private AtomicInteger runProgramThreadCount = new AtomicInteger();

    private final int myThreadId = runProgramThreadCount.incrementAndGet();

    private volatile @Nullable Thread supervisorThread = null;

    private final ExecutorService defaultSupervisorExecutorService = Executors
            .newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    if (null != supervisorThread) {
                        throw new IllegalStateException("supervisorThread=" + supervisorThread);
                    }
                    Thread thread = new Thread(r, "AprsSupervisor" + myThreadId);
                    thread.setDaemon(true);
                    supervisorThread = thread;
                    return thread;
                }
            });

    private final ExecutorService supervisorExecutorService = defaultSupervisorExecutorService;

    private volatile @Nullable Thread randomDelayThread = null;

    private final ExecutorService defaultRandomDelayExecutorService = Executors
            .newSingleThreadExecutor(new ThreadFactory() {

                private int t;

                @Override
                public Thread newThread(Runnable r) {
                    t++;
                    Thread thread = new Thread(r, "AprsSupervisor_random_delay_" + myThreadId + "_" + t);
                    randomDelayThread = thread;
                    thread.setDaemon(true);
                    return thread;
                }
            });

    private final ExecutorService randomDelayExecutorService = defaultRandomDelayExecutorService;

    int getMax_cycles() {
        return max_cycles;
    }

    public void setMax_cycles(int max_cycles) {
        this.max_cycles = max_cycles;
    }

    private final AtomicInteger continueContinuousScanAndRunCount = new AtomicInteger();

    private XFutureVoid continueContinuousScanAndRun(@Nullable String prevBlockerName, int startingAbortCount) {
        try {
            final int cdcCount = ContinuousDemoCycle.get();
            if (!checkMaxCycles()) {
                logEvent(
                        "Continue Continuous Scan and Run quitting because checkMaxCycles() returned false: ContinuousDemoCycle="
                        + cdcCount);
                return checkLastReturnedFuture(null, "continueRandomTest.!checkMaxCycles()").thenRun(() -> {
                    logEvent("continueContinuousScanAndRun : checkMaxCycles");
                });
            }
            logEvent("Continue Continuous Scan and Run : " + cdcCount);
            int currentAbortCount = getAbortCount();
            if (startingAbortCount != currentAbortCount) {
                String msg = "continueContinuousScanAndRun quiting : startingAbortCount=" + startingAbortCount
                        + ", currentAbortCount=" + currentAbortCount;
                logEvent(msg);
                XFutureVoid ret2 = XFutureVoid.completedFutureWithName(msg);
                ContinuousDemoFuture = ret2;
                return ret2;
            }
            final int ccsarCount = continueContinuousScanAndRunCount.incrementAndGet();
            String part1BlockerName = "part1ContinueContinuousScanAndRun" + ccsarCount;
            String part2BlockerName = "part2ContinueContinuousScanAndRun" + ccsarCount;
            String part3BlockerName = "part3ContinueContinuousScanAndRun" + ccsarCount;
            XFuture<LockInfo> disallowTogglesFuture = disallowToggles(part1BlockerName);
            return disallowTogglesFuture.thenComposeAsync(x -> continuousDemoSetup(cdcCount), supervisorExecutorService)
                    .thenComposeAsyncToVoid("continueContinuousScanAndRun.part2",
                            x2 -> {
                                return continueContinuousScanAndRunInternal(startingAbortCount, part1BlockerName, prevBlockerName, part2BlockerName, part3BlockerName, cdcCount);
                            }, supervisorExecutorService);
        } catch (Exception exception) {
            logException(exception);
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", exception);
            throw asRuntimeException(exception);
        }
    }

    private XFutureVoid continueContinuousScanAndRunInternal(int startingAbortCount, String part1BlockerName, String prevBlockerName, String part2BlockerName, String part3BlockerName, final int cdcCount) {
        final XFuture<?> lfr = this.getLastFutureReturned();
        XFutureVoid ret = startCheckAndEnableAllRobots(startingAbortCount)
                .alwaysComposeAsync(() -> allowTogglesNoCheck(part1BlockerName, prevBlockerName),
                        supervisorExecutorService)
                .thenComposeAsync("continueContinuousScanAndRun.scanAllInternal",
                        x -> scanTillNewInternal(), supervisorExecutorService)
                .thenComposeAsync("continueContinuousScanAndRun.checkLastReturnedFuture2",
                        x -> checkLastReturnedFuture(lfr, part2BlockerName), supervisorExecutorService)
                .thenComposeAsync("continueContinuousScanAndRun.startAllActions1",
                        x -> startAllActions("continueContinuousScanAndRun", false, part2BlockerName,
                                startingAbortCount),
                        supervisorExecutorService)
                .thenComposeAsync("continueContinuousScanAndRun.checkLastReturnedFuture1",
                        x -> checkLastReturnedFuture(lfr, part3BlockerName), supervisorExecutorService)
                .thenComposeAsyncToVoid("continueContinuousScanAndRun.incrementContinuousDemoCycle",
                        x -> incrementContinuousDemoCycle("continueContinuousScanAndRun",
                                startingAbortCount),
                        supervisorExecutorService)
                .thenComposeAsync("continueContinuousScanAndRun.enableAndCheckAllRobots",
                        x -> startCheckAndEnableAllRobots(startingAbortCount),
                        supervisorExecutorService)
                .thenCompose("allowToggles" + part3BlockerName,
                        x -> allowToggles(part3BlockerName).thenApply(v -> x))
                .thenComposeAsyncToVoid("continueContinuousScanAndRun.recurse" + cdcCount,
                        ok -> checkOkElseToVoid(ok,
                                () -> continueContinuousScanAndRun(part3BlockerName,
                                        startingAbortCount),
                                this::showCheckEnabledErrorSplash),
                        supervisorExecutorService)
                .peekNoCancelException(this::handleXFutureException).alwaysComposeAsyncToVoid(
                () -> allowTogglesNoCheck(part1BlockerName, part2BlockerName, part3BlockerName),
                supervisorExecutorService);
        ContinuousDemoFuture = ret;
        if (null != randomTestFuture) {
            if (isRandomTestSelected()) {
                resetMainRandomTestFuture();
            } else if (isRandomTestSelected()) {
                resetMainPauseTestFuture();
            }
        }
        return ret;
    }

    private XFutureVoid startContinuousDemoReversActions(@Nullable String prevBlockerName, int startingAbortCount) {
        if (!isContinuousDemoSelected() && !isContinuousDemoRevFirstSelected()) {
            String msg = "startContinuousDemoReversActions : " + ContinuousDemoCycle.get()
                    + " quiting because checkbox not set";
            logEvent(msg);
            return XFutureVoid.completedFutureWithName(msg);
        }
        return startReverseActions(prevBlockerName, startingAbortCount);
    }

    private volatile StackTraceElement continueContinuousDemoTrace@Nullable []  = null;

    private volatile @Nullable XFuture<Boolean> lastContinueContinuousDemoNoRecurseFuture;

    private XFutureVoid continueContinuousDemo(String prevBlockerName, int startingAbortCount) {
        int currentAbortCount = getAbortCount();
        if (startingAbortCount != currentAbortCount) {
            String msg = "continueContinuousDemo quiting : startingAbortCount=" + startingAbortCount
                    + ", currentAbortCount=" + currentAbortCount;
            logEvent(msg);
            allowTogglesNoCheck(prevBlockerName);
            XFutureVoid ret2 = XFutureVoid.completedFutureWithName(msg);
            ContinuousDemoFuture = ret2;
            return ret2
                    .peekNoCancelException(this::handleXFutureException)
                    .thenComposeAsyncToVoid(
                            () -> this.waitSingleStep("continueContinuousDemo(" + prevBlockerName + "," + startingAbortCount + ")"),
                            supervisorExecutorService);
        }

        final int cdcCount = ContinuousDemoCycle.get();
        if (!checkMaxCycles()) {
            logEvent("continue Continuous Demo quitting because checkMaxCycles() returned false: ContinuousDemoCycle="
                    + cdcCount);
            return checkLastReturnedFuture(null, "continueRandomTest.!checkMaxCycles()").thenRun(() -> {
                logEvent("continueContinuousDemo : checkMaxCycles");
            });
        }
        logEvent("Start Continue Continuous Demo : cdcCount=" + cdcCount + ",toggleBlockerMap.keySet()="
                + toggleBlockerMap.keySet());
        if (!isContinuousDemoSelected() && !isContinuousDemoRevFirstSelected()) {
            String msg = "Continue Continuous Demo : " + cdcCount + " quiting because checkbox not set";
            logEvent(msg);
            return checkLastReturnedFuture(null, "continueRandomTest.!checkMaxCycles()").thenRun(() -> {
                logEvent("continueContinuousDemo : !isContinuousDemoSelected() && !isContinuousDemoRevFirstSelected()");
            });
        }
        if (null != lastContinueContinuousDemoNoRecurseFuture && !lastContinueContinuousDemoNoRecurseFuture.isDone()) {
            throw new IllegalStateException(
                    "lastContinueContinuousDemoNoRecurseFuture=" + lastContinueContinuousDemoNoRecurseFuture);
        }
        String part1BlockerName = "part1ContinueContinuousDemo" + cdcCount;
        String part2BlockerName = "part2ContinueContinuousDemo" + cdcCount;
        String part3BlockerName = "part3ContinueContinuousDemo" + cdcCount;
        String part4BlockerName = "part4ContinueContinuousDemo" + cdcCount;
        AprsSystem sysArray[] = systems().toArray(new AprsSystem[0]);
        XFuture<LockInfo> disallowTogglesFuture = disallowToggles(part1BlockerName, sysArray);
        StackTraceElement trace[] = Thread.currentThread().getStackTrace();
        continueContinuousDemoTrace = trace;
        checkAllRunningOrDoingActions(-1, "continueContinuousDemo");
        if (this.stealingRobots) {
            throw new IllegalStateException("stealingRobots flag set when starting continueContinuousDemo");
        }
        return disallowTogglesFuture.thenComposeAsyncToVoid((LockInfo ignored) -> {
            return continuousDemoSetup(cdcCount);
        }, supervisorExecutorService)
                .thenComposeAsyncToVoid("ContinuouseDemo.partw",
                        x2 -> {
                            final XFuture<?> lfr = this.getLastFutureReturned();
                            if (null != lastContinueContinuousDemoNoRecurseFuture
                            && !lastContinueContinuousDemoNoRecurseFuture.isDone()) {
                                throw new IllegalStateException(
                                        "lastContinueContinuousDemoNoRecurseFuture=" + lastContinueContinuousDemoNoRecurseFuture);
                            }
                            XFuture<Boolean> continueContinuousDemoNoRecursePart2Future = startCheckAndEnableAllRobots(
                                    startingAbortCount)
                                    .thenComposeAsync("continueContinuousDemo.checkLastReturnedFuture1",
                                            x -> checkLastReturnedFuture(lfr, part2BlockerName), supervisorExecutorService)
                                    .thenComposeAsync("continueContinuousDemo.startAllActions1", x -> {
                                        XFutureVoid startAllActionsRet = startAllActions("continueContinuousDemo" + cdcCount,
                                                false, prevBlockerName, startingAbortCount);
                                        XFutureVoid allowTogglesRet = allowToggles(part1BlockerName, sysArray);
                                        XFutureVoid allowToggles2Ret = allowToggles(part2BlockerName, sysArray);
                                        return XFutureVoid.allOf(startAllActionsRet, allowTogglesRet, allowToggles2Ret);
                                    }, supervisorExecutorService)
                                    .thenComposeAsync("continueContinuousDemo.checkLastReturnedFuture2",
                                            x -> checkLastReturnedFuture(lfr, part3BlockerName), supervisorExecutorService)
                                    .thenComposeAsync("continueContinuousDemo.startReverseActions",
                                            x -> startContinuousDemoReversActions(part3BlockerName, startingAbortCount),
                                            supervisorExecutorService)
                                    .thenComposeAsync("continueContinuousDemo.checkLastReturnedFuture3",
                                            x -> checkLastReturnedFuture(lfr, part4BlockerName), supervisorExecutorService)
                                    .thenComposeAsyncToVoid("continueContinuousDemo.incrementContinuousDemoCycle",
                                            x -> incrementContinuousDemoCycle("continueContinuousDemo", startingAbortCount),
                                            supervisorExecutorService)
                                    .thenComposeAsync("continueContinuousDemo.enableAndCheckAllRobots",
                                            x -> startCheckAndEnableAllRobots(startingAbortCount), supervisorExecutorService);
                            lastContinueContinuousDemoNoRecurseFuture = continueContinuousDemoNoRecursePart2Future;
                            XFutureVoid ret = continueContinuousDemoNoRecursePart2Future
                                    .thenComposeAsyncToVoid("continueContinuousDemo.recurse" + cdcCount, ok -> {
                                        logEvent("End Continue Continuous Demo : cdcCount=" + cdcCount + ", ok=" + ok
                                                + ",toggleBlockerMap.keySet()=" + toggleBlockerMap.keySet());
                                        return checkOkElse(ok, () -> continueContinuousDemo(part4BlockerName, startingAbortCount),
                                                this::showCheckEnabledErrorSplash);
                                    }, supervisorExecutorService).peekNoCancelException(this::handleXFutureException)
                                    .alwaysComposeAsyncToVoid(() -> allowTogglesNoCheck(prevBlockerName, part1BlockerName,
                                    part2BlockerName, part3BlockerName, part4BlockerName), supervisorExecutorService);
                            ContinuousDemoFuture = ret;
                            if (null != randomTestFuture) {
                                if (isRandomTestSelected()) {
                                    resetMainRandomTestFuture();
                                } else if (isRandomTestSelected()) {
                                    resetMainPauseTestFuture();
                                }
                            }
                            return ret;
                        }, supervisorExecutorService);
    }

    private static final boolean DEBUG_CHECK_MAX_CYCLES = Boolean.parseBoolean("aprs.debugCheckMaxCycles");

    boolean checkMaxCycles() {
        if (max_cycles < 1) {
            return true;
        }
        int cdc = ContinuousDemoCycle.get();
        boolean ret = max_cycles > cdc;
        if (!ret) {
            if (DEBUG_CHECK_MAX_CYCLES) {
                System.out.println("");
                System.out.flush();
                System.err.println("");
                System.err.flush();
                Thread.dumpStack();
                System.out.println("");
                System.out.flush();
                System.err.println("");
                System.err.flush();
            }
            final String blockerName = "max_cycles limit hit = " + cdc;
            if (DEBUG_CHECK_MAX_CYCLES) {
                System.err.println("checkMaxCycles: " + blockerName);
            }
            LockInfo lockInfo = new LockInfo(blockerName);
            toggleBlockerMap.put(blockerName, lockInfo);
            togglesAllowed = false;
            enableChangeCount.incrementAndGet();
            final XFutureVoid origCancelUnstealFuture = cancelUnStealRobotFuture.getAndSet(null);
            final XFutureVoid origCancelStealFuture = cancelStealRobotFuture.getAndSet(null);

            if (null != origCancelUnstealFuture) {
                origCancelUnstealFuture.complete();
            }
            if (null != origCancelStealFuture) {
                origCancelStealFuture.complete();
            }
            setTitleMessage(blockerName);
            XFutureVoid xf = togglesAllowedXfuture.get();
            if (DEBUG_CHECK_MAX_CYCLES) {
                System.out.println(
                        "checkMaxCycles: cancelUnStealRobotFuture.getAndSet(null) = " + origCancelUnstealFuture);
                System.out.println("checkMaxCycles: cancelStealRobotFuture.getAndSet(null) = " + origCancelStealFuture);
                System.out.println("checkMaxCycles: togglesAllowedXfuture.get() = " + xf);
            }
            if (null != xf) {
                xf.complete((Void) null);
            }
            while ((xf = waitForTogglesFutures.poll()) != null) {
                if (DEBUG_CHECK_MAX_CYCLES) {
                    System.out.println("checkMaxCycles: waitForTogglesFutures.poll() = " + xf);
                }
                xf.complete((Void) null);
            }
        }
        return ret;
    }

    private final AtomicInteger continousDemoSetupCount = new AtomicInteger();
    private volatile StackTraceElement continuousDemoSetupTrace@Nullable []  = null;

    private XFutureVoid continuousDemoSetup(int cdcCount) {

        try {
            StackTraceElement trace[] = Thread.currentThread().getStackTrace();
            continuousDemoSetupTrace = trace;
            int cdscount = continousDemoSetupCount.incrementAndGet();
            String blocker = "continuousDemoSetup" + cdscount + "_" + cdcCount;
            AprsSystem sysArray[] = systems().toArray(new AprsSystem[0]);
            XFuture<LockInfo> disallowTogglesFuture = disallowToggles(blocker, sysArray);
            checkAllRunningOrDoingActions(-1, "continueDemoSetup");
            if (this.stealingRobots && !this.keepDisabled) {
                System.err.println("trace = " + Utils.traceToString(trace));
                logEventErr("stealingRobots flag set when starting continousDemoSetup");
            }
            return disallowTogglesFuture.thenComposeAsyncToVoid("contiousDemoSetup", (LockInfo lockInfo) -> {
//                    println("stealingRobots = " + stealingRobots);
//                    println("returnRobotRunnable = " + returnRobotRunnable);
                if (!this.keepDisabled && (this.stealingRobots || null != returnRobotFunction.get())) {
                    System.err.println("trace = " + Utils.traceToString(trace));
                    logEventErr("stealingRobots flag set when starting continousDemoSetup : returnRobotRunnable.get()="
                            + returnRobotFunction.get());
                    disconnectAll();
                    return returnRobots1("contiousDemoSetup", null, null, -1, -1);
                } else {
                    return XFutureVoid.completedFuture();
                }
            }, supervisorExecutorService).thenRunAsync(() -> {
                checkAllUniques();
                connectAll();
            }, supervisorExecutorService).peekNoCancelException(this::handleXFutureException)
                    .alwaysComposeToVoid(() -> allowToggles(blocker, sysArray));
        } catch (Exception exception) {
            logException(exception);
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", exception);
            throw asRuntimeException(exception);
        }
    }

    private volatile boolean debug = false;

    private synchronized void checkFutures() {

        try {
            XFuture<?> xfStealUnsteal = stealUnstealList.poll();
            while (null != xfStealUnsteal) {
                if (!xfStealUnsteal.isDone() && !xfStealUnsteal.isCancelled()
                        && !xfStealUnsteal.isCompletedExceptionally()) {
                    stealUnstealList.add(xfStealUnsteal);
                    String msg = "stealUnstealFuture = " + xfStealUnsteal + " not completed";
                    Thread.dumpStack();
                    System.err.println(msg);
                    xfStealUnsteal.printProfile(System.err);
                    xfStealUnsteal.printStatus(System.err);
                    logEventErr(msg);
                    throw new IllegalStateException(msg);
                }
                xfStealUnsteal = stealUnstealList.poll();
            }
            final XFutureVoid lastStealRobotsInternalBeforeAllowTogglesFinal = lastStealRobotsInternalBeforeAllowToggles;
            if (null != lastStealRobotsInternalBeforeAllowTogglesFinal
                    && !lastStealRobotsInternalBeforeAllowTogglesFinal.isDone()
                    && !lastStealRobotsInternalBeforeAllowTogglesFinal.isCompletedExceptionally()
                    && !lastStealRobotsInternalBeforeAllowTogglesFinal.isCancelled()) {
                Thread.dumpStack();
                System.err.println("lastStealRobotsInternalBeforeAllowTogglesTrace="
                        + Utils.traceToString(lastStealRobotsInternalBeforeAllowTogglesTrace));
                lastStealRobotsInternalBeforeAllowTogglesFinal.printStatus(System.err);
                throw new IllegalStateException(
                        "lastStealRobotsInternalBeforeAllowToggles=" + lastStealRobotsInternalBeforeAllowTogglesFinal);
            }
            final XFutureVoid lastStartReverseActionsFutureFinal = lastStartReverseActionsFuture;
            if (null != lastStartReverseActionsFutureFinal && !lastStartReverseActionsFutureFinal.isDone()
                    && !lastStartReverseActionsFutureFinal.isCompletedExceptionally()
                    && !lastStartReverseActionsFutureFinal.isCancelled()) {
                Thread.dumpStack();
                System.err.println("lastStartReverseActionsTrace=" + Utils.traceToString(lastStartReverseActionsTrace));
                lastStartReverseActionsFutureFinal.printStatus(System.err);
                throw new IllegalStateException("lastStartReverseActionsFuture=" + lastStartReverseActionsFutureFinal);
            }
            final XFuture<?> startAllFutureFinal = startAllFuture;
            if (null != startAllFutureFinal && !startAllFutureFinal.isDone()
                    && !startAllFutureFinal.isCompletedExceptionally() && !startAllFutureFinal.isCancelled()) {
                Thread.dumpStack();
                System.err.println("startAllTrace=" + Utils.traceToString(startAllTrace));
                startAllFutureFinal.printStatus(System.err);
                throw new IllegalStateException("startAllFuture=" + startAllFutureFinal);
            }
            final XFutureVoid startAllActionsFutureFinal = startAllActionsFuture;
            if (null != startAllActionsFutureFinal && !startAllActionsFutureFinal.isDone()
                    && !startAllActionsFutureFinal.isCompletedExceptionally()
                    && !startAllActionsFutureFinal.isCancelled()) {
                Thread.dumpStack();
                System.err.println("startAllActionsTrace=" + Utils.traceToString(startAllActionsTrace));
                startAllActionsFutureFinal.printStatus(System.err);
                throw new IllegalStateException("startAllActionsFuture=" + startAllActionsFutureFinal);
            }
            checkAllRunningOrDoingActions(-1, "checkFutures");
            lastStartReverseActionsFuture = null;
            lastStartReverseActionsTrace = null;
            startAllActionsTrace = null;
            startAllActionsFuture = null;
            startAllFuture = null;
            startAllTrace = null;
            lastStealRobotsInternalBeforeAllowToggles = null;
            lastStealRobotsInternalBeforeAllowTogglesTrace = null;
        } catch (Exception exception) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, exception);
            logException(exception);
            logEventErr(exception.getMessage() + ", toggleBlockerMap.keySet()=" + toggleBlockerMap.keySet());
            throw new RuntimeException(exception);
        }
    }

    private volatile @Nullable XFutureVoid lastStartReverseActionsFuture = null;
    private volatile StackTraceElement lastStartReverseActionsTrace@Nullable []  = null;
    private final AtomicInteger startReverseActionsCount = new AtomicInteger();

    /**
     * Start actions in reverse mode where kit trays will be emptied rather than
     * filled.
     *
     * @return future that can be used to attach additional actions after this
     * is complete
     */
    XFutureVoid startReverseActions(@Nullable String prevBlockerName, int startingAbortCount) {

        StackTraceElement trace[] = Thread.currentThread().getStackTrace();
        lastStartReverseActionsTrace = trace;

        int currentAbortCount = getAbortCount();
        if (startingAbortCount != currentAbortCount) {
            String msg = "startReverseActions quiting : startingAbortCount=" + startingAbortCount
                    + ", currentAbortCount=" + currentAbortCount;
            logEvent(msg);
            XFutureVoid ret2 = XFutureVoid.completedFutureWithName(msg);
            lastStartReverseActionsFuture = ret2;
            return ret2;
        }
        int continousDemoCycleCount = ContinuousDemoCycle.get();
        int sraCount = startReverseActionsCount.incrementAndGet();
        logEvent("startReverseActions  startReverseActionsCount=" + sraCount + ", continousDemoCycleCount="
                + continousDemoCycleCount);
        String blockerName = "START startReverseActions_" + sraCount + "_" + continousDemoCycleCount;
        AprsSystem sysArray[] = systems().toArray(new AprsSystem[0]);
        XFuture<LockInfo> disallowTogglesFuture = disallowToggles(blockerName, sysArray);
        checkFutures();
        checkAllRunningOrDoingActions(-1, "startReverseActions");
        XFuture<Boolean> checkAndEnableFuture = disallowTogglesFuture
                .thenComposeAsync("startReverseActions:startCheckAndEnableAllRobots ", (LockInfo ignored) -> {
                    if (debug) {
                        debugAction();
                    }
                    return startCheckAndEnableAllRobots(startingAbortCount);
                }, supervisorExecutorService);
        XFutureVoid ret = checkAndEnableFuture.thenComposeAsyncToVoid("startReverseActions.startAllActions", ok -> {
            XFutureVoid checkOkRet = checkOkElseToVoid(ok,
                    () -> startAllActions("startReverseActions", true, prevBlockerName, startingAbortCount),
                    this::showCheckEnabledErrorSplash);
            XFutureVoid allowTogglesRet = allowToggles(blockerName, sysArray);
            logEvent("COMPLETED startReverseActions startReverseActionsCount=" + sraCount + ",continousDemoCycleCount="
                    + continousDemoCycleCount);
            return XFutureVoid.allOf(checkOkRet, allowTogglesRet);
        }, supervisorExecutorService);
        lastStartReverseActionsFuture = ret;
        return ret;
    }

    void savePosFile(File f) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            for (int i = 0; i < selectedPosMapFileCachedTable.getColumnCount(); i++) {
                pw.print(selectedPosMapFileCachedTable.getColumnName(i));
                pw.print(",");
            }
            pw.println();
            for (int i = 0; i < selectedPosMapFileCachedTable.getRowCount(); i++) {
                for (int j = 0; j < selectedPosMapFileCachedTable.getColumnCount(); j++) {
                    pw.print(selectedPosMapFileCachedTable.getValueAt(i, j));
                    pw.print(",");
                }
                pw.println();
            }
        }
        lastPosMapFile = f;
    }

    private void clearPositionMappingsFilesTable() {
        positionMappingsFilesCachedTable.setRowColumnCount(0, 0);
        positionMappingsFilesCachedTable.addColumn("System");
        for (String name : robotTaskMap.keySet()) {
            positionMappingsFilesCachedTable.addColumn(name);
        }
        for (String name : robotTaskMap.keySet()) {
            Object data[] = new Object[robotTaskMap.size() + 1];
            data[0] = name;
            positionMappingsFilesCachedTable.addRow(data);
        }
        Utils.autoResizeTableColWidths(positionMappingsFilesCachedTable);
        Utils.autoResizeTableRowHeights(positionMappingsFilesCachedTable);
        hidePosTablePopupMenu();
    }

    private void hidePosTablePopupMenu() {
        if (null != posTablePopupMenu) {
            runOnDispatchThread(this::hidePosTablePopupMenuOnDisplay);
        }
    }

    @UIEffect
    private void hidePosTablePopupMenuOnDisplay() {
        if (null != posTablePopupMenu) {
            posTablePopupMenu.setVisible(false);
        }
    }

    private final @MonotonicNonNull JPopupMenu posTablePopupMenu = null;

    public XFutureVoid enableAllRobotsOnSupervisorService() {
        return XFuture
                .supplyAsync("enableAllRobotsOnSupervisorService", this::enableAllRobots, supervisorExecutorService)
                .thenComposeAsyncToVoid(x -> x, supervisorExecutorService);
    }

    /**
     * Enable all robots. (Note: no check is made if the robot is physically in
     * estop and no change to its estop state is made, only the checkboxes in
     * the robots table are potentially changed.)
     */
    private synchronized XFutureVoid enableAllRobots() {
        try {
            if (keepDisabled) {
                return XFutureVoid.completedFutureWithName("enableAllRobots:keepDisabled");
            }
            logEvent("enableAllRobots() called.");
            clearStealingRobotsFlag();
            AprsSystem sysArray[] = systems().toArray(new AprsSystem[0]);
            String blockerName = "enableAllRobots";
            final XFuture<LockInfo> disallowTogglesFuture = disallowToggles(blockerName, sysArray);
            return disallowTogglesFuture
                    .thenComposeAsyncToVoid(
                            x -> {
                                try {
                                    return updateRobotsTableFromMapsAndEnableAll();
                                } catch (Exception exception) {
                                    log(Level.SEVERE, "", exception);
                                    throw new RuntimeException(exception);
                                }
                            }, supervisorExecutorService)
                    .alwaysComposeAsyncToVoid(() -> allowToggles(blockerName, sysArray), supervisorExecutorService)
                    .peekNoCancelException(this::handleXFutureException)
                    .thenComposeAsyncToVoid(() -> this.waitSingleStep("enableAllRobots"), supervisorExecutorService);
        } catch (Exception exception) {
            log(Level.SEVERE, "", exception);
            throw new RuntimeException(exception);
        }
    }

    private final AtomicInteger enableAndCheckAllRobotsCount = new AtomicInteger();

    /**
     * Enable and check all robots. All of the checkboxes in the robots table
     * will be set and a short nop program will be run on each robot to ensure
     * they are out of estop and able to run programs autonomously. A delay of a
     * second or two and the sound of brakes coming off may be heard. The checks
     * will be performed in other threads asynchronously.
     *
     * @return a future that can be used to determine when and if all the checks
     * succeed.
     */
    synchronized private XFuture<Boolean> startCheckAndEnableAllRobots(int startingAbortCount) {
        final boolean areAllSystemsAlreadyChecked = areAllSystemsAlreadyChecked();
        if (!checkMaxCycles()) {
            return checkLastReturnedFuture(null, null).thenApply(x -> true);
        }
        logEvent("startCheckAndEnableAllRobots: areAllSystemsAlreadyChecked= " + areAllSystemsAlreadyChecked);
        if (areAllSystemsAlreadyChecked) {
            checkAllRunningOrDoingActions(-1, "startCheckAndEnableAllRobots");
            return XFuture.completedFutureWithName("allSystemsAlreadyChecked", true);
        }
        int currentAbortCount = getAbortCount();
        if (startingAbortCount != currentAbortCount) {
            String msg = "startCheckAndEnableAllRobots quiting : startingAbortCount=" + startingAbortCount
                    + ", currentAbortCount=" + currentAbortCount;
            logEvent(msg);
            XFuture<Boolean> ret2 = XFutureVoid.completedFutureWithName(msg, true);
            return ret2;
        }

        String blockerName = "startCheckAndEnableAllRobots" + enableAndCheckAllRobotsCount.incrementAndGet();
        AprsSystem sysArray[] = systems().toArray(new AprsSystem[0]);
        XFuture<LockInfo> disallowTogglesFuture = disallowToggles(blockerName, sysArray);
        checkAllRunningOrDoingActions(-1, "startCheckAndEnableAllRobots");
        return disallowTogglesFuture.thenComposeAsync((LockInfo ignored) -> {
            XFutureVoid step1Future = updateRobotsTableFromMapsAndEnableAll();
            boolean KeepAndDisplayXFutureProfilesSelected = isKeepAndDisplayXFutureProfilesSelected();
            step1Future.setKeepOldProfileStrings(KeepAndDisplayXFutureProfilesSelected);
            XFuture<Boolean> step2Future = step1Future.thenComposeAsyncToVoid("startCheckAndEnableAllRobots.2", () -> {
                try {
                    initColorTextSocket();
                    writeToColorTextSocket("0x00FF00, 0x00FF000\r\n".getBytes());
                } catch (IOException ex) {
                    log(Level.SEVERE, "", ex);
                }
                if (keepDisabled) {
                    return XFutureVoid.completedFuture();
                }
                clearStealingRobotsFlag();
                XFutureVoid rrF = returnRobots1("enableAndCheckAllRobots", null, null, -1, -1);
                rrF.setKeepOldProfileStrings(KeepAndDisplayXFutureProfilesSelected);
                return rrF;
            }, supervisorExecutorService).thenComposeAsync(x2 -> checkEnabledAll(), supervisorExecutorService);
            return step2Future.alwaysComposeAsyncToInput(() -> {
                return allowToggles(blockerName, sysArray).thenRun(() -> {
                    logStartCheckAndEnableAllRobotsComplete();
                });
            }, supervisorExecutorService);
        }, supervisorExecutorService);
    }

    private XFutureVoid updateRobotsTableFromMapsAndEnableAll() {
        try {
            if (!keepDisabled) {
                for (String key : robotTaskMap.keySet()) {
//                    robotTaskMap.put(key, true);
                    for (AprsSystem sys : aprsSystems) {
                        if (sys.getOrigRobotName().equals(key)) {
                            robotTaskMapPut(key, sys);
                            break;
                        }
                    }
                }
            }
            if (!CRCLUtils.isGraphicsEnvironmentHeadless()) {
                initColorTextSocket();
                writeToColorTextSocket("0x00FF00, 0x00FF000\r\n".getBytes());
            }
            if (null == displayJFrame) {
                return XFutureVoid.completedFutureWithName("updateRobotsTableFromMapsAndEnableAll.null==displayJFrame");
            } else {
                Map<String, Integer> robotDisableCountMapCopy = new HashMap<>(robotEnableCountMap);
                Map<String, Long> robotDisableTotalTimeMapCopy = new HashMap<>(robotDisableTotalTimeMap);
                final AprsSupervisorDisplayJFrame checkedDisplayJFrame = displayJFrame;
                XFutureVoid step1Future = runOnDispatchThread(() -> {
                    checkedDisplayJFrame.updateRobotsTableFromMapsAndEnableAll(robotTaskMap, robotDisableCountMapCopy,
                            robotDisableTotalTimeMapCopy);
//                println("sleeping in updateRobotsTableFromMapsAndEnableAll");
//                try {
//                    Thread.sleep(20000);
//                } catch (InterruptedException ex) {
//                    Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", ex);
//                    throw new RuntimeException(ex);
//                }
//                println("sleep in updateRobotsTableFromMapsAndEnableAll");

                });
                return step1Future;
            }
        } catch (Exception exception) {
            log(Level.SEVERE, "", exception);
            throw asRuntimeException(exception);
        }
    }

    public void robotTaskMapPut(String key, AprsSystem sys) {
        logEvent("putting sys = " + sys + " int robotTaskMap for " + key + ", robotTaskMap=" + robotTaskMap);
        robotTaskMap.put(key, sys.getTaskName());
        refreshRobotsTable();
    }

    private void logStartCheckAndEnableAllRobotsComplete() {
        logEvent("startCheckAndEnableAllRobots complete");
    }

    private XFuture<Boolean> checkEnabledAll() {
        final boolean areAllSystemsAlreadyChecked = areAllSystemsAlreadyChecked();
        logEvent("checkEnabledAll : areAllSystemsAlreadyChecked=" + areAllSystemsAlreadyChecked);
        if (areAllSystemsAlreadyChecked) {
            return XFuture.completedFutureWithName("allSystemsAlreadyChecked", true);
        }
        boolean origIgnoreTitleErrs = ignoreTitleErrors.getAndSet(true);
        @SuppressWarnings("unchecked")
        XFuture<Boolean> futures[] = (XFuture<Boolean>[]) new XFuture<?>[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystem sys = aprsSystems.get(i);
            final String robotName = requireNonNull(sys.getRobotName(), "sys.getRobotName()");
            final String robotTaskName = requireNonNull(robotTaskMap.get(robotName),
                    "robotTaskMap.get(sys.getRobotName()) : sys.getRobotName()=" + robotName + ", robotTaskMap="
                    + robotTaskMap);
            if (keepDisabled && (!sys.getTaskName().equals(robotTaskName))) {
                futures[i] = XFutureVoid.completedFuture(skipDisabled);
                continue;
            }
            if (!sys.getTaskName().equals(robotTaskName)) {
                throw new RuntimeException("sys=" + sys + ", robotTaskName=" + robotTaskName);
            }
            futures[i] = sys.startCheckEnabled().thenApplyAsync(x -> {
                logEvent(robotName + " checkEnabled returned " + x);
                return x;
            }, supervisorExecutorService);
        }
        XFuture<Boolean> ret = XFuture.completedFuture(true);
        BiFunction<Boolean, Boolean, Boolean> andBiFunction = (Boolean ok1, Boolean ok2) -> ok1 && ok2;
        for (int i = 0; i < futures.length; i++) {
            XFuture<Boolean> fi = futures[i];
            if (fi.isCompletedExceptionally()) {
                if (!origIgnoreTitleErrs) {
                    ignoreTitleErrors.set(false);
                }
                XFuture<Boolean> newret = new XFuture<>(
                        "checkEnabledAll.alreadyFailed." + aprsSystems.get(i).getTaskName());
                newret.completeExceptionally(new IllegalStateException(
                        "isCompletedExceptionally() for " + aprsSystems.get(i).getTaskName()));
                return newret;
            }
            ret = ret.thenCombine("checkEnabledAll(" + (i + 1) + "/" + futures.length + ")", fi, andBiFunction);
        }
        return ret.alwaysRun(() -> {
            if (!origIgnoreTitleErrs) {
                ignoreTitleErrors.set(false);
            }
        });
    }

    private boolean areAllSystemsAlreadyChecked() {
        boolean allAlreadyCheckedAndEnbled = true;
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystem sys = aprsSystems.get(i);
            if (!sys.isEnableCheckedAlready()) {
                allAlreadyCheckedAndEnbled = false;
                logEvent("!sys.isEnableCheckedAlready():sys=" + sys);
            }
        }
        return allAlreadyCheckedAndEnbled;
    }

    private final AtomicInteger pauseCount = new AtomicInteger();

    private void setPauseCount(int count) {
        if (null != displayJFrame) {
            displayJFrame.setPauseCount(count);
        }
    }

    public void pause() {
        for (AprsSystem aprsSys : aprsSystems) {
            if (aprsSys.isConnected() && !aprsSys.isPaused()) {
                aprsSys.pause();
            }
        }
        if (debug) {
            Thread.dumpStack();
        }
        logEvent("pause");
        stopRunTimeTimer();
        completeResumeFuture();
        int count = pauseCount.incrementAndGet();
        setPauseCount(count);
        if (!isPauseSelected()) {
            setPauseSelected(true);
        }
        resumeFuture.set(new XFutureVoid("resume"));
        setTitleMessage("Paused");
        if (logEventErrCount.get() == 0) {
            runOnDispatchThread(() -> {
                setIconImageOnDisplay(IconImages.DISCONNECTED_IMAGE);
            });
        }
    }

    private XFutureVoid stopRunTimeTimer() {
        if (null != runTimeTimer) {
            return runOnDispatchThread(this::stopRunTimeTimerOnDisplay);
        } else {
            return XFutureVoid.completedFuture();
        }
    }

    @UIEffect
    private void stopRunTimeTimerOnDisplay() {
        if (null != runTimeTimer) {
            runTimeTimer.stop();
            runTimeTimer = null;
        }
    }

    private void completeResumeFuture() {
        XFutureVoid rf = resumeFuture.getAndSet(null);
        if (null != rf) {
            rf.complete(null);
        }
    }

    private XFutureVoid waitResume() {
        XFutureVoid rf = resumeFuture.get();
        if (null != rf) {
            return rf
                    .peekNoCancelException(this::handleXFutureException)
                    .thenComposeAsyncToVoid(() -> this.waitSingleStep("waitResume"), supervisorExecutorService);
        } else {
            return XFutureVoid.completedFutureWithName("waitResume.rf==null")
                    .peekNoCancelException(this::handleXFutureException)
                    .thenComposeAsyncToVoid(() -> this.waitSingleStep("waitResume"), supervisorExecutorService);
        }
    }

    private volatile boolean resuming = false;

    public void resume() {
        logEvent("resume");
        resuming = true;
        try {
            resumeForPrepOnly();
            startUpdateRunningTimeTimer();
            setTitleMessage("");
        } finally {
            resuming = false;
        }
    }

    private void resumeForPrepOnly() {
        if (isPauseSelected()) {
            setPauseSelected(false);
        }
        for (AprsSystem aprsSys : aprsSystems) {
            if (aprsSys.isPaused()) {
                aprsSys.resume();
            }
        }
        completeResumeFuture();
        for (AprsSystem aprsSys : aprsSystems) {
            if (aprsSys.isPaused()) {
                throw new IllegalStateException(aprsSys + " is still paused after resume");
            }
        }
    }

    private volatile XFuture<?> lastStartAllActionsArray@Nullable []  = null;

    private final ConcurrentHashMap<Integer, XFuture<Boolean>> systemContinueMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, XFutureVoid> debugSystemContinueMap = new ConcurrentHashMap<>();

    private XFutureVoid continueSingleContinuousDemo(AprsSystem sys, int recurseNum) {
        XFutureVoid ret = debugSystemContinueMap.compute(sys.getMyThreadId(), (k, v) -> {
            return continueSingleContinuousDemoInner(sys, recurseNum);
        });
        StringBuilder tasksNames = new StringBuilder();
        Set<Integer> keySet = debugSystemContinueMap.keySet();
        @SuppressWarnings("unchecked")
        XFutureVoid futures[] = new XFutureVoid[keySet.size()];
        int i = 0;
        for (int id : keySet) {
            assert (i < futures.length) : "futures=" + Arrays.toString(futures) + ",keySet=" + keySet + ",i=" + i;
            XFutureVoid debugFuture = debugSystemContinueMap.get(id);
            if (null != debugFuture) {
                futures[i++] = debugFuture;
            }
        }
        assert (i == futures.length) : "futures=" + Arrays.toString(futures) + ",keySet=" + keySet + ",i=" + i;
        for (AprsSystem sysTemp : aprsSystems) {
            if (debugSystemContinueMap.containsKey(sysTemp.getMyThreadId())) {
                tasksNames.append(sysTemp.getTaskName()).append(',');
            }
        }
        ContinuousDemoFuture = XFuture.allOfWithName(
                "continueSingleContinuousDemo.allOf(" + tasksNames.toString() + ").recurseNum=" + recurseNum, futures);
        if (null != randomTestFuture) {
            resetMainRandomTestFuture();
        }
        return ret;
    }

    private XFutureVoid continueSingleContinuousDemoInner(AprsSystem sys, int recurseNum) {
        String toggleLockName = "continueSingleContinuousDemoInner" + recurseNum + "_" + sys.getMyThreadId();
        return systemContinueMap.computeIfAbsent(sys.getMyThreadId(), k -> {
            return new XFuture<>("continueSingleContinuousDemo.holder " + sys);
        }).thenCompose("continueSingleContinuousDemo.continuing: " + recurseNum + " " + sys, x -> {
            logEvent("startCheckEnabled(recurseNum=" + recurseNum + ") for " + sys);
            XFuture<LockInfo> disallowTogglesFuture = disallowToggles(toggleLockName, sys);
            return disallowTogglesFuture.thenComposeAsync((LockInfo ignored) -> {
                final String robotTaskName = robotTaskMap.get(sys.getRunName());
                if (keepDisabled && null == robotTaskName) {
                    return XFuture.completedFuture(skipDisabled);
                }
                if (!sys.getTaskName().equals(robotTaskName)) {
                    throw new RuntimeException("sys=" + sys + ",robotTaskName=" + robotTaskName);
                }
                return sys.startCheckEnabled().thenApply(y -> x);
            }, supervisorExecutorService);
        }).thenComposeAsync("continueSingleContinuousDemo.continuing: " + recurseNum + " " + sys, x -> {
            XFuture<Boolean> ret = sys.startPreCheckedContinuousDemo("continueSingleContinuousDemoInner" + sys, x);
            logEvent("startPreCheckedContinuousDemo(recurseNum=" + recurseNum + ",reverseFirst=" + x + ") for " + sys);
            allowToggles(toggleLockName, sys);
            return ret;
        }, supervisorExecutorService)
                .thenComposeAsyncToVoid(
                        "continueSingleContinuousDemo.recurse " + recurseNum + " " + sys,
                        x -> continueSingleContinuousDemo(sys, (recurseNum + 1)),
                        supervisorExecutorService);
    }

    private XFutureVoid startAllIndContinuousDemo() {
        logEvent("startAllIndContinuousDemo");
        @SuppressWarnings("unchecked")
        XFutureVoid futures[] = new XFutureVoid[aprsSystems.size()];
        StringBuilder tasksNames = new StringBuilder();
        boolean revFirst = isContinuousDemoRevFirstSelected();
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystem sys = aprsSystems.get(i);
            logEvent("startContinuousDemo(reverseFirst=false) for " + sys);
            futures[i] = sys.startContinuousDemo("startAllIndContinuousDemo", revFirst)
                    .thenComposeAsyncToVoid(x -> continueSingleContinuousDemo(sys, 1), supervisorExecutorService);
            tasksNames.append(aprsSystems.get(i).getTaskName());
            tasksNames.append(",");
        }
        lastStartAllActionsArray = futures;
//        allowToggles();
        return XFuture.allOfWithName("startAllIndContinuousDemo.allOf(" + tasksNames.toString() + ")", futures);
    }

    private final AtomicInteger startAllActionsCount = new AtomicInteger();

    private volatile @Nullable XFutureVoid startAllActionsFuture = null;
    private volatile StackTraceElement startAllActionsTrace@Nullable []  = null;

    private volatile boolean lastStartAllActionsReverseFlag = false;

    private XFutureVoid startAllActions(@Nullable String comment, boolean reverseFlag, @Nullable String prevBlockerName,
            int startingAbortCount) {
        int saaNumber = startAllActionsCount.incrementAndGet();
        int cdc = ContinuousDemoCycle.get();
        int ecc = enableChangeCount.get();
        lastStartAllActionsReverseFlag = reverseFlag;
        final String info = "startAllActions:rev=" + reverseFlag + ", saa=" + saaNumber + ", cdc=" + cdc + ", ecc="
                + ecc + ", comment=" + comment + ",prevBlockerName=" + prevBlockerName;
        final String endInfo = "endAllActions:rev=" + reverseFlag + ", saa=" + saaNumber + ", cdc=" + cdc + ", ecc="
                + ecc + ", comment=" + comment + ",prevBlockerName=" + prevBlockerName;
        try {
            takeAllSnapshots(info);
        } catch (IOException ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
        }

        int currentAbortCount = getAbortCount();
        if (startingAbortCount != currentAbortCount) {
            String msg = "startAllActions quiting : startingAbortCount=" + startingAbortCount + ", currentAbortCount="
                    + currentAbortCount;
            logEvent(msg);
            XFutureVoid ret2 = XFutureVoid.completedFutureWithName(msg);
            startAllActionsFuture = ret2;
            return ret2;
        }
        final int systemsSize = aprsSystems.size();
        XFuture<?> futures[] = new XFuture<?>[systemsSize + 1];
        StringBuilder tasksNames = new StringBuilder();
        startAllActionsTrace = Thread.currentThread().getStackTrace();
        for (int i = 0; i < systemsSize; i++) {
            AprsSystem sys = aprsSystems.get(i);
            int sysThreadId = sys.getMyThreadId();
            logEvent("startActions for " + sys);

            if (keepDisabled) {
                final String sysRobotName = sys.getRobotName();
                if (!sys.isConnected() || null == sysRobotName) {
                    futures[i] = XFuture.completedFuture(true);
                    continue;
                } else {
                    final String sysRobotNameTask = requireNonNull(robotTaskMap.get(sysRobotName),
                            "robotTaskMap.get(" + sysRobotName + ")");
                    if (!sysRobotNameTask.equals(sys.getTaskName())) {
                        futures[i] = XFuture.completedFuture(true);
                        continue;
                    }
                }
            }
            futures[i] = sys.startActions(comment + ".startAllActions" + saaNumber, reverseFlag).thenComposeAsync(x -> {
                String runName = sys.getRunName();
                logEvent("startActions " + sys + ",saaNumber= " + saaNumber + " completed action list run " + runName
                        + " : " + x);
                return finishAction(sysThreadId).thenApply(x2 -> {
                    logEvent("finish startActions " + sys + ",saaNumber= " + saaNumber + " completed action list run "
                            + runName + " : " + x);
                    return x;
                });
            }, supervisorExecutorService);
            tasksNames.append(sys.getTaskName());
            tasksNames.append(",");
        }
        if (null != prevBlockerName && prevBlockerName.length() > 0) {
            futures[systemsSize] = allowTogglesNoCheck(prevBlockerName);
        } else {
            futures[systemsSize] = XFutureVoid.completedFuture();
        }
        lastStartAllActionsArray = futures;
        String allOfName = "startAllActions(" + comment + "," + reverseFlag + ").allOf(" + tasksNames.toString()
                + ") saaNumber=" + saaNumber;
        XFutureVoid ret = XFutureVoid.allOfWithName(allOfName, futures).thenRunAsync(() -> {
            logEvent(allOfName);
            try {
                takeAllSnapshots(endInfo);
            } catch (IOException ex) {
                Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }, supervisorExecutorService).peekNoCancelException(this::handleXFutureException);
        startAllActionsFuture = ret;
        return ret;
    }

    private final ConcurrentHashMap<Integer, XFutureVoid[]> continueCompletionBlockersMap = new ConcurrentHashMap<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addFinishBlocker(int threadId, XFutureVoid f) {
        continueCompletionBlockersMap.compute(threadId, (Integer k, XFutureVoid @Nullable [] v) -> {
            try {
                if (null != v) {
                    List<XFutureVoid> l = Arrays.stream(v).filter(f2 -> !f2.isDone()).collect(Collectors.toList());
                    l.add(f);
                    return l.toArray(new XFutureVoid[0]);
                } else {
                    return new XFutureVoid[]{f};
                }
            } catch (Throwable e) {
                log(Level.SEVERE, "", e);
                throw new RuntimeException(e);
            }
        });
    }

    private static String shortTrace(StackTraceElement @Nullable [] trace) {
        if (null == trace) {
            return "";
        }
        String shortTrace = Arrays.toString(trace);
        if (shortTrace.length() > 80) {
            shortTrace = shortTrace.substring(0, 75) + " ... ";
        }
        return shortTrace;
    }

    /**
     * Log an exception to the events table.
     *
     * @param level log severity indicator
     * @param msg message to show
     * @param thrown exception causing this event
     */
    private void log(Level level, @Nullable String msg, Throwable thrown) {
        getLogger().log(level, msg, thrown);
        logEvent("Exception thrown : msg=" + msg + ",thrown=" + thrown + ", trace="
                + shortTrace(thrown.getStackTrace()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private XFutureVoid finishAction(int threadId) {
        XFutureVoid[] futures = continueCompletionBlockersMap.compute(threadId,
                (Integer k, XFutureVoid @Nullable [] v) -> {
                    try {
                        if (null != v) {
                            List<XFutureVoid> l = Arrays.stream(v).filter(f2 -> !f2.isDone())
                                    .collect(Collectors.toList());
                            if (l.size() < 1) {
                                return new XFutureVoid[0];
                            }

                            XFutureVoid[] newV = l.toArray(new XFutureVoid[0]);
                            if (newV.length < 1) {
                                return new XFutureVoid[0];
                            }
                            return newV;
                        } else {
                            return new XFutureVoid[0];
                        }
                    } catch (Throwable e) {
                        log(Level.SEVERE, "", e);
                        throw new RuntimeException(e);
                    }
                });
        if (null == futures || futures.length < 1) {
            return XFutureVoid.completedFutureWithName("finishAction(" + threadId + ").completed");
        } else {
            logEvent("finishAction waiting for " + Arrays.toString(futures));
            return XFuture.allOfWithName("finishAction(" + threadId + ").allOf", futures);
        }
    }

    private XFutureVoid finishAction(AprsSystem sys) {
        return finishAction(sys.getMyThreadId());
    }

    XFutureVoid continueAllActions() {
        if (!checkMaxCycles()) {
            logEvent("Continue All Actions quitting because checkMaxCycles() returned false: ContinuousDemoCycle="
                    + ContinuousDemoCycle.get());
            return checkLastReturnedFuture(null, "continueRandomTest.!checkMaxCycles()").thenRun(() -> {
                logEvent("continueAllActions : checkMaxCycles");
            });
        }
        if (!isTogglesAllowed()) {
            throw new IllegalStateException(
                    "continueAllActions when  toggleBlockerMap.keySet()" + toggleBlockerMap.keySet());
        }
        logEvent("continueAllActions");
        List<XFuture<?>> futures = new ArrayList<>();
        StringBuilder tasksNames = new StringBuilder();
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystem sys = aprsSystems.get(i);
            String robotName = sys.getRobotName();
            if (null == robotName || robotName.length() < 1) {
                continue;
            }
            final String robotTaskName = robotTaskMap.get(robotName);
            final boolean enabledInMap = robotTaskName != null;
            if (!enabledInMap) {
                continue;
            }
            int sysThreadId = sys.getMyThreadId();
            logEvent("Continue actions for " + sys.getTaskName() + " with " + sys.getRobotName());
            final XFuture<Boolean> continueActionListFuture;
            if (sys.isConnected()) {
                continueActionListFuture = sys.continueActionList("continueAllActions");
            } else {
                continueActionListFuture = sys.connectRobot()
                        .thenCompose(() -> sys.continueActionList("continueAllActions"));
            }
            XFuture<?> futureI = continueActionListFuture.thenComposeAsync(x -> {
                logEvent("continueAllActions " + sys.getRunName() + " completed action list " + x);
                return finishAction(sysThreadId).thenApply(x2 -> {
                    logEvent("continueAllActions finish " + sys.getRunName() + " completed action list " + x);
                    return x;
                });
            }, supervisorExecutorService);
            futures.add(futureI);
            tasksNames.append(sys.getTaskName());
            tasksNames.append(",");
        }
        lastStartAllActionsArray = futures.toArray(new XFuture<?>[0]);
//        allowToggles();
        return XFuture.allOfWithName("continueAllActions.allOf(" + tasksNames.toString() + ")", futures);
    }

    XFutureVoid enableAndContinueAllActions() {
        if (!checkMaxCycles()) {
            logEvent("Continue All Actions quitting because checkMaxCycles() returned false: ContinuousDemoCycle="
                    + ContinuousDemoCycle.get());
            return checkLastReturnedFuture(null, "continueRandomTest.!checkMaxCycles()").thenRun(() -> {
                logEvent("continueAllActions : checkMaxCycles");
            });
        }
        if (!isTogglesAllowed()) {
            throw new IllegalStateException(
                    "continueAllActions when  toggleBlockerMap.keySet()" + toggleBlockerMap.keySet());
        }
        logEvent("continueAllActions");
        XFuture<?> futures[] = new XFuture<?>[aprsSystems.size()];
        StringBuilder tasksNames = new StringBuilder();
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystem sys = aprsSystems.get(i);
            int sysThreadId = sys.getMyThreadId();
            logEvent("Continue actions for " + sys.getTaskName() + " with " + sys.getRobotName());
            futures[i] = sys.continueActionList("continueAllActions").thenComposeAsync(x -> {
                logEvent("continueAllActions " + sys.getRunName() + " completed action list " + x);
                return finishAction(sysThreadId).thenApply(x2 -> {
                    logEvent("continueAllActions finish " + sys.getRunName() + " completed action list " + x);
                    return x;
                });
            }, supervisorExecutorService);
            tasksNames.append(sys.getTaskName());
            tasksNames.append(",");
        }
        lastStartAllActionsArray = futures;
//        allowToggles();
        return XFuture.allOfWithName("continueAllActions.allOf(" + tasksNames.toString() + ")", futures);
    }

    private <T> XFuture<T> checkOkElse(Boolean ok, Supplier<XFuture<T>> okSupplier,
            Supplier<XFuture<T>> notOkSupplier) {
        if (ok) {
            return okSupplier.get();
        } else {
            return notOkSupplier.get();
        }
    }

    private XFutureVoid checkOkElseToVoid(Boolean ok, Supplier<XFutureVoid> okSupplier,
            Supplier<XFutureVoid> notOkSupplier) {
        if (ok) {
            return okSupplier.get();
        } else {
            return notOkSupplier.get();
        }
    }

    // private XFutureVoid checkOk(Boolean ok, Supplier<XFutureVoid> okSupplier) {
//		if (ok) {
//			return okSupplier.get();
//		} else {
//			return XFutureVoid.completedFutureWithName("checkOk(false)");
//		}
//	}
    public boolean isResuming() {
        return resuming;
    }

    synchronized XFutureVoid continueAll() {
        int startingAbortCount = getAbortCount();
        final int contDemoCycle = ContinuousDemoCycle.get();
        final String msg = "Continoue All : " + contDemoCycle;
        logEvent(msg);
        clearStealingRobotsFlag();
        return XFuture
                .supplyAsync(msg, () -> startCheckAndEnableAllRobots(startingAbortCount), supervisorExecutorService)
                .thenCompose(x -> x).thenComposeAsyncToVoid(
                "continueAll.recurse" + contDemoCycle, ok -> checkOkElseToVoid(ok,
                        this::enableAndContinueAllActions, this::showCheckEnabledErrorSplash),
                supervisorExecutorService);
    }

    private volatile @Nullable XFuture<?> startAllFuture = null;
    private volatile StackTraceElement startAllTrace@Nullable []  = null;

    /**
     * Have all robots start their already assigned list of actions. These will
     * occur in other threads asynchronously.
     *
     * @return future allowing caller to determine when all systems have
     * completed
     */
    synchronized private XFuture<?> startAll() {
        int startingAbortCount = getAbortCount();
        checkFutures();
        logEvent("Start All ");
        clearStealingRobotsFlag();
        return startCheckAndEnableAllRobots(startingAbortCount)
                .thenComposeAsyncToVoid("startAll.recurse",
                        ok -> checkOkElse(ok, () -> startAllActions("startAll", false, null, startingAbortCount),
                                this::showCheckEnabledErrorSplash),
                        supervisorExecutorService);
    }

    /**
     * Clear all previously set errors /error states.
     */
    void clearAllErrors() {
        boolean origIgnoreTitleErrs = ignoreTitleErrors.getAndSet(true);
        for (AprsSystem aprsSystem : aprsSystems) {
            aprsSystem.clearErrors();
        }
        if (!origIgnoreTitleErrs) {
            ignoreTitleErrors.set(false);
        }
    }

    /**
     * Have all systems immediately abort regardless of the robots position or
     * the object in the gripper. Robots that have been temporarily reassigned
     * will be returned. This may require a delay that can be checked on with
     * the returned future.
     *
     * @param comment used to identify the call location information in
     * displays/logs
     * @return future allowing a check on when the abort is complete.
     */
    XFutureVoid immediateAbortAll(String comment) {
        return immediateAbortAll(comment, false);
    }

    private volatile StackTraceElement lastImmediateAbortAllTrace@Nullable []  = null;

    synchronized XFutureVoid immediateAbortAll(String comment, boolean skipLog) {
        incrementAndGetAbortCount();
        if (!keepDisabled) {
            clearStealingRobotsFlag();
        }
        final XFutureVoid initFuture;
        if(SwingUtilities.isEventDispatchThread()) {
            stopRunTimeTimerOnDisplay();
            initFuture = XFutureVoid.completedFuture();
        } else {
            initFuture = stopRunTimeTimer();
        }
        final StackTraceElement immediateAbortAllTrace[] = Thread.currentThread().getStackTrace();
        this.lastImmediateAbortAllTrace = immediateAbortAllTrace;
        final XFuture<?> lastFutureReturnedFinal = lastFutureReturned;
        if (null != lastFutureReturnedFinal) {
            if (!lastFutureReturnedFinal.isDone() && !closing) {
                System.err.println("immediateAbortAll: Cancelling lastFutureReturned=" + lastFutureReturnedFinal);
                lastFutureReturnedFinal.cancelAll(false);
            }
            lastFutureReturned = null;
        }
        if (keepDisabled) {
            cancelAll(true);
        }
        XFutureVoid abortFutures[] = new XFutureVoid[aprsSystems.size()];
        XFutureVoid xfv = initFuture;
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystem aprsSystem = aprsSystems.get(i);
            XFutureVoid nextXfv = 
                     xfv.thenComposeAsyncToVoid(() -> aprsSystem.immediateAbort(),supervisorExecutorService);
            abortFutures[i] = nextXfv;
            xfv = nextXfv;
        }
        XFutureVoid allImmediateAbortFutures = XFutureVoid
                .allOfWithName("immediateAbortAll(" + comment + ").allOf(abortFutures)", abortFutures);
        if (keepDisabled) {
            return allImmediateAbortFutures;
        }
        return allImmediateAbortFutures
                .thenComposeAsyncToVoid("immediateAbortAll(" + comment + ").after.allOf(abortFutures)", () -> {
                    return clearStealingRobots(comment, immediateAbortAllTrace, skipLog);
                }, supervisorExecutorService);
    }

    synchronized XFutureVoid publicReturnRobotStep2(String comment, boolean skipLog) {
        incrementAndGetAbortCount();
        clearStealingRobotsFlag();
        stopRunTimeTimer();
        final StackTraceElement immediateAbortAllTrace[] = Thread.currentThread().getStackTrace();
        this.lastImmediateAbortAllTrace = immediateAbortAllTrace;
        final XFuture<?> lastFutureReturnedFinal = lastFutureReturned;
        if (null != lastFutureReturnedFinal) {
            if (!lastFutureReturnedFinal.isDone() && !closing) {
                System.err.println("immediateAbortAll: Cancelling lastFutureReturned=" + lastFutureReturnedFinal);
                lastFutureReturnedFinal.cancelAll(false);
            }
            lastFutureReturned = null;
        }
        cancelAll(true);
        XFutureVoid abortFutures[] = new XFutureVoid[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystem aprsSystem = aprsSystems.get(i);
            abortFutures[i] = aprsSystem.immediateAbort();
        }
        XFutureVoid allImmediateAbortFutures = XFutureVoid
                .allOfWithName("immediateAbortAll(" + comment + ").allOf(abortFutures)", abortFutures);
        return allImmediateAbortFutures
                .thenComposeAsyncToVoid("immediateAbortAll(" + comment + ").after.allOf(abortFutures)", () -> {
                    return clearStealingRobots(comment, immediateAbortAllTrace, skipLog);
                }, supervisorExecutorService);
    }

    private XFutureVoid clearStealingRobots(String comment, final StackTraceElement[] outerClearStealingRobotsTrace,
            boolean skipLog) {
        if (this.stealingRobots || null != returnRobotFunction.get()) {
            disconnectAllNoLog();
            return returnRobots1("immediateAbortAll." + comment, null, null, -1, -1)
                    .peekException((Throwable throwable) -> {
                        System.out.println("");
                        System.out.flush();
                        System.err.println("comment = " + comment);
                        System.err.println("outerClearStealingRobotsTrace = "
                                + CRCLUtils.traceToString(outerClearStealingRobotsTrace));
                        throw new RuntimeException(throwable);
                    }).thenRun("immediateAbortAll." + comment + "afterReturnRobots", () -> {
                disconnectAllNoLog();
                if (!skipLog) {
                    logEvent("immediateAbort : " + comment);
                }
                setAbortTimeCurrent();
                stopRunTimeTimer();
            });
        } else {
            checkAllUniques();
            if (!skipLog) {
                logEvent("immediateAbort: " + comment);
            }
            setAbortTimeCurrent();
            stopRunTimeTimer();
            return XFutureVoid.completedFuture();
        }
    }

    public void cancelAll(boolean mayInterrupt) {
        String blockerList = toggleBlockerMap.keySet().toString();
        System.out.println("cancelAll : blockerList = " + blockerList);
        for (LockInfo lockInfo : toggleBlockerMap.values()) {
            System.out.println("lockInfo = " + lockInfo);
            XFutureVoid xfv = lockInfo.getFuture();
            xfv.cancelAll(mayInterrupt);
        }
        if (null != ContinuousDemoFuture) {
            ContinuousDemoFuture.cancelAll(mayInterrupt);
            ContinuousDemoFuture = null;
        }
        if (null != lastContinueContinuousDemoNoRecurseFuture) {
            lastContinueContinuousDemoNoRecurseFuture.cancelAll(mayInterrupt);
            lastContinueContinuousDemoNoRecurseFuture = null;
        }
        if (null != lastFutureReturned) {
            lastFutureReturned.cancelAll(mayInterrupt);
            clearLFR();
        }
        if (null != fillTraysAndNextRepeatingFuture) {
            fillTraysAndNextRepeatingFuture.cancelAll(true);
            fillTraysAndNextRepeatingFuture = null;
        }
        if (null != conveyorTestFuture) {
            conveyorTestFuture.cancelAll(true);
            conveyorTestFuture = null;
        }

        if (null != lastDisallowTogglesFuture) {
            lastDisallowTogglesFuture.cancelAll(true);
            lastDisallowTogglesFuture = null;
        }

        if (null != stealAbortFuture) {
            stealAbortFuture.cancelAll(mayInterrupt);
            stealAbortFuture = null;
        }

        if (null != unstealAbortFuture) {
            unstealAbortFuture.cancelAll(mayInterrupt);
            unstealAbortFuture = null;
        }
        XFutureVoid stealFuture = this.stealRobotFuture.getAndSet(null);
        if (null != stealFuture) {
            stealFuture.cancelAll(mayInterrupt);
        }

        XFutureVoid unstealFuture = this.unStealRobotFuture.getAndSet(null);
        if (null != unstealFuture) {
            unstealFuture.cancelAll(mayInterrupt);
        }

        XFutureVoid cancelStealFuture = this.cancelStealRobotFuture.getAndSet(null);
        if (null != cancelStealFuture) {
            cancelStealFuture.cancelAll(mayInterrupt);
        }

        XFutureVoid cancelUnstealFuture = this.cancelUnStealRobotFuture.getAndSet(null);
        if (null != cancelUnstealFuture) {
            cancelUnstealFuture.cancelAll(mayInterrupt);
        }
        if (null != randomTestFuture) {
            randomTestFuture.cancelAll(mayInterrupt);
            randomTestFuture = null;
        }
        TeachScanMonitor oldMonitor = lastCompleteScanTillNewInternalTeachScanMonitor;
        if (null != oldMonitor) {
            oldMonitor.stop();
            lastCompleteScanTillNewInternalTeachScanMonitor = null;
        }
        if (null != executeUnstealRobotsFuture) {
            executeUnstealRobotsFuture.cancelAll(false);
            executeUnstealRobotsFuture = null;
        }
        cancelAllFuturesSet();
    }

    private volatile boolean restoringOrigRobotInfo = false;

    synchronized XFutureVoid restoreOrigRobotInfo() {
        if (keepDisabled) {
            return XFutureVoid.completedFuture();
        }
        restoringOrigRobotInfo = true;
        setBlockConveyorMoves("restoreOrigRobotInfo");
        logEvent("Starting restoreOrigRobotInfo");
        Function<Integer, XFutureVoid> function = unStealRobotsFunction.getAndSet(null);
        if (null != function) {
            println("unStealRobotsSupplier contained" + function);
        }
        clearStealingRobotsFlag();
        enableChangeCount.incrementAndGet();
        disconnectAllNoLog();
        List<XFutureVoid> futuresList = new ArrayList<>();
        for (AprsSystem aprsSys : aprsSystems) {
            XFutureVoid restoreFuture = aprsSys.restoreOrigRobotInfo().thenRun(() -> {
                logEvent("restoreOrigRobotInfo : aprsSys=" + aprsSys);
            });
            futuresList.add(restoreFuture);
        }
        return XFutureVoid.allOfWithName("restoreOrigRobotInfo.allOf(futuresList)", futuresList)
                .thenRunAsync("complete restoreOrigRobotInfo", () -> {
                    logEvent("Completing restoreOrigRobotInfo");
                    restoringOrigRobotInfo = false;
                    clearBlockConveryorMoves();
                }, supervisorExecutorService);
    }

    /**
     * Connect to all robots.
     */
    XFutureVoid connectAll() {
        try {
            initColorTextSocket();
        } catch (IOException ex) {
            log(Level.SEVERE, "", ex);
        }
        boolean globalPause = isPauseSelected();
        Map<String, Boolean> wasConnectedMap = new HashMap<>();
        Map<String, Boolean> wasEnabledMap = new HashMap<>();
        Map<String, String> sysRobotMap = new HashMap<>();
        Map<String, Integer> sysRobotPortMap = new HashMap<>();
        for (AprsSystem aprsSys : aprsSystems) {
            final String taskName = aprsSys.getTaskName();
            wasConnectedMap.put(taskName, aprsSys.isConnected());
            final String robotName = requireNonNull(aprsSys.getRobotName(), "aprsSys.getRobotName()");
            final String robotTaskName = requireNonNull(robotTaskMap.get(robotName),
                    "robotTaskMap.get(" + robotName + ")");
            sysRobotMap.put(taskName, robotName);
            wasEnabledMap.put(taskName, robotTaskName != null);
            sysRobotPortMap.put(taskName, aprsSys.getRobotCrclPort());
        }
        logEvent("connectAll: robotTaskMap=" + robotTaskMap + ", wasConnectedMap=" + wasConnectedMap
                + ", sysRobotPortMap=" + sysRobotPortMap);
        for (AprsSystem aprsSys : aprsSystems) {
            final String robotName = requireNonNull(aprsSys.getRobotName(), "aprsSys.getRobotName()");
            final String robotTaskName = robotTaskMap.get(robotName);
            final boolean canConnect = (!keepDisabled)
                    || (robotTaskName != null && robotTaskName.equals(aprsSys.getTaskName()));
            if (!aprsSys.isConnected()) {
                if (canConnect) {
                    aprsSys.setConnected(true);
                } else {
                    if (skipDisabled) {
                        continue;
                    }
                    throw new RuntimeException(aprsSys.getRobotName() + " must be enabled to continue.");
                }
            }
            if (aprsSys.isPaused() && !globalPause && canConnect) {
                aprsSys.resume();
            } else if (!aprsSys.isPaused() && globalPause) {
                aprsSys.pause();
            }
        }
        try {
            checkAllUniques();
        } catch (Exception ex) {
            System.out.println("wasConnectedMap = " + wasConnectedMap);
            System.out.println("wasEnabledMap = " + wasEnabledMap);
            System.out.println("sysRobotMap = " + sysRobotMap);
            System.out.println("sysRobotPortMap = " + sysRobotPortMap);
            throw new RuntimeException(ex);
        }
        return runOnDispatchThread(() -> {
            if (logEventErrCount.get() == 0) {
                setIconImageOnDisplay(IconImages.BASE_IMAGE);
            }
        });
    }

    private final AtomicInteger safeAbortAllCount = new AtomicInteger();

    /**
     * Have all robots abort their actions after any part they are holding has
     * been dropped off and the robot has been moved out of the way of the
     * vision system.
     *
     * @return future allowing caller to determine when the abort is complete
     */
    private XFutureVoid safeAbortAll() {
        int saaCount = safeAbortAllCount.incrementAndGet();
        String blocker = "safeAbortAll" + saaCount;
        AprsSystem sysArray[] = systems().toArray(new AprsSystem[0]);
        XFuture<LockInfo> disallowTogglesFuture = disallowToggles(blocker, sysArray);
        incrementAndGetAbortCount();
        logEvent("safeAbortAll");
        XFuture<?> prevLastFuture = lastFutureReturned;
        lastFutureReturned = null;
        XFuture<?> futures[] = new XFuture<?>[aprsSystems.size()];

        XFutureVoid abortAllFuture = disallowTogglesFuture.thenComposeAsyncToVoid(x -> {
            for (int i = 0; i < aprsSystems.size(); i++) {
                AprsSystem sys = aprsSystems.get(i);
                futures[i] = sys.startSafeAbort("safeAbortAll")
                        .thenRun(() -> logEvent("safeAbort completed for " + sys + " (part of safeAbortAll)"));
            }
            return XFuture.allOfWithName("safeAbortAll", futures);
        }, supervisorExecutorService);
        XFutureVoid f2 = abortAllFuture.thenComposeAsync((Function<Void, XFuture<@Nullable Void>>) x -> {
            logEvent("safeAbortAll: all systems aborted. calling return robots: futures =" + Arrays.toString(futures));
            return returnRobots1("safeAbortAll", null, null, -1, -1);
        }, supervisorExecutorService).thenRunAsync(() -> {
            XFutureVoid cancelSRF = this.cancelStealRobotFuture.get();
            if (null != cancelSRF) {
                cancelSRF.complete();
            }
            XFutureVoid cancelUSRF = this.cancelUnStealRobotFuture.get();
            if (null != cancelUSRF) {
                cancelUSRF.complete();
            }
        }, supervisorExecutorService).thenRunAsync(() -> {
            if (null != prevLastFuture) {
                prevLastFuture.cancelAll(false);
            }
            logEvent("safeAbortAll completed");
        }, supervisorExecutorService);
        return f2.alwaysComposeToVoid(() -> allowToggles(blocker, sysArray));
    }

    public @Nullable
    NamedFunction<Integer, XFutureVoid> getSafeAbortReturnRobot() {
        return safeAbortReturnRobot;
    }

    public void setSafeAbortReturnRobot(NamedFunction<Integer, XFutureVoid> safeAbortReturnRobot) {
        this.safeAbortReturnRobot = safeAbortReturnRobot;
    }

    private volatile StackTraceElement incrementAndGetAbortCountTrace@Nullable []  = null;

    @SuppressWarnings("UnusedReturnValue")
    private int incrementAndGetAbortCount() {
        incrementAndGetAbortCountTrace = Thread.currentThread().getStackTrace();
        return abortCount.incrementAndGet();
    }

    public int getAbortCount() {
        return abortCount.get();
    }

    private @Nullable File setupFile;

    public XFutureVoid setTitleMessage(String message) {
        if (null != displayJFrame) {
            return displayJFrame.setTitleMessage(message, this.setupFile);
        } else {
            return XFutureVoid.completedFuture();
        }
    }

    @UIEffect
    public void setTitleMessageOnDisplay(String message) {
        assert  SwingUtilities.isEventDispatchThread();
        if (null != displayJFrame) {
            displayJFrame.setTitleMessageOnDisplay(message, this.setupFile);
        } 
    }
    
    /**
     * Set the value of setupFile
     *
     * @param f new value of setupFile
     * @throws java.io.IOException can not save last setup file
     */
    void setSetupFile(File f) throws IOException {
        boolean oldFileEquals = !Objects.equals(this.setupFile, f);
        if (null != f) {
            saveLastSetupFile(f);
        }
        this.setupFile = f;
        setSaveSetupEnabled(f != null);
        if (oldFileEquals) {
            setTitleMessage("");
        }
    }

    private void setSaveSetupEnabled(boolean enabled) {
        if (null != displayJFrame) {
            displayJFrame.setSaveSetupEnabled(enabled);
        }
    }

    /**
     * Save the current setup to the last saved/read setup file.
     */
    private void saveCurrentSetup() {
        try {
            File fileToSave = this.setupFile;
            if (null != fileToSave) {
                saveSetupFile(fileToSave);
            }
        } catch (IOException ex) {
            log(Level.SEVERE, "", ex);
        }
    }

    /**
     * Save the current setup to the given file.
     *
     * @param f file to save setup to
     * @throws IOException file can not be written to
     */
    public void saveSetupFile(File f) throws IOException {
        if (null == propertiesFile) {
            propertiesFile = Utils.file(f.getParentFile(), "supervisor_properties.txt");
        }
        saveProperties(this.propertiesFile);
        saveCachedTable(f, tasksCachedTable,
                Arrays.asList(-99, "supervisor", "supervisor", fileToRelString(propertiesFile, f)),
                Arrays.asList(0, 1, 2, 6));

        setSetupFile(f);
    }

    private String fileToRelString(File inputFile, File referenceFile) throws IOException {
        if (null == inputFile) {
            throw new IllegalArgumentException("null===o");
        }
        if (null == referenceFile) {
            return inputFile.toString();
        }
        File parentFile = referenceFile.getParentFile();
        if (null != parentFile) {
            final String oCanonicalPath = inputFile.getCanonicalPath();
            Path rel = parentFile.toPath().toRealPath().relativize(Paths.get(oCanonicalPath)).normalize();
            if (rel.toString().length() < oCanonicalPath.length()) {
                return rel.toString();
            } else {
                return inputFile.toString();
            }
        } else {
            return inputFile.toString();
        }
    }

    private void saveCachedTable(File f, CachedTable cachedTable, List<Object> firstRecord,
            Iterable<Integer> columnIndexes) throws IOException {
        String headers[] = tableHeaders(cachedTable, columnIndexes);
        CSVFormat format = CSVFormat.DEFAULT.withHeader(headers);
        try (CSVPrinter printer = new CSVPrinter(new PrintStream(new FileOutputStream(f)), format)) {
            printer.printRecord(firstRecord);
            for (int i = 0; i < cachedTable.getRowCount(); i++) {
                List<Object> l = new ArrayList<>();
                for (Integer colIndex : columnIndexes) {
                    if (null == colIndex) {
                        continue;
                    }
                    int j = colIndex;
                    if (j > cachedTable.getColumnCount()) {
                        break;
                    }
                    Object o = cachedTable.getValueAt(i, j);
                    if (o instanceof File) {
                        l.add(fileToRelString((File) o, f));
                    } else if (o != null) {
                        l.add(o);
                    } else {
                        l.add("");
                    }
                }
                printer.printRecord(l);
            }
        }
    }

    private void saveCachedTable(File f, CachedTable cachedTable, CSVFormat csvFormat) throws IOException {
        try (CSVPrinter printer = new CSVPrinter(new PrintStream(new FileOutputStream(f)), csvFormat)) {
            for (int i = 0; i < cachedTable.getRowCount(); i++) {
                List<Object> l = new ArrayList<>();
                for (int j = 0; j < cachedTable.getColumnCount(); j++) {
                    if (j == 3) {
                        continue;
                    }
                    Object o = cachedTable.getValueAt(i, j);
                    if (o instanceof File) {
                        File parentFile = f.getParentFile();
                        if (null != parentFile) {
                            Path rel = parentFile.toPath().toRealPath()
                                    .relativize(Paths.get(((File) o).getCanonicalPath())).normalize();
                            if (rel.toString().length() < ((File) o).getCanonicalPath().length()) {
                                l.add(rel);
                            } else {
                                l.add(o);
                            }
                        } else {
                            l.add(o);
                        }
                    } else if (null == o) {
                        l.add("");
                    } else {
                        l.add(o);
                    }
                }
                printer.printRecord(l);
            }
        }
    }

    /**
     * Save the posmaps to the given file. The posmap file is a CSV file that
     * points to other csv files with infomation needed to transform coordinates
     * from one robot to another.
     *
     * @param f file to safe posmaps in
     * @throws IOException file could not be written to
     */
    public void savePositionMaps(File f) throws IOException {
        saveCachedTable(f, positionMappingsFilesCachedTable, CSVFormat.RFC4180);
        saveLastPositionMappingsFilesFile(f);
    }

    private final Map<String, Map<String, File>> posMaps = new HashMap<>();

    /**
     * Get the file location where data is stored for converting positions from
     * sys1 to sys2. The file is a CSV file.
     *
     * @param sys1 system to convert positions from
     * @param sys2 system to convert positions to
     * @return file for converting positions
     */
    File getPosMapFile(String sys1, String sys2) throws FileNotFoundException {
        Map<String, File> subMap = posMaps.get(sys1);
        if (null == subMap) {
            throw new IllegalStateException("no subMap for system " + sys1 + " in " + posMaps);
        }
        File f = subMap.get(sys2);
        if (null == f) {
            throw new IllegalStateException("no entry  for system " + sys2 + " in " + subMap);
        }
        if (!f.exists()) {
            throw new FileNotFoundException(f + " does not exist. failing for getPosMapFile " + sys1 + " to " + sys2);
        }
        lastPosMapFile = f;
        return f;
    }

    /**
     * Set the file location where data is stored for converting positions from
     * sys1 to sys2. The file is a CSV file.
     *
     * @param sys1 system to convert positions from
     * @param sys2 system to convert positions to
     * @param f new file location
     */
    private void setPosMapFile(String sys1, String sys2, File f) {
        Map<String, File> subMap = posMaps.computeIfAbsent(sys1, k -> new HashMap<>());
        subMap.put(sys2, f);
    }

    /**
     * Load posmaps from the given file.
     *
     * @param f file to load
     * @throws IOException file could not be read
     */
    final void loadPositionMappingsFilesFile(File f) throws IOException {
        println("Loading position mappings files  file :" + f.getCanonicalPath());
        positionMappingsFilesCachedTable.setRowColumnCount(0, 0);
        ;
        positionMappingsFilesCachedTable.addColumn("System");
        try (CSVParser parser = CSVParser.parse(f, Charset.defaultCharset(), CSVFormat.RFC4180)) {
            String line = null;
            int linecount = 0;
            for (CSVRecord csvRecord : parser) {
                linecount++;
                String a[] = new String[csvRecord.size()];
                for (int i = 0; i < a.length; i++) {
                    a[i] = csvRecord.get(i);
                }
                positionMappingsFilesCachedTable.addColumn(a[0]);
            }
        }
        try (CSVParser parser = CSVParser.parse(f, Charset.defaultCharset(), CSVFormat.RFC4180)) {
            int linecount = 0;
            for (CSVRecord csvRecord : parser) {
                linecount++;
                Object a[] = new Object[csvRecord.size()];
                for (int i = 0; i < a.length; i++) {
                    a[i] = csvRecord.get(i);
                    if (null != a[i] && !"null".equals(a[i]) && !"".equals(a[i])) {
                        if (i > 0) {
                            String fname = (String) a[i];
                            File fi = resolveFile(fname, f.getParentFile());
                            setPosMapFile((String) a[0], positionMappingsFilesCachedTable.getColumnName(i), fi);
                        }
                    }
                }
                positionMappingsFilesCachedTable.addRow(a);
            }
        }
        saveLastPositionMappingsFilesFile(f);
    }

    private static File resolveFile(String fname, @Nullable File dir) throws IOException {
        File fi = Utils.file(fname);
        if (!fi.exists() && dir != null && dir.exists() && dir.isDirectory()) {
            File altFile = dir.toPath().toRealPath().resolve(fname).toFile();
            if (altFile.exists()) {
                fi = altFile;
            }
        }
        return fi;
    }

    private @Nullable File lastSetupFile = null;

    private @Nullable
    File getSetupParent() {
        if (null != setupFile) {
            return setupFile.getParentFile();
        } else {
            return null;
        }
    }

    private void saveLastSetupFile(File f) throws IOException {
        lastSetupFile = f;
        savePathInLastFileFile(f, LAST_SETUP_FILE_FILE, f.getParentFile());
    }

    static private synchronized void savePathInLastFileFile(File f, File lastFileFile, @Nullable File setupParent)
            throws IOException {
        String newFilePath = f.getCanonicalPath();
        if (null != newFilePath && newFilePath.length() > 0) {
            String oldPath = null;
            try {
                File oldFile = readPathFromFileFile(lastFileFile, null);
                if (null != oldFile) {
                    oldPath = oldFile.getCanonicalPath();
                }
            } catch (Exception ignored) {
            }
            if (!Objects.equals(oldPath, newFilePath)) {
                try {
                    println("oldPath = " + oldPath);
                    println("newFilePath = " + newFilePath);
                    println("lastFileFile = " + lastFileFile);
                    try (FileWriter fileWriter = new FileWriter(
                            Utils.file(Utils.getAprsUserHomeDir(), ".lastFileChanges"), true);
                            PrintWriter pw = new PrintWriter(fileWriter, true)) {
                        pw.println("date=" + new Date() + ", oldPath = " + oldPath + ", newFilePath = " + newFilePath
                                + ", lastFileFile = " + lastFileFile);

                    }
                } catch (Exception ignored) {
                }
                try (PrintWriter pw = new PrintWriter(new FileWriter(lastFileFile))) {
                    pw.println(newFilePath);
                }
            }
        }
        if (null != setupParent) {
            String lastFileFileName = lastFileFile.getName();
            File savePathFile = Utils.file(setupParent, lastFileFileName);
            saveRelativePathInLastFileFile(f, savePathFile);
        }
    }

    static private synchronized void saveRelativePathInLastFileFile(File f, File lastFileFile) throws IOException {
        String newFilePath = f.getCanonicalPath();
        String relativeNewFilePath = newFilePath;
        File parentFile = lastFileFile.getParentFile();
        if (null == parentFile) {
            return;
        }
        String lastFileFileParentPath = parentFile.getCanonicalPath();
        if (relativeNewFilePath.startsWith(lastFileFileParentPath)) {
            relativeNewFilePath = relativeNewFilePath.substring(lastFileFileParentPath.length());
            if (relativeNewFilePath.startsWith("/") || relativeNewFilePath.startsWith("\\")) {
                relativeNewFilePath = relativeNewFilePath.substring(1);
            }
        }
        if (null != newFilePath && newFilePath.length() > 0) {
            String oldPath = null;
            try {
                File oldFile = readRelativePathFromFileFile(lastFileFile);
                if (null != oldFile) {
                    oldPath = oldFile.getCanonicalPath();
                }
            } catch (Exception ignored) {
            }
            if (!Objects.equals(oldPath, newFilePath)) {
                try {
                    println("oldPath = " + oldPath);
                    println("newFilePath = " + newFilePath);
                    println("lastFileFile = " + lastFileFile);
                    try (PrintWriter pw = new PrintWriter(
                            new FileWriter(Utils.file(Utils.getAprsUserHomeDir(), ".lastFileChanges")), true)) {
                        pw.println("date=" + new Date() + ", oldPath = " + oldPath + ", newFilePath = " + newFilePath
                                + ", lastFileFile = " + lastFileFile);
                    }
                } catch (Exception ignored) {
                }
                try (PrintWriter pw = new PrintWriter(new FileWriter(lastFileFile))) {
                    pw.println(relativeNewFilePath);
                }
            }
        }
    }

    private ProcessLauncherJFrame processLauncher;

    /**
     * Get the value of processLauncher
     *
     * @return the value of processLauncher
     */
    ProcessLauncherJFrame getProcessLauncher() {
        return processLauncher;
    }

    /**
     * Set the value of processLauncher
     *
     * @param processLauncher new value of processLauncher
     */
    public void setProcessLauncher(ProcessLauncherJFrame processLauncher) {
        this.processLauncher = processLauncher;
        runOnDispatchThread(() -> processLauncher.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE));
        processLauncher.addOnCloseRunnable(this::close);
    }

    private @Nullable File lastSimTeachFile = null;

    void saveSimTeach(File f) throws IOException {
        object2DOuterJPanel1.saveCsvItemsFile(f);
        saveLastSimTeachFile(f);
    }

    void loadSimTeach(File f) throws IOException {
        object2DOuterJPanel1.loadFile(f);
        saveLastSimTeachFile(f);
    }

    private void saveLastSimTeachFile(File f) throws IOException {
        lastSimTeachFile = f;
        String lastFileName = LAST_SIM_TEACH_FILE_FILE.getName();
        savePathInLastFileFile(f, LAST_SIM_TEACH_FILE_FILE, getSetupParent());
        if (null != setupFile) {
            File setupParent = setupFile.getParentFile();
            if (null != setupParent) {
                File savePathFile = Utils.file(setupParent, lastFileName);
                saveRelativePathInLastFileFile(f, savePathFile);
            }
        }
    }

    private @Nullable File lastSharedToolsFile = null;

    private void saveLastSharedToolsFile(File f) throws IOException {
        lastSharedToolsFile = f;
        savePathInLastFileFile(f, LAST_SHARED_TOOLS_FILE_FILE, getSetupParent());
    }

    public void saveSharedTools(File f) throws IOException {
        Utils.saveCachedTable(f, sharedToolCachedTable);
        sharedToolsFile = f;
        saveLastSharedToolsFile(f);
    }

    public void loadSharedTools(File f) throws IOException {
        Utils.readCsvFileToTable(sharedToolCachedTable, f);
        sharedToolsFile = f;
        saveLastSharedToolsFile(f);
    }

    private volatile @Nullable File lastPositionMappingsFilesFile = null;

    private volatile @Nullable File lastPosMapFile = null;

    public @Nullable
    File getLastPosMapFile() {
        return lastPosMapFile;
    }

    public void plotLastPosMapFile() {
        if (null != lastPosMapFile) {
            try {
                PositionMap pm = new PositionMap(lastPosMapFile);
                pm.plot();
            } catch (Exception ex) {
                Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    void saveLastPositionMappingsFilesFile(File f) throws IOException {
        lastPositionMappingsFilesFile = f;
        savePathInLastFileFile(f, LAST_POSMAP_FILE_FILE, getSetupParent());
    }

    public File getLastPosMapParent() {
        if (null == lastPosMapFile) {
            throw new IllegalStateException("lastPosMapFile is null");
        }
        File parentFile = lastPosMapFile.getParentFile();
        if (null == parentFile) {
            throw new IllegalStateException("PosMapFile " + lastPosMapFile + " does not have parent");
        }
        return parentFile;
    }

    public XFutureVoid performStartAllAction() {
        immediateAbortAll("performStartAllAction");
        clearEventLog();
        return connectAll().thenComposeAsyncToVoid(this::enableAllRobots, supervisorExecutorService)
                .thenComposeAsyncToVoid(this::completePeformStartAction, supervisorExecutorService);
    }

    private XFutureVoid completePeformStartAction() {
        XFuture<?> peformStartAllFuture = startAll();
        setLastFunctionReturned(peformStartAllFuture);
        setMainFuture(peformStartAllFuture);
        XFuture<?> xf = peformStartAllFuture;
        return xf.thenRunAsync("showStartAllProfiles", () -> {
            try {
                if (isKeepAndDisplayXFutureProfilesSelected()) {
                    File profileFile = Utils.createTempFile("startAll_profile_", ".csv");
                    try (PrintStream ps = new PrintStream(new FileOutputStream(profileFile))) {
                        xf.printProfile(ps);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, supervisorExecutorService)
                .thenComposeAsyncToVoid(x -> showAllTasksCompleteDisplay(), supervisorExecutorService);
    }

    XFutureVoid performReverseStartAllAction() {
        immediateAbortAll("performStartAllAction");
        clearEventLog();
        return connectAll().thenComposeAsyncToVoid(this::enableAllRobots, supervisorExecutorService)
                .thenComposeAsyncToVoid(this::completePeformReverseStartAction, supervisorExecutorService);
    }

    private XFutureVoid completePeformReverseStartAction() {
        int startingAbortCount = getAbortCount();
        XFuture<?> startReverseActionsFuture = startReverseActions(null, startingAbortCount);
        setLastFunctionReturned(startReverseActionsFuture);
        setMainFuture(startReverseActionsFuture);
        XFuture<?> xf = startReverseActionsFuture;
        return xf.thenRunAsync("showStartAllProfiles", () -> {
            try {
                if (isKeepAndDisplayXFutureProfilesSelected()) {
                    File profileFile = Utils.createTempFile("startAll_profile_", ".csv");
                    try (PrintStream ps = new PrintStream(new FileOutputStream(profileFile))) {
                        xf.printProfile(ps);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, supervisorExecutorService).thenComposeAsyncToVoid(x -> showAllTasksCompleteDisplay(), supervisorExecutorService);
    }

    void performRemoveSelectedSystemAction(int selectedIndex) {
        if (selectedIndex >= 0 && selectedIndex < aprsSystems.size()) {
            try {
                AprsSystem aj = aprsSystems.remove(selectedIndex);
                try {
                    aj.setOnCloseRunnable(null);
                    aj.close();
                } catch (Exception ex) {
                    log(Level.SEVERE, "", ex);
                }
                updateTasksTable();
                updateRobotsTable().thenRun(this::saveCurrentSetup);
            } catch (Exception ex) {
                log(Level.SEVERE, "", ex);
            }
        }
    }

    private volatile StackTraceElement lastPerformSafeAbortAllActionTrace@Nullable []  = null;

    private XFutureVoid waitTimeoutOrAllOthers(int timeout, @Nullable XFutureVoid... otherFutures) {
        XFutureVoid timeoutFuture = new XFutureVoid("timeoutfuture");
        AtomicBoolean allOfCompleted = new AtomicBoolean(false);
        List<XFutureVoid> otherFuturesList = new ArrayList<>();

        for (XFutureVoid xfv : otherFutures) {
            if (null != xfv && !xfv.isDone()) {
                otherFuturesList.add(xfv);
            }
        }
        if (otherFuturesList.isEmpty()) {
            final String msg = "waitTimeoutOrAllOthers otherFuturesList.isEmpty()";
            logEvent(msg);
            return XFutureVoid.completedFutureWithName(msg);
        }
        Thread timeoutThread = new Thread(() -> {
            try {
                if (allOfCompleted.get()) {
                    return;
                }
                Thread.sleep(timeout);
                if (allOfCompleted.get()) {
                    return;
                }
                logEvent("timeout occured otherFuturesList=" + otherFuturesList);
                if (allOfCompleted.get()) {
                    return;
                }
                for (XFutureVoid xfv : otherFuturesList) {
                    System.out.println("xfv = " + xfv);
                    if (null != xfv && !xfv.isDone()) {
                        xfv.printProfile();
                        xfv.printStatus();
//                        for(AprsSystem sys : aprsSystems) {
//                            sys.debugAction();
//                            XFuture<Boolean> sysxfb = sys.getContinueActionListFuture();
//                            System.out.println("sysxfb = " + sysxfb);
//                        }
//                        System.out.println("aprsSystems.get(0).getContinueActionListFuture() = " + aprsSystems.get(0).getContinueActionListFuture());
//                        System.out.println("aprsSystems.get(1).getContinueActionListFuture() = " + aprsSystems.get(1).getContinueActionListFuture());
//                        System.out.println("lastStealRobotsInternalPart1 = " + lastStealRobotsInternalPart1);
//                        System.out.println("lastStealRobotsInternalPart2 = " + lastStealRobotsInternalPart2);
//                        System.out.println("lastStealRobotsInternalPart3 = " + lastStealRobotsInternalPart3);
//                        System.out.println("lastStealRobotsInternalPart4 = " + lastStealRobotsInternalPart4);
//                        System.out.println("lastStealRobotsInternalPart5 = " + lastStealRobotsInternalPart5);
//                        System.out.println("lastStealRobotsInternalPart6 = " + lastStealRobotsInternalPart6);
//                        System.out.println("lastStealRobotsInternalPart7 = " + lastStealRobotsInternalPart7);
//                        System.out.println("lastStealRobotsInternalPart8 = " + lastStealRobotsInternalPart8);
                    }
                }
                timeoutFuture.complete();
            } catch (InterruptedException ex) {
                if (allOfCompleted.get()) {
                    return;
                }
                Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }
        }, "timeoutThread");
        timeoutThread.start();

        final XFutureVoid allOfXf = XFutureVoid.allOf(otherFuturesList);
        allOfXf.thenRun(() -> {
            allOfCompleted.set(true);
            logEvent("timeout occured otherFuturesList=" + otherFuturesList);
            timeoutThread.interrupt();
        });
        return XFutureVoid.anyOf(timeoutFuture, allOfXf);
    }

    void performSafeAbortAllAction() {
        lastPerformSafeAbortAllActionTrace = Thread.currentThread().getStackTrace();
        incrementAndGetAbortCount();
        safeAbortReturnRobot = getReturnRobotNamedCallable();
        logEvent("User requested safeAbortAll : safeAbortReturnRobot=" + safeAbortReturnRobot);
        XFutureVoid rf = randomTestFuture;
        if (null != rf) {
            rf.cancelAll(false);
        }
        XFutureVoid f = waitTogglesAllowed().thenComposeAsyncToVoid(x -> safeAbortAll(), supervisorExecutorService)
                .thenComposeAsyncToVoid(
                        () -> {
                            return waitTimeoutOrAllOthers(10000, ContinuousDemoFuture, stealRobotFuture.getAndSet(null), unStealRobotFuture.getAndSet(null));
                        }, supervisorExecutorService);
        setLastSafeAbortAllFuture(f);

        XFutureVoid f2 = f.alwaysComposeAsyncToVoid(() -> {
            int startingAbortCount2 = getAbortCount();
            if (null != safeAbortReturnRobot) {
                try {
                    return safeAbortReturnRobot.apply(null).thenComposeAsyncToVoid(
                            () -> showSafeAbortComplete(startingAbortCount2), supervisorExecutorService);
                } catch (Exception ex) {
                    Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", ex);
                    if (ex instanceof RuntimeException) {
                        throw (RuntimeException) ex;
                    } else {
                        throw new RuntimeException(ex);
                    }
                }
            } else {
                return showSafeAbortComplete(startingAbortCount2);
            }
        }, supervisorExecutorService);
        clearCheckBoxes();
        setMainFuture(f2);
    }

    private final AtomicInteger showSafeAbortCount = new AtomicInteger();
    private volatile StackTraceElement lastShowSafeAbortCompleteTrace@Nullable []  = null;

    private XFutureVoid showSafeAbortComplete(int startingAbortCount) {
        logEvent("showSafeAbortComplete.");
        int currentAbortCount = getAbortCount();
        if (startingAbortCount != currentAbortCount) {
            String msg = "showSafeAbortComplete quiting : startingAbortCount=" + startingAbortCount
                    + ", currentAbortCount=" + currentAbortCount;
            logEvent(msg);
//            System.err.println("incrementAndGetAbortCountTrace = " + Utils.traceToString(incrementAndGetAbortCountTrace) + ", msg=" + msg);
            XFutureVoid ret2 = XFutureVoid.completedFutureWithName(msg);
            return ret2;
        }
        if (null != displayJFrame && !closing) {
            lastShowSafeAbortCompleteTrace = Thread.currentThread().getStackTrace();
            AprsSupervisorDisplayJFrame displayJFrameLocal = displayJFrame;
            String blockerName = "showSafeAbortComplete" + showSafeAbortCount.incrementAndGet();
            AprsSystem systems[] = aprsSystems.toArray(new AprsSystem[0]);
            XFuture<LockInfo> f = disallowToggles(blockerName, systems);
            return f.thenComposeAsyncToVoid(x -> displayJFrameLocal.showSafeAbortComplete(), supervisorExecutorService)
                    .alwaysComposeToVoid(() -> allowTogglesInternal(blockerName, false, systems));
        } else {
            return XFutureVoid.completedFuture();
        }
    }

    private @Nullable File lastTeachPropsFile = null;

    private void saveLastTeachPropsFile(File f) {
        try {
            lastTeachPropsFile = f;
            savePathInLastFileFile(f, LAST_SIM_TEACH_PROPERTIES_FILE_FILE, getSetupParent());
        } catch (IOException ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    XFutureVoid saveTeachProps(File f) throws IOException {
        object2DOuterJPanel1.setPropertiesFile(f);
        return object2DOuterJPanel1.saveProperties().thenRun(() -> saveLastTeachPropsFile(f));
    }

    XFutureVoid loadTeachProps(File f) throws IOException {
        object2DOuterJPanel1.setPropertiesFile(f);
        return runOnDispatchThread(() -> {
            try {
                object2DOuterJPanel1.loadPropertiesOnDisplay();
                saveLastTeachPropsFile(f);
            } catch (Exception ex) {
                Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", ex);
                throw new RuntimeException(ex);
            }
        });
    }

    private final List<AprsSystem> aprsSystems = new ArrayList<>();

    /**
     * Get the value of aprsSystems
     *
     * @return the value of aprsSystems
     */
    public List<AprsSystem> systems() {
        return Collections.unmodifiableList(aprsSystems);
    }

    private volatile boolean preClosing = false;

    public boolean isPreClosing() {
        return preClosing;
    }

    /**
     * Close all systems.
     */
    private void preCloseAllAprsSystems() {
        preClosing = true;
        debugAction();
        System.out.println("preClosing = " + preClosing);
        List<AprsSystem> aprsSystemsCopy = new ArrayList<>(aprsSystems);
        for (AprsSystem aprsSystemInterface : aprsSystemsCopy) {
            System.out.println("aprsSystemInterface = " + aprsSystemInterface);
            try {
                aprsSystemInterface.setCrclClientPreClosing(true);
            } catch (Exception ex) {
                log(Level.SEVERE, "", ex);
            }
        }
    }

    /**
     * Close all systems.
     */
    private XFutureVoid closeAllAprsSystems() {
        System.out.println("Supervisor.closeAllAprsSystems: aprsSystems = " + aprsSystems);
        if (aprsSystems.isEmpty()) {
            return XFutureVoid.completedFutureWithName("closeAllAprsSystems.aprsSystems=" + aprsSystems);
        }
        List<AprsSystem> aprsSystemsCopy = new ArrayList<>(aprsSystems);
        aprsSystems.clear();
        for (AprsSystem aprsSystemInterface : aprsSystemsCopy) {
            try {
                aprsSystemInterface.setOnCloseRunnable(null);
                aprsSystemInterface.close();

            } catch (Exception ex) {
                log(Level.SEVERE, "", ex);
            }
        }
        updateTasksTable();
        return updateRobotsTable();
    }

    @SuppressWarnings("guieffect")
    public static Supervisor createSupervisor() {
        return CRCLUtils.isGraphicsEnvironmentHeadless() ? new Supervisor()
                : createAprsSupervisorWithSwingDisplay(true);
    }

    /**
     * Load the given setup file.
     *
     * @param f setup file to load
     * @throws IOException file could not be read
     */
    public final void loadSharedToolsFile(File f) throws IOException {
        Utils.readCsvFileToTable(sharedToolCachedTable, f);
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

    public final XFutureVoid loadProperties() throws IOException {
        return loadPropertiesFile(this.propertiesFile);
    }

    private static final ConcurrentLinkedDeque<StackTraceElement[]> loadPropertiesTraces = new ConcurrentLinkedDeque<>();

    public XFutureVoid loadPropertiesFile(File propertiesFile) throws IOException {
        this.propertiesFile = propertiesFile;

        List<StackTraceElement[]> loadPropertiesTracesCopy = new ArrayList<>(loadPropertiesTraces);
        loadPropertiesTraces.add(Thread.currentThread().getStackTrace());
        IOException exA[] = new IOException[1];
        final Properties props = new Properties();
        println("Supervisot loading properties from " + propertiesFile.getCanonicalPath());
        try (FileReader fr = new FileReader(propertiesFile)) {
            props.load(fr);
        }
        try {
            if (Utils.isEventDispatchThread() || CRCLUtils.isGraphicsEnvironmentHeadless()) {
                loadPropertiesOnDisplay(props);
                return XFutureVoid.completedFuture();
            }
            XFutureVoid ret = Utils.runOnDispatchThread(() -> {
                loadPropertiesOnDisplay(props);
            });
            return ret;
        } catch (Exception ex) {
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }

    public XFutureVoid saveProperties() throws IOException {
        return saveProperties(this.propertiesFile);
    }

    public XFutureVoid saveProperties(File propertiesFile) throws IOException {
        this.propertiesFile = propertiesFile;
        File propsParent = propertiesFile.getParentFile();
        List<XFutureVoid> futures = new ArrayList<>();
        if (propsParent == null) {
            System.err.println("propertiesFile.getParentFile() returned null : propertiesFile=" + propertiesFile);
            return XFutureVoid.completedFuture();
        }
        if (!propsParent.exists()) {
            println("Directory " + propsParent + " does not exist. (Creating it now.)");
            propsParent.mkdirs();
        }
        Map<String, String> propsMap = new HashMap<>();
        if (null != displayJFrame) {
            propsMap.putAll(displayJFrame.getPropertiesMap());
        }
        propsMap.put("correctionMode", Boolean.toString(isCorrectionMode()));
        if (null != this.conveyorClonedViewSystemTaskName) {
            propsMap.put("conveyorClonedViewSystemTaskName", conveyorClonedViewSystemTaskName);
        }
        propsMap.put("enableConveyor", Boolean.toString(enableConveyor));
        if (null != this.conveyorTestObjectViewSimulatedFilePath) {
            propsMap.put("conveyorTestObjectViewSimulatedFilePath", conveyorTestObjectViewSimulatedFilePath);
        }
        Properties props = new Properties();
        props.putAll(propsMap);
        println("AprsSystem saving properties to " + propertiesFile.getCanonicalPath());
        Utils.saveProperties(propertiesFile, props);
        return XFutureVoid.allOf(futures);
    }

    private boolean enableConveyor = false;

    public boolean isEnableConveyor() {
        return enableConveyor;
    }

    public void setEnableConveyor(boolean enableConveyor) {
        this.enableConveyor = enableConveyor;
    }

    private String conveyorClonedViewSystemTaskName;

    /**
     * Get the value of conveyorClonedViewSystemTaskName
     *
     * @return the value of conveyorClonedViewSystemTaskName
     */
    public String getConveyorClonedViewSystemTaskName() {
        return conveyorClonedViewSystemTaskName;
    }

    /**
     * Set the value of conveyorClonedViewSystemTaskName
     *
     * @param conveyorClonedViewSystemTaskName new value of
     * conveyorClonedViewSystemTaskName
     */
    public void setConveyorClonedViewSystemTaskName(String conveyorClonedViewSystemTaskName) {
        this.conveyorClonedViewSystemTaskName = conveyorClonedViewSystemTaskName;
    }

    private @MonotonicNonNull String conveyorTestObjectViewSimulatedFilePath = null;

    /**
     * Get the value of conveyorTestObjectViewSimulatedFilePath
     *
     * @return the value of conveyorTestObjectViewSimulatedFilePath
     */
    public @Nullable
    String getConveyorTestObjectViewSimulatedFilePath() {
        return conveyorTestObjectViewSimulatedFilePath;
    }

    /**
     * Set the value of conveyorTestObjectViewSimulatedFilePath
     *
     * @param conveyorTestObjectViewSimulatedFilePath new value of
     * conveyorTestObjectViewSimulatedFilePath
     */
    public void setConveyorTestObjectViewSimulatedFilePath(String conveyorTestObjectViewSimulatedFilePath) {
        this.conveyorTestObjectViewSimulatedFilePath = conveyorTestObjectViewSimulatedFilePath;
    }

    private static final ConcurrentLinkedDeque<StackTraceElement[]> loadPropertiesOnDisplayTraces = new ConcurrentLinkedDeque<>();

    private void loadPropertiesOnDisplay(Properties props) {
        List<StackTraceElement[]> loadPropertiesTracesCopy = new ArrayList<>(loadPropertiesOnDisplayTraces);
        loadPropertiesOnDisplayTraces.add(Thread.currentThread().getStackTrace());

        if (null != displayJFrame) {
            displayJFrame.loadPropertiesDirect(props);
        }
        String correctionModeString = props.getProperty("correctionMode");
        if (null != correctionModeString && correctionModeString.length() > 0) {
            setCorrectionMode(Boolean.parseBoolean(correctionModeString));
        }
        String convTaskName = props.getProperty("conveyorClonedViewSystemTaskName");
        if (null != convTaskName) {
            setConveyorClonedViewSystemTaskName(convTaskName);
        }
        String convTestFileName = props.getProperty("conveyorTestObjectViewSimulatedFilePath");
        if (null != convTestFileName) {
            setConveyorTestObjectViewSimulatedFilePath(convTestFileName);
        }
    }

    private static class SysSetupFileEntry {

        final int priority;
        final String taskName;
        final String robotName;
        final File propertiesFile;

        public SysSetupFileEntry(CSVRecord csvRecord, File setupFile) {
            try {
                this.priority = Integer.parseInt(csvRecord.get(0));
                this.taskName = csvRecord.get(1);
                this.robotName = csvRecord.get(2);
                String pathname = csvRecord.get(3);
                File f3 = new File(pathname);
                if (f3.exists()) {
                    this.propertiesFile = f3;
                } else {
                    if (File.separatorChar == '/' && pathname.contains("//")) {
                        pathname = pathname.replace('\\', '/');
                    }
                    File f4 = Utils.file(setupFile.getParentFile(), pathname);
                    if (f4.exists()) {
                        this.propertiesFile = f4;
                    } else {
                        throw new RuntimeException(
                                "Can't find file for pathName=" + pathname + " in setupFile=" + setupFile);
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(Supervisor.SysSetupFileEntry.class.getName()).log(Level.SEVERE, "", ex);
                throw new RuntimeException(ex);
            }
        }
    };

    private static class SysSetupFileInfo {

        final File supervisorProperyFile;
        final List<SysSetupFileEntry> sysEntries = new ArrayList<>();

        public SysSetupFileInfo(File f) throws IOException {
            File supervisorProperyFile = null;
            try (CSVParser parser = CSVParser.parse(f, Charset.defaultCharset(), CSVFormat.DEFAULT.withHeader())) {
                int linecount = 0;
                for (CSVRecord csvRecord : parser) {
                    if (csvRecord.size() != 4) {
                        throw new RuntimeException("Bad CSVRecord :" + linecount + " in " + f + "  --> " + csvRecord
                                + "\n" + "csvRecord.size()=" + csvRecord.size() + "\n"
                                + "csvRecord.size() must equal 4\n");
                    }
                    SysSetupFileEntry sysSetupFileEntry = new SysSetupFileEntry(csvRecord, f);
                    println("propertiesFile = " + sysSetupFileEntry.propertiesFile);
                    if (sysSetupFileEntry.priority == -99 || sysSetupFileEntry.taskName.equalsIgnoreCase("supervisor")
                            || sysSetupFileEntry.robotName.equalsIgnoreCase("supervisor")) {
                        if (null != supervisorProperyFile) {
                            throw new RuntimeException(
                                    "supervisorProperyFile already set to " + supervisorProperyFile + ",f=" + f);
                        }
                        supervisorProperyFile = sysSetupFileEntry.propertiesFile;
                    } else {
                        sysEntries.add(sysSetupFileEntry);
                    }
                }
            }
            if (null == supervisorProperyFile) {
                throw new RuntimeException("Entry for supervisorPropertyFile was not found.");
            }
            if (sysEntries.isEmpty()) {
                throw new IllegalStateException("sysEntries.isEmpty() after reading f=" + f);
            }
            this.supervisorProperyFile = supervisorProperyFile;
        }
    }

    /**
     * Load the given setup file.
     *
     * @param f setup file to load
     * @throws IOException file could not be read
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final XFutureVoid loadSetupFile(File f) throws IOException {
        closeAllAprsSystems();
        println("Loading setup file :" + f.getCanonicalPath());
        tasksCachedTable.setRowCount(0);
        SysSetupFileInfo sysSetupFileInfo = new SysSetupFileInfo(f);
        XFutureVoid loadPropertiesFileFuture = loadPropertiesFile(sysSetupFileInfo.supervisorProperyFile);
        XFutureVoid lastFuture = loadPropertiesFileFuture;
        for (SysSetupFileEntry sysSetupFileEntry : sysSetupFileInfo.sysEntries) {
            XFutureVoid prevFuture = lastFuture;
            lastFuture = prevFuture
                    .thenCompose("create." + sysSetupFileEntry.taskName,
                            () -> AprsSystem.createSystem(sysSetupFileEntry.propertiesFile))
                    .thenAcceptAsync(
                            (AprsSystem aprsSys) -> completeLoadSys(aprsSys, sysSetupFileEntry.priority,
                                    sysSetupFileEntry.taskName, sysSetupFileEntry.robotName),
                            supervisorExecutorService);
        }
        return lastFuture
                .thenComposeAsyncToVoid("completeLoadSetup(" + f + ")", () -> completeLoadSetupFile(f), supervisorExecutorService);
    }

    private XFutureVoid completeLoadSetupFile(File f) {
        try {
            aprsSystems.sort(Comparator.comparingInt(AprsSystem::getPriority));
            updateTasksTable();
            return updateRobotsTable().thenComposeAsyncToVoid(() -> {
                try {
                    clearPositionMappingsFilesTable();
                    setSetupFile(f);
                    String convTaskName = getConveyorClonedViewSystemTaskName();
                    if (null != convTaskName && convTaskName.length() > 0 && null != displayJFrame) {
                        displayJFrame.setConveyorClonedViewSystemTaskName(convTaskName);
                    }
                } catch (Exception exception) {
                    Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", exception);
                    throw new RuntimeException(exception);
                }
                return runOnDispatchThread(this::syncToolsFromRobots);
            }, supervisorExecutorService);
        } catch (Exception ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }

    private void completeLoadSys(AprsSystem aprsSys, int priority, String taskName, String robotName) {

        aprsSys.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        aprsSys.setOnCloseRunnable(this::close);
        aprsSys.setPriority(priority);
        String origTasKName = aprsSys.getTaskName();
        if (!Objects.equals(origTasKName, taskName)) {
            throw new IllegalStateException("origTaskName=" + origTasKName + " not equal to taskName=" + taskName);
        }
        String origRobotName = aprsSys.getRobotName();
        if (!Objects.equals(origRobotName, robotName)) {
            throw new IllegalStateException("origRobotName=" + origRobotName + " not equal to robotName=" + robotName);
        }
        aprsSys.setTaskName(taskName);
        aprsSys.setRobotName(robotName);
        String robotTaskName = robotTaskMap.get(robotName);
        if (null != robotTaskName && !taskName.equals(robotTaskName)) {
            throw new RuntimeException(
                    "taskName=" + taskName + ",robotName=" + robotName + ",robotTaskName=" + robotTaskName);
        }
        robotTaskMapPut(robotName, aprsSys);
        aprsSys.getTitleUpdateRunnables().add(() -> {
            updateTasksTableOnSupervisorService();
        });
        aprsSys.setSupervisorEventLogger(this::logEvent);
        aprsSys.setSupervisor(this);
        aprsSystems.add(aprsSys);
        aprsSys.setVisible(true);
    }

    private volatile boolean clearingWayToHolders = false;

    private volatile StackTraceElement clearWayToHoldersTrace@Nullable []  = null;

    public XFutureVoid clearWayToHolders(AprsSystem requester, String holderName) {
        clearingWayToHolders = true;
        requester.pause();
        List<XFutureVoid> l = new ArrayList<>();
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        this.clearWayToHoldersTrace = trace;
        final String requesterTaskName = requester.getTaskName();
        String name = "clearWayToHolders." + holderName + "." + requesterTaskName;
        for (AprsSystem sys : aprsSystems) {
            final String sysTaskName = sys.getTaskName();
            if (sys != requester && !Objects.equals(sysTaskName, requesterTaskName)) {
                if (!sys.isConnected()) {
                    final String errmsg = "clearWayToHolders \"" + requesterTaskName + "\" task needs \"" + sysTaskName
                            + "\" task to be connected to clear the way to " + holderName;
                    logEventErr(errmsg);
                    throw new RuntimeException(errmsg);
                }
                l.add(sys.startSafeAbort(name));
            }
        }
        return XFutureVoid.allOfWithName("allof" + name, l.toArray(new XFutureVoid[0]))
                .thenComposeAsyncToVoid(() -> clearWayToHoldersStep2(requester, holderName), supervisorExecutorService)
                .peekException((Throwable throwable) -> {
                    if (null != throwable) {
                        System.out.println("throwable = " + throwable);
                        System.out.println("requester = " + requester);
                        System.out.println("holderName = " + holderName);
                        System.out.println("clearWayToHoldersTrace = " + Utils.traceToString(trace));
                        logEventErr("failed to clearWayToHolders for requesterTaskName=" + requesterTaskName
                                + ",holderName=" + holderName);
                    }
                });
    }

    private XFutureVoid clearWayToHoldersStep2(AprsSystem requester, String holderName) {
        List<XFuture<Boolean>> l = new ArrayList<>();
        String name = "clearWayToHolders." + holderName + "." + requester.getTaskName();
        for (AprsSystem sys : aprsSystems) {
            if (sys != requester && !Objects.equals(sys.getTaskName(), requester.getTaskName())) {
                l.add(sys.startLookForParts());
            }
        }
        return XFutureVoid.allOfWithName("allof" + name, l).thenRun(() -> {
            this.clearingWayToHolders = false;
        });
    }

    void clearContinuousDemoCycle() {
        ContinuousDemoCycle.set(0);
    }

    private String lastUpdateTaskTableTaskNames@Nullable []  = null;

    private final ConcurrentHashMap<Integer, String> titleErrorMap = new ConcurrentHashMap<>();

    private volatile long liveImageStartTime = 0;
    private volatile long liveImageLastTime = 0;
    private volatile int liveImageFrameCount = 0;
    private volatile int lastLiveImageFrameCount = 0;
    private volatile @Nullable SeekableByteChannel liveImageMovieByteChannel = null;
    private volatile @Nullable AWTSequenceEncoder liveImageMovieEncoder = null;
    private volatile @Nullable File liveImageMovieFile = null;
    private volatile @Nullable File lastLiveImageMovieFile = null;

    private synchronized void startEncodingLiveImageMovie(BufferedImage image) {

        try {
            if (null == image) {
                throw new IllegalArgumentException("null == image");
            }
            liveImageStartTime = System.currentTimeMillis();
            File movieFile = Utils.createTempFile("liveImage", ".mp4");
            if (null == movieFile) {
                throw new IllegalStateException("null == liveImageMovieFile");
            }
            liveImageMovieFile = movieFile;
            println("startEncodingLiveImageMovie: liveImageMovieFile = " + movieFile);
            SeekableByteChannel byteChannel = NIOUtils.writableChannel(movieFile);
            this.liveImageMovieByteChannel = byteChannel;
            liveImageMovieEncoder = new AWTSequenceEncoder(byteChannel, Rational.R(25, 1));
            liveImageMovieEncoder.encodeImage(image);
            liveImageFrameCount = 1;
        } catch (IOException ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", ex);
            finishEncodingLiveImageMovie();
        }
    }

    private boolean recordLiveImageMovieSelected = Boolean.getBoolean("recordLiveImageMovie");

    /**
     * Get the value of recordLiveImageMovieSelected
     *
     * @return the value of recordLiveImageMovieSelected
     */
    private boolean isRecordLiveImageMovieSelected() {
        if (null != displayJFrame) {
            boolean ret = displayJFrame.isRecordLiveImageMovieSelected();
            recordLiveImageMovieSelected = ret;
            return ret;
        }
        return recordLiveImageMovieSelected;
    }

    /**
     * Set the value of recordLiveImageMovieSelected
     *
     * @param recordLiveImageMovieSelected new value of
     * recordLiveImageMovieSelected
     */
    public void setRecordLiveImageMovieSelected(boolean recordLiveImageMovieSelected) {
        if (null != displayJFrame) {
            displayJFrame.setRecordLiveImageMovieSelected(recordLiveImageMovieSelected);
        }
        this.recordLiveImageMovieSelected = recordLiveImageMovieSelected;
    }

    private final int MAX_LIVE_IMAGE_MOVIE_FRAMES = 6000;

    private synchronized void continueEncodingLiveImageMovie(BufferedImage image) {
        try {
            if (null == liveImageMovieFile) {
                throw new IllegalStateException("null == liveImageMovieFile");
            }
            if (null == liveImageMovieEncoder) {
                throw new IllegalStateException("null == liveImageMovieEncoder");
            }
            if (liveImageFrameCount > MAX_LIVE_IMAGE_MOVIE_FRAMES) {
                finishEncodingLiveImageMovie();
                startEncodingLiveImageMovie(image);
                return;
            }
            liveImageMovieEncoder.encodeImage(image);
            liveImageFrameCount++;
            liveImageLastTime = System.currentTimeMillis();
        } catch (IOException ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", ex);
            finishEncodingLiveImageMovie();
        }
    }

    synchronized void finishEncodingLiveImageMovie() {
        try {
            if (null != liveImageMovieEncoder) {
                liveImageMovieEncoder.finish();
            }
        } catch (IOException ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", ex);
        } finally {
            if (null != liveImageMovieByteChannel) {
                NIOUtils.closeQuietly(liveImageMovieByteChannel);
                liveImageMovieByteChannel = null;
            }
            liveImageMovieEncoder = null;
            lastLiveImageMovieFile = liveImageMovieFile;

            if (debug || liveImageFrameCount > 1) {
                println("liveImageStartTime = " + liveImageStartTime);
                println("liveImageLastTime = " + liveImageStartTime);
                int secs = (int) (500 + liveImageLastTime - liveImageStartTime) / 1000;
                println("secs = " + secs);
                println("liveImageFrameCount = " + liveImageFrameCount);
                if (secs > 0) {
                    println("(liveImageFrameCount/secs) = " + (liveImageFrameCount / secs));
                }
                println("finishEncodingLiveImageMovie: lastLiveImageMovieFile = " + lastLiveImageMovieFile);
            }
            liveImageMovieFile = null;
            lastLiveImageFrameCount = liveImageFrameCount;
            liveImageFrameCount = 0;
        }
    }

    private volatile boolean resetting;

    /**
     * Get the value of resetting
     *
     * @return the value of resetting
     */
    public boolean isResetting() {
        return resetting;
    }

    /**
     * Set the value of resetting
     *
     * @param resetting new value of resetting
     */
    public void setResetting(boolean resetting) {
        this.resetting = resetting;
    }

    public XFutureVoid updateTasksTableOnSupervisorService() {
        if (closing || resetting) {
            return XFutureVoid.completedFuture();
        }
        return XFutureVoid.runAsync("updateTasksTableOnSupervisorService", this::updateTasksTable,
                supervisorExecutorService);
    }

    private volatile Object lastTasksTableData                                                     @Nullable []  [] = null;

    @SuppressWarnings("nullness")
    private synchronized void updateTasksTable() {
        if (closing || resetting) {
            return;
        }
        boolean needSetJListFuturesModel = false;
        if (lastUpdateTaskTableTaskNames == null || lastUpdateTaskTableTaskNames.length != aprsSystems.size()) {
            lastUpdateTaskTableTaskNames = new String[aprsSystems.size()];
            needSetJListFuturesModel = true;
        }
        BufferedImage liveImages[] = new BufferedImage[aprsSystems.size()];
        boolean newImage = false;
        Object newTasksTableData[][] = new Object[aprsSystems.size()][];
        boolean needResize = (null == lastTasksTableData || newTasksTableData.length != lastTasksTableData.length);
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystem aprsSystemInterface = aprsSystems.get(i);
            String taskName = aprsSystemInterface.getTaskName();
            if (null != lastUpdateTaskTableTaskNames) {
                if (!Objects.equals(taskName, lastUpdateTaskTableTaskNames[i])) {
                    lastUpdateTaskTableTaskNames[i] = taskName;
                    needSetJListFuturesModel = true;
                }
            }
            if (!isPauseSelected() && !ignoreTitleErrors.get()) {
                String titleErr = aprsSystemInterface.getTitleErrorString();
                if (titleErr != null && titleErr.length() > 0
                        && !titleErr.equals(titleErrorMap.replace(aprsSystemInterface.getMyThreadId(), titleErr))) {
                    logEventErr(aprsSystemInterface + " has title error " + titleErr);
                    logEventErr(aprsSystemInterface + " title error trace="
                            + shortTrace(aprsSystemInterface.getSetTitleErrorStringTrace()));
                }
                if (aprsSystemInterface.isPaused() && isPauseAllForOneSelected() && !resuming
                        && !clearingWayToHolders) {
                    logEvent(aprsSystemInterface + " is paused");
                    pause();
                }
            }
            BufferedImage liveImage = aprsSystemInterface.getLiveImage();
            if (null != liveImage) {
                liveImages[i] = liveImage;
                newImage = true;
                if (!needResize && lastTasksTableData[i][4] == null) {
                    needResize = true;
                }
            }
            String robotName = aprsSystemInterface.getRobotName();
            Image scanImage = aprsSystemInterface.getScanImage();
            if (null != scanImage) {
                if (!needResize && lastTasksTableData[i][3] == null) {
                    needResize = true;
                }
            }
            String detailsString = aprsSystemInterface.getDetailsString();
            if (!needResize) {
                if (detailsString.length() > ((String) lastTasksTableData[i][5]).length()) {
                    needResize = true;
                } else if (detailsString.length() < ((String) lastTasksTableData[i][5]).length() - 100) {
                    needResize = true;
                }
            }
            File propertiesFile = aprsSystemInterface.getPropertiesFile();
            int priority = aprsSystemInterface.getPriority();
            newTasksTableData[i] = new Object[]{priority, taskName, robotName, scanImage, liveImage, detailsString,
                propertiesFile};
        }
        tasksCachedTable.setData(newTasksTableData);
        lastTasksTableData = newTasksTableData;
        if (isRecordLiveImageMovieSelected()) {
            combineAndEncodeLiveImages(newImage, liveImages);
        }
        completeUpdateTasksTable(needSetJListFuturesModel, needResize);
    }

    @SuppressWarnings("guieffect")
    private synchronized void combineAndEncodeLiveImages(boolean newImage, BufferedImage[] liveImages) {
        long t = System.currentTimeMillis();
        if (newImage && (t - liveImageLastTime) > 40 && null != liveImages[0]) {
            BufferedImage combinedImage = new BufferedImage(liveImages[0].getWidth(),
                    liveImages[0].getHeight() * liveImages.length, liveImages[0].getType());
            for (int i = 0; i < liveImages.length; i++) {
                final Graphics graphics = combinedImage.getGraphics();
                final BufferedImage liveImageI = liveImages[i];
                final int y = i * liveImages[0].getHeight();
                drawImageNoObserver(graphics, liveImageI, 0, y);
            }
            if (liveImageFrameCount == 0) {
                startEncodingLiveImageMovie(combinedImage);
            } else {
                continueEncodingLiveImageMovie(combinedImage);
            }
        }
    }

    @SuppressWarnings("nullness")
    private void drawImageNoObserver(final Graphics graphics, final BufferedImage liveImageI, final int x,
            final int y) {
        graphics.drawImage(liveImageI, x, y, (ImageObserver) null);
    }

    @Nullable
    XFutureVoid getResumeFuture() {
        return resumeFuture.get();
    }

    private void completeUpdateTasksTable(boolean needSetJListFuturesModel, boolean needResize) {
        if (null != displayJFrame) {
            displayJFrame.completeUpdateTasksTable(needSetJListFuturesModel, needResize);
        }
    }

    private static @Nullable
    Field getField(Class<?> clss, String name) {
        Field f = null;
        try {
            f = clss.getField(name);
        } catch (NoSuchFieldException | SecurityException ignored) {
        }
        if (null != f) {
            return f;
        }
        Field fields[] = clss.getFields();

        Field declaredFields[] = clss.getDeclaredFields();
        for (Field fi : fields) {
            if (fi.getName().equals(name)) {
                return fi;
            }
        }
        for (Field dfi : declaredFields) {
            if (dfi.getName().equals(name)) {
                return dfi;
            }
        }
        try {
            f = clss.getDeclaredField(name);
        } catch (NoSuchFieldException ex) {
            try {
                f = clss.getField(name);

            } catch (NoSuchFieldException | SecurityException ex1) {
                Logger.getLogger(Supervisor.class.getName()).log(Level.WARNING, null, ex1);

            }
        } catch (SecurityException ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.WARNING, null, ex);

        }
        if (f == null && clss.getSuperclass() != null && !Objects.equals(clss.getSuperclass(), Object.class)) {
            return getField(clss.getSuperclass(), name);
        }
        return f;
    }

    static private DefaultMutableTreeNode cfutureToNode(CompletableFuture<?> future, boolean showDoneFutures,
            boolean showUnnamedFutures) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(future);
        Class<?> clss = future.getClass();
        Field stackField = getField(clss, "stack");
        if (null != stackField) {
            try {
                stackField.setAccessible(true);
                Object stackFieldObject = stackField.get(future);
                addNodesForStackObject(stackFieldObject, future, node, showDoneFutures, showUnnamedFutures);
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                getLogger().log(Level.SEVERE, "", ex);
            }
        }
        return node;
    }

    private static void addNodesForStackObject(@Nullable Object stackFieldObject, CompletableFuture<?> future,
            DefaultMutableTreeNode node, boolean showDoneFutures, boolean showUnnamedFutures) throws SecurityException {
        if (null != stackFieldObject) {
            Class<?> stackFieldClass = stackFieldObject.getClass();
            Field depField = getField(stackFieldClass, "dep");
            if (null != depField) {
                try {
                    depField.setAccessible(true);
                    Object depFieldObject = depField.get(stackFieldObject);
                    if (depFieldObject != future) {
                        if (depFieldObject instanceof XFuture) {
                            XFuture<?> xf = (XFuture<?>) depFieldObject;
                            if (showDoneFutures
                                    || (!xf.isDone() || xf.isCompletedExceptionally() || xf.isCancelled())) {
                                node.add(xfutureToNode(xf, showDoneFutures, showUnnamedFutures, 1));
                            }
                        } else if (depFieldObject instanceof CompletableFuture) {
                            CompletableFuture<?> cf = (CompletableFuture<?>) depFieldObject;
                            boolean notOk = cf.isCompletedExceptionally() || cf.isCancelled();
                            if (showUnnamedFutures || notOk) {
                                if (showDoneFutures || (!cf.isDone() || notOk)) {
                                    node.add(cfutureToNode(cf, showDoneFutures, showUnnamedFutures));
                                }
                            }
                        }
                    }
                } catch (IllegalArgumentException | IllegalAccessException ex) {
//                    Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, "", ex);
                }
            }
            Field nextField = getField(stackFieldClass, "next");
            if (null != nextField) {
                try {
                    nextField.setAccessible(true);
                    Object nextFieldObject = nextField.get(stackFieldObject);
                    if (null != nextFieldObject) {
                        Class<?> nextFieldClass = nextFieldObject.getClass();
                        Field nextFieldStackField = getField(nextFieldClass, "stack");
                        if (null != nextFieldStackField) {
                            Object nextFielStackObject = nextFieldStackField.get(nextFieldObject);
                            addNodesForStackObject(nextFielStackObject, future, node, showDoneFutures,
                                    showUnnamedFutures);
                        }
                    }
                } catch (IllegalArgumentException | IllegalAccessException ex) {
//                    Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, "", ex);
                }
            }
        }
    }

    private static final int XFUTURE_MAX_DEPTH = 100;
    static private boolean firstDepthOverOccured = false;

    static private DefaultMutableTreeNode xfutureToNode(XFuture<?> future, boolean showDoneFutures,
            boolean showUnnamedFutures, int depth) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(future);

        if (depth >= XFUTURE_MAX_DEPTH) {
            if (!firstDepthOverOccured) {
                Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE,
                        "xfutureToNode : depth >= XFUTURE_MAX_DEPTH");
                firstDepthOverOccured = true;
            }
            return node;
        }
        if (null != future) {
            Collection<?> cancelFutures = future.getAlsoCancel();
            if (null != cancelFutures) {
                for (Object o : cancelFutures) {
                    if (o instanceof XFuture) {
                        XFuture<?> xf = (XFuture<?>) o;
                        if (showDoneFutures || (!xf.isDone() || xf.isCompletedExceptionally() || xf.isCancelled())) {
                            node.add(xfutureToNode(xf, showDoneFutures, showUnnamedFutures, (depth + 1)));
                        }
                    } else if (o instanceof CompletableFuture) {
                        CompletableFuture<?> cf = (CompletableFuture<?>) o;
                        boolean notOk = cf.isCompletedExceptionally() || cf.isCancelled();
                        if (showUnnamedFutures || notOk) {
                            if (showDoneFutures || (!cf.isDone() || notOk)) {
                                node.add(cfutureToNode(cf, showDoneFutures, showUnnamedFutures));
                            }
                        }
                    }
                }
            }
        }
        return node;
    }

    private XFutureVoid updateRobotsTable() {
        if (closing) {
            return XFutureVoid.completedFutureWithName("updateRobotsTable.closing");
        }
        for (AprsSystem aprsSystemInterface : aprsSystems) {
            String robotname = aprsSystemInterface.getRobotName();
            if (null != robotname) {
                robotTaskMapPut(robotname, aprsSystemInterface);
            }
        }
        return loadRobotsTableFromSystemsList(aprsSystems).thenRun(() -> {
            if (null != displayJFrame) {
                displayJFrame.updateRobotsTable();
            }
        });
    }

    private XFutureVoid loadRobotsTableFromSystemsList(List<AprsSystem> aprsSystems) {
        if (null != displayJFrame) {
            final AprsSupervisorDisplayJFrame checkedDisplayJFrame = displayJFrame;
            return runOnDispatchThread(() -> checkedDisplayJFrame.loadRobotsTableFromSystemsList(aprsSystems));
        } else {
            return XFutureVoid.completedFutureWithName("loadRobotsTableFromSystemsList.displayJFrame==null");
        }
    }

    @UIEffect
    public static Supervisor createAprsSupervisorWithSwingDisplay(boolean initVisible) {
        AprsSupervisorDisplayJFrame aprsSupervisorDisplayJFrame1 = new AprsSupervisorDisplayJFrame();
        aprsSupervisorDisplayJFrame1.setDefaultIconImage();
        Supervisor supervisor = new Supervisor(aprsSupervisorDisplayJFrame1);
        aprsSupervisorDisplayJFrame1.setSupervisor(supervisor);
        aprsSupervisorDisplayJFrame1.setVisible(initVisible);
        aprsSupervisorDisplayJFrame1.updateRobotsTable();
        return supervisor;
    }

    @UIEffect
    public static XFuture<Supervisor> createAprsSupervisorWithSwingDisplay(Map<String, String> filesMapOut)
            throws IOException {
        AprsSupervisorDisplayJFrame aprsSupervisorDisplayJFrame1 = new AprsSupervisorDisplayJFrame();
        aprsSupervisorDisplayJFrame1.setDefaultIconImage();
        Supervisor supervisor = new Supervisor(aprsSupervisorDisplayJFrame1);
        aprsSupervisorDisplayJFrame1.setSupervisor(supervisor);
        aprsSupervisorDisplayJFrame1.setVisible(true);
        String setup = filesMapOut.get("Setup");
        if (null != setup) {
            return supervisor.loadSetupFile(Utils.file(setup)).thenRun(() -> {
                try {
                    String mapsFile = filesMapOut.get("PosMap");
                    if (null != mapsFile) {
                        supervisor.loadPositionMappingsFilesFile(Utils.file(mapsFile));
                    }
                    String simTeach = filesMapOut.get("SimTeach");
                    if (null != simTeach) {
                        supervisor.loadSimTeach(Utils.file(simTeach));
                    }

                    String teachProps = filesMapOut.get("TeachProps");
                    if (null != teachProps) {
                        supervisor.loadTeachProps(Utils.file(teachProps));
                    }
                    String sharedTools = filesMapOut.get("SharedTools");
                    if (null != sharedTools) {
                        supervisor.loadSharedTools(Utils.file(sharedTools));
                    }
                    aprsSupervisorDisplayJFrame1.updateRobotsTable();
                } catch (IOException iOException) {
                    Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, "", iOException);
                    throw new RuntimeException(iOException);
                }
            }).thenApply(x -> supervisor);
        } else {
            throw new RuntimeException("No supervisor setup file selected.");
        }
    }

    @UIEffect
    public static XFuture<Supervisor> createAprsSupervisorWithSwingDisplay(File propertiesFile) throws IOException {
        AprsSupervisorDisplayJFrame aprsSupervisorDisplayJFrame1 = new AprsSupervisorDisplayJFrame();
        aprsSupervisorDisplayJFrame1.setDefaultIconImage();
        Supervisor supervisor = new Supervisor(aprsSupervisorDisplayJFrame1);
        return supervisor.loadSetupFile(propertiesFile).thenRun(() -> {
            aprsSupervisorDisplayJFrame1.setSupervisorAndShow(supervisor);
        }).thenApply(x -> supervisor);
    }

    @UIEffect
    public void setIconImageOnDisplay(Image image) {
        assert SwingUtilities.isEventDispatchThread();
        if (null != displayJFrame) {
            displayJFrame.setIconImage(image);
        }
        if (null != processLauncher) {
            processLauncher.setIconImage(image);
        }
    }
}
