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
package aprs.launcher;

import aprs.misc.Utils;
import aprs.supervisor.main.Supervisor;
import static aprs.supervisor.main.Supervisor.createAprsSupervisorWithSwingDisplay;
import static aprs.misc.Utils.copyOfRangeNonNullsOnly;
import static aprs.misc.Utils.readFirstLine;
import aprs.learninggoals.GoalLearnerTest;
import aprs.actions.optaplanner.display.OptaplannerTest;
import aprs.misc.AprsCommonLogger;
import aprs.misc.IconImages;
import static aprs.misc.Utils.PlayAlert;
import aprs.system.AprsSystem;
import crcl.ui.XFuture;
import java.awt.Frame;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings({"unused", "guieffect"})
public class LauncherAprsJFrame extends javax.swing.JFrame {

    /**
     * Creates new form LauncherJFrame
     */
    @SuppressWarnings("initialization")
    @UIEffect
    public LauncherAprsJFrame() {
        AprsCommonLogger.instance();
        initComponents();
        try {
            setIconImage(IconImages.BASE_IMAGE);
        } catch (Exception ex) {
            Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "nullness", "MagicConstant"})
    @UIEffect
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
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuSpecialTests = new javax.swing.JMenu();
        jMenuItemTenCycleMultiSystemTest = new javax.swing.JMenuItem();
        jMenuItemTenCycleMultiSystemTestNoDisables = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jCheckBoxMenuItemLaunchExternal = new javax.swing.JCheckBoxMenuItem();
        jMenuItemSetLaunchFile = new javax.swing.JMenuItem();

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

        javax.swing.GroupLayout jPanelTestsDemosLayout = new javax.swing.GroupLayout(jPanelTestsDemos);
        jPanelTestsDemos.setLayout(jPanelTestsDemosLayout);
        jPanelTestsDemosLayout.setHorizontalGroup(
            jPanelTestsDemosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTestsDemosLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelTestsDemosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonOptaplannerTest, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonGoalLearningTest, javax.swing.GroupLayout.DEFAULT_SIZE, 442, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelTestsDemosLayout.setVerticalGroup(
            jPanelTestsDemosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTestsDemosLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButtonGoalLearningTest)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonOptaplannerTest)
                .addContainerGap())
        );

        jMenu1.setText("File");
        jMenuBar1.add(jMenu1);

        jMenuSpecialTests.setText("Special Tests");

        jMenuItemTenCycleMultiSystemTest.setText("10 Cycle Multi-System with Random Enable/Disable Test ");
        jMenuItemTenCycleMultiSystemTest.setEnabled(false);
        jMenuItemTenCycleMultiSystemTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemTenCycleMultiSystemTestActionPerformed(evt);
            }
        });
        jMenuSpecialTests.add(jMenuItemTenCycleMultiSystemTest);

        jMenuItemTenCycleMultiSystemTestNoDisables.setText("10 Cycle Multi-System without Random Enable/Disable Test ");
        jMenuItemTenCycleMultiSystemTestNoDisables.setEnabled(false);
        jMenuItemTenCycleMultiSystemTestNoDisables.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemTenCycleMultiSystemTestNoDisablesActionPerformed(evt);
            }
        });
        jMenuSpecialTests.add(jMenuItemTenCycleMultiSystemTestNoDisables);

        jMenuBar1.add(jMenuSpecialTests);

        jMenu2.setText("Launcher Settings");

        jCheckBoxMenuItemLaunchExternal.setText("Launch External Processes First");
        jMenu2.add(jCheckBoxMenuItemLaunchExternal);

        jMenuItemSetLaunchFile.setText("Set Launch File ...");
        jMenuItemSetLaunchFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSetLaunchFileActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItemSetLaunchFile);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

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

    @UIEffect
    private void jButtonPrevMultiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPrevMultiActionPerformed
        try {
            if (jCheckBoxMenuItemLaunchExternal.isSelected()) {
                prevMulti(getLastLaunchFile());
            } else {
                prevMulti(null);
            }
            this.setVisible(false);
            this.dispose();
        } catch (Exception e) {
            Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, "", e);
            JOptionPane.showMessageDialog(this, "Exception caught: " + e);
        }
    }//GEN-LAST:event_jButtonPrevMultiActionPerformed

    private final static File lastLaunchFileFile = new File(System.getProperty("aprsLastLaunchFile", Utils.getAprsUserHomeDir() + File.separator + ".lastAprsLaunchFile.txt"));

    /**
     * Get the location of the last text file with lines to execute in the
     * launcher file used.
     *
     * @return setup file location
     * @throws IOException setup files location can not be read
     */
    @Nullable
    private static File getLastLaunchFile() throws IOException {
        if (lastLaunchFileFile.exists()) {
            String firstLine = readFirstLine(lastLaunchFileFile);
            if (null != firstLine && firstLine.length() > 0) {
                return new File(firstLine);
            }
        }
        return null;
    }

    @Nullable
    private File lastLaunchFile = null;

    private void saveLastLaunchFile(File f) throws IOException {
        lastLaunchFile = f;
        try (PrintWriter pw = new PrintWriter(new FileWriter(lastLaunchFileFile))) {
            pw.println(f.getCanonicalPath());
        }
    }

    @UIEffect
    private static void prevMulti(@Nullable File launchFile) {
        Supervisor supervisor = createAprsSupervisorWithSwingDisplay();
        if (null != launchFile) {
            try {
                ProcessLauncherJFrame processLauncher = new ProcessLauncherJFrame();
                processLauncher.setVisible(true);
                processLauncher.run(launchFile)
                        .thenRun(() -> {
                            supervisor.setProcessLauncher(processLauncher);
                            Utils.runOnDispatchThread(() -> supervisor.completePrevMulti());
                        });
            } catch (IOException ex) {
                Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, "", ex);
            }
        } else {
            supervisor.completePrevMulti();
        }
    }

    

    @UIEffect
    private void jButtonNewMultiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonNewMultiActionPerformed
        newMulti();
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_jButtonNewMultiActionPerformed

    private static void newMulti() {
        Supervisor supervisor = createAprsSupervisorWithSwingDisplay();
        supervisor.startColorTextReader();
        supervisor.setVisible(true);
        supervisor.browseSaveSetupAs();
    }

    @UIEffect
    private void jButtonOpenMultiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOpenMultiActionPerformed
        try {
            boolean getLauncher = JOptionPane.showConfirmDialog(this, "Select a launcher text file?") == JOptionPane.YES_OPTION;
            
            if (getLauncher) {
                File launcherFile = null;
                try {
                    launcherFile = getLastLaunchFile();
                } catch (IOException iOException) {
                    Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, "", iOException);
                }
                JFileChooser launcherFileChooser;
                if(null != launcherFile) {
                    launcherFileChooser = new JFileChooser(launcherFile);
                } else {
                    launcherFileChooser = new JFileChooser();
                }
                FileNameExtensionFilter txtExtensionFilter = new FileNameExtensionFilter("txt", "txt");
                launcherFileChooser.addChoosableFileFilter(txtExtensionFilter);
                launcherFileChooser.setFileFilter(txtExtensionFilter);
                launcherFileChooser.setDialogTitle("Choose launch text file for Aprs.");
                if (launcherFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    launcherFile = launcherFileChooser.getSelectedFile();
                    saveLastLaunchFile(launcherFile);
                    openMultiWithLaunchFile(launcherFile,null, this);
                } else {
                    openMultiWithoutLaunchFile(null,this,null);
                }
            } else {
                openMultiWithoutLaunchFile(null,this,null);
            }
            this.setVisible(false);
            this.dispose();
        } catch (IOException ex) {
            Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jButtonOpenMultiActionPerformed

    @UIEffect
    private static void openMulti(String args @Nullable []) throws IOException {
        File launcherFile = null;
        File setupFile = null;
        if (null != args && args.length > 0) {
            setupFile = new File(args[0]);
            if (args.length > 1) {
                launcherFile = new File(args[1]);
                openMultiWithLaunchFile(launcherFile, setupFile,null);
            } else {
                openMultiWithoutLaunchFile(setupFile,null,null);
            }
        } else {
            openMultiWithoutLaunchFile(setupFile,null,null);
        }
    }

    @UIEffect
    private static void openMultiWithLaunchFile(File launcherFile, @Nullable File setupFile, @Nullable Frame parent) throws IOException {

        try {
            ProcessLauncherJFrame processLauncher = new ProcessLauncherJFrame();
            processLauncher.setVisible(true);
            processLauncher.run(launcherFile)
                    .thenRun(() -> {
                        try {
                            XFuture<Supervisor> supervisorFuture = openMultiWithoutLaunchFile(setupFile,parent, launcherFile.getParent());
                            supervisorFuture.thenAccept((Supervisor supervisor) -> {
                                supervisor.setProcessLauncher(processLauncher);
                            });
                        } catch (Exception iOException) {
                            iOException.printStackTrace();
                        }
                    });
        } catch (IOException ex) {
            Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    @UIEffect
    private static XFuture<Supervisor> openMultiWithoutLaunchFile(@Nullable File setupFile, @Nullable Frame parent,@Nullable String dirName) throws IOException {
        if (null == setupFile) {
            return Supervisor.openAll(null, parent, dirName)
                    .thenApply((Supervisor supervisor) -> {
                        supervisor.startColorTextReader();
                        supervisor.setVisible(true);
                        return supervisor;
                    });
        } else {
            Supervisor supervisor
                    = createAprsSupervisorWithSwingDisplay();
            supervisor.startColorTextReader();
            supervisor.setVisible(true);
            return supervisor.loadSetupFile(setupFile)
                    .thenRun(() -> completeOpenSupevisor(supervisor,setupFile.getParent()))
                    .thenApply(x -> supervisor);
        }
    }

    private static void completeOpenSupevisor(Supervisor supervisor, @Nullable String dirName) {
        supervisor.loadPrevPosMapFile(dirName);
        supervisor.loadPrevSimTeach();
        supervisor.loadPrevTeachProperties(dirName)
                .thenRun(() -> PlayAlert());
    }

    @UIEffect
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
        AprsSystem.createPrevSystem()
                .thenAccept((AprsSystem sys) -> sys.setVisible(true));
    }

    @UIEffect
    private void jButtonNewSingleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonNewSingleActionPerformed
        newSingle();
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_jButtonNewSingleActionPerformed

    private static void newSingle() {
        AprsSystem.createEmptySystem()
                .thenAccept((AprsSystem sys) -> sys.setVisible(true));
    }

    @UIEffect
    private void jButtonOpenSingleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOpenSingleActionPerformed
        try {
            openSingle(null);
            this.setVisible(false);
            this.dispose();
        } catch (Exception ex) {
            Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jButtonOpenSingleActionPerformed

    @UIEffect
    private void jButtonOptaplannerTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOptaplannerTestActionPerformed
        this.setVisible(false);
        optaplannerTest();
        this.dispose();
    }//GEN-LAST:event_jButtonOptaplannerTestActionPerformed

    @UIEffect
    private void jButtonGoalLearningTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGoalLearningTestActionPerformed
        this.setVisible(false);
        goalLearningTest();
        this.dispose();
    }//GEN-LAST:event_jButtonGoalLearningTestActionPerformed

    private static void tenCycleTestNoDisables() {
        long startTime = System.currentTimeMillis();
        Supervisor supervisor = createAprsSupervisorWithSwingDisplay();
        supervisor.tenCycleTestNoDisables(startTime);
    }
    
    private static void tenCycleTest(@Nullable File launchFile) {
        long startTime = System.currentTimeMillis();
        Supervisor supervisor = Supervisor.createSupervisor();

        if (null != launchFile) {
            try {
                ProcessLauncherJFrame processLauncher = new ProcessLauncherJFrame();
                processLauncher.setVisible(true);
                processLauncher.run(launchFile)
                        .thenRun(() -> {
                            supervisor.setProcessLauncher(processLauncher);
                            Utils.runOnDispatchThread(() -> supervisor.completeTenCycleTestWithPrevMulti(startTime));
                        });
            } catch (IOException ex) {
                Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, "", ex);
            }
        } else {
            supervisor.completeTenCycleTestWithPrevMulti( startTime);
        }
    }

    
    @UIEffect
    private void jMenuItemTenCycleMultiSystemTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemTenCycleMultiSystemTestActionPerformed
        this.setVisible(false);
        if (jCheckBoxMenuItemLaunchExternal.isSelected()) {
            try {
                tenCycleTest(getLastLaunchFile());
            } catch (IOException ex) {
                Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, "", ex);
            }
        } else {
            tenCycleTest(null);
        }
    }//GEN-LAST:event_jMenuItemTenCycleMultiSystemTestActionPerformed

    @UIEffect
    private void jMenuItemTenCycleMultiSystemTestNoDisablesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemTenCycleMultiSystemTestNoDisablesActionPerformed
        this.setVisible(false);
        tenCycleTestNoDisables();
    }//GEN-LAST:event_jMenuItemTenCycleMultiSystemTestNoDisablesActionPerformed

    @UIEffect
    private void jMenuItemSetLaunchFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSetLaunchFileActionPerformed
        File oldFile = null;
        try {
            oldFile= getLastLaunchFile();
        } catch (IOException ex) {
            Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        JFileChooser chooser;
        if(null !=oldFile) {
            chooser = new JFileChooser(oldFile);
        } else {
            chooser = new JFileChooser();
        }
        FileNameExtensionFilter txtExtensionFilter = new FileNameExtensionFilter("txt", "txt");
        chooser.addChoosableFileFilter(txtExtensionFilter);
        chooser.setFileFilter(txtExtensionFilter);
        chooser.setDialogTitle("Choose launch text file for Aprs.");
        try {
            
            if (null != oldFile) {
                File parentFile = oldFile.getParentFile();
                if (null != parentFile) {
                    chooser.setCurrentDirectory(parentFile);
                }
            }
            if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(this)) {
                saveLastLaunchFile(chooser.getSelectedFile());
            }
        } catch (IOException iOException) {
            Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, "", iOException);
        }
    }//GEN-LAST:event_jMenuItemSetLaunchFileActionPerformed

    private static void openSingle(String args @Nullable []) {
        AprsSystem.createEmptySystem()
                .thenAccept((AprsSystem sys) -> openSingleStep2(sys, args));
    }

    private static void openSingleStep2(AprsSystem aprsSys, String @Nullable [] args) {
        try {
            aprsSys.setVisible(true);
            if (null == args || args.length < 1) {
                aprsSys.browseOpenPropertiesFile();
            } else {
                aprsSys.closeAllWindows();
                aprsSys.setPropertiesFile(new File(args[0]));
                aprsSys.loadProperties();
            }
        } catch (Exception ex) {
            Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }

    private void checkFiles() {
        File f = AprsSystem.getDefaultPropertiesFile();
        if (f != null && f.exists()) {
            try {
                jButtonPrevSingle.setToolTipText("Open " + f.getCanonicalPath() + " ");
                jButtonPrevSingle.setEnabled(true);
            } catch (IOException ex) {
                Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, "", ex);
            }
        } else {
            jButtonPrevSingle.setEnabled(false);
        }
        f = null;
        try {
            f = Supervisor.getLastSetupFile(null);
        } catch (IOException ex) {
            Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, "", ex);
        }
        if (f != null && f.exists()) {
            try {
                jButtonPrevMulti.setToolTipText("Open " + f.getCanonicalPath() + " ");
                jButtonPrevMulti.setEnabled(true);
                jMenuItemTenCycleMultiSystemTest.setEnabled(true);
                jMenuItemTenCycleMultiSystemTestNoDisables.setEnabled(true);
            } catch (IOException ex) {
                Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, "", ex);
            }
        } else {
            jButtonPrevMulti.setEnabled(false);
            jMenuItemTenCycleMultiSystemTest.setEnabled(true);
            jMenuItemTenCycleMultiSystemTestNoDisables.setEnabled(true);
        }
        f = null;
        try {
            f = getLastLaunchFile();
        } catch (IOException ex) {
            Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, "", ex);
        }
        if (f != null && f.exists()) {
            jCheckBoxMenuItemLaunchExternal.setSelected(true);
        }
    }

    

//    static public void PlayBeep() {
//        URL url = LauncherAprsJFrame.class.getResource("alert.wav");
//        System.out.println("url = " + url);
//        AudioClip clip = Applet.newAudioClip(url);
//        clip.play();
//    }
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
        } catch (ClassNotFoundException | javax.swing.UnsupportedLookAndFeelException | IllegalAccessException | InstantiationException ex) {
            java.util.logging.Logger.getLogger(LauncherAprsJFrame.class.getName()).log(java.util.logging.Level.SEVERE, "", ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                if (null != args && args.length > 0) {
                    File launchFile = null;
                    try {
                        String argsLeft[] = copyOfRangeNonNullsOnly(String.class, args, 1, args.length);
                        switch (args[0]) {
                            case "--prevMulti":
                                if (argsLeft.length > 0) {
                                    prevMulti(new File(argsLeft[0]));
                                } else {
                                    prevMulti(null);
                                }
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

                            case "--tenCycleTest":
                                if (argsLeft.length > 0) {
                                    tenCycleTest(new File(argsLeft[0]));
                                } else {
                                    tenCycleTest(null);
                                }
                                break;

                            default:
                                System.err.println("Invalid argumens args=" + Arrays.toString(args));
                                System.err.println("args[0] = " + args[0]);
                                System.err.println("args[0] must be one of:");
                                System.err.println("--prevMulti");
                                System.err.println("--openMulti");
                                System.err.println("--newMulti");
                                System.err.println("--prevSingle");
                                System.err.println("--openSingle");
                                System.err.println("--newSingle");
                                System.err.println();
                                break;
                        }
                        return;
                    } catch (IOException ex) {
                        Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, "", ex);
                    }
                }
                if (!GraphicsEnvironment.isHeadless()) {
                    LauncherAprsJFrame lFrame = new LauncherAprsJFrame();
                    lFrame.checkFiles();
                    lFrame.setVisible(true);
                } else {
                    System.err.println("Can't launch interactive launcher in headless environment!!!");
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonGoalLearningTest;
    private javax.swing.JButton jButtonNewMulti;
    private javax.swing.JButton jButtonNewSingle;
    private javax.swing.JButton jButtonOpenMulti;
    private javax.swing.JButton jButtonOpenSingle;
    private javax.swing.JButton jButtonOptaplannerTest;
    private javax.swing.JButton jButtonPrevMulti;
    private javax.swing.JButton jButtonPrevSingle;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemLaunchExternal;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItemSetLaunchFile;
    private javax.swing.JMenuItem jMenuItemTenCycleMultiSystemTest;
    private javax.swing.JMenuItem jMenuItemTenCycleMultiSystemTestNoDisables;
    private javax.swing.JMenu jMenuSpecialTests;
    private javax.swing.JPanel jPanelMultiWorkcellSystem;
    private javax.swing.JPanel jPanelSingleWorkcellSystem;
    private javax.swing.JPanel jPanelTestsDemos;
    // End of variables declaration//GEN-END:variables
}
