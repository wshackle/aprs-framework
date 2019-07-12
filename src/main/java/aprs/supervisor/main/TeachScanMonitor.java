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
package aprs.supervisor.main;

import aprs.database.PhysicalItem;
import aprs.learninggoals.GoalLearner;
import static aprs.misc.AprsCommonLogger.println;
import aprs.misc.Utils;
import aprs.simview.Object2DOuterJPanel;
import aprs.system.AprsSystem;
import crcl.ui.XFutureVoid;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author shackle
 */
public class TeachScanMonitor {

    final int startingAbortCount;
    boolean anyChanged = false;
    int cycles = 0;
    long start_time = System.currentTimeMillis();
    long last_time = start_time;
    long max_time_diff = 0;
    long maxHandleTime = 0;
    long totalHandleTime = 0;
    long max_kitTrays = 0;
    int max_kitTraySkips = 0;
    int skips = 0;
    int consecututiveSkips = 0;
    final List<AprsSystem> aprsSystems;
    final private AtomicInteger abortCount;
    private final boolean continuousDemoSelected;
    private final boolean useTeachCameraSelected;
    private final XFutureVoid future;
    private final Object2DOuterJPanel object2DOuterJPanel1;
    private final Supplier<Boolean> closedSupplier;
    private volatile boolean futureCompleted = false;
    private volatile boolean stopFlag = false;
    private final DeferredEventLogger deferredEventLogger;
    private final ExecutorService supervisorExecutorService;
    private final Supervisor supervisor;

    public void stop() {
        long now = System.currentTimeMillis();
        println("submitTeachItemsCount = " + submitTeachItemsCount);
        println("handleTeachItemsCount = " + handleTeachItemsCount);
        println("maxHandleTime = " + maxHandleTime);
        println("totalHandleTime = " + totalHandleTime);
        long avgHandleTime = totalHandleTime / handleTeachItemsCount;
        println("avgHandleTime = " + avgHandleTime);
        logEvent("TeachScanMonitor.stop after cycles=" + cycles + ", (now-start_time)=" + (now - start_time) + ",max_time_diff=" + max_time_diff + ",skips=" + skips + ",submitTeachItemsCount = " + submitTeachItemsCount + ",handleTeachItemsCount = " + handleTeachItemsCount + ",maxHandleTime = " + maxHandleTime + ",avgHandleTime = " + avgHandleTime);
        stopFlag = true;
        object2DOuterJPanel1.removeSetItemsListener(teachItemsConsumer);
        if (!future.isDone()) {
            future.cancelAll(false);
        }
    }

    private XFutureVoid logEvent(String s) {
        return deferredEventLogger.logEvent(false, s);
    }

    private XFutureVoid logEventErr(String s) {
        return deferredEventLogger.logEvent(true, s);
    }

    public boolean isUseTeachCameraSelected() {
        return useTeachCameraSelected;
    }

    public boolean isContinuousDemoSelected() {
        return continuousDemoSelected;
    }

    private final Map<String, List<String>> origStartingStringsMap = new HashMap<>();

    @SuppressWarnings("initialization")
    public TeachScanMonitor(List<AprsSystem> aprsSystems,
            AtomicInteger abortCount,
            boolean continuousDemoSelected,
            boolean useTeachCameraSelected,
            Object2DOuterJPanel object2DOuterJPanel1,
            Supplier<Boolean> closedSupplier,
            DeferredEventLogger deferredEventLogger,
            ExecutorService supervisorExecutorService,
            Supervisor supervisor) {
        this.aprsSystems = aprsSystems;
        this.abortCount = abortCount;
        this.startingAbortCount = abortCount.get();
        this.continuousDemoSelected = continuousDemoSelected;
        this.useTeachCameraSelected = useTeachCameraSelected;
        this.future = new XFutureVoid("TeachScanMonitor");
        this.object2DOuterJPanel1 = object2DOuterJPanel1;
        this.closedSupplier = closedSupplier;
        this.deferredEventLogger = deferredEventLogger;
        this.supervisorExecutorService = supervisorExecutorService;
        this.supervisor = supervisor;
        object2DOuterJPanel1.addSetItemsListener(teachItemsConsumer);
        for (AprsSystem aprsSys : aprsSystems) {
            List<String> startingKitStrings = Collections.unmodifiableList(new ArrayList<>(aprsSys.getLastCreateActionListFromVisionKitToCheckStrings()));
            origStartingStringsMap.put(aprsSys.getTaskName(), startingKitStrings);
        }
        if (useTeachCameraSelected && !object2DOuterJPanel1.isUserMouseDown()) {
            List<PhysicalItem> startingItems = object2DOuterJPanel1.getItems();
            if (null != startingItems) {
                submitTeachItems(startingItems);
            }
        }
    }

    public XFutureVoid getFuture() {
        return future;
    }

    @SuppressWarnings({"initialization", "nullness"})
    private final Consumer<List<PhysicalItem>> teachItemsConsumer = this::submitTeachItems;

    private volatile @Nullable
    XFutureVoid handleTeachItemsFuture = null;

    private volatile int submitTeachItemsCount = 0;

    private void submitTeachItems(@Nullable List<PhysicalItem> teachItems) {
        submitTeachItemsCount++;
        if (futureCompleted || stopFlag) {
            object2DOuterJPanel1.removeSetItemsListener(teachItemsConsumer);
            return;
        }
        if (closedSupplier.get() || abortCount.get() != startingAbortCount) {
            futureCompleted = true;
            object2DOuterJPanel1.removeSetItemsListener(teachItemsConsumer);
            future.cancelAll(false);
            return;
        }
        if (null == teachItems) {
            return;
        }
        if (!object2DOuterJPanel1.isUserMouseDown()) {
            List<PhysicalItem> nonNullTeachItems = new ArrayList<>(teachItems);
            XFutureVoid lastHandleTeachItemsFuture = handleTeachItemsFuture;
            if (null == lastHandleTeachItemsFuture || lastHandleTeachItemsFuture.isDone()) {
                handleTeachItemsFuture
                        = XFutureVoid.runAsync(
                                "handleTeachItems",
                                () -> handleTeachItems(nonNullTeachItems),
                                supervisorExecutorService
                        );
            }
        }
    }

    private volatile int handleTeachItemsCount = 0;

    private volatile List<PhysicalItem> lastTeachItems = Collections.emptyList();
    private volatile List<PhysicalItem> lastChangeTeachItems = Collections.emptyList();

    private boolean isPreClosing() {
        return supervisor.isPreClosing();
    }

    private void handleTeachItems(List<PhysicalItem> teachItems) {
        handleTeachItemsCount++;
        long handleTeachItemsStartTime = System.currentTimeMillis();
        if (futureCompleted) {
            return;
        }
        if (isPreClosing()) {
            future.cancelAll(false);
            return;
        }
        if (closedSupplier.get() || abortCount.get() != startingAbortCount) {
            futureCompleted = true;
            object2DOuterJPanel1.removeSetItemsListener(teachItemsConsumer);
            future.cancelAll(false);
        }
        if (object2DOuterJPanel1.isUserMouseDown()) {
            return;
        }
        long kitTrays
                = teachItems
                        .stream()
                        .filter((PhysicalItem item) -> item.getType().equals("KT"))
                        .count();
        if (max_kitTrays < kitTrays) {
            max_kitTrays = kitTrays;
        } else if (kitTrays < max_kitTrays) {
            max_kitTraySkips++;
            skips++;
            consecututiveSkips++;
            if (consecututiveSkips ==  20) {
                logEvent("TeachScanMonitor.handleTeachItems :  kitTrays=" + kitTrays + ", max_kitTrays=" + max_kitTrays + ", skips = " + skips + ", consecututiveSkips = " + consecututiveSkips + ", max_kitTraySkips=" + max_kitTraySkips);
                logTeachItems(teachItems, "kitTraysLessThanMax");
            }
            return;
        }

        List<String> changedSystems = new ArrayList<>();
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystem aprsSys = aprsSystems.get(i);
            if (isPreClosing()) {
                future.cancelAll(false);
                return;
            }
            if (futureCompleted) {
                return;
            }
            if (closedSupplier.get() || abortCount.get() != startingAbortCount) {
                futureCompleted = true;
                object2DOuterJPanel1.removeSetItemsListener(teachItemsConsumer);
                future.cancelAll(false);
            }
            if (object2DOuterJPanel1.isUserMouseDown()) {
                return;
            }
            List<String> startingKitStrings = origStartingStringsMap.get(aprsSys.getTaskName());
            List<String> lastCreateActionListFromVisionKitToCheckStrings = aprsSys.getLastCreateActionListFromVisionKitToCheckStrings();
            if (null == startingKitStrings) {
                System.err.println("null == startingKitStrings");
                startingKitStrings = lastCreateActionListFromVisionKitToCheckStrings;
            } else {
                String diff = GoalLearner.kitToCheckStringsEqual(startingKitStrings, lastCreateActionListFromVisionKitToCheckStrings);
                if (diff != null) {
                    GoalLearner gl = aprsSys.getGoalLearner();
                    if (null == gl) {
                        throw new NullPointerException("aprsSys.getGoalLearner() : aprsSys.getTaskName()=" + aprsSys.getTaskName());
                    }
                    Thread thread = gl.getSetLastCreateActionListFromVisionKitToCheckStringsThread();
                    StackTraceElement trace[] = gl.getSetLastCreateActionListFromVisionKitToCheckStringsTrace();
                    long time = gl.getSetLastCreateActionListFromVisionKitToCheckStringsTime();
                    long diffTime = System.currentTimeMillis() - time;
                    println("");
                    System.err.println("");
                    System.out.flush();
                    System.err.flush();
                    System.err.println("diff = " + diff);
                    System.err.println("trace = " + Arrays.toString(trace));
                    System.err.println("thread = " + thread);
                    System.err.println("diffTime = " + diffTime);
                    logTeachItems(teachItems, "DiffError");
                    Thread currentThread = Thread.currentThread();
                    System.err.println("currentThread = " + currentThread);
                    String errMsg = "TeachScanMonitor.handleTeachItems :  : diff=" + diff + " " + startingKitStrings + " != " + lastCreateActionListFromVisionKitToCheckStrings;
                    logEventErr(errMsg);
                    throw new IllegalStateException(errMsg);
                }
            }
            File actionListFile = supervisor.completeScanOneInternal(aprsSys, teachItems);
            List<String> endingKitStrings = aprsSys.getLastCreateActionListFromVisionKitToCheckStrings();
            if (endingKitStrings.size() < startingKitStrings.size()) {
                // lost a kitTray, here we assume the vision frame is bad
                aprsSys.setLastCreateActionListFromVisionKitToCheckStrings(startingKitStrings);
                skips++;
                consecututiveSkips++;
                origStartingStringsMap.put(aprsSys.getTaskName(), startingKitStrings);
                if (consecututiveSkips != 20) {
                    logEvent("TeachScanMonitor.handleTeachItems : endingKitStrings.size() =" + endingKitStrings.size() + ", startingKitStrings.size()=" + startingKitStrings.size() + ", kitTrays = " + kitTrays + ", skips = " + skips + ", consecututiveSkips = " + consecututiveSkips);
                } else {
                    logEventErr("endingKitStrings=" + endingKitStrings);
                    logEventErr("startingKitStrings=" + startingKitStrings);
                    logTeachItems(teachItems, "endingKitStringsSize");
                    logEventErr("TeachScanMonitor.handleTeachItems : endingKitStrings.size() =" + endingKitStrings.size() + ", startingKitStrings.size()=" + startingKitStrings.size() + ", kitTrays = " + kitTrays + ", skips = " + skips + ", consecututiveSkips = " + consecututiveSkips);
                }
                return;
            }
            String diff = GoalLearner.kitToCheckStringsEqual(startingKitStrings, endingKitStrings);
            if (diff != null) {
                changedSystems.add(aprsSys.getTaskName());
                if (null == actionListFile) {
                    throw new IllegalStateException("actionListFile not created for " + aprsSys.getTaskName());
                } else {
                    logEvent("TeachScanMonitor.handleTeachItems :  causing" + aprsSys.getTaskName() + " to load actionFile = " + actionListFile);
                }
                anyChanged = true;
            }
            consecututiveSkips = 0;
        }
        long now = System.currentTimeMillis();
        long diff = now - last_time;
        long handleTimeDiff = now - handleTeachItemsStartTime;
        totalHandleTime += handleTimeDiff;
        if (handleTimeDiff > maxHandleTime) {
            maxHandleTime = handleTimeDiff;
        }
        if (max_time_diff < diff) {
            max_time_diff = diff;
        }
        last_time = now;
        if (anyChanged) {
            futureCompleted = true;
            println("submitTeachItemsCount = " + submitTeachItemsCount);
            println("handleTeachItemsCount = " + handleTeachItemsCount);
            println("maxHandleTime = " + maxHandleTime);
            println("totalHandleTime = " + totalHandleTime);
            long avgHandleTime = totalHandleTime / handleTeachItemsCount;
            println("avgHandleTime = " + avgHandleTime);
            lastChangeTeachItems = new ArrayList<>(teachItems);
            logEvent("TeachScanMonitor.handleTeachItems :  saw new after cycles=" + cycles + ", changedSystems=" + changedSystems + ", (now-start_time)=" + (now - start_time) + ",max_time_diff=" + max_time_diff + ",diff=" + diff + ",skips=" + skips + ",avgHandleTime = " + avgHandleTime + ",maxHandleTime = " + maxHandleTime);
            object2DOuterJPanel1.removeSetItemsListener(teachItemsConsumer);
            try {
                final String teachItemsImageFileNamePrefix = "teachItems_" + supervisor.incrementAndGetNewTeachCount() + "_" + teachItems.size() + "_";
                final File teachItemsImageFile = Utils.createTempFile(teachItemsImageFileNamePrefix, ".PNG");
                object2DOuterJPanel1.takeSnapshot(teachItemsImageFile, teachItems);
                File teachItemsCsvFile = Utils.createTempFile(teachItemsImageFileNamePrefix, ".csv");
                object2DOuterJPanel1.saveCsvItemsFile(teachItemsCsvFile, teachItems);
            } catch (IOException ex) {
                Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
            }
            future.complete();
        } else {
            lastTeachItems = new ArrayList<>(teachItems);
        }
    }

    private void logTeachItems(List<PhysicalItem> teachItems, final String logLabel) {
        System.err.println("teachItems = " + teachItems);
        try {
            File teachItemsCsvFile = Utils.createTempFile("teachItems" + logLabel + "_", ".csv");
            System.err.println("teachItemsCsvFile = " + teachItemsCsvFile.getCanonicalPath());
            object2DOuterJPanel1.saveCsvItemsFile(teachItemsCsvFile, teachItems);
            File teachItemsImageFile = Utils.createTempFile("teachItems" + logLabel + "_", ".PNG");
            System.err.println("teachItemsImageFile = " + teachItemsImageFile.getCanonicalPath());
            object2DOuterJPanel1.takeSnapshot(teachItemsImageFile, teachItems);
        } catch (Exception ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            File lastTeachItemsCsvFile = Utils.createTempFile("lastTeachItems" + logLabel + "_", ".csv");
            System.err.println("lastTeachItemsCsvFile = " + lastTeachItemsCsvFile.getCanonicalPath());
            object2DOuterJPanel1.saveCsvItemsFile(lastTeachItemsCsvFile, lastTeachItems);
            File lastTeachItemsImageFile = Utils.createTempFile("lastTeachItems" + logLabel + "_", ".PNG");
            System.err.println("lastTeachItemsImageFile = " + lastTeachItemsImageFile.getCanonicalPath());
            object2DOuterJPanel1.takeSnapshot(lastTeachItemsImageFile, lastTeachItems);
        } catch (Exception ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            File lastChangeTeachItemsCsvFile = Utils.createTempFile("lastChangeTeachItems" + logLabel + "_", ".csv");
            System.err.println("lastChangeTeachItemsCsvFile = " + lastChangeTeachItemsCsvFile.getCanonicalPath());
            object2DOuterJPanel1.saveCsvItemsFile(lastChangeTeachItemsCsvFile, lastChangeTeachItems);
            File lastChangeTeachItemsImageFile = Utils.createTempFile("lastChangeTeachItems" + logLabel + "_", ".PNG");
            System.err.println("lastChangeTeachItemsImageFile = " + lastChangeTeachItemsImageFile.getCanonicalPath());
            object2DOuterJPanel1.takeSnapshot(lastChangeTeachItemsImageFile, lastChangeTeachItems);
        } catch (Exception ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
