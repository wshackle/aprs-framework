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
package aprs.actions.optaplanner.display;

import aprs.actions.optaplanner.actionmodel.OpAction;
import aprs.actions.optaplanner.actionmodel.OpActionPlan;
import aprs.actions.optaplanner.actionmodel.OpActionType;
import aprs.actions.optaplanner.actionmodel.score.EasyOpActionPlanScoreCalculator;
import aprs.conveyor.EditPropertiesJPanel;
import aprs.misc.Utils;
import crcl.utils.XFutureVoid;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("serial")
public class OptiplannerDisplayJFrame extends javax.swing.JFrame {

    /**
     * Creates new form OptiplannerTestJFrame
     */
    @SuppressWarnings({"nullness", "initialization"})
    @UIEffect
    public OptiplannerDisplayJFrame() {
        initComponents();
        clearPlans();
        outerOptiplannerJPanelInput.addActionsModifiedListener(() -> doSolve());
        loadRecentFilesMenu();
    }

    private static final boolean enableRecentFilesMenu
            = Boolean.getBoolean("aprs.actions.optaplanner.display.enableRecentFilesMenu");

    private static final File RECENT_ACTIONS_LIST_FILE
            = initRecentActionsFile();

    private static File initRecentActionsFile() {
        try {
            return Utils.file(Utils.getAprsUserHomeDir(), ".recentActionListsFile");
        } catch (Exception ex) {
            Logger.getLogger(OpActionPlan.class.getName()).log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }

    private static final List<File> recentActionListFiles = new ArrayList<>();

    private static volatile boolean recentActionsFileListRead = false;

    private static synchronized void readRecentActionListFile() throws IOException {
        try {
            if (!enableRecentFilesMenu) {
                return;
            }
            if (RECENT_ACTIONS_LIST_FILE.exists()) {

                List<File> newRecentActionListFiles = new ArrayList<>();
                try (BufferedReader br = new BufferedReader(new FileReader(RECENT_ACTIONS_LIST_FILE))) {
                    String line = br.readLine();
                    while (line != null) {
                        File f = Utils.file(line);
                        if (f.exists() && f.canRead()) {
                            newRecentActionListFiles.add(f);
                        }
                        line = br.readLine();
                    }
                }
                newRecentActionListFiles.sort(Comparators.byLongFunction(File::lastModified));
                if (newRecentActionListFiles.size() > 12) {
                    while (newRecentActionListFiles.size() > 12) {
                        newRecentActionListFiles.remove(0);
                    }
                    try (PrintWriter pw = new PrintWriter(new FileWriter(RECENT_ACTIONS_LIST_FILE))) {
                        for (int i = 0; i < recentActionListFiles.size(); i++) {
                            File f = recentActionListFiles.get(i);
                            pw.println(f.getCanonicalPath());
                        }
                    }
                }
                recentActionListFiles.clear();
                recentActionListFiles.addAll(newRecentActionListFiles);
            }
        } finally {
            recentActionsFileListRead = true;
        }
    }

    public static synchronized List<File> getRecentActionListFiles() {
        if (!recentActionsFileListRead) {
            try {
                readRecentActionListFile();
            } catch (IOException ex) {
                Logger.getLogger(OpActionPlan.class.getName()).log(Level.SEVERE, "", ex);
            } finally {
                recentActionsFileListRead = true;
            }
        }
        return java.util.Collections.unmodifiableList(new ArrayList<>(recentActionListFiles));
        //recentActionListFiles;
    }

    private void addRecentActionListFile(File f) throws IOException {
        if (enableRecentFilesMenu) {
            if (!recentActionsFileListRead) {
                try {
                    readRecentActionListFile();
                } finally {
                    recentActionsFileListRead = true;
                }
            }
            try (PrintWriter pw = new PrintWriter(new FileWriter(RECENT_ACTIONS_LIST_FILE, true))) {
                pw.println(f.getCanonicalPath());
            }
            recentActionListFiles.add(f);
        }
    }

    private void loadRecentFilesMenu() {
        jMenuRecentFiles.removeAll();
        if (!enableRecentFilesMenu) {
            jMenuRecentFiles.setEnabled(false);
            return;
        }
        List<File> recentFiles = getRecentActionListFiles();
        for (int i = 0; i < recentFiles.size(); i++) {
            File f = recentFiles.get(i);
            JMenuItem item = new JMenuItem(f.toString());
            item.addActionListener(e -> {
                try {
                    setInputOpActionPlan(OpActionPlan.loadActionList(f));
                } catch (IOException ex) {
                    Logger.getLogger(OptiplannerDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            jMenuRecentFiles.add(item);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    @UIEffect
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jPanelInput = new javax.swing.JPanel();
        outerOptiplannerJPanelInput = new aprs.actions.optaplanner.display.OuterOptiplannerJPanel();
        jPanelOutput = new javax.swing.JPanel();
        outerOptiplannerJPanelOutput = new aprs.actions.optaplanner.display.OuterOptiplannerJPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItemSaveInputList = new javax.swing.JMenuItem();
        jMenuItemLoadInputList = new javax.swing.JMenuItem();
        jMenuItemClear = new javax.swing.JMenuItem();
        jMenuItemSaveOutputList = new javax.swing.JMenuItem();
        jMenuItemLoadOutputList = new javax.swing.JMenuItem();
        jMenuRecentFiles = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        jMenuItemSolve = new javax.swing.JMenuItem();
        jMenuItemShuffleInputList = new javax.swing.JMenuItem();
        jMenuItemRepeatedShuffleTest = new javax.swing.JMenuItem();
        jMenuItemGenerate = new javax.swing.JMenuItem();
        jMenuItemSlowSolve = new javax.swing.JMenuItem();
        jMenuItemGreedySolve = new javax.swing.JMenuItem();
        jMenuItemComboSolve = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        jSplitPane1.setDividerLocation(300);

        outerOptiplannerJPanelInput.setLabel("Input");
        outerOptiplannerJPanelInput.setValueString(". . .");

        javax.swing.GroupLayout jPanelInputLayout = new javax.swing.GroupLayout(jPanelInput);
        jPanelInput.setLayout(jPanelInputLayout);
        jPanelInputLayout.setHorizontalGroup(
            jPanelInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInputLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(outerOptiplannerJPanelInput, javax.swing.GroupLayout.DEFAULT_SIZE, 276, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelInputLayout.setVerticalGroup(
            jPanelInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelInputLayout.createSequentialGroup()
                .addComponent(outerOptiplannerJPanelInput, javax.swing.GroupLayout.DEFAULT_SIZE, 310, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSplitPane1.setLeftComponent(jPanelInput);

        outerOptiplannerJPanelOutput.setLabel("Output");
        outerOptiplannerJPanelOutput.setValueString(". . .");

        javax.swing.GroupLayout jPanelOutputLayout = new javax.swing.GroupLayout(jPanelOutput);
        jPanelOutput.setLayout(jPanelOutputLayout);
        jPanelOutputLayout.setHorizontalGroup(
            jPanelOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelOutputLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(outerOptiplannerJPanelOutput, javax.swing.GroupLayout.DEFAULT_SIZE, 346, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelOutputLayout.setVerticalGroup(
            jPanelOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelOutputLayout.createSequentialGroup()
                .addComponent(outerOptiplannerJPanelOutput, javax.swing.GroupLayout.DEFAULT_SIZE, 310, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSplitPane1.setRightComponent(jPanelOutput);

        jMenu1.setText("File");

        jMenuItemSaveInputList.setText("Save Input List ...");
        jMenuItemSaveInputList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveInputListActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemSaveInputList);

        jMenuItemLoadInputList.setText("Load Input List ...");
        jMenuItemLoadInputList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemLoadInputListActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemLoadInputList);

        jMenuItemClear.setText("Clear");
        jMenuItemClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemClearActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemClear);

        jMenuItemSaveOutputList.setText("Save Output List ...");
        jMenuItemSaveOutputList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveOutputListActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemSaveOutputList);

        jMenuItemLoadOutputList.setText("Load Output List ...");
        jMenuItemLoadOutputList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemLoadOutputListActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemLoadOutputList);

        jMenuRecentFiles.setText("Recent Files ");
        jMenu1.add(jMenuRecentFiles);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Test");

        jMenuItemSolve.setText("Solve");
        jMenuItemSolve.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSolveActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItemSolve);

        jMenuItemShuffleInputList.setText("Shuffle Input List");
        jMenuItemShuffleInputList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemShuffleInputListActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItemShuffleInputList);

        jMenuItemRepeatedShuffleTest.setText("Repeated Shuffle Test");
        jMenuItemRepeatedShuffleTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemRepeatedShuffleTestActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItemRepeatedShuffleTest);

        jMenuItemGenerate.setText("Generate");
        jMenuItemGenerate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemGenerateActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItemGenerate);

        jMenuItemSlowSolve.setText("Simple Exhaustive Solve");
        jMenuItemSlowSolve.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSlowSolveActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItemSlowSolve);

        jMenuItemGreedySolve.setText("Greedy Solve");
        jMenuItemGreedySolve.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemGreedySolveActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItemGreedySolve);

        jMenuItemComboSolve.setText("Combo Solve");
        jMenuItemComboSolve.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemComboSolveActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItemComboSolve);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSplitPane1)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSplitPane1)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    @UIEffect
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        jSplitPane1.setDividerLocation(0.5);
    }//GEN-LAST:event_formComponentResized

    @UIEffect
    private void jMenuItemSaveInputListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveInputListActionPerformed
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                final OpActionPlan opActionPlan = outerOptiplannerJPanelInput.getOpActionPlan();
                if (null != opActionPlan) {
                    final File selectedFile = chooser.getSelectedFile();
                    opActionPlan.saveActionList(selectedFile);
                    addRecentActionListFile(selectedFile);
                }
            } catch (IOException ex) {
                Logger.getLogger(OptiplannerDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jMenuItemSaveInputListActionPerformed

    @UIEffect
    private void jMenuItemLoadInputListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLoadInputListActionPerformed
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                setInputOpActionPlan(OpActionPlan.loadActionList(chooser.getSelectedFile()));
            } catch (IOException ex) {
                Logger.getLogger(OptiplannerDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jMenuItemLoadInputListActionPerformed

    @UIEffect
    private void jMenuItemClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemClearActionPerformed
        clearPlans();
    }//GEN-LAST:event_jMenuItemClearActionPerformed

    private XFutureVoid clearPlans() {
        XFutureVoid setOutputFuture = setOutputOpActionPlan(new OpActionPlan());
        XFutureVoid setInputFuture = setInputOpActionPlan(new OpActionPlan());
        return XFutureVoid.allOf(setOutputFuture, setInputFuture);
    }

    @UIEffect
    private void jMenuItemSaveOutputListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveOutputListActionPerformed
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                final OpActionPlan opActionPlan = outerOptiplannerJPanelOutput.getOpActionPlan();
                if (null != opActionPlan) {
                    final File selectedFile = chooser.getSelectedFile();
                    opActionPlan.saveActionList(selectedFile);
                    addRecentActionListFile(selectedFile);
                }
            } catch (IOException ex) {
                Logger.getLogger(OptiplannerDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jMenuItemSaveOutputListActionPerformed

    @UIEffect
    private void jMenuItemSolveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSolveActionPerformed
        long timeDiff = doSolve();
        System.out.println("time to solve = " + timeDiff);
    }//GEN-LAST:event_jMenuItemSolveActionPerformed

    private long doSolve() {
        long t1 = System.currentTimeMillis();
        OpActionPlan inPlan = outerOptiplannerJPanelInput.getOpActionPlan();
        if (null == inPlan) {
            throw new NullPointerException("outerOptiplannerJPanelInput.getOpActionPlan() returned null");
        }
        inPlan.checkActionList();
        SolverFactory<OpActionPlan> solverFactory = OpActionPlan.createSolverFactory();
        Solver<OpActionPlan> solver = solverFactory.buildSolver();
        OpActionPlan outPlan = solver.solve(inPlan);
        inPlan.checkActionList();
        outPlan.checkActionList();
        setOutputOpActionPlan(outPlan);
        long t2 = System.currentTimeMillis();
        long timeDiff = (t2 - t1);
        return timeDiff;
    }

//    private long doBenchmark() {
//        long t1 = System.currentTimeMillis();
//        OpActionPlan inPlan = outerOptiplannerJPanelInput.getOpActionPlan();
//        if (null == inPlan) {
//            throw new NullPointerException("outerOptiplannerJPanelInput.getOpActionPlan() returned null");
//        }
//        inPlan.checkActionList();
//        PlannerBenchmarkFactory benchmarkFactory = OpActionPlan.createBenchmarkFactory();
//        GenerateActionConfig gac = new GenerateActionConfig();
//        gac.partInfoList.add(new GenerateActionPartInfo("A", 5, 6, 7));
//        gac.partInfoList.add(new GenerateActionPartInfo("B", 7, 6, 5));
//        gac.partInfoList.add(new GenerateActionPartInfo("C", 3, 3, 3));
//        Random rand = new Random();
//        OpActionPlan genPlan1 = generateRandomProblem(gac, rand);
//        OpActionPlan genPlan2 = generateRandomProblem(gac, rand);
//        PlannerBenchmark plannerBenchmark
//                = benchmarkFactory.buildPlannerBenchmark(
//                        inPlan,
//                        inPlan.cloneAndShufflePlan(),
//                        inPlan.cloneAndShufflePlan(),
//                        inPlan.cloneAndShufflePlan(),
//                        inPlan.cloneAndShufflePlan(),
//                        inPlan.cloneAndShufflePlan(),
//                        genPlan1,
//                        genPlan1.cloneAndShufflePlan(),
//                        genPlan2,
//                        genPlan1.cloneAndShufflePlan());
//        plannerBenchmark.benchmarkAndShowReportInBrowser();
//        long t2 = System.currentTimeMillis();
//        long timeDiff = (t2 - t1);
//        return timeDiff;
//    }
    @UIEffect
    private void jMenuItemLoadOutputListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLoadOutputListActionPerformed
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                setOutputOpActionPlan(OpActionPlan.loadActionList(chooser.getSelectedFile()));
            } catch (IOException ex) {
                Logger.getLogger(OptiplannerDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jMenuItemLoadOutputListActionPerformed

    @UIEffect
    private void jMenuItemShuffleInputListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemShuffleInputListActionPerformed
        doShuffle();
    }//GEN-LAST:event_jMenuItemShuffleInputListActionPerformed

    private long doShuffle() {
        long t1 = System.currentTimeMillis();
        OpActionPlan inPlan = outerOptiplannerJPanelInput.getOpActionPlan();
        if (null == inPlan) {
            throw new NullPointerException("outerOptiplannerJPanelInput.getOpActionPlan() returned null");
        }
        OpActionPlan newPlan = inPlan.cloneAndShufflePlan();
        setInputOpActionPlan(newPlan);
        long t2 = System.currentTimeMillis();
        return (t2 - t1);
    }

    private int shuffleTestRepeatCount = 100;

    @UIEffect
    private void jMenuItemRepeatedShuffleTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemRepeatedShuffleTestActionPerformed
        String countString = JOptionPane.showInputDialog("repeat count", shuffleTestRepeatCount);
        final int repeatCount = Integer.parseInt(countString);
        if (repeatCount < 1) {
            return;
        }
        shuffleTestRepeatCount = repeatCount;
        long totalShuffleTime = 0;
        long totalSolveTime = 0;
        OpActionPlan worstPlan = null;
        long worstScore = Long.MAX_VALUE;
        long bestScore = Long.MIN_VALUE;
        long bestInScore = Long.MIN_VALUE;
        OpActionPlan bestPlan = null;
        long worstShuffleTime = 0;
        long totalInScore = 0;
        long totalOutScore = 0;
        final double outScoresArray[] = new double[repeatCount];
        final double inScoresArray[] = new double[repeatCount];
        final double diffScoresArray[] = new double[repeatCount];
        final double timeArray[] = new double[repeatCount];

        boolean bestScoreIsInput = false;
        for (int i = 0; i < repeatCount; i++) {
            long shuffleTime = doShuffle();
            if (shuffleTime > worstShuffleTime) {
                worstShuffleTime = shuffleTime;
            }
            totalShuffleTime += shuffleTime;
            long t0 = System.currentTimeMillis();
            OpActionPlan inPlan = outerOptiplannerJPanelInput.getOpActionPlan();
            if (null == inPlan) {
                throw new NullPointerException("outerOptiplannerJPanelInput.getOpActionPlan() returned null");
            }
            HardSoftLongScore inPlanScore = inPlan.getScore();
            if (null == inPlanScore
                    || inPlanScore.getHardScore() == Long.MIN_VALUE
                    || inPlanScore.getSoftScore() == Long.MIN_VALUE) {
                EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
                inPlanScore = calculator.calculateScore(inPlan);
            }
            final long inSoftScore = inPlanScore.getSoftScore();
            if (inSoftScore > bestScore) {
                bestInScore = inSoftScore;
            }
            if (inSoftScore > bestInScore) {
//                bestScoreIsInput = true;
                bestScore = inSoftScore;
                bestPlan = inPlan;
            }
            if (inSoftScore < worstScore) {
                worstScore = inSoftScore;
                worstPlan = inPlan;
            }
            totalInScore += inSoftScore;
            long solveTime = doSolve();
            totalSolveTime += shuffleTime;
            OpActionPlan outPlan = outerOptiplannerJPanelOutput.getOpActionPlan();
            if (null == outPlan) {
                throw new NullPointerException("outerOptiplannerJPanelOutput.getOpActionPlan() returned null");
            }
            HardSoftLongScore outPlanScore = outPlan.getScore();
            if (null == outPlanScore
                    || outPlanScore.getHardScore() == Long.MIN_VALUE
                    || outPlanScore.getSoftScore() == Long.MIN_VALUE) {
                EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
                outPlanScore = calculator.calculateScore(outPlan);
            }
            final long outSoftScore = outPlanScore.getSoftScore();
            if (outSoftScore > bestScore) {
//                bestScoreIsInput = false;
                bestScore = outSoftScore;
                bestPlan = outPlan;
            }
            if (outSoftScore < worstScore) {
                worstScore = outSoftScore;
                worstPlan = outPlan;
            }
            totalOutScore += outSoftScore;
            long t1 = System.currentTimeMillis();

            outScoresArray[i] = (double) outSoftScore;
            inScoresArray[i] = (double) inSoftScore;
            diffScoresArray[i] = (double) (outSoftScore - inSoftScore);
            timeArray[i] = (double) (t1 - t0);
//            System.out.println("i=" + i + ",repeatCount=" + repeatCount);
            this.setTitle("i=" + i + ",repeatCount=" + repeatCount);
        }
        Arrays.sort(outScoresArray);
        Arrays.sort(inScoresArray);
        Arrays.sort(diffScoresArray);
        Arrays.sort(timeArray);

        setInputOpActionPlanOnDisplay(worstPlan);
        setOutputOpActionPlanOnDisplay(bestPlan);

        try {
            diagapplet.plotter.plotterJFrame.ShowDoubleArray("outscore", outScoresArray);
        } catch (Exception ex) {
            Logger.getLogger(OptiplannerDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            diagapplet.plotter.plotterJFrame.ShowDoubleArray("inscore", inScoresArray);
        } catch (Exception ex) {
            Logger.getLogger(OptiplannerDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            diagapplet.plotter.plotterJFrame.ShowDoubleArray("diffscore", diffScoresArray);
        } catch (Exception ex) {
            Logger.getLogger(OptiplannerDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            diagapplet.plotter.plotterJFrame.ShowDoubleArray("time", timeArray);
        } catch (Exception ex) {
            Logger.getLogger(OptiplannerDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        loadRecentFilesMenu();
    }//GEN-LAST:event_jMenuItemRepeatedShuffleTestActionPerformed

    public static class GenerateActionPartInfo {

        static byte c = (byte) 'A';
        String partType;
        int requiredPickups;
        int requiredDropOffs;
        int optionals;

        public GenerateActionPartInfo(String partType, int requiredPickups, int requiredDropOffs, int optionals) {
            this.partType = partType;
            this.requiredPickups = requiredPickups;
            this.requiredDropOffs = requiredDropOffs;
            this.optionals = optionals;
        }

        public GenerateActionPartInfo() {
            c++;
            this.partType = new String(new byte[]{c});
            this.requiredPickups = 1;
            this.requiredDropOffs = 1;
            this.optionals = 1;
        }
    }

    public static class GenerateActionConfig {

        double minDist;
        double minX;
        double maxX;
        double minY;
        double maxY;
        final List<GenerateActionPartInfo> partInfoList;

        public GenerateActionConfig(double minDist, double minX, double maxX, double minY, double maxY, List<GenerateActionPartInfo> partInfoList) {
            this.minDist = minDist;
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.partInfoList = partInfoList;
        }

        public GenerateActionConfig() {
            this.minDist = 5.0;
            this.minX = 10.0;
            this.maxX = 100.0;
            this.minY = 10.0;
            this.maxY = 100.0;
            this.partInfoList = new ArrayList<>();
        }
    }

    @UIEffect
    private void jMenuItemGenerateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemGenerateActionPerformed

        Map<String, String> map0 = new TreeMap<>();
        map0.put("Number of Part Types?", "1");
        map0.put("Minimum Distance between objects?", "5.0");
        map0.put("Minimum X?", "10.0");
        map0.put("Maximum X?", "100.0");
        map0.put("Minimum Y?", "10.0");
        map0.put("Maximum Y?", "100.0");
        Map<String, String> map1 = EditPropertiesJPanel.editProperties(this, "Problem Generate Properties", true, map0);
        if (null == map1) {
            return;
        }
        GenerateActionConfig gac = new GenerateActionConfig();
        int partsTypeNeeded = Integer.parseInt(map1.getOrDefault("Number of Part Types?", "1"));
        gac.minDist = Double.parseDouble(map1.getOrDefault("Minimum Distance between objects?", "5.0"));
        gac.minX = Double.parseDouble(map1.getOrDefault("Minimum X?", "10.0"));
        gac.maxX = Double.parseDouble(map1.getOrDefault("Maximum X?", "100.0"));
        gac.minY = Double.parseDouble(map1.getOrDefault("Minimum Y?", "10.0"));
        gac.maxY = Double.parseDouble(map1.getOrDefault("Maximum Y?", "100.0"));

        Random rand = new Random();
        for (int i = 0; i < partsTypeNeeded; i++) {
            GenerateActionPartInfo gapi = new GenerateActionPartInfo();
            byte c = (byte) ('A' + i);
            gapi.partType = new String(new byte[]{c});
            Map<String, String> map2 = new TreeMap<>();
            final String pickupsInit = Integer.toString(rand.nextInt(8));
            final String dropoffsInit = Integer.toString(rand.nextInt(8));
            final String optionalsInit = Integer.toString(rand.nextInt(8));
            map2.put("Number of Required Pickups of " + gapi.partType + "?", pickupsInit);
            map2.put("Number of Required Dropoffs of " + gapi.partType + "?", dropoffsInit);
            map2.put("Number of Optionals of " + gapi.partType + "?", Integer.toString(rand.nextInt(8)));
            Map<String, String> map3 = EditPropertiesJPanel.editProperties(this, "Problem Generate Properties for parts of " + gapi.partType, true, map2);
            if (null == map3) {
                return;
            }
            gapi.requiredPickups = Integer.parseInt(map3.getOrDefault("Number of Required Pickups of " + gapi.partType + "?", pickupsInit));
            gapi.requiredDropOffs = Integer.parseInt(map3.getOrDefault("Number of Required Dropoffs of " + gapi.partType + "?", dropoffsInit));
            gapi.optionals = Integer.parseInt(map3.getOrDefault("Number of Optionals of " + gapi.partType + "?", optionalsInit));
            gac.partInfoList.add(gapi);
        }
        OpActionPlan ap = generateRandomProblem(gac, rand);
        try {
            final File tempFile = File.createTempFile("generatedActionsList", ".csv");
            ap.saveActionList(tempFile);
            addRecentActionListFile(tempFile);
        } catch (IOException ex) {
            Logger.getLogger(OptiplannerDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        setInputOpActionPlan(ap);
        doSolve();
        loadRecentFilesMenu();
    }//GEN-LAST:event_jMenuItemGenerateActionPerformed

    public OpActionPlan generateRandomProblem(GenerateActionConfig gac, Random rand) {
        OpAction.setAllowedPartTypes(gac.partInfoList.stream().map(gapi -> gapi.partType).collect(Collectors.toSet()));
        List<OpAction> startingList = new ArrayList<>();
        for (int i = 0; i < gac.partInfoList.size(); i++) {
            GenerateActionPartInfo gapi = gac.partInfoList.get(i);
            int optionalPickups;
            int optionalDropOffs;
            if (gapi.requiredPickups >= gapi.requiredDropOffs) {
                optionalPickups = 0;
                optionalDropOffs = gapi.optionals;
                if (optionalDropOffs < gapi.requiredPickups - gapi.requiredDropOffs) {
                    optionalDropOffs = gapi.requiredPickups - gapi.requiredDropOffs;
                }
            } else {
                optionalPickups = gapi.optionals;
                if (optionalPickups < gapi.requiredDropOffs - gapi.requiredPickups) {
                    optionalPickups = gapi.requiredDropOffs - gapi.requiredPickups;
                }
                optionalDropOffs = 0;
            }
            addActionsForPartType(gapi.requiredPickups, optionalPickups, gapi.requiredDropOffs, optionalDropOffs, gapi.partType, gac.minX, gac.maxX, gac.minY, gac.maxY, rand, startingList, gac.minDist);
        }
        startingList.add(new OpAction(OpActionType.START.toString(), 0, 50.0, OpActionType.START, "START", true));
        List<OpAction> shuffledList = new ArrayList<>(startingList);
        OpActionPlan ap = new OpActionPlan();
        ap.getEndAction().getLocation().x = 100.0;
        ap.getEndAction().getLocation().y = 50.0;
        ap.setAccelleration(0.1);
        ap.setMaxSpeed(0.25);
        ap.setStartEndMaxSpeed(1.0);
        Collections.shuffle(shuffledList);
        ap.setActions(shuffledList);
        // Set the location to return to after the task is complete.
        ap.getEndAction().setLocation(new Point2D.Double(7, 0));
        String apStr = ap.computeString();
        System.out.println("apStr = " + apStr);
        ap.initNextActions();
        ap.checkActionList();
        return ap;
    }

    private void jMenuItemSlowSolveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSlowSolveActionPerformed
        long timeDiff = doExhaustiveSolve();
        System.out.println("time to slow solve = " + timeDiff);
    }//GEN-LAST:event_jMenuItemSlowSolveActionPerformed

    private void jMenuItemGreedySolveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemGreedySolveActionPerformed
        long timeDiff = doGreedySolve();
        System.out.println("time to greedy solve = " + timeDiff);
    }//GEN-LAST:event_jMenuItemGreedySolveActionPerformed

    private void jMenuItemComboSolveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemComboSolveActionPerformed
        long timeDiff = doComboSolve();
        System.out.println("time to combo solve = " + timeDiff);
    }//GEN-LAST:event_jMenuItemComboSolveActionPerformed

    private long doExhaustiveSolve() {
        long t1 = System.currentTimeMillis();
        OpActionPlan inPlan = outerOptiplannerJPanelInput.getOpActionPlan();
        if (null == inPlan) {
            throw new NullPointerException("outerOptiplannerJPanelInput.getOpActionPlan() returned null");
        }
        inPlan.checkActionList();
//        SolverFactory<OpActionPlan> solverFactory = OpActionPlan.createSolverFactory();
//        Solver<OpActionPlan> solver = solverFactory.buildSolver();
//        OpActionPlan outPlan = solver.solve(inPlan);
        OpActionPlan outPlan = inPlan.simpleExhaustiveSearch();
        inPlan.checkActionList();
        outPlan.checkActionList();
        setOutputOpActionPlan(outPlan);
        long t2 = System.currentTimeMillis();
        long timeDiff = (t2 - t1);
        return timeDiff;
    }

    private long doGreedySolve() {
        long t1 = System.currentTimeMillis();
        OpActionPlan inPlan = outerOptiplannerJPanelInput.getOpActionPlan();
        if (null == inPlan) {
            throw new NullPointerException("outerOptiplannerJPanelInput.getOpActionPlan() returned null");
        }
        inPlan.checkActionList();
        OpActionPlan outPlan = inPlan.greedySearch();
        inPlan.checkActionList();
        outPlan.checkActionList();
        setOutputOpActionPlan(outPlan);
        long t2 = System.currentTimeMillis();
        long timeDiff = (t2 - t1);
        return timeDiff;
    }

    private long doComboSolve() {
        long t1 = System.currentTimeMillis();
        OpActionPlan inPlan = outerOptiplannerJPanelInput.getOpActionPlan();
        if (null == inPlan) {
            throw new NullPointerException("outerOptiplannerJPanelInput.getOpActionPlan() returned null");
        }
        inPlan.checkActionList();
        OpActionPlan outPlan = inPlan.comboSearch();
        inPlan.checkActionList();
        outPlan.checkActionList();
        setOutputOpActionPlan(outPlan);
        long t2 = System.currentTimeMillis();
        long timeDiff = (t2 - t1);
        return timeDiff;
    }

    private void addActionsForPartType(int requiredPickups, int optionalPickups, int requiredDropOffs, int optionalDropOffs, String partType, double minX, double maxX, double minY, double maxY, Random rand, List<OpAction> startingList, double minDist) {
        for (int i = 0; i < requiredPickups; i++) {
            OpActionType actionType = OpActionType.PICKUP;
            boolean required = true;
            OpAction newAction = generateNewAction(i, minX, maxX, minY, maxY, rand, startingList, minDist, actionType, partType, required);
            startingList.add(newAction);
        }

        for (int i = 0; i < optionalPickups; i++) {
            OpActionType actionType = OpActionType.PICKUP;
            boolean required = false;
            OpAction newAction = generateNewAction(i, minX, maxX, minY, maxY, rand, startingList, minDist, actionType, partType, required);
            startingList.add(newAction);
        }

        for (int i = 0; i < requiredDropOffs; i++) {
            OpActionType actionType = OpActionType.DROPOFF;
            boolean required = true;
            OpAction newAction = generateNewAction(i, minX, maxX, minY, maxY, rand, startingList, minDist, actionType, partType, required);
            startingList.add(newAction);
        }

        for (int i = 0; i < optionalDropOffs; i++) {
            OpActionType actionType = OpActionType.DROPOFF;
            boolean required = false;
            OpAction newAction = generateNewAction(i, minX, maxX, minY, maxY, rand, startingList, minDist, actionType, partType, required);
            startingList.add(newAction);
        }
    }

//    private void addActionForEachPartType(Set<String> partTypesSet, double minX, double maxX, double minY, double maxY, Random rand, List<OpAction> startingList, double minDist, OpActionType actionType, boolean required) {
//        for (String partType : partTypesSet) {
//            OpAction newAction = generateNewAction(0, minX, maxX, minY, maxY, rand, startingList, minDist, actionType, partType, required);
//            startingList.add(newAction);
//        }
//    }
    private OpAction generateNewAction(int index, double minX, double maxX, double minY, double maxY, Random rand, List<OpAction> startingList, double minDist, OpActionType actionType, String partType, boolean required) {
        double x = minX + (maxX - minX) * rand.nextDouble();
        double y = minY + (maxY - minY) * rand.nextDouble();
        double dist = findMinDistActionList(startingList, x, y);
        while (dist < minDist) {
            x = minX + (maxX - minX) * rand.nextDouble();
            y = minX + (maxY - minY) * rand.nextDouble();
            dist = findMinDistActionList(startingList, x, y);
        }
        OpAction newAction = new OpAction(actionType + " " + partType + index + (required ? "" : "-alt"), x, y, actionType, partType, required);
        return newAction;
    }

    private double findMinDistActionList(List<OpAction> startingList, double x, double y) {
        double dist;
        dist = startingList
                .stream()
                .map(OpAction::getLocation)
                .mapToDouble((Point2D.Double loc) -> Math.hypot(x - loc.x, y - loc.y))
                .min()
                .orElse(Double.POSITIVE_INFINITY);
        return dist;
    }

    /**
     * @param args the command line arguments
     */
    @SuppressWarnings({"guieffect"})
    public static void main(String args[]) {

        Utils.setToAprsLookAndFeel();

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new OptiplannerDisplayJFrame().setVisible(true);
            }
        });
    }

    /**
     * Show a window showing the
     *
     * @param inputPlan input plan to show
     * @param outputPlan output plan to show
     * @param title title of new window
     * @param defaultCloseOperation default close operation for displayed frame
     */
    public static void showPlan(
            OpActionPlan inputPlan,
            OpActionPlan outputPlan,
            String title,
            int defaultCloseOperation) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            OptiplannerDisplayJFrame frm = new OptiplannerDisplayJFrame();
            frm.setDefaultCloseOperation(defaultCloseOperation);
            frm.setInputOpActionPlan(inputPlan);
            frm.setOutputOpActionPlan(outputPlan);
            frm.setTitle(title);
            frm.setVisible(true);
        });
    }

    /**
     * Get the value of opActionPlan
     *
     * @return the value of opActionPlan
     */
    @SafeEffect
    public @Nullable
    OpActionPlan getInputOpActionPlan() {
        return outerOptiplannerJPanelInput.getOpActionPlan();
    }

    /**
     * Set the value of opActionPlan
     *
     * @param opActionPlan new value of opActionPlan
     * @return future for determining when operation is complete
     */
    public XFutureVoid setInputOpActionPlan(@Nullable OpActionPlan opActionPlan) {
        XFutureVoid part1Future = outerOptiplannerJPanelInput.setOpActionPlan(opActionPlan);
        return part1Future.thenComposeAsyncToVoid(() -> setInputOpActionPlanPart2(opActionPlan), Utils.getDispatchThreadExecutorService());
    }

    /**
     * Set the value of opActionPlan
     *
     * @param opActionPlan new value of opActionPlan
     * @return future for determining when operation is complete
     */
    @UIEffect
    public void setInputOpActionPlanOnDisplay(@Nullable OpActionPlan opActionPlan) {
        assert SwingUtilities.isEventDispatchThread();
        outerOptiplannerJPanelInput.setOpActionPlanOnDisplay(opActionPlan);
        setInputOpActionPlanPart2OnDisplay(opActionPlan);
    }

    private void setInputOpActionPlanPart2OnDisplay(@Nullable OpActionPlan opActionPlan) {
        if (null == opActionPlan) {
            outerOptiplannerJPanelInput.setValueString("null");
            return;
        }
        EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
        HardSoftLongScore score = calculator.calculateScore(opActionPlan);
        opActionPlan.setScore(score);
        final long softScoreFinal = score.getSoftScore();
        System.out.println("Input : score = " + score);
        outerOptiplannerJPanelInput.setValueString(" " + softScoreFinal);
    }

    private XFutureVoid setInputOpActionPlanPart2(@Nullable OpActionPlan opActionPlan) {
        if (null == opActionPlan) {
            return Utils.runOnDispatchThread(() -> outerOptiplannerJPanelInput.setValueString("null"));
        }
        EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
        HardSoftLongScore score = calculator.calculateScore(opActionPlan);
        opActionPlan.setScore(score);
        final long softScoreFinal = score.getSoftScore();
        System.out.println("Input : score = " + score);
        return Utils.runOnDispatchThread(() -> outerOptiplannerJPanelInput.setValueString(" " + softScoreFinal));
    }

    /**
     * Get the value of opActionPlan
     *
     * @return the value of opActionPlan
     */
    @SafeEffect
    public @Nullable
    OpActionPlan getOutputOpActionPlan() {
        return outerOptiplannerJPanelOutput.getOpActionPlan();
    }

    /**
     * Set the value of opActionPlan
     *
     * @param opActionPlan new value of opActionPlan
     * @return future for determining when operation is complete
     */
    public XFutureVoid setOutputOpActionPlan(@Nullable OpActionPlan opActionPlan) {
        XFutureVoid part1Future = outerOptiplannerJPanelOutput.setOpActionPlan(opActionPlan);
        return part1Future.thenRunAsync(() -> setOutputOpActionPlanPart2OnDisplay(opActionPlan), Utils.getDispatchThreadExecutorService());
    }

    /**
     * Set the value of opActionPlan
     *
     * @param opActionPlan new value of opActionPlan
     * @return future for determining when operation is complete
     */
    @UIEffect
    public void setOutputOpActionPlanOnDisplay(@Nullable OpActionPlan opActionPlan) {
        assert SwingUtilities.isEventDispatchThread();
        outerOptiplannerJPanelOutput.setOpActionPlanOnDisplay(opActionPlan);
        setOutputOpActionPlanPart2OnDisplay(opActionPlan);
    }

    private XFutureVoid setOutputOpActionPlanPart2(@Nullable OpActionPlan opActionPlan) {
        if (null == opActionPlan) {
            return Utils.runOnDispatchThread(() -> outerOptiplannerJPanelOutput.setValueString(" null"));
        }

        EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
        HardSoftLongScore score = calculator.calculateScore(opActionPlan);
        opActionPlan.setScore(score);
        System.out.println("Output : score = " + score);
        final long softScoreFinal = score.getSoftScore();

        return Utils.runOnDispatchThread(() -> outerOptiplannerJPanelOutput.setValueString(" " + softScoreFinal));
    }

    @UIEffect
    private void setOutputOpActionPlanPart2OnDisplay(@Nullable OpActionPlan opActionPlan) {
        assert SwingUtilities.isEventDispatchThread();
        if (null == opActionPlan) {
            outerOptiplannerJPanelOutput.setValueString(" null");
            return;
        }

        EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
        HardSoftLongScore score = calculator.calculateScore(opActionPlan);
        opActionPlan.setScore(score);
        System.out.println("Output : score = " + score);
        final long softScoreFinal = score.getSoftScore();
        outerOptiplannerJPanelOutput.setValueString(" " + softScoreFinal);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItemClear;
    private javax.swing.JMenuItem jMenuItemComboSolve;
    private javax.swing.JMenuItem jMenuItemGenerate;
    private javax.swing.JMenuItem jMenuItemGreedySolve;
    private javax.swing.JMenuItem jMenuItemLoadInputList;
    private javax.swing.JMenuItem jMenuItemLoadOutputList;
    private javax.swing.JMenuItem jMenuItemRepeatedShuffleTest;
    private javax.swing.JMenuItem jMenuItemSaveInputList;
    private javax.swing.JMenuItem jMenuItemSaveOutputList;
    private javax.swing.JMenuItem jMenuItemShuffleInputList;
    private javax.swing.JMenuItem jMenuItemSlowSolve;
    private javax.swing.JMenuItem jMenuItemSolve;
    private javax.swing.JMenu jMenuRecentFiles;
    private javax.swing.JPanel jPanelInput;
    private javax.swing.JPanel jPanelOutput;
    private javax.swing.JSplitPane jSplitPane1;
    private aprs.actions.optaplanner.display.OuterOptiplannerJPanel outerOptiplannerJPanelInput;
    private aprs.actions.optaplanner.display.OuterOptiplannerJPanel outerOptiplannerJPanelOutput;
    // End of variables declaration//GEN-END:variables
}
