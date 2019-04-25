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

import aprs.actions.optaplanner.actionmodel.OpActionPlan;
import aprs.actions.optaplanner.actionmodel.score.EasyOpActionPlanScoreCalculator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class OptiplannerDisplayJFrame extends javax.swing.JFrame {

    /**
     * Creates new form OptiplannerTestJFrame
     */
    public OptiplannerDisplayJFrame() {
        initComponents();
        clearPlans();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jPanelInput = new javax.swing.JPanel();
        jLabelInput = new javax.swing.JLabel();
        outerOptiplannerJPanelInput = new aprs.actions.optaplanner.display.OuterOptiplannerJPanel();
        jPanelOutput = new javax.swing.JPanel();
        jLabelOutput = new javax.swing.JLabel();
        outerOptiplannerJPanelOutput = new aprs.actions.optaplanner.display.OuterOptiplannerJPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItemSaveInputList = new javax.swing.JMenuItem();
        jMenuItemLoadInputList = new javax.swing.JMenuItem();
        jMenuItemClear = new javax.swing.JMenuItem();
        jMenuItemSaveOutputList = new javax.swing.JMenuItem();
        jMenuItemLoadOutputList = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItemSolve = new javax.swing.JMenuItem();
        jMenuItemShuffleInputList = new javax.swing.JMenuItem();
        jMenuItemRepeatedShuffleTest = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        jSplitPane1.setDividerLocation(300);

        jLabelInput.setText("Input");

        outerOptiplannerJPanelInput.setShowSkippedActions(true);

        javax.swing.GroupLayout jPanelInputLayout = new javax.swing.GroupLayout(jPanelInput);
        jPanelInput.setLayout(jPanelInputLayout);
        jPanelInputLayout.setHorizontalGroup(
            jPanelInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInputLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelInput)
                    .addComponent(outerOptiplannerJPanelInput, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelInputLayout.setVerticalGroup(
            jPanelInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInputLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabelInput)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(outerOptiplannerJPanelInput, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSplitPane1.setLeftComponent(jPanelInput);

        jLabelOutput.setText("Output:");

        javax.swing.GroupLayout jPanelOutputLayout = new javax.swing.GroupLayout(jPanelOutput);
        jPanelOutput.setLayout(jPanelOutputLayout);
        jPanelOutputLayout.setHorizontalGroup(
            jPanelOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelOutputLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelOutputLayout.createSequentialGroup()
                        .addComponent(jLabelOutput)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(outerOptiplannerJPanelOutput, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelOutputLayout.setVerticalGroup(
            jPanelOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelOutputLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabelOutput)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(outerOptiplannerJPanelOutput, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        jSplitPane1.setDividerLocation(0.5);
    }//GEN-LAST:event_formComponentResized

    private void jMenuItemSaveInputListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveInputListActionPerformed
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                outerOptiplannerJPanelInput.getOpActionPlan().saveActionList(chooser.getSelectedFile());
            } catch (IOException ex) {
                Logger.getLogger(OptiplannerDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jMenuItemSaveInputListActionPerformed

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

    private void jMenuItemClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemClearActionPerformed
        clearPlans();
    }//GEN-LAST:event_jMenuItemClearActionPerformed

    private void clearPlans() {
        setOutputOpActionPlan(new OpActionPlan());
        setInputOpActionPlan(new OpActionPlan());
    }

    private void jMenuItemSaveOutputListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveOutputListActionPerformed
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                outerOptiplannerJPanelOutput.getOpActionPlan().saveActionList(chooser.getSelectedFile());
            } catch (IOException ex) {
                Logger.getLogger(OptiplannerDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jMenuItemSaveOutputListActionPerformed

    private void jMenuItemSolveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSolveActionPerformed
        long timeDiff = doSolve();
        System.out.println("time to solve = " + timeDiff);
    }//GEN-LAST:event_jMenuItemSolveActionPerformed

    private long doSolve() {
        long t1 = System.currentTimeMillis();
        OpActionPlan inPlan = outerOptiplannerJPanelInput.getOpActionPlan();
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

    private void jMenuItemShuffleInputListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemShuffleInputListActionPerformed
        doShuffle();
    }//GEN-LAST:event_jMenuItemShuffleInputListActionPerformed

    private long doShuffle() {
        long t1 = System.currentTimeMillis();
        OpActionPlan inPlan = outerOptiplannerJPanelInput.getOpActionPlan();
        OpActionPlan newPlan = OpActionPlan.cloneAndShufflePlan(inPlan);
        setInputOpActionPlan(newPlan);
        long t2 = System.currentTimeMillis();
        return (t2 - t1);
    }

    private void jMenuItemRepeatedShuffleTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemRepeatedShuffleTestActionPerformed
        String countString = JOptionPane.showInputDialog("repeat count", "100");
        int repeatCount = Integer.parseInt(countString);
        long totalShuffleTime = 0;
        long totalSolveTime = 0;
        OpActionPlan worstPlan = null;
        long worstScore = Long.MAX_VALUE;
        long bestScore = Long.MIN_VALUE;
        OpActionPlan bestPlan = null;
        long worstShuffleTime = 0;
        long totalInScore = 0;
        long totalOutScore = 0;
        List<Double> outScoresList = new ArrayList<>();
        for (int i = 0; i < repeatCount; i++) {
            long shuffleTime = doShuffle();
            if(shuffleTime > worstShuffleTime) {
                worstShuffleTime = shuffleTime;
            }
            totalShuffleTime += shuffleTime;
            OpActionPlan inPlan = outerOptiplannerJPanelInput.getOpActionPlan();
            HardSoftLongScore inPlanScore = inPlan.getScore();
            if (null == inPlanScore) {
                EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
                inPlanScore = calculator.calculateScore(inPlan);
            }
            if(inPlanScore.getSoftScore() > bestScore) {
                bestScore = inPlanScore.getSoftScore();
                bestPlan = inPlan;
            }
            if(inPlanScore.getSoftScore() < worstScore) {
                worstScore = inPlanScore.getSoftScore();
                worstPlan = inPlan;
            }
            totalInScore += inPlanScore.getSoftScore();
            long solveTime = doSolve();
            totalSolveTime += shuffleTime;
            OpActionPlan outPlan = outerOptiplannerJPanelOutput.getOpActionPlan();
            HardSoftLongScore outPlanScore = outPlan.getScore();
            if (null == outPlanScore) {
                EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
                outPlanScore = calculator.calculateScore(outPlan);
            }
            if(outPlanScore.getSoftScore() > bestScore) {
                bestScore = outPlanScore.getSoftScore();
                bestPlan = outPlan;
            }
            if(outPlanScore.getSoftScore() < worstScore) {
                worstScore = outPlanScore.getSoftScore();
                worstPlan = outPlan;
            }
            totalOutScore += outPlanScore.getSoftScore();
            outScoresList.add((double)outPlanScore.getSoftScore());
            System.out.println("i="+i+",repeatCount="+repeatCount);
            this.setTitle("i="+i+",repeatCount="+repeatCount);
        }
        Collections.sort(outScoresList);
        double avgInScore = ((double)totalInScore)/((double)repeatCount);
        System.out.println("avgInScore = " + avgInScore);
        double avgOutScore = ((double)totalOutScore)/((double)repeatCount);
        System.out.println("avgOutScore = " + avgOutScore);
        System.out.println("worstScore = " + worstScore);
        System.out.println("bestScore = " + bestScore);
        setInputOpActionPlan(worstPlan);
        setOutputOpActionPlan(bestPlan);
        double outScoresArray[] = new double[outScoresList.size()];
        for (int i = 0; i < outScoresArray.length; i++) {
            outScoresArray[i] = outScoresList.get(i);
        }
        try {
            diagapplet.plotter.plotterJFrame.ShowDoubleArray("outScoreHist", outScoresArray);
        } catch (Exception ex) {
            Logger.getLogger(OptiplannerDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemRepeatedShuffleTestActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(OptiplannerDisplayJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(OptiplannerDisplayJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(OptiplannerDisplayJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(OptiplannerDisplayJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new OptiplannerDisplayJFrame().setVisible(true);
            }
        });
    }

    /**
     * Show a window showing the
     *
     * @param plan plan to show
     * @param title title of new window
     */
    public static void showPlan(OpActionPlan inputPlan, OpActionPlan outputPlan, String title, int defaultCloseOperation) {
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
    @Nullable
    @SafeEffect
    public OpActionPlan getInputOpActionPlan() {
        return outerOptiplannerJPanelInput.getOpActionPlan();
    }

    /**
     * Set the value of opActionPlan
     *
     * @param opActionPlan new value of opActionPlan
     */
    @SafeEffect
    public void setInputOpActionPlan(@Nullable OpActionPlan opActionPlan) {
        outerOptiplannerJPanelInput.setOpActionPlan(opActionPlan);
        HardSoftLongScore score = opActionPlan.getScore();
        if (null == score) {
            EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
            score = calculator.calculateScore(opActionPlan);
            opActionPlan.setScore(score);
        }
        jLabelInput.setText("Input : " + score.getSoftScore());
    }

    /**
     * Get the value of opActionPlan
     *
     * @return the value of opActionPlan
     */
    @Nullable
    @SafeEffect
    public OpActionPlan getOutputOpActionPlan() {
        return outerOptiplannerJPanelOutput.getOpActionPlan();
    }

    /**
     * Set the value of opActionPlan
     *
     * @param opActionPlan new value of opActionPlan
     */
    @SafeEffect
    public void setOutputOpActionPlan(@Nullable OpActionPlan opActionPlan) {
        outerOptiplannerJPanelOutput.setOpActionPlan(opActionPlan);
        HardSoftLongScore score = opActionPlan.getScore();
        if (null == score) {
            EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
            score = calculator.calculateScore(opActionPlan);
            opActionPlan.setScore(score);
        }
        jLabelOutput.setText("Output : " + score.getSoftScore());
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabelInput;
    private javax.swing.JLabel jLabelOutput;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItemClear;
    private javax.swing.JMenuItem jMenuItemLoadInputList;
    private javax.swing.JMenuItem jMenuItemLoadOutputList;
    private javax.swing.JMenuItem jMenuItemRepeatedShuffleTest;
    private javax.swing.JMenuItem jMenuItemSaveInputList;
    private javax.swing.JMenuItem jMenuItemSaveOutputList;
    private javax.swing.JMenuItem jMenuItemShuffleInputList;
    private javax.swing.JMenuItem jMenuItemSolve;
    private javax.swing.JPanel jPanelInput;
    private javax.swing.JPanel jPanelOutput;
    private javax.swing.JSplitPane jSplitPane1;
    private aprs.actions.optaplanner.display.OuterOptiplannerJPanel outerOptiplannerJPanelInput;
    private aprs.actions.optaplanner.display.OuterOptiplannerJPanel outerOptiplannerJPanelOutput;
    // End of variables declaration//GEN-END:variables
}
