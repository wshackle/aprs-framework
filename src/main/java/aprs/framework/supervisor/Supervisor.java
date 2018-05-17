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
 * This software can be redistributed and/or modified freely provided show
 * that any derivative works bear some notice that they are derived from it, 
 * and any modified versions bear some notice that they have been modified.
 * 
 *  See http://www.copyright.gov/title17/92chap1.html#105
 * 
 */
package aprs.framework.supervisor;

import aprs.framework.system.AprsSystemInterface;
import aprs.framework.SlotOffsetProvider;
import aprs.framework.Utils;
import static aprs.framework.system.AprsSystem.createAprsSystemWithSwingDisplay;
import static aprs.framework.Utils.readFirstLine;
import static aprs.framework.Utils.runTimeToString;
import static aprs.framework.Utils.tableHeaders;

import aprs.framework.colortextdisplay.ColorTextJFrame;
import aprs.framework.colortextdisplay.ColorTextJPanel;
import aprs.framework.database.PhysicalItem;
import aprs.framework.database.Slot;
import aprs.framework.learninggoals.GoalLearner;
import aprs.framework.pddl.executor.PositionMap;
import aprs.framework.pddl.executor.PositionMapJPanel;
import aprs.framework.process.launcher.ProcessLauncherJFrame;
import aprs.framework.screensplash.SplashScreen;
import aprs.framework.simview.Object2DOuterJPanel;
import aprs.framework.system.AprsSystem;

import crcl.base.CRCLStatusType;
import crcl.base.CommandStateEnumType;
import crcl.ui.XFuture;
import crcl.ui.XFutureVoid;
import crcl.ui.misc.MultiLineStringJPanel;

import java.awt.Color;
import java.awt.Component;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;

/**
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class Supervisor {

    @MonotonicNonNull private final AprsSupervisorDisplayJFrame displayJFrame;

    public Supervisor() {
        this(null);
    }

    private final Object2DOuterJPanel object2DOuterJPanel1;
    //private final JTable jTableRobots;
    private final JTable jTableSelectedPosMapFile;
    private final JTable jTablePositionMappings;
    private final JTable jTableTasks;

    public ConcurrentHashMap<String, AprsSystemInterface> getSlotProvidersMap() {
        return slotProvidersMap;
    }

    public final SlotOffsetProvider getSlotOffsetProvider() {
        return slotOffsetProvider;
    }

    /**
     * Creates new form AprsMulitSupervisorJFrame
     */
    @SuppressWarnings("initialization")
    private Supervisor(@Nullable AprsSupervisorDisplayJFrame displayJFrame) {
        this.displayJFrame = displayJFrame;
        this.object2DOuterJPanel1 = (displayJFrame != null)
                ? displayJFrame.getObject2DOuterJPanel1()
                : new Object2DOuterJPanel();

        this.jTablePositionMappings = (displayJFrame != null)
                ? displayJFrame.getPositionMappingsTable()
                : new JTable();

        this.jTableTasks = (displayJFrame != null)
                ? displayJFrame.getTasksTable()
                : new JTable();

        this.jTableSelectedPosMapFile = (displayJFrame != null)
                ? displayJFrame.getSelectedPosMapFileTable()
                : new JTable();

//        this.jTableRobots = (displayJFrame != null)
//                ? displayJFrame.getRobotsTable()
//                : new JTable();

        AtomicInteger newWaitForTogglesFutureCount = new AtomicInteger();
        waitForTogglesFutureCount = newWaitForTogglesFutureCount;
        ConcurrentLinkedDeque<XFuture<Void>> newWaitForTogglesFutures = new ConcurrentLinkedDeque<>();
        waitForTogglesFutures = newWaitForTogglesFutures;
        togglesAllowedXfuture = new AtomicReference<>(createFirstWaitForTogglesFuture(newWaitForTogglesFutures, newWaitForTogglesFutureCount));
        if (null != displayJFrame) {
            displayJFrame.setRobotEnableMap(robotEnableMap);
            displayJFrame.setRecordLiveImageMovieSelected(recordLiveImageMovieSelected);
            object2DOuterJPanel1.setSlotOffsetProvider(this.getSlotOffsetProvider());
        }
    }

    private final ConcurrentHashMap<String, AprsSystemInterface> slotProvidersMap
            = new ConcurrentHashMap<>();

    public boolean isDebugStartReverseActions() {
        return debugStartReverseActions;
    }

    public void setDebugStartReverseActions(boolean debugStartReverseActions) {
        this.debugStartReverseActions = debugStartReverseActions;
    }

    public boolean isTogglesAllowed() {
        return togglesAllowed;
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
         *
         * @return list of slots with relative position offsets.
         */
        @Override
        public List<Slot> getSlotOffsets(String name, boolean ignoreEmpty) {
            for (int i = 0; i < aprsSystems.size(); i++) {
                try {
                    AprsSystemInterface sys = aprsSystems.get(i);
                    List<Slot> l = sys.getSlotOffsets(name, true);
                    if (null != l && !l.isEmpty()) {
                        slotProvidersMap.put(name, sys);
                        return l;
                    }
                } catch (IllegalStateException e) {
                    //ignoring trays that can't be found, must be for another system.
                }
            }
            return Collections.emptyList();
        }

        @Override
        @Nullable
        public Slot absSlotFromTrayAndOffset(PhysicalItem tray, Slot offsetItem) {
            AprsSystemInterface sys = slotProvidersMap.get(tray.origName);
            if (null != sys) {
                return sys.absSlotFromTrayAndOffset(tray, offsetItem, 0);
            }
            return null;
        }

        @Override
        @Nullable
        public Slot absSlotFromTrayAndOffset(PhysicalItem tray, Slot offsetItem, double rotationOffset) {
            AprsSystemInterface sys = slotProvidersMap.get(tray.origName);
            if (null != sys) {
                return sys.absSlotFromTrayAndOffset(tray, offsetItem, rotationOffset);
            }
            return null;
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

    public void stopColorTextReader() {
        if (null != displayJFrame) {
            displayJFrame.stopColorTextReader();
        }
    }

    /**
     * Get the location of the last CSV Setup file used.
     *
     * @return setup file location
     * @throws IOException setup files location can not be read
     */
    @Nullable
    public static File getLastSetupFile() throws IOException {
        if (lastSetupFileFile.exists()) {
            String firstLine = readFirstLine(lastSetupFileFile);
            if (null != firstLine && firstLine.length() > 0) {
                return new File(firstLine);
            }
        }
        return null;
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
    public static File getLastSimTeachFile() throws IOException {
        if (lastSimTeachFileFile.exists()) {
            String firstLine = readFirstLine(lastSimTeachFileFile);
            if (null != firstLine && firstLine.length() > 0) {
                return new File(firstLine);
            }
        }
        return null;
    }

    /**
     * Get the location of the last teach properties file used. The properties
     * file contains settings on how to display the teach objects.
     *
     * @return last teach properties file
     * @throws IOException file location can not be read
     */
    @Nullable
    public static File getLastTeachPropertiesFile() throws IOException {
        if (lastTeachPropertiesFileFile.exists()) {
            String firstLine = readFirstLine(lastTeachPropertiesFileFile);
            if (null != firstLine && firstLine.length() > 0) {
                return new File(firstLine);
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
    @Nullable
    public static File getLastPosMapFile() throws IOException {
        if (lastPosMapFileFile.exists()) {
            String firstLine = readFirstLine(lastPosMapFileFile);
            if (null != firstLine && firstLine.length() > 0) {
                return new File(firstLine);
            }
        }
        return null;
    }

    /**
     * Reload the last saved/used setup.
     */
    public void loadPrevSetup() {
        try {
            File setupFile = getLastSetupFile();
            if (null != setupFile && setupFile.exists()) {
                loadSetupFile(setupFile);
            }
        } catch (Exception ex) {
            log(Level.SEVERE, null, ex);
            try {
                closeAllAprsSystems();
            } catch (Exception ex1) {
                log(Level.SEVERE, null, ex1);
            }
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
    public void loadTeachPropertiesFile(File f) throws IOException {
        object2DOuterJPanel1.setPropertiesFile(f);
        object2DOuterJPanel1.loadProperties();
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
            log(Level.SEVERE, null, ex);
            try {
                closeAllAprsSystems();
            } catch (Exception ex1) {
                log(Level.SEVERE, null, ex1);
            }
        }
    }

    /**
     * Reload the last teach properties file read/saved.
     */
    public void loadPrevTeachProperties() {
        try {
            File teachProps = getLastTeachPropertiesFile();
            if (null != teachProps && teachProps.exists()) {
                loadTeachPropertiesFile(teachProps);
            }
        } catch (Exception ex) {
            log(Level.SEVERE, null, ex);
            try {
                closeAllAprsSystems();
            } catch (Exception ex1) {
                log(Level.SEVERE, null, ex1);
            }
        }
    }

    /**
     * Reload the last posmap file read/saved.
     */
    public void loadPrevPosMapFile() {
        try {
            File posFile = getLastPosMapFile();
            if (null != posFile && posFile.exists()) {
                loadPositionMaps(posFile);
            }
        } catch (IOException ex) {
            log(Level.SEVERE, null, ex);
        }
    }

    @Nullable
    public AprsSystemInterface findSystemWithRobot(String robot) {
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystemInterface aj = aprsSystems.get(i);
            String robotName = aj.getRobotName();
            if (robotName != null && robotName.equals(robot)) {
                return aj;
            }
        }
        return null;
    }

    private List<List<PositionMapJPanel>> positionMapJPanels = new ArrayList<>();

    private List<PositionMapJPanel> getPositionMapRow(int row) {
        while (positionMapJPanels.size() <= row) {
            positionMapJPanels.add(new ArrayList<>());
        }
        return positionMapJPanels.get(row);
    }

    private PositionMapJPanel getPositionMap(int row, int col) {
        List<PositionMapJPanel> lrow = getPositionMapRow(row);
        while (lrow.size() <= col) {
            lrow.add(new PositionMapJPanel());
        }
        return lrow.get(col);
    }

    private final AtomicReference<@Nullable XFuture<Void>> stealRobotFuture = new AtomicReference<>(null);
    private final AtomicReference<@Nullable XFuture<Void>> unStealRobotFuture = new AtomicReference<>(null);
    private final AtomicReference<@Nullable XFuture<Void>> cancelStealRobotFuture = new AtomicReference<>(null);
    private final AtomicReference<@Nullable XFuture<Void>> cancelUnStealRobotFuture = new AtomicReference<>(null);

    private XFuture<Void> checkUnstealRobotFuture(XFuture<Void> future) {
        XFuture<Void> currentUnstealRobotFuture = unStealRobotFuture.get();
        if (null != currentUnstealRobotFuture && currentUnstealRobotFuture != future) {
            final XFuture<Void> newFuture = currentUnstealRobotFuture;
            return newFuture
                    .thenCompose("checkUnStealRobotFuture1", x -> checkUnstealRobotFuture(newFuture));
        } else {
            return XFuture.completedFutureWithName("checkUnstealRobotFuture2", null);
        }
    }

    private XFuture<?> checkLastReturnedFuture(@Nullable XFuture<?> inFuture) {
        final XFuture<?> lfr = this.lastFutureReturned;
        if (null != lfr && lfr != inFuture) {
            return lfr
                    .thenCompose("checkLastReturnedFuture1",
                            x -> checkLastReturnedFuture(lfr));
        } else {
            return XFuture.completedFutureWithName("checkLastReturnedFuture2", null);
        }
    }

    private List<XFuture<?>> oldLfrs = new ArrayList<>();

    public List<XFuture<?>> getOldLfrs() {
        return oldLfrs;
    }

    private final ConcurrentHashMap<String, Integer> robotEnableCountMap
            = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Integer> robotDisableCountMap
            = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Long> robotDisableStartMap
            = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Long> robotDisableTotalTimeMap
            = new ConcurrentHashMap<>();

    public int getRobotDisableCount(String robotName) {
        return robotDisableCountMap.getOrDefault(robotName, 0);
    }

    public long getRobotDisableTotalTime(String robotName) {
        return robotDisableTotalTimeMap.getOrDefault(robotName, 0L);
    }

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
        return robotDisableTotalTimeMap.values()
                .stream()
                .collect(Collectors.summingLong(x -> x));
    }

    public int getTotalDisableCount() {
        return robotDisableCountMap.values()
                .stream()
                .collect(Collectors.summingInt(x -> x));
    }

    public void showMessageDialog(Object msg) {
        if (null != displayJFrame) {
            Component component = displayJFrame;
            Utils.runOnDispatchThread(() -> {
                JOptionPane.showMessageDialog(component, msg);
            });
        }
    }

    public void setRobotEnabled(String robotName, Boolean enabled) {
        try {
            long time = System.currentTimeMillis();
            logEvent("setEnabled(" + robotName + "," + enabled + ") : count =" + getAndIncrementEnableCount(robotName, enabled) + ", diff=" + runTimeToString(time - robotDisableStartMap.getOrDefault(robotName, time)) + ", totalTime=" + runTimeToString(robotDisableTotalTimeMap.getOrDefault(robotName, 0L)));
            Utils.runOnDispatchThread(() -> {
                refreshRobotsTable();
            });

            if (null != robotName && null != enabled) {
                robotEnableMap.put(robotName, enabled);
                if (!enabled) {
                    if (stealingRobots || stealRobotFuture.get() != null) {
                        return;
                    }
                    try {
                        final XFuture<Void> origUnstealFuture = unStealRobotFuture.getAndSet(null);
                        final XFuture<Void> origCancelUnstealFuture = cancelUnStealRobotFuture.getAndSet(null);
                        try {
                            if (null != origUnstealFuture && null != origCancelUnstealFuture) {
                                logEvent("Cancelling future created at " + getTimeString(origUnstealFuture.getStartTime()) + ", origUnstealFuture = " + origUnstealFuture);
                                origUnstealFuture.cancelAll(true);
                                printStatus(origUnstealFuture, System.out);
                            }
                            final XFuture<Void> future = stealRobot(robotName);
                            if (null == future) {
                                logEventErr(" stealRobot(" + robotName + ") returned null");
                                XFuture<Void> future2 = stealRobot(robotName);
                                throw new IllegalStateException("stealRobot(" + robotName + ") returned null");
                            }
                            logEvent("stealRobotFuture set to " + future);
                            this.stealRobotFuture.set(future);
                            final XFuture<Void> cancelFuture = new XFuture<>("cancelStealRobotFuture");
                            if (!this.cancelStealRobotFuture.compareAndSet(null, cancelFuture)) {
                                throw new IllegalStateException("cancelStealRobotFuture already set.");
                            }
                            lastFutureReturned = XFuture.anyOfWithName("setRobotEnabled(" + robotName + "," + enabled + ").anyOf(steal,cancel)",
                                    future.handle("setRobotEnabled(" + robotName + "," + enabled + ").anyOf(steal,cancel).handle",
                                            (Void x, Throwable t) -> {
                                                if (t != null) {
                                                    if (!(t instanceof CancellationException)) {
                                                        log(Level.SEVERE, null, t);
                                                        logEvent(t.toString());
                                                        setAbortTimeCurrent();
                                                        pause();
                                                        showMessageDialog(t);
                                                    }
                                                    return t.toString();
                                                } else {
                                                    return "";
                                                }
                                            })
                                            .thenCompose("setRobotEnabled(" + robotName + "," + enabled + ").checkForExceptions",
                                                    (String x) -> {
                                                        if (x == null || x.length() < 1) {
                                                            return XFuture.completedFutureWithName(
                                                                    "setRobotEnabled(" + robotName + "," + enabled + ").alreadyComplete",
                                                                    x);
                                                        } else {
//                                                            System.err.println("Returning xfuture which will never complete. x=" + x);
//                                                            Thread.dumpStack();
                                                            return new XFuture<>(x + ".neverComplete");
                                                        }
                                                    }),
                                    cancelFuture);

                        } finally {
                            if (null != origUnstealFuture && null != origCancelUnstealFuture) {
                                System.out.println("Completing origCancelUnstealFuture= " + origCancelUnstealFuture);
                                origCancelUnstealFuture.complete(null);
                            }
                        }
                    } catch (IOException | PositionMap.BadErrorMapFormatException ex) {
                        log(Level.SEVERE, null, ex);
                    }
                } else {

                    if (!stealingRobots || unStealRobotFuture.get() != null) {
                        return;
                    }
                    stealingRobots = false;
                    XFuture<?> prevLFR = lastFutureReturned;
                    final XFuture<Void> origStealFuture = stealRobotFuture.getAndSet(null);
                    final XFuture<Void> origCancelStealFuture = cancelStealRobotFuture.getAndSet(null);
                    try {
                        if (null != origStealFuture && null != origCancelStealFuture) {
                            logEvent("Cancelling future created at " + getTimeString(origStealFuture.getStartTime()) + ", origStealFuture = " + origStealFuture);
                            origStealFuture.cancelAll(true);
                            System.out.println("origStealFuture = " + origStealFuture);
                            printStatus(origStealFuture, System.out);
                            System.out.println("lastFutureReturned = " + lastFutureReturned);
                            printStatus(lastFutureReturned, System.out);
                        }
                        final XFuture<Void> future = unStealRobots();
                        if (null == future) {
                            throw new IllegalStateException("unstealRobots() returned null");
                        }
                        logEvent("unStealRobotFuture set to " + future);
                        this.unStealRobotFuture.set(future);
                        final XFuture<Void> cancelFuture = new XFuture<>("cancelUnStealRobotFuture");
                        if (!this.cancelUnStealRobotFuture.compareAndSet(null, cancelFuture)) {
                            throw new IllegalStateException("cancelUnStealRobotFuture already set.");
                        }
                        lastFutureReturned = XFuture.anyOfWithName("setRobotEnabled(" + robotName + "," + enabled + ").anyOf(unsteal,cancel)",
                                future.handle("setRobotEnabled(" + robotName + "," + enabled + ").handle1",
                                        (Void x, Throwable t) -> {
                                            if (t == null) {
                                                return "";
                                            } else {
                                                if (!(t instanceof CancellationException)) {
                                                    logEvent(t.toString());
                                                    log(Level.SEVERE, null, t);
                                                    setAbortTimeCurrent();
                                                    pause();
                                                    showMessageDialog(t);
                                                }
                                                return t.toString();
                                            }
                                        })
                                        .thenCompose("setRobotEnabled(" + robotName + "," + enabled + ").handle2",
                                                x -> {
                                                    if (x == null || x.length() < 1) {
                                                        return XFutureVoid.completedFutureWithName("setRobotEnabled(" + robotName + "," + enabled + ").completedFuture2");
                                                    } else {
                                                        System.err.println("Returning xfuture which will never complete x=." + x);
                                                        Thread.dumpStack();
                                                        return new XFuture<>(x + ".neverComplete");
                                                    }
                                                }),
                                cancelFuture);
                        //.thenCompose(x -> checkStealRobotFuture(null)));
                    } finally {
                        if (null != origStealFuture && null != origCancelStealFuture) {
                            System.out.println("Completing origCancelStealFuture= " + origCancelStealFuture);
                            origCancelStealFuture.complete(null);
                            System.out.println("origCancelStealFuture = " + origCancelStealFuture);
                            printStatus(origCancelStealFuture, System.out);
                            if (origCancelStealFuture.isCompletedExceptionally()) {
                                origCancelStealFuture.exceptionally(t -> {
                                    t.printStackTrace();
                                    if (t.getCause() != null) {
                                        t.getCause().printStackTrace();
                                    }
                                    return null;
                                });
                            }
                        }
                    }
                    System.out.println("prevLFR = " + prevLFR);
                    printStatus(prevLFR, System.out);
//                this.unStealRobotFuture = unStealRobots();
//                this.cancelUnStealRobotFuture = new XFuture<>();
//                final XFuture<Void> origStealFuture = stealRobotFuture;
//                    lastFutureReturned = XFuture.anyOfWithName(this.unStealRobotFuture, 
//                            this.cancelUnStealRobotFuture.thenCompose(x -> checkUnstealRobotFuture(stealRobotFuture)));
//                 lastFutureReturned = XFuture.anyOfWithName(this.stealRobotFuture, 
//                            this.cancelStealRobotFuture.thenCompose(x -> (null != unStealRobotFuture)?unStealRobotFuture:XFuture.completedFuture(null)));
                }
                if (null != lastFutureReturned) {
                    oldLfrs.add(lastFutureReturned);
                }
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

    private void refreshRobotsTable() {
        if (null != displayJFrame) {
            displayJFrame.refreshRobotsTable();
        }
    }

    public void setAbortTimeCurrent() {
        abortEventTime = System.currentTimeMillis();
    }

    private final ConcurrentLinkedDeque<XFuture<?>> stealUnstealList
            = new ConcurrentLinkedDeque<>();

    private void addStealUnstealList(XFuture<?> f) {
        stealUnstealList.add(f);
    }

    private XFuture<Void> stealRobot(String robotName) throws IOException, PositionMap.BadErrorMapFormatException {
        Set<String> names = new HashSet<>();
        for (int i = 0; i < aprsSystems.size() - 1; i++) {
            AprsSystemInterface sys = aprsSystems.get(i);
            if (null != sys) {
                String sysRobotName = sys.getRobotName();
                if (null != sysRobotName) {
                    names.add(sysRobotName);
                    if (Objects.equals(sysRobotName, robotName)) {
                        XFuture<Void> f = stealRobot(aprsSystems.get(i + 1), aprsSystems.get(i));
                        addStealUnstealList(f);
                        return f;
                    }
                }
            }
        }
        String errMsg = "Robot " + robotName + " not found in " + names;
        System.out.println("aprsSystems = " + aprsSystems);
        logEventErr(errMsg);
        showErrorSplash(errMsg);
        throw new IllegalStateException(errMsg);
//        return XFuture.completedFutureWithName("stealRobot(" + robotName + ").completedFuture", null);
    }

    final private static String transferrableOptions[] = new String[]{
        "rpy",
        "lookForXYZ",
        "slowTransSpeed",
        "jointAccel",
        "jointSpeed",
        "rotSpeed",
        "fastTransSpeed",
        "settleDwellTime",
        "lookForJoints",
        "useJointLookFor"
    };

    private void copyOptions(String options[], Map<String, String> mapIn, Map<String, String> mapOut) {
        for (String opt : options) {
            if (mapIn.containsKey(opt)) {
                mapOut.put(opt, mapIn.get(opt));
            }
        }
    }

    private final AtomicReference<@Nullable NamedCallable<XFuture<Void>>> returnRobotRunnable = new AtomicReference<>();

    @SuppressWarnings("unchecked")
    private <T> NamedCallable<T> setReturnRobotRunnable(String name, Callable<T> r, AprsSystemInterface... systems) {
        NamedCallable<T> namedR = new NamedCallable<>(r, name, systems);
        returnRobotRunnable.set((NamedCallable<XFuture<Void>>) namedR);
        return namedR;
    }

    public long getFirstEventTime() {
        return firstEventTime;
    }

    public void setFirstEventTime(long firstEventTime) {
        this.firstEventTime = firstEventTime;
    }

    public long getAbortEventTime() {
        return abortEventTime;
    }

    public void setAbortEventTime(long abortEventTime) {
        this.abortEventTime = abortEventTime;
    }

    public void checkRobotsUniquePorts() {
        Set<Integer> set = new HashSet<>();
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystemInterface sys = aprsSystems.get(i);
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

    private volatile StackTraceElement returnRobotsStackTrace @Nullable []  = null;
    @Nullable
    private volatile Thread returnRobotsThread = null;
    private volatile long returnRobotsTime = -1;

    public void printReturnRobotTraceInfo() {
        System.out.println("returnRobotsThread = " + returnRobotsThread);
        System.out.println("returnRobotsStackTrace = " + Arrays.toString(returnRobotsStackTrace));
        System.out.println("returnRobotsTime = " + (returnRobotsTime - System.currentTimeMillis()));
    }

    public XFuture<@Nullable Void> returnRobots(String comment) {
        return returnRobots(returnRobotRunnable.getAndSet(null), comment);
    }

    public XFuture<@Nullable Void> returnRobotsDirect(String comment) {
        return returnRobotsDirect(returnRobotRunnable.getAndSet(null), comment);
    }

    private AtomicInteger returnRobotsNumber = new AtomicInteger();

    public XFuture<Void> returnRobots(@Nullable NamedCallable<XFuture<Void>> r, String comment) {
        checkRobotsUniquePorts();
        this.stealingRobots = false;
        if (r != null) {
            Thread curThread = Thread.currentThread();
            returnRobotsThread = curThread;
            returnRobotsTime = System.currentTimeMillis();
            returnRobotsStackTrace = curThread.getStackTrace();
            String blockerName = "returnRobots" + returnRobotsNumber.incrementAndGet();
            disallowToggles(blockerName, r.getSystems());
            logEvent(r.getName() + ", comment=" + comment);

            return XFuture.supplyAsync(r.getName(), r, supervisorExecutorService)
                    .thenCompose(x -> x)
                    .alwaysAsync(() -> allowToggles(blockerName), supervisorExecutorService);
        } else {
            logReturnRobotsNullRunnable(comment);
            return XFuture.completedFuture(null);
        }
    }

    @Nullable public XFuture<Void> getStealAbortFuture() {
        return stealAbortFuture;
    }

    public void setStealAbortFuture(XFuture<Void> stealAbortFuture) {
        this.stealAbortFuture = stealAbortFuture;
    }

    @Nullable public XFuture<Void> getUnstealAbortFuture() {
        return unstealAbortFuture;
    }

    public void setUnstealAbortFuture(XFuture<Void> unstealAbortFuture) {
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

    private XFuture<@Nullable Void> returnRobotsDirect(@Nullable NamedCallable<XFuture<Void>> r, String comment) {
        checkRobotsUniquePorts();
        this.stealingRobots = false;
        if (r != null) {
            try {
                Thread curThread = Thread.currentThread();
                returnRobotsThread = curThread;
                returnRobotsTime = System.currentTimeMillis();
                returnRobotsStackTrace = curThread.getStackTrace();
                String blockerName = "returnRobotsDirect" + returnRobotsNumber.incrementAndGet();
                disallowToggles(blockerName, r.getSystems());
                logEvent(r.getName() + ", comment=" + comment);
                return r.call()
                        .alwaysAsync(() -> allowToggles(blockerName), supervisorExecutorService);
            } catch (Exception ex) {
                Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
                XFuture<Void> ret = new XFuture<>("returnRobotsDirect." + comment);
                ret.completeExceptionally(ex);
                return ret;
            }
        } else {
            logReturnRobotsNullRunnable(comment);
            return XFuture.completedFuture(null);
        }
    }

    private final AtomicReference< @Nullable Supplier<XFuture<Void>>> unStealRobotsSupplier = new AtomicReference<>(null);

    private XFuture<Void> unStealRobots() {
        Supplier<XFuture<Void>> supplier = unStealRobotsSupplier.getAndSet(null);
        if (null == supplier) {
            return XFuture.completedFutureWithName("unStealRobots.null==supplier", null);
        }
        return supplier.get();
    }

    private volatile boolean pauseSelected = false;

    public void setPauseSelected(boolean selected) {
        if (null != displayJFrame) {
            displayJFrame.setPauseSelected(selected);
        }
        this.pauseSelected = selected;
    }

    public boolean isPauseSelected() {
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

    public boolean isPauseAllForOneSelected() {
        if (null != displayJFrame) {
            boolean ret = displayJFrame.isPauseAllForOneSelected();
            this.pauseAllForOneSelected = ret;
            return ret;
        }
        return this.pauseAllForOneSelected;
    }

    private volatile boolean continousDemoSelected = false;

    public void setContinousDemoSelected(boolean selected) {
        if (null != displayJFrame) {
            displayJFrame.setContinousDemoSelected(selected);
        }
        this.continousDemoSelected = selected;
    }

    public boolean isContinousDemoSelected() {
        if (null != displayJFrame) {
            boolean ret = displayJFrame.isContinousDemoSelected();
            this.continousDemoSelected = ret;
            return ret;
        }
        return this.continousDemoSelected;
    }

    private volatile boolean useTeachCameraSelected = true;

    public void setUseTeachCameraSelected(boolean selected) {
        if (null != displayJFrame) {
            displayJFrame.setUseTeachCameraSelected(selected);
        }
        this.useTeachCameraSelected = selected;
    }

    public boolean isUseTeachCameraSelected() {
        if (null != displayJFrame) {
            boolean ret = displayJFrame.isUseTeachCameraSelected();
            this.useTeachCameraSelected = ret;
            return ret;
        }
        return this.useTeachCameraSelected;
    }

    private volatile boolean indContinousDemoSelected = false;

    public void setIndContinousDemoSelected(boolean selected) {
        if (null != displayJFrame) {
            displayJFrame.setIndContinousDemoSelected(selected);
        }
        this.indContinousDemoSelected = selected;
    }

    public boolean isIndContinousDemoSelected() {
        if (null != displayJFrame) {
            boolean ret = displayJFrame.isIndContinousDemoSelected();
            this.indContinousDemoSelected = ret;
            return ret;
        }
        return this.indContinousDemoSelected;
    }

    private volatile boolean indRandomToggleTestSelected = false;

    public void setIndRandomToggleTestSelected(boolean selected) {
        if (null != displayJFrame) {
            displayJFrame.setIndRandomToggleTestSelected(selected);
        }
        this.indRandomToggleTestSelected = selected;
    }

    public boolean isIndRandomToggleTestSelected() {
        if (null != displayJFrame) {
            boolean ret = displayJFrame.isIndRandomToggleTestSelected();
            this.indRandomToggleTestSelected = ret;
            return ret;
        }
        return this.indRandomToggleTestSelected;
    }

    private volatile boolean randomTestSelected = false;

    public void setRandomTestSelected(boolean selected) {
        if (null != displayJFrame) {
            displayJFrame.setRandomTestSelected(selected);
        }
        this.randomTestSelected = selected;
    }

    public boolean isRandomTestSelected() {
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

    //jCheckBoxMenuItemKeepAndDisplayXFutureProfiles
    private volatile boolean continousDemoRevFirstSelected = false;

    public void setContinousDemoRevFirstSelected(boolean selected) {
        if (null != displayJFrame) {
            displayJFrame.setContinousDemoRevFirstSelected(selected);
        }
        this.continousDemoRevFirstSelected = selected;
    }

    public boolean isContinousDemoRevFirstSelected() {
        if (null != displayJFrame) {
            boolean ret = displayJFrame.isContinousDemoRevFirstSelected();
            this.continousDemoRevFirstSelected = ret;
            return ret;
        }
        return this.continousDemoRevFirstSelected;
    }

    private volatile boolean keepAndDisplayXFutureProfilesSelected = false;

    public void setKeepAndDisplayXFutureProfilesSelected(boolean selected) {
        if (null != displayJFrame) {
            displayJFrame.setKeepAndDisplayXFutureProfilesSelected(selected);
        }
        this.keepAndDisplayXFutureProfilesSelected = selected;
    }

    public boolean isKeepAndDisplayXFutureProfilesSelected() {
        if (null != displayJFrame) {
            boolean ret = displayJFrame.isKeepAndDisplayXFutureProfilesSelected();
            this.keepAndDisplayXFutureProfilesSelected = ret;
            return ret;
        }
        return this.keepAndDisplayXFutureProfilesSelected;
    }

    private XFuture<Void> showCheckEnabledErrorSplash() {

        return showErrorSplash("Not all robots\n could be enabled.")
                .thenRun(() -> {
                    Utils.runOnDispatchThread(() -> {
                        setContinousDemoSelected(false);
                        setContinousDemoRevFirstSelected(false);
                        if (null != continousDemoFuture) {
                            continousDemoFuture.cancelAll(true);
                            continousDemoFuture = null;
                        }
                    });
                });
    }

    private XFuture<Void> showErrorSplash(String errMsgString) {
        if (null != displayJFrame) {
            return displayJFrame.showErrorSplash(errMsgString);
        } else {
            return XFutureVoid.completedFutureWithName("showErrorSplash " + errMsgString);
        }
    }

    @Nullable
    private XFuture<Void> stealAbortFuture = null;
    @Nullable
    private XFuture<Void> unstealAbortFuture = null;

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
    private XFuture<Void> showMessageFullScreen(String message, float fontSize, @Nullable Image image, List<Color> colors, @Nullable GraphicsDevice graphicsDevice) {
        if (null != displayJFrame && null != graphicsDevice) {
            return displayJFrame.showMessageFullScreen(message, fontSize, image, colors, graphicsDevice);
        } else {
            return XFutureVoid.completedFutureWithName("showMessageFullScreen " + message);
        }
    }

    @MonotonicNonNull private AprsSystemInterface posMapInSys = null;
    @MonotonicNonNull private AprsSystemInterface posMapOutSys = null;

    @Nullable public AprsSystemInterface getPosMapInSys() {
        return posMapInSys;
    }

    public void setPosMapInSys(AprsSystemInterface posMapInSys) {
        this.posMapInSys = posMapInSys;
    }

    @Nullable public AprsSystemInterface getPosMapOutSys() {
        return posMapOutSys;
    }

    public void setPosMapOutSys(AprsSystemInterface posMapOutSys) {
        this.posMapOutSys = posMapOutSys;
    }

    private final AtomicInteger stealRobotNumber = new AtomicInteger();
    private final AtomicInteger reverseRobotTransferNumber = new AtomicInteger();

    public XFuture<Void> stealRobot(AprsSystemInterface stealFrom, AprsSystemInterface stealFor) throws IOException, PositionMap.BadErrorMapFormatException {

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
        return stealRobotsInternal(stealFrom, stealFor, stealForRobotName, stealFromRobotName, stealFromOrigCrclHost);
    }

    private void writeToColorTextSocket(byte[] bytes) {
        if (null != displayJFrame) {
            displayJFrame.writeToColorTextSocket(bytes);
        }
    }

    private XFuture<Void> stealRobotsInternal(AprsSystemInterface stealFrom, AprsSystemInterface stealFor, String stealForRobotName, String stealFromRobotName, String stealFromOrigCrclHost) throws IOException, PositionMap.BadErrorMapFormatException {
        final int srn = stealRobotNumber.incrementAndGet();
        logEvent("Transferring " + stealFrom.getRobotName() + " to " + stealFor.getTaskName() + " : srn=" + srn);
        String blocker = "stealRobot" + srn;
        disallowToggles(blocker, stealFrom, stealFor);
        XFuture<Void> origStealRobotFuture = stealRobotFuture.get();
        if (origStealRobotFuture != null) {
            System.out.println("calling stealrRobot when already stealingRobots");
            return origStealRobotFuture;
        }

        File f = getPosMapFile(stealForRobotName, stealFromRobotName);
        PositionMap pm = (f != null && !f.getName().equals("null")) ? new PositionMap(f) : PositionMap.emptyPositionMap();

        initColorTextSocket();

        int stealFromOrigCrclPort = stealFrom.getRobotCrclPort();

        Map<String, String> stealFromOptionsCopy = new HashMap<>();
        Map<String, String> stealFromOptionsOrig = stealFrom.getExecutorOptions();
        if (null != stealFromOptionsOrig) {
            copyOptions(transferrableOptions, stealFromOptionsOrig, stealFromOptionsCopy);
        }

        Map<String, String> stealForOptionsCopy = new HashMap<>();
        Map<String, String> stealForOptionsOrig = stealFor.getExecutorOptions();
        if (null != stealForOptionsOrig) {
            copyOptions(transferrableOptions, stealForOptionsOrig, stealForOptionsCopy);
        }
        NamedCallable<XFuture<?>> returnRobot = setupReturnRobots(srn, stealFor, stealFrom, stealForOptionsCopy, pm);

        final GraphicsDevice gd = (null != displayJFrame)
                ? displayJFrame.getGraphicsConfiguration().getDevice()
                : null;

        setupUnstealRobots(srn, stealFor, stealFrom, stealForRobotName, gd);
        stealingRobots = true;
        logEvent("Starting safe abort and disconnect for " + " : srn=" + srn + " " + stealFor);
        logEvent("    and starting safe abort and disconnect for " + " : srn=" + srn + " " + stealFrom);
        stealAbortFuture = XFuture.allOfWithName("stealAbortAllOf",
                stealFrom.startSafeAbortAndDisconnect("stealAbortAllOf.stealFrom" + " : srn=" + srn)
                        .thenRunAsync(() -> logEvent("Safe abort and disconnect completed for " + stealFrom + " " + stealFromRobotName + " needed for " + stealFor + " : srn=" + srn), supervisorExecutorService),
                stealFor.startSafeAbortAndDisconnect("stealAbortAllOf.stealFor" + " : srn=" + srn)
                        .thenComposeAsync("showDisabledMessage." + stealForRobotName,
                                x -> {
                                    logEvent("Safe abort and disconnect completed for " + stealFor + ", " + stealForRobotName + " being disabled. " + " : srn=" + srn);
                                    writeToColorTextSocket("0xFF0000, 0x00FF00\r\n".getBytes());
                                    if (null != gd) {
                                        return showMessageFullScreen(stealForRobotName + "\n Disabled", 80.0f,
                                                SplashScreen.getDisableImageImage(),
                                                SplashScreen.getRedYellowColorList(), gd);
                                    } else {
                                        return XFutureVoid.completedFutureWithName("showMessageFullScreen " + stealForRobotName + " Disabled");
                                    }
                                }, supervisorExecutorService));

        XFuture<Boolean> part1 = stealAbortFuture.thenComposeAsync(
                "transfer" + " : srn=" + srn, x -> {
                    logEvent("transfer : " + stealFor + " connectRobot(" + stealFromRobotName + "," + stealFromOrigCrclHost + "," + stealFromOrigCrclPort + ")" + " : srn=" + srn);
                    stealFor.addPositionMap(pm);
                    for (String opt : transferrableOptions) {
                        if (stealFromOptionsCopy.containsKey(opt)) {
                            stealFor.setExecutorOption(opt, stealFromOptionsCopy.get(opt));
                        }
                    }
                    return stealFor.connectRobot(stealFromRobotName, stealFromOrigCrclHost, stealFromOrigCrclPort);
                },
                supervisorExecutorService)
                .thenCompose(
                        "showSwitching" + " : srn=" + srn, x -> {
                            return showMessageFullScreen("Switching to \n" + stealFromRobotName, 80.0f,
                                    SplashScreen.getRobotArmImage(), SplashScreen.getBlueWhiteGreenColorList(), gd);
                        }
                )
                .alwaysAsync(() -> {
                    allowToggles(blocker, stealFor);
                }, supervisorExecutorService)
                //                .thenRun(
                //                        () -> this.allowToggles())
                .thenComposeAsync("continueAfterSwitch" + " : srn=" + srn, x -> {
                    int curSrn = stealRobotNumber.get();
                    if (srn != curSrn) {
                        logEvent("continueAfterSwitch srn=" + srn + ", curSrn=" + curSrn);
                        return XFuture.completedFutureWithName("continueAfterSwitch.srn != stealRobotNumber.get()" + " : srn=" + srn, false);
                    }
                    if (stealFor.isAborting()) {
                        logEvent("continueAfterSwitch stealFor.isAborting() : srn=" + srn + ", curSrn=" + curSrn + ", stealFor=" + stealFrom);
                        return XFuture.completedFutureWithName("continueAfterSwitch.stealFor.isAborting()" + " : srn=" + srn, false);
                    }

                    logEvent("Continue actions after switch for " + stealFor.getTaskName() + " with " + stealFor.getRobotName() + " : srn=" + srn);
                    return stealFor.continueActionList("stealFor.continueAfterSwitch" + " : srn=" + srn)
                            .thenComposeAsync(x4 -> {
                                logEvent("continueAfterSwitch " + stealFor.getRunName() + " completed action list after robot switch " + x4 + " : srn=" + srn);
                                return finishAction(stealFor)
                                        .thenApply(x5 -> {
                                            logEvent("finish continueAfterSwitch " + stealFor.getRunName() + " completed action list " + x4 + " : srn=" + srn);
                                            if (x4) {
                                                completeSystemsContinueIndFuture(stealFor, !stealFor.isReverseFlag());
                                            }
                                            return x4;
                                        });
                            }, supervisorExecutorService);
                }, supervisorExecutorService
                );

        if (isIndContinousDemoSelected()
                || isIndRandomToggleTestSelected()) {
            return part1
                    .thenRunAsync("stealRobot :  Checking systemContinueMap " + " : srn=" + srn,
                            () -> {
                                logEvent("completing stealRobot: stealingRobots=" + stealingRobots + ", stealFor=" + stealFor + ",srn=" + srn + ", stealRobotNumber=" + stealRobotNumber.get() + "robotEnableMap.get(" + stealForRobotName + ")=" + robotEnableMap.get(stealForRobotName));
                                if (stealingRobots && srn == stealRobotNumber.get()) {
                                    Boolean enabled = robotEnableMap.get(stealForRobotName);
                                    if (null == enabled) {
                                        throw new IllegalStateException("robotEnableMap has null for " + stealForRobotName);
                                    }
                                    if (!enabled) {
                                        completeSystemsContinueIndFuture(stealFor, !stealFor.isReverseFlag());
                                    }
                                }
                            }, supervisorExecutorService);
        }

        return part1.thenCompose(
                "startSafeAbortAfterSwitch" + " : srn=" + srn, x -> {
                    return stealFor.startSafeAbort("startSafeAbortAfterSwitch" + " : srn=" + srn)
                            .thenRun(() -> logEvent("Safe abort completed for " + stealFor + " : srn=" + srn));
                })
                .thenComposeAsync(
                        "showReturning" + " : srn=" + srn, x -> {
                            return showMessageFullScreen("Returning \n" + stealFromRobotName, 80.0f,
                                    SplashScreen.getRobotArmImage(), SplashScreen.getBlueWhiteGreenColorList(), gd);
                        }, supervisorExecutorService
                )
                .thenComposeAsync(
                        "returnRobots2" + " : srn=" + srn, x -> returnRobotsDirect("returnRobots2" + " : srn=" + srn),
                        supervisorExecutorService)
                .thenCompose("continueAfterReturn" + " : srn=" + srn, x -> {
                    logEvent("Continue actions for " + stealFor.getTaskName() + " with " + stealFor.getRobotName() + " : srn=" + srn);
                    return stealFrom.continueActionList("stealFrom.continueAfterReturn" + " : srn=" + srn)
                            .thenComposeAsync((Boolean x5) -> {
                                logEvent("stealFrom.continueAfterReturn " + stealFrom.getRunName() + " completed action list after return " + x5 + " : srn=" + srn);
                                return finishAction(stealFrom)
                                        .thenApply((Void x6) -> {
                                            logEvent("finish stealFrom.continueAfterReturn " + stealFrom.getRunName() + " completed action list " + x5 + " : srn=" + srn);
                                            if (x5) {
                                                completeSystemsContinueIndFuture(stealFrom, !stealFrom.isReverseFlag());
                                            }
                                            return ((Void) null);
                                        });
                            }, supervisorExecutorService);
                });
    }

    private NamedCallable<XFuture<?>> setupReturnRobots(final int srn, AprsSystemInterface stealFor, AprsSystemInterface stealFrom, Map<String, String> stealForOptions, PositionMap pm) {
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
        NamedCallable<XFuture<?>> returnRobot = setupRobotReturnInternal(stealFrom, stealFor, srn, stealForRobotName, stealFromRobotName, stealFromOrigCrclHost, stealForOptions, pm, stealForOrigCrclHost);
        return returnRobot;
    }

    private NamedCallable<XFuture<?>> setupRobotReturnInternal(AprsSystemInterface stealFrom, AprsSystemInterface stealFor, final int srn, String stealForRobotName, String stealFromRobotName, String stealFromOrigCrclHost, Map<String, String> stealForOptions, PositionMap pm, String stealForOrigCrclHost) {
        int stealFromOrigCrclPort = stealFrom.getRobotCrclPort();
        int stealForOrigCrclPort = stealFor.getRobotCrclPort();
        String returnName = "Return  : srn=" + srn + " " + stealForRobotName + "-> " + stealFor.getTaskName() + " , " + stealFromRobotName + "->" + stealFrom.getTaskName();
        NamedCallable<XFuture<?>> returnRobot
                = setReturnRobotRunnable(returnName,
                        () -> {
                            if (stealFor.isPaused()) {
                                logEvent(stealFor.getTaskName() + " is paused when trying to return robot" + " : srn=" + srn);
                            }
                            if (stealFrom.isPaused()) {
                                logEvent(stealFrom.getTaskName() + " is paused when trying to return robot" + " : srn=" + srn);
                            }
                            if (Objects.equals(stealFrom.getRobotName(), stealFromRobotName)) {
                                logEvent(stealFromRobotName + " already assigned to " + stealFrom + " : srn=" + srn);
                            }
                            if (Objects.equals(stealFor.getRobotName(), stealForRobotName)) {
                                logEvent(stealForRobotName + " already assigned to " + stealFor + " : srn=" + srn);
                            }
                            if (Objects.equals(stealFrom.getRobotName(), stealFromRobotName)
                            && Objects.equals(stealFor.getRobotName(), stealForRobotName)) {
                                return XFuture.completedFutureWithName("returnRobot.alreadyReturned" + " : srn=" + srn, null);
                            }
                            checkRunningOrDoingActions(stealFor, srn);
                            checkRunningOrDoingActions(stealFrom, srn);
                            logEvent("Disconnect robot from " + stealFor);
                            return stealFor.disconnectRobot()
                                    .thenComposeAsync(
                                            "returnRobot." + stealFrom.getTaskName() + " connect to " + stealFromRobotName + " at " + stealFromOrigCrclHost + ":" + stealFromOrigCrclPort + " : srn=" + srn,
                                            x -> {
                                                logEvent(stealForRobotName + " disconnnected from " + stealFor + " : srn=" + srn);
                                                logEvent("start returnRobot." + stealFrom.getTaskName() + " connect to " + stealFromRobotName + " at " + stealFromOrigCrclHost + ":" + stealFromOrigCrclPort + " : srn=" + srn);
                                                return stealFrom.connectRobot(stealFromRobotName, stealFromOrigCrclHost, stealFromOrigCrclPort);
                                            }, supervisorExecutorService)
                                    .thenComposeAsync("returnRobot.transferOption",
                                            x -> {
                                                logEvent(stealFrom.getTaskName() + " connected to " + stealFromRobotName + " at " + stealFromOrigCrclHost + ":" + stealFromOrigCrclPort + " : srn=" + srn);
                                                for (String opt : transferrableOptions) {
                                                    if (stealForOptions.containsKey(opt)) {
                                                        stealFor.setExecutorOption(opt, stealForOptions.get(opt));
                                                    }
                                                }
                                                stealFor.removePositionMap(pm);
                                                logEvent("start returnRobot." + stealFor.getTaskName() + " connect to " + stealForRobotName + " at " + stealForOrigCrclHost + ":" + stealForOrigCrclPort + " : srn=" + srn);
                                                return stealFor.connectRobot(stealForRobotName, stealForOrigCrclHost, stealForOrigCrclPort);
                                            }, supervisorExecutorService)
                                    .thenRun(() -> {
                                        logEvent(stealFor.getTaskName() + " connected to " + stealForRobotName + " at " + stealForOrigCrclHost + ":" + stealForOrigCrclPort + " : srn=" + srn);
                                        checkRobotsUniquePorts();
                                    });
                        }, stealFor, stealFrom);
        return returnRobot;
    }

    private void checkRunningOrDoingActions(AprsSystemInterface sys, int srn) throws IllegalStateException {
        if (sys.isRunningCrclProgram()) {
            String msg = sys.getTaskName() + " is running crcl program when trying to return robot" + " : srn=" + srn;
            logEvent(msg);
            throw new IllegalStateException(msg);
        }
        if (sys.isDoingActions()) {
            String msg = sys.getTaskName() + " is doing actions when trying to return robot" + " : srn=" + srn;
            logEvent(msg);
            throw new IllegalStateException(msg);
        }
    }

    private void setupUnstealRobots(int srn, AprsSystemInterface stealFor, AprsSystemInterface stealFrom, String stealForRobotName, @Nullable GraphicsDevice gd) {
        unStealRobotsSupplier.set(() -> {
            return executeUnstealRobots(srn, stealFor, stealFrom, stealForRobotName, gd);
        });
    }

    private XFuture<Void> executeUnstealRobots(final int srn, AprsSystemInterface stealFor, AprsSystemInterface stealFrom, String stealForRobotName, @Nullable GraphicsDevice gd) {
        String revBlocker = "reverseRobotTransfer" + reverseRobotTransferNumber.incrementAndGet();
        logEvent("Reversing robot transfer after robot reenabled." + " : srn=" + srn);
        disallowToggles(revBlocker, stealFor, stealFrom);
        stealingRobots = false;
        logEvent("Starting safe abort and disconnect for " + stealFor + " : srn=" + srn);
        logEvent("    and starting safe abort and disconnect for" + stealFrom + " : srn=" + srn);
        unstealAbortFuture = XFuture.allOfWithName("unStealAbortAllOf",
                stealFrom.startSafeAbortAndDisconnect("unStealAbortAllOf.stealFrom" + stealFrom + " : srn=" + srn)
                        .thenRunAsync(() -> logEvent("Safe abort and disconnect completed for " + stealFrom + " : srn=" + srn), supervisorExecutorService),
                stealFor.startSafeAbortAndDisconnect("unStealAbortAllOf.stealFor " + stealFor + " : srn=" + srn)
                        .thenComposeAsync("unstealShowReenable", x -> {
                            logEvent("Safe abort and disconnect completed for " + stealFor + " : srn=" + srn);
                            boolean stillAborting = stealFor.isAborting();
                            if (stillAborting) {
                                String msg = "still aborting after safe abort and disconnect" + " : srn=" + srn;
                                logEvent(msg);
                                boolean doubleCheck = stealFor.isAborting();
                                throw new IllegalStateException(msg);
                            }
                            writeToColorTextSocket("0x00FF00, 0x00FF00\r\n".getBytes());
                            return showMessageFullScreen(stealForRobotName + "\n Enabled", 80.0f,
                                    SplashScreen.getDisableImageImage(),
                                    SplashScreen.getBlueWhiteGreenColorList(), gd);
                        }, supervisorExecutorService));
        return unstealAbortFuture
                .thenComposeAsync("unsteal.returnRobots1" + " : srn=" + srn, x -> {
                    return returnRobotsDirect("unsteal.returnRobots1" + " : srn=" + srn);
                }, supervisorExecutorService)
                .thenRun("unsteal.connectAll" + " : srn=" + srn, () -> connectAll())
                .alwaysAsync(() -> {
                    allowToggles(revBlocker, stealFor, stealFrom);
                }, supervisorExecutorService)
                .thenCompose("unsteal.continueAllOf" + " : srn=" + srn, x -> {
                    int curSrn = stealRobotNumber.get();
                    if (srn != curSrn) {
                        logEvent("unsteal.continueAllOf srn=" + srn + ", curSrn=" + curSrn);
                        return XFuture.completedFutureWithName("unsteal.continueAllOf.srn != stealRobotNumber.get()" + " : srn=" + srn, null);
                    }
                    if (stealFrom.isAborting()) {
                        logEvent("unsteal.continueAllOf stealFrom.isAborting() : srn=" + srn + ", curSrn=" + curSrn + ", stealFrom=" + stealFrom);
                        return XFuture.completedFutureWithName("unsteal.continueAllOf.stealFrom.isAborting()" + " : srn=" + srn, null);
                    }
                    if (stealFor.isAborting()) {
                        logEvent("unsteal.continueAllOf stealFor.isAborting() : srn=" + srn + ", curSrn=" + curSrn + ", stealFor=" + stealFrom);
                        return XFuture.completedFutureWithName("unsteal.continueAllOf.stealFor.isAborting()" + " : srn=" + srn, null);
                    }
                    logEvent("unsteal.continueAllOf Continue actions for " + stealFrom.getTaskName() + " with " + stealFrom.getRobotName() + " : srn=" + srn);
                    logEvent("unsteal.continueAllOf Continue actions for " + stealFor.getTaskName() + " with " + stealFor.getRobotName() + " : srn=" + srn);
                    return XFuture.allOf(stealFrom.continueActionList("unsteal.stealFrom" + " : srn=" + srn)
                            .thenComposeAsync(x2 -> {
                                logEvent("unsteal.stealFrom " + stealFrom.getRunName() + " completed action list after return after robot reenabled. " + x2 + " : srn=" + srn);

                                return finishAction(stealFrom)
                                        .thenApply(x3 -> {
                                            logEvent("finish unsteal.stealFrom " + stealFrom.getRunName() + " completed action list " + x2 + " : srn=" + srn);
                                            if (x2 && !stealFrom.isAborting() && srn == stealRobotNumber.get()) {
                                                completeSystemsContinueIndFuture(stealFrom, !stealFrom.isReverseFlag());
                                            }
                                            return x2;
                                        });
                            }, supervisorExecutorService),
                            stealFor.continueActionList("unsteal.stealFor" + " : srn=" + srn)
                                    .thenComposeAsync(x3 -> {
                                        logEvent("unsteal.stealFor " + stealFor.getRunName() + " completed action list after return after robot reenabled. " + x3 + " : srn=" + srn);
                                        return finishAction(stealFrom)
                                                .thenApply(x4 -> {
                                                    logEvent("finish unsteal.stealFor " + stealFor.getRunName() + " completed action list " + x3 + " : srn=" + srn);
                                                    if (x3 && !stealFor.isAborting() && srn == stealRobotNumber.get()) {
                                                        completeSystemsContinueIndFuture(stealFor, !stealFor.isReverseFlag());
                                                    }
                                                    return x3;
                                                });
                                    }, supervisorExecutorService)
                    );
                });
    }

    private String assertFail() {
        logEvent("assertFail");
        pause();
        return "";
    }

    private void completeSystemsContinueIndFuture(AprsSystemInterface sys, boolean value) {
        assert (null != sys) : assertFail() + "sys == null : sys=" + sys;
        String sysRobotName = sys.getRobotName();
        assert (sysRobotName != null) : assertFail() + "sys.getRobotName() == null: sys=" + sys + " @AssumeAssertion(nullness)";
        assert (sysRobotName.length() > 0) : assertFail() + "sys.getRobotName().length() <= 0 : sys=" + sys;
        assert (sys.isConnected()) : assertFail() + "!sys.isConnected() : sys=" + sys;
        assert (!sys.isAborting()) : assertFail() + "sys.isAborting() : sys=" + sys;
        checkRobotsUniquePorts();
        if (!sys.readyForNewActionsList()) {
            System.err.println("Completing future for " + sys + " when not ready");
        }
        logEvent("Checking systemContinueMap for " + sys);
        AtomicReference<XFuture<Boolean>> ref = new AtomicReference<>();
        XFuture<Boolean> f = systemContinueMap.replace(sys.getMyThreadId(),
                new XFuture<>("systemContinueMap." + sys));
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
    public static String getTimeString(long ms) {
        Date date = new Date(ms);
        return timeFormat.format(date);
    }

    @Nullable
    private volatile PrintStream logPrintStream = null;

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

    volatile javax.swing.@Nullable Timer runTimeTimer = null;

    volatile int maxEventStringLen = 0;

    private void logEventPrivate(long time, String s, int blockerSize, String threadname) {

        if (firstEventTime > 0) {
            updateRunningTime();
            startUpdateRunningTimeTimer();
        }
        String timeString = getTimeString(time);
        if (null == logPrintStream) {
            try {
                File logFile = Utils.createTempFile("events_log_", ".txt");
                System.out.println("logFile = " + logFile.getCanonicalPath());
                logPrintStream = new PrintStream(new FileOutputStream(logFile));

            } catch (IOException ex) {
                Logger.getLogger(Supervisor.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        String fullLogString = timeString + " \t" + blockerSize + " \t" + s + " \t:thread= " + threadname;
        if (null != logPrintStream) {
            logPrintStream.println(fullLogString);
        }
        System.out.println(fullLogString);
        if (null != displayJFrame) {
            displayJFrame.addEventToTable(time, blockerSize, s, threadname);
        }
    }

    private void startUpdateRunningTimeTimer() {
        if (closing) {
            return;
        }
        if (runTimeTimer == null) {
            runTimeTimer = new Timer(2000, x -> updateRunningTime());
            runTimeTimer.start();
        }
    }

    private void updateRunningTime() {
        if (firstEventTime > 0 && !isPauseSelected()) {

            if (null != displayJFrame) {
                displayJFrame.updateRunningTime();
            }
        }
    }

    private void scrollToEnd(JTable jTable) {
        int index = jTable.getRowCount() - 1;
        jTable.getSelectionModel().setSelectionInterval(index, index);
        jTable.scrollRectToVisible(new Rectangle(jTable.getCellRect(index, 0, true)));
    }

    private volatile long firstEventTime = -1;
    private volatile long abortEventTime = -1;

    /**
     * Log an event string to be displayed with timestamp in event table.
     *
     * @param s string to log
     */
    public void logEvent(String s) {
        long t = System.currentTimeMillis();
        if (firstEventTime < 0) {
            firstEventTime = t;
        }
        int blockersSize = getToggleBlockerMapSize();
        String threadname = Thread.currentThread().getName();
        Utils.runOnDispatchThread(() -> logEventPrivate(t, s, blockersSize, threadname));
    }

    public int getToggleBlockerMapSize() {
        int blockersSize = toggleBlockerMap.keySet().size();
        return blockersSize;
    }

    private void initColorTextSocket() throws IOException {
        if (null != displayJFrame) {
            displayJFrame.initColorTextSocket();
        }
    }

    private final Map<String, Boolean> robotEnableMap = new HashMap<>();

    private final static File lastSetupFileFile = new File(System.getProperty("aprsLastMultiSystemSetupFile", System.getProperty("user.home") + File.separator + ".lastAprsSetupFile.txt"));
    private final static File lastSimTeachFileFile = new File(System.getProperty("aprsLastMultiSystemSimTeachFile", System.getProperty("user.home") + File.separator + ".lastAprsSimTeachFile.txt"));
    private final static File lastTeachPropertiesFileFile = new File(System.getProperty("aprsLastMultiSystemTeachPropertiesFile", System.getProperty("user.home") + File.separator + ".lastAprsTeachPropertiesFile.txt"));
    private final static File lastPosMapFileFile = new File(System.getProperty("aprsLastMultiSystemPosMapFile", System.getProperty("user.home") + File.separator + ".lastAprsPosMapFile.txt"));

    private static class PositionMappingTableModel extends DefaultTableModel {

        public PositionMappingTableModel() {
        }

        public PositionMappingTableModel(int rowCount, int columnCount) {
            super(rowCount, columnCount);
        }

        @SuppressWarnings("rawtypes")
        public PositionMappingTableModel(Vector columnNames, int rowCount) {
            super(columnNames, rowCount);
        }

        public PositionMappingTableModel(Object[] columnNames, int rowCount) {
            super(columnNames, rowCount);
        }

        @SuppressWarnings("rawtypes")
        public PositionMappingTableModel(Vector data, Vector columnNames) {
            super(data, columnNames);
        }

        @SuppressWarnings({"rawtypes", "nullness"})
        public PositionMappingTableModel(@Nullable Object[][] data, Object[] columnNames) {
            super(data, columnNames);
        }
    }

    @SuppressWarnings({"rawtypes", "nullness"})
    private TableModel defaultPositionMappingsModel() {
        return new PositionMappingTableModel(
                new Object[][]{
                    {"System", "Robot1", "Robot2"},
                    {"Robot1", null, new File("R1R2.csv")},
                    {"Robot2", new File("R1R2.csv"), null},}, new Object[]{"", "", ""});
    }

    @Nullable public File chooseFileForSaveAs(@Nullable File prevChooserFile) throws HeadlessException {
        if (null == displayJFrame) {
            throw new IllegalStateException("null == displayJFrame");
        }
        return displayJFrame.chooseFileForSaveAs(lastSetupFile);
    }

    /**
     * Query the user to select a file to save setup data in.
     */
    public void browseSaveSetupAs() {
        File chosenFile = chooseFileForSaveAs(lastSetupFile);
        if (null != chosenFile) {
            try {
                saveSetupFile(chosenFile);

            } catch (IOException ex) {
                log(Level.SEVERE, null, ex);
            }
        }
    }

    @Nullable public File chooseSetupFileToOpen(@Nullable File prevChosenFile) throws HeadlessException {
        if (null == displayJFrame) {
            throw new IllegalStateException("displayJFrame");
        }
        return displayJFrame.chooseSetupFileToOpen(prevChosenFile);
    }

    /**
     * Query the user to select a setup file to read.
     */
    public void browseOpenSetup() {
        File chosenFile = chooseSetupFileToOpen(lastSetupFile);
        if (null != chosenFile) {
            try {
                loadSetupFile(chosenFile);
            } catch (IOException ex) {
                log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Add a system to show and update the tasks and robots tables.
     *
     * @param sys system to add
     */
    public void addAprsSystem(AprsSystemInterface sys) {
        sys.setPriority(aprsSystems.size() + 1);
        sys.setVisible(true);
        sys.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        sys.setSupervisorEventLogger(this::logEvent);
        aprsSystems.add(sys);
        sys.getTitleUpdateRunnables().add(() -> {
            Utils.runOnDispatchThreadWithCatch(this::updateTasksTable);
        });
        updateTasksTable();
        updateRobotsTable();
    }

    @Nullable
    private volatile XFuture<?> lastFutureReturned = null;

    private XFuture<Void> prepAndFinishOnDispatch(Runnable r) {

        return prepActions()
                .thenRun(() -> {
                    Utils.runOnDispatchThread(() -> {
                        try {
                            r.run();
                        } catch (Exception e) {
                            log(Level.SEVERE, "", e);
                        }
                    });
                });
    }

    private <T> XFuture<T> prepAndFinishOnDispatch(Supplier<XFuture<T>> supplier) {
        return prepActions()
                .thenCompose(x -> Utils.supplyOnDispatchThread(supplier))
                .thenCompose(x -> x);
    }

    public void setAllReverseFlag(boolean reverseFlag) {
        startSetAllReverseFlag(reverseFlag).join();
    }

    XFuture<?> prepActions() {
        boolean origIgnoreFlag = ignoreTitleErrors.getAndSet(true);
        if (null != lastSafeAbortAllFuture) {
            lastSafeAbortAllFuture.cancelAll(true);
            lastSafeAbortAllFuture = null;
        }
        if (null != lastSafeAbortAllFuture2) {
            lastSafeAbortAllFuture2.cancelAll(true);
            lastSafeAbortAllFuture2 = null;
        }
        if (null != lastFutureReturned) {
            lastFutureReturned.cancelAll(true);
            lastFutureReturned = null;
        }
        firstEventTime = -1;
        XFuture<Void> xf = togglesAllowedXfuture.getAndSet(null);
        if (null != xf) {
            xf.cancelAll(true);
        }
        clearAllToggleBlockers();
        clearAllErrors();
        setPauseSelected(false);
        immediateAbortAll("prepActions", true);
        resumeForPrepOnly();
        if (!origIgnoreFlag) {
            ignoreTitleErrors.set(false);
        }
        abortEventTime = -1;
        return returnRobots("prepActions");
    }

    @Nullable public File choosePositionMappingsFileForSaveAs(@Nullable File prevChosenFile) throws HeadlessException {
        if (null == displayJFrame) {
            throw new IllegalStateException("(null == displayJFrame)");
        }
        return displayJFrame.choosePositionMappingsFileForSaveAs(prevChosenFile);
    }

    private void browseAndSavePositionMappings() throws HeadlessException {
        File chosenFile = choosePositionMappingsFileForSaveAs(lastPosMapFile);
        if (null != chosenFile) {
            try {
                savePositionMaps(chosenFile);
            } catch (IOException ex) {
                log(Level.SEVERE, null, ex);
            }
        }
    }

    @Nullable public File choosePosMapsFileToOpen(@Nullable File prevChosenFile) throws HeadlessException {
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
    public void browseOpenPosMapsFile() {
        File chosenFile = choosePosMapsFileToOpen(lastPosMapFile);
        if (null != chosenFile) {
            try {
                loadPositionMaps(chosenFile);

            } catch (IOException ex) {
                log(Level.SEVERE, null, ex);
            }
        }
    }

    private static void setChooserFile(@Nullable File file, JFileChooser chooser) {
        if (file != null) {
            File parentFile = file.getParentFile();
            if (null != parentFile) {
                chooser.setCurrentDirectory(parentFile);
                chooser.setSelectedFile(file);
            }
        }
    }

    @Nullable
    private volatile XFuture<Void> lastSafeAbortAllFuture = null;
    @Nullable
    private volatile XFuture<Void> lastSafeAbortAllFuture2 = null;
    @Nullable
    private volatile NamedCallable<XFuture<Void>> safeAbortReturnRobot = null;

    final private AtomicInteger abortCount = new AtomicInteger();

    private final AtomicBoolean ignoreTitleErrors = new AtomicBoolean(false);

    @Nullable public XFuture<Void> getLastSafeAbortAllFuture() {
        return lastSafeAbortAllFuture;
    }

    public void setLastSafeAbortAllFuture(XFuture<Void> lastSafeAbortAllFuture) {
        this.lastSafeAbortAllFuture = lastSafeAbortAllFuture;
    }

    @Nullable public XFuture<Void> getLastSafeAbortAllFuture2() {
        return lastSafeAbortAllFuture2;
    }

    public void setLastSafeAbortAllFuture2(XFuture<Void> lastSafeAbortAllFuture2) {
        this.lastSafeAbortAllFuture2 = lastSafeAbortAllFuture2;
    }

    public void fullAbortAll() {
        incrementAndGetAbortCount();
        ignoreTitleErrors.set(true);
        if (null != lastFutureReturned) {
            lastFutureReturned.cancelAll(true);
            lastFutureReturned = null;
        }
        if (null != lastSafeAbortAllFuture) {
            lastSafeAbortAllFuture.cancelAll(true);
            lastSafeAbortAllFuture = null;
        }
        if (null != lastSafeAbortAllFuture2) {
            lastSafeAbortAllFuture2.cancelAll(true);
            lastSafeAbortAllFuture2 = null;
        }
        XFuture<Void> xf = togglesAllowedXfuture.getAndSet(null);
        if (null != xf) {
            xf.cancelAll(true);
        }
        clearAllToggleBlockers();
        clearAllErrors();
        immediateAbortAll("fullAbortAll");
        clearCheckBoxes();
        mainFuture = lastFutureReturned;
        ignoreTitleErrors.set(false);
        for (XFuture<Boolean> f : systemContinueMap.values()) {
            f.cancelAll(stealingRobots);
        }
        systemContinueMap.clear();
        for (XFuture<Void> f : debugSystemContinueMap.values()) {
            f.cancelAll(stealingRobots);
        }
        debugSystemContinueMap.clear();
    }

    public void clearCheckBoxes() {
        if (null != displayJFrame) {
            displayJFrame.clearCheckBoxes();
        }
        setContinousDemoSelected(false);
        setContinousDemoRevFirstSelected(false);
        setIndContinousDemoSelected(false);
        setIndRandomToggleTestSelected(false);
        setPauseSelected(false);
        setRandomTestSelected(false);

    }

    private static void printStatus(AtomicReference<@Nullable XFuture<Void>> ref, PrintStream ps) {
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

    @Nullable public NamedCallable<XFuture<Void>> getReturnRobotNamedCallable() {
        return returnRobotRunnable.get();
    }

    private final AtomicInteger debugActionCount = new AtomicInteger();

    public void debugAction() {
        System.out.println("");
        logEventErr("");
        int count = debugActionCount.incrementAndGet();
        System.out.println("Begin AprsSupervisorJFrame.debugAction()" + count);
        System.out.println("waitForTogglesFutures = " + waitForTogglesFutures);
        System.out.println("togglesAllowed = " + togglesAllowed);
        System.out.println("disallowTogglesCount = " + disallowTogglesCount);
        System.out.println("allowTogglesCount = " + allowTogglesCount);
        System.out.println("waitForTogglesFutureCount = " + waitForTogglesFutureCount);

        System.out.println("stealingRobots = " + stealingRobots);
        System.out.println("returnRobotRunnable = " + returnRobotRunnable);

        System.out.println("lastFutureReturned = " + lastFutureReturned);
        printStatus(lastFutureReturned, System.out);
        System.out.println("continousDemoFuture = " + continousDemoFuture);
        printStatus(continousDemoFuture, System.out);

        System.out.println("randomTest = " + randomTestFuture);
        printStatus(randomTestFuture, System.out);

        System.out.println("togglesAllowedXfuture = " + togglesAllowedXfuture);
        printStatus(togglesAllowedXfuture, System.out);

        System.out.println("stealRobotFuture = " + stealRobotFuture);
        printStatus(stealRobotFuture, System.out);

        System.out.println("unStealRobotFuture = " + unStealRobotFuture);
        printStatus(unStealRobotFuture, System.out);
        System.out.println("cancelStealRobotFuture = " + cancelStealRobotFuture);
        printStatus(cancelStealRobotFuture, System.out);

        System.out.println("cancelUnStealRobotFuture = " + cancelUnStealRobotFuture);
        printStatus(cancelUnStealRobotFuture, System.out);

        System.out.println("stealAbortFuture = " + stealAbortFuture);
        printStatus(stealAbortFuture, System.out);

        System.out.println("unstealAbortFuture = " + unstealAbortFuture);
        printStatus(unstealAbortFuture, System.out);

        System.out.println("oldLfrs = " + oldLfrs);
        for (int i = 0; i < oldLfrs.size(); i++) {
            XFuture<?> xf = oldLfrs.get(i);
            if (!xf.isDone() || xf.isCancelled() || xf.isCompletedExceptionally()) {
                System.out.println("oldLfrs.get(" + i + ") = " + xf);
                printStatus(xf, System.out);
            }
        }

        XFuture<?> xfa[] = lastStartAllActionsArray;
        if (null != xfa) {
            System.out.println("lastStartAllActionsArray = " + Arrays.toString(xfa));
            for (int i = 0; i < xfa.length; i++) {
                XFuture<?> xf = xfa[i];
                if (!xf.isDone() || xf.isCancelled() || xf.isCompletedExceptionally()) {
                    System.out.println("oldLfrs.get(" + i + ") = " + xf);
                    printStatus(xf, System.out);
                }
            }
        }

        for (int i = 0; i < aprsSystems.size(); i++) {
            aprsSystems.get(i).debugAction();
        }

        printReturnRobotTraceInfo();

        System.out.println("End AprsSupervisorJFrame.debugAction()" + count);
        System.out.println("");
        logEventErr("");

    }

    private volatile boolean closing = false;

    public void setVisible(boolean visible) {
        if (null != displayJFrame) {
            displayJFrame.setVisible(visible);
        }
    }

    public void close() {
        closing = true;
        try {
            finishEncodingLiveImageMovie();
        } catch (Exception ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (null != runTimeTimer) {
            runTimeTimer.stop();
            runTimeTimer = null;
        }
        stopColorTextReader();
        closeAllAprsSystems();
        if (null != aprsSystems) {
            for (AprsSystemInterface sys : aprsSystems) {
                sys.forceClose();
            }
        }
        if (null != lastFutureReturned) {
            lastFutureReturned.cancelAll(true);
        }
        for (XFuture<Void> xf : waitForTogglesFutures) {
            xf.cancelAll(true);
        }
        if (null != continousDemoFuture) {
            continousDemoFuture.cancelAll(true);
        }
        if (null != togglesAllowedXfuture) {
            XFuture<Void> xf = togglesAllowedXfuture.get();
            if (null != xf) {
                xf.cancelAll(true);
            }
        }
        if (null != stealAbortFuture) {
            stealAbortFuture.cancelAll(true);
        }
        if (null != unstealAbortFuture) {
            unstealAbortFuture.cancelAll(true);
        }
        if (null != stealRobotFuture) {
            XFuture<Void> xf = stealRobotFuture.get();
            if (null != xf) {
                xf.cancelAll(true);
            }
        }
        if (null != unStealRobotFuture) {
            XFuture<Void> xf = unStealRobotFuture.get();
            if (null != xf) {
                xf.cancelAll(true);
            }
        }
        if (null != cancelStealRobotFuture) {
            XFuture<Void> xf = cancelStealRobotFuture.get();
            if (null != xf) {
                xf.cancelAll(true);
            }
        }
        if (null != cancelUnStealRobotFuture) {
            XFuture<Void> xf = cancelUnStealRobotFuture.get();
            if (null != xf) {
                xf.cancelAll(true);
            }
        }
        if (null != mainFuture) {
            mainFuture.cancelAll(true);
        }
        if (null != randomTestFuture) {
            randomTestFuture.cancelAll(true);
        }

        this.setVisible(false);
        supervisorExecutorService.shutdownNow();
        try {
            supervisorExecutorService.awaitTermination(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
        }
        randomDelayExecutorService.shutdownNow();
        try {
            randomDelayExecutorService.awaitTermination(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (null != processLauncher) {
            processLauncher.close()
                    .thenRun(this::myExit);

        } else {
            myExit();
        }

    }

    private void myExit() {
        if (null != displayJFrame) {
            displayJFrame.setVisible(false);
            displayJFrame.removeAll();
            displayJFrame.dispose();
        }
        System.exit(0);
    }

    @Nullable
    private volatile XFuture<Void> continousDemoFuture = null;
    @Nullable
    private volatile XFuture<?> mainFuture = null;

    public void setContinousDemoFuture(XFuture<Void> continousDemoFuture) {
        this.continousDemoFuture = continousDemoFuture;
    }

    @Nullable public XFuture<Void> getContinousDemoFuture() {
        return continousDemoFuture;
    }

    @Nullable public XFuture<?> getMainFuture() {
        return mainFuture;
    }

    public void setMainFuture(XFuture<?> mainFuture) {
        this.mainFuture = mainFuture;
    }

    private final AtomicReference<@Nullable XFuture<Void>> resumeFuture = new AtomicReference<>(null);

    @Nullable
    private volatile XFuture<Void> randomTestFuture = null;

    @Nullable public XFuture<Void> getRandomTestFuture() {
        return randomTestFuture;
    }

    public void setRandomTestFuture(@Nullable XFuture<Void> randomTestFuture) {
        this.randomTestFuture = randomTestFuture;
    }

    public void clearEventLog() {
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
    public XFuture<Void> resetAll(boolean reloadSimFiles) {
        if (null != lastFutureReturned) {
            lastFutureReturned.cancelAll(true);
            lastFutureReturned = null;
        }
        if (null != randomTestFuture) {
            randomTestFuture.cancelAll(true);
            randomTestFuture = null;
        }
        if (null != continousDemoFuture) {
            continousDemoFuture.cancelAll(true);
            continousDemoFuture = null;
        }
        immediateAbortAll("resetAll");
        clearCheckBoxes();
        @SuppressWarnings("unchecked")
        XFuture<Void> allResetFutures[] = (XFuture<Void>[]) new XFuture<?>[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystemInterface sys = aprsSystems.get(i);
            allResetFutures[i] = sys.reset(reloadSimFiles);
            sys.setCorrectionMode(false);
        }
        abortEventTime = -1;
        firstEventTime = -1;
        return XFuture.allOf(allResetFutures);

    }

    int resetMainPauseCount = 0;

    public void resetMainPauseTestFuture() {
        resetMainPauseCount++;
        if (null == continousDemoFuture) {
            if (null == randomTestFuture) {
                if (null == pauseTestFuture) {
                    mainFuture = (XFuture<?>) XFuture.completedFutureWithName("resetMainPauseTestFuture" + resetMainPauseCount, null);
                    return;
                }
                mainFuture = (XFuture<?>) XFuture.allOfWithName("resetMainPauseTestFuture" + resetMainPauseCount, pauseTestFuture);
                return;
            }
            if (null == pauseTestFuture) {
                mainFuture = (XFuture<?>) XFuture.allOfWithName("resetMainPauseTestFuture" + resetMainPauseCount, randomTestFuture);
                return;
            }
            mainFuture = (XFuture<?>) XFuture.allOfWithName("resetMainPauseTestFuture" + resetMainPauseCount, randomTestFuture, pauseTestFuture);
            return;
        }
        if (null == randomTestFuture) {
            if (null == pauseTestFuture) {
                mainFuture = (XFuture<?>) XFuture.allOfWithName("resetMainPauseTestFuture" + resetMainPauseCount, continousDemoFuture);
                return;
            }
            mainFuture = (XFuture<?>) XFuture.allOfWithName("resetMainPauseTestFuture" + resetMainPauseCount, continousDemoFuture, pauseTestFuture);
            return;
        }
        if (null == pauseTestFuture) {
            mainFuture = (XFuture<?>) XFuture.allOfWithName("resetMainPauseTestFuture" + resetMainPauseCount, continousDemoFuture, randomTestFuture);
            return;
        }
        mainFuture = (XFuture<?>) XFuture.allOfWithName("resetMainPauseTestFuture" + resetMainPauseCount, continousDemoFuture, randomTestFuture, pauseTestFuture);
    }

    public XFuture<Void> startContinuousDemoRevFirst() {
        setContinousDemoSelected(false);
        return prepAndFinishOnDispatch(this::startContinuousDemoRevFirstFinish);
    }

    private XFuture<Void> startContinuousDemoRevFirstFinish() {
        immediateAbortAll("startContinuousDemoRevFirst");
        clearEventLog();
        clearAllErrors();
        connectAll();
        setAllReverseFlag(false);
        enableAllRobots();
        continousDemoCycle.set(0);
        if (!isContinousDemoRevFirstSelected()) {
            setContinousDemoRevFirstSelected(true);
        }
        XFuture<Void> ret = startPrivateContinuousDemoRevFirst();
        continousDemoFuture = ret;
        mainFuture = continousDemoFuture;
        return ret;
    }

    public XFuture<Void> startRandomTestFirstActionReversed() {
        try {
            setContinousDemoRevFirstSelected(true);
            setRandomTestSelected(true);
            return prepAndFinishOnDispatch(() -> {
                try {
                    immediateAbortAll("jMenuItemRandomTestReverseFirstActionPerformed");
                    XFuture<Void> outerRet
                            = resetAll(false)
                                    .thenCompose(x -> {
                                        XFuture<Void> innerRet = Utils.supplyOnDispatchThread(() -> {
                                            try {
                                                clearAllErrors();
                                                connectAll();
                                                setAllReverseFlag(true);
                                                enableAllRobots();
                                                continousDemoCycle.set(0);
                                                clearRandomTestCount();
                                                setContinousDemoRevFirstSelected(true);
                                                setRandomTestSelected(true);
                                                lastFutureReturned = null;
                                                XFuture<Void> ret = startRandomTest();
                                                mainFuture = ret;
                                                return ret;
                                            } catch (Exception e) {
                                                Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, e);
                                                showMessageDialog("Exception occurred: " + e);
                                                XFuture<Void> ret = new XFuture<>("internal startRandomTestFirstActionReversed with exception " + e);
                                                ret.completeExceptionally(e);
                                                return ret;
                                            }
                                        }).thenCompose(x3 -> x3);
                                        return innerRet;
                                    });
                    return outerRet;
                } catch (Exception e) {
                    Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, e);
                    showMessageDialog("Exception occurred: " + e);
                    XFuture<Void> ret = new XFuture<>("internal startRandomTestFirstActionReversed with exception " + e);
                    ret.completeExceptionally(e);
                    return ret;
                }
            });
        } catch (Exception e) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, e);
            showMessageDialog("Exception occurred: " + e);
            XFuture<Void> ret = new XFuture<Void>("startRandomTestFirstActionReversed with exception " + e);
            ret.completeExceptionally(e);
            return ret;
        }
    }

    int resetMainRandomTestCount = 0;

    private void resetMainRandomTestFuture() {
//        assert (randomTest != null) : "(randomTest == null) :  @AssumeAssertion(nullness)";
//        assert (continousDemoFuture != null) : "(continousDemoFuture == null)  :  @AssumeAssertion(nullness)";

        if (null != randomTestFuture && null != continousDemoFuture) {
            resetMainRandomTestCount++;
            mainFuture = (XFuture<?>) XFuture.allOfWithName("resetMainRandomTestFuture" + resetMainRandomTestCount, randomTestFuture, continousDemoFuture);
            mainFuture.exceptionally((thrown) -> {
                if (thrown != null) {
                    log(Level.SEVERE, "", thrown);
                }
                if (thrown instanceof RuntimeException) {
                    throw (RuntimeException) thrown;
                }
                throw new RuntimeException(thrown);
            });
        }
    }

    private int randomTestSeed = 959;

    public int getRandomTestSeed() {
        return randomTestSeed;
    }

    public void setRandomTestSeed(int randomTestSeed) {
        this.randomTestSeed = randomTestSeed;
    }

    private static String getDirNameOrHome(@Nullable File f) throws IOException {
        if (f != null) {
            File parentFile = f.getParentFile();
            if (null != parentFile) {
                return parentFile.getCanonicalPath();
            }
        }
        return System.getProperty("user.home");
    }

    private static String canonicalPathOrBuildPath(@Nullable File f, String dirName, String filename) throws IOException {
        if (null != f) {
            return f.getCanonicalPath();
        }
        return dirName + File.separator + filename;
    }

    /**
     * Get the first system with a task name that starts with the given string.
     *
     * @param s name/prefix of task to look for
     * @return system with given task
     */
    @Nullable
    public AprsSystemInterface getSysByTask(String s) {
        for (AprsSystemInterface sys : aprsSystems) {
            if (sys.getTaskName().startsWith(s)) {
                return sys;
            }
        }
        return null;
    }

    private final String INIT_CUSTOM_CODE = "package custom;\n"
            + "import aprs.framework.*; \n"
            + "import java.util.function.Consumer;\n\n"
            + "public class Custom\n\timplements Consumer<AprsSupervisorJFrame> {\n"
            + "\tpublic void accept(AprsSupervisorJFrame sup) {\n"
            + "\t\t// PUT YOUR CODE HERE:\n"
            + "\t\tSystem.out.println(\"sys = \"+sup.getSysByTask(\"Fanuc Cart\"));"
            + "\t}\n"
            + "}\n";

    private String customCode = INIT_CUSTOM_CODE;
    private volatile int max_cycles = -1;

    public void setShowFullScreenMessages(boolean showFullScreenMessages) {
        if (null != displayJFrame) {
            displayJFrame.setShowFullScreenMessages(showFullScreenMessages);
        }
    }

    @SuppressWarnings("nullness")
    private void runCustomCode() {
        try {

            customCode = MultiLineStringJPanel.editText(customCode);
            File customDir = Paths.get(System.getProperty("user.home"), ".aprs", "custom").toFile();
            customDir.delete();
            customDir.mkdirs();
            File tmpFile = new File(customDir, "Custom.java");
            System.out.println("tmpFile = " + tmpFile.getCanonicalPath());
            File[] files1 = {tmpFile};

            Files.write(tmpFile.toPath(), customCode.getBytes());
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (null != compiler) {
                ClassLoader cl = ClassLoader.getSystemClassLoader();

                URL[] origUrls = ((URLClassLoader) cl).getURLs();

                StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

                Iterable<? extends JavaFileObject> compilationUnits1
                        = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(files1));
                String classPath = Arrays.stream(origUrls)
                        .map(Objects::toString)
                        .map(s -> s.startsWith("file:") ? s.substring(4) : s)
                        .collect(Collectors.joining(File.pathSeparator));
                System.out.println("classPath = " + classPath);
                DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
                compiler.getTask(null, fileManager, diagnostics, Arrays.asList("-cp", classPath), null, compilationUnits1).call();
                StringBuilder errBuilder = new StringBuilder();
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    String err = String.format("%s:%d %s %n",
                            diagnostic.getSource().toUri(),
                            diagnostic.getLineNumber(),
                            diagnostic.getMessage(Locale.US));
                    errBuilder.append(err);
                }
                String fullErr = errBuilder.toString();
                boolean origDisableShowText = crcl.ui.misc.MultiLineStringJPanel.disableShowText;
                if (fullErr.length() > 0) {
                    crcl.ui.misc.MultiLineStringJPanel.disableShowText = false;
                    MultiLineStringJPanel.showText(fullErr).thenRun(() -> crcl.ui.misc.MultiLineStringJPanel.disableShowText = origDisableShowText);
                    if (!customCode.contains("class Custom")) {
                        customCode = INIT_CUSTOM_CODE;
                    }
                    return;
                }
                URL[] urls = new URL[origUrls.length + 1];
                System.arraycopy(origUrls, 0, urls, 0, origUrls.length);
                File parentFile = tmpFile.getAbsoluteFile().getParentFile();
                if (null == parentFile) {
                    logEventErr("Temporary file " + tmpFile + " does not have parent.");
                    return;
                }
                File grandParentFile = parentFile.getParentFile();
                if (null == grandParentFile) {
                    logEventErr("Temporary file " + tmpFile + " does not have grandparent.");
                    return;
                }
                urls[urls.length - 1] = grandParentFile.toURI().toURL();
                //tmpFile.getAbsoluteFile().getParentFile().getParentFile().toURI().toURL()};
                System.out.println("urls = " + Arrays.toString(urls));
                ClassLoader loader = new URLClassLoader(urls);
                Class<?> clss = loader.loadClass("custom.Custom");
                @SuppressWarnings("deprecation")
                Object obj = clss.newInstance();
                Method acceptMethod = clss.getMethod("accept", Supervisor.class);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream origOut = System.out;

                try (PrintStream ps = new PrintStream(baos)) {
                    System.setOut(ps);
                    acceptMethod.invoke(obj, this);
                    String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    System.setOut(origOut);
                    System.out.println("content = " + content);
                    if (content.length() > 0) {
                        crcl.ui.misc.MultiLineStringJPanel.disableShowText = false;
                        MultiLineStringJPanel.showText(content).thenRun(() -> crcl.ui.misc.MultiLineStringJPanel.disableShowText = origDisableShowText);

                    }
                } finally {
                    crcl.ui.misc.MultiLineStringJPanel.disableShowText = origDisableShowText;
                    System.setOut(origOut);
                }
            }
        } catch (Exception exception) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, exception);
            StringWriter sw = new StringWriter();
            exception.printStackTrace(new PrintWriter(sw));
            String trace = sw.toString();
            boolean origDisableShowText = crcl.ui.misc.MultiLineStringJPanel.disableShowText;
            crcl.ui.misc.MultiLineStringJPanel.disableShowText = false;
            MultiLineStringJPanel.showText(trace).thenRun(() -> crcl.ui.misc.MultiLineStringJPanel.disableShowText = origDisableShowText);
            if (!customCode.contains("class Custom")) {
                customCode = INIT_CUSTOM_CODE;
            }
        }
    }

    private void setTeachSystemFilter(@Nullable AprsSystemInterface sys) {
        if (null == sys) {
            object2DOuterJPanel1.setForceOutputFlag(false);
            object2DOuterJPanel1.setShowOutputItems(false);
            object2DOuterJPanel1.setOutputItems(object2DOuterJPanel1.getItems());
        } else {
            object2DOuterJPanel1.setForceOutputFlag(true);
            object2DOuterJPanel1.setSimulatedAndDisconnect();
            object2DOuterJPanel1.setShowOutputItems(true);
            object2DOuterJPanel1.setOutputItems(filterForSystem(sys, object2DOuterJPanel1.getItems()));
        }
    }

    @Nullable
    private static PhysicalItem closestPart(double sx, double sy, List<PhysicalItem> items) {
        return items.stream()
                .filter(x -> x.getType().equals("P"))
                .min(Comparator.comparing(pitem -> Math.hypot(sx - pitem.x, sy - pitem.y)))
                .orElse(null);
    }

    private Stream<Slot> absSlotStreamFromTrayAndOffset(PhysicalItem tray, Slot offsetItem) {
        Slot slot = slotOffsetProvider.absSlotFromTrayAndOffset(tray, offsetItem, 0);
        if (null != slot) {
            return Stream.of(slot);
        }
        return Stream.empty();
    }

    public double getClosestSlotDist(Collection<PhysicalItem> kitTrays, PhysicalItem item) {
        return kitTrays.stream()
                .flatMap(kit -> slotOffsetProvider.getSlotOffsets(kit.getName(), false).stream()
                .flatMap(slotOffset -> absSlotStreamFromTrayAndOffset(kit, slotOffset)))
                .mapToDouble(slot -> item.dist(slot))
                .min().orElse(Double.POSITIVE_INFINITY);
    }

    public List<PhysicalItem> filterForSystem(AprsSystemInterface sys, List<PhysicalItem> listIn) {

        Set<PhysicalItem> allTrays = listIn.stream()
                .filter(x -> "KT".equals(x.getType()) || "PT".equals(x.getType()))
                .collect(Collectors.toSet());
        Set<PhysicalItem> kitTrays = listIn.stream()
                .filter(x -> "KT".equals(x.getType()))
                .collect(Collectors.toSet());
        Set<PhysicalItem> sysKitTrays = kitTrays.stream()
                .filter(tray -> {
                    List<Slot> l2 = sys.getSlotOffsets(tray.getName(), true);
                    return l2 != null && !l2.isEmpty();
                }).collect(Collectors.toSet());
        Set<PhysicalItem> otherSysTrays = new HashSet<>(allTrays);
        otherSysTrays.removeAll(sysKitTrays);
        List<PhysicalItem> listOut = new ArrayList<>(sysKitTrays);
        for (PhysicalItem item : listIn) {
            if ("P".equals(item.getType())) {
                double sysKitDist = getClosestSlotDist(sysKitTrays, item);
                double otherKitDist = getClosestSlotDist(otherSysTrays, item);
                if (sysKitDist < otherKitDist) {
                    listOut.add(item);
                }
            }
        }
        return listOut;
    }

    private XFuture<Void> lookForPartsAll() {
        XFuture<?> futures[] = new XFuture<?>[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystemInterface aprsSys = aprsSystems.get(i);
            futures[i] = aprsSys.startLookForParts();
        }
        return XFuture.allOfWithName("lookForPartsAll", futures);
    }

    private XFuture<Void> clearReverseAll() {
        XFuture<?> futures[] = new XFuture<?>[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystemInterface aprsSys = aprsSystems.get(i);
            if (aprsSys.isReverseFlag()) {
                logEvent("Set reverse flag false for " + aprsSys);
                futures[i] = aprsSys.startSetReverseFlag(false, false);
            } else {
                futures[i] = XFuture.completedFuture(null);
            }
        }
        return XFuture.allOfWithName("clearReverseAll", futures);
    }

    private void completeScanAllInternal() {
        List<PhysicalItem> teachItems = Collections.emptyList();
        if (isUseTeachCameraSelected()) {
            teachItems = object2DOuterJPanel1.getItems();
        }
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystemInterface aprsSys = aprsSystems.get(i);
            aprsSys.setCorrectionMode(false);
            if (isUseTeachCameraSelected() && aprsSys.getUseTeachTable()) {
                aprsSys.createActionListFromVision(aprsSys.getObjectViewItems(), filterForSystem(aprsSys, teachItems), true, 0);
            } else {
                aprsSys.createActionListFromVision();
            }
        }
    }

    public void completeScanTillNewInternal() {
        int startingAbortCount = abortCount.get();
        boolean anyChanged = false;
        int cycles = 0;
        long start_time = System.currentTimeMillis();
        long last_time = start_time;
        long max_time_diff = 0;
        long max_kitTrays = 0;
        int skips = 0;

        OUTER_WHILE:
        while (!anyChanged
                && abortCount.get() == startingAbortCount
                && isContinousDemoSelected()
                && !closing) {
            cycles++;
            List<PhysicalItem> teachItems = Collections.emptyList();
            if (isUseTeachCameraSelected()) {
                if (object2DOuterJPanel1.isPartMoving()) {
                    try {
                        skips++;
                        Thread.sleep(50);
                    } catch (InterruptedException ex) {
                        if (!closing) {
                            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        return;
                    }
                    continue OUTER_WHILE;
                }
                teachItems = object2DOuterJPanel1.getItems();
                long kitTrays = teachItems
                        .stream()
                        .filter((PhysicalItem item) -> item.getType().equals("KT"))
                        .count();
                if (max_kitTrays < kitTrays) {
                    max_kitTrays = kitTrays;
                } else if (kitTrays < max_kitTrays) {
                    // lost a kitTray, here we assume the vision frame is bad
                    try {
                        skips++;
                        Thread.sleep(50);
                    } catch (InterruptedException ex) {
                        if (!closing) {
                            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        return;
                    }
                    continue OUTER_WHILE;
                }
            }
            for (int i = 0; i < aprsSystems.size(); i++) {
                AprsSystemInterface aprsSys = aprsSystems.get(i);
                List<String> startingKitStrings = aprsSys.getLastCreateActionListFromVisionKitToCheckStrings();
                aprsSys.setCorrectionMode(true);
                if (isUseTeachCameraSelected() && aprsSys.getUseTeachTable()) {
                    aprsSys.createActionListFromVision(aprsSys.getObjectViewItems(), filterForSystem(aprsSys, teachItems), true, 0);
                } else {
                    aprsSys.createActionListFromVision();
                }
                List<String> endingKitStrings = aprsSys.getLastCreateActionListFromVisionKitToCheckStrings();
                if (endingKitStrings.size() < startingKitStrings.size()) {
                    // lost a kitTray, here we assume the vision frame is bad
                    aprsSys.setLastCreateActionListFromVisionKitToCheckStrings(startingKitStrings);
                    try {
                        skips++;
                        Thread.sleep(50);
                    } catch (InterruptedException ex) {
                        if (!closing) {
                            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        return;
                    }
                    continue OUTER_WHILE;
                }
                boolean equal = GoalLearner.kitToCheckStringsEqual(startingKitStrings, endingKitStrings);
                if (!equal) {
                    anyChanged = true;
                }
            }
            long now = System.currentTimeMillis();
            long diff = now - last_time;
            if (max_time_diff < diff) {
                max_time_diff = diff;
            }
            last_time = now;
            if (anyChanged) {
                logEvent("completeScanTillNewInternal saw new after cycles=" + cycles + ", (now-start_time)=" + (now - start_time) + ",max_time_diff=" + max_time_diff + ",diff=" + diff + ",skips=" + skips);
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                if (!closing) {
                    Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
                }
                return;
            }
        }
    }

    public XFuture<Void> scanAllInternal() {
        return lookForPartsAll()
                .thenComposeAsync("scanAllInternal.clearReverseAll", x -> clearReverseAll(), supervisorExecutorService)
                .thenRunAsync("completeScanAllInternal", this::completeScanAllInternal, supervisorExecutorService)
                .thenCompose(x -> showScanCompleteDisplay());
    }

    public XFuture<Void> scanTillNewInternal() {
        return lookForPartsAll()
                .thenComposeAsync("scanAllInternal.clearReverseAll", x -> clearReverseAll(), supervisorExecutorService)
                .thenRunAsync("completeScanAllInternal", this::completeScanTillNewInternal, supervisorExecutorService)
                .thenCompose(x -> showScanCompleteDisplay());
    }

    private XFuture<Void> showScanCompleteDisplay() {
        logEvent("Scans Complete");
        setAbortTimeCurrent();
        if (null != displayJFrame) {
            return displayJFrame.showScanCompleteDisplay();
        } else {
            return XFutureVoid.completedFutureWithName("showScanCompleteDisplay");
        }
    }

    private XFuture<Void> showAllTasksCompleteDisplay() {
        logEvent("All Tasks Complete");
        setAbortTimeCurrent();
        if (null != displayJFrame) {
            return displayJFrame.showAllTasksCompleteDisplay();
        } else {
            return XFutureVoid.completedFutureWithName("showAllTasksCompleteDisplay");
        }
    }

    private volatile XFuture<?> lastStartScanAllFutures @Nullable []  = null;

    public XFuture<?> startScanAllThenContinuousDemoRevFirst() {
        logEvent("startScanAllThenContinuousDemoRevFirst starting ...");
        XFuture<@Nullable Void> xf1 = this.safeAbortAll();
        XFuture<?> xf2 = xf1
                .thenCompose("startScanAllThenContinuousDemoRevFirst.step2", x -> {
                    logEvent("startScanAllThenContinuousDemoRevFirst.step2 : xf1=" + xf1);
                    return startScanAll();
                });
        XFuture<?> xf3 = xf2
                .thenCompose("startScanAllThenContinuousDemoRevFirst.step3", x -> {
                    logEvent("startScanAllThenContinuousDemoRevFirst.step2 : xf2=" + xf2);
                    return startContinuousDemoRevFirst();
                });
        return xf3;
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
    public XFuture<?> startScanAll() {

        @SuppressWarnings({"unchecked", "rawtypes"})
        XFuture xf[] = new XFuture[6];

        XFuture<Void> resetAllXF = resetAll(false);
        xf[0] = resetAllXF;
        XFuture<?> step2Xf = resetAllXF
                .thenCompose(x -> {
                    Utils.SwingFuture<XFuture<?>> supplyFuture = Utils.supplyOnDispatchThread(() -> {
                        logEvent("Scan all started.");
                        XFuture<Boolean> startCheckAndEnableXF = startCheckAndEnableAllRobots();
                        xf[3] = startCheckAndEnableXF;
                        XFuture<?> scanAll2XF
                                = startCheckAndEnableXF
                                        .thenComposeAsync("scanAll2",
                                                ok -> {
                                                    return checkOkElse(ok, this::scanAllInternal, this::showCheckEnabledErrorSplash);
                                                }, supervisorExecutorService);
                        xf[4] = scanAll2XF;
                        return scanAll2XF;
                    });
                    xf[2] = supplyFuture;
                    XFuture<?> extractedFuture = supplyFuture.thenCompose(x2 -> x2);
                    xf[5] = extractedFuture;
                    return extractedFuture;
                });
        xf[1] = step2Xf;
        lastStartScanAllFutures = xf;
        return step2Xf;
    }

    private final AtomicInteger srtCount = new AtomicInteger();

    /**
     * Perform a test of the continous demo where the motoman robot will be
     * randomly enabled and disabled.
     *
     * @return a future that can be used to determine if the test failed or was
     * cancelled.
     */
    public XFuture<Void> startRandomTest() {
        int c = srtCount.incrementAndGet();
        logEvent("Start Random Test : " + c);
        connectAll();
        return startCheckAndEnableAllRobots()
                .thenCompose("startRandomTest.checkOk" + c,
                        ok -> checkOkElse(ok, this::startRandomTestStep2, this::showCheckEnabledErrorSplash));
    }

    private final AtomicInteger srts2Count = new AtomicInteger();

    private XFuture<Void> startRandomTestStep2() {
        int c = srts2Count.incrementAndGet();
        if (c > 1) {
            throw new IllegalStateException("c=" + c);
        }
        logEvent("Start Random Test Step 2 :" + c);
        XFuture<Void> f1;
        if (!isContinousDemoRevFirstSelected()) {
            f1 = startContinousDemo();
        } else {
            f1 = startPrivateContinuousDemoRevFirst();
        }
        continousDemoFuture = f1;
        setContinousDemoSelected(true);
        XFuture<Void> f2 = continueRandomTest();
        randomTestFuture = f2;
        resetMainRandomTestFuture();
        return XFuture.allOfWithName("startRandomTestStep2.allOff" + c, f2, f1);
    }

    private XFuture<Void> startIndRandomTestStep2() {
        XFuture<Void> f1 = startAllIndContinousDemo();
        continousDemoFuture = f1;
        setContinousDemoSelected(true);
        XFuture<Void> f2 = continueRandomTest();
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

    private XFuture<Void> startRandomDelay(String name, final int millis, final int min_millis) {
        final long val = random.nextInt(millis) + 10 + min_millis;
        return XFuture.runAsync(name + ".randomDelay(" + millis + ":" + val + ")",
                () -> {
                    try {
                        Thread.sleep(val);
                    } catch (InterruptedException ex) {
                        log(Level.SEVERE, null, ex);
                    }
                },
                randomDelayExecutorService);

    }

    private static Logger getLogger() {
        return Logger.getLogger(Supervisor.class
                .getName());
    }

    private volatile boolean togglesAllowed = false;
    private final AtomicInteger waitForTogglesFutureCount;

    private final ConcurrentLinkedDeque<XFuture<Void>> waitForTogglesFutures;

    private XFuture<Void> createWaitForTogglesFuture(@Nullable XFuture<Void> old) {
        if (null != old) {
            return old;
        }
        XFuture<Void> xf = new XFuture<>("waitForTogglesAllowed" + waitForTogglesFutureCount.incrementAndGet());
        waitForTogglesFutures.add(xf);
        return xf;
    }

    private static XFuture<Void> createFirstWaitForTogglesFuture(Deque<XFuture<Void>> waitForTogglesFutures, AtomicInteger waitForTogglesFutureCount) {
        XFuture<Void> xf = new XFuture<>("waitForTogglesAllowed" + waitForTogglesFutureCount.incrementAndGet());
        waitForTogglesFutures.add(xf);
        return xf;
    }

    private final AtomicReference<@Nullable XFuture<Void>> togglesAllowedXfuture;

    public XFuture<Void> waitTogglesAllowed() {
        XFuture<Void> xf = togglesAllowedXfuture.getAndSet(null);
        if (null != xf) {
            return xf;
        }
        return XFuture.completedFutureWithName("completedWaitTogglesAllowed", (Void) null);
    }

    private volatile String roboteEnableToggleBlockerText = "";

    private void setRobotEnableToggleBlockerText(String text) {
        if (null != displayJFrame) {
            displayJFrame.setRobotEnableToggleBlockerText(text);
        }
        roboteEnableToggleBlockerText = text;
    }

    public void clearAllToggleBlockers() {
        allowTogglesCount.incrementAndGet();
        for (LockInfo li : toggleBlockerMap.values()) {
            li.getFuture().cancelAll(true);
        }
        toggleBlockerMap.clear();
        togglesAllowed = true;
        String blockerList = toggleBlockerMap.toString();
        Utils.runOnDispatchThread(() -> {
            showTogglesEnabled(true);
            setRobotEnableToggleBlockerText(blockerList);
        });
        if (togglesAllowed) {
            XFuture<Void> xf = togglesAllowedXfuture.get();
            if (null != xf) {
                xf.complete((Void) null);
            }
            while ((xf = waitForTogglesFutures.poll()) != null) {
                xf.complete((Void) null);
            }
        }
    }

    private final AtomicLong totalBlockTime = new AtomicLong();

    public void allowToggles(String blockerName, AprsSystemInterface... systems) {

        if (closing) {
            return;
        }
        try {
            if (null != systems && systems.length > 0) {
                for (AprsSystemInterface sys : systems) {
                    if (!checkMaxCycles()) {
                        break;
                    } else if (sys.getRobotName() == null || !sys.isConnected() || sys.isAborting()) {
                        String badStateMsg = "allowToggles(" + blockerName + ") : bad state for " + sys;
                        logEvent(badStateMsg);
                        throw new IllegalStateException(badStateMsg);
                    }
                }
            }
            boolean origTogglesAllowed = togglesAllowed;
            allowTogglesCount.incrementAndGet();
            LockInfo lockInfo = toggleBlockerMap.remove(blockerName);
            String blockerList = toggleBlockerMap.keySet().toString();

            if (null == lockInfo) {
                logEvent("allowToggle called for blocker " + blockerName + " not in toggleBlockerMap " + toggleBlockerMap);
            } else {
                long time = lockInfo.getStartTime();
                long blockTime = (System.currentTimeMillis() - time);
                togglesAllowed = toggleBlockerMap.isEmpty();
                if (togglesAllowed && !origTogglesAllowed) {
                    totalBlockTime.addAndGet(blockTime);
                }
                logEvent("allowToggles(" + blockerName + ") after " + blockTime + "ms : blockers=" + blockerList + ", totalBlockTime=" + (totalBlockTime.get() / 1000) + "s");

                final boolean showTogglesEnabledArg = togglesAllowed;
                Utils.runOnDispatchThread(() -> {
                    if (closing) {
                        return;
                    }
                    showTogglesEnabled(showTogglesEnabledArg);
                    setRobotEnableToggleBlockerText(blockerList);
                    XFuture.runAsync("finishAllowToggle." + blockerName, () -> finishAllowToggles(lockInfo), supervisorExecutorService);
                });
            }

        } catch (Exception ex) {
            log(Level.SEVERE, "", ex);
        }
    }

    private void finishAllowToggles(@Nullable LockInfo lockInfo) {
        if (togglesAllowed) {
            XFuture<Void> xf = togglesAllowedXfuture.get();
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

    public LockInfo disallowToggles(String blockerName, AprsSystemInterface... systems) {

        disallowTogglesCount.incrementAndGet();
        LockInfo lockInfo = new LockInfo(blockerName);
        toggleBlockerMap.put(blockerName, lockInfo);
        String blockerList = toggleBlockerMap.keySet().toString();
        logEvent("disallowToggles(" + blockerName + ") blockers=" + blockerList);
        togglesAllowed = false;
        togglesAllowedXfuture.updateAndGet(this::createWaitForTogglesFuture);
        if (null != systems) {
            for (AprsSystemInterface sys : systems) {
                addFinishBlocker(sys.getMyThreadId(), lockInfo.getFuture());
            }
        }
        Utils.runOnDispatchThread(() -> {
            showTogglesEnabled(false);
            setRobotEnableToggleBlockerText(blockerList);
        });
        return lockInfo;
    }

    private void showTogglesEnabled(boolean enabled) {
        if (null != displayJFrame) {
            displayJFrame.showTogglesEnabled(enabled);
        }
    }

    // stupid hard-coded hack to match demo
    private static final String MOTOMAN_NAME = "motoman";
    private static final String SIM_MOTOMAN_NAME = "sim_motoman";
    private static final Set<String> robotsThatCanBeDisabled
            = new HashSet<>(Arrays.asList(MOTOMAN_NAME, SIM_MOTOMAN_NAME));

    public XFuture<Boolean> toggleRobotEnabled(String robotName, boolean wasEnabled) {
        XFuture<Boolean> future
                = (null != displayJFrame)
                        ? displayJFrame.setTableRobotEnabled(robotName, !wasEnabled)
                        : noDisplayToggleRobotEnable(robotName, wasEnabled);
        return future;
    }

    private XFuture<Boolean> noDisplayToggleRobotEnable(String robotName, boolean wasEnabled) {
        return XFuture.supplyAsync("completeToggleRobotEnable",
                () -> {
                    if (isTogglesAllowed()) {
                        setRobotEnabled(robotName, !wasEnabled);
                        return true;
                    }
                    return false;
                },
                supervisorExecutorService);
    }

    public XFuture<Boolean> toggleRobotEnabled() {
        if (isPauseSelected()) {
            return waitResume().thenApply(x -> false);
        }
        if (!togglesAllowed) {
            return XFuture.completedFutureWithName("!togglesAllowd", false);
        }
        for (Map.Entry<String, Boolean> robotEnableEntry : robotEnableMap.entrySet()) {
            String robotName = robotEnableEntry.getKey();
            Boolean wasEnabled = robotEnableEntry.getValue();
            if (null == wasEnabled) {
                throw new IllegalStateException("wasEnabled ==null for " + robotName);
            }
            if (robotsThatCanBeDisabled.contains(robotName.toLowerCase())) {
                return toggleRobotEnabled(robotName, wasEnabled);
            }
        }
        throw new IllegalStateException("no robot that can be disabled found in " + robotEnableMap.keySet() + " and in " + robotsThatCanBeDisabled);
    }

    private final AtomicInteger randomTestCount = new AtomicInteger();

    public void clearRandomTestCount() {
        randomTestCount.set(0);
    }

    private XFuture<Void> updateRandomTestCount() {
        int count = randomTestCount.incrementAndGet();
        XFuture<Void> xf
                = Utils.runOnDispatchThread("updateRandomTest.runOnDispatchThread" + count,
                        () -> {
                            System.out.println("updateRandomTestCount count = " + count);
                        });
        if (null != displayJFrame) {
            final AprsSupervisorDisplayJFrame jfrm = displayJFrame;
            return xf.thenCompose(x -> jfrm.updateRandomTestCount(count));
        }
        return xf;
    }

    private void logEventErr(String err) {
        System.err.println(err);
        logEvent("ERROR: " + err);
    }

    private boolean allSystemsOk() {
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystemInterface sys = aprsSystems.get(i);
            CRCLStatusType status = sys.getCurrentStatus();
            if (status != null
                    && status.getCommandStatus() != null
                    && status.getCommandStatus().getCommandState() == CommandStateEnumType.CRCL_ERROR) {
                logEventErr("allSystemsOk failing: bad status for sys=" + sys);
                return false;
            }
            String titleErrorString = sys.getTitleErrorString();
            if (titleErrorString != null && titleErrorString.length() > 0) {
                logEventErr("allSystemsOk failing: bad titleErrorString (" + titleErrorString + ") for sys=" + sys);
                return false;
            }
            String clientErrorString = sys.getCrclClientErrorString();
            if (clientErrorString != null && clientErrorString.length() > 0) {
                logEventErr("allSystemsOk failing: bad rclClientErrorString (" + clientErrorString + ") for sys=" + sys);
                return false;
            }
        }
        return true;
    }

    @Nullable
    private volatile XFuture<Void> pauseTestFuture = null;

    @Nullable public XFuture<Void> getPauseTestFuture() {
        return pauseTestFuture;
    }

    public void setPauseTestFuture(@Nullable XFuture<Void> pauseTestFuture) {
        this.pauseTestFuture = pauseTestFuture;
    }

    public XFuture<Void> continuePauseTest() {
        if (!allSystemsOk()) {
            logEventErr("allSystemsOk returned false forcing quitRandomTest");
            return quitRandomTest("allSystemsOk returned false forcing quitRandomTest");
        }
        if (!isContinousDemoSelected() && !isContinousDemoRevFirstSelected()) {
            logEventErr("isContinousDemoSelected() returned false forcing quitRandomTest");
            return quitRandomTest("isContinousDemoSelected() returned false forcing quitRandomTest");
        }
        if (!isRandomTestSelected()) {
            logEventErr("isRandomTestSelected().isSelected() returned false forcing quitRandomTest");
            return quitRandomTest("isRandomTestSelected().isSelected() returned false forcing quitRandomTest");
        }
        if (null == continousDemoFuture
                || continousDemoFuture.isCancelled()
                || continousDemoFuture.isDone()
                || continousDemoFuture.isCompletedExceptionally()) {
            System.out.println("continousDemoCycle = " + continousDemoCycle + " forcing quitRandomTest");
            printStatus(continousDemoFuture, System.out);
            return quitRandomTest("continousDemoCycle = " + continousDemoCycle + " forcing quitRandomTest");
        }
        XFuture<Void> ret = startRandomDelay("pauseTest", 30000, 20000)
                .thenCompose("pauseTest.pause" + pauseCount.get(),
                        x -> Utils.runOnDispatchThread(this::pause))
                .thenCompose(x -> startRandomDelay("pauseTest", 1000, 1000))
                .thenCompose("pauseTest.resume" + pauseCount.get(),
                        x -> Utils.runOnDispatchThread(this::resume));
        pauseTestFuture = ret;
        resetMainPauseTestFuture();
        ret
                .thenCompose("pauseTest.recurse" + pauseCount.get(),
                        x -> continuePauseTest());
        pauseTestFuture = ret;
        return ret;
    }

    public XFuture<Void> continueRandomTest() {
        if (!checkMaxCycles()) {
            return XFuture.completedFutureWithName("continueRandomTest.!checkMaxCycles()", null);
        }
        if (!allSystemsOk()) {
            logEventErr("allSystemsOk returned false forcing quitRandomTest");
            return quitRandomTest("allSystemsOk returned false forcing quitRandomTest");
        }
        if (!isContinousDemoSelected()
                && !isContinousDemoRevFirstSelected()
                && !isIndContinousDemoSelected()
                && !isIndRandomToggleTestSelected()) {
            logEventErr("isContinousDemoSelected() returned false forcing quitRandomTest");
            return quitRandomTest("isContinousDemoSelected() returned false forcing quitRandomTest");
        }
        if (!isRandomTestSelected()
                && !isIndRandomToggleTestSelected()) {
            logEventErr("isRandomTestSelected().isSelected() returned false forcing quitRandomTest");
            return quitRandomTest("isRandomTestSelected().isSelected() returned false forcing quitRandomTest");
        }
        XFuture<?> currentContinousDemoFuture = this.continousDemoFuture;
        if (null == currentContinousDemoFuture
                || currentContinousDemoFuture.isCancelled()
                || currentContinousDemoFuture.isDone()
                || currentContinousDemoFuture.isCompletedExceptionally()) {
            System.out.println("continousDemoCycle = " + continousDemoCycle + " forcing quitRandomTest");
            printStatus(currentContinousDemoFuture, System.out);
            return quitRandomTest("continousDemoCycle = " + continousDemoCycle + " forcing quitRandomTest");
        }
        XFuture<Void> ret = startRandomDelay("enableTest", 30000, 20000)
                .thenCompose("checkForWaitResume1", x -> this.waitResume())
                .thenCompose("waitTogglesAllowed", x -> this.waitTogglesAllowed())
                .thenCompose("toggleRobotEnabled", x -> this.toggleRobotEnabled())
                .thenCompose("updateRandomTestCount" + randomTestCount.get(), x -> {
                    if (x) {
                        return this.updateRandomTestCount();
                    } else {
                        return XFuture.completedFuture(null);
                    }
                })
                .thenComposeAsync("continueRandomTest.recurse" + randomTestCount.get(), x -> continueRandomTest(), randomDelayExecutorService);
        randomTestFuture = ret;
        resetMainRandomTestFuture();
        return ret;
    }

    private XFuture<Void> quitRandomTest(String cause) {
        logEvent("quitRandomTest : " + cause);
        XFuture<Void> xf = new XFuture<>("quitRandomTest : " + cause);
        xf.cancel(false);
        System.out.println("continueRandomTest quit");
        setContinousDemoSelected(false);
        setContinousDemoRevFirstSelected(false);
        setRandomTestSelected(false);
        immediateAbortAll("quitRandomTest : " + cause);
        return xf;
    }

    /**
     * Set the reverseFlag for all systems. When the reverseFlag is set systems
     * empty kit trays and put parts back in parts trays. This may occur in
     * another thread.
     *
     * @param reverseFlag false to move parts from parts trays to kitTrays or
     * true to move parts from kitTrays to partsTrays
     *
     * @return a future which can be used to determine when the all reverse
     * flags and related actions are complete.
     */
    public XFuture<Void> startSetAllReverseFlag(boolean reverseFlag) {
        logEvent("setAllReverseFlag(" + reverseFlag + ")");
        @SuppressWarnings("rawtypes")
        XFuture fa[] = new XFuture[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystemInterface sys = aprsSystems.get(i);
            if (sys.isReverseFlag() != reverseFlag) {
                logEvent("setting reverseFlag for " + sys + " to " + reverseFlag);
                fa[i] = sys.startSetReverseFlag(reverseFlag);
            } else {
                fa[i] = XFuture.completedFuture(null);
            }
        }
        return XFuture.allOf(fa);
    }

    public void disconnectAllNoLog() {
        for (int i = 0; i < aprsSystems.size(); i++) {
            aprsSystems.get(i).setConnected(false);
        }
    }

    /**
     * Disconnect all systems.
     */
    public void disconnectAll() {
        logEvent("disconnectAll");
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystemInterface sys = aprsSystems.get(i);
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
    public XFuture<Void> startPrivateContinuousDemoRevFirst() {
        int c = scdrfCount.incrementAndGet();
        logEvent("Start Continous Demo (Reverse First) : " + c);
        connectAll();
        final XFuture<?> lfr = this.lastFutureReturned;
        continousDemoFuture
                = startCheckAndEnableAllRobots()
                        .thenComposeAsync("startContinousDemoRevFirst.checkLastReturnedFuture1", x -> checkLastReturnedFuture(lfr), supervisorExecutorService)
                        .thenComposeAsync("startContinousDemoRevFirst.startReverseActions", x -> startReverseActions(), supervisorExecutorService)
                        .thenComposeAsync("startContinousDemoRevFirst.checkLastReturnedFuture2", x -> checkLastReturnedFuture(lfr), supervisorExecutorService)
                        .thenComposeAsync("startContinousDemoRevFirst.enableAndCheckAllRobots", x -> startCheckAndEnableAllRobots(), supervisorExecutorService)
                        .thenComposeAsync("startContinousDemoRevFirst", ok -> checkOkElse(ok, this::continueContinousDemo, this::showCheckEnabledErrorSplash), supervisorExecutorService);
        return continousDemoFuture;
    }

    /**
     * Start a continuous demo where kit trays will first be filled and then
     * emptied repeatedly. Systems will wait for all systems to be filled before
     * any begin emptying and vice versa.
     *
     * @return future that can be used to determine if it fails or is cancelled
     */
    public XFuture<Void> startContinousScanAndRun() {
        logEvent("Start continous scan and run");
        connectAll();
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystemInterface aprsSys = aprsSystems.get(i);
            aprsSys.setLastCreateActionListFromVisionKitToCheckStrings(Collections.emptyList());
        }
        continousDemoFuture
                = startCheckAndEnableAllRobots()
                        .thenCompose("startContinousScanAndRun", ok -> checkOkElse(ok, this::continueContinousScanAndRun, this::showCheckEnabledErrorSplash));
        return continousDemoFuture;
    }

    /**
     * Start a continuous demo where kit trays will first be filled and then
     * emptied repeatedly. Systems will wait for all systems to be filled before
     * any begin emptying and vice versa.
     *
     * @return future that can be used to determine if it fails or is cancelled
     */
    public XFuture<Void> startContinousDemo() {
        logEvent("Start continous demo");
        connectAll();
        continousDemoFuture
                = startCheckAndEnableAllRobots()
                        .thenCompose("startContinousDemo", ok -> checkOkElse(ok, this::continueContinousDemo, this::showCheckEnabledErrorSplash));
        return continousDemoFuture;
    }

    /**
     * Start a continuous demo where kit trays will first be filled and then
     * emptied repeatedly. Systems will not wait for all systems to be filled
     * before any begin emptying and vice versa, so one might be emptying while
     * another is filling.
     *
     * @return future that can be used to determine if it fails or is canceled
     */
    public XFuture<Void> startIndependentContinousDemo() {
        logEvent("Start continous demo");
        connectAll();
        XFuture<Void> ret
                = startCheckAndEnableAllRobots()
                        .thenCompose("startIndContinousDemo", ok -> checkOkElse(ok, this::startAllIndContinousDemo, this::showCheckEnabledErrorSplash));
        continousDemoFuture = ret;
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
    public XFuture<Void> startRandomEnableToggleIndependentContinousDemo() {
        logEvent("Start Independent Random  Enable Toggle Test");
        connectAll();
        return startCheckAndEnableAllRobots()
                .thenCompose("startRandomEnableToggleIndependentContinousDemo.checkOk",
                        ok -> checkOkElse(ok, this::startIndRandomTestStep2, this::showCheckEnabledErrorSplash));
    }

    private final AtomicInteger continousDemoCycle = new AtomicInteger(0);

    public int getContiousDemoCycleCount() {
        return continousDemoCycle.get();
    }

    public ExecutorService getSupervisorExecutorService() {
        return supervisorExecutorService;
    }

    public ExecutorService getRandomDelayExecutorService() {
        return randomDelayExecutorService;
    }

    private XFuture<Void> incrementContinousDemoCycle() {
        final int c = continousDemoCycle.incrementAndGet();
        System.out.println("incrementContinousDemoCycle : " + c);
        if (null != displayJFrame) {
            return displayJFrame.incrementContinousDemoCycle();
        } else {
            return XFutureVoid.completedFutureWithName("incrementContinousDemoCycle" + c);
        }
    }

    final static private AtomicInteger runProgramThreadCount = new AtomicInteger();

    private final int myThreadId = runProgramThreadCount.incrementAndGet();

    private final ExecutorService defaultSupervisorExecutorService
            = Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "AprsSupervisor" + myThreadId);
                    thread.setDaemon(true);
                    return thread;
                }
            });

    private ExecutorService supervisorExecutorService = defaultSupervisorExecutorService;

    private final ExecutorService defaultRandomDelayExecutorService
            = Executors.newCachedThreadPool(new ThreadFactory() {

                private int t;

                @Override
                public Thread newThread(Runnable r) {
                    t++;
                    Thread thread = new Thread(r, "AprsSupervisor_random_delay_" + myThreadId + "_" + t);
                    thread.setDaemon(true);
                    return thread;
                }
            });

    private ExecutorService randomDelayExecutorService = defaultRandomDelayExecutorService;

    public int getMax_cycles() {
        return max_cycles;
    }

    public void setMax_cycles(int max_cycles) {
        this.max_cycles = max_cycles;
    }

    private XFuture<Void> continueContinousScanAndRun() {
        logEvent("Continue Continous Scan and Run : " + continousDemoCycle.get());
        return continousDemoSetup()
                .thenCompose("continueContinousScanAndRun.part2", x2 -> {
                    final XFuture<?> lfr = this.lastFutureReturned;
                    XFuture<Void> ret
                            = startCheckAndEnableAllRobots()
                                    .thenComposeAsync("continueContinousScanAndRun.scanAllInternal", x -> scanTillNewInternal(), supervisorExecutorService)
                                    .thenComposeAsync("continueContinousScanAndRun.checkLastReturnedFuture2", x -> checkLastReturnedFuture(lfr), supervisorExecutorService)
                                    .thenComposeAsync("continueContinousScanAndRun.startAllActions1", x -> startAllActions(), supervisorExecutorService)
                                    .thenComposeAsync("continueContinousScanAndRun.checkLastReturnedFuture1", x -> checkLastReturnedFuture(lfr), supervisorExecutorService)
                                    .thenCompose("continueContinousScanAndRun.incrementContinousDemoCycle", x -> incrementContinousDemoCycle())
                                    .thenComposeAsync("continueContinousScanAndRun.enableAndCheckAllRobots", x -> startCheckAndEnableAllRobots(), supervisorExecutorService)
                                    .thenComposeAsync("continueContinousScanAndRun.recurse" + continousDemoCycle.get(), ok -> checkOkElse(ok && checkMaxCycles(), this::continueContinousScanAndRun, this::showCheckEnabledErrorSplash), supervisorExecutorService);
                    continousDemoFuture = ret;
                    if (null != randomTestFuture) {
                        if (isRandomTestSelected()) {
                            resetMainRandomTestFuture();
                        } else if (isRandomTestSelected()) {
                            resetMainPauseTestFuture();
                        }
                    }
                    return ret;
                });
    }

    public XFuture<Void> startContinousDemoReversActions() {
        if (!isContinousDemoSelected() && !isContinousDemoRevFirstSelected()) {
            String msg = "startContinousDemoReversActions : " + continousDemoCycle.get() + " quiting because checkbox not set";
            logEvent(msg);
            XFuture<Void> ret = XFutureVoid.completedFutureWithName(msg);
            return ret;
        }
        return startReverseActions();
    }

    private XFuture<Void> continueContinousDemo() {
        logEvent("Continue Continous Demo : " + continousDemoCycle.get());
        if (!isContinousDemoSelected() && !isContinousDemoRevFirstSelected()) {
            String msg = "Continue Continous Demo : " + continousDemoCycle.get() + " quiting because checkbox not set";
            logEvent(msg);
            XFuture<Void> ret = XFutureVoid.completedFutureWithName(msg);
            return ret;
        }
        String blockerName = "start continueContinousDemo" + continousDemoCycle.get();
        AprsSystemInterface sysArray[] = getAprsSystems().toArray(new AprsSystemInterface[getAprsSystems().size()]);
        disallowToggles(blockerName, sysArray);
        return continousDemoSetup()
                .thenCompose("continouseDemo.part2", x2 -> {
                    final XFuture<?> lfr = this.lastFutureReturned;
                    XFuture<Void> ret
                            = startCheckAndEnableAllRobots()
                                    .thenComposeAsync("continueContinousDemo.startAllActions1", x -> {
                                        XFuture<Void> startAllActionsRet = startAllActions();
                                        allowToggles(blockerName, sysArray);
                                        return startAllActionsRet;
                                    }, supervisorExecutorService)
                                    .thenComposeAsync("continueContinousDemo.checkLastReturnedFuture1", x -> checkLastReturnedFuture(lfr), supervisorExecutorService)
                                    .thenComposeAsync("continueContinousDemo.startReverseActions", x -> startContinousDemoReversActions(), supervisorExecutorService)
                                    .thenComposeAsync("continueContinousDemo.checkLastReturnedFuture2", x -> checkLastReturnedFuture(lfr), supervisorExecutorService)
                                    .thenCompose("continueContinousDemo.incrementContinousDemoCycle", x -> incrementContinousDemoCycle())
                                    .thenComposeAsync("continueContinousDemo.enableAndCheckAllRobots", x -> startCheckAndEnableAllRobots(), supervisorExecutorService)
                                    .thenComposeAsync("continueContinousDemo.recurse" + continousDemoCycle.get(), ok -> checkOkElse(ok && checkMaxCycles(), this::continueContinousDemo, this::showCheckEnabledErrorSplash), supervisorExecutorService);
                    continousDemoFuture = ret;
                    if (null != randomTestFuture) {
                        if (isRandomTestSelected()) {
                            resetMainRandomTestFuture();
                        } else if (isRandomTestSelected()) {
                            resetMainPauseTestFuture();
                        }
                    }
                    return ret;
                });
    }

    public boolean checkMaxCycles() {
        if (max_cycles < 1) {
            return true;
        }
        boolean ret = max_cycles > continousDemoCycle.get();
        if (!ret) {
            System.err.println("max_cycles limit hit");
        }
        return ret;
    }

    private XFuture<@Nullable Void> continousDemoSetup() {
        return XFuture
                .runAsync("contiousDemoSetup", () -> {
                    System.out.println("stealingRobots = " + stealingRobots);
                    System.out.println("returnRobotRunnable = " + returnRobotRunnable);
                }, supervisorExecutorService)
                .thenComposeAsync("contiousDemoSetup.part2", x -> {
                    if (this.stealingRobots || null != returnRobotRunnable.get()) {
                        disconnectAll();
                        return returnRobotsDirect("contiousDemoSetup.part2");
                    } else {
                        return XFuture.completedFuture(null);
                    }
                }, supervisorExecutorService)
                .thenRunAsync(() -> {
//                    disconnectAll();
                    checkRobotsUniquePorts();
                    System.out.println("stealingRobots = " + stealingRobots);
                    System.out.println("returnRobotRunnable = " + returnRobotRunnable);
                    cancelAllStealUnsteal(false);
                    connectAll();
                    setAllReverseFlag(false);
                }, supervisorExecutorService);
    }

    private volatile boolean debugStartReverseActions = false;

    /**
     * Start actions in reverse mode where kit trays will be emptied rather than
     * filled.
     *
     * @return future that can be used to attach additional actions after this
     * is complete
     */
    public XFuture<Void> startReverseActions() {
        logEvent("startReverseActions");
        String blockerName = "start startReverseActions" + continousDemoCycle.get();
        AprsSystemInterface sysArray[] = getAprsSystems().toArray(new AprsSystemInterface[getAprsSystems().size()]);
        disallowToggles(blockerName, sysArray);
        setAllReverseFlag(true);
        if (debugStartReverseActions) {
            debugAction();
        }
        return startCheckAndEnableAllRobots()
                .thenCompose("startReverseActions.startAllActions", ok -> {
                    XFuture<Void> checkOkRet = checkOkElse(ok, this::startAllActions, this::showCheckEnabledErrorSplash);
                    allowToggles(blockerName, sysArray);
                    return checkOkRet;
                });
    }

    public void savePosFile(File f) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            for (int i = 0; i < jTableSelectedPosMapFile.getColumnCount(); i++) {
                pw.print(jTableSelectedPosMapFile.getColumnName(i));
                pw.print(",");
            }
            pw.println();
            for (int i = 0; i < jTableSelectedPosMapFile.getRowCount(); i++) {
                for (int j = 0; j < jTableSelectedPosMapFile.getColumnCount(); j++) {
                    pw.print(jTableSelectedPosMapFile.getValueAt(i, j));
                    pw.print(",");
                }
                pw.println();
            }
        }
    }

    private void clearPosTable() {
        DefaultTableModel tm = (DefaultTableModel) jTablePositionMappings.getModel();
        tm.setRowCount(0);
        tm.setColumnCount(0);
        tm.addColumn("System");
        for (String name : robotEnableMap.keySet()) {
            tm.addColumn(name);
        }
        for (String name : robotEnableMap.keySet()) {
            Object data[] = new Object[robotEnableMap.size()];
            data[0] = name;
            tm.addRow(data);
        }
        Utils.autoResizeTableColWidths(jTablePositionMappings);
        Utils.autoResizeTableRowHeights(jTablePositionMappings);
        if (null != posTablePopupMenu) {
            posTablePopupMenu.setVisible(false);
        }
    }

    @Nullable
    private JPopupMenu posTablePopupMenu = null;

    private void showPosTablePopup(Point pt) {
        JPopupMenu menu = posTablePopupMenu;
        if (menu == null) {
            menu = new JPopupMenu();
            JMenuItem mi = new JMenuItem("Clear");
            mi.addActionListener(l -> clearPosTable());
            menu.add(mi);
            posTablePopupMenu = menu;
        }
        menu.setLocation(pt.x, pt.y);
        menu.setVisible(true);
    }

    /**
     * Enable all robots. (Note: no check is made if the robot is physically in
     * estop and no change to its estop state is made, only the checkboxes in
     * the robots table are potentially changed.)
     */
    public void enableAllRobots() {
        cancelAllStealUnsteal(false);
        if (null != displayJFrame) {
            try {
                displayJFrame.initColorTextSocket();
            } catch (IOException ex) {
                log(Level.SEVERE, null, ex);
            }
            displayJFrame.updateRobotsTableFromMapsAndEnableAll(robotDisableCountMap, robotDisableTotalTimeMap);
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
    public XFuture<Boolean> startCheckAndEnableAllRobots() {

        String blockerName = "startCheckAndEnableAllRobots" + enableAndCheckAllRobotsCount.incrementAndGet();
        AprsSystemInterface sysArray[] = getAprsSystems().toArray(new AprsSystemInterface[getAprsSystems().size()]);
        disallowToggles(blockerName, sysArray);
        Utils.SwingFuture<Void> step1Future = Utils.runOnDispatchThread(() -> {
            updateRobotsTableFromMapsAndEnableAll();
        });
        boolean KeepAndDisplayXFutureProfilesSelected = isKeepAndDisplayXFutureProfilesSelected();
        step1Future.setKeepOldProfileStrings(KeepAndDisplayXFutureProfilesSelected);
        XFuture<Void> step2Future = step1Future
                .thenRunAsync("sendEnableToColorTextSocket", () -> {
                    try {
                        initColorTextSocket();
                        writeToColorTextSocket("0x00FF00, 0x00FF000\r\n".getBytes());
                    } catch (IOException ex) {
                        log(Level.SEVERE, null, ex);
                    }
                }, supervisorExecutorService);
        XFuture<Boolean> step3Future
                = step2Future
                        .thenCompose(x -> {
                            cancelAllStealUnsteal(false);
                            XFuture<@Nullable Void> rrF = returnRobots("enableAndCheckAllRobots");
                            rrF.setKeepOldProfileStrings(KeepAndDisplayXFutureProfilesSelected);
                            return rrF;
                        })
                        .thenComposeAsync(x2 -> checkEnabledAll(), supervisorExecutorService);
        return step3Future
                .alwaysAsync(() -> {
                    allowToggles(blockerName, sysArray);
                }, supervisorExecutorService)
                .always(this::logStartCheckAndEnableAllRobotsComplete);
    }

    private void updateRobotsTableFromMapsAndEnableAll() {
        if (null != displayJFrame) {
            displayJFrame.updateRobotsTableFromMapsAndEnableAll(robotDisableCountMap, robotDisableTotalTimeMap);
        }
    }

    private void logStartCheckAndEnableAllRobotsComplete() {
        logEvent("startCheckAndEnableAllRobots complete");
    }

    private XFuture<Boolean> checkEnabledAll() {
        logEvent("checkEnabledAll");
        boolean origIgnoreTitleErrs = ignoreTitleErrors.getAndSet(true);
        @SuppressWarnings("unchecked")
        XFuture<Boolean> futures[] = (XFuture<Boolean>[]) new XFuture<?>[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystemInterface sys = aprsSystems.get(i);
            futures[i] = sys.startCheckEnabled()
                    .thenApplyAsync(x -> {
                        logEvent(sys.getRobotName() + " checkEnabled returned " + x);
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
                XFuture<Boolean> newret = new XFuture<>("checkEnabledAll.alreadyFailed." + aprsSystems.get(i).getTaskName());
                newret.completeExceptionally(new IllegalStateException("isCompletedExceptionally() for " + aprsSystems.get(i).getTaskName()));
                return newret;
            }
            ret = ret
                    .thenCombine("checkEnabledAll(" + (i + 1) + "/" + futures.length + ")",
                            fi, andBiFunction);
        }
        return ret.always(() -> {
            if (!origIgnoreTitleErrs) {
                ignoreTitleErrors.set(false);
            }
        });
    }

    AtomicInteger pauseCount = new AtomicInteger();

    public void setPauseCount(int count) {
        if (null != displayJFrame) {
            displayJFrame.setPauseCount(count);
        }
    }

    public void pause() {
        logEvent("pause");
        if (null != runTimeTimer) {
            runTimeTimer.stop();
            runTimeTimer = null;
        }
        completeResumeFuture();
        int count = pauseCount.incrementAndGet();
        setPauseCount(count);
        if (!isPauseSelected()) {
            setPauseSelected(true);
        }
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystemInterface aprsSys = aprsSystems.get(i);
            if (aprsSys.isConnected() && !aprsSys.isPaused()) {
                aprsSys.pause();
            }
        }
        resumeFuture.set(new XFuture<>("resume"));
    }

    private void completeResumeFuture() {
        XFuture<Void> rf = resumeFuture.getAndSet(null);
        if (null != rf) {
            rf.complete(null);
        }
    }

    private XFuture<Void> waitResume() {
        XFuture<Void> rf = resumeFuture.get();
        if (null != rf) {
            return rf;
        } else {
            return XFuture.completedFutureWithName("waitResume.rf==null", null);
        }
    }

    private volatile boolean resuming = false;

    public void resume() {
        logEvent("resume");
        resuming = true;
        try {
            resumeForPrepOnly();
            startUpdateRunningTimeTimer();
        } finally {
            resuming = false;
        }
    }

    private void resumeForPrepOnly() {
        if (isPauseSelected()) {
            setPauseSelected(false);
        }
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystemInterface aprsSys = aprsSystems.get(i);
            if (aprsSys.isPaused()) {
                aprsSys.resume();
            }
        }
        completeResumeFuture();
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystemInterface aprsSys = aprsSystems.get(i);
            if (aprsSys.isPaused()) {
                throw new IllegalStateException(aprsSys + " is still paused after resume");
            }
        }
    }

    private volatile XFuture<?> lastStartAllActionsArray @Nullable []  = null;

    private final ConcurrentHashMap<Integer, XFuture<Boolean>> systemContinueMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, XFuture<Void>> debugSystemContinueMap = new ConcurrentHashMap<>();

    public XFuture<Void> continueSingleContinousDemo(AprsSystemInterface sys, int recurseNum) {
        XFuture<Void> ret = debugSystemContinueMap.compute(sys.getMyThreadId(),
                (k, v) -> {
                    return continueSingleContinousDemoInner(sys, recurseNum);
                });
        StringBuilder tasksNames = new StringBuilder();
        Set<Integer> keySet = debugSystemContinueMap.keySet();
        @SuppressWarnings("unchecked")
        XFuture<Void> futures[] = (XFuture<Void>[]) new XFuture<?>[keySet.size()];
        int i = 0;
        for (int id : keySet) {
            assert (i < futures.length) : "futures=" + futures + ",keySet=" + keySet + ",i=" + i;
            XFuture<Void> debugFuture = debugSystemContinueMap.get(id);
            if (null != debugFuture) {
                futures[i++] = debugFuture;
            }
        }
        assert (i == futures.length) : "futures=" + futures + ",keySet=" + keySet + ",i=" + i;
        for (int j = 0; j < aprsSystems.size(); j++) {
            AprsSystemInterface sysTemp = aprsSystems.get(j);
            if (debugSystemContinueMap.containsKey(sysTemp.getMyThreadId())) {
                tasksNames.append(sysTemp.getTaskName()).append(',');
            }
        }
        continousDemoFuture = XFuture.allOfWithName("continueSingleContinousDemo.allOf(" + tasksNames.toString() + ").recurseNum=" + recurseNum, futures);
        if (null != randomTestFuture) {
            resetMainRandomTestFuture();
        }
        return ret;
    }

    public XFuture<Void> continueSingleContinousDemoInner(AprsSystemInterface sys, int recurseNum) {
        String toggleLockName = "continueSingleContinousDemoInner" + recurseNum + "_" + sys.getMyThreadId();
        return systemContinueMap.computeIfAbsent(sys.getMyThreadId(), k -> {
            return new XFuture<>("continueSingleContinousDemo.holder " + sys);
        })
                .thenCompose("continueSingleContinousDemo.continuing: " + recurseNum + " " + sys,
                        x -> {
                            logEvent("startCheckEnabled(recurseNum=" + recurseNum + ") for " + sys);
                            disallowToggles(toggleLockName, sys);
                            return sys.startCheckEnabled().thenApply(y -> x);
                        })
                .thenComposeAsync("continueSingleContinousDemo.continuing: " + recurseNum + " " + sys,
                        x -> {
                            XFuture<Boolean> ret = sys.startPreCheckedContinousDemo("continueSingleContinousDemoInner" + sys, x);
                            logEvent("startPreCheckedContinousDemo(recurseNum=" + recurseNum + ",reverseFirst=" + x + ") for " + sys);
                            allowToggles(toggleLockName, sys);
                            return ret;
                        }, supervisorExecutorService)
                .thenCompose("continueSingleContinousDemo.recurse " + recurseNum + " " + sys,
                        x -> continueSingleContinousDemo(sys, (recurseNum + 1)));
    }

    private XFuture<Void> startAllIndContinousDemo() {
        logEvent("startAllIndContinousDemo");
        @SuppressWarnings("unchecked")
        XFuture<Void> futures[] = (XFuture<Void>[]) new XFuture<?>[aprsSystems.size()];
        StringBuilder tasksNames = new StringBuilder();
        boolean revFirst = isContinousDemoRevFirstSelected();
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystemInterface sys = aprsSystems.get(i);
            logEvent("startContinousDemo(reverseFirst=false) for " + sys);
            futures[i] = sys.startContinousDemo("startAllIndContinousDemo", revFirst)
                    .thenCompose(x -> continueSingleContinousDemo(sys, 1));
            tasksNames.append(aprsSystems.get(i).getTaskName());
            tasksNames.append(",");
        }
        lastStartAllActionsArray = futures;
//        allowToggles();
        return XFuture.allOfWithName("startAllIndContinousDemo.allOf(" + tasksNames.toString() + ")", futures);
    }

    private final AtomicInteger startAllActionsCount = new AtomicInteger();

    public XFuture<Void> startAllActions() {
        int saaNumber = startAllActionsCount.incrementAndGet();
        logEvent("startAllActions" + saaNumber);
        XFuture<?> futures[] = new XFuture<?>[aprsSystems.size()];
        StringBuilder tasksNames = new StringBuilder();
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystemInterface sys = aprsSystems.get(i);
            int sysThreadId = sys.getMyThreadId();
            logEvent("startActions for " + sys);
            futures[i] = sys.startActions("startAllActions" + saaNumber)
                    .thenComposeAsync(x -> {
                        String runName = sys.getRunName();
                        logEvent("startActions " + sys + ",saaNumber= " + saaNumber + " completed action list run " + runName + " : " + x);
                        return finishAction(sysThreadId)
                                .thenApply(x2 -> {
                                    logEvent("finish startActions " + sys + ",saaNumber= " + saaNumber + " completed action list run " + runName + " : " + x);
                                    return x;
                                });
                    },
                            supervisorExecutorService);
            tasksNames.append(sys.getTaskName());
            tasksNames.append(",");
        }
        lastStartAllActionsArray = futures;
        return XFuture.allOfWithName("startAllActions.allOf(" + tasksNames.toString() + ")" + saaNumber, futures);
    }

    private final ConcurrentHashMap<Integer, XFuture<Void>[]> continueCompletionBlockersMap = new ConcurrentHashMap<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addFinishBlocker(int threadId, XFuture<Void> f) {
        continueCompletionBlockersMap.compute(threadId,
                (Integer k, XFuture<Void> @Nullable [] v) -> {
                    try {
                        if (null != v) {
                            List<XFuture<Void>> l = Arrays.stream(v)
                                    .filter(f2 -> !f2.isDone())
                                    .collect(Collectors.toList());
                            l.add(f);
                            if (l.size() < 1) {
                                return (XFuture<Void>[]) new XFuture<?>[]{f};
                            }
                            XFuture<Void>[] newV = l.toArray(new XFuture[l.size()]);
                            if (newV.length < 1) {
                                return (XFuture<Void>[]) new XFuture<?>[]{f};
                            }
                            return newV;
                        } else {
                            return (XFuture<Void>[]) new XFuture<?>[]{f};
                        }
                    } catch (Throwable e) {
                        log(Level.SEVERE, null, e);
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
    public void log(Level level, @Nullable String msg, Throwable thrown) {
        getLogger().log(level, msg, thrown);
        logEvent("Exception thrown : msg=" + msg + ",thrown=" + thrown + ", trace=" + shortTrace(thrown.getStackTrace()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public XFuture<Void> finishAction(int threadId) {
        XFuture<Void>[] futures = continueCompletionBlockersMap.compute(threadId,
                (Integer k, XFuture<Void> @Nullable [] v) -> {
                    try {
                        if (null != v) {
                            List<XFuture<Void>> l = Arrays.stream(v)
                                    .filter(f2 -> !f2.isDone())
                                    .collect(Collectors.toList());
                            if (l.size() < 1) {
                                return (XFuture<Void>[]) new XFuture<?>[0];
                            }

                            XFuture<Void>[] newV = l.toArray(new XFuture[l.size()]);
                            if (newV.length < 1) {
                                return (XFuture<Void>[]) new XFuture<?>[0];
                            }
                            return newV;
                        } else {
                            return (XFuture<Void>[]) new XFuture<?>[0];
                        }
                    } catch (Throwable e) {
                        log(Level.SEVERE, null, e);
                        throw new RuntimeException(e);
                    }
                });
        if (null == futures || futures.length < 1) {
            return XFuture.completedFuture((Void) null);
        } else {
            logEvent("finishAction waiting for " + Arrays.toString(futures));
            return XFuture.allOf(futures);
        }
    }

    public XFuture<Void> finishAction(AprsSystemInterface sys) {
        return finishAction(sys.getMyThreadId());
    }

    public XFuture<Void> continueAllActions() {
        logEvent("continueAllActions");
        XFuture<?> futures[] = new XFuture<?>[aprsSystems.size()];
        StringBuilder tasksNames = new StringBuilder();
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystemInterface sys = aprsSystems.get(i);
            int sysThreadId = sys.getMyThreadId();
            logEvent("Continue actions for " + sys.getTaskName() + " with " + sys.getRobotName());
            futures[i] = aprsSystems.get(i).continueActionList("continueAllActions")
                    .thenComposeAsync(x -> {
                        logEvent("continueAllActions " + sys.getRunName() + " completed action list " + x);
                        return finishAction(sysThreadId)
                                .thenApply(x2 -> {
                                    logEvent("continueAllActions finish " + sys.getRunName() + " completed action list " + x);
                                    return x;
                                });
                    },
                            supervisorExecutorService);
            tasksNames.append(aprsSystems.get(i).getTaskName());
            tasksNames.append(",");
        }
        lastStartAllActionsArray = futures;
//        allowToggles();
        return XFuture.allOfWithName("continueAllActions.allOf(" + tasksNames.toString() + ")", futures);
    }

    private <T> XFuture<T> checkOkElse(Boolean ok, Supplier<XFuture<T>> okSupplier, Supplier<XFuture<T>> notOkSupplier) {
        if (ok) {
            return okSupplier.get();
        } else {
            return notOkSupplier.get();
        }
    }

    private XFuture<Void> checkOk(Boolean ok, Supplier<XFuture<Void>> okSupplier) {
        if (ok) {
            return okSupplier.get();
        } else {
            return XFuture.completedFutureWithName("checkOk(false)", null);
        }
    }

    public boolean isResuming() {
        return resuming;
    }

    public XFuture<?> continueAll() {
        logEvent("Continoue All : " + continousDemoCycle.get());
        stealingRobots = false;
        XFuture<?> f = startCheckAndEnableAllRobots()
                .thenCompose("continueAll.recurse" + continousDemoCycle.get(),
                        ok -> checkOkElse(ok, this::continueAllActions, this::showCheckEnabledErrorSplash));
        return f;
    }

    /**
     * Have all robots start their already assigned list of actions. These will
     * occur in other threads asynchronously.
     *
     * @return future allowing caller to determine when all systems have
     * completed
     */
    public XFuture<?> startAll() {
        logEvent("Start All ");
        stealingRobots = false;
        return startCheckAndEnableAllRobots()
                .thenCompose("startAll.recurse",
                        ok -> checkOkElse(ok, this::startAllActions, this::showCheckEnabledErrorSplash));
    }

    /**
     * Clear all previously set errors /error states.
     */
    public void clearAllErrors() {
        boolean origIgnoreTitleErrs = ignoreTitleErrors.getAndSet(true);
        for (int i = 0; i < aprsSystems.size(); i++) {
            aprsSystems.get(i).clearErrors();
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
     *
     * @return future allowing a check on when the abort is complete.
     */
    public XFuture<?> immediateAbortAll(String comment) {
        return immediateAbortAll(comment, false);
    }

    public XFuture<?> immediateAbortAll(String comment, boolean skipLog) {
        incrementAndGetAbortCount();
        stealingRobots = false;
        if (null != runTimeTimer) {
            runTimeTimer.stop();
            runTimeTimer = null;
        }
        cancelAll(true);
        if (null != logPrintStream) {
            logPrintStream.close();
            logPrintStream = null;
        }
        for (int i = 0; i < aprsSystems.size(); i++) {
            aprsSystems.get(i).immediateAbort();
        }
        if (this.stealingRobots || null != returnRobotRunnable.get()) {
            disconnectAllNoLog();
            return returnRobots("immediateAbortAll." + comment).thenRun(() -> {
                disconnectAllNoLog();
                if (!skipLog) {
                    logEvent("immediateAbort : " + comment);
                }
                setAbortTimeCurrent();
                if (null != runTimeTimer) {
                    runTimeTimer.stop();
                    runTimeTimer = null;
                }
            });
        } else {
            checkRobotsUniquePorts();
            if (!skipLog) {
                logEvent("immediateAbort: " + comment);
            }
            setAbortTimeCurrent();
            if (null != runTimeTimer) {
                runTimeTimer.stop();
                runTimeTimer = null;
            }
            return XFuture.completedFuture(null);
        }
    }

    public void cancelAll(boolean mayInterrupt) {
        if (null != continousDemoFuture) {
            continousDemoFuture.cancelAll(mayInterrupt);
            continousDemoFuture = null;
        }
        if (null != lastFutureReturned) {
            lastFutureReturned.cancelAll(mayInterrupt);
            lastFutureReturned = null;
        }
        if (null != stealAbortFuture) {
            stealAbortFuture.cancelAll(mayInterrupt);
            stealAbortFuture = null;
        }

        if (null != unstealAbortFuture) {
            unstealAbortFuture.cancelAll(mayInterrupt);
            unstealAbortFuture = null;
        }
        cancelAllStealUnsteal(mayInterrupt);
        if (null != randomTestFuture) {
            randomTestFuture.cancelAll(mayInterrupt);
            randomTestFuture = null;
        }
    }

    public void cancelAllStealUnsteal(boolean mayInterrupt) {
        stealingRobots = false;
        XFuture<Void> stealFuture = this.stealRobotFuture.getAndSet(null);
        if (null != stealFuture) {
            stealFuture.cancelAll(mayInterrupt);
        }

        XFuture<Void> unstealFuture = this.unStealRobotFuture.getAndSet(null);
        if (null != unstealFuture) {
            unstealFuture.cancelAll(mayInterrupt);
        }

        XFuture<Void> cancelStealFuture = this.cancelStealRobotFuture.getAndSet(null);
        if (null != cancelStealFuture) {
            cancelStealFuture.cancelAll(mayInterrupt);
        }

        XFuture<Void> cancelUnstealFuture = this.cancelUnStealRobotFuture.getAndSet(null);
        if (null != cancelUnstealFuture) {
            cancelUnstealFuture.cancelAll(mayInterrupt);
        }
    }

    public void restoreRobotNames() {
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystemInterface aprsSys = aprsSystems.get(i);
            if (aprsSys.isConnected()) {
                continue;
            }
            String robotName = aprsSys.getRobotName();
            if (robotName == null || robotName.length() < 1) {
                String origRobotName = aprsSys.getOrigRobotName();
                if (null != origRobotName && origRobotName.length() > 0) {
                    aprsSys.setRobotName(robotName);
                }
            }
        }
    }

    /**
     * Connect to all robots.
     */
    public void connectAll() {
        try {
            initColorTextSocket();
        } catch (IOException ex) {
            log(Level.SEVERE, null, ex);
        }
        boolean globalPause = isPauseSelected();
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystemInterface aprsSys = aprsSystems.get(i);
            if (!aprsSys.isConnected()) {
                aprsSys.setConnected(true);
            }
            if (aprsSys.isPaused() && !globalPause) {
                aprsSys.resume();
            } else if (!aprsSys.isPaused() && globalPause) {
                aprsSys.pause();
            }
        }
        checkRobotsUniquePorts();
    }

    /**
     * Have all robots abort their actions after any part they are holding has
     * been dropped off and the robot has been moved out of the way of the
     * vision system.
     *
     * @return future allowing caller to determine when the abort is complete
     */
    public XFuture<@Nullable Void> safeAbortAll() {
        incrementAndGetAbortCount();
        logEvent("safeAbortAll");
        XFuture<?> prevLastFuture = lastFutureReturned;
        XFuture<?> futures[] = new XFuture<?>[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystemInterface sys = aprsSystems.get(i);
            futures[i] = sys.startSafeAbort("safeAbortAll")
                    .thenRun(() -> logEvent("safeAbort completed for " + sys + " (part of safeAbortAll)"));
        }
        return XFuture.allOfWithName("safeAbortAll", futures)
                .thenComposeAsync((Function<Void, XFuture<@Nullable Void>>) x -> {
                    logEvent("safeAbortAll: all systems aborted. calling return robots: futures =" + Arrays.toString(futures));
                    return returnRobotsDirect("safeAbortAll");
                }, supervisorExecutorService)
                .thenRun(() -> {
                    if (null != prevLastFuture) {
                        prevLastFuture.cancelAll(false);
                    }
                    logEvent("safeAbortAll completed");
                });
    }

    @Nullable public NamedCallable<XFuture<Void>> getSafeAbortReturnRobot() {
        return safeAbortReturnRobot;
    }

    public void setSafeAbortReturnRobot(NamedCallable<XFuture<Void>> safeAbortReturnRobot) {
        this.safeAbortReturnRobot = safeAbortReturnRobot;
    }

    public int incrementAndGetAbortCount() {
        return abortCount.incrementAndGet();
    }

    public int getAbortCount() {
        return abortCount.get();
    }

    @Nullable
    private File setupFile;

    /**
     * Get the value of setupFile
     *
     * @return the value of setupFile
     */
    @Nullable
    public File getSetupFile() {
        return setupFile;
    }

    private void setTitleMessage(String message) {
        if (null != displayJFrame) {
            displayJFrame.setTitleMessage(message, this.setupFile);
        }
    }

    /**
     * Set the value of setupFile
     *
     * @param f new value of setupFile
     * @throws java.io.IOException can not save last setup file
     */
    public void setSetupFile(File f) throws IOException {
        if (!Objects.equals(this.setupFile, f)) {
            setTitleMessage("");
        }
        if (null != f) {
            saveLastSetupFile(f);
        }
        this.setupFile = f;
        setSaveSetupEnabled(f != null);
    }

    public void setSaveSetupEnabled(boolean enabled) {
        if (null != displayJFrame) {
            displayJFrame.setSaveSetupEnabled(enabled);
        }
    }

    private int showConfirmDialog(String message) {
        if (null != displayJFrame) {
            return JOptionPane.showConfirmDialog(displayJFrame, message);
        }
        return JOptionPane.YES_OPTION;
    }

    /**
     * Save the current setup to the last saved/read setup file.
     */
    public void saveCurrentSetup() {
        try {
            File fileToSave = this.setupFile;
            if (null != fileToSave) {
                int response
                        = showConfirmDialog("Save Current APRS Supervisor file : " + fileToSave);
                if (response == JOptionPane.YES_OPTION) {
                    saveSetupFile(fileToSave);
                }
            }
        } catch (IOException ex) {
            log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Save the current setup to the given file.
     *
     * @param f file to save setup to
     * @throws IOException file can not be written to
     */
    public void saveSetupFile(File f) throws IOException {
        saveJTable(f, jTableTasks, Arrays.asList(0, 1, 2, 6));
        setSetupFile(f);
    }

    private void saveJTable(File f, JTable jtable, Iterable<Integer> columnIndexes) throws IOException {
        String headers[] = tableHeaders(jtable, columnIndexes);
        CSVFormat format = CSVFormat.DEFAULT.withHeader(headers);
        try (CSVPrinter printer = new CSVPrinter(new PrintStream(new FileOutputStream(f)), format)) {
            TableModel tm = jtable.getModel();
            for (int i = 0; i < tm.getRowCount(); i++) {
                List<Object> l = new ArrayList<>();
                for (Integer colIndex : columnIndexes) {
                    if (null == colIndex) {
                        continue;
                    }
                    int j = (int) colIndex;
                    if (j > tm.getColumnCount()) {
                        break;
                    }
                    Object o = tm.getValueAt(i, j);
                    if (o instanceof File) {
                        File parentFile = f.getParentFile();
                        if (null != parentFile) {
                            Path rel = parentFile.toPath().toRealPath().relativize(Paths.get(((File) o).getCanonicalPath())).normalize();
                            if (rel.toString().length() < ((File) o).getCanonicalPath().length()) {
                                l.add(rel);
                            } else {
                                l.add(o);
                            }
                        } else {
                            l.add(o);
                        }
                    } else {
                        l.add(o);
                    }
                }
                printer.printRecord(l);
            }
        }
    }

    private void saveJTable(File f, JTable jtable, CSVFormat csvFormat) throws IOException {
        try (CSVPrinter printer = new CSVPrinter(new PrintStream(new FileOutputStream(f)), csvFormat)) {
            TableModel tm = jtable.getModel();
            for (int i = 0; i < tm.getRowCount(); i++) {
                List<Object> l = new ArrayList<>();
                for (int j = 0; j < tm.getColumnCount(); j++) {
                    if (j == 3) {
                        continue;
                    }
                    Object o = tm.getValueAt(i, j);
                    if (o instanceof File) {
                        File parentFile = f.getParentFile();
                        if (null != parentFile) {
                            Path rel = parentFile.toPath().toRealPath().relativize(Paths.get(((File) o).getCanonicalPath())).normalize();
                            if (rel.toString().length() < ((File) o).getCanonicalPath().length()) {
                                l.add(rel);
                            } else {
                                l.add(o);
                            }
                        } else {
                            l.add(o);
                        }
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
        saveJTable(f, jTablePositionMappings, CSVFormat.RFC4180);
        saveLastPosMapFile(f);
    }

    private Map<String, Map<String, File>> posMaps = new HashMap<>();

    /**
     * Get the file location where data is stored for converting positions from
     * sys1 to sys2. The file is a CSV file.
     *
     * @param sys1 system to convert positions from
     * @param sys2 system to convert positions to
     * @return file for converting positions
     */
    public File getPosMapFile(String sys1, String sys2) throws FileNotFoundException {
        Map<String, File> subMap = posMaps.get(sys1);
        if (null == subMap) {
            throw new IllegalStateException("no subMap for system " + sys1 + " in " + posMaps);
        }
        File f = subMap.get(sys2);
        if (null == f) {
            throw new IllegalStateException("no entry  for system " + sys2 + " in " + subMap);
        }
        if (f.exists()) {
            return f;
        }
        if (null != lastPosMapFile) {
            File parentFile = lastPosMapFile.getParentFile();
            if (null != parentFile) {
                File altFile = parentFile.toPath().resolve(f.toPath()).toFile();
                if (altFile.exists()) {
                    return altFile;
                }
            }
        }
        if (null == f) {
            throw new IllegalStateException("no entry  for system " + sys1 + " to " + sys2);
        }
        if (!f.exists()) {
            throw new FileNotFoundException(f + " does not exist. failing for getPosMapFile " + sys1 + " to " + sys2);
        }
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
    public void setPosMapFile(String sys1, String sys2, File f) {
        Map<String, File> subMap = posMaps.get(sys1);
        if (null == subMap) {
            subMap = new HashMap<>();
            posMaps.put(sys1, subMap);
        }
        subMap.put(sys2, f);
    }

    /**
     * Load posmaps from the given file.
     *
     * @param f file to load
     * @throws IOException file could not be read
     */
    final public void loadPositionMaps(File f) throws IOException {
        System.out.println("Loading position maps  file :" + f.getCanonicalPath());
        DefaultTableModel tm = (DefaultTableModel) jTablePositionMappings.getModel();
        tm.setRowCount(0);
        tm.setColumnCount(0);
        tm.addColumn("System");
        try (CSVParser parser = CSVParser.parse(f, Charset.defaultCharset(), CSVFormat.RFC4180)) {
            String line = null;
            int linecount = 0;
            for (CSVRecord csvRecord : parser) {
                linecount++;
                Object a[] = new Object[csvRecord.size()];
                for (int i = 0; i < a.length; i++) {
                    a[i] = csvRecord.get(i);
                }
                tm.addColumn(a[0]);
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
                            setPosMapFile((String) a[0], tm.getColumnName(i), fi);
                        }
                    }
                }
                tm.addRow(a);
            }
        }
        saveLastPosMapFile(f);
    }

    private static File resolveFile(String fname, @Nullable File dir) throws IOException {
        File fi = new File(fname);
        if (!fi.exists() && dir != null && dir.exists() && dir.isDirectory()) {
            File altFile = dir.toPath().toRealPath().resolve(fname).toFile();
            if (altFile.exists()) {
                fi = altFile;
            }
        }
        return fi;
    }

    private static String relativeFile(File dir, File f) throws IOException {
        return dir.toPath().relativize(f.toPath()).toString();
    }

    @Nullable
    private File lastSetupFile = null;

    private void saveLastSetupFile(File f) throws IOException {
        lastSetupFile = f;
        try (PrintWriter pw = new PrintWriter(new FileWriter(lastSetupFileFile))) {
            pw.println(f.getCanonicalPath());
        }

    }

    private ProcessLauncherJFrame processLauncher;

    /**
     * Get the value of processLauncher
     *
     * @return the value of processLauncher
     */
    public ProcessLauncherJFrame getProcessLauncher() {
        return processLauncher;
    }

    /**
     * Set the value of processLauncher
     *
     * @param processLauncher new value of processLauncher
     */
    public void setProcessLauncher(ProcessLauncherJFrame processLauncher) {
        this.processLauncher = processLauncher;
        processLauncher.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        processLauncher.addOnCloseRunnable(this::close);
    }

    @Nullable
    private File lastSimTeachFile = null;

    public void saveSimTeach(File f) throws IOException {
        object2DOuterJPanel1.saveFile(f);
        saveLastSimTeachFile(f);
    }

    public void saveLastSimTeachFile(File f) throws IOException {
        lastSimTeachFile = f;
        try (PrintWriter pw = new PrintWriter(new FileWriter(lastSimTeachFileFile))) {
            pw.println(f.getCanonicalPath());
        }
    }

    @Nullable
    private File lastPosMapFile = null;

    public void saveLastPosMapFile(File f) throws IOException {
        lastPosMapFile = f;
        try (PrintWriter pw = new PrintWriter(new FileWriter(lastPosMapFileFile))) {
            pw.println(f.getCanonicalPath());
        }
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

    public void performStartAllAction() {
        prepAndFinishOnDispatch(() -> {
            immediateAbortAll("jMenuItemStartAllActionPerformed");
            clearEventLog();
            connectAll();
            setAllReverseFlag(false);
            enableAllRobots();
            lastFutureReturned = startAll();
            setMainFuture(lastFutureReturned);
            XFuture<?> xf = lastFutureReturned;
            xf.thenRunAsync("showStartAllProfiles", () -> {
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
            }, getSupervisorExecutorService()).thenCompose(x -> showAllTasksCompleteDisplay());
        });
    }

    public void performRemoveSelectedSystemAction(int selectedIndex) {
        if (selectedIndex >= 0 && selectedIndex < aprsSystems.size()) {
            try {
                AprsSystemInterface aj = aprsSystems.remove(selectedIndex);
                try {
                    aj.close();
                } catch (Exception ex) {
                    log(Level.SEVERE, null, ex);
                }
                updateTasksTable();
                updateRobotsTable();
                saveCurrentSetup();
            } catch (Exception ex) {
                log(Level.SEVERE, null, ex);
            }
        }
    }

    public void performSafeAbortAllAction() {
        incrementAndGetAbortCount();
        safeAbortReturnRobot = getReturnRobotNamedCallable();
        logEvent("User requested safeAbortAll : safeAbortReturnRobot=" + safeAbortReturnRobot);
        XFuture<Void> rf = randomTestFuture;
        if (null != rf) {
            rf.cancelAll(false);
        }
        XFuture<Void> f
                = waitTogglesAllowed()
                        .thenCompose(x -> safeAbortAll());
        setLastSafeAbortAllFuture(f);
        XFuture<Void> f2 = f.always(() -> {
            if (null != safeAbortReturnRobot) {
                try {
                    safeAbortReturnRobot
                            .call()
                            .always(this::showSafeAbortComplete);
                    return;
                } catch (Exception ex) {
                    Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            showSafeAbortComplete();
        });
        setLastSafeAbortAllFuture2(f2);
        clearCheckBoxes();
        setMainFuture(f2);
    }

    private void showSafeAbortComplete() {
        if (null != displayJFrame) {
            displayJFrame.showSafeAbortComplete();
        }
    }

    @Nullable
    private File lastTeachPropsFile = null;

    public void saveLastTeachPropsFile(File f) throws IOException {
        lastTeachPropsFile = f;
        try (PrintWriter pw = new PrintWriter(new FileWriter(lastTeachPropertiesFileFile))) {
            pw.println(f.getCanonicalPath());
        }
    }

    public void saveTeachProps(File f) throws IOException {
        object2DOuterJPanel1.setPropertiesFile(f);
        object2DOuterJPanel1.saveProperties();
        saveLastTeachPropsFile(f);
    }

    private final List<AprsSystemInterface> aprsSystems = new ArrayList<>();

    /**
     * Get the value of aprsSystems
     *
     * @return the value of aprsSystems
     */
    public List<AprsSystemInterface> getAprsSystems() {
        return Collections.unmodifiableList(aprsSystems);
    }

    /**
     * Close all systems.
     */
    public void closeAllAprsSystems() {
        for (AprsSystemInterface aprsJframe : aprsSystems) {
            try {
                aprsJframe.close();

            } catch (Exception ex) {
                log(Level.SEVERE, null, ex);
            }
        }
        aprsSystems.clear();
        updateTasksTable();
        updateRobotsTable();
    }

    public static Supervisor createSupervisor() {
        final Supervisor supervisor
                = GraphicsEnvironment.isHeadless()
                ? new Supervisor()
                : createAprsSupervisorWithSwingDisplay();
        return supervisor;
    }

    /**
     * Load the given setup file.
     *
     * @param f setup file to load
     * @throws IOException file could not be read
     */
    public final void loadSetupFile(File f) throws IOException {
        closeAllAprsSystems();
        System.out.println("Loading setup file :" + f.getCanonicalPath());
        try (CSVParser parser = CSVParser.parse(f, Charset.defaultCharset(), CSVFormat.DEFAULT.withHeader())) {
            DefaultTableModel tm = (DefaultTableModel) jTableTasks.getModel();
            String line = null;
            tm.setRowCount(0);
            int linecount = 0;
            for (CSVRecord csvRecord : parser) {
                if (csvRecord.size() < 4) {
                    logEventErr("Bad CSVRecord :" + linecount + " in " + f + "  --> " + csvRecord);
                    logEventErr("csvRecord.size()=" + csvRecord.size());
                    logEventErr("csvRecord.size() must equal 4");
                    System.out.println("");
                    break;
                }
                int priority = Integer.parseInt(csvRecord.get(0));
                String fileString = csvRecord.get(3);
                File propertiesFile = new File(csvRecord.get(3));
                File parentFile = f.getParentFile();
                if (null != parentFile) {
                    File altPropFile = parentFile.toPath().toRealPath().resolve(fileString).toFile();
                    if (altPropFile.exists()) {
                        propertiesFile = altPropFile;
                    }
                }
                AprsSystemInterface aj = AprsSystem.createSystem(propertiesFile);
                aj.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                aj.setPriority(priority);
                aj.setTaskName(csvRecord.get(1));
                aj.setRobotName(csvRecord.get(2));
                aj.setPropertiesFile(propertiesFile);
                aj.loadProperties();
                aj.setPriority(priority);
                aj.setTaskName(csvRecord.get(1));
                aj.setRobotName(csvRecord.get(2));
                aj.setVisible(true);
                aj.getTitleUpdateRunnables().add(() -> {
                    Utils.runOnDispatchThreadWithCatch(this::updateTasksTable);
                });
                aj.setSupervisorEventLogger(this::logEvent);
                aprsSystems.add(aj);
            }
        }
        Collections.sort(aprsSystems,
                (AprsSystemInterface o1, AprsSystemInterface o2) -> Integer.compare(o1.getPriority(), o2.getPriority()));
        updateTasksTable();
        updateRobotsTable();

        clearPosTable();
        setSetupFile(f);
    }

    public int incrementAndGetContinousDemoCycle() {
        return continousDemoCycle.incrementAndGet();
    }

    public void clearContinousDemoCycle() {
        continousDemoCycle.set(0);
    }

    private String lastUpdateTaskTableTaskNames @Nullable []  = null;

    private final ConcurrentHashMap<Integer, String> titleErrorMap = new ConcurrentHashMap<>();

    private volatile long liveImageStartTime = 0;
    private volatile long liveImageLastTime = 0;
    private volatile int liveImageFrameCount = 0;
    private volatile int lastLiveImageFrameCount = 0;
    private volatile @Nullable SeekableByteChannel liveImageMovieByteChannel = null;
    private volatile @Nullable AWTSequenceEncoder liveImageMovieEncoder = null;
    private volatile @Nullable File liveImageMovieFile = null;
    private volatile @Nullable File lastLiveImageMovieFile = null;

    public synchronized void startEncodingLiveImageMovie(BufferedImage image) {

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
            System.out.println("startEncodingLiveImageMovie: liveImageMovieFile = " + movieFile);
            SeekableByteChannel byteChannel = NIOUtils.writableChannel(movieFile);
            this.liveImageMovieByteChannel = byteChannel;
            liveImageMovieEncoder = new AWTSequenceEncoder(byteChannel, Rational.R(25, 1));
            liveImageMovieEncoder.encodeImage(image);
            liveImageFrameCount = 1;
        } catch (IOException ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
            finishEncodingLiveImageMovie();
        }
    }

    private boolean recordLiveImageMovieSelected = Boolean.getBoolean("recordLiveImageMovie");

    /**
     * Get the value of recordLiveImageMovieSelected
     *
     * @return the value of recordLiveImageMovieSelected
     */
    public boolean isRecordLiveImageMovieSelected() {
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

    public synchronized void continueEncodingLiveImageMovie(BufferedImage image) {
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
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
            finishEncodingLiveImageMovie();
        }
    }

    public synchronized void finishEncodingLiveImageMovie() {
        try {
            if (null != liveImageMovieEncoder) {
                liveImageMovieEncoder.finish();
            }
        } catch (IOException ex) {
            Logger.getLogger(Supervisor.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (null != liveImageMovieByteChannel) {
                NIOUtils.closeQuietly(liveImageMovieByteChannel);
                liveImageMovieByteChannel = null;
            }
            liveImageMovieEncoder = null;
            lastLiveImageMovieFile = liveImageMovieFile;
            System.out.println("liveImageStartTime = " + liveImageStartTime);
            System.out.println("liveImageLastTime = " + liveImageStartTime);
            int secs = (int) (500 + liveImageLastTime - liveImageStartTime) / 1000;
            System.out.println("secs = " + secs);
            System.out.println("liveImageFrameCount = " + liveImageFrameCount);
            if (secs > 0) {
                System.out.println("(liveImageFrameCount/secs) = " + (liveImageFrameCount / secs));
            }
            System.out.println("finishEncodingLiveImageMovie: lastLiveImageMovieFile = " + lastLiveImageMovieFile);
            liveImageMovieFile = null;
            lastLiveImageFrameCount = liveImageFrameCount;
            liveImageFrameCount = 0;
        }
    }

    private void updateTasksTable() {
        if (closing) {
            return;
        }
        DefaultTableModel tm = (DefaultTableModel) jTableTasks.getModel();
        boolean needSetJListFuturesModel = false;
        tm.setRowCount(0);
        if (lastUpdateTaskTableTaskNames == null
                || lastUpdateTaskTableTaskNames.length != aprsSystems.size()) {
            lastUpdateTaskTableTaskNames = new String[aprsSystems.size()];
            needSetJListFuturesModel = true;
        }
        BufferedImage liveImages[] = new BufferedImage[aprsSystems.size()];
        boolean newImage = false;
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystemInterface aprsJframe = aprsSystems.get(i);
            String taskName = aprsJframe.getTaskName();
            if (null != lastUpdateTaskTableTaskNames) {
                if (!Objects.equals(taskName, lastUpdateTaskTableTaskNames[i])) {
                    lastUpdateTaskTableTaskNames[i] = taskName;
                    needSetJListFuturesModel = true;
                }
            }
            if (!isPauseSelected() && !ignoreTitleErrors.get()) {
                String titleErr = aprsJframe.getTitleErrorString();
                if (titleErr != null
                        && titleErr.length() > 0
                        && !titleErr.equals(titleErrorMap.replace(aprsJframe.getMyThreadId(), titleErr))) {
                    logEvent(aprsJframe + " has title error " + titleErr);
                    logEvent(aprsJframe + " title error trace=" + shortTrace(aprsJframe.getSetTitleErrorStringTrace()));
                }
                if (aprsJframe.isPaused() && isPauseAllForOneSelected() && !resuming) {
                    logEvent(aprsJframe + " is paused");
                    pause();
                }
            }
            BufferedImage liveImage = aprsJframe.getLiveImage();
            if (null != liveImage) {
                liveImages[i] = liveImage;
                newImage = true;
            }
            tm.addRow(new Object[]{aprsJframe.getPriority(), taskName, aprsJframe.getRobotName(), aprsJframe.getScanImage(), liveImage, aprsJframe.getDetailsString(), aprsJframe.getPropertiesFile()});
        }
        if (isRecordLiveImageMovieSelected()) {
            combineAndEncodeLiveImages(newImage, liveImages);
        }
        completeUpdateTasksTable(needSetJListFuturesModel);
    }

    private synchronized void combineAndEncodeLiveImages(boolean newImage, BufferedImage[] liveImages) {
        long t = System.currentTimeMillis();
        if (newImage && (t - liveImageLastTime) > 40 && null != liveImages[0]) {
            BufferedImage combinedImage = new BufferedImage(liveImages[0].getWidth(), liveImages[0].getHeight() * liveImages.length, liveImages[0].getType());
            for (int i = 0; i < liveImages.length; i++) {
                combinedImage.getGraphics().drawImage(liveImages[i], 0, i * liveImages[0].getHeight(), null);
            }
            if (liveImageFrameCount == 0) {
                startEncodingLiveImageMovie(combinedImage);
            } else {
                continueEncodingLiveImageMovie(combinedImage);
            }
        }
    }

    @Nullable public XFuture<Void> getResumeFuture() {
        return resumeFuture.get();
    }

    private void completeUpdateTasksTable(boolean needSetJListFuturesModel) {
        if (null != displayJFrame) {
            displayJFrame.completeUpdateTasksTable(needSetJListFuturesModel);
        }
    }

    @Nullable
    private static Field getField(Class<?> clss, String name) {
        Field f = null;
        try {
            f = clss.getField(name);
        } catch (NoSuchFieldException | SecurityException ex) {
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
                Logger.getLogger(Supervisor.class
                        .getName()).log(Level.WARNING, null, ex1);

            }
        } catch (SecurityException ex) {
            Logger.getLogger(Supervisor.class
                    .getName()).log(Level.WARNING, null, ex);

        }
        if (f == null && clss.getSuperclass() != null && !Objects.equals(clss.getSuperclass(), Object.class
        )) {
            return getField(clss.getSuperclass(), name);
        }
        return f;
    }

    static private DefaultMutableTreeNode cfutureToNode(
            CompletableFuture<?> future,
            boolean showDoneFutures,
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
                getLogger().log(Level.SEVERE, null, ex);
            }
        }
        return node;
    }

    private static void addNodesForStackObject(
            @Nullable Object stackFieldObject,
            CompletableFuture<?> future,
            DefaultMutableTreeNode node,
            boolean showDoneFutures,
            boolean showUnnamedFutures)
            throws SecurityException {
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
                                if (showDoneFutures
                                        || (!cf.isDone() || notOk)) {
                                    node.add(cfutureToNode(cf, showDoneFutures, showUnnamedFutures));
                                }
                            }
                        }
                    }
                } catch (IllegalArgumentException | IllegalAccessException ex) {
//                    Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
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
                            addNodesForStackObject(nextFielStackObject, future, node, showDoneFutures, showUnnamedFutures);
                        }
                    }
                } catch (IllegalArgumentException | IllegalAccessException ex) {
//                    Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private static final int XFUTURE_MAX_DEPTH = 100;
    static private boolean firstDepthOverOccured = false;

    static private DefaultMutableTreeNode xfutureToNode(XFuture<?> future, boolean showDoneFutures, boolean showUnnamedFutures, int depth) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(future);

        if (depth >= XFUTURE_MAX_DEPTH) {
            if (!firstDepthOverOccured) {
                Logger.getLogger(AprsSystemInterface.class
                        .getName()).log(Level.SEVERE, "xfutureToNode : depth >= XFUTURE_MAX_DEPTH");
                firstDepthOverOccured = true;
            }
            return node;
        }
        if (null != future && depth < XFUTURE_MAX_DEPTH) {
            ConcurrentLinkedDeque<?> deque = future.getAlsoCancel();
            if (null != deque) {
                for (Object o : deque) {
                    if (o instanceof XFuture) {
                        XFuture<?> xf = (XFuture<?>) o;
                        if (showDoneFutures
                                || (!xf.isDone() || xf.isCompletedExceptionally() || xf.isCancelled())) {
                            node.add(xfutureToNode(xf, showDoneFutures, showUnnamedFutures, (depth + 1)));
                        }
                    } else if (o instanceof CompletableFuture) {
                        CompletableFuture<?> cf = (CompletableFuture<?>) o;
                        boolean notOk = cf.isCompletedExceptionally() || cf.isCancelled();
                        if (showUnnamedFutures || notOk) {
                            if (showDoneFutures
                                    || (!cf.isDone() || notOk)) {
                                node.add(cfutureToNode(cf, showDoneFutures, showUnnamedFutures));
                            }
                        }
                    }
                }
            }
        }
        return node;
    }

    private void updateRobotsTable() {
        if (closing) {
            return;
        }
        for (AprsSystemInterface aprsJframe : aprsSystems) {
            String robotname = aprsJframe.getRobotName();
            if (null != robotname) {
                robotEnableMap.put(robotname, true);
            }
        }
        loadRobotsTableFromSystemsList(aprsSystems);
        if (null != displayJFrame) {
            displayJFrame.updateRobotsTable();
        }
    }

    public void loadRobotsTableFromSystemsList(List<AprsSystemInterface> aprsSystems) {
        if(null != displayJFrame) {
            displayJFrame.loadRobotsTableFromSystemsList(aprsSystems);
        }
    }

    private void resetTeachSystemViewComboBox() {
        if (null != displayJFrame) {
            displayJFrame.resetTeachSystemViewComboBox();
        }
    }

    private void addTeachSystemViewComboBoxElement(String el) {
        if (null != displayJFrame) {
            displayJFrame.addTeachSystemViewComboBoxElement(el);
        }
    }

    private void setColorTextPanelLabelsAndIcons(@Nullable String robot0Name, @Nullable String robot1Name) {
        if (null != displayJFrame) {
            displayJFrame.setColorTextPanelLabelsAndIcons(robot0Name, robot1Name);
        }
    }

    public static Supervisor createAprsSupervisorWithSwingDisplay() {
        AprsSupervisorDisplayJFrame aprsSupervisorDisplayJFrame1 = new AprsSupervisorDisplayJFrame();
        aprsSupervisorDisplayJFrame1.setDefaultIconImage();
        Supervisor supervisor = new Supervisor(aprsSupervisorDisplayJFrame1);
        aprsSupervisorDisplayJFrame1.setSupervisor(supervisor);
        aprsSupervisorDisplayJFrame1.updateRobotsTable();
        return supervisor;
    }

    public static Supervisor createAprsSupervisorWithSwingDisplay(File propertiesFile) throws IOException {
        AprsSupervisorDisplayJFrame aprsSupervisorDisplayJFrame1 = new AprsSupervisorDisplayJFrame();
        aprsSupervisorDisplayJFrame1.setDefaultIconImage();
        Supervisor system = new Supervisor(aprsSupervisorDisplayJFrame1);
        system.loadSetupFile(propertiesFile);
        aprsSupervisorDisplayJFrame1.setSupervisor(system);
        aprsSupervisorDisplayJFrame1.updateRobotsTable();
        return system;
    }
}