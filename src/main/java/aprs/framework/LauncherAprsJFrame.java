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
package aprs.framework;

import static aprs.framework.Utils.copyOfRangeNonNullsOnly;
import aprs.framework.learninggoals.GoalLearnerTest;
import aprs.framework.optaplanner.OptaplannerTest;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class LauncherAprsJFrame extends javax.swing.JFrame {

    /**
     * Creates new form LauncherJFrame
     */
    @SuppressWarnings("initialization")
    public LauncherAprsJFrame() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked","nullness"})
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelMultiWorkcellSystem = new javax.swing.JPanel();
        jButtonPrevMulti = new javax.swing.JButton();
        jButtonOpenMulti = new javax.swing.JButton();
        jButtonNewMulti = new javax.swing.JButton();
        jPanelSingleWorkcellSystem = new javax.swing.JPanel();
        jButtonNewSingle = new javax.swing.JButton();
        jButtonPrevSingle = new javax.swing.JButton();
        jButtonOpenSingle = new javax.swing.JButton();
        jPanelTestsDemos = new javax.swing.JPanel();
        jButtonOptaplannerTest = new javax.swing.JButton();
        jButtonGoalLearningTest = new javax.swing.JButton();
        jButtonOpenSingle1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("APRS Launcher");
        setResizable(false);

        jPanelMultiWorkcellSystem.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Multi Workcell Supervisor System", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("DejaVu Sans", 1, 24))); // NOI18N

        jButtonPrevMulti.setFont(new java.awt.Font("DejaVu Sans", 0, 18)); // NOI18N
        jButtonPrevMulti.setText("Previous ");
        jButtonPrevMulti.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPrevMultiActionPerformed(evt);
            }
        });

        jButtonOpenMulti.setFont(new java.awt.Font("DejaVu Sans", 0, 18)); // NOI18N
        jButtonOpenMulti.setText("Open ");
        jButtonOpenMulti.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOpenMultiActionPerformed(evt);
            }
        });

        jButtonNewMulti.setFont(new java.awt.Font("DejaVu Sans", 0, 18)); // NOI18N
        jButtonNewMulti.setText("New ");
        jButtonNewMulti.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonNewMultiActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelMultiWorkcellSystemLayout = new javax.swing.GroupLayout(jPanelMultiWorkcellSystem);
        jPanelMultiWorkcellSystem.setLayout(jPanelMultiWorkcellSystemLayout);
        jPanelMultiWorkcellSystemLayout.setHorizontalGroup(
            jPanelMultiWorkcellSystemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelMultiWorkcellSystemLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelMultiWorkcellSystemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonOpenMulti, javax.swing.GroupLayout.PREFERRED_SIZE, 442, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonNewMulti, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonPrevMulti, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanelMultiWorkcellSystemLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jButtonNewMulti, jButtonOpenMulti, jButtonPrevMulti});

        jPanelMultiWorkcellSystemLayout.setVerticalGroup(
            jPanelMultiWorkcellSystemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelMultiWorkcellSystemLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButtonPrevMulti)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonNewMulti)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButtonOpenMulti)
                .addContainerGap())
        );

        jPanelMultiWorkcellSystemLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jButtonNewMulti, jButtonOpenMulti, jButtonPrevMulti});

        jPanelSingleWorkcellSystem.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Single Workcell System", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("DejaVu Sans", 1, 24))); // NOI18N

        jButtonNewSingle.setFont(new java.awt.Font("DejaVu Sans", 0, 18)); // NOI18N
        jButtonNewSingle.setText("New ");
        jButtonNewSingle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonNewSingleActionPerformed(evt);
            }
        });

        jButtonPrevSingle.setFont(new java.awt.Font("DejaVu Sans", 0, 18)); // NOI18N
        jButtonPrevSingle.setText("Previous ");
        jButtonPrevSingle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPrevSingleActionPerformed(evt);
            }
        });

        jButtonOpenSingle.setFont(new java.awt.Font("DejaVu Sans", 0, 18)); // NOI18N
        jButtonOpenSingle.setText("Open ");
        jButtonOpenSingle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOpenSingleActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelSingleWorkcellSystemLayout = new javax.swing.GroupLayout(jPanelSingleWorkcellSystem);
        jPanelSingleWorkcellSystem.setLayout(jPanelSingleWorkcellSystemLayout);
        jPanelSingleWorkcellSystemLayout.setHorizontalGroup(
            jPanelSingleWorkcellSystemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSingleWorkcellSystemLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelSingleWorkcellSystemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonOpenSingle, javax.swing.GroupLayout.PREFERRED_SIZE, 442, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonNewSingle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonPrevSingle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelSingleWorkcellSystemLayout.setVerticalGroup(
            jPanelSingleWorkcellSystemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSingleWorkcellSystemLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButtonPrevSingle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonNewSingle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonOpenSingle)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelTestsDemos.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Tests/Quick Demos", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("DejaVu Sans", 1, 24))); // NOI18N

        jButtonOptaplannerTest.setFont(new java.awt.Font("DejaVu Sans", 0, 18)); // NOI18N
        jButtonOptaplannerTest.setText("Optaplanner Test");
        jButtonOptaplannerTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOptaplannerTestActionPerformed(evt);
            }
        });

        jButtonGoalLearningTest.setFont(new java.awt.Font("DejaVu Sans", 0, 18)); // NOI18N
        jButtonGoalLearningTest.setText("Goal Learning Test");
        jButtonGoalLearningTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGoalLearningTestActionPerformed(evt);
            }
        });

        jButtonOpenSingle1.setFont(new java.awt.Font("DejaVu Sans", 0, 18)); // NOI18N
        jButtonOpenSingle1.setText("...");
        jButtonOpenSingle1.setEnabled(false);
        jButtonOpenSingle1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOpenSingle1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelTestsDemosLayout = new javax.swing.GroupLayout(jPanelTestsDemos);
        jPanelTestsDemos.setLayout(jPanelTestsDemosLayout);
        jPanelTestsDemosLayout.setHorizontalGroup(
            jPanelTestsDemosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTestsDemosLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelTestsDemosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonOpenSingle1, javax.swing.GroupLayout.PREFERRED_SIZE, 442, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonOptaplannerTest, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonGoalLearningTest, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelTestsDemosLayout.setVerticalGroup(
            jPanelTestsDemosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTestsDemosLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButtonGoalLearningTest)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonOptaplannerTest)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonOpenSingle1)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelSingleWorkcellSystem, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanelMultiWorkcellSystem, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanelTestsDemos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelMultiWorkcellSystem, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanelSingleWorkcellSystem, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanelTestsDemos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonPrevMultiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPrevMultiActionPerformed
        try {
            prevMulti();
            this.setVisible(false);
            this.dispose();
        } catch (Exception e) {
            Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, null, e);
            JOptionPane.showMessageDialog(this, "Exception caught: "+e);
        }

    }//GEN-LAST:event_jButtonPrevMultiActionPerformed

    private static void prevMulti() {
        AprsSupervisorJFrame amsFrame = new AprsSupervisorJFrame();
        amsFrame.startColorTextReader();
        amsFrame.loadPrevSetup();
        amsFrame.loadPrevPosMapFile();
        amsFrame.loadPrevSimTeach();
        amsFrame.loadPrevTeachProperties();
        amsFrame.setVisible(true);
    }

    private void jButtonNewMultiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonNewMultiActionPerformed
        newMulti();
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_jButtonNewMultiActionPerformed

    private static void newMulti() {
        AprsSupervisorJFrame amsFrame = new AprsSupervisorJFrame();
        amsFrame.startColorTextReader();
        amsFrame.setVisible(true);
        amsFrame.browseSaveSetupAs();
    }

    private void jButtonOpenMultiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOpenMultiActionPerformed
        try {
            openMulti(null);
            this.setVisible(false);
            this.dispose();
        } catch (IOException ex) {
            Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonOpenMultiActionPerformed

    private static void openMulti(String args @Nullable []) throws IOException {
        AprsSupervisorJFrame amsFrame = new AprsSupervisorJFrame();
        amsFrame.startColorTextReader();
        amsFrame.setVisible(true);
        if (null == args || args.length < 1) {
            amsFrame.browseOpenSetup();
        } else {
            amsFrame.loadSetupFile(new File(args[0]));
        }
        amsFrame.loadPrevPosMapFile();
        amsFrame.loadPrevSimTeach();
        amsFrame.loadPrevTeachProperties();
    }

    private void jButtonPrevSingleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPrevSingleActionPerformed
        prevSingle();
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_jButtonPrevSingleActionPerformed

    private static void goalLearningTest() {
        GoalLearnerTest.main(new String[]{});
    }

    private static void optaplannerTest() {
        OptaplannerTest.main(new String[]{});
    }

    private static void prevSingle() {
        AprsJFrame aFrame = new AprsJFrame();
        aFrame.defaultInit();
        aFrame.setVisible(true);
    }

    private void jButtonNewSingleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonNewSingleActionPerformed
        newSingle();
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_jButtonNewSingleActionPerformed

    private static void newSingle() {
        AprsJFrame aFrame = new AprsJFrame();
        aFrame.emptyInit();
        aFrame.setVisible(true);
    }

    private void jButtonOpenSingleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOpenSingleActionPerformed
        try {
            openSingle(null);
            this.setVisible(false);
            this.dispose();
        } catch (IOException ex) {
            Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonOpenSingleActionPerformed

    private void jButtonOptaplannerTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOptaplannerTestActionPerformed
        this.setVisible(false);
        optaplannerTest();
        this.dispose();
    }//GEN-LAST:event_jButtonOptaplannerTestActionPerformed

    private void jButtonGoalLearningTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGoalLearningTestActionPerformed
        this.setVisible(false);
        goalLearningTest();
        this.dispose();
    }//GEN-LAST:event_jButtonGoalLearningTestActionPerformed

    private void jButtonOpenSingle1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOpenSingle1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButtonOpenSingle1ActionPerformed

    private static void openSingle(String args @Nullable []) throws IOException {
        AprsJFrame aFrame = new AprsJFrame();
        aFrame.emptyInit();
        aFrame.setVisible(true);
        if (null == args || args.length < 1) {
            aFrame.browseOpenPropertiesFile();
        } else {
            aFrame.closeAllWindows();
            aFrame.setPropertiesFile(new File(args[0]));
            aFrame.loadProperties();
        }
    }

    private void checkFiles() {
        File f = AprsJFrame.getDefaultPropertiesFile();
        if (f != null && f.exists()) {
            try {
                jButtonPrevSingle.setToolTipText("Open " + f.getCanonicalPath() + " ");
                jButtonPrevSingle.setEnabled(true);
            } catch (IOException ex) {
                Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            jButtonPrevSingle.setEnabled(false);
        }
        f = null;
        try {
            f = AprsSupervisorJFrame.getLastSetupFile();
        } catch (IOException ex) {
            Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (f != null && f.exists()) {
            try {
                jButtonPrevMulti.setToolTipText("Open " + f.getCanonicalPath() + " ");
                jButtonPrevMulti.setEnabled(true);
            } catch (IOException ex) {
                Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            jButtonPrevMulti.setEnabled(false);
        }
    }

    
    
    
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
            java.util.logging.Logger.getLogger(LauncherAprsJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(LauncherAprsJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(LauncherAprsJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(LauncherAprsJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                if (null != args && args.length > 0) {
                    try {
                        String argsLeft[] = copyOfRangeNonNullsOnly(String.class,args, 1, args.length);
                        switch (args[0]) {
                            case "--prevMulti":
                                prevMulti();
                                break;

                            case "--openMulti":
                                openMulti(argsLeft);
                                break;

                            case "--newMulti":
                                newMulti();
                                break;

                            case "--prevSingle":
                                prevSingle();
                                break;

                            case "--openSingle":
                                openSingle(argsLeft);
                                break;

                            case "--newSingle":
                                newSingle();
                                break;

                            default:
                                System.err.println("Invalid argumens args=" + Arrays.toString(args));
                                System.err.println("args[0] must be one of:");
                                System.err.println("--prevMulti");
                                System.err.println("--openMulti");
                                System.err.println("--newMulti");
                                System.err.println("--prevSingle");
                                System.err.println("--openSingle");
                                System.err.println("--newSingle");
                                System.err.println("");
                                break;
                        }
                        return;
                    } catch (IOException ex) {
                        Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                LauncherAprsJFrame lFrame = new LauncherAprsJFrame();
                lFrame.checkFiles();
                lFrame.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonGoalLearningTest;
    private javax.swing.JButton jButtonNewMulti;
    private javax.swing.JButton jButtonNewSingle;
    private javax.swing.JButton jButtonOpenMulti;
    private javax.swing.JButton jButtonOpenSingle;
    private javax.swing.JButton jButtonOpenSingle1;
    private javax.swing.JButton jButtonOptaplannerTest;
    private javax.swing.JButton jButtonPrevMulti;
    private javax.swing.JButton jButtonPrevSingle;
    private javax.swing.JPanel jPanelMultiWorkcellSystem;
    private javax.swing.JPanel jPanelSingleWorkcellSystem;
    private javax.swing.JPanel jPanelTestsDemos;
    // End of variables declaration//GEN-END:variables
}
